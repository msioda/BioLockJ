/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Dec 18, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;

/**
 * This utility helps work with OTU count files as formatted by the
 * {@link biolockj.module.implicit.parser.ParserModule}.
 */
public class OtuUtil
{
	/**
	 * This inner class is used to hold a signle line from an OTU count file.
	 */
	public static class OtuCountLine
	{
		/**
		 * Each OTU count file line has 2 parts: OTU Name and OTU Count
		 * 
		 * @param line OTU count file line
		 * @throws Exception if errors occur
		 */
		public OtuCountLine( final String line ) throws Exception
		{
			final StringTokenizer st = new StringTokenizer( line, Constants.TAB_DELIM );
			if( st.countTokens() != 2 )
			{
				throw new Exception(
						"OTU count lines should have 2 tokens {OTU, COUNT}, #tokens found: " + st.countTokens() );
			}
			otu = st.nextToken();
			count = Integer.valueOf( st.nextToken() );
		}

		/**
		 * Get OTU count
		 * 
		 * @return OTU count
		 */
		public Integer getCount()
		{
			return count;
		}

		/**
		 * Get OTU name
		 * 
		 * @return OTU name
		 */
		public String getOtu()
		{
			return otu;
		}

		private Integer count = null;
		private String otu = null;
	}

	// Prevent instantiation
	private OtuUtil()
	{}

	/**
	 * Build taxa name into OTU path, returns: level + {@value biolockj.Constants#DELIM_SEP} + taxa
	 * 
	 * @param level Taxonomy level
	 * @param taxa Taxa name
	 * @return level + {@value biolockj.Constants#DELIM_SEP} + taxa
	 * @throws Exception if errors occur
	 */
	public static String buildOtuTaxa( final String level, final String taxa ) throws Exception
	{
		return level + Constants.DELIM_SEP + BioLockJUtil.removeQuotes( taxa );
	}

	/**
	 * Compile OTU counts from an individual sample OTU count file
	 * 
	 * @param file OTU count file
	 * @return TreeMap(OTU, count)
	 * @throws Exception if errors occur
	 */
	public static TreeMap<String, Integer> compileSampleOtuCounts( final File file ) throws Exception
	{
		final TreeMap<String, Integer> otuCounts = new TreeMap<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final OtuCountLine ocl = new OtuCountLine( line );
				final String otu = ocl.getOtu();
				final Integer count = ocl.getCount();
				otuCounts.put( otu, count );
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		return otuCounts;
	}

	/**
	 * Find every unique OTU across all samples.
	 * 
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @return Ordered TreeSet of unique OTUs
	 * @throws Exception if errors occur
	 */
	public static TreeSet<String> findUniqueOtus( final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts )
			throws Exception
	{
		final TreeSet<String> otus = new TreeSet<>();
		for( final String id: sampleOtuCounts.keySet() )
		{
			final TreeMap<String, Integer> taxaCounts = sampleOtuCounts.get( id );
			if( taxaCounts != null && !taxaCounts.isEmpty() )
			{
				otus.addAll( sampleOtuCounts.get( id ).keySet() );
			}
		}

		return otus;
	}

	/**
	 * Build OTU count file using a standard format in the directory given.<br>
	 * Format: pipeline_name + prefix + {@value biolockj.Constants#OTU_COUNT} + sampleId + {@value biolockj.Constants#TSV_EXT}
	 * 
	 * @param dir File directory
	 * @param sampleId Sample ID
	 * @param prefix File prefix (after pipeline name)
	 * @return OTU count file
	 * @throws Exception if errors occur
	 */
	public static File getOtuCountFile( final File dir, String sampleId, String prefix ) throws Exception
	{
		if( prefix == null )
		{
			prefix = "_";
		}
		if( !prefix.startsWith( "_" ) )
		{
			prefix = "_" + prefix;
		}
		if( !prefix.endsWith( "_" ) )
		{
			prefix += "_";
		}

		if( sampleId != null )
		{
			sampleId = "_" + sampleId;
		}
		else
		{
			sampleId = "";
		}

		return new File( dir.getAbsolutePath() + File.separator + Config.pipelineName() + prefix + Constants.OTU_COUNT
				+ sampleId + Constants.TSV_EXT );
	}

	/**
	 * Extract the sampleId from the OTU count file name.<br>
	 * Input files should include a file name just before the .tsv file extension.
	 * 
	 * @param otuCountFile {@value biolockj.Constants#OTU_COUNT} file
	 * @return Sample ID
	 * @throws Exception if errors occur
	 */
	public static String getSampleId( final File otuCountFile ) throws Exception
	{
		if( otuCountFile.getName().lastIndexOf( "_" ) < 0 )
		{
			throw new Exception( "Unexpected format!  Missing \"_\" from input file name: " + otuCountFile.getName() );
		}
		return otuCountFile.getName().substring( otuCountFile.getName().lastIndexOf( Constants.OTU_COUNT + "_" ) + 9,
				otuCountFile.getName().length() - Constants.TSV_EXT.length() );
	}

	/**
	 * TreeMap OTU counts for each sample file formatted and named as in
	 * {@link biolockj.module.implicit.parser.ParserModule} output.
	 * 
	 * @param files Collection of OTU count files
	 * @return TreeMap(SampleID, TreeMap(OTU, count)) OTU counts by sample
	 * @throws Exception if any of the input file names are missing "_{@value biolockj.Constants#OTU_COUNT}_"
	 */
	public static TreeMap<String, TreeMap<String, Integer>> getSampleOtuCounts( final Collection<File> files )
			throws Exception
	{
		final TreeMap<String, TreeMap<String, Integer>> otuCountsBySample = new TreeMap<>();
		for( final File file: files )
		{
			if( !file.getName().contains( "_" + Constants.OTU_COUNT + "_" ) )
			{
				throw new Exception( "Module input files must contain sample OTU counts with \"_" + Constants.OTU_COUNT
						+ "_\" as part of the file name.  Found file: " + file.getAbsolutePath() );
			}

			otuCountsBySample.put( getSampleId( file ), compileSampleOtuCounts( file ) );
		}

		return otuCountsBySample;
	}

	/**
	 * Check the file name and contents to determine if file is an OTU count file.
	 * 
	 * @param file File
	 * @return boolean TRUE if file is an OTU count file
	 * @throws Exception if errors occur
	 */
	public static boolean isOtuFile( final File file ) throws Exception
	{
		final String name = file.getName();
		if( name.contains( "_" + Constants.OTU_COUNT + "_" ) && name.endsWith( Constants.TSV_EXT ) )
		{
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					new OtuCountLine( line );
				}
			}
			catch( final Exception ex )
			{
				Log.debug( OtuUtil.class, "File is not a valid OTU count file: " + file.getAbsolutePath() );
				return false;
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
			}

			return true;
		}

		return false;
	}

}
