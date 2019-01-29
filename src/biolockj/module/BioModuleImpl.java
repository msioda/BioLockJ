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
import biolockj.util.*;

/**
 * Superclass for standard BioModules (classifiers, parsers, etc). Sets standard behavior for many of the BioModule
 * interface methods.
 */
public abstract class BioModuleImpl implements BioModule
{

	/**
	 * Cache the input files for quick access on subsequent calls to {@linke #getInputFiles()}
	 * 
	 * @param files Input files
	 * @throws Exception if errors occur
	 */
	public void cacheInputFiles( final Collection<File> files ) throws Exception
	{
		if( files == null || files.isEmpty() )
		{
			throw new Exception( "No input files found!" );
		}

		inputFiles.addAll( files );
		Collections.sort( inputFiles );
		Log.info( getClass(), "# Input Files: " + inputFiles.size() );
		for( int i = 0; i < inputFiles.size(); i++ )
		{
			Log.info( getClass(), "Input File [" + i + "]: " + inputFiles.get( i ).getAbsolutePath() );
		}
	}

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

	/**
	 * Compared based on ID
	 */
	@Override
	public boolean equals( final Object o )
	{
		if( o == this )
		{
			return true;
		}
		else if( !( o instanceof BioModule ) )
		{
			return false;
		}

		return ( (BioModule) o ).getID().equals( getID() );
	}

	@Override
	public abstract void executeTask() throws Exception;

	@Override
	public Integer getID()
	{
		return moduleId;
	}

	/**
	 * BioModule {@link #getInputFiles()} is called to initialize upon first call and cached.
	 */
	@Override
	public List<File> getInputFiles() throws Exception
	{
		if( getFileCache().isEmpty() )
		{
			cacheInputFiles( findModuleInputFiles() );
		}

		return getFileCache();
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
	public List<String> getPostRequisiteModules() throws Exception
	{
		return new ArrayList<>();
	}

	/**
	 * By default, no prerequisites are required.
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
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
	 * This method must be called immediately upon instantiation.
	 * 
	 * @throws Exception if errors occur
	 */
	@Override
	public void init() throws Exception
	{
		moduleId = nextId++;
		moduleDir = new File( Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath()
				+ File.separator + BioLockJUtil.formatDigits( moduleId, ModuleUtil.maxNumModules().toString().length() )
				+ "_" + getClass().getSimpleName() );

		if( !moduleDir.exists() )
		{
			moduleDir.mkdirs();
			Log.info( getClass(), "Construct module [ " + moduleId + " ] for new" + moduleDir.getAbsolutePath() );
		}
		else
		{
			Log.info( getClass(), "Construct module [ " + moduleId + " ] for existing" + moduleDir.getAbsolutePath() );
		}
	}

	/**
	 * In the early stages of the pipeline, starting with the very 1st module
	 * {@link biolockj.module.implicit.ImportMetadata}, most modules expect sequence files as input. This method returns
	 * false if the previousModule only produced a new metadata file, such as
	 * {@link biolockj.module.implicit.ImportMetadata} or {@link biolockj.module.implicit.RegisterNumReads}.
	 * 
	 * When {@link #getInputFiles()} is called, this method determines if the previousModule output is valid input for
	 * the current BioModule. The default implementation of this method returns FALSE if the previousModule only
	 * generates a new metadata file.
	 */
	@Override
	public boolean isValidInputModule( final BioModule module ) throws Exception
	{
		return !ModuleUtil.isMetadataModule( module );
	}

	@Override
	public String toString()
	{
		return getClass().getName() + "_" + getID();
	}

	/**
	 * Called upon first access of input files to return sorted list of files from all
	 * {@link biolockj.Config}.{@value biolockj.Config#INPUT_DIRS}<br>
	 * Hidden files (starting with ".") are ignored<br>
	 * Call {@link #isValidInputModule(BioModule)} on each previous module until acceptable input files are found<br>
	 * 
	 * @return Set of input files
	 * @throws Exception if errors occur
	 */
	protected List<File> findModuleInputFiles() throws Exception
	{
		final Set<File> moduleInputFiles = new HashSet<>();
		Log.debug( getClass(), "Initialize input files..." );
		boolean validInput = false;
		BioModule previousModule = ModuleUtil.getPreviousModule( this );
		while( !validInput )
		{
			if( previousModule == null )
			{
				Log.debug( getClass(), "Previous module is NULL.  Return pipleline input from: " + Config.INPUT_DIRS );
				moduleInputFiles.addAll( BioLockJUtil.getPipelineInputFiles() );
				validInput = true;
			}
			else
			{
				Log.debug( getClass(),
						"Check previous module for valid input files... # " + previousModule.getClass().getName()
								+ " ---> dir: " + previousModule.getOutputDir().getAbsolutePath() );
				validInput = isValidInputModule( previousModule );
				if( validInput )
				{
					Log.debug( getClass(),
							"Found VALID input in the output dir of: " + previousModule.getClass().getName() + " --> "
									+ previousModule.getOutputDir().getAbsolutePath() );
					moduleInputFiles.addAll( FileUtils.listFiles( previousModule.getOutputDir(),
							HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );
					Log.debug( getClass(), "# Files found: " + moduleInputFiles.size() );
				}
				else
				{
					previousModule = ModuleUtil.getPreviousModule( previousModule );
				}
			}
		}

		return removeIgnoredFiles( moduleInputFiles );
	}

	/**
	 * Get cached input files
	 * 
	 * @return List of input files
	 */
	protected List<File> getFileCache()
	{
		return inputFiles;
	}

	/**
	 * Remove ignore files from the input files.
	 * 
	 * @param files Collection of files
	 * @return valid files
	 * @throws Exception if errors occur
	 */
	protected List<File> removeIgnoredFiles( final Collection<File> files ) throws Exception
	{
		return SeqUtil.removeIgnoredAndEmptyFiles( files );
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
	private Integer moduleId;
	/**
	 * BioLockJ gzip file extension constant: {@value #GZIP_EXT}
	 */
	public static final String GZIP_EXT = BioLockJ.GZIP_EXT;
	/**
	 * BioLockJ log file extension constant: {@value #LOG_EXT}
	 */
	public static final String LOG_EXT = BioLockJ.LOG_EXT;

	/**
	 * BioLockJ PDF file extension constant: {@value #PDF_EXT}
	 */
	public static final String PDF_EXT = BioLockJ.PDF_EXT;

	/**
	 * Return character constant *backslash-n*
	 */
	public static final String RETURN = BioLockJ.RETURN;

	/**
	 * BioLockJ shell script file extension constant: {@value #SH_EXT}
	 */
	public static final String SH_EXT = BioLockJ.SH_EXT;

	/**
	 * BioLockJ tab character constant: {@value #TAB_DELIM}
	 */
	public static final String TAB_DELIM = BioLockJ.TAB_DELIM;

	/**
	 * BioLockJ tab delimited text file extension constant: {@value #TSV_EXT}
	 */
	public static final String TSV_EXT = BioLockJ.TSV_EXT;

	/**
	 * BioLockJ tab delimited text file extension constant: {@value #TXT_EXT}
	 */
	public static final String TXT_EXT = BioLockJ.TXT_EXT;

	private static Integer nextId = 0;

}
