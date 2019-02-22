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
public class RuntimeParamUtil
{
	/**
	 * Return TRUE if runtime parameters indicate change password request
	 * 
	 * @return boolean
	 */
	public static boolean doChangePassword()
	{
		return params.get( PASSWORD_FLAG ) != null;
	}

	/**
	 * Return TRUE if runtime parameters indicate attempt to restart pipeline
	 * 
	 * @return boolean
	 */
	public static boolean doRestart()
	{
		return params.get( RESTART_FLAG ) != null;
	}

	/**
	 * Runtime property getter for {@value #PASSWORD_FLAG}
	 * 
	 * @return New clear-text password
	 */
	public static String getAdminEmailPassword()
	{
		return params.get( PASSWORD_FLAG );
	}

	/**
	 * Runtime property getter for {@value #BASE_DIR_FLAG}
	 * 
	 * @return $BLJ_PROJ pipeline parent directory
	 * @throws Exception if errors occur
	 */
	public static File getBaseDir() throws Exception
	{
		return params.get( BASE_DIR_FLAG ) == null ? null
				: new File( Config.getSystemFilePath( params.get( BASE_DIR_FLAG ) ) );
	}

	/**
	 * @return String
	 * @throws Exception if errors occur
	 */
	public static String getBaseDirParam() throws Exception
	{
		return BASE_DIR_FLAG + " " + getBaseDir().getAbsolutePath();
	}

	/**
	 * @return String
	 * @throws Exception if errors occur
	 */
	public static String getConficFileParam() throws Exception
	{
		return CONFIG_FLAG + " " + Config.getConfigFilePath();
	}

	/**
	 * Runtime property getter for {@value #CONFIG_FLAG}
	 * 
	 * @return {@link biolockj.Config} file
	 * @throws Exception if errors occur
	 */
	public static File getConfigFile() throws Exception
	{
		return params.get( CONFIG_FLAG ) == null ? null
				: new File( Config.getSystemFilePath( params.get( CONFIG_FLAG ) ) );
	}

	/**
	 * Runtime property getter for {@value #DIRECT_FLAG}
	 * 
	 * @return Direct module BioModule class name
	 */
	public static String getDirectModuleDir()
	{
		return params.get( DIRECT_FLAG );
	}

	/**
	 * Direct module parameters contain 2 parts separated by a colon: (pipeline directory name):(module name)
	 * 
	 * @param module BioModule
	 * @return Direct parameter flag + value
	 * @throws Exception if errors occur
	 */
	public static String getDirectModuleParam( final BioModule module ) throws Exception
	{
		return DIRECT_FLAG + " " + Config.pipelineName() + ":" + module.getModuleDir().getName();
	}

	/**
	 * Runtime property getter for direct module pipeline directory
	 * 
	 * @return Pipeline directory dir
	 * @throws Exception if errors occur
	 */
	public static File getDirectPipelineDir() throws Exception
	{
		return params.get( DIRECT_PIPELINE_DIR ) == null ? null
				: new File( Config.getSystemFilePath( params.get( DIRECT_PIPELINE_DIR ) ) );
	}

	/**
	 * Runtime property getter for {@value #CONFIG_DIR_FLAG}
	 * 
	 * @return PCR primer file directory path
	 */
	public static String getDockerHostConfigDir()
	{
		return params.get( CONFIG_DIR_FLAG );
	}

	/**
	 * Runtime property getter for {@value #INPUT_DIR_FLAG}
	 * 
	 * @return Host {@value biolockj.Constants#INPUT_DIRS} directory
	 */
	public static String getDockerHostInputDir()
	{
		return params.get( INPUT_DIR_FLAG );
	}

	/**
	 * Runtime property getter for {@value #META_DIR_FLAG}
	 * 
	 * @return Metadata file directory path
	 */
	public static String getDockerHostMetaDir()
	{
		return params.get( META_DIR_FLAG );
	}

	/**
	 * Runtime property getter for Docker host pipeline dir
	 * 
	 * @return Host {@value biolockj.Constants#PROJECT_PIPELINE_DIR} directory
	 */
	public static String getDockerHostPipelineDir()
	{
		return params.get( HOST_PIPELINE_DIR );
	}

	/**
	 * Runtime property getter for {@value #PRIMER_DIR_FLAG}
	 * 
	 * @return PCR primer file directory path
	 */
	public static String getDockerHostPrimerDir()
	{
		return params.get( PRIMER_DIR_FLAG );
	}

	/**
	 * Get Docker runtime arguments, passed to BioLockJ from dockblj script.<br>
	 * These are used to build Docker worker scripts by the modules.
	 * 
	 * @return Docker runtime args
	 * @throws Exception if errors occur
	 */
	public static String getDockerRuntimeArgs() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final TreeMap<String, String> args = new TreeMap<>( params );

		for( final String key: args.keySet() )
		{
			if( key.equals( RESTART_FLAG ) || key.equals( BASE_DIR_FLAG ) )
			{
				continue;
			}
			else if( key.equals( HOST_PIPELINE_DIR ) )
			{
				sb.append( ( sb.toString().isEmpty() ? "": " " ) + BASE_DIR_FLAG + " " + args.get( key ) );
			}
			else
			{
				sb.append( ( sb.toString().isEmpty() ? "": " " ) + key );
				sb.append( args.get( key ).equals( Constants.TRUE ) ? "": " " + args.get( key ) );
			}
		}

		return sb.toString();
	}

	/**
	 * Return restart pipeline directory
	 * 
	 * @return File directory path
	 * @throws Exception if errors occur
	 */
	public static File getRestartDir() throws Exception
	{
		return params.get( RESTART_FLAG ) == null ? null
				: new File( Config.getSystemFilePath( params.get( RESTART_FLAG ) ) );
	}

	/**
	 * Return TRUE if runtime parameters indicate attempt to run in direct mode
	 * 
	 * @return boolean
	 */
	public static boolean isDirectMode()
	{
		return getDirectModuleDir() != null;
	}

	/**
	 * Return TRUE if runtime parameter {@value #DOCKER_FLAG} was found
	 * 
	 * @return boolean
	 */
	public static boolean isDockerMode()
	{
		return params.get( DOCKER_FLAG ) != null;
	}

	/**
	 * Print the runtime args to System.out or using the Logger based on useSysOut parameter.
	 * 
	 * @param args Runtime Args
	 * @param useSysOut Set TRUE if should use System.out to print
	 */
	public static void printRuntimeArgs( final String[] args, final boolean useSysOut )
	{
		try
		{
			useSystemOut = useSysOut;
			info( RETURN + Constants.LOG_SPACER );
			if( args != null && args.length > 0 )
			{
				info( "Application runtime args:" );
				int i = 0;
				for( final String arg: args )
				{
					info( " ---> arg[" + i++ + "] = " + arg );
				}
			}
			else
			{
				info( "NO RUNTIME ARGS FOUND!" );
			}
			info( Constants.LOG_SPACER );

		}
		catch( final Exception ex )
		{
			Log.error( RuntimeParamUtil.class, "Error reading inpur parameters", ex );
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
	public static void registerRuntimeParameters( final String[] args ) throws Exception
	{
		printRuntimeArgs( args, false );

		if( args == null || args.length < 2 )
		{
			throw new Exception( "Missing required runtime parameters." + RETURN + "Required system path is not found. "
					+ RETURN + RETURN + "PROGRAM TERMINATED!" );
		}

		parseParams( simplifyArgs( args ) );

		if( isDockerMode() )
		{
			reassignDockerConfig();
		}

		if( isDirectMode() )
		{
			assignDirectPipelineDir();
		}

		if( doRestart() )
		{
			assignRestartConfig();
		}

		validateParams();
	}

	private static void assignDirectPipelineDir() throws Exception
	{
		Log.info( RuntimeParamUtil.class,
				"Separating pipeline dir name and module name from: \"" + DIRECT_FLAG + "\" " + getDirectModuleDir() );
		final StringTokenizer st = new StringTokenizer( getDirectModuleDir(), ":" );
		if( st.countTokens() != 2 )
		{
			throw new Exception(
					"Direct module param format requires pipelineDir name & BioModule directory name are separated"
							+ " by a colon \":\" which should appear exactly once but was found " + st.countTokens()
							+ " times in the param [ " + getDirectModuleDir() + " ]" );
		}

		final String pipelineName = st.nextToken();
		final File pipelineDir = new File( getBaseDir().getAbsolutePath() + File.separator + pipelineName );
		if( !pipelineDir.exists() )
		{
			throw new Exception( "Direct module pipeline directory not found: " + pipelineDir.getAbsolutePath() );
		}

		params.put( DIRECT_PIPELINE_DIR, pipelineDir.getAbsolutePath() );
		params.put( DIRECT_FLAG, st.nextToken() );
	}

	private static void assignLastParam( final String param ) throws Exception
	{
		if( param.equals( BASE_DIR_FLAG ) || param.equals( CONFIG_FLAG ) || param.equals( RESTART_FLAG )
				|| param.equals( CONFIG_DIR_FLAG ) || param.equals( DIRECT_FLAG ) || param.equals( PASSWORD_FLAG )
				|| param.equals( INPUT_DIR_FLAG ) || param.equals( META_DIR_FLAG ) || param.equals( PRIMER_DIR_FLAG ) )
		{
			throw new Exception( "Missing argument value after paramter: \"" + param + "\"" );
		}

		if( !params.keySet().contains( CONFIG_FLAG ) && !params.keySet().contains( RESTART_FLAG )
				&& !params.keySet().contains( param ) && !params.values().contains( param ) )
		{
			params.put( CONFIG_FLAG, param );
		}
	}

	private static void assignRestartConfig() throws Exception
	{
		Log.info( RuntimeParamUtil.class, "Found \"" + RESTART_FLAG + "\" arg ---> RESTART PIPELINE" );
		if( params.keySet().contains( RESTART_FLAG ) && getConfigFile() == null )
		{
			params.put( CONFIG_FLAG, getRestartConfigFile() );
		}
	}

	private static String getRestartConfigFile() throws Exception
	{
		if( getRestartDir() != null && getRestartDir().isDirectory() )
		{
			Log.info( RuntimeParamUtil.class,
					"Found \"" + RESTART_FLAG + "\" directory = " + getRestartDir().getAbsolutePath() );
			if( getRestartDir().listFiles().length > 0 )
			{
				for( final File f: getRestartDir().listFiles() )
				{
					if( f.getName().startsWith( Constants.MASTER_PREFIX ) )
					{
						return f.getAbsolutePath();
					}
				}
			}
		}

		throw new Exception( "Restarted pipelines require a valid pipeline directory path after \"-r\" parameter." );
	}

	private static void info( final String msg ) throws Exception
	{
		if( useSystemOut )
		{
			System.out.println( msg );
		}
		else
		{
			Log.info( RuntimeParamUtil.class, msg );
		}
	}

	private static void parseParams( final String[] args ) throws Exception
	{
		String prevParam = "";
		for( final String arg: args )
		{
			if( arg.equals( DOCKER_FLAG ) )
			{
				params.put( DOCKER_FLAG, Constants.TRUE );
			}
			else if( prevParam.equals( RESTART_FLAG ) )
			{
				params.put( RESTART_FLAG, arg );
			}
			else if( prevParam.equals( BASE_DIR_FLAG ) )
			{
				params.put( BASE_DIR_FLAG, arg );
			}
			else if( prevParam.equals( CONFIG_FLAG ) )
			{
				params.put( CONFIG_FLAG, arg );
			}
			else if( prevParam.equals( CONFIG_DIR_FLAG ) )
			{
				params.put( CONFIG_DIR_FLAG, arg );
			}
			else if( prevParam.equals( DIRECT_FLAG ) )
			{
				params.put( DIRECT_FLAG, arg );
			}
			else if( prevParam.equals( PASSWORD_FLAG ) )
			{
				params.put( PASSWORD_FLAG, arg );
			}
			else if( prevParam.equals( INPUT_DIR_FLAG ) )
			{
				params.put( INPUT_DIR_FLAG, arg );
			}
			else if( prevParam.equals( META_DIR_FLAG ) )
			{
				params.put( META_DIR_FLAG, arg );
			}
			else if( prevParam.equals( PRIMER_DIR_FLAG ) )
			{
				params.put( PRIMER_DIR_FLAG, arg );
			}
			else
			{
				extraParams.add( arg );
			}

			prevParam = arg;
		}

		assignLastParam( prevParam );

		extraParams.removeAll( params.keySet() );
		extraParams.removeAll( params.values() );
	}

	private static void reassignDockerConfig() throws Exception
	{
		Log.info( RuntimeParamUtil.class, "Found \"" + DOCKER_FLAG + "\" arg ---> ENV = DOCKER CONTAINER" );
		Log.info( RuntimeParamUtil.class,
				"Assign \"" + HOST_PIPELINE_DIR + "\" arg ---> " + params.get( BASE_DIR_FLAG ) );
		Log.info( RuntimeParamUtil.class,
				"Reassign \"" + BASE_DIR_FLAG + "\" arg ---> " + DockerUtil.CONTAINER_OUTPUT_DIR );
		params.put( HOST_PIPELINE_DIR, params.get( BASE_DIR_FLAG ) );
		params.put( BASE_DIR_FLAG, DockerUtil.CONTAINER_OUTPUT_DIR );
	}

	private static String[] simplifyArgs( final String[] args ) throws Exception
	{
		final String[] simpleArgs = new String[ args.length ];
		int i = 0;
		for( final String arg: args )
		{
			if( arg.equals( RESTART_FLAG_EXT ) )
			{
				simpleArgs[ i++ ] = RESTART_FLAG;
			}
			else if( arg.equals( PASSWORD_FLAG_EXT ) )
			{
				simpleArgs[ i++ ] = PASSWORD_FLAG;
			}
			else if( arg.equals( CONFIG_FLAG_EXT ) )
			{
				simpleArgs[ i++ ] = CONFIG_FLAG;
			}
			else if( arg.equals( BASE_DIR_FLAG_EXT ) )
			{
				simpleArgs[ i++ ] = BASE_DIR_FLAG;
			}
			else
			{
				simpleArgs[ i++ ] = arg;
			}
		}

		return simpleArgs;
	}

	private static void validateParams() throws Exception
	{
		if( !extraParams.isEmpty() )
		{
			throw new Exception( "Too many runtime parameters found for command [ biolockj ]. " + RETURN
					+ "Extra paramaters = { " + BioLockJUtil.getCollectionAsString( extraParams ) + " }" + RETURN );
		}

		if( getConfigFile() == null )
		{
			throw new Exception(
					"Required runtime parameter (Config file path) is undefined, missing: " + CONFIG_FLAG );
		}

		if( getBaseDir() == null )
		{
			throw new Exception( "Required environment variable $BLJ_PROJ is undefined, missing: " + BASE_DIR_FLAG );
		}

		params.put( CONFIG_FLAG, Config.getSystemFilePath( params.get( CONFIG_FLAG ) ) );
		params.put( BASE_DIR_FLAG, Config.getSystemFilePath( params.get( BASE_DIR_FLAG ) ) );

		if( !getConfigFile().exists() )
		{
			throw new Exception( getConfigFile().getAbsolutePath() + " is not a valid system path!" );
		}
		if( !getConfigFile().isFile() )
		{
			throw new Exception( getConfigFile().getAbsolutePath() + " is not a valid system file!" );
		}
		if( !getBaseDir().exists() )
		{
			throw new Exception( getBaseDir().getAbsolutePath() + " is not a valid system path!" );
		}
		if( !getBaseDir().isDirectory() )
		{
			throw new Exception( getBaseDir().getAbsolutePath() + " is not a valid system directory!" );
		}

		if( isDockerMode() )
		{
			if( getDockerHostInputDir() == null )
			{
				throw new Exception( "Docker host input directory enviroment variable must be passed in Docker mode:"
						+ INPUT_DIR_FLAG + RETURN + RETURN + "PROGRAM TERMINATED!" );
			}

			if( getDockerHostConfigDir() == null )
			{
				throw new Exception( "Docker host config directory enviroment variable must be passed in Docker mode:"
						+ CONFIG_DIR_FLAG + RETURN + RETURN + "PROGRAM TERMINATED!" );
			}
		}
	}

	/**
	 * Pipeline parent directory file-path runtime parameter switch: {@value #BASE_DIR_FLAG}
	 */
	protected static final String BASE_DIR_FLAG = "-b";

	/**
	 * {@link biolockj.Config} file directory path runtime parameter switch: {@value #CONFIG_DIR_FLAG}
	 */
	protected static final String CONFIG_DIR_FLAG = "-C";

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
	protected static final String DOCKER_FLAG = "-D";

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
	 * Primer file directory path runtime parameter switch: {@value #PRIMER_DIR_FLAG}
	 */
	protected static final String PRIMER_DIR_FLAG = "-t";

	/**
	 * Restart pipeline runtime parameter switch: {@value #RESTART_FLAG}
	 */
	protected static final String RESTART_FLAG = "-r";

	private static final String BASE_DIR_FLAG_EXT = "--baseDir";

	private static final String CONFIG_FLAG_EXT = "--config";
	private static final String DIRECT_PIPELINE_DIR = "--pipeline-dir";
	private static final List<String> extraParams = new ArrayList<>();
	private static final String HOST_PIPELINE_DIR = "--host_pipelineDir";
	private static final Map<String, String> params = new HashMap<>();
	private static final String PASSWORD_FLAG_EXT = "--password";
	private static final String RESTART_FLAG_EXT = "--restart";
	private static final String RETURN = Constants.RETURN;
	private static boolean useSystemOut = false;
}
