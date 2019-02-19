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
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;

/**
 * This utility helps work individual Taxa names, not full OTU path files which need {@link biolockj.util.OtuUtil}.<br>
 * Taxa files containing sample taxa counts for a given taxonomy level as output by
 * {@link biolockj.module.report.taxa.BuildTaxaTables}.
 */
public class TaxaUtil
{
	// Prevent instantiation
	private TaxaUtil()
	{}

	/**
	 * Returns a list of all taxonomy levels, not only the levels configured via
	 * {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}.
	 * 
	 * @return Ordered List of all possible taxonomy levels
	 */
	public static List<String> allTaxonomyLevels()
	{
		return allLevels;
	}

	/**
	 * Return the bottom configured taxonomy level from
	 * {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * 
	 * @return Taxonomy level
	 * @throws Exception if errors occur
	 */
	public static String bottomTaxaLevel() throws Exception
	{
		if( bottomLevel == null )
		{
			bottomLevel = getTaxaLevels().get( getTaxaLevels().size() - 1 );
		}
		return bottomLevel;
	}

	/**
	 * Build the name of an unclassified taxa using the given "taxa" parameter.<br>
	 * Returns a name like Unclassified (taxa name) Taxa
	 * 
	 * @param taxa Taxa name
	 * @return Unclassified Taxa name
	 */
	public static String buildUnclassifiedTaxa( final String taxa )
	{
		return UNCLASSIFIED + " " + taxa + " " + TAXA;
	}

	/**
	 * Extract taxonomy names at the given level from all given OTUs.
	 * 
	 * @param otus TreeSet of OTUs in {@link biolockj.module.implicit.parser.ParserModule} format
	 * @param level {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * @return Ordered TreeSet of unique taxonomy names
	 * @throws Exception if errors occur
	 */
	public static TreeSet<String> findUniqueTaxa( final TreeSet<String> otus, final String level ) throws Exception
	{
		final TreeSet<String> uniqueTaxa = new TreeSet<>();
		for( final String otu: otus )
		{
			final String taxa = getTaxaName( otu, level );
			if( taxa != null )
			{
				uniqueTaxa.add( taxa );
			}
		}
		return uniqueTaxa;
	}

	/**
	 * Return TreeMap keyed on Sample ID, each sample maps to an inner map(taxa, count).<br>
	 * Input param sampleOtuCounts contains OTUs for which many can share the same level taxonomy assignment.<br>
	 * For example if sample42 contained the following 3 OTU values:
	 * <ul>
	 * <li>phylum__Actinobacteria|class__Actinobacteria|order__Actinomycetales|family__Actinomycetaceae|genus__Actinomyces
	 * 1000
	 * <li>phylum__Bacteroidetes|class__Bacteroidia|order__Bacteroidales|family__Porphyromonadaceae|genus__Barnesiella
	 * 500
	 * <li>phylum__Bacteroidetes|class__Bacteroidia|order__Bacteroidales|family__Porphyromonadaceae|genus__Parabacteroides
	 * 77
	 * </ul>
	 * The phylum level output would contain entries:
	 * <ol>
	 * <li>TreeMap(sample42, TreeMap( Actinobacteria=1000, Bacteroidetes=577 ) )
	 * </ol>
	 * 
	 * @param sampleOtuCounts TreeMap(sampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @param level {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * @return TreeMap(sampleId, TreeMap(taxa, count))
	 * @throws Exception if errors occur
	 */
	public static TreeMap<String, TreeMap<String, Integer>> getLevelTaxaCounts(
			final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts, final String level ) throws Exception
	{
		final TreeMap<String, TreeMap<String, Integer>> taxaCounts = new TreeMap<>();

		for( final String sampleId: sampleOtuCounts.keySet() )
		{
			final TreeMap<String, Integer> otuCounts = sampleOtuCounts.get( sampleId );
			for( final String otu: otuCounts.keySet() )
			{
				final String taxa = getTaxaName( otu, level );
				if( taxa != null )
				{
					if( taxaCounts.get( sampleId ) == null )
					{
						taxaCounts.put( sampleId, new TreeMap<>() );
					}
					if( taxaCounts.get( sampleId ).get( taxa ) == null )
					{
						taxaCounts.get( sampleId ).put( taxa, 0 );
					}
					taxaCounts.get( sampleId ).put( taxa,
							taxaCounts.get( sampleId ).get( taxa ) + otuCounts.get( otu ) );
				}
			}
		}

		return taxaCounts;
	}

	/**
	 * Return a map of the given otu parameter split by level.
	 * 
	 * @param otu OTU path value
	 * @return TreeMap(level, taxa)
	 * @throws Exception if errors occur
	 */
	public static TreeMap<String, String> getTaxaByLevel( final String otu ) throws Exception
	{
		final TreeMap<String, String> map = new TreeMap<>();
		for( final String level: getTaxaLevels() )
		{
			final String name = getTaxaName( otu, level );
			if( name != null )
			{
				map.put( level, getTaxaName( otu, level ) );
			}
		}
		return map;
	}

	/**
	 * Method ensures taxonomy levels are returned in descending order from top to bottom.
	 * 
	 * @return configLevels
	 */
	public static List<String> getTaxaLevels()
	{
		return configLevels;
	}

	/**
	 * Return taxa levels from top to bottom level, including in-between levels not configured as part of
	 * {@value Constants#REPORT_TAXONOMY_LEVELS}
	 * 
	 * @return List of taxonomy levels
	 * @throws Exception if errors occur
	 */
	public static List<String> getTaxaLevelSpan() throws Exception
	{
		if( levelSpan != null )
		{
			return levelSpan;
		}

		levelSpan = new ArrayList<>();
		for( final String level: allTaxonomyLevels() )
		{
			if( !levelSpan.isEmpty() || level.equals( topTaxaLevel() ) )
			{
				levelSpan.add( level );
			}

			if( level.equals( bottomTaxaLevel() ) )
			{
				break;
			}
		}
		return levelSpan;
	}

	/**
	 * Extract a taxonomy name at the given level from the given OTU.
	 * 
	 * @param otu OTU name in {@link biolockj.module.implicit.parser.ParserModule} format
	 * @param level {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * @return Taxonomy name
	 * @throws Exception if errors occur
	 */
	public static String getTaxaName( final String otu, final String level ) throws Exception
	{
		final StringTokenizer st = new StringTokenizer( otu, Constants.SEPARATOR );
		while( st.hasMoreTokens() )
		{
			final String levelOtu = st.nextToken();
			if( levelOtu.startsWith( level + Constants.DELIM_SEP ) )
			{
				return levelOtu.replaceFirst( level + Constants.DELIM_SEP, "" );
			}
		}
		return null;
	}

	/**
	 * Create File object of a taxonomy table at the given level, with the given suffix, in the given directory dir.
	 * 
	 * @param dir Target directory
	 * @param level Taxonomy level
	 * @param suffix File suffix
	 * @return Taxonomy Table File
	 * @throws Exception if errors occur
	 */
	public static File getTaxonomyTableFile( final File dir, final String level, String suffix ) throws Exception
	{
		if( level == null )
		{
			throw new Exception( "Level is required to build a taonomy table" );
		}

		if( suffix != null && !suffix.endsWith( "_" ) )
		{
			suffix += "_";
		}
		if( suffix != null && !suffix.startsWith( "_" ) )
		{
			suffix = "_" + suffix;
		}
		if( suffix == null )
		{
			suffix = "_";
		}

		return new File( dir.getAbsolutePath() + File.separator + Config.pipelineName() + "_" + TAXA_TABLE + suffix
				+ level + Constants.TSV_EXT );
	}

	/**
	 * Extract the table taxonomy level from an existing taxonomy table file.
	 * 
	 * @param file File Taxonomy Table
	 * @return Taxonomy level
	 * @throws Exception if errors occur
	 */
	public static String getTaxonomyTableLevel( final File file ) throws Exception
	{
		for( final String level: getTaxaLevels() )
		{
			if( file.getName().endsWith( level + Constants.TSV_EXT ) )
			{
				return level;
			}
		}
		return null;
	}

	/**
	 * Set taxonomy levels ordered by level, from highest to lowest. Require {@value Constants#REPORT_TAXONOMY_LEVELS} property.
	 * Accepts only valid options: {@value Constants#DOMAIN}, {@value Constants#PHYLUM}, {@value Constants#CLASS}, {@value Constants#ORDER},
	 * {@value Constants#FAMILY}, {@value Constants#GENUS}, {@value Constants#SPECIES}
	 *
	 * @return List of ordered taxonomy levels
	 * @throws ConfigNotFoundException if {@value Constants#REPORT_TAXONOMY_LEVELS} is undefined
	 * @throws ConfigFormatException if {@value Constants#REPORT_TAXONOMY_LEVELS} is defined, but does not contain any valid
	 * taxonomy levels
	 */
	public static List<String> initTaxaLevels() throws ConfigNotFoundException, ConfigFormatException
	{
		configLevels = new ArrayList<>();
		final String errorMsg = "Property only accepts valid taxonomy levels ==>  "
				+ BioLockJUtil.getCollectionAsString( allLevels );
		final Set<String> configuredLevels = new HashSet<>();
		final List<String> validOptions = allTaxonomyLevels();

		for( final String element: Config.requireList( null, Constants.REPORT_TAXONOMY_LEVELS ) )
		{
			if( validOptions.contains( element.toLowerCase() ) )
			{
				configuredLevels.add( element.toLowerCase() );
			}
			else
			{
				throw new ConfigFormatException( Constants.REPORT_TAXONOMY_LEVELS,
						"Invalid level defined [" + element + "]  " + errorMsg );
			}
		}

		if( configuredLevels.contains( Constants.DOMAIN ) )
		{
			configLevels.add( Constants.DOMAIN );
		}
		if( configuredLevels.contains( Constants.PHYLUM ) )
		{
			configLevels.add( Constants.PHYLUM );
		}
		if( configuredLevels.contains( Constants.CLASS ) )
		{
			configLevels.add( Constants.CLASS );
		}
		if( configuredLevels.contains( Constants.ORDER ) )
		{
			configLevels.add( Constants.ORDER );
		}
		if( configuredLevels.contains( Constants.FAMILY ) )
		{
			configLevels.add( Constants.FAMILY );
		}
		if( configuredLevels.contains( Constants.GENUS ) )
		{
			configLevels.add( Constants.GENUS );
		}
		if( configuredLevels.contains( Constants.SPECIES ) )
		{
			configLevels.add( Constants.SPECIES );
		}

		if( configLevels.isEmpty() )
		{
			throw new ConfigFormatException( Constants.REPORT_TAXONOMY_LEVELS, "No valid options configured.  " + errorMsg );
		}

		Config.setConfigProperty( Constants.REPORT_TAXONOMY_LEVELS, configLevels );
		return configLevels;
	}

	/**
	 * Check the file name to determine if it is a taxonomy table file.
	 * 
	 * @param file File to test
	 * @return boolean TRUE if file is a taxonomy count file
	 * @throws Exception if errors occur
	 */
	public static boolean isTaxaFile( final File file ) throws Exception
	{
		if( file.getName().contains( "_" + TAXA_TABLE + "_" ) )
		{
			for( final String level: getTaxaLevels() )
			{
				if( file.getName().endsWith( level + Constants.TSV_EXT ) )
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get parent level name of the given level parameter.
	 * 
	 * @param level Taxonomy level
	 * @return Parent taxonomy level
	 * @throws Exception if errors occur
	 */
	public static String parentTaxaLevel( final String level ) throws Exception
	{
		final int x = allTaxonomyLevels().indexOf( level );
		if( x == 0 )
		{
			Log.debug( TaxaUtil.class, level + " is already the highest taxonomy level so has no parent" );
			return null;
		}
		return allTaxonomyLevels().get( x - 1 );

	}

	/**
	 * Return the top configured taxonomy level from
	 * {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * 
	 * @return Taxonomy level
	 * @throws Exception if errors occur
	 */
	public static String topTaxaLevel() throws Exception
	{
		if( topLevel == null )
		{
			topLevel = getTaxaLevels().get( 0 );
		}
		return topLevel;
	}

	/**
	 * Included in the file name of each file output. One file per sample is output by the ParserModule.
	 */
	protected static final String TAXA_TABLE = "taxaCount";

	private static final List<String> allLevels = Arrays
			.asList( new String[] { Constants.DOMAIN, Constants.PHYLUM, Constants.CLASS, Constants.ORDER,
					Constants.FAMILY, Constants.GENUS, Constants.SPECIES } );

	private static String bottomLevel = null;
	private static List<String> configLevels = null;
	private static List<String> levelSpan = null;
	private static final String TAXA = "Taxa";

	private static String topLevel = null;
	private static final String UNCLASSIFIED = "Unclassified";
}
