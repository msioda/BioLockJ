/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.classifier.wgs;

import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.*;

/**
 * This BioModule assigns taxonomy to WGS sequences and translates the results into mpa-format. Command line options are
 * defined in the online manual: <a href="http://ccb.jhu.edu/software/kraken/MANUAL.html" target=
 * "_top">http://ccb.jhu.edu/software/kraken/MANUAL.html</a>
 * 
 * @blj.web_desc Kraken Classifier
 */
public class KrakenClassifier extends ClassifierModuleImpl {

	/**
	 * Build bash script lines to classify unpaired WGS reads with Kraken. The inner list contains 2 bash script lines
	 * used to classify 1 sample.
	 * <p>
	 * Example lines:
	 * <ol>
	 * <li>kraken --fasta-input --only-classified-output --threads 8 --db /database/kraken --output
	 * ./temp/sample42_kraken.txt ./input/sample42.fasta<br>
	 * <li>kraken-translate --db /database/kraken --mpa-format ./temp/sample42_kraken.txt &gt;
	 * ./output/sample42_reported.tsv
	 * </ol>
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files ) {
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String tempFile = getTempDir().getAbsolutePath() + File.separator + fileId + KRAKEN_FILE;
			final String krakenOutput =
				getOutputDir().getAbsolutePath() + File.separator + fileId + Constants.PROCESSED;
			final ArrayList<String> lines = new ArrayList<>( 2 );
			lines.add( FUNCTION_KRAKEN + " " + tempFile + " " + file.getAbsolutePath() );
			lines.add( FUNCTION_TRANSLATE + " " + tempFile + " " + krakenOutput );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Build bash script lines to classify paired WGS reads with Kraken. The inner list contains 2 bash script lines
	 * used to classify 1 sample (2 files: forward and reverse reads).
	 * <p>
	 * Example lines:
	 * <ol>
	 * <li>kraken --paired --fasta-input --only-classified-output --threads 8 --db /database/kraken --output
	 * ./temp/sample42_kraken.txt ./input/sample42_R1.fasta ./input/sample42_R2.fasta<br>
	 * <li>kraken-translate --db /database/kraken --mpa-format ./temp/sample42_kraken.txt &gt;
	 * ./output/sample42_reported.tsv
	 * </ol>
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		for( final File file: map.keySet() ) {
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String tempFile = getTempDir().getAbsolutePath() + File.separator + fileId + KRAKEN_FILE;
			final String krakenOutput =
				getOutputDir().getAbsolutePath() + File.separator + fileId + Constants.PROCESSED;
			final ArrayList<String> lines = new ArrayList<>( 2 );
			lines.add( FUNCTION_KRAKEN + " " + tempFile + " " + file.getAbsolutePath() + " " +
				map.get( file ).getAbsolutePath() );
			lines.add( FUNCTION_TRANSLATE + " " + tempFile + " " + krakenOutput );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Verify that none of the derived command line parameters are included in
	 * {@link biolockj.Config}.{@value #EXE_KRAKEN}{@value biolockj.Constants#PARAMS}.
	 */
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		getParams();
	}

	/**
	 * Get kraken executable command: {@value #EXE_KRAKEN}
	 */
	@Override
	public String getClassifierExe() throws Exception {
		return Config.getExe( this, EXE_KRAKEN );
	}

	/**
	 * Obtain the kraken runtime params
	 */
	@Override
	public List<String> getClassifierParams() throws Exception {
		return Config.getList( this, getExeParamName() );
	}

	@Override
	public File getDB() throws ConfigNotFoundException, ConfigPathException {
		if( DockerUtil.inDockerEnv() ) return new File( Config.requireString( this, KRAKEN_DATABASE ) );
		return Config.requireExistingDir( this, KRAKEN_DATABASE );
	}

	/**
	 * This method generates the required bash functions: {@value #FUNCTION_TRANSLATE} and {@value #FUNCTION_KRAKEN}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		final String params = "$1 $2" + ( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) ? " $3": "" );
		lines.add( "function " + FUNCTION_KRAKEN + "() {" );
		lines.add( getClassifierExe() + getWorkerFunctionParams() + "--output " + params );
		lines.add( "}" + RETURN );
		lines.add( "function " + FUNCTION_TRANSLATE + "() {" );
		lines.add(
			getClassifierExe() + "-translate " + DB_PARAM + getKrakenDB().getAbsolutePath() + " --mpa-format $1 > $2" );
		lines.add( "}" + RETURN );
		return lines;
	}

	private File getKrakenDB() throws ConfigPathException, ConfigNotFoundException {
		if( DockerUtil.inDockerEnv() ) return DockerUtil.getDockerDB( this, getDB().getAbsolutePath() );
		return getDB();
	}

	private String getParams() throws Exception {
		if( this.defaultSwitches == null ) {
			final List<String> classifierParams = getClassifierParams();
			final String params = BioLockJUtil.join( classifierParams );

			if( params.indexOf( FASTA_PARAM ) > -1 ) classifierParams.remove( FASTA_PARAM );
			if( params.indexOf( FASTQ_PARAM ) > -1 ) classifierParams.remove( FASTQ_PARAM );
			if( params.indexOf( NUM_THREADS_PARAM ) > -1 )
				throw new Exception( "Invalid classifier option (" + NUM_THREADS_PARAM + ") found in property(" +
					getExeParamName() + "). BioLockJ derives this value from property: " + SCRIPT_NUM_THREADS );
			if( params.indexOf( PAIRED_PARAM ) > -1 )
				throw new Exception( "Invalid classifier option (" + PAIRED_PARAM + ") found in property(" +
					getExeParamName() + "). BioLockJ derives this value by analyzing input sequence files" );
			if( params.indexOf( OUTPUT_PARAM ) > -1 ) throw new Exception(
				"Invalid classifier option (" + OUTPUT_PARAM + ") found in property(" + getExeParamName() +
					"). BioLockJ hard codes this file path based on sequence files names in: " + Constants.INPUT_DIRS );
			if( params.indexOf( DB_PARAM ) > -1 ) throw new Exception(
				"Invalid classifier option (" + DB_PARAM + ") found in property(" + getExeParamName() +
					"). BioLockJ hard codes this directory path based on Config property: " + KRAKEN_DATABASE );
			if( params.indexOf( "--help " ) > -1 ) throw new Exception(
				"Invalid classifier option (--help) found in property(" + getExeParamName() + ")." );
			if( params.indexOf( "--version " ) > -1 ) throw new Exception(
				"Invalid classifier option (--version) found in property(" + getExeParamName() + ")." );

			this.defaultSwitches = getRuntimeParams( classifierParams, NUM_THREADS_PARAM );
		}

		return this.defaultSwitches;
	}

	private String getWorkerFunctionParams() throws Exception {
		String params = " " + getParams();
		if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) ) params += PAIRED_PARAM;

		if( !getInputFiles().isEmpty() && SeqUtil.isGzipped( getInputFiles().get( 0 ).getName() ) )
			params += GZIP_PARAM;

		params += DB_PARAM + getKrakenDB().getAbsolutePath() + " " + getInputSwitch();
		if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) ) params += PAIRED_PARAM;

		if( !getInputFiles().isEmpty() && SeqUtil.isGzipped( getInputFiles().get( 0 ).getName() ) )
			params += GZIP_PARAM;

		return params;
	}

	private static String getExeParamName() {
		return EXE_KRAKEN + Constants.PARAMS;
	}

	/**
	 * Set the input switch based reading a sample input file.
	 *
	 * @return file type switch
	 * @throws Exception if errors occur
	 */
	private static String getInputSwitch() throws Exception {
		if( SeqUtil.isFastA() ) return FASTA_PARAM;
		if( SeqUtil.isFastQ() ) return FASTQ_PARAM;
		return null;
	}

	private String defaultSwitches = null;

	/**
	 * {@link biolockj.Config} exe property for kraken executable: {@value #EXE_KRAKEN}
	 */
	protected static final String EXE_KRAKEN = "exe.kraken";

	/**
	 * Name of the kraken function used to assign taxonomy: {@value #FUNCTION_KRAKEN}
	 */
	protected static final String FUNCTION_KRAKEN = "runKraken";

	/**
	 * Name of the translate function used to convert mpa-format to standard format: {@value #FUNCTION_TRANSLATE}
	 */
	protected static final String FUNCTION_TRANSLATE = "translate";

	/**
	 * {@link biolockj.Config} property must contain file path to Kraken kmer database directory:
	 * {@value #KRAKEN_DATABASE}
	 */
	protected static final String KRAKEN_DATABASE = "kraken.db";

	/**
	 * File suffix added by BioLockJ to kraken output files (before translation): {@value #KRAKEN_FILE}
	 */
	protected static final String KRAKEN_FILE = "_kraken_out" + TXT_EXT;

	private static final String DB_PARAM = "--db ";
	private static final String FASTA_PARAM = "--fasta-input ";
	private static final String FASTQ_PARAM = "--fastq-input ";
	private static final String GZIP_PARAM = "--gzip-compressed ";
	private static final String NUM_THREADS_PARAM = "--threads";
	private static final String OUTPUT_PARAM = "--output ";
	private static final String PAIRED_PARAM = "--paired ";
}
