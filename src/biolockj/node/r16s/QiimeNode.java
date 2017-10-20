/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
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
package biolockj.node.r16s;

import java.util.StringTokenizer;
import biolockj.node.OtuNode;
import biolockj.node.OtuNodeImpl;

/**
 * To see file format example: head otu_table_L2.txt
 *
 * # Constructed from biom file
 * #OTU ID 3A.1 6A.1 120A.1 7A.1
 * k__Bacteria;p__Actinobacteria 419.0 26.0 90.0 70.0
 *
 */
public class QiimeNode extends OtuNodeImpl implements OtuNode
{
	/**
	 * Build the OtuNode by extracting the OTU names for each level from the line.
	 *
	 * @param String id extracted from header line in the classifier output file
	 * @param String taxas extracted from the classifier output file
	 * @param Long count
	 * @throws Exception if propagated from addOtu() method
	 */
	public QiimeNode( final String id, final String line, final Long count ) throws Exception
	{
		setLine( id + "_" + line + "_" + count );
		setSampleId( id );
		setCount( count );
		final StringTokenizer taxas = new StringTokenizer( line, QIIME_DELIM );
		while( taxas.hasMoreTokens() )
		{
			final String token = taxas.nextToken();
			final String level = token.substring( 0, 3 );
			final String taxa = token.substring( 3 );
			addOtu( level, taxa );
		}
	}

	private static final String QIIME_DELIM = ";";

	static
	{
		DOMAIN_DELIM = "k__";
	}
}