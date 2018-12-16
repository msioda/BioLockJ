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
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.ScriptModule;
import biolockj.module.report.Email;
import biolockj.util.*;

/**
 * This class initializes and executes each {@link biolockj.module.BioModule} configured for execution.<br>
 * {@link biolockj.module.BioModule}s that generate scripts are monitored until all scripts are complete, before moving
 * on to the next module.<br>
 */
public class Pipeline
{

	private Pipeline()
	{}

	/**
	 * Return a list of {@link biolockj.module.BioModule}s constructed by the {@link biolockj.BioModuleFactory}
	 *
	 * @return List of BioModules
	 */
	public static List<BioModule> getModules()
	{
		return bioModules;
	}

	/**
	 * Return {@value #SCRIPT_SUCCESS} if no pipelineException has been thrown, otherwise return
	 * {@value #SCRIPT_FAILURES}
	 *
	 * @return pipeline status (success or failed)
	 * @throws Exception if unable to get status for all modules
	 */
	public static String getStatus() throws Exception
	{
		return pipelineException == null ? Pipeline.SCRIPT_SUCCESS: Pipeline.SCRIPT_FAILURES;
	}

	/**
	 * This method initializes the Pipeline by building the modules and checking module dependencies.
	 * 
	 * @throws Exception if errors occur
	 */
	public static void initializePipeline() throws Exception
	{
		bioModules = BioModuleFactory.buildModules();
		Config.setConfigProperty( Config.INTERNAL_ALL_MODULES, BioLockJUtil.getClassNames( bioModules ) );
		int modNum = 0;
		info( "Pipeline Module Execution Order:" );
		for( final BioModule bioModule: bioModules )
		{
			info( "BioModule[" + modNum++ + "] = " + bioModule.getClass().getName() );
		}
		initializeModules();
	}

	/**
	 * If moduleName is null, run all modules, otherwise only run the specified module.
	 * 
	 * @param moduleName Name of a single module to run
	 * @throws Exception if any fatal error occurs during execution
	 */
	public static void runDirectModule( final String moduleName ) throws Exception
	{
		Log.info( Pipeline.class, "Run Direct BioModule: " + moduleName );
		final BioModule module = ModuleUtil.getModule( moduleName );
		if( module instanceof JavaModule )
		{
			( (JavaModule) module ).runModule();
			refreshOutputMetadata( module );
			module.cleanUp();
		}
		else
		{
			executeScript( (ScriptModule) module );
		}
	}

	/**
	 * This method initializes and executes the BioModules set in the BioLockJ configuration file.<br>
	 * 
	 * @throws Exception if any fatal error occurs during execution
	 */
	public static void runPipeline() throws Exception
	{
		try
		{
			executeModules();
			SummaryUtil.reportSuccess( null );
		}
		catch( final Exception ex )
		{
			Log.error( Pipeline.class, "Pipeline failed! " + ex.getMessage(), ex );
			pipelineException = ex;
			SummaryUtil.reportFailure( ex );
			try
			{
				final BioModule emailMod = ModuleUtil.getModule( Email.class.getName() );
				if( emailMod != null && !ModuleUtil.isIncomplete( emailMod ) )
				{
					emailMod.executeTask();
				}
				else
				{
					Log.error( Pipeline.class, "Email module failed! " + ex.getMessage(), ex );
				}
			}
			catch( final Exception innerEx )
			{
				Log.error( Pipeline.class,
						"Attempt to send Email after pipeline failure has also failed!  " + innerEx.getMessage(),
						innerEx );
			}

			throw ex;
		}
		finally
		{
			Log.info( Pipeline.class, "Log Pipeline Summary" + BioLockJ.RETURN + SummaryUtil.getSummary() );
		}
	}

	/**
	 * Delete and recreate incomplete module directory.
	 * 
	 * @param module BioModule
	 * @throws Exception if errors occur
	 */
	protected static void deleteIncompleteModule( final BioModule module ) throws Exception
	{
		Log.info( Pipeline.class, "Reset incomplete module: " + module.getModuleDir().getAbsolutePath() );
		if( BioLockJUtil.deleteWithRetry( module.getModuleDir(), 10 ) )
		{
			module.setModuleDir( ModuleUtil.getModuleRootDir( module ) ); // recreate deleted directory
		}
		else if( module.getModuleDir().listFiles().length > 0 )
		{
			for( final File f: module.getModuleDir().listFiles() )
			{
				if( BioLockJUtil.deleteWithRetry( f, 5 ) )
				{
					Log.info( Pipeline.class, "Deleted: " + f.getAbsolutePath() );
				}
				else
				{
					throw new Exception( "Unable to Delete: " + f.getAbsolutePath() );
				}
			}
		}
	}

	/**
	 * This method executes all new and incomplete modules<br>
	 * Before/after a module is executed, set persistent module status by creating status indicator files. Incomplete
	 * modules have an empty file {@value #BLJ_STARTED} in the module directory.<br>
	 * Complete modules have an empty file {@value #BLJ_COMPLETE} in the module directory.<br>
	 * {@link biolockj.module.BioModule}s are run in the order listed in the {@link biolockj.Config} file.<br>
	 * <p>
	 * Execution steps:
	 * <ol>
	 * <li>File {@value #BLJ_STARTED} is added to the module directory
	 * <li>Execute {@link #refreshOutputMetadata(BioModule)} to cache updated metadata, if generatede.
	 * <li>Run module scripts, if any, polling 1/minute for status until all scripts complete or time out.
	 * <li>File {@value #BLJ_STARTED} is replaced by {@value #BLJ_COMPLETE} as status indicator
	 * </ol>
	 *
	 * @throws Exception if script errors occur
	 */
	protected static void executeModules() throws Exception
	{
		BioModule prevModule = null;
		for( final BioModule module: Pipeline.getModules() )
		{
			if( !ModuleUtil.isComplete( module ) )
			{
				ModuleUtil.markStarted( module );
				refreshOutputMetadata( prevModule );
				refreshRCacheIfNeeded( module );
				module.executeTask();

				final boolean isJava = module instanceof JavaModule;
				final boolean runScripts = ModuleUtil.getMainScript( module ) != null;

				if( runScripts )
				{
					Job.submit( (ScriptModule) module );
					pollAndSpin( (ScriptModule) module );
				}

				refreshOutputMetadata( module );
				module.cleanUp();

				if( !isJava || !runScripts )
				{
					SummaryUtil.reportSuccess( module );
				}

				ModuleUtil.markComplete( module );
			}
			else
			{
				Log.debug( Pipeline.class,
						"Skipping succssfully completed BioLockJ Module: " + module.getClass().getName() );
			}
			prevModule = module;
		}
	}

	/**
	 * If the bioModule is complete and contains a metadata file in its output directory, return the metadata file,
	 * since it must be a new version.
	 *
	 * @param bioModule BioModule
	 * @return New metadata file (or null)
	 */
	protected static File getMetadata( final BioModule bioModule )
	{
		if( bioModule != null )
		{
			final File metadata = new File(
					bioModule.getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );

			if( metadata.exists() )
			{
				return metadata;
			}
		}

		return null;
	}

	/**
	 * Initialization occurs by calling {@link biolockj.module.BioModule} methods on configured modules<br>
	 * <ol>
	 * <li>Create module sub-directories under {@value biolockj.Config#INTERNAL_PIPELINE_DIR} as ordered in
	 * {@link biolockj.Config} file.<br>
	 * <li>Reset the {@link biolockj.util.SummaryUtil} module so previous summary descriptions can be used for completed
	 * modules
	 * <li>Delete incomplete module contents if restarting a failed pipeline
	 * {@value biolockj.module.BioModule#OUTPUT_DIR} directory<br>
	 * <li>Call {@link #refreshOutputMetadata(BioModule)} to cache metadata if output by a complete module<br>
	 * <li>Call {@link #refreshRCacheIfNeeded(BioModule)} to cache R fields after 1st R module runs<br>
	 * <li>Verify dependencies with {@link biolockj.module.BioModule#checkDependencies()}<br>
	 * </ol>
	 *
	 * @throws Exception thrown if propagated by called methods
	 * @return true if no errors are thrown
	 */
	protected static boolean initializeModules() throws Exception
	{
		for( final BioModule module: getModules() )
		{
			module.setModuleDir( ModuleUtil.getModuleRootDir( module ) );
			if( ModuleUtil.isIncomplete( module ) && !RuntimeParamUtil.isDirectMode() || module instanceof Email )
			{
				deleteIncompleteModule( module );
			}
			else if( ModuleUtil.isComplete( module ) )
			{
				module.cleanUp();
				refreshOutputMetadata( module );
				refreshRCacheIfNeeded( module );
			}
			else if( !RuntimeParamUtil.isDockerMode() || DockerUtil.isManager() || RuntimeParamUtil.isDirectMode()
					&& RuntimeParamUtil.getDirectModule().equals( module.getClass().getName() ) )
			{
				info( "Check dependencies for: " + module.getClass().getName() );
				module.checkDependencies();
			}
			else
			{
				Log.debug( Pipeline.class, "No initialization for: " + module.getClass().getName() );
			}
		}

		return true;
	}

	/**
	 * The {@link biolockj.module.ScriptModule#getScriptDir()} will contain one main script and one ore more worker
	 * scripts.<br>
	 * An empty file with {@value #SCRIPT_STARTED} appended to the script name is created when execution begins.<br>
	 * If successful, an empty file with {@value #SCRIPT_SUCCESS} appended to the script name is created.<br>
	 * Upon failure, an empty file with {@value #SCRIPT_FAILURES} appended to the script name is created.<br>
	 * Script status is polled each minute, determining status by counting indicator files.<br>
	 * {@link biolockj.Log} outputs the # of started, failed, and successful scripts (if any change).<br>
	 * {@link biolockj.Log} repeats the previous message every 10 minutes if no status change is detected.<br>
	 *
	 * @param module ScriptModule
	 * @return true if all scripts are complete, regardless of status
	 * @throws Exception thrown to end pipeline execution
	 */
	protected static boolean poll( final ScriptModule module ) throws Exception
	{
		final File mainScript = ModuleUtil.getMainScript( module );
		final IOFileFilter ff = new WildcardFileFilter( "*.sh" );
		final Collection<File> scriptFiles = FileUtils.listFiles( module.getScriptDir(), ff, null );
		final File mainSuccess = new File( mainScript.getAbsolutePath() + "_" + SCRIPT_SUCCESS );
		final File mainFailed = new File( mainScript.getAbsolutePath() + "_" + SCRIPT_FAILURES );
		int numSuccess = 0;
		int numStarted = 0;
		int numFailed = 0;

		scriptFiles.remove( mainScript );

//		Log.debug( Pipeline.class, "mainScript = " + mainScript.getAbsolutePath() );
//		for( final File f: scriptFiles )
//		{
//			Log.debug( Pipeline.class, "Worker Script = " + f.getAbsolutePath() );
//		}

		for( final File f: scriptFiles )
		{
			final File testStarted = new File( f.getAbsolutePath() + "_" + SCRIPT_STARTED );
			final File testSuccess = new File( f.getAbsolutePath() + "_" + SCRIPT_SUCCESS );
			final File testFailure = new File( f.getAbsolutePath() + "_" + SCRIPT_FAILURES );
			numStarted = numStarted + ( testStarted.exists() ? 1: 0 );
			numSuccess = numSuccess + ( testSuccess.exists() ? 1: 0 );
			numFailed = numFailed + ( testFailure.exists() ? 1: 0 );
		}

		int numScripts = scriptFiles.size();
		if( !mainScript.getName().endsWith( ".sh" ) ) // must be R script
		{
			numScripts = 1;
			numStarted = 1;
			numSuccess = mainSuccess.exists() ? 1: numSuccess;
			numFailed = mainFailed.exists() ? 1: numFailed;
		}

		final int numRunning = numStarted - numSuccess - numFailed;
		final File failure = mainFailed.exists() ? mainFailed: null;
		final String logMsg = mainScript.getName() + " Status (Total=" + numScripts + "): Success=" + numSuccess
				+ "; Failed=" + numFailed + "; Running=" + numRunning + "; Queued=" + ( numScripts - numStarted );

		if( !statusMsg.equals( logMsg ) )
		{
			statusMsg = logMsg;
			pollCount = 0;
			Log.info( Pipeline.class, logMsg );
		}
		else if( ++pollCount % 10 == 0 )
		{
			Log.info( Pipeline.class, logMsg );
		}

		if( mainFailed.exists() || failure != null && failure.exists() )
		{
			final String failMsg = "SCRIPT FAILED: "
					+ BioLockJUtil.getCollectionAsString( ModuleUtil.getScriptErrors( module ) );
			Log.warn( Pipeline.class, failMsg );
			throw new Exception( failMsg );
		}

		return numSuccess + numFailed == numScripts;
	}

	/**
	 * Call {@link biolockj.util.MetaUtil} to refresh the metadata cache if a new metadata file was output by the
	 * bioModule .
	 * 
	 * @param module BioModule
	 * @throws Exception if unable to refresh the cache
	 */
	protected static void refreshOutputMetadata( final BioModule module ) throws Exception
	{
		if( module != null )
		{
			final File metadata = new File(
					module.getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );

			if( metadata.exists() )
			{
				MetaUtil.setFile( metadata );
				MetaUtil.refreshCache();
			}
		}
	}

	/**
	 * Refresh R cache if about to run the 1st R module.
	 * 
	 * @param module BioModule
	 * @throws Exception if errors occur
	 */
	protected static void refreshRCacheIfNeeded( final BioModule module ) throws Exception
	{
		if( ModuleUtil.isFirstRModule( module ) )
		{
			Log.info( Pipeline.class, "Refresh R-cache before running 1st R module: " + module.getClass().getName() );
			RMetaUtil.classifyReportableMetadata();
			BioLockJUtil.updateMasterConfig( RMetaUtil.getUpdatedRConfig() );
		}
	}

	private static void executeScript( final ScriptModule module ) throws Exception
	{
		ModuleUtil.markStarted( module );
		module.executeTask();
		Job.submit( module );
		pollAndSpin( module );
		refreshOutputMetadata( module );
		module.cleanUp();
	}

	private static void info( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( Pipeline.class, msg );
		}
	}

	/**
	 * This method calls poll to check status of scripts and then sleeps for pollTime (60) seconds.
	 *
	 * @param mainScript
	 * @throws Exception
	 */
	private static void pollAndSpin( final ScriptModule module ) throws Exception
	{
		Log.info( Pipeline.class, "Java program wakes every 60 seconds to check execution progress" );
		Log.info( Pipeline.class,
				"Status determined by existance of indicator files in " + module.getScriptDir().getAbsolutePath() );
		Log.info( Pipeline.class, "Indicator files end with: \"_" + SCRIPT_STARTED + "\", \"_" + SCRIPT_SUCCESS
				+ "\", or \"_" + SCRIPT_FAILURES + "\"" );
		Log.info( Pipeline.class, "If any change to #Success/#Failed/#Running/#Queued changed, new values logged" );
		Log.info( Pipeline.class,
				"The status message repeats every 10 minutes while scripts are executing (if status remains unchanged)." );

		int numMinutes = 0;
		boolean finished = false;
		while( !finished )
		{
			finished = poll( module );
			if( !finished )
			{
				if( module.getTimeout() != null && module.getTimeout() > 0 && numMinutes++ >= module.getTimeout() )
				{
					throw new Exception( ModuleUtil.getMainScript( module ).getAbsolutePath() + " timed out after "
							+ numMinutes + " minutes." );
				}

				Thread.sleep( POLL_TIME * 1000 );
			}
		}
	}

	/**
	 * Name of the file created in the BioModule or {@value biolockj.Config#INTERNAL_PIPELINE_DIR} root directory to
	 * indicate execution was successful: {@value #BLJ_COMPLETE}
	 */
	public static final String BLJ_COMPLETE = "biolockjComplete";

	/**
	 * Name of the file created in the {@value biolockj.Config#INTERNAL_PIPELINE_DIR} root directory to indicate fatal
	 * application errors halted execution: {@value #BLJ_FAILED}
	 */
	public static final String BLJ_FAILED = "biolockjFailed";

	/**
	 * Name of the file created in the BioModule root directory to indicate execution has started: {@value #BLJ_STARTED}
	 */
	public static final String BLJ_STARTED = "biolockjStarted";

	/**
	 * File suffix appended to failed scripts: {@value #SCRIPT_FAILURES}
	 */
	public static final String SCRIPT_FAILURES = "Failures";

	/**
	 * File suffix appended to started script: {@value #SCRIPT_STARTED}
	 */
	public static final String SCRIPT_STARTED = "Started";

	/**
	 * File suffix appended to successful scripts: {@value #SCRIPT_SUCCESS}
	 */
	public static final String SCRIPT_SUCCESS = "Success";

	private static List<BioModule> bioModules = null;
	private static Exception pipelineException = null;
	private static final int POLL_TIME = 60;
	private static int pollCount = 0;
	private static String statusMsg = "";
}
