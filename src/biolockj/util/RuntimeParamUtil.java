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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;

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
		return params.keySet().contains( RESTART_FLAG );
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
	 */
	public static File getBaseDir()
	{
		return params.get( BASE_DIR_FLAG ) == null ? null: new File( params.get( BASE_DIR_FLAG ) );
	}

	/**
	 * Runtime property getter for {@value #CONFIG_FLAG}
	 * 
	 * @return {@link biolockj.Config} file
	 */
	public static File getConfigFile()
	{
		return params.get( CONFIG_FLAG ) == null ? null: new File( params.get( CONFIG_FLAG ) );
	}

	/**
	 * Runtime property getter for {@value #DIRECT_FLAG}
	 * 
	 * @return Direct module BioModule class name
	 */
	public static String getDirectModule()
	{
		return params.get( DIRECT_FLAG );
	}

	/**
	 * Returns the name of the Docker container running the current module.
	 * 
	 * @return Name of the Docker container
	 */
	public static String getDockerContainerName()
	{
		if( isDirectMode() )
		{
			return getName( getDirectModule() );
		}

		return DockerUtil.MANAGER;

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
	 * @return Host {@value biolockj.Config#INPUT_DIRS} directory
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
	 * @return Host {@value biolockj.Config#INTERNAL_PIPELINE_DIR} directory
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
	 * Return all runtime args
	 * 
	 * @return boolean
	 */
	public static String getRuntimeArgs()
	{
		return runtimeArgs;
	}

	/**
	 * Return TRUE if runtime parameters indicate attempt to run in direct mode
	 * 
	 * @return boolean
	 */
	public static boolean isDirectMode()
	{
		return getDirectModule() != null;
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
		if( useSysOut )
		{
			System.out.println( Log.LOG_SPACER );
		}
		else
		{
			Log.info( RuntimeParamUtil.class, Log.LOG_SPACER );
		}
		if( args != null && args.length > 0 )
		{
			final String msg = "Application runtime args:";
			if( useSysOut )
			{
				System.out.println( msg );
			}
			else
			{
				Log.info( RuntimeParamUtil.class, msg );
			}
			int i = 0;
			for( final String arg: args )
			{
				final String out = " ---> arg[" + i++ + "] = " + arg;
				if( useSysOut )
				{
					System.out.println( out );
				}
				else
				{
					Log.info( RuntimeParamUtil.class, out );
				}
			}
		}
		else
		{
			final String msg = "NO RUNTIME ARGS FOUND!";
			if( useSysOut )
			{
				System.out.println( msg );
			}
			else
			{
				Log.info( RuntimeParamUtil.class, msg );
			}
		}
		if( useSysOut )
		{
			System.out.println( Log.LOG_SPACER );
		}
		else
		{
			Log.info( RuntimeParamUtil.class, Log.LOG_SPACER );
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
			throw new Exception( "Missing required runtime parameters." + RETURN + "Config file path is required."
					+ RETURN + RETURN + "PROGRAM TERMINATED!" );
		}

		parseParams( simplifyArgs( args ) );

		cacheRuntimeArgs( args );

		if( isDockerMode() )
		{
			params.put( HOST_PIPELINE_DIR, params.get( BASE_DIR_FLAG ) );
			params.put( BASE_DIR_FLAG, DockerUtil.CONTAINER_OUTPUT_DIR );
		}

		validateParams();
	}

	private static void cacheRuntimeArgs( final String[] args ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		if( args != null )
		{
			for( final String val: args )
			{
				sb.append( ( sb.length() > 0 ? " ": "" ) + val );
			}
		}

		runtimeArgs = sb.toString();
	}

	private static String getName( final String javaClassName )
	{
		final int len = javaClassName.lastIndexOf( "." ) + 1;
		return javaClassName.lastIndexOf( "." ) > 0
				? len < javaClassName.length() ? javaClassName.substring( len ): null
				: null;
	}

	private static void parseParams( final String[] args )
	{
		String prevParam = "";
		for( final String arg: args )
		{
			if( arg.equals( RESTART_FLAG ) )
			{
				params.put( RESTART_FLAG, Config.TRUE );
			}
			else if( arg.equals( DOCKER_FLAG ) )
			{
				params.put( DOCKER_FLAG, Config.TRUE );
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

		if( !params.keySet().contains( CONFIG_FLAG ) && !params.keySet().contains( prevParam )
				&& !params.values().contains( prevParam ) )
		{
			params.put( CONFIG_FLAG, prevParam );
		}

		extraParams.removeAll( params.keySet() );
		extraParams.removeAll( params.values() );
	}

	private static String[] simplifyArgs( final String[] args )
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
					+ "Extra paramaters = { " + StringUtil.getCollectionAsString( extraParams ) + " }" + RETURN );
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

			if( isDirectMode() && !( Class.forName( getDirectModule() ).newInstance() instanceof BioModule ) )
			{
				throw new Exception( "Docker direct runtime parameter module does not exist: " + getDirectModule()
						+ RETURN + RETURN + "PROGRAM TERMINATED!" );
			}

			if( getDockerHostConfigDir() == null )
			{
				throw new Exception( "Docker host config directory enviroment variable must be passed in Docker mode:"
						+ CONFIG_DIR_FLAG + RETURN + RETURN + "PROGRAM TERMINATED!" );
			}
		}
		else if( isDirectMode() && !( Class.forName( getDirectModule() ).newInstance() instanceof JavaModule ) )
		{
			throw new Exception( "Direct runtime parameter module is not a JavaModule: " + getDirectModule() + RETURN
					+ RETURN + "PROGRAM TERMINATED!" );
		}
	}

	/**
	 * Pipeline parent directory file-path runtime parameter switch: {@value #BASE_DIR_FLAG}
	 */
	public static final String BASE_DIR_FLAG = "-b";

	/**
	 * {@link biolockj.Config} file directory path runtime parameter switch: {@value #CONFIG_DIR_FLAG}
	 */
	public static final String CONFIG_DIR_FLAG = "-C";

	/**
	 * {@link biolockj.Config} file path runtime parameter switch: {@value #CONFIG_FLAG}
	 */
	public static final String CONFIG_FLAG = "-c";

	/**
	 * Direct mode runtime parameter switch: {@value #DIRECT_FLAG}
	 */
	public static final String DIRECT_FLAG = "-d";

	/**
	 * Docker mode runtime parameter switch: {@value #DOCKER_FLAG}
	 */
	public static final String DOCKER_FLAG = "-D";

	/**
	 * Input directory file-path runtime parameter switch: {@value #INPUT_DIR_FLAG}
	 */
	public static final String INPUT_DIR_FLAG = "-i";

	/**
	 * Metadata file directory path runtime parameter switch: {@value #META_DIR_FLAG}
	 */
	public static final String META_DIR_FLAG = "-m";

	/**
	 * Change password runtime parameter switch: {@value #PASSWORD_FLAG}
	 */
	public static final String PASSWORD_FLAG = "-p";

	/**
	 * Primer file directory path runtime parameter switch: {@value #PRIMER_DIR_FLAG}
	 */
	public static final String PRIMER_DIR_FLAG = "-t";

	/**
	 * Restart pipeline runtime parameter switch: {@value #RESTART_FLAG}
	 */
	public static final String RESTART_FLAG = "-r";

	private static final String BASE_DIR_FLAG_EXT = "--baseDir";

	private static final String CONFIG_FLAG_EXT = "--config";
	private static final List<String> extraParams = new ArrayList<>();
	private static final String HOST_PIPELINE_DIR = "--host_pipelineDir";
	private static final Map<String, String> params = new HashMap<>();
	private static final String PASSWORD_FLAG_EXT = "--password";
	private static final String RESTART_FLAG_EXT = "--restart";
	private static final String RETURN = BioLockJ.RETURN;
	private static String runtimeArgs = "";

	// private static void checkDirectoryForSingleFile( final String prop ) throws Exception
	// {
	// final File testFile = new File( params.get( prop ) );
	// if( !testFile.isFile() )
	// {
	// if( testFile.isDirectory() && testFile.list( HiddenFileFilter.VISIBLE ).length == 1 )
	// {
	// final String newPath = testFile.list( HiddenFileFilter.VISIBLE )[ 0 ];
	// Log.info( RuntimeParamUtil.class, "Runtime parameter [" + prop
	// + "] is a directory containing only one file, so setting this file as the property value: "
	// + newPath );
	// params.put( CONFIG_FLAG, newPath );
	// }
	// else
	// {
	// throw new Exception( testFile.getAbsolutePath() + " is not a valid system file!" );
	// }
	// }
	// }

}
