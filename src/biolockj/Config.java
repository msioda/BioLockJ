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
     * Gets the configuration file name
     *
     * @return Config file name
     */
    public static String getConfigFileName() {
        return configFile.getName();
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
        Log.debug( Config.class, "Found module specific property: [ " + moduleProp + "=" + val + " ]" );
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
     * If $BLJ or $BLJ_SUP or $USER or $HOME was used, it would already be converted to the actual file path by
     * {@link biolockj.Properties} before this method is called.
     *
     * @param module BioModule to check for module-specific form of this property
     * @param property {@link biolockj.Config} file property name
     * @return String value or null
     */
    public static String getString( final BioModule module, final String property ) {
        if( props == null ) return null;
        Object obj = null;
        String val = null;
        String propName = property;
        if( module != null ) {
            final String modPropName = Config.getModuleProp( module, property );
            obj = props.getProperty( modPropName );
            if( obj != null ) {
                propName = modPropName;
            }
        }

        if( obj == null ) {
            obj = props.getProperty( propName );
        }

        if( obj == null ) {
            usedProps.put( propName, null );
            return null;
        }

        val = obj.toString().trim();

        /* Allow internal references to avoid re-typing paths. For example:
         * project.dataDir=/projects/data/internal/research_labs project.experimentID=1987209C
         * project.labUrl=$project.dataDir/fodor_lab/$project.experimentID reportBuilder.massSpecReportHeading=Mass Spec
         * $project.labID */
        if( val.contains( "${" ) && val.contains( "}" ) ) {
            val = getInternalRefProp( val );
        }

        if( val.isEmpty() ) {
            usedProps.put( propName, "" );
            return null;
        }
        usedProps.put( propName, val );

        return val;
    }

    /**
     * Return filePath with system parameter values replaced. If filePath starts with "~" or $HOME it is replaced with
     * System.getProperty( "user.home" ). If filePath contains $USER it is replaced with System.getProperty( "user.name"
     * ). Also $BLJ and $BLJ_SUP params are handled for use in Docker implementations
     * 
     * @param filePath File path
     * @return Formatted filePath
     * @throws Exception if path is invalid or other errors occur
     */
    public static String getSystemFilePath( final String filePath ) throws Exception {
        String filePath2 = filePath;
        if( filePath2 != null && filePath2.startsWith( "~" ) ) {
            filePath2 = replaceEnvVar( filePath2, "~", System.getProperty( "user.home" ) );
        } else if( filePath2 != null && filePath2.startsWith( "$HOME" ) ) {
            filePath2 = replaceEnvVar( filePath2, "$HOME", System.getProperty( "user.home" ) );
        } else if( filePath2 != null && filePath2.startsWith( "$BLJ_META" ) ) {
            String testPath = null;
            if( Config.getString( null, MetaUtil.BLJ_META_PROP ) != null ) {
                testPath = replaceEnvVar( filePath2, "$BLJ_META", Config.getString( null, MetaUtil.BLJ_META_PROP ) );
            }
            if( testPath == null ) {
                filePath2 = replaceEnvVar( filePath2, "$BLJ_META",
                    System.getProperty( "user.home" ) + File.separator + METADATA );
            } else {
                filePath2 = testPath;
            }
        } else if( filePath2 != null && filePath2.startsWith( "$BLJ_SUP" ) ) {
            filePath2 = replaceEnvVar( filePath2, "$BLJ_SUP",
                BioLockJUtil.getBljDir().getParentFile().getAbsolutePath() + File.separator + BLJ_SUPPORT );
        } else if( filePath2 != null && filePath2.startsWith( "$BLJ" ) ) {
            filePath2 = replaceEnvVar( filePath2, "$BLJ", BioLockJUtil.getBljDir().getAbsolutePath() );
        }

        if( filePath2 != null && filePath2.contains( "$USER" ) ) {
            filePath2 = replaceEnvVar( filePath2, "$USER", System.getProperty( "user.name" ) );
        }

        return filePath2;
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
        props = Properties.loadProperties( configFile );
        setPipelineRootDir();
        for( final Object key: props.keySet() ) {
            Log.debug( Config.class, "Project Config: " + key + "=" + props.getProperty( (String) key ) );
        }
        Log.debug( Config.class, "# initial props: " + props.size() );
        unmodifiedInputProps.putAll( props );
        Log.debug( Config.class, "# initial unmodifiedInputProps: " + unmodifiedInputProps.size() );
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
     * @throws Exception if errors occur
     */
    public static List<File> requireExistingDirs( final BioModule module, final String property ) throws Exception {
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
            Log.info( Config.class, "Set Config property [" + name + "] = " + val );
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
            Log.info( Config.class, "Set Config property [" + name + "] = " + val );
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
     * Build File using filePath. Replace "~" with System.getProperty( "user.home" ) if found.
     *
     * @param filePath File path
     * @return File or null
     * @throws Exception if path is defined but is not found on the file system or other errors occur
     */
    protected static File getExistingFileObject( final String filePath ) throws Exception {
        if( filePath != null ) {
            final File f = new File( getSystemFilePath( filePath ) );
            if( f.exists() ) return f;
            throw new ConfigPathException( filePath );
        }
        return null;
    }

    /**
     * Set {@value Constants#INTERNAL_PIPELINE_DIR} Create a pipeline root directory if the pipeline is new.
     * 
     * @throws Exception if any errors occur
     */
    protected static void setPipelineRootDir() throws Exception {
        if( RuntimeParamUtil.doRestart() ) {
            setPipelineDir( RuntimeParamUtil.getRestartDir() );
            Log.info( Config.class, "Assign RESTART pipeline root directory: " + Config.pipelinePath() );
        } else if( RuntimeParamUtil.isDirectMode() ) {
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

    private static String getInternalRefProp( final String propName ) {
        String propName2 = propName;
        final String origPropName = propName2;
        String val = "";
        try {
            int startIndex = propName2.indexOf( "${" );
            int endIndex = propName2.indexOf( "}" );

            while( startIndex > -1 && endIndex > -1 ) {
                val += propName2.substring( 0, startIndex );
                final String refProp = propName2.substring( startIndex + 2, endIndex );
                final String refVal = props.getProperty( refProp );
                if( refVal == null ) throw new Exception(
                    "Could not find Config.prop references (in ${val} format) of property: " + origPropName );
                propName2 = propName2.substring( endIndex + 1 );
                val += refVal;
                startIndex = propName2.indexOf( "${" );
                endIndex = propName2.indexOf( "}" );
            }

            val += propName2;
            System.out.println( "FINAL NAME: ===> " + propName2 );

        } catch( final Exception ex ) {
            Log.warn( Config.class, ex.getMessage() );
            return origPropName;
        }

        return val;
    }

    private static File getPipelineDir() {
        if( pipelineDir == null ) {
            try {
                pipelineDir = requireExistingDir( null, Constants.INTERNAL_PIPELINE_DIR );
            } catch( final Exception ex ) {
                ex.printStackTrace();
            }
        }
        return pipelineDir;
    }

    private static String replaceEnvVar( final String filePath, final String envVar, final String replacementValue ) {
        if( replacementValue != null && !replacementValue.isEmpty() ) {
            final File newFilePath = new File( filePath.replace( envVar, replacementValue ) );
            Log.info( Config.class, "Decode: " + filePath + " --> " + replacementValue );
            if( newFilePath.exists() ) return newFilePath.getAbsolutePath();
        }
        Log.warn( Config.class, "Directory not found! Please verify  bash runtime env var: \"" + envVar + "\"" );
        return null;
    }

    private static String suffix( final String prop ) {
        return prop.indexOf( "." ) > -1 ? prop.substring( prop.indexOf( "." ) + 1 ): prop;
    }

    private static final String BLJ_SUPPORT = "blj_support";
    private static File configFile = null;
    private static final String METADATA = "metadata";
    private static File pipelineDir = null;
    private static Properties props = null;
    private static Properties unmodifiedInputProps = new Properties();
    private static final Map<String, String> usedProps = new HashMap<>();
}
