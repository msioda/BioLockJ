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
import java.io.*;
import java.util.*;
import biolockj.Config;
import biolockj.Log;
import biolockj.exception.ConfigFormatException;
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
 * approach can yeild new singleton OTU assignments but these are less likely to be due to contaminent and thus, should
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
	}

	/**
	 * Set {@value #NUM_RAREFIED_OTUS} as the number of hits field.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		ParserModuleImpl.getModule().setNumHitsFieldName( getMetaColName() );
	}

	/**
	 * Apply the quantile config to the number of OTUs per sample to calculate the maximum OTU count per sample. For
	 * each sample rarefy the configured number of times and output a file with the average counts. Update the metadata
	 * to add the {@value #NUM_RAREFIED_OTUS} column with the new OTU count per sample.
	 */
	@Override
	public void runModule() throws Exception
	{
		final Map<String, Map<String, Integer>> sampleOtuCounts = OtuUtil.getSampleOtuCounts( getInputFiles() );
		final Integer quantileNum = getNumOtusForQuantile( sampleOtuCounts );

		for( final String sampleId: sampleOtuCounts.keySet() )
		{
			final TreeMap<String, Integer> data = rarefy( sampleId, sampleOtuCounts.get( sampleId ), quantileNum );
			if( data != null )
			{
				generateOtuput( getOutputFile( getFileMap().get( sampleId ) ), data );
			}

		}

		updateMetadata();
	}

	/**
	 * Print the output file wit rarefied counts.
	 * 
	 * @param file Output file
	 * @param otuCounts Map(OTU, count)
	 * @throws Exception if errors occur
	 */
	protected void generateOtuput( final File file, final Map<String, Integer> otuCounts ) throws Exception
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

	protected List<String> getData( final String sampleId, final Map<String, Integer> otuCounts ) throws Exception
	{
		final String metaField = MetaUtil.getField( sampleId, ParserModuleImpl.getModule().getNumHitsFieldName() );
		final Integer val = Integer.valueOf( metaField );
		final List<String> otus = new ArrayList<>( val );
		for( final String otu: otuCounts.keySet() )
		{
			for( int i = 0; i < otuCounts.get( otu ); i++ )
			{
				otus.add( otu );
			}
		}

		return otus;
	}

	/**
	 * Get the quantile number of OTUs. If quantile = 0.5 the median value is returned.
	 * 
	 * @param sampleOtuCounts Map(SampleId, Map(OTU, count)) OTU counts for every sample
	 * @return quantile number of OTUs
	 * @throws Exception if errors occur
	 */
	protected Integer getNumOtusForQuantile( final Map<String, Map<String, Integer>> sampleOtuCounts ) throws Exception
	{
		final Map<String, Integer> countMap = new HashMap<>();
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
	 * @return Map(OTU, count) of rarefied data
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, Integer> rarefy( final String sampleId, final Map<String, Integer> otuCounts,
			final int quantileNum ) throws Exception
	{
		final Map<String, Integer> otuCount = new HashMap<>();
		final List<String> data = getData( sampleId, otuCounts );
		if( Config.getBoolean( REMOVE_LOW_ABUNDANT_SAMPLES ) && data.size() < quantileNum )
		{
			badSamples.add( sampleId );
			return null;
		}

		for( int i = 0; i < Config.requirePositiveInteger( NUM_ITERATIONS ); i++ )
		{
			for( final String otu: getRandomQuantileOtus( data, quantileNum ) )
			{
				if( otuCount.get( otu ) == null )
				{
					otuCount.put( otu, 0 );
				}

				otuCount.put( otu, otuCount.get( otu ) + 1 );
			}
		}
		
		int sum = 0;
		final TreeMap<String, Integer> aveCount = new TreeMap<>();
		for( final String otu: otuCount.keySet() )
		{
			final int avg = Math.round( otuCount.get( otu ) / Config.requirePositiveInteger( NUM_ITERATIONS ) );
			if( avg > 0 )
			{
				aveCount.put( otu, avg );
				sum += avg;
			}
		}
		
		hitsPerSample.put( sampleId, String.valueOf( sum ) );
		return aveCount;
	}

	/**
	 * Remove the invalid samples from the metadata file and add {@value #NUM_RAREFIED_OTUS} column with the new OTU
	 * counts.
	 *
	 * @throws Exception if errors occur
	 */
	protected void updateMetadata() throws Exception
	{

		if( badSamples.isEmpty() )
		{
			Log.info( getClass(),
					"All samples have valid OTUs that meet minimum read threshold - none will be ommitted..." );
			MetaUtil.addColumn( getMetaColName(), hitsPerSample, getOutputDir() );
			return;
		}
		else
		{
			Log.warn( getClass(), "Removing samples containing nothing but low count OTUs that have been removed: "
					+ BioLockJUtil.printLongFormList( badSamples ) );
		}

		final File newMapping = new File(
				getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );
		final BufferedReader reader = BioLockJUtil.getFileReader( MetaUtil.getFile() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newMapping ) );
		try
		{
			String line = reader.readLine();
			writer.write( line + TAB_DELIM + getMetaColName() + RETURN );

			for( line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
				final String id = st.nextToken();
				if( !badSamples.contains( id ) )
				{
					writer.write( line + TAB_DELIM + hitsPerSample.get( id ) + RETURN );
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

	private Map<String, File> getFileMap() throws Exception
	{
		if( fileMap == null )
		{
			fileMap = new HashMap<>();
			for( final File f: getInputFiles() )
			{
				fileMap.put( OtuUtil.getSampleId( f ), f );
			}
		}
		return fileMap;
	}

	private String getMetaColName() throws Exception
	{
		if( otuColName == null )
		{
			otuColName = ModuleUtil.getSystemMetaCol( this, NUM_RAREFIED_OTUS );
		}

		return otuColName;
	}

	private File getOutputFile( final File f ) throws Exception
	{
		return new File( getOutputDir().getAbsolutePath() + File.separator + f.getName() );
	}

	private final Set<String> badSamples = new HashSet<>();
	private Map<String, File> fileMap = null;
	private final Map<String, String> hitsPerSample = new HashMap<>();
	private String otuColName = null;

	/**
	 * Metadata column name for column that holds number of rarefied hits per sample: {@value #NUM_RAREFIED_OTUS}
	 */
	public static final String NUM_RAREFIED_OTUS = "Num_Rarefied_Otus";

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
