/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date May 15, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.stream.Collectors;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.module.r.R_Module;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;
import biolockj.util.StringUtil;
import biolockj.util.SummaryUtil;

/**
 * This BioModule is used to add metadata columns to the OTU abundance tables.
 */
public class AddMetadataToOtuTables extends JavaModuleImpl implements JavaModule
{

	/**
	 * Produce summary message with min, max, mean, and median hit ratios
	 */
	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			sb.append( "# Samples: " + MetaUtil.getSampleIds().size() + RETURN );
			sb.append( "# Fields:  " + MetaUtil.getFieldNames().size() + RETURN );

			if( !hitRatioPerSample.isEmpty() )
			{

				for( final String key: hitRatioPerSample.keySet() )
				{
					if( hitRatioPerSample.get( key ) == null
							|| hitRatioPerSample.get( key ).equals( Config.requireString( MetaUtil.META_NULL_VALUE ) ) )
					{
						hitRatioPerSample.put( key, "0.0" );
					}
				}

				if( !hitRatioPerSample.isEmpty() )
				{
					final TreeSet<Double> vals = new TreeSet<>( hitRatioPerSample.values().stream()
							.map( Double::parseDouble ).collect( Collectors.toSet() ) );

					sb.append( "Min. Hit Ratio:   " + vals.first() + RETURN );
					sb.append( "Max. Hit Ratio:   " + vals.last() + RETURN );
					sb.append( "Mean Hit Ratio:   " + SummaryUtil.getMean( vals, true ) + RETURN );
					sb.append( "Median Hit Ratio: " + SummaryUtil.getMedian( vals, true ) + RETURN );

					if( !vals.first().equals( vals.last() ) )
					{
						final Set<String> minSamples = new HashSet<>();
						for( final String id: hitRatioPerSample.keySet() )
						{
							if( hitRatioPerSample.get( id ).equals( vals.first().toString() ) )
							{
								minSamples.add( id );
							}
						}

						sb.append( "Samples w/ Min. Hit Ratio: " + minSamples + RETURN );
					}
				}
			}
		}
		catch( final Exception ex )
		{
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.get( getClass() ).warn( msg );
		}

		return super.getSummary() + sb.toString();
	}

	/**
	 * This method matches records from the OTU table and the metadata file by matching the sample ID value in the very
	 * 1st column.
	 */
	@Override
	public void runModule() throws Exception
	{
		if( MetaUtil.getFieldNames().contains( RegisterNumReads.NUM_READS )
				&& MetaUtil.getFieldNames().contains( ParserModuleImpl.NUM_HITS ) )
		{
			addHitRatioToMetadata();
		}

		generateMergedTables();

		Log.get( getClass() ).info( mergeHeaderLine );
		Log.get( getClass() ).info( mergeSampleLine );
		Log.get( Pipeline.class ).info( "Direct runModule() complete!" );
	}

	/**
	 * Add Num_Hits/Num_Reads as Hit_Ratio column to the metadata file
	 * 
	 * @throws Exception if unable to build the new metadata column
	 */
	protected void addHitRatioToMetadata() throws Exception
	{
		for( final String id: MetaUtil.getSampleIds() )
		{
			final String numReadsField = MetaUtil.getField( id, RegisterNumReads.getNumReadFieldName() );
			final String numHitsField = MetaUtil.getField( id, ParserModuleImpl.NUM_HITS );

			if( numReadsField == null || numHitsField == null
					|| numReadsField.equals( Config.requireString( MetaUtil.META_NULL_VALUE ) )
					|| numHitsField.equals( Config.requireString( MetaUtil.META_NULL_VALUE ) ) )
			{
				hitRatioPerSample.put( id, Config.requireString( MetaUtil.META_NULL_VALUE ) );
			}
			else
			{
				final long numReads = Long.valueOf( numReadsField );
				final long numHits = Long.valueOf( numHitsField );
				Log.get( getClass() ).info(
						HIT_RATIO + " for: [" + id + "] ==> " + StringUtil.formatPercentage( numHits, numReads ) );
				hitRatioPerSample.put( id, Double.valueOf( (double) numHits / numReads ).toString() );
			}
		}

		MetaUtil.addColumn( HIT_RATIO, hitRatioPerSample, getOutputDir() );
	}

	/**
	 * Create the merged metadata tables.
	 * 
	 * @throws Exception if unable to build tables
	 */
	protected void generateMergedTables() throws Exception
	{
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		for( final File file: getInputFiles() )
		{
			final String name = Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + "_"
					+ file.getName().replaceAll( R_Module.TSV_EXT, "" ) + META_MERGED;
			Log.get( getClass() ).info( "Merge OTU table + Metadata file: " + outDir + name );
			final BufferedReader reader = SeqUtil.getFileReader( file );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( outDir + name ) );

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
			Log.get( getClass() ).info( "Done merging table: " + file.getAbsolutePath() );
		}

	}

	/**
	 * Return OTU table line with metadata row appended (both have PK = sample ID)
	 *
	 * @param line OTU table line
	 * @return OTU table line + metadata line
	 * @throws Exception if unable to create merged line
	 */
	protected String getMergedLine( final String line ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final String sampleId = new StringTokenizer( line, TAB_DELIM ).nextToken();
		if( sampleId.equals( MetaUtil.getID() ) || MetaUtil.getSampleIds().contains( sampleId ) )
		{
			sb.append( line );
			for( final String field: MetaUtil.getMetadataRecord( sampleId ) )
			{
				sb.append( TAB_DELIM ).append( field.replaceAll( "'", "" ).replaceAll( "\"", "" ) );
			}
		}
		else
		{
			Log.get( getClass() ).warn(
					"Missing record for: " + sampleId + " in metadata: " + MetaUtil.getFile().getAbsolutePath() );
			return null;
		}

		if( mergeHeaderLine == null )
		{
			mergeHeaderLine = "Merged OTU table header [" + sampleId + "] = " + sb.toString();
		}
		else if( mergeSampleLine == null )
		{
			mergeSampleLine = "Example Merged OTU table row [" + sampleId + "] = " + sb.toString();
		}

		return sb.toString();
	}

	private String mergeHeaderLine = null;
	private String mergeSampleLine = null;

	/**
	 * Metadata column name for column that stores the calculation for
	 * {@value biolockj.module.implicit.parser.ParserModuleImpl#NUM_HITS}/{@value biolockj.module.implicit.RegisterNumReads#NUM_READS}:
	 * {@value #HIT_RATIO}.
	 */
	public static final String HIT_RATIO = "Hit_Ratio";
	/**
	 * File suffix added to OTU table file name once merged with metadata.
	 */
	public static final String META_MERGED = "_metaMerged" + R_Module.TSV_EXT;
	private static final Map<String, String> hitRatioPerSample = new HashMap<>();

}
