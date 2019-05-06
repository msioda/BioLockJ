/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jul 31, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.exception.RuntimeParamException;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;

/**
 * This utility processes the application runtime parameters passed to BioLockJ.
 */
public class RuntimeParamUtil {

	/**
	 * Return TRUE if runtime parameters indicate change password request
	 * 
	 * @return boolean
	 */
	public static boolean doChangePassword() {
		return params.get( PASSWORD ) != null;
	}

	/**
	 * Return TRUE if runtime parameters indicate attempt to restart pipeline
	 * 
	 * @return boolean
	 */
	public static boolean doRestart() {
		return getRestartDir() != null;
	}

	/**
	 * Runtime property getter for {@value #PASSWORD}
	 * 
	 * @return New clear-text password
	 */
	public static String getAdminEmailPassword() {
		return params.get( PASSWORD );
	}

	/**
	 * Runtime property getter for {@value #BLJ_PROJ_DIR}
	 * 
	 * @return $BLJ_PROJ_DIR pipeline parent directory
	 */
	public static File getBaseDir() {
		return new File( params.get( BLJ_PROJ_DIR ) );
	}

	/**
	 * Get the baseDir param and value
	 * 
	 * @return String baseDir param and value
	 */
	public static String getBaseDirParam() {
		return BLJ_PROJ_DIR + " " + getBaseDir().getAbsolutePath();
	}

	/**
	 * Runtime property getter for {@value #CONFIG_FILE}
	 * 
	 * @return {@link biolockj.Config} file
	 */
	public static File getConfigFile() {
		return new File( params.get( CONFIG_FILE ) );
	}

	/**
	 * Used to build Docker run commands.
	 * 
	 * @return {@link biolockj.Config} file param and value.
	 */
	public static String getConfigFileParam() {
		return CONFIG_FILE + " " + getConfigFile().getAbsolutePath();
	}

	/**
	 * Runtime property getter for {@value #DIRECT_MODE}
	 * 
	 * @return Direct module BioModule class name
	 */
	public static String getDirectModuleDir() {
		return params.get( DIRECT_MODE );
	}

	/**
	 * Direct module parameters contain 2 parts separated by a colon: (pipeline directory name):(module name)
	 * 
	 * @param module BioModule
	 * @return Direct parameter flag + value
	 */
	public static String getDirectModuleParam( final BioModule module ) {
		return DIRECT_MODE + " " + Config.pipelineName() + ":" + module.getModuleDir().getName();
	}

	/**
	 * Runtime property getter for direct module pipeline directory
	 * 
	 * @return Pipeline directory dir
	 */
	public static File getDirectPipelineDir() {
		return params.get( DIRECT_PIPELINE_DIR ) == null ? null: new File( params.get( DIRECT_PIPELINE_DIR ) );
	}

	/**
	 * Runtime property getter for Docker host BioLockJ dir
	 * 
	 * @return Host {@value #HOST_BLJ_DIR} directory
	 */
	public static File getDockerHostBLJ() {
		return params.get( HOST_BLJ_DIR ) == null ? null: new File( params.get( HOST_BLJ_DIR ) );
	}

	/**
	 * Runtime property getter for Docker host blj_support dir
	 * 
	 * @return Host {@value #HOST_BLJ_SUP_DIR} directory
	 */
	public static File getDockerHostBLJ_SUP() {
		return params.get( HOST_BLJ_SUP_DIR ) == null ? null: new File( params.get( HOST_BLJ_SUP_DIR ) );
	}

	/**
	 * Runtime property getter for Docker host config dir
	 * 
	 * @return Host directory mapped to config volume
	 */
	public static String getDockerHostConfigDir() {
		return params.get( HOST_CONFIG_DIR );
	}

	/**
	 * Runtime property getter for Docker host $USER $HOME path
	 * 
	 * @return Host {@value #HOST_HOME_DIR} directory path
	 */
	public static String getDockerHostHomeDir() {
		return params.get( HOST_HOME_DIR );
	}

	/**
	 * Runtime property getter for {@value #INPUT_DIR}
	 * 
	 * @return Host {@value biolockj.Constants#INPUT_DIRS} directory
	 */
	public static String getDockerHostInputDir() {
		return params.get( INPUT_DIR );
	}

	/**
	 * Runtime property getter for {@value #META_DIR}
	 * 
	 * @return Metadata file directory path
	 */
	public static String getDockerHostMetaDir() {
		return params.get( META_DIR );
	}

	/**
	 * Runtime property getter for Docker host pipeline dir
	 * 
	 * @return Host {@value biolockj.Constants#INTERNAL_PIPELINE_DIR} directory
	 */
	public static String getDockerHostPipelineDir() {
		return params.get( HOST_BLJ_PROJ_DIR );
	}

	/**
	 * Runtime property getter for Docker host $USER $HOME dir
	 * 
	 * @return Host {@value #HOST_HOME_DIR} directory
	 */
	public static File getHomeDir() {
		return new File( params.get( HOME_DIR ) );
	}

	/**
	 * Get Docker runtime arguments passed to BioLockJ from dockblj script.<br>
	 * These are used to to populate BLJ_OPTIONS in Docker java_module scripts.
	 * 
	 * @param module JavaModule BioModule subclass
	 * @return Docker runtime parameters
	 */
	public static String getJavaModuleParams( final JavaModule module ) {
		Log.info( RuntimeParamUtil.class, "Building Docker BLJ_OPTIONS for java_module Docker script -->" );
		String javaModArgs = "";
		for( final String key: params.keySet() ) {
			Log.debug( RuntimeParamUtil.class, "Found Docker param: " + key + "=" + params.get( key ) );
			if( BLJ_CONTROLLER_ONLY_ARGS.contains( key ) ) {
				continue;
			}
			String val = null;
			if( key.equals( HOST_CONFIG_DIR ) ) {
				val = CONFIG_FILE + " " + params.get( key ) + getConfigFile().getName();
			} else if( key.equals( HOST_HOME_DIR ) ) {
				val = HOME_DIR + " " + params.get( key );
			} else if( key.equals( HOST_BLJ_PROJ_DIR ) ) {
				val = BLJ_PROJ_DIR + " " + params.get( key );
			} else if( ARG_FLAGS.contains( key ) ) {
				val = key;
			} else {
				val = key + " " + params.get( key );
			}

			javaModArgs += ( javaModArgs.isEmpty() ? "": " " ) + val;
			Log.info( RuntimeParamUtil.class, "Add Docker param: " + val );
		}

		return javaModArgs + " " + RuntimeParamUtil.getDirectModuleParam( module );
	}

	/**
	 * Extract the project name from the Config file.
	 * 
	 * @return Project name
	 */
	public static String getProjectName() {
		final String configName = getConfigFile().getName();
		String name = configName;
		final String[] exts = { ".ascii", ".asc", ".plain", ".rft", ".tab", ".text", ".tsv", ".txt",
			Constants.PROPS_EXT, ".prop", ".props", ".config" };

		for( final String ext: exts ) {
			if( name.toLowerCase().endsWith( ext ) ) {
				name = name.substring( 0, name.length() - ext.length() );
			}
		}

		final int i = name.indexOf( "." );
		if( name.equals( configName ) && i > -1 && name.length() > i + 1 ) {
			name = name.substring( i + 1 );
		}

		if( name.length() > Constants.MASTER_PREFIX.length() && name.startsWith( Constants.MASTER_PREFIX ) ) {
			name = name.replace( Constants.MASTER_PREFIX, "" );
		}

		return name;
	}

	/**
	 * Return restart pipeline directory
	 * 
	 * @return File directory path
	 */
	public static File getRestartDir() {
		return params.get( RESTART_DIR ) == null ? null: new File( params.get( RESTART_DIR ) );
	}

	/**
	 * Return the runtime args as a String.
	 * 
	 * @return Runtime arg String value
	 */
	public static String getRuntimeArgs() {
		return runtimeArgs;
	}

	/**
	 * Return TRUE if runtime parameter {@value #AWS_FLAG} was found
	 * 
	 * @return boolean
	 */
	public static boolean isAwsMode() {
		return params.get( AWS_FLAG ) != null;
	}

	/**
	 * Return TRUE if runtime parameters indicate Logs should be written to system.out
	 * 
	 * @return boolean
	 */
	public static boolean isDebugMode() {
		return params.get( SYSTEM_OUT_FLAG ) != null;
	}

	/**
	 * Return TRUE if /.dockerenv file exists.
	 * 
	 * @return boolean
	 */
	public static boolean isDockerMode() {
		try {
			return new File( "/.dockerenv" ).isFile();
		} catch( final Exception ex ) {
			Log.warn( RuntimeParamUtil.class, "Error occured checking file-system root directory for \"/.dockerenv\"" );
			ex.printStackTrace();
		}
		Log.info( RuntimeParamUtil.class, "Detected NOTE-IN-DOCKER mode because \"/.dockerenv\" file not found" );
		return false;
	}

	/**
	 * Register and verify the runtime parameters. There are 2 required parameters:<br>
	 * <ol>
	 * <li>The {@link biolockj.Config} file path
	 * <li>The pipeline parent directory ($DOCKER_PROJ)
	 * </ol>
	 *
	 * @param args Program runtime parameters
	 * @throws RuntimeParamException if invalid parameters found
	 */
	public static void registerRuntimeParameters( final String[] args ) throws RuntimeParamException {
		printRuntimeArgs( args, true );

		parseParams( simplifyArgs( args ) );

		if( isDockerMode() ) {
			reassignDockerConfig();
		}

		verifyBaseDir();

		if( getDirectModuleDir() != null ) {
			assignMasterConfig( DIRECT_MODE, assignDirectPipelineDir() );
		} else if( doRestart() && getConfigFile() == null ) {
			assignMasterConfig( RESTART_DIR, getRestartDir() );
		} else {
			try {
				final File localConfig = Config.getLocalConfigFile( params.get( CONFIG_FILE ) );
				params.put( CONFIG_FILE, localConfig.getAbsolutePath() );
			} catch( final Exception ex ) {
				throw new RuntimeParamException( CONFIG_FILE, params.get( CONFIG_FILE ),
					"Failed to get local Confg file path: " + ex.getMessage() );
			}
		}

		validateParams();
	}

	/**
	 * Print the runtime args to System.out or using the Logger based on useSysOut parameter.
	 * 
	 * @param args Runtime Args
	 * @param useSysOut Set TRUE if should use System.out to print
	 */
	protected static void printRuntimeArgs( final String[] args, final boolean useSysOut ) {
		int numArgs = 0;
		Log.info( RuntimeParamUtil.class, RETURN + Constants.LOG_SPACER );
		if( args != null && args.length > 0 ) {
			Log.info( RuntimeParamUtil.class, "Application runtime args:" );
			for( final String arg: args ) {
				runtimeArgs += ( runtimeArgs.isEmpty() ? "": " " ) + arg;
				Log.info( RuntimeParamUtil.class, " ---> arg[" + numArgs++ + "] = " + arg );
			}
		} else {
			Log.info( RuntimeParamUtil.class, "No Java runtime args found!" );
		}
		Log.info( RuntimeParamUtil.class, Constants.LOG_SPACER );
	}

	private static File assignDirectPipelineDir() throws RuntimeParamException {
		Log.info( RuntimeParamUtil.class,
			"Separating pipeline dir name and module name from: \"" + DIRECT_MODE + "\" " + getDirectModuleDir() );
		final StringTokenizer st = new StringTokenizer( getDirectModuleDir(), ":" );
		if( st.countTokens() != 2 ) throw new RuntimeParamException( DIRECT_MODE, getDirectModuleDir(),
			"Required parameter format = $PIPELINE_DIR_NAME:$MODULE_DIR_NAME (with a single colon \":\" - but "
				+ st.countTokens() + " instances of \":\" were found" );

		final String pipelineName = st.nextToken();
		final File pipelineDir = new File( getBaseDir().getAbsolutePath() + File.separator + pipelineName );
		if( !pipelineDir.isDirectory() ) throw new RuntimeParamException( DIRECT_MODE, getDirectModuleDir(),
			"Direct module pipeline directory not found: " + pipelineDir.getAbsolutePath() );

		params.put( DIRECT_PIPELINE_DIR, pipelineDir.getAbsolutePath() );
		params.put( DIRECT_MODE, st.nextToken() );
		return getDirectPipelineDir();
	}

	private static void assignLastParam( final String param ) throws RuntimeParamException {
		if( !params.keySet().contains( CONFIG_FILE )
			&& !params.keySet().contains( RESTART_DIR ) & !params.keySet().contains( param )
			&& !params.values().contains( param ) ) {
			params.put( CONFIG_FILE, param );
		} else if( NAMED_ARGS.contains( param ) )
			throw new RuntimeParamException( param, "", "Missing argument for named parameter" );
	}

	private static void assignMasterConfig( final String param, final File pipelineDir ) throws RuntimeParamException {
		final String masterPrefix = Constants.MASTER_PREFIX.substring( 0, Constants.MASTER_PREFIX.length() - 1 );
		if( pipelineDir == null ) throw new RuntimeParamException( param, "", "Pipeline root directory not found!" );
		if( !pipelineDir.isDirectory() )
			throw new RuntimeParamException( param, pipelineDir.getAbsolutePath(), "System directory not found" );
		for( final File file: pipelineDir.listFiles() ) {
			if( file.getName().startsWith( Constants.MASTER_PREFIX ) ) {
				params.put( CONFIG_FILE, file.getAbsolutePath() );
				if( isDockerMode() ) {
					params.put( HOST_CONFIG_DIR, getDockerHostPipelineDir() );
				}
				return;
			}
		}

		throw new RuntimeParamException( param, pipelineDir.getAbsolutePath(),
			masterPrefix + " Config file not found in: " + pipelineDir.getAbsolutePath() );
	}

	private static String getDir( final String path ) {
		if( path != null && path.endsWith( File.separator ) ) return path.substring( 0, path.length() - 1 );
		return path;
	}

	private static void parseParams( final String[] args ) throws RuntimeParamException {
		String prevParam = "";
		for( final String arg: args ) {
			if( ARG_FLAGS.contains( arg ) ) {
				params.put( arg, Constants.TRUE );
			} else if( DIR_ARGS.contains( prevParam ) ) {
				params.put( prevParam, getDir( arg ) );
			} else if( NAMED_ARGS.contains( prevParam ) ) {
				params.put( prevParam, arg );
			} else {
				extraParams.add( arg );
			}
			prevParam = arg;
		}

		assignLastParam( prevParam );

		extraParams.removeAll( params.keySet() );
		extraParams.removeAll( params.values() );

		if( !extraParams.isEmpty() )
			throw new RuntimeParamException( "Unexpected runtime parameters found:  { " + extraParams + " }" );
	}

	private static void reassignDockerConfig() {
		Log.info( RuntimeParamUtil.class,
			"Assign \"" + HOST_BLJ_PROJ_DIR + "\" arg ---> " + params.get( BLJ_PROJ_DIR ) );
		Log.info( RuntimeParamUtil.class,
			"Reassign \"" + BLJ_PROJ_DIR + "\" arg ---> " + DockerUtil.CONTAINER_OUTPUT_DIR );
		params.put( HOST_BLJ_PROJ_DIR, params.get( BLJ_PROJ_DIR ) );
		params.put( HOST_CONFIG_DIR, getConfigFile().getParentFile().getAbsolutePath() );
		params.put( HOST_HOME_DIR, params.get( HOME_DIR ) );
		if( doRestart() ) {
			params.put( RESTART_DIR, DockerUtil.CONTAINER_OUTPUT_DIR + File.separator + getRestartDir().getName() );
		}

		params.put( BLJ_PROJ_DIR, DockerUtil.CONTAINER_OUTPUT_DIR );
		params.put( HOME_DIR, DockerUtil.DOCKER_HOME );
	}

	private static String[] simplifyArgs( final String[] args ) {
		final String[] simpleArgs = new String[ args.length ];
		int i = 0;
		String prevArg = "";
		boolean foundConfig = false;

		for( String arg: args ) {
			if( LONG_ARG_NAMES.contains( arg ) || NAMED_ARGS.contains( prevArg ) || DIR_ARGS.contains( prevArg )
				|| i == args.length - 1 && !foundConfig ) {
				simpleArgs[ i++ ] = arg;
			} else {
				if( arg.startsWith( "--" ) ) {
					arg = arg.substring( 1 );
				}
				if( !arg.startsWith( "-" ) ) {
					arg = "-" + arg;
				}
				arg = arg.substring( 0, 2 );
				simpleArgs[ i++ ] = arg;
			}
			prevArg = arg;
			foundConfig = arg.equals( CONFIG_FILE ) || foundConfig;
		}

		return simpleArgs;
	}

	private static void validateParams() throws RuntimeParamException {
		if( isDockerMode() && getDockerHostInputDir() == null )
			throw new RuntimeParamException( INPUT_DIR, "", "Docker host input directory required, but not found" );
		if( isDockerMode() && getDockerHostHomeDir() == null )
			throw new RuntimeParamException( HOME_DIR, "", "Docker host $HOME directory required, but not found" );
		if( isDockerMode() && getDockerHostConfigDir() == null ) throw new RuntimeParamException( HOST_CONFIG_DIR, "",
			"Docker host Config directory required, but not found" );
		if( getHomeDir() == null )
			throw new RuntimeParamException( HOME_DIR, "", "$HOME directory required, but not found" );
		if( getConfigFile() == null )
			throw new RuntimeParamException( CONFIG_FILE, "", "Config file required, but not found" );
		if( !getHomeDir().isDirectory() ) throw new RuntimeParamException( HOME_DIR, getHomeDir().getAbsolutePath(),
			"System directory-path not found" );
		if( !getConfigFile().isFile() ) throw new RuntimeParamException( CONFIG_FILE, getConfigFile().getAbsolutePath(),
			"System file-path not found" );
	}

	private static void verifyBaseDir() throws RuntimeParamException {
		if( getBaseDir() == null )
			throw new RuntimeParamException( BLJ_PROJ_DIR, "", "$BLJ_PROJ directory required, but not found" );
		if( !getBaseDir().isDirectory() ) throw new RuntimeParamException( BLJ_PROJ_DIR, getBaseDir().getAbsolutePath(),
			"System directory-path not found" );
	}

	/**
	 * {@link biolockj.Config} AWS end parameter switch: {@value #AWS_FLAG}
	 */
	public static final String AWS_FLAG = "-a";

	/**
	 * Automatically added $BLJ_PROJ by biolockj script: {@value #BLJ_PROJ_DIR}
	 */
	protected static final String BLJ_PROJ_DIR = "-b";

	/**
	 * {@link biolockj.Config} file path runtime parameter switch: {@value #CONFIG_FILE}
	 */
	protected static final String CONFIG_FILE = "-c";

	/**
	 * Direct mode runtime parameter switch: {@value #DIRECT_MODE}
	 */
	protected static final String DIRECT_MODE = "-d";

	/**
	 * Automatically added $HOME by biolockj script: {@value #HOME_DIR}
	 */
	protected static final String HOME_DIR = "-u";

	/**
	 * Host BioLockJ deployment to run - used to override installed $BLJ in Docker containers with BioLockJ installed:
	 * {@value #HOST_BLJ_DIR}
	 */
	protected static final String HOST_BLJ_DIR = "--host-blj";

	/**
	 * Host $USER $BLJ_PROJ_DIR param: {@value #HOST_BLJ_PROJ_DIR}
	 */
	protected static final String HOST_BLJ_PROJ_DIR = "--host-pipeline";

	/**
	 * Host BioLockJ deployment to run - used to map $BLJ_SUP volume in Docker containers with BioLockJ installed:
	 * {@value #HOST_BLJ_SUP_DIR}
	 */
	protected static final String HOST_BLJ_SUP_DIR = "--host-blj_sup";

	/**
	 * Host $USER config file path param: {@value #HOST_CONFIG_DIR}
	 */
	protected static final String HOST_CONFIG_DIR = "--host-config";

	/**
	 * Host $USER $HOME param: {@value #HOST_HOME_DIR}
	 */
	protected static final String HOST_HOME_DIR = "--host-home";

	/**
	 * Input directory file-path runtime parameter switch: {@value #INPUT_DIR}
	 */
	protected static final String INPUT_DIR = "-i";

	/**
	 * Metadata file directory path runtime parameter switch: {@value #META_DIR}
	 */
	protected static final String META_DIR = "-m";

	/**
	 * Change password runtime parameter switch: {@value #PASSWORD}
	 */
	protected static final String PASSWORD = "-p";

	/**
	 * Restart pipeline runtime parameter switch: {@value #RESTART_DIR}
	 */
	protected static final String RESTART_DIR = "-r";

	/**
	 * Log to System.out instead of Log for debug early runtime errors with switch: {@value #SYSTEM_OUT_FLAG}
	 */
	protected static final String SYSTEM_OUT_FLAG = "-s";

	private static final List<String> ARG_FLAGS = Arrays.asList( AWS_FLAG, SYSTEM_OUT_FLAG );
	private static final List<
		String> BLJ_CONTROLLER_ONLY_ARGS = Arrays.asList( BLJ_PROJ_DIR, CONFIG_FILE, HOME_DIR, PASSWORD, RESTART_DIR );
	private static final List<String> DIR_ARGS = Arrays.asList( BLJ_PROJ_DIR, HOME_DIR, HOST_BLJ_DIR, HOST_BLJ_PROJ_DIR,
		HOST_BLJ_SUP_DIR, HOST_CONFIG_DIR, HOST_HOME_DIR, INPUT_DIR, META_DIR, RESTART_DIR );
	private static final String DIRECT_PIPELINE_DIR = "--pipeline-dir";
	private static final List<String> extraParams = new ArrayList<>();
	private static final List<String> LONG_ARG_NAMES = Arrays.asList( DIRECT_PIPELINE_DIR, HOST_BLJ_DIR,
		HOST_BLJ_PROJ_DIR, HOST_BLJ_SUP_DIR, HOST_CONFIG_DIR, HOST_HOME_DIR );
	private static final List<String> NAMED_ARGS = Arrays.asList( CONFIG_FILE, DIRECT_MODE, PASSWORD );
	private static final Map<String, String> params = new HashMap<>();
	private static final String RETURN = Constants.RETURN;
	private static String runtimeArgs = "";
}
