/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jan 20, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.humann2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.parser.wgs.Humann2Parser;
import biolockj.util.BioLockJUtil;
import biolockj.util.PathwayUtil;

/**
 * TBD
 */
public abstract class Humann2CountModule extends JavaModuleImpl implements JavaModule
{
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		PathwayUtil.verifyConfig( this );
	}

	@Override
	public List<File> getInputFiles() throws Exception
	{
		if( getFileCache().isEmpty() )
		{
			final List<File> files = new ArrayList<>();
			for( final File f: findModuleInputFiles() )
			{
				if( PathwayUtil.isPathwayFile( f ) )
				{
					files.add( f );
				}
			}
			cacheInputFiles( files );
		}
		return getFileCache();
	}

	/**
	 * Module prerequisite: {@link biolockj.module.implicit.parser.wgs.Humann2Parser}
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
	{
		final List<String> preReqs = new ArrayList<>();
		if( !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_HUMANN2_COUNT_TABLE_INPUT_TYPE ) )
		{
			preReqs.add( Humann2Parser.class.getName() );
		}
		preReqs.addAll( super.getPreRequisiteModules() );
		return preReqs;
	}

	@Override
	public boolean isValidInputModule( final BioModule module )
	{
		return isHumann2CountModule( module );
	}

	protected boolean hasAbund() throws Exception
	{
		return Config.getBoolean( this, Constants.REPORT_NUM_HITS )
				&& !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_ABUNDANCE );

	}

	/**
	 * Check the module to determine if it generated OTU count files.
	 * 
	 * @param module BioModule
	 * @return TRUE if module generated OTU count files
	 */
	protected boolean isHumann2CountModule( final BioModule module )
	{
		try
		{
			final Collection<File> files = BioLockJUtil.removeIgnoredAndEmptyFiles(
					FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );

			for( final File f: files )
			{
				if( PathwayUtil.isPathwayFile( f ) )
				{
					return true;
				}
			}
		}
		catch( final Exception ex )
		{
			Log.warn( getClass(), "Error occurred while inspecting module output files: " + module );
			ex.printStackTrace();
		}
		return false;
	}
}
