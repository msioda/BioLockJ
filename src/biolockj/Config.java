/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;
import biolockj.util.BioLockJUtil;
import biolockj.util.RuntimeParamUtil;
import biolockj.util.TaxaUtil;

/**
 * Provides type-safe, validated methods for storing/accessing system properties.<br>
 * Initially populated by the properties in the Config file, several additional properties are created and stored in the
 * the Config (to save system determined info such as: pipeline directory and name, has paired reads?, has multiplexed
 * reads?, etc.).
 */
public class Config
{
	/**
	 * Parse property value (Y or N) to return boolean, if not found, return false;
	 *
	 * @param propertyName Property name
	 * @return boolean value
	 * @throws ConfigFormatException if property value is not null but also not Y or N.
	 */
	public static boolean getBoolean( final String propertyName ) throws ConfigFormatException
	{
		if( getString( propertyName ) != null && getString( propertyName ).equalsIgnoreCase( TRUE ) )
		{
			return true;
		}
		else if( getString( propertyName ) == null )
		{
			Config.setConfigProperty( propertyName, FALSE );
			Log.debug( Config.class, propertyName + " is undefined, so return: " + FALSE );
		}
		else if( !getString( propertyName ).equalsIgnoreCase( FALSE ) )
		{
			throw new ConfigFormatException( propertyName, "Boolean properties must be set to either " + TRUE + " or "
					+ FALSE + ".  Update this property in your Config file to a valid option." );
		}

		return false;
	}

	/**
	 * Gets the configuration file extension (often ".properties")
	 *
	 * @return Config file extension
	 */
	public static String getConfigFileExt()
	{
		String ext = null;
		final StringTokenizer st = new StringTokenizer( configFile.getName(), "." );
		if( st.countTokens() > 1 )
		{
			while( st.hasMoreTokens() )
			{
				ext = st.nextToken();
			}
		}

		return "." + ext;
	}

	/**
	 * Gets the configuration file name
	 *
	 * @return Config file name
	 */
	public static String getConfigFileName()
	{
		return configFile.getName();
	}

	/**
	 * Gets the full Config file path passed to BioLockJ as a runtime parameter.
	 *
	 * @return Config file path
	 */
	public static String getConfigFilePath()
	{
		return configFile.getAbsolutePath();
	}

	/**
	 * Parse property for numeric (double) value
	 *
	 * @param propertyName Property name
	 * @return Double value or null
	 * @throws ConfigFormatException if propertyName is defined, but set with a non-numeric value
	 */
	public static Double getDoubleVal( final String propertyName ) throws ConfigFormatException
	{
		if( getString( propertyName ) != null )
		{
			try
			{
				final Double val = Double.parseDouble( getString( propertyName ) );
				return val;
			}
			catch( final Exception ex )
			{
				throw new ConfigFormatException( propertyName, "Property only accepts numeric values" );
			}
		}
		return null;
	}

	/**
	 * Get exe.* property name. If null, return the property name (without the "exe." prefix)
	 *
	 * @param propertyName Property name
	 * @return String value of executable
	 * @throws Exception if propertyName does not start with "exe."
	 */
	public static String getExe( final String propertyName ) throws Exception
	{
		if( !propertyName.startsWith( "exe." ) )
		{
			throw new Exception( "Config.getExe() can be called for properties that begin with \"exe.\"" );
		}

		// return name of property after trimming "exe.", for example if exe.pear is undefined, return "pear"
		if( getString( propertyName ) == null || getString( propertyName ).equals( propertyName.substring( 4 ) ) )
		{
			return propertyName.substring( 4 );
		}

		// DO NOT VERIFY FILE EXISTS --> EXECUTABLE MAY BE CLUSTER MODULE OR ALIAS
		// if( !RuntimeParamUtil.isDockerMode() && !BashScriptBuilder.clusterModuleExists( getString( propertyName ) ) )
		// {
		// return requireExistingFile( propertyName ).getAbsolutePath();
		// }

		return getString( propertyName );
	}

	/**
	 * Get a valid File directory or return null
	 *
	 * @param propertyName Property name
	 * @return File directory or null
	 * @throws ConfigPathException if path is defined but is not an existing directory
	 */
	public static File getExistingDir( final String propertyName ) throws ConfigPathException
	{
		final File f = getExistingFileObject( getString( propertyName ) );
		if( f != null && !f.isDirectory() )
		{
			throw new ConfigPathException( propertyName, ConfigPathException.DIRECTORY );
		}
		return f;
	}

	/**
	 * Get a valid File or return null. If path is a directory containing exactly 1 file, return that file.
	 *
	 * @param propertyName Property name
	 * @return File (not directory) or null
	 * @throws ConfigPathException if path is defined but is not an existing file
	 */
	public static File getExistingFile( final String propertyName ) throws ConfigPathException
	{
		final File f = getExistingFileObject( getString( propertyName ) );
		if( f != null && !f.isFile() )
		{
			if( f.isDirectory() && f.list( HiddenFileFilter.VISIBLE ).length == 1 )
			{
				Log.warn( Config.class,
						propertyName + " is a directory with only 1 valid file.  Return the lone file within." );
				return new File( f.list( HiddenFileFilter.VISIBLE )[ 0 ] );
			}

			throw new ConfigPathException( propertyName, ConfigPathException.FILE );
		}
		return f;
	}

	/**
	 * Get initial properties ordered by propertyName
	 *
	 * @return map ordered by propertyName
	 */
	public static TreeMap<String, String> getInitialProperties()
	{
		return convertToMap( unmodifiedInputProps );
	}

	/**
	 * Parse comma delimited property value to return list
	 *
	 * @param propertyName Property name
	 * @return List of String values (or an empty list)
	 */
	public static List<String> getList( final String propertyName )
	{
		final List<String> list = new ArrayList<>();
		final String val = getString( propertyName );
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

	/**
	 * Parse property as non-negative integer value
	 *
	 * @param propertyName Property name
	 * @return Non-negative integer or null
	 * @throws ConfigFormatException if defined but is not a non-negative integer value
	 */
	public static Integer getNonNegativeInteger( final String propertyName ) throws ConfigFormatException
	{
		final Integer val = getIntegerProp( propertyName );
		if( val != null && val < 0 )
		{
			throw new ConfigFormatException( propertyName, "Property only accepts non-negative integer values" );
		}
		return val;
	}

	// timestamp.getTime()
	public static File getOrigConfig() throws Exception
	{
		if( origConfigFile != null && origConfigFile.exists() )
		{
			return origConfigFile;
		}
		if( origConfigFile == null )
		{
			origConfigFile = new File( Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath()
					+ File.separator + ORIG_PREFIX + configFile.getName() );
		}
		if( !origConfigFile.exists() )
		{
			final FileWriter writer = new FileWriter( origConfigFile );
			writer.close();
		}
		
		return origConfigFile;
	}

	/**
	 * Parse property as positive double value
	 *
	 * @param propertyName Property name
	 * @return Positive Double value or null
	 * @throws ConfigFormatException if propertyName is defined, but not set with a positive number
	 */
	public static Double getPositiveDoubleVal( final String propertyName ) throws ConfigFormatException
	{
		final Double val = getDoubleVal( propertyName );
		if( val != null && val <= 0 )
		{
			throw new ConfigFormatException( propertyName, "Property only accepts positive numeric values" );
		}

		return val;
	}

	/**
	 * Parse property as positive integer value
	 *
	 * @param propertyName Property name
	 * @return Positive Integer value or null
	 * @throws ConfigFormatException if propertyName is defined, but not set with a positive integer
	 */
	public static Integer getPositiveInteger( final String propertyName ) throws ConfigFormatException
	{
		final Integer val = getIntegerProp( propertyName );
		if( val != null && val <= 0 )
		{
			throw new ConfigFormatException( propertyName, "Property only accepts positive integer values" );
		}
		return val;
	}

	/**
	 * Get current properties ordered by propertyName
	 *
	 * @return map ordered by propertyName
	 */
	public static TreeMap<String, String> getProperties()
	{
		return convertToMap( props );
	}

	/**
	 * Parse comma-separated property value to build an unordered Set
	 *
	 * @param propertyName Property name
	 * @return Set of values or an empty set (if no values)
	 */
	public static Set<String> getSet( final String propertyName )
	{
		final Set<String> set = new HashSet<>();
		set.addAll( getList( propertyName ) );
		return set;
	}

	/**
	 * Get property value as String. Empty strings return null.<br>
	 * If $BLJ or $BLJ_SUP or $USER or $HOME was used, it would already be converted to the actual file path by
	 * {@link biolockj.Properties} before this method is called.
	 *
	 * @param propertyName {@link biolockj.Config} file property name
	 * @return String value or null
	 */
	public static String getString( final String propertyName )
	{

		String val = null;
		final Object obj = props.getProperty( propertyName );
		if( obj == null )
		{
			usedProps.put( propertyName, null );
			return null;
		}

		val = obj.toString().trim();

		/*
		 * Allow internal references to avoid re-typing paths. For example:
		 * 
		 * project.dataDir=/projects/data/internal/research_labs project.experimentID=1987209C
		 * project.labUrl=$project.dataDir/fodor_lab/$project.experimentID reportBuilder.massSpecReportHeading=Mass Spec
		 * $project.labID
		 */
		if( val.contains( "${" ) && val.contains( "}" ) )
		{
			val = getInternalRefProp( val );
		}

		if( val.isEmpty() )
		{
			usedProps.put( propertyName, "" );
			return null;
		}

		usedProps.put( propertyName, val );

		return val;
	}

	/**
	 * Return filePath with system parameter values replaced. If filePath starts with "~" or $HOME it is replaced with
	 * System.getProperty( "user.home" ). If filePath contains $USER it is replaced with System.getProperty( "user.name"
	 * ).
	 * 
	 * @param filePath File path
	 * @return Formatted filePath
	 * @throws ConfigPathException if path is invalid
	 */
	public static String getSystemFilePath( String filePath ) throws ConfigPathException
	{

		if( filePath != null && filePath.startsWith( "~" ) )
		{
			Log.debug( Config.class, "Replacing ~ in file-path: " + filePath );
			filePath = System.getProperty( "user.home" ) + filePath.substring( 1 );
			Log.debug( Config.class, "Updated file-path: " + filePath );
		}

		if( filePath != null && filePath.startsWith( "$HOME" ) )
		{
			Log.debug( Config.class, "Replacing $HOME in file-path: " + filePath );
			filePath = System.getProperty( "user.home" ) + filePath.substring( 5 );
			Log.debug( Config.class, "Updated file-path: " + filePath );
		}

		if( filePath != null && filePath.contains( "$USER" ) )
		{
			Log.debug( Config.class, "Replacing $USER in file-path: " + filePath );
			filePath = filePath.replace( "$USER", System.getProperty( "user.name" ) );
			Log.debug( Config.class, "Updated file-path: " + filePath );
		}

		if( filePath != null && filePath.contains( "$BLJ_SUP" ) )
		{
			final File bljSup = new File(
					BioLockJUtil.getBljDir().getParentFile().getAbsolutePath() + File.separator + BLJ_SUPPORT );
			if( bljSup.exists() && bljSup.isDirectory() )
			{
				Log.debug( Config.class, "Replacing $BLJ_SUP in file-path: " + filePath );
				filePath = filePath.replace( "$BLJ_SUP", bljSup.getAbsolutePath() );
				Log.debug( Config.class, "Updated file-path: " + filePath );
			}
			else
			{
				Log.warn( Config.class, "Could not find/replace $BLJ_SUP in file-path: " + filePath );
			}

		}
		else if( filePath != null && filePath.contains( "$BLJ" ) )
		{
			Log.debug( Config.class, "Replacing $BLJ in file-path: " + filePath );
			filePath = filePath.replace( "$BLJ", BioLockJUtil.getBljDir().getAbsolutePath() );
			Log.debug( Config.class, "Updated file-path: " + filePath );
		}

		return filePath;
	}

	/**
	 * Parse comma-separated property value to build an ordered Set
	 *
	 * @param propertyName Property name
	 * @return Set of values or an empty set (if no values)
	 */
	public static Set<String> getTreeSet( final String propertyName )
	{
		final Set<String> set = new TreeSet<>();
		set.addAll( getList( propertyName ) );
		return set;
	}

	public static Map<String, String> getUsedProps()
	{
		getString( Config.PROJECT_DEFAULT_PROPS );
		return new HashMap<>( usedProps );
	}

	/**
	 * Initialize Config by reading in props from file and setting taxonomy as an ordered list.
	 * 
	 * @throws Exception if unable to load Props
	 */
	public static void initialize() throws Exception
	{
		configFile = RuntimeParamUtil.getConfigFile();
		Log.info( Config.class, "Initialize Config: " + configFile.getAbsolutePath() );
		props = Properties.loadProperties( configFile );
		if( initProjectProps() )
		{
			origConfigFile = getOrigConfig();
			Log.enableLogs( false );
			unmodifiedInputProps = Properties.loadProperties( configFile );
			Log.enableLogs( true );
		}

		TaxaUtil.initTaxaLevels();
	}

	/**
	 * Check if running on cluster
	 * 
	 * @return TRUE if running on the cluster
	 */
	public static boolean isOnCluster()
	{
		return getString( PROJECT_ENV ) != null && getString( PROJECT_ENV ).equals( PROJECT_ENV_CLUSTER );
	}

	/**
	 * Required to return a valid boolean {@value #TRUE} or {@value #FALSE}
	 *
	 * @param propertyName Property name
	 * @return boolean {@value #TRUE} or {@value #FALSE}
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigFormatException if propertyName is defined, but not set to a boolean value
	 */
	public static boolean requireBoolean( final String propertyName )
			throws ConfigNotFoundException, ConfigFormatException
	{
		final String val = requireString( propertyName );
		if( val.equalsIgnoreCase( TRUE ) )
		{
			return true;
		}
		if( val.equalsIgnoreCase( FALSE ) )
		{
			return false;
		}

		throw new ConfigFormatException( propertyName,
				"Property only accepts boolean values: " + TRUE + " or " + FALSE );
	}

	/**
	 * Requires valid double value
	 *
	 * @param propertyName Property name
	 * @return Double value
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigFormatException if propertyName is defined, but set with a non-numeric value
	 */
	public static Double requireDoubleVal( final String propertyName )
			throws ConfigNotFoundException, ConfigFormatException
	{
		final Double val = getDoubleVal( propertyName );
		if( val == null )
		{
			throw new ConfigNotFoundException( propertyName );
		}

		return val;
	}

	/**
	 * Requires valid existing directory.
	 *
	 * @param propertyName Property name
	 * @return File directory
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigPathException if propertyName is not a valid directory path
	 */
	public static File requireExistingDir( final String propertyName )
			throws ConfigNotFoundException, ConfigPathException
	{
		final File f = getExistingDir( propertyName );
		if( f == null )
		{
			throw new ConfigNotFoundException( propertyName );
		}

		return f;
	}

	/**
	 * Requires valid list of file directories
	 *
	 * @param propertyName Property name
	 * @return List of File directories
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigPathException if propertyName is not a valid directory path
	 */
	public static List<File> requireExistingDirs( final String propertyName )
			throws ConfigNotFoundException, ConfigPathException
	{
		final List<File> returnDirs = new ArrayList<>();
		for( final String d: requireSet( propertyName ) )
		{
			final File dir = getExistingFileObject( d );
			if( dir != null && !dir.isDirectory() )
			{
				throw new ConfigPathException( propertyName, ConfigPathException.DIRECTORY );
			}
			returnDirs.add( dir );
		}

		return returnDirs;
	}

	/**
	 * Require valid existing file
	 *
	 * @param propertyName Property name
	 * @return File with filename defined by propertyName
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigPathException if propertyName is not a valid file path
	 */
	public static File requireExistingFile( final String propertyName )
			throws ConfigNotFoundException, ConfigPathException
	{
		final File f = getExistingFile( propertyName );
		if( f == null )
		{
			throw new ConfigNotFoundException( propertyName );
		}
		return f;
	}

	/**
	 * Requires valid integer value
	 *
	 * @param propertyName Property name
	 * @return Integer value
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigFormatException if propertyName is not a valid integer
	 */
	public static int requireInteger( final String propertyName ) throws ConfigNotFoundException, ConfigFormatException
	{
		final Integer val = getIntegerProp( propertyName );
		if( val == null )
		{
			throw new ConfigNotFoundException( propertyName );
		}

		return val;
	}

	/**
	 * Require valid list property
	 *
	 * @param propertyName Property name
	 * @return List
	 * @throws ConfigNotFoundException if propertyName is undefined
	 */
	public static List<String> requireList( final String propertyName ) throws ConfigNotFoundException
	{
		final List<String> val = getList( propertyName );
		if( val == null || val.isEmpty() )
		{
			throw new ConfigNotFoundException( propertyName );
		}
		return val;
	}

	/**
	 * Require valid positive double value
	 *
	 * @param propertyName Property name
	 * @return Positive Double
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigFormatException if propertyName is defined, but not set to a positive numeric value
	 */
	public static Double requirePositiveDouble( final String propertyName )
			throws ConfigNotFoundException, ConfigFormatException
	{
		final Double val = requireDoubleVal( propertyName );
		if( val <= 0 )
		{
			throw new ConfigFormatException( propertyName, "Property only accepts positive numeric values" );
		}

		return val;
	}

	/**
	 * Require valid positive integer value
	 *
	 * @param propertyName Property name
	 * @return Positive Integer
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigFormatException if propertyName is defined, but not set to a positive integer value
	 */
	public static int requirePositiveInteger( final String propertyName )
			throws ConfigNotFoundException, ConfigFormatException
	{
		final int val = requireInteger( propertyName );
		if( val <= 0 )
		{
			throw new ConfigFormatException( propertyName, "Property only accepts positive integers" );
		}
		return val;
	}

	/**
	 * Require valid Set value
	 *
	 * @param propertyName Property name
	 * @return Set of values
	 * @throws ConfigNotFoundException if propertyName is undefined
	 */
	public static Set<String> requireSet( final String propertyName ) throws ConfigNotFoundException
	{
		final Set<String> val = getTreeSet( propertyName );
		if( val == null || val.isEmpty() )
		{
			throw new ConfigNotFoundException( propertyName );
		}
		return val;
	}

	/**
	 * Require valid String value
	 *
	 * @param propertyName Property name
	 * @return String value
	 * @throws ConfigNotFoundException if propertyName is undefined
	 */
	public static String requireString( final String propertyName ) throws ConfigNotFoundException
	{
		if( getString( propertyName ) == null )
		{
			throw new ConfigNotFoundException( propertyName );
		}

		return getString( propertyName ).trim();
	}

	/**
	 * Sets a property value in the props cache as a list
	 *
	 * @param propertyName Property name
	 * @param data Collection of data to store using the key = propertyName
	 */
	public static void setConfigProperty( final String propertyName, final Collection<?> data )
	{
		String val = null;
		if( data != null && !data.isEmpty() && data.iterator().next() instanceof File )
		{
			final Collection<String> fileData = new ArrayList<>();
			for( final Object obj: data )
			{
				fileData.add( ( (File) obj ).getAbsolutePath() );
			}
			val = BioLockJUtil.getCollectionAsString( fileData );
		}
		else
		{
			val = BioLockJUtil.getCollectionAsString( data );

		}
		props.setProperty( propertyName, val );
		Log.info( Config.class, "Set Config property [" + propertyName + "] = " + val );
	}

	/**
	 * Sets a property value in the props cache
	 *
	 * @param propertyName Property name
	 * @param propertyValue Value to assign to propertyName
	 */
	public static void setConfigProperty( final String propertyName, final String propertyValue )
	{
		props.setProperty( propertyName, propertyValue );
		Log.info( Config.class, "Set Config property [" + propertyName + "] = " + propertyValue );
	}

	/**
	 * Build File using filePath. Replace "~" with System.getProperty( "user.home" ) if found.
	 *
	 * @param filePath File path
	 * @return File or null
	 * @throws ConfigPathException if path is defined but is not found on the file system
	 */
	protected static File getExistingFileObject( final String filePath ) throws ConfigPathException
	{
		if( filePath != null )
		{
			final File f = new File( getSystemFilePath( filePath ) );
			if( f.exists() )
			{
				return f;
			}
			else
			{
				throw new ConfigPathException( filePath );
			}
		}
		return null;
	}

	/**
	 * Set the {@value biolockj.Config#PROJECT_PIPELINE_NAME} and {@value biolockj.Config#PROJECT_PIPELINE_DIR} Create a
	 * pipeline root directory if the pipeline is new.
	 * 
	 * @return TRUE if a new pipeline directory was created
	 * @throws Exception if errors occur
	 */
	protected static boolean initProjectProps() throws Exception
	{
		boolean isNew = false;
		if( RuntimeParamUtil.doRestart() )
		{
			Config.setConfigProperty( Config.PROJECT_PIPELINE_DIR, RuntimeParamUtil.getRestartDir().getAbsolutePath() );
		}
		else if( RuntimeParamUtil.isDirectMode() )
		{
			Config.setConfigProperty( Config.PROJECT_PIPELINE_DIR,
					RuntimeParamUtil.getDirectPipelineDir().getAbsolutePath() );
		}
		else
		{
			isNew = true;
			Config.setConfigProperty( Config.PROJECT_PIPELINE_DIR,
					BioLockJ.createPipelineDirectory().getAbsolutePath() );
		}

		Config.setConfigProperty( Config.PROJECT_PIPELINE_NAME,
				Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getName() );

		return isNew;
	}

	private static TreeMap<String, String> convertToMap( final Properties bljProps )
	{
		final TreeMap<String, String> map = new TreeMap<>();
		final Iterator<String> it = bljProps.stringPropertyNames().iterator();
		while( it.hasNext() )
		{
			final String key = it.next();
			map.put( key, bljProps.getProperty( key ) );
		}
		return map;
	}

	/**
	 * Parse property value as integer
	 *
	 * @param propertyName Property name
	 * @return integer value or null
	 * @throws ConfigFormatException if propertyName is defined, but does not return an integer
	 */
	private static Integer getIntegerProp( final String propertyName ) throws ConfigFormatException
	{
		if( getString( propertyName ) != null )
		{
			try
			{
				final Integer val = Integer.parseInt( getString( propertyName ) );
				return val;
			}
			catch( final Exception ex )
			{
				throw new ConfigFormatException( propertyName, "Property only accepts integer values" );
			}
		}

		return null;
	}

	private static String getInternalRefProp( String propName )
	{
		final String origPropName = propName;
		String val = "";
		try
		{
			int startIndex = propName.indexOf( "${" );
			int endIndex = propName.indexOf( "}" );

			while( startIndex > -1 && endIndex > -1 )
			{
				val += propName.substring( 0, startIndex );
				final String refProp = propName.substring( startIndex + 2, endIndex );
				final String refVal = props.getProperty( refProp );
				if( refVal == null )
				{
					throw new Exception(
							"Could not find Config.prop references (in ${val} format) of property: " + origPropName );
				}
				propName = propName.substring( endIndex + 1 );
				val += refVal;
				startIndex = propName.indexOf( "${" );
				endIndex = propName.indexOf( "}" );
			}

			if( propName != null )
			{
				val += propName;
			}

			System.out.println( "FINAL NAME: ===> " + propName );

		}
		catch( final Exception ex )
		{
			Log.warn( Config.class, ex.getMessage() );
			return origPropName;
		}

		return val;
	}

	/**
	 * {@link biolockj.Config} String property: {@value #EXE_AWK}<br>
	 * Set command line executable awk.
	 */
	public static final String EXE_AWK = "exe.awk";

	/**
	 * {@link biolockj.Config} String property {@value #EXE_DOCKER}<br>
	 * Set command line executable docker
	 */
	public static final String EXE_DOCKER = "exe.docker";

	/**
	 * {@link biolockj.Config} String property {@value #EXE_GZIP}<br>
	 * Set command line executable gzip
	 */
	public static final String EXE_GZIP = "exe.gzip";

	/**
	 * Boolean {@link biolockj.Config} property value option: {@value #FALSE}
	 */
	public static final String FALSE = "N";

	/**
	 * {@link biolockj.Config} List property: {@value #INPUT_DIRS}<br>
	 * Set sequence file directories
	 */
	public static final String INPUT_DIRS = "input.dirPaths";

	/**
	 * {@link biolockj.Config} List property: {@value #INPUT_IGNORE_FILES}<br>
	 * Set file names to ignore if found in {@value #INPUT_DIRS}
	 */
	public static final String INPUT_IGNORE_FILES = "input.ignoreFiles";

	/**
	 * Internal {@link biolockj.Config} List property: {@value #INTERNAL_ALL_MODULES}<br>
	 * List of all configured, implicit, and pre/post-requisite modules for the pipeline.<br>
	 * Example: biolockj.module.ImportMetadata, etc.
	 */
	public static final String INTERNAL_ALL_MODULES = "internal.allModules";

	/**
	 * Set BioModule tag in {@link biolockj.Config} file to include in pipeline: {@value #INTERNAL_BLJ_MODULE}<br>
	 * Example: #BioModule biolockj.module.ImportMetadata
	 */
	public static final String INTERNAL_BLJ_MODULE = "#BioModule";

	/**
	 * Internal {@link biolockj.Config} List property: {@value #INTERNAL_DEFAULT_CONFIG}<br>
	 * List of all nested default config files.<br>
	 */
	public static final String INTERNAL_DEFAULT_CONFIG = "internal.defaultConfig";

	/**
	 * {@link biolockj.Config} String property: {@value #PROJECT_DEFAULT_PROPS}<br>
	 * Set file path of default property file. Nested default properties are supported (so the default property file can
	 * also have a default, and so on).
	 */
	public static final String PROJECT_DEFAULT_PROPS = "project.defaultProps";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #PROJECT_ENV}<br>
	 * Options: {@value #PROJECT_ENV_CLUSTER}, {@value #PROJECT_ENV_AWS}, {@value #PROJECT_ENV_LOCAL}
	 */
	public static final String PROJECT_ENV = "project.env";

	/**
	 * {@link biolockj.Config} option for property: {@value #PROJECT_ENV}<br>
	 * Used to indicate running as an Amazon web service: {@value #PROJECT_ENV_AWS}
	 */
	public static final String PROJECT_ENV_AWS = "aws";

	/**
	 * {@link biolockj.Config} option for property: {@value #PROJECT_ENV}<br>
	 * Used to indicate running on the cluster: {@value #PROJECT_ENV_CLUSTER}
	 */
	public static final String PROJECT_ENV_CLUSTER = "cluster";

	/**
	 * {@link biolockj.Config} option for property: {@value #PROJECT_ENV}<br>
	 * Used to indicate running on a local machine (laptop, etc): {@value #PROJECT_ENV_LOCAL}
	 */
	public static final String PROJECT_ENV_LOCAL = "local";

	/**
	 * {@link biolockj.Config} String property: {@value #PROJECT_PIPELINE_DIR}<br>
	 * Stores the path of the pipeline root directory path set by the application runtime code.
	 */
	public static final String PROJECT_PIPELINE_DIR = "project.pipelineDir";

	/**
	 * {@link biolockj.Config} String property: {@value #PROJECT_PIPELINE_NAME}<br>
	 * Stores the root name of the pipeline (derived from the configuration file name).
	 */
	public static final String PROJECT_PIPELINE_NAME = "project.pipelineName";

	/**
	 * {@link biolockj.Config} String property: {@value #REPORT_LOG_BASE}<br>
	 * Required to be set to "e" or "10" to build log normalized reports.
	 */
	public static final String REPORT_LOG_BASE = "report.logBase";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #REPORT_NUM_HITS}<br>
	 * If set to {@value #TRUE}, NUM_OTUS will be added to metadata file by
	 * {@link biolockj.module.implicit.parser.ParserModuleImpl} and included in R reports
	 */
	public static final String REPORT_NUM_HITS = "report.numHits";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #REPORT_NUM_READS}<br>
	 * If set to {@value #TRUE} and NUM_READS exists in metadata file, NUM_READS will be included in the R reports
	 */
	public static final String REPORT_NUM_READS = "report.numReads";

	/**
	 * Boolean {@link biolockj.Config} property value option: {@value #TRUE}
	 */
	public static final String TRUE = "Y";
	private static final String BLJ_SUPPORT = "blj_support";
	private static File configFile = null;
	private static final String ORIG_PREFIX = ".ORIG_";
	private static File origConfigFile = null;
	private static Properties props = null;
	private static Properties unmodifiedInputProps = null;

	private static final Map<String, String> usedProps = new HashMap<>();

}
