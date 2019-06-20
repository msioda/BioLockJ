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
package biolockj.module.implicit.parser.wgs;

import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.node.OtuNode;
import biolockj.node.ParsedSample;
import biolockj.node.wgs.Kraken2Node;
import biolockj.util.*;

/**
 * This BioModules parses KrakenClassifier output reports to build standard OTU abundance tables.
 * 
 * @blj.web_desc Kraken2 Parser
 */
public class Kraken2Parser extends ParserModuleImpl {
	/**
	 * Parse all {@link biolockj.module.classifier.wgs.Kraken2Classifier} reports in the input directory.<br>
	 * Build an {@link biolockj.node.wgs.Kraken2Node} for each line.<br>
	 * If {@link #isValid(OtuNode)}: <br>
	 * <ol>
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.wgs.KrakenNode#getSampleId()} if not
	 * yet created.
	 * <li>Add the {@link biolockj.node.wgs.KrakenNode#getCount()} (1) to {@link biolockj.node.ParsedSample} OTU count.
	 * </ol>
	 * <p>
	 * Sample Kraken report line (head 7A_reported.tsv) :<br>
	 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales 20094
	 */
	@Override
	public void parseSamples() throws Exception {
		for( final File file: getInputFiles() ) {
			MemoryUtil.reportMemoryUsage( "Parse " + file.getAbsolutePath() );
			assignLeafCounts( file );
			reportLeafCounts();
		}
	}
	
	private static Map<String, Long> levelCounts( final Map<String, Long> otuCounts, final String name ) {
		final Map<String, Long> map = new TreeMap<>();
		for( final String otu: otuCounts.keySet() ) {
			if( otu.contains( name ) ) map.put( otu, otuCounts.get( otu ) );
		}
		return map;
	}
	
	private Map<String, Long> findUnclassifiedTaxa() throws Exception {
		List<String> childLevels = new ArrayList<>();
		childLevels.add( TaxaUtil.bottomTaxaLevel() );
		final Map<String, Long> otuCounts = leafCounts().get( childLevels.get( 0 ) );
		List<String> levels = TaxaUtil.getTaxaLevelSpan();
		levels.remove( childLevels.get( 0 ) );
		Collections.reverse( levels );
		for( final String level: levels ) {
			final Map<String, Long> leafOtuCounts = leafCounts().get( level );
			for( String keyOtu: leafOtuCounts.keySet() ) {
				String taxa = TaxaUtil.getTaxaName( keyOtu, level );
				final String name = OtuUtil.buildOtuTaxa( level, taxa );
				Map<String, Long> levelCounts = levelCounts( otuCounts, name );
				Long levelSum = levelCounts.isEmpty() ? 0L : levelCounts.values().stream().mapToLong( Long::longValue ).sum();
				Long diff = leafOtuCounts.get( keyOtu ) - levelSum;
				if( diff > 0 && levelSum > 0 ) {
					String newTaxa = TaxaUtil.getUnclassifiedTaxa( taxa, level );
					String otu = levelCounts.keySet().iterator().next();
					otu = otu.substring( 0, otu.indexOf( childLevels.get( childLevels.size() - 1 ) ) );
					for( int i=childLevels.size()-1; i>=0; i-- ) {
						otu += OtuUtil.buildOtuTaxa( childLevels.get( i ), newTaxa );
						if( i > 0 ) otu += Constants.SEPARATOR;
					}
					otuCounts.put( otu, diff );
				} else if( diff < 0 ) throw new Exception( "Diff cannot be less than zero!" );
			}
			childLevels.add( level );

		}
		return otuCounts;
	}
	
	@Override
	public void buildOtuCountFiles() throws Exception {	
		for( final ParsedSample sample: getParsedSamples() ) {
			final Map<String, Long> otuCounts = findUnclassifiedTaxa();
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
	
	private void parseSample( final File file ) throws Exception {
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
				// Log.debug( getClass(), " LINE = " + line );
				addOtuNode( new Kraken2Node( SeqUtil.getSampleId( file.getName() ), line ) );
		} finally {
			if( reader != null ) reader.close();
		}
	}
	
	@Override
	public void addOtuNode( final OtuNode node ) throws Exception {
		if( isValid( node ) ) {
			final ParsedSample sample = getParsedSample( node.getSampleId() );
			//if( sample == null ) addParsedSample( new ParsedSample( node, leafCounts ) );
			if( sample == null ) addParsedSample( new ParsedSample( node ) );
			else sample.addNode( node );
		}
	}
	
	private void assignLeafCounts( final File file ) throws Exception {
		boolean reportMissingTaxa = Config.getBoolean( this, Constants.REPORT_UNCLASSIFIED_TAXA );
		String modProp = Config.getModulePropName( this, Constants.REPORT_UNCLASSIFIED_TAXA );
		if( reportMissingTaxa ) Config.setConfigProperty( modProp, Constants.FALSE );
		try {
			parseSample( file );
			ParsedSample sample = getParsedSample( SeqUtil.getSampleId( file.getName() ) );
			Map<String, Long> otuCounts = sample.getOtuCounts( false );
			for( String otu: otuCounts.keySet() ) {
				String level = TaxaUtil.getLeafTaxa( otu ).keySet().iterator().next();
				if( leafCounts().get( level ) == null ) leafCounts().put( level, new HashMap<>() );
				leafCounts().get( level ).put( otu, otuCounts.get( otu ) );
			}
			
		} finally {
			if( reportMissingTaxa ) Config.setConfigProperty( modProp, Constants.TRUE );
		}
	}
	
	private void reportLeafCounts() {
		for( String level: leafCounts().keySet() )
			for( String otu: leafCounts().get( level ).keySet() )
				Log.debug( getClass(), "Level[ " + level + " ] - OTU [ " + otu + " ] --> " + leafCounts().get( level ).get( otu ) );
	}
	
	private Map<String, Map<String, Long>> leafCounts() {
		return this.leafCounts;
	}
	
	private Map<String, Map<String, Long>> leafCounts = new HashMap<>();
}
