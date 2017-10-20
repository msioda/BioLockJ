/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Apr 9, 2017
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
package biolockj.module.parser.wgs;

import java.io.BufferedReader;
import java.io.File;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.parser.ParsedSample;
import biolockj.module.parser.ParserModule;
import biolockj.module.parser.ParserModuleImpl;
import biolockj.node.wgs.MetaphlanNode;
import biolockj.util.ModuleUtil;

/**
 * To see file format: > head 7A_1_processed.txt
 *
 * #SampleID Metaphlan2_Analysis
 * #clade_name relative_abundance coverage average_genome_length_in_the_clade estimated_number_of_reads_from_the_clade
 * k__Bacteria|p__Bacteroidetes 14.68863 0.137144143537 4234739 580770
 *
 */
public class MetaphlanParser extends ParserModuleImpl implements ParserModule
{

	/**
	 * MetaPhlAn doesn't support Config.INPUT_DEMULTIPLEX option
	 *
	 * @Exception thrown if Config.INPUT_DEMULTIPLEX == TRUE
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( Config.getBoolean( Config.INPUT_DEMULTIPLEX ) )
		{
			throw new Exception( "MetaPhlAn doesn't support multiplexed files.  Config must set: "
					+ Config.INPUT_DEMULTIPLEX + "=TRUE" );
		}
	}

	/**
	 * Parse each line of each file to extract lines used to build OtuNodes.  The addOtuNode()
	 * method is used to populate the otuNodes map.  Ignore header lines beginning with # symbol.
	 * Always builds OtuNode with parsed Sample ID from the file name.
	 */
	@Override
	public void parseSamples() throws Exception
	{
		int fileCount = 0;
		for( final File file: getInputFiles() )
		{
			Log.out.info( "PARSE FILE # (" + String.valueOf( fileCount++ ) + ") = " + file.getName() );
			final BufferedReader reader = ModuleUtil.getFileReader( file );

			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( !line.startsWith( "#" ) )
				{
					final MetaphlanNode node = new MetaphlanNode( getFileID( file ), line );
					if( isValid( node ) )
					{
						final ParsedSample sample = getParsedSample( node.getSampleId() );
						if( sample == null )
						{
							addParsedSample( new ParsedSample( node ) );
						}
						else
						{
							sample.addNode( node );
						}
					}
				}
			}
			reader.close();
		}
	}
}
