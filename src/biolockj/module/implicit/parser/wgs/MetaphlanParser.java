/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Apr 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.parser.wgs;

import java.io.BufferedReader;
import java.io.File;
import biolockj.Log;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.node.ParsedSample;
import biolockj.node.wgs.MetaphlanNode;
import biolockj.util.BioLockJUtil;
import biolockj.util.MemoryUtil;

/**
 * This BioModules parses MetaphlanClassifier output reports to build standard OTU abundance tables.
 */
public class MetaphlanParser extends ParserModuleImpl implements ParserModule
{

	/**
	 * To parse the taxonomy level reports output by {@link biolockj.module.classifier.wgs.MetaphlanClassifier}:
	 * <ol>
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.wgs.MetaphlanNode#getSampleId()} if
	 * not yet created.
	 * <li>Add the {@link biolockj.node.wgs.MetaphlanNode#getCount()} (1) to {@link biolockj.node.ParsedSample} OTU
	 * count.
	 * </ol>
	 * <p>
	 * Sample Metaphlan report line (head 7A_1_reported.tsv) :<br>
	 * #SampleID Metaphlan2_Analysis #clade_name relative_abundance coverage average_genome_length_in_the_clade
	 * estimated_number_of_reads_from_the_clade k__Bacteria|p__Bacteroidetes 14.68863 0.137144143537 4234739 580770
	 */
	@Override
	public void parseSamples() throws Exception
	{
		for( final File file: getInputFiles() )
		{
			ParsedSample sample = null;
			MemoryUtil.reportMemoryUsage( "Parse " + file.getAbsolutePath() );
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{

				if( !line.startsWith( "#" ) )
				{
					final MetaphlanNode node = new MetaphlanNode(
							file.getName().replace( ClassifierModule.PROCESSED, "" ), line );
					if( isValid( node ) )
					{
						sample = getParsedSample( node.getSampleId() );
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
			Log.debug( getClass(), "Sample # " + getParsedSamples().size() );
			if( sample != null )
			{
				sample.buildOtuCounts();
				sample.report();
			}
		}
	}
}
