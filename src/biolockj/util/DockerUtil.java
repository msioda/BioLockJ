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
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.DatabaseModule;
import biolockj.module.JavaModule;
import biolockj.module.implicit.qiime.BuildQiimeMapping;
import biolockj.module.implicit.qiime.MergeQiimeOtuTables;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.module.report.r.R_Module;
import biolockj.module.seq.*;

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
	public static List<String> buildSpawnDockerContainerFunction( final BioModule module ) throws Exception
	{
		final List<String> lines = new ArrayList<>();
		Log.info( DockerUtil.class, "Docker volumes:" + getDockerVolumes( module ) );

		lines.add( "# Spawn Docker container" );
		lines.add( "function " + SPAWN_DOCKER_CONTAINER + "() {" );
		lines.add( Config.getExe( module, Constants.EXE_DOCKER ) + " run " + rmFlag( module )
				+ getDockerEnvVars( module ) + getDockerVolumes( module ) + getDockerImage( module ) );
		lines.add( "}" + Constants.RETURN );
		return lines;
	}

	/**
	 * Return the name of the Docker image needed for the given module.
	 * 
	 * @param module BioModule
	 * @return Docker image name
	 * @throws Exception if errors occur
	 */
	public static String getDockerImage( final BioModule module ) throws Exception
	{
		final String className = module.getClass().getSimpleName();
		final boolean isQiime = module instanceof BuildQiimeMapping || module instanceof MergeQiimeOtuTables
				|| module instanceof QiimeClassifier;

		final String name = isQiime ? QiimeClassifier.class.getSimpleName()
				: module instanceof R_Module ? R_Module.class.getSimpleName()
						: module instanceof JavaModule ? JavaModule.class.getSimpleName(): className;

		return " " + getDockerUser( className ) + "/" + getImageName( name ) + ":" + getImageVersion( className );
	}

	/**
	 * Return the Docker Hub user ID. If none configured, return biolockj.
	 * 
	 * @param moduleName Calling module
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
	 * Get mapped Docker system File from {@link biolockj.Config} property by replacing the host system path with the
	 * mapped container path.
	 * 
	 * @param prop {@link biolockj.Config} property
	 * @param containerPath Local container path
	 * @return Docker volume file
	 * @throws Exception if errors occur
	 */
	public static File getDockerVolumeFile( final String prop, final String containerPath ) throws Exception
	{
		return new File( getDockerVolumePath( prop, containerPath ) );
	}

	/**
	 * Return the Docker Image name for the given class name.<br>
	 * Return {@value #BLJ_BASH} for simple bash script modules that don't rely on special software<br>
	 * Class names contain no spaces, words are separated via CamelCaseConvension.<br>
	 * Docker image names cannot contain upper case letters, so this method substitutes "_" before the lower-case
	 * version of each capital letter.<br>
	 * <br>
	 * Example: JavaModule becomes java_module
	 * 
	 * @param className BioModule class name
	 * @return Docker Image Name
	 * @throws Exception if errors occur
	 */
	public static String getImageName( final String className ) throws Exception
	{
		String imageName = "";
		if( useBasicBashImg( className ) )
		{
			imageName += BLJ_BASH;
		}
		else
		{
			imageName += className.substring( 0, 1 ).toLowerCase();
			for( int i = 2; i < className.length() + 1; i++ )
			{
				final int len = imageName.toString().length();
				final String prevChar = imageName.toString().substring( len - 1, len );
				final String val = className.substring( i - 1, i );
				if( !prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM )
						&& val.equals( val.toUpperCase() ) && !NumberUtils.isNumber( val ) )
				{
					imageName += IMAGE_NAME_DELIM + val.toLowerCase();
				}
				else if( !prevChar.equals( IMAGE_NAME_DELIM )
						|| prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM ) )
				{
					imageName += val.toLowerCase();
				}
			}

			if( ( className.startsWith( Constants.MODULE_WGS_CLASSIFIER_PACKAGE )
					|| className.contains( KneadData.class.getName() ) ) && hasDB( getShellModule( className ) ) )
			{
				imageName += DB_FREE;
			}
		}

		Log.info( DockerUtil.class,
				"Map: Class [" + className + "] <--> Docker Image [ " + imageName.toString() + " ]" );
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
		String ver = Config.getString( null, Config.getModuleProp( moduleName, DOCKER_IMG_VERSION ) );
		if( ver == null )
		{
			ver = DOCKER_LATEST;
		}
		return ver;
	}

	/**
	 * Function used to determine if an alternate database has been defined (other than /db).
	 * 
	 * @param module BioModule
	 * @return TRUE if module has a custom DB defined
	 * @throws Exception if errors occur
	 */
	public static final boolean hasDB( final BioModule module ) throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() && module instanceof DatabaseModule )
		{
			final String db = ( (DatabaseModule) module ).getDB().getAbsolutePath();
			return !db.equals( CONTAINER_DB_DIR );
		}

		return false;
	}

	/**
	 * Return TRUE if running on AWS (based on Config props).
	 * 
	 * @return TRUE if project.env=aws
	 * @throws Exception if errors occur
	 */
	public static boolean inAwsEnv() throws Exception
	{
		return Config.requireString( null, Constants.PIPELINE_ENV ).equals( Constants.PIPELINE_ENV_AWS );
	}

	/**
	 * Boolean to determine if should initialize Docker aws_manager
	 * 
	 * @return TRUE if running Docker aws_manager in init mode
	 * @throws Exception if unable to determine Docker module type
	 */
	public static boolean initAwsCloudManager() throws Exception
	{
		return RuntimeParamUtil.isDockerMode() && !RuntimeParamUtil.isDirectMode() && inAwsEnv()
				&& !RuntimeParamUtil.runAws();
	}

	/**
	 * Boolean to determine if running Docker blj_manager
	 * 
	 * @return TRUE if running Docker blj_manager
	 * @throws Exception if unable to determine Docker module type
	 */
	public static boolean isBljManager() throws Exception
	{
		return RuntimeParamUtil.isDockerMode() && !RuntimeParamUtil.isDirectMode() && !inAwsEnv();
	}

	/**
	 * Boolean to determine if running Docker aws_manager
	 * 
	 * @return TRUE if running Docker aws_manager
	 * @throws Exception if unable to determine Docker module type
	 */
	public static boolean runAwsCloudManager() throws Exception
	{
		return RuntimeParamUtil.isDockerMode() && !RuntimeParamUtil.isDirectMode() && inAwsEnv()
				&& RuntimeParamUtil.runAws();
	}

	private static final String getDockerEnvVars( final BioModule module ) throws Exception
	{
		return " -e \"" + COMPUTE_SCRIPT + "=$1\"";
	}

	private static String getDockerVolumePath( final String prop, final String containerPath ) throws Exception
	{
		final File hostFile = new File( Config.requireString( null, prop ) );
		return containerPath + File.separator + hostFile.getName();
	}

	private static final String getDockerVolumes( final BioModule module ) throws Exception
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

		if( module instanceof TrimPrimers )
		{
			final File primers = new File( Config.getSystemFilePath( Config.requireString( module, TrimPrimers.INPUT_TRIM_SEQ_FILE ) ) ).getParentFile();
			dockerVolumes += " -v " + primers.getAbsolutePath() + ":" + CONTAINER_PRIMER_DIR;
		}

		if( hasDB( module ) )
		{
			String db = ( (DatabaseModule) module ).getDB().getAbsolutePath();
			if( db.startsWith( DOCKER_ROOT_HOME ) )
			{
				db = db.replace( DOCKER_ROOT_HOME, "~" );
			}
			
			dockerVolumes += " -v " + db + ":" + CONTAINER_DB_DIR;
		}

		return dockerVolumes;
	}

	private static BioModule getShellModule( final String className ) throws Exception
	{
		return (BioModule) Class.forName( className ).getDeclaredConstructor().newInstance();
	}

	private static final String rmFlag( final BioModule module ) throws Exception
	{
		return Config.getBoolean( module, SAVE_CONTAINER_ON_EXIT ) ? "": DOCK_RM_FLAG;
	}

	private static boolean useBasicBashImg( final String className ) throws Exception
	{
		return className.contains( PearMergeReads.class.getSimpleName() )
				|| className.contains( AwkFastaConverter.class.getSimpleName() )
				|| className.contains( Gunzipper.class.getSimpleName() );
	}

	/**
	 * All containers mount the host {@link biolockj.Config} directory to the container"config" volume
	 */
	public static final String CONTAINER_CONFIG_DIR = File.separator + "config";

	/**
	 * Some containers mount a database to the containers "db" volume.
	 */
	public static final String CONTAINER_DB_DIR = File.separator + "db";

	/**
	 * All containers mount the host {@value biolockj.Constants#INPUT_DIRS} to the container "input" volume
	 */
	public static final String CONTAINER_INPUT_DIR = File.separator + "input";

	/**
	 * Some containers mount the {@value biolockj.util.MetaUtil#META_FILE_PATH} to the container "meta" volume
	 */
	public static final String CONTAINER_META_DIR = File.separator + "meta";

	/**
	 * All containers mount {@value biolockj.Constants#PIPELINE_DIR} to the container "pipelines" volume
	 */
	public static final String CONTAINER_OUTPUT_DIR = File.separator + "pipelines";

	/**
	 * Some containers mount the {@value biolockj.module.seq.TrimPrimers#INPUT_TRIM_SEQ_FILE} to the containers "primer"
	 * volume.
	 */
	public static final String CONTAINER_PRIMER_DIR = File.separator + "primer";

	/**
	 * Name of the bash script function used to generate a new Docker container: {@value #SPAWN_DOCKER_CONTAINER}
	 */
	public static final String SPAWN_DOCKER_CONTAINER = "spawnDockerContainer";

	

	/**
	 * Docker image name for simple bash scripts (awk,gzip,pear).
	 */
	protected static final String BLJ_BASH = "blj_bash";

	/**
	 * Docker environment variable holding the name of the compute script file: {@value #COMPUTE_SCRIPT}
	 */
	protected static final String COMPUTE_SCRIPT = "COMPUTE_SCRIPT";

	/**
	 * Name of the BioLockJ Docker account ID: {@value #DEFAULT_DOCKER_HUB_USER}
	 */
	protected static final String DEFAULT_DOCKER_HUB_USER = "biolockj";

	/**
	 * {@link biolockj.Config} name of the Docker Hub user with the BioLockJ containers: {@value #DOCKER_HUB_USER}<br>
	 * Docker Hub URL: <a href="https://hub.docker.com" target="_top">https://hub.docker.com</a><br>
	 * By default the "biolockj" user is used to pull the standard modules, but advanced users can deploy their own
	 * versions of these modules and add new modules in their own Docker Hub account.
	 */
	protected static final String DOCKER_HUB_USER = "docker.user";

	/**
	 * {@link biolockj.Config} property removed the default --rm flag on docker run command if set to TRUE:
	 * {@value #SAVE_CONTAINER_ON_EXIT}
	 */
	protected static final String SAVE_CONTAINER_ON_EXIT = "docker.saveContainerOnExit";

	/**
	 * Docker container root user $HOME directory
	 */
	public static final String DOCKER_ROOT_HOME = "/root";
	private static final String DB_FREE = "_dbfree";
	private static final String DOCK_RM_FLAG = "--rm";
	private static final String DOCKER_IMG_VERSION = "docker.imgVersion";
	private static final String DOCKER_LATEST = "latest";
	private static final String DOCKER_SOCKET = "/var/run/docker.sock";
	private static final String IMAGE_NAME_DELIM = "_";
}
