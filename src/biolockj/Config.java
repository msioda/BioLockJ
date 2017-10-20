/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer 	This code is free software; you can redistribute it and/or
 * 				modify it under the terms of the GNU General Public License
 * 				as published by the Free Software Foundation; either version 2
 * 				of the License, or (at your option) any later version,
 * 				provided that any use properly credits the author.
 * 				This program is distributed in the hope that it will be useful,
 * 				but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * 				GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;
import biolockj.module.BioModule;

/**
 * Config reads in the properties file and is used to initialize the root directory.
 */
public class Config extends Properties
{

	public Config()
	{
		super();
	}

	public Config( final Config defaults )
	{
		super( defaults );
	}

	protected void load( final FileInputStream fis ) throws IOException
	{
		final Scanner in = new Scanner( fis );
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		while( in.hasNext() )
		{
			out.write( in.nextLine().replace( "\\", "\\\\" ).getBytes() );
			out.write( BioLockJ.RETURN.getBytes() );
		}
		in.close();
		final InputStream is = new ByteArrayInputStream( out.toByteArray() );
		super.load( is );
	}

	public static void copyConfig() throws Exception
	{
		final File projectDir = Config.requireExistingDir( Config.PROJECT_DIR );
		if( defaultConfigFile != null )
		{
			FileUtils.copyFileToDirectory( defaultConfigFile, projectDir );
		}

		FileUtils.copyFileToDirectory( configFile, projectDir );
		configFile = new File( projectDir.getAbsolutePath() + File.separator + configFile.getName() );
	}

	/**
	 * Gets boolean value from prop file, if not found, return FALSE;
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static boolean getBoolean( final String propertyName ) throws Exception
	{
		if( ( getString( propertyName ) != null ) && getString( propertyName ).equalsIgnoreCase( Config.TRUE ) )
		{
			return true;
		}

		return false;
	}

	public static String getDoubleVal( final String propertyName ) throws Exception
	{
		if( getString( propertyName ) != null )
		{
			Double.parseDouble( getString( propertyName ) );
		}

		return getString( propertyName );
	}

	public static String getDownloadDir() throws Exception
	{
		String dir = Config.getString( Config.DOWNLOAD_DIR );
		if( Config.getBoolean( Config.SCRIPT_RUN_ON_CLUSTER ) && ( dir != null ) )
		{
			if( !dir.endsWith( File.separator ) )
			{
				dir = dir + File.separator;
			}
			return dir + Config.requireExistingDir( Config.PROJECT_DIR ).getName() + File.separator;
		}

		return null;
	}

	/**
	 * Get a directory - if it exists
	 *
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static File getExistingDir( final String propertyName ) throws Exception
	{
		final File f = getFileObject( propertyName );
		if( ( f != null ) && !f.isDirectory() )
		{
			throw new Exception( propertyName + " is not an existing directory!" );
		}
		return f;
	}

	/**
	 * Get a file - if it exists
	 *
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static File getExistingFile( final String propertyName ) throws Exception
	{
		final File f = getFileObject( propertyName );
		if( ( f != null ) && f.isDirectory() )
		{
			throw new Exception( propertyName + " is not an existing file!" );
		}
		return f;
	}

	/**
	 * Get a property as list (must be comma delimited)
	 * @param propName
	 * @return
	 */
	public static List<String> getList( final String propName )
	{
		final List<String> list = new ArrayList<>();
		final String val = getAProperty( propName );
		if( val != null )
		{
			final StringTokenizer st = new StringTokenizer( val, "," );
			while( st.hasMoreTokens() )
			{
				list.add( st.nextToken().trim() );
			}
		}

		return list;
	}

	public static String getLogPrefix() throws Exception
	{
		return "Log" + Config.requireString( Config.REPORT_LOG_BASE ) + "_";
	}

	public static List<BioModule> getModules() throws Exception
	{
		if( bioModules.isEmpty() )
		{
			final BufferedReader reader = new BufferedReader( new FileReader( configFile ) );
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( line.startsWith( Config.BLJ_MODULE ) )
				{
					bioModules.add(
							(BioModule) Class.forName( line.substring( line.indexOf( " " ) + 1 ) ).newInstance() );
				}
			}

			reader.close();
		}

		return bioModules;
	}

	/**
	 * Get positive integer from prop file, if it exists, otherwise return null
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static Integer getNonNegativeInteger( final String propertyName ) throws Exception
	{
		final Integer val = getIntegerProp( propertyName );
		if( ( val != null ) && ( val < 0 ) )
		{
			throw new Exception( propertyName + " must contain a non-negative integer value if configured - "
					+ "instead, property value = " + ( ( val == null ) ? "null": val ) );
		}
		return val;
	}

	public static String getPositiveDoubleVal( final String propertyName ) throws Exception
	{
		final String val = getDoubleVal( propertyName );
		if( val != null )
		{
			if( Double.parseDouble( val ) < 0 )
			{
				throw new Exception( propertyName + " must be a positive double value!" );
			}
		}

		return val;
	}

	/**
	 * Get positive integer from prop file, if it exists, otherwise return null
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static Integer getPositiveInteger( final String propertyName ) throws Exception
	{
		final Integer val = getIntegerProp( propertyName );
		if( ( val != null ) && ( val < 1 ) )
		{
			throw new Exception( propertyName + " must contain a positive integer value if configured -  "
					+ "instead, property value = " + ( ( val == null ) ? "null": val ) );
		}
		return val;
	}

	/**
	 * Convenience method returns all prop values as a Map.
	 * @return Map<String, String> properties
	 */
	public static TreeMap<String, String> getProperties() throws Exception
	{
		final TreeMap<String, String> map = new TreeMap<>();
		final Iterator<String> it = props.stringPropertyNames().iterator();
		while( it.hasNext() )
		{
			final String key = it.next();
			map.put( key, props.getProperty( key ) );
		}
		return map;
	}

	public static Set<String> getPropertyAsSet( final String propName )
	{
		final Set<String> set = new TreeSet<>();
		set.addAll( getList( propName ) );
		return set;
	}

	/**
	 * Get property as list (must be comma delimited in prop file)
	 * @param propertyName
	 * @return
	 */
	public static Set<String> getSet( final String propertyName )
	{
		return Config.getPropertyAsSet( propertyName );
	}

	/**
	 * Get required string value from ConfigRead4er.
	 * @param propertyName
	 * @return
	 */
	public static String getString( final String propertyName )
	{
		return Config.getAProperty( propertyName );
	}

	/**
	 * A new Config simply must read in props from the prop file.
	 * @param file
	 * @throws Exception
	 */
	public static void loadProperties( String file ) throws Exception
	{
		if( file.startsWith( "~" ) )
		{
			file = System.getProperty( "user.home" ) + file.substring( 1 );
		}

		configFile = new File( file );
		props = readProps( configFile, null );
		String defaultProps = getAProperty( DEFAULT_PROPERTIES );
		if( ( defaultProps != null ) && defaultProps.startsWith( "~" ) )
		{
			defaultProps = System.getProperty( "user.home" ) + defaultProps.substring( 1 );
		}

		if( defaultProps != null )
		{
			defaultConfigFile = new File( defaultProps );
			if( !defaultConfigFile.exists() )
			{
				throw new Exception( DEFAULT_PROPERTIES + " property defined but file does not exist!" );
			}

			props = readProps( configFile, readProps( defaultConfigFile, null ) );
		}
		props.list( System.out );
	}

	public static Config readProps( final File config, final Config defaultProps ) throws Exception
	{
		if( config.exists() )
		{
			final FileInputStream in = new FileInputStream( config );
			final Config tempProps = ( defaultProps == null ) ? new Config(): new Config( defaultProps );
			tempProps.load( in );
			in.close();
			return tempProps;
		}

		return null;
	}

	/**
	 * Requires boolean in prop file.
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static boolean requireBoolean( final String propertyName ) throws Exception
	{
		if( requireString( propertyName ).equalsIgnoreCase( Config.TRUE ) )
		{
			return true;
		}
		if( requireString( propertyName ).equalsIgnoreCase( Config.FALSE ) )
		{
			return false;
		}

		throw new Exception( propertyName + " MUST BE SET TO EITHER " + Config.TRUE + " or " + Config.FALSE );
	}

	public static String requireDoubleVal( final String propertyName ) throws Exception
	{
		Double.parseDouble( requireString( propertyName ) );
		return requireString( propertyName );
	}

	/**
	 * Get required existing directory.
	 *
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static File requireExistingDir( final String propertyName ) throws Exception
	{
		final File f = getExistingDir( propertyName );
		if( f == null )
		{
			throwPropNotFoundException( propertyName );
		}

		return f;
	}

	/**
	 * Get list of required directories.
	 *
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static List<File> requireExistingDirs( final String propertyName ) throws Exception
	{
		final List<File> returnDirs = new ArrayList<>();
		for( String d: requireSet( propertyName ) )
		{
			if( d.startsWith( "~" ) )
			{
				d = System.getProperty( "user.home" ) + d.substring( 1 );
			}

			final File dir = new File( d );
			if( ( dir == null ) || !dir.isDirectory() )
			{
				throwPropNotFoundException( propertyName );
			}
			returnDirs.add( dir );
		}

		return returnDirs;
	}

	/**
	 * Get required file.
	 *
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static File requireExistingFile( final String propertyName ) throws Exception
	{
		final File f = getExistingFile( propertyName );
		if( f == null )
		{
			throwPropNotFoundException( propertyName );
		}
		return f;
	}

	/**
	 * Get required  integer from prop file.
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static int requireInteger( final String propertyName ) throws Exception
	{
		final Integer val = getIntegerProp( propertyName );
		if( val == null )
		{
			throw new Exception( propertyName + " must be an integer value!" );
		}

		return val;
	}

	/**
	 * Get required list (must be comma delimited value in prop file).
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static List<String> requireList( final String propertyName ) throws Exception
	{
		final List<String> val = getList( propertyName );
		if( ( val == null ) || val.isEmpty() )
		{
			throwPropNotFoundException( propertyName );
		}
		return val;
	}

	public static String requirePositiveDoubleVal( final String propertyName ) throws Exception
	{
		final String val = requireDoubleVal( propertyName );
		final Double dub = Double.parseDouble( val );
		if( dub < 0 )
		{
			throw new Exception( propertyName + " must be a positive double value!" );
		}

		return val;
	}

	/**
	 * Get required positive integer from prop file.
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static int requirePositiveInteger( final String propertyName ) throws Exception
	{
		final int val = requireInteger( propertyName );
		if( val < 1 )
		{
			throw new Exception( propertyName + " must be a positive integer value!" );
		}
		return val;
	}

	/**
	 * Get property as list (must be comma dlimited in prop file)
	 * @param propertyName
	 * @return
	 */
	public static Set<String> requireSet( final String propertyName ) throws Exception
	{
		final Set<String> val = getSet( propertyName );
		if( ( val == null ) || val.isEmpty() )
		{
			throwPropNotFoundException( propertyName );
		}
		return val;
	}

	/**
	 * Get required String value from prop file.
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static String requireString( final String propertyName ) throws Exception
	{
		if( getString( propertyName ) == null )
		{
			throwPropNotFoundException( propertyName );
		}

		return getString( propertyName ).trim();
	}

	public static List<String> requireTaxonomy() throws Exception
	{
		if( taxonomy == null )
		{
			taxonomy = new ArrayList<>();
			final List<String> taxa = Config.requireList( Config.REPORT_TAXONOMY_LEVELS );
			if( taxa.contains( DOMAIN ) )
			{
				taxonomy.add( DOMAIN );
			}
			if( taxa.contains( PHYLUM ) )
			{
				taxonomy.add( PHYLUM );
			}
			if( taxa.contains( CLASS ) )
			{
				taxonomy.add( CLASS );
			}
			if( taxa.contains( ORDER ) )
			{
				taxonomy.add( ORDER );
			}
			if( taxa.contains( FAMILY ) )
			{
				taxonomy.add( FAMILY );
			}
			if( taxa.contains( GENUS ) )
			{
				taxonomy.add( GENUS );
			}
			if( taxa.contains( SPECIES ) )
			{
				taxonomy.add( SPECIES );
			}
		}
		return taxonomy;
	}

	/**
	 * Set a prop value for a single value.
	 * @param name
	 * @param value
	 */
	public static void setAProperty( final String name, final String value )
	{
		props.setProperty( name, value );
	}

	/**
	 * Set a prop value for a list.
	 * @param name
	 * @param list
	 */
	public static void setProperty( final String name, final Collection<String> list )
	{
		final StringBuffer sb = new StringBuffer();
		for( final String val: list )
		{
			if( sb.length() > 0 ) // add to existing list string
			{
				sb.append( "," );
			}
			sb.append( val );
		}

		props.setProperty( name, sb.toString() );
	}

	/**
	 * Generic exception to throw when a property cannot be found in prop file.
	 * @param prop
	 * @throws Exception
	 */
	public static void throwPropNotFoundException( final String prop ) throws Exception
	{
		throw new Exception( "Config File Error: [ " + prop
				+ " ] is missing or invalid!  Please update your config and restart BioLockJ." );
	}

	/**
	 * Gets the value for a given property.
	 * @param propName
	 * @return
	 */
	private static String getAProperty( final String propName )
	{
		final Object obj = props.getProperty( propName );
		if( obj == null )
		{
			return null;
		}

		String val = obj.toString().trim();

		// allow statements like x = $someOtherDir to avoid re-typing paths
		if( val.startsWith( "$" ) )
		{
			val = props.getProperty( val.substring( 1 ) );

			if( val == null )
			{
				return null;
			}

			val = val.trim();
		}

		if( val.isEmpty() )
		{
			return null;
		}

		return val;
	}

	private static File getFileObject( final String propertyName ) throws Exception
	{
		String val = getString( propertyName );
		if( val != null )
		{
			if( val.startsWith( "~" ) )
			{
				val = System.getProperty( "user.home" ) + val.substring( 1 );
			}

			final File f = new File( val );
			if( f.exists() )
			{
				return f;
			}
		}

		return null;
	}

	/**
	 * Get integer value from config file, if it exists, otherwise return null
	 * @param propertyName
	 * @return integer value or null
	 * @throws Exception
	 */
	private static Integer getIntegerProp( final String propertyName ) throws Exception
	{
		if( getString( propertyName ) != null )
		{
			return Integer.parseInt( getString( propertyName ) );
		}

		return null;
	}

	public static final String BLJ_MODULE = "#BioLockJ";
	public static final String CLASS = "class";
	public static final String CLUSTER_DOWNLOAD_MODULE = "cluster.downloadModule";
	public static final String DEFAULT_PROPERTIES = "project.defaultProps";
	public static final String DOMAIN = "domain";
	public static final String DOWNLOAD_DIR = "cluster.downloadDir";
	public static final String FALSE = "N";
	public static final String FAMILY = "family";
	public static final String GENUS = "genus";
	public static final String INPUT_DEMULTIPLEX = "input.demultiplex";
	public static final String INPUT_DIRS = "input.dirs";
	public static final String INPUT_FORWARD_READ_SUFFIX = "input.forwardFileSuffix";
	public static final String INPUT_IGNORE_FILES = "input.ignoreFiles";
	public static final String INPUT_METADATA = "input.metadata";
	public static final String INPUT_PAIRED_READS = "input.pairedReads";
	public static final String INPUT_REVERSE_READ_SUFFIX = "input.reverseFileSuffix";
	public static final String INPUT_TRIM_PREFIX = "input.trimPrefix";
	public static final String INPUT_TRIM_SUFFIX = "input.trimSuffix";
	public static final String ORDER = "order";
	public static final String PHYLUM = "phylum";
	public static final String PROJECT_DIR = "project.dir";
	public static final String PROJECT_NAME = "project.name";
	public static final String RDP_THRESHOLD_SCORE = "rdp.minThresholdScore";
	public static final String REPORT_ADD_GENUS_NAME_TO_SPECIES = "report.addGenusToSpeciesName";
	public static final String REPORT_FULL_TAXONOMY_NAMES = "report.fullTaxonomyNames";

	public static final String REPORT_LOG_BASE = "report.logBase";
	public static final String REPORT_NUM_HITS = "report.numHits";
	public static final String REPORT_NUM_READS = "report.numReads";
	public static final String REPORT_TAXONOMY_LEVELS = "report.taxonomyLevels";
	public static final String REPORT_USE_GENUS_FIRST_INITIAL = "report.useGenusFirstInitial";
	public static final String SCRIPT_BATCH_SIZE = "script.batchSize";
	public static final String SCRIPT_CHMOD_COMMAND = "script.chmodCommand";
	public static final String SCRIPT_EXIT_ON_ERROR = "script.exitOnError";
	public static final String SCRIPT_NUM_THREADS = "script.numThreads";
	public static final String SCRIPT_RUN_ON_CLUSTER = "script.runOnCluster";
	public static final String SPECIES = "species";
	public static final String TRUE = "Y";
	private static final List<BioModule> bioModules = new ArrayList<>();
	private static File configFile = null;
	private static File defaultConfigFile = null;
	private static Config props = null;
	private static final long serialVersionUID = 2980376615128441545L;
	private static List<String> taxonomy = null;
}