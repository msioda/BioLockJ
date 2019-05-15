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
import java.util.*;
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
public class PathwayUtil {
	// Prevent instantiation
	private PathwayUtil() {}

	/**
	 * Check pipeline input contains Humann2Parser module output.
	 * 
	 * @param files Files to test
	 * @return TRUE if pipeline input contains module input
	 */
	public static boolean containsHn2ParserOutput( final Collection<File> files ) {
		for( final File file: files ) {
			final String name = file.getName();
			if( name.equals( PathwayUtil.getHn2ClassifierOutput( Constants.HN2_PATH_ABUND_SUM ) )
				|| name.equals( PathwayUtil.getHn2ClassifierOutput( Constants.HN2_PATH_COVG_SUM ) )
				|| name.equals( PathwayUtil.getHn2ClassifierOutput( Constants.HN2_GENE_FAM_SUM ) ) ) return true;

		}
		return false;
	}

	/**
	 * Humann2 classifier outputs 3 types of files:
	 * <ul>
	 * <li>{@value biolockj.Constants#HN2_PATH_ABUND_SUM}
	 * <li>{@value biolockj.Constants#HN2_PATH_COVG_SUM}
	 * <li>{@value biolockj.Constants#HN2_GENE_FAM_SUM}
	 * </ul>
	 * Given the type, this method retuns the output file name.
	 * 
	 * @param type Humann2 classifier output type
	 * @return Name of humann2 output file of the given type
	 */
	public static String getHn2ClassifierOutput( final String type ) {
		return Config.pipelineName() + "_" + type + Constants.TSV_EXT;
	}

	/**
	 * Humann2 classifier outputs 3 types of files:
	 * <ul>
	 * <li>{@value biolockj.Constants#HN2_PATH_ABUND_SUM}
	 * <li>{@value biolockj.Constants#HN2_PATH_COVG_SUM}
	 * <li>{@value biolockj.Constants#HN2_GENE_FAM_SUM}
	 * </ul>
	 * This method returns the type included in the given file name.
	 * 
	 * 
	 * @param file Humann2 classifier output type
	 * @return Name of humann2 output file of the given type
	 * @throws Exception if unable to determine the type
	 */
	public static String getHn2Type( final File file ) throws Exception {
		if( file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) ) return Constants.HN2_PATH_ABUND_SUM;
		if( file.getName().contains( Constants.HN2_PATH_COVG_SUM ) ) return Constants.HN2_PATH_COVG_SUM;
		if( file.getName().contains( Constants.HN2_GENE_FAM_SUM ) ) return Constants.HN2_GENE_FAM_SUM;
		throw new Exception( "Invalid Pathway file [ " + file.getAbsolutePath()
			+ " ] name does not match HumanN2 output file name format.  Valid file suffixes contain: " + validFormats );
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
	public static File getPathwayCountFile( final File dir, final File hn2OutputFile, final String prefix )
		throws Exception {
		String myPrefix = prefix == null ? "": prefix;
		if( !myPrefix.startsWith( Config.pipelineName() ) ) myPrefix = Config.pipelineName() + "_" + prefix;
		final String name = myPrefix + "_" + getHn2Type( hn2OutputFile ) + Constants.TSV_EXT;
		return new File( dir.getAbsolutePath() + File.separator + name );
	}

	/**
	 * Check the file name to determine if it is a pathway abundance table file.
	 * 
	 * @param file File to test
	 * @return boolean TRUE if file is a pathway abundance file
	 */
	public static boolean isPathwayFile( final File file ) {
		final boolean isPathAund = file.getName().endsWith( "_" + Constants.HN2_PATH_ABUND_SUM + Constants.TSV_EXT );
		final boolean isPathCovg = file.getName().contains( "_" + Constants.HN2_PATH_COVG_SUM + Constants.TSV_EXT );
		final boolean isGeneFaml = file.getName().contains( "_" + Constants.HN2_GENE_FAM_SUM + Constants.TSV_EXT );

		if( isPathAund || isPathCovg || isGeneFaml ) return true;
		return false;
	}

	/**
	 * Check the module to determine if it generated OTU count files.
	 * 
	 * @param module BioModule
	 * @return TRUE if module generated OTU count files
	 */
	public static boolean isPathwayModule( final BioModule module ) {
		try {
			final Collection<File> files = BioLockJUtil.removeIgnoredAndEmptyFiles(
				FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );

			for( final File f: files )
				if( isPathwayFile( f ) ) return true;
		} catch( final Exception ex ) {
			Log.warn( PathwayUtil.class, "Error occurred while inspecting module output files: " + module );
			ex.printStackTrace();
		}
		return false;
	}

	/**
	 * Determine if humann2 provided most recent raw count data, used to determine getPreReq modules.
	 * 
	 * @param module BioModule
	 * @return TRUE if humann2 counts should be used
	 * @throws Exception if errors occur
	 */
	public static Boolean useHumann2RawCount( final BioModule module ) throws Exception {
		Integer initCount = initMap.get( module.getClass().getName() );
		if( initCount == null ) initCount = 0;
		initMap.put( module.getClass().getName(), ++initCount );

		Log.debug( module.getClass(), "Check to see if module inherits HumanN2 or Taxa tables..." );
		final List<String> mods = Config.requireList( null, Constants.INTERNAL_BLJ_MODULE );
		final List<Integer> classifiers = new ArrayList<>();
		final List<Integer> parsers = new ArrayList<>();
		final List<Integer> hn2Classifiers = new ArrayList<>();
		final List<Integer> otherRModules = new ArrayList<>();
		final List<Integer> thisClass = new ArrayList<>();
		for( int i = 0; i < mods.size(); i++ ) {
			final boolean isParser = mods.get( i ).toLowerCase().contains( "parser" );
			final boolean isClassifier = mods.get( i ).toLowerCase().contains( "classifier" );
			final boolean isHn2 = isClassifier && mods.get( i ).toLowerCase().contains( "humann2" );
			if( mods.get( i ).equals( module.getClass().getName() ) ) thisClass.add( i );
			else if( mods.get( i ).startsWith( "R_" ) ) otherRModules.add( i );
			else if( isHn2 ) hn2Classifiers.add( i );
			else if( isClassifier ) classifiers.add( i );
			else if( isParser ) parsers.add( i );
		}

		final boolean hasPathwayInputs = BioLockJUtil
			.pipelineInputType( BioLockJUtil.PIPELINE_HUMANN2_COUNT_TABLE_INPUT_TYPE );
		final boolean hasTaxaInputs = BioLockJUtil
			.pipelineInputType( BioLockJUtil.PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE );
		Log.debug( module.getClass(), "hasPathwayInputs: " + hasPathwayInputs );
		Log.debug( module.getClass(), "hasTaxaInputs: " + hasTaxaInputs );
		if( hn2Classifiers.isEmpty() && classifiers.isEmpty() ) {
			if( hasPathwayInputs && !hasTaxaInputs ) {
				Log.debug( module.getClass(), "No classifier module found & pipeline input contains Humann2 reports" );
				return true;
			}
			Log.debug( module.getClass(),
				"No classifier modules found & pipeline inputs do not contain Humann2 reports" );
			return false;
		}
		if( hn2Classifiers.isEmpty() ) {
			Log.debug( module.getClass(), "No HN2 classifiers configured: count tables must be taxa tables" );
			return false;
		}
		if( classifiers.isEmpty() ) {
			Log.debug( module.getClass(), "No standard classifiers configured: return( TRUE )" );
			return true;
		}

		final Integer rIndex = thisClass.get( initCount - 1 );
		final Integer hn2Index = getClosestIndex( hn2Classifiers, rIndex );
		final Integer classifierIndex = getClosestIndex( classifiers, rIndex );
		final Integer parserIndex = getClosestIndex( parsers, rIndex );
		Log.debug( module.getClass(), "rIndex: " + ( rIndex == null ? "N/A": rIndex ) );
		Log.debug( module.getClass(), "hn2Index: " + ( hn2Index == null ? "N/A": hn2Index ) );
		Log.debug( module.getClass(), "classifierIndex: " + ( classifierIndex == null ? "N/A": classifierIndex ) );
		Log.debug( module.getClass(), "parserIndex: " + ( parserIndex == null ? "N/A": parserIndex ) );

		if( hn2Index == null ) {
			Log.debug( module.getClass(), "No HN2 classifiers BEFORE R-module index so return( FALSE ): " + rIndex );
			return false;
		}
		boolean useHn2 = classifierIndex == null || classifierIndex < hn2Index;
		useHn2 = useHn2 && ( parserIndex == null || parserIndex < hn2Index );
		Log.debug( module.getClass(), "Final assessment --> use HumanN2 tables?  return( " + useHn2 + " ) " );
		return useHn2;
	}

	/**
	 * Verify the HumanN2 Config contains at least one of the following reports are enabled:<br>
	 * <ul>
	 * <li>{@value biolockj.Constants#HN2_DISABLE_GENE_FAMILIES}
	 * <li>{@value biolockj.Constants#HN2_DISABLE_PATH_COVERAGE}
	 * <li>{@value biolockj.Constants#HN2_DISABLE_GENE_FAMILIES}
	 * </ul>
	 * 
	 * @param module HumanN2 module
	 * @throws Exception if errors occur
	 */
	public static void verifyConfig( final BioModule module ) throws Exception {
		if( Config.getBoolean( module, Constants.HN2_DISABLE_PATH_ABUNDANCE )
			&& Config.getBoolean( module, Constants.HN2_DISABLE_PATH_COVERAGE )
			&& Config.getBoolean( module, Constants.HN2_DISABLE_GENE_FAMILIES ) )
			throw new ConfigViolationException(
				"Must enable at least one type of HumanN2 report.  All 3 reports are disable via Config properties: "
					+ Constants.HN2_DISABLE_PATH_ABUNDANCE + "=" + Constants.FALSE + ", "
					+ Constants.HN2_DISABLE_PATH_COVERAGE + "=" + Constants.FALSE + ", "
					+ Constants.HN2_DISABLE_GENE_FAMILIES + "=" + Constants.FALSE );
	}

	private static Integer getClosestIndex( final List<Integer> indexes, final Integer target ) {
		Integer hit = null;
		for( final Integer i: indexes )
			if( i < target ) hit = i;
		return hit;
	}

	private static Map<String, Integer> initMap = new HashMap<>();

	private static String validFormats = "[ " + Constants.HN2_PATH_ABUND_SUM + ", " + Constants.HN2_PATH_COVG_SUM + ", "
		+ Constants.HN2_GENE_FAM_SUM + " ]";
}
