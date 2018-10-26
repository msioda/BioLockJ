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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.exception.ConfigPathException;

/**
 * Simple utility containing String manipulation and formatting functions.
 */
public class BioLockJUtil
{

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
		return file.getName().toLowerCase().endsWith( ".gz" )
				? new BufferedReader( new InputStreamReader( new GZIPInputStream( new FileInputStream( file ) ) ) )
				: new BufferedReader( new FileReader( file ) );
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
		if( data != null )
		{
			for( final Object val: data )
			{
				sb.append( ( sb.length() > 0 ? ", ": "" ) + val.toString() );
			}
		}

		return sb.toString();
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
	 * Return the MASTER config file.
	 * 
	 * @return MASTER config
	 * @throws Exception if errors occur building the path
	 */
	public static File getMasterConfig() throws Exception
	{
		String configName = Config.getConfigFileName();
		if( configName.startsWith( MASTER_PREFIX ) ) configName = configName.replaceAll( MASTER_PREFIX, "" );
		return new File( Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath() + File.separator
				+ MASTER_PREFIX + configName );
	}
	
	/**
	 * Method returns the current version of BioLockJ.
	 * 
	 * @return BioLockJ version
	 * @throws Exception if errors occur
	 */
	public static String getVersion() throws Exception
	{
		String missingMsg = "undetermined - mission $BLJ/.version file";
		File file = new File( getSource().getAbsoluteFile() + File.separator + VERSION_FILE );
		if( file.exists() )
		{
			BufferedReader reader = getFileReader( file );
			for( String line = reader.readLine(); line != null; )
			{
				return line;
			}
			reader.close();
		}
		
		return missingMsg;
	}
	
	private static final String VERSION_FILE = ".version";

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
	 * Save a single version of the config file by combing with default config files, if any exist.
	 * 
	 * @throws Exception if errors occur
	 */
	public static void saveNewMasterConfig() throws Exception
	{

		final File masterConfig = getMasterConfig();

		if( masterConfig.exists() && !RuntimeParamUtil.doRestart() )
		{
			throw new Exception( "MASTER Config should not exist unless restarting a failed pipeline, but was found: "
					+ masterConfig.getAbsolutePath() );
		}

		final BufferedWriter writer = new BufferedWriter( new FileWriter( masterConfig ) );
		try
		{
			writer.write( "# This MASTER Config file can be used to fully reproduce all pipeline analysis." + RETURN );
			writer.write( "# This MASTER Config file was generated from the following Config files: " + RETURN );
			writer.write( "# ----> Project Config: " + Config.getConfigFilePath() + RETURN );

			final List<String> defaults = Config.getList( Config.INTERNAL_DEFAULT_CONFIG );
			if( defaults != null && !defaults.isEmpty() )
			{
				for( final String defConfig: Config.getList( Config.INTERNAL_DEFAULT_CONFIG ) )
				{
					writer.write( "# ----> Default Config: " + defConfig + RETURN );
				}
				writer.write( RETURN );
			}

			for( final String module: Config.getList( Config.INTERNAL_BLJ_MODULE ) )
			{
				writer.write( Config.INTERNAL_BLJ_MODULE + " " + module + RETURN );
			}

			writer.write( RETURN );

			final Map<String, String> map = Config.getProperties();
			map.remove( Config.INTERNAL_BLJ_MODULE );
			map.remove( Config.INTERNAL_PIPELINE_DIR );
			map.remove( Config.INTERNAL_DEFAULT_CONFIG );
			map.remove( Config.INTERNAL_PAIRED_READS );
			map.remove( Config.INTERNAL_ALL_MODULES );
			map.remove( Config.INTERNAL_MULTIPLEXED );
			map.remove( SeqUtil.INTERNAL_SEQ_HEADER_CHAR );
			map.remove( SeqUtil.INTERNAL_SEQ_TYPE );
			map.remove( Config.PROJECT_DEFAULT_PROPS );

			final Iterator<String> it = map.keySet().iterator();
			while( it.hasNext() )
			{
				final String key = it.next();
				writer.write( key + "=" + map.get( key ) + RETURN );
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
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
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getTempConfig() ) );
		final BufferedReader reader = BioLockJUtil.getFileReader( getMasterConfig() );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, "=" );
				if( st.countTokens() < 2 || !props.keySet().contains( st.nextToken() ) )
				{
					writer.write( line + RETURN );
				}
			}

			for( final String name: props.keySet() )
			{
				writer.write( name + "=" + props.get( name ) + RETURN );
			}

			FileUtils.forceDelete( getMasterConfig() );
			FileUtils.moveFile( getTempConfig(), getMasterConfig() );
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
			if( writer != null )
			{
				writer.close();
			}
		}
	}

	private static File getTempConfig() throws Exception
	{
		return new File( Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath() + File.separator
				+ TEMP_PREFIX + Config.getConfigFileName() );
	}

	/**
	 * Prefix added to the master Config file: {@value #MASTER_PREFIX}
	 */
	public static final String MASTER_PREFIX = "MASTER_";
	private static final String RETURN = BioLockJ.RETURN;
	private static final String TEMP_PREFIX = "TEMP_";
}
