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
import java.util.*;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.*;
import biolockj.exception.*;
import biolockj.module.*;
import biolockj.module.classifier.r16s.RdpClassifier;
import biolockj.module.implicit.qiime.*;
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
	 * @throws ConfigNotFoundException If required {@link biolockj.Config} properties are undefined
	 * @throws ConfigViolationException If {@value biolockj.Constants#EXE_DOCKER} property name does not start with
	 * prefix "exe."
	 * @throws ConfigFormatException If {@value #SAVE_CONTAINER_ON_EXIT} property value is not set as a boolean
	 * {@value biolockj.Constants#TRUE} or {@value biolockj.Constants#FALSE}
	 * @throws ConfigPathException If mounted Docker volumes are not found on host or container file-system
	 */
	public static List<String> buildSpawnDockerContainerFunction( final BioModule module )
		throws ConfigViolationException, ConfigNotFoundException, ConfigFormatException, ConfigPathException {
		final List<String> lines = new ArrayList<>();
		final String cmd = Config.getExe( module, Constants.EXE_DOCKER ) + " run " + rmFlag( module ) +
			getDockerEnvVars() + " " + getDockerVolumes( module ) + getDockerImage( module );
		Log.debug( DockerUtil.class, "----> Docker CMD:" + cmd );
		lines.add( "# Spawn Docker container" );
		lines.add( "function " + SPAWN_DOCKER_CONTAINER + "() {" );
		lines.add( cmd );
		lines.add( "echo \"Docker container " + module.getClass().getSimpleName() + " execution complete\"" );
		lines.add( "}" + Constants.RETURN );
		return lines;
	}

	/**
	 * Download a database for a Docker container
	 * 
	 * @param args Terminal command + args
	 * @param label Log file identifier for subprocess
	 * @return Thread ID
	 */
	public static Long downloadDB( final String[] args, final String label ) {
		if( downloadDbCmdRegister.contains( args ) ) {
			Log.warn( DockerUtil.class,
				"Ignoring duplicate download request - already downloading Docker DB: " + label );
			return null;
		}

		downloadDbCmdRegister.add( args );
		return Processor.runSubprocess( args, label ).getId();
	}

	/**
	 * Get Config file path - update for Docker env or bash env var references as needed.
	 * 
	 * @param path Runtime arg or Config property path
	 * @return Local File
	 * @throws ConfigPathException if errors occur due to invalid file path
	 */
	public static File getConfigFile( final String path ) throws ConfigPathException {
		final File config = getDockerVolumePath( path, DOCKER_CONFIG_DIR );
		if( !config.isFile() ) throw new ConfigPathException( config, "Config file not found in Docker container" );
		return config;
	}

	/**
	 * Get the Docker container database found under the DockerDB directory or one of it's sub-directories.
	 * 
	 * @param module DatabaseModule
	 * @param dbPath Database file or sub-directory under the main Docker $BLJ_DB directory.
	 * @return Container database directory
	 * @throws ConfigPathException if DB property not found
	 * @throws ConfigNotFoundException if path is defined but is not an existing directory
	 */
	public static File getDockerDB( final DatabaseModule module, final String dbPath )
		throws ConfigPathException, ConfigNotFoundException {
		if( hasCustomDockerDB( module ) ) {
			if( dbPath == null ) return new File( DOCKER_DB_DIR );
			if( inAwsEnv() ) return new File( dbPath );
			return new File( dbPath.replace( getDbDirPath( module ), DockerUtil.DOCKER_DB_DIR ) );
		}

		if( dbPath == null || module.getDB() == null ) return new File( DOCKER_DEFAULT_DB_DIR );
		if( inAwsEnv() ) return new File( dbPath );
		return new File( dbPath.replace( getDbDirPath( module ), DockerUtil.DOCKER_DEFAULT_DB_DIR ) );
	}

	/**
	 * Return the name of the Docker image needed for the given module.
	 * 
	 * @param module BioModule
	 * @return Docker image name
	 * @throws ConfigNotFoundException if Docker image version is undefined
	 */
	public static String getDockerImage( final BioModule module ) throws ConfigNotFoundException {
		return " " + getDockerUser( module ) + "/" + getImageName( module ) + ":" +
			Config.requireString( module, DockerUtil.DOCKER_IMG_VERSION );
	}

	/**
	 * Return the Docker Hub user ID. If none configured, return biolockj.
	 * 
	 * @param module BioModule
	 * @return Docker Hub User ID
	 */
	public static String getDockerUser( final BioModule module ) {
		final String user = Config.getString( module, DOCKER_HUB_USER );
		if( user == null ) return DEFAULT_DOCKER_HUB_USER;
		return user;
	}

	/**
	 * Get mapped Docker system File from {@link biolockj.Config} directory-property by replacing the host system path
	 * with the mapped container path.
	 * 
	 * @param prop {@link biolockj.Config} directory-property
	 * @param containerPath Local container path
	 * @return Docker volume directory or null
	 * @throws ConfigNotFoundException if prop not found
	 * @throws ConfigPathException if path is defined but is not an existing directory
	 */
	public static File getDockerVolumeDir( final String prop, final String containerPath )
		throws ConfigPathException, ConfigNotFoundException {
		final String path = Config.requireString( null, prop );
		final File dir = inAwsEnv() ? getDockerVolumePath( path, containerPath ): new File( containerPath );
		if( !dir.isDirectory() ) throw new ConfigPathException( dir );
		Log.info( BioLockJUtil.class, "Replace Config directory path \"" + path + "\" with Docker container path \"" +
			dir.getAbsolutePath() + "\"" );
		return dir;
	}

	/**
	 * Get mapped Docker system File from {@link biolockj.Config} file-property by replacing the host system path with
	 * the mapped container path.
	 * 
	 * @param prop {@link biolockj.Config} file-property
	 * @param containerPath Local container path
	 * @return Docker volume file or null
	 * @throws ConfigNotFoundException if prop not found
	 */
	public static File getDockerVolumeFile( final String prop, final String containerPath )
		throws ConfigNotFoundException {
		return getDockerVolumePath( Config.requireString( null, prop ), containerPath );
	}

	/**
	 * Get Docker file path through mapped volume
	 * 
	 * @param path {@link biolockj.Config} file or directory path
	 * @param containerPath Local container path
	 * @return Docker file path
	 */
	public static File getDockerVolumePath( final String path, final String containerPath ) {
		if( path == null || path.isEmpty() ) return null;
		return new File( containerPath + path.substring( path.lastIndexOf( File.separator ) ) );
	}

	/**
	 * Return the Docker Image name for the given class name.<br>
	 * Return blj_bash for simple bash script modules that don't rely on special software<br>
	 * Class names contain no spaces, words are separated via CamelCaseConvension.<br>
	 * Docker image names cannot contain upper case letters, so this method substitutes "_" before the lower-case
	 * version of each capital letter.<br>
	 * <br>
	 * Example: JavaModule becomes java_module
	 * 
	 * @param module BioModule
	 * @return Docker Image Name
	 */
	public static String getImageName( final BioModule module ) {
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
			if( !prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM ) &&
				val.equals( val.toUpperCase() ) && !NumberUtils.isNumber( val ) )
				imageName += IMAGE_NAME_DELIM + val.toLowerCase();
			else if( !prevChar.equals( IMAGE_NAME_DELIM ) ||
				prevChar.equals( IMAGE_NAME_DELIM ) && !val.equals( IMAGE_NAME_DELIM ) ) imageName += val.toLowerCase();
		}

		if( hasCustomDockerDB( module ) && className.toLowerCase().contains( "knead_data" ) ||
			className.toLowerCase().contains( "kraken" ) ) imageName += DB_FREE;
		Log.info( DockerUtil.class, "Map: Class [" + className + "] <--> Docker Image [ " + imageName + " ]" );
		return imageName;
	}

	/**
	 * Function used to determine if an alternate database has been defined (other than /db).
	 * 
	 * @param module BioModule
	 * @return TRUE if module has a custom DB defined runtime env
	 */
	public static boolean hasCustomDockerDB( final BioModule module ) {
		try {
			if( inDockerEnv() && module instanceof DatabaseModule ) {
				final File db = ( (DatabaseModule) module ).getDB();
				if( db != null ) return !db.getAbsolutePath().startsWith( DOCKER_DEFAULT_DB_DIR );
			}
		} catch( ConfigPathException | ConfigNotFoundException ex ) {
			Log.error( DockerUtil.class,
				"Error occurred checking database path of module: " + module.getClass().getName(), ex );
		}
		return false;
	}

	/**
	 * Return TRUE if running in AWS (based on Config props).
	 * 
	 * @return TRUE if pipeline.env=aws
	 */
	public static boolean inAwsEnv() {
		return RuntimeParamUtil.isAwsMode();
	}

	/**
	 * Check runtime env for /.dockerenv
	 * 
	 * @return TRUE if Java running in Docker container
	 */
	public static boolean inDockerEnv() {
		return DOCKER_ENV_FLAG_FILE.isFile();
	}

	/**
	 * Return TRUE if runtime parameters indicate attempt to run in direct mode
	 * 
	 * @return boolean
	 */
	public static boolean isDirectMode() {
		return RuntimeParamUtil.getDirectModuleDir() != null;
	}

	private static String getDbDirPath( final DatabaseModule module )
		throws ConfigPathException, ConfigNotFoundException {
		if( module.getDB() == null ) return null;
		if( module instanceof RdpClassifier ) return module.getDB().getParentFile().getAbsolutePath();
		return module.getDB().getAbsolutePath();
	}

	private static String getDockerClassName( final BioModule module ) {
		final String className = module.getClass().getSimpleName();
		final boolean isQiime = module instanceof BuildQiimeMapping || module instanceof MergeQiimeOtuTables ||
			module instanceof QiimeClassifier;

		return isQiime ? QiimeClassifier.class.getSimpleName(): module instanceof R_Module ?
			R_Module.class.getSimpleName(): module instanceof JavaModule ? JavaModule.class.getSimpleName(): className;
	}

	private static String getDockerEnvVars() {
		return " -e \"" + COMPUTE_SCRIPT + "=$1\"";
	}

	private static String getDockerVolumes( final BioModule module )
		throws ConfigPathException, ConfigNotFoundException {
		Log.debug( DockerUtil.class, "Assign Docker volumes for module: " + module.getClass().getSimpleName() );

		String dockerVolumes = "-v " + DOCKER_SOCKET + ":" + DOCKER_SOCKET + " -v " +
			RuntimeParamUtil.getDockerHostHomeDir() + ":" + AWS_EC2_HOME + ":delegated";

		if( inAwsEnv() )
			return dockerVolumes + " -v " + DOCKER_BLJ_MOUNT_DIR + ":" + DOCKER_BLJ_MOUNT_DIR + ":delegated";

		dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostInputDir() + ":" + DOCKER_INPUT_DIR + ":ro";
		dockerVolumes +=
			" -v " + RuntimeParamUtil.getDockerHostPipelineDir() + ":" + DOCKER_PIPELINE_DIR + ":delegated";
		dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostConfigDir() + ":" + DOCKER_CONFIG_DIR + ":ro";

		if( RuntimeParamUtil.getDockerHostMetaDir() != null )
			dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostMetaDir() + ":" + DOCKER_META_DIR + ":ro";

		if( RuntimeParamUtil.getDockerHostPrimerDir() != null )
			dockerVolumes += " -v " + RuntimeParamUtil.getDockerHostPrimerDir() + ":" + DOCKER_PRIMER_DIR + ":ro";

		if( RuntimeParamUtil.getDockerHostBLJ() != null ) dockerVolumes +=
			" -v " + RuntimeParamUtil.getDockerHostBLJ().getAbsolutePath() + ":" + CONTAINER_BLJ_DIR + ":ro";

		if( RuntimeParamUtil.getDockerHostBLJ_SUP() != null ) dockerVolumes +=
			" -v " + RuntimeParamUtil.getDockerHostBLJ_SUP().getAbsolutePath() + ":" + CONTAINER_BLJ_SUP_DIR + ":ro";

		if( hasCustomDockerDB( module ) )
			dockerVolumes += " -v " + getDbDirPath( (DatabaseModule) module ) + ":" + DOCKER_DB_DIR + ":ro";

		return dockerVolumes;
	}

	// private static String getVolumePath( final String path ) {
	// Log.info( DockerUtil.class, "Map Docker volume getVolumePath( " + path + " )" );
	// String newPath = path;
	// if( path.startsWith( CONTAINER_BLJ_SUP_DIR ) )
	// newPath = RuntimeParamUtil.getDockerHostBLJ_SUP().getAbsolutePath() +
	// path.substring( CONTAINER_BLJ_SUP_DIR.length() );
	// if( path.startsWith( CONTAINER_BLJ_DIR ) ) newPath =
	// RuntimeParamUtil.getDockerHostBLJ().getAbsolutePath() + path.substring( CONTAINER_BLJ_DIR.length() );
	// Log.info( DockerUtil.class, "Map Docker volume newPath -----> ( " + newPath + " )" );
	// return newPath;
	// }

	private static final String rmFlag( final BioModule module ) throws ConfigFormatException {
		return Config.getBoolean( module, SAVE_CONTAINER_ON_EXIT ) ? "": DOCK_RM_FLAG;
	}

	private static boolean useBasicBashImg( final BioModule module ) {
		return module instanceof PearMergeReads || module instanceof AwkFastaConverter || module instanceof Gunzipper;
	}

	/**
	 * Docker container dir to map HOST $HOME to save logs + find Config values using $HOME: {@value #AWS_EC2_HOME} Need
	 * to name this dir = "/home/ec2-user" so Nextflow config is same inside + outside of container
	 */
	public static final String AWS_EC2_HOME = "/home/ec2-user";

	/**
	 * Docker container root user EFS directory: /mnt/efs
	 */
	public static final String DOCKER_BLJ_MOUNT_DIR = "/mnt/efs";

	/**
	 * Docker container root user DB directory: /mnt/efs/db
	 */
	public static final String DOCKER_DB_DIR = DOCKER_BLJ_MOUNT_DIR + "/db";

	/**
	 * Docker container root user DB directory: /mnt/efs/db
	 */
	public static final String DOCKER_DEFAULT_DB_DIR = "/mnt/db";

	/**
	 * All containers mount the host {@value biolockj.Constants#INPUT_DIRS} to the container "input" volume: :
	 * /mnt/efs/input
	 */
	public static final String DOCKER_INPUT_DIR = DOCKER_BLJ_MOUNT_DIR + "/input";

	/**
	 * All containers mount {@value biolockj.Constants#INTERNAL_PIPELINE_DIR} to the container volume: /mnt/efs/output
	 */
	public static final String DOCKER_PIPELINE_DIR = DOCKER_BLJ_MOUNT_DIR + "/pipelines";

	/**
	 * Some containers mount the {@value biolockj.Constants#INPUT_TRIM_SEQ_FILE} to the containers "primer":
	 * /mnt/efs/primer
	 */
	public static final String DOCKER_PRIMER_DIR = DOCKER_BLJ_MOUNT_DIR + "/primer";

	/**
	 * AWS deployed containers mount $BLJ/script to {@value #DOCKER_BLJ_MOUNT_DIR}/script dir: /mnt/efs/script
	 */
	public static final String DOCKER_SCRIPT_DIR = DOCKER_BLJ_MOUNT_DIR + "/script";

	/**
	 * Docker container default $USER: {@value #DOCKER_USER}
	 */
	public static final String DOCKER_USER = "root";

	/**
	 * Docker container root user $HOME directory: /root
	 */
	public static final String ROOT_HOME = File.separator + DOCKER_USER;

	/**
	 * {@link biolockj.Config} name of the Docker Hub user with the BioLockJ containers: {@value #DOCKER_HUB_USER}<br>
	 * Docker Hub URL: <a href="https://hub.docker.com" target="_top">https://hub.docker.com</a><br>
	 * By default the "biolockj" user is used to pull the standard modules, but advanced users can deploy their own
	 * versions of these modules and add new modules in their own Docker Hub account.
	 */
	protected static final String DOCKER_HUB_USER = "docker.user";

	/**
	 * Docker container blj_support dir for dev support: {@value #CONTAINER_BLJ_DIR}
	 */
	static final String CONTAINER_BLJ_DIR = "/app/biolockj";

	/**
	 * Docker container blj_support dir for dev support: {@value #CONTAINER_BLJ_SUP_DIR}
	 */
	static final String CONTAINER_BLJ_SUP_DIR = "/app/blj_support";

	/**
	 * All containers mount the host {@link biolockj.Config} directory to the container volume: /mnt/efs/config
	 */
	static final String DOCKER_CONFIG_DIR = DOCKER_BLJ_MOUNT_DIR + "/config";

	/**
	 * {@link biolockj.Config} String property used to run specific version of Docker images:
	 * {@value #DOCKER_IMG_VERSION}
	 */
	static final String DOCKER_IMG_VERSION = "docker.imgVersion";

	/**
	 * Some containers mount the {@value biolockj.util.MetaUtil#META_FILE_PATH} to the container "meta" volume:
	 * /mnt/efs/metadata
	 */
	static final String DOCKER_META_DIR = DOCKER_BLJ_MOUNT_DIR + "/metadata";

	/**
	 * {@link biolockj.Config} Boolean property - enable to avoid docker run --rm flag: {@value #SAVE_CONTAINER_ON_EXIT}
	 */
	static final String SAVE_CONTAINER_ON_EXIT = "docker.saveContainerOnExit";

	/**
	 * Name of the bash script function used to generate a new Docker container: {@value #SPAWN_DOCKER_CONTAINER}
	 */
	static final String SPAWN_DOCKER_CONTAINER = "spawnDockerContainer";

	private static final String BLJ_BASH = "blj_bash";
	private static final String COMPUTE_SCRIPT = "COMPUTE_SCRIPT";
	private static final String DB_FREE = "_dbfree";
	private static final String DEFAULT_DOCKER_HUB_USER = "biolockj";
	private static final String DOCK_RM_FLAG = "--rm";
	private static final File DOCKER_ENV_FLAG_FILE = new File( "/.dockerenv" );
	private static final String DOCKER_SOCKET = "/var/run/docker.sock";
	private static final Set<String[]> downloadDbCmdRegister = new HashSet<>();
	private static final String IMAGE_NAME_DELIM = "_";
}
