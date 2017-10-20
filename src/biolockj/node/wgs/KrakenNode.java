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
package biolockj.node.wgs;

import java.util.StringTokenizer;
import biolockj.BioLockJ;
import biolockj.Log;
import biolockj.node.OtuNode;
import biolockj.node.OtuNodeImpl;
import biolockj.util.SeqUtil;

/**
 * To see file format example: head 7A_1_reported.tsv
 *
 * FCC6MMAACXX:8:1101:1968:2100#GTATTCTC/1
 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales|f__Bacteroidaceae|g__Bacteroides|s__Bacteroides_vulgatus
 *
 */
public class KrakenNode extends OtuNodeImpl implements OtuNode
{

	/**
	 * Constructor used by multiplexed files.  Sample ID will be extracted from each
	 * sequence header in buildNode() method.
	 *
	 * @param String line from classifier output file
	 * @throws Exception propagated from buildNode
	 */
	public KrakenNode( final String line ) throws Exception
	{
		buildNode( null, line );
	}

	/**
	 * Constructor used by multiplexed files.  Sample ID will be extracted from each
	 * sequence header in buildNode() method.
	 *
	 * @param String id is the Sample ID
	 * @param String line from classifier output file
	 * @throws Exception propagated from buildNode
	 */
	public KrakenNode( final String id, final String line ) throws Exception
	{
		buildNode( id, line );
	}

	/**
	 * Build the OtuNode by extracting the OTU names for each level from the line.
	 *
	 * @param String id is the Sample ID
	 * @param String line from classifier output file
	 * @throws Exception for invalid line format
	 */
	private void buildNode( final String id, final String line ) throws Exception
	{
		final StringTokenizer st = new StringTokenizer( line, BioLockJ.TAB_DELIM );
		if( st.countTokens() == 2 )
		{
			setLine( line );
			setCount( 1L );

			final String lineId = st.nextToken();
			if( id == null )
			{
				setSampleId( SeqUtil.getSampleId( lineId ) );
			}
			else
			{
				setSampleId( id );
			}

			final StringTokenizer taxas = new StringTokenizer( st.nextToken(), KRAKEN_DELIM );
			while( taxas.hasMoreTokens() )
			{
				final String token = taxas.nextToken();
				final String levelDelim = token.substring( 0, 3 );
				final String taxa = token.substring( 3 );
				addOtu( levelDelim, taxa );
			}
		}
		else
		{
			while( st.hasMoreTokens() )
			{
				Log.out.warn( "Kraken token [ should only have 2! ]: " + st.nextToken() );
			}

			throw new Exception( "Invalid Record = (" + line + ")" + BioLockJ.RETURN
					+ "Kraken output must have exactly 2 tab delimited columns per line. " );
		}
	}

	private static final String KRAKEN_DELIM = "\\|";
}