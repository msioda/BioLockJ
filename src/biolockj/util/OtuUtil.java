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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;

/**
 * This utility helps work with OTU count files as formatted by the
 * {@link biolockj.module.implicit.parser.ParserModule}.
 */
public class OtuUtil
{
	/**
	 * This inner class is used to hold a signle line from an OTU count file.
	 */
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
		return level + DELIM_SEP + BioLockJUtil.removeQuotes( taxa );
	}

	/**
	 * Compile OTU counts from an individual sample OTU count file
	 * 
	 * @param files Collection of OTU count files
	 * @return TreeMap(OTU, count)
	 * @throws Exception if errors occur
	 */
	public static TreeMap<String, Integer> compileSampleOtuCounts( final File file ) throws Exception
	{
		final TreeMap<String, Integer> otuCounts = new TreeMap<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final OtuCountLine ocl = new OtuCountLine( line );
				final String otu = ocl.getOtu();
				final Integer count = ocl.getCount();
				otuCounts.put( otu, count );
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
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @return Ordered TreeSet of unique OTUs
	 * @throws Exception if errors occur
	 */
	public static TreeSet<String> findUniqueOtus( final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts )
			throws Exception
	{
		final TreeSet<String> otus = new TreeSet<>();
		for( final String id: sampleOtuCounts.keySet() )
		{
			final TreeMap<String, Integer> taxaCounts = sampleOtuCounts.get( id );
			if( taxaCounts != null && !taxaCounts.isEmpty() )
			{
				otus.addAll( sampleOtuCounts.get( id ).keySet() );
			}
		}

		return otus;
	}

	/**
	 * Extract taxonomy names at the given level from all given OTUs.
	 * 
	 * @param otus TreeSet of OTUs in {@link biolockj.module.implicit.parser.ParserModule} format
	 * @param level {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}
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
	 * Returns a list of all taxonomy levels, not only the levels configured via
	 * {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}.
	 * 
	 * @return List<String> ordered taxonomy levels configured
	 */
	public static List<String> getAllTaxonomyLevels()
	{
		return allLevels;
	}

	public static String getBottomTaxaLevel() throws Exception
	{
		if( bottomLevel == null )
		{
			final Iterator<String> levels = Config.requireList( Config.REPORT_TAXONOMY_LEVELS ).iterator();
			while( levels.hasNext() )
			{
				bottomLevel = levels.next();
			}
		}
		return bottomLevel;
	}

	/**
	 * Return TreeMap keyed on Sample ID, each sample maps to an inner map(taxa, count).<br>
	 * Input param sampleOtuCounts contains OTUs for which many can share the same level taxonomy assignment.<br>
	 * For example if sample42 contained the following 3 OTU values, a phylum level report would contain the entry:
	 * TreeMap(sample42, TreeMap(Firmicutes, 405))
	 * <ul>
	 * <li>phylum__Firmicutes;class__Bacilli=5
	 * <li>phylum__Firmicutes;class__Clostridia=100
	 * <li>phylum__Firmicutes;class__Erysipelotrichia=300
	 * </ul>
	 * 
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @param level {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}
	 * @return TreeMap(SampleId, TreeMap(taxa, count))
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
	 * Build OTU count file using a standard format in the directory given.<br>
	 * Format: pipeline_name + prefix + {@value #OTU_COUNT} + sampleId + {@value biolockj.module.BioModule#TSV_EXT}
	 * 
	 * @param dir File directory
	 * @param sampleId Sample ID
	 * @param prefix File prefix (after pipeline name)
	 * @return OTU count file
	 * @throws Exception if errors occur
	 */
	public static File getOtuCountFile( final File dir, String sampleId, String prefix ) throws Exception
	{
		if( prefix == null )
		{
			prefix = "_";
		}
		if( !prefix.startsWith( "_" ) )
		{
			prefix = "_" + prefix;
		}
		if( !prefix.endsWith( "_" ) )
		{
			prefix += "_";
		}

		if( sampleId != null )
		{
			sampleId = "_" + sampleId;
		}
		else
		{
			sampleId = "";
		}

		return new File( dir.getAbsolutePath() + File.separator + Config.requireString( Config.INTERNAL_PIPELINE_NAME )
				+ prefix + OTU_COUNT + sampleId + BioLockJ.TSV_EXT );
	}

	public static String getParentTaxaLevel( final String level ) throws Exception
	{
		boolean foundValid = false;
		String parent = null;
		for( final String testLevel: getAllTaxonomyLevels() )
		{
			if( Config.requireList( Config.REPORT_TAXONOMY_LEVELS ).contains( level ) )
			{
				foundValid = true;
			}
			if( foundValid && testLevel.equals( level ) )
			{
				return parent;
			}
			else
			{
				parent = testLevel;
			}
		}
		return null;
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
				otuCountFile.getName().length() - BioLockJ.TSV_EXT.length() );
	}

	/**
	 * TreeMap OTU counts for each sample file formatted and named as in
	 * {@link biolockj.module.implicit.parser.ParserModule} output.
	 * 
	 * @return TreeMap(SampleID, TreeMap(OTU, count)) OTU counts by sample
	 * @throws Exception if errors occur
	 */
	public static TreeMap<String, TreeMap<String, Integer>> getSampleOtuCounts( final Collection<File> files )
			throws Exception
	{
		final TreeMap<String, TreeMap<String, Integer>> otuCountsBySample = new TreeMap<>();
		for( final File file: files )
		{
			if( !file.getName().contains( "_" + OTU_COUNT + "_" ) )
			{
				throw new Exception( "Module input files must contain sample OTU counts with \"_" + OTU_COUNT
						+ "_\" as part of the file name.  Found file: " + file.getAbsolutePath() );
			}

			otuCountsBySample.put( getSampleId( file ), compileSampleOtuCounts( file ) );
		}

		return otuCountsBySample;
	}

	/**
	 * Return a map of the OTU split by level.
	 * 
	 * @param otu OTU with multiple taxa levels
	 * @return TreeMap(level,taxa)
	 * @throws Exception if errors occur
	 */
	public static TreeMap<String, String> getTaxaByLevel( final String otu ) throws Exception
	{
		final TreeMap<String, String> map = new TreeMap<>();
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			final String name = getTaxaName( otu, level );
			if( name != null )
			{
				map.put( level, getTaxaName( otu, level ) );
			}
		}
		return map;
	}

	public static List<String> getTaxaLevelSpan() throws Exception
	{
		if( levelSpan != null )
		{
			return levelSpan;
		}

		levelSpan = new ArrayList<>();
		for( final String level: getAllTaxonomyLevels() )
		{
			if( !levelSpan.isEmpty() || level.equals( getTopTaxaLevel() ) )
			{
				levelSpan.add( level );
			}

			if( level.equals( getBottomTaxaLevel() ) )
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

		return new File( dir.getAbsolutePath() + File.separator + Config.requireString( Config.INTERNAL_PIPELINE_NAME )
				+ "_" + TAXA_TABLE + suffix + level + BioLockJ.TSV_EXT );
	}

	/**
	 * Extract the level from an existing taxonomy table.
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static String getTaxonomyTableLevel( final File file ) throws Exception
	{
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			if( file.getName().endsWith( level + BioLockJ.TSV_EXT ) )
			{
				return level;
			}
		}
		return null;
	}

	public static String getTopTaxaLevel() throws Exception
	{
		if( topLevel == null )
		{
			topLevel = Config.requireList( Config.REPORT_TAXONOMY_LEVELS ).get( 0 );
		}
		return topLevel;
	}

	/**
	 * Build the name of an unclassified taxa using the given parentTaxa
	 * 
	 * @param parentTaxa Name of parent taxa
	 * @return Unclassified OTU name
	 */
	public static String getUnclassifiedTaxa( final String parentTaxa )
	{
		return UNCLASSIFIED + " " + parentTaxa + " " + OTU;
	}

	/**
	 * Check the file name and contents to verify file is an OTU count file.
	 * 
	 * @param file
	 * @return boolean
	 * @throws Exception if errors occur
	 */
	public static boolean isOtuFile( final File file ) throws Exception
	{
		final String name = file.getName();
		if( name.startsWith( Config.requireString( Config.INTERNAL_PIPELINE_NAME ) )
				&& name.endsWith( BioLockJ.TSV_EXT ) && name.contains( "_" + OTU_COUNT + "_" ) )
		{
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					new OtuCountLine( line );
				}
			}
			catch( final Exception ex )
			{
				Log.debug( OtuUtil.class, "File is not an OTU count file: " + file.getAbsolutePath() );
				return false;
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * Check the file name to determine if it is a taxonomy table file.
	 * 
	 * @param file
	 * @return boolean
	 * @throws Exception if errors occur
	 */
	public static boolean isTaxonomyTable( final File file ) throws Exception
	{
		if( file.getName()
				.startsWith( Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + "_" + TAXA_TABLE + "_" ) )
		{
			for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
			{
				if( file.getName().endsWith( level + BioLockJ.TSV_EXT ) )
				{
					return true;
				}
			}
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
	public static boolean outputHasOtuCountFiles( final BioModule module ) throws Exception
	{
		final Collection<File> files = FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE,
				HiddenFileFilter.VISIBLE );

		if( files == null || files.isEmpty() )
		{
			throw new Exception( module.getClass().getSimpleName() + " has no output!" );
		}

		for( final File f: files )
		{
			if( !Config.getTreeSet( Config.INPUT_IGNORE_FILES ).contains( f.getName() ) )
			{
				return OtuUtil.isOtuFile( f );
			}
		}

		return false;
	}

	/**
	 * Check the module to determine if it generated taxonomy table files.
	 * 
	 * @param module BioModule
	 * @return TRUE if module generated taxonomy table files
	 * @throws Exception if errors occur
	 */
	public static boolean outputHasTaxonomyTables( final BioModule module ) throws Exception
	{
		final Collection<File> files = FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE,
				HiddenFileFilter.VISIBLE );

		if( files == null || files.isEmpty() )
		{
			throw new Exception( module.getClass().getSimpleName() + " has no output!" );
		}

		for( final File f: files )
		{
			if( !Config.getTreeSet( Config.INPUT_IGNORE_FILES ).contains( f.getName() ) )
			{
				return OtuUtil.isTaxonomyTable( f );
			}
		}

		return false;
	}

	/**
	 * In an otu string for multiple levels, each separated by {@value #SEPARATOR}, each otu has a level prefix ending
	 * with {@value #DELIM_SEP}
	 */
	public static final String DELIM_SEP = "__";

	/**
	 * Included in the file name of each file output. One file per sample is output by the ParserModule.
	 */
	public static final String OTU_COUNT = "otuCount";

	/**
	 * Semi-colon is used to separate each taxa {@value #SEPARATOR}
	 */
	public static final String SEPARATOR = "|";

	/**
	 * Included in the file name of each file output. One file per sample is output by the ParserModule.
	 */
	public static final String TAXA_TABLE = "taxaCount";

	private static final List<String> allLevels = Arrays.asList( new String[] { Config.DOMAIN, Config.PHYLUM,
			Config.CLASS, Config.ORDER, Config.FAMILY, Config.GENUS, Config.SPECIES } );

	private static String bottomLevel = null;

	private static List<String> levelSpan = null;
	private static final String OTU = "OTU";

	private static String topLevel = null;
	private static final String UNCLASSIFIED = "Unclassified";
}
