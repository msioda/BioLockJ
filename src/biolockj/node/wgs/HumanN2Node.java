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
 * This class represents one line of {@link biolockj.module.classifier.wgs.HumanN2Classifier} output.<br>
 * 1st cell format: [Pathway_ID]:[Pathway_Descr] | g__[genus_taxa].s__[species_taxa]<br>
 * Example: ARO-PWY: chorismate biosynthesis I|g__Acidaminococcus.s__Acidaminococcus_intestini Samples IDs are found in
 * the column headers starting with the 2nd column.<br>
 * The count type depends on the HumanN2 config properties.
 */
public class HumanN2Node extends OtuNodeImpl implements OtuNode
{
	/**
	 * Build the OtuNode by extracting the OTU names for each level from the line. Sample ID is passed as a parameter
	 * pulled from the file name.
	 *
	 * @param id Sample ID
	 * @param line Kraken2 output line
	 * @throws Exception if an invalid line format is found
	 */
	public HumanN2Node( final String id, final String line ) throws Exception
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

			final StringTokenizer taxas = new StringTokenizer( parts[ 0 ], TAXA_DELIM );
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

	private static final String TAXA_DELIM = "\\|";

}
