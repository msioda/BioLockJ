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
	 * {@value biolockj.Config#PROJECT_PIPELINE_DIR}/input.
	 *
	 * @throws Exception if unable to copy the files
	 */
	public static void copyInputData() throws Exception
	{
		final File inputFileDir = new File(
				Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath() + File.separator + "input" );

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
	 * Return regex version of IUPAC DNA substitution bases so that only ACGT values are used. For example, if base =
	 * "Y", return "[CT]", if base = "A", return "A".<br>
	 * Function returns base in the same case as the input parameter.
	 * 
	 * @param base DNA Base
	 * @return
	 */
	public static String getIupacBase( final String base )
	{
		final String upperCaseBase = base.toUpperCase();
		if( DNA_BASE_MAP.keySet().contains( upperCaseBase ) )
		{
			String regexBase = DNA_BASE_MAP.get( upperCaseBase );
			if( !base.equals( upperCaseBase ) )
			{
				regexBase = regexBase.toLowerCase();
			}
			return regexBase;
		}
		return base;
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
	 * @throws Exception if other errors occur
	 */
	public static Map<File, File> getPairedReads( final Collection<File> files ) throws Exception
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
				map.put( fwRead, rvRead );
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

		if( Config.getString( INTERNAL_PAIRED_READS ) != null && Config.getBoolean( INPUT_REQUIRE_COMPLETE_PAIRS )
				&& !msg.isEmpty() )
		{
			throw new ConfigViolationException( INPUT_REQUIRE_COMPLETE_PAIRS, msg );
		}
		else if( Config.getString( INTERNAL_PAIRED_READS ) != null && !msg.isEmpty() )
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
		if( Config.getBoolean( INTERNAL_PAIRED_READS ) )
		{
			if( SeqUtil.isForwardRead( fileName ) )
			{
				return Config.requireString( INPUT_FORWARD_READ_SUFFIX );
			}
			else
			{
				return Config.requireString( INPUT_REVERSE_READ_SUFFIX );
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
			final String fileNameCol = Config.getString( MetaUtil.META_FILENAME_COLUMN );
			if( MetaUtil.hasColumn( fileNameCol ) )
			{
				int ind;
				if( isForwardRead( value ) )
				{
					ind = MetaUtil.getFieldValues( fileNameCol, false ).indexOf( value );
				}

				else
				{
					// if this file name is a reverse file, look up the corresponding forward file name.
					final String fwReadSuffix = Config.getString( INPUT_FORWARD_READ_SUFFIX );
					final String rvReadSuffix = Config.getString( INPUT_REVERSE_READ_SUFFIX );
					revValue = value.substring( 0, value.lastIndexOf( rvReadSuffix ) ) + fwReadSuffix
							+ value.substring( value.lastIndexOf( rvReadSuffix ) + rvReadSuffix.length() );
					Log.debug( SeqUtil.class, value + " is a reverse read. Seeking sample id for file: " + revValue );
					ind = MetaUtil.getFieldValues( fileNameCol, false ).indexOf( revValue );
				}
				if( ind == -1 )
				{
					Log.info( SeqUtil.class, "Filename [" + ( isForwardRead( value ) ? value: revValue )
							+ "] does not appear in column [" + fileNameCol + "]. This file will be ignored." );
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
				if( isGzipped( id ) )
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
			if( !Config.getBoolean( INTERNAL_MULTIPLEXED ) ) // must be a file name
			{
				final String fwReadSuffix = Config.getString( INPUT_FORWARD_READ_SUFFIX );
				final String rvReadSuffix = Config.getString( INPUT_REVERSE_READ_SUFFIX );
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
			final String trimPrefix = Config.getString( INPUT_TRIM_PREFIX );
			final String trimSuffix = Config.getString( INPUT_TRIM_SUFFIX );
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
	 * Return only sequence files for sample IDs found in the metadata file.<br>
	 * If {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_REQUIRED} = {@value biolockj.Config#TRUE}, an
	 * error is thrown to list the files that cannot be matched to a metadata row.
	 * 
	 * @param files List of input files
	 * @return Module input sequence files
	 * @throws Exception if no input files are found
	 */
	public static List<File> getSeqFiles( final Collection<File> files ) throws Exception
	{
		final List<File> seqFiles = new ArrayList<>();
		final List<File> seqsWithoutMetaId = new ArrayList<>();
		for( final File file: files )
		{
			if( MetaUtil.getSampleIds().contains( SeqUtil.getSampleId( file.getName() ) ) )
			{
				seqFiles.add( file );
			}
			else if( Config.getBoolean( MetaUtil.META_REQUIRED ) ) // metadata required
			{
				seqsWithoutMetaId.add( file );
			}
			else
			{
				Log.warn( SeqUtil.class, "Ignoring input file not found in metadata because Config property [ "
						+ MetaUtil.META_REQUIRED + "=" + Config.FALSE + " ]: " + file.getAbsolutePath() );
			}
		}

		if( !seqsWithoutMetaId.isEmpty() && Config.getBoolean( MetaUtil.META_REQUIRED ) )
		{
			throw new ConfigViolationException( MetaUtil.META_REQUIRED, "No metadata found for the following files: "
					+ BioLockJ.RETURN + BioLockJUtil.printLongFormList( seqsWithoutMetaId ) );
		}

		return seqFiles;
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
		// 1st call to BioLockJUtil.getPipelineInputFiles() initializes PIPELINE_SEQ_INPUT_TYPE
		BioLockJUtil.getPipelineInputFiles();
		if( piplineHasSeqInput() )
		{
			initSeqParams();
			registerDemuxStatus();
			registerPairedReadStatus();

			if( mapSampleIdWithMetaFileNameCol() )
			{
				final String colName = Config.requireString( MetaUtil.META_FILENAME_COLUMN );
				final int numVals = MetaUtil.getFieldValues( colName, true ).size();
				final int numUniqueVals = MetaUtil.getUniqueFieldValues( colName, true ).size();
				if( numVals != numUniqueVals )
				{
					throw new ConfigViolationException( colName, "File paths must be unique for this metadata column: "
							+ colName + numUniqueVals + " unique values found in " + numVals + " non-null records." );
				}

				DemuxUtil.clearDemuxConfig();
				Config.setConfigProperty( INPUT_TRIM_PREFIX, "" );
				Config.setConfigProperty( INPUT_TRIM_SUFFIX, "" );

			}
			else
			{
				Config.setConfigProperty( MetaUtil.META_FILENAME_COLUMN, "" );
			}
		}
		else
		{
			Config.setConfigProperty( MetaUtil.META_FILENAME_COLUMN, "" );
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
		final String suffix = Config.getString( INPUT_REVERSE_READ_SUFFIX );
		if( suffix != null && name.contains( suffix ) )
		{
			return false;
		}

		return true;
	}

	/**
	 * Determine if file is gzipped based on its file extension.<br>
	 * Any file ending with {@value biolockj.BioLockJ#GZIP_EXT} is treated as a gzipped file.
	 * 
	 * @param fileName File name
	 * @return TRUE if file name ends with .gz
	 */
	public static boolean isGzipped( final String fileName )
	{
		return fileName != null && fileName.toLowerCase().endsWith( BioLockJ.GZIP_EXT );
	}

	/**
	 * Verify 1st character of sequence header and mask 1st sequence for valid DNA/RNA bases "acgtu"
	 * 
	 * @param file File
	 * @return TRUE if file is a squence file
	 * @throws Exception if errors occur
	 */
	public static boolean isSeqFile( final File file ) throws Exception
	{
		Log.warn( SeqUtil.class, "Check if input file is a SEQ file: " + file.getAbsolutePath() );
		boolean isSeq = false;
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try
		{
			final String header = SeqUtil.scanFirstLine( reader, file );
			final String idChar = header.substring( 0, 1 );
			String seq = reader.readLine();
			Log.info( SeqUtil.class, "header: " + header );
			Log.info( SeqUtil.class, "idChar: " + idChar );
			Log.info( SeqUtil.class, "seq: " + seq );
			if( FASTA_HEADER_DELIMS.contains( idChar ) || idChar.equals( FASTQ_HEADER_DELIM ) )
			{
				if( seq != null && !seq.trim().isEmpty() )
				{
					seq = seq.trim().toLowerCase().replaceAll( "a", "" ).replaceAll( "c", "" ).replaceAll( "g", "" )
							.replaceAll( "t", "" ).replaceAll( "n", "" );
					Log.debug( SeqUtil.class, "After removing acgtu from the seq (what is left?) ---> " + seq );
					Log.debug( SeqUtil.class, "Is seq empty? ---> " + seq.isEmpty() );
					Log.debug( SeqUtil.class, "Is seq.trim() empty? ---> " + seq.trim().isEmpty() );
					isSeq = seq.trim().isEmpty();
				}
			}
			else
			{
				Log.info( SeqUtil.class, file.getAbsolutePath() + " is not a sequence file! " + BioLockJ.RETURN
						+ "Line 1: [ " + header + " ]" + BioLockJ.RETURN + "Line 2= [ " + seq + " ]" );
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}
		return isSeq;
	}

	/**
	 * Return TRUE if pipeline input files are sequence files.
	 * 
	 * @return TRUE if pipeline input files are sequence files.
	 * @throws Exception if errors occur
	 */
	public static boolean piplineHasSeqInput() throws Exception
	{
		return BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_SEQ_INPUT_TYPE );
	}

	/**
	 * Remove ignored and empty files from the input files.
	 * 
	 * @param files Collection of files
	 * @return valid files
	 * @throws Exception if errors occur
	 */
	public static List<File> removeIgnoredAndEmptyFiles( final Collection<File> files ) throws Exception
	{
		final List<File> validInputFiles = new ArrayList<>();
		for( final File file: files )
		{
			final boolean isEmpty = FileUtils.sizeOf( file ) < 1L;
			if( isEmpty )
			{
				Log.warn( SeqUtil.class, "Skip empty file: " + file.getAbsolutePath() );
			}
			else if( Config.getSet( INPUT_IGNORE_FILES ).contains( file.getName() ) )
			{
				Log.debug( SeqUtil.class, "Ignore file " + file.getAbsolutePath() );
			}
			else
			{
				validInputFiles.add( file );
			}
		}
		return validInputFiles;
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
	 * This method returns the 1st non-empty line and moves the BufferedReader pointer to this line. By passing the
	 * reader, subsequent calls to reader.readLine() will pick up from here.
	 * 
	 * @param reader BufferedReader reader for sequence file
	 * @param file Sequence file read by the BufferedReader (used for log and error messages)
	 * @return String First non-empty line
	 * @throws Exception if errors occur
	 */
	public static String scanFirstLine( final BufferedReader reader, final File file ) throws Exception
	{
		Integer i = 0;
		String line = reader.readLine();
		while( line != null && line.isEmpty() )
		{
			i++;
			line = reader.readLine();
		}

		if( line == null )
		{
			throw new Exception( "Input dir contains empty file: " + file.getAbsolutePath() );
		}
		if( i != 0 )
		{
			Log.warn( SeqUtil.class, "Skipped [ " + i + " ] empty lines at the top of ---> " + file.getAbsolutePath() );
		}

		return line.trim();
	}

	/**
	 * Set {@value biolockj.Config#INPUT_IGNORE_FILES}, {@value #INTERNAL_SEQ_HEADER_CHAR}, and
	 * {@value #INTERNAL_SEQ_TYPE}
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
		String headerChar = null;
		for( final File f: BioLockJUtil.getPipelineInputFiles() )
		{
			final BufferedReader reader = BioLockJUtil.getFileReader( f );
			try
			{
				String line = scanFirstLine( reader, f );
				int numSeqLines = 0;
				while( line != null )
				{
					line = line.trim();
					if( headerChar == null )
					{
						headerChar = line.substring( 0, 1 ); // set only once
						Log.debug( SeqUtil.class, " --> 1st headerChar = " + headerChar );
					}
					else
					{
						Log.debug( SeqUtil.class, f.getName() + " --> Line[" + ( numSeqLines + 1 ) + "] = " + line );
						final String testChar = line.trim().substring( 0, 1 );
						if( !testChar.equals( headerChar ) && !testChar.equals( "+" ) )
						{
							numSeqLines++;
							if( numSeqLines > 1 )
							{
								Log.warn( SeqUtil.class, f.getName() + " --> Line [ " + ( numSeqLines + 1 )
										+ " ] after TEST CHARACTER: " + headerChar + " = #seq lines = " + numSeqLines );
							}
						}
						else
						{
							if( numSeqLines == 0 )
							{
								numSeqLines++;
							}
							break;
						}
					}
					line = reader.readLine();
				}

				Log.info( SeqUtil.class, f.getAbsolutePath() + " --> #lines/read: " + ( numSeqLines + 1 ) );
				if( numSeqLines > 1 && Config.getString( INTERNAL_IS_MULTI_LINE_SEQ ) == null
						&& ( FASTA_HEADER_DELIMS.contains( headerChar ) || headerChar.equals( FASTQ_HEADER_DELIM ) ) )
				{
					Log.info( SeqUtil.class, "Multi-line input file detected: # lines/seq: " + numSeqLines );
					Config.setConfigProperty( INTERNAL_IS_MULTI_LINE_SEQ, Config.TRUE );
					if( numMultiSeqLines != null && numMultiSeqLines == 0 )
					{
						numMultiSeqLines = numSeqLines;
					}
					else if( numMultiSeqLines != null && numMultiSeqLines != numSeqLines )
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

			if( FASTA_HEADER_DELIMS.contains( headerChar ) )
			{
				foundFasta = f.getAbsolutePath();
				Log.debug( SeqUtil.class, "detected FASTA: " + foundFasta );
			}
			else if( headerChar.equals( FASTQ_HEADER_DELIM ) )
			{
				foundFastq = f.getAbsolutePath();
				Log.debug( SeqUtil.class, "detected FASTQ: " + foundFastq );
			}
			else
			{
				throw new Exception( "Invalid sequence file format (1st character = " + headerChar + ") in: "
						+ f.getAbsolutePath() + BioLockJ.RETURN + "FASTA files must begin with either character: "
						+ BioLockJUtil.getCollectionAsString( FASTA_HEADER_DELIMS )
						+ " and FASTQ files must begin with \"" + FASTQ_HEADER_DELIM + "\"" );
			}
		}

		if( foundFasta != null && foundFastq != null )
		{
			throw new Exception( "Input files from: " + INPUT_DIRS + " must all be of a single type (FASTA or FASTQ)."
					+ BioLockJ.RETURN + "FASTA file found: " + foundFasta + BioLockJ.RETURN + "FASTQ file found: "
					+ foundFastq );
		}

		if( headerChar != null )
		{
			Config.setConfigProperty( INTERNAL_SEQ_HEADER_CHAR, headerChar );
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
		if( Config.getBoolean( INTERNAL_MULTIPLEXED ) )
		{
			if( BioLockJUtil.getPipelineInputFiles().size() > 1 )
			{
				foundPairedReads = true;
			}
			else
			{
				boolean foundFw = false;
				boolean foundRv = false;
				final File testFile = BioLockJUtil.getPipelineInputFiles().iterator().next();
				final BufferedReader reader = BioLockJUtil.getFileReader( testFile );
				try
				{
					Log.info( SeqUtil.class,
							"Reading multiplexed file to check for paired reads: " + testFile.getAbsolutePath() );
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
				"Set " + INTERNAL_PAIRED_READS + "=" + ( foundPairedReads ? Config.TRUE: Config.FALSE ) );
		Config.setConfigProperty( INTERNAL_PAIRED_READS, foundPairedReads ? Config.TRUE: Config.FALSE );
	}

	private static boolean mapSampleIdWithMetaFileNameCol() throws Exception
	{
		final String metaCol = Config.getString( MetaUtil.META_FILENAME_COLUMN );
		return metaCol != null && MetaUtil.hasColumn( metaCol ) && !MetaUtil.getFieldValues( metaCol, true ).isEmpty();
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
		final int count = BioLockJUtil.getPipelineInputFiles().size();
		Log.info( SeqUtil.class, "Register Demux Status for: " + count + " input files." );
		boolean isMultiplexed = false;
		if( DemuxUtil.doDemux() != null && !DemuxUtil.doDemux() )
		{
			Log.debug( SeqUtil.class, "Do not demux!" );
			Log.info( SeqUtil.class, "TEMP INFO --> Do not demux!" );
		}
		else if( count == 1 )
		{
			Log.info( SeqUtil.class, "Must demultiplex data!  Found exactly 1 input file" );
			isMultiplexed = true;
		}
		else if( count == 2 )
		{

			boolean foundFw = false;
			boolean foundRv = false;
			final String fwRead = Config.getString( INPUT_FORWARD_READ_SUFFIX );
			final String rvRead = Config.getString( INPUT_REVERSE_READ_SUFFIX );
			final Set<String> suffixes = new HashSet<>();
			if( fwRead != null )
			{
				suffixes.add( fwRead );
			}
			if( rvRead != null )
			{
				suffixes.add( rvRead );
			}
			Log.info( SeqUtil.class, "Checking 2 input files for paired read suffix values: " + suffixes );
			for( final File file: BioLockJUtil.getPipelineInputFiles() )
			{
				if( fwRead != null && file.getName().contains( fwRead ) )
				{
					Log.info( SeqUtil.class, "Found forward read: " + file.getName() );
					foundFw = true;
				}
				else if( rvRead != null && file.getName().contains( rvRead ) )
				{
					Log.info( SeqUtil.class, "Found reverse read: " + file.getName() );
					foundRv = true;
				}
			}

			if( foundFw && foundRv )
			{
				Log.info( SeqUtil.class, "Must demultiplex data!  Found exactly 1 set of paired input files" );
				isMultiplexed = true;
			}
		}

		Config.setConfigProperty( INTERNAL_MULTIPLEXED, isMultiplexed ? Config.TRUE: Config.FALSE );

		if( isMultiplexed && numMultiSeqLines == null )
		{
			throw new Exception(
					"Multi-line sequence files must be demultiplexed before analyzed by BioLockJ because the number of lines per read is inconsistant." );
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
	 * {@link biolockj.Config} List property: {@value #INPUT_DIRS}<br>
	 * Set sequence file directories
	 */
	public static final String INPUT_DIRS = "input.dirPaths";

	/**
	 * {@link biolockj.Config} String property: {@value #INPUT_FORWARD_READ_SUFFIX}<br>
	 * Set file suffix used to identify forward reads in {@value #INPUT_DIRS}
	 */
	public static final String INPUT_FORWARD_READ_SUFFIX = "input.suffixFw";

	/**
	 * {@link biolockj.Config} List property: {@value #INPUT_IGNORE_FILES}<br>
	 * Set file names to ignore if found in {@value #INPUT_DIRS}
	 */
	public static final String INPUT_IGNORE_FILES = "input.ignoreFiles";

	/**
	 * {@link biolockj.Config} String property: {@value #INPUT_REVERSE_READ_SUFFIX}<br>
	 * Set file suffix used to identify forward reads in {@value #INPUT_DIRS}
	 */
	public static final String INPUT_REVERSE_READ_SUFFIX = "input.suffixRv";

	/**
	 * {@link biolockj.Config} String property: {@value #INPUT_TRIM_PREFIX}<br>
	 * Set value of prefix to trim from sequence file names or headers to obtain Sample ID.
	 */
	public static final String INPUT_TRIM_PREFIX = "input.trimPrefix";

	/**
	 * {@link biolockj.Config} String property: {@value #INPUT_TRIM_SUFFIX}<br>
	 * Set value of suffix to trim from sequence file names or headers to obtain Sample ID.
	 */
	public static final String INPUT_TRIM_SUFFIX = "input.trimSuffix";

	/**
	 * {@link biolockj.Config} Internal Boolean property: {@value #INTERNAL_IS_MULTI_LINE_SEQ}<br>
	 * Store TRUE if {@link biolockj.util.SeqUtil} determines input sequences are multi-line format.
	 */
	public static final String INTERNAL_IS_MULTI_LINE_SEQ = "internal.isMultiLineSeq";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #INTERNAL_MULTIPLEXED}<br>
	 * Set to true if multiplexed reads are found, set by the application runtime code.
	 */
	public static final String INTERNAL_MULTIPLEXED = "internal.multiplexed";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #INTERNAL_PAIRED_READS}<br>
	 * Set to true if paired reads are found, set by the application runtime code.
	 */
	public static final String INTERNAL_PAIRED_READS = "internal.pairedReads";

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

	/**
	 * {@link biolockj.Config} Boolean property: {@value #INPUT_REQUIRE_COMPLETE_PAIRS}<br>
	 * Require 100% sequence input files are matching paired reads
	 */
	protected static final String INPUT_REQUIRE_COMPLETE_PAIRS = "input.requireCompletePairs";

	private static final Map<String, String> DNA_BASE_MAP = new HashMap<>();

	private static Integer numMultiSeqLines = 0;

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
