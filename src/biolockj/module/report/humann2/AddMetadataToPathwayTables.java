/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date May 15, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.humann2;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.JavaModule;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.PathwayUtil;

/**
 * This BioModule is used to add metadata columns to the OTU abundance tables.
 * 
 * @blj.web_desc Add Metadata to Pathway Tables
 */
public class AddMetadataToPathwayTables extends Humann2CountModule implements JavaModule
{
	/**
	 * For R to report HumanN2 reports instead of taxa levels
	 */
	@Override
	public void cleanUp() throws Exception
	{
		Config.setConfigProperty( Constants.R_INTERNAL_RUN_HN2, Constants.TRUE );
	}

	/**
	 * Produce summary message with min, max, mean, and median hit ratios
	 */
	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			sb.append( "Merged metadata for " + types.size() + " HumanN2 reports: " + types );
		}
		catch( final Exception ex )
		{
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( getClass(), msg );
		}

		return super.getSummary() + sb.toString();
	}

	/**
	 * This method matches records from the Pathway Abundance table and the metadata file by matching the sample ID
	 * value in the very 1st column (regardless of column title).<br>
	 * <br>
	 * Set
	 */
	@Override
	public void runModule() throws Exception
	{
		generateMergedTables();
		Log.info( getClass(), mergeHeaderLine );
		Log.info( getClass(), mergeSampleLine );
		Log.info( getClass(), "Metadata has been appended to the pathway abundance table" );
	}

	/**
	 * Create the merged metadata tables.
	 *
	 * @throws Exception if unable to build tables
	 */
	protected void generateMergedTables() throws Exception
	{
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		for( final File file: getInputFiles() )
		{
			final String name = file.getName().replaceAll( TSV_EXT, "" ) + META_MERGED;
			types.add( PathwayUtil.getHn2Type( file ) );
			Log.info( getClass(),
					"Merge HumanN2 " + PathwayUtil.getHn2Type( file ) + " table with metadata: " + outDir + name );
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( outDir + name ) );

			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final String mergedLine = getMergedLine( line );
				if( mergedLine != null )
				{
					writer.write( mergedLine + RETURN );
				}
			}

			writer.close();
			reader.close();
			Log.info( getClass(), "Done merging table: " + file.getAbsolutePath() );
		}

	}

	/**
	 * Return OTU table line with metadata row appended (both have PK = sample ID)
	 *
	 * @param line OTU table line
	 * @return OTU table line + metadata line
	 * @throws Exception if unable to create merged line
	 */
	protected String getMergedLine( final String line ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final String sampleId = new StringTokenizer( line, TAB_DELIM ).nextToken();
		if( sampleId.equals( MetaUtil.getID() ) || MetaUtil.getSampleIds().contains( sampleId ) )
		{
			sb.append( BioLockJUtil.removeQuotes( line ) );
			for( final String field: MetaUtil.getMetadataRecord( sampleId ) )
			{
				sb.append( TAB_DELIM ).append( BioLockJUtil.removeQuotes( field ) );
			}
		}
		else
		{
			Log.warn( getClass(), "Missing record for: " + sampleId + " in metadata: " + MetaUtil.getPath() );
			return null;
		}

		if( mergeHeaderLine == null )
		{
			mergeHeaderLine = "Merged OTU table header [" + sampleId + "] = " + sb.toString();
		}
		else if( mergeSampleLine == null )
		{
			mergeSampleLine = "Example Merged OTU table row [" + sampleId + "] = " + sb.toString();
		}

		return sb.toString();
	}

	private String mergeHeaderLine = null;
	private String mergeSampleLine = null;

	private final List<String> types = new ArrayList<>();

	/**
	 * File suffix added to OTU table file name once merged with metadata.
	 */
	private static final String META_MERGED = "_metaMerged" + TSV_EXT;
}
