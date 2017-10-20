package biolockj.module.parser;

import java.util.Iterator;
import java.util.TreeMap;
import biolockj.Config;
import biolockj.Log;
import biolockj.node.OtuNode;

public class ParsedSample
{
	/**
	 * Construct a ParsedSample with it's 1st OtuNode.
	 *
	 * @param OtuNode node
	 * @throws Exception
	 */
	public ParsedSample( final OtuNode node ) throws Exception
	{
		sampleId = node.getSampleId();
		addNode( node );
	}

	public void addNode( final OtuNode node ) throws Exception
	{
		//node.report();

		for( final String level: node.getOtuMap().keySet() )
		{
			addOtu( level, node.getOtuMap().get( level ), node.getCount() );
		}
	}

	public void addOtu( final String level, final String otu, final Long count ) throws Exception
	{

		if( otuCountMap.get( level ) == null )
		{
			otuCountMap.put( level, new TreeMap<String, Long>() );
		}

		if( otuCountMap.get( level ).get( otu ) == null )
		{
			otuCountMap.get( level ).put( otu, 0L );
		}

		otuCountMap.get( level ).put( otu, ( otuCountMap.get( level ).get( otu ) + count ) );
	}

	public Long getNumHits() throws Exception
	{
		long total = 0L;

		final Iterator<String> it = Config.requireTaxonomy().iterator();
		while( it.hasNext() )
		{
			final TreeMap<String, Long> topLevelMap = otuCountMap.get( it.next() );
			if( topLevelMap != null )
			{
				for( final Long count: topLevelMap.values() )
				{
					total += count;
				}
				return total;
			}
		}

		Log.out.warn( "ParsedSample has no data: " + sampleId + " has 0 hits!" );
		return 0L;
	}

	public TreeMap<String, TreeMap<String, Long>> getOtuCountMap()
	{
		return otuCountMap;
	}

	/**
	 * Get the Sample ID.
	 *
	 * @return String Sample ID.
	 */
	public String getSampleId()
	{
		return sampleId;
	}

	// key = level
	// value = Map<outName, count>
	private final TreeMap<String, TreeMap<String, Long>> otuCountMap = new TreeMap<>();
	private final String sampleId;
}
