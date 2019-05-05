/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 11, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.ScriptModule;
import biolockj.module.implicit.ImportMetadata;
import biolockj.module.report.Email;

/**
 * This utility builds the Nextflow main.nf used in AWS pipelines.
 */
public class NextflowUtil {

	/**
	 * Sync file or directory with S3 bucket.<br>
	 * Dir example: aws s3 sync $EFS/config s3://blj-2019-04-05/config<br>
	 * File example: aws s3 cp ${BLJ_META}/testMetadata.tsv s3://blj-2019-04-05/metadata/testMetadata.tsv
	 * 
	 * @param efsPath File or directory to sync
	 * @param waitUntilComplete Boolean if enabled will block until process completes before moving on
	 * @throws Exception if errors occur
	 */
	public static void awsSyncS3( final String efsPath, final boolean waitUntilComplete ) throws Exception {
		String s3Dir = getAwsS3();
		if( efsPath.contains( Config.pipelinePath() ) ) {
			s3Dir += efsPath.replace( Config.pipelinePath(), "" );
		}
		Log.info( BioLockJ.class, "Transfer " + efsPath + " to --> " + s3Dir );
		final String[] s3args = new String[ 5 ];
		s3args[ 0 ] = "aws";
		s3args[ 1 ] = "s3";
		s3args[ 2 ] = new File( efsPath ).isFile() ? "cp": "sync";
		s3args[ 3 ] = efsPath;
		s3args[ 4 ] = s3Dir;

		if( waitUntilComplete ) {
			Processor.runSubprocess( s3args, "S3-Transfer" );
		} else {
			Processor.submit( s3args, "S3-Transfer" );
		}
	}

	/**
	 * Get the Nextflow main.nf file path.
	 * 
	 * @return Nextflow main.nf
	 */
	public static File getMainNf() {
		return new File( Config.pipelinePath() + File.separator + MAIN_NF );
	}

	/**
	 * Get the Nextflow report directory
	 * 
	 * @return Nextflow report directory
	 */
	public static File getNextflowReportDir() {
		final File dir = new File( Config.pipelinePath() + File.separator + NEXTFLOW_CMD );
		if( !dir.isDirectory() ) {
			dir.mkdir();
		}
		return dir;
	}

	/**
	 * Return true if the Nextflow log has been saved to the pipeline root directory
	 * 
	 * @return TRUE if log exists in pipeline root directory
	 */
	public static boolean nextflowLogExists() {
		return new File( getNextflowReportDir().getAbsolutePath() + File.separator + NF_LOG_NAME ).isFile();
	}

	/**
	 * Purge EFS data based on pipeline Config.
	 * 
	 * @return TRUE if not errors occur
	 */
	public static boolean purgeEfsData() {
		try {
			if( Config.getBoolean( null, AWS_PURGE_EFS_OUTPUT ) ) {
				purge( Config.pipelinePath() );
			} else if( Config.getBoolean( null, AWS_PURGE_EFS_INPUTS ) ) {
				purge( Config.getConfigFilePath() );
				purge( AWS_DB_DIR );
				purge( AWS_INPUT_DIR );
				purge( AWS_META_DIR );
				purge( AWS_PRIMER_DIR );
			}
			return true;
		} catch( final Exception ex ) {
			Log.error( NextflowUtil.class, "Failed to save datat to S3" );
			return false;
		}
	}

	/**
	 * Save EFS data to S3 based on pipeline Config.
	 * 
	 * @return TRUE if not errors occur
	 */
	public static boolean saveEfsDataToS3() {
		try {
			final boolean savePipeline = Config.getBoolean( null, NextflowUtil.AWS_COPY_PIPELINE_TO_S3 );
			final boolean saveReports = Config.getBoolean( null, NextflowUtil.AWS_COPY_REPORTS_TO_S3 );

			if( savePipeline ) {
				awsSyncS3( Config.pipelinePath(), true );
			} else if( DownloadUtil.getDownloadListFile().exists() && saveReports ) {
				final BufferedReader reader = BioLockJUtil.getFileReader( DownloadUtil.getDownloadListFile() );
				try {
					for( String path = reader.readLine(); path != null; path = reader.readLine() ) {
						awsSyncS3( Config.pipelinePath() + File.separator + path, true );
					}
				} finally {
					if( reader != null ) {
						reader.close();
					}
				}
			} else {
				Log.warn( NextflowUtil.class,
					"Due to Config [ " + AWS_COPY_PIPELINE_TO_S3 + "=" + Constants.FALSE + " ] & [ " + AWS_REPORT_DIR
						+ "=" + Constants.FALSE + " ]: pipeline ouput will be not saved to configured AWS S3 bucket: "
						+ Config.requireString( null, AWS_S3 ) );
			}
			return true;
		} catch( final Exception ex ) {
			Log.error( NextflowUtil.class, "Failed to save datat to S3" );
			return false;
		}
	}

	/**
	 * Save a copy of the Nextflow log file to the Pipeline root directory
	 */
	public static void saveNextflowLog() {
		try {
			final File log = new File( NF_LOG );
			if( !log.exists() ) {
				Log.warn( NextflowUtil.class, NF_LOG + " not found, cannot copy to pipeline root directory!" );
				return;
			}
			FileUtils.copyFileToDirectory( new File( NF_LOG ), getNextflowReportDir() );
		} catch( final Exception ex ) {
			Log.error( NextflowUtil.class, "Failed to copy nextflow.log to pipeline root directory ", ex );
		}
	}

	/**
	 * Call this method to build the Nextflow main.nf for the current pipeline.
	 * 
	 * @param modules Pipeline modules
	 * @throws Exception if errors occur
	 */
	public static void startNextflow( final List<BioModule> modules ) throws Exception {
		final File template = buildInitialTemplate( asString( modules ) );
		writeNextflowMainNF( getNextflowLines( template ) );
		Log.info( NextflowUtil.class, "Nextflow main.nf generated: " + getMainNf().getAbsolutePath() );
		template.delete();
		startService();
		pollAndSpin();
		Log.info( NextflowUtil.class, "Nextflow service sub-process started!" );
	}

	/**
	 * Build the main.nf lines from the template file by replacing several parameters.
	 * 
	 * @param template Generated .main.nf template
	 * @return List of lines to save in final main.nf
	 * @throws Exception if errors occur
	 */
	protected static List<String> getNextflowLines( final File template ) throws Exception {
		Log.info( NextflowUtil.class, "Build main.nf from the Nextflow template: " + template.getAbsolutePath() );
		final List<String> lines = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( template );
		try {
			ScriptModule module = null;

			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				String onDemandLabel = null;
				Log.debug( NextflowUtil.class, "READ LINE: " + line );
				 if( line.contains( PIPELINE_ROOT_DIR ) ) {
						Log.debug( NextflowUtil.class, "Found final proc worker line: " + line );
						line = line.replace( PIPELINE_ROOT_DIR, Config.pipelinePath() );
				} else if( line.trim().startsWith( PROCESS ) ) {
					Log.debug( NextflowUtil.class, "Found module PROCESS declaration: " + line );
					module = getModule( line.replaceAll( PACKAGE_SEPARATOR, "\\." ).replace( PROCESS, "" )
						.replaceAll( "\\{", "" ).trim() );
					Log.debug( NextflowUtil.class, "START module BLOCK for: " + module.getClass().getName() );
					line = line.replaceAll( PACKAGE_SEPARATOR, "_" );
				} else if( module != null ) {
					if( line.contains( NF_CPUS ) ) {
						final String prop = Config.getModuleProp( module, NF_CPUS.substring( 1 ) );
						line = line.replace( NF_CPUS, Config.getString( module, prop ) );
					} else if( line.contains( NF_MEMORY ) ) {
						final String prop = Config.getModuleProp( module, NF_MEMORY.substring( 1 ) );
						String ram = Config.requireString( module, prop );
						if( !ram.startsWith( "'" ) ) {
							ram = "'" + ram;
						}
						if( !ram.endsWith( "'" ) ) {
							ram = ram + "'";
						}
						line = line.replace( NF_MEMORY, ram );
					} else if( line.contains( NF_DOCKER_IMAGE ) ) {
						line = line.replace( NF_DOCKER_IMAGE, getDockerImageLabel( module ) );
						if( Config.requireString( module, EC2_ACQUISITION_STRATEGY ).toUpperCase()
							.equals( ON_DEMAND ) ) {
							onDemandLabel = "    label '" + ON_DEMAND + "'";
						}
					} else if( line.contains( MODULE_SCRIPT ) ) {
						Log.debug( NextflowUtil.class, "Found worker line: " + line );
						line = line.replace( MODULE_SCRIPT, module.getScriptDir().getAbsolutePath() );
					} else if( line.trim().equals( "}" ) ) {
						Log.debug( NextflowUtil.class, "END module BLOCK: " + module.getClass().getName() );
						module = null;
					}
				}
				
				Log.debug( NextflowUtil.class, "ADD LINE: " + line );
				lines.add( line );
				if( onDemandLabel != null ) {
					lines.add( onDemandLabel );
					Log.info( NextflowUtil.class, "ADD LINE: " + onDemandLabel );
				}
			}
		} finally {
			if( reader != null ) {
				reader.close();
			}
		}
		Log.info( NextflowUtil.class, "Done building main.nf with # lines = " + lines.size() );
		return lines;
	}

	private static String asString( final List<BioModule> modules ) {
		String flatMods = "";
		for( final BioModule module: modules ) {
			if( !( module instanceof ImportMetadata ) && !( module instanceof Email ) ) {
				flatMods += ( flatMods.isEmpty() ? "": MODULE_SEPARATOR )
					+ module.getClass().getName().replaceAll( "\\.", PACKAGE_SEPARATOR );
			}
		}

		return flatMods;
	}

	private static File buildInitialTemplate( final String modules ) throws Exception {
		Log.info( NextflowUtil.class, "Build Nextflow initial template: " + templateConfig().getAbsolutePath() );
		Log.info( NextflowUtil.class, "Nextflow modules: " + modules );

		final String[] args = new String[ 3 ];
		args[ 0 ] = templateScript().getAbsolutePath();
		args[ 1 ] = templateConfig().getAbsolutePath();
		args[ 2 ] = modules;
		Processor.submit( args, "Build Nf Template" );
		if( !templateConfig().exists() )
			throw new Exception( "Nextflow Template file is not found at path: " + templateConfig().getAbsolutePath() );
		Log.info( NextflowUtil.class, "Nextflow Template file created: " + templateConfig().getAbsolutePath() );
		return templateConfig();
	}

	private static String getAwsS3() throws Exception {
		final String s3 = Config.requireString( null, AWS_S3 ) + File.separator + Config.pipelineName();
		if( s3.startsWith( S3_DIR ) ) return s3;
		return S3_DIR + s3;
	}

	private static String getDockerImageLabel( final BioModule module ) throws Exception {
		return "'" + IMAGE + "_" + DockerUtil.getDockerUser( module ) + "_" + DockerUtil.getImageName( module ) + "_"
			+ DockerUtil.getImageVersion( module ) + "'";
	}

	private static ScriptModule getModule( final String className ) {
		Log.debug( NextflowUtil.class, "Calling getModule( " + className + " )" );
		for( final BioModule module: Pipeline.getModules() ) {
			if( module.getClass().getName().equals( className ) ) {
				if( usedModules.contains( module.getID() ) ) {
					Log.debug( NextflowUtil.class,
						"Skip module [ ID = " + module.getID()
							+ " ] in since it was already used, look for another module of type: "
							+ module.getClass().getName() );
				} else {
					Log.debug( NextflowUtil.class, "getModule( " + className + " ) RETURN module [ ID = "
						+ module.getID() + " ] --> " + module.getClass().getName() );
					usedModules.add( module.getID() );
					return (ScriptModule) module;
				}
			}
		}
		return null;
	}

	private static boolean poll() throws Exception {
		final File nfLog = new File( NF_LOG );
		if( nfLog.exists() ) {
			final BufferedReader reader = BioLockJUtil.getFileReader( nfLog );
			try {
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					if( line.contains( NF_INIT_FLAG ) ) return true;
				}
			} finally {
				if( reader != null ) {
					reader.close();
				}
			}
		} else {
			Log.info( NextflowUtil.class, "Nextflow log file \"" + NF_LOG + "\" has not been created yet..." );
		}
		return false;
	}

	private static void pollAndSpin() throws Exception {
		Log.info( NextflowUtil.class,
			"Poll " + NF_LOG + " every 15 seconds until the status message \"" + NF_INIT_FLAG + "\" is logged" );
		int numSecs = 0;
		boolean finished = false;
		while( !finished ) {
			finished = poll();
			if( !finished ) {
				if( numSecs > NF_TIMEOUT )
					throw new Exception( "Nextflow initialization timed out after " + numSecs + " seconds." );
				Log.info( NextflowUtil.class, "Nextflow initializing..." );
				Thread.sleep( 15 * 1000 );
				numSecs += 15;
			} else {
				Log.info( NextflowUtil.class, "Nextflow initialization complete!" );
			}
		}
	}

	private static boolean purge( final String subDir ) throws Exception {
		final String target = DockerUtil.AWS_EFS + File.separator + subDir;
		Log.info( BioLockJ.class, "Delete everything under/including --> " + target );
		final String[] args = new String[ 3 ];
		args[ 0 ] = "rm";
		args[ 1 ] = "-rf";
		args[ 2 ] = target;
		Processor.submit( args, "Clear-EFS-Data" );
		return true;
	}

	private static void startService() throws Exception {
		final String reportBase = getNextflowReportDir().getAbsolutePath() + File.separator + Config.pipelineName()
			+ "_";
		final String[] args = new String[ 11 ];
		args[ 0 ] = NEXTFLOW_CMD;
		args[ 1 ] = "run";
		args[ 2 ] = "-work-dir";
		args[ 3 ] = S3_DIR + Config.requireString( null, AWS_S3 ) + File.separator + NEXTFLOW_CMD;
		args[ 4 ] = "-with-trace";
		args[ 5 ] = reportBase + "nextflow_trace.tsv";
		args[ 6 ] = "-with-timeline";
		args[ 7 ] = reportBase + "nextflow_timeline.html";
		args[ 8 ] = "-with-dag";
		args[ 9 ] = reportBase + "nextflow_diagram.html";
		args[ 10 ] = getMainNf().getAbsolutePath();
		Processor.runSubprocess( args, "Nextflow" );
	}

	private static File templateConfig() {
		return new File( Config.pipelinePath() + File.separator + ".template_" + MAIN_NF );
	}

	private static File templateScript() throws Exception {
		return new File( BioLockJUtil.getBljDir().getAbsolutePath() + File.separator + Constants.SCRIPT_DIR
			+ File.separator + MAKE_NEXTFLOW_SCRIPT );
	}

	private static void writeNextflowMainNF( final List<String> lines ) throws Exception {
		Log.debug( NextflowUtil.class, "Create " + getMainNf().getAbsolutePath() + " with # lines = " + lines.size() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getMainNf() ) );
		try {
			boolean indent = false;
			for( String line: lines ) {
				if( line.trim().equals( "}" ) ) {
					indent = !indent;
				}
				if( indent ) {
					line = "    " + line;
				}
				writer.write( line + Constants.RETURN );
				if( line.trim().endsWith( "{" ) ) {
					indent = !indent;
				}
			}
		} finally {
			writer.close();
		}
	}

	/**
	 * Name of the AWS S3 sub-directory used to save pipeline reports
	 */
	public static final String AWS_CONFIG_DIR = "config";

	/**
	 * {@link biolockj.Config} Boolean property: If enabled save all input files to S3: {@value #AWS_COPY_INPUTS_TO_S3}
	 */
	public static final String AWS_COPY_INPUTS_TO_S3 = "aws.copyInputsToS3";

	/**
	 * {@link biolockj.Config} Boolean property: If enabled save pipeline to S3: {@value #AWS_COPY_PIPELINE_TO_S3}
	 */
	public static final String AWS_COPY_PIPELINE_TO_S3 = "aws.copyPipelineToS3";

	/**
	 * {@link biolockj.Config} Boolean property: If enabled save reports to S3: {@value #AWS_COPY_PIPELINE_TO_S3}
	 */
	public static final String AWS_COPY_REPORTS_TO_S3 = "aws.copyReportsToS3";

	/**
	 * {@link biolockj.Config} Boolean property: If enabled delete all EFS dirs (except pipelines):
	 * {@value #AWS_PURGE_EFS_INPUTS}
	 */
	public static final String AWS_PURGE_EFS_INPUTS = "aws.purgeEfsInputs";

	/**
	 * {@link biolockj.Config} Boolean property: If enabled delete all EFS/pipelines: {@value #AWS_PURGE_EFS_OUTPUT}
	 */
	public static final String AWS_PURGE_EFS_OUTPUT = "aws.purgeEfsOutput";

	/**
	 * {@link biolockj.Config} String property: AWS memory set in Nextflow main.nf: {@value #AWS_RAM}
	 */
	public static final String AWS_RAM = "aws.ram";

	/**
	 * Name of the AWS S3 subdirectory used to save pipeline reports
	 */
	public static final String AWS_REPORT_DIR = "reports";

	/**
	 * {@link biolockj.Config} String property: AWS S3 pipeline output directory used by Nextflow main.nf:
	 * {@value #AWS_S3}
	 */
	public static final String AWS_S3 = "aws.s3";

	/**
	 * The Docker container will generate a nextflow.log file in the root directory, this is the file name
	 */
	protected static final String NF_LOG = "/.nextflow.log";

	private static final String AWS_DB_DIR = "db";
	private static final String AWS_INPUT_DIR = "input";
	private static final String AWS_META_DIR = "metadata";
	private static final String AWS_PRIMER_DIR = "primer";
	private static final String EC2_ACQUISITION_STRATEGY = "aws.ec2AcquisitionStrategy";
	private static final String IMAGE = "image";
	private static final String MAIN_NF = "main.nf";
	private static final String MAKE_NEXTFLOW_SCRIPT = "make_nextflow";
	private static final String MODULE_SCRIPT = "BLJ_MODULE_SUB_DIR";
	private static final String PIPELINE_ROOT_DIR = "BLJ_PIPELINE_DIR";
	private static final String MODULE_SEPARATOR = ".";
	private static final String NEXTFLOW_CMD = "nextflow";
	private static final String NF_CPUS = "$" + ScriptModule.SCRIPT_NUM_THREADS;
	private static final String NF_DOCKER_IMAGE = "$nextflow.dockerImage";
	private static final String NF_INIT_FLAG = "Session await";
	private static final String NF_LOG_NAME = ".nextflow.log";
	private static final String NF_MEMORY = "$" + AWS_RAM;
	private static final int NF_TIMEOUT = 180;
	private static final String ON_DEMAND = "DEMAND";
	private static final String PACKAGE_SEPARATOR = "_:_";
	private static final String PROCESS = "process";
	private static final String S3_DIR = "s3://";
	private static final Set<Integer> usedModules = new HashSet<>();
}
