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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import biolockj.Constants;
import biolockj.module.BioModule;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.*;

/**
 * This BioModules parses HumanN2Classifier output reports to build standard OTU abundance tables.<br>
 * Samples IDs are found in the column headers starting with the 2nd column.<br>
 * The count type depends on the HumanN2 config properties.
 */
public class HumanN2Parser extends ParserModuleImpl implements ParserModule
{

	@Override
	public boolean isValidInputModule( final BioModule module )
	{
		return PathwayUtil.isPathwayModule( module );
	}

	/**
	 * To parse the taxonomy level reports output by {@link biolockj.module.classifier.wgs.HumanN2Classifier}.
	 * 
	 * Sample HumanN2 report line (head output_pAbund.tsv):<br>
	 * 1st cell format: [Pathway_ID]:[Pathway_Descr] | g__[genus_taxa].s__[species_taxa]<br>
	 * Example: ARO-PWY: chorismate biosynthesis I|g__Acidaminococcus.s__Acidaminococcus_intestini #SampleID
	 * Metaphlan2_Analysis #clade_name relative_abundance coverage average_genome_length_in_the_clade
	 * estimated_number_of_reads_from_the_clade k__Bacteria|p__Bacteroidetes 14.68863 0.137144143537 4234739 580770
	 */
	@Override
	public void parseSamples() throws Exception
	{
		for( final File file: getInputFiles() )
		{
			final String[][] data = transpose( assignSampleIDs( BioLockJUtil.parseCountTable( file ) ) );
			final File outFile = PathwayUtil.getPathwayCountFile( getOutputDir(), Constants.HN2_FULL_REPORT );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( outFile ) );
			try
			{
				for( final String[] record: data )
				{
					boolean newRecord = true;
					for( final String cell: record )
					{
						writer.write( ( !newRecord ? Constants.TAB_DELIM: "" ) + stripQuotes( cell ) );
						newRecord = false;
					}
					writer.write( Constants.RETURN );
				}
			}
			finally
			{
				if( writer != null )
				{
					writer.close();
				}
			}

			MemoryUtil.reportMemoryUsage( "Parsed " + file.getAbsolutePath() );
		}
	}
	
	private String stripQuotes( String val ) throws Exception
	{
		return val.replaceAll( "'", "" ).replaceAll( "\"", "" );
	}

	@Override
	public void runModule() throws Exception
	{
		parseSamples();
	}

	private List<List<String>> assignSampleIDs( final List<List<String>> data ) throws Exception
	{
		final List<List<String>> output = new ArrayList<>();
		boolean firstRecord = true;
		for( final List<String> row: data )
		{
			final ArrayList<String> record = new ArrayList<>();
			if( firstRecord )
			{
				for( final String cell: row )
				{
					record.add( record.isEmpty() ? MetaUtil.getID(): getSampleID( cell ) );
				}
			}
			else
			{
				record.addAll( row );
			}

			output.add( record );
			firstRecord = false;
		}

		return output;
	}

	private String getSampleID( String name ) throws Exception
	{
		if( name.contains( PAIRED_SUFFIX ) )
		{
			name = name.replace( PAIRED_SUFFIX, "" );
		}
		if( name.contains( KD_SUFFIX ) )
		{
			name = name.replace( KD_SUFFIX, "" );
		}
		if( name.contains( ABUND_SUFFIX ) )
		{
			name = name.replace( ABUND_SUFFIX, "" );
		}

		return name;
	}

	private static String[][] transpose( final List<List<String>> data ) throws Exception
	{
		final List<List<String>> transposed = new ArrayList<>();
		for( final List<String> row: data )
		{
			final List<String> outputRow = new ArrayList<>();
			outputRow.add( row.get( 0 ) );
			transposed.add( outputRow );
		}

		final int m = data.size();
		final int n = data.get( 0 ).size();

		final String transpose[][] = new String[ n ][ m ];

		for( int c = 0; c < m; c++ )
		{
			for( int d = 0; d < n; d++ )
			{
				transpose[ d ][ c ] = data.get( c ).get( d );
			}
		}

		return transpose;
	}

	private static final String ABUND_SUFFIX = "_Abundance";
	private static final String KD_SUFFIX = "_kneaddata";
	private static final String PAIRED_SUFFIX = "_paired_merged";
}
