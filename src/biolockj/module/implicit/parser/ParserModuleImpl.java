/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.parser;

import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.JavaModuleImpl;
import biolockj.node.OtuNode;
import biolockj.node.ParsedSample;
import biolockj.util.*;

/**
 * Parser {@link biolockj.module.BioModule}s read {@link biolockj.module.classifier.ClassifierModule} output to build
 * standardized OTU count tables. This class provides the default abstract implementation.
 */
public abstract class ParserModuleImpl extends JavaModuleImpl implements ParserModule {

	@Override
	public void addOtuNode( final OtuNode node ) throws Exception {
		if( isValid( node ) ) {
			final ParsedSample sample = getParsedSample( node.getSampleId() );
			if( sample == null ) addParsedSample( new ParsedSample( node ) );
			else sample.addNode( node );
		}
	}
	
	@Override
	public void buildOtuCountFiles() throws Exception {	
		for( final ParsedSample sample: getParsedSamples() ) {
			final TreeMap<String, Long> otuCounts = sample.getOtuCounts( true );
			if( otuCounts != null ) {
				final File outputFile = OtuUtil.getOtuCountFile( getOutputDir(), sample.getSampleId(), null );
				Log.info( getClass(), "Build output sample: " + sample.getSampleId() + " | #OTUs=" + otuCounts.size() +
					"--> " + outputFile.getAbsolutePath() );
				final BufferedWriter writer = new BufferedWriter( new FileWriter( outputFile ) );
				try {
					long numOtus = 0;
					for( final String otu: otuCounts.keySet() ) {
						getUniqueOtus().add( otu );
						final long count = otuCounts.get( otu );
						writer.write( otu + TAB_DELIM + count + RETURN );
						numOtus += count;
					}

					getHitsPerSample().put( sample.getSampleId(), String.valueOf( numOtus ) );

				} finally {
					writer.close();
				}
			} else Log.error( getClass(),
				"buildOtuCountFiles should not encounter empty sample files where sample.getOtuCounts() == null!  Found null for: " +
					sample.getSampleId() );
		}
	}

	/**
	 * Execute {@link #validateModuleOrder()} to validate module configuration order.
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		validateModuleOrder();
	}
	
	@Override
	public ParsedSample getParsedSample( final String sampleId ) {
		for( final ParsedSample sample: getParsedSamples() )
			if( sample.getSampleId().equals( sampleId ) ) return sample;
		return null;
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception {
		String summary = SummaryUtil.getCountSummary( getHitsPerSample(), "OTUs", true );
		getSampleIds().removeAll( getHitsPerSample().keySet() );
		if( !getSampleIds().isEmpty() ) summary += "Removed empty metadata records: " + getSampleIds() + RETURN;

		summary += "# Unique OTUs: " + getUniqueOtus().size() + RETURN;
		freeMemory();
		return super.getSummary() + summary;
	}

	@Override
	public abstract void parseSamples() throws Exception;

	/**
	 * Parsers execute a task with 3 core functions:
	 * <ol>
	 * <li>{@link #parseSamples()} - generates {@link biolockj.node.ParsedSample}s
	 * <li>{@link #buildOtuCountFiles()} - builds OTU tree tables from {@link biolockj.node.ParsedSample}s
	 * </ol>
	 */
	@Override
	public void runModule() throws Exception {
		getSampleIds().addAll( MetaUtil.getSampleIds() );
		MemoryUtil.reportMemoryUsage( "About to parse samples" );
		parseSamples();
		if( getParsedSamples().isEmpty() ) throw new Exception( "Parser failed to produce output!" );
		Log.debug( getClass(), "# Samples parsed: " + getParsedSamples().size() );
		buildOtuCountFiles();

		if( Config.getBoolean( this, Constants.REPORT_NUM_HITS ) )
			MetaUtil.addColumn( NUM_OTUS, getHitsPerSample(), getOutputDir(), true );
	}

	/**
	 * Some {@link biolockj.module.classifier.ClassifierModule}s can include taxonomy level identifiers without an OTU
	 * name in the sample report files. This method verifies the node exists, has a valid sample ID, and that no empty
	 * String OTU names are reported.
	 *
	 * @param node OtuNode build from 1 line of a {@link biolockj.module.classifier.ClassifierModule} output file
	 * @return boolean if {@link biolockj.node.OtuNode} is valid
	 * @throws Exception if errors occur checking if node is valid
	 */
	protected boolean isValid( final OtuNode node ) throws Exception {
		boolean isValid = node != null && node.getSampleId() != null && !node.getSampleId().isEmpty() &&
			node.getTaxaMap() != null && !node.getTaxaMap().isEmpty() && node.getCount() > 0;
		if( !isValid ) Log.warn( getClass(), "" );
		return isValid;
	}

	/**
	 * Validate that no {@link biolockj.module.seq} modules run after this parser unless a new classifier branch is
	 * started.
	 * 
	 * @throws Exception if modules are out of order
	 */
	protected void validateModuleOrder() throws Exception {
		for( final BioModule module: ModuleUtil.getModules( this, true ) )
			if( module.equals( ModuleUtil.getClassifier( this, true ) ) ) break;
			else if( module.getClass().getName().startsWith( Constants.MODULE_SEQ_PACKAGE ) )
				throw new Exception( "Invalid BioModule configuration order! " + module.getClass().getName() +
					" must run before the ParserModule." );
	}

	/**
	 * Add {@link biolockj.node.ParsedSample} to parser cache
	 *  
	 * @param parsedSample ParsedSample
	 * @throws Exception if method is used to add a duplicate sample
	 */
	protected void addParsedSample( final ParsedSample parsedSample ) throws Exception {
		for( final ParsedSample sample: getParsedSamples() )
			if( sample.getSampleId().equals( parsedSample.getSampleId() ) )
				throw new Exception( "Attempt to add duplicate sample! " + sample.getSampleId() );
		getParsedSamples().add( parsedSample );
	}

	private void freeMemory() {
		this.hitsPerSample = null;
		this.parsedSamples = null;
		this.sampleIds = null;
		this.uniqueOtus = null;
	}

	/**
	 * Getter for depricatedOtuCountFields
	 * 
	 * @return depricatedOtuCountFields
	 */
	public static Set<String> getDepricatedOtuCountFields() {
		return depricatedOtuCountFields;
	}

	/**
	 * Getter for otuCountField
	 * 
	 * @return otuCountField
	 */
	public static String getOtuCountField() {
		return otuCountField;
	}

	/**
	 * When a module modifies the number of hits, the new counts must replace the old count fields.
	 * 
	 * @param name Name of new number of hits metadata field
	 * @throws Exception if null value passed
	 */
	public static void setNumHitsFieldName( final String name ) throws Exception {
		if( name == null )
			throw new Exception( "Null name value passed to ParserModuleImpl.setNumHitsFieldName(name)" );
		else if( otuCountField != null && otuCountField.equals( name ) )
			Log.warn( ParserModuleImpl.class, "Num Hits field already set to: " + otuCountField );
		else {
			if( otuCountField != null ) depricatedOtuCountFields.add( otuCountField );

			depricatedOtuCountFields.remove( name );
			otuCountField = name;
		}
	}
	
	protected TreeSet<ParsedSample> getParsedSamples() {
		return this.parsedSamples;
	}
	
	protected Set<String> getUniqueOtus() {
		return this.uniqueOtus;
	}
	
	protected Set<String> getSampleIds() {
		return this.sampleIds;
	}
	
	protected Map<String, String> getHitsPerSample() {
		return this.hitsPerSample;
	}

	private Map<String, String> hitsPerSample = new HashMap<>();
	private TreeSet<ParsedSample> parsedSamples = new TreeSet<>();
	private Set<String> sampleIds = new HashSet<>();
	private Set<String> uniqueOtus = new HashSet<>();

	/**
	 * Metadata column name for column that holds number of OTU hits after any {@link biolockj.module.implicit.parser}
	 * module executes: {@value #NUM_OTUS}
	 */
	protected static final String NUM_OTUS = "OTU_COUNT";
	private static final Set<String> depricatedOtuCountFields = new HashSet<>();
	private static String otuCountField = NUM_OTUS;

}
