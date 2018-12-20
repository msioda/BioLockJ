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

import java.util.*;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.util.OtuUtil;

/**
 * The default implementation of {@link biolockj.node.OtuNode} is also the superclass for all WGS and 16S OtuNode
 * classes. OtuNodes hold taxonomy assignment info, represents one line of
 * {@link biolockj.module.classifier.ClassifierModule} output. The assignment is stored in a local taxaMap, which holds
 * one OTU name per level (level-gaps allowed).
 */
public abstract class OtuNodeImpl implements OtuNode
{

	@Override
	public void addTaxa( final String taxa, final String levelDelim )
	{
		// Log.debug( getClass(), id + " addTaxa --> Taxa: " + otu + " : delim = " + levelDelim );
		if( levelDelim != null && taxa != null && !levelDelim.trim().isEmpty() && !taxa.trim().isEmpty() )
		{
			final String taxaLevel = delimToLevelMap().get( levelDelim );

			// if taxaLevel not configured or for level BioLockJ doesn't support (like strain) - take no action
			if( taxaLevel == null || !Config.getList( Config.REPORT_TAXONOMY_LEVELS ).contains( taxaLevel ) )
			{
				return;
			}

			if( taxaMap.get( taxaLevel ) != null )
			{
				Log.debug( getClass(), id + " overwriting OTU: " + taxaMap.get( taxaLevel ) + " with " + taxa
						+ "  --> Line = " + line );
			}

			taxaMap.put( taxaLevel, taxa );
		}
		else
		{
			Log.debug( getClass(), "ID=[ " + id + " ] --> Taxa level missing! for: levelDelim=[ " + levelDelim
					+ " ]; Taxa=[ " + taxa + " ]; Line =[ " + line + " ]" );
		}
	}

	@Override
	public Map<String, String> delimToLevelMap()
	{
		if( delimToLevelMap.isEmpty() )
		{
			delimToLevelMap.put( DOMAIN_DELIM, Config.DOMAIN );
			delimToLevelMap.put( PHYLUM_DELIM, Config.PHYLUM );
			delimToLevelMap.put( CLASS_DELIM, Config.CLASS );
			delimToLevelMap.put( ORDER_DELIM, Config.ORDER );
			delimToLevelMap.put( FAMILY_DELIM, Config.FAMILY );
			delimToLevelMap.put( GENUS_DELIM, Config.GENUS );
			delimToLevelMap.put( SPECIES_DELIM, Config.SPECIES );
		}
		return delimToLevelMap;
	}

	@Override
	public Integer getCount()
	{
		return count;
	}

	@Override
	public String getLine()
	{
		return line;
	}

	@Override
	public String getSampleId()
	{
		return id;
	}

	/**
	 * This implementation ensures all levels between top and bottom taxonomy levels are complete. If missing middle
	 * levels are found, they inherit their parent taxa.
	 */
	@Override
	public Map<String, String> getTaxaMap() throws Exception
	{
		String parentTaxa = null;
		final Set<String> populatedLevels = new HashSet<>( taxaMap.keySet() );
		for( final String level: OtuUtil.getAllTaxonomyLevels() )
		{
			if( populatedLevels.contains( level ) )
			{
				populatedLevels.remove( level );
			}

			final String levelTaxa = taxaMap.get( level );
			if( levelTaxa == null && parentTaxa != null && !populatedLevels.isEmpty() )
			{
				taxaMap.put( level, parentTaxa );
			}
			if( levelTaxa != null )
			{
				parentTaxa = levelTaxa;
			}
		}

		return taxaMap;
	}

	@Override
	public void report() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( "[" + id + "]=" + count + " | line=" + line + BioLockJ.RETURN );
		for( final String key: getTaxaMap().keySet() )
		{
			sb.append( key + " = " + getTaxaMap().get( key ) + BioLockJ.RETURN );
		}
		Log.debug( getClass(), sb.toString() );
	}

	@Override
	public void setCount( final int count )
	{
		this.count = count;
	}

	@Override
	public void setLine( final String line )
	{
		try
		{
			if( Log.doDebug() )
			{
				this.line = line;
			}
		}
		catch( final Exception ex )
		{
			ex.printStackTrace();
		}
	}

	@Override
	public void setSampleId( final String id )
	{
		this.id = id;
	}

	private int count = 0;
	private String id = null;
	private String line = "";

	// key=level, val=otu
	private final Map<String, String> taxaMap = new HashMap<>();

	/**
	 * Standard classifier output level delimiter for CLASS
	 */
	protected static String CLASS_DELIM = "c__";

	/**
	 * Map level delim to full taxonomy level name.
	 */
	protected static final Map<String, String> delimToLevelMap = new HashMap<>();

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
}
