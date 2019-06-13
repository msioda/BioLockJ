package biolockj.module.report.otu;

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
import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.exception.ConfigFormatException;
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
 * 
 * @blj.web_desc Rarefy OTU Counts
 */
public class RarefyOtuCounts extends OtuCountModule {

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		if( Config.requirePositiveDouble( this, QUANTILE ) > 1 )
			throw new ConfigFormatException( QUANTILE, "This value must be x, wehre x: { 0.0 < x < 1.0 }" );
		Config.requirePositiveInteger( this, NUM_ITERATIONS );
		Config.getBoolean( this, REMOVE_LOW_ABUNDANT_SAMPLES );
		Config.requirePositiveDouble( this, LOW_ABUNDANT_CUTOFF );

	}

	/**
	 * Update {@link biolockj.module.implicit.parser.ParserModuleImpl} OTU_COUNT field name.
	 */
	@Override
	public void cleanUp() throws Exception {
		super.cleanUp();
		ParserModuleImpl.setNumHitsFieldName( getMetaColName() + "_" + Constants.OTU_COUNT );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception {
		final String label = "OTUs";
		final int pad = SummaryUtil.getPad( label );
		String summary = SummaryUtil.getCountSummary( this.hitsPerSample, "OTUs", false );
		this.sampleIds.removeAll( this.hitsPerSample.keySet() );
		if( !this.sampleIds.isEmpty() ) summary += BioLockJUtil.addTrailingSpaces( "Removed empty metadata records:", pad ) +
			BioLockJUtil.getCollectionAsString( this.sampleIds );
		this.hitsPerSample = null;
		return super.getSummary() + summary;
	}

	/**
	 * Apply the quantile Config to the number of OTUs per sample to calculate the maximum OTU count per sample. For
	 * each sample rarefy the configured number of times and output a file with the average counts. Update the metadata
	 * to add the new OTU_COUNT column with the new OTU count per sample.
	 */
	@Override
	public void runModule() throws Exception {
		this.sampleIds.addAll( MetaUtil.getSampleIds() );
		Log.info( getClass(),
			"Rarefied OTU counts will be stored in metadata column: " + getMetaColName() + "_" + Constants.OTU_COUNT );
		final TreeMap<String, TreeMap<String, Long>> sampleOtuCounts = OtuUtil.getSampleOtuCounts( getInputFiles() );
		final Long quantileNum = getNumOtusForQuantile( sampleOtuCounts );

		Log.info( getClass(), "Rarefy " + sampleOtuCounts.size() + " to " + quantileNum );
		for( final String sampleId: sampleOtuCounts.keySet() ) {
			Log.info( getClass(), "Rarefy " + sampleId );
			final TreeMap<String, Long> data = rarefy( sampleId, sampleOtuCounts.get( sampleId ), quantileNum );
			if( data != null )
				generateOtuput( OtuUtil.getOtuCountFile( getOutputDir(), sampleId, getMetaColName() ), data );
		}

		if( Config.getBoolean( this, Constants.REPORT_NUM_HITS ) ) MetaUtil
			.addColumn( getMetaColName() + "_" + Constants.OTU_COUNT, this.hitsPerSample, getOutputDir(), true );
	}

	/**
	 * Return a list of low abundant samples, as defined by {@value #LOW_ABUNDANT_CUTOFF}
	 *
	 * @return List of Sample IDs
	 * @throws Exception if errors occur
	 */
	protected List<String> getLowAbundantSamples() throws Exception {
		final List<String> ids = MetaUtil.getSampleIds();
		final int numToRemove = Double
			.valueOf(
				Math.ceil( new Double( Config.requirePositiveDouble( this, LOW_ABUNDANT_CUTOFF ) * ids.size() ) ) )
			.intValue();

		final String otuCountField = ParserModuleImpl.getOtuCountField();
		if( otuCountField == null || !MetaUtil.getFieldNames().contains( otuCountField ) ||
			MetaUtil.getFieldValues( otuCountField, true ).isEmpty() )
			Log.warn( getClass(),
				"Cannot remove low abundant fields without OTU Count files, field is empty: " + otuCountField );
		else {
			final TreeMap<Long, String> lowest = new TreeMap<>();
			for( final String sampleId: ids ) {
				final String val = MetaUtil.getField( sampleId, otuCountField );
				try {
					if( lowest.size() < numToRemove && val != null && !val.isEmpty() & Long.valueOf( val ) > 0 )
						lowest.put( Long.valueOf( val ), sampleId );
					else {
						final List<Long> intList = new ArrayList<>( lowest.keySet() );
						Collections.sort( intList );
						for( final Long lowVal: intList )
							if( Long.valueOf( val ) < lowVal ) {
								final Long high = intList.get( intList.size() - 1 );
								lowest.remove( high );
								lowest.put( Long.valueOf( val ), sampleId );
								Log.debug( getClass(), "Replace " + high + " with new low: " + Long.valueOf( val ) );
								break;
							}
					}
				} catch( final Exception ex ) {
					Log.warn( getClass(),
						"Quiet try-catch for format exception: " + sampleId + " --> " + ex.getMessage() );
				}
			}

			ids.removeAll( lowest.values() );

		}

		return ids;
	}

	/**
	 * Get the quantile number of OTUs. If quantile = 0.5 the median value is returned.
	 *
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @return quantile number of OTUs
	 * @throws Exception if errors occur
	 */
	protected Long getNumOtusForQuantile( final TreeMap<String, TreeMap<String, Long>> sampleOtuCounts )
		throws Exception {
		final TreeMap<String, Long> countMap = new TreeMap<>();
		for( final String sampleId: sampleOtuCounts.keySet() )
			countMap.put( sampleId,
				sampleOtuCounts.get( sampleId ).values().stream().mapToLong( Long::longValue ).sum() );

		final List<Long> data = new ArrayList<>( countMap.values() );
		Collections.sort( data );

		final int index = new Double( Config.requirePositiveDouble( this, QUANTILE ) * countMap.size() ).intValue();

		return data.get( index );
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
	protected TreeMap<String, Long> rarefy( final String sampleId, final TreeMap<String, Long> otuCounts,
		final long quantileNum ) throws Exception {

		final TreeMap<String, Long> otuCount = new TreeMap<>();
		final List<String> data = getData( sampleId, otuCounts );
		if( Config.getBoolean( this, REMOVE_LOW_ABUNDANT_SAMPLES ) && data.size() < quantileNum ) {
			Log.info( getClass(), "REMOVE LOW ABUNDANT sample: " + sampleId );
			return null;
		}

		for( int i = 0; i < Config.requirePositiveInteger( this, NUM_ITERATIONS ); i++ ) {
			Log.debug( getClass(), sampleId + " iteration[ " + i + " ]" );
			for( final String otu: getRandomQuantileOtus( data, quantileNum ) ) {
				if( otuCount.get( otu ) == null ) otuCount.put( otu, 0L );

				otuCount.put( otu, otuCount.get( otu ) + 1 );
			}
		}

		long totalSampleOtuCount = 0L;
		final TreeMap<String, Long> meanCountValues = new TreeMap<>();
		int i = 0;
		for( final String otu: otuCount.keySet() ) {
			final long avg =
				new Integer( Math.round( otuCount.get( otu ) / Config.requirePositiveInteger( this, NUM_ITERATIONS ) ) )
					.longValue();
			if( avg > 0 ) {
				meanCountValues.put( otu, avg );
				totalSampleOtuCount += avg;
			}
			Log.debug( getClass(), "Total Sample Otu Count[" + i++ + "] = " + totalSampleOtuCount );
		}

		this.hitsPerSample.put( sampleId, String.valueOf( totalSampleOtuCount ) );
		return meanCountValues;
	}

	private String getMetaColName() throws Exception {
		return "postRareQ" + new Double( Config.requirePositiveDouble( this, QUANTILE ) * 100 ).intValue();
	}

	/**
	 * Print the output file wit rarefied counts.
	 *
	 * @param file Output file
	 * @param otuCounts TreeMap(OTU, count)
	 * @throws Exception if errors occur
	 */
	protected static void generateOtuput( final File file, final TreeMap<String, Long> otuCounts ) throws Exception {
		final BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
		try {
			for( final String otu: otuCounts.keySet() )
				writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
		} finally {
			writer.close();
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
	protected static List<String> getData( final String sampleId, final TreeMap<String, Long> otuCounts )
		throws Exception {
		final String metaField = MetaUtil.getField( sampleId, ParserModuleImpl.getOtuCountField() );
		final Long val = Long.valueOf( metaField );
		final List<String> otus = new ArrayList<>( val.intValue() );

		for( final String otu: otuCounts.keySet() ) {
			for( int i = 0; i < otuCounts.get( otu ); i++ )
				otus.add( otu );
			Collections.sort( otus );
		}

		return otus;
	}

	/**
	 * Select random OTUs based on the quantileNum from the list of OTUs in data.
	 *
	 * @param data List( OTUs )
	 * @param quantileNum Qunatile (range: 0.0 - 1.0)
	 * @return List of randomly selected OTUs up to the given quantile
	 */
	protected static List<String> getRandomQuantileOtus( final List<String> data, final Long quantileNum ) {
		Collections.shuffle( data );
		if( data.size() > quantileNum ) return data.subList( 0, quantileNum.intValue() );
		return data;
	}

	private Map<String, String> hitsPerSample = new HashMap<>();
	private final Set<String> sampleIds = new HashSet<>();

	/**
	 * {@link biolockj.Config} Posivite Double property to define minimum percentage of samples that must contain an
	 * OTU. Low abundance OTUs will be removed: {@value #LOW_ABUNDANT_CUTOFF}
	 */
	protected static final String LOW_ABUNDANT_CUTOFF = "rarefyOtuCounts.lowAbundantCutoff";

	/**
	 * {@link biolockj.Config} Positive Integer property {@value #NUM_ITERATIONS} defines the number of iterations to
	 * randomly select the {@value #QUANTILE}% of OTUs.
	 */
	protected static final String NUM_ITERATIONS = "rarefyOtuCounts.iterations";

	/**
	 * {@link biolockj.Config} Positive Double property {@value #QUANTILE} defines quantile for rarefication. The number
	 * of OTUs/sample are ordered, all samples with more OTUs than the quantile sample are subselected without
	 * replacement until they have the same number of OTUs as the quantile sample value. A quantile of 0.50 returns the
	 * median value.
	 */
	protected static final String QUANTILE = "rarefyOtuCounts.quantile";

	/**
	 * {@link biolockj.Config} Boolean property {@value #REMOVE_LOW_ABUNDANT_SAMPLES} if TRUE, all samples below the
	 * quantile sample are removed.
	 */
	protected static final String REMOVE_LOW_ABUNDANT_SAMPLES = "rarefyOtuCounts.rmLowSamples";
}
