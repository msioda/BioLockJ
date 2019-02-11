package biolockj.module.report.humann2;

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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.exception.ConfigFormatException;
import biolockj.module.JavaModule;
import biolockj.util.*;

/**
 * This BioModule
 */
public class RemoveScarcePathwayCounts extends HumanN2CountModule implements JavaModule
{

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( getScarceCutoff() > 1 )
		{
			throw new ConfigFormatException( getScarceProp(), "Required range 0.0 < " + getScarceProp() + " < 1.0 " );
		}
	}

	/**
	 * Produce summary message with min, max, mean, and median number of pathways.
	 */
	@Override
	public String getSummary() throws Exception
	{
		String summary = "Remove rare Pathways found in less than " + getCutoff() + " samples" + RETURN;
		final String label = "Unique Pathways";
		summary += SummaryUtil.getCountSummary( uniquePathwaysPerSample, label, label.length(), false );
		summary += SummaryUtil.getCountSummary( totalPathwaysPerSample, "Total Pathways", label.length(), true );
		sampleIds.removeAll( totalPathwaysPerSample.keySet() );
		if( !sampleIds.isEmpty() )
		{
			summary += "Removed empty samples: " + sampleIds;
		}
		freeMemory();
		return super.getSummary() + summary;
	}

	@Override
	public void runModule() throws Exception
	{
		sampleIds.addAll( MetaUtil.getSampleIds() );
		final TreeMap<String, TreeSet<String>> scarceCounts = removeScarcePathwayCounts( getScarcePathways() );
		logScarcePathways( scarceCounts );
		if( Config.getBoolean( Constants.REPORT_NUM_HITS ) )
		{
			MetaUtil.addColumn( getMetaColName(), uniquePathwaysPerSample, getTempDir(), true );
			MetaUtil.addColumn( getMetaColName(), totalPathwaysPerSample, getOutputDir(), true );
		}
	}

	/**
	 * Save a list of low count pathways to the module temp directory.
	 * 
	 * @param map TreeMap(sampleId, TreeSet(Pathway)) of Pathways found in too few samples
	 * @throws Exception if errors occur
	 */
	protected void logScarcePathways( final TreeMap<String, TreeSet<String>> map ) throws Exception
	{
		if( map == null || map.isEmpty() )
		{
			Log.info( getClass(), "No low-count pathways detected" );
			return;
		}
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getScarcePathwayLogFile() ) );
		try
		{
			for( final String id: map.keySet() )
			{
				for( final String pathway: map.get( id ) )
				{
					writer.write( id + ": " + pathway + RETURN );
				}
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}

		Log.info( getClass(),
				"Found " + map.size() + " samples with scarce pathways to removed- Pathway list saved to --> "
						+ getScarcePathwayLogFile().getAbsolutePath() );
	}

	/**
	 * Remove Pathway Counts below the {@link biolockj.Config}.{@value biolockj.Constants#REPORT_MIN_COUNT}
	 * 
	 * @return TreeMap(SampleId, TreeMap(Pathway)) Map removed pathways to sample ID
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, TreeSet<String>> removeScarcePathwayCounts( final Set<String> scarcePathways )
			throws Exception
	{
		final TreeMap<String, TreeSet<String>> scarcePathMap = new TreeMap<>();
		final File inputFile = getInputFiles().iterator().next();
		final List<List<String>> table = BioLockJUtil.parseCountTable( inputFile );
		final List<List<String>> output = new ArrayList<>();

		List<String> pathways = null;

		final List<String> validPathways = new ArrayList<>();
		int totalPathwayCount = 0;
		for( final List<String> record: table )
		{
			final List<String> line = new ArrayList<>();
			final String id = record.get( 0 );
			line.add( id );
			if( pathways == null )
			{
				for( int i = 1; i < record.size(); i++ )
				{
					final String pathway = record.get( i );
					if( !scarcePathways.contains( pathway ) )
					{
						line.add( pathway );
						validPathways.add( pathway );
					}
				}
				pathways = record;
				output.add( line );
				continue;
			}

			final TreeSet<String> validSamplePathways = new TreeSet<>();

			for( int i = 1; i < record.size(); i++ )
			{
				final String pathway = pathways.get( i );
				if( !scarcePathways.contains( pathway ) )
				{
					final Integer count = Integer.valueOf( record.get( i ) );
					line.add( count.toString() );
					totalPathwayCount += count;
					if( count > 0 )
					{
						validSamplePathways.add( pathway );
					}
				}
			}

			output.add( line );

			final TreeSet<String> badSamplePathways = new TreeSet<>( validPathways );
			badSamplePathways.removeAll( validSamplePathways );

			totalPathwaysPerSample.put( id, String.valueOf( totalPathwayCount ) );
			uniquePathwaysPerSample.put( id, String.valueOf( validSamplePathways.size() ) );

			if( !badSamplePathways.isEmpty() )
			{
				scarcePathMap.put( id, badSamplePathways );
				Log.warn( getClass(), id + ": Removed " + badSamplePathways.size() + " low Pathway counts (below "
						+ getScarceProp() + "=" + getMetaColName() + ") --> " + badSamplePathways );
			}
		}

		buildOutputTable( output );

		return scarcePathMap;
	}

	private void buildOutputTable( final List<List<String>> data ) throws Exception
	{
		final File file = PathwayUtil.getPathwayCountFile( getOutputDir(), getMetaColName() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
		try
		{
			for( final List<String> record: data )
			{
				boolean newRecord = true;
				for( int i = 0; i < record.size(); i++ )
				{
					writer.write( ( !newRecord ? Constants.TAB_DELIM: "" ) + record.get( i ) );
					newRecord = false;
				}

				writer.write( RETURN );
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

	private void freeMemory()
	{
		uniquePathwaysPerSample = null;
		totalPathwaysPerSample = null;
	}

	private int getCutoff() throws Exception
	{
		if( cutoff == null )
		{
			cutoff = new Double( Math.ceil( MetaUtil.getSampleIds().size() * getScarceCutoff() ) ).intValue();
		}

		return cutoff;
	}

	private String getMetaColName() throws Exception
	{
		return "scarcePathway" + new Double( getScarceCutoff() * 100 ).intValue() + "%";
	}

	private Double getScarceCutoff() throws Exception
	{
		return Config.requirePositiveDouble( getScarceProp() );
	}

	private File getScarcePathwayLogFile() throws Exception
	{
		return new File( getTempDir().getAbsolutePath() + File.separator + "scarcePathways" + TXT_EXT );
	}

	private Set<String> getScarcePathways() throws Exception
	{
		final Set<String> scarcePathways = new HashSet<>();
		final File inputFile = getInputFiles().iterator().next();
		final List<List<String>> table = BioLockJUtil.parseCountTable( inputFile );
		final Map<String, Integer> pathMap = new HashMap<>();
		List<String> pathways = null;
		for( final List<String> record: table )
		{
			if( pathMap.isEmpty() )
			{
				pathways = record;
				for( int i = 1; i < record.size(); i++ )
				{
					pathMap.put( record.get( i ), 0 );
				}

				continue;
			}

			for( int i = 1; i < record.size(); i++ )
			{
				final Integer count = Integer.valueOf( record.get( i ) );
				final String pathway = pathways.get( i );
				if( count > 0 )
				{
					pathMap.put( pathway, pathMap.get( pathway ) + 1 );
				}
			}
		}

		for( final String pathway: pathways )
		{
			Integer count = pathMap.get( pathway );
			if( count != null && count < getCutoff() )
			{
				scarcePathways.add( pathway );
			}
		}

		return scarcePathways;
	}

	private String getScarceProp() throws Exception
	{
		if( prop == null )
		{
			prop = getProperty( Constants.REPORT_SCARCE_CUTOFF );
		}
		return prop;
	}

	private String prop = null;
	private Integer cutoff = null;
	private final Set<String> sampleIds = new HashSet<>();
	private Map<String, String> totalPathwaysPerSample = new HashMap<>();
	private Map<String, String> uniquePathwaysPerSample = new HashMap<>();
}
