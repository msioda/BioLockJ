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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.exception.ConfigFormatException;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.r.R_Module;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;

/**
 * This utility is used to normalize and/or log-transform the raw OTU counts using the formulas:
 * <ul>
 * <li>Normalized OTU count formula = (RC/n)*((SUM(x))/N)+1
 * <li>Relative abundance formula = Log(log_base) [ (RC/n)*((SUM(x))/N)+1 ]
 * </ul>
 * The code implementation supports (log_base = e) and (log_base = 10) which is configured via
 * {@link biolockj.Config#REPORT_LOG_BASE} property.
 * <ul>
 * <li>RC = Sample OTU count read in from each Sample-OTU cell in the raw count file passed to the constructor
 * <li>n = number of sequences in the sample, read in as the row sum (sum of OTU counts for the sample)
 * <li>SUM(x) = total number of counts in the table, read in as the table sum (sum of OTU counts for all samples)
 * <li>N = total number of samples, rowCount - 1 (header row)
 * </ul>
 * Further explanation regarding the normalization scheme, please read The ISME Journal 2013 paper by Dr. Anthony Fodor:
 * "Stochastic changes over time and not founder effects drive cage effects in microbial community assembly in a mouse
 * model" <a href= "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3806260/" target=
 * "_top">https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3806260/</a>
 */
public class Normalizer extends JavaModuleImpl implements JavaModule
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
		logBase = Config.getString( Config.REPORT_LOG_BASE );
		if( logBase != null )
		{
			if( !logBase.equals( "10" ) && !logBase.equals( "e" ) )
			{
				throw new ConfigFormatException( Config.REPORT_LOG_BASE,
						"Property only accepts value \"10\" or \"e\"" );
			}
		}
		else
		{
			logBase = "";
		}
	}

	/**
	 * 
	 */
	@Override
	public void runModule() throws Exception
	{
		for( final File file: getInputFiles() )
		{
			normalize( file );
		}
	}

	/**
	 * Populate normalized OTU counts with the formula: (RC/n)*((SUM(x))/N)+1
	 *
	 * @param otuTable OTU raw count table
	 * @throws Exception if unable to construct DataNormalizer
	 */
	protected void normalize( final File otuTable ) throws Exception
	{
		final BufferedReader reader = SeqUtil.getFileReader( otuTable );
		final List<List<Double>> dataPointsNormalized = new ArrayList<>();
		final List<List<Double>> dataPointsNormalizedThenLogged = new ArrayList<>();
		final List<List<Double>> dataPointsUnnormalized = new ArrayList<>();
		final List<String> sampleNames = new ArrayList<>();
		final List<String> otuNames = removeOuterQuotes( reader.readLine() );

		String nextLine = reader.readLine();
		int totalCounts = 0;
		while( nextLine != null )
		{
			final StringTokenizer st = new StringTokenizer( nextLine, BioLockJ.TAB_DELIM );
			final String sampleName = st.nextToken();
			final List<Double> innerList = new ArrayList<>();
			sampleNames.add( sampleName );
			dataPointsUnnormalized.add( innerList );
			dataPointsNormalized.add( new ArrayList<Double>() );
			dataPointsNormalizedThenLogged.add( new ArrayList<Double>() );

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
		final double avgNumber = totalCounts / dataPointsNormalized.size();

		for( int x = 0; x < dataPointsUnnormalized.size(); x++ )
		{
			final List<Double> unnormalizedInnerList = dataPointsUnnormalized.get( x );
			double sum = 0;

			for( final Double d: unnormalizedInnerList )
			{
				sum += d;
			}

			final List<Double> normalizedInnerList = dataPointsNormalized.get( x );
			final List<Double> loggedInnerList = dataPointsNormalizedThenLogged.get( x );

			for( int y = 0; y < unnormalizedInnerList.size(); y++ )
			{
				final double val = avgNumber * unnormalizedInnerList.get( y ) / sum;
				normalizedInnerList.add( val );

				if( logBase.equalsIgnoreCase( LOG_E ) )
				{
					loggedInnerList.add( Math.log( val + 1 ) );
				}
				else if( logBase.equalsIgnoreCase( LOG_10 ) )
				{
					loggedInnerList.add( Math.log10( val + 1 ) );
				}
			}
		}

		final String pathPrefix = getOutputDir().getAbsolutePath() + File.separator
				+ otuTable.getName().substring( 0, otuTable.getName().indexOf( R_Module.TSV_EXT ) );

		if( !logBase.isEmpty() )
		{
			writeDataToFile( new File( pathPrefix + "_" + "Log" + logBase + NORMAL + R_Module.TSV_EXT ), sampleNames,
					otuNames, dataPointsNormalizedThenLogged );
		}
		else
		{
			writeDataToFile( new File( pathPrefix + NORMAL + R_Module.TSV_EXT ), sampleNames, otuNames,
					dataPointsNormalized );
		}

	}

	private List<String> removeOuterQuotes( final String firstLine )
	{
		final List<String> otuNames = new ArrayList<>();
		final StringTokenizer st = new StringTokenizer( firstLine, BioLockJ.TAB_DELIM );
		st.nextToken(); // skip ID & then strip quotes
		while( st.hasMoreTokens() )
		{
			String taxaName = st.nextToken();
			if( taxaName.startsWith( "\"" ) && taxaName.endsWith( "\"" ) )
			{
				taxaName = taxaName.substring( 1, taxaName.length() - 1 );
			}
			otuNames.add( taxaName );
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
	 * File suffix appended to normalized OTU tables
	 */
	public static final String NORMAL = "_norm";

	/**
	 * Log 10 display string as 1/2 supported values for: {@value biolockj.Config#REPORT_LOG_BASE}
	 */
	protected static final String LOG_10 = "10";

	/**
	 * Log e display string as 1/2 supported values for: {@value biolockj.Config#REPORT_LOG_BASE}
	 */
	protected static final String LOG_E = "e";
}
