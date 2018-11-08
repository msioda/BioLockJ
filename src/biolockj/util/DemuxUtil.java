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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import biolockj.Config;
import biolockj.Log;
import biolockj.exception.ConfigNotFoundException;

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
	public static boolean barcodeInSeq() throws ConfigNotFoundException
	{
		return Config.getString( DEMUX_STRATEGY ) != null
				&& Config.requireString( DEMUX_STRATEGY ).equals( OPTION_BARCODE_IN_SEQ );
	}

	/**
	 * Return TRUE if barcode column is defined in the Config file and is populated in the metadata file
	 * 
	 * @return TRUE or FALSE
	 * @throws Exception if unable to access metadata file or Config
	 */
	public static boolean demuxWithBarcode() throws Exception
	{
		if( ( barcodeInHeader() || barcodeInSeq() )
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
	 * Determine Sample Id by examining the sequence lines.<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_ID_IN_HEADER }, extract the Sample Id from the sequence header via
	 * {@link biolockj.util.SeqUtil#getSampleId(String)}<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_BARCODE_IN_HEADER } and the sequence header contains a bar-code in
	 * the idMap, return the corresponding SampleID from the idMap.<br>
	 * If {@value #DEMUX_STRATEGY }={@value #OPTION_BARCODE_IN_SEQ } and the sequence itself begins with a bar-code in
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
					if( barcodeInHeader() && seqLines.get( 0 ).contains( barCodeId )
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

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} tells BioLockJ how to match sequences with Sample
	 * IDs.<br>
	 * Options: barcode_in_header, barcode_in_seq, id_in_header, do_not_demux.<br>
	 * If using barcodes, metadata column {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} is required.
	 */
	public static final String DEMUX_STRATEGY = "demux.strategy";

	/**
	 * {@link biolockj.Config} boolean property {@value #BARCODE_USE_REV_COMP} will use the reverse compliment of
	 * {@value biolockj.util.MetaUtil#META_BARCODE_COLUMN} to match sample IDs to sequences.
	 */
	protected static final String BARCODE_USE_REV_COMP = "demux.barcodeUseReverseCompliment";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option
	 */
	protected static final String OPTION_BARCODE_IN_HEADER = "barcode_in_header";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option
	 */
	protected static final String OPTION_BARCODE_IN_SEQ = "barcode_in_seq";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option
	 */
	protected static final String OPTION_DO_NOT_DEMUX = "do_not_demux";

	/**
	 * {@link biolockj.Config} property {@value #DEMUX_STRATEGY} option
	 */
	protected static final String OPTION_ID_IN_HEADER = "id_in_header";

	private static final Map<String, String> idMap = new HashMap<>();

}
