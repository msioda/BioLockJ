/**
 * @UNCC Fodor Lab
 * @author Ivory Blakley
 * @email ieclabau@uncc.edu
 * @date May 27, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.ConfigViolationException;
import biolockj.exception.ExpectationFileFormatException;
import biolockj.exception.ValidationException;
import biolockj.module.BioModule;

/**
 * This utility measures attributes of the files in the output directory of each module as it completes. If there is no
 * expectation file, the utility reports the values for these attributes and saves a file in the
 * {@value #VALIDATION_FOLDER} folder which can be used as an expectation in future runs. If there is an expectation
 * file, the values for the current outputs will be compared with the expected values and the utility will stop the
 * pipeline if the files do not meet expectations. Set {@value #ALWAYS_PASS} to Y to report on the comparison but not
 * stop the pipeline.
 * 
 * This is an easy way to verify that the outputs of a pipeline are the same as a previous run, which is particularly
 * helpful after software has been updated, or after any other changes that are not expected to alter results.
 * 
 * To provide an expectation file, use {@link biolockj.Config} property {@value #EXPECTATION_FILE} and give the file
 * path to the {@value #VALIDATION_FOLDER} folder of the run that you want to match. To specify which file attributes
 * are reported, use {@link biolockj.Config} property {@value #REPORT_ON}. To specify which file attributes are used for
 * the comparison, use use {@link biolockj.Config} property {@value #COMPARE_ON}.
 * 
 * @author Ivory
 *
 */
public class ValidationUtil
{

	private static class FileSummary
	{

		private File file = null;

		private String md5 = null;

		private final String name;

		/**
		 * The size of the file in bytes
		 */
		private long size = -1;

		/**
		 * The size rounded to the nearest GB.
		 */
		private long sizeGB = -1;

		/**
		 * The size rounded to the nearest MB.
		 */
		private long sizeMB = -1;

		private int validationStatus = ValidationUtil.REPORT;

		public FileSummary( final File inFile ) throws Exception
		{
			file = inFile;
			name = file.getName();
			size = file.length();
			sizeMB = Math.round( size / Math.pow( 1024, 2 ) );
			sizeGB = Math.round( size / Math.pow( 1024, 3 ) );
		}

		public FileSummary( final String fileName )
		{
			name = fileName;
		}

		protected void calcMd5() throws IOException, NoSuchAlgorithmException
		{
			final MessageDigest md = MessageDigest.getInstance( "MD5" );
			final InputStream fis = new FileInputStream( file.getAbsoluteFile() );
			final byte[] bytes = new byte[ 1024 ];
			int numRead;
			do
			{
				numRead = fis.read( bytes );
				if( numRead > 0 )
				{
					md.update( bytes, 0, numRead );
				}
			}
			while( numRead != -1 );
			fis.close();
			final byte[] digest = md.digest();

			String md5sum = "";
			for( final byte element: digest )
			{
				md5sum += Integer.toString( ( element & 0xff ) + 0x100, 16 ).substring( 1 );
			}
			md5 = md5sum;
		}

		public int compareToExpected( final FileSummary other, final Collection<String> comparisons ) throws Exception
		{
			if( !ValidationUtil.availableAttributes.containsAll( comparisons ) )
			{
				Log.error( this.getClass(),
						"Cannot do comparisons on: " + comparisons.removeAll( ValidationUtil.availableAttributes ) );
				throw new Exception();
			}
			if( other == null )
			{
				Log.warn( this.getClass(), "Cannot compare against a null." );
				validationStatus = ValidationUtil.FAIL;
				return ValidationUtil.FAIL;
			}
			int tested = 0;
			int passed = 0;

			for( final String att: ValidationUtil.availableAttributes )
			{
				if( comparisons.contains( att ) )
				{
					tested++;
					try
					{
						if( other.getAtt( att ).equals( getAtt( att ) ) )
						{
							passed++;
						}
						else
						{
							mentionMismatch( att, other.getAtt( att ), getAtt( att ) );
						}
					}
					catch( final NullPointerException np )
					{
						Log.warn( this.getClass(), "Cannot compare missing attribute: " + att );
					}

				}
			}

			if( tested == passed && passed == comparisons.size() )
			{
				validationStatus = ValidationUtil.PASS;
				return ValidationUtil.PASS;
			}
			else
			{
				validationStatus = ValidationUtil.FAIL;
				Log.error( this.getClass(),
						"File " + name + " passed only " + passed + " out of " + comparisons.size() + " comparisons." );
				return ValidationUtil.FAIL;
			}
		}

		protected String getAtt( final String col ) throws Exception
		{
			switch( col )
			{
				case "name":
					return getName();
				case "size":
					return String.valueOf( size );
				case "sizeMB":
					return String.valueOf( sizeMB );
				case "sizeGB":
					return String.valueOf( sizeGB );
				case "md5":
					return md5;
			}
			if( col.equals( ValidationUtil.MATCHED_EXPECTATION ) )
			{
				return ValidationUtil.statusStrings[ validationStatus ];
			}
			return null;
		}

		public String getName()
		{
			return name;
		}

		private void mentionMismatch( final String attribute, final Object expectedValue, final Object foundValue )
		{
			Log.debug( this.getClass(), "File " + name + " failed " + attribute + " comparison.  Expected value: "
					+ expectedValue + "; found: " + foundValue + "." );
		}

		protected void setAtt( final String col, final String val ) throws Exception
		{
			Log.debug( this.getClass(), "Setting " + col + " to " + val );
			if( !ValidationUtil.availableAttributes.contains( col ) )
			{
				throw new ConfigViolationException(
						"Available file attributes for validation are: " + ValidationUtil.availableAttributes );
			}
			switch( col )
			{
				case "size":
					size = Long.parseLong( val );
					break;
				case "sizeMB":
					sizeMB = Long.parseLong( val );
					break;
				case "sizeGB":
					sizeGB = Long.parseLong( val );
					break;
				case "md5":
					md5 = val;
					break;
			}
		}

		@Override
		public String toString()
		{
			return "FileSummary [file=" + file + ", name=" + name + ", size=" + size + ", validationStatus="
					+ ValidationUtil.statusStrings[ validationStatus ] + "]";
		}

	}

	/**
	 * {@link biolockj.Config} boolean property. Even if the files do not meet expectation, just record this, do not
	 * halt the pipeline.
	 */
	protected static final String ALWAYS_PASS = "validation.alwaysPass";

	/**
	 * {@link biolockj.Config} set property giving the file metrics to use in comparing to the expectation. Default is
	 * to use all metrics in the expectation file.
	 */
	protected static final String COMPARE_ON = "validation.compareOn";

	/**
	 * {@link biolockj.Config} boolean property {@value #DISABLE_VALIDATION}. Disable validation for the all modules in
	 * the pipeline. Can be specified for individual modules to override pipeline setting.
	 */
	private static final String DISABLE_VALIDATION = "validation.disableValidation";

	/**
	 * {@link biolockj.Config} String property giving the file path that gives the expected values for file metrics.
	 * Probably generated by a previous run of the same pipeline.
	 */
	public static final String EXPECTATION_FILE = "validation.expectationFile";

	/**
	 * The last column in a the output file, {@value #MATCHED_EXPECTATION}, indicates if the referenced file met all
	 * expectations (PASS), or not (FAIL), or was not
	 * compared to any expectations (REPORT). The value "MATCHED_EXPECTATION" is
	 * hard-coded in the FileSummary.getAtt() method. If this is a column in the expectation file, it is ignored.
	 */
	protected static final String MATCHED_EXPECTATION = "MATCHED_EXPECTATION";

	/**
	 * The first column in an expectation file must be {@value #NAME}
	 */
	protected static final String NAME = "name";

	/**
	 * Append the String {@value #OUTPUT_FILE_SUFFIX} to the name of the validated module to get the output file name.
	 */
	protected static final String OUTPUT_FILE_SUFFIX = "_validation.txt";
	
	protected static final int FAIL = 0;

	protected static final int PASS = 1;

	protected static final int REPORT = 2;

	/**
	 * {@link biolockj.Config} property {@value #REPORT_ON} giving the set of file metrics to use report. Default is to use all currently available metrics.
	 */
	protected static final String REPORT_ON = "validation.reportOn";

	private static final String[] sa = { "name", "size", "sizeMB", "sizeGB", "md5" };
	protected static final ArrayList<String> availableAttributes = new ArrayList<>( Arrays.asList( sa ) );

	protected static final String[] statusStrings = { "FAIL", "PASS", "REPORT" };

	protected static final String VALIDATION_FOLDER = "validation";

	private static boolean alwaysPass( final BioModule module ) throws ConfigFormatException
	{
		return Config.getBoolean( module, ALWAYS_PASS );
	}

	/**
	 * This method calls the methods that are used by executeTask to get the validation input. The goal is to ensure
	 * that IF the validation utility is going to fail due to any kind of problem with the validation input, it fails
	 * before the pipeline starts so it can be corrected immediately.
	 * 
	 * @param module BioModule to check validation dependencies for
	 * @throws Exception if errors occur
	 */
	public static void checkDependencies( final BioModule module ) throws Exception
	{
		if( !Config.getBoolean( module, DISABLE_VALIDATION ) )
		{
			if( hasExp( module ) )
			{
				Log.info( ValidationUtil.class, "The " + module.getClass().getSimpleName()
						+ " module is expected to produce " + getPrevSummaries( module ).size() + " output files." );
			}
			getReportSet( module );
			if( alwaysPass( module ) )
			{
				Log.info( ValidationUtil.class,
						"The pipeline will continue even if module outputs from [" + module.getID() + "_"
								+ module.getClass().getSimpleName() + "] do no match the expectation file." );
			}
		}
	}

	public static void executeTask( final BioModule module ) throws Exception
	{
		if( !Config.getBoolean( module, DISABLE_VALIDATION ) )
		{
			HashMap<String, FileSummary> prevOutput = new HashMap<>();
			if( hasExp( module ) )
			{
				prevOutput = getPrevSummaries( module );
			}

			final BufferedWriter writer = new BufferedWriter( new FileWriter( getOutputFile( module ) ) );
			writeRow( writer, getReportSet( module ) );

			final File[] outputs = module.getOutputDir().listFiles();
			Arrays.sort(outputs);
			Log.debug( ValidationUtil.class,
					"Found [" + outputs.length + "] files in output dir of module [" + module + "]." );
			int passingFiles = 0;
			for( final File f: outputs )
			{
				final FileSummary fs = new FileSummary( f );
				if( getReportSet( module ).contains( "md5" )
						|| hasExp( module ) && getCompareSet( module ).contains( "md5" ) )
				{
					fs.calcMd5();
				}
				if( hasExp( module ) )
				{
					final String ekey = fileNameToKey( fs.getAtt( NAME ) );
					final FileSummary expected = prevOutput.get( ekey );
					if( fs.compareToExpected( expected, getCompareSet( module ) ) == PASS )
					{
						passingFiles += 1;
					}
					prevOutput.remove( ekey );
				}
				final ArrayList<String> row = new ArrayList<>();
				for( final String col: getReportSet( module ) )
				{
					row.add( fs.getAtt( col ) );
				}
				writeRow( writer, row );
			}
			writer.close();
			if( prevOutput.size() > 0 )
			{
				Log.warn( ValidationUtil.class, "Expectation file includes " + prevOutput.size()
						+ " files that were not verified in output of " + module.getClass().getSimpleName() + "." );
				for( final String oldFileName: prevOutput.keySet() )
				{
					Log.warn( ValidationUtil.class, prevOutput.get( oldFileName ).toString() );
				}
				if( !alwaysPass( module ) )
				{
					throw new ValidationException( module );
				}
			}
			if( hasExp( module ) && !alwaysPass( module ) && passingFiles < outputs.length )
			{
				Log.warn( ValidationUtil.class, "passingFiles: " + passingFiles );
				Log.warn( ValidationUtil.class, "outputs to validate: " + outputs.length );
				if( !alwaysPass( module ) )
				{
					throw new ValidationException( module );
				}
			}
		}
		else
		{
			Log.debug( ValidationUtil.class, "Validation is turned off for module: " + ModuleUtil.displayID( module )
					+ "_" + module.getClass().getSimpleName() );
		}
	}

	/**
	 * If the file name contains a date, replace that with the word "DATE", otherwise return it unchanged.
	 * 
	 * @param fileName
	 * @return
	 */
	private static String fileNameToKey( final String fileName )
	{
		String key = fileName.replaceAll( "_[0-9]+_[0-9]{4}[A-Za-z]{3}[0-9]{2}", "_DATE" );
		if( key.equals( fileName ) )
		{
			key = fileName.replaceAll( "_[0-9]{4}[A-Za-z]{3}[0-9]{2}", "_DATE" );
		}
		return key;
	}

	private static ArrayList<String> getCompareSet( final BioModule module ) throws Exception
	{
		ArrayList<String> compareFeatures = new ArrayList<>();
		final ArrayList<String> configCompareOn = new ArrayList<>( Config.getSet( module, COMPARE_ON ) );
		final List<String> headers = getHeaders( module );
		if( configCompareOn == null || configCompareOn.isEmpty() )
		{
			compareFeatures = new ArrayList<>( headers );
		}
		else
		{
			compareFeatures = configCompareOn;
		}
		// result of prior run of the pipeline, always ignore this.
		compareFeatures.remove( MATCHED_EXPECTATION );
		// name is always compared on by being the key between files.
		compareFeatures.remove( NAME );
		if( !availableAttributes.containsAll( compareFeatures ) )
		{
			throw new ConfigViolationException(
					"Available file attributes for validation are: " + availableAttributes );
		}
		if( !headers.containsAll( compareFeatures ) )
		{
			throw new ConfigViolationException( COMPARE_ON,
					"Cannot compare on features that are not given in expectation file." );
		}
		Log.debug( ValidationUtil.class, "Comparing based on features: " + compareFeatures );
		return compareFeatures;
	}

	private static File getExpectationFile( final BioModule module ) throws Exception
	{
		File expectationFile = null;
		final String expectationFilePath = Config.getString( module, EXPECTATION_FILE );
		if( expectationFilePath != null && !expectationFilePath.isEmpty() )
		{
			expectationFile = new File( expectationFilePath );
			if( !expectationFile.exists() )
			{
				throw new ConfigPathException( expectationFile );
			}
			if( expectationFile.isDirectory() )
			{
				expectationFile = new File(
						Config.getString( module, EXPECTATION_FILE ) + File.separator + getOutputFileName( module ) );
				if( !expectationFile.exists() )
				{
					throw new ConfigPathException( expectationFile,
							"Could not find file: " + getOutputFileName( module ) + " in directory "
									+ Config.getString( module, EXPECTATION_FILE ) );
				}
			}
		}
		return expectationFile;
	}

	private static List<String> getHeaders( final BioModule module ) throws ExpectationFileFormatException, Exception
	{
		List<String> headers = null;
		final List<List<String>> table = parseTableFile( module );
		final Iterator<List<String>> rows = table.iterator();
		headers = rows.next();
		if( headers == null || headers.isEmpty() )
		{
			throw new ExpectationFileFormatException( "No expectations in expectation file",
					getExpectationFile( module ) );
		}
		final String id = headers.get( 0 );
		if( !id.equals( NAME ) )
		{
			throw new ExpectationFileFormatException(
					"First column in expectation file should be \"" + NAME + "\". Found first column: \"" + id + "\"].",
					getExpectationFile( module ) );
		}
		Log.debug( ValidationUtil.class, "Expectation file headers: " + headers );
		return headers;
	}

	private static File getOutputFile( final BioModule module ) throws IOException
	{
		final String outName = getValidationDir() + File.separator + getOutputFileName( module );
		final File outFile = new File( outName );
		if( !outFile.exists() )
		{
			outFile.createNewFile();
		}
		return outFile;
	}

	private static String getOutputFileName( final BioModule module )
	{
		return ModuleUtil.displayID( module ) + "_" + module.getClass().getSimpleName() + OUTPUT_FILE_SUFFIX;
	}

	private static HashMap<String, FileSummary> getPrevSummaries( final BioModule module ) throws Exception
	{
		final HashMap<String, FileSummary> prevOutput = new HashMap<>();
		final List<String> headers = getHeaders( module );
		final List<List<String>> table = parseTableFile( module );
		int rowNum = 1;
		final Iterator<List<String>> rows = table.iterator();
		rows.next();// drop the header
		while( rows.hasNext() )
		{
			final List<String> row = rows.next();
			final String id = row.get( 0 );
			if( id == null || id.isEmpty() && !row.toString().isEmpty() )
			{
				throw new ExpectationFileFormatException(
						"Row [" + rowNum + "] does not have a value in \"" + NAME + "\" column.",
						getExpectationFile( module ) );
			}
			else
			{
				final FileSummary fs = new FileSummary( id );
				prevOutput.put( fileNameToKey( id ), fs );
				for( final String cf: getCompareSet( module ) )
				{
					fs.setAtt( cf, row.get( headers.indexOf( cf ) ) );
				}
			}
			rowNum++;
		}
		if( prevOutput == null || prevOutput.isEmpty() )
		{
			Log.info( ValidationUtil.class, "Module " + ModuleUtil.displayID( module ) + "_"
					+ module.getClass().getSimpleName() + " is expected to have not output." );
		}
		return prevOutput;
	}

	private static ArrayList<String> getReportSet( final BioModule module ) throws Exception
	{
		ArrayList<String> reportFeatures = new ArrayList<>( Config.getSet( module, REPORT_ON ) );
		if( reportFeatures == null || reportFeatures.isEmpty() )
		{
			reportFeatures = availableAttributes;
		}
		else
		{
			if( !availableAttributes.containsAll( reportFeatures ) )
			{
				reportFeatures.removeAll( availableAttributes );
				throw new ConfigViolationException( REPORT_ON, "Cannot report on: " + reportFeatures + "."
						+ Constants.RETURN + "Options to report on include: " + availableAttributes.toString() + "." );
			}
		}
		// ensure that this column is present and at the beginning.
		reportFeatures.remove( NAME );
		reportFeatures.add( 0, NAME );
		// ensure that this column is present and at the end.
		reportFeatures.remove( MATCHED_EXPECTATION );
		reportFeatures.add( MATCHED_EXPECTATION );
		Log.debug( ValidationUtil.class, "Reporting on features: " + reportFeatures );
		return reportFeatures;
	}

	public static File getValidationDir()
	{
		final File dir = new File( Config.pipelinePath() + File.separator + VALIDATION_FOLDER );
		if( !dir.isDirectory() )
		{
			dir.mkdirs();
			Log.info( ValidationUtil.class, "Create directory: " + dir.getAbsolutePath() );
		}
		return dir;
	}

	private static boolean hasExp( final BioModule module ) throws Exception
	{
		return getExpectationFile( module ) != null;
	}

	private static List<List<String>> parseTableFile( final BioModule module ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		BufferedReader reader = null;
		try
		{
			reader = BioLockJUtil.getFileReader( getExpectationFile( module ) );
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final ArrayList<String> record = new ArrayList<>();
				final String[] cells = line.split( Constants.TAB_DELIM, -1 );
				for( final String cell: cells )
				{
					if( cell == null || cell.trim().isEmpty() )
					{
						record.add( null );
					}
					else
					{
						record.add( cell.trim() );
					}
				}
				data.add( record );
			}
		}
		catch( final Exception ex )
		{
			throw new Exception( "Error occurrred parsing expectation file!", ex );
		}
		finally
		{
			try
			{
				if( reader != null )
				{
					reader.close();
				}
			}
			catch( final IOException ex )
			{
				Log.error( ValidationUtil.class, "Failed to close file reader", ex );
			}
		}
		return data;
	}

	private static void writeRow( final BufferedWriter writer, final ArrayList<String> row ) throws Exception
	{
		try
		{
			writer.write( String.join( Constants.TAB_DELIM, row ) + Constants.RETURN );
		}
		catch( final Exception e )
		{
			writer.close();
			throw e;
		}
	}

}
