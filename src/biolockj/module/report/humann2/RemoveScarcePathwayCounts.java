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
import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.*;
import biolockj.exception.*;
import biolockj.util.*;

/**
 * This BioModule removes scarce pathways not found in enough samples.<br>
 * Each pathway must be found in a configurable percentage of samples to be retained.
 * 
 * @blj.web_desc Remove Scarce Pathway Counts
 */
public class RemoveScarcePathwayCounts extends Humann2CountModule {

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		if( getScarceCountCutoff() > 1 )
			throw new ConfigFormatException( Constants.REPORT_SCARCE_CUTOFF, "Required range 0.0 - 1.0 " );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of pathways.
	 */
	@Override
	public String getSummary() throws Exception {
		final String summary = "Removed rare Pathways found in less than " + getCutoff() + " samples" + RETURN;
		// if( !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_ABUNDANCE ) ) {
		// summary += SummaryUtil.getCountSummary( this.uniquePathwaysPerSample, "Unique Pathways", false );
		// summary += SummaryUtil.getCountSummary( this.totalPathwaysPerSample, "Total Pathways ", true );
		// this.sampleIds.removeAll( this.totalPathwaysPerSample.keySet() );
		// if( !this.sampleIds.isEmpty() ) {
		// summary += "Removed empty samples: " + this.sampleIds;
		// }
		// }
		freeMemory();
		return super.getSummary() + summary;
	}

	@Override
	public void runModule() throws Exception {
		this.sampleIds.addAll( MetaUtil.getSampleIds() );
		final int cutoff = getCutoff();
		for( final File file: getInputFiles() )
			if( cutoff < 1 ) FileUtils.copyFileToDirectory( file, getOutputDir() );
			else {
				final List<List<String>> table = BioLockJUtil.parseCountTable( file );
				logScarceData( removeScarcePathwayCounts( file, getScarcePathways( table ) ),
					getScarcePathwayLogFile() );
				logScarceData( removeScarceSamples( file, getScarceSampleIds( file, table ) ),
					getScarceSampleLogFile() );
			}
	}

	/**
	 * Save a list of low count pathways or samples to the module temp directory.
	 *
	 * @param map TreeMap(sampleId, TreeSet(data)) of Pathways found in too few samples or pathways
	 * @param file Output file
	 * @throws Exception if errors occur
	 */
	protected void logScarceData( final TreeMap<String, TreeSet<String>> map, final File file ) throws Exception {
		if( map == null || map.isEmpty() ) {
			Log.info( getClass(), "No low-count pathways detected" );
			return;
		}
		final BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
		try {
			for( final String id: map.keySet() )
				for( final String pathway: map.get( id ) )
					writer.write( id + ": " + pathway + RETURN );
		} finally {
			writer.close();
		}

		Log.info( getClass(),
			"Found " + map.size() + " samples with scarce pathways to removed- Pathway list saved to --> " +
				getScarcePathwayLogFile().getAbsolutePath() );
	}

	/**
	 * Remove Pathway Counts below the {@link biolockj.Config}.{@value biolockj.Constants#REPORT_MIN_COUNT}
	 *
	 * @param file input file
	 * @param scarcePathways Set of pathway names to be eliminated
	 * @return TreeMap(SampleId, TreeMap(Pathway)) Map removed pathways to sample ID
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, TreeSet<String>> removeScarcePathwayCounts( final File file,
		final Set<String> scarcePathways ) throws Exception {
		if( scarcePathways.isEmpty() ) return null;
		final TreeMap<String, TreeSet<String>> scarcePathMap = new TreeMap<>();
		final List<List<String>> table = BioLockJUtil.parseCountTable( file );
		final List<List<String>> output = new ArrayList<>();

		List<String> pathways = null;

		final List<String> validPathways = new ArrayList<>();
		int totalPathwayCount = 0;
		for( final List<String> record: table ) {
			final List<String> line = new ArrayList<>();
			final String id = record.get( 0 );
			line.add( id );
			if( pathways == null ) {
				for( int i = 1; i < record.size(); i++ ) {
					final String pathway = record.get( i );
					if( !scarcePathways.contains( pathway ) ) {
						line.add( pathway );
						validPathways.add( pathway );
					}
				}
				pathways = record;
				output.add( line );
				continue;
			}

			final TreeSet<String> validSamplePathways = new TreeSet<>();

			for( int i = 1; i < record.size(); i++ ) {
				final String pathway = pathways.get( i );
				if( !scarcePathways.contains( pathway ) ) {
					final Double count = Double.valueOf( record.get( i ) );
					line.add( count.toString() );
					totalPathwayCount += count;
					if( count > 0 ) validSamplePathways.add( pathway );
				}
			}

			output.add( line );

			final TreeSet<String> badSamplePathways = new TreeSet<>( validPathways );
			badSamplePathways.removeAll( validSamplePathways );

			this.totalPathwaysPerSample.put( id, String.valueOf( totalPathwayCount ) );
			this.uniquePathwaysPerSample.put( id, String.valueOf( validSamplePathways.size() ) );

			if( !badSamplePathways.isEmpty() ) {
				scarcePathMap.put( id, badSamplePathways );
				Log.warn( getClass(), id + ": Remove " + badSamplePathways.size() +
					" Pathways found in % samples below threshold: " + getMetaColName() );
				Log.debug( getClass(), id + ": Removed Pathways: " + badSamplePathways );
			}
		}

		buildOutputTable( file, output );

		return scarcePathMap;
	}

	@SuppressWarnings("unused")
	private boolean addMetaCol( final File file ) throws Exception {
		return Config.getBoolean( this, Constants.REPORT_NUM_HITS ) &&
			!Config.getBoolean( this, Constants.HN2_DISABLE_PATH_ABUNDANCE ) &&
			file.getName().contains( Constants.HN2_PATH_ABUND_SUM );
	}

	private void buildOutputTable( final File file, final List<List<String>> data )
		throws ConfigNotFoundException, ConfigFormatException, ConfigViolationException, IOException {
		final String cutoff = getMetaColName().replaceAll( "%", "per" );
		final File outFile = PathwayUtil.getPathwayCountFile( getOutputDir(), file, cutoff );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( outFile ) );
		try {
			for( final List<String> record: data ) {
				boolean newRecord = true;
				for( int i = 0; i < record.size(); i++ ) {
					writer.write( ( !newRecord ? Constants.TAB_DELIM: "" ) + record.get( i ) );
					newRecord = false;
				}

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

	private int getCutoff() throws Exception {
		if( this.scarceCountCutoff == null ) this.scarceCountCutoff =
			new Double( Math.ceil( MetaUtil.getSampleIds().size() * getScarceCountCutoff() ) ).intValue();
		return this.scarceCountCutoff;
	}

	private String getMetaColName() throws ConfigNotFoundException, ConfigFormatException {
		return "scarce" + new Double( getScarceCountCutoff() * 100 ).intValue() + "%";
	}

	private int getSampleCutoff() throws Exception {
		if( this.scarceSampleCutoff == null ) this.scarceSampleCutoff =
			new Double( Math.ceil( MetaUtil.getFieldNames().size() * getScarceSampleCutoff() ) ).intValue();

		return this.scarceSampleCutoff;
	}

	private Double getScarceCountCutoff() throws ConfigNotFoundException, ConfigFormatException {
		return Config.requirePositiveDouble( this, Constants.REPORT_SCARCE_CUTOFF );
	}

	private File getScarcePathwayLogFile() {
		return new File( getTempDir().getAbsolutePath() + File.separator + "scarcePathways" + TXT_EXT );
	}

	private Set<String> getScarcePathways( final List<List<String>> table ) throws Exception {
		final Set<String> scarcePathways = new HashSet<>();
		final Map<String, Double> pathMap = new HashMap<>();
		final List<String> pathways = table.get( 0 ).subList( 1, table.get( 0 ).size() - 1 );
		for( final String pathway: pathways )
			pathMap.put( pathway, 0.0 );

		for( final List<String> record: table.subList( 1, table.size() - 1 ) )
			for( int i = 0; i < pathways.size(); i++ ) {
				final Double count = Double.valueOf( record.get( i + 1 ) );
				final String pathway = pathways.get( i );
				if( count > 0 ) pathMap.put( pathway, pathMap.get( pathway ) + 1 );
			}

		for( final String pathway: pathways ) {
			final Double count = pathMap.get( pathway );
			if( count > 0 && count < getCutoff() ) scarcePathways.add( pathway );
		}

		return scarcePathways;
	}

	private Double getScarceSampleCutoff() throws ConfigNotFoundException, ConfigFormatException {
		return Config.requirePositiveDouble( this, Constants.REPORT_SAMPLE_CUTOFF );
	}

	private Set<String> getScarceSampleIds( final File file, final List<List<String>> table ) throws Exception {
		final Set<String> scarceIds = new HashSet<>();
		table.remove( 0 );
		for( final List<String> record: table ) {
			String id = null;
			int count = 0;
			boolean newRecord = true;
			for( final String cell: record ) {
				if( newRecord ) id = cell;
				else if( NumberUtils.isNumber( cell ) && Double.valueOf( cell ) > 0 ) count++;
				newRecord = false;
			}

			if( count < getSampleCutoff() && file.getName().contains( Constants.HN2_PATH_ABUND_SUM ) ) {
				scarceIds.add( id );
				this.totalPathwaysPerSample.remove( id );
				this.uniquePathwaysPerSample.remove( id );
			}
		}

		return scarceIds;
	}

	private File getScarceSampleLogFile() {
		return new File( getTempDir().getAbsolutePath() + File.separator + "scarceSamples" + TXT_EXT );
	}

	private TreeMap<String, TreeSet<String>> removeScarceSamples( final File file, final Set<String> scarceIds )
		throws ConfigNotFoundException, ConfigFormatException, ConfigViolationException, FileNotFoundException,
		IOException {
		final TreeMap<String, TreeSet<String>> scarceSampleMap = new TreeMap<>();
		final List<List<String>> table = BioLockJUtil.parseCountTable( file );
		final List<List<String>> output = new ArrayList<>();
		List<String> pathways = null;
		for( final List<String> record: table ) {
			final String id = record.get( 0 );
			if( id.equals( MetaUtil.getID() ) ) pathways = record.subList( 1, record.size() );
			if( scarceIds.contains( id ) && pathways != null ) {
				scarceSampleMap.put( id, new TreeSet<>() );
				for( int i = 1; i < record.size(); i++ )
					if( !record.get( i ).equals( "0" ) && !record.get( i ).equals( "0.0" ) )
						scarceSampleMap.get( id ).add( pathways.get( i - 1 ) );
			} else output.add( record );
		}

		buildOutputTable( file, output );
		return scarceSampleMap;
	}

	private final Set<String> sampleIds = new HashSet<>();
	private Integer scarceCountCutoff = null;
	private Integer scarceSampleCutoff = null;
	private Map<String, String> totalPathwaysPerSample = new HashMap<>();
	private Map<String, String> uniquePathwaysPerSample = new HashMap<>();
}
