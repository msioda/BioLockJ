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
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.BioModuleImpl;
import biolockj.util.ModuleUtil;
import biolockj.util.SeqUtil;

/**
 * This utility trims primers configured using regular expressions.
 */
public class Trimmer extends BioModuleImpl implements BioModule
{
	/**
	 * Verify file containing primers to trim exists.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		Config.requireExistingFile( INPUT_TRIM_SEQ_FILE );
		primers = getSeqs();
		numLinesPerRead = SeqUtil.isFastA() ? 2: 4;
	}

	/**
	 * Will trim primers from fasta or fastq files (typically fastq).
	 * @throws Exception
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		verifyMergeAgentHasNotRun();
		trimSeqs();
		Log.out.debug( "Checking numLinesWithPrimer.size() = " + numLinesWithPrimer.size() );
		Log.out.debug( "Checking numLinesNoPrimer.size() = " + numLinesNoPrimer.size() );
		Log.out.debug( "Checking validReadsPerSample.size() = " + validReadsPerSample.size() );
		Log.out.debug( "Checking fileNames.size() = " + fileNames.size() );
		final TreeSet<String> ids = new TreeSet<>( validReadsPerSample.keySet() );
		for( final String name: fileNames )
		{
			boolean found = false;
			for( final String id: ids )
			{
				if( name.startsWith( id ) )
				{
					Log.out.debug( "found" );
					found = true;
					break;
				}
			}
			if( !found )
			{
				Log.out.warn( "File contains no primers!  File name: " + name );
			}
		}

		if( !Config.getBoolean( INPUT_KEEP_SEQS_MISSING_PRIMER ) )
		{
			Log.out.warn( INPUT_KEEP_SEQS_MISSING_PRIMER + "=N so any sequences without a primer have been discarded" );
		}

		long totalPrimerR = 0L;
		long totalNoPrimerR = 0L;
		long totalPrimerF = 0L;
		long totalNoPrimerF = 0L;
		long totalValid = 0L;
		for( final String key: ids )
		{
			final int v = validReadsPerSample.get( key );
			final int a = numLinesWithPrimer.get( key );
			final int b = numLinesNoPrimer.get( key );
			totalValid += v;

			Log.out.info( key + " Reads with primer ----------------- " + a + "/" + ( a + b ) + " = "
					+ getPercentage( a, a + b ) );

			if( ModuleUtil.moduleExists( Merger.class.getName() ) )
			{
				Log.out.info( key + " Paired reads with primer in both -- " + v + "/" + ( a + b ) + " = "
						+ getPercentage( v, a + b ) );
			}

			if( foundPaired && key.contains( Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX ) ) )
			{
				totalPrimerR += a;
				totalNoPrimerR += b;
			}
			else
			{
				totalPrimerF += a;
				totalNoPrimerF += b;
			}
		}

		final long totalF = totalPrimerF + totalNoPrimerF;
		final long totalR = totalPrimerR + totalNoPrimerR;
		final long totalNoPrimer = totalNoPrimerF + totalNoPrimerR;
		final long totalWithPrimer = totalPrimerF + totalPrimerR;
		final long total = totalNoPrimer + totalWithPrimer;
		if( total > 1 )
		{
			//registerNumPrimersPerSample();

			Log.out.info( "% Reads with primer (all files) = " + totalWithPrimer + "/" + total + " = "
					+ getPercentage( totalWithPrimer, total ) );

			if( ModuleUtil.moduleExists( Merger.class.getName() ) )
			{
				Log.out.info( "Forward reads with primers (all files) = " + totalPrimerF + "/" + totalF + " = "
						+ getPercentage( totalPrimerF, totalF ) );
				Log.out.info( "Reverse reads with primers (all files) = " + totalPrimerR + "/" + totalR + " = "
						+ getPercentage( totalPrimerR, totalR ) );
				Log.out.info( "Paired reads with primer in both (all files) = " + totalValid + "/" + total + " = "
						+ getPercentage( totalValid, total ) );
			}
		}
	}

	private void addFileName( final File f )
	{
		String fileName = f.getName();
		if( fileName.toLowerCase().endsWith( ".gz" ) )
		{
			fileName = fileName.substring( 0, fileName.length() - 3 );
		}
		fileNames.add( fileName );
	}

	private String getHeader( final String line )
	{
		return line.substring( 0, line.indexOf( " " ) - 1 );
	}

	private String getPercentage( final long num, final long denom )
	{
		String percentage = Double.valueOf( df.format( 100 * ( (double) num / denom ) ) ).toString();
		if( percentage.indexOf( "." ) < 3 )
		{
			percentage += "0";
		}

		return percentage + "%";
	}

	private Set<String> getSeqs() throws Exception
	{
		final Set<String> seqs = new HashSet<>();
		final File trimSeqFile = Config.requireExistingDir( INPUT_TRIM_SEQ_FILE );
		final BufferedReader reader = ModuleUtil.getFileReader( trimSeqFile );
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			if( !line.startsWith( "#" ) )
			{
				final String seq = line.trim();
				if( seq.length() > 0 )
				{
					Log.out.info( "Found primer to trim: " + seq );
					seqs.add( seq );
				}
			}
		}

		reader.close();

		if( seqs.size() < 1 )
		{
			throw new Exception( "No Primers found in: " + trimSeqFile.getAbsolutePath() );
		}

		return seqs;
	}

	private String getTrimFilePath( final File f ) throws Exception
	{
		String suffix = "";
		if( Config.getBoolean( Config.INPUT_PAIRED_READS ) )
		{
			if( SeqUtil.isForwardRead( f.getName() ) )
			{
				suffix = Config.requireString( Config.INPUT_FORWARD_READ_SUFFIX );
			}
			else
			{
				foundPaired = true;
				suffix = Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX );
			}
		}

		return getOutputDir() + File.separator + SeqUtil.getSampleId( f.getName() ) + suffix + "."
				+ SeqUtil.getInputSequenceType();
	}

	private Set<String> getValidHeaders( final File file ) throws Exception
	{
		final Set<String> validHeaders = new HashSet<>();
		final BufferedReader reader = ModuleUtil.getFileReader( file );
		int lineCounter = 1;
		String header = null;
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				line = line.trim();
				if( ( lineCounter % numLinesPerRead ) == 1 )
				{
					header = getHeader( line );
				}
				else if( ( lineCounter % numLinesPerRead ) == 2 )
				{
					for( final String seq: primers )
					{
						final int seqLength = line.length();
						line = line.replaceFirst( seq, "" );
						if( seqLength != line.length() )
						{
							if( validHeaders.contains( header ) )
							{
								Log.out.warn( "DUPLICATE HEADER - SHOULD THROW EXCEPTION?" );
							}
							validHeaders.add( header );
						}
					}
				}
				lineCounter++;
			}
		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to get header line from file: " + file.getAbsolutePath(), ex );
			throw new Exception( ex.getMessage() );
		}
		finally
		{
			reader.close();
		}

		Log.out.debug( file.getName() + " | How many valid headers? = " + validHeaders.size() );

		return validHeaders;
	}

	private void processFile( final File file ) throws Exception
	{
		processFile( file, new HashSet<>() );
	}

	private void processFile( final File file, final Set<String> validHeaders ) throws Exception
	{
		Log.out.debug( file.getName() + " processing with " + validHeaders.size() + " valid headers." );
		final File trimmedFile = new File( getTrimFilePath( file ) );
		addFileName( file );
		final String trimFileName = trimmedFile.getName();
		final BufferedReader reader = ModuleUtil.getFileReader( file );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( trimmedFile ) );
		try
		{
			int lineCounter = 1;
			int seqLength = 0;
			int seqCount = 0;
			String header = "";
			boolean validRecord = false;
			final String[] seqLines = new String[ numLinesPerRead ];
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				boolean found = false;
				line = line.trim();

				if( SeqUtil.isFastQ() && ( ( lineCounter % numLinesPerRead ) == 1 ) )
				{
					header = getHeader( line );
				}
				else if( SeqUtil.isFastQ() && ( ( lineCounter % numLinesPerRead ) == 0 ) )
				{
					line = line.substring( seqLength );
				}
				else if( ( SeqUtil.isFastA() && ( ( lineCounter % numLinesPerRead ) == 0 ) )
						|| ( SeqUtil.isFastQ() && ( ( lineCounter % numLinesPerRead ) == 2 ) ) )
				{
					seqLength = 0;
					for( final String seq: primers )
					{
						seqLength = line.length();
						line = line.replaceFirst( seq, "" );
						if( seqLength != line.length() )
						{
							seqLength = seqLength - line.length();
							found = true;
							break;
						}
					}

					if( !found )
					{
						seqLength = 0;
						final Integer x = numLinesNoPrimer.get( trimFileName );
						numLinesNoPrimer.put( trimFileName, ( ( x == null ) ? 1: x + 1 ) );
					}
					else
					{
						validRecord = ModuleUtil.moduleExists( Merger.class.getName() )
								? validHeaders.contains( header ): true;
						final Integer x = numLinesWithPrimer.get( trimFileName );
						numLinesWithPrimer.put( trimFileName, ( ( x == null ) ? 1: x + 1 ) );
					}
				}

				seqLines[ seqCount++ ] = line;
				if( seqCount == numLinesPerRead )
				{
					if( Config.getBoolean( INPUT_KEEP_SEQS_MISSING_PRIMER ) || validRecord )
					{
						final Integer x = validReadsPerSample.get( trimFileName );
						validReadsPerSample.put( trimFileName, ( ( x == null ) ? 1: x + 1 ) );

						for( int j = 0; j < numLinesPerRead; j++ )
						{
							writer.write( seqLines[ j ] + RETURN );
						}
					}
					seqCount = 0;
					validRecord = false;
				}

				lineCounter++;
			}
		}
		catch( final Exception ex )
		{
			Log.out.error( "Error removing primers from file = " + file.getAbsolutePath(), ex );
		}
		finally
		{
			reader.close();
			writer.close();
		}
	}

	private void trimSeqs() throws Exception
	{
		final boolean mergeModuleExists = ModuleUtil.moduleExists( Merger.class.getName() );
		final Map<File, File> pairedReads = mergeModuleExists ? SeqUtil.getPairedReads( getInputFiles() ): null;
		final List<File> files = mergeModuleExists ? new ArrayList<>( pairedReads.keySet() ): getInputFiles();
		final int count = ( ( files == null ) ? 0: files.size() );
		int i = 0;
		Log.out.info( "Trimming primers from " + ( mergeModuleExists ? ( 2 * count ): count ) + " files..." );
		for( final File file: files )
		{
			if( mergeModuleExists )
			{
				final Set<String> validReads = getValidHeaders( file );
				validReads.retainAll( getValidHeaders( pairedReads.get( file ) ) );
				processFile( file, validReads );
				processFile( pairedReads.get( file ), validReads );
			}
			else
			{
				processFile( file );
			}

			if( ( ( i++ + 1 ) % 25 ) == 0 )
			{
				Log.out.info( "Done trimming " + i + "/" + count + ( mergeModuleExists ? " file pairs": " files" ) );
			}
		}

		Log.out.info( "Done trimming " + i + "/" + count + ( mergeModuleExists ? " file pairs": " files" ) );
	}

	private void verifyMergeAgentHasNotRun() throws Exception
	{
		final BioModule merger = ModuleUtil.getModule( Merger.class.getName() );
		if( ( merger != null ) && ModuleUtil.isComplete( merger ) )
		{
			throw new Exception( "Paired reads must not be merged until after trimming!  "
					+ "Reorder modules in config file so " + getClass().getName() + " is 1st executed." );
		}
	}

	private final DecimalFormat df = new DecimalFormat( "##.##" );
	private final Set<String> fileNames = new HashSet<>();
	private boolean foundPaired = false;
	private final Map<String, Integer> numLinesNoPrimer = new HashMap<>();
	private int numLinesPerRead = 0;
	private final Map<String, Integer> numLinesWithPrimer = new HashMap<>();
	private Set<String> primers = null;
	private final Map<String, Integer> validReadsPerSample = new HashMap<>();
	private static final String INPUT_KEEP_SEQS_MISSING_PRIMER = "input.keepSeqsMissingPrimer";
	private static final String INPUT_TRIM_SEQ_FILE = "input.trimSeqPath";

}