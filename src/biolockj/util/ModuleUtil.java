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

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.ScriptModule;
import biolockj.module.implicit.parser.ParserModule;
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
	 * Return first R module configured in pipeline.
	 * 
	 * @return First {@link biolockj.module.r.R_Module} or NULL
	 * @throws Exception if errors occur
	 */
	public static R_Module getFirstRModule() throws Exception
	{
		for( final BioModule module: Pipeline.getModules() )
		{
			if( module instanceof R_Module )
			{
				return (R_Module) module;
			}
		}

		return null;
	}

	/**
	 * Get the main script file in the bioModule script directory, with prefix:
	 * {@value biolockj.module.BioModule#MAIN_SCRIPT_PREFIX}
	 *
	 * @param module BioModule that may be a ScriptModule
	 * @return Main script file
	 */
	public static File getMainScript( final BioModule module )
	{
		final File scriptDir = ModuleUtil.getSubDir( module, ScriptModule.SCRIPT_DIR );
		if( scriptDir != null )
		{
			for( final File f: scriptDir.listFiles() )
			{
				if( f.getName().startsWith( BioModule.MAIN_SCRIPT_PREFIX )
						&& !f.getName().endsWith( Pipeline.SCRIPT_FAILURES )
						&& !f.getName().endsWith( Pipeline.SCRIPT_SUCCESS )
						&& !f.getName().endsWith( Pipeline.SCRIPT_STARTED ) )
				{
					if( module instanceof R_Module )
					{
						if( !RuntimeParamUtil.isDockerMode() && f.getName().endsWith( R_Module.R_EXT ) )
						{
							return f;
						}
						else if( RuntimeParamUtil.isDockerMode() && f.getName().endsWith( BioLockJ.SH_EXT ) )
						{
							return f;
						}
					}
					else
					{
						return f;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Return module with given name. If not found, return null.
	 *
	 * @param name Name of BioModule
	 * @return BioModule with given name, else null
	 * @throws Exception if unable to determine list of BioModules
	 */
	public static BioModule getModule( final String name ) throws Exception
	{
		for( final BioModule m: Pipeline.getModules() )
		{
			if( name != null && m.getClass().getName().equals( name ) )
			{
				return m;
			}
		}

		return null;
	}

	/**
	 * Get the module number, formated as found in the module directory names.
	 * 
	 * @param module BioModule
	 * @return Formatted module number (with leading zeros if needed)
	 * @throws Exception if errors occur
	 */
	public static String getModuleNum( final BioModule module ) throws Exception
	{
		final Collection<File> dirs = FileUtils.listFilesAndDirs( Config.getExistingDir( Config.INTERNAL_PIPELINE_DIR ),
				FalseFileFilter.INSTANCE, HiddenFileFilter.VISIBLE );
		for( final File dir: dirs )
		{
			if( dir.getName().contains( module.getClass().getSimpleName() ) )
			{
				final String modNum = dir.getName().substring( 0, dir.getName().indexOf( "_" ) );
				if( NumberUtils.isNumber( modNum ) )
				{
					return modNum;
				}
			}
		}

		return null;
	}

	/**
	 * Get the Module root directory full path.
	 * 
	 * @param bioModule BioModule
	 * @return Name of module root directory
	 * @throws Exception if errors occur
	 */
	public static String getModuleRootDir( final BioModule bioModule ) throws Exception
	{
		final String i = BioLockJUtil.formatDigits( Pipeline.getModules().indexOf( bioModule ),
				String.valueOf( Pipeline.getModules().size() ).length() );
		return Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath() + File.separator + i + "_"
				+ bioModule.getClass().getSimpleName();
	}

	/**
	 * Return duration bioModule ran based on modified data of started file, formatted for display (as hours, minutes,
	 * seconds).
	 *
	 * @param bioModule BioModule
	 * @return Formatted bioModule runtime
	 */
	public static String getModuleRunTime( final BioModule bioModule )
	{
		final File started = new File(
				bioModule.getModuleDir().getAbsolutePath() + File.separator + Pipeline.BLJ_STARTED );
		return getRunTime( System.currentTimeMillis() - started.lastModified() );
	}

	/**
	 * Returns the PerserModule, if configured. If more than 1 exists (due to new modules re-using Parser suffix), the
	 * first configured is returned.
	 *
	 * @return PerserModule if configured, otherwise null
	 * @throws Exception if unable to get the list of configured BioModules
	 */
	public static ParserModule getParserModule() throws Exception
	{
		for( final BioModule m: Pipeline.getModules() )
		{
			if( m instanceof ParserModule )
			{
				return (ParserModule) m;
			}
		}

		return null;
	}
	
	/**
	 * Method checks if preReq is configured to run prior to the bioModule.
	 * 
	 * @param bioModule BioModule
	 * @param preReq Prerequisite BioModule
	 * @return TRUE if preReq is found 
	 * @throws Exception if errors occur
	 */
	public static boolean preReqModuleExists( final BioModule bioModule, final String preReq ) throws Exception
	{
		boolean foundCurrentModule = false;
		for( final String module: Config.requireList( Config.INTERNAL_BLJ_MODULE ) )
		{
			if( !foundCurrentModule && module.equals( bioModule.getClass().getName() ) )
			{
				foundCurrentModule = true;
			}
			else if( module.equals( preReq ) ) 
			{
				return true;
			}
		}
		
		return false;
	}

	/**
	 * BioModules are run in the order configured. Return the module configured just before the bioModule param.
	 *
	 * @param bioModule BioModule
	 * @return Previous BioModule
	 * @throws Exception if unable to get the list of configured BioModules
	 */
	public static BioModule getPreviousModule( final BioModule bioModule ) throws Exception
	{
		BioModule previousModule = null;
		for( final BioModule module: Pipeline.getModules() )
		{
			if( previousModule != null && module.equals( bioModule ) )
			{
				return previousModule;
			}
			previousModule = module;
		}

		return null;
	}

	/**
	 * Get runtime message (formatted as hours, minutes, seconds) based on startTime passed.
	 *
	 * @param duration Milliseconds of run time
	 * @return Formatted runtime as XX hours : XX minutes: XX seconds
	 */
	public static String getRunTime( final long duration )
	{
		final String format = String.format( "%%0%dd", 2 );
		long elapsedTime = duration / 1000;
		if( elapsedTime < 0 )
		{
			elapsedTime = 0;
		}
		final String hours = String.format( format, elapsedTime / 3600 );
		final String minutes = String.format( format, elapsedTime % 3600 / 60 );
		String seconds = String.format( format, elapsedTime % 60 );
		if( hours.equals( "00" ) && minutes.equals( "00" ) && seconds.equals( "00" ) )
		{
			seconds = "01";
		}

		return hours + " hours : " + minutes + " minutes : " + seconds + " seconds";
	}

	/**
	 * This method returns all of the lines from any failure files found in the script directory.
	 * 
	 * @param module ScriptModule
	 * @return List of script errors
	 * @throws IOException if errors occur reading failure files
	 */
	public static List<String> getScriptErrors( final ScriptModule module ) throws IOException
	{
		final List<String> errors = new ArrayList<>();
		final IOFileFilter ffFailed = new WildcardFileFilter( "*_" + Pipeline.SCRIPT_FAILURES );
		final Collection<File> failedScripts = FileUtils.listFiles( module.getScriptDir(), ffFailed, null );
		for( final File script: failedScripts )
		{
			final BufferedReader reader = BioLockJUtil.getFileReader( script );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					errors.add( script.getName() + " | " + line );
				}
			}
			finally
			{
				reader.close();
			}
		}

		return errors;
	}

	/**
	 * Get BioModule subdirectory File object with given name.
	 *
	 * @param bioModule BioModule
	 * @param subDirName BioModule subdirectory name
	 * @return BioModule subdirectory File object
	 */
	public static File getSubDir( final BioModule bioModule, final String subDirName )
	{
		final File dir = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + subDirName );
		if( !dir.exists() )
		{
			return null;
		}

		return dir;
	}

	/**
	 * Return a system generated metadata column name based on the module status.
	 * 
	 * @param module BioModule
	 * @param col Column name
	 * @return Metadata column name
	 * @throws Exception if errors occur
	 */
	public static String getSystemMetaCol( final BioModule module, final String col ) throws Exception
	{
		final File outputMeta = new File(
				module.getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );
		if( ModuleUtil.isComplete( module ) || outputMeta.exists() )
		{
			if( outputMeta.exists() )
			{
				MetaUtil.setFile( outputMeta );
				MetaUtil.refreshCache();
			}
			return MetaUtil.getLatestColumnName( col );
		}
		else
		{
			return MetaUtil.getForcedColumnName( col );
		}
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
	 * Check pipeline for configured {@link biolockj.module.r.R_Module}s.
	 * 
	 * @return TRUE if {@link biolockj.module.r.R_Module} is found
	 * @throws Exception if errors occur
	 */
	public static boolean hasRModules() throws Exception
	{
		return getFirstRModule() != null;
	}

	/**
	 * Return TRUE if bioModule completed successfully.
	 *
	 * @param bioModule BioModule
	 * @return TRUE if bioModule has completed successfully.
	 */
	public static boolean isComplete( final BioModule bioModule )
	{
		final File f = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + Pipeline.BLJ_COMPLETE );
		return f.exists();
	}

	/**
	 * Test if module is the first {@link biolockj.module.r.R_Module} configured in the pipeline.
	 * 
	 * @param module BioModule to test
	 * @return TRUE if module is 1st {@link biolockj.module.r.R_Module}
	 * @throws Exception if errors occur
	 */
	public static boolean isFirstRModule( final BioModule module ) throws Exception
	{
		return module != null && hasRModules()
				&& module.getClass().getName().equals( getFirstRModule().getClass().getName() );
	}

	/**
	 * Return TRUE if bioModule started execution but is not complete.
	 *
	 * @param bioModule BioModule
	 * @return TRUE if bioModule started execution but is not complete
	 */
	public static boolean isIncomplete( final BioModule bioModule )
	{
		final File f = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + Pipeline.BLJ_STARTED );
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
		for( final File f: module.getOutputDir().listFiles() )
		{
			if( f.getName().equals( MetaUtil.getMetadataFileName() ) )
			{
				foundMeta = true;
			}
			else if( !Config.getTreeSet( Config.INPUT_IGNORE_FILES ).contains( f.getName() ) )
			{
				foundOther = true;
			}
		}

		return foundMeta && !foundOther;
	}

	/**
	 * Method creates a file named {@value biolockj.Pipeline#BLJ_COMPLETE} in bioModule root directory to document
	 * bioModule has completed successfully. Also clean up by removing file {@value biolockj.Pipeline#BLJ_STARTED}.
	 *
	 * @param bioModule BioModule
	 * @throws Exception if unable to create {@value biolockj.Pipeline#BLJ_COMPLETE} file
	 */
	public static void markComplete( final BioModule bioModule ) throws Exception
	{
		final File f = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + Pipeline.BLJ_COMPLETE );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.exists() )
		{
			throw new Exception( "Unable to create " + f.getAbsolutePath() );
		}
		final File startFile = new File(
				bioModule.getModuleDir().getAbsolutePath() + File.separator + Pipeline.BLJ_STARTED );
		BioLockJUtil.deleteWithRetry( startFile, 5 );
		Log.info( ModuleUtil.class, Log.LOG_SPACER );
		Log.info( ModuleUtil.class, "FINISHED " + bioModule.getClass().getName() );
		Log.info( ModuleUtil.class, Log.LOG_SPACER );
	}

	/**
	 * Method creates a file named {@value biolockj.Pipeline#BLJ_STARTED} in bioModule root directory to document
	 * bioModule has completed successfully. Also sets the start time and caches module name to list of executed modules
	 * so we can check later if it ran during this pipeline execution (as opposed to a previous failed run).
	 *
	 * @param bioModule BioModule
	 * @throws Exception if unable to create {@value biolockj.Pipeline#BLJ_STARTED} file
	 */
	public static void markStarted( final BioModule bioModule ) throws Exception
	{
		final File f = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + Pipeline.BLJ_STARTED );
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
	 * Return true if moduleName is configured to run in pipeline
	 *
	 * @param moduleName Name of module to check
	 * @return true if moduleName is configured to run in pipeline, else false
	 * @throws Exception if propagated by {@link #getModule( String )}
	 */
	public static boolean moduleExists( final String moduleName ) throws Exception
	{
		return getModule( moduleName ) != null;
	}

	/**
	 * Get BioModule subdirectory File object with given name. If directory doesn't exist, create it.
	 *
	 * @param bioModule BioModule
	 * @param subDirName BioModule subdirectory name
	 * @return BioModule subdirectory File object
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
	 * Return TRUE if BioModule subdirectory exists
	 *
	 * @param bioModule BioModule
	 * @param subDirName BioModule subdirectory name
	 * @return TRUE if BioModule subdirectory exists
	 */
	public static boolean subDirExists( final BioModule bioModule, final String subDirName )
	{
		final File dir = new File( bioModule.getModuleDir().getAbsolutePath() + File.separator + subDirName );
		return dir.exists();
	}

}
