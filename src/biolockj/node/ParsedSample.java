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
import biolockj.Log;
import biolockj.util.BioLockJUtil;
import biolockj.util.OtuUtil;

/**
 * {@link biolockj.module.implicit.parser.ParserModule}s create and store one {@link biolockj.node.ParsedSample}, with
 * OTU assignments and counts, for each sample. Only the samples that contain one or more reads used by
 * {@link biolockj.module.classifier.ClassifierModule} to generate a valid taxonomic assignment are used.
 */
public class ParsedSample implements Serializable, Comparable<ParsedSample>
{

	/**
	 * Construct a new ParsedSample with it's 1st OtuNode.
	 *
	 * @param node OtuNode
	 * @throws Exception if propagated by {@link #addNode(OtuNode)}
	 */
	public ParsedSample( final OtuNode node ) throws Exception
	{
		sampleId = node.getSampleId();
		addNode( node );
	}

	/**
	 * Add the OtuNode to the ParsedSample.
	 *
	 * @param node OtuNode
	 */
	public void addNode( final OtuNode node )
	{
		otuNodes.add( node );
	}

	@Override
	public int compareTo( final ParsedSample o )
	{
		return o.getSampleId().compareTo( getSampleId() );
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
	public Map<String, Integer> getOtuCounts() throws Exception
	{
		if( otuNodes.isEmpty() )
		{
			Log.warn( getClass(), "No samples found for " + sampleId );
			return null;
		}

		final Map<String, Integer> otuCounts = new TreeMap<>();

		final Map<String, String> map = otuNodes.iterator().next().delimToLevelMap();

		for( final OtuNode otuNode: otuNodes )
		{
			final StringBuffer otu = new StringBuffer();
			for( final String level: map.keySet() )
			{
				final String name = otuNode.getTaxaMap().get( level );
				if( name != null )
				{
					if( !otu.toString().isEmpty() )
					{
						otu.append( OtuUtil.SEPARATOR );
					}

					otu.append( OtuUtil.buildOtuTaxa( map.get( level ), BioLockJUtil.removeQuotes( name ) ) );
				}
			}

			if( !otuCounts.keySet().contains( otu.toString() ) )
			{
				otuCounts.put( otu.toString(), 0 );
			}

			otuCounts.put( otu.toString(), otuCounts.get( otu.toString() ) + otuNode.getCount() );

		}

		otuNodes = null;
		otuNodes = new HashSet<>();

		return otuCounts;
	}

	/**
	 * Getter for sampleId.
	 *
	 * @return Sample ID.
	 */
	public String getSampleId()
	{
		return sampleId;
	}

	private Set<OtuNode> otuNodes = new HashSet<>();

	private final String sampleId;

	private static final long serialVersionUID = 4882054401193953055L;
}
