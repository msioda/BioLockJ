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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import org.apache.commons.io.FileUtils;
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
public class BioLockJ
{

	private BioLockJ()
	{}

	/**
	 * Copy file to pipeline root directory.
	 * 
	 * @param file File to copy
	 * @throws Exception if errors occur
	 */
	public static void copyFileToPipelineRoot( final File file ) throws Exception
	{
		final String rootPath = Config.getExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath();
		final File localFile = new File( rootPath + File.separator + file.getName() );
		if( !localFile.exists() )
		{
			FileUtils.copyFileToDirectory( file, Config.getExistingDir( Config.PROJECT_PIPELINE_DIR ) );
			if( !localFile.exists() )
			{
				throw new Exception( "Unable to copy file to pipeline root directory: " + file.getAbsolutePath() );
			}

		}
	}

	/**
	 * Print error file path, restart instructions, and link to the BioLockJ Wiki
	 * 
	 * @param errFile Error File
	 * @return Help Info
	 */
	public static String getHelpInfo( final File errFile )
	{
		try
		{
			return RETURN + "To view the BioLockJ help menu, run \"biolockj -h\"" + RETURN
					+ ( errFile != null ? "Writing error file to " + errFile.getAbsolutePath() + RETURN: "" )
					+ "For more information, please visit the BioLockJ Wiki:" + BLJ_WIKI + RETURN;
		}
		catch( final Exception ex )
		{
			ex.printStackTrace();
		}
		return "";
	}

	/**
	 * Set the {@value biolockj.Config#PROJECT_PIPELINE_NAME} and {@value biolockj.Config#PROJECT_PIPELINE_DIR}
	 * 
	 * @throws Exception if errors occur
	 */
	public static void initProjectProps() throws Exception
	{
		if( RuntimeParamUtil.doRestart() )
		{
			Config.setConfigProperty( Config.PROJECT_PIPELINE_DIR, RuntimeParamUtil.getRestartDir().getAbsolutePath() );
		}
		else if( RuntimeParamUtil.isDirectMode() )
		{

		}
		else
		{
			Config.setConfigProperty( Config.PROJECT_PIPELINE_DIR, createPipelineDirectory().getAbsolutePath() );
		}

		Config.setConfigProperty( Config.PROJECT_PIPELINE_NAME,
				Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getName() );
	}

	/**
	 * {@link biolockj.BioLockJ} is the BioLockj.jar Main-Class, so this main method is the first method executed when
	 * BioLockJ runs. The biolockj shell script always passed the project directory path $DOCKER_PROJ as 1st param.<br>
	 * The program requires a single user-provided parameter, the path to a {@link biolockj.Config} file.<br>
	 * If -r switch is passed, attempt to restart a filed pipeline.<br>
	 * If -d switch is passed, run direct pipeline for given Java module.<br>
	 * An optional parameter can be used to set the encrypted email password or as the direct pipelines Java class.<br>
	 * Execution summary:<br>
	 * <ol>
	 * <li>Call {@link biolockj.util.RuntimeParamUtil#registerRuntimeParameters(String[])} to validate runtime
	 * parameters
	 * <li>Call {@link biolockj.Config#initialize()} to load project properties
	 * <li>Call {@link biolockj.util.MetaUtil#initialize()} to verify metadata dependencies
	 * <li>Copy {@link biolockj.Config} file and nested {@value biolockj.Config#PROJECT_DEFAULT_PROPS} files into
	 * {@value biolockj.Config#PROJECT_PIPELINE_DIR} to preserve the state of these files at runtime.
	 * <li>Initialize {@link Log} using /resources/log4J.properties
	 * <li>If {@value #PROJECT_COPY_FILES} = {@value biolockj.Config#TRUE}, copy input files into a new "input"
	 * directory under {@value biolockj.Config#PROJECT_PIPELINE_DIR}
	 * <li>Call {@link biolockj.util.SeqUtil#initialize()} to set Config parameters based on sequence files
	 * <li>Call {@link biolockj.Pipeline#initializePipeline()} to initialize Pipeline modules
	 * <li>Call {@link biolockj.Pipeline#runPipeline()} or {@link biolockj.Pipeline#runDirectModule(Integer)} to execute
	 * pipeline modules.
	 * <li>Call {@link #removeTempFiles()} to complete clean up operation, and if
	 * {@link biolockj.Config}.{@value #PROJECT_DELETE_TEMP_FILES} = {@value biolockj.Config#TRUE}, delete temp
	 * directories
	 * <li>Call {@link #markProjectStatus(String)} to set the overall pipeline status
	 * </ol>
	 * <p>
	 * If pipeline has failed, attempt execute {@link biolockj.module.report.Email} (if configured) to notify user of
	 * failures.
	 *
	 * @param args - String[] runtime parameters passed to the Java program when launching BioLockJ
	 */
	public static void main( final String[] args )
	{
		try
		{
			System.out.println( "Staring BioLockj..." );
			MemoryUtil.reportMemoryUsage( "INTIAL MEMORY STATS" );
			RuntimeParamUtil.registerRuntimeParameters( args );
			Config.initialize();
			MetaUtil.initialize();

			if( RuntimeParamUtil.isDirectMode() )
			{
				Log.initialize( getDirectLogName( RuntimeParamUtil.getDirectModuleDir() ) );
			}
			else
			{
				Log.initialize( Config.requireString( Config.PROJECT_PIPELINE_NAME ) );
			}

			Log.info( BioLockJ.class, "Project name: " + Config.requireString( Config.PROJECT_PIPELINE_NAME ) );
			Log.info( BioLockJ.class, "Project directory: "
					+ Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath() );

			if( RuntimeParamUtil.doChangePassword() )
			{
				Email.encryptAndStoreEmailPassword( RuntimeParamUtil.getConfigFile(),
						RuntimeParamUtil.getAdminEmailPassword() );
				System.exit( 0 );
			}
			else if( RuntimeParamUtil.doRestart() )
			{
				initRestart();
			}
		}
		catch( final Exception ex )
		{
			printErrorFileAndExitProgram( args, ex );
		}

		try
		{
			if( MetaUtil.getMetadata() != null )
			{
				BioLockJ.copyFileToPipelineRoot( MetaUtil.getMetadata() );
			}

			SeqUtil.initialize();
			Pipeline.initializePipeline();

			if( !RuntimeParamUtil.isDirectMode() )
			{
				BioLockJUtil.saveNewMasterConfig( null );
			}

			if( RuntimeParamUtil.isDirectMode() )
			{
				Pipeline.runDirectModule( getDirectModuleID( RuntimeParamUtil.getDirectModuleDir() ) );
				singleModeSuccess = true;
			}
			else
			{
				if( Config.getBoolean( PROJECT_COPY_FILES ) )
				{
					SeqUtil.copyInputData();
				}

				Pipeline.runPipeline();

				if( Config.getBoolean( PROJECT_DELETE_TEMP_FILES ) )
				{
					removeTempFiles();
				}

				markProjectStatus( Pipeline.BLJ_COMPLETE );
				BioLockJUtil.sanitizeMasterConfig();
			}
		}
		catch( final Exception ex )
		{
			printFatalError( ex );
			markProjectStatus( Pipeline.BLJ_FAILED );
			logFinalException( args, ex );
			SummaryUtil.addSummaryFooterForFailedPipeline( getHelpInfo( null ) );
		}
		finally
		{
			try
			{
				if( RuntimeParamUtil.isDirectMode() )
				{
					setSingleModeStatus();
				}
			}
			catch( final Exception ex )
			{
				logFinalException( args, ex );
			}

			try
			{
				final String pipelineRoot = Config.getString( Config.PROJECT_PIPELINE_DIR );
				final String perm = Config.getString( PROJECT_PERMISSIONS );
				if( pipelineRoot != null && perm != null )
				{
					Job.setFilePermissions( Config.getExistingDir( Config.PROJECT_PIPELINE_DIR ), perm );
				}
			}
			catch( final Exception ex )
			{
				logFinalException( args, ex );
			}
		}
	}

	
	private static Integer getDirectModuleID( String moduleDir ) throws Exception
	{
		return Integer.valueOf( moduleDir.substring( 0, moduleDir.indexOf( "_" ) ) );
	}
	
	/**
	 * Create the pipeline root directory under $DOCKER_PROJ and save the path to
	 * {@link biolockj.Config}.{@value biolockj.Config#PROJECT_PIPELINE_DIR}.
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
	 * @throws Exception if errors occur
	 */
	protected static File createPipelineDirectory() throws Exception
	{
		final String year = String.valueOf( new GregorianCalendar().get( Calendar.YEAR ) );
		final String month = new GregorianCalendar().getDisplayName( Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH );
		final String day = BioLockJUtil.formatDigits( new GregorianCalendar().get( Calendar.DATE ), 2 );
		final String baseString = RuntimeParamUtil.getBaseDir().getAbsolutePath() + File.separator + getProjectName();
		final String dateString = "_" + year + month + day;
		File projectDir = new File( baseString + dateString );

		int i = 2;
		while( projectDir.exists() )
		{
			// throw new Exception( "Project already exists with today's date: " + projectDir.getAbsolutePath()
			// + ". Restart failed pipelines with -r or save the Config file with a unique name and try again." );

			projectDir = new File( baseString + "_" + i++ + dateString );
		}

		projectDir.mkdirs();
		return projectDir;
	}

	/**
	 * Initialize restarted pipeline by:
	 * <ol>
	 * <li>Initialize {@link biolockj.Log} file, name after {@value biolockj.Config#PROJECT_PIPELINE_NAME}
	 * <li>Update summary #Attempts count
	 * <li>If pipeline status = {@value biolockj.Pipeline#BLJ_COMPLETE}
	 * <li>Delete status file {@value biolockj.Pipeline#BLJ_FAILED} in pipeline root directory
	 * </ol>
	 * 
	 * @throws Exception if errors occur
	 */
	protected static void initRestart() throws Exception
	{
		Log.initialize( Config.requireString( Config.PROJECT_PIPELINE_NAME ) );
		Log.warn( BioLockJ.class, RETURN + Log.LOG_SPACER + RETURN + "RESTART PROJECT DIR --> "
				+ RuntimeParamUtil.getRestartDir().getAbsolutePath() + RETURN + Log.LOG_SPACER + RETURN );
		Log.info( BioLockJ.class, "Initializing Pipeline..." );

		SummaryUtil.updateNumAttempts();
		if( isPipelineComplete( RuntimeParamUtil.getRestartDir() ) )
		{
			throw new Exception( "RESTART FAILED!  Restart pipeline still shows status as: " + Pipeline.BLJ_COMPLETE
					+ " --> Check restart directory: " + RuntimeParamUtil.getRestartDir().getAbsolutePath() );
		}
		else
		{
			final File f = new File(
					RuntimeParamUtil.getRestartDir().getAbsolutePath() + File.separator + Pipeline.BLJ_FAILED );
			if( f.exists() )
			{
				if( !BioLockJUtil.deleteWithRetry( f, 5 ) )
				{
					Log.warn( BioLockJ.class, "Unable to delete " + f.getAbsolutePath() );
				}
			}
		}
	}

	/**
	 * Create indicator file in pipeline root directory, with name = status parameter.
	 * {@link biolockj.Config}.{@value biolockj.Config#PROJECT_PIPELINE_DIR}.
	 * 
	 * @param status Status indicator file name
	 */
	protected static void markProjectStatus( final String status )
	{
		try
		{
			Log.info( BioLockJ.class,
					"BioLockJ Pipeline [" + Config.requireString( Config.PROJECT_PIPELINE_NAME ) + "] = " + status );

			final File f = new File( Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath()
					+ File.separator + status );
			final FileWriter writer = new FileWriter( f );
			writer.close();
			if( !f.exists() )
			{
				throw new Exception( "Unable to create " + f.getAbsolutePath() );
			}
		}
		catch( final Exception ex )
		{
			Log.error( BioLockJ.class, "Unable to create pipeline status indicator file!", ex );
		}

	}

	/**
	 * Delete all {@link biolockj.module.BioModule}/{@value biolockj.module.BioModule#TEMP_DIR} folders.
	 *
	 * @throws Exception if unable to delete temp files
	 */
	protected static void removeTempFiles() throws Exception
	{
		Log.info( BioLockJ.class, "Cleaning up BioLockJ Modules..." );
		for( final BioModule bioModule: Pipeline.getModules() )
		{
			if( ModuleUtil.subDirExists( bioModule, BioModule.TEMP_DIR ) )
			{
				Log.info( BioLockJ.class, "Delete temp dir for BioLockJ Module: " + bioModule.getClass().getName() );
				BioLockJUtil.deleteWithRetry( ModuleUtil.requireSubDir( bioModule, BioModule.TEMP_DIR ), 10 );
			}
		}
	}

	private static String getDirectLogName( final String moduleDir ) throws Exception
	{
		final File modDir = new File( Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath()
				+ File.separator + moduleDir );
		if( !modDir.exists() )
		{
			throw new Exception( "Direct module directory not found --> " + modDir.getAbsolutePath() );
		}

		final File tempDir = new File( Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath()
				+ File.separator + moduleDir + BioModule.TEMP_DIR );

		if( !tempDir.exists() )
		{
			tempDir.mkdir();
		}

		return BioModule.TEMP_DIR + File.separator + moduleDir;

	}

	private static String getProjectName() throws Exception
	{
		String name = RuntimeParamUtil.getConfigFile().getName();
		final String[] exts = { ".ascii", ".asc", ".plain", ".rft", ".tab", ".text", ".tsv", ".txt", ".properties",
				".prop", ".props", ".config" };

		for( final String ext: exts )
		{
			if( name.toLowerCase().endsWith( ext ) )
			{
				name = name.substring( 0, name.length() - ext.length() );
			}
		}

		if( name.startsWith( BioLockJUtil.MASTER_PREFIX ) )
		{
			name = name.replace( BioLockJUtil.MASTER_PREFIX, "" );
		}

		return name;
	}

	/**
	 * Determine project status based on existence of {@value biolockj.Pipeline#BLJ_COMPLETE} in pipeline root
	 * directory.
	 *
	 * @param projDir File path for the pipeline
	 * @return true if {@value biolockj.Pipeline#BLJ_COMPLETE} exists in the pipeline root directory, otherwise false
	 */
	private static boolean isPipelineComplete( final File projDir )
	{
		final File f = new File( projDir.getAbsolutePath() + File.separator + Pipeline.BLJ_COMPLETE );
		return f.exists();
	}

	private static void logFinalException( final String[] args, final Exception ex )
	{
		if( Log.getFile() != null )
		{
			Log.error( BioLockJ.class, "FATAL APPLICATION ERROR - " + ex.getMessage() + " --> Program args: "
					+ BioLockJUtil.getCollectionAsString( Arrays.asList( args ) ), ex );
		}
		else
		{
			System.out.println( "FATAL APPLICATION ERROR - " + ex.getMessage() + " --> Program args: "
					+ BioLockJUtil.getCollectionAsString( Arrays.asList( args ) ) );
		}
		ex.printStackTrace();
	}

	/**
	 * Print the {@link biolockj.Log} messages saved via {@link biolockj.Log#addMsg(String)} and the exception stack
	 * trace info to the $USER $HOME directory.
	 * 
	 * @param fatalException Fatal application Exception
	 */
	private static void printErrorFileAndExitProgram( final String[] args, final Exception fatalException )
	{
		try
		{
			Log.error( BioLockJ.class, "Pipeline failed before root directory or Log file was created!" );
			String suffix = null;
			try
			{
				if( Config.getString( Config.PROJECT_PIPELINE_NAME ) != null )
				{
					suffix = Config.getString( Config.PROJECT_PIPELINE_NAME );
				}
				else if( RuntimeParamUtil.getConfigFile() != null )
				{
					suffix = RuntimeParamUtil.getConfigFile().getName();
				}
				else
				{
					suffix = "Config_param_not_found";
				}
			}
			catch( final Exception ex )
			{
				suffix = "Config_initialzation_failed";
			}

			int index = 0;
			final String prefix = ( RuntimeParamUtil.isDockerMode() ? DockerUtil.CONTAINER_OUTPUT_DIR: "~" )
					+ File.separator;
			File errFile = new File( Config.getSystemFilePath( prefix + FATAL_ERROR_FILE_PREFIX + suffix + LOG_EXT ) );
			while( errFile.exists() )
			{
				errFile = new File( Config.getSystemFilePath( prefix + FATAL_ERROR_FILE_PREFIX + suffix + "_"
						+ new Integer( ++index ).toString() + LOG_EXT ) );
			}

			System.out.println( getHelpInfo( errFile ) );

			printFatalError( fatalException );

			final BufferedWriter writer = new BufferedWriter( new FileWriter( errFile ) );
			try
			{
				for( final String msg: Log.getMsgs() )
				{
					writer.write( msg + RETURN );
				}
			}
			finally
			{
				if( writer != null )
				{
					writer.close();
				}
			}

		}
		catch( final Exception ex )
		{
			System.out.println( "Unable to access Log or write to $USER $HOME directory!" );
			ex.printStackTrace();
			RuntimeParamUtil.printRuntimeArgs( args, true );
			printFatalError( ex );
		}
		finally
		{
			System.exit( 1 );
		}
	}

	private static void printFatalError( final Exception ex )
	{
		Log.error( BioLockJ.class, Log.LOG_SPACER );
		Log.error( BioLockJ.class, "Fatal Exception: " + ex.getMessage() );
		for( int i = 0; i < ex.getStackTrace().length; i++ )
		{
			Log.error( BioLockJ.class, ex.getStackTrace()[ i ].toString() );
		}
		Log.error( BioLockJ.class, Log.LOG_SPACER );
	}

	private static void setSingleModeStatus() throws Exception
	{
		Log.info( BioLockJ.class, "Reporting Direct module status" );
		final JavaModule module = (JavaModuleImpl) Pipeline.getModules()
				.get( getDirectModuleID( RuntimeParamUtil.getDirectModuleDir() ) );
		if( singleModeSuccess )
		{
			Log.info( BioLockJ.class, "Save success status for direct module: " + module.getClass().getName() );
			module.moduleComplete();
		}
		else
		{
			Log.info( BioLockJ.class, "Save failure status for direct module: " + module.getClass().getName() );
			module.moduleFailed();
		}

		SummaryUtil.reportSuccess( module );
	}

	/**
	 * Captures the application start time
	 */
	public static final long APP_START_TIME = System.currentTimeMillis();

	/**
	 * URL to the BioLockJ WIKI
	 */
	public static final String BLJ_WIKI = "https://github.com/msioda/BioLockJ/wiki";

	/**
	 * Gzip compressed file extension constant: {@value #GZIP_EXT}
	 */
	public static final String GZIP_EXT = ".gz";

	/**
	 * BioLockJ log file extension constant: {@value #LOG_EXT}
	 */
	public static final String LOG_EXT = ".log";

	/**
	 * BioLockJ PDF file extension constant: {@value #PDF_EXT}
	 */
	public static final String PDF_EXT = ".pdf";

	/**
	 * {@link biolockj.Config} property set to copy input files into pipeline root directory:
	 * {@value #PROJECT_COPY_FILES}
	 */
	public static final String PROJECT_COPY_FILES = "project.copyInput";

	/**
	 * {@link biolockj.Config} property set to delete {@link biolockj.module.BioModule#getTempDir()} files:
	 * {@value #PROJECT_DELETE_TEMP_FILES}
	 */
	public static final String PROJECT_DELETE_TEMP_FILES = "project.deleteTempFiles";

	/**
	 * Return character constant *backslash-n*
	 */
	public static final String RETURN = "\n";

	/**
	 * BioLockJ shell script file extension constant: {@value #SH_EXT}
	 */
	public static final String SH_EXT = ".sh";

	/**
	 * BioLockJ tab character constant: {@value #TAB_DELIM}
	 */
	public static final String TAB_DELIM = "\t";

	/**
	 * BioLockJ tab delimited text file extension constant: {@value #TSV_EXT}
	 */
	public static final String TSV_EXT = ".tsv";

	/**
	 * BioLockJ standard text file extension constant: {@value #TXT_EXT}
	 */
	public static final String TXT_EXT = ".txt";

	/**
	 * {@link biolockj.Config} property to define permission setttings when running chmod on pipeline root dir:
	 * {@value #PROJECT_PERMISSIONS}
	 */
	protected static final String PROJECT_PERMISSIONS = "project.permissions";

	private static final String FATAL_ERROR_FILE_PREFIX = "BioLockJ_FATAL_ERROR_";

	private static boolean singleModeSuccess = false;
}
