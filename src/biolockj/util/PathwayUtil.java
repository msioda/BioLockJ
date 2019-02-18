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
import biolockj.exception.ConfigViolationException;
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
	 * Check pipeline input contains Humann2Parser module output.
	 * 
	 * @return TRUE if pipeline input contains module input
	 */
	public static boolean containsHn2ParserOutput( final Collection<File> files )
	{
		for( final File file: files )
		{
			final String name = file.getName();
			if( name.equals( PathwayUtil.getHn2ClassifierOutput( Constants.HN2_PATH_ABUND_SUM ) )
					|| name.equals( PathwayUtil.getHn2ClassifierOutput( Constants.HN2_PATH_COVG_SUM ) )
					|| name.equals( PathwayUtil.getHn2ClassifierOutput( Constants.HN2_GENE_FAM_SUM ) ) )
			{
				return true;
			}

		}
		return false;
	}

	public static String getHn2ClassifierOutput( final String key )
	{
		return Config.pipelineName() + "_" + key + "_" + TaxaUtil.SPECIES + Constants.TSV_EXT;
	}

	public static String getHn2Type( final File file ) throws Exception
	{
		if( file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) )
		{
			return Constants.HN2_PATH_ABUND_SUM;
		}
		if( file.getName().contains( Constants.HN2_PATH_COVG_SUM ) )
		{
			return Constants.HN2_PATH_COVG_SUM;
		}
		if( file.getName().contains( Constants.HN2_GENE_FAM_SUM ) )
		{
			return Constants.HN2_GENE_FAM_SUM;
		}
		throw new Exception( "Invalid Pathway file [ " + file.getAbsolutePath()
				+ " ] name does not match HumanN2 output file name format.  Valid file suffixes contain: "
				+ validFormats );
	}

	/**
	 * Return a pathway abundance file path in the given dir with the given prefix (if provided).
	 * 
	 * @param dir Directory for pathway file
	 * @param hn2OutputFile Root file indicates type of coverage, abundance, or gene
	 * @param prefix Optional prefix
	 * @return Pathway count file
	 * @throws Exception if invalid file name formats are found
	 */
	public static File getPathwayCountFile( final File dir, final File hn2OutputFile, String prefix ) throws Exception
	{
		prefix = prefix == null ? "": prefix;
		if( !prefix.startsWith( Config.pipelineName() ) )
		{
			prefix = Config.pipelineName() + "_" + prefix;
		}
		final String name = prefix + "_" + getHn2Type( hn2OutputFile ) + pathwayFileSuffix();
		return new File( dir.getAbsolutePath() + File.separator + name );
	}

	/**
	 * Check the file name to determine if it is a pathway abundance table file.
	 * 
	 * @param file File to test
	 * @return boolean TRUE if file is a pathway abundance file
	 * @throws Exception if errors occur
	 */
	public static boolean isPathwayFile( final File file ) throws Exception
	{
		final boolean isPathAund = file.getName().contains( Constants.HN2_PATH_ABUND_SUM );
		final boolean isPathCovg = file.getName().contains( Constants.HN2_PATH_COVG_SUM );
		final boolean isGeneFaml = file.getName().contains( Constants.HN2_GENE_FAM_SUM );

		if( file.getName().endsWith( pathwayFileSuffix() ) && ( isPathAund || isPathCovg || isGeneFaml ) )
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


	private static String pathwayFileSuffix()
	{
		return "_" + TaxaUtil.SPECIES + Constants.TSV_EXT;
	}

	/**
	 * Verify the HumanN2 Config contains at least one of the following reports are enabled:<br>
	 * <ul>
	 * <li>{@valud biolockj.Constants#HN2_DISABLE_GENE_FAMILIES}
	 * <li>{@valud biolockj.Constants#HN2_DISABLE_PATH_COVERAGE}
	 * <li>{@valud biolockj.Constants#HN2_DISABLE_GENE_FAMILIES}
	 * </ul>
	 * 
	 * @param module HumanN2 module
	 * @throws Exception
	 */
	public static void verifyConfig( final BioModule module ) throws Exception
	{
		if( Config.getBoolean( module, Constants.HN2_DISABLE_PATH_ABUNDANCE )
				&& Config.getBoolean( module, Constants.HN2_DISABLE_PATH_COVERAGE )
				&& Config.getBoolean( module, Constants.HN2_DISABLE_GENE_FAMILIES ) )
		{
			throw new ConfigViolationException(
					"Must enable at least one type of HumanN2 report.  All 3 reports are disable via Config properties: "
							+ Constants.HN2_DISABLE_PATH_ABUNDANCE + "=" + Constants.FALSE + ", "
							+ Constants.HN2_DISABLE_PATH_COVERAGE + "=" + Constants.FALSE + ", "
							+ Constants.HN2_DISABLE_GENE_FAMILIES + "=" + Constants.FALSE );
		}
	}

	private static String validFormats = "[ " + Constants.HN2_PATH_ABUND_SUM + ", " + Constants.HN2_PATH_COVG_SUM + ", "
			+ Constants.HN2_GENE_FAM_SUM + " ]";
}
