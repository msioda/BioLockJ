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
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.module.*;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.module.r.R_Module;
import biolockj.module.report.Email;
import biolockj.module.report.otu.CompileOtuCounts;
import biolockj.module.report.taxa.BuildTaxaTables;
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
		initializeModules();
//
//		info( "Pipeline Module Execution Order:" );
//		final List<String> ids = new ArrayList<>();
//		int branchScore = 0;
//		int prevScore = 0;
//		String branchType = Branch.SEQ_TYPE;
//		String prevType = null;
//		final Iterator<BioModule> it = bioModules.iterator();
//		while( it.hasNext() )
//		{
//			final BioModule module = it.next();
//			final String name = module.getClass().getName();
//			info( "BioModule [ " + module.getID() + " ] = " + name );
//
//			// boolean isEmailMod = isEmailModule( module ); // -1 (can be run anytime)
//			final boolean isSeqMod = isSeqModule( module ); // 0
//			final boolean isClassifierMod = isClassifierModule( module ); // 1
//			// boolean isQiimeMod = isQiimeModule( module ); // NA
//
//			final boolean isParserMod = isParserModule( module ); // 2
//			final boolean isOtuMod = isOtuModule( module ); // 2
//			final boolean isTaxaMod = isTaxaModule( module ); // 2 ~fuzzy 3 (requires at least one 2 or 3 has run)
//
//			final boolean isStatsMod = isStatsModule( module ); // 3 ~fuzzy 3 (requires at least one 4 has run)
//			final boolean isReportMod = isReportModule( module ); // 4 ~fuzzy 3 (Json requires 2 or 3 has run, Email can
//																	// run ANYTIME)
//
//			final boolean isRMod = isRModule( module ); // 4 ~fuzzy 3
//
//			if( isSeqMod && !branches.isEmpty() )
//			{
//				throw new Exception( "BioLockJ only supports branched pipelines for a single dataset."
//						+ "All sequence preparation modules must run prior to running the 1st classifier" );
//			}
//			else if( isClassifierMod )
//			{
//				branchType = Branch.CLASSIFIER_TYPE;
//				branchScore = 1;
//			}
//			else if( isParserMod || isOtuMod || isTaxaMod || isReportMod )
//			{
//				branchType = isReportMod ? Branch.REPORT_TYPE
//						: isTaxaMod ? Branch.TAXA_COUNT_TYPE: Branch.OTU_COUNT_TYPE;
//				branchScore = 2;
//			}
//			else if( isStatsMod )
//			{
//				branchType = Branch.STATS_TYPE;
//				branchScore = 3;
//			}
//			else if( isRMod )
//			{
//				branchType = Branch.R_TYPE;
//				branchScore = 4;
//			}
//			// else --> No changes to score or type for an implicit module like MergeOtuTables
//
//			if( !ids.isEmpty() )
//			{
//				// 1st branch is frequently sequence preparation
//				// If branchScore is lower, this is the 2nd classifier to run
//				if( isClassifierMod && prevType.equals( Branch.SEQ_TYPE ) || branchScore < prevScore )
//				{
//					branches.add( new Branch( prevType, new ArrayList<>( ids ) ) );
//					ids.clear();
//				}
//				else if( isStatsMod && prevType.equals( Branch.CLASSIFIER_TYPE ) ) // Create next branch = From
//																					// classifier to just before stats
//				{
//					branches.add( new Branch( prevType, new ArrayList<>( ids ) ) );
//					ids.clear();
//				}
//				else if( isStatsMod && prevType.equals( Branch.R_TYPE ) ) // Create next branch = From classifier to
//																			// just before stats
//				{
//					branches.add( new Branch( prevType, new ArrayList<>( ids ) ) );
//					ids.clear();
//				}
//
//				if( !it.hasNext() ) // No more modules, dump remaining modules into branchMap
//				{
//					ids.add( module.getID() );
//					branches.add( new Branch( branchType, new ArrayList<>( ids ) ) );
//				}
//			}
//
//			prevType = branchType;
//			prevScore = branchScore;
//			ids.add( module.getID() );
//		}
//
//		for( final Branch b: branches )
//		{
//			Log.info( Pipeline.class, "Branch #" + 2 + " [" + b.getBranchType() + "] = " + b.getIds() );
//		}

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
		final BioModule module = ModuleUtil.getFirstModule( moduleName );

		( (JavaModule) module ).runModule();
		refreshOutputMetadata( module );
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
			Log.error( Pipeline.class, "Pipeline failed! " + ex.getMessage(), ex );
			pipelineException = ex;
			SummaryUtil.reportFailure( ex );
			try
			{
				final BioModule emailMod = ModuleUtil.getFirstModule( Email.class.getName() );
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
			if( ModuleUtil.isIncomplete( module ) && ( !RuntimeParamUtil.isDirectMode() || module instanceof Email ) )
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
		final boolean is_R = !RuntimeParamUtil.isDirectMode() && module instanceof R_Module;
		final File mainScript = ModuleUtil.getMainScript( module );
		final IOFileFilter ff = new WildcardFileFilter( "*" + ( is_R ? R_Module.R_EXT: BioLockJ.SH_EXT ) );
		final Collection<File> scriptFiles = FileUtils.listFiles( module.getScriptDir(), ff, null );
		scriptFiles.remove( mainScript );

		// Log.debug( Pipeline.class, "mainScript = " + mainScript.getAbsolutePath() );
		// for( final File f: scriptFiles )
		// {
		// Log.debug( Pipeline.class, "Worker Script = " + f.getAbsolutePath() );
		// }

		if( is_R )
		{
			scriptFiles.clear();
			scriptFiles.add( mainScript );
		}

		final int numScripts = scriptFiles.size();
		int numSuccess = 0;
		int numStarted = 0;
		int numFailed = 0;

		for( final File f: scriptFiles )
		{
			final File testStarted = new File( f.getAbsolutePath() + "_" + SCRIPT_STARTED );
			final File testSuccess = new File( f.getAbsolutePath() + "_" + SCRIPT_SUCCESS );
			final File testFailure = new File( f.getAbsolutePath() + "_" + SCRIPT_FAILURES );
			numStarted = numStarted + ( testStarted.exists() ? 1: 0 );
			numSuccess = numSuccess + ( testSuccess.exists() ? 1: 0 );
			numFailed = numFailed + ( testFailure.exists() ? 1: 0 );
		}

		final String logMsg = mainScript.getName() + " Status (Total=" + numScripts + "): Success=" + numSuccess
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

		final File mainFailed = new File( mainScript.getAbsolutePath() + "_" + SCRIPT_FAILURES );
		if( mainFailed.exists() || numFailed > 0 )
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

	private static void info( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( Pipeline.class, msg );
		}
	}

	private static boolean isClassifierModule( final BioModule module ) throws Exception
	{
		return module instanceof ClassifierModule;
	}

	private static boolean isEmailModule( final BioModule module ) throws Exception
	{
		return module instanceof Email;
	}

	private static boolean isOtuModule( final BioModule module ) throws Exception
	{
		return module.getClass().getPackage().getName().startsWith( CompileOtuCounts.class.getPackage().getName() );
	}

	private static boolean isParserModule( final BioModule module ) throws Exception
	{
		return module instanceof ParserModule;
	}

	private static boolean isQiimeModule( final BioModule module ) throws Exception
	{
		return module.getClass().getName().toLowerCase().contains( "qiime" );
	}

	private static boolean isReportModule( final BioModule module ) throws Exception
	{
		return module.getClass().getPackage().getName().startsWith( Email.class.getPackage().getName() );
	}

	private static boolean isRModule( final BioModule module ) throws Exception
	{
		return module instanceof R_Module;
	}

	private static boolean isSeqModule( final BioModule module ) throws Exception
	{
		return module instanceof SeqModule;
	}

	private static boolean isStatsModule( final BioModule module ) throws Exception
	{
		return module.getClass().getName().equals( BioModuleFactory.getDefaultStatsModule() );
	}

	private static boolean isTaxaModule( final BioModule module ) throws Exception
	{
		return module.getClass().getPackage().getName().startsWith( BuildTaxaTables.class.getPackage().getName() );
	}

	private static void logScriptTimeOutMsg( final ScriptModule module ) throws Exception
	{
		final String prompt = "------> ";
		Log.info( Pipeline.class, prompt + "Java program wakes every 60 seconds to check execution progress" );
		Log.info( Pipeline.class, prompt + "Status determined by existance of indicator files in "
				+ module.getScriptDir().getAbsolutePath() );
		Log.info( Pipeline.class, prompt + "Indicator files end with: \"_" + SCRIPT_STARTED + "\", \"_" + SCRIPT_SUCCESS
				+ "\", or \"_" + SCRIPT_FAILURES + "\"" );
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
	 * This method calls poll to check status of scripts and then sleeps for pollTime (60) seconds.
	 *
	 * @param mainScript
	 * @throws Exception
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
	private static List<Branch> branches = new ArrayList<>();
	private static Exception pipelineException = null;
	private static final int POLL_TIME = 60;
	private static int pollCount = 0;
	private static String statusMsg = "";
}
