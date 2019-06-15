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
package biolockj.module.report.taxa;

import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.exception.ConfigFormatException;
import biolockj.util.*;

/**
 * This utility is used to normalize and/or log-transform the raw OTU counts using the formulas:
 * <ul>
 * <li>Normalized OTU count formula = (RC/n)*((SUM(x))/N)+1
 * <li>Relative abundance formula = Log(log_base) [ (RC/n)*((SUM(x))/N)+1 ]
 * </ul>
 * The code implementation supports (log_base = e) and (log_base = 10) which is configured via
 * {@link biolockj.Constants#REPORT_LOG_BASE} property.
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
 * 
 * @blj.web_desc Normalize Taxa Tables
 */
public class NormalizeTaxaTables extends TaxaCountModule {
	/**
	 * Verify {@link biolockj.Config}.{@value biolockj.Constants#REPORT_LOG_BASE} property is valid (if defined) with a
	 * value = (e or 10).
	 *
	 * @throws ConfigFormatException if REPORT_LOG_BASE is not set to a valid option (e or 10)
	 */
	@Override
	public void checkDependencies() throws Exception {
		this.logBase = Config.getString( this, Constants.REPORT_LOG_BASE );
		if( this.logBase != null ) {
			if( !this.logBase.equals( "10" ) && !this.logBase.equals( "e" ) )
				throw new ConfigFormatException( Constants.REPORT_LOG_BASE,
					"Property only accepts value \"10\" or \"e\"" );
			Log.debug( getClass(), "Found logBase: " + this.logBase );
		} else this.logBase = "";
		super.checkDependencies();
	}

	@Override
	public String getSummary() throws Exception {
		if( Config.getString( this, Constants.REPORT_LOG_BASE ) != null )
			this.summary += " Log(" + Config.getString( this, Constants.REPORT_LOG_BASE ) + ")";

		this.summary += " normalized tables";
		return super.getSummary() + this.summary;
	}

	@Override
	public void runModule() throws Exception {
		for( final File file: getInputFiles() )
			transform( file );

		this.summary = "Output " + getOutputDir().listFiles().length;
	}

	/**
	 * Get the Log base (10 or e)
	 * 
	 * @return Log base
	 */
	protected String getLogBase() {
		return this.logBase;
	}

	/**
	 * Populate normalized OTU counts with the formula: (RC/n)*((SUM(x))/N)+1
	 *
	 * @param taxaTable OTU raw count table
	 * @throws Exception if unable to construct NormalizeTaxaTables
	 */
	protected void transform( final File taxaTable ) throws Exception {
		final List<List<String>> dataPointsNormalized = new ArrayList<>();
		final List<List<String>> dataPointsNormalizedThenLogged = new ArrayList<>();
		final List<List<Long>> dataPointsUnnormalized = new ArrayList<>();
		final List<String> sampleIDs = new ArrayList<>();
		final List<String> otuNames = new ArrayList<>();
		long tableSum = 0;

		final BufferedReader reader = BioLockJUtil.getFileReader( taxaTable );
		try {
			otuNames.addAll( getOtuNames( reader.readLine() ) );
			String nextLine = reader.readLine();

			while( nextLine != null ) {
				final StringTokenizer st = new StringTokenizer( nextLine, Constants.TAB_DELIM );
				final String sampleID = st.nextToken();
				final List<Long> innerList = new ArrayList<>();
				sampleIDs.add( sampleID );
				dataPointsUnnormalized.add( innerList );
				dataPointsNormalized.add( new ArrayList<String>() );
				dataPointsNormalizedThenLogged.add( new ArrayList<String>() );

				while( st.hasMoreTokens() ) {
					final String nextToken = st.nextToken();
					long d = 0;
					if( nextToken.length() > 0 ) d = Long.parseLong( nextToken );
					innerList.add( d );
				}

				final long rowSum = innerList.stream().mapToLong( Long::longValue ).sum();
				tableSum += rowSum;
				if( rowSum == 0 ) throw new Exception( sampleID + " has all zeros for table counts." );
				nextLine = reader.readLine();

				Log.debug( getClass(), "Row Sum [" + sampleIDs.size() + "] = " + rowSum );
			}
		} finally {
			if( reader != null ) reader.close();
		}

		Log.debug( getClass(), "Table Sum [ #samples=" + sampleIDs.size() + "] = " + tableSum );

		final Set<Integer> allZeroIndex = findAllZeroIndex( dataPointsUnnormalized );
		final List<String> filteredSampleIDs = filterZeroSampleIDs( sampleIDs, allZeroIndex );
		final double aveRowSum = (double) tableSum / (double) filteredSampleIDs.size();

		Log.debug( getClass(), "Final Table Sum = " + tableSum );
		Log.debug( getClass(), "Average Row Sum = " + aveRowSum );
		Log.debug( getClass(), "# samples with all zeros (to be removed)  = " + allZeroIndex.size() );

		for( int x = 0; x < dataPointsUnnormalized.size(); x++ ) {
			final long rowSum = dataPointsUnnormalized.get( x ).stream().mapToLong( Long::longValue ).sum();
			final List<String> loggedInnerList = dataPointsNormalizedThenLogged.get( x );

			for( int y = 0; y < dataPointsUnnormalized.get( x ).size(); y++ ) {
				final Double normVal =
					aveRowSum * (double) dataPointsUnnormalized.get( x ).get( y ) / ( (double) rowSum + 1 );
				dataPointsNormalized.get( x ).add( new Long( normVal.longValue() ).toString() );
				if( allZeroIndex.contains( x ) ) {
					// index 0 = col headers, so add + 1
					final String id = MetaUtil.getSampleIds().get( x + 1 );
					Log.warn( getClass(), "All zero row will not be transformed - ID removed: " + id );
				} else if( getLogBase().equalsIgnoreCase( LOG_E ) )
					loggedInnerList.add( new Double( Math.log( normVal ) ).toString() );
				else if( getLogBase().equalsIgnoreCase( LOG_10 ) )
					loggedInnerList.add( new Double( Math.log10( normVal ) ).toString() );
			}
		}

		File normOutDir = getOutputDir();
		final String level = TaxaUtil.getTaxonomyTableLevel( taxaTable );
		Log.debug( getClass(), "Normalizing table for level: " + level );
		if( !getLogBase().isEmpty() ) {
			normOutDir = getTempDir();
			final File logNormTable = getLogTransformedFile( level );
			writeDataToFile( logNormTable, filteredSampleIDs, otuNames, dataPointsNormalizedThenLogged );
		}

		final File normTable = TaxaUtil.getTaxonomyTableFile( normOutDir, level, TaxaUtil.NORMALIZED );
		writeDataToFile( normTable, filteredSampleIDs, otuNames, dataPointsNormalized );
	}

	private File getLogTransformedFile( final String level ) throws Exception {
		return TaxaUtil.getTaxonomyTableFile( getOutputDir(), level, TaxaUtil.NORMALIZED + "_Log" + getLogBase() );
	}

	/**
	 * Filter Sample IDs with all zero rows
	 * 
	 * @param sampleIDs List of Sample IDs
	 * @param allZeroIndex Table row index for all-zero rows
	 * @return Filtered list of Sample IDs
	 */
	protected static List<String> filterZeroSampleIDs( final List<String> sampleIDs, final Set<Integer> allZeroIndex ) {
		final List<String> zeroSampleIDs = getNonZeroSampleIDs( sampleIDs, allZeroIndex );
		for( final String id: zeroSampleIDs )
			sampleIDs.remove( id );
		return sampleIDs;
	}

	/**
	 * Return the table index for rows with all zer count values
	 * 
	 * @param data List of table rows
	 * @return Set of empty zero-rows
	 */
	protected static Set<Integer> findAllZeroIndex( final List<List<Long>> data ) {
		final Set<Integer> allZero = new HashSet<>();
		for( int x = 0; x < data.size(); x++ )
			for( int y = 0; y < data.get( x ).size(); y++ ) {
				long sum = 0;
				for( final Long d: data.get( x ) )
					sum += d;
				if( sum == 0 ) allZero.add( x );
			}
		return allZero;
	}

	/**
	 * Parse Taxa names from the given header line.
	 * 
	 * @param header Head line of table
	 * @return List of Taxa
	 */
	protected static List<String> getOtuNames( final String header ) {
		final List<String> otuNames = new ArrayList<>();
		final StringTokenizer st = new StringTokenizer( header, Constants.TAB_DELIM );
		st.nextToken(); // skip ID & then strip quotes
		while( st.hasMoreTokens() )
			otuNames.add( BioLockJUtil.removeOuterQuotes( st.nextToken() ) );
		return otuNames;
	}

	/**
	 * Write transformed data to file
	 * 
	 * @param inputFile Count table
	 * @param sampleNames Sample
	 * @param taxaNames Taxa names
	 * @param taxaCounts Taxa counts
	 * @throws Exception if errors occur
	 */
	protected static void writeDataToFile( final File inputFile, final List<String> sampleNames,
		final List<String> taxaNames, final List<List<String>> taxaCounts ) throws Exception {
		final BufferedWriter writer = new BufferedWriter( new FileWriter( inputFile ) );

		writer.write( MetaUtil.getID() );

		for( final String s: taxaNames )
			writer.write( Constants.TAB_DELIM + s );

		writer.write( Constants.RETURN );

		final int size = sampleNames.size();
		for( int x = 0; x < size; x++ ) {
			writer.write( sampleNames.get( x ) );

			for( int y = 0; y < taxaNames.size(); y++ )
				writer.write( Constants.TAB_DELIM + taxaCounts.get( x ).get( y ) );

			if( x + 1 != size ) writer.write( Constants.RETURN );
		}

		writer.close();
	}

	private static List<String> getNonZeroSampleIDs( final List<String> sampleIDs, final Set<Integer> allZeroIndex ) {
		final List<String> zeroSampleIDs = new ArrayList<>();
		for( final Integer i: allZeroIndex )
			zeroSampleIDs.add( sampleIDs.get( i ) );

		return zeroSampleIDs;
	}

	private String logBase = "";
	private String summary = "";

	/**
	 * Log 10 display string as 1/2 supported values for: {@value biolockj.Constants#REPORT_LOG_BASE}
	 */
	protected static final String LOG_10 = "10";

	/**
	 * Log e display string as 1/2 supported values for: {@value biolockj.Constants#REPORT_LOG_BASE}
	 */
	protected static final String LOG_E = "e";
}
