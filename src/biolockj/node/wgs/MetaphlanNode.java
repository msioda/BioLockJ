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
import biolockj.node.OtuNode;
import biolockj.node.OtuNodeImpl;

/**
 * To see file format example: head 7A_1_processed.txt
 *
 * #SampleID Metaphlan2_Analysis
 * #clade_name relative_abundance coverage average_genome_length_in_the_clade estimated_number_of_reads_from_the_clade
 * k__Bacteria|p__Bacteroidetes 14.68863 0.137144143537 4234739 580770
 *
 */
public class MetaphlanNode extends OtuNodeImpl implements OtuNode
{

	/**
	 * Build the OtuNode by extracting the OTU names for each level from the line.
	 *
	 * @param String id of the sample, extracted from the classifier output file name
	 * @param String line from the classifier output file
	 * @throws Exception if the line format is invalid
	 */
	public MetaphlanNode( final String id, final String line ) throws Exception
	{
		final String[] parts = line.split( "\\s" );
		if( parts.length != 5 )
		{
			throw new Exception( "INVALID FILE FORMAT.  Line should have 5 parts.  LINE =  (" + line
					+ ") METAPHLAN CLASSIFICATION NOT RUN WITH SWITCH: -t (ANALYSIS_TYPE) rel_ab_w_read_stats.  Add "
					+ " \"-t rel_ab_w_read_stats\" when calling metaphlan." );
		}

		try
		{
			setSampleId( id );
			setLine( line );
			setCount( Long.valueOf( parts[ 4 ] ) );

			final StringTokenizer taxas = new StringTokenizer( parts[ 0 ], METAPHLAN_DELIM );
			while( taxas.hasMoreTokens() )
			{
				final String token = taxas.nextToken();
				final String level = token.substring( 0, 3 );
				final String taxa = token.substring( 3 );
				addOtu( level, taxa );
			}
		}
		catch( final Exception ex )
		{
			throw new Exception( "Error parsing Sample ID:" + id + "> line: " + line + ": " + ex.getMessage() );
		}
	}

	private static final String METAPHLAN_DELIM = "\\|";

	static
	{
		DOMAIN_DELIM = "k__";
	}
}