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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.Constants;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.*;

/**
 * This BioModules parses HumanN2Classifier output reports to build standard OTU abundance tables.<br>
 * Samples IDs are found in the column headers starting with the 2nd column.<br>
 * The count type depends on the HumanN2 config properties.
 */
public class HumanN2Parser2 extends ParserModuleImpl implements ParserModule
{
	
	@Override
	public void runModule() throws Exception
	{
		parseSamples();
	}
	
	/**
	 * To parse the taxonomy level reports output by {@link biolockj.module.classifier.wgs.HumanN2Classifier}.
	 * 
	 * Sample HumanN2 report line (head output_pathabundance.tsv):<br>
	 * 1st cell format: [Pathway_ID]:[Pathway_Descr] | g__[genus_taxa].s__[species_taxa]<br>
	 * Example: ARO-PWY: chorismate biosynthesis I|g__Acidaminococcus.s__Acidaminococcus_intestini #SampleID
	 * Metaphlan2_Analysis #clade_name relative_abundance coverage average_genome_length_in_the_clade
	 * estimated_number_of_reads_from_the_clade k__Bacteria|p__Bacteroidetes 14.68863 0.137144143537 4234739 580770
	 */
	@Override
	public void parseSamples() throws Exception
	{
		MemoryUtil.reportMemoryUsage( "Parse pathway abundance file" );
		for( final File file: getInputFiles() )
		{
			final String[][] data = transpose( parsePathAbundanceFile( file ) );

			final BufferedWriter writer = new BufferedWriter( new FileWriter( getOutputFile( file ) ) );
			try
			{
				boolean firstRecord = true;
				for( final String[] record: data )
				{
					boolean newRecord = true;
					for( final String cell: record )
					{
						if( !newRecord )
						{
							newRecord = false;
							writer.write( Constants.TAB_DELIM );
						}
						if( firstRecord && !cell.equals( HN2_PATHWAY ) )
						{
							
							writer.write( getSampleID( cell ) );
						}
						else
						{
							writer.write( cell );
						}
					}
					writer.write( Constants.RETURN );
					firstRecord = false;
				}
			}
			finally
			{
				if( writer != null )
				{
					writer.close();
				}
			}

			MemoryUtil.reportMemoryUsage( "Parse " + file.getAbsolutePath() );
		}
	}

	private File getOutputFile( final File inputFile ) throws Exception
	{
		return new File( getOutputDir().getAbsolutePath() + File.separator + Config.pipelineName() + "_"
				+ inputFile.getName().replaceAll( Constants.TSV_EXT, "" ) + Constants.PROCESSED );
	}

	/**
	 * Read in path abundance file, each inner lists represents 1 line from the file. Each cell in the tab delimited
	 * file is storeda as 1 element in the inner lists.
	 * 
	 * @param file Path abundance file
	 * @return List of Lists - each inner list 1 line
	 * @throws Exception if errors occur
	 */
	protected static List<List<String>> parsePathAbundanceFile( final File file ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final ArrayList<String> record = new ArrayList<>();
				final String[] cells = line.split( Constants.TAB_DELIM, -1 );
				for( final String cell: cells )
				{
					record.add( cell );
				}
				data.add( record );
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		return data;
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

	public static final String ABUND_SUFFIX = "_Abundance";
	public static final String PAIRED_SUFFIX = "_paired_merged";
	public static final String KD_SUFFIX = "_kneaddata";
	public static final String HN2_PATHWAY = "# Pathway";
	public static final String HN2_UNINTEGRATED = "UNINTEGRATED";
	public static final String HN2_UNMAPPED = "UNMAPPED";
}
