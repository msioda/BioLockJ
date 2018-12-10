/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date March 8, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.seq;

import java.io.*;
import java.util.*;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule validates fasta/fastq file formats are valid and enforces min/max read lengths.
 */
public class SeqFileValidator extends JavaModuleImpl implements JavaModule
{

	/**
	 * Set {@value #NUM_VALID_READS} as the number of reads field.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		RegisterNumReads.setNumReadFieldName( NUM_VALID_READS );
	}

	/**
	 * Produce a summary message with counts on total number of reads and number of valid reads containing a barcode
	 * defined in the metadata file.
	 */
	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			boolean foundTrimmed = false;
			boolean foundTooShort = false;
			boolean foundInvalid = false;

			int numSamples = sampleStats.keySet().size();
			long totalMeanReads = 0L;
			long overallMinReadLen = 0L;
			long overallMaxReadLen = 0L;
			
			for( final String sampleId: new TreeSet<>( sampleStats.keySet() ) )
			{
				final Long[] stats = sampleStats.get( sampleId );
				final Long numValid = stats[ NUM_VALID_READS_INDEX ];
				final Long numTooShort = stats[ NUM_TOO_SHORT_READS_INDEX ];
				final Long numTrimmed = stats[ NUM_TRIMMED_READS_INDEX ];
				final Long numInvalid = stats[ NUM_INVALID_FORMAT_INDEX ];
				final Long meanReadLen = stats[ MEAN_NUM_READ_INDEX ];
				final Long maxReadLen = stats[ MAX_READ_INDEX ];
				final Long minReadLen = stats[ MIN_READ_INDEX ];
				
				totalMeanReads += meanReadLen;
			
				if( overallMinReadLen == 0 || minReadLen < overallMinReadLen  )
				{
					overallMinReadLen = minReadLen;
				}
				
				if( maxReadLen > overallMaxReadLen  )
				{
					overallMaxReadLen = maxReadLen;
				}
				
				if( numTooShort > 0 || numTrimmed > 0 || numInvalid > 0 )
				{
					sb.append( sampleId + ": " );
					if( numInvalid > 0 )
					{
						foundInvalid = true;
						sb.append( "# Seq with valid format = " + numValid + " | " );
					}
					if( numInvalid > 0 )
					{
						foundInvalid = true;
						sb.append( "# Seq with invalid format = " + numInvalid + " | " );
					}
					if( numTooShort > 0 )
					{
						foundTooShort = true;
						sb.append( "# Seq length below threshold = " + numTooShort + " | " );
					}
					if( numTrimmed > 0 )
					{
						foundTrimmed = true;
						sb.append( "# Seq length above threshold = " + numTrimmed + " | " );
					}
					sb.append( RETURN );
				}
			}
			
			sb.append( "Mean valid read length = " + new Double(totalMeanReads/numSamples).toString() + RETURN );
			sb.append( "Min valid read length = " + new Long( overallMinReadLen ).toString() + RETURN );
			sb.append( "Max valid read length = " + new Long( overallMaxReadLen ).toString() + RETURN );

			if( foundTooShort )
			{
				sb.append( "Minimum discarded read length = " + minReadLen() + RETURN );
			}
			if( foundTrimmed )
			{
				sb.append( "Maximum discarded read length = " + Config.getPositiveInteger( INPUT_SEQ_MAX ) + RETURN );
			}
			if( foundInvalid || foundTooShort )
			{
				sb.append( "Discarded sequences stored in: " + getTempDir().getAbsolutePath() + RETURN );
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
	 * Call {@link #validateFile(File)} for each input file.
	 */
	@Override
	public void runModule() throws Exception
	{
		for( final File file: getInputFiles() )
		{
			validateFile( file );
			Log.info( getClass(), "Done validating " + file.getAbsolutePath() );
		}

		MetaUtil.addColumn( NUM_VALID_READS, readsPerSample, getOutputDir() );
	}

	/**
	 * Validate sequence files:
	 * <ol>
	 * <li>Validate valid 1st sequence header character is expected character
	 * <li>Validate fastq files have same number of bases and quality scores per read
	 * <li>Remove reads below minimum threshold: {@value #INPUT_SEQ_MIN}
	 * <li>Trim reads if above the maximum threshold: {@value #INPUT_SEQ_MAX}
	 * </ol>
	 * Invalid reads are saved to a file in the module temp directory for analysis/review.
	 * 
	 * @param file Sequence file
	 * @throws Exception if I/O errors occur while processing sequence files
	 */
	protected void validateFile( final File file ) throws Exception
	{
		long totalReadLen = 0L;
		long minReadLen = 0L;
		long maxReadLen = 0L;
		long seqNum = 0L;
		long numValid = 0L;
		long numTrimmed = 0L;
		long numTooShort = 0L;
		long numInvalidFormat = 0L;
		final Integer seqMax = Config.getPositiveInteger( INPUT_SEQ_MAX );

		final List<String> seqLines = new ArrayList<>();
		final List<String> badLines = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		final BufferedWriter writer = new BufferedWriter(
				new FileWriter( new File( getFileName( getOutputDir(), file.getName() ) ) ) );

		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			seqLines.add( line );

			if( seqLines.size() == SeqUtil.getNumLinesPerRead() )
			{
				seqNum++;
				final String headerChar = seqLines.get( 0 ).substring( 0, 1 );
				if( !SeqUtil.getSeqHeaderChars().contains( headerChar ) )
				{
					numInvalidFormat++;
					badLines.addAll( seqLines );
					Log.warn( getClass(),
							"Sequence #" + seqNum + " format invalid.  Must begin with a valid header char ("
									+ SeqUtil.getSeqHeaderChars() + ")  --> header line = " + seqLines.get( 0 ) );
				}
				else if( seqLines.get( 1 ).length() < minReadLen() )
				{
					numTooShort++;
					badLines.addAll( seqLines );
					Log.warn( getClass(),
							"Sequence #" + seqNum + " format invalid.  Must have a minimum number of bases ("
									+ minReadLen() + ")  --> \n" + seqLines.get( 0 ) + "\n" + seqLines.get( 1 ) );
				}
				else if( SeqUtil.isFastQ() && seqLines.get( 1 ).length() != seqLines.get( 3 ).length() )
				{
					numInvalidFormat++;
					badLines.addAll( seqLines );
					Log.warn( getClass(), "Sequence #" + seqNum + " fastq format invalid.  Must have equal "
							+ " number of bases and quality scores: " + seqLines.get( 0 ) );
				}
				else
				{
					numValid++;
					if( seqMax != null && seqMax > 0 && seqLines.get( 1 ).length() > seqMax )
					{
						numTrimmed++;
						seqLines.set( 1, seqLines.get( 1 ).substring( 0, seqMax ) );
						if( SeqUtil.isFastQ() )
						{
							seqLines.set( 3, seqLines.get( 3 ).substring( 0, seqMax ) );
						}
					}
					
					
					int readLen = seqLines.get( 1 ).length();
					
					totalReadLen += readLen;
					
					if( minReadLen == 0 || readLen < minReadLen  )
					{
						minReadLen = readLen;
					}
					if( readLen > maxReadLen  )
					{
						maxReadLen = readLen;
					}
					
					for( final String seqLine: seqLines )
					{
						writer.write( seqLine + BioLockJ.RETURN );
					}
				}

				seqLines.clear();
			}
		}
		
		writer.close();
		reader.close();

		if( !badLines.isEmpty() )
		{
			final File tempFile = new File( getFileName( getTempDir(), "INVALID_READS_" + file.getName() ) );
			Log.warn( getClass(), "Extracting invalid reads to --> " + tempFile.getAbsolutePath() );
			final BufferedWriter invalidWriter = new BufferedWriter( new FileWriter( tempFile ) );
			for( final String seqLine: badLines )
			{
				invalidWriter.write( seqLine + RETURN );
			}

			invalidWriter.close();
		}

		final Long[] stats = new Long[ 4 ];
		stats[ NUM_VALID_READS_INDEX ] = numValid;
		stats[ NUM_TRIMMED_READS_INDEX ] = numTrimmed;
		stats[ NUM_TOO_SHORT_READS_INDEX ] = numTooShort;
		stats[ NUM_INVALID_FORMAT_INDEX ] = numInvalidFormat;
		
		stats[ MEAN_NUM_READ_INDEX ] = Double.valueOf( totalReadLen / numValid ).longValue();
		stats[ MIN_READ_INDEX ] = minReadLen;
		stats[ MAX_READ_INDEX ] = maxReadLen;
		minReadLen = 0;
		
		sampleStats.put( SeqUtil.getSampleId( file.getName() ), stats );

		if( SeqUtil.isForwardRead( file.getName() ) )
		{
			readsPerSample.put( SeqUtil.getSampleId( file.getName() ), String.valueOf( numValid ) );
		}

	}

	private String getFileName( final File dir, String name ) throws Exception
	{
		// trim .gz extension
		if( name.toLowerCase().endsWith( ".gz" ) )
		{
			name = name.substring( 0, name.length() - 3 );
		}

		return dir.getAbsolutePath() + File.separator + name;
	}

	private int minReadLen() throws Exception
	{
		Integer seqMin = Config.getPositiveInteger( INPUT_SEQ_MIN );
		if( seqMin == null )
		{
			seqMin = 1;
		}
		return seqMin;
	}

	private final Map<String, String> readsPerSample = new HashMap<>();
	private final Map<String, Long[]> sampleStats = new HashMap<>();

	/**
	 * Column name that holds number of valid reads per sample: {@value #NUM_VALID_READS}
	 */
	public static final String NUM_VALID_READS = "Num_Valid_Reads";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_SEQ_MAX} defines the maximum number of reads per file
	 */
	protected static final String INPUT_SEQ_MAX = "input.seqMaxLen";

	/**
	 * {@link biolockj.Config} property {@value #INPUT_SEQ_MIN} defines the minimum number of reads per file
	 */
	protected static final String INPUT_SEQ_MIN = "input.seqMinLen";

	private static final int MAX_READ_INDEX = 7;
	private static final int MIN_READ_INDEX = 6;
	private static final int NUM_INVALID_FORMAT_INDEX = 5;
	private static final int NUM_TOO_SHORT_READS_INDEX = 4;
	private static final int NUM_TRIMMED_READS_INDEX = 3;
	private static final int MEAN_NUM_READ_INDEX = 1;
	private static final int NUM_VALID_READS_INDEX = 0;
}
