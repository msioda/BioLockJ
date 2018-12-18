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

import java.io.BufferedReader;
import java.io.File;
import java.util.*;
import biolockj.BioLockJ;
import biolockj.module.BioModule;

/**
 * This utility helps work with OTU count files as formatted by the
 * {@link biolockj.module.implicit.parser.ParserModule}.
 */
public class OtuUtil
{

	public static class OtuCountLine
	{
		public OtuCountLine( final String line ) throws Exception
		{
			final StringTokenizer st = new StringTokenizer( line, BioLockJ.TAB_DELIM );
			if( st.countTokens() != 2 )
			{
				throw new Exception(
						"OTU count lines should have 2 tokens {OTU, COUNT}, #tokens found: " + st.countTokens() );
			}
			otu = st.nextToken();
			count = Integer.valueOf( st.nextToken() );
		}

		public Integer getCount()
		{
			return count;
		}

		public String getOtu()
		{
			return otu;
		}

		private Integer count = null;

		private String otu = null;
	}

	// Prevent instantiation
	private OtuUtil()
	{}

	public static String buildOtuTaxa( final String level, final String taxa ) throws Exception
	{
		return level + DELIM_SEP + taxa;
	}

	/**
	 * Compile OTU counts from an individual sample OTU count file
	 * 
	 * @param files Collection of OTU count files
	 * @return Map(OTU, count)
	 * @throws Exception if errors occur
	 */
	public static Map<String, Integer> compileSampleOtuCounts( final File file ) throws Exception
	{
		final Map<String, Integer> otuCounts = new TreeMap<>();

		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final OtuCountLine ocl = new OtuCountLine( line );
				final String otu = ocl.getOtu();
				final Integer count = ocl.getCount();

				if( !otuCounts.keySet().contains( otu ) )
				{
					otuCounts.put( otu, 0 );
				}

				otuCounts.put( otu, otuCounts.get( otu ) + count );
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		return otuCounts;
	}

	/**
	 * Find every unique OTU across all samples.
	 * 
	 * @param sampleOtuCounts Map(SampleId, Map(OTU, count)) OTU counts for every sample
	 * @return Ordered list of unique OTUs
	 * @throws Exception if errors occur
	 */
	public static List<String> findUniqueOtus( final Map<String, Map<String, Integer>> sampleOtuCounts )
			throws Exception
	{
		final TreeSet<String> otus = new TreeSet<>();
		for( final String id: sampleOtuCounts.keySet() )
		{
			final Map<String, Integer> taxaCounts = sampleOtuCounts.get( id );
			if( taxaCounts != null && !taxaCounts.isEmpty() )
			{
				otus.addAll( sampleOtuCounts.get( id ).keySet() );
			}
		}
		final List<String> returnList = new ArrayList<>( otus );
		Collections.sort( returnList );
		return returnList;
	}

	/**
	 * Extract taxonomy names at the given level from all given OTUs.
	 * 
	 * @param otus List of OTUs in {@link biolockj.module.implicit.parser.ParserModule} format
	 * @param level {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}
	 * @return Ordered list of unique taxonomy names
	 * @throws Exception
	 */
	public static List<String> findUniqueTaxa( final List<String> otus, final String level ) throws Exception
	{
		final Set<String> uniqueOtus = new HashSet<>();
		for( final String otu: otus )
		{
			final String taxa = getTaxaName( otu, level );
			if( taxa != null )
			{
				uniqueOtus.add( taxa );
			}
		}
		final List<String> returnList = new ArrayList<>( uniqueOtus );
		Collections.sort( returnList );
		return returnList;
	}

	/**
	 * Return Map keyed on Sample ID, each sample maps to an inner map(taxa, count).<br>
	 * Input param sampleOtuCounts contains OTUs for which many can share the same level taxonomy assignment.<br>
	 * For example if sample42 contained the following 3 OTU values, a phylum level report would contain the entry:
	 * Map(sample42, Map(Firmicutes, 405))
	 * <ul>
	 * <li>phylum@Firmicutes;class@Bacilli=5
	 * <li>phylum@Firmicutes;class@Clostridia=100
	 * <li>phylum@Firmicutes;class@Erysipelotrichia=300
	 * </ul>
	 * 
	 * @param sampleOtuCounts Map(SampleId, Map(OTU, count)) OTU counts for every sample
	 * @param level {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}
	 * @return Map(SampleId, Map(taxa, count))
	 * @throws Exception if errors occur
	 */
	public static TreeMap<String, Map<String, Integer>> getLevelTaxaCounts(
			final Map<String, Map<String, Integer>> sampleOtuCounts, final String level ) throws Exception
	{
		final TreeMap<String, Map<String, Integer>> taxaCounts = new TreeMap<>();

		for( final String id: sampleOtuCounts.keySet() )
		{
			taxaCounts.put( id, getTaxaCounts( sampleOtuCounts.get( id ), level ) );
		}

		return taxaCounts;
	}

	/**
	 * Extract the sampleId from the OTU count file name.<br>
	 * Input files should include a file name just before the .tsv file extension.
	 * 
	 * @param otuCountFile {@value #OTU_COUNT} file
	 * @return Sample ID
	 * @throws Exception if errors occur
	 */
	public static String getSampleId( final File otuCountFile ) throws Exception
	{
		if( otuCountFile.getName().lastIndexOf( "_" ) < 0 )
		{
			throw new Exception( "Unexpected format!  Missing \"_\" from input file name: " + otuCountFile.getName() );
		}
		return otuCountFile.getName().substring( otuCountFile.getName().lastIndexOf( OTU_COUNT + "_" ) + 9,
				otuCountFile.getName().length() - BioModule.TSV.length() );
	}

	/**
	 * Map OTU counts for each sample file formatted and named as in
	 * {@link biolockj.module.implicit.parser.ParserModule} output.
	 * 
	 * @return Map(SampleID, Map(OTU, count)) OTU counts by sample
	 * @throws Exception if errors occur
	 */
	public static Map<String, Map<String, Integer>> getSampleOtuCounts( final Collection<File> files ) throws Exception
	{
		final Map<String, Map<String, Integer>> otuCountsBySample = new TreeMap<>();
		for( final File file: files )
		{
			if( !file.getName().contains( "_" + OTU_COUNT + "_" ) )
			{
				throw new Exception( "Module input files must contain sample OTU counts with \"" + OTU_COUNT
						+ "\" as part of the file name" );
			}

			otuCountsBySample.put( getSampleId( file ), compileSampleOtuCounts( file ) );
		}

		return otuCountsBySample;
	}

	/**
	 * Build the Map(taxa, count), alphabetically ordered by taxa.<br>
	 * Extract taxonomy name for the given level from each OTU in the otuMap.keySet().<br>
	 * Assign each taxa the combined count values from each OTU belonging to the same taxa.<br>
	 * 
	 * @param otuMap Map(OTU, count)
	 * @param level {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}
	 * @return Map(taxa, count)
	 * @throws Exception if errors occur
	 */
	public static TreeMap<String, Integer> getTaxaCounts( final Map<String, Integer> otuMap, final String level )
			throws Exception
	{
		final TreeMap<String, Integer> map = new TreeMap<>();
		for( final String otu: otuMap.keySet() )
		{
			final String taxa = getTaxaName( otu, level );
			if( taxa != null )
			{
				if( !map.keySet().contains( taxa ) )
				{
					map.put( taxa, 0 );
				}
				map.put( taxa, map.get( taxa ) + otuMap.get( otu ) );
			}
		}

		return map;
	}

	/**
	 * Extract a taxonomy name at the given level from the given OTU.
	 * 
	 * @param otu OTU name in {@link biolockj.module.implicit.parser.ParserModule} format
	 * @param level {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}
	 * @return Taxonomy name
	 * @throws Exception if errors occur
	 */
	public static String getTaxaName( final String otu, final String level ) throws Exception
	{
		final StringTokenizer st = new StringTokenizer( otu, SEPARATOR );
		while( st.hasMoreTokens() )
		{
			final String levelOtu = st.nextToken();
			if( levelOtu.startsWith( level + DELIM_SEP ) )
			{
				return levelOtu.replaceFirst( level + DELIM_SEP, "" );
			}
		}
		return null;
	}

	/**
	 * In an otu string for multiple levels, each separated by {@value #SEPARATOR}, each otu has a level prefix endind
	 * with {@value #DELIM_SEP}
	 */
	public static String DELIM_SEP = "@";
	/**
	 * Included in the file name of each file output. One file per sample is output by the ParserModule.
	 */
	public static final String OTU_COUNT = "otuCount";

	/**
	 * Semi-colon is used to separate each taxa {@value #SEPARATOR}
	 */
	public static String SEPARATOR = ";";
}
