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
import java.util.stream.IntStream;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.SeqModule;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.util.*;

/**
 * This BioModule imposes a minimum and/or maximum number of reads per sample. Samples below the minimum are discarded.
 * Samples above the maximum are limited by selecting random reads up to the maximum value.
 */
public class RarefySeqs extends JavaModuleImpl implements JavaModule, SeqModule
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
					"Invalid parameters!  RarefySeqs requires " + INPUT_RAREFYING_MIN + " <= " + INPUT_RAREFYING_MAX );
		}
	}

	/**
	 * Set {@value #NUM_RAREFIED_READS} as the number of reads field.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		final String colName = getMetaColName();
		super.cleanUp();
		RegisterNumReads.setNumReadFieldName( colName );
		MetaUtil.addColumn( colName, readsPerSample, getOutputDir(), true );
	}

	/**
	 * This method always requires a prerequisite module with a "number of reads" count such as:
	 * {@link biolockj.module.implicit.RegisterNumReads}. If paired reads found, also return a 2nd module:
	 * {@link biolockj.module.seq.PearMergeReads}.
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
	{
		final List<String> preReqs = super.getPreRequisiteModules();
		if( Config.getBoolean( SeqUtil.INTERNAL_PAIRED_READS ) )
		{
			preReqs.add( ModuleUtil.getDefaultMergePairedReadsConverter() );
		}
		else if( SeqUtil.piplineHasSeqInput() && needsCountModule() )
		{
			preReqs.add( RegisterNumReads.class.getName() );
		}

		return preReqs;
	}

	@Override
	public List<File> getSeqFiles( final Collection<File> files ) throws Exception
	{
		return SeqUtil.getSeqFiles( files );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception
	{
		String summary = SummaryUtil.getCountSummary( readsPerSample, "Reads" );
		sampleIds.removeAll( readsPerSample.keySet() );
		if( !sampleIds.isEmpty() )
		{
			summary += "Removed empty samples: " + BioLockJUtil.getCollectionAsString( sampleIds );
		}
		readsPerSample = null;
		return super.getSummary() + summary;
	}

	/**
	 * For each file with number reads outside of {@link biolockj.Config}.{@link #INPUT_RAREFYING_MIN} and
	 * {@link biolockj.Config}.{@link #INPUT_RAREFYING_MAX} values, generate a new sequence file from a shuffled list of
	 * its sequences.
	 */
	@Override
	public void runModule() throws Exception
	{
		sampleIds.addAll( MetaUtil.getSampleIds() );
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

	}

	/**
	 * Build the rarefied file for the input file, keeping only the given indexes
	 *
	 * @param input Sequence file
	 * @param indexes List of indexes to keep
	 * @throws Exception if unable to build rarefied file
	 */
	protected void buildRarefiedFile( final File input, final List<Integer> indexes ) throws Exception
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
			int index = 0;
			int i = 0;
			final Set<Integer> usedIndexes = new HashSet<>();
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

			readsPerSample.put( SeqUtil.getSampleId( input.getName() ), Integer.toString( indexes.size() ) );

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
		final int numReads = getCount( sampleId, RegisterNumReads.getNumReadFieldName() );
		final int count = Integer.parseInt( Long.toString( numReads ) );
		max = count < max ? count: max;
		Log.debug( getClass(), "Sample[" + sampleId + "]  numReads = " + numReads );
		if( numReads >= Config.getNonNegativeInteger( INPUT_RAREFYING_MIN ) )
		{
			final int[] range = IntStream.rangeClosed( 0, numReads - 1 ).toArray();
			final List<Integer> indexes = new ArrayList<>();
			Collections.addAll( indexes, Arrays.stream( range ).boxed().toArray( Integer[]::new ) );
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
		}
	}

	private Integer getCount( final String sampleId, final String attName ) throws Exception
	{
		if( MetaUtil.getFieldNames().contains( attName ) )
		{
			final String count = MetaUtil.getField( sampleId, attName );
			if( count != null && NumberUtils.isNumber( count ) )
			{
				return Integer.valueOf( count );
			}
		}

		return null;
	}

	private String getMetaColName() throws Exception
	{
		if( otuColName == null )
		{
			otuColName = MetaUtil.getSystemMetaCol( this, NUM_RAREFIED_READS );
		}

		return otuColName;
	}

	private boolean needsCountModule() throws Exception
	{
		for( final String module: Config.requireList( Constants.INTERNAL_BLJ_MODULE ) )
		{
			if( module.equals( SeqFileValidator.class.getName() ) || module.equals( TrimPrimers.class.getName() ) )
			{
				return false;
			}
			else if( module.equals( getClass().getName() ) )
			{
				return true;
			}
		}

		return true;
	}

	private String otuColName = null;
	private Map<String, String> readsPerSample = new HashMap<>();
	private final Set<String> sampleIds = new HashSet<>();

	/**
	 * Metadata column name for column that holds number of rarefied reads per sample: {@value #NUM_RAREFIED_READS}
	 */
	public static final String NUM_RAREFIED_READS = "Num_Rarefied_Reads";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_RAREFYING_MAX} defines the maximum number of reads per file
	 */
	protected static final String INPUT_RAREFYING_MAX = "rarefySeqs.max";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_RAREFYING_MIN} defines the minimum number of reads per file
	 */
	protected static final String INPUT_RAREFYING_MIN = "rarefySeqs.min";

}
