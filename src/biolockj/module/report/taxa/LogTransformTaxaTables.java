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

import java.io.BufferedReader;
import java.io.File;
import java.util.*;
import biolockj.Constants;
import biolockj.Log;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.TaxaUtil;

/**
 * This utility is used to log-transform the raw OTU counts on Log10 or Log-e scales.
 * 
 * @blj.web_desc Log Transform Taxa Tables
 */
public class LogTransformTaxaTables extends NormalizeTaxaTables {
    /**
     * Log transform the data
     *
     * @param otuTable OTU raw count table
     * @throws Exception if unable to construct LogTransformTaxaTables
     */
    @Override
    protected void transform( final File otuTable ) throws Exception {
        final List<List<String>> dataPointsLogged = new ArrayList<>();
        final List<List<Long>> dataPointsUnnormalized = new ArrayList<>();
        final List<String> sampleIDs = new ArrayList<>();
        final List<String> otuNames = new ArrayList<>();
        final BufferedReader reader = BioLockJUtil.getFileReader( otuTable );
        try {
            otuNames.addAll( getOtuNames( reader.readLine() ) );
            String nextLine = reader.readLine();
            while( nextLine != null ) {
                final StringTokenizer st = new StringTokenizer( nextLine, Constants.TAB_DELIM );
                final String sampleID = st.nextToken();
                final List<Long> innerList = new ArrayList<>();
                sampleIDs.add( sampleID );
                dataPointsUnnormalized.add( innerList );
                dataPointsLogged.add( new ArrayList<String>() );

                while( st.hasMoreTokens() ) {
                    final String nextToken = st.nextToken();
                    long d = 0;
                    if( nextToken.length() > 0 ) {
                        d = Long.parseLong( nextToken );
                    }
                    innerList.add( d );
                }

                final long rowSum = innerList.stream().mapToLong( Long::longValue ).sum();

                if( rowSum == 0 ) throw new Exception( sampleID + " has all zeros for table counts." );
                nextLine = reader.readLine();
            }
        } finally {
            if( reader != null ) {
                reader.close();
            }
        }

        final Set<Integer> allZeroIndex = findAllZeroIndex( dataPointsUnnormalized );
        final List<String> filteredSampleIDs = filterZeroSampleIDs( sampleIDs, allZeroIndex );

        Log.debug( getClass(), "# samples with all zeros (to be removed)  = " + allZeroIndex.size() );

        for( int x = 0; x < dataPointsUnnormalized.size(); x++ ) {
            final List<String> loggedInnerList = dataPointsLogged.get( x );
            for( int y = 0; y < dataPointsUnnormalized.get( x ).size(); y++ ) {
                final long val = dataPointsUnnormalized.get( x ).get( y ) + 1;
                if( allZeroIndex.contains( x ) ) {
                    // index 0 = col headers, so add + 1
                    final String id = MetaUtil.getSampleIds().get( x + 1 );
                    Log.warn( getClass(), "All zero row will not be transformed - ID ommitted: " + id );
                } else if( getLogBase().equalsIgnoreCase( LOG_E ) ) {
                    loggedInnerList.add( new Double( Math.log( val ) ).toString() );
                } else if( getLogBase().equalsIgnoreCase( LOG_10 ) ) {
                    loggedInnerList.add( new Double( Math.log10( val ) ).toString() );
                }
            }
        }

        final String level = TaxaUtil.getTaxonomyTableLevel( otuTable );
        Log.debug( getClass(), "Transforming table for level: " + level );
        final File logNormTable = getLogTransformedFile( level );
        writeDataToFile( logNormTable, filteredSampleIDs, otuNames, dataPointsLogged );
    }

    private File getLogTransformedFile( final String level ) throws Exception {
        return TaxaUtil.getTaxonomyTableFile( getOutputDir(), level, "_Log" + getLogBase() );
    }

}
