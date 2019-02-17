/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Aug 14, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.implicit.qiime.BuildQiimeMapping;
import biolockj.module.implicit.qiime.MergeQiimeOtuTables;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.module.report.r.R_Module;

/**
 * DockerUtil for Docker integration.
 */
public class DockerUtil
{
	/**
	 * Build the {@value #SPAWN_DOCKER_CONTAINER} method, which takes container name, in/out port, and optionally script
	 * path parameters.
	 * 
	 * @param module BioModule
	 * @return Bash function to run docker
	 * @throws Exception if unable to build the function
	 */
	public static List<String> buildRunDockerFunction( final BioModule module ) throws Exception
	{
		final List<String> lines = new ArrayList<>();

		Log.info( DockerUtil.class, "Docker volumes:" + getDockerVolumes() + BioLockJ.RETURN );
		Log.info( DockerUtil.class, "Docker Environment variables:" + getDockerEnvVars( module ) + BioLockJ.RETURN );

		lines.add( "# Spawn Docker container" );
		lines.add( "function " + SPAWN_DOCKER_CONTAINER + "() {" );
		lines.add( Config.getExe( module, Constants.EXE_DOCKER ) + " run" + rmFlag() + getDockerEnvVars( module )
				+ getDockerVolumes() + getDockerImage( module ) );
		lines.add( "}" );
		return lines;
	}

	/**
	 * Get the name of the Docker image.
	 * 
	 * @param module BioModule
	 * @return Docker image name
	 * @throws Exception if errors occur
	 */
	public static String getDockerImage( final BioModule module ) throws Exception
	{
		final boolean isQiime = module instanceof BuildQiimeMapping || module instanceof MergeQiimeOtuTables
				|| module instanceof QiimeClassifier;

		final boolean isR = module instanceof R_Module;

		final String name = isR ? R_Module.class.getSimpleName()
				: isQiime ? QiimeClassifier.class.getSimpleName()
						: module instanceof JavaModule ? JavaModule.class.getSimpleName() 
								: module.getClass().getSimpleName();

		

		return " " + getDockerUser( module.getClass().getSimpleName() ) + "/" + getImageName( name ) + ":" + getImageVersion( module.getClass().getSimpleName() );
	}
	
	/**
	 * Return the Docker Hub user ID.  If none configured, return biolockj.
	 * 
	 * @param module Calling module
	 * @return Docker Hub User ID 
	 * @throws Exception if errors occur
	 */
	public static String getDockerUser( final String moduleName ) throws Exception
	{
		String user = Config.getString( null, Config.getModuleProp( moduleName, DOCKER_HUB_USER ) );
		user = user == null ? DEFAULT_DOCKER_HUB_USER: user;
		return user;
	}

	/**
	 * Get mapped Docker system path from {@link biolockj.Config} property by replacing the host system path with the
	 * mapped container path.
	 * 
	 * @param path {@link biolockj.Config} property
	 * @param containerPath Local container path
	 * @param label Path label
	 * @return Docker volume file object
	 * @throws Exception if errors occur
	 */
	public static File getDockerVolumeFile( final String path, final String containerPath, final String label )
			throws Exception
	{
		final File hostFile = new File( path );
		final String newPath = containerPath + File.separator + hostFile.getName();
		final File newFile = new File( newPath );
		if( !newFile.exists() )
		{
			throw new Exception(
					"Container missing mapped " + label + " volume system path: " + newFile.getAbsolutePath() );
		}
		return newFile;
	}
	
	/**
	 * Return TRUE if running on AWS (based on Config props).
	 * 
	 * @return TRUE if project.env=aws
	 * @throws Exception if errors occur
	 */
	public static boolean runAws() throws Exception
	{
		return Config.requireString( null, Constants.PROJECT_ENV ).equals( Constants.PROJECT_ENV_AWS );
	}

	/**
	 * Return the Docker Image name for the given class name.<br>
	 * Class names contain no spaces, words are separated via CamelCaseConvension.<br>
	 * Docker image names cannot contain upper case letters, so this method substitutes "_" before the lower-case
	 * version of each capital letter.<br>
	 * <br>
	 * Example: JavaModule becomes java_module
	 * 
	 * @param className BioModule class name
	 * @return Image Name
	 * @throws Exception if errors occur
	 */
	public static String getImageName( final String className ) throws Exception
	{
		final StringBuffer imageName = new StringBuffer();
		imageName.append( className.substring( 0, 1 ).toLowerCase() );

		for( int i = 2; i < className.length() + 1; i++ )
		{
			int len = imageName.toString().length();
			final String prevChar = imageName.toString().substring( len - 1, len );
			final String val = className.substring( i - 1, i );
			if( !prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM ) 
					&& val.equals( val.toUpperCase() ) && !NumberUtils.isNumber( val ) )
			{
				imageName.append( IMAGE_NAME_DELIM ).append( val.toLowerCase() );
			}
			else if( prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM ) )
			{
				imageName.append( val.toLowerCase() );
			}
			else if( !prevChar.equals( IMAGE_NAME_DELIM ) )
			{
				imageName.append( val.toLowerCase() );
			}
		}		

		Log.info( DockerUtil.class, "Use Docker image: " + imageName.toString() );

		return imageName.toString();
	}

	/**
	 * Get the Docker image version if defined in the {@link biolockj.Config} file<br>
	 * If not found, return the default version "latest"
	 * 
	 * @param moduleName BioModule name
	 * @return Docker image version
	 * @throws Exception if errors occur
	 */
	public static String getImageVersion( final String moduleName ) throws Exception
	{
		String ver = Config.getString( null, Config.getModuleProp( moduleName, Constants.DOCKER_IMG_VERSION ) );
		if( ver == null )
		{
			ver = DOCKER_LATEST;
		}
		return ver;
	}

	


	/**
	 * Boolean to determine if running Docker manager module.
	 * 
	 * @return TRUE if running the Docker manager module.
	 */
	public static boolean isManager()
	{
		return !RuntimeParamUtil.isDirectMode();
	}


	private static final String getDockerEnvVars( final BioModule module ) throws Exception
	{
		return " -e \"" + COMPUTE_SCRIPT + "=$1\"";
	}

	private static final String getDockerVolumes() throws Exception
	{
		String dockerVolumes = " -v " + DOCKER_SOCKET + ":" + DOCKER_SOCKET;
		dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostInputDir() + ":" + CONTAINER_INPUT_DIR;
		dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostPipelineDir() + ":" + CONTAINER_OUTPUT_DIR
				+ ":delegated";
		dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostConfigDir() + ":" + CONTAINER_CONFIG_DIR;

		if( RuntimeParamUtil.getDockerHostMetaDir() != null )
		{
			dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostMetaDir() + ":" + CONTAINER_META_DIR;
		}
		if( RuntimeParamUtil.getDockerHostPrimerDir() != null )
		{
			dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostPrimerDir() + ":" + CONTAINER_PRIMER_DIR;
		}

		return dockerVolumes;
	}

	private static final String rmFlag() throws Exception
	{
		return Config.getBoolean( null, DELETE_ON_EXIT ) ? " " + DOCK_RM_FLAG: "";
	}


	/**
	 * Docker environment variable holding the name of the compute script file: {@value #COMPUTE_SCRIPT}
	 */
	public static final String COMPUTE_SCRIPT = "COMPUTE_SCRIPT";

	/**
	 * All containers mount the host {@link biolockj.Config} directory to the container"config" volume
	 */
	public static final String CONTAINER_CONFIG_DIR = File.separator + "config";

	/**
	 * All containers mount the host {@value biolockj.Config#INPUT_DIRS} to the container "input" volume
	 */
	public static final String CONTAINER_INPUT_DIR = File.separator + "input";

	/**
	 * Some containers mount the {@value biolockj.util.MetaUtil#META_FILE_PATH} to the container "meta" volume
	 */
	public static final String CONTAINER_META_DIR = File.separator + "meta";

	/**
	 * All containers mount {@value biolockj.Config#PROJECT_PIPELINE_DIR} to the container "pipelines" volume
	 */
	public static final String CONTAINER_OUTPUT_DIR = File.separator + "pipelines";

	/**
	 * Some containers mount the {@value biolockj.module.seq.TrimPrimers#INPUT_TRIM_SEQ_FILE} to the containers "primer"
	 * volume.
	 */
	public static final String CONTAINER_PRIMER_DIR = File.separator + "primer";

	/**
	 * Name of the BioLockJ Docker account ID: {@value #DEFAULT_DOCKER_HUB_USER}
	 */
	public static final String DEFAULT_DOCKER_HUB_USER = "biolockj";

	/**
	 * Docker socket path: {@value #DOCKER_SOCKET}
	 */
	public static final String DOCKER_SOCKET = "/var/run/docker.sock";

	/**
	 * Docker manager module name variable holding the name of the Config file: {@value #MANAGER}
	 */
	public static final String MANAGER = "blj_manager";

	/**
	 * Name of the bash script function used to generate a new Docker container: {@value #SPAWN_DOCKER_CONTAINER}
	 */
	public static final String SPAWN_DOCKER_CONTAINER = "spawnDockerContainer";

	/**
	 * {@link biolockj.Config} property sets --rm flag on docker run command if set to TRUE: {@value #DELETE_ON_EXIT}
	 */
	protected static final String DELETE_ON_EXIT = "docker.deleteContainerOnExit";

	/**
	 * {@link biolockj.Config} name of the Docker Hub user with the BioLockJ containers: {@value #DOCKER_HUB_USER}<br>
	 * Docker Hub URL: <a href="https://hub.docker.com" target="_top">https://hub.docker.com</a><br>
	 * By default the "biolockj" user is used to pull the standard modules, but advanced users can deploy their own
	 * versions of these modules and add new modules in their own Docker Hub account.
	 */
	protected static final String DOCKER_HUB_USER = "docker.user";

	private static final String DOCK_RM_FLAG = "--rm";
	private static final String DOCKER_LATEST = "latest";
	private static final String IMAGE_NAME_DELIM = "_";
}
