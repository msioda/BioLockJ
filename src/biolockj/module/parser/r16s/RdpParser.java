/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Feb 9, 2017
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
package biolockj.module.parser.r16s;

import java.io.BufferedReader;
import java.io.File;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.parser.ParsedSample;
import biolockj.module.parser.ParserModule;
import biolockj.module.parser.ParserModuleImpl;
import biolockj.node.OtuNode;
import biolockj.node.r16s.RdpNode;
import biolockj.util.ModuleUtil;

/**
 * To see file format: > head 7A_1_reported.tsv
 *
 * FCABK7W:1:2105:21787:12788#/1 Root rootrank 1.0 Bacteria domain 1.0
 * Firmicutes phylum 1.0 Clostridia class 1.0 Clostridiales order 1.0
 * Ruminococcaceae family 1.0 Faecalibacterium genus 1.0
 *
 */
public class RdpParser extends ParserModuleImpl implements ParserModule
{

	/**
	 * Summary message.
	
	@Override
	public String getSummary()
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			int i = 0;
			for( final String gap: RdpNode.getGaps() )
			{
				sb.append( "Taxonomy gap[" + ( i++ ) + "]: " + gap + RETURN );
			}

			return sb.toString() + super.getSummary();
		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to produce module summary for: " + getClass().getName() + " : " + ex.getMessage(),
					ex );
		}

		return super.getSummary();
	} */

	/**
	 * Parse each line of each file to extract lines used to build OtuNodes.  The addOtuNode()
	 * method is used to populate the otuNodes map if above Config.RDP_THRESHOLD_SCORE.
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
				final RdpNode node = demux ? new RdpNode( line ): new RdpNode( getFileID( file ), line );
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

			reader.close();
		}
	}

	@Override
	protected boolean isValid( final OtuNode node )
	{
		try
		{
			if( ( (RdpNode) node ).getScore() >= Config.requirePositiveInteger( Config.RDP_THRESHOLD_SCORE ) )
			{
				return super.isValid( node );
			}
		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to verify if OTU node is valid! " + ex.getMessage(), ex );
		}
		return false;
	}

}