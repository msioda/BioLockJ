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
import biolockj.module.r.R_Module;

/**
 * This utility holds general methods useful for BioModule interaction and management.
 */
public class ModuleUtil
{

	// Prevent instantiation
	private ModuleUtil()
	{}

	/**
	 * Get a classifier module<br>
	 * Use checkAhead parameter to determine if we look forward or backwards starting from the given module.
	 * 
	 * @param module Calling BioModule
	 * @param checkAhead Boolean TRUE to check for NEXT, set FALSE to check BEHIND
	 * @return BioModule
	 * @throws Exception if errors occur
	 */
	public static BioModule getClassifier( final BioModule module, final boolean checkAhead ) throws Exception
	{
		for( final BioModule m: getModules( module, checkAhead ) )
		{
			if( m instanceof ClassifierModule )
			{
				return module;
			}
		}
		return null;
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
		final BioModule classifier = getClassifier( module, checkAhead );
		for( final BioModule m: getModules( module, checkAhead ) )
		{
			if( m.getClass().getName().equals( className ) )
			{
				if( classifier == null || m.getID() < classifier.getID() )
				{
					return m;
				}

			}
		}

		return null;
	}

	/**
	 * BioModules are run in the order configured.<br>
	 * Return the module configured to run before the given module.
	 *
	 * @param module BioModule
	 * @return Previous BioModule
	 * @throws Exception if module not found in the pipeline
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
	 * Return TRUE if bioModule has executed.
	 *
	 * @param bioModule BioModule
	 * @return TRUE if bioModule has executed
	 */
	public static boolean hasExecuted( final BioModule bioModule )
	{
		return isComplete( bioModule ) || isIncomplete( bioModule );
	}

	/**
	 * Return TRUE if bioModule completed successfully.
	 *
	 * @param bioModule BioModule
	 * @return TRUE if bioModule has completed successfully.
	 */
	public static boolean isComplete( final BioModule bioModule )
	{
		final File f = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_COMPLETE );
		return f.exists();
	}

	/**
	 * Test if module is the first {@link biolockj.module.r.R_Module} configured in the pipeline.
	 * 
	 * @param module BioModule to test
	 * @return TRUE if module is 1st {@link biolockj.module.r.R_Module} in this branch
	 * @throws Exception if errors occur
	 */
	public static boolean isFirstRModule( final BioModule module ) throws Exception
	{
		final List<Integer> rIds = getRModulesIds();
		final List<Integer> filteredR_ids = new ArrayList<>( rIds );
		Log.info( ModuleUtil.class, "Checking for 1st R modue, total #R_Module found = " + rIds.size() );
		if( rIds.contains( module.getID() ) )
		{
			final List<Integer> cIds = getClassifierIds();
			Log.info( ModuleUtil.class, "Total #ClassifierModules found = " + cIds.size() );
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
	 * Return TRUE if bioModule started execution but is not complete.
	 *
	 * @param bioModule BioModule
	 * @return TRUE if bioModule started execution but is not complete
	 */
	public static boolean isIncomplete( final BioModule bioModule )
	{
		final File f = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_STARTED );
		return f.exists();
	}

	/**
	 * Method determines if the given module is a metadata-module (which does not use/modify sequence data.
	 * 
	 * @param module BioModule in question
	 * @return TRUE if module produced exactly 1 file (metadata file)
	 * @throws Exception if errors occur
	 */
	public static boolean isMetadataModule( final BioModule module ) throws Exception
	{
		boolean foundMeta = false;
		boolean foundOther = false;
		final List<File> files = Arrays.asList( module.getOutputDir().listFiles() );
		for( final File f: files )
		{
			if( f.getName().equals( MetaUtil.getMetadataFileName() ) )
			{
				foundMeta = true;
			}
			else if( !Config.getSet( Config.INPUT_IGNORE_FILES ).contains( f.getName() ) )
			{
				foundOther = true;
			}
		}

		return foundMeta && !foundOther;
	}

	/**
	 * Method creates a file named {@value biolockj.BioLockJ#BLJ_COMPLETE} in bioModule root directory to document
	 * bioModule has completed successfully. Also clean up by removing file {@value biolockj.Constants#BLJ_STARTED}.
	 *
	 * @param bioModule BioModule
	 * @throws Exception if unable to create {@value biolockj.BioLockJ#BLJ_COMPLETE} file
	 */
	public static void markComplete( final BioModule bioModule ) throws Exception
	{
		final File f = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_COMPLETE );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.exists() )
		{
			throw new Exception( "Unable to create " + f.getAbsolutePath() );
		}
		final File startFile = new File(
				bioModule.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_STARTED );
		BioLockJUtil.deleteWithRetry( startFile, 5 );
		Log.info( ModuleUtil.class, Log.LOG_SPACER );
		Log.info( ModuleUtil.class, "FINISHED " + bioModule.getClass().getName() );
		Log.info( ModuleUtil.class, Log.LOG_SPACER );
	}

	/**
	 * Method creates a file named {@value biolockj.Constants#BLJ_STARTED} in bioModule root directory to document
	 * bioModule has completed successfully. Also sets the start time and caches module name to list of executed modules
	 * so we can check later if it ran during this pipeline execution (as opposed to a previous failed run).
	 *
	 * @param bioModule BioModule
	 * @throws Exception if unable to create {@value biolockj.Constants#BLJ_STARTED} file
	 */
	public static void markStarted( final BioModule bioModule ) throws Exception
	{
		final File f = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_STARTED );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.exists() )
		{
			throw new Exception( "Unable to create " + f.getAbsolutePath() );
		}
		Log.info( ModuleUtil.class, Log.LOG_SPACER );
		Log.info( ModuleUtil.class, "STARTING " + bioModule.getClass().getName() );
		Log.info( ModuleUtil.class, Log.LOG_SPACER );
	}

	/**
	 * Overestimate the max number of modules that will be created in total, after all implicit and pre/post-requisite
	 * modules have been added.
	 * 
	 * @return Estimate Max
	 */
	public static Integer maxNumModules()
	{
		Integer count = 8;
		try
		{
			for( String mod: Config.requireList( Config.INTERNAL_BLJ_MODULE ) )
			{
				mod = mod.toLowerCase();
				if( mod.contains( "qiime" ) && mod.contains( "classifier" ) )
				{
					count = count + 5;
				}
				else if( mod.toLowerCase().contains( "humann2" ) && mod.contains( "classifier" ) )
				{
					count = count + 5;
				}
				count++;
			}
		}
		catch( final Exception ex )
		{
			Log.error( ModuleUtil.class, "Unable to effectively estiamte count" );
			count = 100;
		}

		return count;
	}

	/**
	 * Get BioModule subdirectory File object with given name. If directory doesn't exist, create it.
	 *
	 * @param bioModule BioModule
	 * @param subDirName BioModule sub-directory name
	 * @return BioModule sub-directory File object
	 */
	public static File requireSubDir( final BioModule bioModule, final String subDirName )
	{
		final File dir = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + subDirName );
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
	 * @param bioModule BioModule
	 * @param subDirName BioModule sub-directory name
	 * @return TRUE if BioModule sub-directory exists
	 */
	public static boolean subDirExists( final BioModule bioModule, final String subDirName )
	{
		final File dir = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + subDirName );
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

	/**
	 * Return pipeline modules after the given module if checkAhead = TRUE<br>
	 * Otherwise return pipeline modules before the given module.<br>
	 * If returning the prior modules, return the pipeline modules in reverse order, so the 1st item in the list is the
	 * module immediately preceding the given module.
	 * 
	 */
	private static List<BioModule> getModules( final BioModule module, final Boolean checkAhead ) throws Exception
	{
		if( checkAhead )
		{
			return Pipeline.getModules().subList( module.getID() + 1, Pipeline.getModules().size() );
		}

		final List<BioModule> modules = Pipeline.getModules().subList( 0, module.getID() );
		Collections.reverse( modules );
		return modules;
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
