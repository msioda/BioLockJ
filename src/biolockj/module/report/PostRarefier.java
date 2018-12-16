package biolockj.module.report;
/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
import java.io.*;
import java.util.*;
import java.util.stream.LongStream;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.BioModuleFactory;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule
 */
public class PostRarefier extends JavaModuleImpl implements JavaModule
{

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.getPositiveInteger( MIN_OTU_COUNT );
		Config.getPositiveDoubleVal( MIN_OTU_THRESHOLD );
		Config.requirePositiveDoubleVal( QUANTILE );
		Config.getBoolean( REMOVE_LOW_ABUNDANT_SAMPLES );

	}

	/**
	 * Set {@value #NUM_RAREFIED_HITS} as the number of hits field.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		//RegisterNumReads.setNumReadFieldName( NUM_RAREFIED_HITS );
	}



	/**
	 * 
	 */
	@Override
	public void runModule() throws Exception
	{
		final List<File> files = getInputFiles();
		for( int i=0; i< files.size(); i++ )
		{
			File f = files.get( i );
			rarefy( f );
			if( i % 25 == 0 || (i + 1) == files.size() )
			{
				Log.info( getClass(), "Done rarefying " + i + "/" + files.size() + " files." );
			}
		}

		updateMetadata();
	}

	/**
	 * Build the rarefied file for the input file, keeping only the given indexes
	 *
	 * @param input Sequence file
	 * @param indexes List of indexes to keep
	 * @throws Exception if unable to build rarefied file
	 */
	protected void buildRarefiedFile( final File input, final List<Long> indexes ) throws Exception
	{
		Log.info( getClass(), "Rarefy [#index=" + indexes.size() + "]: " + input.getAbsolutePath() );
		Log.debug( getClass(), "indexes: " + BioLockJUtil.getCollectionAsString( indexes ) );
		final String fileExt = "." + Config.requireString( SeqUtil.INTERNAL_SEQ_TYPE );
		final String name = getOutputDir().getAbsolutePath() + File.separator + SeqUtil.getSampleId( input.getName() )
				+ fileExt;
		final File output = new File( name );
		final BufferedReader reader = BioLockJUtil.getFileReader( input );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( output ) );
		Log.debug( getClass(),
				"Building file [#lines/read=" + SeqUtil.getNumLinesPerRead() + "]: " + output.getAbsolutePath() );
		try
		{
			long index = 0L;
			long i = 0L;
			final Set<Long> usedIndexes = new HashSet<>();
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( indexes.contains( index ) )
				{
					if( !usedIndexes.contains( index ) )
					{
						Log.debug( getClass(), "Add to usedIndexes: " + index );
					}

					usedIndexes.add( index );
					writer.write( line + RETURN );
				}

				if( ++i % SeqUtil.getNumLinesPerRead() == 0 )
				{
					index++;
				}
			}

			rarefiedHitsPerSample.put( SeqUtil.getSampleId( input.getName() ), Integer.toString( indexes.size() ) );

			if( !usedIndexes.containsAll( indexes ) )
			{
				indexes.removeAll( usedIndexes );
				throw new Exception(
						"Error occurred rarefying indexes for: " + input.getAbsolutePath() + " ---> " + indexes );
			}
		}
		finally
		{
			reader.close();
			writer.close();
		}
	}

	/**
	 * Builds the rarefied file if too many seqs found, or adds files with too few samples to the list of bad samples.
	 *
	 * @param seqFile Sequence file to rarefy
	 * @throws Exception if processing errors occur
	 */
	protected void rarefy( final File seqFile ) throws Exception
	{
		int max = Config.getNonNegativeInteger( INPUT_RAREFYING_MAX );
		final String sampleId = SeqUtil.getSampleId( seqFile.getName() );
		final long numReads = getCount( sampleId, RegisterNumReads.getNumReadFieldName() );
		final int count = Integer.parseInt( Long.toString( numReads ) );
		max = count < max ? count: max;
		Log.debug( getClass(), "Sample[" + sampleId + "]  numReads = " + numReads );
		if( numReads >= Config.getNonNegativeInteger( INPUT_RAREFYING_MIN ) )
		{
			final long[] range = LongStream.rangeClosed( 0L, numReads - 1L ).toArray();
			final List<Long> indexes = new ArrayList<>();
			Collections.addAll( indexes, Arrays.stream( range ).boxed().toArray( Long[]::new ) );
			Collections.shuffle( indexes );
			indexes.subList( max, indexes.size() ).clear();
			buildRarefiedFile( seqFile, indexes );
		}
		else
		{
			Log.info( getClass(),
					"Remove sample [" + sampleId + "] - contains (" + numReads
							+ ") reads, which is less than minimum # reads ("
							+ Config.getNonNegativeInteger( INPUT_RAREFYING_MIN ) + ")" );
			badSamples.add( sampleId );
		}
	}

	/**
	 * Remove the invalid samples from the metadata file.
	 *
	 * @throws Exception if unable to create the new
	 */
	protected void updateMetadata() throws Exception
	{
		if( badSamples.isEmpty() )
		{
			Log.info( getClass(), "All samples rarefied & meet minimum read threshold - none will be ommitted..." );
		}
		else
		{
			Log.warn( getClass(), "Removing samples below rarefying threshold" );
			Log.warn( getClass(), "Removing bad samples ===> " + badSamples );
		}

		final File newMapping = new File(
				getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );
		final BufferedReader reader = BioLockJUtil.getFileReader( MetaUtil.getFile() );

		final BufferedWriter writer = new BufferedWriter( new FileWriter( newMapping ) );

		try
		{
			String line = reader.readLine();
			writer.write( line + TAB_DELIM + NUM_RAREFIED_HITS + RETURN );

			for( line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
				final String id = st.nextToken();
				if( !badSamples.contains( id ) )
				{
					writer.write( line + TAB_DELIM + rarefiedPerSample.get( id ) + RETURN );
				}
			}
		}
		finally
		{
			reader.close();
			writer.close();
		}

		MetaUtil.setFile( newMapping );
		MetaUtil.refreshCache();
	}



	private final Set<String> badSamples = new HashSet<>();
	private final Map<String, String> rarefiedHitsPerSample = new HashMap<>();
	
	/**
	 * Metadata column name for column that holds number of rarefied hits per sample: {@value #NUM_RAREFIED_HITS}
	 */
	public static final String NUM_RAREFIED_HITS = "Num_Rarefied_Hits";

	/**
	 * {@link biolockj.Config} Positive Integer property {@value #NUM_ITERATIONS} defines the number of iterations to randomly select
	 * the {@value #QUANTILE}% of OTUs.
	 */
	protected static final String NUM_ITERATIONS = "postRarefier.iterations";

	/**
	 * {@link biolockj.Config} Positive Double property {@value #MIN_OTU_THRESHOLD} defines minimum percentage of samples
	 * that must contain an OTU for it to be kept.
	 */
	protected static final String MIN_OTU_THRESHOLD = "postRarefier.minOtuThreshold";
	
	/**
	 * {@link biolockj.Config} Positive Integer property {@value #MIN_OTU_COUNT} defines the minimum number of OTUs allowed, if a count less that this
	 * value is found, it is set to 0.
	 */
	protected static final String MIN_OTU_COUNT = "postRarefier.minOtuCount";
	
	/**
	 * {@link biolockj.Config} Positive Double property {@value #QUANTILE} defines quantile for rarefication.  The number of OTUs/sample are ordered, all samples with
	 * more OTUs than the quantile sample are subselected without replacement until they have the same number of OTUs as the quantile sample value.  A quantile of 0.50
	 * returns the median value. 
	 */
	protected static final String QUANTILE = "postRarefier.quantile";
	
	/**
	 * {@link biolockj.Config} Boolean property {@value #REMOVE_LOW_ABUNDANT_SAMPLES} if TRUE, all samples below the quantile sample are removed.
	 */
	protected static final String REMOVE_LOW_ABUNDANT_SAMPLES = "postRarefier.removeSamplesBelowQuantile";
	

	
	

}
