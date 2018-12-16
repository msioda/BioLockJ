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

import java.io.*;
import java.util.*;
import biolockj.Config;
import biolockj.Log;

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
	
	/**
	 * Get the streamlined taxonomy tree with counts, each OTU listed only 1 time with num occurrences in the sample.<br>
	 * Example: d__Bacteria;p__Bacteroidetes;c__Bacteroidia;o__Bacteroidales;f__Bacteroidaceae;g__Bacteroides;s__Bacteroides_vulgatus 87342
	 * @return map OTU-count
	 * @throws Exception if errors occur
	 */
	public Map<String, Integer> getTreeCounts() throws Exception
	{
		if( otuNodes.isEmpty() )
		{
			Log.warn( getClass(), "No samples found for " + sampleId );
			return null;
		}
		
		final Map<String, Integer> counts = new TreeMap<>();

		final Map<String, String> map = getDelimMap( otuNodes.iterator().next() );
		
		for( final OtuNode otuNode: otuNodes )
		{
			final StringBuffer otu = new StringBuffer();
			for( final String level: map.keySet() )
			{
				final String name = otuNode.getOtuMap().get( level );
				if( name != null )
				{
					if( !otu.toString().isEmpty() )
					{
						otu.append( SEPARATOR );
					}
							
					otu.append( map.get( level ) ).append( name );
				}
			}
			
			if( counts.get( otu.toString() ) == null )
			{
				counts.put( otu.toString(), 0 );
			}
			
			counts.put( otu.toString() , counts.get( otu.toString() ) + otuNode.getCount() );

		}

		otuNodes = null;
		otuNodes = new HashSet<>();
		
		return counts;
	}


	

	private Map<String, String> delimMap = null;
	
	private Map<String, String> getDelimMap( OtuNode node ) throws Exception
	{
		if( delimMap == null )
		{
			delimMap = new HashMap<>();
			Map<String, String> map = node.delimToLevelMap();
			for( final String level: map.keySet() )
			{
				if( map.get( level ).equals( Config.DOMAIN ) )
				{
					delimMap.put( level, DOMAIN_DELIM );
				}
				else if( map.get( level ).equals( Config.PHYLUM ) )
				{
					delimMap.put( level, PHYLUM_DELIM );
				}
				else if( map.get( level ).equals( Config.CLASS ) )
				{
					delimMap.put( level, CLASS_DELIM );
				}
				else if( map.get( level ).equals( Config.ORDER ) )
				{
					delimMap.put( level, ORDER_DELIM );
				}
				else if( map.get( level ).equals( Config.FAMILY ) )
				{
					delimMap.put( level, FAMILY_DELIM );
				}
				else if( map.get( level ).equals( Config.GENUS ) )
				{
					delimMap.put( level, GENUS_DELIM );
				}
				else if( map.get( level ).equals( Config.SPECIES ) )
				{
					delimMap.put( level, SPECIES_DELIM );
				}
			}
		}
		return delimMap;
	}

	
	@Override
	public int compareTo( final ParsedSample o )
	{
		return o.getSampleId().compareTo( getSampleId() );
	}

	/**
	 * Getter for otuCountMap, a nested TreeMap with outer key = level, inner key = OTU, inner value = count.
	 *
	 * @return OTU count map
	 */
	public TreeMap<String, TreeMap<String, Long>> getOtuCountMap()
	{
		return otuCountMap;
	}

	/**
	 * Return all otuNodes
	 * 
	 * @return OTU nodes
	 */
	public Set<OtuNode> getOtuNodes()
	{
		return otuNodes;
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

	/**
	 * Print OTU counts at every level to the log file.
	 * 
	 * @param countsOnly Set true to print level counts instead of OTU counts.
	 */
	public void report()
	{
		try
		{
			String val = "Parsed [" + sampleId + "] ==> ";

			for( final String level: otuCountMap.keySet() )
			{
				final String otuCountMsg = level + " #OTUs: " + otuCountMap.get( level ).size();

				val += otuCountMsg + " | ";
				Log.debug( getClass(), Log.LOG_SPACER );
				Log.debug( getClass(), otuCountMsg );
				for( final String otu: otuCountMap.get( level ).keySet() )
				{
					Log.debug( getClass(), otu + " = " + otuCountMap.get( level ).get( otu ) );
				}

			}

			Log.debug( getClass(), Log.LOG_SPACER );
			Log.info( getClass(), val );
		}
		catch( final Exception ex )
		{
			Log.error( getClass(), "Unable to report ParsedSample! " + ex.getMessage(), ex );
		}
	}
	
	private static String SEPARATOR = ";";


	private static String DOMAIN_DELIM = "d__";

	private static String PHYLUM_DELIM = "p__";

	private static String CLASS_DELIM = "c__";

	private static String ORDER_DELIM = "o__";

	private static String FAMILY_DELIM = "f__";

	private static String GENUS_DELIM = "g__";

	private static String SPECIES_DELIM = "s__";

	// key=level
	private final TreeMap<String, TreeMap<String, Long>> otuCountMap = new TreeMap<>();
	private Set<OtuNode> otuNodes = new HashSet<>();
	private final String sampleId;
	private static final long serialVersionUID = 4882054401193953055L;
}
