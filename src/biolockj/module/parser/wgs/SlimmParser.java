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
import biolockj.module.parser.ParserModuleImpl;
import biolockj.node.wgs.SlimmNode;
import biolockj.util.ModuleUtil;

/**
 * To see file format: > head 7A_1_phylum_reported.tsv
 *
 * No. Name Taxid NoOfReads RelativeAbundance Contributers Coverage 1
 * Bacteroidetes 976 1137994 29.7589 17 24.7204
 */
public class SlimmParser extends ParserModuleImpl
{
	/**
	 * SLIMM doesn't support Config.INPUT_DEMULTIPLEX option
	 *
	 * @Exception thrown if Config.INPUT_DEMULTIPLEX == TRUE
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( Config.getBoolean( Config.INPUT_DEMULTIPLEX ) )
		{
			throw new Exception( "SLIMM doesn't support multiplexed files.  Config must set: "
					+ Config.INPUT_DEMULTIPLEX + "=TRUE" );
		}
	}

	/**
	 * Parse each line of each file to extract lines used to build OtuNodes.  The addOtuNode()
	 * method is used to populate the otuNodes map.  Pass file name to SlimmNode which will
	 * extract Sample ID & level information from the file name.
	 */
	@Override
	public void parseSamples() throws Exception
	{
		int fileCount = 0;
		for( final File file: getInputFiles() )
		{
			Log.out.info( "PARSE FILE # (" + String.valueOf( fileCount++ ) + ") = " + file.getName() );
			final BufferedReader reader = ModuleUtil.getFileReader( file );

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

	//	/**
	//	 * Slimm provides one file/sample so pull SampleID from the classifier output file name.
	//	 */
	//	@Override
	//	protected String getFileID( final File file ) throws Exception
	//	{
	//		for( final String suffix: reportNameSuffixList )
	//		{
	//			if( file.getName().contains( suffix ) )
	//			{
	//				return file.getName().replace( suffix, "" );
	//			}
	//		}
	//
	//		throw new Exception(
	//				"SLIMM REPORT NAME INVALID - REQUIRED FORMAT = <Sample_ID>_<Taxonomy_Level>_reported.tsv" );
	//	}
	//
	//	private final List<String> reportNameSuffixList = new ArrayList<>();
	//	{
	//		reportNameSuffixList.add( DOMAIN_REPORT );
	//		reportNameSuffixList.add( PHYLUM_REPORT );
	//		reportNameSuffixList.add( CLASS_REPORT );
	//		reportNameSuffixList.add( ORDER_REPORT );
	//		reportNameSuffixList.add( FAMILY_REPORT );
	//		reportNameSuffixList.add( GENUS_REPORT );
	//		reportNameSuffixList.add( SPECIES_REPORT );
	//	}
	//
	//	private static final String CLASS_REPORT = "_class_reported.tsv";
	//	private static final String DOMAIN_REPORT = "_superkingdom_reported.tsv";
	//	private static final String FAMILY_REPORT = "_family_reported.tsv";
	//	private static final String GENUS_REPORT = "_genus_reported.tsv";
	//	private static final String ORDER_REPORT = "_order_reported.tsv";
	//	private static final String PHYLUM_REPORT = "_phylum_reported.tsv";
	//	private static final String SPECIES_REPORT = "_species_reported.tsv";
}
