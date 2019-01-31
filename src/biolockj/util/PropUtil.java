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
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.*;

/**
 * Simple utility containing String manipulation and formatting functions.
 */
public class PropUtil
{

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
		return new File( Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath() + File.separator
				+ MASTER_PREFIX + configName );
	}

	/**
	 * 
	 * @throws Exception
	 */
	public static void sanitizeMasterConfig() throws Exception
	{
		final Map<String, String> nonNullProps = new HashMap<>();;
		final Map<String, String> usedProps = Config.getUsedProps();

		if( !Log.doDebug() )
		{
			Log.info( PropUtil.class, "To view the list of removed Config properties in future runs, enable: "
					+ Log.LOG_LEVEL_PROPERTY + "=" + Config.TRUE );
			Log.info( PropUtil.class,
					"To add DEBUG statements for only this utility class, add the property (this is a list property so multiple"
							+ " class names could be provided - in this example, wee add a single class): "
							+ Log.LIMIT_DEBUG_CLASSES + "=" + PropUtil.class.getName() );
		}

		for( final String key: usedProps.keySet() )
		{
			final String val = usedProps.get( key );
			if( val == null || val.trim().isEmpty() )
			{
				Log.debug( PropUtil.class, "Remove unused property from sanatized MASTER Config: " + key + "=" + val );
			}
			else
			{
				nonNullProps.put( key, val );
			}
		}

		Log.info( PropUtil.class,
				"Sanitizing MASTER Config file so only properties accessed during pipeline execution are retained." );
		Log.info( PropUtil.class, "The original version of project Config contained: "
				+ Config.getInitialProperties().size() + " properties" );
		Log.info( PropUtil.class,
				"The final version of MASTER Config contains: " + nonNullProps.size() + " properties" );

		saveNewMasterConfig( nonNullProps );
	}

	/**
	 * Save a single version of the config file by combing with default config files, if any exist. If props are
	 * provided, then only include these proerty values from the Config.
	 * 
	 * @param props Properties map
	 * @throws Exception if errors occur
	 */
	public static void saveNewMasterConfig( Map<String, String> props ) throws Exception
	{
		final File masterConfig = getMasterConfig();
		final boolean masterExists = masterConfig.exists();
		if( props == null && masterExists && !RuntimeParamUtil.doRestart() )
		{
			throw new Exception( "MASTER Config should not exist unless restarting a failed pipeline, but was found: "
					+ masterConfig.getAbsolutePath() );
		}

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
			writeConfigHeaders( writer );
			if( props != null )
			{
				writeCompleteHeader( writer, props );
			}

			if( props == null )
			{
				props = Config.getProperties();
			}
			final TreeMap<String, String> map = new TreeMap<>( props );
			map.remove( Config.INTERNAL_BLJ_MODULE );
			map.remove( Config.PROJECT_DEFAULT_PROPS );

			final Set<String> keys = new TreeSet<>( map.keySet() );
			for( final String key: keys )
			{
				if( !key.startsWith( INTERNAL_PREFIX ) )
				{
					writer.write( key + "=" + map.get( key ) + RETURN );
				}
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
	 * This method removes any given props and then adds the new values.<br>
	 * 
	 * @param props Collection of config props
	 * @throws Exception if errors occur
	 */
	public static void updateMasterConfig( final Map<String, String> props )
			throws Exception
	{
		final List<String> configLinesNotInProps = getHeaderLines( props.keySet() );
	//	final Map<String, String> prevProps = getProps( getMasterConfig() );
		if( props == null || props.isEmpty() )
		{
			Log.warn( PropUtil.class, "Calling updateMasterConfig() but no changes have been detected!" );
			return;
		}
		
		Log.info( PropUtil.class, "Update MASTER config va updateMasterConfig()" );

		BufferedWriter writer = new BufferedWriter( new FileWriter( getTempConfig() ) );
		try
		{
			for( final String line: configLinesNotInProps )
			{
				final StringTokenizer st = new StringTokenizer( line, "=" );
				final int numTokens = st.countTokens();
				final String token = st.nextToken();
				final boolean headingLine = token != null && !token.trim().endsWith( "=" ) && numTokens == 1;
				if( headingLine )
				{
					writer.write( line + RETURN );
				}
			}

			final TreeSet<String> keys = new TreeSet<>( props.keySet() );
			for( final String key: keys )
			{
				Log.info( PropUtil.class, "Update MASTER config property: " + key + "=" + props.get( key ) );
				writer.write( key + "=" + props.get( key ) + RETURN );
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
						"Error occurred updating MASTER config.  Orig MASTER was deleted, please recover using the backup file: "
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

	/**
	 * Return the lines that are not properties (comments + modules). Will throw error if none found, it will never
	 * return null or empty list
	 */
	private static List<String> getHeaderLines( final Collection<String> props ) throws Exception
	{
		final List<String> lines = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( getMasterConfig() );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( line != null )
				{
					final StringTokenizer st = new StringTokenizer( line, "=" );
					final int numTokens = st.countTokens();
					if( numTokens < 2 || !props.contains( st.nextToken() ) )
					{
						lines.add( line );
					}
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

		if( lines.isEmpty() )
		{
			final File masterProps = getMasterConfig();
			final String displayVal = masterProps == null || !masterProps.exists() ? "<FILE NOT FOUND>"
					: masterProps.getAbsolutePath();
			if( masterProps == null || !masterProps.exists() )
			{
				throw new Exception( "MASTER Config properties is empty! --> " + displayVal );
			}
			throw new Exception( "Failed Attempt to update " );
		}

		return lines;
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
				if( line.startsWith( PROJ_CONFIG_FLAG ) || line.startsWith( DEFAULT_CONFIG_FLAG ) )
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

	private static Map<String, String> getProps( final File file ) throws Exception
	{
		final Map<String, String> props = new HashMap<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( getMasterConfig() );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( line != null )
				{
					final int index = line.indexOf( "=" );
					if( index > -1 )
					{
						props.put( line.substring( 0, index ), line.substring( index + 1 ) );
					}
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

		return props;
	}

	private static File getTempConfig() throws Exception
	{
		return new File( Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath() + File.separator
				+ TEMP_PREFIX + Config.getConfigFileName() );
	}

	private static void writeCompleteHeader( final BufferedWriter writer, final Map<String, String> props )
			throws Exception
	{
		writer.write( "####################################################################################" + RETURN );
		writer.write( "#" + RETURN );
		writer.write( "#   Based on the above configuration, the following pipeline was run." + RETURN );
		writer.write( "#   The additional BioModules were added as required pre/postrequisits or as " + RETURN );
		writer.write( "#   implicit modules that BioLockJ determined were required to meet BioLockJ " + RETURN );
		writer.write( "#   standard requirements or BioModule input file format requirments." + RETURN );
		writer.write( "#" + RETURN );
		for( final String mod: Config.requireList( Config.INTERNAL_ALL_MODULES ) )
		{
			writer.write( "#      " + Config.INTERNAL_BLJ_MODULE + " " + mod + RETURN );
		}
		writer.write( "#" + RETURN );
		writer.write( "####################################################################################" + RETURN );

		if( Log.doDebug() )
		{

			writer.write( "###" + RETURN );
			writer.write( "###   Pipline = DEBUG mode so printing internal properties - FYI only." + RETURN );
			writer.write( "###   Internal properties are discarded at runtime & refenerated as needed." + RETURN );
			writer.write( "###" + RETURN );
			writer.write( "###   [ " + Constants.DISABLE_ADD_IMPLICIT_MODULES + "=" + Config.TRUE
					+ " ] to run this full list because it includes the implicit BioModules" + RETURN );
			writer.write( "###" + RETURN );
			final TreeSet<String> keys = new TreeSet<>( props.keySet() );
			for( final String key: keys )
			{
				final String val = props.get( key );
				if( key.startsWith( INTERNAL_PREFIX ) && val != null && !val.isEmpty() )
				{

					writer.write( "###     " + key + "=" + props.get( key ) + RETURN );
				}
			}
			writer.write( "###" + RETURN );
			writer.write(
					"####################################################################################" + RETURN );
		}
		writer.write( RETURN );
	}

	private static void writeConfigHeaders( final BufferedWriter writer ) throws Exception
	{
		writer.write( "# The MASTER Config file can be used to fully reproduce all pipeline analysis." + RETURN );
		writer.write( "# The MASTER Config file was generated from the following Config files: " + RETURN );
		final List<String> initConfig = getInitConfig();
		if( initConfig == null )
		{
			writer.write( PROJ_CONFIG_FLAG + Config.getConfigFilePath() + RETURN );

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
	}

	/**
	 * Prefix added to the master Config file: {@value #MASTER_PREFIX}
	 */
	public static final String MASTER_PREFIX = "MASTER_";

	private static final String DEFAULT_CONFIG_FLAG = "# ----> Default Config: ";
	private static final String INTERNAL_PREFIX = "internal.";
	private static final String PROJ_CONFIG_FLAG = "# ----> Project Config: ";
	private static final String RETURN = BioLockJ.RETURN;
	private static final String TEMP_PREFIX = ".TEMP_";
}
