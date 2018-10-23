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
import biolockj.module.implicit.qiime.MergeOtuTables;
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
	 * @param containerName Name of Docker container
	 * @return Bash script lines for the docker script
	 * @throws Exception if errors occur
	 */
	public static List<List<String>> buildDockerScript() throws Exception
	{
		final List<List<String>> dockerScriptLines = new ArrayList<>();
		final List<String> innerList = new ArrayList<>();
		innerList.add( DOCKER_RUN );
		dockerScriptLines.add( innerList );
		return dockerScriptLines;
	}

	/**
	 * Build the {@value #DOCKER_RUN} method, which takes container name, in/out port, and optionally script path
	 * parameters.
	 * 
	 * @param module BioModule
	 * @return Bash function to run docker
	 * @throws Exception if unable to build the function
	 */
	public static List<String> buildRunDockerFunction( final BioModule module ) throws Exception
	{

		final String rmFlag = Config.getBoolean( DELETE_ON_EXIT ) ? " " + DOCK_RM_FLAG: "";

		final StringBuffer opts = new StringBuffer();
		opts.append( BLJ_OPTIONS + "=\""
				+ RuntimeParamUtil.getRuntimeArgs().replaceAll( RuntimeParamUtil.RESTART_FLAG + " ", "" ) );

		final StringBuffer vols = new StringBuffer();
		//vols.append( " -v " + DOCKER_SOCKET + ":" + DOCKER_SOCKET );
		vols.append( " -v " + RuntimeParamUtil.getDockerHostInputDir() + ":" + CONTAINER_INPUT_DIR );
		vols.append( " -v " + RuntimeParamUtil.getDockerHostPipelineDir() + ":" + CONTAINER_OUTPUT_DIR + ":delegated" );
		vols.append( " -v " + RuntimeParamUtil.getDockerHostConfigDir() + ":" + CONTAINER_CONFIG_DIR );

		String miscParams = "";
		if( isDockerScriptModule( module ) )
		{
			miscParams = " -e " + COMPUTE_SCRIPT + "=$4";
		}
		else
		{
			opts.append( " " + RuntimeParamUtil.DIRECT_FLAG + " " + module.getClass().getName() );
		}

		if( RuntimeParamUtil.getDockerHostMetaDir() != null )
		{
			vols.append( " -v " + RuntimeParamUtil.getDockerHostMetaDir() + ":" + CONTAINER_META_DIR );
		}
		if( RuntimeParamUtil.getDockerHostPrimerDir() != null )
		{
			vols.append( " -v " + RuntimeParamUtil.getDockerHostPrimerDir() + ":" + CONTAINER_PRIMER_DIR );
		}

		opts.append( "\"" );

		final String bljOptions = " -e \"" + BLJ_OPTIONS + "=$" + BLJ_OPTIONS + "\"";

		Log.info( DockerUtil.class, "Docker volumes: " + vols.toString() + BioLockJ.RETURN );
		Log.info( DockerUtil.class, "Docker environment var: " + opts.toString() + BioLockJ.RETURN );

		final List<String> lines = new ArrayList<>();
		lines.add( opts.toString() );
		lines.add( "function " + DOCKER_RUN + "() {" );
		lines.add( Config.getExe( Config.EXE_DOCKER ) + " run " + miscParams + rmFlag + bljOptions
				+ vols.toString() + " " + getDockerImage( module ) );
		lines.add( "}" );
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
	 * Get the name of the Docker image
	 * 
	 * @param module BioModule
	 * @return Docker image name
	 */
	protected static String getDockerImage( final BioModule module )
	{
		final boolean isQiime = module instanceof BuildQiimeMapping || module instanceof MergeOtuTables
				|| module instanceof QiimeClassifier;

		final boolean isR = module instanceof R_Module;

		final String name = isR ? R_Module.class.getSimpleName()
				: isQiime ? QiimeClassifier.class.getSimpleName()
						: isDockerScriptModule( module ) ? module.getClass().getSimpleName()
								: JavaModule.class.getSimpleName();

		return DOCKER_USER + "/" + name.toLowerCase();
	}

	

	/**
	 * Docker environment variable holding the Docker program switches: {@value #BLJ_OPTIONS}
	 */
	public static final String BLJ_OPTIONS = "BLJ_OPTIONS";

	/**
	 * Docker environment variable holding the name of the compute script file: {@value #COMPUTE_SCRIPT}
	 */
	public static final String COMPUTE_SCRIPT = "BLJ_SCRIPT";

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
	 * All containers mount {@value biolockj.Config#INTERNAL_PIPELINE_DIR} to the container "pipeline" volume
	 */
	public static final String CONTAINER_OUTPUT_DIR = File.separator + "pipeline";

	/**
	 * Some containers mount the {@value biolockj.module.seq.TrimPrimers#INPUT_TRIM_SEQ_FILE} to the container "primer"
	 * volume
	 */
	public static final String CONTAINER_PRIMER_DIR = File.separator + "primer";

	/**
	 * Name of the bash script function used to launch a new Docker module: {@value #DOCKER_RUN}
	 */
	public static final String DOCKER_RUN = "runDocker";

	/**
	 * Docker socket path: {@value #DOCKER_SOCKET}
	 */
	public static final String DOCKER_SOCKET = "/var/run/docker.sock";

	/**
	 * Name of the BioLockJ Docker account ID: {@value #DOCKER_USER}
	 */
	public static final String DOCKER_USER = "biolockj";

	/**
	 * Docker manager module name variable holding the name of the Config file: {@value #MANAGER}
	 */
	public static final String MANAGER = "manager";

	/**
	 * {@link biolockj.Config} property sets --rm flag on docker run command if set to TRUE: {@value #DELETE_ON_EXIT}
	 */
	protected static final String DELETE_ON_EXIT = "docker.deleteContainerOnExit";

	private static final String DOCK_RM_FLAG = "--rm";


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
