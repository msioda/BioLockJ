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
import java.text.DecimalFormat;
import java.util.*;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.util.*;

/**
 * This BioModule removes sequence primers from demultiplexed files.<br>
 * The primers are defined using regular expressions in a separate file.
 * 
 */
public class TrimPrimers extends JavaModuleImpl implements JavaModule
{
	/**
	 * Validates the file that defines the REGEX primers. If primers are located at start of read, add REGEX line anchor
	 * "^" to the start of the primer sequence in {@value #INPUT_TRIM_SEQ_FILE}.
	 * <ul>
	 * <li>If seqs are multiplexed, {@link biolockj.module.implicit.Demultiplexer} must run as a prerequisite module
	 * <li>If seqs are paired-end reads, and {@link biolockj.module.seq.PearMergeReads} is configured before this
	 * module, {@value #INPUT_TRIM_SEQ_FILE} must contain the reverse compliment of the reverse primer with a REGEX line
	 * anchor "$" at the end.
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		BioLockJ.copyFileToPipelineRoot( getPrimerFile() );
		getPrimers( false );
	}

	/**
	 * Set {@value #NUM_TRIMMED_READS} as the number of reads field.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		MetaUtil.addColumn( NUM_TRIMMED_READS, getValidReadsPerSample(), getOutputDir() );
		RegisterNumReads.setNumReadFieldName( NUM_TRIMMED_READS );
	}

	/**
	 * Output the summary messages generated by the module.
	 */
	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			for( final String msg: summaryMsgs )
			{
				sb.append( msg + RETURN );
			}

			return sb.toString() + super.getSummary();

		}
		catch( final Exception ex )
		{
			Log.get( getClass() ).warn( "Unable to complete module summary! " + ex.getMessage() );
		}

		return super.getSummary();
	}

	/**
	 * Trims primers from fasta or fastq files. Saves messages for summary email with metrics on best/worst and average
	 * files. Just a quick implementation on reporting metrics is working but code needs clean-up and simplification.
	 */
	@Override
	public void runModule() throws Exception
	{
		trimSeqs();
		Log.get( getClass() ).debug( "numLinesPerRead = " + SeqUtil.getNumLinesPerRead() );
		Log.get( getClass() ).debug( "#samples in table numLinesWithPrimer = " + numLinesWithPrimer.size() );
		Log.get( getClass() ).debug( "#samples in table numLinesNoPrimer = " + numLinesNoPrimer.size() );
		Log.get( getClass() ).debug( "#samples in table validReadsPerFile = " + validReadsPerFile.size() );
		Log.get( getClass() ).debug( "fileNames.size() = " + fileNames.size() );
		final TreeSet<File> files = new TreeSet<>( validReadsPerFile.keySet() );
		for( final String name: fileNames )
		{
			Log.get( getClass() ).debug( "Checking file to see if primers have been trimmed = " + name );
			boolean found = false;
			for( final File f: files )
			{
				Log.get( getClass() ).debug( "Checking validReadsPerFile.keySet().next() = " + f.getAbsolutePath() );
				if( name.startsWith( f.getAbsolutePath() ) )
				{
					Log.get( getClass() ).debug( "found match!" );
					found = true;
					break;
				}
			}
			if( !found && numDebugLines++ < 4 )
			{
				summaryMsgs.add( "File contains no primers!  File name: " + name );
				Log.get( getClass() ).warn( summaryMsgs.get( summaryMsgs.size() - 1 ) );
			}
		}

		if( Config.getBoolean( INPUT_REQUIRE_PRIMER ) )
		{
			Log.get( getClass() )
					.warn( INPUT_REQUIRE_PRIMER + "=Y so any sequences without a primer have been discarded" );
		}

		long totalPrimerR = 0L;
		long totalNoPrimerR = 0L;
		long totalPrimerF = 0L;
		long totalNoPrimerF = 0L;
		long totalValid = 0L;
		double maxFw = 0.0;
		double minFw = 0.0;
		double maxRv = 0.0;
		double minRv = 0.0;
		String maxFileFw = null;
		String minFileFw = null;
		String maxFileRv = null;
		String minFileRv = null;
		String maxFwDisplay = null;
		String minFwDisplay = null;
		String maxRvDisplay = null;
		String minRvDisplay = null;
		for( final File f: files )
		{
			Long v = validReadsPerFile.get( f );
			Long a = numLinesWithPrimer.get( f.getAbsolutePath() );
			Long b = numLinesNoPrimer.get( f.getAbsolutePath() );

			if( v == null )
			{
				v = 0L;
			}
			if( a == null )
			{
				a = 0L;
			}
			if( b == null )
			{
				b = 0L;
			}

			totalValid += v;

			double ratio = Double.valueOf( df.format( 100 * ( (double) a / ( a + b ) ) ) );
			String per = BioLockJUtil.formatPercentage( a, a + b );

			Log.get( getClass() )
					.info( f.getName() + " Reads with primer ----------------- " + a + "/" + ( a + b ) + " = " + per );

			if( ModuleUtil.moduleExists( PearMergeReads.class.getName() ) )
			{
				ratio = Double.valueOf( df.format( 100 * ( (double) v / ( a + b ) ) ) );
				per = BioLockJUtil.formatPercentage( v, a + b );
				Log.get( getClass() ).info(
						f.getName() + " Paired reads with primer in both -- " + v + "/" + ( a + b ) + " = " + per );
			}

			Log.get( getClass() ).debug( "file = " + f.getAbsolutePath() );
			Log.get( getClass() ).debug( "maxFw = " + maxFw );
			Log.get( getClass() ).debug( "minFw = " + minFw );
			Log.get( getClass() ).debug( "ratio = " + ratio );
			Log.get( getClass() ).debug( "maxFw < ratio = " + ( maxFw < ratio ) );
			Log.get( getClass() ).debug( "minFw > ratio = " + ( minFw > ratio ) );

			if( foundPaired && f.getName().contains( Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX ) ) )
			{
				if( maxRvDisplay == null || maxRv < ratio )
				{
					maxRv = ratio;
					maxRvDisplay = per;
					maxFileRv = f.getAbsolutePath();
					Log.get( getClass() ).info(
							"Found new: Max % reads kept in Reverse Read = " + maxFileRv + " = " + maxRvDisplay );
				}

				if( minRvDisplay == null || minRv > ratio )
				{
					minRv = ratio;
					minRvDisplay = per;
					minFileRv = f.getAbsolutePath();
					Log.get( getClass() ).info(
							"Found new: Min % reads kept in Reverse Read = " + minFileRv + " = " + minRvDisplay );
				}

				totalPrimerR += a;
				totalNoPrimerR += b;
			}
			else
			{
				if( maxFwDisplay == null || maxFw < ratio )
				{
					maxFw = ratio;
					maxFwDisplay = per;
					maxFileFw = f.getAbsolutePath();
					Log.get( getClass() ).info(
							"Found new: Max % reads kept in Forward Read = " + maxFileFw + " = " + maxFwDisplay );
				}

				if( minFwDisplay == null || minFw > ratio )
				{
					minFw = ratio;
					minFwDisplay = per;
					minFileFw = f.getAbsolutePath();
					Log.get( getClass() ).info(
							"Found new: Min % reads kept in Forward Read = " + minFileFw + " = " + minFwDisplay );
				}

				totalPrimerF += a;
				totalNoPrimerF += b;
			}
		}

		if( ModuleUtil.moduleExists( PearMergeReads.class.getName() ) )
		{
			summaryMsgs.add( "Max % reads kept in Forward Read = " + maxFileFw + " = " + maxFwDisplay );
			Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );
			summaryMsgs.add( "Min % reads kept in Forward Read = " + minFileFw + " = " + minFwDisplay );
			Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );

			summaryMsgs.add( "Max % reads kept in Reverse Read = " + maxFileRv + " = " + maxRvDisplay );
			Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );
			summaryMsgs.add( "Min % reads kept in Reverse Read = " + minFileRv + " = " + minRvDisplay );
			Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );

		}
		else
		{
			summaryMsgs.add( "Max % reads kept in = " + maxFileFw + " = " + maxFwDisplay );
			Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );
			summaryMsgs.add( "Min % reads kept in = " + minFileFw + " = " + minFwDisplay );
			Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );
		}

		final long totalF = totalPrimerF + totalNoPrimerF;
		final long totalR = totalPrimerR + totalNoPrimerR;
		final long totalNoPrimer = totalNoPrimerF + totalNoPrimerR;
		final long totalWithPrimer = totalPrimerF + totalPrimerR;
		final long total = totalNoPrimer + totalWithPrimer;
		if( total > 1 )
		{

			summaryMsgs.add( "Mean % reads with primer = " + totalWithPrimer + "/" + total + " = "
					+ BioLockJUtil.formatPercentage( totalWithPrimer, total ) );

			Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );

			if( ModuleUtil.moduleExists( PearMergeReads.class.getName() ) )
			{
				summaryMsgs.add( "Mean % Forward reads with primer = " + totalPrimerF + "/" + totalF + " = "
						+ BioLockJUtil.formatPercentage( totalPrimerF, totalF ) );
				Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );
				summaryMsgs.add( "Mean % Reverse reads with primer  = " + totalPrimerR + "/" + totalR + " = "
						+ BioLockJUtil.formatPercentage( totalPrimerR, totalR ) );
				Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );
				if( !Config.getBoolean( INPUT_REQUIRE_PRIMER ) )
				{
					summaryMsgs.add( "Mean % Paired reads with matching primer = " + totalValid + "/" + total + " = "
							+ BioLockJUtil.formatPercentage( totalValid, total ) );
				}

				Log.get( getClass() ).info( summaryMsgs.get( summaryMsgs.size() - 1 ) );
			}
		}
	}

	/**
	 * Get the primers listed in the {@value #INPUT_TRIM_SEQ_FILE}. If a merged fw primer (starting with ^) and a merged
	 * rv primer (ending with $) are found set mergedReadTwoPrimers = true to enforce reads must have both primers if
	 * discarding reads without valid primers
	 *
	 * @param doLog Boolean set TRUE to print to log
	 * @return Set of primers
	 * @throws Exception if unable to read the file
	 */
	protected Set<String> getPrimers( final boolean doLog ) throws Exception
	{
		boolean fwMergePrimerFound = false;
		boolean rvMergePrimerFound = false;
		final Set<String> primers = new HashSet<>();
		final File trimSeqFile = getPrimerFile();
		final BufferedReader reader = BioLockJUtil.getFileReader( trimSeqFile );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( !line.startsWith( "#" ) )
				{
					final String seq = line.trim().toUpperCase();
					if( seq.length() > 0 )
					{
						if( doLog )
						{
							Log.get( getClass() ).info( "Found primer to trim: " + seq );
						}
						String regexSeq = "";

						for( int i = 1; i <= seq.length(); i++ )
						{
							final String base = seq.substring( i - 1, i );
							final String iupac = getIupacBase( base );
							regexSeq = regexSeq + iupac;
							if( !base.equals( iupac ) )
							{
								if( !substitutions.contains( base ) )
								{
									if( doLog )
									{
										Log.get( getClass() )
												.info( "IUPAC substitution of base: " + base + " to: " + iupac );
									}
									substitutions.add( base );
								}
							}
						}

						if( regexSeq.startsWith( "^" ) )
						{
							fwMergePrimerFound = true;
						}
						else if( seq.endsWith( "$" ) )
						{
							rvMergePrimerFound = true;
						}
						else
						{
							throw new Exception(
									"INVALID PRIMER!  Primers must start with \"^\" or end with \"$\"  Update primer file: "
											+ trimSeqFile.getAbsolutePath() );
						}

						primers.add( regexSeq );

					}
				}
			}

			if( fwMergePrimerFound && rvMergePrimerFound )
			{
				mergedReadTwoPrimers = true;
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}
		if( primers.size() < 1 )
		{
			throw new Exception( "No primers found in: " + trimSeqFile.getAbsolutePath() );
		}

		return primers;
	}

	private File getPrimerFile() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			return DockerUtil.getDockerVolumeFile( INPUT_TRIM_SEQ_FILE, DockerUtil.CONTAINER_PRIMER_DIR, "primer" );
		}
		return Config.requireExistingFile( INPUT_TRIM_SEQ_FILE );
	}

	private String getTrimFilePath( final File f ) throws Exception
	{
		String suffix = "";
		if( Config.requireBoolean( Config.INTERNAL_PAIRED_READS ) && !SeqUtil.isForwardRead( f.getName() ) )
		{
			foundPaired = true;
			suffix = Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX );
		}
		else
		{
			suffix = Config.requireString( Config.INPUT_FORWARD_READ_SUFFIX );
		}

		return getOutputDir().getAbsolutePath() + File.separator + SeqUtil.getSampleId( f.getName() ) + suffix + "."
				+ SeqUtil.getInputSequenceType();
	}

	private Set<String> getValidHeaders( final File file, final Set<String> primers ) throws Exception
	{
		final Set<String> validHeaders = new HashSet<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		int lineCounter = 1;
		String header = null;
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				line = line.trim();
				if( lineCounter % SeqUtil.getNumLinesPerRead() == 1 )
				{
					header = SeqUtil.getHeader( line );
				}
				else if( lineCounter % SeqUtil.getNumLinesPerRead() == 2 )
				{
					boolean foundHeader = false;
					for( final String seq: primers )
					{
						final int seqLength = line.length();
						line = line.replaceFirst( seq, "" );
						if( seqLength != line.length() )
						{
							foundHeader = true;
						}
					}

					if( foundHeader )
					{
						if( validHeaders.contains( header ) )
						{
							throw new Exception( "NON-FATAL Exception: Duplicate header: " + header );
						}

						validHeaders.add( header );
					}
				}
				lineCounter++;
			}

			Log.get( getClass() ).info( file.getName() + " # valid headers = " + validHeaders.size() );

		}
		finally
		{
			reader.close();
		}

		return validHeaders;
	}

	private Map<String, String> getValidReadsPerSample() throws Exception
	{
		if( !MetaUtil.getFieldNames().contains( NUM_TRIMMED_READS ) && validReadsPerSample.isEmpty() )
		{
			for( final File f: validReadsPerFile.keySet() )
			{
				if( !Config.requireBoolean( Config.INTERNAL_PAIRED_READS ) || SeqUtil.isForwardRead( f.getName() ) )
				{
					validReadsPerSample.put( SeqUtil.getSampleId( f.getName() ),
							Long.toString( validReadsPerFile.get( f ) ) );
				}
			}
		}
		return validReadsPerSample;
	}

	private void printReports( final Map<String, Map<String, String>> missingPrimers, final String reportLabel )
			throws Exception
	{
		if( !missingPrimers.isEmpty() )
		{
			for( final String key: missingPrimers.keySet() )
			{
				final Map<String, String> map = missingPrimers.get( key );
				Log.get( getClass() ).warn( "TrimPrimers " + key + " # " + reportLabel + " = " + map.size() );

				BufferedWriter writer = null;
				try
				{
					writer = new BufferedWriter( new FileWriter( new File(
							getTempDir().getAbsolutePath() + File.separator + key + "_" + reportLabel + " .txt" ) ) );
					for( final String header: map.keySet() )
					{
						writer.write( header + RETURN );
						writer.write( map.get( header ) + RETURN );
					}
				}
				catch( final Exception ex )
				{
					Log.get( getClass() ).error( "Error occurred writng primer report" + ex.getMessage(), ex );
				}
				finally
				{
					if( writer != null )
					{
						writer.close();
					}
				}
			}
		}
		else if( mergedReadTwoPrimers )
		{
			Log.get( getClass() ).warn( "TrimPrimers # " + reportLabel + " = 0" );
		}
	}

	private void processFile( final File file, final Set<String> primers ) throws Exception
	{
		processFile( file, new HashSet<>(), primers );
	}

	private void processFile( final File file, final Set<String> validHeaders, final Set<String> primers )
			throws Exception
	{
		Log.get( getClass() ).info( "Processing file = " + file.getAbsolutePath() );
		fileNames.add( file.getAbsolutePath() );
		final File trimmedFile = new File( getTrimFilePath( file ) );
		Log.get( getClass() ).info( "Create trimmed file = " + trimmedFile.getAbsolutePath() );

		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( trimmedFile ) );
		try
		{
			int fwPrimerLength = 0;
			int rvPrimerLength = 0;
			final List<String> seqLines = new ArrayList<>();
			boolean found = false;

			String origSequence = "";
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( seqLines.size() == 1 )
				{
					origSequence = line;
					found = false;
					for( final String seq: primers )
					{
						if( line.replaceFirst( seq, "" ).length() != line.length() )
						{
							if( seq.startsWith( "^" ) )
							{
								if( fwPrimerLength != 0 )
								{
									throw new Exception(
											"INVALID SEQ!  Read contains 2 forward primers!  " + origSequence );
								}

								fwPrimerLength = line.length() - line.replaceFirst( seq, "" ).length();
							}
							else if( seq.endsWith( "$" ) )
							{
								if( rvPrimerLength != 0 )
								{
									throw new Exception(
											"INVALID SEQ!  Read contains 2 reverse primers!  " + origSequence );
								}

								rvPrimerLength = line.length() - line.replaceFirst( seq, "" ).length();
							}
							else
							{
								throw new Exception(
										"INVALID PRIMER!  Primers must start with \"^\" or end with \"$\"" );
							}

							line = line.replaceFirst( seq, "" );

							if( mergedReadTwoPrimers && fwPrimerLength < 1 && rvPrimerLength < 1 )
							{
								// Log.get( getClass() ).warn( "Read missing BOTH primers " + origSequence );
								if( missingBothPrimers.get( file.getName() ) == null )
								{
									final Map<String, String> m = new HashMap<>();
									m.put( seqLines.get( 0 ), origSequence );
									missingBothPrimers.put( file.getName(), m );
								}
								else
								{
									missingBothPrimers.get( file.getName() ).put( seqLines.get( 0 ), origSequence );
								}
							}
							else if( mergedReadTwoPrimers && fwPrimerLength < 1 )
							{
								Log.get( getClass() ).debug( "Read missing forward primer " + origSequence );
								if( missingFwPrimers.get( file.getName() ) == null )
								{
									final Map<String, String> m = new HashMap<>();
									m.put( seqLines.get( 0 ), origSequence );
									missingFwPrimers.put( file.getName(), m );
								}
								else
								{
									missingFwPrimers.get( file.getName() ).put( seqLines.get( 0 ), origSequence );
								}

							}
							else if( mergedReadTwoPrimers && rvPrimerLength < 1 )
							{
								Log.get( getClass() ).debug( "Read missing reverse primer " + origSequence );
								if( missingRvPrimers.get( file.getName() ) == null )
								{
									final Map<String, String> m = new HashMap<>();
									m.put( seqLines.get( 0 ), origSequence );
									missingRvPrimers.put( file.getName(), m );
								}
								else
								{
									missingRvPrimers.get( file.getName() ).put( seqLines.get( 0 ), origSequence );
								}
							}
							else
							{
								found = true;
							}
						}
					}

					if( found )
					{
						final Long x = numLinesWithPrimer.get( file.getAbsolutePath() );
						numLinesWithPrimer.put( file.getAbsolutePath(), x == null ? 1L: x + 1L );
					}
					else
					{
						final Long x = numLinesNoPrimer.get( file.getAbsolutePath() );
						numLinesNoPrimer.put( file.getAbsolutePath(), x == null ? 1L: x + 1L );
					}
				}
				else if( seqLines.size() == 3 )
				{
					if( fwPrimerLength > 0 )
					{
						line = line.substring( fwPrimerLength );
					}
					if( rvPrimerLength > 0 )
					{
						line = line.substring( 0, line.length() - rvPrimerLength );
					}
				}

				seqLines.add( line );

				if( seqLines.size() == SeqUtil.getNumLinesPerRead() )
				{
					// seqLines.set( 0, SeqUtil.getHeader( seqLines.get( 0 ) ) );
					final boolean validRecord = found && ( ModuleUtil.moduleExists( PearMergeReads.class.getName() )
							? validHeaders.contains( seqLines.get( 0 ) )
							: true );

					if( !Config.getBoolean( INPUT_REQUIRE_PRIMER ) || validRecord )
					{
						final Long x = validReadsPerFile.get( file );
						validReadsPerFile.put( file, x == null ? 1L: x + 1L );

						for( int j = 0; j < SeqUtil.getNumLinesPerRead(); j++ )
						{
							writer.write( seqLines.get( j ) + RETURN );
						}
					}
					fwPrimerLength = 0;
					rvPrimerLength = 0;
					seqLines.clear();
				}
			}
		}
		catch( final Exception ex )
		{
			Log.get( getClass() ).error( "Error removing primers from file = " + file.getAbsolutePath(), ex );
		}
		finally
		{
			reader.close();
			writer.close();
		}
	}

	private void trimSeqs() throws Exception
	{
		final Set<String> primers = getPrimers( true );
		final boolean mergeModuleExists = ModuleUtil.moduleExists( PearMergeReads.class.getName() );
		final Map<File, File> pairedReads = mergeModuleExists ? SeqUtil.getPairedReads( getInputFiles() ): null;
		final List<File> files = mergeModuleExists ? new ArrayList<>( pairedReads.keySet() ): getInputFiles();
		final int count = files == null ? 0: files.size();
		int i = 0;
		Log.get( getClass() ).info( "Trimming primers from " + ( mergeModuleExists ? 2 * count: count ) + " files..." );
		for( final File file: files )
		{

			final Set<String> validReads = getValidHeaders( file, primers );
			if( mergeModuleExists )
			{
				validReads.retainAll( getValidHeaders( pairedReads.get( file ), primers ) );
				processFile( file, validReads, primers );
				processFile( pairedReads.get( file ), validReads );
			}
			else
			{
				processFile( file, primers );
			}

			if( ( i++ + 1 ) % 25 == 0 )
			{
				Log.get( getClass() )
						.info( "Done trimming " + i + "/" + count + ( mergeModuleExists ? " file pairs": " files" ) );
			}
		}

		Log.get( getClass() )
				.info( "Done trimming " + i + "/" + count + ( mergeModuleExists ? " file pairs": " files" ) );

		printReports( missingBothPrimers, "missingBothPrimers" );
		printReports( missingFwPrimers, "missingFwPrimers" );
		printReports( missingRvPrimers, "missingRvPrimers" );
	}

	private static String getIupacBase( final String base )
	{
		if( DNA_BASE_MAP.keySet().contains( base ) )
		{
			return DNA_BASE_MAP.get( base );
		}
		return base;
	}

	private final DecimalFormat df = new DecimalFormat( "##.##" );

	private final Set<String> fileNames = new HashSet<>();

	private boolean foundPaired = false;

	private boolean mergedReadTwoPrimers = false;
	private final Map<String, Map<String, String>> missingBothPrimers = new HashMap<>();
	private final Map<String, Map<String, String>> missingFwPrimers = new HashMap<>();
	private final Map<String, Map<String, String>> missingRvPrimers = new HashMap<>();
	private final Map<String, Long> numLinesNoPrimer = new HashMap<>();
	private final Map<String, Long> numLinesWithPrimer = new HashMap<>();
	private final Map<File, Long> validReadsPerFile = new HashMap<>();
	private final Map<String, String> validReadsPerSample = new HashMap<>();
	/**
	 * {@link biolockj.Config} property {@value #INPUT_TRIM_SEQ_FILE} defines the file path to the file that defines the
	 * primers as regular expressions.
	 */
	public static final String INPUT_TRIM_SEQ_FILE = "trimPrimers.filePath";
	/**
	 * Metadata column name for column that holds number of trimmed reads per sample: {@value #NUM_TRIMMED_READS}
	 */
	public static final String NUM_TRIMMED_READS = "Num_Trimmed_Reads";
	/**
	 * {@link biolockj.Config} property {@value #INPUT_REQUIRE_PRIMER} is a boolean used to determine if sequences
	 * without a primer should be kept or discarded
	 */
	protected static final String INPUT_REQUIRE_PRIMER = "trimPrimers.requirePrimer";
	private static final Map<String, String> DNA_BASE_MAP = new HashMap<>();
	private static long numDebugLines = 0;
	private static Set<String> substitutions = new HashSet<>();
	private static final List<String> summaryMsgs = new ArrayList<>();
	static
	{
		// IUPAC DNA BASE Substitutions
		// http://www.dnabaser.com/articles/IUPAC%20ambiguity%20codes.html
		DNA_BASE_MAP.put( "Y", "[CT]" );
		DNA_BASE_MAP.put( "R", "[AG]" );
		DNA_BASE_MAP.put( "W", "[AT]" );
		DNA_BASE_MAP.put( "S", "[GC]" );
		DNA_BASE_MAP.put( "K", "[TG]" );
		DNA_BASE_MAP.put( "M", "[CA]" );
		DNA_BASE_MAP.put( "D", "[AGT]" );
		DNA_BASE_MAP.put( "V", "[ACG]" );
		DNA_BASE_MAP.put( "H", "[ACT]" );
		DNA_BASE_MAP.put( "B", "[CGT]" );
		DNA_BASE_MAP.put( "N", "[ACGT]" );
		DNA_BASE_MAP.put( "X", "[ACGT]" );
	}
}
