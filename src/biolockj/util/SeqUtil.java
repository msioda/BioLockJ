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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.classifier.ClassifierModule;

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
			FileUtils.forceDelete( inputFileDir );
		}

		for( final File dir: getInputDirs() )
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
	public static long countNumReads( final File seqFile ) throws Exception
	{
		long count = 0L;
		final BufferedReader r = getFileReader( seqFile );
		for( String line = r.readLine(); line != null; line = r.readLine() )
		{
			count++;
		}
		r.close();

		return count / getNumLinesPerRead();
	}

	/**
	 * Get a {@link BufferedReader} for standard text file or {@link GZIPInputStream} for gzipped files ending in ".gz"
	 *
	 * @param file to be read
	 * @return {@link BufferedReader} or {@link GZIPInputStream} if file is gzipped
	 * @throws FileNotFoundException if file does not exist
	 * @throws IOException if unable to read or write the file
	 */
	public static BufferedReader getFileReader( final File file ) throws FileNotFoundException, IOException
	{
		return file.getName().toLowerCase().endsWith( ".gz" )
				? new BufferedReader( new InputStreamReader( new GZIPInputStream( new FileInputStream( file ) ) ) )
				: new BufferedReader( new FileReader( file ) );
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
		final BufferedReader reader = getFileReader( seq );
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
	 * Get the  list of input directories for the pipeline.
	 * 
	 * @return List of system directory file paths
	 * @throws Exception if errors occur
	 */
	public static List<File> getInputDirs() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			final List<File> dirs = new ArrayList<>();
			final File dir = new File( DockerUtil.CONTAINER_INPUT_DIR );
			if( !dir.exists() )
			{
				throw new Exception( "Container missing mapped input volume system path: " + dir.getAbsolutePath() );
			}
			dirs.add( dir );
			return dirs;
		}
		return Config.requireExistingDirs( Config.INPUT_DIRS );
	}

	/**
	 * Determine if seqs are fasta or fastq by checking input file format (check 1st character only).
	 *
	 * @return {@value #FASTA} or {@value #FASTQ}
	 * @throws Exception if unable to determine sequence type
	 */
	public static String getInputSequenceType() throws Exception
	{
		if( Config.getString( INTERNAL_SEQ_TYPE ) == null )
		{
			Log.info( SeqUtil.class, "Cache 1st character in 1st sequence header to " + INTERNAL_SEQ_HEADER_CHAR
					+ " & cache the sequence type (fasta/fastq) to " + INTERNAL_SEQ_TYPE );
			String foundFasta = null;
			String foundFastq = null;
			String headerChar = null;

			for( final File f: getPipelineInputFiles() )
			{
				final BufferedReader reader = getFileReader( f );
				headerChar = reader.readLine().trim().substring( 0, 1 );
				Log.debug( SeqUtil.class, "First character of test input sequence file: " + headerChar );
				if( FASTA_HEADER_DELIMS.contains( headerChar ) )
				{
					foundFasta = f.getAbsolutePath();
				}
				else if( headerChar.equals( FASTQ_HEADER_DELIM ) )
				{
					foundFastq = f.getAbsolutePath();
				}
			}

			if( foundFasta != null && foundFastq != null )
			{
				throw new Exception( "Input files from: " + Config.INPUT_DIRS
						+ " must all be of a single type (FASTA or FASTQ)." + BioLockJ.RETURN + "FASTA file found: "
						+ foundFasta + BioLockJ.RETURN + "FASTQ file found: " + foundFastq );
			}

			Config.setConfigProperty( INTERNAL_SEQ_HEADER_CHAR, headerChar );

			if( foundFasta != null )
			{
				Config.setConfigProperty( INTERNAL_SEQ_TYPE, FASTA );

			}
			else if( foundFastq != null )
			{
				Config.setConfigProperty( INTERNAL_SEQ_TYPE, FASTQ );
			}
			else if( foundClassifier() )
			{
				throw new Exception( "Sequence type cannot be determined!" );
			}

			if( Config.getString( INTERNAL_SEQ_TYPE ) == null && !foundClassifier() )
			{
				Log.warn( SeqUtil.class, INTERNAL_SEQ_HEADER_CHAR
						+ " set to NA - no classifier found so input files do not have to be sequence files." );
				Config.setConfigProperty( INTERNAL_SEQ_TYPE, Config.requireString( MetaUtil.META_NULL_VALUE ) );
				Config.setConfigProperty( INTERNAL_SEQ_HEADER_CHAR, Config.requireString( MetaUtil.META_NULL_VALUE ) );
			}
		}

		return Config.getString( INTERNAL_SEQ_TYPE );
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
	 * @throws Exception if unable to find paired reads
	 */
	public static Map<File, File> getPairedReads( final Collection<File> files ) throws Exception
	{
		Log.debug( SeqUtil.class, "Looking for paired reads in " + ( files == null ? 0: files.size() ) + " files " );
		final Map<File, File> map = new HashMap<>();
		for( final File fwRead: files )
		{
			final String name = fwRead.getName();

			if( !isForwardRead( name ) )
			{
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
				Log.debug( SeqUtil.class, "Missing paired read!  Unpaired foward read = " + fwRead.getAbsolutePath() );
			}
		}

		return map;
	}

	/**
	 * Recursively get files located in the directories listed in
	 * {@link biolockj.Config}.{@value biolockj.Config#INPUT_DIRS} after removing
	 * {@link biolockj.Config}.{@value biolockj.Config#INPUT_IGNORE_FILES}
	 * 
	 * @return Pipeline input files
	 * @throws Exception if no input files are found
	 */
	public static List<File> getPipelineInputFiles() throws Exception
	{
		if( filteredInputFiles.isEmpty() )
		{
			for( final File file: getBasicInputFiles() )
			{
				if( !hasSeqInput() || Config.getBoolean( Config.INTERNAL_MULTIPLEXED ) || MetaUtil.getMetadata() == null
						|| MetaUtil.getSampleIds().contains( SeqUtil.getSampleId( file.getName() ) ) )
				{
					filteredInputFiles.add( file );
				}
				else if( Config.getBoolean( MetaUtil.META_REQUIRE_METADATA ) ) // not multiplexed, has meta, has seqs,
																				// ID not found in meta
				{
					throw new Exception( "Sample ID not found in metadata file: " + file.getAbsolutePath() );
				}
				else // not multiplexed, has seq input, metadata record not required
				{

					Log.warn( SeqUtil.class, "Ignoring input file not found in metadata because Config property [ "
							+ MetaUtil.META_REQUIRE_METADATA + "=" + Config.FALSE + " ]: " + file.getAbsolutePath() );
				}
			}

			if( filteredInputFiles.isEmpty() )
			{
				throw new Exception( "No valid files found in: " + Config.INPUT_DIRS );
			}
		}

		return filteredInputFiles;
	}

	/**
	 * Method extracts Sample ID from the name param. Possibly input is a file name so remove file extensions. If
	 * demultiplexing (RDP/Kraken support this option), input is a sequence header
	 *
	 * @param value File name or sequence header
	 * @return Sample ID
	 * @throws Exception if unable to determine Sample ID
	 */
	public static String getSampleId( final String value ) throws Exception
	{
		String id = value;
		try
		{
			final String fwReadSuffix = Config.getString( Config.INPUT_FORWARD_READ_SUFFIX );
			final String rvReadSuffix = Config.getString( Config.INPUT_REVERSE_READ_SUFFIX );
			final String trimPrefix = Config.getString( Config.INPUT_TRIM_PREFIX );
			final String trimSuffix = Config.getString( Config.INPUT_TRIM_SUFFIX );

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

			if( !Config.requireBoolean( Config.INTERNAL_MULTIPLEXED ) ) // must be a file
			{
				if( fwReadSuffix != null && isForwardRead( id ) && id.lastIndexOf( fwReadSuffix ) > 0 )
				{
					id = id.substring( 0, id.lastIndexOf( fwReadSuffix ) );
				}
				else if( rvReadSuffix != null && id.lastIndexOf( rvReadSuffix ) > 0 )
				{
					id = id.substring( 0, id.lastIndexOf( rvReadSuffix ) );
				}
			}

			if( trimPrefix != null && trimPrefix.length() > 0 && id.indexOf( trimPrefix ) > -1 )
			{
				id = id.substring( trimPrefix.length() + id.indexOf( trimPrefix ) );
			}

			if( trimSuffix != null && trimSuffix.length() > 0 && id.indexOf( trimSuffix ) > 0 )
			{
				id = id.substring( 0, id.indexOf( trimSuffix ) );
			}

		}
		catch( final Exception ex )
		{
			Log.error( SeqUtil.class, "Unable to get SampleID from: " + value, ex );
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
		initIgnoreFiles();
		registerDemuxStatus();
		getInputSequenceType();
		registerPairedReadStatus();

		if( !RuntimeParamUtil.isDirectMode() )
		{
			printCollection( Config.getSet( Config.INPUT_IGNORE_FILES ), "Ignore Module Input Files" );
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
		if( getInputSequenceType().equals( FASTA ) )
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
		if( getInputSequenceType().equals( FASTQ ) )
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
	 * Set {@link biolockj.Config#INPUT_IGNORE_FILES}:
	 * <ul>
	 * <li>Ignore the metadata file {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_FILE_PATH}
	 * <li>If {@link biolockj.Config} pipeline contains {@link biolockj.module.seq} or
	 * {@link biolockj.module.classifier} {@link biolockj.module.BioModule}s, ignore non-(fasta/fastq) files found in
	 * {@link biolockj.Config#INPUT_DIRS}
	 * </ul>
	 *
	 * @throws Exception if {@link biolockj.Config#INPUT_DIRS} undefined or file reader I/O Exception occurs
	 */
	protected static void initIgnoreFiles() throws Exception
	{
		final Set<String> ignore = new HashSet<>();
		if( Config.getSet( Config.INPUT_IGNORE_FILES ) != null )
		{
			ignore.addAll( Config.getSet( Config.INPUT_IGNORE_FILES ) );
		}

		if( MetaUtil.getFile() != null )
		{
			ignore.add( MetaUtil.getMetadataFileName() );
		}

		Config.setConfigProperty( Config.INPUT_IGNORE_FILES, ignore );

		final Collection<File> basicInputFiles = getBasicInputFiles();
		for( final File f: basicInputFiles )
		{
			if( hasSeqInput() )
			{
				final BufferedReader reader = SeqUtil.getFileReader( f );
				final String testChar = reader.readLine().trim().substring( 0, 1 );
				Log.debug( SeqUtil.class, "First character of input file = " + testChar );
				if( !SeqUtil.FASTA_HEADER_DELIMS.contains( testChar )
						&& !testChar.equals( SeqUtil.FASTQ_HEADER_DELIM ) )
				{
					Log.warn( SeqUtil.class,
							"IGNORE INPUT FILE!  Adding file name to Config." + Config.INPUT_IGNORE_FILES
									+ " due to invalid sequence file format (1st character = " + testChar + "): "
									+ f.getAbsolutePath() + BioLockJ.RETURN
									+ "FASTA files must begin with \">\" or \";\" and FASTQ files must begin with \"@\""
									+ BioLockJ.RETURN );
					ignore.add( f.getName() );
					inputFiles.remove( f );
				}
			}
		}

		Config.setConfigProperty( Config.INPUT_IGNORE_FILES, ignore );
	}

	private static boolean foundClassifier()
	{
		for( final String name: Config.getList( Config.INTERNAL_BLJ_MODULE ) )
		{
			if( name.startsWith( ClassifierModule.class.getPackage().getName() ) )
			{
				return true;
			}
		}

		return false;
	}

	private static Collection<File> getBasicInputFiles() throws Exception
	{
		if( inputFiles.isEmpty() )
		{
			final Collection<File> files = new HashSet<>();
			for( final File dir: getInputDirs() )
			{
				Log.info( SeqUtil.class, "Found pipeline input dir " + dir.getAbsolutePath() );
				files.addAll( FileUtils.listFiles( dir, HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );
			}
			Log.info( SeqUtil.class, "# Initial input Files found: " + files.size() );
			for( final File file: files )
			{
				if( !Config.getSet( Config.INPUT_IGNORE_FILES ).contains( file.getName() ) )
				{
					inputFiles.add( file );
				}
				else
				{
					Log.warn( SeqUtil.class, "Ignoring file: " + file.getName() );
				}
			}
		}

		return inputFiles;
	}

	/**
	 * Most pipelines have {@value #FASTA} or {@value #FASTQ} files in {@value biolockj.Config#INPUT_DIRS}. However,
	 * some pipeline {@value biolockj.Config#INPUT_DIRS} contain files that are not sequence files, such as from a
	 * {@link biolockj.module.implicit.parser.ParserModule} output directory. This method returns
	 * {@value biolockj.Config#TRUE} if a {@link biolockj.module.BioModule} is found from one of the 2 packages that
	 * require sequence data input: {@link biolockj.module.classifier} and {@link biolockj.module.seq}.
	 *
	 * @return boolean {@value biolockj.Config#TRUE} if classifier or seq {@link biolockj.module.BioModule} are found
	 */
	private static boolean hasSeqInput() throws Exception
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

	private static void printCollection( final Collection<String> items, final String label )
	{
		int i = 0;
		final Iterator<String> it = items.iterator();
		while( it.hasNext() )
		{
			Log.warn( SeqUtil.class, label + " [" + i++ + "] = " + it.next() );
		}
	}

	/**
	 * Unpaired reads must be multiplexed into a single file. Multiplexed paired reads must be contained in either 1
	 * file, or 2 (1 file with forward reads and 1 file with reverse reads). Based on the number of files and file
	 * names, we determine if the data is multiplexed. This function can be overridden by the Config property
	 * {@value biolockj.module.implicit.Demultiplexer#INPUT_DO_NOT_DEMUX} to return false regardless of input file data.
	 *
	 * @return boolean TRUE if pipeline data-set is multiplexed
	 * @throws Exception if validation fails
	 */
	private static boolean registerDemuxStatus() throws Exception
	{
		boolean isMultiplexed = false;
		final String fwRead = Config.getString( Config.INPUT_FORWARD_READ_SUFFIX );
		final String rvRead = Config.getString( Config.INPUT_REVERSE_READ_SUFFIX );

		if( !DemuxUtil.doDemux() )
		{
			Log.debug( SeqUtil.class, "Do not demux!" );
		}
		else if( hasSeqInput() && getBasicInputFiles().size() == 1 )
		{
			Log.info( SeqUtil.class, "Must demultiplex data!  Found exactly 1 input file" );
			isMultiplexed = true;
		}
		else if( hasSeqInput() && getBasicInputFiles().size() == 2 )
		{
			boolean foundFw = false;
			boolean foundRv = false;
			for( final File file: getBasicInputFiles() )
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
		return isMultiplexed;
	}

	/**
	 * Inspect the pipeline input files to determine if input includes paired reads.
	 * 
	 * @return boolean TRUE if paired reads are found
	 * @throws Exception if unable to determine if reads are paired
	 */
	private static boolean registerPairedReadStatus() throws Exception
	{
		boolean foundPairedReads = false;
		if( Config.requireBoolean( Config.INTERNAL_MULTIPLEXED ) )
		{
			if( getPipelineInputFiles().size() > 1 )
			{
				foundPairedReads = true;
			}
			else
			{
				boolean foundFw = false;
				boolean foundRv = false;
				final BufferedReader reader = SeqUtil.getFileReader( getPipelineInputFiles().get( 0 ) );
				Log.info( SeqUtil.class, "Reading multiplexed file to check for paired reads: "
						+ getPipelineInputFiles().get( 0 ).getAbsolutePath() );

				Log.info( SeqUtil.class,
						"Save Sequence File Property [ #lines/read ] = " + SeqUtil.getNumLinesPerRead() );
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
							Log.warn( SeqUtil.class, "Invalid header, no direction indicator: " + seqLines.get( 0 ) );
						}

						seqLines.clear();
					}
				}

				reader.close();
				foundPairedReads = foundFw && foundRv;
				Log.debug( SeqUtil.class, "Found Forward reads: " + foundFw );
				Log.debug( SeqUtil.class, "Found Reverse reads: " + foundRv );
				Log.debug( SeqUtil.class, "Found Paired Reads: " + foundPairedReads );

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
			foundPairedReads = !getPairedReads( getPipelineInputFiles() ).isEmpty();
		}

		Log.info( SeqUtil.class,
				"Set " + Config.INTERNAL_PAIRED_READS + "=" + ( foundPairedReads ? Config.TRUE: Config.FALSE ) );

		Config.setConfigProperty( Config.INTERNAL_PAIRED_READS, foundPairedReads ? Config.TRUE: Config.FALSE );
		return foundPairedReads;
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
	 * {@link biolockj.Config} property: {@value #INTERNAL_SEQ_HEADER_CHAR}<br>
	 * The property holds the 1st character used in the sequence header for the given dataset
	 */
	public static final String INTERNAL_SEQ_HEADER_CHAR = "internal.seqHeaderChar";

	/**
	 * {@link biolockj.Config} property: {@value #INTERNAL_SEQ_TYPE}<br>
	 * The sequence type path will save either {@value #FASTA} or {@value #FASTQ} to {@link biolockj.Config} using this
	 * property.
	 */
	public static final String INTERNAL_SEQ_TYPE = "internal.seqType";

	private final static List<File> filteredInputFiles = new ArrayList<>();
	private final static List<File> inputFiles = new ArrayList<>();
}
