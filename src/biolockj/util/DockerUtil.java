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
import biolockj.module.DatabaseModule;
import biolockj.module.JavaModule;
import biolockj.module.classifier.r16s.RdpClassifier;
import biolockj.module.implicit.qiime.BuildQiimeMapping;
import biolockj.module.implicit.qiime.MergeQiimeOtuTables;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.module.report.r.R_Module;
import biolockj.module.seq.*;

/**
 * DockerUtil for Docker integration.
 */
public class DockerUtil {
	
	/**
	 * Build the {@value #SPAWN_DOCKER_CONTAINER} method, which takes container name, in/out port, and optionally script
	 * path parameters.
	 * 
	 * @param module BioModule
	 * @return Bash function to run docker
	 * @throws Exception if unable to build the function
	 */
	public static List<String> buildSpawnDockerContainerFunction( final BioModule module ) throws Exception {
		final List<String> lines = new ArrayList<>();
		final String cmd = Config.getExe( module, Constants.EXE_DOCKER ) + " run " + rmFlag( module ) + getDockerEnvVars( module )
			+ getDockerVolumes( module ) + getDockerImage( module );
		Log.debug( DockerUtil.class, "Docker CMD:" + cmd );
		lines.add( "# Spawn Docker container" );
		lines.add( "function " + SPAWN_DOCKER_CONTAINER + "() {" );
		lines.add( cmd );
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
	public static String getDockerImage( final BioModule module ) throws Exception {
		return " " + getDockerUser( module ) + "/" + getImageName( module ) + ":" + getImageVersion( module );
	}

	/**
	 * Return the Docker Hub user ID. If none configured, return biolockj.
	 * 
	 * @param module BioModule
	 * @return Docker Hub User ID
	 */
	public static String getDockerUser( final BioModule module ) {
		String user = Config.getString( null, Config.getModuleProp( module, DOCKER_HUB_USER ) );
		user = user == null ? DEFAULT_DOCKER_HUB_USER: user;
		return user;
	}

	/**
	 * Get mapped Docker system File from {@link biolockj.Config} directory-property by replacing the host system path
	 * with the mapped container path.
	 * 
	 * @param prop {@link biolockj.Config} directory-property
	 * @param containerPath Local container path
	 * @return Docker volume directory or null
	 * @throws Exception if path is defined but is not an existing directory or if runtime-errors occur
	 */
	public static File getDockerVolumeDir( final String prop, final String containerPath ) throws Exception {
		return Config.getExistingDir( null, getDockerVolumePath( Config.requireString( null, prop ), containerPath ) );
	}

	/**
	 * Get mapped Docker system File from {@link biolockj.Config} file-property by replacing the host system path with
	 * the mapped container path.
	 * 
	 * @param prop {@link biolockj.Config} file-property
	 * @param containerPath Local container path
	 * @return Docker volume file or null
	 * @throws Exception if path is defined but is not an existing file or if runtime-errors occur
	 */
	public static File getDockerVolumeFile( final String prop, final String containerPath ) throws Exception {
		return Config.getExistingFile( null, getDockerVolumePath( Config.requireString( null, prop ), containerPath ) );
	}

	/**
	 * Get Docker file path through mapped volume
	 * 
	 * @param path {@link biolockj.Config} file or directory path
	 * @param containerPath Local container path
	 * @return Docker file path
	 */
	public static String getDockerVolumePath( final String path, final String containerPath ) {
		if( path == null || path.isEmpty() ) return null;
		return containerPath + File.separator + path.substring( path.lastIndexOf( File.separator ) );
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
	 * @param module BioModule
	 * @return Docker Image Name
	 * @throws Exception if errors occur
	 */
	public static String getImageName( final BioModule module ) throws Exception {
		final String className = module.getClass().getName();
		if( useBasicBashImg( module ) ) {
			Log.info( DockerUtil.class, "Map: Class [" + className + "] <--> Docker Image [ " + BLJ_BASH + " ]" );
			return BLJ_BASH;
		}

		final String simpleName = getDockerClassName( module );
		Log.debug( DockerUtil.class, "Found Java simple class name: " + simpleName );
		String imageName = simpleName.substring( 0, 1 ).toLowerCase();

		for( int i = 2; i < simpleName.length() + 1; i++ ) {
			final int len = imageName.toString().length();
			final String prevChar = imageName.toString().substring( len - 1, len );
			final String val = simpleName.substring( i - 1, i );
			if( !prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM )
				&& val.equals( val.toUpperCase() ) && !NumberUtils.isNumber( val ) ) {
				imageName += IMAGE_NAME_DELIM + val.toLowerCase();
			} else if( !prevChar.equals( IMAGE_NAME_DELIM )
				|| prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM ) ) {
				imageName += val.toLowerCase();
			}
		}

		if( ( className.startsWith( Constants.MODULE_WGS_CLASSIFIER_PACKAGE )
			|| className.equals( KneadData.class.getName() ) ) && hasDB( module ) ) {
			imageName += DB_FREE;
		}

		Log.info( DockerUtil.class, "Map: Class [" + className + "] <--> Docker Image [ " + imageName + " ]" );

		return imageName;
	}

	/**
	 * Get the Docker image version if defined in the {@link biolockj.Config} file<br>
	 * If not found, return the default version "latest"
	 * 
	 * @param module BioModule
	 * @return Docker image version
	 */
	public static String getImageVersion( final BioModule module ) {
		String ver = Config.getString( null, Config.getModuleProp( module, DOCKER_IMG_VERSION ) );
		if( ver == null ) {
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
	public static final boolean hasDB( final BioModule module ) throws Exception {
		if( DockerUtil.inDockerEnv() && module instanceof DatabaseModule ) {
			final File db = ( (DatabaseModule) module ).getDB();
			if( db != null ) return !db.getAbsolutePath().equals( CONTAINER_DB_DIR );
		}

		return false;
	}

	/**
	 * Return TRUE if running in AWS (based on Config props).
	 * 
	 * @return TRUE if pipeline.env=aws
	 * @throws Exception if errors occur
	 */
	public static boolean inAwsEnv() throws Exception {
		return RuntimeParamUtil.isAwsMode();
	}

	/**
	 * Return TRUE if Java program passed {@value biolockj.util.RuntimeParamUtil#DOCKER_FLAG}.
	 * 
	 * @return TRUE if Java running in Docker container
	 */
	public static boolean inDockerEnv() {
		return RuntimeParamUtil.isDockerMode();
	}

	/**
	 * Return TRUE if runtime parameters indicate attempt to run in direct mode
	 * 
	 * @return boolean
	 */
	public static boolean isDirectMode() {
		return RuntimeParamUtil.getDirectModuleDir() != null;
	}

	private static String getDockerClassName( final BioModule module ) {
		final String className = module.getClass().getSimpleName();
		final boolean isQiime = module instanceof BuildQiimeMapping || module instanceof MergeQiimeOtuTables
			|| module instanceof QiimeClassifier;

		return isQiime ? QiimeClassifier.class.getSimpleName()
			: module instanceof R_Module ? R_Module.class.getSimpleName()
				: module instanceof JavaModule ? JavaModule.class.getSimpleName(): className;
	}

	private static String getDockerEnvVars( final BioModule module ) {
		return " -e \"" + COMPUTE_SCRIPT + "=$1\"";
	}

	private static String getDockerVolumes( final BioModule module ) throws Exception {
		Log.debug( DockerUtil.class, "Build Docker volumes for module: " + module.getClass().getSimpleName() );

		String dockerVolumes = " -v " + DOCKER_SOCKET + ":" + DOCKER_SOCKET;
		dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostInputDir() + ":" + CONTAINER_INPUT_DIR;
		dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostPipelineDir() + ":" + CONTAINER_OUTPUT_DIR
			+ ":delegated";
		dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostConfigDir() + ":" + CONTAINER_CONFIG_DIR;

		if( RuntimeParamUtil.getDockerHostMetaDir() != null ) {
			dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostMetaDir() + ":" + CONTAINER_META_DIR;
		}
		
		if( isJavaModule( module ) && RuntimeParamUtil.getDockerHostBLJ() != null ) {
			dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostBLJ().getAbsolutePath() + ":" + CONTAINER_BLJ_DIR;
		}

		if( isJavaModule( module ) && RuntimeParamUtil.getDockerHostBLJ_SUP() != null ) {
			dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostBLJ_SUP().getAbsolutePath() + ":"
				+ CONTAINER_BLJ_SUP_DIR;
		}

		if( module instanceof TrimPrimers ) {
			final File primers = new File( Config.requireString( module, Constants.INPUT_TRIM_SEQ_FILE ) )
				.getParentFile();
			dockerVolumes += " -v " + primers.getAbsolutePath() + ":" + CONTAINER_PRIMER_DIR;
		}

		if( hasDB( module ) ) {
			final File db = ( (DatabaseModule) module ).getDB();
			String dbPath = db.getAbsolutePath();
			Log.info( DockerUtil.class, "Map Docker volume for DB: " + dbPath );

			if( module instanceof RdpClassifier ) {
				dbPath = db.getParentFile().getAbsolutePath();
				Log.info( DockerUtil.class, "RDP DB directory path: " + dbPath );
			}

			if( dbPath.startsWith( DOCKER_ROOT_HOME ) ) {
				dbPath = dbPath.replace( DOCKER_ROOT_HOME,
					RuntimeParamUtil.getDockerHostHomeUserDir().getAbsolutePath() );
				Log.info( DockerUtil.class, "Replace " + DOCKER_ROOT_HOME + " with DB Host dir: " + dbPath );
			}

			dockerVolumes += " -v " + dbPath + ":" + CONTAINER_DB_DIR;
		}

		return dockerVolumes;
	}

	private static boolean isJavaModule( final BioModule module ) {
		return getDockerClassName( module ).equals( JavaModule.class.getSimpleName() );
	}

	private static final String rmFlag( final BioModule module ) throws Exception {
		return Config.getBoolean( module, SAVE_CONTAINER_ON_EXIT ) ? "": DOCK_RM_FLAG;
	}

	private static boolean useBasicBashImg( final BioModule module ) {
		return module instanceof PearMergeReads || module instanceof AwkFastaConverter || module instanceof Gunzipper;
	}
	
//	private static List<String> DOCKER_CONFIG_DIRS = Arrays.asList( 
//		new String[] { MetaUtil.META_FILE_PATH, Constants.INPUT_TRIM_SEQ_FILE, Constants.INPUT_DIRS } );

	
	/**
	 * Docker container blj_support dir for dev support
	 */
	public static final String CONTAINER_BLJ_SUP_DIR = "/app/blj_support";
	
	/**
	 * Docker container blj_support dir for dev support
	 */
	public static final String CONTAINER_BLJ_DIR = "/app/biolockj";
	
	/**
	 * Docker container root user EFS directory
	 */
	public static final String AWS_EFS = "/mnt/efs";

	/**
	 * Docker container root user DB directory
	 */
	public static final String AWS_EFS_DB = AWS_EFS + "/db";

	/**
	 * All containers mount the host {@link biolockj.Config} directory to the container volume:
	 * {@value #CONTAINER_CONFIG_DIR}
	 */
	public static final String CONTAINER_CONFIG_DIR = AWS_EFS + "/config";

	/**
	 * Some containers mount a database to the containers "db" volume: {@value #CONTAINER_DB_DIR}
	 */
	public static final String CONTAINER_DB_DIR = AWS_EFS + "/db";

	/**
	 * All containers mount the host {@value biolockj.Constants#INPUT_DIRS} to the container "input" volume:
	 * {@value #CONTAINER_INPUT_DIR}
	 */
	public static final String CONTAINER_INPUT_DIR = AWS_EFS + "/input";

	/**
	 * Some containers mount the {@value biolockj.util.MetaUtil#META_FILE_PATH} to the container "meta" volume:
	 * {@value #CONTAINER_META_DIR}
	 */
	public static final String CONTAINER_META_DIR = AWS_EFS + "/metadata";

	/**
	 * All containers mount {@value biolockj.Constants#INTERNAL_PIPELINE_DIR} to the container volume:
	 * {@value #CONTAINER_OUTPUT_DIR}
	 */
	public static final String CONTAINER_OUTPUT_DIR = AWS_EFS + "/pipelines";

	/**
	 * Some containers mount the {@value biolockj.Constants#INPUT_TRIM_SEQ_FILE} to the containers "primer":
	 * {@value #CONTAINER_PRIMER_DIR} volume.
	 */
	public static final String CONTAINER_PRIMER_DIR = AWS_EFS + "/primer";

	/**
	 * Docker container root user $HOME directory
	 */
	public static final String DOCKER_ROOT_HOME = "/root";

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
	private static final String DB_FREE = "_dbfree";
	private static final String DOCK_RM_FLAG = "--rm";
	private static final String DOCKER_IMG_VERSION = "docker.imgVersion";
	private static final String DOCKER_LATEST = "latest";
	private static final String DOCKER_SOCKET = "/var/run/docker.sock";
	private static final String IMAGE_NAME_DELIM = "_";
}
