/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.parser;

import java.io.*;
import java.util.*;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.JavaModuleImpl;
import biolockj.node.OtuNode;
import biolockj.node.ParsedSample;
import biolockj.util.BioLockJUtil;
import biolockj.util.MemoryUtil;
import biolockj.util.MetaUtil;

/**
 * Parser {@link biolockj.module.BioModule}s read {@link biolockj.module.classifier.ClassifierModule} output to build
 * standardized OTU abundance tables. This class provides the default abstract implementation.
 */
public abstract class ParserModuleImpl extends JavaModuleImpl implements ParserModule
{
	@Override
	public void addParsedSample( final ParsedSample newSample ) throws Exception
	{
		for( final ParsedSample sample: parsedSamples )
		{
			if( sample.getSampleId().equals( newSample.getSampleId() ) )
			{
				throw new Exception( "Attempt to add duplicate sample! " + sample.getSampleId() );
			}
		}

		parsedSamples.add( newSample );
	}

	/**
	 * Method executes 2 core OTU table building functions:
	 * <ul>
	 * <li>{@link #createTaxaSparseThreeColFiles()}
	 * <li>{@link #buildRawCountTables(List)}
	 * </ul>
	 */
	@Override
	public void buildOtuTables() throws Exception
	{
		buildRawCountTables( createTaxaSparseThreeColFiles() );
	}

	/**
	 * Validate module dependencies:
	 * <ul>
	 * <li>Verify {@link biolockj.Config}.{@value #REPORT_MINIMUM_OTU_COUNT} is a non-negative integer (if defined)
	 * <li>Verify {@link biolockj.Config}.{@value #REPORT_MINIMUM_OTU_THRESHOLD} is a non-negative integer (if defined)
	 * <li>Execute {@link #validateModuleOrder()} to validate module configuration order.
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.getNonNegativeInteger( REPORT_MINIMUM_OTU_COUNT );
		Config.getNonNegativeInteger( REPORT_MINIMUM_OTU_THRESHOLD );
		validateModuleOrder();
	}

	/**
	 * If restarting a pipeline, the set of parsedSamples will be empty.<br>
	 * If this is the case, call {@link #parseSamples()}.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		if( parsedSamples.isEmpty() )
		{
			parseSamples();
		}
	}

	@Override
	public ParsedSample getParsedSample( final String sampleId )
	{
		for( final ParsedSample sample: parsedSamples )
		{
			if( sample.getSampleId().equals( sampleId ) )
			{
				return sample;
			}
		}
		return null;
	}

	@Override
	public TreeSet<ParsedSample> getParsedSamples()
	{
		return parsedSamples;
	}

	@Override
	public abstract void parseSamples() throws Exception;

	/**
	 * Parsers execute a task with 3 core functions:
	 * <ol>
	 * <li>{@link #parseSamples()} - generates {@link biolockj.node.ParsedSample}s
	 * <li>{@link #buildOtuCounts()} - stores internal summary counts for the {@link biolockj.node.ParsedSample}s
	 * <li>{@link #registerNumHits()} (if {@link biolockj.Config}.{@value biolockj.Config#REPORT_NUM_HITS} =
	 * {@value biolockj.Config#TRUE})
	 * <li>{@link #buildOtuTables()} - builds output tables
	 * </ol>
	 */
	@Override
	public void runModule() throws Exception
	{
		MemoryUtil.reportMemoryUsage( "About to parse samples" );
		parseSamples();

		Log.debug( getClass(), "# Samples parsed: " + parsedSamples.size() );

		if( parsedSamples.isEmpty() )
		{
			throw new Exception( "Parser failed to produce output!" );
		}

		// MemoryUtil.reportMemoryUsage( "About to build OTU-counts" );
		// buildOtuCounts();
		// MemoryUtil.reportMemoryUsage( "Build OTU-counts complete!" );

		if( Config.getBoolean( Config.REPORT_NUM_HITS ) )
		{
			registerNumHits();
		}

		buildOtuTables();
	}

	// /**
	// * Build OTU counts after all classifier output has been parsed into OtuNode objects.
	// */
	// protected void buildOtuCounts()
	// {
	// for( final ParsedSample sample: parsedSamples )
	// {
	// sample.buildOtuCounts();
	// }
	// }

	/**
	 * This method reads the temp 3-col-table to build the raw count table populated with raw OTU counts for OTUs that
	 * meet the threshold value: {@link biolockj.Config}.{@value #REPORT_MINIMUM_OTU_COUNT}
	 *
	 * @param files Sparse 3-col tables
	 * @throws Exception if unable to build the basic OTU table
	 */
	protected void buildRawCountTables( final List<File> files ) throws Exception
	{
		for( final File file: files )
		{
			final Map<String, Map<String, Integer>> sparse3colTableMap = getSparseTableMap( file );
			final File otuTable = new File( getOutputDir().getAbsolutePath() + File.separator + file.getName() );

			final BufferedWriter writer = new BufferedWriter( new FileWriter( otuTable ) );
			Log.info( getClass(), "Building: " + otuTable );
			writer.write( MetaUtil.getID() );
			final List<String> otuList = getOTUSAtThreshold( sparse3colTableMap );
			Collections.sort( otuList );
			for( final String s: otuList )
			{
				writer.write( TAB_DELIM + s.replaceAll( "'", "" ).replaceAll( "\"", "" ) );
			}

			writer.write( RETURN );
			final List<String> samples = new ArrayList<>();
			for( final String sample: sparse3colTableMap.keySet() )
			{
				samples.add( sample );
			}

			Collections.sort( samples );
			int sampleCount = 0;

			for( final String sample: samples )
			{
				writer.write( getOtuTableRowId( sample ) );
				for( final String otu: otuList )
				{
					Integer count = sparse3colTableMap.get( sample ).get( otu );
					if( count == null )
					{
						count = 0;
					}
					writer.write( TAB_DELIM + count );
				}

				if( ++sampleCount != samples.size() )
				{
					writer.write( RETURN );
				}
			}

			writer.write( RETURN );
			writer.close();
		}
	}

	/**
	 * Use {@link biolockj.node.ParsedSample}s to generate the temp 3-col OTU tables for each
	 * {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}
	 *
	 * @return List of "sparse-3-col" tables
	 * @throws Exception if unable to build "sparse-3-col" tables
	 */
	protected List<File> createTaxaSparseThreeColFiles() throws Exception
	{
		final List<File> files = new ArrayList<>();

		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			final File spTable = new File( getTempDir().getAbsolutePath() + File.separator + level + EXT );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( spTable ) );
			Log.info( getClass(), "Building: " + spTable.getAbsolutePath() );
			for( final ParsedSample sample: parsedSamples )
			{
				final TreeMap<String, Long> levelCounts = sample.getOtuCountMap().get( level );
				if( levelCounts != null )
				{
					for( final String otu: levelCounts.keySet() )
					{
						Log.debug( getClass(),
								"sample ID = " + ( sample.getSampleId() == null ? "null": sample.getSampleId() ) );

						Log.debug( getClass(), "otu = " + ( otu == null ? "null": otu ) );

						Log.debug( getClass(), "levelCounts.get( otu )  = "
								+ ( levelCounts.get( otu ) == null ? "null": levelCounts.get( otu ) ) );

						writer.write(
								sample.getSampleId() + TAB_DELIM + otu + TAB_DELIM + levelCounts.get( otu ) + RETURN );
					}
				}
				else
				{
					Log.warn( getClass(), sample.getSampleId() + " has 0 OTUs for level: " + level );
					sample.report( true );
				}
			}
			writer.close();
			files.add( spTable );
		}

		return files;
	}

	/**
	 * This method is used to get an R-friendly OTU table row value by stripping out the quotes.
	 *
	 * @param id Sample ID
	 * @return id with quotes removed
	 * @throws Exception If id value is null
	 */
	protected String getOtuTableRowId( final String id ) throws Exception
	{
		return id.replaceAll( "'", "" ).replaceAll( "\"", "" );
	}

	/**
	 * Some {@link biolockj.module.classifier.ClassifierModule}s can include taxonomy level identifiers without an OTU
	 * name in the sample report files. This method verifies the node exists, has a valid sample ID, and that no emptry
	 * String OTU names are reported.
	 *
	 * @param node OtuNode build from 1 line of a {@link biolockj.module.classifier.ClassifierModule} output file
	 * @return boolean if {@link biolockj.node.OtuNode} is valid
	 */
	protected boolean isValid( final OtuNode node )
	{
		if( node != null && node.getSampleId() != null && node.getCount() != null && !node.getOtuMap().isEmpty() )
		{
			for( final String level: node.getOtuMap().keySet() )
			{
				final String otu = node.getOtuMap().get( level );
				if( Config.getList( Config.REPORT_TAXONOMY_LEVELS ).contains( level ) && otu != null
						&& !otu.trim().isEmpty() )
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Total {@link biolockj.node.ParsedSample} OTU counts to populate metadata {@value #NUM_HITS}
	 *
	 * @throws Exception if unable add {@value #NUM_HITS} to the metadata file
	 */
	protected void registerNumHits() throws Exception
	{
		final Map<String, String> hitsPerSample = new HashMap<>();
		Log.info( getClass(), "Counting # hits/sample for " + getParsedSamples().size() + " files" );
		for( final ParsedSample sample: getParsedSamples() )
		{
			hitsPerSample.put( sample.getSampleId(), Long.toString( sample.getNumHits() ).toString() );
		}

		MetaUtil.addColumn( NUM_HITS, hitsPerSample, getOutputDir() );
	}

	/**
	 * Validate {@link biolockj.module.implicit.parser} modules run after {@link biolockj.module.classifier} and
	 * {@link biolockj.module.seq} modules.
	 * 
	 * @throws Exception if modules are out of order
	 */
	protected void validateModuleOrder() throws Exception
	{
		boolean found = false;
		for( final BioModule module: Pipeline.getModules() )
		{
			if( found )
			{
				if( module.getClass().getName().startsWith( "biolockj.module.seq" )
						|| module.getClass().getName().startsWith( "biolockj.module.classifier" ) )
				{
					throw new Exception( "Invalid BioModule configuration order! " + module.getClass().getName()
							+ " must run before any " + getClass().getPackage().getName() + " BioModule." );
				}
			}
			else if( module.getClass().equals( getClass() ) )
			{
				found = true;
			}
		}
	}

	private int getOtuCountIfAboveMinThreshold( final String sample, final String otu, Integer val ) throws Exception
	{
		if( val == null )
		{
			val = 0;
		}

		final Integer minOtu = Config.getNonNegativeInteger( REPORT_MINIMUM_OTU_COUNT );
		if( val != 0 && minOtu != null && minOtu > 0 && val < minOtu )
		{
			Log.warn( getClass(),
					"Reporting 0 instead of " + val + " since count < " + REPORT_MINIMUM_OTU_COUNT + "=" + minOtu + "["
							+ ( sample == null ? "": " Sample:" + sample + " | " ) + "OTU:" + otu + " | count:" + val
							+ "]" );
			val = 0;
		}

		return val;
	}

	/**
	 * Return OTU counts for OTUs with a total count that meets or exceeds the
	 * {@link biolockj.Config}.{@value #REPORT_MINIMUM_OTU_THRESHOLD}
	 *
	 * @param map Map linking sampleID to OTU counts
	 * @return List of OTUs with counts greater than {@link biolockj.Config}.{@value #REPORT_MINIMUM_OTU_THRESHOLD}
	 */
	private List<String> getOTUSAtThreshold( final Map<String, Map<String, Integer>> map ) throws Exception
	{
		final Map<String, Integer> countMap = new HashMap<>();
		for( final String s: map.keySet() )
		{
			final Map<String, Integer> innerMap = map.get( s );
			for( final String otu: innerMap.keySet() )
			{
				Integer oldCount = countMap.get( otu );
				if( oldCount == null )
				{
					oldCount = 0;
				}

				oldCount += innerMap.get( otu );
				countMap.put( otu, oldCount );
			}
		}
		final Integer otuThreshold = Config.getNonNegativeInteger( REPORT_MINIMUM_OTU_THRESHOLD );

		final List<String> otuList = new ArrayList<>();
		for( final String otu: countMap.keySet() )
		{
			if( otuThreshold == null || otuThreshold < 1 || countMap.get( otu ) >= otuThreshold )
			{
				otuList.add( otu );
			}
			else
			{
				Log.info( getClass(), "Skipping OTU.  Count < " + REPORT_MINIMUM_OTU_THRESHOLD + " [OTU:" + otu
						+ "| count:" + countMap.get( otu ) + "]" );
			}
		}

		return otuList;

	}

	/**
	 * Read the temp 3-col-table files to build a map for each sample: key=SampleID, with value = Map<taxa, count>
	 *
	 * @param sparseFile Temp 3-col OTU table
	 * @return Map linking sample ID to OTU counts
	 * @throws Exception if unable to build map
	 */
	private Map<String, Map<String, Integer>> getSparseTableMap( final File sparseFile ) throws Exception
	{
		final Map<String, Map<String, Integer>> map = new HashMap<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( sparseFile );
		String nextLine = null;
		try
		{
			nextLine = reader.readLine();
			while( nextLine != null && nextLine.trim().length() > 0 )
			{
				final StringTokenizer st = new StringTokenizer( nextLine, TAB_DELIM );
				final String sample = st.nextToken();
				final String taxa = st.nextToken();
				final String next = st.nextToken();
				final int count = Integer.parseInt( next );
				Map<String, Integer> innerMap = map.get( sample );
				if( innerMap == null )
				{
					innerMap = new HashMap<>();
					map.put( sample, innerMap );
				}

				if( innerMap.containsKey( taxa ) )
				{
					throw new Exception( "Duplicate OTU: " + sample + ": " + taxa + ":" + next );
				}

				innerMap.put( taxa, count );
				nextLine = reader.readLine();
			}
		}
		catch( final Exception ex )
		{
			Log.warn( getClass(), "BAD LINE: " + nextLine );
			ex.printStackTrace();
			throw new Exception( "Error occurred processing (" + sparseFile.getAbsolutePath() + "): " + ex.getMessage(),
					ex );
		}
		finally
		{
			reader.close();
		}

		return removeAllZeroOtus( map );
	}

	private Map<String, Map<String, Integer>> removeAllZeroOtus( final Map<String, Map<String, Integer>> map )
			throws Exception
	{
		Log.debug( getClass(), "Calling removeAllZeroOtus..." );
		// key = taxa, val=count
		final Map<String, Integer> otuTotalCounts = new HashMap<>();

		for( final String sample: map.keySet() )
		{
			final Map<String, Integer> otuCounts = map.get( sample );
			for( final String otu: otuCounts.keySet() )
			{
				Log.debug( getClass(), "Check sample: " + sample + " = " + map.get( sample ).get( otu ) );
				if( otuTotalCounts.get( otu ) == null )
				{
					otuTotalCounts.put( otu, 0 );
				}

				otuTotalCounts.put( otu, otuTotalCounts.get( otu )
						+ getOtuCountIfAboveMinThreshold( sample, otu, otuCounts.get( otu ) ) );
			}
		}

		final List<String> otusToRemove = new ArrayList<>();
		for( final String otu: otuTotalCounts.keySet() )
		{
			if( otuTotalCounts.get( otu ) == 0 )
			{
				otusToRemove.add( otu );
				Log.warn( getClass(), "Remove OTU [" + otu + "] since it is below minimum threshold: "
						+ Config.getNonNegativeInteger( REPORT_MINIMUM_OTU_COUNT ) );
			}
		}

		for( final String otu: otusToRemove )
		{
			for( final String sample: map.keySet() )
			{
				final Map<String, Integer> sampleOtuCounts = map.get( sample );

				if( sampleOtuCounts.keySet().contains( otu ) )
				{
					map.get( sample ).remove( otu );
					Log.warn( getClass(), "Removing OTU [" + otu + "]" );
				}
			}
		}

		return map;
	}

	/**
	 * Metadata column name for column that holds number of OTU hits after any {@link biolockj.module.implicit.parser}
	 * module executes: {@value #NUM_HITS}
	 */
	public static final String NUM_HITS = "Num_Hits";

	/**
	 * {@link biolockj.Config} property {@value #REPORT_MINIMUM_OTU_COUNT} defines the minimum OTU count that will be
	 * added to the raw count table. A sample will only include an OTU if the count meets or exceeds this value.
	 */
	protected static final String REPORT_MINIMUM_OTU_COUNT = "report.minOtuCount";

	/**
	 * {@link biolockj.Config} property {@value #REPORT_MINIMUM_OTU_THRESHOLD} defines the total across all samples an
	 * OTU must reach to be included in the raw count table. OTUs that do not reach the threshold, will be omitted.
	 */
	protected static final String REPORT_MINIMUM_OTU_THRESHOLD = "report.minOtuThreshold";

	private static final String EXT = ".tsv";
	private static final TreeSet<ParsedSample> parsedSamples = new TreeSet<>();

}
