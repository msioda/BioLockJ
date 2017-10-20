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

import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.module.classifier.wgs.SlimmClassifier;
import biolockj.node.OtuNode;
import biolockj.node.OtuNodeImpl;

/**
 * To see file format example: head 7A_1_phylum_reported.tsv
 *
 * No. Name Taxid NoOfReads RelativeAbundance Contributers Coverage
 * 1 Bacteroidetes 976 1137994 29.7589 17 24.7204
 */
public class SlimmNode extends OtuNodeImpl implements OtuNode
{

	/**
	 * SLIMM splits OTU reports into separate files, so we do not have the full taxonomy path.
	 *
	 * @param String fileName of from the classifier output file
	 * @param String line from the classifier output file
	 * @throws Exception if level not found
	 */
	public SlimmNode( final String fileName, final String line ) throws Exception
	{

		final String[] parts = line.split( BioLockJ.TAB_DELIM );
		final String taxa = parts[ 1 ];
		final String level = getLevel( fileName );
		if( level != null ) // will skip unsupported levels such as superkingdom
		{
			setLine( line );
			setSampleId( fileName.substring( 0, fileName.indexOf( level ) - 1 ) );
			setCount( Long.valueOf( parts[ 3 ] ) );
			addOtu( level, taxa );
		}
	}

	private static String getLevel( final String fileName ) throws Exception
	{
		for( final String level: Config.requireTaxonomy() )
		{
			if( fileName.contains( level ) )
			{
				return level;
			}
		}
		return null;
	}

	static
	{
		DOMAIN_DELIM = SlimmClassifier.SLIMM_DOMAIN_DELIM;
		CLASS_DELIM = Config.CLASS;
		FAMILY_DELIM = Config.FAMILY;
		GENUS_DELIM = Config.GENUS;
		ORDER_DELIM = Config.ORDER;
		PHYLUM_DELIM = Config.PHYLUM;
		SPECIES_DELIM = Config.SPECIES;
	}
}