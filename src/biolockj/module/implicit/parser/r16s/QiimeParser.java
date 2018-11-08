/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.parser.r16s;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;
import biolockj.BioLockJ;
import biolockj.Log;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.node.OtuNode;
import biolockj.node.ParsedSample;
import biolockj.node.r16s.QiimeNode;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModules parses QiimeClassifier output reports to build standard OTU abundance tables.
 */
public class QiimeParser extends ParserModuleImpl implements ParserModule
{

	@Override
	public void cleanUp() throws Exception
	{
		setOrderedQiimeIDs( getInputFiles().get( 0 ) );
		super.cleanUp();
	}

	/**
	 * Get only the lowest level report since it contains taxa info for all higher OTU reports.
	 */
	@Override
	public List<File> getInputFiles() throws Exception
	{
		File lowestLevelReport = null;
		Integer levelNum = null;
		for( final File file: super.getInputFiles() )
		{
			final Integer reportLevel = Integer.valueOf( file.getName()
					.substring( QiimeClassifier.OTU_TABLE_PREFIX.length() + 2, file.getName().length() - 4 ) );

			if( levelNum == null || levelNum < reportLevel )
			{
				levelNum = reportLevel;
				lowestLevelReport = file;
			}
		}

		final List<File> files = new ArrayList<>();
		files.add( lowestLevelReport );
		return files;
	}

	/**
	 * Parse lowest level {@link biolockj.module.implicit.qiime.QiimeClassifier} report in the input directory. The
	 * classifier report lists 1 OTU/line with 1 column/sample, each column holds the OTU count for the QIIME ID in the
	 * column header. For each line, build a {@link biolockj.node.r16s.QiimeNode} for each sample with a positive OTU
	 * count.
	 * <p>
	 * {@link biolockj.node.r16s.QiimeNode}s will be created using QiimeID (not SampleID) in order to match the metadata
	 * file #SampleID
	 * <p>
	 * If {@link #isValid(OtuNode)}:
	 * <ol>
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.r16s.QiimeNode#getSampleId()} if not
	 * yet created.
	 * <li>Add the {@link biolockj.node.r16s.QiimeNode#getCount()} (1) to {@link biolockj.node.ParsedSample} OTU count.
	 * </ol>
	 */
	@Override
	public void parseSamples() throws Exception
	{
		final File file = getInputFiles().get( 0 );
		Log.info( getClass() , "Parse file: " + file.getName() );
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			if( line.startsWith( "#" ) )
			{
				continue;
			}

			final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
			int index = 0;
			final String taxas = st.nextToken();

			while( st.hasMoreTokens() )
			{
				final Long count = Double.valueOf( st.nextToken() ).longValue();
				final String id = orderedQiimeIDs.get( index++ );
				if( count > 0 )
				{
					final QiimeNode node = new QiimeNode( id, taxas, count );
					if( isValid( node ) )
					{
						final ParsedSample sample = getParsedSample( node.getSampleId() );
						if( sample == null )
						{
							addParsedSample( new ParsedSample( node ) );
						}
						else
						{
							sample.addNode( node );
						}
					}

				}
			}
		}
		reader.close();
		for( final ParsedSample sample: getParsedSamples() )
		{
			sample.buildOtuCounts();
			sample.report( true );
		}
	}

	/**
	 * To parse the taxonomy level reports output by {@link biolockj.module.implicit.qiime.QiimeClassifier}:
	 * <ol>
	 * <li>Call {@link #getInputFiles()}
	 * <li>Call {@link #initializeMaps()}
	 * <li>Call {@link #setOrderedQiimeIDs(File)}
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.r16s.QiimeNode#getSampleId()} if not
	 * yet created.
	 * <li>Add the {@link biolockj.node.r16s.QiimeNode#getCount()} (1) to {@link biolockj.node.ParsedSample} OTU count.
	 * </ol>
	 * <p>
	 * Sample QIIME report line: (head otu_table_L2.txt): <br>
	 * # Constructed from biom file #OTU ID 3A.1 6A.1 120A.1 7A.1 k__Bacteria;p__Actinobacteria 419.0 26.0 90.0 70.0
	 */
	@Override
	public void runModule() throws Exception
	{
		final List<File> inputFiles = getInputFiles();
		initializeMaps();
		setOrderedQiimeIDs( inputFiles.get( 0 ) );
		super.runModule();
	}

	/**
	 * Examines the header line to find the column index for
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#DEMUX_COLUMN} which holds the demultiplexed sample file
	 * name.
	 *
	 * @param line Header line containing column names
	 * @return Index of Qiime mapping column {@value biolockj.module.implicit.qiime.QiimeClassifier#DEMUX_COLUMN}
	 * @throws Exception If {@value biolockj.module.implicit.qiime.QiimeClassifier#DEMUX_COLUMN} not found.
	 */
	protected int getFileNameColumn( final String line ) throws Exception
	{
		final StringTokenizer header = new StringTokenizer( line, BioLockJ.TAB_DELIM );
		int colNum = 0;
		while( header.hasMoreTokens() )
		{
			final String token = header.nextToken();
			Log.get( getClass() ).info( "column(" + colNum + ") = " + token );
			if( token.equals( QiimeClassifier.DEMUX_COLUMN ) )
			{
				Log.get( getClass() ).info( "Found DEMUX_COLUMN, not checking remaining metadata columns!" );
				return colNum;
			}
			colNum++;
		}

		throw new Exception( "Unable to find " + QiimeClassifier.DEMUX_COLUMN + " in header [ " + line + "]" );
	}

	/**
	 * This method is used to get an R-friendly Sample ID value by stripping out the quotes AND by replacing any Qiime
	 * ID found with the original Sample ID.
	 *
	 * @param id Sample ID
	 * @return formatted Sample ID
	 * @throws Exception If any QIIME ID does not have a corresponding Sample ID
	 */
	@Override
	protected String getOtuTableRowId( final String id ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final String valWithoutQuotes = id.replaceAll( "'", "" ).replaceAll( "\"", "" );
		final StringTokenizer st = new StringTokenizer( valWithoutQuotes, TAB_DELIM );
		while( st.hasMoreTokens() )
		{
			final String token = st.nextToken();
			if( !sb.toString().isEmpty() )
			{
				sb.append( TAB_DELIM ).append( token );
			}
			else if( token.equals( MetaUtil.getID() ) )
			{
				sb.append( token );
			}
			else // must be a Qiime ID
			{
				sb.append( getSampleId( token ).replaceAll( "'", "" ).replaceAll( "\"", "" ) );
			}
		}

		return sb.toString();
	}

	/**
	 * Get the Sample ID from {@link #qiimeIdToSampleIdMap}. Qiime removes special characters with the script:
	 * {@link biolockj.module.implicit.qiime.BuildQiimeMapping#SCRIPT_VALIDATE_MAPPING}, to create the qiimeId, which
	 * this method accepts as a parameter to lookup the Sample ID.
	 *
	 * @param qiimeId Qiime ID that was created from sample ID in a previous script
	 * @return sampleId Original sample ID from metadata file
	 * @throws Exception if the qiimeId is not found in {@link #qiimeIdToSampleIdMap}
	 */
	protected String getSampleId( final String qiimeId ) throws Exception
	{
		final String sampleId = qiimeIdToSampleIdMap.get( qiimeId );
		if( sampleId == null )
		{
			throw new Exception( "QIIME ID: " + qiimeId + " not cached in qiimeIdToSampleIdMap" );
		}
		return sampleId;
	}

	/**
	 * The mapping file used the original sampleId when generating the demultipled files, as recorded in mapping file
	 * column - {@value biolockj.module.implicit.qiime.QiimeClassifier#DEMUX_COLUMN}. Example: sampleId = gutSample_42 /
	 * qiimeId = gutSample.42 / {@value biolockj.module.implicit.qiime.QiimeClassifier#DEMUX_COLUMN} =
	 * gutSample_42.fasts
	 *
	 * @param qiimeId Qiime corrected ID
	 * @param demuxIndex Qiime mapping column index for
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#DEMUX_COLUMN}
	 * @return Original sampleId extracted from {@value biolockj.module.implicit.qiime.QiimeClassifier#DEMUX_COLUMN}
	 * @throws Exception if unable to get sample ID from metadata file column
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#DEMUX_COLUMN}
	 */
	protected String getSampleIdFromMappingFile( final String qiimeId, final int demuxIndex ) throws Exception
	{
		if( demuxIndex != -1 )
		{
			return MetaUtil.getMetadataRecord( qiimeId ).get( demuxIndex - 1 ).replaceAll( "." + SeqUtil.FASTA, "" );
		}

		return qiimeId;
	}

	/**
	 * Sample IDs in the original metadata file may contain restricted characters that will be replaced with a "." via
	 * {@value biolockj.module.implicit.qiime.BuildQiimeMapping#SCRIPT_VALIDATE_MAPPING}. The
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#DEMUX_COLUMN} column will contain the original characters
	 * (and are used in the file name).
	 *
	 * @see <a href= "http://qiime.org/scripts/validate_mapping_file.html" target=
	 * "_top">http://qiime.org/scripts/validate_mapping_file.html</a>
	 * <p>
	 * Convenience maps are initialized to simplify lookups:
	 * <ul>
	 * <li>{@link #qiimeIdToSampleIdMap}
	 * <li>{@link #sampleIdToQiimeIdMap}
	 * </ul>
	 * 
	 * @throws Exception if unable to initialize the maps
	 */
	protected void initializeMaps() throws Exception
	{
		Log.get( getClass() )
				.info( "Initialize QIIME_ID to SAMPLE_ID Maps for: " + MetaUtil.getFile().getAbsolutePath() );
		int fileNameCol = 0;
		boolean isHeaderRow = true;
		final BufferedReader reader = BioLockJUtil.getFileReader( MetaUtil.getFile() );

		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			final String qiimeId = new StringTokenizer( line, BioLockJ.TAB_DELIM ).nextToken();
			if( isHeaderRow )
			{
				isHeaderRow = false;
				fileNameCol = getFileNameColumn( line );
				Log.get( getClass() ).info( "Header ID (" + qiimeId + ") has " + QiimeClassifier.DEMUX_COLUMN
						+ " in column #" + fileNameCol );
			}
			else
			{
				final String sampleId = getSampleIdFromMappingFile( qiimeId, fileNameCol );
				Log.get( getClass() ).info( "[Id-Map Entry] QIIME_ID(" + qiimeId + ")<=>SAMPLE_ID(" + sampleId + ")" );
				qiimeIdToSampleIdMap.put( qiimeId, sampleId );
				sampleIdToQiimeIdMap.put( sampleId, qiimeId );
			}
		}

		reader.close();

	}

	/**
	 * Qiime IDs are read in from the header line, converted to the sampleId, and saved to {@link #orderedQiimeIDs}.
	 *
	 * @param file Taxonomy level report file
	 * @throws Exception If unable to read the file parameter
	 */
	protected void setOrderedQiimeIDs( final File file ) throws Exception
	{
		Log.get( getClass() ).info(
				"Configure ordered list of Qiime IDs based on the 1st taxonomy report: " + file.getAbsolutePath() );
		final BufferedReader reader = BioLockJUtil.getFileReader( file );

		final String commenLine = reader.readLine(); // skip first line (its a comment)
		final String headerLine = reader.readLine();
		final String header = headerLine.replace( OTU_ID, "" );
		Log.get( getClass() ).info( "Skip comment line: " + commenLine );
		Log.get( getClass() ).info( "Read header line: " + headerLine );
		Log.get( getClass() ).info( "Remove " + OTU_ID + " from header" );
		Log.get( getClass() ).info( "Remaining header line should contain the QIIME IDs for all samples: " + header );

		final String[] parts = header.split( "\\s" );
		for( final String qiimeId: parts )
		{
			if( qiimeId.trim().length() > 0 )
			{
				Log.get( getClass() ).debug( "Add QiimeID: " + qiimeId );
				orderedQiimeIDs.add( qiimeId );
			}
		}

		reader.close();

		Log.get( getClass() ).info( "List QIIME IDs( total#" + orderedQiimeIDs.size() + " ) = " + orderedQiimeIDs );
	}

	/**
	 * Qiime IDs are listed in the same order in each taxonomy level report. The values are cached here after being read
	 * from the first report.
	 */
	protected static final List<String> orderedQiimeIDs = new ArrayList<>();

	/**
	 * Convenience map, to convert Qiime ID to Sample ID
	 */
	protected static final Map<String, String> qiimeIdToSampleIdMap = new HashMap<>();

	/**
	 * Convenience map, to convert Sample ID to Qiime ID
	 */
	protected static final Map<String, String> sampleIdToQiimeIdMap = new HashMap<>();

	private static final String OTU_ID = "#OTU ID";
}
