/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module;

import java.io.File;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.util.ModuleUtil;
import biolockj.util.SeqUtil;
import biolockj.util.SummaryUtil;

/**
 * Superclass for standard BioModules (classifiers, parsers, etc). Sets standard behavior for many of the BioModule
 * interface methods.
 */
public abstract class BioModuleImpl implements BioModule
{

	/**
	 * If restarting or running a direct pipeline execute the cleanup for completed modules.
	 */
	@Override
	public abstract void checkDependencies() throws Exception;

	/**
	 * By default, no cleanUp code is required.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		Log.info( getClass(), "Clean up: " + getClass().getName() );
	}

	@Override
	public abstract void executeTask() throws Exception;

	/**
	 * BioModule {@link #initInputFiles()} is called to initialize upon first call and cached.
	 */
	@Override
	public List<File> getInputFiles() throws Exception
	{
		if( inputFiles.isEmpty() )
		{
			initInputFiles();
		}

		return inputFiles;
	}

	/**
	 * All BioModule work must be contained within the scope of its root directory.
	 */
	@Override
	public File getModuleDir()
	{
		return moduleDir;
	}

	/**
	 * Returns moduleDir/output which will be used as the next module's input.
	 */
	@Override
	public File getOutputDir()
	{
		return ModuleUtil.requireSubDir( this, OUTPUT_DIR );
	}

	/**
	 * By default, no post-requisites are required.
	 */
	@Override
	public List<Class<?>> getPostRequisiteModules() throws Exception
	{
		return new ArrayList<>();
	}

	/**
	 * By default, no prerequisites are required.
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		return new ArrayList<>();
	}

	/**
	 * Returns summary message to be displayed by Email module so must not contain confidential info. ModuleUtil
	 * provides summary metrics on output files
	 */
	@Override
	public String getSummary() throws Exception
	{
		Log.info( SummaryUtil.class, "Building module summary for: " + getClass().getSimpleName() );
		return SummaryUtil.getOutputDirSummary( this );
	}

	/**
	 * Returns moduleDir/temp for intermediate files. If {@link biolockj.BioLockJ#PROJECT_DELETE_TEMP_FILES} =
	 * {@value biolockj.Config#TRUE}, this directory is deleted after pipeline completes successfully.
	 */
	@Override
	public File getTempDir()
	{
		return ModuleUtil.requireSubDir( this, TEMP_DIR );
	}

	/**
	 * In the early stages of the pipeline, starting with the very 1st module
	 * {@link biolockj.module.implicit.ImportMetadata}, most modules expect sequence files as input. This method returns
	 * false if the previousModule only produced a new metadata file, such as
	 * {@link biolockj.module.implicit.ImportMetadata} or {@link biolockj.module.implicit.RegisterNumReads}.
	 * 
	 * When {@link #initInputFiles()} is called, this method determines if the previousModule output is valid input for
	 * the current BioModule. The default implementation of this method returns FALSE if the previousModule only
	 * generates a new metadata file.
	 */
	@Override
	public boolean isValidInputModule( final BioModule module ) throws Exception
	{
		return !ModuleUtil.isMetadataModule( module );
	}

	/**
	 * Creates the BioModule root directory if it doesn't exist and sets moduleDir member variable.
	 */
	@Override
	public void setModuleDir( final String filePath )
	{
		moduleDir = new File( filePath );
		if( !moduleDir.exists() )
		{
			moduleDir.mkdirs();
			Log.info( getClass(), "Create BioModule root directory: " + moduleDir.getAbsolutePath() );
		}
	}

	/**
	 * Called upon first access of input files to return sorted list of files from all inputDirs.<br>
	 * Hidden files (starting with ".") are ignored.<br>
	 * {@link #isValidInputModule(BioModule)} is called to determine if the previous module output is acceptable input
	 * for the current module. If not, it checks the previous module recursively until valid input is found.
	 * 
	 * @throws Exception if errors occur
	 */
	protected void initInputFiles() throws Exception
	{
		Log.debug( getClass(), "Initialize input files..." );
		boolean validInput = false;
		final Set<File> files = new HashSet<>();
		BioModule previousModule = ModuleUtil.getPreviousModule( this );
		while ( !validInput )
		{
			if( previousModule == null )
			{
				Log.debug( getClass(), "Previous module is null...pull input.dirPaths data" );
				files.addAll( SeqUtil.getPipelineInputFiles() );
				validInput = true;
			}
			else
			{
				Log.debug( getClass(), "Check previous module for valid input files... # " + previousModule.getClass().getName() );
				validInput = isValidInputModule( previousModule );
				if( validInput )
				{
					Log.debug( getClass(), "Found VALID input in the output dir of: " 
							+ previousModule.getClass().getName() + " --> " + previousModule.getOutputDir().getAbsolutePath() );
					files.addAll( FileUtils.listFiles( previousModule.getOutputDir(), HiddenFileFilter.VISIBLE,
							HiddenFileFilter.VISIBLE ) );
				}
				else
				{
					previousModule = ModuleUtil.getPreviousModule( previousModule );
					//List<File> prevInput = previousModule.getInputFiles();
					//files.addAll( prevInput );
				}
			}
		}

		for( final File f: files )
		{
			if( !Config.getSet( Config.INPUT_IGNORE_FILES ).contains( f.getName() ) )
			{
				inputFiles.add( f );
			}
			else
			{
				Log.debug( getClass(), "Ignore file " + f.getAbsolutePath() );
			}
		}

		if( inputFiles.isEmpty() )
		{
			throw new Exception( "No input files found!" );
		}

		Collections.sort( inputFiles );

		Log.info( getClass(), "# Input Files: " + inputFiles.size() );
		for( int i = 0; i < inputFiles.size(); i++ )
		{
			Log.info( getClass(), "Input File[" + i + "] = " + inputFiles.get( i ).getAbsolutePath() );
		}
	}

	/**
	 * Validate files in {@link biolockj.Config#INPUT_DIRS} have unique names. BioModules that expect duplicates must
	 * override this method.
	 *
	 * @param fileNames A registry of module input file names added so far
	 * @param file Next file to validate
	 * @throws Exception if a duplicate file name found
	 */
	protected void validateFileNameUnique( final Set<String> fileNames, final File file ) throws Exception
	{
		if( fileNames.contains( file.getName() ) )
		{
			throw new Exception( "File names must be unique!  Duplicate file: " + file.getAbsolutePath() );
		}
	}


	private final List<File> inputFiles = new ArrayList<>();

	private File moduleDir = null;

	/**
	 * BioLockJ newline = "\n"
	 */
	public static final String RETURN = BioLockJ.RETURN;

	/**
	 * BioLockJ tab delim = "\t"
	 */
	public static final String TAB_DELIM = BioLockJ.TAB_DELIM;
}
