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
package biolockj.module.report.otu;

import java.io.*;
import java.util.*;
import biolockj.Log;
import biolockj.util.BioLockJUtil;
import biolockj.util.MetaUtil;
import biolockj.util.OtuUtil;

/**
 * This BioModule compiles the counts from all OTU count files into a single summary OTU count file containing OTU
 * counts for the entire dataset.
 * 
 * @blj.web_desc Compile OTU Counts
 */
public class CompileOtuCounts extends OtuCountModule {

    /**
     * Add summary with unique OTU counts/level.
     */
    @Override
    public String getSummary() throws Exception {
        String msg = "# Samples:     "
            + BioLockJUtil.formatNumericOutput( new Integer( MetaUtil.getSampleIds().size() ).longValue(), false )
            + RETURN;
        long uniqueOtus = 0;
        long totalOtus = 0;
        BufferedReader reader = null;
        try {
            reader = BioLockJUtil.getFileReader( getSummaryOtuFile() );
            for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
                final OtuUtil.OtuCountLine otuLine = new OtuUtil.OtuCountLine( line );
                uniqueOtus++;
                totalOtus += otuLine.getCount();
            }
        } finally {
            if( reader != null ) {
                reader.close();
            }
        }

        msg += "# Unique OTUs: " + BioLockJUtil.formatNumericOutput( uniqueOtus, false ) + RETURN;
        msg += "# Total OTUs:  " + BioLockJUtil.formatNumericOutput( totalOtus, false ) + RETURN;
        msg += getMinOtusPerSample() + RETURN;
        msg += getMaxOtusPerSample() + RETURN;
        return super.getSummary() + msg;
    }

    @Override
    public void runModule() throws Exception {
        buildSummaryOtuCountFile( compileOtuCounts( getInputFiles() ) );
    }

    /**
     * Build Summary OTU count file for all samples.
     *
     * @param otuCounts OTU-count mapping
     * @throws Exception if errors occur
     */
    protected void buildSummaryOtuCountFile( final TreeMap<String, Long> otuCounts ) throws Exception {
        final File otuCountFile = OtuUtil.getOtuCountFile( getOutputDir(), null, SUMMARY );
        Log.info( getClass(),
            "Build " + otuCountFile.getAbsolutePath() + " from " + otuCounts.size() + " unqiue OTU strings" );
        final BufferedWriter writer = new BufferedWriter( new FileWriter( otuCountFile ) );
        try {
            for( final String otu: otuCounts.keySet() ) {
                writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
            }
        } finally {
            writer.close();
        }
    }

    /**
     * Compile OTU counts from the individual sample OTU count files
     *
     * @param files Collection of OTU count files
     * @return TreeMap(OTU, count)
     * @throws Exception if errors occur
     */
    protected TreeMap<String, Long> compileOtuCounts( final Collection<File> files ) throws Exception {
        final TreeMap<String, Long> combinedOtuCounts = new TreeMap<>();
        for( final File file: files ) {
            final TreeMap<String, Long> otuCounts = OtuUtil.compileSampleOtuCounts( file );
            this.uniqueOtuPerSample.put( OtuUtil.getSampleId( file ), new Integer( otuCounts.size() ).longValue() );
            for( final String otu: otuCounts.keySet() ) {
                final Long count = otuCounts.get( otu );

                if( !combinedOtuCounts.keySet().contains( otu ) ) {
                    combinedOtuCounts.put( otu, 0L );
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
    protected String getMaxOtusPerSample() {
        final TreeSet<String> ids = new TreeSet<>();
        long max = 0L;
        for( final String sampleId: this.uniqueOtuPerSample.keySet() ) {
            if( this.uniqueOtuPerSample.get( sampleId ) == max ) {
                ids.add( sampleId );
            } else if( this.uniqueOtuPerSample.get( sampleId ) > max ) {
                ids.clear();
                ids.add( sampleId );
                max = this.uniqueOtuPerSample.get( sampleId );
            }
        }
        return "Max # Unique OTUs[ " + BioLockJUtil.formatNumericOutput( max, false ) + " ]: "
            + BioLockJUtil.getCollectionAsString( ids );
    }

    /**
     * Find the minimum OTU count per sample.
     *
     * @return Min OTU per sample.
     */
    protected String getMinOtusPerSample() {
        final TreeSet<String> ids = new TreeSet<>();
        Long min = null;
        for( final String sampleId: this.uniqueOtuPerSample.keySet() ) {
            if( min == null || this.uniqueOtuPerSample.get( sampleId ) == min ) {
                ids.add( sampleId );
                min = this.uniqueOtuPerSample.get( sampleId );
            } else if( this.uniqueOtuPerSample.get( sampleId ) < min ) {
                ids.clear();
                ids.add( sampleId );
                min = this.uniqueOtuPerSample.get( sampleId );
            }
        }
        return "Min # Unique OTUs[ " + BioLockJUtil.formatNumericOutput( min, false ) + " ]: "
            + BioLockJUtil.getCollectionAsString( ids );
    }

    /**
     * Get the summary output file
     *
     * @return OTU summary file
     * @throws Exception if errors occur
     */
    protected File getSummaryOtuFile() throws Exception {
        return OtuUtil.getOtuCountFile( getOutputDir(), null, SUMMARY );
    }

    private final Map<String, Long> uniqueOtuPerSample = new HashMap<>();

    /**
     * Output file prefix: {@value #SUMMARY}
     */
    public static final String SUMMARY = "summary";
}
