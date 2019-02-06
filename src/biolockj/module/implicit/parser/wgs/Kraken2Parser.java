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
package biolockj.module.implicit.parser.wgs;

import java.io.BufferedReader;
import java.io.File;
import biolockj.Constants;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.node.OtuNode;
import biolockj.node.wgs.Kraken2Node;
import biolockj.util.BioLockJUtil;
import biolockj.util.MemoryUtil;

/**
 * This BioModules parses KrakenClassifier output reports to build standard OTU abundance tables.
 */
public class Kraken2Parser extends ParserModuleImpl implements ParserModule
{
	/**
	 * Parse all {@link biolockj.module.classifier.wgs.Kraken2Classifier} reports in the input directory.<br>
	 * Build an {@link biolockj.node.wgs.Kraken2Node} for each line.<br>
	 * If {@link #isValid(OtuNode)}: <br>
	 * <ol>
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.wgs.KrakenNode#getSampleId()} if not
	 * yet created.
	 * <li>Add the {@link biolockj.node.wgs.KrakenNode#getCount()} (1) to {@link biolockj.node.ParsedSample} OTU count.
	 * </ol>
	 * <p>
	 * Sample Kraken report line (head 7A_1_reported.tsv) :<br>
	 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales 20094
	 */
	@Override
	public void parseSamples() throws Exception
	{
		for( final File file: getInputFiles() )
		{
			MemoryUtil.reportMemoryUsage( "Parse " + file.getAbsolutePath() );
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					// Log.debug( getClass(), " LINE = " + line );
					addOtuNode( new Kraken2Node( file.getName().replace( Constants.PROCESSED, "" ), line ) );
				}
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
			}

		}
	}
}
