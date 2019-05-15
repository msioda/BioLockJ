/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Apr 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.parser.wgs;

import java.io.*;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.module.BioModule;
import biolockj.module.classifier.wgs.Humann2Classifier;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.*;

/**
 * This BioModules parses Humann2Classifier output reports to build standard OTU abundance tables.<br>
 * Samples IDs are found in the column headers starting with the 2nd column.<br>
 * The count type depends on the HumanN2 config properties.
 * 
 * @blj.web_desc HumanN2 Parser
 */
public class Humann2Parser extends ParserModuleImpl {
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		PathwayUtil.verifyConfig( this );
		Config.getBoolean( this, HN2_KEEP_UNMAPPED );
		Config.getBoolean( this, HN2_KEEP_UNINTEGRATED );
	}

	@Override
	public String getSummary() throws Exception {
		final String longestLabel = "# Abundance File Pathways:";
		final int pad = longestLabel.length() + 4;

		String summary =
			SummaryUtil.getOutputDirSummary( this ) + ( hasScripts() ? SummaryUtil.getScriptDirSummary( this ): "" );
		if( this.numPathwayAbund != null )
			summary += BioLockJUtil.addTrailingSpaces( longestLabel, pad ) + this.numPathwayAbund + RETURN;
		if( this.numPathwayCovg != null ) summary +=
			BioLockJUtil.addTrailingSpaces( "# Coverage File Pathways:", pad ) + this.numPathwayCovg + RETURN;
		if( this.numGeneFamilies != null )
			summary += BioLockJUtil.addTrailingSpaces( "# Gene Families:", pad ) + this.numGeneFamilies + RETURN;
		if( !Config.getBoolean( this, HN2_KEEP_UNMAPPED ) )
			summary += "UNMAPPED column discarded from output tables" + RETURN;
		if( !Config.getBoolean( this, HN2_KEEP_UNINTEGRATED ) )
			summary += "UNINTEGRATED column discarded from output tables" + RETURN;

		return summary;
	}

	@Override
	public boolean isValidInputModule( final BioModule module ) {
		return module instanceof Humann2Classifier;
	}

	/**
	 * To parse the taxonomy level reports output by {@link biolockj.module.classifier.wgs.Humann2Classifier}. Skip
	 * mapping of UNMAPPED and UNINTEGRATED columns
	 *
	 * Sample HumanN2 report line (head output_pAbund.tsv):<br>
	 * 1st cell format: [Pathway_ID]:[Pathway_Descr] | g__[genus_taxa].s__[species_taxa]<br>
	 * Example: ARO-PWY: chorismate biosynthesis I|g__Acidaminococcus.s__Acidaminococcus_intestini #SampleID
	 * Metaphlan22_Analysis #clade_name relative_abundance coverage average_genome_length_in_the_clade
	 * estimated_number_of_reads_from_the_clade k__Bacteria|p__Bacteroidetes 14.68863 0.137144143537 4234739 580770
	 */
	@Override
	public void parseSamples() throws Exception {
		int count = 0;
		for( final File file: getInputFiles() ) {
			final String[][] data = transpose( assignSampleIDs( BioLockJUtil.parseCountTable( file ) ) );
			final File outFile = PathwayUtil.getPathwayCountFile( getOutputDir(), file, HN2_PARSED );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( outFile ) );
			try {
				boolean headerRow = true;
				final Set<Integer> skipCols = new HashSet<>();
				for( final String[] record: data ) {
					if( this.numSamples == null ) count++;
					boolean newRecord = true;
					for( int i = 0; i < record.length; i++ ) {
						final String cell = BioLockJUtil.removeQuotes( record[ i ] );
						if( headerRow && cell.equals( UNMAPPED ) && !Config.getBoolean( this, HN2_KEEP_UNMAPPED ) )
							skipCols.add( i );
						else if( headerRow && cell.equals( UNINTEGRATED ) &&
							!Config.getBoolean( this, HN2_KEEP_UNINTEGRATED ) ) skipCols.add( i );
						else if( skipCols.contains( i ) ) skipCols.add( i );
						else writer.write( ( !newRecord ? Constants.TAB_DELIM: "" ) + cell );

						newRecord = false;
					}
					writer.write( Constants.RETURN );
					headerRow = false;
				}
			} finally {
				writer.close();
			}

			if( this.numSamples == null ) this.numSamples = count;
			if( PathwayUtil.getHn2Type( file ).equals( Constants.HN2_PATH_ABUND_SUM ) )
				this.numPathwayAbund = data[ 0 ].length - 1;
			else if( PathwayUtil.getHn2Type( file ).equals( Constants.HN2_PATH_COVG_SUM ) )
				this.numPathwayCovg = data[ 0 ].length - 1;
			else if( PathwayUtil.getHn2Type( file ).equals( Constants.HN2_GENE_FAM_SUM ) )
				this.numGeneFamilies = data[ 0 ].length - 1;

			MemoryUtil.reportMemoryUsage( "Parsed " + file.getAbsolutePath() );
		}
	}

	@Override
	public void runModule() throws Exception {
		parseSamples();
	}

	private static List<List<String>> assignSampleIDs( final List<List<String>> data ) {
		final List<List<String>> output = new ArrayList<>();
		boolean firstRecord = true;
		for( final List<String> row: data ) {
			final ArrayList<String> record = new ArrayList<>();
			if( firstRecord ) for( final String cell: row )
				record.add( record.isEmpty() ? MetaUtil.getID(): getSampleID( cell ) );
			else record.addAll( row );

			output.add( record );
			firstRecord = false;
		}

		return output;
	}

	private static String getSampleID( final String name ) {
		String id = name;
		if( id.contains( PAIRED_SUFFIX ) ) id = id.replace( PAIRED_SUFFIX, "" );
		if( id.contains( KD_SUFFIX ) ) id = id.replace( KD_SUFFIX, "" );
		if( id.contains( ABUND_SUFFIX ) ) id = id.replace( ABUND_SUFFIX, "" );
		if( id.contains( COVERAGE_SUFFIX ) ) id = id.replace( COVERAGE_SUFFIX, "" );
		if( id.contains( RPK_SUFFIX ) ) id = id.replace( RPK_SUFFIX, "" );

		return id;
	}

	private static String[][] transpose( final List<List<String>> data ) {
		final List<List<String>> transposed = new ArrayList<>();
		for( final List<String> row: data ) {
			final List<String> outputRow = new ArrayList<>();
			outputRow.add( row.get( 0 ) );
			transposed.add( outputRow );
		}

		final int m = data.size();
		final int n = data.get( 0 ).size();

		final String transpose[][] = new String[ n ][ m ];

		for( int c = 0; c < m; c++ )
			for( int d = 0; d < n; d++ )
				transpose[ d ][ c ] = data.get( c ).get( d );

		return transpose;
	}

	private Integer numGeneFamilies = null;

	private Integer numPathwayAbund = null;

	private Integer numPathwayCovg = null;
	private Integer numSamples = null;
	/**
	 * {@link biolockj.Config} Boolean property: {@value #HN2_KEEP_UNINTEGRATED}<br>
	 * Set value = {@value biolockj.Constants#TRUE} to keep UNINTEGRATED column in count tables
	 */
	protected static final String HN2_KEEP_UNINTEGRATED = "humann2.keepUnintegrated";
	/**
	 * {@link biolockj.Config} Boolean property: {@value #HN2_KEEP_UNMAPPED}<br>
	 * Set value = {@value biolockj.Constants#TRUE} to keep UNMAPPED column in count tables
	 */
	protected static final String HN2_KEEP_UNMAPPED = "humann2.keepUnmapped";
	private static final String ABUND_SUFFIX = "_Abundance";
	private static final String COVERAGE_SUFFIX = "_Coverage";
	private static final String HN2_PARSED = "hn2";
	private static final String KD_SUFFIX = "_kneaddata";
	private static final String PAIRED_SUFFIX = "_paired_merged";
	private static final String RPK_SUFFIX = "-RPKs";
	private static final String UNINTEGRATED = "UNINTEGRATED";
	private static final String UNMAPPED = "UNMAPPED";

}
