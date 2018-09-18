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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
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
 * This BioModule imposes a minimum and/or maximum number of reads per sample. Samples below the minimum are discarded.
 * Samples above the maximum are limited by selecting random reads up to the maximum value.
 */
public class Rarefier extends JavaModuleImpl implements JavaModule
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

		if( rarefyingMax == null || rarefyingMax < 1 || rarefyingMin == null || rarefyingMin > rarefyingMax )
		{
			throw new Exception( "Invalid parameter value.  Rarefier requires that (" + INPUT_RAREFYING_MIN + " <= "
					+ INPUT_RAREFYING_MAX + ") & (" + INPUT_RAREFYING_MAX + " > 1)" );
		}
	}

	/**
	 * Set {@value #NUM_RAREFIED_READS} as the number of reads field.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		RegisterNumReads.setNumReadFieldName( NUM_RAREFIED_READS );
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
		final int numFiles = files == null ? 0: files.size();

		Log.get( getClass() ).info( "Rarefying " + numFiles + " " + SeqUtil.getInputSequenceType() + " files..." );
		Log.get( getClass() ).info( "=====> Min # Reads = " + Config.getNonNegativeInteger( INPUT_RAREFYING_MIN ) );
		Log.get( getClass() ).info( "=====> Max # Reads = " + Config.getPositiveInteger( INPUT_RAREFYING_MAX ) );

		int i = 0;
		for( final File f: files )
		{
			rarefy( f );
			if( ++i % 25 == 0 )
			{
				Log.get( getClass() ).info( "Done rarefying " + i + "/" + numFiles + " files." );
			}
		}

		if( i % 25 != 0 )
		{
			Log.get( getClass() ).info( "Done rarefying " + i + "/" + numFiles + " files." );
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
		Log.get( getClass() ).info( "Rarefy [#index=" + indexes.size() + "]: " + input.getAbsolutePath() );
		Log.get( getClass() ).debug( "indexes: " + BioLockJUtil.getCollectionAsString( indexes ) );
		final String fileExt = "." + SeqUtil.getInputSequenceType();
		final String name = getOutputDir().getAbsolutePath() + File.separator + SeqUtil.getSampleId( input.getName() )
				+ fileExt;
		final File output = new File( name );
		final BufferedReader reader = SeqUtil.getFileReader( input );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( output ) );
		Log.get( getClass() ).debug(
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
						Log.get( getClass() ).debug( "Add to usedIndexes: " + index );
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
		final long numReads = getCount( sampleId, RegisterNumReads.getNumReadFieldName() );
		final int count = Integer.parseInt( Long.toString( numReads ) );
		max = count < max ? count: max;
		Log.get( getClass() ).debug( "Sample[" + sampleId + "]  numReads = " + numReads );
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
			Log.get( getClass() )
					.info( "Remove sample [" + sampleId + "] - contains (" + numReads
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
			Log.get( getClass() )
					.info( "All samples rarefied & meet minimum read threshold - none will be ommitted..." );
		}
		else
		{
			Log.get( getClass() ).warn( "Removing samples below rarefying threshold" );
			Log.get( getClass() ).warn( "Removing bad samples ===> " + badSamples );
		}

		final File newMapping = new File(
				getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );
		final BufferedReader reader = SeqUtil.getFileReader( MetaUtil.getFile() );

		final BufferedWriter writer = new BufferedWriter( new FileWriter( newMapping ) );

		try
		{
			String line = reader.readLine();
			writer.write( line + TAB_DELIM + NUM_RAREFIED_READS + RETURN );

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

	private final Set<String> badSamples = new HashSet<>();
	/**
	 * Metadata column name for column that holds number of rarefied reads per sample: {@value #NUM_RAREFIED_READS}
	 */
	public static final String NUM_RAREFIED_READS = "Num_Rarefied_Reads";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_RAREFYING_MAX} defines the maximum number of reads per file
	 */
	protected static final String INPUT_RAREFYING_MAX = "rarefier.max";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_RAREFYING_MIN} defines the minimum number of reads per file
	 */
	protected static final String INPUT_RAREFYING_MIN = "rarefier.min";

	private static final Map<String, String> rarefiedPerSample = new HashMap<>();

}
