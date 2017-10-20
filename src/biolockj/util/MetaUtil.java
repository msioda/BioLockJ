/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer 	This code is free software; you can redistribute it and/or
 * 				modify it under the terms of the GNU General Public License
 * 				as published by the Free Software Foundation; either version 2
 * 				of the License, or (at your option) any later version,
 * 				provided that any use properly credits the author.
 * 				This program is distributed in the hope that it will be useful,
 * 				but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * 				GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.util.r.RScript;

/**
 * The metadataUtil helps access and modify data in the metadata file.
 */
public class MetaUtil
{
	/**
	 * When a new column is added to metadata, this method will add the column, with all row values.
	 * The updated file is output to the "outputDir" to be picked up by the next executor.
	 * @param name
	 * @param map
	 * @param fileDir
	 * @throws Exception
	 */
	public static void addNumericColumn( final String name, final Map<String, Long> map, final File fileDir )
			throws Exception
	{
		if( getAttributeNames().contains( name ) )
		{
			Log.out.warn( "MetaUtil already contains column [" + name + "] so this data will not be added to "
					+ metadataFile.getName() );
			return;
		}

		numericFields.add( name );

		final BufferedReader reader = new BufferedReader( new FileReader( metadataFile ) );
		metadataFile = new File( fileDir.getAbsolutePath() + File.separator + metadataFile.getName() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( metadataFile ) );

		Log.out.info( "Adding new attribute [" + name + "] to metadata" );
		boolean isHeaderRow = true;
		try
		{
			final Set<String> keys = map.keySet();
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, BioLockJ.TAB_DELIM );
				if( isHeaderRow )
				{
					isHeaderRow = false;
					line += BioLockJ.TAB_DELIM + name;
				}
				else
				{
					final String id = st.nextToken();
					if( keys.contains( id ) )
					{
						line += BioLockJ.TAB_DELIM + map.get( id );
					}
					else
					{
						line += BioLockJ.TAB_DELIM + Config.requireString( MetaUtil.INPUT_NULL_VALUE );
					}
				}

				writer.write( line + BioLockJ.RETURN );
			}
		}
		catch( final Exception ex )
		{
			Log.out.error( "Error occurred updating metadata with new attribute [" + name + "]", ex );
		}
		finally
		{
			reader.close();
			writer.close();
			refresh();
		}
	}

	public static void addNumericField( final String field )
	{
		numericFields.add( field );
	}

	public static boolean exists()
	{
		return ( metadataFile != null ) && metadataFile.exists();
	}

	/**
	 * Many users modify metadata spreadsheets in Excel.  If the first cell value starts with # symbol,
	 * Excel adds a ZERO WIDTH NO-BREAK space as an invisible character.  Here we strip this value;
	 * See http://www.fileformat.info/info/unicode/char/feff/index.htm
	 * @param id
	 * @return
	 */
	public static String formatMetaId( String id )
	{
		final char c = id.trim().toCharArray()[ 0 ];
		if( c == 65279 )
		{
			Log.out.warn(
					"MetadataUtil found row ID starting with ASCII 65279 - this invalid invisble character has been removed!" );

			final char[] chars = id.trim().toCharArray();
			for( int i = 0; i < chars.length; i++ )
			{
				Log.out.debug( "ID[" + i + "] = " + chars[ i ] );
			}

			id = id.substring( 1 );
			Log.out.info( "Updated ID = " + id );
		}
		return id;
	}

	/**
	 * Get a list of all attribute names from the metadata file column names.
	 * @return
	 */
	public static List<String> getAttributeNames()
	{
		return metadataMap.get( metaId );
	}

	/**
	 * Get attribute values from metadata (get row for a given ID).
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public static List<String> getAttributes( final String id ) throws Exception
	{
		try
		{
			return metadataMap.get( id );
		}
		catch( final Exception ex )
		{
			throw new Exception( "Invalid ID: " + id );
		}

	}

	public static Set<String> getBinaryFields()
	{
		return binaryFields;
	}

	public static List<String> getCleanVals( final Collection<String> vals ) throws Exception
	{
		final List<String> formattedValues = new ArrayList<>();
		for( final String val: vals )
		{
			formattedValues.add( rScriptFormat( val ) );
		}
		return formattedValues;
	}

	public static File getFile()
	{
		return metadataFile;
	}

	public static String getID()
	{
		return metaId;
	}

	public static String getMetadataFileName() throws Exception
	{
		return Config.getExistingFile( Config.INPUT_METADATA ) == null ? MetaUtil.DEFAULT_METADATA
				: Config.getExistingFile( Config.INPUT_METADATA ).getName();
	}

	/**
	 * Get the first column from the metadata file.
	 * @return
	 */
	public static Set<String> getMetaFileFirstColValues()
	{
		return metadataMap.keySet();
	}

	public static Set<String> getNominalFields()
	{
		return nominalFields;
	}

	public static Set<String> getNumericFields()
	{
		return numericFields;
	}

	/**
	 * Loading new metadata will set the static field values and populate the attributeMap.
	 * @param metadata
	 * @throws Exception
	 */
	public static void refresh() throws Exception
	{
		metadataMap.clear();
		attributeMap.clear();
		processMetadata( processFile() );
		setRscriptFields();
		populateAttributeMap();
		validateConfig();

		Config.getSet( Config.INPUT_IGNORE_FILES ).add( MetaUtil.getFile().getName() );

		Log.out.info( "MetaUtil Attributes: " + getAttributeNames() );
		Log.out.info( "MetaUtil 1st Column (Header ID name & Sample IDs): " + getMetaFileFirstColValues() );
		Log.out.info( Log.LOG_SPACER );
		Log.out.info( "New MetaUtil file loaded: " + metadataFile.getAbsolutePath() );

	}

	public static void setFile( final File f )
	{
		if( f != null )
		{
			Log.out.info( "===> Set new metadata: " + f.getAbsolutePath() );
		}
		metadataFile = f;
	}

	/**
	 * The attributeMap maps attributes to their set of values.  Only done for metadata that will
	 * be used in the R-script.
	 *
	 * @throws Exception
	 */
	private static void populateAttributeMap() throws Exception
	{
		//Log.out.warn( "===> calling populateAttributeMap() for metaId = " + metaId );
		//Log.out.warn( "===> metadataMap.get( metaId ) = " + metadataMap.get( metaId ) );
		Log.out.info( "===> All rScriptFields = " + rScriptFields );

		final Map<String, Integer> colIndexMap = new HashMap<>();
		int j = 0;
		for( final String att: metadataMap.get( metaId ) )
		{
			if( rScriptFields.contains( att ) )
			{
				Log.out.info( "Initialize Attribute Map | attribute(" + att + ") = index(" + j + ")" );
				colIndexMap.put( att, j );
				attributeMap.put( att, new HashSet<>() );
			}
			j++;
		}

		for( final String att: colIndexMap.keySet() )
		{
			final int target = colIndexMap.get( att );
			for( final String key: metadataMap.keySet() )
			{

				if( !key.equals( metaId ) )
				{
					//					Log.out.warn( "===> metadataMap KEY DOES NOT MATCH metaId" );
					//					Log.out.warn( "===> metadataMap CHECK IF --> key( " + key + " ) = metaId( " + metaId + " )" );
					//					Log.out.warn(
					//							"===> key LENGTH( " + key.length() + " ) = metaId LENGTH( " + metaId.length() + " )" );
					int i = 0;
					final List<String> row = metadataMap.get( key );
					for( final String value: row )
					{
						if( ( i++ == target ) && !value.equals( Config.requireString( MetaUtil.INPUT_NULL_VALUE ) ) )
						{
							Log.out.debug(
									"===> Add (" + value + ") to existing attributeMap " + attributeMap.get( att ) );
							attributeMap.get( att ).add( value );
						}
					}
				}
			}
		}

		for( final String key: attributeMap.keySet() )
		{
			final Set<String> vals = attributeMap.get( key );
			Log.out.info( "Attribute Map (" + key + ") = " + vals );
			if( nominalFields.contains( key ) && ( vals.size() < 2 ) )
			{
				throw new Exception( "Property " + RScript.R_NOMINAL_DATA + " contains attribute [" + key
						+ "] with only " + vals.size()
						+ " values in the metadata file.  Statistical tests require at least 2 unique options." );
			}
			else if( nominalFields.contains( key ) && ( vals.size() == 2 ) )
			{

				binaryFields.add( key );
				nominalFields.remove( key );
			}
			else if( numericFields.contains( key ) )
			{
				for( final String val: vals )
				{
					if( !NumberUtils.isNumber( val ) )
					{
						throw new Exception( "Property " + RScript.R_NUMERIC_DATA + " contains attribute [" + key
								+ "] with non-numeric data [" + val + "]" );
					}
				}
			}
		}
	}

	/**
	 * Process a file by getting clean values for each cell in the spreadsheet.
	 * @param file
	 * @return
	 */
	private static List<List<String>> processFile() throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final BufferedReader reader = new BufferedReader( new FileReader( metadataFile ) );
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			final ArrayList<String> record = new ArrayList<>();
			final String[] cells = line.split( BioLockJ.TAB_DELIM, -1 );
			for( final String cell: cells )
			{
				record.add( rScriptFormat( cell ) );
			}
			data.add( record );
		}
		reader.close();
		return data;
	}

	/**
	 * Process metadata & output some values to output file for verification.
	 * @param data
	 */
	private static void processMetadata( final List<List<String>> data )
	{
		final int digits = new Integer( data.size() ).toString().length();
		int rowNum = 0;
		final Iterator<List<String>> rows = data.iterator();
		while( rows.hasNext() )
		{
			final List<String> row = trim( rows.next() );
			String id = row.get( 0 );
			row.remove( 0 );

			if( rowNum == 0 )
			{
				id = formatMetaId( id );
				metaId = id;
				Log.out.info( "Loading METADATA [ID = " + metaId + "] with " + row.size() + " attribute columns" );
			}

			if( rowNum < 2 )
			{
				Log.out.info( "Example MetaUtil Row[" + String.format( "%0" + digits + "d", rowNum ) + "]: Key(" + id
						+ "): " + row );
			}

			metadataMap.put( id, row );
			rowNum++;
		}
	}

	/**
	 * Clean values avoid commas, and replace spaces with underscores.
	 * @param val
	 * @return
	 */
	private static String rScriptFormat( String val ) throws Exception
	{
		if( ( val == null ) || val.trim().isEmpty() )
		{
			return Config.requireString( MetaUtil.INPUT_NULL_VALUE );
		}

		final int index = val.indexOf( Config.requireString( MetaUtil.INPUT_COMMENT ) );
		if( index > -1 )
		{
			val = val.substring( 0, val.indexOf( Config.requireString( MetaUtil.INPUT_COMMENT ) ) );
		}

		return val.trim();

	}

	/**
	 * Set rScriptFields variable =  all metadata attributes referenced in R Script:
	 * Uses 4 config file props: nominalFields, numericFields, filterNaAttributes, & filterAttributes
	 */
	private static void setRscriptFields() throws Exception
	{
		nominalFields.addAll( Config.getSet( RScript.R_NOMINAL_DATA ) );
		numericFields.addAll( Config.getSet( RScript.R_NUMERIC_DATA ) );
		rScriptFields.addAll( nominalFields );
		rScriptFields.addAll( numericFields );
		rScriptFields.addAll( Config.getSet( RScript.R_FILTER_NA_ATTRIBUTES ) );
		rScriptFields.addAll( Config.getSet( RScript.R_FILTER_ATTRIBUTES ) );

		for( final String att: metadataMap.get( metaId ) )
		{
			if( att.equals( NUM_READS ) && Config.getBoolean( Config.REPORT_NUM_READS ) )
			{
				rScriptFields.add( NUM_READS );
			}
			else if( att.equals( NUM_HITS ) && Config.getBoolean( Config.REPORT_NUM_HITS ) )
			{
				rScriptFields.add( NUM_HITS );
			}
		}
	}

	/**
	 * Trim all values in row.
	 * @param row
	 * @return
	 */
	private static List<String> trim( final List<String> row )
	{
		final List<String> formattedRow = new ArrayList<>();
		final Iterator<String> it = row.iterator();
		while( it.hasNext() )
		{
			formattedRow.add( it.next().trim() );
		}

		return formattedRow;
	}

	/**
	 * Verify any fields to be used in R scripts.
	 * @throws Exception
	 */
	private static void validateConfig() throws Exception
	{
		if( Config.requireString( MetaUtil.INPUT_COMMENT ).length() > 1 )
		{
			throw new Exception( MetaUtil.INPUT_COMMENT + " must be a single character with length = 1" );
		}

		if( Config.requireString( MetaUtil.INPUT_NULL_VALUE ).equals( Config.requireString( MetaUtil.INPUT_COMMENT ) ) )
		{
			throw new Exception( "BioLockJ requires unique values for config properties: " + MetaUtil.INPUT_NULL_VALUE
					+ " & " + MetaUtil.INPUT_COMMENT );
		}

		for( final String field: rScriptFields )
		{
			if( !MetaUtil.getAttributeNames().contains( field ) )
			{
				throw new Exception( field + " is not found in metadata: " + metadataFile.getAbsolutePath() );
			}
		}
	}

	public static final String DEFAULT_METADATA = "defaultMetadata.tsv";
	public static final String INPUT_COMMENT = "input.commentDelim";
	public static final String INPUT_NULL_VALUE = "input.nullValue";
	public static final String META_MERGED_SUFFIX = "MetaMerged.tsv";
	public static final String NUM_HITS = "Num_Hits";
	public static final String NUM_READS = "Num_Reads";

	private static final Map<String, Set<String>> attributeMap = new HashMap<>();
	private static final Set<String> binaryFields = new TreeSet<>();
	private static File metadataFile = null;
	private static final Map<String, List<String>> metadataMap = new HashMap<>();
	private static String metaId = null;
	private static final Set<String> nominalFields = new TreeSet<>();
	private static final Set<String> numericFields = new TreeSet<>();
	private static final Set<String> rScriptFields = new HashSet<>();
}