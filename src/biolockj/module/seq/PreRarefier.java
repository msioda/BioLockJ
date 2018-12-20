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
package biolockj.module.seq;

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
import biolockj.util.*;

/**
 * This BioModule imposes a minimum and/or maximum number of reads per sample. Samples below the minimum are discarded.
 * Samples above the maximum are limited by selecting random reads up to the maximum value.
 */
public class PreRarefier extends JavaModuleImpl implements JavaModule
{
	/**
	 * Validate module dependencies
	 * <ol>
	 * <li>Validate {@link biolockj.Config}.{@link #INPUT_RAREFYING_MIN} is a non-negative integer
	 * <li>Validate {@link biolockj.Config}.{@link #INPUT_RAREFYING_MAX} is a positive integer that is greater than or
	 * equal to {@link biolockj.Config}.{@link #INPUT_RAREFYING_MIN} (if defined)
	 * </ol>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		final Integer rarefyingMax = Config.getPositiveInteger( INPUT_RAREFYING_MAX );
		final Integer rarefyingMin = Config.getNonNegativeInteger( INPUT_RAREFYING_MIN );

		if( rarefyingMin == null && rarefyingMax == null
				|| rarefyingMax != null && rarefyingMin != null && rarefyingMin > rarefyingMax )
		{
			throw new Exception(
					"Invalid parameters!  PreRarefier requires " + INPUT_RAREFYING_MIN + " <= " + INPUT_RAREFYING_MAX );
		}
	}

	/**
	 * Set {@value #NUM_RAREFIED_READS} as the number of reads field.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		RegisterNumReads.getModule().setNumReadFieldName( getMetaColName() );
	}

	/**
	 * This method always requires the prerequisite module: {@link biolockj.module.implicit.RegisterNumReads}. If paired
	 * reads found, also return a 2nd module: {@link biolockj.module.seq.PearMergeReads}.
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		final List<Class<?>> preReqs = super.getPreRequisiteModules();
		preReqs.add( RegisterNumReads.class );
		if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			preReqs.add( Class.forName( BioModuleFactory.getDefaultMergePairedReadsConverter() ) );
		}

		return preReqs;
	}

	/**
	 * For each file with number reads outside of {@link biolockj.Config}.{@link #INPUT_RAREFYING_MIN} and
	 * {@link biolockj.Config}.{@link #INPUT_RAREFYING_MAX} values, generate a new sequence file from a shuffled list of
	 * its sequences.
	 */
	@Override
	public void runModule() throws Exception
	{
		final List<File> files = getInputFiles();
		for( int i = 0; i < files.size(); i++ )
		{
			final File f = files.get( i );
			rarefy( f );
			if( i % 25 == 0 || i + 1 == files.size() )
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

			rarefiedPerSample.put( SeqUtil.getSampleId( input.getName() ), Integer.toString( indexes.size() ) );

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
		final long numReads = getCount( sampleId, RegisterNumReads.getModule().getNumReadFieldName() );
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
		String metaColName = getMetaColName();
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
			writer.write( line + TAB_DELIM + metaColName + RETURN );

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

	private Long getCount( final String sampleId, final String attName ) throws Exception
	{
		if( MetaUtil.getFieldNames().contains( attName ) )
		{
			final String count = MetaUtil.getField( sampleId, attName );
			if( count != null && NumberUtils.isNumber( count ) )
			{
				return Long.valueOf( count );
			}
		}

		return null;
	}

	private String getMetaColName() throws Exception
	{
		if( otuColName == null )
		{
			otuColName = ModuleUtil.getSystemMetaCol( this, NUM_RAREFIED_READS );
		}

		return otuColName;
	}

	private final Set<String> badSamples = new HashSet<>();

	private String otuColName = null;
	private final Map<String, String> rarefiedPerSample = new HashMap<>();

	/**
	 * Metadata column name for column that holds number of rarefied reads per sample: {@value #NUM_RAREFIED_READS}
	 */
	public static final String NUM_RAREFIED_READS = "Num_Rarefied_Reads";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_RAREFYING_MAX} defines the maximum number of reads per file
	 */
	protected static final String INPUT_RAREFYING_MAX = "preRarefier.max";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_RAREFYING_MIN} defines the minimum number of reads per file
	 */
	protected static final String INPUT_RAREFYING_MIN = "preRarefier.min";

}
