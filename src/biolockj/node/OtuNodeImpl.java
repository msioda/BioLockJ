/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 2, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.node;

import java.util.HashMap;
import java.util.Map;
import biolockj.*;
import biolockj.exception.ConfigFormatException;
import biolockj.util.OtuUtil;
import biolockj.util.TaxaUtil;

/**
 * The default implementation of {@link biolockj.node.OtuNode} is also the superclass for all WGS and 16S OtuNode
 * classes. OtuNodes hold taxonomy assignment info, represents one line of
 * {@link biolockj.module.classifier.ClassifierModule} output. The assignment is stored in a local taxaMap, which holds
 * one taxa name per taxonomy level.
 */
public abstract class OtuNodeImpl implements OtuNode, Comparable<OtuNode> {

	/**
	 * If called for level not included in {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS},
	 * check if the top level taxonomy level is already assigned. If so, populate missing levels by passing the parent
	 * taxa to {@link biolockj.util.TaxaUtil#getUnclassifiedTaxa(String, String)} until the given in level is
	 * assigned.<br>
	 * <br>
	 * This method assumes it is called repeatedly for each OTU (once/level) and that each subsequent call passes a
	 * lower taxonomy level than the previous.
	 * 
	 * @param taxa Taxa name
	 * @param level Taxonomy level
	 * @throws Exception if errors occur
	 */
	@Override
	public void addTaxa( final String taxa, final String level ) throws Exception {
		if( level == null || taxa == null || level.trim().isEmpty() || taxa.trim().isEmpty() ) {
			Log.debug( getClass(), "ID=[ " + this.sampleId + " ] --> Taxa missing for: level=[ " + level +
				" ]; Taxa=[ " + taxa + " ]; Line =[ " + this.line + " ]" );
			return;
		}

		// Some classifiers report species or genus name but are missing
		// domain/phylum/class/order/family info.
		// If top level taxonomy level undefined, we omit the result for lack of context.
		if( !TaxaUtil.getTaxaLevels().contains( level ) ) {
			if( !this.taxaMap.containsKey( TaxaUtil.topTaxaLevel() ) ) return;

			// Some classifiers will report a phylum and a lower level (like genus) but are missing in-between levels.
			// Populate missing levels between top-level and current level
			String parentTaxa = null;
			String parentLevel = null;
			for( final String testLevel: TaxaUtil.getTaxaLevelSpan() ) {
				if( this.taxaMap.keySet().contains( testLevel ) ) {
					parentTaxa = this.taxaMap.get( testLevel );
					parentLevel = testLevel;
				} else if( parentTaxa != null && Config.getBoolean( Pipeline.exeModule(), Constants.REPORT_UNCLASSIFIED_TAXA ) ) {
					final String unclassifiedTaxa = TaxaUtil.getUnclassifiedTaxa( parentTaxa, parentLevel );
					this.taxaMap.put( testLevel, unclassifiedTaxa );
				}

				if( level.equals( testLevel ) ) return;
			}

			// Log.debug( getClass(), level + " = Not a valid level" );
			return;
		}

		if( this.taxaMap.get( level ) != null ) Log.debug( getClass(), this.sampleId + " overwriting OTU: " +
			this.taxaMap.get( level ) + " with " + taxa + "  --> Line = " + this.line );

		// Log.debug( getClass(), "taxaMap.put( level=" + level + ", taxa=" + taxa + " )" );
		this.taxaMap.put( level, taxa );
	}

	/**
	 * The sort order of the TreeSet({@link biolockj.node.OtuNode}) is absolutely required for proper processing in
	 * {@link #addTaxa(String, String)} and {@link #getTaxaMap()}. We sort alphabetically by otuName, however if one
	 * otuName is a substring of the compared otuName, the longer name is ordered before the shorter name.<br>
	 * Because of this, when iterating through a sorted list of {@link biolockj.node.OtuNode}s we can be sure we add
	 * nodes in the taxonomy hierarchy from the bottom-up.
	 */
	@Override
	public int compareTo( final OtuNode node ) {
		return node.getOtuName().compareTo( getOtuName() );
	}

	@Override
	public Map<String, String> delimToLevelMap() {
		if( delimToLevelMap.isEmpty() ) {
			delimToLevelMap.put( DOMAIN_DELIM, Constants.DOMAIN );
			delimToLevelMap.put( PHYLUM_DELIM, Constants.PHYLUM );
			delimToLevelMap.put( CLASS_DELIM, Constants.CLASS );
			delimToLevelMap.put( ORDER_DELIM, Constants.ORDER );
			delimToLevelMap.put( FAMILY_DELIM, Constants.FAMILY );
			delimToLevelMap.put( GENUS_DELIM, Constants.GENUS );
			delimToLevelMap.put( SPECIES_DELIM, Constants.SPECIES );
		}
		return delimToLevelMap;
	}

	@Override
	public long getCount() {
		return this.count;
	}

	@Override
	public String getLine() {
		return this.line;
	}

	@Override
	public String getOtuName() {
		if( this.name != null ) return this.name;

		final StringBuffer otu = new StringBuffer();
		try {
			String parentTaxa = null;
			String parentLevel = null;

			for( final String level: TaxaUtil.getTaxaLevels() ) {
				String taxaName = this.taxaMap.get( level );
				if( taxaName != null && taxaName.trim().isEmpty() ) taxaName = null;
				if( taxaName == null && Config.getBoolean( Pipeline.exeModule(), Constants.REPORT_UNCLASSIFIED_TAXA ) ) 
					taxaName = TaxaUtil.getUnclassifiedTaxa( parentTaxa, parentLevel );
				else if( taxaName != null ) {
					parentTaxa = taxaName;
					parentLevel = level;
				}
				
				if( taxaName == null ) continue;
				if( !otu.toString().isEmpty() ) otu.append( Constants.SEPARATOR );
				otu.append( OtuUtil.buildOtuTaxa( level, taxaName ) );
			}
		} catch( final Exception ex ) {
			Log.error( getClass(), "Unable to build OTU name for " + this.sampleId, ex );
		}
		this.name = otu.toString();
		return this.name;
	}

	@Override
	public String getSampleId() {
		return this.sampleId;
	}

	/**
	 * This implementation ensures all levels between top and bottom taxonomy levels are complete. If missing middle
	 * levels are found, they inherit their parent taxa.
	 */
	@Override
	public Map<String, String> getTaxaMap() throws Exception {
		if( !this.taxaMap.containsKey( TaxaUtil.topTaxaLevel() ) ) {
			Log.debug( getClass(), "Omit incomplete [ " + this.sampleId + " ] OTU missing the top taxonomy level: " +
				TaxaUtil.topTaxaLevel() + ( this.line.isEmpty() ? "": ", classifier output = " + this.line ) );
			return null;
		}

		populateInBetweenTaxa();
		return this.taxaMap;
	}

	@Override
	public void setCount( final long count ) {
		this.count = count;
	}

	/**
	 * Set line only if in DEBUG mode, so we can print OtuNode constructor inputs.
	 */
	@Override
	public void setLine( final String line ) {
		try {
			if( Log.doDebug() ) this.line = line;
		} catch( final Exception ex ) {
			Log.error( getClass(),
				"Unable to set OtuNode line: " + ( this.sampleId == null ? "sampleId UNDEFINED": this.sampleId ), ex );
		}
	}

	@Override
	public void setSampleId( final String sampleId ) {
		this.sampleId = sampleId;
	}

	/**
	 * Populate missing OTUs if top level taxa is defined and there is a level gap between the top level and the bottom
	 * level. If configured, missing levels inherit the parent name as "Unclassified (parent-name) OTU".
	 * 
	 * @throws ConfigFormatException if Config prop boolean does not contain Y or N
	 */
	protected void populateInBetweenTaxa() throws ConfigFormatException {
		final int numTaxa = this.taxaMap.size();
		int numFound = 0;
		String parentTaxa = null;
		String parentLevel = null;
		for( final String level: TaxaUtil.getTaxaLevelSpan() )
			if( this.taxaMap.get( level ) != null ) {
				numFound++;
				parentTaxa = this.taxaMap.get( level );
				parentLevel = level;
			} else if( Config.getBoolean( Pipeline.exeModule(), Constants.REPORT_UNCLASSIFIED_TAXA ) && parentTaxa != null &&
				this.taxaMap.get( level ) == null && numFound < numTaxa )
				this.taxaMap.put( level, TaxaUtil.getUnclassifiedTaxa( parentTaxa, parentLevel ) );
			else if( numFound == numTaxa ) break;
	}

	private long count = 0;
	private String line = "";
	private String name = null;
	private String sampleId = null;

	// key=level, val=otu
	private final Map<String, String> taxaMap = new HashMap<>();
	/**
	 * Standard classifier output level delimiter for CLASS
	 */
	protected static String CLASS_DELIM = "c__";

	/**
	 * Standard classifier output level delimiter for DOMAIN
	 */
	protected static String DOMAIN_DELIM = "d__";

	/**
	 * Standard classifier output level delimiter for FAMILY
	 */
	protected static String FAMILY_DELIM = "f__";

	/**
	 * Standard classifier output level delimiter for GENUS
	 */
	protected static String GENUS_DELIM = "g__";

	/**
	 * Standard classifier output level delimiter for ORDER
	 */
	protected static String ORDER_DELIM = "o__";

	/**
	 * Standard classifier output level delimiter for PHYLUM
	 */
	protected static String PHYLUM_DELIM = "p__";

	/**
	 * Standard classifier output level delimiter for SPECIES
	 */
	protected static String SPECIES_DELIM = "s__";

	private static final Map<String, String> delimToLevelMap = new HashMap<>();
}
