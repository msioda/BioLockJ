/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.exception.ConfigFormatException;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;

/**
 * This utility is used to log-transform the raw OTU counts on Log10 or Log-e scales.
 */
public class LogTransformer extends JavaModuleImpl implements JavaModule
{
	/**
	 * Verify {@link biolockj.Config}.{@value biolockj.Config#REPORT_LOG_BASE} property is valid (if defined) with a
	 * value = (e or 10).
	 *
	 * @throws ConfigFormatException if REPORT_LOG_BASE is not set to a valid option (e or 10)
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		logBase = Config.requireString( Config.REPORT_LOG_BASE );

		if( !logBase.equals( "10" ) && !logBase.equals( "e" ) )
		{
			throw new ConfigFormatException( Config.REPORT_LOG_BASE,
					"Property only accepts value \"10\" or \"e\"" );
		}
		
		super.checkDependencies();
	}

	/**
	 * 
	 */
	@Override
	public void runModule() throws Exception
	{
		for( final File file: getInputFiles() )
		{
			transform( file );
		}
	}

	/**
	 * Log transform the data
	 *
	 * @param otuTable OTU raw count table
	 * @throws Exception if unable to construct DataNormalizer
	 */
	protected void transform( final File otuTable ) throws Exception
	{
		final BufferedReader reader = BioLockJUtil.getFileReader( otuTable );
		final List<List<Double>> dataPointsLogged = new ArrayList<>();
		final List<List<Double>> dataPointsUnnormalized = new ArrayList<>();
		final List<String> sampleNames = new ArrayList<>();
		final List<String> otuNames = getOtuNames( reader.readLine() );

		String nextLine = reader.readLine();
		int totalCounts = 0;
		while( nextLine != null )
		{
			final StringTokenizer st = new StringTokenizer( nextLine, BioLockJ.TAB_DELIM );
			final String sampleName = st.nextToken();
			final List<Double> innerList = new ArrayList<>();
			sampleNames.add( sampleName );
			dataPointsUnnormalized.add( innerList );
			dataPointsLogged.add( new ArrayList<Double>() );

			while( st.hasMoreTokens() )
			{
				final String nextToken = st.nextToken();
				double d = 0;
				if( nextToken.length() > 0 )
				{
					d = Double.parseDouble( nextToken );
				}

				innerList.add( d );
				totalCounts += d;
			}

			nextLine = reader.readLine();
		}

		reader.close();
		assertNum( totalCounts, dataPointsUnnormalized );
		assertNoZeros( dataPointsUnnormalized );

		for( int x = 0; x < dataPointsUnnormalized.size(); x++ )
		{
			final List<Double> unnormalizedInnerList = dataPointsUnnormalized.get( x );


			final List<Double> loggedInnerList = dataPointsLogged.get( x );

			for( int y = 0; y < unnormalizedInnerList.size(); y++ )
			{
				if( logBase.equalsIgnoreCase( LOG_E ) )
				{
					loggedInnerList.add( Math.log( y + 1 ) );
				}
				else if( logBase.equalsIgnoreCase( LOG_10 ) )
				{
					loggedInnerList.add( Math.log10( y + 1 ) );
				}
			}
		}

		final String pathPrefix = getOutputDir().getAbsolutePath() + File.separator
				+ otuTable.getName().substring( 0, otuTable.getName().indexOf( TSV ) );

		writeDataToFile( new File( pathPrefix + "_" + "Log" + logBase + TSV ), sampleNames, otuNames, dataPointsLogged );
	}

	private List<String> getOtuNames( final String firstLine ) throws Exception
	{
		final List<String> otuNames = new ArrayList<>();
		final StringTokenizer st = new StringTokenizer( firstLine, BioLockJ.TAB_DELIM );
		st.nextToken(); // skip ID & then strip quotes
		while( st.hasMoreTokens() )
		{
			otuNames.add( BioLockJUtil.removeOuterQuotes( st.nextToken() ) );
		}

		return otuNames;
	}

	private void writeDataToFile( final File file, final List<String> sampleNames, final List<String> otuNames,
			final List<List<Double>> otuCounts ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );

		writer.write( MetaUtil.getID() );

		for( final String s: otuNames )
		{
			writer.write( BioLockJ.TAB_DELIM + s );
		}

		writer.write( BioLockJ.RETURN );

		final int size = sampleNames.size();
		for( int x = 0; x < size; x++ )
		{
			writer.write( sampleNames.get( x ) );

			for( int y = 0; y < otuNames.size(); y++ )
			{
				writer.write( BioLockJ.TAB_DELIM + otuCounts.get( x ).get( y ) );
			}

			if( x + 1 != size )
			{
				writer.write( BioLockJ.RETURN );
			}
		}

		writer.close();
	}

	private static void assertNoZeros( final List<List<Double>> dataPointsUnnormalized ) throws Exception
	{
		for( int x = 0; x < dataPointsUnnormalized.size(); x++ )
		{
			for( int y = 0; y < dataPointsUnnormalized.get( x ).size(); y++ )
			{
				double sum = 0;

				for( final Double d: dataPointsUnnormalized.get( x ) )
				{
					sum += d;
				}

				if( sum == 0 )
				{
					throw new Exception( "Logic error" );
				}

			}
		}
	}

	private static void assertNum( final int totalCounts, final List<List<Double>> dataPointsUnnormalized )
			throws Exception
	{
		int sum = 0;

		for( int x = 0; x < dataPointsUnnormalized.size(); x++ )
		{
			for( int y = 0; y < dataPointsUnnormalized.get( x ).size(); y++ )
			{
				sum += dataPointsUnnormalized.get( x ).get( y );
			}
		}

		if( totalCounts != sum )
		{
			throw new Exception( "Logic error " + totalCounts + " " + sum );
		}

		if( dataPointsUnnormalized.size() > 0 )
		{
			final int length = dataPointsUnnormalized.get( 0 ).size();

			for( int x = 0; x < dataPointsUnnormalized.size(); x++ )
			{
				if( length != dataPointsUnnormalized.get( x ).size() )
				{
					throw new Exception( "Jagged array" );
				}
			}
		}
	}

	private String logBase = "";


	/**
	 * Log 10 display string as 1/2 supported values for: {@value biolockj.Config#REPORT_LOG_BASE}
	 */
	protected static final String LOG_10 = "10";

	/**
	 * Log e display string as 1/2 supported values for: {@value biolockj.Config#REPORT_LOG_BASE}
	 */
	protected static final String LOG_E = "e";
}
