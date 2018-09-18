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
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.node.ParsedSample;
import biolockj.node.wgs.SlimmNode;
import biolockj.util.SeqUtil;

/**
 * This BioModule parses SlimmClassifier output reports to build standard OTU abundance tables.
 */
public class SlimmParser extends ParserModuleImpl
{

	/**
	 * To parse the taxonomy level reports output by {@link biolockj.module.classifier.wgs.SlimmClassifier}:
	 * <ol>
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.wgs.SlimmNode#getSampleId()} if not
	 * yet created.
	 * <li>Add the {@link biolockj.node.wgs.SlimmNode#getCount()} (1) to {@link biolockj.node.ParsedSample} OTU count.
	 * </ol>
	 * <p>
	 * Sample SLIMM report line (head 7A_1_phylum_reported.tsv) :<br>
	 * No. Name Taxid NoOfReads RelativeAbundance Contributers Coverage 1 Bacteroidetes 976 1137994 29.7589 17 24.7204
	 */
	@Override
	public void parseSamples() throws Exception
	{
		for( final File file: getInputFiles() )
		{
			final BufferedReader reader = SeqUtil.getFileReader( file );

			String line = reader.readLine(); // skip header
			for( line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final SlimmNode node = new SlimmNode( file.getName(), line );
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
}
