/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Aug 9, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.exception.ConfigPathException;
import biolockj.exception.ConfigViolationException;
import biolockj.module.BioModule;

/**
 * Simple utility containing String manipulation and formatting functions.
 */
public class BioLockJUtil
{

	/**
	 * Add leading spaces until the val is padded to given length
	 * 
	 * @param val Value to add spaces
	 * @param length Total length of val with spaces
	 * @return Padded value
	 * @throws Exception if errors occur.
	 */
	public static String addLeadingSpaces( String val, final int length ) throws Exception
	{
		while( val.length() < length )
		{
			val = " " + val;
		}
		return val;
	}

	/**
	 * Add trailing spaces until the val is padded to given length
	 * 
	 * @param val Value to add spaces
	 * @param length Total length of val with spaces
	 * @return Padded value
	 * @throws Exception if errors occur.
	 */
	public static String addTrailingSpaces( String val, final int length ) throws Exception
	{
		while( val.length() < length )
		{
			val += " ";
		}
		return val;
	}

	/**
	 * Delete file or directory with retry. Wait 3 seconds between each try - waiting for resource to release lock if .
	 * 
	 * @param file File or directory
	 * @param numTries Number of attempts
	 * @return boolean status
	 * @throws Exception if errors occur
	 */
	public static boolean deleteWithRetry( final File file, final int numTries ) throws Exception
	{
		int i = 0;
		while( i++ < numTries )
		{
			try
			{
				Thread.sleep( 3 * 1000 );
				if( i == numTries )
				{
					file.delete();
					Log.warn( BioLockJUtil.class, "FileUtils.forceDelete( file ) failed, but file.delete() worked" );
				}
				else
				{
					FileUtils.forceDelete( file );
				}

				return true;
			}
			catch( final IOException ex )
			{
				Log.info( BioLockJUtil.class,
						"Waiting for resource to become free [" + i + "]: " + file.getAbsolutePath() );
			}
		}

		Log.warn( BioLockJUtil.class, "Failed to delete file: " + file.getAbsolutePath() );

		return false;
	}

	/**
	 * This method formats the input number to have a length of at least numDigits.<br>
	 * Simply add leading zeros until numDigits is reached.
	 *
	 * @param input Integer value
	 * @param numDigits Number of digits return value should contain
	 * @return number as String with leading zeros.
	 */
	public static String formatDigits( final Integer input, final Integer numDigits )
	{
		String val = input.toString();
		while( val.length() < numDigits )
		{
			val = "0" + val;
		}

		return val;
	}

	/**
	 * This method formats the input number by adding commas.
	 *
	 * @param input Integer value
	 * @return number as String with commas
	 * 
	 */
	@SuppressWarnings("unused")
	public static String formatNumericOutput( final Integer input )
	{
		if( input == null )
		{
			return "0";
		}
		else if( true )
		{
			return input.toString();
		}

		String output = "";
		for( int i = input.toString().length(); i > 0; i-- )
		{
			if( output.length() % 4 == 0 )
			{
				output = "," + output;
			}
			output = input.toString().substring( i - 1, i ) + output;
		}

		return output.substring( 0, output.length() - 1 );
	}

	/**
	 * Build the percentage display string for the num/denom ratio as "##.##%"
	 * 
	 * @param num Numerator
	 * @param denom Denominator
	 * @return ratio
	 */
	public static String formatPercentage( final long num, final long denom )
	{
		final DecimalFormat df = new DecimalFormat( "##.##" );
		String percentage = Double.valueOf( df.format( 100 * ( (double) num / denom ) ) ).toString();
		if( percentage.indexOf( "." ) < 3 )
		{
			percentage += "0";
		}

		return percentage + "%";
	}

	/**
	 * Basic input files may be sequences, or any other file type acceptable in a pipeline module.
	 * 
	 * @return Collection( File ) input files
	 * @throws Exception if errors occur
	 */
	public static Collection<File> getBasicInputFiles() throws Exception
	{
		if( inputFiles.isEmpty() )
		{
			final Collection<File> files = new HashSet<>();
			for( final File dir: getInputDirs() )
			{
				Log.info( SeqUtil.class, "Found pipeline input dir " + dir.getAbsolutePath() );
				files.addAll( FileUtils.listFiles( dir, HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );
			}
			Log.info( SeqUtil.class, "# Initial input files found: " + files.size() );
			for( final File file: files )
			{
				if( !Config.getTreeSet( Config.INPUT_IGNORE_FILES ).contains( file.getName() ) )
				{
					inputFiles.add( file );
				}
				else
				{
					Log.warn( SeqUtil.class, "Ignoring file: " + file.getName() );
				}
			}
		}

		return inputFiles;
	}

	/**
	 * Return an ordered list of the class names from the input collection.
	 * 
	 * @param objs Objects
	 * @return List of class names
	 */
	public static List<String> getClassNames( final Collection<?> objs )
	{
		final List<String> names = new ArrayList<>();
		for( final Object obj: objs )
		{
			names.add( obj.getClass().getName() );
		}

		return names;
	}

	/**
	 * Concatenate data and return as a comma separated String.
	 * 
	 * @param data Collection of data
	 * @return Collection data as a String
	 */
	public static String getCollectionAsString( final Collection<?> data )
	{
		final StringBuffer sb = new StringBuffer();
		if( data != null && !data.isEmpty() )
		{
			for( final Object val: data )
			{
				sb.append( ( sb.length() > 0 ? ", ": "" ) + val.toString() );
			}
		}

		return sb.toString();
	}

	/**
	 * Direct module parameters contain 2 parts separated by a colon: (pipeline directory name):(module name)
	 * 
	 * @param module BioModule
	 * @return Direct parameter flag + value
	 * @throws Exception if errors occur
	 */
	public static String getDirectModuleParam( final BioModule module ) throws Exception
	{
		return RuntimeParamUtil.DIRECT_FLAG + " " + Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getName()
				+ ":" + module.getClass().getName();
	}

	/**
	 * Return an ordered list of absolute file paths from the input collection.
	 * 
	 * @param files Files
	 * @return List of file paths
	 */
	public static List<String> getFilePaths( final Collection<File> files )
	{
		final List<String> paths = new ArrayList<>();
		for( final File file: files )
		{
			paths.add( file.getAbsolutePath() );
		}

		return paths;
	}

	/**
	 * Get a {@link BufferedReader} for standard text file or {@link GZIPInputStream} for gzipped files ending in ".gz"
	 *
	 * @param file to be read
	 * @return {@link BufferedReader} or {@link GZIPInputStream} if file is gzipped
	 * @throws FileNotFoundException if file does not exist
	 * @throws IOException if unable to read or write the file
	 */
	public static BufferedReader getFileReader( final File file ) throws FileNotFoundException, IOException
	{
		return SeqUtil.isGzipped( file.getName() )
				? new BufferedReader( new InputStreamReader( new GZIPInputStream( new FileInputStream( file ) ) ) )
				: new BufferedReader( new FileReader( file ) );
	}

	/**
	 * Get the list of input directories for the pipeline.
	 * 
	 * @return List of system directory file paths
	 * @throws Exception if errors occur
	 */
	public static List<File> getInputDirs() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			final List<File> dirs = new ArrayList<>();
			final File dir = new File( DockerUtil.CONTAINER_INPUT_DIR );
			if( !dir.exists() )
			{
				throw new Exception( "Container missing mapped input volume system path: " + dir.getAbsolutePath() );
			}
			dirs.add( dir );
			return dirs;
		}
		return Config.requireExistingDirs( Config.INPUT_DIRS );
	}

	/**
	 * Return the MASTER config file.
	 * 
	 * @return MASTER config
	 * @throws Exception if errors occur building the path
	 */
	public static File getMasterConfig() throws Exception
	{
		String configName = Config.getConfigFileName();
		if( configName.startsWith( MASTER_PREFIX ) )
		{
			configName = configName.replaceAll( MASTER_PREFIX, "" );
		}
		return new File( Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath() + File.separator
				+ MASTER_PREFIX + configName );
	}

	/**
	 * Recursively get files located in the directories listed in
	 * {@link biolockj.Config}.{@value biolockj.Config#INPUT_DIRS} after removing
	 * {@link biolockj.Config}.{@value biolockj.Config#INPUT_IGNORE_FILES}
	 * 
	 * @return Pipeline input files
	 * @throws ConfigViolationException if sequence files without valid metadata are detected
	 * @throws Exception if no input files are found
	 */
	public static List<File> getPipelineInputFiles() throws Exception
	{
		if( filteredInputFiles.isEmpty() )
		{
			final List<File> seqsWithoutMetaId = new ArrayList<>();
			for( final File file: BioLockJUtil.getBasicInputFiles() )
			{
				if( !SeqUtil.requireSeqInput() || Config.getBoolean( Config.INTERNAL_MULTIPLEXED )
						|| MetaUtil.getMetadata() == null
						|| MetaUtil.getSampleIds().contains( SeqUtil.getSampleId( file.getName() ) ) )
				{
					filteredInputFiles.add( file );
				}
				else if( Config.getBoolean( MetaUtil.META_REQUIRED ) ) // metadata required
				{
					seqsWithoutMetaId.add( file );
					throw new ConfigViolationException( MetaUtil.META_REQUIRED,
							"Sample ID not found in metadata file: " + file.getAbsolutePath() );
				}
				else
				{
					Log.warn( SeqUtil.class, "Ignoring input file not found in metadata because Config property [ "
							+ MetaUtil.META_REQUIRED + "=" + Config.FALSE + " ]: " + file.getAbsolutePath() );
				}
			}

			if( !seqsWithoutMetaId.isEmpty() && Config.getBoolean( MetaUtil.META_REQUIRED ) )
			{
				throw new ConfigViolationException( MetaUtil.META_REQUIRED,
						"No metadata found for the following files: " + BioLockJ.RETURN
								+ BioLockJUtil.printLongFormList( seqsWithoutMetaId ) );
			}

			if( filteredInputFiles.isEmpty() )
			{
				throw new Exception( "No valid files found in: " + Config.INPUT_DIRS );
			}
		}

		return filteredInputFiles;
	}

	/**
	 * Get the program source (either the jar path or main class biolockj.BioLockJ);
	 * 
	 * @return java source parameter (either Jar or main class with classpath)
	 * @throws ConfigPathException if unable to determine $BLJ source
	 */
	public static File getSource() throws ConfigPathException
	{
		try
		{
			final File f = new File( BioLockJUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI() );
			// source will return JAR path or MAIN class file in bin dir
			if( f.isFile() ) // must be jar
			{
				return f.getParentFile().getParentFile();
			}
			else if( f.isDirectory() && f.getName().equals( "bin" ) )
			{
				return f.getParentFile();
			}
		}
		catch( final Exception ex )
		{
			throw new ConfigPathException( "Unable to decode $BLJ environment variable." );
		}
		return null;
	}

	/**
	 * Method returns the current version of BioLockJ.
	 * 
	 * @return BioLockJ version
	 * @throws Exception if errors occur
	 */
	public static String getVersion() throws Exception
	{
		final String missingMsg = "undetermined - mission $BLJ/.version file";
		final File file = new File( getSource().getAbsoluteFile() + File.separator + VERSION_FILE );
		if( file.exists() )
		{
			final BufferedReader reader = getFileReader( file );
			for( final String line = reader.readLine(); line != null; )
			{
				return line;
			}
			reader.close();
		}

		return missingMsg;
	}

	/**
	 * Print collection one item per line.
	 * 
	 * @param data Collection of data
	 * @return Collection data as a String
	 */
	public static String printLongFormList( final Collection<?> data )
	{
		final StringBuffer sb = new StringBuffer();
		if( data != null && !data.isEmpty() )
		{
			sb.append( BioLockJ.RETURN );
			for( final Object val: data )
			{
				sb.append( val ).append( BioLockJ.RETURN );
			}
		}

		return sb.toString();
	}

	/**
	 * Remove the outer single or double quotes of the given value.<br>
	 * Quotes are only removed if quotes are found as very 1st and last characters.
	 * 
	 * @param value Possibly quoted value
	 * @return value without outer quotes
	 * @throws Exception if errors occur
	 */
	public static String removeOuterQuotes( final String value ) throws Exception
	{
		if( value.startsWith( "\"" ) && value.endsWith( "\"" ) )
		{
			return value.substring( 1, value.length() - 1 );
		}
		if( value.startsWith( "'" ) && value.endsWith( "'" ) )
		{
			return value.substring( 1, value.length() - 1 );
		}

		return value;
	}

	/**
	 * Remove all single and double quotation marks found in value.
	 * 
	 * @param value Possibly quoted value
	 * @return value with no quotes
	 * @throws Exception if errors occur
	 */
	public static String removeQuotes( final String value ) throws Exception
	{
		if( value == null )
		{
			return null;
		}

		return value.replaceAll( "'", "" ).replaceAll( "\"", "" );
	}

	/**
	 * Save a single version of the config file by combing with default config files, if any exist.
	 * 
	 * @throws Exception if errors occur
	 */
	public static void saveNewMasterConfig() throws Exception
	{
		final File masterConfig = getMasterConfig();
		final boolean masterExists = masterConfig.exists();
		if( masterExists && !RuntimeParamUtil.doRestart() )
		{
			throw new Exception( "MASTER Config should not exist unless restarting a failed pipeline, but was found: "
					+ masterConfig.getAbsolutePath() );
		}

		final List<String> initConfig = getInitConfig();
		if( masterExists )
		{
			if( getTempConfig().exists() )
			{
				deleteTempConfigFile();
			}

			FileUtils.moveFile( getMasterConfig(), getTempConfig() );
			if( getMasterConfig().exists() )
			{
				throw new Exception(
						"Moved MASTER Config to " + getTempConfig().getAbsolutePath() + " but MASTER still exists!" );
			}
		}

		final BufferedWriter writer = new BufferedWriter( new FileWriter( masterConfig ) );
		try
		{
			writer.write( "# The MASTER Config file can be used to fully reproduce all pipeline analysis." + RETURN );
			writer.write( "# The MASTER Config file was generated from the following Config files: " + RETURN );

			if( initConfig == null )
			{
				writer.write( ORIG_CONFIG_FLAG + Config.getConfigFilePath() + RETURN );

				final List<String> defaults = Config.getList( Config.INTERNAL_DEFAULT_CONFIG );
				if( defaults != null && !defaults.isEmpty() )
				{
					for( final String defConfig: Config.getList( Config.INTERNAL_DEFAULT_CONFIG ) )
					{
						writer.write( DEFAULT_CONFIG_FLAG + defConfig + RETURN );
					}
				}
			}
			else
			{
				for( final String line: initConfig )
				{
					writer.write( line + RETURN );
				}
			}

			writer.write( RETURN );
			for( final String module: Config.getList( Config.INTERNAL_BLJ_MODULE ) )
			{
				writer.write( Config.INTERNAL_BLJ_MODULE + " " + module + RETURN );
			}
			writer.write( RETURN );

			final Map<String, String> map = new HashMap<>( Config.getProperties() );
			map.remove( Config.INTERNAL_BLJ_MODULE );
			map.remove( Config.INTERNAL_PIPELINE_DIR );
			map.remove( Config.INTERNAL_DEFAULT_CONFIG );
			map.remove( Config.INTERNAL_PAIRED_READS );
			map.remove( Config.INTERNAL_ALL_MODULES );
			map.remove( Config.INTERNAL_MULTIPLEXED );
			map.remove( SeqUtil.INTERNAL_SEQ_HEADER_CHAR );
			map.remove( SeqUtil.INTERNAL_SEQ_TYPE );
			map.remove( Config.PROJECT_DEFAULT_PROPS );
			map.remove( Config.INTERNAL_IS_MULTI_LINE_SEQ );

			final Set<String> keys = new TreeSet<>( map.keySet() );
			for( final String key: keys )
			{
				writer.write( key + "=" + map.get( key ) + RETURN );
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}

			deleteTempConfigFile();
		}

		if( !masterConfig.exists() )
		{
			throw new Exception( "Unable to build MASTER CONFIG: " + masterConfig.getAbsolutePath() );
		}
	}

	/**
	 * This method removes any given props and then adds the new values.
	 * 
	 * @param props List of config props
	 * @throws Exception if errors occur
	 */
	public static void updateMasterConfig( final Map<String, String> props ) throws Exception
	{
		final List<String> configLines = readMasterConfig( props );
		if( configLines == null || props == null || props.isEmpty() )
		{
			return;
		}

		BufferedWriter writer = new BufferedWriter( new FileWriter( getTempConfig() ) );
		try
		{
			for( final String line: configLines )
			{
				writer.write( line + RETURN );
			}

			for( final String name: props.keySet() )
			{
				Log.info( BioLockJUtil.class, "Update MASTER config property: " + name + "=" + props.get( name ) );
				writer.write( name + "=" + props.get( name ) + RETURN );
			}

			writer.close();
			writer = null;

			BioLockJUtil.deleteWithRetry( getMasterConfig(), 10 );
			FileUtils.moveFile( getTempConfig(), getMasterConfig() );
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
			if( getTempConfig().exists() && getMasterConfig().exists() )
			{
				BioLockJUtil.deleteWithRetry( getTempConfig(), 10 );
			}
			else if( getTempConfig().exists() )
			{
				throw new Exception(
						"Error occurred updating MASTER config.  File has been deleted, please recover from: "
								+ getTempConfig().getAbsolutePath() );
			}

		}
	}

	private static void deleteTempConfigFile() throws Exception
	{
		if( getTempConfig().exists() && getMasterConfig().exists() )
		{
			BioLockJUtil.deleteWithRetry( getTempConfig(), 10 );
		}
		else if( getTempConfig().exists() )
		{
			throw new Exception( "Error occurred updating MASTER config.  File has been deleted, please recover using: "
					+ getTempConfig().getAbsolutePath() );
		}
	}

	private static List<String> getInitConfig() throws Exception
	{
		final File masterConfig = getMasterConfig();
		if( !masterConfig.exists() || !Config.getConfigFilePath().equals( masterConfig.getAbsolutePath() ) )
		{
			return null;
		}
		final List<String> initConfig = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( masterConfig );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( line.startsWith( ORIG_CONFIG_FLAG ) || line.startsWith( DEFAULT_CONFIG_FLAG ) )
				{
					initConfig.add( line );
				}
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}
		return initConfig;
	}

	private static File getTempConfig() throws Exception
	{
		return new File( Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath() + File.separator
				+ TEMP_PREFIX + Config.getConfigFileName() );
	}

	private static List<String> readMasterConfig( final Map<String, String> props ) throws Exception
	{
		final List<String> newLines = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( getMasterConfig() );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, "=" );
				if( st.countTokens() < 2 || !props.keySet().contains( st.nextToken() ) )
				{
					newLines.add( line );
				}
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		return newLines;
	}

	/**
	 * Prefix added to the master Config file: {@value #MASTER_PREFIX}
	 */
	public static final String MASTER_PREFIX = "MASTER_";
	private static final String DEFAULT_CONFIG_FLAG = "# ----> Default Config: ";
	private static final List<File> filteredInputFiles = new ArrayList<>();

	private static final List<File> inputFiles = new ArrayList<>();
	private static final String ORIG_CONFIG_FLAG = "# ----> Project Config: ";
	private static final String RETURN = BioLockJ.RETURN;
	private static final String TEMP_PREFIX = ".TEMP_";
	private static final String VERSION_FILE = ".version";
}
