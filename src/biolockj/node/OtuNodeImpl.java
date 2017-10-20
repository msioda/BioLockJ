/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 2, 2017
 * @disclaimer 	This code is free software; you can redistribute it and/or
 * 				modify it under the terms of the GNU General Public License
 * 				as published by the Free Software Foundation; either version 2
 * 				of the License, or (at your option) any later version,
 * 				provided that any use properly credits the author.
 * 				This program is distributed in the hope that it will be useful,
 * 				but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * 				GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.node;

import java.util.HashMap;
import java.util.Map;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;

/**
 * OtuNode holds taxonomy info representing one line of output from the classifier output file.
 */
public abstract class OtuNodeImpl implements OtuNode
{
	/**
	 * Add OTU for a taxonomy level.
	 *
	 * @param String levelDelim
	 * @param String otuName
	 */
	@Override
	public void addOtu( final String levelDelim, final String otuName )
	{
		if( ( levelDelim != null ) && ( otuName != null ) && !levelDelim.trim().isEmpty() && !otuName.trim().isEmpty() )
		{
			final String taxaLevel = delimToLevelMap().get( levelDelim );
			try
			{
				// if taxaLevel not configured or for level BioLockJ doesn't support (like strain) - take no action
				if( ( taxaLevel == null ) || !Config.requireTaxonomy().contains( taxaLevel ) )
				{
					Log.out.debug(
							"OtuNode.addOtu() skipping invalid taxaLevel (not configured or not supported): delim="
									+ levelDelim + "; taxa=" + taxaLevel );
					return;
				}
			}
			catch( final Exception ex )
			{
				Log.out.error( "Unable to get configured taxonomy! " + ex.getMessage(), ex );
			}

			if( otuMap.get( taxaLevel ) != null )
			{
				Log.out.warn( "OtuNode.addOtu() " + id + " overwriting OTU: " + otuMap.get( taxaLevel ) + " with "
						+ otuName + "  --> Line = " + line );
			}

			otuMap.put( taxaLevel, otuName );
		}
		else
		{
			Log.out.warn( "OtuNode.addOtu() " + id + " called with null params! --> Line = " + line );
		}
	}

	/**
	 * Get the OTU count.
	 *
	 * @return Long count
	 */
	@Override
	public Long getCount()
	{
		return count;
	}

	/**
	 * Get the line from classifier output file used to create the OtuNode.
	 *
	 * @return String classifier output file line
	 */
	@Override
	public String getLine()
	{
		return line;
	}

	@Override
	public Map<String, String> getOtuMap()
	{
		return otuMap;
	}

	/**
	 * Get the Sample ID
	 *
	 * @return String id
	 */
	@Override
	public String getSampleId()
	{
		return id;
	}

	@Override
	public void report()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getClass().getSimpleName() + "[" + id + "]=" + count + " | line=" + line + BioLockJ.RETURN );
		for( final String key: otuMap.keySet() )
		{
			sb.append( key + " = " + otuMap.get( key ) + BioLockJ.RETURN );
		}
		Log.out.warn( sb.toString() );
	}

	/**
	 * Set the OTU count
	 *
	 * @param Long count
	 */
	@Override
	public void setCount( final Long count )
	{
		this.count = count;
	}

	/**
	 * Set line from classifier output file used to create the OtuNode.
	 *
	 * @param String line
	 */
	@Override
	public void setLine( final String line )
	{
		this.line = line;
	}

	/**
	 * Set the Sample ID.
	 *
	 * @param String id
	 */
	@Override
	public void setSampleId( final String id )
	{
		this.id = id;
	}

	private static Map<String, String> delimToLevelMap()
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

	private Long count = 0L;
	private String id = null;
	private String line = null;
	private final Map<String, String> otuMap = new HashMap<>();
	protected static String CLASS_DELIM = "c__";
	protected static String DOMAIN_DELIM = "d__";
	protected static String FAMILY_DELIM = "f__";
	protected static String GENUS_DELIM = "g__";
	protected static String ORDER_DELIM = "o__";
	protected static String PHYLUM_DELIM = "p__";

	protected static String SPECIES_DELIM = "s__";

	private static final Map<String, String> delimToLevelMap = new HashMap<>();

	//private static final Map<String, String> levelToDelimMap = new HashMap<>();
	//static
	//{
	//		levelToDelimMap.put( Config.DOMAIN, DOMAIN_DELIM );
	//		levelToDelimMap.put( Config.PHYLUM, PHYLUM_DELIM );
	//		levelToDelimMap.put( Config.CLASS, CLASS_DELIM );
	//		levelToDelimMap.put( Config.ORDER, ORDER_DELIM );
	//		levelToDelimMap.put( Config.FAMILY, FAMILY_DELIM );
	//		levelToDelimMap.put( Config.GENUS, GENUS_DELIM );
	//		levelToDelimMap.put( Config.SPECIES, SPECIES_DELIM );
	//}
}