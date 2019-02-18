package biolockj.module.report.humann2;

import java.io.*;
import java.util.*;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.implicit.parser.wgs.Humann2Parser;
import biolockj.util.*;

/**
 * This BioModule is used to build a JSON file (summary.json) compiled from all OTUs in the dataset.
 */
public class Humann2Report extends Humann2CountModule implements JavaModule
{

	private String spaces( int x )
	{
		String val = "";
		for(int y=0;y<x;y++)
		{
			val += " ";
		}
		return val;
	}
	
	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception
	{
		String summary = "";
		if( hasAbund() )
		{
			int labelSize = Math.max( Constants.HN2_UNIQUE_PATH_COUNT.length(), Constants.HN2_UNINTEGRATED_COUNT.length() );
			labelSize = Math.max( labelSize, Constants.HN2_UNMAPPED_COUNT.length() );
			labelSize = Math.max( labelSize, Constants.HN2_TOTAL_PATH_COUNT.length() );
			
			summary += "Total # Unique Pathways:" + spaces( 11 - new Integer( pathways.size() ).toString().length() ) + pathways.size() + RETURN;
			summary += SummaryUtil.getCountSummary( uniquePathwaysPerSample, Constants.HN2_UNIQUE_PATH_COUNT, labelSize,
					false );
			summary += SummaryUtil.getCountSummary( totalPathwaysPerSample, Constants.HN2_TOTAL_PATH_COUNT, labelSize,
					true );
			summary += SummaryUtil.getCountSummary( unintegratedPerSample, Constants.HN2_UNINTEGRATED_COUNT, labelSize,
					true );
			summary += SummaryUtil.getCountSummary( unmappedPerSample, Constants.HN2_UNMAPPED_COUNT, labelSize, true );
		}
		freeMemory();
		return super.getSummary() + summary;
	}

	@Override
	public boolean isValidInputModule( final BioModule module )
	{
		return module instanceof Humann2Parser;
	}

	@Override
	public void runModule() throws Exception
	{
		for( final File file: getInputFiles() )
		{
			writePathwayReport( file, parseFullReport( file, validColIndexes( file ) ) );

			if( hasAbund() && file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) )
			{
				final File unintegratedTemp = new File(
						getTempDir().getAbsolutePath() + File.separator + Constants.HN2_UNINTEGRATED_COUNT );
				if( !unintegratedTemp.exists() )
				{
					unintegratedTemp.mkdirs();
				}

				MetaUtil.addColumn( Constants.HN2_UNINTEGRATED_COUNT, unintegratedPerSample, unintegratedTemp, true );
				MetaUtil.addColumn( Constants.HN2_UNMAPPED_COUNT, unmappedPerSample, getTempDir(), true );
				MetaUtil.addColumn( Constants.HN2_UNIQUE_PATH_COUNT, uniquePathwaysPerSample, getOutputDir(), true );
			}
		}
	}

	/**
	 * Read in path abundance file, each inner lists represents 1 line from the file.<br>
	 * Each cell in the tab delimited file is stored as 1 element in the inner lists.
	 * 
	 * @param file Path abundance file
	 * @param valid columns
	 * @return List of Lists - each inner list 1 line
	 * @throws Exception if errors occur
	 */
	protected List<List<String>> parseFullReport( final File file, final TreeSet<Integer> validCols ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		if( validCols.isEmpty() )
		{
			throw new Exception( "No valid data pathway data found in: " + file.getAbsolutePath() );
		}
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try
		{
			int unMappedIndex = -1;
			int unIntegratedIndex = -1;
			boolean firstRecord = true;
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final List<String> record = new ArrayList<>();
				int i = 0;
				Integer sampleUniquePathways = 0;
				Integer sampleTotalPathways = 0;
				String id = null;
				final String[] cells = line.split( Constants.TAB_DELIM, -1 );

				for( final String cell: cells )
				{
					if( validCols.contains( i ) )
					{
						if( firstRecord && cell.equals( HN2_UNMAPPED ) )
						{
							unMappedIndex = i;
						}
						else if( firstRecord && cell.equals( HN2_UNINTEGRATED ) )
						{
							unIntegratedIndex = i;
						}
						else if( i == unMappedIndex && file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) )
						{
							unmappedPerSample.put( id, getCount( cell ).toString() );
						}
						else if( i == unIntegratedIndex && file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) )
						{
							unintegratedPerSample.put( id, getCount( cell ).toString() );
						}
						else
						{
							if( firstRecord )
							{
								if( !cell.equals( MetaUtil.getID() ) && file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) )
								{
									pathways.add( cell );
								}

								record.add( cell );
							}
							else if( id == null )
							{
								record.add( cell );
							}
							else
							{
								final Integer count = getCount( cell );
								sampleTotalPathways += count;
								if( count > 0 )
								{
									sampleUniquePathways++;
								}
								record.add( count.toString() );
							}

							if( id == null )
							{
								id = cell;
							}
						}
					}
					i++;
				}

				if( !firstRecord && file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) )
				{
					totalPathwaysPerSample.put( id, sampleTotalPathways.toString() );
					uniquePathwaysPerSample.put( id, sampleUniquePathways.toString() );
				}

				data.add( record );
				firstRecord = false;
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		return data;
	}


	/**
	 * Check column headers for valid pathway indexes (avoid columns with genus/species info).
	 * 
	 * @param file Module input pathway abundance full report as output by HumanN2 parser.
	 * @return Ordered set of valid col indexes
	 * @throws Exception if errors occur
	 */
	protected TreeSet<Integer> validColIndexes( final File file ) throws Exception
	{
		final TreeSet<Integer> validCols = new TreeSet<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try
		{
			int i = 0;
			final String line = reader.readLine();
			final String[] cells = line.split( Constants.TAB_DELIM, -1 );
			for( final String cell: cells )
			{
				if( !cell.contains( Constants.SEPARATOR ) )
				{
					validCols.add( i );
				}
				i++;
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		return validCols;
	}

	/**
	 * Write the pathway report with the input data.
	 * 
	 * @param file Root file
	 * @param data List of lines, each inner list contains a row of cells for spreadsheet
	 * @throws Exception if errors occur
	 */
	protected void writePathwayReport( final File file, final List<List<String>> data ) throws Exception
	{
		final File outFile = PathwayUtil.getPathwayCountFile( getOutputDir(), file, Constants.HN2_PATHWAY_REPORT );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( outFile ) );
		try
		{
			for( final List<String> record: data )
			{
				boolean newRecord = true;
				for( final String cell: record )
				{
					writer.write( ( !newRecord ? Constants.TAB_DELIM: "" ) + cell );
					newRecord = false;
				}
				writer.write( Constants.RETURN );
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}
	}

	private void freeMemory() throws Exception
	{
		unmappedPerSample = null;
		unintegratedPerSample = null;
		uniquePathwaysPerSample = null;
		totalPathwaysPerSample = null;
		pathways = null;
	}

	private Integer getCount( final String val )
	{
		try
		{
			if( NumberUtils.isNumber( val ) )
			{
				return Double.valueOf( val ).intValue();
			}
		}
		catch( final Exception ex )
		{
			Log.warn( getClass(), "Cell does not have valid numeric data: " + val );
		}
		return 0;
	}
	

	private Set<String> pathways = new HashSet<>();
	private Map<String, String> totalPathwaysPerSample = new HashMap<>();
	private Map<String, String> unintegratedPerSample = new HashMap<>();
	private Map<String, String> uniquePathwaysPerSample = new HashMap<>();
	private Map<String, String> unmappedPerSample = new HashMap<>();
	private static final String HN2_UNINTEGRATED = "UNINTEGRATED";
	private static final String HN2_UNMAPPED = "UNMAPPED";

}
