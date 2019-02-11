/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Dec 18, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.File;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.BioModule;

/**
 * This utility helps work HumanN2 pathway abundance data.<br>
 */
public class PathwayUtil
{
	// Prevent instantiation
	private PathwayUtil()
	{}

	/**
	 * Return a pathway abundance file path in the given dir with the given suffix (if provided).
	 * 
	 * @param dir Directory for pathway file
	 * @param suffix Optional suffix
	 * @return Pathway count file
	 * @throws Exception if errors occur
	 */
	public static File getPathwayCountFile( final File dir, String suffix ) throws Exception
	{
		suffix = suffix == null ? "": "_" + suffix;
		final String name = Config.pipelineName() + "_" + Constants.HN2_PATH_ABUND_TABLE + suffix + Constants.TSV_EXT;
		return new File( dir.getAbsolutePath() + File.separator + name );
	}

	/**
	 * Check the file name to determine if it is a pathway abundance table file.
	 * 
	 * @param file File to test
	 * @return boolean TRUE if file is a athway abundance file
	 * @throws Exception if errors occur
	 */
	public static boolean isPathwayFile( final File file ) throws Exception
	{
		final String name = file.getName();
		if( name.contains( "_" + Constants.HN2_PATH_ABUND_TABLE ) && name.endsWith( Constants.TSV_EXT ) )
		{
			return true;
		}
		return false;
	}

	/**
	 * Check the module to determine if it generated OTU count files.
	 * 
	 * @param module BioModule
	 * @return TRUE if module generated OTU count files
	 * @throws Exception if errors occur
	 */
	public static boolean isPathwayModule( final BioModule module )
	{
		try
		{
			final Collection<File> files = BioLockJUtil.removeIgnoredAndEmptyFiles(
					FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );

			for( final File f: files )
			{
				if( isPathwayFile( f ) )
				{
					return true;
				}
			}
		}
		catch( final Exception ex )
		{
			Log.warn( PathwayUtil.class, "Error occurred while inspecting module output files: " + module );
			ex.printStackTrace();
		}
		return false;
	}
}
