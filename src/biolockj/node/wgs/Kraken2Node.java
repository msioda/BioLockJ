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
package biolockj.node.wgs;

import java.util.StringTokenizer;
import biolockj.node.OtuNode;
import biolockj.node.OtuNodeImpl;

/**
 * This class represents one line of {@link biolockj.module.classifier.wgs.Kraken2Classifier} output. Each line can
 * represent multiple reads since the estimated_number_of_reads_from_the_clade column is used to approximate the number
 * of reads with the given OTU in the sample.
 * <p>
 * Sample Kraken report line (head 7A_1_reported.tsv) :<br>
 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales 20094
 */
public class Kraken2Node extends OtuNodeImpl implements OtuNode
{
	/**
	 * Build the OtuNode by extracting the OTU names for each level from the line. Sample ID is passed as a parameter
	 * pulled from the file name.
	 *
	 * @param id Sample ID
	 * @param line Kraken2 output line
	 * @throws Exception if an invalid line format is found
	 */
	public Kraken2Node( final String id, final String line ) throws Exception
	{
		final String[] parts = line.split( "\\t" );
		if( parts.length != 2 )
		{
			throw new Exception( "INVALID FILE FORMAT.  Line should have 2 parts.  LINE =  (" + line + ") " );
		}

		try
		{
			setSampleId( id );
			setLine( line );
			setCount( Integer.valueOf( parts[ 1 ] ) );

			String levelDelim = null;
			String taxa = null;
			final StringTokenizer taxas = new StringTokenizer( parts[ 0 ], TAXA_DELIM );
			while( taxas.hasMoreTokens() )
			{
				final String token = taxas.nextToken();
				levelDelim = token.substring( 0, 3 );

				if( !token.substring( 3 ).trim().isEmpty() )
				{
					taxa = token.substring( 3 );
				}
			}

			buildOtuNode( taxa, levelDelim );

		}
		catch( final Exception ex )
		{
			throw new Exception( "Error parsing Sample ID:" + id + "> line: " + line + ": " + ex.getMessage() );
		}
	}

	private static final String TAXA_DELIM = "\\|";

}
