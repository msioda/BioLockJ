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
import java.util.Arrays;
import java.util.List;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.node.wgs.HumanN2Node;
import biolockj.node.wgs.MetaphlanNode;
import biolockj.util.BioLockJUtil;
import biolockj.util.MemoryUtil;
import biolockj.util.MetaUtil;

/**
 * This BioModules parses HumanN2Classifier output reports to build standard OTU abundance tables.<br>
 * Samples IDs are found in the column headers starting with the 2nd column.<br>
 * The count type depends on the HumanN2 config properties.
 */
public class HumanN2Parser2 extends ParserModuleImpl implements ParserModule
{

	/**
	 * To parse the taxonomy level reports output by {@link biolockj.module.classifier.wgs.HumanN2Classifier}:
	 * <ol>
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.wgs.HumanN2Classifier#getSampleId()} if
	 * not yet created.
	 * <li>Add {@link biolockj.node.wgs.HumanN2Classifier#getCount()} to {@link biolockj.node.ParsedSample} OTU
	 * count.
	 * </ol>
	 * <p>
	 * Sample HumanN2 report line (head output_pathabundance.tsv):<br>
	 * 1st cell format: [Pathway_ID]:[Pathway_Descr] | g__[genus_taxa].s__[species_taxa]<br>
	 * Example:  ARO-PWY: chorismate biosynthesis I|g__Acidaminococcus.s__Acidaminococcus_intestini
	 * #SampleID Metaphlan2_Analysis #clade_name relative_abundance coverage average_genome_length_in_the_clade
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
				for( String[] record: data )
				{
					boolean newRecord = true;
					for( String cell: record )
					{
						if( !newRecord )
						{
							newRecord = false;
							writer.write( Constants.TAB_DELIM );
						}
						writer.write( cell );
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
			
			MemoryUtil.reportMemoryUsage( "Parse " + file.getAbsolutePath() );
		}
	}
	
	
	private File getOutputFile( File inputFile ) throws Exception
	{
		return new File( getOutputDir().getAbsolutePath() + File.separator + Config.pipelineName() + "_" 
				+ inputFile.getName().replaceAll( Constants.TSV_EXT, "" ) + Constants.PROCESSED );
	}
	
	private static String[][] transpose( List<List<String>> data ) throws Exception
	{
		final List<List<String>> transposed = new ArrayList<>();
		for( List<String> row: data )
		{
			final List<String> outputRow = new ArrayList<>();
			outputRow.add( row.get( 0 ) );
			transposed.add( outputRow );
		}
		
		int m = data.size();
		int n = data.get( 0 ).size();
		
		final String transpose[][] = new String[n][m];
	     
	      for (int c = 0; c < m; c++)
	         for (int d = 0; d < n; d++)              
	            transpose[d][c] = data.get( c ).get( d ); 
		
		
		return transpose;
	}
	
	private static List<List<String>> parsePathAbundanceFile( final File file ) throws Exception
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
			if( reader != null ) reader.close();
		}

		return data;
	}
	
	
	
	
	
	public static final String HN2_UNMAPPED = "UNMAPPED";
	public static final String HN2_UNINTEGRATED = "UNINTEGRATED";
	public static final String HN2_PATHWAY = "# Pathway";
}
