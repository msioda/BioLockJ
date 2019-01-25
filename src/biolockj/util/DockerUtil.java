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
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.implicit.qiime.BuildQiimeMapping;
import biolockj.module.implicit.qiime.MergeQiimeOtuTables;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.module.r.R_Module;

/**
 * DockerUtil for Docker integration.
 */
public class DockerUtil
{
	/**
	 * Build a docker bash script.
	 * 
	 * @return Bash script lines for the docker script
	 * @throws Exception if errors occur
	 */
	public static List<List<String>> buildDockerScript() throws Exception
	{
		final List<List<String>> dockerScriptLines = new ArrayList<>();
		final List<String> innerList = new ArrayList<>();
		innerList.add( SPAWN_DOCKER_CONTAINER );
		dockerScriptLines.add( innerList );
		return dockerScriptLines;
	}

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
		if( isDockerJavaModule( module ) )
		{
			lines.add( getBljOptions( module ) + BioLockJ.RETURN );
			Log.info( DockerUtil.class, "BioLockJ parameters: " + getBljOptions( module ) + BioLockJ.RETURN );
		}

		Log.info( DockerUtil.class, "Docker volumes:" + getDockerVolumes() + BioLockJ.RETURN );

		Log.info( DockerUtil.class, "Docker Environment variables:" + getDockerEnvVars( module ) + BioLockJ.RETURN );

		lines.add( "# Spawn Docker container" );
		lines.add( "function " + SPAWN_DOCKER_CONTAINER + "() {" );
		lines.add( Config.getExe( Config.EXE_DOCKER ) + " run" + rmFlag() + getDockerEnvVars( module )
				+ getDockerVolumes() + getDockerImage( module ) );
		lines.add( "}" + BioLockJ.RETURN );
		return lines;
	}

	/**
	 * Get mapped Docker system path from {@link biolockj.Config} property by replacing the host system path with the
	 * mapped container path.
	 * 
	 * @param prop {@link biolockj.Config} property
	 * @param containerPath Local container path
	 * @param label Path label
	 * @return Docker volume file object
	 * @throws Exception if errors occur
	 */
	public static File getDockerVolumeFile( final String prop, final String containerPath, final String label )
			throws Exception
	{
		final File hostFile = new File( Config.getString( prop ) );
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
	 * Method to indicate module is a Docker Java module
	 * 
	 * @param module BioModule
	 * @return TRUE if module is a Docker Java module
	 */
	public static boolean isDockerJavaModule( final BioModule module )
	{
		return RuntimeParamUtil.isDockerMode() && module instanceof JavaModule;
	}

	/**
	 * Boolean to determine if running Docker compute node. Return TRUE if inDocker() and not a JavaModule.
	 * 
	 * @param module BioModule
	 * @return TRUE if running in a Docker container.
	 */
	public static boolean isDockerScriptModule( final BioModule module )
	{
		return RuntimeParamUtil.isDockerMode() && !isDockerJavaModule( module );
	}

	/**
	 * Boolean to determine if running Docker manager module.
	 * 
	 * @return TRUE if running the Docker manager module.
	 */
	public static boolean isManager()
	{
		return RuntimeParamUtil.getDockerContainerName() != null
				&& RuntimeParamUtil.getDockerContainerName().equals( MANAGER );
	}

	/**
	 * Get the name of the Docker image.
	 * 
	 * @param module BioModule
	 * @return Docker image name
	 * @throws Exception if errors occur
	 */
	protected static String getDockerImage( final BioModule module ) throws Exception
	{
		final boolean isQiime = module instanceof BuildQiimeMapping || module instanceof MergeQiimeOtuTables
				|| module instanceof QiimeClassifier;

		final boolean isR = module instanceof R_Module;
		
		final String name = isR ? R_Module.class.getSimpleName()
				: isQiime ? QiimeClassifier.class.getSimpleName()
						: isDockerScriptModule( module ) ? module.getClass().getSimpleName()
								: JavaModule.class.getSimpleName();

		return " " + dockerHubUser( name ) + "/" + buildImageName( name );
	}
	
	private static String dockerHubUser( String className ) throws Exception
	{
		String user = Config.getString( DOCKER_HUB_USER );
		if( user == null )
		{
			user = DEFAULT_DOCKER_HUB_USER;
		}
		
		return user;
	}
	
	
	private static String buildImageName( String className ) throws Exception
	{
		StringBuffer imageName = new StringBuffer();
		imageName.append( className.substring( 0, 1 ).toLowerCase() );

		for( int i=2; i<className.length(); i++ )
		{
			String val = className.substring( i-1, i );
			String upperCase = val.toUpperCase();
			if( val.equals( upperCase ) )
			{
				imageName.append( DOCK_IMAGE_NAME_DELIM );
			}
		}
		
		Log.info( DockerUtil.class, "User Docker image name: " + imageName.toString() );

		return imageName.toString();
	}

	private static String getBljOptions( final BioModule module ) throws Exception
	{
		final String args = RuntimeParamUtil.getDockerRuntimeArgs() + " " + BioLockJUtil.getDirectModuleParam( module );

		return BLJ_OPTIONS + "=\"" + args + "\"";
	}

	private static final String getDockerEnvVars( final BioModule module ) throws Exception
	{
		if( isDockerScriptModule( module ) )
		{
			return " -e \"" + COMPUTE_SCRIPT + "=$1\"";
		}

		return " -e \"" + BLJ_OPTIONS + "=$" + BLJ_OPTIONS + "\"";
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
		return Config.getBoolean( DELETE_ON_EXIT ) ? " " + DOCK_RM_FLAG: "";
	}

	/**
	 * Docker environment variable holding the Docker program switches: {@value #BLJ_OPTIONS}
	 */
	public static final String BLJ_OPTIONS = "BLJ_OPTIONS";

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
	 * All containers mount {@value biolockj.Config#INTERNAL_PIPELINE_DIR} to the container "pipelines" volume
	 */
	public static final String CONTAINER_OUTPUT_DIR = File.separator + "pipelines";

	/**
	 * Some containers mount the {@value biolockj.module.seq.TrimPrimers#INPUT_TRIM_SEQ_FILE} to the containers "primer"
	 * volume.
	 */
	public static final String CONTAINER_PRIMER_DIR = File.separator + "primer";

	/**
	 * Docker socket path: {@value #DOCKER_SOCKET}
	 */
	public static final String DOCKER_SOCKET = "/var/run/docker.sock";

	/**
	 * Name of the BioLockJ Docker account ID: {@value #DEFAULT_DOCKER_HUB_USER}
	 */
	public static final String DEFAULT_DOCKER_HUB_USER = "biolockj";

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
	protected static final String DOCKER_HUB_USER = "docker.dockerHubUser";

	private static final String DOCK_RM_FLAG = "--rm";
	private static final String DOCK_IMAGE_NAME_DELIM = "_";

	/**
	 * Update Config file paths to use the container paths in place of host paths
	 * 
	 * @throws Exception if errors occur
	 * 
	 * public static void updateDockerConfig() throws Exception { if( RuntimeParamUtil.getDockerHostMetaDir() != null )
	 * { updateDockerProperty( MetaUtil.META_FILE_PATH, CONTAINER_META_DIR );
	 * 
	 * } if( RuntimeParamUtil.getDockerHostPrimerDir() != null ) { updateDockerProperty(
	 * TrimPrimers.INPUT_TRIM_SEQ_FILE, CONTAINER_PRIMER_DIR ); } }
	 */

	/**
	 * Update a Config property by setting the container path in place of the host path.
	 * 
	 * @param prop Config property
	 * @param containerDir Container directory path
	 * @throws Exception if errors occur
	 *
	 * protected static void updateDockerProperty( final String prop, final String containerDir ) throws Exception {
	 * final File hostFile = new File( Config.requireString( prop ) ); final String newPath = File.separator +
	 * containerDir + File.separator + hostFile.getName(); Config.requireExistingFile( newPath );
	 * Config.setConfigProperty( prop, newPath ); }
	 */

}
