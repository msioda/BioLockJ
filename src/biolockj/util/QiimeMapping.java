package biolockj.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import biolockj.BioLockJ;
import biolockj.Log;
import biolockj.module.classifier.r16s.QiimeClassifier;

/**
 * This file is used to help work with the Qiime mapping file.
 * @author mike
 *
 */
public class QiimeMapping
{
	/**
	 * Converts the QIIME mapping (with key=QIIME_ID) into standard metadata (with key=QIIME_ID).
	 * Converts header column #SAMPLE_ID into an R-friendly value (without a #).
	 * Populates the maps needed to convert QIIME_ID <=> SAMPLE_ID.
	 * @param metaUtil
	 * @param outputDir
	 * @return
	 * @throws Exception
	 */
	public static void buildMapping( final File targetDir, final File sampleFile ) throws Exception
	{
		initializeMaps();
		setOrderedSampleIDs( sampleFile );
		final BufferedReader reader = new BufferedReader( new FileReader( MetaUtil.getFile() ) );
		MetaUtil.setFile( new File( targetDir.getAbsolutePath() + File.separator + MetaUtil.getFile().getName() ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( MetaUtil.getFile() ) );
		boolean isHeaderRow = true;

		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			if( !isHeaderRow )
			{
				final String rowId = new StringTokenizer( line, BioLockJ.TAB_DELIM ).nextToken();
				line = getSampleId( rowId ) + line.substring( rowId.length() );
			}
			isHeaderRow = false;
			writer.write( line + BioLockJ.RETURN );
		}

		reader.close();
		writer.close();

	}

	public static List<String> getSampleIds()
	{
		return orderedSampleIDs;
	}

	/**
	 * Examines the line to find the column number of the DEMUX_COLUMN
	 * @param line - must be a header containing column names
	 * @return
	 */
	private static int getFileNameColumn( final String line )
	{
		final StringTokenizer header = new StringTokenizer( line, BioLockJ.TAB_DELIM );
		int colNum = 0;
		while( header.hasMoreTokens() )
		{
			final String token = header.nextToken();
			Log.out.info( "column(" + colNum + ") = " + token );
			if( token.equals( QiimeClassifier.DEMUX_COLUMN ) )
			{
				return colNum;
			}
			colNum++;
		}
		return -1;
	}

	/**
	 * Get the SampleID from the qiimeIdToSampleIdMap.
	 * @param qiimeId
	 * @return
	 * @throws Exception
	 */
	private static String getSampleId( final String qiimeId ) throws Exception
	{
		final String sampleId = qiimeIdToSampleIdMap.get( qiimeId );
		if( sampleId == null )
		{
			throw new Exception( "QiimeMappingUtil cannot find QIIME ID: " + qiimeId );
		}
		return sampleId;
	}

	//	/**
	//	 * The required "#SampleID" column must be converted to a unique name that doesn't include
	//	 * the "#" symbol which is a special character in R.
	//	 * @param headerId
	//	 * @param restOfLine
	//	 * @return
	//	 */
	//	private static String getUniqueId( final String headerId, final String restOfLine )
	//	{
	//		String id = "SampleID";
	//		final ArrayList<String> colNames = new ArrayList<>( Arrays.asList( restOfLine.split( reportDelim ) ) );
	//
	//		int x = 0;
	//		if( headerId.equals( QIIME_ID ) )
	//		{
	//			while( colNames.contains( id ) && ( x < 8 ) )
	//			{
	//				switch( x )
	//				{
	//					case 0:
	//						id = "sampleId";
	//						x++;
	//						break;
	//					case 1:
	//						id = "id";
	//						x++;
	//						break;
	//					case 2:
	//						id = "ID";
	//						x++;
	//						break;
	//					case 3:
	//						id = "sample_id";
	//						x++;
	//						break;
	//					case 4:
	//						id = "SAMPLE_ID";
	//						x++;
	//						break;
	//					case 5:
	//						id = "BioLockJ_ID";
	//						x++;
	//						break;
	//					case 6:
	//						id = "BLJ_ID";
	//						x++;
	//						break;
	//					case 7:
	//						id = "SampleId";
	//						x++;
	//						break;
	//					default:
	//						x++;
	//						break;
	//				}
	//			}
	//		}
	//
	//		return id;
	//	}

	/**
	 * The mapping file contains the sampleID in the DEMUX_COLUMN where it is parsed out of the
	 * formatted file name: "sampleId.fasta"
	 * @param qiimeId
	 * @param fileNameCol
	 * @return
	 * @throws Exception
	 */
	private static String getSampleIdFromMappingFile( final String qiimeId, final int fileNameCol ) throws Exception
	{
		if( fileNameCol != -1 )
		{
			return MetaUtil.getAttributes( qiimeId ).get( ( fileNameCol - 1 ) ).replaceAll( "." + SeqUtil.FASTA, "" );
		}

		return qiimeId;
	}

	/**
	 * Populate the qiimeIdToSampleIdMap & sampleIdToQiimeIdMap by reading each row in the formatted
	 * Qiime mapping file.
	 *
	 * @throws Exception
	 */
	private static void initializeMaps() throws Exception
	{
		Log.out.info( "Initialize QIIME_ID to SAMPLE_ID Maps for: " + MetaUtil.getFile().getAbsolutePath() );
		int fileNameCol = 0;
		boolean isHeaderRow = true;
		int count = 0;
		final BufferedReader reader = new BufferedReader( new FileReader( MetaUtil.getFile() ) );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final String qiimeId = new StringTokenizer( line, BioLockJ.TAB_DELIM ).nextToken();
				if( isHeaderRow )
				{
					isHeaderRow = false;
					fileNameCol = getFileNameColumn( line );
					Log.out.info( "Header ID (" + qiimeId + ") has " + QiimeClassifier.DEMUX_COLUMN + " in column #"
							+ fileNameCol );
				}
				else
				{
					final String sampleId = getSampleIdFromMappingFile( qiimeId, fileNameCol );

					if( count++ < 1 )
					{
						Log.out.info(
								"[Example Id-Map Entry] QIIME_ID(" + qiimeId + ")<=>SAMPLE_ID(" + sampleId + ")" );
					}
					else
					{
						Log.out.debug( "[Id-Map Entry] QIIME_ID(" + qiimeId + ")<=>SAMPLE_ID(" + sampleId + ")" );
					}

					qiimeIdToSampleIdMap.put( qiimeId, sampleId );
					sampleIdToQiimeIdMap.put( sampleId, qiimeId );
				}
			}
		}
		finally
		{
			reader.close();
		}
	}

	/**
	 * Sample IDs are read in from the header line, in order & saved to orderedSampleIDs.
	 * @param file
	 * @throws Exception
	 */
	private static void setOrderedSampleIDs( final File file ) throws Exception
	{
		Log.out.info( "Configure ordered list of Sample IDs based on example file: " + file.getAbsolutePath() );
		final BufferedReader reader = ModuleUtil.getFileReader( file );
		try
		{
			String header = reader.readLine(); // skip first line (its a comment)
			header = reader.readLine().replace( OTU_ID, "" );
			final String[] parts = header.split( "\\s" );
			for( final String qiimeId: parts )
			{
				if( qiimeId.trim().length() > 0 )
				{
					orderedSampleIDs.add( getSampleId( qiimeId ) );
				}
			}
		}
		finally
		{
			reader.close();
		}
		Log.out.info( "orderedSampleIDs( " + orderedSampleIDs.size() + " ) = " + orderedSampleIDs.size() );
	}

	private static final List<String> orderedSampleIDs = new ArrayList<>();
	private static final String OTU_ID = "#OTU ID";
	private static final Map<String, String> qiimeIdToSampleIdMap = new HashMap<>();
	private static final Map<String, String> sampleIdToQiimeIdMap = new HashMap<>();
}