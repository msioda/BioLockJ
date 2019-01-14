/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date September 21, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit;

import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigViolationException;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.util.*;

/**
 * This BioModule splits multiplexed data into a separate file or pair of files (for paired reads) for each sample. The
 * sample ID, or an identification barcode must be included in the header of each read. Barcodes are matched to Sample
 * IDs using the metadata column configured via property "metadata.barcodeColumn".<br>
 * If demux.strategy=barcode_in_header, register a match if the barcode is found anywhere in the sequence header.<br>
 * If demux.strategy=barcode_in_seq, register a match if the sequence itself starts with any of the barcodes. The output
 * multiplexed file will have barcodes removed from the sequences since they no longer serve any purpose.<br>
 * Sequences with no matching barcode, or without a matching paired header (if paired), are output to a "NO_MATCH" file
 * in the module temp directory.<br>
 * Paired reads are accepted in 2 multiplexed formats:
 * <ol>
 * <li>Forward and reverse reads can be partitioned into 2 separate files identified by identified by: input.suffixFw
 * and input.suffixRv.
 * <li>Forward and reverse reads can be combined in a single file with read direction indicated in the header by values
 * "1:N:" and "2:N:"
 * </ol>
 */
public class Demultiplexer extends JavaModuleImpl implements JavaModule
{
	/**
	 * Validate module dependencies:
	 * <ol>
	 * <li>If {@link biolockj.Config}.{@value biolockj.util.DemuxUtil#DEMUX_STRATEGY} indicates use of barcodes to
	 * demux, validate metadata column named {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_BARCODE_COLUMN}
	 * exists
	 * <li>Call {@link #setMultiplexedConfig()} to set multiplexed Config if needed
	 * <li>If {@link biolockj.Config}.{@value biolockj.util.DemuxUtil#BARCODE_CUTOFF} defined, validate between 0.0 -
	 * 1.0
	 * </ol>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		setMultiplexedConfig();
		getBarcodeCutoff();
		final String demuxStrategy = "Config property [ " + DemuxUtil.DEMUX_STRATEGY + "="
				+ Config.getString( DemuxUtil.DEMUX_STRATEGY ) + " ]";
		if( DemuxUtil.demuxWithBarcode() )
		{
			if( MetaUtil.getFile() == null || !MetaUtil.getFile().exists() )
			{
				throw new Exception( demuxStrategy + " but metadata file is undefined" );
			}

			if( !MetaUtil.getFieldNames().contains( Config.requireString( MetaUtil.META_BARCODE_COLUMN ) ) )
			{
				throw new Exception( demuxStrategy + " but the barcode column configured [ "
						+ MetaUtil.META_BARCODE_COLUMN + "=" + Config.requireString( MetaUtil.META_BARCODE_COLUMN )
						+ " ] is not found in the metadata: " + MetaUtil.getFile().getAbsolutePath() );
			}

			if( MetaUtil.getFieldValues( Config.requireString( MetaUtil.META_BARCODE_COLUMN ) ).isEmpty() )
			{
				throw new Exception( demuxStrategy + " but the barcode column configured [ "
						+ MetaUtil.META_BARCODE_COLUMN + "=" + Config.requireString( MetaUtil.META_BARCODE_COLUMN )
						+ " ] is empty in the metadata file: " + MetaUtil.getFile().getAbsolutePath() );
			}

			if( DemuxUtil.barcodeInSeq() )
			{
				Log.info( getClass(), "Barcode sequences will be removed from the output file sequences." );
			}
		}
		else
		{
			Log.info( getClass(),
					demuxStrategy + " so Demultiplexer will use the Config properties [" + Config.INPUT_TRIM_PREFIX
							+ " & " + Config.INPUT_TRIM_SUFFIX
							+ "] to extract the Sample ID from the sequence header" );
			Log.info( getClass(), "Config property [ " + Config.INPUT_TRIM_PREFIX + " ] = "
					+ Config.getString( Config.INPUT_TRIM_PREFIX ) );
			Log.info( getClass(), "Config property [ " + Config.INPUT_TRIM_SUFFIX + " ] = "
					+ Config.getString( Config.INPUT_TRIM_SUFFIX ) );

		}
		super.checkDependencies();
	}

	/**
	 * Update SeqUtil to indicate data has been demuxed.<br>
	 * If {@value biolockj.util.MetaUtil#META_REQUIRED}, verify each sequence has a metadata record.
	 * 
	 * @throws Exception if unable to modify property
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		Config.setConfigProperty( Config.INTERNAL_MULTIPLEXED, Config.FALSE );
		if( Config.getBoolean( MetaUtil.META_REQUIRED ) )
		{
			verifySeqMetadata();
		}
	}

	/**
	 * Produces initial count and demultiplexed output count summaries for forward/reverse reads.
	 */
	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
			{
				sb.append( "# Forward Reads: " + numTotalFwReads + RETURN );
				sb.append( "# Reverse Reads: " + numTotalRvReads + RETURN );

				if( numTotalFwReads != numValidFwReads )
				{
					sb.append( "# Valid Forward Reads (matched to a sample): " + numValidFwReads + RETURN );
				}
				if( numTotalRvReads != numValidRvReads )
				{

					sb.append( "# Valid Reverse Reads (matched to a sample): " + numValidRvReads + RETURN );
				}
			}
			else
			{
				sb.append( "# Total Reads:  " + numTotalFwReads + RETURN );
				if( numTotalFwReads != numValidFwReads )
				{
					sb.append( "# Valid Reads (matched to a sample): " + numValidFwReads + RETURN );
				}
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
	
	
	private int hasBarcode( String line ) throws Exception
	{
		for( final String code: MetaUtil.getFieldValues( MetaUtil.META_BARCODE_COLUMN ) )
		{
			if( line.contains( code ) ) return 1;
			if( line.contains( SeqUtil.reverseComplement( code ) ) ) return 2;
		}
		return 0;
	}

	/**
	 * Module execution summary:<br>
	 * <ol>
	 * <li>Execute {@link #breakUpFiles()} to split the multiplex file into smaller size for processing
	 * <li>Execute {@link #getValidHeaders()} to obtain list of valid headers matched to sample ID with the metadata
	 * file and also verifies matching forward and reverse read headers if demuliplexing paired reads.
	 * <li>Execute {@link #demultiplex(Map)} to demultiplex the data into a separate file (or pair of files) for each
	 * sample
	 * </ol>
	 * <p>
	 * If paired reads are combined in a single file the read direction must be identified in the sequence header using
	 * key strings {@value biolockj.util.SeqUtil#ILLUMINA_FW_READ_IND}
	 * {@value biolockj.util.SeqUtil#ILLUMINA_RV_READ_IND}
	 */
	@Override
	public void runModule() throws Exception
	{
		breakUpFiles();
		setBarcodeReverseIfNeeded();

		demultiplex( getValidHeaders() );
	}

	/**
	 * Some multiplexed files can be very large. This method breaks each input file into smaller files, each
	 * {@value #NUM_LINES_TEMP_FILE} lines in size to avoid memory issues while processing.
	 * 
	 * @throws Exception if unexpected errors occur at runtime
	 */
	protected void breakUpFiles() throws Exception
	{
		
		for( final File file: getInputFiles() )
		{
			Log.info( getClass(),
					"Break multiplexed file [ " + file.getAbsolutePath() + " ] into files with a max #lines = [ "
							+ NUM_LINES_TEMP_FILE + " ] to avoid memory issues while processing" );
			Integer fileReads = 0;
			Integer fileFwBarcodes = 0;
			Integer fileRvBarcodes = 0;
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try
			{
				final List<String> seqLines = new ArrayList<>();
				int i = 0;
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					seqLines.add( line );
					
					if( testFile == null && ( ( seqLines.size() == 1 && DemuxUtil.barcodeInHeader() ) || ( seqLines.size() == 2 && DemuxUtil.barcodeInSeq() ) ) )
					{
						fileReads++;
						int testBarcodes = hasBarcode( line );
						if( testBarcodes == 1 ) fileFwBarcodes++;
						else if( testBarcodes == 2 ) fileRvBarcodes++;
					}

					if( seqLines.size() >= NUM_LINES_TEMP_FILE )
					{
						writeSample( seqLines, getSplitFileName( file.getName(), i++ ) );
						seqLines.clear();
					}

				}

				if( !seqLines.isEmpty() )
				{
					writeSample( seqLines, getSplitFileName( file.getName(), i++ ) );
				}

				if( !seqLines.isEmpty() )
				{
					writeSample( seqLines, getSplitFileName( file.getName(), i++ ) );
				}
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
			}

			Log.info( getClass(), "Done splitting file: " + file.getAbsolutePath() );
			
			if( testFile == null )
			{
				testFile = file.getAbsolutePath();
				numReads = fileReads;
				fwBarcodes = fileFwBarcodes;
				rvBarcodes = fileRvBarcodes;
			}
		}

		Log.info( getClass(), "Number of temp files created: " + getSplitDir().listFiles().length );
	}

	/**
	 * Demultiplex the file into separate small temp files, with {@value #NUM_LINES_TEMP_FILE} lines each for
	 * processing. If paired reads, generate separate forward and reverse files for each sample output file named by
	 * sample ID.
	 *
	 * @param validHeaders Set of valid headers
	 * @throws Exception if error occurs reading the multiplexed file
	 */
	protected void demultiplex( final Map<String, Set<String>> validHeaders ) throws Exception
	{
		printCounts( validHeaders );
		// int count = 0;
		// int loopCount = 0;
		boolean doPrint = true;

		for( final File file: getSplitDir().listFiles() )
		{
			Log.info( getClass(), "Demultiplexing file " + file.getAbsolutePath() );
			final List<String> seqLines = new ArrayList<>();
			final Map<String, List<String>> output = new HashMap<>();
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					seqLines.add( line );
					if( seqLines.size() == SeqUtil.getNumLinesPerRead() )
					{
						final String header = SeqUtil.getHeader( seqLines.get( 0 ) );
						final String sampleId = getSampleId( header, validHeaders );

						String otu = null;

						if( sampleId == null )
						{
							otu = getNoMatchFileName( file.getName(), seqLines.get( 0 ) );
						}
						else
						{
							otu = getOutputFileName( sampleId, file.getName(), seqLines.get( 0 ) );
							incrementCounts( file.getName(), seqLines.get( 0 ) );

							if( doPrint )
							{
								doPrint = false;
								Log.info( getClass(), "EXAMPLE Demultiplexed Sample ID: " + sampleId );
								Log.info( getClass(), "EXAMPLE Demultiplexed sequence file: " + otu );
							}
						}

						if( !output.keySet().contains( otu ) )
						{
							output.put( otu, new ArrayList<>() );
						}

						output.get( otu ).addAll( seqLines );
						seqLines.clear();
					}
				}

				for( final String outName: output.keySet() )
				{
					Log.debug( getClass(), outName + " adding # lines = " + output.get( outName ).size() );
					writeSample( output.get( outName ), outName );
				}
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
			}
		}
	}

	/**
	 * Get valid forward read headers that belong to reads with a valid barcode or sample identifier.
	 * 
	 * @return Map sampleID to list of valid headers
	 * @throws Exception if error occur
	 */
	protected Map<String, Set<String>> getValidFwHeaders() throws Exception
	{
		final Map<String, Set<String>> validHeaders = new HashMap<>();
		final boolean isPaird = Config.getBoolean( Config.INTERNAL_PAIRED_READS );
		final boolean isCombined = isPaird && getInputFiles().size() == 1;

		Log.info( getClass(), "Get FW Headers from temp files "
				+ ( isCombined ? "created from multiplexed file with both forward and reverse reads": "" ) );

		// check forward reads only
		for( final File file: getSplitDir().listFiles() )
		{
			// skip files ending with _R2 suffix since contain only reverse reads
			if( !isCombined && !SeqUtil.isForwardRead( file.getName() ) )
			{
				continue;
			}

			Log.info( getClass(), "Processing split file for FW headers: " + file.getAbsolutePath() );

			final List<String> seqLines = new ArrayList<>();
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					seqLines.add( line );
					if( seqLines.size() == SeqUtil.getNumLinesPerRead() )
					{
						// if not combined must be a file of only forward reads due to continue above
						if( !isCombined || seqLines.get( 0 ).contains( SeqUtil.ILLUMINA_FW_READ_IND ) )
						{
							numTotalFwReads++;
							final String sampleId = DemuxUtil.getSampleId( seqLines );
							if( sampleId != null )
							{
								if( validHeaders.get( sampleId ) == null )
								{
									validHeaders.put( sampleId, new HashSet<>() );
								}
								final String header = SeqUtil.getHeader( seqLines.get( 0 ) );
								validHeaders.get( sampleId ).add( header );
							}
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

		}

		String msg = " # valid reads = ";
		if( Config.requireBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			msg = " # valid fw read headers = ";
		}

		for( final String sampleId: validHeaders.keySet() )
		{
			Log.debug( getClass(), sampleId + msg + validHeaders.get( sampleId ).size() );
		}

		return validHeaders;
	}

	/**
	 * This method obtains all valid headers for the forward reads, and returns only headers that also have a matching
	 * reverse read
	 * 
	 * @return Map of valid headers for each sample ID
	 * @throws Exception if unable to obtain headers
	 */
	protected Map<String, Set<String>> getValidHeaders() throws Exception
	{
		final Map<String, Set<String>> validFwHeaders = getValidFwHeaders();
		if( !Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			Log.info( getClass(), "Demultiplexing unpaired reads...# " + validFwHeaders.size() );
			return validFwHeaders;
		}

		final Map<String, Set<String>> validHeaders = new HashMap<>();
		final boolean isCombined = getInputFiles().size() == 1;

		// check reverse reads only
		for( final File file: getSplitDir().listFiles() )
		{
			// skip files ending with _R1 suffix since contain only forward reads
			if( !isCombined && SeqUtil.isForwardRead( file.getName() ) )
			{
				continue;
			}

			Log.info( getClass(), "Processing split file for RV headers: " + file.getAbsolutePath() );

			final List<String> seqLines = new ArrayList<>();
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					seqLines.add( line );
					if( seqLines.size() == SeqUtil.getNumLinesPerRead() )
					{
						// if not combined must be a file of only reverse reads due to continue above
						if( !isForwardRead( file.getName(), seqLines.get( 0 ) ) )
						{
							final String header = SeqUtil.getHeader( seqLines.get( 0 ) );
							numTotalRvReads++;

							for( final String sampleId: validFwHeaders.keySet() )
							{
								if( validFwHeaders.get( sampleId ).contains( header ) )
								{
									if( validHeaders.get( sampleId ) == null )
									{
										validHeaders.put( sampleId, new HashSet<>() );
									}

									validHeaders.get( sampleId ).add( header );
									break;
								}
							}
						}

						seqLines.clear();
					}
				}
			}
			finally
			{
				reader.close();
			}
		}

		for( final String sampleId: validHeaders.keySet() )
		{
			Log.info( getClass(), sampleId + " # valid headers = " + validHeaders.get( sampleId ).size() );
		}

		return validHeaders;
	}

	/**
	 * Set Config property: {@value biolockj.util.DemuxUtil#BARCODE_USE_REV_COMP} based on percentage of reads with
	 * reverse compliments of the barcodes
	 * 
	 * @throws Exception
	 */
	protected void setBarcodeReverseIfNeeded() throws Exception
	{
		if( DemuxUtil.demuxWithBarcode() && Config.getString( DemuxUtil.BARCODE_USE_REV_COMP ) == null
				&& getBarcodeCutoff() != null && numReads > 0  && testFile != null )
		{
			final double cutoff = getBarcodeCutoff();
			final String displayCutoff = BioLockJUtil.formatPercentage( Math.round( cutoff * 100 ), 100L );
			Log.info( getClass(), "Detected #Lines/Read = " + SeqUtil.getNumLinesPerRead() + " in TEST FILE: " + testFile );
			Log.info( getClass(), "Found total # lines in" + numReads + " in TEST FILE: " + testFile );
			Log.info( getClass(), "Checking to find required percentage of lines with barcode" + displayCutoff );

			final double fwPer = fwBarcodes / numReads;
			final double rvPer = rvBarcodes / numReads;

			final String displayFwPer = BioLockJUtil.formatPercentage( fwBarcodes, numReads );
			final String displayRvPer = BioLockJUtil.formatPercentage( rvBarcodes, numReads );

			Log.info( getClass(), "Total #Reads w/ valid barcode: " + fwBarcodes );
			Log.info( getClass(), "Total #Reads w/ valid REVERSE COMPLIMENT( barcode ): " + rvBarcodes );
			
			Log.info( getClass(), "Found total # lines in" + rvBarcodes + " in TEST FILE: " + testFile  );

			if( (fwPer > cutoff) && (rvPer > cutoff) )
			{
				throw new Exception( "Cannot auto-detect if barcodes in" + testFile
						+ " require reverse compliment or not: fw = " + displayFwPer + " & rv = " + displayRvPer );
			}
			else if( rvPer > cutoff )
			{
				Log.info( getClass(), "Using reverse compliment barcodes!" );
				Config.setConfigProperty( DemuxUtil.BARCODE_USE_REV_COMP, Config.TRUE );
			}
			else
			{
				throw new Exception( "Barcodes and their reverse compliments found in less than Config cuttoff: "
						+ cutoff + " = " + displayCutoff + " of " + testFile + " reads" );
			}
		}
	}

	/**
	 * Set the {@link biolockj.Config} properties needed to read the sample IDs from a multiplexed file if no barcode is
	 * provided<br>
	 * Set {@link biolockj.Config}.{@value biolockj.Config#INPUT_TRIM_PREFIX} = 1st sequence header character.<br>
	 * Set {@link biolockj.Config}.{@value biolockj.Config#INPUT_TRIM_SUFFIX}
	 * ={@value #SAMPLE_ID_SUFFIX_TRIM_DEFAULT}<br>
	 * 
	 * @throws Exception if unable to update the property values
	 */
	protected void setMultiplexedConfig() throws Exception
	{
		if( DemuxUtil.sampleIdInHeader() )
		{
			final String defaultSeqHeadChar = Config.requireString( SeqUtil.INTERNAL_SEQ_HEADER_CHAR );

			if( Config.getString( Config.INPUT_TRIM_PREFIX ) == null )
			{
				Config.setConfigProperty( Config.INPUT_TRIM_PREFIX, defaultSeqHeadChar );
				Log.info( getClass(), "====> Set: " + Config.INPUT_TRIM_PREFIX + " = " + defaultSeqHeadChar );
			}

			if( Config.getString( Config.INPUT_TRIM_SUFFIX ) == null )
			{
				Config.setConfigProperty( Config.INPUT_TRIM_SUFFIX, SAMPLE_ID_SUFFIX_TRIM_DEFAULT );
				Log.info( getClass(),
						"====> Set: " + Config.INPUT_TRIM_SUFFIX + " = " + SAMPLE_ID_SUFFIX_TRIM_DEFAULT );
			}
		}
	}

	/**
	 * Verify that each sequence file has a corresponding record in the metadata file.
	 * 
	 * @throws Exception if errors occur
	 */
	protected void verifySeqMetadata() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		int x = 0;
		for( final File seq: getOutputDir().listFiles() )
		{
			final String id = SeqUtil.getSampleId( seq.getName() );
			if( SeqUtil.isForwardRead( seq.getName() ) && !MetaUtil.getSampleIds().contains( id ) )
			{
				x++;
				sb.append( sb.toString().isEmpty() ? "": ", " ).append( id );
			}
		}

		if( !sb.toString().isEmpty() )
		{
			throw new ConfigViolationException( MetaUtil.META_REQUIRED,
					"Metadata row is missing for the following " + x + " samples IDs: " + sb.toString() );
		}
	}

	private Double getBarcodeCutoff() throws Exception
	{
		final Double val = Config.getPositiveDoubleVal( DemuxUtil.BARCODE_CUTOFF );
		if( val != null && val > 1 )
		{
			throw new ConfigFormatException( DemuxUtil.BARCODE_CUTOFF, "Must be between 0.0 - 1.0" );
		}
		return val;
	}

	private String getFileSuffix( final String name, final String header ) throws Exception
	{
		String suffix = "";
		if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			suffix = isForwardRead( name, header ) ? Config.requireString( Config.INPUT_FORWARD_READ_SUFFIX )
					: Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX );
		}

		return suffix + "." + ( SeqUtil.isFastA() ? SeqUtil.FASTA: SeqUtil.FASTQ );
	}
	
	private String[] getGrepGzArgs( final File file, final String code ) throws Exception
	{
		final String[] args = new String[ 6 ];
		args[ 0 ] = "zcat";
		args[ 1 ] = file.getAbsolutePath();
		args[ 2 ] = "|";
		args[ 3 ] = "grep";
		args[ 4 ] = "-c";
		args[ 5 ] = code;
		return args;
	}
	
	
	private String[] getGrepArgs( final File file, final String code ) throws Exception
	{
		if( SeqUtil.isGzipped( file.getName() ) )
		{
			return getGrepGzArgs( file, code );
		}
		
		final String[] args = new String[ 4 ];
		args[ 0 ] = "grep";
		args[ 1 ] = "-c";
		args[ 2 ] = code;
		args[ 3 ] = file.getAbsolutePath();
		return args;
	}

	private String getNoMatchFileName( final String fileName, final String header ) throws Exception
	{
		return getTempDir().getAbsolutePath() + File.separator + "NO_MATCH" + getFileSuffix( fileName, header );

	}

	private String getOutputFileName( final String sampleId, final String fileName, final String header )
			throws Exception
	{
		return getOutputDir().getAbsolutePath() + File.separator + sampleId + getFileSuffix( fileName, header );
	}

	private String getSampleId( final String header, final Map<String, Set<String>> validHeaders ) throws Exception
	{
		for( final String sampleId: validHeaders.keySet() )
		{
			if( validHeaders.get( sampleId ).contains( header ) )
			{
				return sampleId;
			}
		}
		return null;
	}

	private File getSplitDir() throws Exception
	{
		return ModuleUtil.requireSubDir( this, getTempDir().getName() + File.separator + "split" );
	}

	private String getSplitFileName( final String fileName, final int i ) throws Exception
	{
		String suffix = "";
		if( getInputFiles().size() > 1 )
		{
			suffix = SeqUtil.getReadDirectionSuffix( fileName );
		}

		suffix += "." + ( SeqUtil.isFastA() ? SeqUtil.FASTA: SeqUtil.FASTQ );

		return getSplitDir().getAbsolutePath() + File.separator + "split_" + i + suffix;
	}

	private String[] getWcArgs( final File file ) throws Exception
	{
		final String[] args = new String[ 5 ];
		args[ 0 ] = SeqUtil.isGzipped( file.getName() ) ? "zcat" : "cat";
		args[ 1 ] = file.getAbsolutePath();
		args[ 2 ] = "|";
		args[ 3 ] = "wc";
		args[ 4 ] = "-l";
		return args;
	}

	private void incrementCounts( final String name, final String header ) throws Exception
	{
		if( isForwardRead( name, header ) )
		{
			numValidFwReads++;
		}
		else
		{
			numValidRvReads++;
		}
	}

	private boolean isForwardRead( final String name, final String header ) throws Exception
	{
		if( !Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			return true;
		}

		final boolean isCombined = getInputFiles().size() == 1;
		if( isCombined )
		{
			if( header.contains( SeqUtil.ILLUMINA_FW_READ_IND ) )
			{
				return true;
			}
			else if( header.contains( SeqUtil.ILLUMINA_RV_READ_IND ) )
			{
				return false;
			}
			else
			{
				throw new Exception(
						"Sequence header in " + name + " does not indicate forward[" + SeqUtil.ILLUMINA_FW_READ_IND
								+ "] or reverse[" + SeqUtil.ILLUMINA_RV_READ_IND + "] read for header = " + header );
			}
		}

		return SeqUtil.isForwardRead( name );
	}

	private void printCounts( final Map<String, Set<String>> validHeaders )
	{
		long size = 0L;
		for( final String sampleId: validHeaders.keySet() )
		{
			size += validHeaders.get( sampleId ).size();
		}

		Log.info( getClass(), "Total fw reads = " + numTotalFwReads );
		Log.info( getClass(), "Total rv reads = " + numTotalRvReads );
		Log.info( getClass(), "Number valid reads = " + size );
	}

	private void writeSample( final List<String> lines, final String fileName ) throws Exception
	{
		final File outFile = new File( fileName );
		final boolean exists = outFile.exists();
		final BufferedWriter writer = new BufferedWriter( new FileWriter( outFile, exists ) );
		for( final String line: lines )
		{
			writer.write( line + BioLockJ.RETURN );
		}
		writer.close();
	}

	
	private int fwBarcodes = 0;
	private int rvBarcodes = 0;
	private int numReads = 0;
	private String testFile = null;
	
	private long numTotalFwReads = 0L;
	private long numTotalRvReads = 0L;
	private long numValidFwReads = 0L;
	private long numValidRvReads = 0L;

	/**
	 * Module splits multiplexed file into smaller files with this number of lines: {@value #NUM_LINES_TEMP_FILE}
	 */
	protected static final int NUM_LINES_TEMP_FILE = 2000000;

	/**
	 * Multiplexed files created by BioLockJ may add sample ID to the sequence header if no barcode is provided.<br>
	 * If sample ID is added, it is immediately followed by the character: {@value #SAMPLE_ID_SUFFIX_TRIM_DEFAULT}<br>
	 * This value can be used then to set {@link biolockj.Config}.{@value biolockj.Config#INPUT_TRIM_SUFFIX}
	 */
	protected static final String SAMPLE_ID_SUFFIX_TRIM_DEFAULT = "_";

}
