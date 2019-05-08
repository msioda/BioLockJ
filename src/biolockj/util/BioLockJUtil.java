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
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.*;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;
import biolockj.exception.ConfigViolationException;
import biolockj.module.report.r.R_CalculateStats;

/**
 * Simple utility containing String manipulation and formatting functions.
 */
public class BioLockJUtil {

	/**
	 * Add leading spaces until the val is padded to given length
	 * 
	 * @param val Value to add spaces
	 * @param length Total length of val with spaces
	 * @return Padded value
	 */
	public static String addLeadingSpaces( final String val, final int length ) {
		String val2 = val;
		while( val2.length() < length ) {
			val2 = " " + val2;
		}
		return val2;
	}

	/**
	 * Add trailing spaces until the val is padded to given length
	 * 
	 * @param val Value to add spaces
	 * @param length Total length of val with spaces
	 * @return Padded value
	 */
	public static String addTrailingSpaces( final String val, final int length ) {
		String val2 = val;
		while( val2.length() < length ) {
			val2 += " ";
		}
		return val2;
	}

	/**
	 * Used to save status files for modules and the pipeline.
	 * 
	 * @param path Target dir path
	 * @return File Created file
	 * @throws Exception if errors occur attempting to save the file
	 */
	public static File createFile( final String path ) throws Exception {
		final File f = new File( path );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.isFile() ) throw new Exception( "Unable to create status file: " + f.getAbsolutePath() );
		return f;
	}

	/**
	 * Delete file or directory with retry. Wait 3 seconds between each try - waiting for resource to release lock if .
	 * 
	 * @param file File or directory
	 * @param numTries Number of attempts
	 * @return boolean status
	 * @throws Exception if errors occur
	 */
	public static boolean deleteWithRetry( final File file, final int numTries ) throws Exception {
		int i = 0;
		while( i++ < numTries ) {
			try {
				Thread.sleep( 3 * 1000 );
				if( i == 0 || i == numTries ) {
					file.delete();
					Log.warn( BioLockJUtil.class, "file.delete() removed: " + file.getAbsolutePath() );
				} else {
					FileUtils.forceDelete( file );
					Log.warn( BioLockJUtil.class, "FileUtils.forceDelete( file ) removed: " + file.getAbsolutePath() );
				}

				return true;
			} catch( final IOException ex ) {
				Log.info( BioLockJUtil.class,
					"Failed while still waiting for resource to become free [" + i + "]: " + file.getAbsolutePath() );
			}
		}

		Log.warn( BioLockJUtil.class, "Failed to delete file: " + file.getAbsolutePath() );

		return false;
	}

	/**
	 * Return the file extension - but ignore {@value biolockj.Constants#GZIP_EXT}.
	 * 
	 * @param file File
	 * @return File extension
	 */
	public static String fileExt( final File file ) {
		String ext = file.getName();
		int index = ext.lastIndexOf( Constants.GZIP_EXT );
		if( SeqUtil.isGzipped( ext ) && index > 0 ) {
			ext = ext.substring( 0, index );
		}
		index = ext.lastIndexOf( "." );
		if( index > 0 ) {
			ext = ext.substring( index );
		}

		return ext;
	}

	/**
	 * This method formats the input number to have a length of at least numDigits.<br>
	 * Simply add leading zeros until numDigits is reached.
	 *
	 * @param input Integer value
	 * @param numDigits Number of digits return value should contain
	 * @return number as String with leading zeros.
	 */
	public static String formatDigits( final Integer input, final Integer numDigits ) {
		String val = input.toString();
		while( val.length() < numDigits ) {
			val = "0" + val;
		}

		return val;
	}

	/**
	 * This method formats the input number by adding commas.
	 *
	 * @param input Long value
	 * @param addCommas boolean if numeric output should be displayed using commas
	 * @return number as String with commas
	 */
	public static String formatNumericOutput( final Long input, final boolean addCommas ) {
		if( input == null ) return "0";
		else if( !addCommas ) return input.toString();

		String output = "";
		for( int i = input.toString().length(); i > 0; i-- ) {
			if( output.length() % 4 == 0 ) {
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
	public static String formatPercentage( final long num, final long denom ) {
		final DecimalFormat df = new DecimalFormat( "##.##" );
		String percentage = Double.valueOf( df.format( 100 * ( (double) num / denom ) ) ).toString();
		if( percentage.indexOf( "." ) < 3 ) {
			percentage += "0";
		}

		return percentage + "%";
	}

	/**
	 * Get the program source (either the jar path or main class biolockj.BioLockJ);
	 * 
	 * @return java source parameter (either Jar or main class with classpath)
	 * @throws ConfigPathException if unable to determine $BLJ source
	 */
	public static File getBljDir() throws ConfigPathException {
		if( DockerUtil.inDockerEnv() ) return new File( DockerUtil.CONTAINER_BLJ_DIR );
		File f = null;
		try {
			f = getSource();
			// source will return JAR path or MAIN class file in bin dir
			if( f.isFile() ) return f.getParentFile().getParentFile();
			else if( f.isDirectory() && f.getName().equals( "bin" ) ) return f.getParentFile();
		} catch( final Exception ex ) {
			throw new ConfigPathException( f, "Unable to decode ${BLJ} environment variable: " + ex.getMessage() );
		}
		return null;
	}

	/**
	 * Return default ${BLJ_SUP} dir
	 * 
	 * @return blj_support dir
	 * @throws ConfigPathException if $BLJ_SUP directory path is configured, but invalid
	 */
	public static File getBljSupDir() throws ConfigPathException {
		if( DockerUtil.inDockerEnv() ) return new File( DockerUtil.CONTAINER_BLJ_SUP_DIR );
		File f = null;
		try {
			f = new File( getBljDir().getParentFile().getAbsolutePath() + File.separator + BLJ_SUPPORT );
			if( f.isDirectory() ) return f;
		} catch( final Exception ex ) {
			throw new ConfigPathException( f, "Unable to decode ${BLJ_SUP} environment variable: " + ex.getMessage() );
		}

		return null;

	}

	/**
	 * Return an ordered list of the class names from the input collection.
	 * 
	 * @param objs Objects
	 * @return List of class names
	 */
	public static List<String> getClassNames( final Collection<?> objs ) {
		final List<String> names = new ArrayList<>();
		for( final Object obj: objs ) {
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
	public static String getCollectionAsString( final Collection<?> data ) {
		final StringBuffer sb = new StringBuffer();
		if( data != null && !data.isEmpty() ) {
			for( final Object val: data ) {
				sb.append( ( sb.length() > 0 ? ", ": "" ) + val.toString() );
			}
		}

		return sb.toString();
	}

	/**
	 * Compare files to return a common parent directory (if any)
	 * 
	 * @param f1 File 1
	 * @param f2 File 2
	 * @return Directory path share by f1 and f2
	 */
	public static File getCommonParent( final File f1, final File f2 ) {
		File f22 = f2;
		if( f1 == null || f22 == null ) return null;
		final String f1Path = f1.getAbsolutePath();
		while( true ) {
			if( f1Path.contains( f22.getAbsolutePath() ) ) return new File( f22.getAbsolutePath() );
			final File parent = f22.getParentFile();
			if( parent == null || f22 == parent ) return null;
			f22 = parent;
		}
	}

	/**
	 * Return an ordered list of absolute file paths from the input collection.
	 * 
	 * @param files Files
	 * @return List of file paths
	 */
	public static List<String> getFilePaths( final Collection<File> files ) {
		final List<String> paths = new ArrayList<>();
		for( final File file: files ) {
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
	public static BufferedReader getFileReader( final File file ) throws FileNotFoundException, IOException {
		return SeqUtil.isGzipped( file.getName() )
			? new BufferedReader( new InputStreamReader( new GZIPInputStream( new FileInputStream( file ) ) ) )
			: new BufferedReader( new FileReader( file ) );
	}

	/**
	 * Get the list of input directories for the pipeline.
	 * 
	 * @return List of system directory file paths
	 * @throws ConfigNotFoundException if a required property is undefined
	 * @throws ConfigPathException if configured directory does not exist on the file-system
	 */
	public static List<File> getInputDirs() throws ConfigNotFoundException, ConfigPathException {
		if( DockerUtil.inDockerEnv() ) {
			final List<File> dirs = new ArrayList<>();
			final String path = Config.requireString( null, Constants.INPUT_DIRS );
			File dir = null;
			if( path.equals( File.separator ) ) {
				dir = new File( DockerUtil.DOCKER_INPUT_DIR );
			} else {
				dir = new File(
					path.replace( new File( path ).getParentFile().getAbsolutePath(), DockerUtil.DOCKER_INPUT_DIR ) );
			}
			
			dirs.add( dir );
			return dirs;
		}
		return Config.requireExistingDirs( null, Constants.INPUT_DIRS );
	}

	/**
	 * Basic input files may be sequences, or any other file type acceptable in a pipeline module.
	 * 
	 * @return Collection of pipeline input files
	 * @throws ConfigNotFoundException if a required property is undefined
	 * @throws ConfigPathException if configured directory does not exist on the file-system
	 * @throws ConfigViolationException if input directories contain duplicate file names
	 */
	public static Collection<File> getPipelineInputFiles()
		throws ConfigNotFoundException, ConfigPathException, ConfigViolationException {
		if( inputFiles.isEmpty() ) {
			Collection<File> files = new HashSet<>();
			for( final File dir: getInputDirs() ) {
				Log.info( BioLockJUtil.class, "Found pipeline input dir " + dir.getAbsolutePath() );
				files.addAll( findDups( files, removeIgnoredAndEmptyFiles(
					FileUtils.listFiles( dir, HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) ) ) );
			}
			Log.info( BioLockJUtil.class, "# Initial input files found: " + files.size() );
			files = removeIgnoredAndEmptyFiles( files );
			inputFiles.addAll( files );
			Log.info( BioLockJUtil.class, "# Initial input files after removing empty/ignored files: " + files.size() );
			setPipelineInputFileTypes();
		}
		return inputFiles;
	}

	/**
	 * Get the source of the java runtime classes ( /bin directory or JAR file ).
	 * 
	 * @return File object
	 * @throws URISyntaxException if unable to locate the Java source
	 */
	public static File getSource() throws URISyntaxException {
		return new File( BioLockJUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI() );
	}

	/**
	 * Return the user shell profile
	 * 
	 * @return Shell profile
	 */
	public static File getUserProfile() {
		if( userProfile != null ) return userProfile;
		File prof = null;
		try {
			if( DockerUtil.inAwsEnv() ) {
				prof = getProfile( DockerUtil.ROOT_HOME + File.separator + Constants.BASH_PROFILE );
			}
			if( prof == null && DockerUtil.inDockerEnv() ) {
				prof = getProfile( DockerUtil.ROOT_HOME + File.separator + Constants.BASH_RC );
			}
			final String path = Config.getString( null, Constants.USER_PROFILE );
			if( prof == null && path != null ) {
				prof = getProfile( path );
			}
			if( prof == null ) {
				prof = getProfile( Processor.submit( DEFAULT_PROFILE_CMD, "Detect-profile" ) );
			}
		} catch( final Exception ex ) {
			Log.error( LogUtil.class, "Failed to find user shell profile ", ex );
		}

		if( prof != null ) {
			userProfile = prof;
		}
		return prof;
	}

	/**
	 * Method returns the current version of BioLockJ.
	 * 
	 * @return BioLockJ version
	 * @throws Exception if errors occur
	 */
	public static String getVersion() throws Exception {
		final String missingMsg = "undetermined - missing $BLJ/.version file";
		final File file = new File( getBljDir().getAbsoluteFile() + File.separator + VERSION_FILE );
		if( file.exists() ) {
			final BufferedReader reader = getFileReader( file );
			for( final String line = reader.readLine(); line != null; )
				return line;
			reader.close();
		}

		return missingMsg;
	}

	/**
	 * Check collection vals for null or empty toString() values
	 * 
	 * @param vals Collection of objects
	 * @return Boolean TRUE if all at least 1 value is null or empty
	 */
	public static boolean hasNullOrEmptyVal( final Collection<Object> vals ) {
		for( final Object val: vals ) {
			if( val == null || val.toString().isEmpty() ) return true;
		}
		return false;
	}

	/**
	 * Method used to add a file to the ignore file list property.
	 * 
	 * @param file File to ignore
	 */
	public static void ignoreFile( final File file ) {
		final Set<String> ignore = new HashSet<>();
		if( Config.getSet( null, Constants.INPUT_IGNORE_FILES ) != null ) {
			ignore.addAll( Config.getSet( null, Constants.INPUT_IGNORE_FILES ) );
		}

		ignore.add( file.getName() );
		Config.setConfigProperty( Constants.INPUT_IGNORE_FILES, ignore );
	}

	/**
	 * Merge the collection into a String with 1 space between each element.toString() value.
	 * 
	 * @param collection Collection of objects
	 * @return Joined values
	 */
	public static String join( final Collection<?> collection ) {
		if( collection == null || collection.isEmpty() ) return "";

		final StringBuilder sb = new StringBuilder();
		for( final Object item: collection ) {
			sb.append( item.toString().trim() ).append( " " );
		}

		return sb.toString();
	}

	/**
	 * Convert milliseconds to minutes - useful to convert Java millisecond output rounded to the nearest minute.
	 * 
	 * @param milliseconds number of milliseconds
	 * @return same amount of time as minutes
	 */
	public static int millisToMinutes( final long milliseconds ) {
		return new Long( Math.round( milliseconds / 1000 / 60 ) ).intValue();
	}

	/**
	 * Convert minutes to milliseconds - useful to convert Config props measure in minutes to milliseconds since many
	 * Java functions have milliseconds args.
	 * 
	 * @param minutes number of minutes
	 * @return same amount of time as milliseconds
	 */
	public static long minutesToMillis( final int minutes ) {
		return 1000 * 60 * minutes;
	}

	/**
	 * Read in BioLockJ count table, each inner lists represents 1 line from the file.<br>
	 * Each cell in the tab delimited file is stored as 1 element in the inner lists.
	 * 
	 * @param file Path abundance file
	 * @return List of Lists - each inner list 1 line
	 * @throws Exception if errors occur
	 */
	public static List<List<String>> parseCountTable( final File file ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try {
			boolean firstRecord = true;
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				final ArrayList<String> record = new ArrayList<>();
				final String[] cells = line.split( Constants.TAB_DELIM, -1 );
				for( final String cell: cells ) {
					record.add( firstRecord && record.isEmpty() ? MetaUtil.getID(): cell );
				}
				data.add( record );
				firstRecord = false;
			}
		} finally {
			if( reader != null ) {
				reader.close();
			}
		}

		return data;
	}

	/**
	 * Convenience method to check pipeline input file type.
	 * 
	 * @param type Pipeline input file type
	 * @return TRUE if type = {@link biolockj.Config}.{@value #INTERNAL_PIPELINE_INPUT_TYPES}
	 * @throws ConfigNotFoundException if {@value #INTERNAL_PIPELINE_INPUT_TYPES} is undefined
	 */
	public static boolean pipelineInputType( final String type ) throws ConfigNotFoundException {
		return Config.requireSet( null, INTERNAL_PIPELINE_INPUT_TYPES ).contains( type );
	}

	/**
	 * Print collection one item per line.
	 * 
	 * @param data Collection of data
	 * @return Collection data as a String
	 */
	public static String printLongFormList( final Collection<?> data ) {
		final StringBuffer sb = new StringBuffer();
		if( data != null && !data.isEmpty() ) {
			sb.append( Constants.RETURN );
			for( final Object val: data ) {
				sb.append( val ).append( Constants.RETURN );
			}
		}

		return sb.toString();
	}

	/**
	 * Remove ignored and empty files from the input files.
	 * 
	 * @param files Collection of files
	 * @return valid files
	 */
	public static List<File> removeIgnoredAndEmptyFiles( final Collection<File> files ) {
		final List<File> validInputFiles = new ArrayList<>();
		for( final File file: files ) {
			final boolean isEmpty = FileUtils.sizeOf( file ) < 1L;
			if( isEmpty ) {
				Log.warn( SeqUtil.class, "Skip empty file: " + file.getAbsolutePath() );
			} else if( Config.getSet( null, Constants.INPUT_IGNORE_FILES ).contains( file.getName() ) ) {
				Log.debug( SeqUtil.class, "Ignore file " + file.getAbsolutePath() );
			} else {
				validInputFiles.add( file );
			}
		}
		return validInputFiles;
	}

	/**
	 * Remove the outer single or double quotes of the given value.<br>
	 * Quotes are only removed if quotes are found as very 1st and last characters.
	 * 
	 * @param value Possibly quoted value
	 * @return value without outer quotes
	 */
	public static String removeOuterQuotes( final String value ) {
		if( value.startsWith( "\"" ) && value.endsWith( "\"" ) ) return value.substring( 1, value.length() - 1 );
		if( value.startsWith( "'" ) && value.endsWith( "'" ) ) return value.substring( 1, value.length() - 1 );

		return value;
	}

	/**
	 * Remove all single and double quotation marks found in value.
	 * 
	 * @param value Possibly quoted value
	 * @return value with no quotes
	 */
	public static String removeQuotes( final String value ) {
		if( value == null ) return null;

		return value.replaceAll( "'", "" ).replaceAll( "\"", "" );
	}

	/**
	 * Setter for pipeline input files.
	 * 
	 * @param files List of input files
	 */
	public static void setPipelineInputFiles( final List<File> files ) {
		inputFiles = files;
	}

	private static Collection<File> findDups( final Collection<File> files, final Collection<File> newFiles )
		throws ConfigViolationException {
		final Map<String, String> names = new HashMap<>();
		for( final File f: files ) {
			names.put( f.getName(), f.getAbsolutePath() );
		}
		for( final File f: newFiles ) {
			if( names.keySet().contains( f.getName() ) )
				throw new ConfigViolationException( "Pipeline input file names must be unique [ " + f.getAbsolutePath()
					+ " ] has the same file name as [ " + names.get( f.getName() ) + " ]" );
			names.put( f.getName(), f.getAbsolutePath() );
		}
		return newFiles;
	}

	private static File getProfile( final String path ) {
		if( path != null ) {
			final File prof = new File( path );
			if( prof.isFile() ) return prof;
		}
		return null;
	}

	private static void setPipelineInputFileTypes() {
		final Set<String> fileTypes = new HashSet<>();

		for( final File file: inputFiles ) {
			if( SeqUtil.isSeqFile( file ) ) {
				fileTypes.add( PIPELINE_SEQ_INPUT_TYPE );
			} else if( file.getName().endsWith( Constants.PROCESSED ) ) {
				fileTypes.add( PIPELINE_PARSER_INPUT_TYPE );
			} else if( R_CalculateStats.isStatsFile( file ) ) {
				fileTypes.add( PIPELINE_STATS_TABLE_INPUT_TYPE );
			} else if( RMetaUtil.isMetaMergeTable( file ) ) {
				fileTypes.add( PIPELINE_R_INPUT_TYPE );
			} else if( OtuUtil.isOtuFile( file ) ) {
				fileTypes.add( PIPELINE_OTU_COUNT_TABLE_INPUT_TYPE );
			} else if( TaxaUtil.isLogNormalizedTaxaFile( file ) ) {
				fileTypes.add( PIPELINE_LOG_NORMAL_TAXA_COUNT_TABLE_INPUT_TYPE );
				fileTypes.add( PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE );
			} else if( TaxaUtil.isNormalizedTaxaFile( file ) ) {
				fileTypes.add( PIPELINE_NORMAL_TAXA_COUNT_TABLE_INPUT_TYPE );
				fileTypes.add( PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE );
			} else if( TaxaUtil.isTaxaFile( file ) ) {
				fileTypes.add( PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE );
			} else if( PathwayUtil.isPathwayFile( file ) ) {
				fileTypes.add( PIPELINE_HUMANN2_COUNT_TABLE_INPUT_TYPE );
			}
		}

		Config.setConfigProperty( INTERNAL_PIPELINE_INPUT_TYPES, fileTypes );
	}

	/**
	 * Internal {@link biolockj.Config} String property: {@value #INTERNAL_PIPELINE_INPUT_TYPES}<br>
	 *
	 * This value is set after parsing the input files from {@link biolockj.Config} property:
	 * {@value biolockj.Constants#INPUT_DIRS} in the method: {@link #getPipelineInputFiles()}. The primary purpose of
	 * storing this value is to determine if {@link biolockj.module.BioModule#getPreRequisiteModules()} are appropriate
	 * to add during pipeline initialization.<br>
	 * <br>
	 * 
	 * {@link biolockj.module.BioModule#getPreRequisiteModules()} are add dependent modules if missing from the
	 * {@link biolockj.Config}. This ensures the current module will have the correct input files and is a convenient
	 * way to manage the size and readability of {@link biolockj.Config} files. Prerequisite modules are always
	 * appropriate for full pipelines with sequence input file, however if the output from a prerequisite module is used
	 * as the input for a new pipeline via {@value biolockj.Constants#INPUT_DIRS}, adding the prerequisite module will
	 * cause FATAL pipeline errors.<br>
	 * <br>
	 * 
	 * New pipelines can be run starting with any module, so BioLockJ must be prepared to accept the input files
	 * required for any module. All BioModules require input files from one of the following 6 categories:<br>
	 * <ul>
	 * <li>{@value #PIPELINE_OTU_COUNT_TABLE_INPUT_TYPE}
	 * <li>{@value #PIPELINE_PARSER_INPUT_TYPE}
	 * <li>{@value #PIPELINE_R_INPUT_TYPE}
	 * <li>{@value #PIPELINE_SEQ_INPUT_TYPE}
	 * <li>{@value #PIPELINE_STATS_TABLE_INPUT_TYPE}
	 * <li>{@value #PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE}
	 * </ul>
	 * 
	 * In rare cases, such as running R plot modules, the input directories must contain multiple data types.<br>
	 * For example:<br>
	 * <ul>
	 * <li>{@value #PIPELINE_OTU_COUNT_TABLE_INPUT_TYPE}
	 * <li>{@value #PIPELINE_R_INPUT_TYPE}
	 * <li>{@value #PIPELINE_STATS_TABLE_INPUT_TYPE}
	 * </ul>
	 * With this input a user can run a pipeline with only 2 modules:<br>
	 * <ol>
	 * <li>{@link biolockj.module.report.JsonReport}
	 * <li>{@link biolockj.module.report.r.R_PlotOtus}
	 * </ol>
	 * 
	 */
	public static final String INTERNAL_PIPELINE_INPUT_TYPES = "internal.pipelineInputTypes";

	/**
	 * Pipeline input file type indicating the file is Humann2 generated
	 */
	public static final String PIPELINE_HUMANN2_COUNT_TABLE_INPUT_TYPE = "hn2";

	/**
	 * Internal {@link biolockj.Config} String property: {@value #PIPELINE_LOG_NORMAL_TAXA_COUNT_TABLE_INPUT_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} for log-normalized taxa count files that meet the
	 * file requirements to pass {@link biolockj.util.TaxaUtil#isLogNormalizedTaxaFile(File)}.
	 */
	public static final String PIPELINE_LOG_NORMAL_TAXA_COUNT_TABLE_INPUT_TYPE = "taxa_count_log_norm";

	/**
	 * Internal {@link biolockj.Config} String property: {@value #PIPELINE_NORMAL_TAXA_COUNT_TABLE_INPUT_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} for normalized taxa count files that meet the file
	 * requirements to pass {@link biolockj.util.TaxaUtil#isNormalizedTaxaFile(File)}.
	 */
	public static final String PIPELINE_NORMAL_TAXA_COUNT_TABLE_INPUT_TYPE = "taxa_count_norm";

	/**
	 * Internal {@link biolockj.Config} String property: {@value #PIPELINE_OTU_COUNT_TABLE_INPUT_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} for OTU count files that meet the file requirements
	 * to pass {@link biolockj.util.OtuUtil#isOtuFile(File)}.
	 */
	public static final String PIPELINE_OTU_COUNT_TABLE_INPUT_TYPE = "otu_count";

	/**
	 * Internal {@link biolockj.Config} String property: {@value #PIPELINE_PARSER_INPUT_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} for classifier output files.
	 */
	public static final String PIPELINE_PARSER_INPUT_TYPE = "classifier_output";

	/**
	 * Internal {@link biolockj.Config} String property: {@value #PIPELINE_R_INPUT_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} if input files are some type of count table merged
	 * with the metadata such as those output by {@link biolockj.module.report.taxa.AddMetadataToTaxaTables}. These
	 * files can be input into any {@link biolockj.module.report.r.R_Module}.
	 */
	public static final String PIPELINE_R_INPUT_TYPE = "R";

	/**
	 * Internal {@link biolockj.Config} String property: {@value #PIPELINE_SEQ_INPUT_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} for sequence input files.
	 */
	public static final String PIPELINE_SEQ_INPUT_TYPE = "seq";

	/**
	 * Internal {@link biolockj.Config} String property: {@value #PIPELINE_STATS_TABLE_INPUT_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} if input files are tables of statistics such as those
	 * output by {@link biolockj.module.report.r.R_CalculateStats}.
	 */
	public static final String PIPELINE_STATS_TABLE_INPUT_TYPE = "stats";

	/**
	 * Internal {@link biolockj.Config} String property: {@value #PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} for taxa count files that meet the file requirements
	 * to pass {@link biolockj.util.TaxaUtil#isTaxaFile(File)}.
	 */
	public static final String PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE = "taxa_count";

	private static final String BLJ_SUPPORT = "blj_support";
	private static final String DEFAULT_PROFILE_CMD = "get_default_profile";
	private static List<File> inputFiles = new ArrayList<>();
	private static File userProfile = null;
	private static final String VERSION_FILE = ".version";
}
