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
import java.util.*;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;
import biolockj.module.BioModule;
import biolockj.util.*;

/**
 * Provides type-safe, validated methods for storing/accessing system properties.<br>
 * Initially populated by the properties in the Config file, several additional properties are created and stored in the
 * the Config (to save system determined info such as: pipeline directory and name, has paired reads?, has multiplexed
 * reads?, etc.).
 */
public class Config {
	/**
	 * Parse property value (Y or N) to return boolean, if not found, return false;
	 *
	 * @param module Source BioModule calling this function
	 * @param property Property name
	 * @return boolean value
	 * @throws ConfigFormatException if property value is not null but also not Y or N.
	 */
	public static boolean getBoolean( final BioModule module, final String property ) throws ConfigFormatException {
		if( getString( module, property ) != null && getString( module, property ).equalsIgnoreCase( Constants.TRUE ) )
			return true;
		else if( getString( module, property ) == null ) {
			setConfigProperty( property, Constants.FALSE );
			Log.debug( Config.class, property + " is undefined, so return: " + Constants.FALSE );
		} else if( !getString( module, property ).equalsIgnoreCase( Constants.FALSE ) )
			throw new ConfigFormatException( property, "Boolean properties must be set to either " + Constants.TRUE
				+ " or " + Constants.FALSE + ".  Update this property in your Config file to a valid option." );

		return false;
	}

	/**
	 * Gets the configuration file extension (often ".properties")
	 *
	 * @return Config file extension
	 */
	public static String getConfigFileExt() {
		String ext = null;
		final StringTokenizer st = new StringTokenizer( configFile.getName(), "." );
		if( st.countTokens() > 1 ) {
			while( st.hasMoreTokens() ) {
				ext = st.nextToken();
			}
		}

		return "." + ext;
	}

	/**
	 * Gets the full Config file path passed to BioLockJ as a runtime parameter.
	 *
	 * @return Config file path
	 */
	public static String getConfigFilePath() {
		return configFile.getAbsolutePath();
	}

	/**
	 * Parse property for numeric (double) value
	 * 
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Double value or null
	 * @throws ConfigFormatException if property is defined, but set with a non-numeric value
	 */
	public static Double getDoubleVal( final BioModule module, final String property ) throws ConfigFormatException {
		if( getString( module, property ) != null ) {
			try {
				final Double val = Double.parseDouble( getString( module, property ) );
				return val;
			} catch( final Exception ex ) {
				throw new ConfigFormatException( property, "Property only accepts numeric values" );
			}
		}
		return null;
	}

	/**
	 * Get exe.* property name. If null, return the property name (without the "exe." prefix)
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return String value of executable
	 * @throws Exception if property does not start with "exe."
	 */
	public static String getExe( final BioModule module, final String property ) throws Exception {
		if( !property.startsWith( "exe." ) )
			throw new Exception( "Config.getExe() can only be called for properties that begin with \"exe.\"" );

		// return name of property after trimming "exe." prefix, for example if exe.Rscript is undefined, return
		// "Rscript"
		if( getString( module, property ) == null ) return property.substring( property.indexOf( "." ) + 1 );

		return getString( module, property );
	}

	/**
	 * Call this function to get the parameters configured for this property.<br>
	 * Make sure the last character for non-null results is an empty character for use in bash scripts calling the
	 * corresponding executable.
	 * 
	 * @param module Calling module
	 * @param property exe parameter name
	 * @return Executable program parameters
	 * @throws Exception if errors occur
	 */
	public static String getExeParams( final BioModule module, final String property ) throws Exception {
		final String property2 = property;
		if( !property2.startsWith( "exe." ) )
			throw new Exception( "Config.getExeParams() can only be called for properties that begin with \"exe.\"" );
		if( getString( module, property2 + Constants.PARAMS ) == null ) return "";
		String val = getString( module, property2 + Constants.PARAMS );
		if( val != null && !val.isEmpty() && !val.endsWith( " " ) ) {
			val = val + " ";
		}
		return val;
	}

	/**
	 * Get a valid File directory or return null
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return File directory or null
	 * @throws Exception if path is defined but is not an existing directory or other errors occur
	 */
	public static File getExistingDir( final BioModule module, final String property ) throws Exception {
		final File f = getExistingFileObject( getString( module, property ) );
		if( f != null && !f.isDirectory() ) throw new ConfigPathException( property, ConfigPathException.DIRECTORY );

		if( props != null && f != null ) {
			Config.setConfigProperty( property, f.getAbsolutePath() );
		}

		return f;
	}

	/**
	 * Get a valid File or return null. If path is a directory containing exactly 1 file, return that file.
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return File (not directory) or null
	 * @throws Exception if path is defined but is not an existing file or if other errors occur
	 */
	public static File getExistingFile( final BioModule module, final String property ) throws Exception {
		File f = getExistingFileObject( getString( module, property ) );
		if( f != null && !f.isFile() ) {
			if( f.isDirectory() && f.list( HiddenFileFilter.VISIBLE ).length == 1 ) {
				Log.warn( Config.class,
					property + " is a directory with only 1 valid file.  Return the lone file within." );
				f = new File( f.list( HiddenFileFilter.VISIBLE )[ 0 ] );
			} else throw new ConfigPathException( property, ConfigPathException.FILE );
		}

		if( props != null && f != null ) {
			Config.setConfigProperty( property, f.getAbsolutePath() );
		}

		return f;
	}

	/**
	 * Get initial properties ordered by property
	 *
	 * @return map ordered by property
	 */
	public static TreeMap<String, String> getInitialProperties() {
		return convertToMap( unmodifiedInputProps );
	}

	/**
	 * Parse comma delimited property value to return list
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return List of String values (or an empty list)
	 */
	public static List<String> getList( final BioModule module, final String property ) {
		final List<String> list = new ArrayList<>();
		final String val = getString( module, property );
		if( val != null ) {
			final StringTokenizer st = new StringTokenizer( val, "," );
			while( st.hasMoreTokens() ) {
				list.add( st.nextToken().trim() );
			}
		}

		return list;
	}

	/**
	 * Return file for path after modifying if running in a Docker container and/or interpreting bash env vars.
	 * 
	 * @param path File path
	 * @return Local File
	 * @throws ConfigPathException if the local path
	 */
	public static File getLocalConfigFile( final String path ) throws ConfigPathException {
		if( path == null || path.trim().isEmpty() ) return null;
		final File file = new File( replaceEnvVar( path.trim() ) );
		if( DockerUtil.inDockerEnv() && !file.isFile() ) return DockerUtil.getConfigFile( path.trim() );
		return file;
	}

	/**
	 * Return module specific property if configured, otherwise use the given prop.
	 * 
	 * @param module BioModule
	 * @param prop Property
	 * @return Config property
	 */
	public static String getModuleProp( final BioModule module, final String prop ) {
		return getModuleProp( module.getClass().getSimpleName(), prop );
	}

	/**
	 * Return module specific property if configured, otherwise use the given prop.
	 * 
	 * @param moduleName BioModule name
	 * @param prop Property
	 * @return property name
	 */
	public static String getModuleProp( final String moduleName, final String prop ) {
		final String moduleProp = moduleName + "." + suffix( prop );
		final String val = Config.getString( null, moduleProp );
		if( val == null || val.isEmpty() ) return prop;
		Log.debug( Config.class, "Use module specific property: [ " + moduleProp + "=" + val + " ]" );
		return moduleProp;
	}

	/**
	 * Parse property as non-negative integer value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Non-negative integer or null
	 * @throws ConfigFormatException if defined but is not a non-negative integer value
	 */
	public static Integer getNonNegativeInteger( final BioModule module, final String property )
		throws ConfigFormatException {
		final Integer val = getIntegerProp( module, property );
		if( val != null && val < 0 )
			throw new ConfigFormatException( property, "Property only accepts non-negative integer values" );
		return val;
	}

	/**
	 * Get the pipeline directory if it is a valid directory on the file system.
	 * 
	 * @return Pipeline directory (if it exists)
	 */
	public static File getPipelineDir() {
		if( pipelineDir == null && props != null && props.getProperty( Constants.INTERNAL_PIPELINE_DIR ) != null ) {
			try {
				pipelineDir = requireExistingDir( null, Constants.INTERNAL_PIPELINE_DIR );
			} catch( final Exception ex ) {
				Log.error( Config.class, "Pipeline directory does not exist", ex );
			}
		}
		return pipelineDir;
	}

	/**
	 * Parse property as positive double value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Positive Double value or null
	 * @throws ConfigFormatException if property is defined, but not set with a positive number
	 */
	public static Double getPositiveDoubleVal( final BioModule module, final String property )
		throws ConfigFormatException {
		final Double val = getDoubleVal( module, property );
		if( val != null && val <= 0 )
			throw new ConfigFormatException( property, "Property only accepts positive numeric values" );

		return val;
	}

	/**
	 * Parse property as positive integer value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Positive Integer value or null
	 * @throws ConfigFormatException if property is defined, but not set with a positive integer
	 */
	public static Integer getPositiveInteger( final BioModule module, final String property )
		throws ConfigFormatException {
		final Integer val = getIntegerProp( module, property );
		if( val != null && val <= 0 )
			throw new ConfigFormatException( property, "Property only accepts positive integer values" );
		return val;
	}

	/**
	 * Get current properties ordered by property
	 *
	 * @return map ordered by property
	 */
	public static TreeMap<String, String> getProperties() {
		return convertToMap( props );
	}

	/**
	 * Parse comma-separated property value to build an unordered Set
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Set of values or an empty set (if no values)
	 */
	public static Set<String> getSet( final BioModule module, final String property ) {
		final Set<String> set = new HashSet<>();
		set.addAll( getList( module, property ) );
		return set;
	}

	/**
	 * Get property value as String. Empty strings return null.<br>
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property {@link biolockj.Config} file property name
	 * @return String or null
	 */
	public static String getString( final BioModule module, final String property ) {
		if( props == null ) return null;
		String propName = property;

		if( module != null ) {
			final String modPropName = Config.getModuleProp( module, propName );
			if( props.getProperty( modPropName ) != null ) {
				propName = modPropName;
			}
		}

		final String val = props.getProperty( propName );
		usedProps.put( propName, val );
		if( val == null || val.isEmpty() ) return null;
		return val;
	}

	/**
	 * Parse comma-separated property value to build an ordered Set
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Set of values or an empty set (if no values)
	 */
	public static Set<String> getTreeSet( final BioModule module, final String property ) {
		final Set<String> set = new TreeSet<>();
		set.addAll( getList( module, property ) );
		return set;
	}

	/**
	 * Cache of the properties used in this pipeline.
	 * 
	 * @return list of properties
	 */
	public static Map<String, String> getUsedProps() {
		getString( null, Constants.PIPELINE_DEFAULT_PROPS );
		return new HashMap<>( usedProps );
	}

	/**
	 * Initialize {@link biolockj.Config} by reading in properties from config runtime parameter.
	 * 
	 * @throws Exception if unable to load Props
	 */
	public static void initialize() throws Exception {
		configFile = RuntimeParamUtil.getConfigFile();
		Log.info( Config.class, "Initialize Config: " + configFile.getAbsolutePath() );
		props = replaceEnvVars( Properties.loadProperties( configFile ) );
		setPipelineRootDir();
		Log.info( Config.class, "Total # initial properties: " + props.size() );
		unmodifiedInputProps.putAll( props );
		TaxaUtil.initTaxaLevels();
	}

	/**
	 * Check if running on cluster
	 * 
	 * @return TRUE if running on the cluster
	 */
	public static boolean isOnCluster() {
		return getString( null, Constants.PIPELINE_ENV ) != null
			&& getString( null, Constants.PIPELINE_ENV ).equals( Constants.PIPELINE_ENV_CLUSTER );
	}

	/**
	 * Get the current pipeline name (root folder name)
	 * 
	 * @return Pipeline name
	 */
	public static String pipelineName() {
		if( getPipelineDir() == null ) return null;
		return getPipelineDir().getName();
	}

	/**
	 * Get the current pipeline absolute directory path (root folder path)
	 * 
	 * @return Pipeline directory path
	 */
	public static String pipelinePath() {
		return getPipelineDir().getAbsolutePath();
	}

	/**
	 * Interpret env variable if included in the arg string, otherwise return the arg.
	 * 
	 * @param arg Property or runtime argument
	 * @return Updated arg value after replacing env variables
	 */
	public static String replaceEnvVar( final String arg ) {
		if( arg == null ) return null;
		String val = arg.toString().trim();
		try {
			if( !hasEnvVar( val ) ) return val;
			if( val.substring( 0, 1 ).equals( "~" ) ) {
				Log.debug( Config.class, "Found property value starting with \"~\" --> \"" + arg + "\"" );
				val = val.replace( "~", "${HOME}" );
				Log.debug( Config.class, "Converted value to use standard syntax --> " + val + "\"" );
			}

			while( hasEnvVar( val ) ) {
				final String bashVar = val.substring( val.indexOf( "${" ), val.indexOf( "}" ) + 1 );
				Log.debug( Config.class, "Replace \"" + bashVar + "\" in \"" + arg + "\"" );
				final String bashVal = getBashVal( bashVar );
				Log.debug( Config.class, "Bash var \"" + bashVar + "\" = \"" + bashVal + "\"" );
				if( bashVal != null && bashVal.equals( bashVar ) ) return arg;
				val = val.replace( bashVar, bashVal );
				Log.debug( Config.class, "Updated \"" + arg + "\" --> " + val + "\"" );
			}
			Log.info( Config.class, "--------> Bash Var Converted \"" + arg + "\" ======> \"" + val + "\"" );
			return val;
		} catch( final Exception ex ) {
			Log.warn( Config.class, "Failed to convert arg \"" + arg + "\"" + ex.getMessage() );
		}
		Log.warn( Config.class, "Return unchanged value \"" + arg + "\"" );
		return arg;
	}

	/**
	 * Required to return a valid boolean {@value Constants#TRUE} or {@value Constants#FALSE}
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return boolean {@value Constants#TRUE} or {@value Constants#FALSE}
	 * @throws ConfigNotFoundException if propertyName is undefined
	 * @throws ConfigFormatException if property is defined, but not set to a boolean value
	 */
	public static boolean requireBoolean( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		final String val = requireString( module, property );
		if( val.equalsIgnoreCase( Constants.TRUE ) ) return true;
		if( val.equalsIgnoreCase( Constants.FALSE ) ) return false;

		throw new ConfigFormatException( property,
			"Property only accepts boolean values: " + Constants.TRUE + " or " + Constants.FALSE );
	}

	/**
	 * Requires valid double value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Double value
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws ConfigFormatException if property is defined, but set with a non-numeric value
	 */
	public static Double requireDoubleVal( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		final Double val = getDoubleVal( module, property );
		if( val == null ) throw new ConfigNotFoundException( property );

		return val;
	}

	/**
	 * Requires valid existing directory.
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return File directory
	 * @throws Exception if property is undefined or if property is not a valid directory path
	 */
	public static File requireExistingDir( final BioModule module, final String property ) throws Exception {
		final File f = getExistingDir( module, property );
		if( f == null ) throw new ConfigNotFoundException( property );

		return f;
	}

	/**
	 * Requires valid list of file directories
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return List of File directories
	 * @throws ConfigPathException if directory paths are undefined or do not exist
	 * @throws ConfigNotFoundException if a required property is undefined
	 */
	public static List<File> requireExistingDirs( final BioModule module, final String property )
		throws ConfigPathException, ConfigNotFoundException {
		final List<File> returnDirs = new ArrayList<>();
		for( final String d: requireSet( module, property ) ) {
			final File dir = getExistingFileObject( d );
			if( dir != null && !dir.isDirectory() )
				throw new ConfigPathException( property, ConfigPathException.DIRECTORY );

			returnDirs.add( dir );
		}

		if( !returnDirs.isEmpty() ) {
			Config.setConfigProperty( property, returnDirs );
		}
		return returnDirs;
	}

	/**
	 * Require valid existing file
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return File with filename defined by property
	 * @throws Exception if property is undefined or if property is not a valid file path
	 */
	public static File requireExistingFile( final BioModule module, final String property ) throws Exception {
		final File f = getExistingFile( module, property );
		if( f == null ) throw new ConfigNotFoundException( property );
		return f;
	}

	/**
	 * Requires valid integer value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Integer value
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws ConfigFormatException if property is not a valid integer
	 */
	public static Integer requireInteger( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		final Integer val = getIntegerProp( module, property );
		if( val == null ) throw new ConfigNotFoundException( property );

		return val;
	}

	/**
	 * Require valid list property
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return List
	 * @throws ConfigNotFoundException if property is undefined
	 */
	public static List<String> requireList( final BioModule module, final String property )
		throws ConfigNotFoundException {
		final List<String> val = getList( module, property );
		if( val == null || val.isEmpty() ) throw new ConfigNotFoundException( property );
		return val;
	}

	/**
	 * Require valid positive double value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Positive Double
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws ConfigFormatException if property is defined, but not set to a positive numeric value
	 */
	public static Double requirePositiveDouble( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		final Double val = requireDoubleVal( module, property );
		if( val <= 0 ) throw new ConfigFormatException( property, "Property only accepts positive numeric values" );

		return val;
	}

	/**
	 * Require valid positive integer value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Positive Integer
	 * @throws ConfigNotFoundException if property is undefined
	 * @throws ConfigFormatException if property is defined, but not set to a positive integer value
	 */
	public static Integer requirePositiveInteger( final BioModule module, final String property )
		throws ConfigNotFoundException, ConfigFormatException {
		final Integer val = requireInteger( module, property );
		if( val <= 0 ) throw new ConfigFormatException( property, "Property only accepts positive integers" );
		return val;
	}

	/**
	 * Require valid Set value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return Set of values
	 * @throws ConfigNotFoundException if property is undefined
	 */
	public static Set<String> requireSet( final BioModule module, final String property )
		throws ConfigNotFoundException {
		final Set<String> val = getTreeSet( module, property );
		if( val == null || val.isEmpty() ) throw new ConfigNotFoundException( property );
		return val;
	}

	/**
	 * Require valid String value
	 *
	 * @param module BioModule to check for module-specific form of this property
	 * @param property Property name
	 * @return String value
	 * @throws ConfigNotFoundException if property is undefined
	 */
	public static String requireString( final BioModule module, final String property ) throws ConfigNotFoundException {
		if( getString( module, property ) == null ) throw new ConfigNotFoundException( property );

		return getString( module, property ).trim();
	}

	/**
	 * Sets a property value in the props cache as a list
	 *
	 * @param name Property name
	 * @param data Collection of data to store using the key = property
	 */
	public static void setConfigProperty( final String name, final Collection<?> data ) {
		String origProp = usedProps.get( name );
		origProp = origProp != null && origProp.isEmpty() ? null: origProp;

		String val = null;
		if( data != null && !data.isEmpty() && data.iterator().next() instanceof File ) {
			final Collection<String> fileData = new ArrayList<>();
			for( final Object obj: data ) {
				fileData.add( ( (File) obj ).getAbsolutePath() );
			}
			val = BioLockJUtil.getCollectionAsString( fileData );
		} else {
			val = BioLockJUtil.getCollectionAsString( data );

		}

		props.setProperty( name, val );

		final boolean hasVal = val != null && !val.isEmpty();
		if( origProp == null && hasVal || origProp != null && !hasVal
			|| origProp != null && hasVal && !origProp.equals( val ) ) {
			Log.info( Config.class, "Set Config property [ " + name + " ] = " + val );
			usedProps.put( name, val );
		}
	}

	/**
	 * Sets a property value in the props cache
	 *
	 * @param name Property name
	 * @param val Value to assign to property
	 */
	public static void setConfigProperty( final String name, final String val ) {
		String origProp = usedProps.get( name );
		origProp = origProp != null && origProp.isEmpty() ? null: origProp;
		props.setProperty( name, val );
		final boolean hasVal = val != null && !val.isEmpty();
		if( origProp == null && hasVal || origProp != null && !hasVal
			|| origProp != null && hasVal && !origProp.equals( val ) ) {
			Log.info( Config.class, "Set Config property [ " + name + " ] = " + val );
			usedProps.put( name, val );
		}
	}

	/**
	 * Set the root pipeline directory path
	 * 
	 * @param dir Pipeline directory path
	 */
	public static void setPipelineDir( final File dir ) {
		setConfigProperty( Constants.INTERNAL_PIPELINE_DIR, dir.getAbsolutePath() );
		pipelineDir = dir;
	}

	/**
	 * Build File using filePath.
	 *
	 * @param filePath File path
	 * @return File or null
	 * @throws ConfigPathException if path is defined but is not found on the file system
	 */
	protected static File getExistingFileObject( final String filePath ) throws ConfigPathException {
		if( filePath != null ) {
			final File f = new File( filePath );
			if( f.exists() ) return f;
			throw new ConfigPathException( f );
		}
		return null;
	}

	/**
	 * Interpret env variables defined in the Config file and runtime env - for example<br>
	 * These props are used in: $BLJ/resources/config/defult/docker.properties:<br>
	 * <ul>
	 * <li>BLJ_ROOT=/mnt/efs
	 * <li>EFS_DB=${BLJ_ROOT}/db
	 * <li>humann2.protDB=${EFS_DB}/uniref
	 * </ul>
	 * Therefore, getString( "humann2.protDB" ) returns "/mnt/efs/db/uniref"<br>
	 * If not found, check runtiem env (i.e., $HOME/bash_profile)
	 * 
	 * @param properties All Config Properties
	 * @return Properties after replacing env variables
	 */
	protected static Properties replaceEnvVars( final Properties properties ) {
		final Properties convertedProps = properties;
		final Enumeration<?> en = properties.propertyNames();
		Log.debug( Properties.class, " ---------------------- replace Config Env Vars ----------------------" );
		while( en.hasMoreElements() ) {
			final String key = en.nextElement().toString();
			String val = properties.getProperty( key );
			val = replaceEnvVar( val );
			Log.debug( Properties.class, key + " = " + val );
			convertedProps.put( key, val );
		}
		Log.debug( Properties.class, " --------------------------------------------------------------------" );
		return convertedProps;
	}

	/**
	 * Set {@value Constants#INTERNAL_PIPELINE_DIR} Create a pipeline root directory if the pipeline is new.
	 * 
	 * @throws Exception if any errors occur
	 */
	protected static void setPipelineRootDir() throws Exception {
		if( RuntimeParamUtil.doRestart() ) {
			setPipelineDir( RuntimeParamUtil.getRestartDir() );
			Log.info( Config.class, "Assign RESTART_DIR pipeline root directory: " + Config.pipelinePath() );
		} else if( DockerUtil.isDirectMode() ) {
			setPipelineDir( RuntimeParamUtil.getDirectPipelineDir() );
			Log.info( Config.class, "Assign DIRECT pipeline root directory: " + Config.pipelinePath() );
		} else {
			setPipelineDir( BioLockJ.createPipelineDirectory() );
			Log.info( Config.class, "Assign NEW pipeline root directory: " + Config.pipelinePath() );
		}

		if( !Config.getPipelineDir().isDirectory() )
			throw new ConfigPathException( Constants.INTERNAL_PIPELINE_DIR, ConfigPathException.DIRECTORY );
	}

	private static TreeMap<String, String> convertToMap( final Properties bljProps ) {
		final TreeMap<String, String> map = new TreeMap<>();
		final Iterator<String> it = bljProps.stringPropertyNames().iterator();
		while( it.hasNext() ) {
			final String key = it.next();
			map.put( key, bljProps.getProperty( key ) );
		}
		return map;
	}

	private static String getBashVal( final String bashVar ) {
		try {
			if( bashVarMap.get( bashVar ) != null ) return bashVarMap.get( bashVar );
			String bashVal = props == null ? null: props.getProperty( stripBashMarkUp( bashVar ) );
			if( bashVal == null || bashVal.trim().isEmpty() ) {
				if( bashVar.equals( BLJ_BASH_VAR ) ) {
					final File blj = BioLockJUtil.getBljDir();
					if( blj != null && blj.isDirectory() ) {
						bashVal = blj.getAbsolutePath();
					}
				} else if( bashVar.equals( BLJ_SUP_BASH_VAR ) ) {
					final File bljSup = BioLockJUtil.getBljSupDir();
					if( bljSup != null && bljSup.isDirectory() ) {
						bashVal = bljSup.getAbsolutePath();
					}
				} else {
					bashVal = Processor.getBashVar( bashVar );
				}
			}

			if( bashVal != null && !bashVal.trim().isEmpty() ) {
				bashVarMap.put( bashVar, bashVal );
				return bashVal;
			}

		} catch( final Exception ex ) {
			Log.warn( Config.class, "Error occurred attempting to decode bash var: " + bashVar );
		}

		return bashVar;
	}

	/**
	 * Parse property value as integer
	 *
	 * @param property Property name
	 * @return integer value or null
	 * @throws ConfigFormatException if property is defined, but does not return an integer
	 */
	private static Integer getIntegerProp( final BioModule module, final String property )
		throws ConfigFormatException {
		if( getString( module, property ) != null ) {
			try {
				final Integer val = Integer.parseInt( getString( module, property ) );
				return val;
			} catch( final Exception ex ) {
				throw new ConfigFormatException( property, "Property only accepts integer values" );
			}
		}

		return null;
	}

	private static boolean hasEnvVar( final String val ) {
		return val.startsWith( "~" )
			|| val.contains( "${" ) && val.contains( "}" ) && val.indexOf( "${" ) < val.indexOf( "}" );
	}

	private static String stripBashMarkUp( final String bashVar ) {
		if( bashVar != null && bashVar.length() > 3 ) return bashVar.substring( 2, bashVar.length() - 1 );
		return bashVar;
	}

	private static String suffix( final String prop ) {
		return prop.indexOf( "." ) > -1 ? prop.substring( prop.indexOf( "." ) + 1 ): prop;
	}

	/**
	 * Bash variable with path to BioLockJ directory: {@value #BLJ_BASH_VAR}
	 */
	public static final String BLJ_BASH_VAR = "${BLJ}";

	/**
	 * Bash variable with path to blj_support directory: {@value #BLJ_SUP_BASH_VAR}
	 */
	public static final String BLJ_SUP_BASH_VAR = "${BLJ_SUP}";

	private static final Map<String, String> bashVarMap = new HashMap<>();
	private static File configFile = null;
	private static File pipelineDir = null;
	private static Properties props = null;
	private static Properties unmodifiedInputProps = new Properties();
	private static final Map<String, String> usedProps = new HashMap<>();
}
