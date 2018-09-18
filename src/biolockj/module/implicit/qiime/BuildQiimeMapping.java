/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.qiime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import biolockj.BioModuleFactory;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.ScriptModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule converts the metadata file into a tab delimited QIIME mapping file (if provided).<br>
 * The QIIME mapping file is validated by calling QIIME script validate_mapping_file.py<br>
 * <p>
 * QIIME mapping file requirements:
 * <ol>
 * <li>#SampleID = Required name of 1st column, holds sample unique identifiers, not nullable
 * <li>BarcodeSequence = Required name of 2nd column, is nullable
 * <li>LinkerPrimerSequence = Required name of 3rd column, is nullable
 * <li>InputFileName = Column is required by add_qiime_labels.py, containing demultiplexed file names, not nullable
 * <li>Description = Required name of last column, not nullable
 * </ol>
 * <p>
 * Steps to convert metadata into QIIME mapping:
 * <ol>
 * <li>Reorder existing QIIME mapping columns, if any are out of order
 * <li>Rename the 1st column (containing sample IDs) to the required value: #SampleID
 * <li>Add missing QIIME mapping columns, if any are missing
 * <li>Rename the validated mapping file to the original metadata file name and save to the module output directory.
 * </ol>
 */
public class BuildQiimeMapping extends ScriptModuleImpl implements ScriptModule
{

	/**
	 * Create QIIME mapping based on metadata file, output to temp dir. Add required fields if missing.
	 *
	 * @return New Metadata file
	 * @throws Exception if unable to build the mapping file
	 */
	public File addMissingFields() throws Exception
	{
		final boolean hasQm1 = MetaUtil.getFieldNames().contains( BARCODE_SEQUENCE );
		final boolean hasQm2 = MetaUtil.getFieldNames().contains( LINKER_PRIMER_SEQUENCE );
		final boolean hasQm3 = MetaUtil.getFieldNames().contains( DEMUX_COLUMN );
		final boolean hasQm4 = MetaUtil.getFieldNames().contains( DESCRIPTION );

		final Map<String, String> metaLines = new HashMap<>();
		String header = null;
		final BufferedReader reader = SeqUtil.getFileReader( MetaUtil.getFile() );
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
			metaLines.put( st.nextToken(), line );
			if( header == null )
			{
				Log.get( getClass() ).info( "Initial metadata columns = " + line );
				header = line;
			}
		}
		reader.close();

		MetaUtil.setFile( getQiimeMapping() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( MetaUtil.getFile() ) );
		writer.write( QIIME_ID );
		if( !hasQm1 )
		{
			Log.get( getClass() ).info( "Add required column(2) field = " + BARCODE_SEQUENCE );
			writer.write( TAB_DELIM + BARCODE_SEQUENCE );
		}

		if( !hasQm2 )
		{
			Log.get( getClass() ).info( "Add required column(3) field = " + LINKER_PRIMER_SEQUENCE );
			writer.write( TAB_DELIM + LINKER_PRIMER_SEQUENCE );
		}
		final StringTokenizer ht = new StringTokenizer( header, TAB_DELIM );
		ht.nextToken(); // skip the ID column
		while( ht.hasMoreTokens() )
		{
			writer.write( TAB_DELIM + ht.nextToken() );
		}
		if( !hasQm3 )
		{
			Log.get( getClass() ).info( "Add required column(n-1) field = " + DEMUX_COLUMN );
			writer.write( TAB_DELIM + DEMUX_COLUMN );
		}

		if( !hasQm4 )
		{
			Log.get( getClass() ).info( "Add required column(n) field = " + DESCRIPTION );
			writer.write( TAB_DELIM + DESCRIPTION );
		}

		writer.write( RETURN );

		for( final File f: getInputFiles() )
		{
			for( final String key: metaLines.keySet() )
			{
				if( !SeqUtil.getSampleId( f.getName() ).equals( key ) )
				{
					continue;
				}

				writer.write( key + TAB_DELIM );
				if( !hasQm1 )
				{
					writer.write( Config.requireString( MetaUtil.META_NULL_VALUE ) + TAB_DELIM );
				}

				if( !hasQm2 )
				{
					writer.write( Config.requireString( MetaUtil.META_NULL_VALUE ) + TAB_DELIM );
				}

				final StringTokenizer st = new StringTokenizer( metaLines.get( key ), TAB_DELIM );
				st.nextToken(); // skip the id
				writer.write( st.nextToken() );
				while( st.hasMoreTokens() )
				{
					writer.write( TAB_DELIM + st.nextToken() );
				}

				if( !hasQm3 )
				{

					writer.write( TAB_DELIM + f.getName() );
					// writer.write( TAB_DELIM + key + "." + SeqUtil.FASTA );
				}

				if( !hasQm4 )
				{
					writer.write( TAB_DELIM + QIIME_COMMENT );
				}

				writer.write( RETURN );
			}
		}

		writer.close();
		MetaUtil.refreshCache();
		return getQiimeMapping();
	}

	/**
	 * Create the Qiime corrected mapping file by building a new metadata file with ordered columns, and verify the
	 * format via the {@value #SCRIPT_VALIDATE_MAPPING} script.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		initMetaFile = MetaUtil.getFile();
		data.add( createQiimeCorrectedMapping() );
		return data;
	}

	/**
	 * If paired reads found, add prerequisite module: {@link biolockj.module.seq.PearMergeReads}. If sequence files are
	 * not in fasta format, add prerequisite module: {@link biolockj.module.seq.AwkFastaConverter}.
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		final List<Class<?>> preReqs = super.getPreRequisiteModules();
		if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			preReqs.add( Class.forName( BioModuleFactory.getDefaultMergePairedReadsConverter() ) );
		}
		if( !SeqUtil.isFastA() )
		{
			preReqs.add( Class.forName( BioModuleFactory.getDefaultFastaConverter() ) );
		}

		return preReqs;
	}

	/**
	 * Message summarizes contents of {@link biolockj.util.SummaryUtil#getScriptDirSummary(ScriptModule) scriptDir}, and
	 * status of the QIIME mapping files (exists or not).
	 */
	@Override
	public String getSummary() throws Exception
	{
		final String msg = ( getOutputDir().listFiles().length == 0 ? "QIIME Mapping not found"
				: "Generated QIIME mapping file" ) + RETURN;

		return super.getSummary() + msg;
	}

	/**
	 * This method generates the bash function used to reorder columns as per QIIME requirements:
	 * {@value #FUNCTION_REORDER_FIELDS}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> cols = getMetaCols();
		if( cols == null )
		{
			return null;
		}

		final List<Integer> skip = Arrays.asList( new Integer[] { cols.indexOf( BARCODE_SEQUENCE ),
				cols.indexOf( LINKER_PRIMER_SEQUENCE ), cols.indexOf( DEMUX_COLUMN ), cols.indexOf( DESCRIPTION ) } );

		final StringBuffer sb = new StringBuffer();
		sb.append(
				Config.getExe( Config.EXE_AWK ) + " -F'\\" + TAB_DELIM + "' -v OFS=\"\\" + TAB_DELIM + "\" '{ print $1,"
						+ colIndex( cols, BARCODE_SEQUENCE ) + "," + colIndex( cols, LINKER_PRIMER_SEQUENCE ) + "," );

		for( int i = 1; i < cols.size(); i++ )
		{
			if( !skip.contains( i ) )
			{
				sb.append( " $" + ( i + 1 ) + "," );
			}
		}

		sb.append( colIndex( cols, DEMUX_COLUMN ) + "," );
		sb.append( colIndex( cols, DESCRIPTION ) );
		sb.append( " }' " + initMetaFile.getAbsolutePath() + " > " + getOrderedMapping().getAbsolutePath() );

		Log.get( getClass() )
				.debug( FUNCTION_REORDER_FIELDS + " will update column order using awk --> " + sb.toString() );

		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_REORDER_FIELDS + "() {" );
		lines.add( sb.toString() );
		lines.add( "}" );
		return lines;

	}

	private String colIndex( final List<String> cols, final String name )
	{
		return " $" + ( cols.indexOf( name ) + 1 );
	}

	/**
	 * Create the bash script line that will save a copy of the validated mapping file into the output directory,
	 * renamed to the file name configured in {@link biolockj.util.MetaUtil#META_FILE_PATH
	 * Config}.{@value biolockj.util.MetaUtil#META_FILE_PATH}
	 *
	 * @return Bash script line to save mapping file to output dir
	 * @throws Exception if unable to create the bash script line
	 */
	private String copyMappingToOutputDir() throws Exception
	{
		return " cp " + getMappingDir() + "*" + VALIDATED_MAPPING + " " + getOutputDir().getAbsolutePath()
				+ File.separator + MetaUtil.getMetadataFileName();
	}

	/**
	 * Create QIIME Mapping file with columns arranged as required by QIIME. Build bash script line calling
	 * {@value #SCRIPT_VALIDATE_MAPPING} to verify file format. The updated metadata file, containing all fields, is
	 * saved to initMetaFile for use in the bash script function used to reorder the columns.
	 *
	 * @return Bash script lines to create the corrected mapping file
	 * @throws Exception if unable to build the mapping or return the script lines
	 */
	private List<String> createQiimeCorrectedMapping() throws Exception
	{
		Log.get( getClass() ).info( "Create QIIME Specific Mapping File" );
		final List<String> lines = new ArrayList<>();
		initMetaFile = addMissingFields();
		final List<String> cols = getMetaCols();
		if( cols != null )
		{
			Log.get( getClass() ).info( "Metadata column order:" );
			int d = 0;
			for( final String col: cols )
			{
				Log.get( getClass() ).info( "col[" + d++ + "] = " + col );
			}

			lines.add( FUNCTION_REORDER_FIELDS );
			MetaUtil.setFile( getOrderedMapping() );
		}
		else
		{
			Log.get( getClass() ).info( "Qiime mapping file columns already in order!" );
		}

		lines.add( validateMapping() );
		lines.add( copyMappingToOutputDir() );
		return lines;
	}

	/**
	 * Get mapping dir, called "mapping" which is the directory the new mapping is output by Qiime
	 * {@value #SCRIPT_VALIDATE_MAPPING}.
	 *
	 * @return Path to {@value #SCRIPT_VALIDATE_MAPPING} output dir
	 * @throws Exception if unable to find temp dir
	 */
	private String getMappingDir() throws Exception
	{
		final File dir = new File( getTempDir().getAbsolutePath() + File.separator + "mapping" );
		if( !dir.exists() )
		{
			dir.mkdirs();
		}

		return dir.getAbsolutePath() + File.separator;
	}

	private List<String> getMetaCols() throws Exception
	{
		final List<String> cols = new ArrayList<>();
		cols.add( MetaUtil.getID() );
		cols.addAll( MetaUtil.getFieldNames() );

		if( cols.indexOf( BARCODE_SEQUENCE ) == 1 && cols.indexOf( LINKER_PRIMER_SEQUENCE ) == 2
				&& cols.indexOf( DEMUX_COLUMN ) == cols.size() - 2 && cols.indexOf( DESCRIPTION ) == cols.size() - 1 )
		{
			return null;
		}

		return cols;
	}

	private File getOrderedMapping() throws Exception
	{
		final File orderedDir = new File( getTempDir().getAbsolutePath() + File.separator + "orderedColumns" );
		if( !orderedDir.exists() )
		{
			orderedDir.mkdirs();
		}

		return new File( orderedDir.getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );
	}

	private File getQiimeMapping() throws Exception
	{
		return new File( getTempDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );
	}

	/**
	 * Call validate_mapping_file.py to get corrected QIIME Mapping.
	 */
	private String validateMapping() throws Exception
	{
		return SCRIPT_VALIDATE_MAPPING + " -p -b -m " + MetaUtil.getFile().getAbsolutePath() + " -o " + getMappingDir()
				+ " -j " + DEMUX_COLUMN;
	}

	private File initMetaFile = null;

	/**
	 * QIIME mapping file required 2nd column name
	 */
	public static final String BARCODE_SEQUENCE = "BarcodeSequence";

	/**
	 * QIIME mapping file column that holds the name of the sample demultiplexed sequence file.
	 */
	public static final String DEMUX_COLUMN = QiimeClassifier.DEMUX_COLUMN;

	/**
	 * QIIME mapping file required name of last column
	 */
	public static final String DESCRIPTION = "Description";

	/**
	 * QIIME mapping file required 3rd column name
	 */
	public static final String LINKER_PRIMER_SEQUENCE = "LinkerPrimerSequence";

	/**
	 * Comment used to populate {@value #DESCRIPTION} column
	 */
	public static final String QIIME_COMMENT = "BioLockJ Generated Mapping";

	/**
	 * QIIME mapping file required 1st column name, containing the sample ID
	 */
	public static final String QIIME_ID = "#SampleID";

	/**
	 * QIIME script used to validate the QIIME mapping file format
	 */
	public static final String SCRIPT_VALIDATE_MAPPING = "validate_mapping_file.py";

	/**
	 * Suffix appended to the validate QIIME mapping file output to temp/mapping dir by bash script
	 */
	public static final String VALIDATED_MAPPING = "_corrected.txt";

	/**
	 * Name of the bash function that reorders metadata columns: {@value #FUNCTION_REORDER_FIELDS}
	 */
	protected static final String FUNCTION_REORDER_FIELDS = "reorderColumns";
}
