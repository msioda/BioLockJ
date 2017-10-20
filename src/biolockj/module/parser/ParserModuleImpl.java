/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
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
package biolockj.module.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModuleImpl;
import biolockj.module.classifier.ClassifierModule;
import biolockj.node.OtuNode;
import biolockj.util.DataNormalizer;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.SeqUtil;

/**
 * The ParserModule is used output standardized tables for any classifier.
 * hitsPerSample is populated if report.numHits=Y
 * minNumHits from prop file is used to ignore any taxa lower than this threshold number
 */
public abstract class ParserModuleImpl extends BioModuleImpl implements ParserModule
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

	@Override
	public void buildOtuCountTables() throws Exception
	{
		createTaxaSparseThreeColFiles();
		createOtuCountTables();

		if( MetaUtil.exists() )
		{
			SeqUtil.registerNumHitsPerSample( parsedSamples, getOutputDir() );
			appendMetaToTaxaCountTables();
		}
	}

	/**
	 * Validate dependencies
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		Config.requirePositiveInteger( REPORT_MINIMUM_OTU_COUNT );
		Config.requireString( Config.REPORT_LOG_BASE ); //used by DataNormalizer
	}

	/**
	 * Create sparse 3 col tables, rawCount tables, metaMerged tables, normalized, and
	 * logNormalized tables.  Also count hits/sample.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		parseSamples();
		buildOtuCountTables();
	}

	/**
	 * Get parsed sample for given sample ID.
	 *
	 * @param sampleId
	 * @return ParsedSample
	 */
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

	/**
	 * Get parsed samples, mapped with key = Sample ID.
	 */
	@Override
	public Set<ParsedSample> getParsedSamples()
	{
		return parsedSamples;
	}

	@Override
	public abstract void parseSamples() throws Exception;

	/**
	 * Get FileID by removing the PROCESSED suffix.
	 * @param file
	 * @return
	 * @throws Exception
	 */
	protected String getFileID( final File file ) throws Exception
	{
		return file.getName().replace( ClassifierModule.PROCESSED, "" );
	}

	/**
	 * Get merged line by adding metadata for the sampleID found in the first column of the line.
	 * @param line
	 * @return
	 * @throws Exception
	 */
	protected String getMergedLine( final String line ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final String sampleId = new StringTokenizer( line, TAB_DELIM ).nextToken();
		if( MetaUtil.getMetaFileFirstColValues().contains( sampleId ) )
		{
			sb.append( rFormat( line ) );
			for( final String attribute: MetaUtil.getAttributes( sampleId ) )
			{
				sb.append( TAB_DELIM ).append( rFormat( attribute ) );
			}
		}
		else
		{
			Log.out.warn( "Missing record for: " + sampleId + " in metadata: " + MetaUtil.getFile().getAbsolutePath() );
			return null;
		}

		return sb.toString();
	}

	protected boolean isValid( final OtuNode node )
	{
		try
		{
			if( ( node != null ) && ( node.getSampleId() != null ) && ( node.getCount() != null ) )
			{
				for( final String key: node.getOtuMap().keySet() )
				{
					final String val = node.getOtuMap().get( key );
					if( Config.requireTaxonomy().contains( key ) && ( val != null ) && !val.trim().isEmpty() )
					{
						return true;
					}
				}
			}
		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to verify if OTU node is valid! " + ex.getMessage(), ex );
		}
		return false;
	}

	/**
	 * Merge taxonomy level raw count and relative abundance files with metadata.
	 *
	 * @throws Exception
	 */
	private void appendMetaToTaxaCountTables() throws Exception
	{
		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		for( final String taxa: Config.requireTaxonomy() )
		{
			final String normFile = tempDir + taxa + BioLockJ.NORMAL + EXT;
			final String normMetaMergedFile = outDir + taxa + BioLockJ.NORMAL + MetaUtil.META_MERGED_SUFFIX;
			outputMetaMergeTables( normFile, normMetaMergedFile );
			final String logNormFile = tempDir + taxa + BioLockJ.LOG_NORMAL + EXT;
			final String logNormMetaMergedFile = outDir + taxa + BioLockJ.LOG_NORMAL + MetaUtil.META_MERGED_SUFFIX;
			outputMetaMergeTables( logNormFile, logNormMetaMergedFile );
		}
	}

	/**
	 * This method builds tables containing raw counts (above the given threshold set by minNumHits)
	 * @param map
	 * @param file
	 * @throws Exception
	 */
	private File buildRawCountTable( final String pathPrefix, final String taxa ) throws Exception
	{
		final Map<String, Map<String, Integer>> map = getMapFromFile( pathPrefix + SPARSE_TABLE );
		final File otuTable = new File( pathPrefix + RAW_COUNT_TABLE );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( otuTable ) );
		Log.out.info( "Building: " + otuTable );
		writer.write( MetaUtil.getID() );
		final List<String> otuList = getOTUSAtThreshold( map );
		Collections.sort( otuList );
		for( final String s: otuList )
		{
			writer.write( TAB_DELIM + s );
		}

		writer.write( RETURN );
		final List<String> samples = new ArrayList<>();
		for( final String s: map.keySet() )
		{
			samples.add( s );
		}

		Collections.sort( samples );
		int sampleCount = 0;

		for( final String sample: samples )
		{
			writer.write( sample );
			for( final String otu: otuList )
			{
				final Integer val = map.get( sample ).get( otu );
				writer.write( TAB_DELIM + ( ( val == null ) ? 0: val ) );
			}

			if( ++sampleCount != samples.size() )
			{
				writer.write( RETURN );
			}
		}

		writer.write( RETURN );
		writer.close();

		return otuTable;
	}

	/**
	 * Create rawCount and (if configured) output normalized tables based on sparse 3 col tables.
	 * @throws Exception
	 */
	private void createOtuCountTables() throws Exception
	{
		for( final String taxa: Config.requireTaxonomy() )
		{
			final String pathPrefix = getTempDir().getAbsolutePath() + File.separator + taxa;
			final File taxaColFile = buildRawCountTable( pathPrefix, taxa );
			final DataNormalizer wrapper = new DataNormalizer( taxaColFile );
			wrapper.writeNormalizedDataToFile( MetaUtil.getID(), pathPrefix + BioLockJ.NORMAL + EXT );
			wrapper.writeNormalizedLoggedDataToFile( MetaUtil.getID(), pathPrefix + BioLockJ.LOG_NORMAL + EXT );
		}
	}

	/**
	 * Read the OtuNodes to output sparse 3 col tables.  Here we limit by minNumHits if needed.
	 * @throws Exception
	 */
	private void createTaxaSparseThreeColFiles() throws Exception
	{
		for( final String level: Config.requireTaxonomy() )
		{
			final File spTable = new File( getTempDir().getAbsolutePath() + File.separator + level + SPARSE_TABLE );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( spTable ) );
			Log.out.info( "Building: " + spTable );
			for( final ParsedSample sample: parsedSamples )
			{
				final TreeMap<String, Long> levelCounts = sample.getOtuCountMap().get( level );
				for( final String otu: levelCounts.keySet() )
				{
					writer.write(
							sample.getSampleId() + TAB_DELIM + otu + TAB_DELIM + levelCounts.get( otu ) + RETURN );
				}
			}
			writer.close();
		}
	}

	/**
	 * Read the sparse 3 col file to get a map for each key=SampleID, with value = Map<taxa, count>
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	private Map<String, Map<String, Integer>> getMapFromFile( final String filePath ) throws Exception
	{
		final Map<String, Map<String, Integer>> map = new HashMap<>();
		final BufferedReader reader = ModuleUtil.getFileReader( new File( filePath ) );
		String nextLine = null;
		try
		{
			nextLine = reader.readLine();
			while( ( nextLine != null ) && ( nextLine.trim().length() > 0 ) )
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
			Log.out.warn( "BAD LINE: " + nextLine );
			ex.printStackTrace();
			throw new Exception( "Error occurred processing (" + filePath + "): " + ex.getMessage(), ex );
		}
		finally
		{
			reader.close();
		}

		return map;
	}

	/**
	 * Get all OTUs above the required threshold set by minNumHits.
	 * @param map
	 * @param threshold
	 * @return
	 */
	private List<String> getOTUSAtThreshold( final Map<String, Map<String, Integer>> map ) throws Exception
	{
		final Map<String, Integer> countMap = new HashMap<>();
		for( final String s: map.keySet() )
		{
			final Map<String, Integer> innerMap = map.get( s );
			for( final String possibleOtu: innerMap.keySet() )
			{
				Integer oldCount = countMap.get( possibleOtu );
				if( oldCount == null )
				{
					oldCount = 0;
				}

				oldCount += innerMap.get( possibleOtu );
				countMap.put( possibleOtu, oldCount );
			}
		}

		final List<String> otuList = new ArrayList<>();
		for( final String s: countMap.keySet() )
		{
			if( countMap.get( s ) >= Config.requirePositiveInteger( REPORT_MINIMUM_OTU_COUNT ) )
			{
				otuList.add( s );
			}
		}

		return otuList;

	}

	/**
	 * Output meta-merged tables by calling getMergedLine()
	 * @param in
	 * @param out
	 * @throws Exception
	 */
	private void outputMetaMergeTables( final String in, final String out ) throws Exception
	{
		Log.out.info( "Building " + out );
		final BufferedReader reader = new BufferedReader( new FileReader( in ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( out ) );
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			final String mergedLine = getMergedLine( line );
			if( mergedLine != null )
			{
				writer.write( mergedLine + RETURN );
			}
		}

		writer.close();
		reader.close();
	}

	/**
	 * This method is used to get an R-friendly value from the input val.
	 * Comments are ignored, and # symbols are replaced by "Num".
	 * Also, any quotes are removed.
	 *
	 * @param val
	 * @return
	 * @throws Exception
	 */
	protected static String rFormat( final String val ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final String valWithoutQuotes = val.replaceAll( "'", "" ).replaceAll( "\"", "" );
		final StringTokenizer st = new StringTokenizer( valWithoutQuotes, TAB_DELIM );
		while( st.hasMoreTokens() )
		{
			if( !sb.toString().isEmpty() )
			{
				sb.append( TAB_DELIM );
			}

			sb.append( st.nextToken() );
		}

		return sb.toString();
	}

	private static final String EXT = ".tsv";

	private static final Set<ParsedSample> parsedSamples = new HashSet<>();
	//otuNodes --> key=sampleID,value=Set<OtuNode>
	//private static final TreeMap<String, Set<OtuNode>> otuNodes = new TreeMap<>();
	private static final String RAW_COUNT_TABLE = "_RawCounts" + EXT;
	private static final String REPORT_MINIMUM_OTU_COUNT = "report.minOtuCount";
	private static final String SPARSE_TABLE = "_SparseThreeColumns" + EXT;
}