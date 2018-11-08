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
	 * Build OTU counts after all otuNodes have been built.
	 */
	public void buildOtuCounts()
	{
		if( otuNodes.isEmpty() )
		{
			return;
		}

		for( final OtuNode otuNode: otuNodes )
		{
			final Map<String, String> map = otuNode.getOtuMap();
			for( final String level: map.keySet() )
			{
				final String otu = map.get( level );

				if( otuCountMap.get( level ) == null )
				{
					otuCountMap.put( level, new TreeMap<String, Long>() );
				}

				if( otuCountMap.get( level ).get( otu ) == null )
				{
					otuCountMap.get( level ).put( otu, 0L );
				}

				otuCountMap.get( level ).put( otu, otuCountMap.get( level ).get( otu ) + otuNode.getCount() );
			}
		}

		otuNodes.clear();
	}

	@Override
	public int compareTo( final ParsedSample o )
	{
		return o.getSampleId().compareTo( getSampleId() );
	}

	/**
	 * Return total OTU count for all OTUs assigned at the highest taxonomy level found in the sample.
	 *
	 * @return Total OTU counts at top taxonomy level found in sample
	 */
	public Long getNumHits()
	{
		long total = 0L;
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			final TreeMap<String, Long> topLevelMap = otuCountMap.get( level );
			if( topLevelMap != null )
			{
				for( final Long count: topLevelMap.values() )
				{
					total += count;
				}
				return total;
			}
		}

		Log.warn( getClass(), "ParsedSample has no data: " + sampleId + " has 0 hits!" );
		return 0L;
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
	 */
	public void report()
	{
		try
		{
			Log.info( getClass(), Log.LOG_SPACER );
			Log.info( getClass(), "PARSED SAMPLE REPORT" );
			Log.info( getClass(), Log.LOG_SPACER );
			Log.info( getClass(), "Sample ID = " + sampleId );
			for( final String level: otuCountMap.keySet() )
			{
				Log.get( getClass() ).info( "OTUs at Level = " + level );
				for( final String otu: otuCountMap.get( level ).keySet() )
				{
					Log.info( getClass(), otu + " = " + otuCountMap.get( level ).get( otu ) );
				}
			}
		}
		catch( final Exception ex )
		{
			Log.error( getClass(), "Unable to report ParsedSample! " + ex.getMessage(), ex );
		}

		Log.info( getClass(), Log.LOG_SPACER );
	}

	// key=level
	private final TreeMap<String, TreeMap<String, Long>> otuCountMap = new TreeMap<>();
	private final Set<OtuNode> otuNodes = new HashSet<>();
	private final String sampleId;
	private static final long serialVersionUID = 4882054401193953055L;
}
