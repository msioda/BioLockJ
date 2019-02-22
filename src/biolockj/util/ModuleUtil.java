/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Apr 01, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.implicit.Demultiplexer;
import biolockj.module.report.r.R_CalculateStats;
import biolockj.module.report.r.R_Module;
import biolockj.module.seq.AwkFastaConverter;
import biolockj.module.seq.PearMergeReads;

/**
 * This utility holds general methods useful for BioModule interaction and management.
 */
public class ModuleUtil
{

	// Prevent instantiation
	private ModuleUtil()
	{}

	/**
	 * Return the module ID as a 2 digit display number (add leading zero if needed).
	 * 
	 * @param module BioModule
	 * @return ID display value
	 * @throws Exception if errors occur
	 */
	public static String displayID( final BioModule module ) throws Exception
	{
		return BioLockJUtil.formatDigits( module.getID(), 2 );
	}

	/**
	 * Get a classifier module<br>
	 * Use checkAhead parameter to determine if we look forward or backwards starting from the given module.
	 * 
	 * @param module Calling BioModule
	 * @param checkAhead Boolean TRUE to check for NEXT, set FALSE to check BEHIND
	 * @return BioModule
	 * @throws Exception if errors occur
	 */
	public static ClassifierModule getClassifier( final BioModule module, final boolean checkAhead ) throws Exception
	{
		for( final BioModule m: getModules( module, checkAhead ) )
		{
			if( m instanceof ClassifierModule )
			{
				return (ClassifierModule) m;
			}
		}
		return null;
	}

	/**
	 * Get the Java Class name for the default Demultiplexer module
	 * 
	 * @return Demultiplexer module Java class name
	 */
	public static String getDefaultDemultiplexer()
	{
		return getDefaultModule( Constants.DEFAULT_MOD_DEMUX, Demultiplexer.class.getName() );
	}

	/**
	 * Get the Java Class name for the default Fasta converter module
	 * 
	 * @return Fasta converter module Java class name
	 */
	public static String getDefaultFastaConverter()
	{
		return getDefaultModule( Constants.DEFAULT_MOD_FASTA_CONV, AwkFastaConverter.class.getName() );
	}

	/**
	 * Get the Java Class name for the default Merge paired read module
	 * 
	 * @return Merge paired read module Java class name
	 */
	public static String getDefaultMergePairedReadsConverter()
	{
		return getDefaultModule( Constants.DEFAULT_MOD_SEQ_MERGER, PearMergeReads.class.getName() );
	}

	/**
	 * Return the class name of the default R statistics BioModule
	 * 
	 * @return default stats module
	 */
	public static String getDefaultStatsModule()
	{
		return getDefaultModule( Constants.DEFAULT_STATS_MODULE, R_CalculateStats.class.getName() );
	}

	/**
	 * Get a module with given className unless a classifier module is found 1st.<br>
	 * Use checkAhead parameter to determine if we look forward or backwards starting from the given module.
	 * 
	 * @param module Calling BioModule
	 * @param className Target BioModule class name
	 * @param checkAhead Boolean TRUE to check for NEXT, set FALSE to check BEHIND
	 * @return BioModule
	 * @throws Exception if errors occur
	 */
	public static BioModule getModule( final BioModule module, final String className, final boolean checkAhead )
			throws Exception
	{
		if( module instanceof ClassifierModule )
		{
			throw new Exception(
					"ModuleUtil.getModule( module, className, checkAhead) - Param \"module\" cannot be a ClassifierModule: "
							+ module.getClass().getName() );
		}

		final ClassifierModule classifier = getClassifier( module, checkAhead );
		for( final BioModule m: getModules( module, checkAhead ) )
		{
			if( m.getClass().getName().equals( className ) )
			{
				final boolean targetBeforeClassifier = m.getID() < classifier.getID();
				final boolean targetAfterClassifier = m.getID() > classifier.getID();
				if( classifier == null || checkAhead && targetBeforeClassifier || !checkAhead && targetAfterClassifier )
				{
					return m;
				}

			}
		}

		return null;
	}

	/**
	 * Construct a BioModule based on its className.
	 * 
	 * @param className BioModule class name
	 * @return BioModule module
	 * @throws Exception if errors occur
	 */
	public static BioModule getModule( final String className ) throws Exception
	{
		return (BioModule) Class.forName( className ).newInstance();
	}

	/**
	 * Return pipeline modules after the given module if checkAhead = TRUE<br>
	 * Otherwise return pipeline modules before the given module.<br>
	 * If returning the prior modules, return the pipeline modules in reverse order, so the 1st item in the list is the
	 * module immediately preceding the given module.
	 * 
	 * @param module Reference BioModule
	 * @param checkAhead Set TRUE to return modules after the given reference module
	 * @return List of BioModules before/after the current module, as determined by checkAhead parameter
	 * @throws Exception if errors occur
	 */
	public static List<BioModule> getModules( final BioModule module, final Boolean checkAhead ) throws Exception
	{
		List<BioModule> modules = null;
		if( checkAhead )
		{
			modules = new ArrayList<>( new TreeSet<>(
					Pipeline.getModules().subList( module.getID() + 1, Pipeline.getModules().size() ) ) );
		}
		else
		{
			modules = new ArrayList<>( new TreeSet<>( Pipeline.getModules().subList( 0, module.getID() ) ) );
			Collections.reverse( modules );
		}

		return modules;
	}

	/**
	 * BioModules are run in the order configured.<br>
	 * Return the module configured to run after the given module.
	 *
	 * @param module BioModule
	 * @return Next BioModule
	 * @throws Exception if input module not found in the pipeline
	 */
	public static BioModule getNextModule( final BioModule module ) throws Exception
	{
		if( module.getID() + 1 == Pipeline.getModules().size() )
		{
			return null;
		}

		return Pipeline.getModules().get( module.getID() + 1 );
	}

	/**
	 * BioModules are run in the order configured.<br>
	 * Return the module configured to run before the given module.
	 *
	 * @param module BioModule
	 * @return Previous BioModule
	 * @throws Exception if input module not found in the pipeline
	 */
	public static BioModule getPreviousModule( final BioModule module ) throws Exception
	{
		if( module.getID() == 0 )
		{
			return null;
		}

		return Pipeline.getModules().get( module.getID() - 1 );
	}

	/**
	 * Return TRUE if module has executed.
	 *
	 * @param module BioModule
	 * @return TRUE if module has executed
	 */
	public static boolean hasExecuted( final BioModule module )
	{
		return isComplete( module ) || isIncomplete( module );
	}

	/**
	 * Return TRUE if module completed successfully.
	 *
	 * @param module BioModule
	 * @return TRUE if module has completed successfully.
	 */
	public static boolean isComplete( final BioModule module )
	{
		final File f = new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_COMPLETE );
		return f.exists();
	}

	/**
	 * Test if module is the first {@link biolockj.module.report.r.R_Module} configured in the pipeline.
	 * 
	 * @param module BioModule to test
	 * @return TRUE if module is 1st {@link biolockj.module.report.r.R_Module} in this branch
	 * @throws Exception if errors occur
	 */
	public static boolean isFirstRModule( final BioModule module ) throws Exception
	{
		final List<Integer> rIds = getRModulesIds();
		final List<Integer> filteredR_ids = new ArrayList<>( rIds );
		if( rIds.contains( module.getID() ) )
		{
			final List<Integer> cIds = getClassifierIds();
			if( !cIds.isEmpty() )
			{
				final BioModule prevClassMod = getClassifier( module, false );
				final BioModule nextClassMod = getClassifier( module, true );
				for( final Integer rId: rIds )
				{
					if( prevClassMod != null && rId < prevClassMod.getID() )
					{
						filteredR_ids.remove( rId );
					}
					if( nextClassMod != null && rId > nextClassMod.getID() )
					{
						filteredR_ids.remove( rId );
					}
				}

				Log.info( ModuleUtil.class, "Removed out-of-scope IDs, leaving valid R IDs = " + filteredR_ids.size() );
			}

			if( !filteredR_ids.isEmpty() )
			{
				return filteredR_ids.get( 0 ).equals( module.getID() );

			}
		}

		return false;
	}

	/**
	 * Return TRUE if module started execution but is not complete.
	 *
	 * @param module BioModule
	 * @return TRUE if module started execution but is not complete
	 */
	public static boolean isIncomplete( final BioModule module )
	{
		final File f = new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_STARTED );
		return f.exists();
	}

	/**
	 * Method determines if the given module is a metadata-module (which does not use/modify sequence data.
	 * 
	 * @param module BioModule in question
	 * @return TRUE if module produced exactly 1 file (metadata file)
	 */
	public static boolean isMetadataModule( final BioModule module )
	{
		boolean foundMeta = false;
		boolean foundOther = false;
		try
		{
			final List<File> files = Arrays.asList( module.getOutputDir().listFiles() );
			for( final File f: files )
			{
				if( f.getName().equals( MetaUtil.getMetadataFileName() ) )
				{
					foundMeta = true;
				}
				else if( !Config.getSet( module, Constants.INPUT_IGNORE_FILES ).contains( f.getName() ) )
				{
					foundOther = true;
				}
			}
		}
		catch( final Exception ex )
		{
			return false;
		}
		return foundMeta && !foundOther;
	}

	/**
	 * Method creates a file named {@value biolockj.Constants#BLJ_COMPLETE} in module root directory to document module
	 * has completed successfully. Also clean up by removing file {@value biolockj.Constants#BLJ_STARTED}.
	 *
	 * @param module BioModule
	 * @throws Exception if unable to create {@value biolockj.Constants#BLJ_COMPLETE} file
	 */
	public static void markComplete( final BioModule module ) throws Exception
	{
		final File f = new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_COMPLETE );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.exists() )
		{
			throw new Exception( "Unable to create " + f.getAbsolutePath() );
		}
		final File startFile = new File(
				module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_STARTED );
		BioLockJUtil.deleteWithRetry( startFile, 5 );
		Log.info( ModuleUtil.class, Constants.LOG_SPACER );
		Log.info( ModuleUtil.class,
				"FINISHED [ " + ModuleUtil.displayID( module ) + " ] " + module.getClass().getName() );
		Log.info( ModuleUtil.class, Constants.LOG_SPACER );
	}

	/**
	 * Method creates a file named {@value biolockj.Constants#BLJ_STARTED} in module root directory to document module
	 * has completed successfully. Also sets the start time and caches module name to list of executed modules so we can
	 * check later if it ran during this pipeline execution (as opposed to a previous failed run).
	 *
	 * @param module module
	 * @throws Exception if unable to create {@value biolockj.Constants#BLJ_STARTED} file
	 */
	public static void markStarted( final BioModule module ) throws Exception
	{
		final File f = new File( module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_STARTED );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.exists() )
		{
			throw new Exception( "Unable to create " + f.getAbsolutePath() );
		}
		Log.info( ModuleUtil.class, Constants.LOG_SPACER );
		Log.info( ModuleUtil.class,
				"STARTING [ " + ModuleUtil.displayID( module ) + " ] " + module.getClass().getName() );
		Log.info( ModuleUtil.class, Constants.LOG_SPACER );
	}

	/**
	 * Check if a module was in the pipeline at least once.
	 * 
	 * @param className module simple name
	 * @return boolean
	 * @throws Exception if runtime errors occur
	 */
	public static boolean moduleExists( final String className ) throws Exception
	{
		for( final BioModule m: Pipeline.getModules() )
		{
			if( m.getClass().getName().equals( className ) )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Get BioModule subdirectory File object with given name. If directory doesn't exist, create it.
	 *
	 * @param module BioModule
	 * @param subDirName BioModule sub-directory name
	 * @return BioModule sub-directory File object
	 */
	public static File requireSubDir( final BioModule module, final String subDirName )
	{
		final File dir = new File( module.getModuleDir().getAbsolutePath() + File.separator + subDirName );
		if( !dir.exists() )
		{
			dir.mkdirs();
			Log.info( ModuleUtil.class, "Create directory: " + dir.getAbsolutePath() );
		}
		return dir;
	}

	/**
	 * Return TRUE if BioModule sub-directory exists
	 *
	 * @param module BioModule
	 * @param subDirName BioModule sub-directory name
	 * @return TRUE if BioModule sub-directory exists
	 */
	public static boolean subDirExists( final BioModule module, final String subDirName )
	{
		final File dir = new File( module.getModuleDir().getAbsolutePath() + File.separator + subDirName );
		return dir.exists();
	}

	private static List<Integer> getClassifierIds() throws Exception
	{
		final List<Integer> ids = new ArrayList<>();
		for( final BioModule m: Pipeline.getModules() )
		{
			if( m instanceof ClassifierModule )
			{
				ids.add( m.getID() );
			}
		}
		return ids;
	}

	private static String getDefaultModule( final String name, final String className )
	{
		String defaultModule = Config.getString( null, name );
		if( defaultModule == null )
		{
			defaultModule = className;
		}

		return defaultModule;
	}

	private static List<Integer> getRModulesIds() throws Exception
	{
		final List<Integer> ids = new ArrayList<>();
		for( final BioModule m: Pipeline.getModules() )
		{
			if( m instanceof R_Module )
			{
				ids.add( m.getID() );
			}
		}
		return ids;
	}

}
