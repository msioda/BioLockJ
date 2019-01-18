/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Dec 20, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report;

import java.io.*;
import java.util.*;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.OtuUtil;

/**
 * This BioModule compiles the counts from all OTU count files into a single summary OTU count file containing OTU
 * counts for the entire dataset.
 */
public class CompileOtuCounts extends JavaModuleImpl implements JavaModule
{

	/**
	 * Add summary with unique OTU counts/level.
	 */
	@Override
	public String getSummary() throws Exception
	{
		String msg = "# Samples: " + BioLockJUtil.formatNumericOutput( MetaUtil.getSampleIds().size() ) + RETURN;
		int uniqueOtus = 0;
		int totalOtus = 0;
		BufferedReader reader = null;
		try
		{
			reader = BioLockJUtil.getFileReader( getSummaryOtuFile() );
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final OtuUtil.OtuCountLine otuLine = new OtuUtil.OtuCountLine( line );
				uniqueOtus++;
				totalOtus += otuLine.getCount();
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		msg += "# Unique OTUs: " + BioLockJUtil.formatNumericOutput( uniqueOtus ) + RETURN;
		msg += "# Total OTUs: " + BioLockJUtil.formatNumericOutput( totalOtus ) + RETURN;
		msg += getMinOtusPerSample() + RETURN;
		msg += getMaxOtusPerSample() + RETURN;
		return super.getSummary() + msg;
	}

	@Override
	public boolean isValidInputModule( final BioModule previousModule ) throws Exception
	{
		return OtuUtil.isOtuModule( previousModule );
	}

	@Override
	public void runModule() throws Exception
	{
		buildSummaryOtuCountFile( compileOtuCounts( getInputFiles() ) );
	}

	/**
	 * Build Summary OTU count file for all samples.
	 * 
	 * @param otuCounts OTU-count mapping
	 * @throws Exception if errors occur
	 */
	protected void buildSummaryOtuCountFile( final TreeMap<String, Integer> otuCounts ) throws Exception
	{
		final File otuCountFile = OtuUtil.getOtuCountFile( getOutputDir(), null, SUMMARY );
		Log.info( getClass(),
				"Build " + otuCountFile.getAbsolutePath() + " from " + otuCounts.size() + " unqiue OTU strings" );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( otuCountFile ) );
		try
		{
			for( final String otu: otuCounts.keySet() )
			{
				writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}
	}

	/**
	 * Compile OTU counts from the individual sample OTU count files
	 * 
	 * @param files Collection of OTU count files
	 * @return TreeMap(OTU, count)
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, Integer> compileOtuCounts( final Collection<File> files ) throws Exception
	{
		final TreeMap<String, Integer> combinedOtuCounts = new TreeMap<>();
		for( final File file: files )
		{
			final TreeMap<String, Integer> otuCounts = OtuUtil.compileSampleOtuCounts( file );
			uniqueOtuPerSample.put( OtuUtil.getSampleId( file ), otuCounts.size() );
			for( final String otu: otuCounts.keySet() )
			{
				final Integer count = otuCounts.get( otu );

				if( !combinedOtuCounts.keySet().contains( otu ) )
				{
					combinedOtuCounts.put( otu, 0 );
				}

				combinedOtuCounts.put( otu, combinedOtuCounts.get( otu ) + count );
			}

		}
		return combinedOtuCounts;
	}

	/**
	 * Find the maximum OTU count per sample.
	 * 
	 * @return Max OTU per sample.
	 */
	protected String getMaxOtusPerSample()
	{
		final TreeSet<String> ids = new TreeSet<>();
		int max = 0;
		for( final String sampleId: uniqueOtuPerSample.keySet() )
		{
			if( uniqueOtuPerSample.get( sampleId ) == max )
			{
				ids.add( sampleId );
			}
			else if( uniqueOtuPerSample.get( sampleId ) > max )
			{
				ids.clear();
				ids.add( sampleId );
				max = uniqueOtuPerSample.get( sampleId );
			}
		}
		return "Max # Unique OTUs[ " + BioLockJUtil.formatNumericOutput( max ) + " ]: "
				+ BioLockJUtil.getCollectionAsString( ids );
	}

	/**
	 * Find the minimum OTU count per sample.
	 * 
	 * @return Min OTU per sample.
	 */
	protected String getMinOtusPerSample()
	{
		final TreeSet<String> ids = new TreeSet<>();
		Integer min = null;
		for( final String sampleId: uniqueOtuPerSample.keySet() )
		{
			if( min == null || uniqueOtuPerSample.get( sampleId ) == min )
			{
				ids.add( sampleId );
				min = uniqueOtuPerSample.get( sampleId );
			}
			else if( uniqueOtuPerSample.get( sampleId ) < min )
			{
				ids.clear();
				ids.add( sampleId );
				min = uniqueOtuPerSample.get( sampleId );
			}
		}
		return "Min # Unique OTUs[ " + BioLockJUtil.formatNumericOutput( min ) + " ]: "
				+ BioLockJUtil.getCollectionAsString( ids );
	}

	/**
	 * Get the summary output file
	 * 
	 * @return OTU summary file
	 * @throws Exception if errors occur
	 */
	protected File getSummaryOtuFile() throws Exception
	{
		return OtuUtil.getOtuCountFile( getOutputDir(), null, SUMMARY );
	}

	private final Map<String, Integer> uniqueOtuPerSample = new HashMap<>();
	
	/**
	 * Output file prefix: {@value #SUMMARY}
	 */
	public static final String SUMMARY = "summary";
}
