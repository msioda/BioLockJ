/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 2, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.*;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigViolationException;

/**
 * This utility helps interact with FastA and FastQ sequence files.
 */
public class SeqUtil
{

	// Prevent instantiation
	private SeqUtil()
	{}

	/**
	 * Create a copy of the sequence files in property {@value biolockj.Config#INPUT_DIRS}, output to a directory named
	 * {@value biolockj.Config#INTERNAL_PIPELINE_DIR}/input.
	 *
	 * @throws Exception if unable to copy the files
	 */
	public static void copyInputData() throws Exception
	{
		final File inputFileDir = new File( Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath()
				+ File.separator + "input" );

		if( !inputFileDir.exists() )
		{
			inputFileDir.mkdirs();
		}

		final File startedFlag = new File( inputFileDir.getAbsolutePath() + File.separator + Pipeline.BLJ_STARTED );
		if( startedFlag.exists() )
		{
			BioLockJUtil.deleteWithRetry( inputFileDir, 10 );
		}

		for( final File dir: BioLockJUtil.getInputDirs() )
		{
			Log.info( SeqUtil.class, "Copying input files from " + dir + " to " + inputFileDir );
			FileUtils.copyDirectory( dir, inputFileDir );
		}

		final File completeFlag = new File( inputFileDir.getAbsolutePath() + File.separator + Pipeline.BLJ_COMPLETE );
		final FileWriter writer = new FileWriter( completeFlag );
		writer.close();
		if( !completeFlag.exists() )
		{
			throw new Exception( "Unable to create " + completeFlag.getAbsolutePath() );
		}
	}

	/**
	 * Method counts number of reads in the given sequence file by counting the number of lines and dividing by the
	 * number of lines/sample (fasta=2, fastq=4)
	 * 
	 * @param seqFile Sequence file
	 * @return Number of reads in seqFile
	 * @throws Exception if errors occur
	 */
	public static int countNumReads( final File seqFile ) throws Exception
	{
		int count = 0;
		final BufferedReader r = BioLockJUtil.getFileReader( seqFile );
		for( String line = r.readLine(); line != null; line = r.readLine() )
		{
			count++;
		}
		r.close();

		return count / getNumLinesPerRead();
	}

	/**
	 * Extract the header from the first line of a read (parse to first space). This part of the header line will be
	 * identical on forward and reverse reads, so can be used to find matching pairs of reads.
	 * <p>
	 * To extract the header, trim to the Illumina read direction indicator, if it exists, otherwise to the 1st space.
	 * Method recognizes the headers: {@value #ILLUMINA_FW_READ_IND} and {@value #ILLUMINA_RV_READ_IND}
	 * 
	 * @param line Sequence line
	 * @return Sequence header
	 */
	public static String getHeader( final String line )
	{
		final boolean hasFwDelim = line.contains( ILLUMINA_FW_READ_IND );
		final boolean hasRvDelim = line.contains( ILLUMINA_RV_READ_IND );
		final String delim = hasFwDelim ? ILLUMINA_FW_READ_IND: hasRvDelim ? ILLUMINA_RV_READ_IND: null;
		if( delim != null )
		{
			return line.trim().substring( 0, line.trim().indexOf( delim ) );
		}
		else
		{
			return line.trim();
		}

	}

	/**
	 * Return the header of each read in the sequence file.
	 * 
	 * @param seq Fasta or Fastq file
	 * @return Set of sequence headers
	 * @throws Exception if unable to parse the seq file
	 */
	public static Set<String> getHeaders( final File seq ) throws Exception
	{
		final Set<String> headers = new HashSet<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( seq );
		int lineCounter = 1;
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( lineCounter++ % getNumLinesPerRead() == 1 )
				{
					headers.add( getHeader( line ) );
				}
			}
		}
		finally
		{
			reader.close();
		}

		return headers;
	}

	/**
	 * Get valid headers found in both forward and reverse read.
	 * 
	 * @param fwRead Forward read sequence file
	 * @param rvRead Reverse read sequence file
	 * @return Sequence headers found in both files
	 * @throws Exception if I/O errors occur
	 */
	public static Set<String> getHeaders( final File fwRead, final File rvRead ) throws Exception
	{
		final Set<String> headers = getHeaders( fwRead );
		headers.retainAll( getHeaders( rvRead ) );
		return headers;
	}

	/**
	 * Return number of lines per read (for fasta return 2, for fastq return 4)
	 * 
	 * @return 2 or 4
	 * @throws Exception if unable to determine sequence format
	 */
	public static int getNumLinesPerRead() throws Exception
	{
		return isFastA() ? 2: 4;
	}

	/**
	 * Return number of reads given number of lines in a sample.
	 * 
	 * @param numLines Total number of lines in sample
	 * @return Number of reads
	 * @throws Exception if number of reads cannot be determined
	 */
	public static long getNumReads( final long numLines ) throws Exception
	{
		return new Double( numLines / getNumLinesPerRead() ).longValue();
	}

	/**
	 * Paired reads must have a unique file suffix to identify forward and reverse reads. Parameter files read and a map
	 * with forward read file names as keys and matching reverse reads as the map return value
	 *
	 * @param files List of paired read files
	 * @return Map with key=fwRead and val=rvRead
	 * @throws ConfigViolationException if unpaired reads are found and
	 * {@link biolockj.Config}.{@value #INPUT_REQUIRE_COMPLETE_PAIRS} = {@value biolockj.Config#TRUE}
	 * @throws Exception if unable to find paired reads
	 */
	public static Map<File, File> getPairedReads( final Collection<File> files ) throws Exception
	{
		return getPairedReads( files, true );
	}

	/**
	 * Paired reads must have a unique file suffix to identify forward and reverse reads. Parameter files read and a map
	 * with forward read file names as keys and matching reverse reads as the map return value
	 *
	 * @param files List of paired read files
	 * @param fwReadAsKey boolean if true map uses forward read file as key and reverse as value; if false, this is
	 * reversed.
	 * @return Map with key=fwRead and val=rvRead, or the reverse if fwReadAsKey is false.
	 * @throws Exception if unable to find paired reads
	 */
	public static Map<File, File> getPairedReads( final Collection<File> files, final boolean fwReadAsKey )
			throws Exception
	{
		Log.debug( SeqUtil.class, "Looking for paired reads in " + ( files == null ? 0: files.size() ) + " files " );
		final Map<File, File> map = new HashMap<>();
		final Set<String> rvReads = new HashSet<>();
		final Set<File> unpairedFwReads = new HashSet<>();
		for( final File fwRead: files )
		{
			final String name = fwRead.getName();

			if( !isForwardRead( name ) )
			{
				rvReads.add( name );
				continue;
			}

			File rvRead = null;
			final String sampleID = getSampleId( name );
			Log.debug( SeqUtil.class,
					"Search for paired read to match forward read ( " + name + " ) with sample ID: " + sampleID );
			for( final File searchFile: files )
			{
				if( sampleID.equals( getSampleId( searchFile.getName() ) ) && !isForwardRead( searchFile.getName() ) )
				{
					Log.debug( SeqUtil.class, "Matching reverse read: " + searchFile.getName() );
					rvRead = searchFile;
					break;
				}
			}

			if( rvRead != null )
			{
				if( fwReadAsKey )
				{
					map.put( fwRead, rvRead );
				}
				else
				{
					map.put( rvRead, fwRead );
				}
			}
			else
			{
				unpairedFwReads.add( fwRead );
			}

		}

		for( final File f: map.values() )
		{
			for( final String rv: rvReads )
			{
				if( rv.equals( f.getName() ) )
				{
					rvReads.remove( rv );
					break;
				}
			}
		}

		String msg = "";
		if( !unpairedFwReads.isEmpty() )
		{
			msg = "Unpaired FW Reads:" + BioLockJUtil.printLongFormList( unpairedFwReads );
		}

		if( !rvReads.isEmpty() )
		{
			msg += "Unpaired RV Reads: " + BioLockJUtil.printLongFormList( rvReads );
		}

		if( Config.getString( Config.INTERNAL_PAIRED_READS ) != null
				&& Config.getBoolean( INPUT_REQUIRE_COMPLETE_PAIRS ) && !msg.isEmpty() )
		{
			throw new ConfigViolationException( INPUT_REQUIRE_COMPLETE_PAIRS, msg );
		}
		else if( Config.getString( Config.INTERNAL_PAIRED_READS ) != null && !msg.isEmpty() )
		{
			Log.warn( SeqUtil.class, "Unpaired reads will be ignored because Config property [ "
					+ INPUT_REQUIRE_COMPLETE_PAIRS + "=" + Config.FALSE + " ]" + BioLockJ.RETURN + msg );
		}

		return map;
	}

	/**
	 * Return read direction indicator for forward or reverse read if found in the file name.
	 * 
	 * @param file Sequence file
	 * @return Forward or reverse read file suffix
	 * @throws Exception if errors occur
	 */
	public static String getReadDirectionSuffix( final File file ) throws Exception
	{
		return getReadDirectionSuffix( file.getName() );
	}

	/**
	 * Return read direction indicator for forward or reverse read if found in the file name.
	 * 
	 * @param fileName Sequence file name
	 * @return Forward or reverse read file suffix
	 * @throws Exception if errors occur
	 */
	public static String getReadDirectionSuffix( final String fileName ) throws Exception
	{
		if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			if( SeqUtil.isForwardRead( fileName ) )
			{
				return Config.requireString( Config.INPUT_FORWARD_READ_SUFFIX );
			}
			else
			{
				return Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX );
			}
		}

		return "";
	}

	/**
	 * Method extracts Sample ID from the name param. Possibly input is a file name so remove file extensions. If
	 * demultiplexing (RDP/Kraken support this option), input is a sequence header. If
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_FILENAME_COLUMN} is supplied, then possible return
	 * values are limited to the given samples ids, or "" if the file is not in the filename column.
	 *
	 * @param value File name or sequence header
	 * @return Sample ID
	 * @throws Exception if unable to determine Sample ID
	 */
	public static String getSampleId( final String value ) throws Exception
	{
		String id;
		String revValue = "";
		try
		{
			if( MetaUtil.hasColumn( Config.getString( MetaUtil.META_FILENAME_COLUMN ) ) )
			{
				int ind;
				if( isForwardRead( value ) )
				{
					ind = MetaUtil.getFieldValues( Config.getString( MetaUtil.META_FILENAME_COLUMN ) ).indexOf( value );
				}

				else
				{
					// if this file name is a reverse file, look up the corresponding forward file name.
					final String fwReadSuffix = Config.getString( Config.INPUT_FORWARD_READ_SUFFIX );
					final String rvReadSuffix = Config.getString( Config.INPUT_REVERSE_READ_SUFFIX );
					revValue = value.substring( 0, value.lastIndexOf( rvReadSuffix ) ) + fwReadSuffix
							+ value.substring( value.lastIndexOf( rvReadSuffix ) + rvReadSuffix.length() );
					Log.debug( SeqUtil.class, value + " is a reverse read. Seeking sample id for file: " + revValue );
					ind = MetaUtil.getFieldValues( Config.getString( MetaUtil.META_FILENAME_COLUMN ) )
							.indexOf( revValue );
				}
				if( ind == -1 )
				{
					Log.info( SeqUtil.class, "Filename [" + ( isForwardRead( value ) ? value: revValue )
							+ "] does not appear in column [" + Config.getString( MetaUtil.META_FILENAME_COLUMN )
							+ "]. This file will be ignored." );
					id = "";
				}
				else
				{
					id = MetaUtil.getSampleIds().get( ind );
				}
			}
			else
			{
				id = value;
				// trim .gz extension
				if( id.toLowerCase().endsWith( ".gz" ) )
				{
					id = id.substring( 0, id.length() - 3 ); // 9_R2.fastq
				}

				// trim .fasta or .fastq extension
				if( id.toLowerCase().endsWith( "." + FASTA ) || id.toLowerCase().endsWith( "." + FASTQ ) )
				{
					id = id.substring( 0, id.length() - 6 );
				}
			}
			// trim directional suffix
			if( !Config.getBoolean( Config.INTERNAL_MULTIPLEXED ) ) // must be a file name
			{
				final String fwReadSuffix = Config.getString( Config.INPUT_FORWARD_READ_SUFFIX );
				final String rvReadSuffix = Config.getString( Config.INPUT_REVERSE_READ_SUFFIX );
				if( fwReadSuffix != null && isForwardRead( id ) && id.lastIndexOf( fwReadSuffix ) > 0 )
				{
					id = id.substring( 0, id.lastIndexOf( fwReadSuffix ) );
				}
				else if( rvReadSuffix != null && id.lastIndexOf( rvReadSuffix ) > 0 )
				{
					id = id.substring( 0, id.lastIndexOf( rvReadSuffix ) );
				}
			}

			// trim user defined file prefix and/or suffix patterns
			final String trimPrefix = Config.getString( Config.INPUT_TRIM_PREFIX );
			final String trimSuffix = Config.getString( Config.INPUT_TRIM_SUFFIX );
			if( trimPrefix != null && !trimPrefix.isEmpty() && id.indexOf( trimPrefix ) > -1 )
			{
				id = id.substring( trimPrefix.length() + id.indexOf( trimPrefix ) );
			}

			if( trimSuffix != null && !trimSuffix.isEmpty() && id.indexOf( trimSuffix ) > 0 )
			{
				id = id.substring( 0, id.indexOf( trimSuffix ) );
			}
		}
		catch( final Exception ex )
		{
			Log.error( SeqUtil.class, "Unable to extract Sample ID from: " + value, ex );
			throw ex;
		}

		return id;
	}

	/**
	 * Get all sequence header characters for fasta and fastq files.
	 * 
	 * @return List of sequence header 1st character options
	 * @throws Exception if unable to determine sequence type
	 */
	public static final List<String> getSeqHeaderChars() throws Exception
	{
		if( isFastA() )
		{
			return FASTA_HEADER_DELIMS;
		}
		else if( isFastQ() )
		{
			return Arrays.asList( new String[] { FASTQ_HEADER_DELIM } );
		}
		else
		{
			throw new Exception( "Sequence type undefined!" );
		}
	}

	/**
	 * Initialize Config params set by SeqUtil.
	 * 
	 * @throws Exception if runtime errors occur
	 */
	public static void initialize() throws Exception
	{
		if( requireSeqInput() )
		{
			initSeqParams();
			registerDemuxStatus();
			registerPairedReadStatus();

			if( MetaUtil.hasColumn( Config.getString( MetaUtil.META_FILENAME_COLUMN ) )
					&& ( Config.getString( Config.INPUT_TRIM_PREFIX ) != null
							|| Config.getString( Config.INPUT_TRIM_SUFFIX ) != null ) )
			{
				Log.warn( SeqUtil.class,
						"The properties " + Config.INPUT_TRIM_PREFIX + " and " + Config.INPUT_TRIM_SUFFIX
								+ " will be ignored. Samples will be matched to file names useing the \""
								+ Config.getString( MetaUtil.META_FILENAME_COLUMN ) + "\" column in the metadata." );
				Config.setConfigProperty( Config.INPUT_TRIM_PREFIX, "" );
				Config.setConfigProperty( Config.INPUT_TRIM_SUFFIX, "" );
			}

		}
		else
		{
			Config.setConfigProperty( INTERNAL_SEQ_TYPE, Config.requireString( MetaUtil.META_NULL_VALUE ) );
		}
	}

	/**
	 * Return TRUE if input files are in FastA format.
	 *
	 * @return TRUE if input files are in FastA format
	 * @throws Exception if unable to determine sequence type
	 */
	public static boolean isFastA() throws Exception
	{
		if( Config.requireString( INTERNAL_SEQ_TYPE ).equals( FASTA ) )
		{
			return true;
		}

		return false;
	}

	/**
	 * Return TRUE if input files are in FastQ format.
	 *
	 * @return TRUE if input files are in FastQ format
	 * @throws Exception if unable to determine sequence type
	 */
	public static boolean isFastQ() throws Exception
	{
		if( Config.requireString( INTERNAL_SEQ_TYPE ).equals( FASTQ ) )
		{
			return true;
		}

		return false;
	}

	/**
	 * Return TRUE if reads are unpaired or if name does not contain the reverse read file suffix:
	 * {@value biolockj.Config#INPUT_REVERSE_READ_SUFFIX}
	 *
	 * @param name Name of file
	 * @return TRUE for unpaired or forward reads
	 * @throws Exception if {@link biolockj.Config} properties are missing or invalid
	 */
	public static boolean isForwardRead( final String name ) throws Exception
	{
		final String suffix = Config.getString( Config.INPUT_REVERSE_READ_SUFFIX );
		if( suffix != null && name.contains( suffix ) )
		{
			return false;
		}

		return true;
	}

	/**
	 * Most pipelines have {@value #FASTA} or {@value #FASTQ} files in {@value biolockj.Config#INPUT_DIRS}. However,
	 * some pipeline {@value biolockj.Config#INPUT_DIRS} contain files that are not sequence files, such as from a
	 * {@link biolockj.module.implicit.parser.ParserModule} output directory. This method returns
	 * {@value biolockj.Config#TRUE} if a {@link biolockj.module.BioModule} is found from one of the 2 packages that
	 * require sequence data input: {@link biolockj.module.classifier} and {@link biolockj.module.seq}.
	 *
	 * @return boolean {@value biolockj.Config#TRUE} if classifier or seq {@link biolockj.module.BioModule} are found
	 * @throws Exception if errors occur
	 */
	public static boolean requireSeqInput() throws Exception
	{
		for( final String modName: Config.getList( Config.INTERNAL_BLJ_MODULE ) )
		{
			if( modName.startsWith( "biolockj.module.classifier" ) || modName.startsWith( "biolockj.module.seq" ) )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Return the DNA reverse compliment for the input dna parameter.
	 * 
	 * @param dna DNA base sequence
	 * @return DNA reverse complement
	 * @throws Exception if sequence contains a non-standard letter (only ACGT accepted)
	 */
	public static String reverseComplement( final String dna ) throws Exception
	{
		String out = "";
		for( int i = dna.length() - 1; i >= 0; --i )
		{
			final char curr = dna.charAt( i );
			if( curr == 'A' )
			{
				out += 'T';
			}
			else if( curr == 'T' )
			{
				out += 'A';
			}
			else if( curr == 'C' )
			{
				out += 'G';
			}
			else if( curr == 'G' )
			{
				out += 'C';
			}
			else
			{
				throw new Exception( "ERROR: Input is not a DNA Sequence: " + dna );
			}
		}
		Log.debug( SeqUtil.class, "Reverse compliment for:" + dna + " = " + out );
		return out;
	}

	/**
	 * Set {@value biolockj.Config#INPUT_IGNORE_FILES}, {@value #INTERNAL_SEQ_HEADER_CHAR} < {@value #INTERNAL_SEQ_TYPE}
	 * <ul>
	 * <li>Ignore the metadata file {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_FILE_PATH}
	 * <li>If {@link biolockj.Config} pipeline contains {@link biolockj.module.seq} or
	 * {@link biolockj.module.classifier} {@link biolockj.module.BioModule}s, ignore non-(fasta/fastq) files found in
	 * {@link biolockj.Config#INPUT_DIRS}
	 * </ul>
	 *
	 * @throws Exception if {@link biolockj.Config#INPUT_DIRS} undefined or file reader I/O Exception occurs
	 */
	protected static void initSeqParams() throws Exception
	{
		String foundFasta = null;
		String foundFastq = null;
		String testChar = null;
		for( final File f: BioLockJUtil.getBasicInputFiles() )
		{
			final BufferedReader reader = BioLockJUtil.getFileReader( f );
			try
			{
				String line = reader.readLine().trim();
				testChar = line.substring( 0, 1 );

				int numLines = 0;
				do
				{
					line = reader.readLine();
					if( line != null && !line.isEmpty() )
					{
						final String lineChar = line.trim().substring( 0, 1 );
						if( !lineChar.equals( "+" ) && !FASTA_HEADER_DELIMS.contains( lineChar )
								&& !lineChar.equals( FASTQ_HEADER_DELIM ) )
						{
							numLines++;
							if( numLines > 1 )
							{
								Log.debug( SeqUtil.class, f.getName() + " --> Line[" + numLines
										+ "] after TEST CHARACTER: " + testChar + " = #" + numLines );
							}
						}
						else
						{
							break;
						}
					}
				}
				while( line != null );

				Log.debug( SeqUtil.class, f.getAbsolutePath() + " TEST CHARACTER: " + testChar + ": " + numLines );

				if( numLines > 1 && Config.getString( Config.INTERNAL_IS_MULTI_LINE_SEQ ) == null
						&& ( FASTA_HEADER_DELIMS.contains( testChar ) || testChar.equals( FASTQ_HEADER_DELIM ) ) )
				{
					Log.info( SeqUtil.class, "Multi-line input file detected: # lines/seq: " + numLines );
					Config.setConfigProperty( Config.INTERNAL_IS_MULTI_LINE_SEQ, Config.TRUE );
					if( numMultiSeqLines != null && numMultiSeqLines == 0 )
					{
						numMultiSeqLines = numLines;
					}
					else if( numMultiSeqLines != null && numMultiSeqLines != numLines )
					{
						Log.warn( SeqUtil.class, "Multi-line input file has inconsistant number of lines per read" );
						numMultiSeqLines = null;
					}
				}
			}
			catch( final Exception ex )
			{
				throw new Exception( "Found invalid sequence file: " + f.getAbsolutePath() );
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
			}

			if( FASTA_HEADER_DELIMS.contains( testChar ) )
			{
				foundFasta = f.getAbsolutePath();
				Log.debug( SeqUtil.class, "detected FASTA: " + foundFasta );
			}
			else if( testChar.equals( FASTQ_HEADER_DELIM ) )
			{
				foundFastq = f.getAbsolutePath();
				Log.debug( SeqUtil.class, "detected FASTQ: " + foundFastq );
			}
			else
			{
				throw new Exception( "Invalid sequence file format (1st character = " + testChar + ") in: "
						+ f.getAbsolutePath() + BioLockJ.RETURN + "FASTA files must begin with either character: "
						+ BioLockJUtil.getCollectionAsString( FASTA_HEADER_DELIMS )
						+ " and FASTQ files must begin with \"" + FASTQ_HEADER_DELIM + "\"" );
			}
		}

		if( foundFasta != null && foundFastq != null )
		{
			throw new Exception( "Input files from: " + Config.INPUT_DIRS
					+ " must all be of a single type (FASTA or FASTQ)." + BioLockJ.RETURN + "FASTA file found: "
					+ foundFasta + BioLockJ.RETURN + "FASTQ file found: " + foundFastq );
		}

		if( testChar != null )
		{
			Config.setConfigProperty( INTERNAL_SEQ_HEADER_CHAR, testChar );
		}

		final String seqType = Config.getString( INTERNAL_SEQ_TYPE );
		if( seqType != null )
		{
			if( !seqType.toLowerCase().equals( FASTA ) && !seqType.toLowerCase().equals( FASTQ ) )
			{
				throw new ConfigFormatException( INTERNAL_SEQ_TYPE,
						"Must be configured to " + FASTA + " or " + FASTQ + "." );
			}
		}
		else if( foundFasta != null || seqType != null && seqType.toLowerCase().equals( FASTA ) )
		{
			Config.setConfigProperty( INTERNAL_SEQ_TYPE, FASTA );

		}
		else if( foundFastq != null || seqType != null && seqType.toLowerCase().equals( FASTQ ) )
		{
			Config.setConfigProperty( INTERNAL_SEQ_TYPE, FASTQ );
		}
		else
		{
			Config.setConfigProperty( INTERNAL_SEQ_TYPE, Config.requireString( MetaUtil.META_NULL_VALUE ) );
			Config.setConfigProperty( INTERNAL_SEQ_HEADER_CHAR, Config.requireString( MetaUtil.META_NULL_VALUE ) );
		}
	}

	/**
	 * Inspect the pipeline input files to determine if input includes paired reads.
	 * 
	 * @throws Exception if unable to determine if reads are paired
	 */
	protected static void registerPairedReadStatus() throws Exception
	{
		boolean foundPairedReads = false;
		if( Config.getBoolean( Config.INTERNAL_MULTIPLEXED ) )
		{
			if( BioLockJUtil.getPipelineInputFiles().size() > 1 )
			{
				foundPairedReads = true;
			}
			else
			{
				boolean foundFw = false;
				boolean foundRv = false;
				final BufferedReader reader = BioLockJUtil
						.getFileReader( BioLockJUtil.getPipelineInputFiles().get( 0 ) );
				try
				{
					Log.info( SeqUtil.class, "Reading multiplexed file to check for paired reads: "
							+ BioLockJUtil.getPipelineInputFiles().get( 0 ).getAbsolutePath() );
					final List<String> seqLines = new ArrayList<>();
					for( String line = reader.readLine(); line != null; line = reader.readLine() )
					{
						seqLines.add( line );
						if( seqLines.size() == SeqUtil.getNumLinesPerRead() )
						{
							if( seqLines.get( 0 ).contains( ILLUMINA_FW_READ_IND ) )
							{
								foundFw = true;
							}
							else if( seqLines.get( 0 ).contains( ILLUMINA_RV_READ_IND ) )
							{
								foundRv = true;
							}
							else
							{
								Log.warn( SeqUtil.class,
										"Invalid header, no direction indicator: " + seqLines.get( 0 ) );
							}

							seqLines.clear();
						}
					}
				}
				finally
				{
					if( reader != null )
					{
						reader.close();
					}
				}

				foundPairedReads = foundFw && foundRv;
				if( foundRv && !foundFw )
				{
					throw new Exception( "Invalid dataset.  Found only reverse reads in mutliplexed file!"
							+ "Reverse read indicator found in headers: " + ILLUMINA_RV_READ_IND
							+ "All datasets must contain forward read indicators in the headers: "
							+ ILLUMINA_FW_READ_IND );
				}
			}
		}
		else // not multiplexed
		{
			foundPairedReads = !getPairedReads( BioLockJUtil.getPipelineInputFiles() ).isEmpty();
		}

		Log.info( SeqUtil.class,
				"Set " + Config.INTERNAL_PAIRED_READS + "=" + ( foundPairedReads ? Config.TRUE: Config.FALSE ) );
		Config.setConfigProperty( Config.INTERNAL_PAIRED_READS, foundPairedReads ? Config.TRUE: Config.FALSE );
	}

	/**
	 * Unpaired reads must be multiplexed into a single file. Multiplexed paired reads must be contained in either 1
	 * file, or 2 (1 file with forward reads and 1 file with reverse reads). Based on the number of files and file
	 * names, we determine if the data is multiplexed. This function can be overridden by the Config property
	 * {@value biolockj.module.implicit.Demultiplexer#INPUT_DO_NOT_DEMUX} to return false regardless of input file data.
	 *
	 * @throws Exception if validation fails
	 */
	private static void registerDemuxStatus() throws Exception
	{
		boolean isMultiplexed = false;

		if( !DemuxUtil.doDemux() )
		{
			Log.debug( SeqUtil.class, "Do not demux!" );
		}
		else if( BioLockJUtil.getBasicInputFiles().size() == 1 )
		{
			Log.info( SeqUtil.class, "Must demultiplex data!  Found exactly 1 input file" );
			isMultiplexed = true;
		}
		else if( BioLockJUtil.getBasicInputFiles().size() == 2 )
		{
			boolean foundFw = false;
			boolean foundRv = false;
			final String fwRead = Config.getString( Config.INPUT_FORWARD_READ_SUFFIX );
			final String rvRead = Config.getString( Config.INPUT_REVERSE_READ_SUFFIX );
			for( final File file: BioLockJUtil.getBasicInputFiles() )
			{
				if( fwRead != null && file.getName().contains( fwRead ) )
				{
					foundFw = true;
				}
				else if( rvRead != null && file.getName().contains( rvRead ) )
				{
					foundRv = true;
				}
			}

			if( foundFw && foundRv )
			{
				Log.info( SeqUtil.class, "Must demultiplex data!  Found exactly 1 set of paired input files" );
				isMultiplexed = true;
			}
		}

		Config.setConfigProperty( Config.INTERNAL_MULTIPLEXED, isMultiplexed ? Config.TRUE: Config.FALSE );

		if( isMultiplexed && numMultiSeqLines == null )
		{
			throw new Exception( "Multi-line sequence files must be demultiplexed before analyzed by BioLockJ" );
		}
	}

	/**
	 * File extension for fasta files = .fasta
	 */
	public static final String FASTA = "fasta";

	/**
	 * Default 1st character for a FASTA file: "greater-than-sign"
	 */
	public static final String FASTA_HEADER_DEFAULT_DELIM = ">";

	/**
	 * List of acceptable 1st characters for a FASTA file
	 */
	public static final List<String> FASTA_HEADER_DELIMS = Arrays.asList( new String[] { ">", ";" } );

	/**
	 * File extension for fastq files: {@value #FASTQ}
	 */
	public static final String FASTQ = "fastq";

	/**
	 * Only acceptable 1st character for a FASTQ file: {@value #FASTQ_HEADER_DELIM}
	 */
	public static final String FASTQ_HEADER_DELIM = "@";

	/**
	 * Illumina forward read indicator found in sequence header
	 */
	public static final String ILLUMINA_FW_READ_IND = " 1:N:";

	/**
	 * Illumina reverse read indicator found in sequence header
	 */
	public static final String ILLUMINA_RV_READ_IND = " 2:N:";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #INPUT_REQUIRE_COMPLETE_PAIRS}<br>
	 * Require 100% sequence input files are matching paired reads
	 */
	public static final String INPUT_REQUIRE_COMPLETE_PAIRS = "input.requireCompletePairs";

	/**
	 * {@link biolockj.Config} property: {@value #INTERNAL_SEQ_HEADER_CHAR}<br>
	 * The property holds the 1st character used in the sequence header for the given dataset
	 */
	public static final String INTERNAL_SEQ_HEADER_CHAR = "internal.seqHeaderChar";

	/**
	 * {@link biolockj.Config} Internal property: {@value #INTERNAL_SEQ_TYPE}<br>
	 * The sequence type requires either {@value #FASTA} or {@value #FASTQ}<br>
	 * System will auto-detect if not configured
	 */
	public static final String INTERNAL_SEQ_TYPE = "internal.seqType";

	private static Integer numMultiSeqLines = 0;

}
