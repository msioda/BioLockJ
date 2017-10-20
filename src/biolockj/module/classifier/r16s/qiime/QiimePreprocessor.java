/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
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
package biolockj.module.classifier.r16s.qiime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.r16s.QiimeClassifier;
import biolockj.util.BashScriptBuilder;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;

/**
 * This class prepares QIIME input files. 1. Reorder any metadata columns if
 * required for QIIME mapping. 2. Add columns to metadata if required for QIIME
 * mapping. 3. Decompress gzipped fasta/fastq files, if any. 4.
 * Convert FastQ files to FastA format, if any.
 */
public class QiimePreprocessor extends QiimeClassifier implements ClassifierModule
{

	/**
	 * This script will unzip if gzipped files are found and will convert fastQ to fastA if needed.
	 * Otherwise, files are simply loaded to the output dir for next executor.  Last script will
	 * also create the Qiime corrected mapping file by using QIIME verifyMapping python script.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		String ext = "." + SeqUtil.getInputSequenceType();

		for( final File f: files )
		{
			final ArrayList<String> lines = new ArrayList<>();
			final String fileId = SeqUtil.getSampleId( f.getName() );
			final String zipExe = getZipExe( f );

			if( Config.getBoolean( Config.INPUT_PAIRED_READS ) )
			{
				if( SeqUtil.isForwardRead( f.getName() ) )
				{
					ext = Config.requireString( Config.INPUT_FORWARD_READ_SUFFIX ) + ext;
				}
				else
				{
					ext = Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX ) + ext;
				}
			}

			String filePath = f.getAbsolutePath();

			if( zipExe != null )
			{
				filePath = ( SeqUtil.isFastQ() ? tempDir: outDir ) + fileId + ext;
				lines.add( unzip( zipExe, f, filePath ) );
			}

			if( SeqUtil.isFastQ() )
			{
				lines.add( convert2fastA( filePath, fileId, outDir ) );
			}

			if( ( zipExe == null ) && SeqUtil.isFastA() )
			{
				lines.add( copyToOutputDir( filePath, fileId + ext ) );
			}

			failFiles.add( f );
			data.add( lines );
		}

		data.add( createQiimeCorrectedMapping() );
		failFiles.add( new File( MetaUtil.getFile().getAbsolutePath() ) );

		return data;
	}

	/**
	 * Read in required QIIME prop values.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.requireString( EXE_AWK );
	}

	/**
	 * Register num reads persample and create build script.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		SeqUtil.registerNumReadsPerSample( getInputFiles(), getTempDir() );
		BashScriptBuilder.buildScripts( this, buildScript( getInputFiles() ), failFiles,
				Config.requirePositiveInteger( Config.SCRIPT_BATCH_SIZE ) );
	}

	/**
	 * Convert file format using awk.
	 *
	 * @param filePath
	 * @param fileId
	 * @param outDir
	 * @return
	 */
	private String convert2fastA( final String filePath, final String fileId, final String outDir ) throws Exception
	{
		return "cat " + filePath + " | " + Config.requireString( EXE_AWK )
				+ " '{if(NR%4==1) {printf(\">%s \\n\",substr($0,2));} " + "else if(NR%4==2) print;}' > " + outDir
				+ fileId + "." + SeqUtil.FASTA;
	}

	private String copyMappingToOutputDir() throws Exception
	{
		final String outputMetadata = getOutputDir().getAbsolutePath() + File.separator
				+ MetaUtil.getMetadataFileName();
		MetaUtil.setFile( new File( outputMetadata ) );
		return " cp " + getMappingDir() + "*" + VALIDATED_MAPPING + " " + outputMetadata;
	}

	/**
	 * Copy files to output dir.
	 * @param source
	 * @param target
	 * @return
	 * @throws Exception
	 */
	private String copyToOutputDir( final String source, final String target ) throws Exception
	{
		return "cp " + source + " " + getOutputDir().getAbsolutePath() + File.separator + target;
	}

	/**
	 * First, print_qiime_config.py will output version info, next we create the QIIME Mapping file
	 * and rearranging columns as required by QIIME format rules.  Finally we call validate_mapping_file.py
	 * to add the proper QIIME script call to the bash script.
	 *
	 * @return
	 * @throws Exception
	 */
	private List<String> createQiimeCorrectedMapping() throws Exception
	{
		Log.out.info( "Create QIIME Specific Mapping File" );
		final List<String> lines = new ArrayList<>();
		lines.add( SCRIPT_PRINT_CONFIG );

		createQiimeMapping();
		final String alignColLine = getAlignedMetadataColumns();
		if( alignColLine != null )
		{
			Log.out.info( "Add line to BASH script to arrange QIIME mapping columns in metadata." );
			lines.add( alignColLine );
		}

		lines.add( sortMetadata() );
		lines.add( validateMapping() );
		lines.add( copyMappingToOutputDir() );
		return lines;
	}

	private void createQiimeMapping() throws Exception
	{
		if( !MetaUtil.exists() )
		{
			generateNewMappingFile();
		}
		else
		{
			generateMappingFromExistingMetadata();
		}

		MetaUtil.refresh();
	}

	/**
	 * Create QIIME mapping based on metadata file, output to temp/QIIME_MAPPING.
	 * Add required fields if missing.
	 * @throws Exception
	 */
	private void generateMappingFromExistingMetadata() throws Exception
	{
		final BufferedReader reader = new BufferedReader( new FileReader( MetaUtil.getFile() ) );
		MetaUtil.setFile( new File( getQiimeMapping() ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( MetaUtil.getFile() ) );

		final boolean hasQm1 = MetaUtil.getAttributeNames().contains( BARCODE_SEQUENCE );
		final boolean hasQm2 = MetaUtil.getAttributeNames().contains( LINKER_PRIMER_SEQUENCE );
		final boolean hasQm3 = MetaUtil.getAttributeNames().contains( DEMUX_COLUMN );
		final boolean hasQm4 = MetaUtil.getAttributeNames().contains( DESCRIPTION );

		boolean isHeaderRow = true;
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
			boolean firstColumn = true;
			String id = null;

			while( st.hasMoreTokens() )
			{
				final String next = st.nextToken();
				if( firstColumn )
				{
					firstColumn = false;
					if( isHeaderRow )
					{
						writer.write( QIIME_ID + TAB_DELIM );

						if( !hasQm1 )
						{
							writer.write( BARCODE_SEQUENCE + TAB_DELIM );
						}

						if( !hasQm2 )
						{
							writer.write( LINKER_PRIMER_SEQUENCE + TAB_DELIM );
						}
					}
					else
					{
						id = next;
						writer.write( id + TAB_DELIM );

						if( !hasQm1 )
						{
							writer.write( TAB_DELIM );
						}

						if( !hasQm2 )
						{
							writer.write( TAB_DELIM );
						}
					}
				}
				else
				{
					writer.write( next + TAB_DELIM );
				}
			}

			if( isHeaderRow )
			{
				if( !hasQm3 )
				{
					writer.write( DEMUX_COLUMN + TAB_DELIM );
				}

				if( !hasQm4 )
				{
					writer.write( DESCRIPTION );
				}

				isHeaderRow = false;
			}
			else
			{
				if( !hasQm3 )
				{
					writer.write( id + "." + SeqUtil.FASTA + TAB_DELIM );
				}

				if( !hasQm4 )
				{
					writer.write( QIIME_COMMENT );
				}
			}

			writer.write( RETURN );
		}

		reader.close();
		writer.close();
	}

	private void generateNewMappingFile() throws Exception
	{
		MetaUtil.setFile( new File( getQiimeMapping() ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( MetaUtil.getFile() ) );
		writer.write( QIIME_ID + TAB_DELIM + BARCODE_SEQUENCE + TAB_DELIM + LINKER_PRIMER_SEQUENCE + TAB_DELIM
				+ DEMUX_COLUMN + TAB_DELIM + DESCRIPTION + RETURN );

		for( final File f: getInputFiles() )
		{
			final String id = SeqUtil.getSampleId( f.getName() );

			writer.write( id + TAB_DELIM + TAB_DELIM + TAB_DELIM + id + "." + SeqUtil.FASTA + TAB_DELIM + QIIME_COMMENT
					+ RETURN );
		}

		writer.close();
	}

	/**
	 * If QIIME required fields exist in metadata, but are not in proper position,
	 * output line for bash script that will move the column to the proper position.
	 * --> BarcodeSequence = col 2
	 * --> LinkerPrimerSequence = col 3
	 * --> InputFileName = 2nd to last col
	 * --> Description = last col
	 * @return String - line for bash script
	 * @throws Exception
	 */
	private String getAlignedMetadataColumns() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( Config.requireString( EXE_AWK ) + " -F'\\" + TAB_DELIM + "' -v OFS=\"\\" + TAB_DELIM
				+ "\" '{ print $1" );
		final List<String> cols = MetaUtil.getAttributes( QIIME_ID );

		final boolean hasQm1 = MetaUtil.getAttributeNames().contains( BARCODE_SEQUENCE );
		final boolean hasQm2 = MetaUtil.getAttributeNames().contains( LINKER_PRIMER_SEQUENCE );
		final boolean hasQm3 = MetaUtil.getAttributeNames().contains( DEMUX_COLUMN );
		final boolean hasQm4 = MetaUtil.getAttributeNames().contains( DESCRIPTION );

		final int numCols = cols.size();
		int demuxIndex = numCols;
		int descIndex = numCols + 1;

		final List<Integer> colsToSkip = new ArrayList<>();
		if( hasQm1 && !cols.get( 0 ).equals( BARCODE_SEQUENCE ) )
		{
			skipIndex( BARCODE_SEQUENCE, cols, sb, colsToSkip, "column #2" );
		}
		if( hasQm2 && ( numCols > 1 ) && !cols.get( 1 ).equals( LINKER_PRIMER_SEQUENCE ) )
		{
			skipIndex( LINKER_PRIMER_SEQUENCE, cols, sb, colsToSkip, "column #3" );
		}
		if( hasQm3 && ( numCols > 2 ) && !cols.get( ( numCols - 2 ) ).equals( DEMUX_COLUMN ) )
		{
			demuxIndex = skipIndex( DEMUX_COLUMN, cols, sb, colsToSkip, " 2nd to last column" );
		}
		if( hasQm4 && !cols.get( ( numCols - 1 ) ).equals( DESCRIPTION ) )
		{
			descIndex = skipIndex( DESCRIPTION, cols, sb, colsToSkip, " last column" );
		}

		if( colsToSkip.isEmpty() )
		{
			Log.out.info( "MetaUtil does not contain QIIME specific fields to reorder." );
			return null;
		}

		for( int i = 0; i < colsToSkip.size(); i++ )
		{
			Log.out.debug( "colsToSkip(" + i + ")=" + colsToSkip.get( i ) );
		}

		for( int i = 2; i < ( numCols + 2 ); i++ )
		{
			if( !colsToSkip.contains( i ) )
			{
				Log.out.debug( "colsToSkip() must not contain =" + i );
				sb.append( ", $" + i );
			}
		}

		if( demuxIndex != numCols )
		{
			sb.append( ", $" + demuxIndex );
		}

		if( descIndex != ( numCols + 1 ) )
		{
			sb.append( ", $" + descIndex );
		}

		final String path = getTempDir().getAbsolutePath() + File.separator + ORDERED_MAPPING;
		sb.append( " }' " + MetaUtil.getFile().getAbsolutePath() + " > " + path );
		MetaUtil.setFile( new File( path ) );

		return sb.toString();
	}

	/**
	 * Get mapping dir, called "mapping" which is the directory the new mapping is output by Qiime
	 * validate_mapping_file.py.
	 * @return
	 * @throws Exception
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

	private String getQiimeMapping() throws Exception
	{
		return getTempDir().getAbsolutePath() + File.separator + QIIME_MAPPING;
	}

	private String getSortedMapping() throws Exception
	{
		return getTempDir().getAbsolutePath() + File.separator + SORTED_MAPPING;
	}

	/**
	 * Get zipExe from prop file.
	 * @param f
	 * @return
	 * @throws Exception
	 */
	private String getZipExe( final File f ) throws Exception
	{
		final String name = f.getName().toLowerCase();
		if( name.endsWith( ".gz" ) )
		{
			return Config.requireString( EXE_GZIP );
		}

		return null;
	}

	/**
	 * When rearranging files, skip any index when adding columns, if it will be moved.
	 * @param field
	 * @param cols
	 * @param sb
	 * @param colsToSkip
	 * @param colMsg
	 * @return
	 */
	private int skipIndex( final String field, final List<String> cols, final StringBuffer sb,
			final List<Integer> colsToSkip, final String colMsg )
	{
		final int index = ( cols.indexOf( field ) + 2 );
		sb.append( ", $" + index ); // $9
		colsToSkip.add( index ); // 9
		Log.out.info( field + " found in column #" + index + " but QIIME requires " + colMsg );
		return index;
	}

	private String sortMetadata() throws Exception
	{
		final String map = MetaUtil.getFile().getAbsolutePath();
		return "(head -n 1 " + map + " && tail -n +2 " + map + " | sort -n) > " + getSortedMapping();
	}

	/**
	 * Get line for bash script to unzip file.
	 * @param zipExe
	 * @param f
	 * @param filePath
	 * @return
	 */
	private String unzip( final String zipExe, final File f, final String filePath )
	{
		return zipExe + " -cd " + f.getAbsolutePath() + " > " + filePath;
	}

	/**
	 * Call validate_mapping_file.py to get corrected QIIME Mapping.
	 * @return
	 * @throws Exception
	 */
	private String validateMapping() throws Exception
	{
		return SCRIPT_VALIDATE_MAPPING + getSortedMapping() + " -o " + getMappingDir() + " -j " + DEMUX_COLUMN;
	}

	private final List<File> failFiles = new ArrayList<>();
	private static final String EXE_GZIP = "exe.gzip";
	private static final String ORDERED_MAPPING = "orderedMapping.tsv";
	private static final String QIIME_COMMENT = "BioLockJ Generated Mapping";
	private static final String QIIME_ID = "#SampleID";
	private static final String QIIME_MAPPING = "qiimeMapping.tsv";
	private static final String SCRIPT_PRINT_CONFIG = "print_qiime_config.py -t";
	private static final String SCRIPT_VALIDATE_MAPPING = "validate_mapping_file.py -p -b -m ";
	private static final String SORTED_MAPPING = "sortedMapping.txt";
	private static final String VALIDATED_MAPPING = "_corrected.txt";
}
