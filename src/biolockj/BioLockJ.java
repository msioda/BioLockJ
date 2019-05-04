/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org
 */
package biolockj;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.exception.FatalExceptionHandler;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.report.Email;
import biolockj.util.*;

/**
 * This is the primary BioLockJ class - its main() method is executed when the jar is run.<br>
 * This class validates the runtime parameters to run a new pipeline or restart a failed pipeline.<br>
 * The Java log file is initialized and the configuration file is processed before starting the pipeline.<br>
 * If the pipeline is successful, the program executes clean up operations (if configured) and creates a status-complete
 * indicator file in the pipeline root directory.<br>
 */
public class BioLockJ {

	private BioLockJ() {}

	/**
	 * Copy file to pipeline root directory.
	 * 
	 * @param file File to copy
	 * @throws Exception if errors occur
	 */
	public static void copyFileToPipelineRoot( final File file ) throws Exception {
		Log.info( BioLockJ.class,
			"Copy file: " + file.getAbsolutePath() + " to pipeline root: " + Config.pipelinePath() );
		final File localFile = new File( Config.pipelinePath() + File.separator + file.getName() );
		if( !localFile.isFile() ) {
			FileUtils.copyFileToDirectory( file, new File( Config.pipelinePath() ) );
			if( !localFile.isFile() )
				throw new Exception( "Unable to copy file to pipeline root directory: " + file.getAbsolutePath() );
		}
	}

	/**
	 * Print error file path, restart instructions, and link to the BioLockJ Wiki
	 * 
	 * @return Help Info
	 */
	public static String getHelpInfo() {
		final File errFile = FatalExceptionHandler.getErrorLog();
		return Constants.RETURN + "To view the BioLockJ help menu, run \"biolockj -h\"" + Constants.RETURN
			+ ( errFile != null ? "Check error logs here --> " + errFile.getAbsolutePath() + Constants.RETURN: "" )
			+ "For more information, please visit the BioLockJ Wiki:" + Constants.BLJ_WIKI + Constants.RETURN;
	}

	/**
	 * Determine project status based on existence of {@value biolockj.Constants#BLJ_COMPLETE} in pipeline root
	 * directory.
	 *
	 * @return true if {@value biolockj.Constants#BLJ_COMPLETE} exists in the pipeline root directory, otherwise false
	 */
	public static boolean isPipelineComplete() {
		File f = null;
		try {
			f = new File( Config.pipelinePath() + Constants.BLJ_COMPLETE );
		} catch( final Exception ex ) {
			return false;
		}
		return f.isFile();
	}

	/**
	 * {@link biolockj.BioLockJ} is the BioLockj.jar Main-Class, and is the first method executed.<br>
	 * Execution summary:<br>
	 * <ol>
	 * <li>Call {@link #initBioLockJ(String[])} to assign pipeline root dir and log file
	 * <li>If change password pipeline, call {@link biolockj.module.report.Email#encryptAndStoreEmailPassword()}
	 * <li>Otherwise execute {@link #runPipeline()}
	 * </ol>
	 * <p>
	 * If pipeline has failed, attempt execute {@link biolockj.module.report.Email} (if configured) to notify user of
	 * failures.
	 *
	 * @param args - String[] runtime parameters passed to the Java program when launching BioLockJ
	 */
	public static void main( final String[] args ) {
		System.out.println( "Starting BioLockj..." + Constants.APP_START_TIME );
		try {

			initBioLockJ( args );

			runPipeline();

			if( DockerUtil.inAwsEnv() ) {
				NextflowUtil.saveNextflowLog();
				final boolean s3saved = NextflowUtil.saveEfsDataToS3();
				if( s3saved ) {
					NextflowUtil.purgeEfsData();
				} else throw new Exception( "Pipeline completed successfully, EFS data failed to transfer to S3!" );
			}
		} catch( final Exception ex ) {
			FatalExceptionHandler.logFatalError( args, ex );
		} finally {
			pipelineShutDown();
		}
	}

	/**
	 * Return the pipeline input directory
	 * 
	 * @return Input dir
	 */
	public static File pipelineInputDir() {
		return new File( Config.pipelinePath() + File.separator + "input" );
	}

	/**
	 * Create a copy of the sequence files in property {@value biolockj.Constants#INPUT_DIRS}, output to a directory
	 * named {@value biolockj.Constants#INTERNAL_PIPELINE_DIR}/input.
	 *
	 * @throws Exception if unable to copy the files
	 */
	protected static void copyInputData() throws Exception {
		final String statusFileName = pipelineInputDir().getName() + File.separator + Constants.BLJ_COMPLETE;
		final File statusFile = new File( Config.pipelinePath() + File.separator + statusFileName );
		if( !pipelineInputDir().exists() ) {
			pipelineInputDir().mkdirs();
		} else if( statusFile.exists() ) return;

		for( final File dir: BioLockJUtil.getInputDirs() ) {
			info( "Copying input files from " + dir + " to " + pipelineInputDir() );
			FileUtils.copyDirectory( dir, pipelineInputDir() );
			markStatus( statusFileName );
			BioLockJUtil.ignoreFile( statusFile );
		}

		final List<File> inputFiles = new ArrayList<>(
			FileUtils.listFiles( pipelineInputDir(), HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );
		info( "Total number of input files: " + inputFiles.size() );
		int i = 0;
		for( final File file: inputFiles ) {
			info( "Imported Input File [ " + i++ + " ]: " + file.getAbsolutePath() );
		}

		BioLockJUtil.setPipelineInputFiles( inputFiles );
		Config.setConfigProperty( Constants.INPUT_DIRS, pipelineInputDir().getAbsolutePath() );
	}

	/**
	 * Create the pipeline root directory under $DOCKER_PROJ and save the path to
	 * {@link biolockj.Config}.{@value biolockj.Constants#INTERNAL_PIPELINE_DIR}.
	 * <p>
	 * For example, the following {@link biolockj.Config} settings will create:
	 * <b>/projects/MicrobeProj_2018Jan01</b><br>
	 * <ul>
	 * <li>$DOCKER_PROJ = /projects
	 * <li>{@link biolockj.Config} file name = MicrobeProj.properties
	 * <li>Current date = January 1, 2018
	 * </ul>
	 *
	 * @return Pipeline root directory
	 */
	protected static File createPipelineDirectory() {
		final String year = String.valueOf( new GregorianCalendar().get( Calendar.YEAR ) );
		final String month = new GregorianCalendar().getDisplayName( Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH );
		final String day = BioLockJUtil.formatDigits( new GregorianCalendar().get( Calendar.DATE ), 2 );
		final String baseString = RuntimeParamUtil.getBaseDir().getAbsolutePath() + File.separator
			+ RuntimeParamUtil.getProjectName();
		final String dateString = "_" + year + month + day;
		File projectDir = new File( baseString + dateString );
		int i = 2;
		while( projectDir.exists() ) {
			projectDir = new File( baseString + "_" + i++ + dateString );
		}
		projectDir.mkdirs();
		return projectDir;
	}

	/**
	 * Execution summary:<br>
	 * <ol>
	 * <li>Call {@link biolockj.util.MemoryUtil#reportMemoryUsage(String)} for baseline memory info
	 * <li>Call {@link biolockj.util.RuntimeParamUtil#registerRuntimeParameters(String[])}
	 * <li>Call {@link biolockj.util.MetaUtil#initialize()} to verify metadata dependencies
	 * <li>Call {@link biolockj.Config#initialize()} to create pipeline root dir and load properties
	 * <li>Initialize {@link Log} with /resources/log4J.properties
	 * <li>Copy initial metadata file into the pipeline root directory
	 * <li>Call {@link biolockj.util.SeqUtil#initialize()} to set Config parameters based on sequence files
	 * </ol>
	 * <p>
	 *
	 * @param args - String[] runtime parameters passed to the Java program when launching BioLockJ
	 * @throws Exception if errors occur
	 */
	protected static void initBioLockJ( final String[] args ) throws Exception {
		MemoryUtil.reportMemoryUsage( "INTIAL MEMORY STATS" );
		RuntimeParamUtil.registerRuntimeParameters( args );
		Config.initialize();
		if( isPipelineComplete() ) throw new Exception( "Pipeline Cancelled!  Pipeline already contains status file: "
			+ Constants.BLJ_COMPLETE + " --> Check directory: " + Config.pipelinePath() );
		Config.getString( null, Constants.INPUT_DIRS );
		MetaUtil.initialize();

		if( DockerUtil.isDirectMode() ) {
			Log.initialize( getDirectLogName( RuntimeParamUtil.getDirectModuleDir() ) );
		} else {
			Log.initialize( Config.pipelineName() );
		}

		if( RuntimeParamUtil.doRestart() ) {
			initRestart();
		}

		if( !DockerUtil.isDirectMode() ) {
			if( MetaUtil.getMetadata() != null ) {
				BioLockJ.copyFileToPipelineRoot( MetaUtil.getMetadata() );
			}

			// Initializes PIPELINE_SEQ_INPUT_TYPE
			BioLockJUtil.getPipelineInputFiles();

			if( doCopyInput() ) {
				if( DockerUtil.inAwsEnv() ) {
					NextflowUtil.awsSyncS3( DockerUtil.CONTAINER_INPUT_DIR, false );
				}
				copyInputData();
			}

			SeqUtil.initialize();
		}
	}

	/**
	 * Initialize restarted pipeline by:
	 * <ol>
	 * <li>Initialize {@link biolockj.Log} file using the name of the pipeline root directory
	 * <li>Update summary #Attempts count
	 * <li>Delete status file {@value biolockj.Constants#BLJ_FAILED} in pipeline root directory
	 * <li>If pipeline status = {@value biolockj.Constants#BLJ_COMPLETE}
	 * <li>Delete file {@value biolockj.util.DownloadUtil#DOWNLOAD_LIST} in pipeline root directory
	 * </ol>
	 * 
	 * @throws Exception if errors occur
	 */
	protected static void initRestart() throws Exception {
		Log.initialize( Config.pipelineName() );
		Log.warn( BioLockJ.class,
			Constants.RETURN + Constants.LOG_SPACER + Constants.RETURN + "RESTART_DIR PROJECT DIR --> "
				+ RuntimeParamUtil.getRestartDir().getAbsolutePath() + Constants.RETURN + Constants.LOG_SPACER
				+ Constants.RETURN );
		Log.info( BioLockJ.class, "Initializing Restarted Pipeline - this may take a couple of minutes..." );

		SummaryUtil.updateNumAttempts();
		if( DownloadUtil.getDownloadListFile().exists() ) {
			DownloadUtil.getDownloadListFile().delete();
		}
		if( NextflowUtil.getMainNf().exists() ) {
			NextflowUtil.getMainNf().delete();
		}

		final File f = new File( Config.pipelinePath() + File.separator + Constants.BLJ_FAILED );
		if( f.exists() ) {
			f.delete();
			// if( !BioLockJUtil.deleteWithRetry( f, 5 ) )
			// {
			// Log.warn( BioLockJ.class, "Unable to delete " + f.getAbsolutePath() );
			// }
		}
	}

	/**
	 * Delete all {@link biolockj.module.BioModule}/{@value biolockj.module.BioModule#TEMP_DIR} folders.
	 *
	 * @throws Exception if unable to delete temp files
	 */
	protected static void removeTempFiles() throws Exception {
		Log.info( BioLockJ.class, "Cleaning up BioLockJ Modules..." );
		for( final BioModule bioModule: Pipeline.getModules() ) {
			if( ModuleUtil.subDirExists( bioModule, BioModule.TEMP_DIR ) ) {
				Log.info( BioLockJ.class, "Delete temp dir for BioLockJ Module: " + bioModule.getClass().getName() );
				BioLockJUtil.deleteWithRetry( ModuleUtil.requireSubDir( bioModule, BioModule.TEMP_DIR ), 10 );
			}
		}
	}

	/**
	 * Execution summary:<br>
	 * <ol>
	 * <li>Call {@link biolockj.Pipeline#initializePipeline()} to initialize Pipeline modules
	 * <li>For direct module execution call {@link biolockj.Pipeline#runDirectModule(Integer)}
	 * <li>Otherwise execute {@link biolockj.Pipeline#startPipeline()} and save MASTER {@link biolockj.Config}
	 * <li>If {@link biolockj.Config}.{@value biolockj.Constants#PIPELINE_DELETE_TEMP_FILES} =
	 * {@value biolockj.Constants#TRUE}, Call {@link #removeTempFiles()} to delete temp files
	 * <li>Call {@link biolockj.util.BioLockJUtil#createFile(String)} to set the overall pipeline status as successful
	 * </ol>
	 * 
	 * @throws Exception if runtime errors occur
	 */
	protected static void runPipeline() throws Exception {
		if( RuntimeParamUtil.doChangePassword() ) {
			Log.info( BioLockJ.class, "Save encrypted password to: " + Config.getConfigFilePath() );
			Email.encryptAndStoreEmailPassword();
			MasterConfigUtil.saveMasterConfig();
			return;
		}

		Pipeline.initializePipeline();

		if( DockerUtil.isDirectMode() ) {
			try {
				final Integer id = getDirectModuleID( RuntimeParamUtil.getDirectModuleDir() );
				Pipeline.runDirectModule( id );
				reportDirectModuleSucess();
				MasterConfigUtil.saveMasterConfig();
				System.exit( 0 );
			} catch( final Exception ex ) {
				reportDirectModuleFailure( ex );
				System.exit( 1 );
			}
		} else {
			MasterConfigUtil.saveMasterConfig();
			if( DockerUtil.inAwsEnv() ) {
				NextflowUtil.startNextflow( Pipeline.getModules() );
			}

			Pipeline.startPipeline();

			if( Config.getBoolean( null, Constants.PIPELINE_DELETE_TEMP_FILES ) ) {
				removeTempFiles();
			}

			MasterConfigUtil.sanitizeMasterConfig();
			markStatus( Constants.BLJ_COMPLETE );
			info( "Log Pipeline Summary..." + Constants.RETURN + SummaryUtil.getSummary() );
		}
	}

	private static boolean doCopyInput() throws Exception {
		final boolean hasMixedInputs = BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_R_INPUT_TYPE )
			|| BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_HUMANN2_COUNT_TABLE_INPUT_TYPE )
			|| BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_NORMAL_TAXA_COUNT_TABLE_INPUT_TYPE )
			|| BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE )
			|| BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_STATS_TABLE_INPUT_TYPE );

		if( hasMixedInputs ) {
			Log.warn( BioLockJ.class, "Non-sequence inputs found - copy input files from "
				+ Config.requireString( null, Constants.INPUT_DIRS ) + " to:" + pipelineInputDir().getAbsolutePath() );
		}
		return Config.getBoolean( null, Constants.PIPELINE_COPY_FILES ) || hasMixedInputs;
	}

	private static String getDirectLogName( final String moduleDir ) throws Exception {
		final File modDir = new File( Config.pipelinePath() + File.separator + moduleDir );
		if( !modDir.exists() )
			throw new Exception( "Direct module directory not found --> " + modDir.getAbsolutePath() );

		final File tempDir = new File( modDir.getAbsoluteFile() + File.separator + BioModule.TEMP_DIR );
		if( !tempDir.exists() ) {
			tempDir.mkdir();
		}

		return modDir.getName() + File.separator + tempDir.getName() + File.separator + moduleDir;

	}

	private static Integer getDirectModuleID( final String moduleDir ) throws Exception {
		return Integer.valueOf( moduleDir.substring( 0, moduleDir.indexOf( "_" ) ) );
	}

	private static void info( final String msg ) {
		if( !DockerUtil.isDirectMode() ) {
			Log.info( BioLockJ.class, msg );
		}
	}

	private static void markStatus( final String status ) throws Exception {
		final File f = new File( Config.pipelinePath() + File.separator + status );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.exists() ) throw new Exception( "Unable to create " + f.getAbsolutePath() );
	}

	private static void pipelineShutDown() {
		if( !DockerUtil.isDirectMode() ) {
			if( DockerUtil.inAwsEnv() && !NextflowUtil.nextflowLogExists() ) {
				NextflowUtil.saveNextflowLog();
			}
			setPipelineSecurity();

		} else if( isPipelineComplete() ) {
			Log.info( BioLockJ.class, "Analysis complete!" );
		} else {
			System.exit( 1 );
		}
	}

	private static void reportDirectModuleFailure( final Exception ex ) throws Exception {
		final JavaModule module = (JavaModuleImpl) Pipeline.getModules()
			.get( getDirectModuleID( RuntimeParamUtil.getDirectModuleDir() ) );
		Log.info( BioLockJ.class, "Save failure status for direct module: " + module.getClass().getName() );
		module.moduleFailed();
		SummaryUtil.reportFailure( ex );
	}

	private static void reportDirectModuleSucess() throws Exception {
		final JavaModule module = (JavaModuleImpl) Pipeline.getModules()
			.get( getDirectModuleID( RuntimeParamUtil.getDirectModuleDir() ) );
		Log.info( BioLockJ.class, "Save success status for direct module: " + module.getClass().getName() );
		module.moduleComplete();
		SummaryUtil.reportSuccess( module );
		System.exit( 0 );
	}

	private static void setPipelineSecurity() {
		try {
			Processor.setFilePermissions( Config.pipelinePath(), Config.getString( null, Constants.PIPELINE_PRIVS ) );
		} catch( final Exception ex ) {
			System.out.println( "Unable to set pipeline filesystem privileges" );
			ex.printStackTrace();
		}
	}
}
