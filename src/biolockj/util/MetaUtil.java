/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.*;
import java.util.*;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;

/**
 * This utility is used to read, modify, or create a metadata file for the sequence data. The 1st row must hold the
 * Sample ID and column names must be unique. Metadata information is cached in this class for quick access throughout
 * the application.
 */
public class MetaUtil
{
	// Prevent instantiation
	private MetaUtil()
	{}

	/**
	 * Adds a column to the metadata file. The updated metadata file is output to the fileDir. Once the new file is
	 * created, the new file is cached in memory and used as the new metadata.
	 *
	 * @param colName Name of new column
	 * @param map Map relates Sample ID to a field value
	 * @param fileDir File representing output directory for new metadata file
	 * @throws Exception if unable to add the new column
	 */
	public static void addColumn( final String colName, final Map<String, String> map, final File fileDir )
			throws Exception
	{
		final File newMeta = new File( fileDir.getAbsolutePath() + File.separator + getMetadataFileName() );
		Log.info( MetaUtil.class, "Adding new field [" + colName + "] to metadata: " + newMeta.getAbsolutePath() );
		if( getFieldNames().contains( colName ) )
		{
			Log.warn( MetaUtil.class,
					"Metadata column [" + colName + "] already exists in: " + getFile().getAbsolutePath() );
			return;
		}

		final Set<String> keys = map.keySet();
		final BufferedReader reader = BioLockJUtil.getFileReader( getFile() );
		setFile( newMeta );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getFile() ) );
		try
		{
			writer.write( reader.readLine() + BioLockJ.TAB_DELIM + colName + BioLockJ.RETURN );
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, BioLockJ.TAB_DELIM );

				final String id = st.nextToken();
				if( keys.contains( id ) )
				{
					writer.write( line + BioLockJ.TAB_DELIM + map.get( id ) + BioLockJ.RETURN );
				}
				else
				{
					writer.write(
							line + BioLockJ.TAB_DELIM + Config.requireString( META_NULL_VALUE ) + BioLockJ.RETURN );
				}
			}
		}
		finally
		{
			reader.close();
			writer.close();
			refreshCache();
		}
	}

	/**
	 * Get the comment delimeter (or default empty string)
	 * 
	 * @return Comment char
	 */
	public static String getCommentChar()
	{
		String commentDelim = Config.getString( META_COMMENT_CHAR );
		if( commentDelim == null )
		{
			commentDelim = "";
		}

		return DEFAULT_COMMENT_CHAR;
	}

	/**
	 * Get metadata field value for given sampleId.
	 *
	 * @param sampleId Sample ID
	 * @param attName Field Name (column name in metadata file)
	 * @return Metadata field value
	 * @throws Exception if parameters are invalid
	 */
	public static String getField( final String sampleId, final String attName ) throws Exception
	{
		if( !getFieldNames().contains( attName ) )
		{
			throw new Exception(
					"Invalid field [" + attName + "] not found in Metadata = " + getFile().getAbsolutePath() );
		}

		if( getMetadataRecord( sampleId ) == null )
		{
			throw new Exception(
					"Invalid Sample ID [" + sampleId + "] not found in Metadata = " + getFile().getAbsolutePath() );
		}

		return getMetadataRecord( sampleId ).get( getFieldNames().indexOf( attName ) );
	}

	/**
	 * Get a list of all metadata fields (metadata file column names except the 1st).
	 *
	 * @return List of metadata column names, excluding the 1st column
	 * @throws Exception if unable to read metadata file
	 */
	public static List<String> getFieldNames() throws Exception
	{

		if( getFile() == null || !getFile().exists() )
		{
			Log.warn( MetaUtil.class, "Metadata file path is undefined.  Cannot get metadata field names." );
		}

		final List<String> headers = getMetadataRecord( metaId );

		if( headers == null )
		{
			Log.warn( MetaUtil.class,
					"Metadata headers not found!  Please verify 1st column header is not missing from: "
							+ getFile().getAbsolutePath() );
		}

		return headers;
	}

	/**
	 * Get metadata column for given field name.
	 *
	 * @param attName Column name
	 * @return List of Column values for sample ID
	 * @throws Exception if metadata file or column name not found
	 */
	public static List<String> getFieldValues( final String attName ) throws Exception
	{
		if( !getFieldNames().contains( attName ) )
		{
			throw new Exception( "Invalid field [" + attName + "] in Metadata = " + getFile().getAbsolutePath() );
		}

		final List<String> vals = new ArrayList<>();

		for( final String id: getSampleIds() )
		{
			final String val = getField( id, attName );
			if( val != null && val.trim().length() > 0 )
			{
				vals.add( val );
			}
		}

		return vals;
	}

	/**
	 * Get the current metadata file. This path may change as updates are made by BioModules.
	 *
	 * @return Metadata file
	 */
	public static File getFile()
	{
		return metadataFile;
	}

	/**
	 * Get the metadata file ID column name. This value may change as updates are made by BioModules.
	 *
	 * @return Metadata ID column name
	 */
	public static String getID()
	{
		return metaId;
	}

	/**
	 * Metadata file getter.
	 * 
	 * @return Metadata file
	 * @throws Exception if any errors occur
	 */
	public static File getMetadata() throws Exception
	{
		if( Config.getString( META_FILE_PATH ) == null )
		{
			return null;
		}

		if( RuntimeParamUtil.isDockerMode() )
		{
			return DockerUtil.getDockerVolumeFile( META_FILE_PATH, DockerUtil.CONTAINER_META_DIR, "metadata" );
		}
		return Config.requireExistingFile( META_FILE_PATH );
	}

	/**
	 * Get the metadata file name, if it exists, otherwise return projectName.tsv
	 *
	 * @return Name of metadata file, or a default name if no metadata file exists
	 */
	public static String getMetadataFileName()
	{
		try
		{
			if( metadataFile == null )
			{
				metadataFile = getMetadata();
			}

			if( metadataFile != null )
			{
				return metadataFile.getName();
			}
		}
		catch( final Exception ex )
		{
			Log.error( MetaUtil.class, "Error occurred accessing Config property: " + META_FILE_PATH, ex );
			ex.printStackTrace();
		}

		return Config.getString( Config.INTERNAL_PIPELINE_NAME ) + ".tsv";

	}

	/**
	 * Get metadata row for a given Sample ID.
	 *
	 * @param sampleId Sample ID
	 * @return Metadata row values for sample ID
	 * @throws Exception if Sample ID not found or metadata file doesn't exist
	 */
	public static List<String> getMetadataRecord( final String sampleId ) throws Exception
	{
		try
		{
			return metadataMap.get( sampleId );
		}
		catch( final Exception ex )
		{
			throw new Exception( "Invalid ID: " + sampleId );
		}
	}

	/**
	 * Get the first column from the metadata file.
	 *
	 * @return Sample IDs found in metadata file
	 */
	public static List<String> getSampleIds()
	{
		final List<String> ids = new ArrayList<>();
		for( final String key: metadataMap.keySet() )
		{
			if( !key.equals( metaId ) )
			{
				ids.add( key );
			}
		}

		return ids;
	}

	/**
	 * Check dependencies used to handle the metadata file are defined and unique. For undefined properties we set the
	 * same default values as used by the R{utils} read.table() function.
	 * <ul>
	 * <li>{@value #META_COLUMN_DELIM} column separator
	 * <li>{@value #META_COMMENT_CHAR} indicates start of comment in a cell, Requires length == 1
	 * <li>{@value #META_NULL_VALUE} indicates null cell
	 * </ul>
	 * 
	 * @throws Exception if invalid Config properties are detected
	 */
	public static void initialize() throws Exception
	{
		if( Config.getString( META_NULL_VALUE ) == null )
		{
			Config.setConfigProperty( META_NULL_VALUE, DEFAULT_NULL_VALUE );
		}

		if( Config.getString( META_COLUMN_DELIM ) == null )
		{
			Config.setConfigProperty( META_COLUMN_DELIM, DEFAULT_COL_DELIM );
		}

		if( getCommentChar().length() > 1 )
		{
			throw new Exception(
					META_COMMENT_CHAR + " must be a single character of length = 1.  Current property value is "
							+ getCommentChar().length() + " characters in length, value=\"" + getCommentChar() + "\"" );
		}

		final Set<String> metaProps = new HashSet<>();
		metaProps.add( Config.requireString( META_NULL_VALUE ) );
		metaProps.add( Config.requireString( META_COLUMN_DELIM ) );
		metaProps.add( getCommentChar() );

		if( metaProps.size() < 3 )
		{
			throw new Exception( "BioLockJ requires 3 unique values for config properties: (" + META_NULL_VALUE + ", "
					+ META_COLUMN_DELIM + ", " + META_COMMENT_CHAR + ") | Current values = (\""
					+ Config.requireString( META_NULL_VALUE ) + "\", \"" + Config.requireString( META_COLUMN_DELIM )
					+ "\", \"" + getCommentChar() + "\")" );
		}

		if( Config.getString( META_FILE_PATH ) != null )
		{
			setFile( MetaUtil.getMetadata() );
			refreshCache();
		}

	}

	/**
	 * Refresh the metadata cache.
	 *
	 * @throws Exception if unable to refresh cache
	 */
	public static void refreshCache() throws Exception
	{
		if( metadataFile != null && metadataFile.exists() )
		{
			Log.info( MetaUtil.class, "Cache metadata: " + metadataFile.getAbsolutePath() );
			metadataMap.clear();
			cacheMetadata( parseMetadataFile() );

			final String exId = getSampleIds().get( 0 );

			if( !RuntimeParamUtil.isDirectMode() )
			{
				Log.info( MetaUtil.class, META_SPACER );
				Log.info( MetaUtil.class, "===> New Metadata file: " + metadataFile.getAbsolutePath() );
				Log.info( MetaUtil.class, "===> Sample IDs: " + getSampleIds() );
				Log.info( MetaUtil.class, "===> Metadata fields: " + getFieldNames() );
				Log.info( MetaUtil.class, "===> 1st Record: [" + exId + "]: " + getMetadataRecord( exId ) );
				Log.info( MetaUtil.class, META_SPACER );
			}
		}
		else
		{
			Log.warn( MetaUtil.class, "Cannot cache metadata - invalid/missing file path in Config property: "
					+ MetaUtil.META_FILE_PATH );
		}
	}

	/**
	 * Remove the metadata column from the metadata file
	 * 
	 * @param colName Name of column to remove
	 * @param fileDir File representing output directory for new metadata file
	 * @throws Exception if errors occur
	 */
	public static void removeColumn( final String colName, final File fileDir ) throws Exception
	{
		if( getFile() == null || !getFile().exists() )
		{
			Log.warn( MetaUtil.class, "Cannot remove column [" + colName + "] because no metadata file exists." );
			return;
		}

		if( !getFieldNames().contains( colName ) )
		{
			Log.warn( MetaUtil.class, "Metadata column [" + colName
					+ "] cannot be removed, because it does not exists in: " + getFile().getAbsolutePath() );
			return;
		}

		Log.info( MetaUtil.class, "Removing field [" + colName + "] from metadata: " + getFile().getAbsolutePath() );
		final int index = MetaUtil.getFieldNames().indexOf( colName );

		final File newMeta = new File( fileDir.getAbsolutePath() + File.separator + getMetadataFileName() );
		final BufferedReader reader = BioLockJUtil.getFileReader( getFile() );
		setFile( newMeta );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getFile() ) );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				int i = 1;
				final StringTokenizer st = new StringTokenizer( line, BioLockJ.TAB_DELIM );
				writer.write( st.nextToken() );
				while( st.hasMoreTokens() )
				{
					final String token = st.nextToken();
					if( i++ != index )
					{
						writer.write( BioLockJ.TAB_DELIM + token );
					}
				}
				writer.write( BioLockJ.RETURN );
			}
		}
		finally
		{
			reader.close();
			writer.close();
			refreshCache();
		}
	}

	/**
	 * Set a new metadata file.
	 *
	 * @param newMetadataFile New metadata file
	 * @throws Exception if null parameter is passed
	 */
	public static void setFile( final File newMetadataFile ) throws Exception
	{
		if( newMetadataFile != null )
		{
			if( metadataFile != null && newMetadataFile.getAbsolutePath().equals( metadataFile.getAbsolutePath() ) )
			{
				Log.debug( MetaUtil.class, "===> MetaUtil.setFile() not required, file already defined as "
						+ metadataFile.getAbsolutePath() );
			}
		}
		else
		{
			throw new Exception( "Metadata file is required!" );
		}

		metadataFile = newMetadataFile;
	}

	private static void cacheMetadata( final List<List<String>> data ) throws Exception
	{
		int rowNum = 0;
		final Iterator<List<String>> rows = data.iterator();
		while( rows.hasNext() )
		{
			final List<String> row = rows.next();
			final String id = row.get( 0 );

			if( rowNum == 0 )
			{
				metaId = id;
				Log.debug( MetaUtil.class, "Metadata Headers: " + row );
			}

			if( rowNum++ == 1 )
			{
				Log.debug( MetaUtil.class, "Metadata Record (1st Row): " + row );
			}

			if( id != null && !id.equals( Config.requireString( META_NULL_VALUE ) ) )
			{
				row.remove( 0 );
				Log.debug( MetaUtil.class, "metadataMap add: " + id + " = " + row );

				metadataMap.put( id, row );
			}
		}
	}

	/**
	 * The processed data is trimmed, with comments removed, and empty cells replaced with NA values.
	 * 
	 * @return
	 * @throws Exception
	 */
	private static List<List<String>> parseMetadataFile() throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( getFile() );
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			Log.debug( MetaUtil.class, "===> Meta line: " + line );
			final ArrayList<String> record = new ArrayList<>();
			final String[] cells = line.split( BioLockJ.TAB_DELIM, -1 );
			for( final String cell: cells )
			{
				record.add( removeComments( setNullValueIfEmpty( cell ) ) );
			}
			data.add( record );
		}
		reader.close();
		return data;
	}

	private static String removeComments( String val ) throws Exception
	{
		if( getCommentChar().length() > 0 )
		{
			final int index = val.indexOf( getCommentChar() );
			if( index > -1 )
			{
				val = val.substring( 0, index );
			}
		}

		return val;
	}

	private static String setNullValueIfEmpty( final String val ) throws Exception
	{
		if( val == null || val.trim().isEmpty() )
		{
			return Config.requireString( META_NULL_VALUE );
		}
		return val.trim();
	}

	/**
	 * {@link biolockj.Config} property {@value #META_BARCODE_COLUMN} defines metadata column with identifying barcode
	 */
	public static final String META_BARCODE_COLUMN = "metadata.barcodeColumn";

	/**
	 * {@link biolockj.Config} property that defines how metadata columns are separated: {@value #META_COLUMN_DELIM}
	 * Typically files are tab or comma separated.
	 */
	public static final String META_COLUMN_DELIM = "metadata.columnDelim";

	/**
	 * {@link biolockj.Config} property to set metadata file comment indicator: {@value #META_COMMENT_CHAR}<br>
	 * Empty string is a valid option indicating no comments in metadata file
	 */
	public static final String META_COMMENT_CHAR = "metadata.commentChar";

	/**
	 * {@link biolockj.Config} String property: {@value #META_FILE_PATH}<br>
	 * If absolute file path, use file as metadata.<br>
	 * If directory path, must find exactly 1 file within, to use as metadata.
	 */
	public static final String META_FILE_PATH = "metadata.filePath";

	/**
	 * {@link biolockj.Config} property to set metadata file empty cell: {@value #META_NULL_VALUE}
	 */
	public static final String META_NULL_VALUE = "metadata.nullValue";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #META_REQUIRE_METADATA}<br>
	 * If Y, require metadata row for every sample in {@value biolockj.Config#INPUT_DIRS}.<br>
	 * If N, samples without metadata row will be ignored.
	 */
	public static final String META_REQUIRE_METADATA = "metadata.requireMetadata";

	/**
	 * Default column delimiter = tab character
	 */
	protected static final String DEFAULT_COL_DELIM = BioLockJ.TAB_DELIM;

	/**
	 * Default comment character for any new metadata file created by a BioModule: {@value #DEFAULT_COMMENT_CHAR}
	 */
	protected static final String DEFAULT_COMMENT_CHAR = "";

	/**
	 * Default field value to represent null-value: {@value #DEFAULT_NULL_VALUE}
	 */
	protected static final String DEFAULT_NULL_VALUE = "NA";

	private static String META_SPACER = "************************************************************************";
	private static File metadataFile = null;
	private static final Map<String, List<String>> metadataMap = new HashMap<>();

	private static String metaId = "SAMPLE_ID";
}
