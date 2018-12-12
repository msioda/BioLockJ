/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import biolockj.Log;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;
import biolockj.util.SummaryUtil;

/**
 * This BioModule parses sequence file to count the number of reads per sample. The data is stored in a new column
 * Num_Reads. If no metadata file exists, it is created with 2 columns: SampleID and Num_Reads.
 */
public class RegisterNumReads extends JavaModuleImpl implements JavaModule
{

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception
	{
		String msg = "# Samples:      " + readsPerSample.size() + RETURN;
		if( !readsPerSample.isEmpty() )
		{
			final TreeSet<Long> vals = new TreeSet<>(
					readsPerSample.values().stream().map( Long::parseLong ).collect( Collectors.toSet() ) );

			msg += "Min. # Reads:   " + vals.first() + RETURN;
			msg += "Max. # Reads:   " + vals.last() + RETURN;
			msg += "Mean # Reads:   " + SummaryUtil.getMean( vals, false ) + RETURN;
			msg += "Median # Reads: " + SummaryUtil.getMedian( vals, false ) + RETURN;

			if( !vals.first().equals( vals.last() ) )
			{
				final Set<String> minSamples = new HashSet<>();
				for( final String id: readsPerSample.keySet() )
				{
					if( readsPerSample.get( id ).equals( vals.first().toString() ) )
					{
						minSamples.add( id );
					}
				}

				msg += "Samples w/ Min. # Reads: " + minSamples + RETURN;
			}
		}

		return super.getSummary() + msg;
	}

	/**
	 * Register number of reads for each sample by parsing each file and counting number of lines. Add
	 * {@value #NUM_READS} column to metadata and refresh the cache.
	 */
	@Override
	public void runModule() throws Exception
	{
		if( MetaUtil.getFieldNames().contains( NUM_READS ) )
		{
			if( MetaUtil.getFieldValues( NUM_READS ).size() == MetaUtil.getSampleIds().size() )
			{
				Log.warn( getClass(), NUM_READS + " column already fully populated in metadata file :"
						+ MetaUtil.getFile().getAbsolutePath() );
				FileUtils.copyFileToDirectory( MetaUtil.getFile(), getOutputDir() );
				return;
			}
			else
			{
				Log.warn( getClass(), NUM_READS + " column partially populated.  Clearing values & recounting reads" );
				MetaUtil.removeColumn( NUM_READS, getTempDir() );
			}
		}

		final List<File> files = getInputFiles();
		Log.info( getClass(), "Counting # reads/sample for " + files.size() + " files" );

		for( final File f: files )
		{
			if( SeqUtil.isForwardRead( f.getName() ) )
			{
				final long count = SeqUtil.countNumReads( f );
				Log.debug( getClass(), "Num Reads for :[" + SeqUtil.getSampleId( f.getName() ) + "] = " + count );
				readsPerSample.put( SeqUtil.getSampleId( f.getName() ), Long.toString( count ) );
			}
		}

		MetaUtil.addColumn( NUM_READS, readsPerSample, getOutputDir() );
	}

	/**
	 * Getter for depricatedReadFields
	 * 
	 * @return depricatedReadFields
	 */
	public static Set<String> getDepricatedReadFields()
	{
		return depricatedReadFields;
	}

	/**
	 * Getter for numReadFieldName
	 * 
	 * @return numReadFieldName
	 */
	public static String getNumReadFieldName()
	{
		return numReadFieldName;
	}

	/**
	 * When a module modifies the number of reads, the new counts must replace the old count fields.
	 * 
	 * @param name Name of new number of reads metadata field
	 * @throws Exception if null value passed
	 */
	public static void setNumReadFieldName( final String name ) throws Exception
	{
		if( name == null )
		{
			throw new Exception( "Null name value passed to RegisterNumReads.setNumReadFieldName()" );
		}
		else if( numReadFieldName != null && numReadFieldName.equals( name ) )
		{
			Log.warn( RegisterNumReads.class, "NumReads field already set to: " + numReadFieldName );
		}
		else
		{
			if( numReadFieldName != null )
			{
				depricatedReadFields.add( numReadFieldName );
			}

			depricatedReadFields.remove( name );
			numReadFieldName = name;
		}
	}

	/**
	 * Metadata column name for column that holds number of reads per sample: {@value #NUM_READS}
	 */
	public static final String NUM_READS = "Num_Reads";

	private static final Set<String> depricatedReadFields = new HashSet<>();
	private static String numReadFieldName = NUM_READS;
	private static final Map<String, String> readsPerSample = new HashMap<>();

}
