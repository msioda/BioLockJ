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
import biolockj.module.report.r.R_Module;
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
	
	
	private static boolean hasScripts( final BioModule module ) throws Exception
	{
		final File scriptDir = new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.SCRIPT_DIR );
		return scriptDir.exists() && module instanceof ScriptModule;
	}

	/**
	 * Execute a single pipeline module.
	 * 
	 * @param module BioModule current module
	 * @throws Exception if runtime errors occur
	 */
	public static void executeModule( final BioModule module ) throws Exception
	{
		ModuleUtil.markStarted( module );
		refreshOutputMetadata( ModuleUtil.getPreviousModule( module ) );
		refreshRCacheIfNeeded( module );
		module.executeTask();

		final boolean isJava = module instanceof JavaModule;
		final boolean runScripts = hasScripts( module );

		if( runScripts && !RuntimeParamUtil.runAws() )
		{
			Job.submit( (ScriptModule) module );
		}
		
		if( runScripts )
		{
			pollAndSpin( (ScriptModule) module );
		}

		refreshOutputMetadata( module );
		module.cleanUp();

		if( !isJava && !runScripts )
		{
			SummaryUtil.reportSuccess( module );
		}

		ModuleUtil.markComplete( module );
	}

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
	 * Return {@value Constants#SCRIPT_SUCCESS} if no pipelineException has been thrown, otherwise return
	 * {@value Constants#SCRIPT_FAILURES}
	 *
	 * @return pipeline status (success or failed)
	 * @throws Exception if unable to get status for all modules
	 */
	public static String getStatus() throws Exception
	{
		return pipelineException == null ? Constants.SCRIPT_SUCCESS: Constants.SCRIPT_FAILURES;
	}

	/**
	 * This method initializes the Pipeline by building the modules and checking module dependencies.
	 * 
	 * @throws Exception if errors occur
	 */
	public static void initializePipeline() throws Exception
	{
		bioModules = BioModuleFactory.buildPipeline();
		Config.setConfigProperty( Constants.INTERNAL_ALL_MODULES, BioLockJUtil.getClassNames( bioModules ) );
		initializeModules();
	}

	/**
	 * If moduleName is null, run all modules, otherwise only run the specified module.
	 * 
	 * @param moduleID of a single module to run
	 * @throws Exception if any fatal error occurs during execution
	 */
	public static void runDirectModule( final Integer moduleID ) throws Exception
	{
		Log.info( Pipeline.class,
				"Run Direct BioModule ID[ " + moduleID + " ] = " + Pipeline.getModules().get( moduleID ) );
		final JavaModule module = (JavaModule) Pipeline.getModules().get( moduleID );
		module.runModule();
		refreshOutputMetadata( module ); // keep in case cleanup does something with metadata
		module.cleanUp();
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
			try
			{
				Log.error( Pipeline.class, "Pipeline failed! " + ex.getMessage(), ex );
				pipelineException = ex;
				SummaryUtil.reportFailure( ex );
			}
			catch( final Exception ex2 )
			{
				Log.error( Pipeline.class, "Attempt to update summary has failed: " + ex2.getMessage(), ex2 );
			}

			try
			{
				BioModule emailMod = null;
				boolean foundIncomplete = false;
				for( final BioModule module: Pipeline.getModules() )
				{
					if( module instanceof Email )
					{
						emailMod = module;
					}
					if( !foundIncomplete && !ModuleUtil.isComplete( module ) )
					{
						foundIncomplete = true;
					}
					if( foundIncomplete && emailMod != null )
					{
						Log.warn( Pipeline.class, "Attempting to send failure notification with Email module: "
								+ emailMod.getModuleDir().getName() );
						emailMod.executeTask();
						Log.warn( Pipeline.class, "Attempt appears to be a success!" );
						break;
					}
				}

			}
			catch( final Exception innerEx )
			{
				Log.error( Pipeline.class,
						"Attempt to Email pipeline failure info --> also failed!  " + innerEx.getMessage(), innerEx );
			}

			throw ex;
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
		final File moduleDir = module.getModuleDir();
		if( BioLockJUtil.deleteWithRetry( module.getModuleDir(), 10 ) )
		{
			moduleDir.mkdirs(); // recreate deleted directory
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
	 * modules have an empty file {@value Constants#BLJ_STARTED} in the module directory.<br>
	 * Complete modules have an empty file {@value Constants#BLJ_COMPLETE} in the module directory.<br>
	 * {@link biolockj.module.BioModule}s are run in the order listed in the {@link biolockj.Config} file.<br>
	 * <p>
	 * Execution steps:
	 * <ol>
	 * <li>File {@value Constants#BLJ_STARTED} is added to the module directory
	 * <li>Execute {@link #refreshOutputMetadata(BioModule)} to cache updated metadata, if generatede.
	 * <li>Run module scripts, if any, polling 1/minute for status until all scripts complete or time out.
	 * <li>File {@value Constants#BLJ_STARTED} is replaced by {@value Constants#BLJ_COMPLETE} as status indicator
	 * </ol>
	 *
	 * @throws Exception if script errors occur
	 */
	protected static void executeModules() throws Exception
	{
		for( final BioModule module: Pipeline.getModules() )
		{
			if( !ModuleUtil.isComplete( module ) )
			{
				executeModule( module );
			}
			else
			{
				Log.debug( Pipeline.class,
						"Skipping succssfully completed BioLockJ Module: " + module.getClass().getName() );
			}
		}
	}

	/**
	 * If the bioModule is complete and contains a metadata file in its output directory, return the metadata file,
	 * since it must be a new version.
	 *
	 * @param bioModule BioModule
	 * @return New metadata file (or null)
	 * @throws Exception if errors occur
	 */
	protected static File getMetadata( final BioModule bioModule ) throws Exception
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
	 * <li>Create module sub-directories under {@value biolockj.Constants#PIPELINE_DIR} as ordered in
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
			if( ModuleUtil.isIncomplete( module ) && ( !RuntimeParamUtil.isDirectMode() || module instanceof Email ) )
			{
				deleteIncompleteModule( module );
			}

			if( ModuleUtil.isComplete( module ) )
			{
				module.cleanUp();
				refreshOutputMetadata( module );
				refreshRCacheIfNeeded( module );
			}

			info( "Check dependencies for: " + module.getClass().getName() );
			module.checkDependencies();
		}

		return true;
	}

	/**
	 * The {@link biolockj.module.ScriptModule#getScriptDir()} will contain one main script and one ore more worker
	 * scripts.<br>
	 * An empty file with {@value Constants#SCRIPT_STARTED} appended to the script name is created when execution
	 * begins.<br>
	 * If successful, an empty file with {@value Constants#SCRIPT_SUCCESS} appended to the script name is created.<br>
	 * Upon failure, an empty file with {@value Constants#SCRIPT_FAILURES} appended to the script name is created.<br>
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
		final boolean is_R = !RuntimeParamUtil.isDirectMode() && module instanceof R_Module;
		final File mainScript = module.getMainScript();
		final IOFileFilter ff = new WildcardFileFilter( "*" + ( is_R ? Constants.R_EXT: Constants.SH_EXT ) );
		final Collection<File> scriptFiles = FileUtils.listFiles( module.getScriptDir(), ff, null );
		
		if( mainScript != null )
		{
			scriptFiles.remove( mainScript );
		}
		

		// Log.debug( Pipeline.class, "mainScript = " + mainScript.getAbsolutePath() );
		// for( final File f: scriptFiles )
		// {
		// Log.debug( Pipeline.class, "Worker Script = " + f.getAbsolutePath() );
		// }

		if( is_R && mainScript != null )
		{
			scriptFiles.clear();
			scriptFiles.add( mainScript );
		}

		final int numScripts = scriptFiles.size();
		int numSuccess = 0;
		int numStarted = 0;
		int numFailed = 0;
		
		File firstFailure = null;

		for( final File f: scriptFiles )
		{
			final File testStarted = new File( f.getAbsolutePath() + "_" + Constants.SCRIPT_STARTED );
			final File testSuccess = new File( f.getAbsolutePath() + "_" + Constants.SCRIPT_SUCCESS );
			final File testFailure = new File( f.getAbsolutePath() + "_" + Constants.SCRIPT_FAILURES );
			numStarted = numStarted + ( testStarted.exists() ? 1: 0 );
			numSuccess = numSuccess + ( testSuccess.exists() ? 1: 0 );
			numFailed = numFailed + ( testFailure.exists() ? 1: 0 );
			
			if( firstFailure == null && testFailure.exists() ) 
			{
				firstFailure = testFailure;
			}
		}

		final String logMsg = module.getClass().getSimpleName() + " Status (Total=" + numScripts + "): Success=" + numSuccess
				+ "; Failed=" + numFailed + "; Running=" + ( numStarted - numSuccess - numFailed ) + "; Queued="
				+ ( numScripts - numStarted );

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
		
		if( numFailed > 0 )
		{
			final String failMsg = "SCRIPT FAILED: " + BioLockJUtil.getCollectionAsString( module.getScriptErrors() );
			throw new Exception( failMsg );
		}
		
		return numScripts > 0 && ( numSuccess + numFailed == numScripts );
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
			RMetaUtil.classifyReportableMetadata( module );
		}
	}

	private static void info( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( Pipeline.class, msg );
		}
	}

	private static void logScriptTimeOutMsg( final ScriptModule module ) throws Exception
	{
		final String prompt = "------> ";
		Log.info( Pipeline.class, prompt + "Java program wakes every 60 seconds to check execution progress" );
		Log.info( Pipeline.class, prompt + "Status determined by existance of indicator files in "
				+ module.getScriptDir().getAbsolutePath() );
		Log.info( Pipeline.class, prompt + "Indicator files end with: \"_" + Constants.SCRIPT_STARTED + "\", \"_"
				+ Constants.SCRIPT_SUCCESS + "\", or \"_" + Constants.SCRIPT_FAILURES + "\"" );
		Log.info( Pipeline.class,
				prompt + "If any change to #Success/#Failed/#Running/#Queued changed, new values logged" );
		if( module.getTimeout() == null || module.getTimeout() > 10 )
		{
			Log.info( Pipeline.class, prompt
					+ "Status message repeats every 10 minutes while scripts are executing (if status remains unchanged)." );
		}

		if( module.getTimeout() != null )
		{
			Log.info( Pipeline.class, prompt + "Running scripts will time out after the configured SCRIPT TIMEOUT = "
					+ module.getTimeout() );
		}
		else
		{
			Log.info( Pipeline.class, prompt + "Running scripts will NEVER TIME OUT." );
		}
	}

	/**
	 * This method calls poll to check status of scripts and then sleeps for {@value #POLL_TIME} seconds.
	 *
	 * @param module ScriptModule
	 * @throws Exception if errors occur
	 */
	private static void pollAndSpin( final ScriptModule module ) throws Exception
	{
		logScriptTimeOutMsg( module );
		int numMinutes = 0;
		boolean finished = false;
		while( !finished )
		{
			finished = poll( module );
			if( !finished )
			{
				if( module.getTimeout() != null && module.getTimeout() > 0 && numMinutes++ >= module.getTimeout() )
				{
					throw new Exception( module.getClass().getName() + " timed out after " + numMinutes + " minutes." );
				}

				Thread.sleep( POLL_TIME * 1000 );
			}
		}
	}

	private static List<BioModule> bioModules = null;
	private static Exception pipelineException = null;
	private static final int POLL_TIME = 60;
	private static int pollCount = 0;
	private static String statusMsg = "";
}
