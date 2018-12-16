/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.node.r16s;

import java.util.StringTokenizer;
import biolockj.node.OtuNode;
import biolockj.node.OtuNodeImpl;

/**
 * This class holds the OTU assignments from one line of {@link biolockj.module.implicit.qiime.QiimeClassifier} output.
 * <p>
 * 2 sample lines (header + 1st row) from otu_table_L*.txt output by QIIME script:
 * {@value biolockj.module.implicit.qiime.QiimeClassifier#SCRIPT_SUMMARIZE_TAXA} <br>
 * # Constructed from biom file #OTU ID 3A.1 7A.1 120A.1 <br>
 * 7A.1 k__Bacteria;p__Actinobacteria 419.0 26.0 90.0 70.0
 */
public class QiimeNode extends OtuNodeImpl implements OtuNode
{
	/**
	 * Build the OtuNode by extracting the OTU names for each level from the line.
	 *
	 * @param id Sample ID extracted from count column header
	 * @param taxas OTU names with level indicator prefix
	 * @param count Extracted from column of the Sample ID
	 * @throws Exception if propagated from addOtu() method
	 */
	public QiimeNode( final String id, final String taxas, final int count ) throws Exception
	{
		setLine( id + "_" + taxas + "_" + count );
		setSampleId( id );
		setCount( count );
		final StringTokenizer st = new StringTokenizer( taxas, QIIME_DELIM );
		String taxa = st.nextToken(); // skip domain

		while( st.hasMoreTokens() )
		{
			taxa = st.nextToken();
			final String levelDelim = getLevelDelim( taxa );
			final String otu = taxa.substring( levelDelimSize );
			buildOtuNode( otu, levelDelim );
		}
	}

	/**
	 * Get the first taxonomy level delim.
	 * 
	 * @param taxa Full classifier taxonomy string
	 * @return Level delim
	 * @throws Exception if the taxa delim is undefined
	 */
	protected String getLevelDelim( final String taxa ) throws Exception
	{
		if( taxa.startsWith( SILVA_AMBIGUOUS_DELIM ) )
		{
			return null;
		}
		else if( !setLevelDelims )
		{

			if( delimToLevelMap().get( taxa.substring( 0, levelDelimSize ) ) == null )
			{
				delimToLevelMap.clear();
				levelDelimSize = 5;
				DOMAIN_DELIM = SILVA_DOMAIN_DELIM;
				PHYLUM_DELIM = SILVA_PHYLUM_DELIM;
				CLASS_DELIM = SILVA_CLASS_DELIM;
				ORDER_DELIM = SILVA_ORDER_DELIM;
				FAMILY_DELIM = SILVA_FAMILY_DELIM;
				GENUS_DELIM = SILVA_GENUS_DELIM;
				SPECIES_DELIM = SILVA_SPECIES_DELIM;

				if( delimToLevelMap().get( taxa.substring( 0, levelDelimSize ) ) == null )
				{
					throw new Exception( "Taxa delim is undefined for taxas: " + getSampleId() + ": " + taxa );
				}

			}
			setLevelDelims = true;
		}

		return taxa.substring( 0, levelDelimSize );
	}

	/**
	 * QIIME domain taxonomy level delimiter: {@value #QIIME_DOMAIN_DELIM}
	 */
	protected static final String QIIME_DOMAIN_DELIM = "k__";

	/**
	 * Silva ambiguous OTU delim: {@value #SILVA_AMBIGUOUS_DELIM}
	 */
	protected static final String SILVA_AMBIGUOUS_DELIM = "Ambiguous_taxa";

	/**
	 * Silva class taxonomy level delimiter: {@value #SILVA_CLASS_DELIM}
	 */
	protected static final String SILVA_CLASS_DELIM = "D_2__";

	/**
	 * Silva domain taxonomy level delimiter: {@value #SILVA_DOMAIN_DELIM}
	 */
	protected static final String SILVA_DOMAIN_DELIM = "D_0__";

	/**
	 * Silva family taxonomy level delimiter: {@value #SILVA_FAMILY_DELIM}
	 */
	protected static final String SILVA_FAMILY_DELIM = "D_4__";

	/**
	 * Silva genus taxonomy level delimiter: {@value #SILVA_GENUS_DELIM}
	 */
	protected static final String SILVA_GENUS_DELIM = "D_5__";

	/**
	 * Silva order taxonomy level delimiter: {@value #SILVA_ORDER_DELIM}
	 */
	protected static final String SILVA_ORDER_DELIM = "D_3__";

	/**
	 * Silva phylum taxonomy level delimiter: {@value #SILVA_PHYLUM_DELIM}
	 */
	protected static final String SILVA_PHYLUM_DELIM = "D_1__";

	/**
	 * Silva species taxonomy level delimiter: {@value #SILVA_SPECIES_DELIM}
	 */
	protected static final String SILVA_SPECIES_DELIM = "D_6__";

	private static int levelDelimSize = 3;
	private static final String QIIME_DELIM = ";";
	private static boolean setLevelDelims = false;

	// Override default DOMAIN taxonomy level delimiter (d__) set in OtuNodeImpl with QIIME domain delim (k__)
	static
	{
		DOMAIN_DELIM = QIIME_DOMAIN_DELIM;
	}
}
