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
	 * @param removeMissingIds if TRUE, sampleIds not include in the map param will be removed from the metadata
	 * @throws Exception if unable to add the new column
	 */
	public static void addColumn( final String colName, final Map<String, String> map, final File fileDir,
			final boolean removeMissingIds ) throws Exception
	{
		final File newMeta = new File( fileDir.getAbsolutePath() + File.separator + getMetadataFileName() );
		Log.info( MetaUtil.class, "Adding new field [" + colName + "] to metadata: " + newMeta.getAbsolutePath() );
		if( getFieldNames().contains( colName ) )
		{
			Log.warn( MetaUtil.class,
					"Metadata column [" + colName + "] already exists in: " + getFile().getAbsolutePath() );
			return;
		}

		final Set<String> sampleIds = map.keySet();
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
				if( sampleIds.contains( id ) )
				{
					writer.write( line + BioLockJ.TAB_DELIM + map.get( id ) + BioLockJ.RETURN );
				}
				else if( !removeMissingIds )
				{
					writer.write(
							line + BioLockJ.TAB_DELIM + Config.requireString( META_NULL_VALUE ) + BioLockJ.RETURN );
				}
				else
				{
					Log.warn( MetaUtil.class,
							"REMOVE SAMPLE ID [" + id + "] due to no data in metadata column: " + colName );
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
	 * Determine if metadata file exists
	 *
	 * @return TRUE if metadata exists
	 */
	public static boolean exists()
	{
		return getFile() != null && getFile().exists();
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
			throw new Exception( "Metadata file path is undefined.  Cannot get metadata field names." );
		}

		final List<String> headers = getMetadataRecord( metaId );

		if( headers == null )
		{
			throw new Exception( "Metadata headers not found!  Please verify 1st column header is not missing from: "
					+ getFile().getAbsolutePath() );
		}

		if( debug )
		{
			Log.debug( MetaUtil.class, "Found metadata headers: " + BioLockJUtil.getCollectionAsString( headers ) );
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
	 * Used to generate a guaranteed to be unique column name. If parameter "name" already exists, this will return
	 * name_1, or name_2, etc until an unused column name is found. If a column already exists, but contains only
	 * {@link biolockj.Config}.{@value #META_NULL_VALUE} values, return the name of that empty column to overwrite.
	 * 
	 * @param name Base column name
	 * @return Column name
	 * @throws Exception if errors occur
	 */
	public static String getForcedColumnName( final String name ) throws Exception
	{
		int suffix = 1;
		String testName = name;
		while( getFieldNames().contains( testName ) )
		{
			final Set<String> testSet = new HashSet<>( getFieldValues( testName ) );
			testSet.remove( Config.requireString( META_NULL_VALUE ) );
			if( testSet.isEmpty() )
			{
				MetaUtil.removeColumn( testName, null );
				break; // reuse the column
			}

			testName = name + "_" + suffix++;
		}
		return testName;
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
	 * Returns the latest version of the given column.
	 * 
	 * @param name Base column name
	 * @return column name
	 * @throws Exception if errors occur
	 */
	public static String getLatestColumnName( final String name ) throws Exception
	{
		int suffix = 1;
		String testName = name;
		String foundName = null;
		while( getFieldNames().contains( testName ) )
		{
			foundName = testName;
			testName = name + "_" + suffix++;
		}
		return foundName;
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
	 * #throws Exception if errors occur
	 */
	public static String getMetadataFileName() throws Exception
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

		return Config.requireString( Config.PROJECT_PIPELINE_NAME ) + BioLockJ.TSV_EXT;

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

		Collections.sort( ids );

		return ids;
	}

	/**
	 * Check if columnName exists in the current metadata file.
	 * 
	 * @param columnName Column name
	 * @return TRUE if columnName exists in hearder row of metadata file
	 */
	public static boolean hasColumn( final String columnName )
	{
		try
		{
			return columnName != null && getFieldNames().contains( columnName );
		}
		catch( final Exception ex )
		{
			return false;
		}
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
		
		if( Config.getString( META_COMMENT_CHAR ) == null )
		{
			Config.setConfigProperty( META_COMMENT_CHAR, DEFAULT_COMMENT_CHAR );
		}

		String commentChar = Config.getString( MetaUtil.META_COMMENT_CHAR );
		if( commentChar != null && commentChar.length() > 1 )
		{
			throw new Exception( META_COMMENT_CHAR + " property must be a single character.  Config value = \""
							+ commentChar + "\"" );
		}

		if( Config.getString( META_FILE_PATH ) != null )
		{
			final Set<String> ignore = Config.getSet( Config.INPUT_IGNORE_FILES );
			ignore.add( MetaUtil.getMetadataFileName() );
			Config.setConfigProperty( Config.INPUT_IGNORE_FILES, ignore );
			setFile( MetaUtil.getMetadata() );
			refreshCache();

			// verify that values in columns used as identifiers are unique per sample.
			checkUniqueVals( META_BARCODE_COLUMN );
			checkUniqueVals( META_FILENAME_COLUMN );
		}
	}

	/**
	 * Refresh the metadata cache.
	 *
	 * @throws Exception if unable to refresh cache
	 */
	public static void refreshCache() throws Exception
	{
		if( isUpdated() )
		{
			Log.info( MetaUtil.class, "Update metadata cache: " + metadataFile.getAbsolutePath() );
			metadataMap.clear();
			cacheMetadata( parseMetadataFile() );

			if( !RuntimeParamUtil.isDirectMode() )
			{
				report();
			}

			reportedMetadata = metadataFile;
		}
		else if( debug )
		{
			Log.debug( MetaUtil.class, "Skip metadata refresh cache, path unchanged: "
					+ ( metadataFile == null ? "<NO_METADATA_PATH>": metadataFile.getAbsolutePath() ) );
		}
	}

	/**
	 * Remove the metadata column from the metadata file
	 * 
	 * @param colName Name of column to remove
	 * @param fileDir File representing output directory for new metadata file
	 * @throws Exception if errors occur
	 */
	public static void removeColumn( final String colName, File fileDir ) throws Exception
	{
		if( fileDir == null )
		{
			fileDir = new File( Config.requireExistingDir( Config.PROJECT_PIPELINE_DIR ).getAbsolutePath()
					+ File.separator + ".temp" );
			if( !fileDir.exists() )
			{
				fileDir.mkdir();
			}
		}

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
	 * @param file New metadata file
	 * @throws Exception if null parameter is passed
	 */
	public static void setFile( final File file ) throws Exception
	{
		if( file != null )
		{
			if( metadataFile != null && file.getAbsolutePath().equals( metadataFile.getAbsolutePath() ) )
			{
				if( debug )
				{
					Log.debug( MetaUtil.class, "===> MetaUtil.setFile() not required, file already defined as "
							+ metadataFile.getAbsolutePath() );
				}
			}
		}
		else
		{
			throw new Exception( "Must pass valid file to MetaUtil.setFile( file ), found value: "
					+ ( file == null ? "": file.getAbsolutePath() ) );
		}

		metadataFile = file;
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
				if( debug && isUpdated() )
				{
					Log.debug( MetaUtil.class, "Metadata Headers: " + row );
				}
			}

			if( rowNum++ == 1 && debug && isUpdated() )
			{
				Log.debug( MetaUtil.class, "Metadata Record (1st Row): " + row );
			}

			if( id != null && !id.equals( Config.requireString( META_NULL_VALUE ) ) )
			{
				row.remove( 0 );
				if( isUpdated() )
				{
					Log.debug( MetaUtil.class, "metadataMap add: " + id + " = " + row );
				}

				metadataMap.put( id, row );
			}
		}
	}

	/**
	 * Verify that a column in the metadata is unique for each sample
	 * 
	 * @param columnAttr {@link biolockj.Config} property giving the name of the column.
	 * @throws Exception if number of unique values in the column does not match number of samples
	 */
	private static void checkUniqueVals( final String columnAttr ) throws Exception
	{
		final String columnName = Config.getString( columnAttr );
		if( columnName != null )
		{
			if( MetaUtil.getFieldNames().contains( columnName ) )
			{
				final int lenSamples = new HashSet<>( getSampleIds() ).size();
				final int lenVals = new HashSet<>( getFieldValues( columnName ) ).size(); // get unique values
				if( lenSamples != lenVals )
				{
					throw new Exception( "Should have exactly 1 unique value per sample in column " + columnName
							+ ". Found " + lenVals + " unique values for " + lenSamples + " samples." );
				}
				Log.info( MetaUtil.class, "Parameter [" + columnAttr + "] with value [" + columnName
						+ "] gives a column of unique values in the meta data file." );
			}
			else
			{
				Log.info( MetaUtil.class, "Parameter [" + columnAttr + "] with value [" + columnName
						+ "] is ignored because it does not appear as a header in the meta data file." );
			}
		}
	}

	private static boolean isUpdated()
	{
		return metadataFile != null && metadataFile.exists() && reportedMetadata == null
				|| !reportedMetadata.getAbsolutePath().equals( metadataFile.getAbsolutePath() );
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
			if( debug && isUpdated() )
			{
				Log.debug( MetaUtil.class, "===> Meta line: " + line );
			}
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
		final String commentChar = Config.getString( META_COMMENT_CHAR );
		if( commentChar != null && commentChar.length() > 0 )
		{
			final int index = val.indexOf( commentChar );
			if( index > -1 )
			{
				val = val.substring( 0, index );
			}
		}

		return val;
	}

	/**
	 * Report metadata sample IDs, field names, 1 example row.
	 * 
	 * @throws Exception
	 */
	private static void report() throws Exception
	{
		final String exId = getSampleIds().get( 0 );
		Log.info( MetaUtil.class, META_SPACER );
		Log.info( MetaUtil.class, "===> New Metadata file: " + metadataFile.getAbsolutePath() );
		Log.info( MetaUtil.class, "===> Sample IDs: " + getSampleIds() );
		Log.info( MetaUtil.class, "===> Metadata fields: " + getFieldNames() );
		Log.info( MetaUtil.class, "===> 1st Record: [" + exId + "]: " + getMetadataRecord( exId ) );
		Log.info( MetaUtil.class, META_SPACER );
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
	 * {@link biolockj.Config} property {@value #META_FILENAME_COLUMN} defines metadata column with input file names
	 */
	public static final String META_FILENAME_COLUMN = "metadata.fileNameColumn";

	/**
	 * {@link biolockj.Config} property to set metadata file empty cell: {@value #META_NULL_VALUE}
	 */
	public static final String META_NULL_VALUE = "metadata.nullValue";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #META_REQUIRED}<br>
	 * If Y, require metadata row for each sample with sequence data in {@value biolockj.Config#INPUT_DIRS}.<br>
	 * If N, samples without metadata are ignored.
	 */
	public static final String META_REQUIRED = "metadata.required";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #USE_EVERY_ROW}<br>
	 * If Y, require a sequence file for every SampleID (every row) in {@value #META_FILE_PATH}.<br>
	 * If N, metadata can include extraneous SampleIDs.
	 */
	public static final String USE_EVERY_ROW = "metadata.useEveryRow";

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

	private static boolean debug = false;
	private static String META_SPACER = "************************************************************************";
	private static File metadataFile = null;
	private static final Map<String, List<String>> metadataMap = new HashMap<>();
	private static String metaId = "SAMPLE_ID";
	private static File reportedMetadata = null;
}
