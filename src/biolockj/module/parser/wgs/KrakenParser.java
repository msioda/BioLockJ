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
package biolockj.module.parser.wgs;

import java.io.BufferedReader;
import java.io.File;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.parser.ParsedSample;
import biolockj.module.parser.ParserModule;
import biolockj.module.parser.ParserModuleImpl;
import biolockj.node.wgs.KrakenNode;
import biolockj.util.ModuleUtil;

/**
 * WGS files can contain 1 million+ lines/file so the OtuNodes used for 16s data
 * require too much memory.  Process individual files as completed to save space.
 *
 *
 *
 * To see file format: > head 7A_1_reported.tsv
 *
 * FCC6MMAACXX:8:1101:1968:2100#GTATTCTC/1
 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales|f__Bacteroidaceae|g__Bacteroides|s__Bacteroides_vulgatus
 *
 */
public class KrakenParser extends ParserModuleImpl implements ParserModule
{

	/**
	 * Kraken nodes may be multiplexed so determine Sample ID based on Config.INPUT_DEMULTIPLEX.
	 * Parse each line of each file, each builds a KrakenNode.
	 * Add the KrakenNode the the ParsedSample for the KrakenNode SampleId.
	 */
	@Override
	public void parseSamples() throws Exception
	{
		final boolean demux = Config.getBoolean( Config.INPUT_DEMULTIPLEX );
		int fileCount = 0;
		for( final File file: getInputFiles() )
		{
			Log.out.info( "PARSE FILE # (" + String.valueOf( fileCount++ ) + ") = " + file.getName() );
			final BufferedReader reader = ModuleUtil.getFileReader( file );
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final KrakenNode node = demux ? new KrakenNode( line ): new KrakenNode( getFileID( file ), line );
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
	}
}