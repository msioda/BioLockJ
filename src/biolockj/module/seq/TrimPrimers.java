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
package biolockj.module.seq;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import biolockj.*;
import biolockj.module.JavaModuleImpl;
import biolockj.module.SeqModule;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.util.*;

/**
 * This BioModule removes sequence primers from demultiplexed files.<br>
 * The primers are defined using regular expressions in a separate file.
 * 
 * @blj.web_desc Trim Primers
 */
public class TrimPrimers extends JavaModuleImpl implements SeqModule {

    /**
     * Validates the file that defines the REGEX primers. If primers are located at start of read, add REGEX line anchor
     * "^" to the start of the primer sequence in {@value biolockj.Constants#INPUT_TRIM_SEQ_FILE}.
     * <ul>
     * <li>If seqs are multiplexed, {@link biolockj.module.implicit.Demultiplexer} must run as a prerequisite module
     * <li>If seqs are paired-end reads, and {@link biolockj.module.seq.PearMergeReads} is configured before this
     * module, {@value biolockj.Constants#INPUT_TRIM_SEQ_FILE} must contain the reverse compliment of the reverse primer with a REGEX line
     * anchor "$" at the end.
     * </ul>
     */
    @Override
    public void checkDependencies() throws Exception {
        super.checkDependencies();

        if( DockerUtil.inDockerEnv() ) {
            Config.requireString( null, Constants.INPUT_TRIM_SEQ_FILE );
        } else {
            Config.requireExistingFile( null, Constants.INPUT_TRIM_SEQ_FILE );
        }
    }

    /**
     * Set {@value #NUM_TRIMMED_READS} as the number of reads field.
     */
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        RegisterNumReads.setNumReadFieldName( getMetaColName() );
    }

    @Override
    public List<File> getSeqFiles( final Collection<File> files ) throws Exception {
        return SeqUtil.getSeqFiles( files );
    }

    /**
     * Output the summary messages generated by the module.
     */
    @Override
    public String getSummary() throws Exception {
        final StringBuffer sb = new StringBuffer();
        try {
            sb.append( "Primer file: " + Config.requireString( this, Constants.INPUT_TRIM_SEQ_FILE ) );
            for( final String msg: summaryMsgs ) {
                sb.append( msg + RETURN );
            }

            return sb.toString() + super.getSummary();

        } catch( final Exception ex ) {
            Log.warn( getClass(), "Unable to complete module summary! " + ex.getMessage() );
        }

        return super.getSummary();
    }

    /**
     * Trims primers from fasta or fastq files. Saves messages for summary email with metrics on best/worst and average
     * files. Just a quick implementation on reporting metrics is working but code needs clean-up and simplification.
     */
    @Override
    public void runModule() throws Exception {
        BioLockJ.copyFileToPipelineRoot( getSeqPrimerFile() );
        trimSeqs();
        Log.debug( getClass(), "numLinesPerRead = " + SeqUtil.getNumLinesPerRead() );
        Log.debug( getClass(), "#samples in table numLinesWithPrimer = " + this.numLinesWithPrimer.size() );
        Log.debug( getClass(), "#samples in table numLinesNoPrimer = " + this.numLinesNoPrimer.size() );
        Log.debug( getClass(), "#samples in table validReadsPerFile = " + this.seqsWithPrimersTrimmed.size() );
        Log.debug( getClass(), "#seq files = " + this.seqs.size() );

        addBadFilesToSummary();

        if( Config.getBoolean( this, INPUT_REQUIRE_PRIMER ) ) {
            Log.warn( getClass(), INPUT_REQUIRE_PRIMER + "=Y so any sequences without a primer have been discarded" );
        }

        long totalPrimerR = 0L;
        long totalNoPrimerR = 0L;
        long totalPrimerF = 0L;
        long totalNoPrimerF = 0L;
        long totalValid = 0L;
        double maxFw = 0.0;
        double minFw = 0.0;
        double maxRv = 0.0;
        double minRv = 0.0;
        String maxFileFw = null;
        String minFileFw = null;
        String maxFileRv = null;
        String minFileRv = null;
        String maxFwDisplay = null;
        String minFwDisplay = null;
        String maxRvDisplay = null;
        String minRvDisplay = null;

        final TreeSet<File> files = new TreeSet<>( this.seqsWithPrimersTrimmed.keySet() );
        for( final File f: files ) {
            Long v = this.seqsWithPrimersTrimmed.get( f );
            Long a = this.numLinesWithPrimer.get( f.getAbsolutePath() );
            Long b = this.numLinesNoPrimer.get( f.getAbsolutePath() );

            if( v == null ) {
                v = 0L;
            }
            if( a == null ) {
                a = 0L;
            }
            if( b == null ) {
                b = 0L;
            }

            totalValid += v;

            double ratio = Double.valueOf( this.df.format( 100 * ( (double) a / ( a + b ) ) ) );
            String per = BioLockJUtil.formatPercentage( a, a + b );

            if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) ) {
                ratio = Double.valueOf( this.df.format( 100 * ( (double) v / ( a + b ) ) ) );
                per = BioLockJUtil.formatPercentage( v, a + b );
                Log.info( getClass(),
                    f.getName() + " Paired reads with primer in both -- " + v + "/" + ( a + b ) + " = " + per );
            } else {
                Log.info( getClass(),
                    f.getName() + " Reads with primer ----------------- " + a + "/" + ( a + b ) + " = " + per );
            }

            Log.debug( getClass(), "file = " + f.getAbsolutePath() );
            Log.debug( getClass(), "maxFw = " + maxFw );
            Log.debug( getClass(), "minFw = " + minFw );
            Log.debug( getClass(), "ratio = " + ratio );
            Log.debug( getClass(), "maxFw < ratio = " + ( maxFw < ratio ) );
            Log.debug( getClass(), "minFw > ratio = " + ( minFw > ratio ) );

            if( this.foundPaired && !SeqUtil.isForwardRead( f.getName() ) ) {
                if( maxRvDisplay == null || maxRv < ratio ) {
                    maxRv = ratio;
                    maxRvDisplay = per;
                    maxFileRv = f.getAbsolutePath();
                    Log.info( getClass(),
                        "Found new: Max % reads kept in Reverse Read = " + maxFileRv + " = " + maxRvDisplay );
                }

                if( minRvDisplay == null || minRv > ratio ) {
                    minRv = ratio;
                    minRvDisplay = per;
                    minFileRv = f.getAbsolutePath();
                    Log.info( getClass(),
                        "Found new: Min % reads kept in Reverse Read = " + minFileRv + " = " + minRvDisplay );
                }

                totalPrimerR += a;
                totalNoPrimerR += b;
            } else {
                if( maxFwDisplay == null || maxFw < ratio ) {
                    maxFw = ratio;
                    maxFwDisplay = per;
                    maxFileFw = f.getAbsolutePath();
                    Log.info( getClass(),
                        "Found new: Max % reads kept in Forward Read = " + maxFileFw + " = " + maxFwDisplay );
                }

                if( minFwDisplay == null || minFw > ratio ) {
                    minFw = ratio;
                    minFwDisplay = per;
                    minFileFw = f.getAbsolutePath();
                    Log.info( getClass(),
                        "Found new: Min % reads kept in Forward Read = " + minFileFw + " = " + minFwDisplay );
                }

                totalPrimerF += a;
                totalNoPrimerF += b;
            }
        }

        if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) ) {
            summaryMsgs.add( "Max % reads kept in Forward Read = " + maxFileFw + " = " + maxFwDisplay );
            Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );
            summaryMsgs.add( "Min % reads kept in Forward Read = " + minFileFw + " = " + minFwDisplay );
            Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );

            summaryMsgs.add( "Max % reads kept in Reverse Read = " + maxFileRv + " = " + maxRvDisplay );
            Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );
            summaryMsgs.add( "Min % reads kept in Reverse Read = " + minFileRv + " = " + minRvDisplay );
            Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );
        } else {
            summaryMsgs.add( "Max % reads kept in = " + maxFileFw + " = " + maxFwDisplay );
            Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );
            summaryMsgs.add( "Min % reads kept in = " + minFileFw + " = " + minFwDisplay );
            Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );
        }

        final long totalF = totalPrimerF + totalNoPrimerF;
        final long totalR = totalPrimerR + totalNoPrimerR;
        final long totalNoPrimer = totalNoPrimerF + totalNoPrimerR;
        final long totalWithPrimer = totalPrimerF + totalPrimerR;
        final long total = totalNoPrimer + totalWithPrimer;
        if( total > 1 ) {

            summaryMsgs.add( "Mean % reads with primer = " + totalWithPrimer + "/" + total + " = "
                + BioLockJUtil.formatPercentage( totalWithPrimer, total ) );

            Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );

            if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) ) {
                summaryMsgs.add( "Mean % Forward reads with primer = " + totalPrimerF + "/" + totalF + " = "
                    + BioLockJUtil.formatPercentage( totalPrimerF, totalF ) );
                Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );
                summaryMsgs.add( "Mean % Reverse reads with primer  = " + totalPrimerR + "/" + totalR + " = "
                    + BioLockJUtil.formatPercentage( totalPrimerR, totalR ) );
                Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );
                if( !Config.getBoolean( this, INPUT_REQUIRE_PRIMER ) ) {
                    summaryMsgs.add( "Mean % Paired reads with matching primer = " + totalValid + "/" + total + " = "
                        + BioLockJUtil.formatPercentage( totalValid, total ) );
                }

                Log.info( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );
            }
        }

        MetaUtil.addColumn( getMetaColName(), getValidReadsPerSample(), getOutputDir(), true );
    }

    /**
     * Update summaryMsgs with list of seq files that contained no reads with a valid primer.
     */
    protected void addBadFilesToSummary() {
        final TreeSet<File> files = new TreeSet<>( this.seqsWithPrimersTrimmed.keySet() );
        for( final File seq: this.seqs ) {
            boolean found = false;
            for( final File f: files ) {
                Log.debug( getClass(), "Looking for primers in:" + f.getAbsolutePath() );
                if( seq.getAbsolutePath().equals( f.getAbsolutePath() ) ) {
                    Log.debug( getClass(), "Bad seq file:" + f.getAbsolutePath() );
                    found = true;
                    break;
                }
            }
            if( !found ) {
                summaryMsgs.add( "No valid primers found in: " + seq.getAbsolutePath() );
                Log.warn( getClass(), summaryMsgs.get( summaryMsgs.size() - 1 ) );
            }
        }
    }

    /**
     * Get the primers listed in the {@value biolockj.Constants#INPUT_TRIM_SEQ_FILE}. If a merged fw primer (starting with ^) and a merged
     * rv primer (ending with $) are found set mergedReadTwoPrimers = true to enforce reads must have both primers if
     * discarding reads without valid primers
     *
     * @return Set of primers
     * @throws Exception if unable to read the file
     */
    protected Set<String> getPrimers() throws Exception {
        boolean fwMergePrimerFound = false;
        boolean rvMergePrimerFound = false;
        final Set<String> primers = new HashSet<>();
        final File trimSeqFile = getSeqPrimerFile();
        final BufferedReader reader = BioLockJUtil.getFileReader( trimSeqFile );
        try {
            for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
                if( !line.startsWith( "#" ) ) {
                    final String seq = line.trim().toUpperCase();
                    if( seq.length() > 0 ) {
                        Log.info( getClass(), "Found primer to trim: " + seq );
                        String regexSeq = "";

                        for( int i = 1; i <= seq.length(); i++ ) {
                            final String base = seq.substring( i - 1, i );
                            final String iupac = SeqUtil.getIupacBase( base );
                            regexSeq = regexSeq + iupac;
                            if( !base.equals( iupac ) ) {
                                if( !substitutions.contains( base ) ) {
                                    Log.info( getClass(), "IUPAC substitution of base: " + base + " to: " + iupac );
                                    substitutions.add( base );
                                }
                            }
                        }

                        if( regexSeq.startsWith( "^" ) ) {
                            fwMergePrimerFound = true;
                        } else if( seq.endsWith( "$" ) ) {
                            rvMergePrimerFound = true;
                        } else throw new Exception(
                            "INVALID PRIMER!  Primers must start with \"^\" or end with \"$\"  Update primer file: "
                                + trimSeqFile.getAbsolutePath() );

                        primers.add( regexSeq );

                    }
                }
            }

            if( fwMergePrimerFound && rvMergePrimerFound ) {
                this.mergedReadTwoPrimers = true;
            }
        } finally {
            if( reader != null ) {
                reader.close();
            }
        }
        if( primers.size() < 1 ) throw new Exception( "No primers found in: " + trimSeqFile.getAbsolutePath() );

        return primers;
    }

    private String getMetaColName() throws Exception {
        if( this.otuColName == null ) {
            this.otuColName = MetaUtil.getSystemMetaCol( this, NUM_TRIMMED_READS );
        }

        return this.otuColName;
    }

    private String getTrimFilePath( final File file ) throws Exception {
        return getOutputDir().getAbsolutePath() + File.separator + SeqUtil.getSampleId( file.getName() )
            + SeqUtil.getReadDirectionSuffix( file ) + "." + SeqUtil.getSeqType();
    }

    private Set<String> getValidHeaders( final File file, final Set<String> primers ) throws Exception {
        final Set<String> validHeaders = new HashSet<>();
        final BufferedReader reader = BioLockJUtil.getFileReader( file );
        int lineCounter = 1;
        String header = null;
        try {
            for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
                line = line.trim();
                if( lineCounter % SeqUtil.getNumLinesPerRead() == 1 ) {
                    header = SeqUtil.getHeader( line );
                } else if( lineCounter % SeqUtil.getNumLinesPerRead() == 2 ) {
                    boolean foundHeader = false;
                    for( final String seq: primers ) {
                        final int seqLength = line.length();
                        line = line.replaceFirst( seq, "" );
                        if( seqLength != line.length() ) {
                            foundHeader = true;
                        }
                    }

                    if( foundHeader ) {
                        if( validHeaders.contains( header ) )
                            throw new Exception( "NON-FATAL Exception: Duplicate header: " + header );

                        validHeaders.add( header );
                    }
                }
                lineCounter++;
            }

            Log.info( getClass(), file.getName() + " # valid headers = " + validHeaders.size() );

        } finally {
            reader.close();
        }

        return validHeaders;
    }

    private Map<String, String> getValidReadsPerSample() throws Exception {
        if( !MetaUtil.getFieldNames().contains( NUM_TRIMMED_READS ) && this.validReadsPerSample.isEmpty() ) {
            for( final File f: this.seqsWithPrimersTrimmed.keySet() ) {
                if( !Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS )
                    || SeqUtil.isForwardRead( f.getName() ) ) {
                    this.validReadsPerSample.put( SeqUtil.getSampleId( f.getName() ),
                        Long.toString( this.seqsWithPrimersTrimmed.get( f ) ) );
                }
            }
        }
        return this.validReadsPerSample;
    }

    private void printReports( final Map<String, Map<String, String>> missingPrimers, final String reportLabel )
        throws Exception {
        if( !missingPrimers.isEmpty() ) {
            for( final String key: missingPrimers.keySet() ) {
                final Map<String, String> map = missingPrimers.get( key );
                Log.warn( getClass(), "TrimPrimers " + key + " # " + reportLabel + " = " + map.size() );

                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter( new FileWriter( new File(
                        getTempDir().getAbsolutePath() + File.separator + key + "_" + reportLabel + TXT_EXT ) ) );
                    for( final String header: map.keySet() ) {
                        writer.write( header + RETURN );
                        writer.write( map.get( header ) + RETURN );
                    }
                } catch( final Exception ex ) {
                    Log.error( getClass(), "Error occurred writng primer report" + ex.getMessage(), ex );
                } finally {
                    if( writer != null ) {
                        writer.close();
                    }
                }
            }
        } else if( this.mergedReadTwoPrimers ) {
            Log.warn( getClass(), "TrimPrimers # " + reportLabel + " = 0" );
        }
    }

    private void processFile( final File file, final Set<String> primers ) throws Exception {
        processFile( file, new HashSet<>(), primers );
    }

    private void processFile( final File file, final Set<String> validHeaders, final Set<String> primers )
        throws Exception {
        Log.info( getClass(), "Processing file = " + file.getAbsolutePath() );
        this.seqs.add( file );

        if( !SeqUtil.isForwardRead( file.getName() ) ) {
            this.foundPaired = true;
        }

        final File trimmedFile = new File( getTrimFilePath( file ) );
        Log.info( getClass(), "Create trimmed file = " + trimmedFile.getAbsolutePath() );

        final BufferedReader reader = BioLockJUtil.getFileReader( file );
        final BufferedWriter writer = new BufferedWriter( new FileWriter( trimmedFile ) );
        try {
            int fwPrimerLength = 0;
            int rvPrimerLength = 0;
            final List<String> seqLines = new ArrayList<>();
            boolean found = false;

            String origSequence = "";
            for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
                if( seqLines.size() == 1 ) {
                    origSequence = line;
                    found = false;
                    for( final String seq: primers ) {
                        if( line.replaceFirst( seq, "" ).length() != line.length() ) {
                            if( seq.startsWith( "^" ) ) {
                                if( fwPrimerLength != 0 ) throw new Exception(
                                    "INVALID SEQ!  Read contains 2 forward primers!  " + origSequence );

                                fwPrimerLength = line.length() - line.replaceFirst( seq, "" ).length();
                            } else if( seq.endsWith( "$" ) ) {
                                if( rvPrimerLength != 0 ) throw new Exception(
                                    "INVALID SEQ!  Read contains 2 reverse primers!  " + origSequence );

                                rvPrimerLength = line.length() - line.replaceFirst( seq, "" ).length();
                            } else throw new Exception(
                                "INVALID PRIMER!  Primers must start with \"^\" or end with \"$\"" );

                            line = line.replaceFirst( seq, "" );

                            if( this.mergedReadTwoPrimers && fwPrimerLength < 1 && rvPrimerLength < 1 ) {
                                // Log.warn( getClass(), "Read missing BOTH primers " + origSequence );
                                if( this.missingBothPrimers.get( file.getName() ) == null ) {
                                    final Map<String, String> m = new HashMap<>();
                                    m.put( seqLines.get( 0 ), origSequence );
                                    this.missingBothPrimers.put( file.getName(), m );
                                } else {
                                    this.missingBothPrimers.get( file.getName() ).put( seqLines.get( 0 ),
                                        origSequence );
                                }
                            } else if( this.mergedReadTwoPrimers && fwPrimerLength < 1 ) {
                                Log.debug( getClass(), "Read missing forward primer " + origSequence );
                                if( this.missingFwPrimers.get( file.getName() ) == null ) {
                                    final Map<String, String> m = new HashMap<>();
                                    m.put( seqLines.get( 0 ), origSequence );
                                    this.missingFwPrimers.put( file.getName(), m );
                                } else {
                                    this.missingFwPrimers.get( file.getName() ).put( seqLines.get( 0 ), origSequence );
                                }

                            } else if( this.mergedReadTwoPrimers && rvPrimerLength < 1 ) {
                                Log.debug( getClass(), "Read missing reverse primer " + origSequence );
                                if( this.missingRvPrimers.get( file.getName() ) == null ) {
                                    final Map<String, String> m = new HashMap<>();
                                    m.put( seqLines.get( 0 ), origSequence );
                                    this.missingRvPrimers.put( file.getName(), m );
                                } else {
                                    this.missingRvPrimers.get( file.getName() ).put( seqLines.get( 0 ), origSequence );
                                }
                            } else {
                                found = true;
                            }
                        }
                    }

                    if( found ) {
                        final Long x = this.numLinesWithPrimer.get( file.getAbsolutePath() );
                        this.numLinesWithPrimer.put( file.getAbsolutePath(), x == null ? 1L: x + 1L );
                    } else {
                        final Long x = this.numLinesNoPrimer.get( file.getAbsolutePath() );
                        this.numLinesNoPrimer.put( file.getAbsolutePath(), x == null ? 1L: x + 1L );
                    }
                } else if( seqLines.size() == 3 ) {
                    if( fwPrimerLength > 0 ) {
                        line = line.substring( fwPrimerLength );
                    }
                    if( rvPrimerLength > 0 ) {
                        line = line.substring( 0, line.length() - rvPrimerLength );
                    }
                }

                seqLines.add( line );

                if( seqLines.size() == SeqUtil.getNumLinesPerRead() ) {
                    final boolean validRecord = found && ( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS )
                        ? validHeaders.contains( seqLines.get( 0 ) )
                        : true );

                    if( !Config.getBoolean( this, INPUT_REQUIRE_PRIMER ) || validRecord ) {
                        final Long x = this.seqsWithPrimersTrimmed.get( file );
                        this.seqsWithPrimersTrimmed.put( file, x == null ? 1L: x + 1L );

                        for( int j = 0; j < SeqUtil.getNumLinesPerRead(); j++ ) {
                            writer.write( seqLines.get( j ) + RETURN );
                        }
                    }
                    fwPrimerLength = 0;
                    rvPrimerLength = 0;
                    seqLines.clear();
                }
            }
        } catch( final Exception ex ) {
            Log.error( getClass(), "Error removing primers from file = " + file.getAbsolutePath(), ex );
        } finally {
            reader.close();
            writer.close();
        }
    }
    
    private List<File> getFwReads( Map<File, File> pairedReads  ) throws Exception {
        if( pairedReads != null ) return new ArrayList<>( pairedReads.keySet() );
        return getInputFiles();
    }

    private void trimSeqs() throws Exception {
        final Set<String> primers = getPrimers();
        final boolean hasPairedReads = Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS );
        final Map<File, File> pairedReads = hasPairedReads ? SeqUtil.getPairedReads( getInputFiles() ) : null;
        final List<File> files = getFwReads( pairedReads );
        if( files == null ) throw new Exception( "Failed to obtain input sequence files" );
        final int count = files.size();
        int i = 0;
        Log.info( getClass(), "Trimming primers from " + ( hasPairedReads ? 2 * count: count ) + " files..." );
        for( final File file: files ) {
            final Set<String> validReads = getValidHeaders( file, primers );
            if( pairedReads != null ) {
                validReads.retainAll( getValidHeaders( pairedReads.get( file ), primers ) );
                processFile( file, validReads, primers );
                processFile( pairedReads.get( file ), validReads );
            } else {
                processFile( file, primers );
            }

            if( ( i++ + 1 ) % 25 == 0 ) {
                Log.info( getClass(),
                    "Done trimming " + i + "/" + count + ( hasPairedReads ? " file pairs": " files" ) );
            }
        }

        Log.info( getClass(), "Done trimming " + i + "/" + count + ( hasPairedReads ? " file pairs": " files" ) );

        printReports( this.missingBothPrimers, "missingBothPrimers" );
        printReports( this.missingFwPrimers, "missingFwPrimers" );
        printReports( this.missingRvPrimers, "missingRvPrimers" );
    }

    /**
     * Return the primer file.
     * 
     * @return File with sequence primers
     * @throws Exception if errors occur
     */
    public static File getSeqPrimerFile() throws Exception {
        if( DockerUtil.inDockerEnv() )
            return DockerUtil.getDockerVolumeFile( Constants.INPUT_TRIM_SEQ_FILE, DockerUtil.CONTAINER_PRIMER_DIR );
        return Config.requireExistingFile( null, Constants.INPUT_TRIM_SEQ_FILE );
    }

    private final DecimalFormat df = new DecimalFormat( "##.##" );

    private boolean foundPaired = false;

    private boolean mergedReadTwoPrimers = false;

    private final Map<String, Map<String, String>> missingBothPrimers = new HashMap<>();

    private final Map<String, Map<String, String>> missingFwPrimers = new HashMap<>();
    private final Map<String, Map<String, String>> missingRvPrimers = new HashMap<>();
    private final Map<String, Long> numLinesNoPrimer = new HashMap<>();
    private final Map<String, Long> numLinesWithPrimer = new HashMap<>();
    private String otuColName = null;
    private final Set<File> seqs = new HashSet<>();
    private final Map<File, Long> seqsWithPrimersTrimmed = new HashMap<>();
    private final Map<String, String> validReadsPerSample = new HashMap<>();

    /**
     * Metadata column name for column that holds number of trimmed reads per sample: {@value #NUM_TRIMMED_READS}
     */
    public static final String NUM_TRIMMED_READS = "Num_Trimmed_Reads";

    /**
     * {@link biolockj.Config} property {@value #INPUT_REQUIRE_PRIMER} is a boolean used to determine if sequences
     * without a primer should be kept or discarded
     */
    protected static final String INPUT_REQUIRE_PRIMER = "trimPrimers.requirePrimer";

    private static Set<String> substitutions = new HashSet<>();
    private static final List<String> summaryMsgs = new ArrayList<>();
}
