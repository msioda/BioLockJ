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
import biolockj.util.*;

/**
 * This BioModule set low Pathway counts below a configured threshold to zero.<br>
 * These low sample counts are assumed to be miscategorized or genomic contamination.
 * 
 * @blj.web_desc Remove Low Pathway Counts
 */
public class RemoveLowPathwayCounts extends Humann2CountModule {

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		getMinCount();
	}

	/**
	 * Produce summary message with min, max, mean, and median number of pathways.
	 */
	@Override
	public String getSummary() throws Exception {
		String summary = "Remove Pathway counts below --> " + getMetaColName() + RETURN;
		if( !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_ABUNDANCE ) ) {
			summary += SummaryUtil.getCountSummary( this.uniquePathwaysPerSample, "Unique Pathways", false );
			summary += SummaryUtil.getCountSummary( this.totalPathwaysPerSample, "Total Pathways ", true );
			this.sampleIds.removeAll( this.totalPathwaysPerSample.keySet() );
			if( !this.sampleIds.isEmpty() ) {
				summary += "Removed empty samples: " + this.sampleIds;
			}
		}
		freeMemory();
		return super.getSummary() + summary;
	}

	@Override
	public void runModule() throws Exception {
		this.sampleIds.addAll( MetaUtil.getSampleIds() );
		for( final File file: getInputFiles() ) {
			logLowCountPathways( removeLowPathwayCounts( file ) );
			if( Config.getBoolean( this, Constants.REPORT_NUM_HITS )
				&& !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_ABUNDANCE )
				&& file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) ) {
				MetaUtil.addColumn( getMetaColName() + "_" + Constants.HN2_UNIQUE_PATH_COUNT,
					this.uniquePathwaysPerSample, getTempDir(), true );
				MetaUtil.addColumn( getMetaColName() + "_" + Constants.HN2_TOTAL_PATH_COUNT,
					this.totalPathwaysPerSample, getOutputDir(), true );
			}
		}
	}

	/**
	 * Save a list of low count pathways to the module temp directory.
	 *
	 * @param map TreeMap(sampleId, TreeSet(Pathway)) of Pathways found in too few samples
	 * @throws Exception if errors occur
	 */
	protected void logLowCountPathways( final TreeMap<String, TreeSet<String>> map ) throws Exception {
		if( map == null || map.isEmpty() ) {
			Log.info( getClass(), "No low-count pathways detected" );
			return;
		}
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getLowCountPathwayLogFile() ) );
		try {
			for( final String id: map.keySet() ) {
				final TreeSet<String> pathways = map.get( id );

				for( final String pathway: pathways ) {
					writer.write( id + ": " + pathway + RETURN );
				}
			}
		} finally {
			writer.close();
		}

		Log.info( getClass(),
			"Found " + map.size() + " samples with low count pathways removed - Pathway list saved to --> "
				+ getLowCountPathwayLogFile().getAbsolutePath() );
	}

	/**
	 * Remove Pathway Counts below the {@link biolockj.Config}.{@value biolockj.Constants#REPORT_MIN_COUNT}
	 *
	 * @param file Input file
	 * @return TreeMap(SampleId, TreeMap(Pathway)) Map removed pathways to sample ID
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, TreeSet<String>> removeLowPathwayCounts( final File file ) throws Exception {
		Log.info( getClass(), "Inspecting for Low Pathway count: " + file.getAbsolutePath() );
		final TreeMap<String, TreeSet<String>> lowCountPathways = new TreeMap<>();
		final List<List<String>> table = BioLockJUtil.parseCountTable( file );
		final List<List<String>> output = new ArrayList<>();
		List<String> pathways = null;
		final TreeSet<String> validPathways = new TreeSet<>();
		final Set<String> foundSamplePathways = new TreeSet<>();
		int totalPathwayCount = 0;
		for( final List<String> record: table ) {
			final String sampleId = record.get( 0 );

			if( pathways == null ) {
				output.add( record );
				validPathways.addAll( record.subList( 1, record.size() ) );
				pathways = record;
				continue;
			}

			final TreeSet<String> validSamplePathways = new TreeSet<>();

			final List<String> line = new ArrayList<>();
			line.add( sampleId );
			for( int i = 1; i < record.size(); i++ ) {
				final Double count = Double.valueOf( record.get( i ) );
				final String pathway = pathways.get( i - 1 );
				if( count < getMinCount() ) {
					line.add( "0.0" );
					Log.debug( getClass(), sampleId + ": Remove Low Pathway count: " + pathway + "=" + count );
				} else {
					validSamplePathways.add( pathway );
					line.add( count.toString() );
					totalPathwayCount += count;
					validPathways.add( pathway );
				}
			}

			output.add( line );

			final TreeSet<String> badSamplePathways = new TreeSet<>( validPathways );
			badSamplePathways.removeAll( validSamplePathways );
			foundSamplePathways.addAll( validSamplePathways );
			if( file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) ) {
				Log.info( getClass(), "Set totalPathwaysPerSample: " + sampleId + "=" + totalPathwayCount );
				Log.info( getClass(), "Set uniquePathwaysPerSample: " + sampleId + "=" + foundSamplePathways.size() );
				this.totalPathwaysPerSample.put( sampleId, String.valueOf( totalPathwayCount ) );
				this.uniquePathwaysPerSample.put( sampleId, String.valueOf( foundSamplePathways.size() ) );
			}

			if( !badSamplePathways.isEmpty() ) {
				lowCountPathways.put( sampleId, badSamplePathways );
				Log.warn( getClass(), sampleId + ": Remove " + badSamplePathways.size()
					+ " Pathways with #counts below threshold: " + getProp() + "=" + getMinCount() );
				Log.debug( getClass(), sampleId + ": Removed Pathways: " + badSamplePathways );
			}
		}

		final TreeSet<String> allRemovedPathways = new TreeSet<>( validPathways );
		allRemovedPathways.removeAll( foundSamplePathways );
		if( !allRemovedPathways.isEmpty() ) {
			Log.warn( getClass(), "Remove " + allRemovedPathways.size() + " Pathways with #counts below threshold: "
				+ getProp() + "=" + getMinCount() );
			Log.debug( getClass(), "Removed Pathways: " + allRemovedPathways );
		}

		buildOutputTable( output, file, allRemovedPathways );

		return lowCountPathways;
	}

	private void buildOutputTable( final List<List<String>> data, final File file, final Set<String> badPathways )
		throws Exception {
		final File outTable = PathwayUtil.getPathwayCountFile( getOutputDir(), file, getMetaColName() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( outTable ) );
		try {
			final Set<Integer> badIndex = new HashSet<>();
			boolean firstRecord = true;
			for( final List<String> record: data ) {
				boolean newRecord = true;
				if( firstRecord && !badPathways.isEmpty() ) {
					for( final String pathway: record ) {
						if( badPathways.contains( pathway ) ) {
							badIndex.add( record.indexOf( pathway ) );
						} else {
							writer.write( ( !newRecord ? Constants.TAB_DELIM: "" ) + pathway );
						}
						newRecord = false;
					}
				} else if( !firstRecord ) {
					for( int i = 0; i < record.size(); i++ ) {
						if( !badIndex.contains( i ) ) {
							writer.write( ( !newRecord ? Constants.TAB_DELIM: "" ) + record.get( i ) );
						}
						newRecord = false;
					}
				} else {
					for( final String pathway: record ) {
						writer.write( ( !newRecord ? Constants.TAB_DELIM: "" ) + pathway );
						newRecord = false;
					}
				}
				firstRecord = false;
				writer.write( RETURN );
			}
		} finally {
			writer.close();
		}
	}

	private void freeMemory() {
		this.uniquePathwaysPerSample = null;
		this.totalPathwaysPerSample = null;
	}

	private File getLowCountPathwayLogFile() {
		return new File( getTempDir().getAbsolutePath() + File.separator + "lowCountPathways" + TXT_EXT );
	}

	private String getMetaColName() throws Exception {
		return "minCount" + getMinCount();
	}

	private Integer getMinCount() throws Exception {
		return Config.requirePositiveInteger( this, getProp() );
	}

	private String getProp() {
		if( this.prop == null ) {
			this.prop = Config.getModuleProp( this, Constants.REPORT_MIN_COUNT );
		}
		return this.prop;
	}

	private String prop = null;
	private final Set<String> sampleIds = new HashSet<>();
	private Map<String, String> totalPathwaysPerSample = new HashMap<>();
	private Map<String, String> uniquePathwaysPerSample = new HashMap<>();
}
