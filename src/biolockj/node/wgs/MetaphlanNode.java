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
 * This class represents one line of {@link biolockj.module.classifier.wgs.MetaphlanClassifier} output. For a full
 * description of the output file format
 * <a href= "https://bitbucket.org/biobakery/biobakery/wiki/metaphlan2#rst-header-output-files" target=
 * "_top">https://bitbucket.org/biobakery/biobakery/wiki/metaphlan2#rst-header-output-files</a> Each line can represent
 * multiple reads since the estimated_number_of_reads_from_the_clade column is used to approximate the number of reads
 * with the given OTU in the sample.
 * <p>
 * 3 sample lines of Metaphlan output (2 header rows + 1st row): <br>
 * #SampleID Metaphlan2_Analysis <br>
 * #clade_name relative_abundance coverage average_genome_length_in_the_clade estimated_number_of_reads_from_the_clade
 * <br>
 * k__Bacteria|p__Bacteroidetes 14.68863 0.137144143537 4234739 580770
 */
public class MetaphlanNode extends OtuNodeImpl implements OtuNode
{
	/**
	 * Build the OtuNode by extracting the OTU names for each level from the line. Sample ID is passed as a parameter
	 * pulled from the file name. The number of reads matching the line taxonomy is extracted from the last column
	 * (estimated_number_of_reads_from_the_clade)
	 *
	 * @param id Sample ID
	 * @param line Metaphlan output line
	 * @throws Exception if an invalid line format is found
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
			setCount( Integer.valueOf( parts[ 4 ] ) );

			final StringTokenizer taxas = new StringTokenizer( parts[ 0 ], METAPHLAN_DELIM );
			while( taxas.hasMoreTokens() )
			{
				final String token = taxas.nextToken();
				final String levelDelim = token.substring( 0, 3 );
				final String taxa = token.substring( 3 ).trim();
				if( !taxa.isEmpty() )
				{
					addTaxa( taxa, delimToLevelMap().get( levelDelim ) );
				}
			}
		}
		catch( final Exception ex )
		{
			throw new Exception( "Error parsing Sample ID:" + id + "> line: " + line + ": " + ex.getMessage() );
		}
	}

	private static final String METAPHLAN_DELIM = "\\|";

	// Override default DOMAIN taxonomy level delimiter (d__) set in OtuNodeImpl with QIIME domain delim (k__)
	static
	{
		DOMAIN_DELIM = "k__";
	}
}
