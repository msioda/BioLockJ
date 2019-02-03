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
package biolockj.module.report.taxa;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.SummaryUtil;

/**
 * This BioModule is used to add metadata columns to the OTU abundance tables.
 */
public class AddMetadataToTaxaTables extends JavaModuleImpl implements JavaModule
{
	/**
	 * Require taxonomy table module as prerequisite
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
	{
		final List<String> preReqs = new ArrayList<>();
		if( !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE ) )
		{
			preReqs.add( BuildTaxaTables.class.getName() );
		}
		preReqs.addAll( super.getPreRequisiteModules() );
		return preReqs;
	}

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
			Log.warn( getClass(), msg );
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
		final String numReadsCol = RegisterNumReads.getNumReadFieldName();
		final String numHitsCol = ParserModuleImpl.getOtuCountField();
		if( numReadsCol != null && numHitsCol != null && MetaUtil.getFieldNames().contains( numReadsCol )
				&& MetaUtil.getFieldNames().contains( numHitsCol ) )
		{
			addHitRatioToMetadata();
		}

		generateMergedTables();

		Log.info( getClass(), mergeHeaderLine );
		Log.info( getClass(), mergeSampleLine );
		Log.info( getClass(), "Direct runModule() complete!" );
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
			final String numHitsField = MetaUtil.getField( id, ParserModuleImpl.getOtuCountField() );

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
				Log.info( getClass(),
						HIT_RATIO + " for: [" + id + "] ==> " + BioLockJUtil.formatPercentage( numHits, numReads ) );
				hitRatioPerSample.put( id, Double.valueOf( (double) numHits / numReads ).toString() );
			}
		}

		MetaUtil.addColumn( HIT_RATIO, hitRatioPerSample, getOutputDir(), true );
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
			final String name = file.getName().replaceAll( TSV_EXT, "" ) + META_MERGED;
			Log.info( getClass(), "Merge OTU table + Metadata file: " + outDir + name );
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
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
			Log.info( getClass(), "Done merging table: " + file.getAbsolutePath() );
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
			Log.warn( getClass(), "Missing record for: " + sampleId + " in metadata: " + MetaUtil.getPath() );
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

	private final Map<String, String> hitRatioPerSample = new HashMap<>();
	private String mergeHeaderLine = null;
	private String mergeSampleLine = null;

	/**
	 * Metadata column name for column that stores the calculation for:
	 * {@link biolockj.module.implicit.parser.ParserModuleImpl#getOtuCountField()}/
	 * {@link biolockj.module.implicit.RegisterNumReads#getNumReadFieldName()}: {@value #HIT_RATIO}.
	 */
	public static final String HIT_RATIO = "Hit_Ratio";
	/**
	 * File suffix added to OTU table file name once merged with metadata.
	 */
	public static final String META_MERGED = "_metaMerged" + TSV_EXT;
}
