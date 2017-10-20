/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Oct 6, 2017
 */
package biolockj.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.parser.ParsedSample;

/**
 *
 */
public class SeqUtil
{

	/**
	 * If user prop indicates they need a copy of the input files, copy them to the project dir.
	 *
	 * @throws Exception
	 */
	public static void copyInputData() throws Exception
	{
		final File inputFileDir = new File(
				Config.requireExistingDir( Config.PROJECT_DIR ).getAbsolutePath() + File.separator + "input" );

		if( !inputFileDir.exists() )
		{
			inputFileDir.mkdirs();
		}

		final File startedFlag = new File( inputFileDir.getAbsolutePath() + File.separator + BioModule.BLJ_STARTED );
		if( startedFlag.exists() )
		{
			FileUtils.forceDelete( inputFileDir );
		}

		for( final File dir: Config.requireExistingDirs( Config.INPUT_DIRS ) )
		{
			Log.out.info( "Copying input files from " + dir + " to " + inputFileDir );
			FileUtils.copyDirectory( dir, inputFileDir );
		}

		final File completeFlag = new File( inputFileDir.getAbsolutePath() + File.separator + BioModule.BLJ_COMPLETE );
		final FileWriter writer = new FileWriter( completeFlag );
		writer.close();
		if( !completeFlag.exists() )
		{
			throw new Exception( "Unable to create " + completeFlag.getAbsolutePath() );
		}
	}

	public static Map<String, Long> getHitsPerSample()
	{
		return hitsPerSample;
	}

	/**
	 * Determine if fasta or fastq by checking input file format.
	 * @return
	 * @throws Exception
	 */
	public static String getInputSequenceType() throws Exception
	{
		if( inputSequenceType != null )
		{
			return inputSequenceType;
		}

		if( Config.getBoolean( Config.INPUT_PAIRED_READS ) )
		{
			Config.requireString( Config.INPUT_FORWARD_READ_SUFFIX );
			Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX );
		}

		String foundFasta = null;
		String foundFastq = null;
		String foundIgnore = null;
		for( final File f: Config.requireExistingDirs( Config.INPUT_DIRS ).get( 0 ).listFiles() )
		{
			final String name = f.getName();
			if( Config.getSet( Config.INPUT_IGNORE_FILES ).contains( name ) || name.startsWith( "." )
					|| f.isDirectory() )
			{
				Log.out.warn( "Skipping input file while looking for example to determine sequence type: "
						+ f.getAbsolutePath() );
				continue;
			}

			final BufferedReader reader = ModuleUtil.getFileReader( f );
			final String testChar = reader.readLine().trim().substring( 0, 1 );
			Log.out.debug( "First character of test input sequence file: " + testChar );
			if( testChar.equals( ">" ) || testChar.equals( ";" ) )
			{
				foundFasta = FASTA;
			}
			else if( testChar.equals( "@" ) )
			{
				foundFastq = FASTQ;
			}
			else
			{
				foundIgnore = f.getAbsolutePath();
				Log.out.warn( "Ignoring invalid input file (Fast-A or FastQ format not found): " + f.getName() );
				Config.getSet( Config.INPUT_IGNORE_FILES ).add( f.getName() );
			}

		}

		if( ( foundFasta == null ) && ( foundFastq == null ) )
		{
			throw new Exception( "No input files found in: "
					+ Config.INPUT_DIRS + ( ( foundIgnore != null )
							? ( " other than files with invalid formats such as: " + foundIgnore ): "" )
					+ "FASTA must begin with \">\" or \";\" and  FASTQ must begin with \"@\"" );
		}

		if( ( foundFasta != null ) && ( foundFastq != null ) )
		{
			throw new Exception( "Input files from: " + Config.INPUT_DIRS
					+ " must all be of a single type (Fast-A or Fast-Q" + BioLockJ.RETURN + "Fast-A found: "
					+ foundFasta + BioLockJ.RETURN + "Fast-Q found: " + foundFastq );
		}

		if( foundFasta != null )
		{
			inputSequenceType = FASTA;
		}
		else if( foundFastq != null )
		{
			inputSequenceType = FASTQ;
		}

		return inputSequenceType;
	}

	public static Map<File, File> getPairedReads( final List<File> files ) throws Exception
	{
		Log.out.info( "Calling getPairedReads for " + ( ( files == null ) ? 0: files.size() ) + " files " );
		final Map<File, File> map = new HashMap<>();
		for( final File fwRead: files )
		{
			final String name = fwRead.getName();
			Log.out.debug( "Checking getPairedReads( " + name + " )" );
			if( !isForwardRead( name ) )
			{
				continue;
			}

			File rvRead = null;
			final String sampleID = getSampleId( name );
			Log.out.debug( "========> " + name + " = FW Read --> for sampleID: " + sampleID );
			for( final File searchFile: files )
			{
				final String testName = searchFile.getName();
				final String testID = getSampleId( testName );
				Log.out.debug(
						"========> Check for matching RV Read ( " + testName + " ) --> which has SampleID: " + testID );
				Log.out.debug( "==================> Is sampleID.equals( testID ) = " + sampleID.equals( testID ) );
				if( sampleID.equals( testID ) )
				{
					Log.out.debug( "==================> Found ID: " + testID + " --> Is this a RV Read? "
							+ !isForwardRead( testName ) );
				}

				if( sampleID.equals( testID ) && !isForwardRead( testName ) )
				{
					Log.out.debug( "===============================> MATCH FOUND " + testName );
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
				Log.out.warn( "PAIRED_READS=TRUE - UNPAIRED FORWARD READ FOUND = " + fwRead.getAbsolutePath() );
			}
		}

		return map;
	}

	public static Map<String, Long> getReadsPerSample()
	{
		return readsPerSample;
	}

	/**
	 * Method extracts Sample ID from the name param.
	 * Possibly input is a file name so remove file extensions.
	 * If demultiplexing (RDP/Kraken support this option), input is a sequence header
	 *
	 * @param String - Sample Name
	 * @return String - Sample ID
	 * @throws Exception
	 */
	public static String getSampleId( final String name ) throws Exception
	{
		String id = name; // 9_R2.fastq.gz
		try
		{
			final String fwReadSuffix = Config.getString( Config.INPUT_FORWARD_READ_SUFFIX );
			final String rvReadSuffix = Config.getString( Config.INPUT_REVERSE_READ_SUFFIX );
			final String trimPrefix = Config.getString( Config.INPUT_TRIM_PREFIX );
			final String trimSuffix = Config.getString( Config.INPUT_TRIM_SUFFIX );

			if( !Config.getBoolean( Config.INPUT_DEMULTIPLEX ) ) // must be a file
			{
				// trim .gz extension
				if( id.toLowerCase().endsWith( ".gz" ) )
				{
					id = id.substring( 0, id.length() - 3 ); // 9_R2.fastq
				}

				// trim  .fasta or .fastq extension
				if( id.toLowerCase().endsWith( "." + SeqUtil.FASTA )
						|| id.toLowerCase().endsWith( "." + SeqUtil.FASTQ ) )
				{
					id = id.substring( 0, id.length() - 6 );
				}

				if( ( fwReadSuffix != null ) && SeqUtil.isForwardRead( id ) && ( id.lastIndexOf( fwReadSuffix ) > 0 ) )
				{
					id = id.substring( 0, id.lastIndexOf( fwReadSuffix ) );
				}
				else if( ( rvReadSuffix != null ) && ( id.lastIndexOf( rvReadSuffix ) > 0 ) )
				{
					id = id.substring( 0, id.lastIndexOf( rvReadSuffix ) );
				}
			}

			if( ( trimPrefix != null ) && ( trimPrefix.length() > 0 ) && ( id.indexOf( trimPrefix ) > -1 ) )
			{
				id = id.substring( trimPrefix.length() + id.indexOf( trimPrefix ) );
			}

			if( ( trimSuffix != null ) && ( trimSuffix.length() > 0 ) && ( id.indexOf( trimSuffix ) > -1 ) )
			{
				id = id.substring( 0, id.lastIndexOf( trimSuffix ) );
			}

		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to get SampleID from: " + name, ex );
			throw ex;
		}

		return id;
	}

	public static boolean isFastA() throws Exception
	{
		if( getInputSequenceType().equals( FASTA ) )
		{
			return true;
		}

		return false;
	}

	public static boolean isFastQ() throws Exception
	{
		if( getInputSequenceType().equals( FASTQ ) )
		{
			return true;
		}

		return false;
	}

	/**
	 * Suffix may be part of regular file name, so only remove the last index
	 *
	 * @param id
	 * @return
	 */
	public static boolean isForwardRead( final String name ) throws Exception
	{
		if( !Config.getBoolean( Config.INPUT_PAIRED_READS ) )
		{
			return true;
		}

		// both forward & reverse suffixes must be defined
		final int fwIndex = name.lastIndexOf( Config.requireString( Config.INPUT_FORWARD_READ_SUFFIX ) );
		final int rvIndex = name.lastIndexOf( Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX ) );
		if( ( fwIndex > 0 ) && ( fwIndex > rvIndex ) )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Read nodes to get numHits for a given sample.
	 * @throws Exception
	 */
	public static void registerNumHitsPerSample( final Set<ParsedSample> parsedSamples, final File targetDir )
			throws Exception
	{
		if( ( MetaUtil.exists() ) && Config.getBoolean( Config.REPORT_NUM_HITS ) && hitsPerSample.isEmpty() )
		{
			Log.out.info( "Counting # hits/sample for " + parsedSamples.size() + " files" );
			for( final ParsedSample sample: parsedSamples )
			{
				hitsPerSample.put( sample.getSampleId(), sample.getNumHits() );
			}

			MetaUtil.addNumericColumn( MetaUtil.NUM_HITS, hitsPerSample, targetDir );
		}
	}

	/**
	 * Register num reads for each sample by parsing the files & counting number of lines.
	 * @param files
	 * @param targetDir
	 * @throws Exception
	 */
	public static void registerNumReadsPerSample( final List<File> files, final File targetDir ) throws Exception
	{
		if( ( MetaUtil.exists() ) && Config.getBoolean( Config.REPORT_NUM_READS ) && readsPerSample.isEmpty() )
		{
			Log.out.info( "Counting # reads/sample for " + ( ( files == null ) ? 0: files.size() ) + " files" );
			for( final File f: files )
			{
				if( isForwardRead( f.getName() ) )
				{
					final long count = countNumReads( f );
					Log.out.info( "Num Reads for File:[" + f.getName() + "] ==> ID:[" + getSampleId( f.getName() )
							+ "] = " + count );
					readsPerSample.put( getSampleId( f.getName() ), count );
				}
			}
			MetaUtil.addNumericColumn( MetaUtil.NUM_READS, readsPerSample, targetDir );
		}
	}

	private static long countNumReads( final File f ) throws Exception
	{
		long count = 0L;
		final BufferedReader r = ModuleUtil.getFileReader( f );
		for( String line = r.readLine(); line != null; line = r.readLine() )
		{
			count++;
		}
		r.close();

		return ( count / ( isFastA() ? 2: 4 ) );
	}

	public static final String FASTA = "fasta";
	public static final String FASTQ = "fastq";

	private static final Map<String, Long> hitsPerSample = new HashMap<>();
	private static String inputSequenceType = null;
	private static final Map<String, Long> readsPerSample = new HashMap<>();
}
