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
import biolockj.module.BioModule;

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
		return params.get( PASSWORD_FLAG ) != null;
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
	 * Runtime property getter for {@value #PASSWORD_FLAG}
	 * 
	 * @return New clear-text password
	 */
	public static String getAdminEmailPassword() {
		return params.get( PASSWORD_FLAG );
	}

	/**
	 * Runtime property getter for {@value #BASE_DIR_FLAG}
	 * 
	 * @return $BLJ_PROJ pipeline parent directory
	 */
	public static File getBaseDir() {
		return new File( params.get( BASE_DIR_FLAG ) );
	}

	/**
	 * Get the baseDir param and value
	 * 
	 * @return String baseDir param and value
	 */
	public static String getBaseDirParam() {
		return BASE_DIR_FLAG + " " + getBaseDir().getAbsolutePath();
	}

	/**
	 * Runtime property getter for {@value #CONFIG_FLAG}
	 * 
	 * @return {@link biolockj.Config} file
	 */
	public static File getConfigFile() {
		return new File( params.get( CONFIG_FLAG ) );
	}

	/**
	 * Used to build Docker run commands.
	 * 
	 * @return {@link biolockj.Config} file param and value.
	 */
	public static String getConfigFileParam() {
		return CONFIG_FLAG + " " + Config.getConfigFilePath();
	}

	/**
	 * Runtime property getter for {@value #DIRECT_FLAG}
	 * 
	 * @return Direct module BioModule class name
	 */
	public static String getDirectModuleDir() {
		return params.get( DIRECT_FLAG );
	}

	/**
	 * Direct module parameters contain 2 parts separated by a colon: (pipeline directory name):(module name)
	 * 
	 * @param module BioModule
	 * @return Direct parameter flag + value
	 */
	public static String getDirectModuleParam( final BioModule module ) {
		return DIRECT_FLAG + " " + Config.pipelineName() + ":" + module.getModuleDir().getName();
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
	 * @return Host {@value #HOST_BLJ} directory
	 */
	public static File getDockerHostBLJ() {
		return params.get( HOST_BLJ ) == null ? null: new File( params.get( HOST_BLJ ) );
	}

	/**
	 * Runtime property getter for Docker host blj_support dir
	 * 
	 * @return Host {@value #HOST_BLJ_SUP} directory
	 */
	public static File getDockerHostBLJ_SUP() {
		return params.get( HOST_BLJ_SUP ) == null ? null: new File( params.get( HOST_BLJ_SUP ) );
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
	 * Runtime property getter for Docker host $USER $HOME dir
	 * 
	 * @return Host {@value #HOST_HOME_USER_DIR} directory
	 */
	public static File getDockerHostHomeUserDir() {
		return new File( params.get( HOST_HOME_USER_DIR ) );
	}

	/**
	 * Runtime property getter for {@value #INPUT_DIR_FLAG}
	 * 
	 * @return Host {@value biolockj.Constants#INPUT_DIRS} directory
	 */
	public static String getDockerHostInputDir() {
		return params.get( INPUT_DIR_FLAG );
	}

	/**
	 * Runtime property getter for {@value #META_DIR_FLAG}
	 * 
	 * @return Metadata file directory path
	 */
	public static String getDockerHostMetaDir() {
		return params.get( META_DIR_FLAG );
	}

	/**
	 * Runtime property getter for Docker host pipeline dir
	 * 
	 * @return Host {@value biolockj.Constants#INTERNAL_PIPELINE_DIR} directory
	 */
	public static String getDockerHostPipelineDir() {
		return params.get( HOST_PIPELINE_DIR );
	}

	/**
	 * Get Docker runtime arguments passed to BioLockJ from dockblj script.<br>
	 * These are used to to populate BLJ_OPTIONS in Docker java_module scripts.
	 * 
	 * @return Docker runtime parameters
	 */
	public static String getDockerJavaModuleParams() {
		String javaModArgs = "";
		for( final String key: params.keySet() ) {
			if( key.equals( CONFIG_FLAG ) || key.equals( RESTART_FLAG ) || key.equals( BASE_DIR_FLAG )
				|| key.equals( PASSWORD_FLAG ) ) {
				continue;
			} else if( key.equals( HOST_PIPELINE_DIR ) ) {
				javaModArgs += ( javaModArgs.isEmpty() ? "": " " ) + BASE_DIR_FLAG + " " + params.get( key );
			} else if( key.equals( HOST_CONFIG_DIR ) ) {
				javaModArgs += ( javaModArgs.isEmpty() ? "": " " ) + CONFIG_FLAG + " " + params.get( key )
					+ getConfigFile().getName();
			} else {
				javaModArgs += ( javaModArgs.isEmpty() ? "": " " ) + key;
				javaModArgs += params.get( key ).equals( Constants.TRUE ) ? "": " " + params.get( key );
			}
		}

		return javaModArgs;
	}

	/**
	 * Extract the project name from the Config file.
	 * 
	 * @return Project name
	 */
	public static String getProjectName() {
		final String configName = getConfigFile().getName();
		String name = configName;
		final String[] exts = { ".ascii", ".asc", ".plain", ".rft", ".tab", ".text", ".tsv", ".txt", ".properties",
			".prop", ".props", ".config" };

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
		return params.get( RESTART_FLAG ) == null ? null: new File( params.get( RESTART_FLAG ) );
	}

	/**
	 * Return TRUE if runtime parameter {@value #DOCKER_FLAG} was found
	 * 
	 * @return boolean
	 */
	public static boolean isAwsMode() {
		return params.get( AWS_FLAG ) != null;
	}

	/**
	 * Return TRUE if runtime parameter {@value #DOCKER_FLAG} was found
	 * 
	 * @return boolean
	 */
	public static boolean isDockerMode() {
		return params.get( DOCKER_FLAG ) != null;
	}

	/**
	 * Print the runtime args to System.out or using the Logger based on useSysOut parameter.
	 * 
	 * @param args Runtime Args
	 * @param useSysOut Set TRUE if should use System.out to print
	 */
	public static void printRuntimeArgs( final String[] args, final boolean useSysOut ) {
		try {
			useSystemOut = useSysOut;
			info( RETURN + Constants.LOG_SPACER );
			if( args != null && args.length > 0 ) {
				info( "Application runtime args:" );
				int i = 0;
				for( final String arg: args ) {
					info( " ---> arg[" + i++ + "] = " + arg );
				}
			} else {
				info( "NO RUNTIME ARGS FOUND!" );
			}
			info( Constants.LOG_SPACER );

		} catch( final Exception ex ) {
			Log.error( RuntimeParamUtil.class, "Error reading input parameters", ex );
		}
	}

	/**
	 * Register and verify the runtime parameters. There are 2 required parameters:<br>
	 * <ol>
	 * <li>The {@link biolockj.Config} file path
	 * <li>The pipeline parent directory ($DOCKER_PROJ)
	 * </ol>
	 *
	 * @param args Program runtime parameters
	 * @throws Exception if invalid parameters detected
	 */
	public static void registerRuntimeParameters( final String[] args ) throws Exception {

		printRuntimeArgs( args, false );

		if( args == null || args.length < 2 ) throw new Exception( "Missing required runtime parameters." + RETURN
			+ "Required system path is not found. " + RETURN + RETURN + "PROGRAM TERMINATED!" );

		parseParams( simplifyArgs( args ) );

		if( isDockerMode() ) {
			reassignDockerConfig();
		}

		verifyBaseDir();

		if( getDirectModuleDir() != null ) {
			assignMasterConfig( assignDirectPipelineDir() );
		} else if( doRestart() && getConfigFile() == null ) {
			assignMasterConfig( getRestartDir() );
		} else {
			params.put( CONFIG_FLAG, Config.getConfigFile( params.get( CONFIG_FLAG ) ).getAbsolutePath() );
		}

		validateParams();
	}

	private static File assignDirectPipelineDir() throws Exception {
		Log.info( RuntimeParamUtil.class,
			"Separating pipeline dir name and module name from: \"" + DIRECT_FLAG + "\" " + getDirectModuleDir() );
		final StringTokenizer st = new StringTokenizer( getDirectModuleDir(), ":" );
		if( st.countTokens() != 2 ) throw new Exception(
			"Direct module param format requires pipelineDir name & BioModule directory name are separated"
				+ " by a colon \":\" which should appear exactly once but was found " + st.countTokens()
				+ " times in the param [ " + getDirectModuleDir() + " ]" );

		final String pipelineName = st.nextToken();
		final File pipelineDir = new File( getBaseDir().getAbsolutePath() + File.separator + pipelineName );
		if( !pipelineDir.isDirectory() )
			throw new Exception( "Direct module pipeline directory not found: " + pipelineDir.getAbsolutePath() );

		params.put( DIRECT_PIPELINE_DIR, pipelineDir.getAbsolutePath() );
		params.put( DIRECT_FLAG, st.nextToken() );
		return getDirectPipelineDir();
	}

	private static void assignLastParam( final String param ) throws Exception {
		if( param.equals( BASE_DIR_FLAG ) || param.equals( CONFIG_FLAG ) || param.equals( RESTART_FLAG )
			|| param.equals( DIRECT_FLAG ) || param.equals( PASSWORD_FLAG ) || param.equals( HOST_BLJ )
			|| param.equals( HOST_BLJ_SUP ) || param.equals( HOST_HOME_USER_DIR ) || param.equals( INPUT_DIR_FLAG )
			|| param.equals( META_DIR_FLAG ) )
			throw new Exception( "Missing argument value after paramter: \"" + param + "\"" );

		if( !params.keySet().contains( CONFIG_FLAG ) && !params.keySet().contains( RESTART_FLAG )
			&& !params.keySet().contains( param ) && !params.values().contains( param ) ) {
			params.put( CONFIG_FLAG, param );
		}
	}

	private static File assignMasterConfig( final File pipelineDir ) throws Exception {
		if( pipelineDir == null ) throw new Exception( "Pipeline root directory undefined!" );
		if( !pipelineDir.isDirectory() )
			throw new Exception( "Pipeline root directory undefined: " + pipelineDir.getAbsolutePath() );
		for( final File file: pipelineDir.listFiles() ) {
			if( file.getName().startsWith( Constants.MASTER_PREFIX ) ) {
				params.put( CONFIG_FLAG, file.getAbsolutePath() );
				if( isDockerMode() ) {
					params.put( HOST_CONFIG_DIR, getDockerHostPipelineDir() );
				}
				return getConfigFile();
			}
		}
		throw new Exception( Constants.MASTER_PREFIX.substring( 0, Constants.MASTER_PREFIX.length() - 1 )
			+ " Config file not found in: " + pipelineDir.getAbsolutePath() );
	}

	private static void info( final String msg ) {
		if( useSystemOut ) {
			System.out.println( msg );
		} else {
			Log.info( RuntimeParamUtil.class, msg );
		}
	}

	private static void parseParams( final String[] args ) throws Exception {
		String prevParam = "";
		for( final String arg: args ) {
			if( arg.equals( DOCKER_FLAG ) || arg.equals( AWS_FLAG ) ) {
				params.put( arg, Constants.TRUE );
			} else if( prevParam.equals( RESTART_FLAG ) ) {
				params.put( RESTART_FLAG, arg );
			} else if( prevParam.equals( HOST_HOME_USER_DIR ) ) {
				params.put( HOST_HOME_USER_DIR, arg );
			} else if( prevParam.equals( HOST_BLJ ) ) {
				params.put( HOST_BLJ, arg );
			} else if( prevParam.equals( HOST_BLJ_SUP ) ) {
				params.put( HOST_BLJ_SUP, arg );
			} else if( prevParam.equals( BASE_DIR_FLAG ) ) {
				params.put( BASE_DIR_FLAG, arg );
			} else if( prevParam.equals( CONFIG_FLAG ) ) {
				params.put( CONFIG_FLAG, arg );
			} else if( prevParam.equals( DIRECT_FLAG ) ) {
				params.put( DIRECT_FLAG, arg );
			} else if( prevParam.equals( PASSWORD_FLAG ) ) {
				params.put( PASSWORD_FLAG, arg );
			} else if( prevParam.equals( INPUT_DIR_FLAG ) ) {
				params.put( INPUT_DIR_FLAG, arg );
			} else if( prevParam.equals( META_DIR_FLAG ) ) {
				params.put( META_DIR_FLAG, arg );
			} else {
				extraParams.add( arg );
			}

			prevParam = arg;
		}

		assignLastParam( prevParam );

		extraParams.removeAll( params.keySet() );
		extraParams.removeAll( params.values() );
	}

	private static void reassignDockerConfig() {
		Log.info( RuntimeParamUtil.class, "Found \"" + DOCKER_FLAG + "\" arg ---> ENV = DOCKER CONTAINER" );
		Log.info( RuntimeParamUtil.class,
			"Assign \"" + HOST_PIPELINE_DIR + "\" arg ---> " + params.get( BASE_DIR_FLAG ) );
		Log.info( RuntimeParamUtil.class,
			"Reassign \"" + BASE_DIR_FLAG + "\" arg ---> " + DockerUtil.CONTAINER_OUTPUT_DIR );
		params.put( HOST_PIPELINE_DIR, params.get( BASE_DIR_FLAG ) );
		params.put( HOST_CONFIG_DIR, getConfigFile().getParentFile().getAbsolutePath() );
		params.put( BASE_DIR_FLAG, DockerUtil.CONTAINER_OUTPUT_DIR );

		if( doRestart() ) {
			params.put( RESTART_FLAG, DockerUtil.CONTAINER_OUTPUT_DIR + File.separator + getRestartDir().getName() );
		}
	}

	private static String[] simplifyArgs( final String[] args ) {
		final String[] simpleArgs = new String[ args.length ];
		int i = 0;
		String prevArg = "";
		boolean foundConfig = false;
		final List<String> dockerLongNames = Arrays.asList( new String[] { DOCKER_FLAG, HOST_BLJ, HOST_BLJ_SUP } );

		for( String arg: args ) {
			if( dockerLongNames.contains( arg ) || ARGS.contains( prevArg ) || i == args.length - 1 && !foundConfig ) {
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
			foundConfig = arg.equals( CONFIG_FLAG ) || foundConfig;
		}

		return simpleArgs;
	}

	private static void validateParams() throws Exception {
		if( !extraParams.isEmpty() ) throw new Exception( "Too many runtime parameters found for command [ biolockj ]. "
			+ RETURN + "Extra paramaters = { " + BioLockJUtil.getCollectionAsString( extraParams ) + " }" + RETURN );

		if( isDockerMode() && getDockerHostInputDir() == null )
			throw new Exception( "Docker host input directory enviroment variable must be passed in Docker mode:"
				+ INPUT_DIR_FLAG + RETURN + RETURN + "PROGRAM TERMINATED!" );
	}

	private static void verifyBaseDir() throws Exception {
		if( getBaseDir() == null )
			throw new Exception( "Required environment variable $BLJ_PROJ is undefined, missing: " + BASE_DIR_FLAG );

		if( !getBaseDir().isDirectory() )
			throw new Exception( getBaseDir().getAbsolutePath() + " is not a valid system directory!" );
	}

	/**
	 * {@link biolockj.Config} AWS end parameter switch: {@value #AWS_FLAG}
	 */
	protected static final String AWS_FLAG = "-a";

	/**
	 * Pipeline parent directory file-path runtime parameter switch: {@value #BASE_DIR_FLAG}
	 */
	protected static final String BASE_DIR_FLAG = "-b";

	/**
	 * {@link biolockj.Config} file path runtime parameter switch: {@value #CONFIG_FLAG}
	 */
	protected static final String CONFIG_FLAG = "-c";

	/**
	 * Direct mode runtime parameter switch: {@value #DIRECT_FLAG}
	 */
	protected static final String DIRECT_FLAG = "-d";

	/**
	 * Docker mode runtime parameter switch: {@value #DOCKER_FLAG}
	 */
	protected static final String DOCKER_FLAG = "-docker";

	/**
	 * Host BioLockJ deployment to run - used to override installed $BLJ in Docker containers with BioLockJ installed:
	 * {@value #HOST_BLJ}
	 */
	protected static final String HOST_BLJ = "-blj";

	/**
	 * Host BioLockJ deployment to run - used to map $BLJ_SUP volume in Docker containers with BioLockJ installed:
	 * {@value #HOST_BLJ_SUP}
	 */
	protected static final String HOST_BLJ_SUP = "-bljSup";

	/**
	 * Host $USER $HOME param: {@value #HOST_HOME_USER_DIR}
	 */
	protected static final String HOST_HOME_USER_DIR = "-u";

	/**
	 * Input directory file-path runtime parameter switch: {@value #INPUT_DIR_FLAG}
	 */
	protected static final String INPUT_DIR_FLAG = "-i";

	/**
	 * Metadata file directory path runtime parameter switch: {@value #META_DIR_FLAG}
	 */
	protected static final String META_DIR_FLAG = "-m";

	/**
	 * Change password runtime parameter switch: {@value #PASSWORD_FLAG}
	 */
	protected static final String PASSWORD_FLAG = "-p";

	/**
	 * Restart pipeline runtime parameter switch: {@value #RESTART_FLAG}
	 */
	protected static final String RESTART_FLAG = "-r";

	private static List<String> ARGS = Arrays.asList( new String[] { RESTART_FLAG, PASSWORD_FLAG, META_DIR_FLAG,
		INPUT_DIR_FLAG, HOST_HOME_USER_DIR, DIRECT_FLAG, CONFIG_FLAG, BASE_DIR_FLAG, HOST_BLJ_SUP, HOST_BLJ } );

	private static final String DIRECT_PIPELINE_DIR = "--pipeline-dir";
	private static final List<String> extraParams = new ArrayList<>();
	private static final String HOST_CONFIG_DIR = "--host_configDir";
	private static final String HOST_PIPELINE_DIR = "--host_pipelineDir";
	private static final Map<String, String> params = new HashMap<>();
	private static final String RETURN = Constants.RETURN;
	private static boolean useSystemOut = false;
}
