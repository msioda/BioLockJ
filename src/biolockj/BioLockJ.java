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
import java.io.FileFilter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.ScriptModule;
import biolockj.module.report.Email;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.RuntimeParamUtil;
import biolockj.util.SeqUtil;
import biolockj.util.SummaryUtil;

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
		final String rootPath = Config.getExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath();
		final File localFile = new File( rootPath + File.separator + file.getName() );
		if( !localFile.exists() )
		{
			FileUtils.copyFileToDirectory( file, Config.getExistingDir( Config.INTERNAL_PIPELINE_DIR ) );
			if( !localFile.exists() )
			{
				throw new Exception( "Unable to copy file to pipeline root directory: " + file.getAbsolutePath() );
			}

		}
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
	 * <li>Call {@link biolockj.Config#initialize(File)} to load project properties
	 * <li>Call {@link biolockj.util.MetaUtil#initialize()} to verify metadata dependencies
	 * <li>Copy {@link biolockj.Config} file and nested {@value biolockj.Config#PROJECT_DEFAULT_PROPS} files into
	 * {@value biolockj.Config#INTERNAL_PIPELINE_DIR} to preserve the state of these files at runtime.
	 * <li>Initialize {@link Log} using /resources/log4J.properties
	 * <li>If {@value #PROJECT_COPY_FILES} = {@value biolockj.Config#TRUE}, copy input files into a new "input"
	 * directory under {@value biolockj.Config#INTERNAL_PIPELINE_DIR}
	 * <li>Call {@link biolockj.util.SeqUtil#initialize()} to set Config parameters based on sequence files
	 * <li>Call {@link biolockj.Pipeline#initializePipeline()} to initialize Pipeline modules
	 * <li>Call {@link biolockj.Pipeline#runPipeline()} or {@link biolockj.Pipeline#runDirectModule(String)} to execute
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
			RuntimeParamUtil.registerRuntimeParameters( args );

			Config.initialize( RuntimeParamUtil.getConfigFile() );

			Config.setConfigProperty( Config.INTERNAL_PIPELINE_NAME, getProjectName() );

			MetaUtil.initialize();

			if( RuntimeParamUtil.doChangePassword() )
			{
				Email.encryptAndStoreEmailPassword( RuntimeParamUtil.getConfigFile(),
						RuntimeParamUtil.getAdminEmailPassword() );
				System.exit( 0 );
			}
			else if( RuntimeParamUtil.isDirectMode() )
			{
				Config.setConfigProperty( Config.INTERNAL_PIPELINE_DIR, getRestartDir().getAbsolutePath() );
				Log.initialize( getDirectLogName( RuntimeParamUtil.getDirectModule() ) );
			}
			else if( RuntimeParamUtil.doRestart() )
			{
				Config.setConfigProperty( Config.INTERNAL_PIPELINE_DIR, getRestartDir().getAbsolutePath() );
				SummaryUtil.updateNumAttempts();
				Log.initialize( Config.requireString( Config.INTERNAL_PIPELINE_NAME ) );
			}
			else
			{
				Config.setConfigProperty( Config.INTERNAL_PIPELINE_DIR, createPipelineDirectory().getAbsolutePath() );
				Log.initialize( Config.requireString( Config.INTERNAL_PIPELINE_NAME ) );
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
				BioLockJUtil.saveNewMasterConfig();
			}

			if( RuntimeParamUtil.isDirectMode() )
			{
				Pipeline.runDirectModule( RuntimeParamUtil.getDirectModule() );
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
			}

		}
		catch( final Exception ex )
		{
			printFatalError( ex );
			if( !RuntimeParamUtil.isDirectMode() )
			{
				markProjectStatus( Pipeline.BLJ_FAILED );
			}

			logFinalException( args, ex );
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
				String pipelineRoot = Config.getString( Config.INTERNAL_PIPELINE_DIR );
				String perm = Config.getString( PROJECT_PERMISSIONS );
				if( pipelineRoot != null && perm != null )
				{
					Job.setFilePermissions( Config.getExistingDir( Config.INTERNAL_PIPELINE_DIR ), perm, true );
				}
			}
			catch( final Exception ex )
			{
				logFinalException( args, ex );
			}
		}
	}
	

	/**
	 * Create the pipeline root directory under $DOCKER_PROJ and save the path to
	 * {@link biolockj.Config}.{@value biolockj.Config#INTERNAL_PIPELINE_DIR}.
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
	 * @throws Exception if projectDir already exists and optionalParam != restart
	 */
	protected static File createPipelineDirectory() throws Exception
	{
		final String year = String.valueOf( new GregorianCalendar().get( Calendar.YEAR ) );
		final String month = new GregorianCalendar().getDisplayName( Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH );
		final String day = BioLockJUtil.formatDigits( new GregorianCalendar().get( Calendar.DATE ), 2 );
		final File projectDir = new File( RuntimeParamUtil.getBaseDir().getAbsolutePath() + File.separator
				+ Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + "_" + year + month + day );

		if( projectDir.exists() )
		{
			throw new Exception( "Project already exists with today's date: " + projectDir.getAbsolutePath()
					+ ".  Restart failed pipelines with -r or save the Config file with a unique name and try again." );
		}

		projectDir.mkdirs();
		return projectDir;
	}

	/**
	 * Method called if restart flag passed to BioLockJ to look in $DOCKER_PROJ for the most recent pipeline sharing the
	 * same project name. Delete status indicator if {@value biolockj.Pipeline#BLJ_FAILED} found in root directory.
	 *
	 * @return File - the existing pipeline root directory to restart
	 * @throws Exception thrown if the most recent pipeline with the same project name is not found or ran successfully
	 */
	protected static File getRestartDir() throws Exception
	{
		File restartDir = null;
		GregorianCalendar mostRecent = null;
		Log.info( BioLockJ.class,
				"Looking for most recent pipeline for: " + Config.requireString( Config.INTERNAL_PIPELINE_NAME ) );
		final FileFilter ff = new WildcardFileFilter( Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + "*" );
		final File[] dirs = RuntimeParamUtil.getBaseDir().listFiles( ff );

		for( final File d: dirs )
		{
			if( !d.isDirectory()
					|| d.getName().length() != Config.requireString( Config.INTERNAL_PIPELINE_NAME ).length() + 10 )
			{
				continue;
			}

			final String name = d.getName();
			final int len = name.length();
			final String year = name.substring( len - 9, len - 5 );
			final String mon = name.substring( len - 5, len - 2 );
			final String day = name.substring( len - 2 );
			final Date date = new SimpleDateFormat( "yyyyMMMdd" ).parse( year + mon + day );
			final GregorianCalendar projectDate = new GregorianCalendar();
			projectDate.setTime( date );

			// Value > 0 if projectDate has a more recent date than mostRecent
			if( mostRecent == null || projectDate.compareTo( mostRecent ) > 0 )
			{
				Log.info( BioLockJ.class, "Found previous pipeline run = " + d.getAbsolutePath() );
				restartDir = d;
				mostRecent = projectDate;
			}
		}

		if( restartDir == null )
		{
			throw new Exception(
					"Unalbe to locate restart directory in --> " + RuntimeParamUtil.getBaseDir().getAbsolutePath() );
		}

		if( isPipelineComplete( restartDir ) )
		{
			throw new Exception(
					"RESTART FAILED!  Pipeline already ran successfully: " + restartDir.getAbsolutePath() );
		}
		else
		{
			final File f = new File( restartDir.getAbsolutePath() + File.separator + Pipeline.BLJ_FAILED );
			if( f.exists() )
			{
				FileUtils.forceDelete( f );
			}
		}

		Log.warn( BioLockJ.class, RETURN );
		Log.warn( BioLockJ.class, Log.LOG_SPACER );
		Log.warn( BioLockJ.class, "RESTART PROJECT DIR --> " + restartDir.getAbsolutePath() );
		Log.warn( BioLockJ.class, Log.LOG_SPACER );
		Log.warn( BioLockJ.class, RETURN );

		return restartDir;
	}

	/**
	 * Create indicator file in pipeline root directory, with name = status parameter.
	 * {@link biolockj.Config}.{@value biolockj.Config#INTERNAL_PIPELINE_DIR}.
	 * 
	 * @param status Status indicator file name
	 */
	protected static void markProjectStatus( final String status )
	{
		try
		{
			Log.info( BioLockJ.class,
					"BioLockJ Pipeline [" + Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + "] = " + status );

			final File f = new File( Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath()
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
		Log.get( BioLockJ.class ).info( "Cleaning up BioLockJ Modules..." );
		for( final BioModule bioModule: Pipeline.getModules() )
		{
			if( ModuleUtil.subDirExists( bioModule, BioModule.TEMP_DIR ) )
			{
				Log.get( BioLockJ.class )
						.info( "Delete temp dir for BioLockJ Module: " + bioModule.getClass().getName() );
				FileUtils.forceDelete( ModuleUtil.requireSubDir( bioModule, BioModule.TEMP_DIR ) );
			}
		}
	}

	private static String getDirectLogName( final String className ) throws Exception
	{
		final ScriptModule module = (ScriptModule) Class.forName( className ).newInstance();
		final String name = module.getClass().getSimpleName();
		final File pipelinDir = Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR );
		final Collection<File> dirs = FileUtils.listFilesAndDirs( pipelinDir, FalseFileFilter.INSTANCE,
				TrueFileFilter.INSTANCE );
		for( final File moduleDir: dirs )
		{
			if( moduleDir.getName().endsWith( "_" + name ) )
			{
				final File tempDir = new File( moduleDir.getAbsolutePath() + File.separator + BioModule.TEMP_DIR );
				tempDir.mkdir();
				final String logName = moduleDir.getName() + File.separator + BioModule.TEMP_DIR + File.separator
						+ name;
				return logName;
			}

		}

		throw new Exception(
				"Unable to find module [" + className + "] directory under " + pipelinDir.getAbsolutePath() );
	}

	private static String getProjectName() throws Exception
	{
		String name = RuntimeParamUtil.getConfigFile().getName();
		final String[] exts = { ".ascii", ".asc", ".plain", ".rft", ".tab", ".text", ".tsv", ".txt", ".properties",
				".prop", ".config" };

		for( final String ext: exts )
		{
			if( name.toLowerCase().endsWith( ext ) )
			{
				name = name.substring( 0, name.length() - ext.length() );
			}
		}

		Log.info( BioLockJ.class, "Set project name to: " + name );
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
			Log.get( BioLockJ.class ).error( "BioLockJ exception: " + ex.getMessage() + " --> Program args: "
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
			Log.info( BioLockJ.class, "Pipeline failed before root directory or Log file was created!" );
			printFatalError( fatalException );

			String suffix = null;
			try
			{
				if( Config.getString( Config.INTERNAL_PIPELINE_NAME ) != null )
				{
					suffix = Config.getString( Config.INTERNAL_PIPELINE_NAME );
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
			final String prefix = ( RuntimeParamUtil.isDockerMode() ? RuntimeParamUtil.getBaseDir().getAbsolutePath()
					: "~" ) + File.separator;
			File errFile = new File( Config.getSystemFilePath( prefix + FATAL_ERROR_FILE_PREFIX + suffix + ".log" ) );
			while( errFile.exists() )
			{
				errFile = new File( Config.getSystemFilePath( prefix + FATAL_ERROR_FILE_PREFIX + suffix + "_"
						+ new Integer( ++index ).toString() + ".log" ) );
			}

			final BufferedWriter writer = new BufferedWriter( new FileWriter( errFile ) );
			for( final String msg: Log.getMsgs() )
			{
				writer.write( msg + RETURN );
			}
			writer.close();

		}
		catch( final Exception ex )
		{
			System.out.println( "Unable to access Log or write to $USER $HOME directory!" );
			ex.printStackTrace();
			RuntimeParamUtil.printRuntimeArgs( args, true );
			printFatalError( fatalException );
		}
		finally
		{
			System.exit( 1 );
		}
	}

	private static void printFatalError( final Exception ex )
	{
		Log.warn( BioLockJ.class, Log.LOG_SPACER );
		Log.warn( BioLockJ.class, "Fatal Exception: " + ex.getMessage() );
		for( int i = 0; i < ex.getStackTrace().length; i++ )
		{
			Log.warn( BioLockJ.class, ex.getStackTrace()[ i ].toString() );
		}
		Log.warn( BioLockJ.class, Log.LOG_SPACER );
	}

	private static void setSingleModeStatus() throws Exception
	{
		Log.info( BioLockJ.class, "Reporting Direct module status" );
		final JavaModule module = (JavaModuleImpl) ModuleUtil.getModule( RuntimeParamUtil.getDirectModule() );
		if( singleModeSuccess )
		{
			Log.info( BioLockJ.class, "Save success status" );
			module.moduleComplete();
		}
		else
		{
			Log.info( BioLockJ.class, "Save failure status" );
			module.moduleFailed();
		}

		SummaryUtil.reportSuccess( module );
	}

	/**
	 * Captures the application start time
	 */
	public static final long APP_START_TIME = System.currentTimeMillis();

	/**
	 * {@link biolockj.Config} property set to copy input files into pipeline root directory:
	 * {@value #PROJECT_COPY_FILES}
	 */
	public static final String PROJECT_COPY_FILES = "project.copyInput";
	
	/**
	 * {@link biolockj.Config} property to define permission setttings when running chmod on pipeline root dir:
	 * {@value #PROJECT_PERMISSIONS}
	 */
	protected static final String PROJECT_PERMISSIONS = "project.permissions";


	/**
	 * {@link biolockj.Config} property set to delete {@link biolockj.module.BioModule#getTempDir()} files:
	 * {@value #PROJECT_DELETE_TEMP_FILES}
	 */
	public static final String PROJECT_DELETE_TEMP_FILES = "project.deleteTempFiles";

	/**
	 * Return character: {@value #RETURN}
	 */
	public static final String RETURN = "\n";

	/**
	 * Tab delimiter character: {@value #TAB_DELIM}
	 */
	public static final String TAB_DELIM = "\t";

	private static final String FATAL_ERROR_FILE_PREFIX = "BioLockJ_FATAL_ERROR_";

	private static boolean singleModeSuccess = false;
}
