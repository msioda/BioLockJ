/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 20, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.node;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.commons.lang.builder.EqualsBuilder;
import biolockj.Log;

/**
 * Each JsonNode holds an OTU, count, and link to its parent node.
 */
public class JsonNode implements Serializable, Comparable<JsonNode>
{
	/**
	 * JsonNode Constructor
	 * 
	 * @param taxa Taxa name
	 * @param count Taxa count
	 * @param parent Taxa parent
	 * @param level Taxa level
	 */
	public JsonNode( final String taxa, final Integer count, final JsonNode parent, final String level )
	{
		this.taxa = taxa;
		this.count = count;
		this.parent = parent;
		this.level = level;
	}

	/**
	 * Add taxa count
	 * 
	 * @param count Taxa count
	 */
	public void addCount( final Integer count )
	{
		this.count += count;
	}

	@Override
	public int compareTo( final JsonNode node )
	{
		return taxa.compareTo( node.taxa );
	}

	@Override
	public boolean equals( final Object o )
	{
		if( o != null && o instanceof JsonNode )
		{
			if( o == this )
			{
				return true;
			}
			if( ( (JsonNode) o ).parent != null )
			{
				if( parent == null )
				{
					return false;
				}
				return new EqualsBuilder().append( taxa, ( (JsonNode) o ).taxa )
						.append( parent, ( (JsonNode) o ).parent ).isEquals();
			}
			return new EqualsBuilder().append( taxa, ( (JsonNode) o ).taxa ).isEquals();
		}

		return false;
	}

	/**
	 * Getter method for count
	 * 
	 * @return OTU count
	 */
	public Integer getCount()
	{
		return count;
	}

	/**
	 * Getter method for level
	 * 
	 * @return JsonNode level
	 */
	public String getLevel()
	{
		return level;
	}

	/**
	 * Getter method for parent
	 * 
	 * @return JsonNode parent
	 */
	public JsonNode getParent()
	{
		return parent;
	}

	/**
	 * Getter method for stats
	 * 
	 * @return Map of OTU stats
	 */
	public HashMap<String, Double> getStats()
	{
		return stats;
	}

	/**
	 * Getter method for taxa
	 * 
	 * @return Taxa Name
	 */
	public String getTaxa()
	{
		return taxa;
	}

	@Override
	public int hashCode()
	{
		if( parent == null )
		{
			return taxa.hashCode();
		}
		return ( taxa + parent ).hashCode();
	}

	/**
	 * Print node info.
	 */
	public void report()
	{
		Log.info( getClass(), "Report JsonNode[ " + level + ":" + taxa + ":" + count + " ]" );
	}

	/**
	 * Setter method for level
	 * 
	 * @param level Taxonomy level
	 */
	public void setLevel( final String level )
	{
		this.level = level;
	}

	/**
	 * Setter method for parent
	 * 
	 * @param parent Node parent
	 */
	public void setParent( final JsonNode parent )
	{
		this.parent = parent;
	}

	/**
	 * Setter method for taxa
	 * 
	 * @param taxa Taxa name
	 */
	public void setTaxa( final String taxa )
	{
		this.taxa = taxa;
	}

	/**
	 * Update stats, add val to key name.
	 * 
	 * @param name Statistic name
	 * @param val Statistic value
	 */
	public void updateStats( final String name, final Double val )
	{
		if( !stats.keySet().contains( name ) )
		{
			stats.put( name, 0D );
		}

		stats.put( name, stats.get( name ) + val );
	}

	private Integer count = 0;
	private String level;
	private JsonNode parent;
	private final HashMap<String, Double> stats = new LinkedHashMap<>();
	private String taxa;
	private static final long serialVersionUID = 7967794387383764650L;
}
