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
package biolockj.module.seq;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.report.Email;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule will merge sequence files into a single combined sequence file, with either the sample ID or an
 * identifying barcode (if defined in the metatata) is stored in the sequence header.<br>
 * BioLockJ is designed to run on demultiplexed data so this must be the last module to run before the Summary module.
 */
public class Multiplexer extends JavaModuleImpl implements JavaModule
{
	/**
	 * Validate module dependencies:
	 * <ol>
	 * <li>Validate this is the last module to run (excluding {@link biolockj.module.report.Email})
	 * </ol>
	 * If this module was completed on a previous run, update the property:
	 * {@link biolockj.Config}.{@value biolockj.Config#INTERNAL_MULTIPLEXED} = {@value biolockj.Config#TRUE}
	 * 
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Log.get( getClass() )
				.warn( "BioLockJ requires demultiplexed data, so this must be the last module except Email" );

		validateModuleOrder();
	}

	/**
	 * Produce a summary message with counts on total number of reads and number reads multiplexed.
	 */
	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			if( totalNumRvReads > 0 )
			{
				sb.append( "# Forward Read Files multiplexed = " + fwMap.keySet().size() + RETURN );
				sb.append( "# Reverse Read Files multiplexed = " + rvMap.keySet().size() + RETURN );
				sb.append( "# Total Forward Reads = " + totalNumFwReads + RETURN );
				sb.append( "# Total Reverse Reads = " + totalNumRvReads + RETURN );
			}
			else if( totalNumFwReads > 0 )
			{
				sb.append( "# Samples multiplexed = " + fwMap.keySet().size() + RETURN );
				sb.append( "# Total Reads = " + totalNumFwReads + RETURN );
			}
			else
			{
				sb.append( "Module incomplete - no output produced!" + RETURN );
			}
		}
		catch( final Exception ex )
		{
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.get( getClass() ).warn( msg );
		}

		return super.getSummary() + sb.toString();
	}

	/**
	 * Execute {@link #multiplex(File)} on each input file
	 */
	@Override
	public void runModule() throws Exception
	{
		Log.get( getClass() ).info( "Multiplexing file type = " + SeqUtil.getInputSequenceType() );
		if( !hasValidBarcodes() )
		{
			Log.get( getClass() ).info(
					"Multiplexer setting Sample ID in sequence header for identification since valid barcodes are not provided" );

		}

		for( final File f: getInputFiles() )
		{
			multiplex( f );
		}
	}

	/**
	 * Get the header for the sequence.
	 * 
	 * @param file Sequence file in fasta or fastq format
	 * @param seqLines Sequence lines for 1 read
	 * @return the header row for the sequence
	 * @throws Exception if errors occur while obtaining header
	 */
	protected String getHeader( final File file, final List<String> seqLines ) throws Exception
	{
		final String header = seqLines.get( 0 );
		final String headerChar = seqLines.get( 0 ).substring( 0, 1 );
		final String sampleId = SeqUtil.getSampleId( file.getName() );
		final long numReads = incrementNumReads( file );
		if( hasValidBarcodes() )
		{
			final String barcode = MetaUtil.getField( sampleId, Config.getString( MetaUtil.META_BARCODE_COLUMN ) );
			if( header.contains( barcode ) )
			{
				return header;
			}
			else
			{
				return header + ":" + barcode;
			}
		}
		else
		{
			final String idCol = Config.getString( ID_COL );
			final String id = idCol == null ? sampleId: MetaUtil.getField( sampleId, idCol );
			return headerChar + id + "_" + id + "." + numReads + ":" + header.substring( 1 );
		}
	}

	/**
	 * Add file sequences to the multiplexed file. If barcode is defined in the metadata file and set in the Config
	 * file, it will be added to the header (if not already in the header line). If no barcode value is configured, the
	 * sample ID will be used in the sequence headers.
	 * 
	 * @param sample Sequence file in Fasta or Fastq format
	 * @throws Exception if I/O errors occur creating multiplexed file
	 */
	protected void multiplex( final File sample ) throws Exception
	{
		Log.get( getClass() ).info( "Multiplexing file  = " + sample.getAbsolutePath() );
		final List<String> seqLines = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( sample );
		final BufferedWriter writer = new BufferedWriter(
				new FileWriter( new File( getMutliplexeFileName( sample ) ), true ) );
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			seqLines.add( line );
			if( seqLines.size() == SeqUtil.getNumLinesPerRead() ) // full read
			{
				writer.write( getHeader( sample, seqLines ) + RETURN );
				for( int i = 1; i < seqLines.size(); i++ )
				{
					writer.write( seqLines.get( i ) + RETURN );
				}
				seqLines.clear();
			}
		}
		writer.close();
		reader.close();
	}

	/**
	 * This method ensures that the multiplexed data is not passed to other modules.<br>
	 * This must be the last module, or last module before the Email.
	 * 
	 * @throws Exception if not the last module before {@link biolockj.module.report.Email}
	 */
	protected void validateModuleOrder() throws Exception
	{
		boolean invalid = false;
		boolean found = false;
		for( final BioModule bioModule: Pipeline.getModules() )
		{
			if( found && !( bioModule instanceof Email ) )
			{
				invalid = true;
			}
			else if( bioModule.getClass().equals( getClass() ) )
			{
				found = true;
			}
		}

		if( invalid )
		{
			throw new Exception( "Invalid BioModule configuration! " + getClass().getName()
					+ " must be the last module executed (or last before Email)." + RETURN
					+ "All other BioLockJ modules are designed to only run on demultiplexed data." );

		}
	}

	private String getMutliplexeFileName( final File file ) throws Exception
	{
		String suffix = "";
		if( Config.requireBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			if( SeqUtil.isForwardRead( file.getName() ) )
			{
				suffix = Config.requireString( Config.INPUT_FORWARD_READ_SUFFIX );
			}
			else
			{
				suffix = Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX );
			}
		}

		return getOutputDir().getAbsolutePath() + File.separator + Config.requireString( Config.INTERNAL_PIPELINE_NAME )
				+ suffix + "." + SeqUtil.getInputSequenceType();
	}

	private long getNumReads( final File file ) throws Exception
	{
		Long numReads = null;
		if( Config.requireBoolean( Config.INTERNAL_PAIRED_READS ) && !SeqUtil.isForwardRead( file.getName() ) )
		{
			numReads = rvMap.get( file.getName() );
		}
		else
		{
			numReads = fwMap.get( file.getName() );
		}

		if( numReads == null )
		{
			numReads = 0L;
		}
		return numReads;
	}

	private boolean hasValidBarcodes()
	{
		if( useBarcode != null )
		{
			return useBarcode;
		}
		try
		{
			useBarcode = false;
			final String barCodeCol = Config.getString( MetaUtil.META_BARCODE_COLUMN );
			if( barCodeCol != null && MetaUtil.getFieldNames().contains( MetaUtil.META_BARCODE_COLUMN ) )
			{
				final Set<String> sampleIds = new HashSet<>( MetaUtil.getSampleIds() );
				final Set<String> vals = new HashSet<>( MetaUtil.getFieldValues( MetaUtil.META_BARCODE_COLUMN ) );
				sampleIds.remove( Config.requireString( MetaUtil.META_NULL_VALUE ) );
				vals.remove( Config.requireString( MetaUtil.META_NULL_VALUE ) );
				if( sampleIds.size() != vals.size() )
				{
					Log.get( getClass() ).warn(
							"Multiplexer setting Sample ID in output instead of barcode because dataset contains "
									+ sampleIds.size() + " unique Sample IDs but only " + vals.size()
									+ " unique barcodes" );
					for( final String id: MetaUtil.getSampleIds() )
					{
						Log.get( getClass() ).warn(
								"ID [ " + id + " ] ==> " + MetaUtil.getField( id, MetaUtil.META_BARCODE_COLUMN ) );
					}
				}
				else
				{
					useBarcode = true;
				}
			}
		}
		catch( final Exception ex )
		{
			Log.get( getClass() ).error( "" + ex.getMessage(), ex );
		}

		return useBarcode;
	}

	private long incrementNumReads( final File file ) throws Exception
	{
		Long numReads = getNumReads( file );
		numReads++;

		if( Config.requireBoolean( Config.INTERNAL_PAIRED_READS ) && !SeqUtil.isForwardRead( file.getName() ) )
		{
			rvMap.put( file.getName(), numReads );
			totalNumRvReads++;
		}
		else
		{
			fwMap.put( file.getName(), numReads );
			totalNumFwReads++;
		}

		return numReads;
	}

	private final Map<String, Long> fwMap = new HashMap<>();

	private final Map<String, Long> rvMap = new HashMap<>();
	private long totalNumFwReads = 0L;
	private long totalNumRvReads = 0L;
	private Boolean useBarcode = null;
	private static final String ID_COL = "multiplexer.idCol";
}
