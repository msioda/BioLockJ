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
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.implicit.qiime.QiimeClassifier;

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
		return new File( Config.pipelinePath() + File.separator + MASTER_PREFIX + configName );
	}

	/**
	 * Remove unused properties from the MASTER config file.
	 * 
	 * @throws Exception if runtime errors occur
	 */
	public static void sanitizeMasterConfig() throws Exception
	{
		final Map<String, String> props = new HashMap<>();
		final Map<String, String> usedProps = Config.getUsedProps();

		final String defaultDemux = ModuleUtil.getDefaultDemultiplexer();
		final String defaultFaCon = ModuleUtil.getDefaultFastaConverter();
		final String defaultMerger = ModuleUtil.getDefaultMergePairedReadsConverter();
		final String defaultStats = ModuleUtil.getDefaultStatsModule();
		final Set<String> configMods = Config.requireSet( null, Constants.INTERNAL_BLJ_MODULE );
		boolean foundQiime = false;
		for( final String mod: configMods )
		{
			if( mod.toLowerCase().contains( Constants.QIIME ) )
			{
				foundQiime = true;
			}
		}

		if( !foundQiime )
		{
			usedProps.remove( QiimeClassifier.QIIME_ALPHA_DIVERSITY_METRICS );
		}

		if( !ModuleUtil.moduleExists( defaultDemux ) && !configMods.contains( defaultDemux ) )
		{
			usedProps.remove( Constants.DEFAULT_MOD_DEMUX );
			usedProps.remove( MetaUtil.META_BARCODE_COLUMN );
			usedProps.remove( Constants.DEFAULT_MOD_DEMUX );
			usedProps.remove( Constants.DEFAULT_MOD_DEMUX );
		}

		if( !ModuleUtil.moduleExists( defaultFaCon ) && !configMods.contains( defaultFaCon ) )
		{
			usedProps.remove( Constants.DEFAULT_MOD_FASTA_CONV );
		}

		if( !ModuleUtil.moduleExists( defaultMerger ) && !configMods.contains( defaultMerger ) )
		{
			usedProps.remove( Constants.DEFAULT_MOD_SEQ_MERGER );
		}

		if( !configMods.contains( defaultStats ) && !ModuleUtil.moduleExists( defaultStats ) )
		{
			usedProps.remove( Constants.DEFAULT_STATS_MODULE );
		}

		if( !Log.doDebug() )
		{
			Log.info( PropUtil.class, "To view the list of removed Config properties in future runs, enable: "
					+ Log.LOG_LEVEL_PROPERTY + "=" + Constants.TRUE );
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
			else if( !key.startsWith( INTERNAL_PREFIX ) )
			{
				props.put( key, val );
			}
		}

		Log.info( PropUtil.class,
				"Sanitizing MASTER Config file so only properties accessed during pipeline execution are retained." );
		Log.info( PropUtil.class, "The original version of project Config contained: "
				+ Config.getInitialProperties().size() + " properties" );
		Log.info( PropUtil.class, "The final version of MASTER Config contains: " + props.size() + " properties" );

		saveMasterConfig( props );
	}

	/**
	 * Save a single version of the config file by combing with default config files, if any exist. If props are
	 * provided, then only include these proerty values from the Config.
	 * 
	 * @param props Properties map
	 * @throws Exception if errors occur
	 */
	public static void saveMasterConfig( Map<String, String> props ) throws Exception
	{
		final File masterConfig = getMasterConfig();
		final boolean masterExists = masterConfig.exists();
		if( masterExists )
		{
			FileUtils.moveFile( getMasterConfig(), getTempConfig() );
			if( getMasterConfig().exists() )
			{
				throw new Exception( "Cannot backup MASTER before modifying - trying to save as: "
						+ getTempConfig().getAbsolutePath() );
			}
		}

		final BufferedWriter writer = new BufferedWriter( new FileWriter( masterConfig ) );
		try
		{
			writeConfigHeaders( writer );
			if( props == null )
			{
				props = Config.getProperties();
			}
			else
			{
				writeCompleteHeader( writer, props );
			}

			props.remove( Constants.INTERNAL_BLJ_MODULE );
			props.remove( Constants.PROJECT_DEFAULT_PROPS );

			final Set<String> keys = new TreeSet<>( props.keySet() );
			for( final String key: keys )
			{
				if( !key.startsWith( INTERNAL_PREFIX ) )
				{
					writer.write( key + "=" + props.get( key ) + RETURN );
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

	private static File getTempConfig() throws Exception
	{
		return new File( Config.pipelinePath() + File.separator + TEMP_PREFIX + Config.getConfigFileName() );
	}

	private static void writeCompleteHeader( final BufferedWriter writer, final Map<String, String> props )
			throws Exception
	{

		final Set<String> configProps = Config.getSet( null, Constants.INTERNAL_BLJ_MODULE );
		final Set<String> allProps = Config.getSet( null, Constants.INTERNAL_ALL_MODULES );

		if( !configProps.equals( allProps ) )
		{
			writer.write(
					"####################################################################################" + RETURN );
			writer.write( "#" + RETURN );
			writer.write( "#   Based on the above configuration, the following pipeline was run." + RETURN );
			writer.write( "#   The additional BioModules were added as required pre/postrequisits or as" + RETURN );
			writer.write( "#   implicit modules that BioLockJ determined were required to meet BioLockJ" + RETURN );
			writer.write( "#   standard requirements or BioModule input file format requirments." + RETURN );
			writer.write( "#" + RETURN );
			for( final String mod: Config.requireList( null, Constants.INTERNAL_ALL_MODULES ) )
			{
				writer.write( "#      " + Constants.INTERNAL_BLJ_MODULE + " " + mod + RETURN );
			}
			writer.write( "#" + RETURN );
			writer.write(
					"####################################################################################" + RETURN );
		}
		if( Log.doDebug() )
		{
			writer.write( "###" + RETURN );
			writer.write( "###   Pipline = DEBUG mode so printing internal properties - FYI only." + RETURN );
			writer.write( "###   Internal properties are discarded at runtime & refenerated as needed." + RETURN );
			writer.write( "###" + RETURN );
			if( !configProps.equals( allProps ) )
			{
				writer.write( "###  Set [ " + Constants.DISABLE_ADD_IMPLICIT_MODULES + "=" + Constants.TRUE
						+ " ] to run this full list because it includes the implicit BioModules" + RETURN );
				writer.write( "###" + RETURN );
			}
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
		writer.write( "# The MASTER Config file was generated from the following Config files:" + RETURN );
		final List<String> initConfig = getInitConfig();
		if( initConfig == null )
		{
			writer.write( PROJ_CONFIG_FLAG + Config.getConfigFilePath() + RETURN );
			final List<String> defaults = Config.getList( null, Constants.INTERNAL_DEFAULT_CONFIG );
			if( defaults != null && !defaults.isEmpty() )
			{
				for( final String defConfig: Config.getList( null, Constants.INTERNAL_DEFAULT_CONFIG ) )
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
		for( final String module: Config.getList( null, Constants.INTERNAL_BLJ_MODULE ) )
		{
			writer.write( Constants.INTERNAL_BLJ_MODULE + " " + module + RETURN );
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
	private static final String RETURN = Constants.RETURN;
	private static final String TEMP_PREFIX = ".TEMP_";
}
