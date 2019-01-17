/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Mar 19, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import biolockj.Config;
import biolockj.Log;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;

/**
 * This utility contains standard methods used to handle multiplexed data.
 */
public class DemuxUtil
{

	/**
	 * Return TRUE if Config is setup to demux the sequence data based on barcodes in the sequence headers.
	 * 
	 * @return boolean
	 * @throws ConfigNotFoundException if {@value #DEMUX_STRATEGY} is undefined
	 */
	public static boolean barcodeInHeader() throws ConfigNotFoundException
	{
		return Config.getString( DEMUX_STRATEGY ) != null
				&& Config.requireString( DEMUX_STRATEGY ).equals( OPTION_BARCODE_IN_HEADER );
	}

	/**
	 * Return TRUE if Config is setup to demux the sequence data based on barcodes in the sequence itself.
	 * 
	 * @return boolean
	 * @throws ConfigNotFoundException if {@value #DEMUX_STRATEGY} is undefined
	 */
	public static boolean barcodeInMapping() throws ConfigNotFoundException
	{
		return Config.getString( DEMUX_STRATEGY ) != null
				&& Config.requireString( DEMUX_STRATEGY ).equals( OPTION_BARCODE_IN_MAPPING );
	}

	/**
	 * Return TRUE if Config is setup to demux the sequence data based on barcodes in the sequence itself.
	 * 
	 * @return boolean
	 * @throws ConfigNotFoundException if {@value #DEMUX_STRATEGY} is undefined
	 */
	public static boolean barcodeInSeq() throws ConfigNotFoundException
	{
		return Config.getString( DEMUX_STRATEGY ) != null
				&& Config.requireString( DEMUX_STRATEGY ).equals( OPTION_BARCODE_IN_SEQ );
	}

	// /**
	// * Build header refs from mapping file
	// *
	// * INCOMPLETE CODE -- not currently used
	// *
	// * @throws Exception if errors occur
	// */
	// public static void buildHeaderRefs() throws Exception
	// {
	// final BufferedReader reader = BioLockJUtil.getFileReader( DemuxUtil.getMapping() );
	// try
	// {
	// final Map<String, String> barcodeMap = getIdMap();
	// final List<String> seqLines = new ArrayList<>();
	// for( String line = reader.readLine(); line != null; line = reader.readLine() )
	// {
	// seqLines.add( line );
	// if( seqLines.size() == SeqUtil.getNumLinesPerRead() )
	// {
	// final String header = seqLines.get( 0 );
	// final String barcode = seqLines.get( 1 );
	// if( barcodeMap.keySet().contains( barcode ) )
	// {
	// final BufferedWriter writer = new BufferedWriter(
	// new FileWriter( getHeaderFile( barcode ), true ) );
	// writer.write( header + BioLockJ.RETURN );
	// writer.close();
	// }
	// }
	// }
	//
	// }
	// finally
	// {
	// if( reader != null )
	// {
	// reader.close();
	// }
	// }
	// }

	/**
	 * Return TRUE if barcode column is defined in the Config file and is populated in the metadata file
	 * 
	 * @return TRUE or FALSE
	 * @throws Exception if unable to access metadata file or Config
	 */
	public static boolean demuxWithBarcode() throws Exception
	{
		if( ( barcodeInHeader() || barcodeInSeq() || barcodeInMapping() )
				&& MetaUtil.getFieldNames().contains( Config.requireString( MetaUtil.META_BARCODE_COLUMN ) )
				&& !MetaUtil.getFieldValues( Config.requireString( MetaUtil.META_BARCODE_COLUMN ) ).isEmpty() )
		{
			return true;
		}
		return false;
	}

	/**
	 * Return TRUE if Config is setup to demux the sequence data.
	 * 
	 * @return boolean
	 * @throws ConfigNotFoundException if {@value #DEMUX_STRATEGY} is undefined
	 */
	public static boolean doDemux() throws ConfigNotFoundException
	{
		return Config.getString( DEMUX_STRATEGY ) != null
				&& !Config.requireString( DEMUX_STRATEGY ).equals( OPTION_DO_NOT_DEMUX );
	}

	/**
	 * Return the demux mapping file used to map sequence files to barcode.
	 * 
	 * @return Mapping file path
	 * @throws ConfigNotFoundException if {@value #MAPPING_FILE} is undefined
	 * @throws ConfigPathException if {@value #MAPPING_FILE} path does not exist on the file system
	 * 
	 */
	public static File getMapping() throws ConfigNotFoundException, ConfigPathException
	{
		if( barcodeInMapping() )
		{
			return Config.requireExistingFile( MAPPING_FILE );
		}
		return null;
	}

	/**
	 * Determine Sample Id by examining the sequence lines.<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_ID_IN_HEADER}, extract the Sample Id from the sequence header via
	 * {@link biolockj.util.SeqUtil#getSampleId(String)}<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_BARCODE_IN_HEADER} and the sequence header contains a bar-code in
	 * the idMap, return the corresponding SampleID from the idMap.<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_BARCODE_IN_MAPPING} and the sequence header maps to a bar-code in
	 * the idMap, return the corresponding SampleID from the idMap.<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_BARCODE_IN_SEQ} and the sequence itself begins with a bar-code in
	 * the idMap, return the corresponding SampleID from the idMap.<br>
	 * 
	 * @param seqLines List of lines for one fasta or fatsq read
	 * @return Sample ID or null
	 * @throws Exception if propagated from {@link biolockj.util.SeqUtil} or {@link biolockj.Config}
	 */
	public static String getSampleId( final List<String> seqLines ) throws Exception
	{
		if( demuxWithBarcode() )
		{
			final Map<String, String> idMap = getIdMap();
			if( idMap != null )
			{
				for( final String barCodeId: idMap.keySet() )
				{
					if( ( barcodeInHeader() || barcodeInMapping() ) && seqLines.get( 0 ).contains( barCodeId )
							|| barcodeInSeq() && seqLines.get( 1 ).startsWith( barCodeId ) )
					{
						return idMap.get( barCodeId );
					}

				}
			}

			return null;
		}

		return SeqUtil.getSampleId( seqLines.get( 0 ) );

	}

	/**
	 * Return TRUE if Config is setup to demux the sequence data based on Sample IDs in the sequence headers.
	 * 
	 * @return boolean
	 * @throws ConfigNotFoundException if {@value #DEMUX_STRATEGY} is undefined
	 */
	public static boolean sampleIdInHeader() throws ConfigNotFoundException
	{
		return Config.getString( DEMUX_STRATEGY ) != null
				&& Config.requireString( DEMUX_STRATEGY ).equals( OPTION_ID_IN_HEADER );
	}

	/**
	 * Return the ID map (key=barcodeID, value=sampleId) Print the map to the log file.<br>
	 * Barcodes, if used, are located in the metadata column defined by property:
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_BARCODE_COLUMN}
	 *
	 * @return Id map
	 * @throws Exception if propagated from {@link biolockj.util.MetaUtil} or {@link biolockj.Config}
	 */
	protected static Map<String, String> getIdMap() throws Exception
	{
		if( !idMap.isEmpty() )
		{
			return idMap;
		}

		if( demuxWithBarcode() )
		{
			Log.info( DemuxUtil.class,
					"Detected Config." + BARCODE_USE_REV_COMP + " = " + Config.getBoolean( BARCODE_USE_REV_COMP ) );

			Log.info( DemuxUtil.class,
					"Building Barcode-SampleID Map using the "
							+ ( Config.getBoolean( BARCODE_USE_REV_COMP ) ? "reverse compliment of ": "" )
							+ "metadata column = " + Config.getString( MetaUtil.META_BARCODE_COLUMN ) );
		}
		else
		{
			Log.debug( DemuxUtil.class, "Lookup multiplexed Sample IDs in header (no barcodes)" );
			return null;
		}

		for( final String id: MetaUtil.getSampleIds() )
		{
			String val = MetaUtil.getField( id, Config.requireString( MetaUtil.META_BARCODE_COLUMN ) );

			if( Config.getBoolean( BARCODE_USE_REV_COMP ) )
			{
				val = SeqUtil.reverseComplement( val );
			}

			idMap.put( val, id );
		}

		for( final String key: idMap.keySet() )
		{
			Log.info( DemuxUtil.class,
					"Barcode-SampleID Map key[ " + key + " ] -->  value[ " + idMap.get( key ) + " ]" );
		}

		return idMap;
	}

	// private static File getHeaderFile( final String barcode ) throws Exception
	// {
	// if( headerFileMap.get( barcode ) == null )
	// {
	// final File barcodeDir = new File(
	// ModuleUtil.getModule( Demultiplexer.class.getName() ).getTempDir().getAbsolutePath()
	// + File.separator + "barcodeHeaderMaps" );
	// if( !barcodeDir.exists() )
	// {
	// barcodeDir.mkdirs();
	// }
	//
	// final File file = new File( barcodeDir.getAbsolutePath() + File.separator + barcode + ".txt" );
	// if( !file.exists() )
	// {
	// headerFileMap.put( barcode, file.getAbsolutePath() );
	// }
	// }
	//
	// return new File( headerFileMap.get( barcode ) );
	// }

	/**
	 * {@link biolockj.Config} boolean property {@value #BARCODE_CUTOFF} will look for barcodes in
	 * {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} to exist in this percentage of multiplex file.
	 */
	public static final String BARCODE_CUTOFF = "demux.barcodeCutoff";

	/**
	 * {@link biolockj.Config} boolean property {@value #BARCODE_USE_REV_COMP} will use the reverse compliment of
	 * {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} to match sample IDs to sequences.
	 */
	public static final String BARCODE_USE_REV_COMP = "demux.barcodeUseReverseCompliment";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} tells BioLockJ how to match sequences with Sample
	 * IDs.<br>
	 * Options: barcode_in_header, barcode_in_mapping, barcode_in_seq, id_in_header, do_not_demux.<br>
	 * If using barcodes, metadata column {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} is required.
	 */
	public static final String DEMUX_STRATEGY = "demux.strategy";

	/**
	 * {@link biolockj.Config} String property {@value #MAPPING_FILE} lists the path to the mapping file to match
	 * sequence headers to barcodes. The barcodes found in metadata file
	 * {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} can be used to match sample IDs to sequences.
	 */
	public static final String MAPPING_FILE = "demux.mapping";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_BARCODE_IN_HEADER}
	 */
	public static final String OPTION_BARCODE_IN_HEADER = "barcode_in_header";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_BARCODE_IN_MAPPING}
	 */
	public static final String OPTION_BARCODE_IN_MAPPING = "barcode_in_mapping";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_BARCODE_IN_SEQ}
	 */
	public static final String OPTION_BARCODE_IN_SEQ = "barcode_in_seq";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_DO_NOT_DEMUX}
	 */
	public static final String OPTION_DO_NOT_DEMUX = "do_not_demux";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option: {@value #OPTION_ID_IN_HEADER}
	 */
	public static final String OPTION_ID_IN_HEADER = "id_in_header";

	// private static final Map<String, String> headerFileMap = new HashMap<>();
	private static final Map<String, String> idMap = new HashMap<>();
}
