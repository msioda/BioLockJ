package biolockj.module.report.humann2;

import java.io.*;
import java.util.*;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.implicit.parser.wgs.HumanN2Parser;
import biolockj.util.*;

/**
 * This BioModule is used to build a JSON file (summary.json) compiled from all OTUs in the dataset.
 */
public class HumanN2ExtractPathwayCounts extends HumanN2CountModule implements JavaModule
{
	/**
	 * Module prerequisite: {@link biolockj.module.report.otu.CompileOtuCounts}
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
	{
		final List<String> preReqs = super.getPreRequisiteModules();
		if( !pipelineInputContainsFullPathwayReport() )
		{
			preReqs.add( HumanN2Parser.class.getName() );
		}

		return preReqs;
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception
	{
		int labelSize = Math.max( UNIQUE_PATH_COUNT.length(), UNINTEGRATED_COUNT.length() );
		labelSize = Math.max( labelSize, UNMAPPED_COUNT.length() );
		labelSize = Math.max( labelSize, TOTAL_PATH_COUNT.length() );

		String summary = "Total # Unique Pathways:         " + pathways.size() + RETURN;

		summary += SummaryUtil.getCountSummary( uniquePathwaysPerSample, UNIQUE_PATH_COUNT, labelSize, false );
		summary += SummaryUtil.getCountSummary( totalPathwaysPerSample, TOTAL_PATH_COUNT, labelSize, true );
		summary += SummaryUtil.getCountSummary( unintegratedPerSample, UNINTEGRATED_COUNT, labelSize, true );
		summary += SummaryUtil.getCountSummary( unmappedPerSample, UNMAPPED_COUNT, labelSize, true );
		freeMemory();
		return super.getSummary() + summary;
	}

	@Override
	public boolean isValidInputModule( final BioModule module )
	{
		return module instanceof HumanN2Parser;
	}

	@Override
	public void runModule() throws Exception
	{
		final File file = getInputFiles().get( 0 );
		writePathwayReport( parseFullReport( file, validColIndexes( file ) ) );

		if( Config.getBoolean( Constants.REPORT_NUM_HITS ) )
		{
			final File unintegratedTemp = new File(
					getTempDir().getAbsolutePath() + File.separator + UNINTEGRATED_COUNT );
			if( !unintegratedTemp.exists() )
			{
				unintegratedTemp.mkdirs();
			}

			MetaUtil.addColumn( UNINTEGRATED_COUNT, unintegratedPerSample, unintegratedTemp, true );
			MetaUtil.addColumn( UNMAPPED_COUNT, unmappedPerSample, getTempDir(), true );
			MetaUtil.addColumn( UNIQUE_PATH_COUNT, uniquePathwaysPerSample, getOutputDir(), true );
		}
	}

	/**
	 * Read in path abundance file, each inner lists represents 1 line from the file.<br>
	 * Each cell in the tab delimited file is stored as 1 element in the inner lists.
	 * 
	 * @param file Path abundance file
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
						else if( i == unMappedIndex )
						{
							unmappedPerSample.put( id, getCount( cell ).toString() );
						}
						else if( i == unIntegratedIndex )
						{
							unintegratedPerSample.put( id, getCount( cell ).toString() );
						}
						else
						{
							if( firstRecord )
							{
								if( !cell.equals( MetaUtil.getID() ) )
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

				if( !firstRecord )
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
	 * Check pipeline input for module input file type.
	 * 
	 * @return TRUE if pipeline input contains module input
	 * @throws Exception if errors occur
	 */
	protected boolean pipelineInputContainsFullPathwayReport() throws Exception
	{
		final Iterator<File> it = BioLockJUtil.getPipelineInputFiles().iterator();
		while( it.hasNext() )
		{
			if( it.next().getName().endsWith( fullPathwayReportSuffix() ) )
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Check column headers for valid pathway indexes (avoid columns with genus/species info).
	 * 
	 * @param file Module input pathway abundance full report as output by HumanN2 parser.
	 * @return
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
		Log.info( getClass(), "Valid pathway columns: " + validCols );

		return validCols;
	}

	/**
	 * Write the pathway report with the input data.
	 * 
	 * @param data List of lines, each inner list contains a row of cells for spreadsheet
	 * @throws Exception if errors occur
	 */
	protected void writePathwayReport( final List<List<String>> data ) throws Exception
	{
		final File outFile = PathwayUtil.getPathwayCountFile( getOutputDir(), Constants.HN2_PATHWAY_REPORT );
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

	private String fullPathwayReportSuffix() throws Exception
	{
		return Constants.HN2_PATH_ABUND_TABLE + "_" + Constants.HN2_FULL_REPORT + TSV_EXT;
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
	private static final String TOTAL_PATH_COUNT = "Total_Pathway_Count";
	private static final String UNINTEGRATED_COUNT = "Unintegrated_Count";
	private static final String UNIQUE_PATH_COUNT = "Unique_Pathway_Count";
	private static final String UNMAPPED_COUNT = "Unmapped_Count";
}
