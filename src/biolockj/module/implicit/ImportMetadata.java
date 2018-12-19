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
package biolockj.module.implicit;

import java.io.*;
import java.util.*;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.exception.ConfigViolationException;
import biolockj.module.BioModule;
import biolockj.module.BioModuleImpl;
import biolockj.util.*;

/**
 * This BioModule validates the contents/format of the project metadata file and the related Config properties. If
 * successful, the metadata is saved as a tab delimited text file.
 */
public class ImportMetadata extends BioModuleImpl implements BioModule
{

	@Override
	public void checkDependencies() throws Exception
	{
		inputDelim = Config.requireString( MetaUtil.META_COLUMN_DELIM );
		if( inputDelim.equals( "\\t" ) )
		{
			inputDelim = TAB_DELIM;
		}

		if( Config.getBoolean( Config.INTERNAL_MULTIPLEXED )
				&& ( MetaUtil.getFile() == null || !MetaUtil.getFile().exists() ) )
		{
			throw new Exception( "Metadata file is required for multiplexed datasets, please set Config property: "
					+ MetaUtil.META_FILE_PATH );
		}
	}

	/**
	 * Verify the metadata fields configured for R reports.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		if( getMetadata().exists() )
		{
			MetaUtil.setFile( getMetadata() );
			MetaUtil.refreshCache();
			addMetadataToConfigIgnoreInputFiles();
			if( ModuleUtil.hasRModules() )
			{
				RMetaUtil.classifyReportableMetadata();
			}
		}
		else
		{
			throw new Exception( "Failed to import metadata filels" );
		}
	}
	
	private void addMetadataToConfigIgnoreInputFiles() throws Exception
	{
		final Set<String> ignore = new HashSet<>();
		if( Config.getSet( Config.INPUT_IGNORE_FILES ) != null )
		{
			ignore.addAll( Config.getSet( Config.INPUT_IGNORE_FILES ) );
		}

		ignore.add( MetaUtil.getMetadataFileName() );
		Config.setConfigProperty( Config.INPUT_IGNORE_FILES, ignore );
	}

	/**
	 * If {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_FILE_PATH} is undefined, build a new metadata file
	 * with only 1 column of sample IDs. Otherwise, import {@value biolockj.util.MetaUtil#META_FILE_PATH} file and call
	 * {@link biolockj.util.MetaUtil#refreshCache()} to validate, format, and cache metadata as a tab delimited text
	 * file.
	 */
	@Override
	public void executeTask() throws Exception
	{
		configMeta = MetaUtil.getMetadata();
		if( configMeta == null )
		{
			buildNewMetadataFile();
		}
		else
		{
			if( ModuleUtil.moduleExists( Demultiplexer.class.getName() )
					&& MetaUtil.hasColumn( Config.getString( MetaUtil.META_FILENAME_COLUMN ) ) )
			{
				MetaUtil.removeColumn( Config.getString( MetaUtil.META_FILENAME_COLUMN ), getTempDir() );
				Log.warn( ImportMetadata.class,
						"The " + MetaUtil.META_FILENAME_COLUMN
								+ " option is incompatible with using the demultiplexer module. The supplied value \""
								+ Config.getString( MetaUtil.META_FILENAME_COLUMN ) + "\" will be ignored." );
				Config.setConfigProperty( MetaUtil.META_FILENAME_COLUMN, "" );
			}
			Log.info( getClass(),
					"Importing metadata (column delim=" + Config.requireString( MetaUtil.META_COLUMN_DELIM ) + "): "
							+ MetaUtil.getFile().getAbsolutePath() );

			final BufferedReader reader = BioLockJUtil.getFileReader( MetaUtil.getFile() );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( getMetadata() ) );
			try
			{
				int lineNum = 0;
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					writer.write( parseRow( line, lineNum++ == 0 ) );
				}
			}
			finally
			{
				reader.close();
				writer.close();
			}
			
			if( doIdToSeqVerifiction() )
			{
				MetaUtil.setFile( getMetadata() );
				MetaUtil.refreshCache();
				verifyAllRowsMapToSeqFile( getInputFiles() );
			}
		}
	}
	
	private boolean doIdToSeqVerifiction() throws Exception
	{
		return Config.getBoolean( MetaUtil.USE_EVERY_ROW ) && ( SeqUtil.isFastA() || SeqUtil.isFastQ() ) &&
				!Config.getBoolean( Config.INTERNAL_MULTIPLEXED );
	}
	
	
	

	/**
	 * The metadata file can be updated several times during pipeline execution. Summary prints the file-path of the
	 * final metadata file, along with sample and field count (obtained from {@link biolockj.util.MetaUtil}).
	 *
	 * @return Module summary of metadata path, #samples and #fields
	 */
	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			if( configMeta != null )
			{
				sb.append( "Imported file:  " + configMeta.getAbsolutePath() + RETURN );
			}

			sb.append( "# Samples: " + MetaUtil.getSampleIds().size() + RETURN );
			sb.append( "# Fields:  " + MetaUtil.getFieldNames().size() + RETURN );
		}
		catch( final Exception ex )
		{
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( getClass(), msg );
		}

		return super.getSummary() + sb.toString();
	}

	/**
	 * Create a simple metadata file in the module output directory, with only the 1st column populated with Sample IDs.
	 *
	 * @return Metadata file
	 * @throws Exception if unable to build the new file due to invalid params or I/O errors
	 */
	protected File buildNewMetadataFile() throws Exception
	{
		final File meta = getMetadata();
		final BufferedWriter writer = new BufferedWriter( new FileWriter( meta ) );
		try
		{
			writer.write( MetaUtil.getID() + BioLockJ.RETURN );

			for( final String id: getSampleIds() )
			{
				writer.write( id + BioLockJ.RETURN );
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}

		return meta;
	}

	/**
	 * Format the metadata ID to remove problematic invisible characters (particularly converted Excel files). If the
	 * first Excel cell value starts with # symbol, Excel adds a ZERO WIDTH NO-BREAK space as an invisible character.
	 * Here we strip this value; See <a href= "http://www.fileformat.info/info/unicode/char/feff/index.htm" target=
	 * "_top">http://www.fileformat.info/info/unicode/char/feff/index.htm</a>
	 *
	 * @param sampleIdColumnName Current name of metadata Sample ID column
	 * @return Formatted Sample ID column name
	 */
	protected String formatMetaId( String sampleIdColumnName )
	{
		final char c = sampleIdColumnName.trim().toCharArray()[ 0 ];
		if( c == 65279 )
		{
			Log.warn( getClass(),
					"Removed ZERO WIDTH NO-BREAK invisible character [ASCII 65279] from 1st cell in metadata file.  "
							+ "For more details, see http://www.fileformat.info/info/unicode/char/feff/index.htm" );

			final char[] chars = sampleIdColumnName.trim().toCharArray();
			for( int i = 0; i < chars.length; i++ )
			{
				Log.debug( getClass(), "ID[" + i + "] = " + chars[ i ] );
			}

			sampleIdColumnName = sampleIdColumnName.substring( 1 );
			Log.info( getClass(), "Updated ID = " + sampleIdColumnName );
		}
		return sampleIdColumnName;
	}

	/**
	 * The member variable quotedText caches the input held within a quoted block. This block can contain the
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_COLUMN_DELIM} which will be read as a character. If
	 * val closes an open quoted block, the entire quotedBlock is returned (ending with
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_COLUMN_DELIM} as all cells do) and the quotedText
	 * cache is cleared.
	 *
	 * @param val Parameter to evaluate
	 * @return Quoted text block (so far)
	 */
	protected String getQuotedValue( String val )
	{
		quotedText = quotedText + val;
		val = quotedText;
		if( val.endsWith( "\"" ) )
		{
			quotedText = "";
		}
		else
		{
			quotedText = quotedText + inputDelim;
		}
		return val;
	}

	/**
	 * Extract the sample IDs from the file names with {@link biolockj.util.SeqUtil#getSampleId(String)}
	 * 
	 * @return Ordered set of Sample IDs
	 * @throws Exception if any duplicate or invalid sample IDs are returned by {@link biolockj.util.SeqUtil}
	 */
	protected TreeSet<String> getSampleIds() throws Exception
	{
		final TreeSet<String> ids = new TreeSet<>();
		final Collection<File> inputFiles = Config.requireBoolean( Config.INTERNAL_PAIRED_READS ) ? 
			new TreeSet<File> ( SeqUtil.getPairedReads( getInputFiles() ).keySet() ) : getInputFiles();
		
		for( final File file: inputFiles )
		{
			final String id = SeqUtil.getSampleId( file.getName() );
			if( id == null || id.length() < 1 )
			{
				throw new Exception( "No Sample ID found in metadata for file: " + file.getAbsolutePath() );
			}
			else if( ids.contains( id ) )
			{
				throw new Exception(
						"Duplicate Sample ID [ " + id + " ] returned for file: " + file.getAbsolutePath() );
			}

			ids.add( id );
		}

		return ids;

	}

	/**
	 * Method called each time a line from metadata contains the
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_COLUMN_DELIM}. If the
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_COLUMN_DELIM} is encountered within a quoted block,
	 * it should be interpreted as a character (not interpreted as a column delimiter).
	 *
	 * @param val Parameter to evaluate
	 * @return true if within open quotes
	 */
	protected boolean inQuotes( final String val )
	{
		if( !quoteEnded() || !inputDelim.equals( TAB_DELIM ) && val.startsWith( "\"" ) && !val.endsWith( "\"" ) )
		{
			return true;
		}

		return false;
	}
	
	/**
	 * Verify every row (every Sample ID) maps to a sequence file
	 * 
	 * @param files List of sequence files
	 * @throws ConfigViolationException if unmapped Sample IDs are found
	 * @throws Exception if other errors occur
	 */
	protected void verifyAllRowsMapToSeqFile(final List<File> files ) throws Exception
	{
		List<String> ids = MetaUtil.getSampleIds();
		for( String id: MetaUtil.getSampleIds() )
		{
			for( File seq: files )
			{
				if( SeqUtil.isForwardRead( seq.getName() ) &&
						SeqUtil.getSampleId( seq.getName() ).equals( id ) )
				{
					ids.remove( id );
					break;
				}
			}
		}
		
		if( !ids.isEmpty() )
		{
			throw new ConfigViolationException( MetaUtil.USE_EVERY_ROW, 
					"This property requires every Sample ID in the metadata file " + MetaUtil.getMetadataFileName() +
					" map to one of the sequence files in an input directory: " + Config.getString( Config.INPUT_DIRS ) +
					BioLockJ.RETURN + "The following " + ids.size() + " Sample IDs  do not map to a sequence file: " + BioLockJUtil.printLongFormList( ids ) );
		}
	}

	/**
	 * Method called to parse a row from the metadata file, where
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_COLUMN_DELIM} separates columns. The quotedText
	 * member variable serves as a cache to build cell values contained in quotes which may include the
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_COLUMN_DELIM} as a standard character. Each row
	 * increments rowNum member variable. When the header row is processed, colNames caches the field names.
	 *
	 * @param line read from metadata file
	 * @param isHeader is true for only the first row
	 * @return parsed row value
	 * @throws Exception if required Config values are missing or invalid
	 */
	protected String parseRow( final String line, final boolean isHeader ) throws Exception
	{
		final String[] cells = line.split( inputDelim, -1 );
		int colNum = 1;

		final StringBuffer sb = new StringBuffer();
		for( String cell: cells )
		{
			cell = cell.trim();

			if( inQuotes( cell ) )
			{
				cell = getQuotedValue( cell );
				if( !quoteEnded() )
				{
					continue;
				}
			}

			if( isHeader )
			{
				verifyHeader( cell, colNames, colNum );
				if( colNames.isEmpty() )
				{
					cell = formatMetaId( cell );
					if( cell == null || cell.equals( Config.requireString( MetaUtil.META_NULL_VALUE ) ) )
					{
						continue;
					}
				}
				colNames.add( cell );
			}
			else if( cell.isEmpty() )
			{
				cell = Config.requireString( MetaUtil.META_NULL_VALUE );
				Log.debug( getClass(), "====> Set Row#[" + rowNum + "] - Column#[" + colNum + "] = " + cell );
			}

			if( colNum++ > 1 )
			{
				sb.append( TAB_DELIM );
			}

			sb.append( cell );
		}
		rowNum++;
		return sb.toString() + RETURN;
	}

	/**
	 * Verify column headers are not null and unique
	 *
	 * @param cell value of the header column name
	 * @param colNames a list of column names read so far
	 * @param colNum included for reference in error message if needed
	 * @throws Exception if a column header is null or not unique
	 */
	protected void verifyHeader( final String cell, final List<String> colNames, final int colNum ) throws Exception
	{
		if( cell.isEmpty() )
		{
			throw new Exception(
					"MetaUtil column names must not be null. Column #" + colNum + " must be given a name!" );
		}
		else if( colNames.contains( cell ) )
		{
			int j = 1;
			String dup = null;
			for( final String name: colNames )
			{
				if( name.equals( cell ) )
				{
					dup = name;
					break;
				}
				j++;
			}

			throw new Exception( "MetaUtil file column names must be unique.  Column #" + colNum
					+ " is a duplicate of Column #" + j + " - duplicate name = [" + dup + "]" );
		}
	}

	private File getMetadata()
	{
		return new File( getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );
	}

	/**
	 * Determine if quoted block has ended.
	 *
	 * @return true if quotedText cache has been cleared by {@value #getQuotedValue( String )}
	 */
	private boolean quoteEnded()
	{
		if( quotedText.equals( "" ) )
		{
			return true;
		}
		return false;
	}

	private final List<String> colNames = new ArrayList<>();
	private File configMeta = null;
	private String quotedText = "";
	private int rowNum = 0;
	private static String inputDelim = null;
}
