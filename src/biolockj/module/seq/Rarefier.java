/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
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
package biolockj.module.seq;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.LongStream;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.BioModuleImpl;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.SeqUtil;

/**
 *
 */
public class Rarefier extends BioModuleImpl implements BioModule
{
	/**
	 * Parameter rarefyingMin will be set to 0 if undefined in the config file.
	 * Parameter rarefyingMax must be defined in the config file as a poitive integer > 1.
	 * @Exception thrown if rarefyingMax < 2
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		SeqUtil.getInputSequenceType(); // for validation
		final Integer rarefyingMax = Config.getPositiveInteger( INPUT_RAREFYING_MAX );
		final Integer rarefyingMin = Config.getNonNegativeInteger( INPUT_RAREFYING_MIN );

		if( ( rarefyingMax == null ) || ( rarefyingMax < 1 ) || ( rarefyingMin == null )
				|| ( rarefyingMin > rarefyingMax ) )
		{
			throw new Exception( "Invalid parameter value.  Rarefier requires that (" + INPUT_RAREFYING_MIN + " <= "
					+ INPUT_RAREFYING_MAX + ") & (" + INPUT_RAREFYING_MAX + " > 1)" );
		}
	}

	/**
	 * Shuffle list of sequences
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		verifyNoUnmergedFiles();

		final List<File> files = getInputFiles();
		final int numFiles = ( ( files == null ) ? 0: files.size() );
		//SeqUtil.registerNumReadsPerSample( files, requireSubDir( TEMP_DIR ) );

		Log.out.info( "Rarefying " + numFiles + " " + SeqUtil.getInputSequenceType() + " files..." );
		Log.out.info( "=====> Min # Reads = " + Config.getNonNegativeInteger( INPUT_RAREFYING_MIN ) );
		Log.out.info( "=====> Max # Reads = " + Config.getPositiveInteger( INPUT_RAREFYING_MAX ) );

		int i = 0;
		for( final File f: files )
		{
			rarefy( f );
			if( ( ++i % 5 ) == 0 )
			{
				Log.out.info( "Done rarefying " + i + "/" + numFiles + " files." );
			}
		}

		if( ( i % 5 ) != 0 )
		{
			Log.out.info( "Done rarefying " + i + "/" + numFiles + " files." );
		}

		removeBadSamples();
	}

	private void buildRarefiedFile( final File input, final List<Long> indexes ) throws Exception
	{
		final String fileExt = "." + SeqUtil.getInputSequenceType();
		final int blockSize = SeqUtil.isFastA() ? 2: 4;
		final String name = getOutputDir().getAbsolutePath() + File.separator + SeqUtil.getSampleId( input.getName() )
				+ fileExt;
		final File output = new File( name );
		final BufferedReader reader = ModuleUtil.getFileReader( input );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( output ) );
		try
		{
			long index = 0L;
			long i = 0L;
			final Set<Long> usedIndexes = new HashSet<>();
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( indexes.contains( index ) )
				{
					usedIndexes.add( index );
					writer.write( line + RETURN );
				}

				if( ( ++i % blockSize ) == 0 )
				{
					index++;
				}
			}

			if( !usedIndexes.containsAll( indexes ) )
			{
				indexes.removeAll( usedIndexes );
				Log.out.warn( "Error occurred rarefying indexes for: " + input.getAbsolutePath() );
				for( final Long x: indexes )
				{
					Log.out.warn( "Missing index: " + x );
				}
			}
		}
		catch( final Exception ex )
		{
			Log.out.error( "Error occurred rarefying " + input.getAbsolutePath(), ex );
		}
		finally
		{
			reader.close();
			writer.close();
		}
	}

	private void rarefy( final File f ) throws Exception
	{
		final String sampleId = SeqUtil.getSampleId( f.getName() );
		final long numReads = SeqUtil.getReadsPerSample().get( sampleId );
		Log.out.info( "Sample[" + sampleId + "]  numReads = " + numReads );
		if( numReads >= Config.getNonNegativeInteger( INPUT_RAREFYING_MIN ) )
		{
			final long[] range = LongStream.rangeClosed( 0L, ( numReads - 1L ) ).toArray();
			final List<Long> indexes = new ArrayList<>();
			Collections.addAll( indexes, Arrays.stream( range ).boxed().toArray( Long[]::new ) );
			Collections.shuffle( indexes );
			indexes.subList( Config.getNonNegativeInteger( INPUT_RAREFYING_MAX ), indexes.size() ).clear();
			buildRarefiedFile( f, indexes );
		}
		else
		{
			Log.out.info( "Remove sample [" + sampleId + "] - contains less than minimum # reads ("
					+ Config.getNonNegativeInteger( INPUT_RAREFYING_MIN ) + ")" );
			badSamples.add( sampleId );
		}
	}

	private void removeBadSamples() throws Exception
	{
		if( !MetaUtil.exists() )
		{
			return;
		}

		if( badSamples.isEmpty() )
		{
			Log.out.info( "All samples rarefied & meet minimum read threshold - none will be ommitted..." );
			return;
		}

		Log.out.info( "Removing samples below rarefying threshold" );
		Log.out.info( "Removing bad samples ===> " + badSamples );

		final File newMapping = new File(
				getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getFile().getName() );
		final BufferedReader reader = new BufferedReader( new FileReader( MetaUtil.getFile().getAbsolutePath() ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newMapping ) );

		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
				final String id = st.nextToken();
				if( !badSamples.contains( id ) )
				{
					writer.write( line + RETURN );
				}
			}
		}
		finally
		{
			reader.close();
			writer.close();
		}
	}

	private void verifyNoUnmergedFiles() throws Exception
	{
		if( Config.getBoolean( Config.INPUT_PAIRED_READS ) )
		{
			final BioModule merger = ModuleUtil.getModule( Merger.class.getName() );
			if( ( merger != null ) && !ModuleUtil.isComplete( merger ) )
			{
				throw new Exception(
						"Paired reads must be merged before rarefying!  " + "Reorder modules in config file so "
								+ getClass().getName() + " is executed after " + Merger.class.getName() );
			}
		}
	}

	private final Set<String> badSamples = new HashSet<>();
	private static final String INPUT_RAREFYING_MAX = "input.rarefyMax";
	private static final String INPUT_RAREFYING_MIN = "input.rarefyMin";
}
