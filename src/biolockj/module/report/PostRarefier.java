package biolockj.module.report;

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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import biolockj.Config;
import biolockj.Log;
import biolockj.exception.ConfigFormatException;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.*;

/**
 * This BioModule applies a mean iterative post-OTU classification rarefication algorithm so that each output sample
 * will have approximately the same number of OTUs. Samples are rarefied to a configured quantile, most commonly (and
 * most appropriately) to the default 0.50 quantile, otherwise known as the median. Users can configure this module to
 * discard all samples with less OTUs than the rarefication threshold. Each sample is rarefied the configured number of
 * times and the average OTU count is used for each sample. Note that even if a previous module removed singletons, this
 * approach can yield new singleton OTU assignments but these are less likely to be due to contaminant and thus, should
 * generally be allowed in the OTU table output.
 */
public class PostRarefier extends JavaModuleImpl implements JavaModule
{

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( Config.requirePositiveDouble( QUANTILE ) > 1 )
		{
			throw new ConfigFormatException( QUANTILE, "This value must be x, wehre x: { 0.0 < x < 1.0 }" );
		}
		Config.requirePositiveInteger( NUM_ITERATIONS );
		Config.getBoolean( REMOVE_LOW_ABUNDANT_SAMPLES );
		Config.requirePositiveDouble( LOW_ABUNDANT_CUTOFF );

	}

	/**
	 * Update {@link biolockj.module.implicit.parser.ParserModuleImpl} OTU_COUNT field name.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		ParserModuleImpl.setNumHitsFieldName( getMetaColName() );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception
	{
		String summary = SummaryUtil.getCountSummary( hitsPerSample, "OTUs" );
		sampleIds.removeAll( hitsPerSample.keySet() );
		if( !sampleIds.isEmpty() )
		{
			summary += "Removed empty samples: " + BioLockJUtil.getCollectionAsString( sampleIds );
		}
		hitsPerSample = null;
		return super.getSummary() + summary;
	}

	@Override
	public boolean isValidInputModule( final BioModule previousModule ) throws Exception
	{
		return OtuUtil.outputHasOtuCountFiles( previousModule );
	}

	/**
	 * Apply the quantile Config to the number of OTUs per sample to calculate the maximum OTU count per sample. For
	 * each sample rarefy the configured number of times and output a file with the average counts. Update the metadata
	 * to add the new OTU_COUNT column with the new OTU count per sample.
	 */
	@Override
	public void runModule() throws Exception
	{
		sampleIds.addAll( MetaUtil.getSampleIds() );
		Log.info( getClass(), "Rarefied OTU counts will be stored in metadata column: " + getMetaColName() );
		final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts = OtuUtil.getSampleOtuCounts( getInputFiles() );
		final Integer quantileNum = getNumOtusForQuantile( sampleOtuCounts );

		Log.info( getClass(), "Rarefy " + sampleOtuCounts.size() + " to " + quantileNum );
		for( final String sampleId: sampleOtuCounts.keySet() )
		{
			Log.info( getClass(), "Rarefy " + sampleId );
			final TreeMap<String, Integer> data = rarefy( sampleId, sampleOtuCounts.get( sampleId ), quantileNum );
			if( data != null )
			{
				generateOtuput( OtuUtil.getOtuCountFile( getOutputDir(), sampleId, getMetaColName() ), data );
			}
		}

		if( Config.getBoolean( Config.REPORT_NUM_HITS ) )
		{
			MetaUtil.addColumn( getMetaColName(), hitsPerSample, getOutputDir(), true );
		}
	}

	/**
	 * Print the output file wit rarefied counts.
	 * 
	 * @param file Output file
	 * @param otuCounts TreeMap(OTU, count)
	 * @throws Exception if errors occur
	 */
	protected void generateOtuput( final File file, final TreeMap<String, Integer> otuCounts ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
		try
		{
			for( final String otu: otuCounts.keySet() )
			{
				writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}
	}

	/**
	 * Get OTU count data for the given sampleId.
	 * 
	 * @param sampleId Sample ID
	 * @param otuCounts All OTU counts
	 * @return List of OTUs for the given sampleId
	 * @throws Exception if errors occur
	 */
	protected List<String> getData( final String sampleId, final TreeMap<String, Integer> otuCounts ) throws Exception
	{
		final String metaField = MetaUtil.getField( sampleId, ParserModuleImpl.getOtuCountField() );
		final Integer val = Integer.valueOf( metaField );
		final List<String> otus = new ArrayList<>( val );
		for( final String otu: otuCounts.keySet() )
		{
			for( int i = 0; i < otuCounts.get( otu ); i++ )
			{
				otus.add( otu );
			}
			Collections.sort( otus );
		}

		return otus;
	}

	/**
	 * Return a list of low abundant samples, as defined by {@value #LOW_ABUNDANT_CUTOFF}
	 * 
	 * @return List of Sample IDs
	 * @throws Exception if errors occur
	 */
	protected List<String> getLowAbundantSamples() throws Exception
	{
		final List<String> sampleIds = MetaUtil.getSampleIds();
		final int numToRemove = Double
				.valueOf( Math
						.ceil( new Double( Config.requirePositiveDouble( LOW_ABUNDANT_CUTOFF ) * sampleIds.size() ) ) )
				.intValue();

		final String otuCountField = ParserModuleImpl.getOtuCountField();
		if( otuCountField == null || !MetaUtil.getFieldNames().contains( otuCountField )
				|| MetaUtil.getFieldValues( otuCountField ) == null
				|| MetaUtil.getFieldValues( otuCountField ).isEmpty() )
		{
			Log.warn( getClass(),
					"Cannot remove low abundant fields without OTU Count files, field is empty: " + otuCountField );
		}
		else
		{
			final TreeMap<Integer, String> lowest = new TreeMap<>();
			for( final String sampleId: sampleIds )
			{
				final String val = MetaUtil.getField( sampleId, otuCountField );
				try
				{
					if( lowest.size() < numToRemove && val != null && !val.isEmpty() & Integer.valueOf( val ) > 0 )
					{
						lowest.put( Integer.valueOf( val ), sampleId );
					}
					else
					{
						final List<Integer> intList = new ArrayList<>( lowest.keySet() );
						Collections.sort( intList );
						for( final Integer lowVal: intList )
						{
							if( Integer.valueOf( val ) < lowVal )
							{
								final Integer high = intList.get( intList.size() - 1 );
								lowest.remove( high );
								lowest.put( Integer.valueOf( val ), sampleId );
								Log.debug( getClass(), "Replace " + high + " with new low: " + Integer.valueOf( val ) );
								break;
							}

						}
					}
				}
				catch( final Exception ex )
				{
					Log.warn( getClass(), "Quiet try-catch for format exception: " + sampleId );
				}
			}

			sampleIds.removeAll( lowest.values() );

		}

		return sampleIds;
	}

	/**
	 * Get the quantile number of OTUs. If quantile = 0.5 the median value is returned.
	 * 
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @return quantile number of OTUs
	 * @throws Exception if errors occur
	 */
	protected Integer getNumOtusForQuantile( final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts )
			throws Exception
	{
		final TreeMap<String, Integer> countMap = new TreeMap<>();
		for( final String sampleId: sampleOtuCounts.keySet() )
		{
			countMap.put( sampleId,
					sampleOtuCounts.get( sampleId ).values().stream().mapToInt( Integer::intValue ).sum() );
		}

		final List<Integer> data = new ArrayList<>( countMap.values() );
		Collections.sort( data );

		final int index = new Double( Config.requirePositiveDouble( QUANTILE ) * countMap.size() ).intValue();

		return data.get( index );
	}

	/**
	 * Select random OTUs based on the quantileNum from the list of OTUs in data.
	 * 
	 * @param data List( OTUs )
	 * @param quantileNum Qunatile (range: 0.0 - 1.0)
	 * @return List of randomly selected OTUs up to the given quantile
	 * @throws Exception if errors occur
	 */
	protected List<String> getRandomQuantileOtus( List<String> data, final int quantileNum ) throws Exception
	{
		Collections.shuffle( data );
		if( data.size() > quantileNum )
		{
			data = data.subList( 0, quantileNum );
		}
		return data;
	}

	/**
	 * Rarefy the data by taking the average value of {@value #NUM_ITERATIONS}
	 * 
	 * @param sampleId Sample ID
	 * @param otuCounts OTU counts
	 * @param quantileNum Maximum number
	 * @return TreeMap(OTU, count) of rarefied data
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, Integer> rarefy( final String sampleId, final TreeMap<String, Integer> otuCounts,
			final int quantileNum ) throws Exception
	{

		final TreeMap<String, Integer> otuCount = new TreeMap<>();
		final List<String> data = getData( sampleId, otuCounts );
		if( Config.getBoolean( REMOVE_LOW_ABUNDANT_SAMPLES ) && data.size() < quantileNum )
		{
			Log.info( getClass(), "REMOVE LOW ABUNDANT sample: " + sampleId );
			return null;
		}

		for( int i = 0; i < Config.requirePositiveInteger( NUM_ITERATIONS ); i++ )
		{
			Log.debug( getClass(), sampleId + " iteration[ " + i + " ]" );
			for( final String otu: getRandomQuantileOtus( data, quantileNum ) )
			{
				if( otuCount.get( otu ) == null )
				{
					otuCount.put( otu, 0 );
				}

				otuCount.put( otu, otuCount.get( otu ) + 1 );
			}
		}

		int totalSampleOtuCount = 0;
		final TreeMap<String, Integer> meanCountValues = new TreeMap<>();
		int i = 0;
		for( final String otu: otuCount.keySet() )
		{
			final int avg = Math.round( otuCount.get( otu ) / Config.requirePositiveInteger( NUM_ITERATIONS ) );
			if( avg > 0 )
			{
				meanCountValues.put( otu, avg );
				totalSampleOtuCount += avg;
			}
			Log.debug( getClass(), "Total Sample Otu Count[" + i++ + "] = " + totalSampleOtuCount );
		}

		hitsPerSample.put( sampleId, String.valueOf( totalSampleOtuCount ) );
		return meanCountValues;
	}

	private String getMetaColName() throws Exception
	{
		return "postRareQ" + new Double( Config.requirePositiveDouble( QUANTILE ) * 100 ).intValue();
	}

	private Map<String, String> hitsPerSample = new HashMap<>();
	private final Set<String> sampleIds = new HashSet<>();

	/**
	 * {@link biolockj.Config} Posivite Double property to define minimum percentage of samples that must contain an
	 * OTU. Low abundance OTUs will be removed: {@value #LOW_ABUNDANT_CUTOFF}
	 */
	protected static final String LOW_ABUNDANT_CUTOFF = "postRarefier.lowAbundantCutoff";

	/**
	 * {@link biolockj.Config} Positive Integer property {@value #NUM_ITERATIONS} defines the number of iterations to
	 * randomly select the {@value #QUANTILE}% of OTUs.
	 */
	protected static final String NUM_ITERATIONS = "postRarefier.iterations";

	/**
	 * {@link biolockj.Config} Positive Double property {@value #QUANTILE} defines quantile for rarefication. The number
	 * of OTUs/sample are ordered, all samples with more OTUs than the quantile sample are subselected without
	 * replacement until they have the same number of OTUs as the quantile sample value. A quantile of 0.50 returns the
	 * median value.
	 */
	protected static final String QUANTILE = "postRarefier.quantile";

	/**
	 * {@link biolockj.Config} Boolean property {@value #REMOVE_LOW_ABUNDANT_SAMPLES} if TRUE, all samples below the
	 * quantile sample are removed.
	 */
	protected static final String REMOVE_LOW_ABUNDANT_SAMPLES = "postRarefier.removeSamplesBelowQuantile";
}
