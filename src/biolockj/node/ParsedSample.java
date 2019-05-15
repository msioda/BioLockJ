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

import java.io.Serializable;
import java.util.*;
import biolockj.Constants;
import biolockj.Log;
import biolockj.util.OtuUtil;
import biolockj.util.TaxaUtil;

/**
 * {@link biolockj.module.implicit.parser.ParserModule}s create and store one {@link biolockj.node.ParsedSample}, with
 * OTU assignments and counts, for each sample. Only the samples that contain one or more reads used by
 * {@link biolockj.module.classifier.ClassifierModule} to generate a valid taxonomic assignment are used.
 */
public class ParsedSample implements Serializable, Comparable<ParsedSample> {

	/**
	 * Construct a new ParsedSample with it's 1st OtuNode.
	 *
	 * @param node OtuNode
	 */
	public ParsedSample( final OtuNode node ) {
		this.sampleId = node.getSampleId();
		addNode( node );
	}

	/**
	 * Add the OtuNode to the ParsedSample.
	 *
	 * @param node OtuNode
	 */
	public void addNode( final OtuNode node ) {
		if( this.otuCounts.get( node.getOtuName() ) == null ) {
			Log.debug( getClass(), "Add new OtuNode: " + node.getOtuName() + "=" + node.getCount() );
			this.otuCounts.put( node.getOtuName(), node.getCount() );
		} else {
			final int count = this.otuCounts.get( node.getOtuName() ) + node.getCount();
			Log.debug( getClass(), "Update OtuNode: " + node.getOtuName() + "=" + count );
			this.otuCounts.put( node.getOtuName(), count );
		}
	}

	@Override
	public int compareTo( final ParsedSample o ) {
		return o.getSampleId().compareTo( getSampleId() );
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( getSampleId() == null ) ? 0: getSampleId().hashCode() );
		return result;
	}
	
	@Override
	public boolean equals( Object obj ) {
		if( this == obj ) return true;
		if( obj == null ) return false;
		if( !( obj instanceof ParsedSample ) ) return false;
		ParsedSample other = (ParsedSample) obj;
		if( getSampleId() == null ) {
			if( other.getSampleId() != null ) return false;
		} else if( !getSampleId().equals( other.getSampleId() ) ) return false;
		return true;
	}

	/**
	 * Get the streamlined taxonomy tree with counts, each OTU listed only 1 time with num occurrences in the
	 * sample.<br>
	 * Example:
	 * d__Bacteria;p__Bacteroidetes;c__Bacteroidia;o__Bacteroidales;f__Bacteroidaceae;g__Bacteroides;s__Bacteroides_vulgatus
	 * 87342
	 * 
	 * @return map OTU-count
	 * @throws Exception if errors occur
	 */
	public TreeMap<String, Integer> getOtuCounts() throws Exception {
		// Log.debug( getClass(), "Calling getOtuCounts() for: " + sampleId );
		if( this.otuCounts == null ) throw new Exception( getClass().getName()
			+ ".getOtuCounts() should be called only once - cached data is cleared after 1st call." );
		else if( this.otuCounts.isEmpty() ) {
			Log.debug( getClass(), "No valid OTUs found for: " + this.sampleId );
			return null;
		}

		final TreeMap<String, Integer> fullPathOtuCounts = new TreeMap<>();
		for( String otu: this.otuCounts.keySet() ) {
			if( otu.isEmpty() ) continue;

			final Set<String> kids = getChildren( fullPathOtuCounts, otu );
			final int otuCount = this.otuCounts.get( otu );
			if( kids.isEmpty() ) {
				Log.debug( getClass(), "Add [ " + this.sampleId + " ] OTU " + otu + "=" + otuCount );
				fullPathOtuCounts.put( otu, otuCount );
			} else {
				final int totalCount = totalCount( fullPathOtuCounts, kids );
				if( totalCount < otuCount ) {
					String parentTaxa = null;
					for( final String level: TaxaUtil.getTaxaLevelSpan() )
						if( otu.contains( level ) ) parentTaxa = TaxaUtil.getTaxaName( otu, level );
						else if( parentTaxa != null ) otu += Constants.SEPARATOR
							+ OtuUtil.buildOtuTaxa( level, TaxaUtil.buildUnclassifiedTaxa( parentTaxa ) );

					final int diff = otuCount - totalCount;
					fullPathOtuCounts.put( otu, diff );
					Log.debug( getClass(),
						"Add parent remainder count [ " + this.sampleId + " ] Unclassified OTU: " + otu + "=" + diff );
				} else if( otuCount >= totalCount )
					Log.debug( getClass(), "Ignore [" + this.sampleId + " ] Parent OTU " + otu + "=" + otuCount );
			}
		}
		this.otuCounts = null;
		return fullPathOtuCounts;
	}

	/**
	 * Getter for sampleId.
	 *
	 * @return Sample ID.
	 */
	public String getSampleId() {
		return this.sampleId;
	}

	private static Set<String> getChildren( final TreeMap<String, Integer> otuCounts, final String otu ) {
		final Set<String> kids = new HashSet<>();
		for( final String key: otuCounts.keySet() )
			if( key.contains( otu ) ) kids.add( key );
		return kids;
	}

	private static int totalCount( final TreeMap<String, Integer> otuCounts, final Set<String> otus ) {
		int count = 0;
		for( final String otu: otus )
			count += otuCounts.get( otu );
		return count;
	}

	private Map<String, Integer> otuCounts = new TreeMap<>();
	private final String sampleId;
	private static final long serialVersionUID = 4882054401193953055L;
}
