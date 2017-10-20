/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer 	This code is free software; you can redistribute it and/or
 * 				modify it under the terms of the GNU General Public License
 * 				as published by the Free Software Foundation; either version 2
 * 				of the License, or (at your option) any later version,
 * 				provided that any use properly credits the author.
 * 				This program is distributed in the hope that it will be useful,
 * 				but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * 				GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;

/**
 * Superclass for basic Modules (classifiers, parsers, reports).
 */
public abstract class BioModuleImpl implements BioModule
{

	@Override
	public abstract void checkDependencies() throws Exception;

	@Override
	public abstract void executeProjectFile() throws Exception;

	@Override
	public File getInputDir()
	{
		return inputDir;
	}

	@Override
	public List<File> getInputFiles() throws Exception
	{
		if( inputFiles.isEmpty() )
		{
			initInputFiles( TrueFileFilter.INSTANCE, null );
		}

		if( MetaUtil.exists() )
		{
			final File inputMeta = new File(
					getInputDir().getAbsolutePath() + File.separator + MetaUtil.getFile().getName() );
			if( inputMeta.exists() )
			{
				MetaUtil.setFile( inputMeta );
				MetaUtil.refresh();
				inputFiles.remove( inputMeta );
			}
		}

		return inputFiles;
	}

	@Override
	public String[] getJobParams() throws Exception
	{
		final String[] cmd = new String[ 1 ];
		cmd[ 0 ] = ModuleUtil.getMainScript( this ).getAbsolutePath();
		Log.out.info( "Executing Script: " + ModuleUtil.getMainScript( this ).getName() );
		return cmd;
	}

	@Override
	public File getModuleDir()
	{
		return moduleDir;
	}

	@Override
	public File getOutputDir()
	{
		return ModuleUtil.requireSubDir( this, OUTPUT_DIR );
	}

	@Override
	public File getScriptDir()
	{
		return ModuleUtil.requireSubDir( this, SCRIPT_DIR );
	}

	/**
	 * Summary message.
	 */
	@Override
	public String getSummary()
	{
		return ModuleUtil.getScriptDirSummary( this ) + ModuleUtil.getOutputDirSummary( this )
				+ ModuleUtil.getFailureDirSummary( this );
	}

	@Override
	public File getTempDir()
	{
		return ModuleUtil.requireSubDir( this, TEMP_DIR );
	}

	@Override
	public Integer getTimeout()
	{
		return null;
	}

	/**
	 * Set input directory and set inputFiles to any file in top level of dir that doesn't
	 * start with "." to avoid hidden files.
	 *
	 *
	 * @throws Exception
	 */
	@Override
	public void initInputFiles( final IOFileFilter ff, final IOFileFilter recursive ) throws Exception
	{
		final List<File> dirs = getInputDirs( inputDir );
		final Set<String> fileNames = new HashSet<>();
		int index = 0;
		for( final File d: dirs )
		{
			final Collection<File> files = FileUtils.listFiles( d, ff, recursive );
			if( ( files == null ) || files.isEmpty() )
			{
				throw new Exception( "No input files found in directory: " + d );
			}

			Log.out.info( "# Files found: " + ( ( files == null ) ? 0: files.size() ) );
			for( final File f: files )
			{
				if( !f.isDirectory() && !f.getName().startsWith( "." )
						&& !Config.getSet( Config.INPUT_IGNORE_FILES ).contains( f.getName() ) )
				{
					validateFileNameUnique( fileNames, f );
					fileNames.add( f.getName() );
					Log.out.info( "INPUT FILE[" + index++ + "] = " + f.getAbsolutePath() );
					inputFiles.add( f );
				}
				else
				{
					Log.out.warn( "Skipping file: " + f.getName() );
				}
			}
		}

		Collections.sort( inputFiles );
	}

	@Override
	public void setInputDir( final File dir )
	{
		Log.out.info( "Set " + getClass().getName() + " input dir: " + dir );
		inputDir = dir;
	}

	@Override
	public void setModuleDir( final String name )
	{
		moduleDir = new File( name );
		if( !moduleDir.exists() )
		{
			moduleDir.mkdirs();
			Log.out.info( "Create Executor Directory: " + moduleDir.getAbsolutePath() );
		}
	}

	protected void validateFileNameUnique( final Set<String> fileNames, final File f ) throws Exception
	{
		if( fileNames.contains( f.getName() ) )
		{
			throw new Exception( "File names must be unique!  Duplicate file: " + f.getAbsolutePath() );
		}
	}

	private List<File> getInputDirs( final File dir ) throws Exception
	{
		if( dir == null )
		{
			final List<File> inputDirs = Config.requireExistingDirs( Config.INPUT_DIRS );
			setInputDir( inputDirs.get( 0 ) );
			return inputDirs;
		}

		setInputDir( dir );
		return new ArrayList<>( Arrays.asList( dir ) );
	}

	private File inputDir = null;
	private final List<File> inputFiles = new ArrayList<>();
	private File moduleDir = null;
	public static final String RETURN = BioLockJ.RETURN;
	public static final String TAB_DELIM = BioLockJ.TAB_DELIM;
}
