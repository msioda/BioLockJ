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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import biolockj.Config;
import biolockj.Constants;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.*;

/**
 * This BioModule assigns taxonomy to WGS sequences and translates the results into mpa-format. Command line options are
 * defined in the online manual: <a href="http://ccb.jhu.edu/software/kraken/MANUAL.html" target=
 * "_top">http://ccb.jhu.edu/software/kraken/MANUAL.html</a>
 * 
 * @blj.web_desc Kraken2 Classifier
 */
public class Kraken2Classifier extends ClassifierModuleImpl implements ClassifierModule
{
	/**
	 * Build bash script lines to classify unpaired WGS reads with Kraken2. The inner list contains 1 bash script line
	 * used to classify 1 sample.
	 * <p>
	 * Example lines:
	 * <ol>
	 * <li>kraken2 --mpa-format --only-classified-output --threads 8 --db /database/kraken --output
	 * ./output/sample42_reported.tsv ./input/sample42.fasta<br>
	 * </ol>
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String tempFile = getTempDir().getAbsolutePath() + File.separator + fileId + KRAKEN_FILE;
			final String krakenOutput = getOutputDir().getAbsolutePath() + File.separator + fileId
					+ Constants.PROCESSED;
			final ArrayList<String> lines = new ArrayList<>( 1 );
			lines.add( FUNCTION_KRAKEN + " " + krakenOutput + " " + tempFile + " " + file.getAbsolutePath() );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Build bash script lines to classify paired WGS reads with Kraken. The inner list contains 1 bash script line used
	 * to classify 1 sample (2 files: forward and reverse reads).
	 * <p>
	 * Example lines:
	 * <ol>
	 * <li>kraken --mpa-format --paired --fasta-input --only-classified-output --threads 8 --db /database/kraken
	 * --output ./output/sample42_reported.tsv ./input/sample42_R1.fasta ./input/sample42_R2.fasta<br>
	 * *
	 * </ol>
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		for( final File file: map.keySet() )
		{
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String tempFile = getTempDir().getAbsolutePath() + File.separator + fileId + KRAKEN_FILE;
			final String krakenOutput = getOutputDir().getAbsolutePath() + File.separator + fileId
					+ Constants.PROCESSED;
			final ArrayList<String> lines = new ArrayList<>( 1 );
			lines.add( FUNCTION_KRAKEN + " " + krakenOutput + " " + tempFile + " " + file.getAbsolutePath() + " "
					+ map.get( file ).getAbsolutePath() );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Verify that none of the derived command line parameters are included in
	 * {@link biolockj.Config}.{@value #EXE_KRAKEN2}{@value biolockj.Constants#PARAMS}.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getParams();
		getDB();
	}

	/**
	 * Get kraken executable command: {@value #EXE_KRAKEN2}
	 */
	@Override
	public String getClassifierExe() throws Exception
	{
		return Config.getExe( this, EXE_KRAKEN2 );
	}

	/**
	 * Obtain the kraken2 runtime params
	 */
	@Override
	public List<String> getClassifierParams() throws Exception
	{
		return Config.getList( this, getExeParamName() );
	}

	/**
	 * This method generates the required bash function: {@value #FUNCTION_KRAKEN}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		final String inFiles = "$3" + ( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) ? " $4": "" );
		lines.add( "function " + FUNCTION_KRAKEN + "() {" );
		lines.add( getClassifierExe() + getWorkerFunctionParams() + REPORT_PARAM + "$1 " + OUTPUT_PARAM + " $2 "
				+ inFiles );
		lines.add( "}" + RETURN );

		return lines;
	}

	private String getDB() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			return DockerUtil.CONTAINER_DB_DIR;
		}

		return Config.requireExistingDir( this, KRAKEN_DATABASE ).getAbsolutePath();
	}

	private String getExeParamName()
	{
		return EXE_KRAKEN2 + Constants.PARAMS;
	}

	private String getParams() throws Exception
	{
		if( defaultSwitches == null )
		{
			final List<String> classifierParams = getClassifierParams();
			final String params = BioLockJUtil.join( classifierParams );

			if( params.contains( FASTA_PARAM ) )
			{
				classifierParams.remove( FASTA_PARAM );
			}
			if( params.contains( FASTQ_PARAM ) )
			{
				classifierParams.remove( FASTQ_PARAM );
			}
			if( params.contains( USE_NAMES_PARAM ) )
			{
				classifierParams.remove( USE_NAMES_PARAM );
			}
			if( params.contains( USE_MPA_PARAM ) )
			{
				classifierParams.remove( USE_MPA_PARAM );
			}
			if( params.indexOf( NUM_THREADS_PARAM ) > -1 )
			{
				throw new Exception( "Invalid classifier option (" + NUM_THREADS_PARAM + ") found in property("
						+ getExeParamName() + "). BioLockJ derives this value from property: " + SCRIPT_NUM_THREADS );
			}
			if( params.indexOf( PAIRED_PARAM ) > -1 )
			{
				throw new Exception( "Invalid classifier option (" + PAIRED_PARAM + ") found in property("
						+ getExeParamName() + "). BioLockJ derives this value by analyzing input sequence files" );
			}
			if( params.indexOf( OUTPUT_PARAM ) > -1 )
			{
				throw new Exception( "Invalid classifier option (" + OUTPUT_PARAM + ") found in property("
						+ getExeParamName() + "). BioLockJ hard codes this file path based on sequence files names in: "
						+ Constants.INPUT_DIRS );
			}
			if( params.indexOf( DB_PARAM ) > -1 )
			{
				throw new Exception( "Invalid classifier option (" + DB_PARAM + ") found in property("
						+ getExeParamName() + "). BioLockJ hard codes this directory path based on Config property: "
						+ KRAKEN_DATABASE );
			}
			if( params.indexOf( "--help " ) > -1 )
			{
				throw new Exception(
						"Invalid classifier option (--help) found in property(" + getExeParamName() + ")." );
			}
			if( params.indexOf( "--version " ) > -1 )
			{
				throw new Exception(
						"Invalid classifier option (--version) found in property(" + getExeParamName() + ")." );
			}
			if( params.indexOf( REPORT_PARAM ) > -1 )
			{
				throw new Exception( "Invalid classifier option (" + REPORT_PARAM + ") found in property("
						+ getExeParamName() + "). BioLockJ hard codes this value based on Sample IDs found in: "
						+ Constants.INPUT_DIRS );
			}

			defaultSwitches = getRuntimeParams( classifierParams, NUM_THREADS_PARAM ) + DB_PARAM + getDB() + " "
					+ USE_NAMES_PARAM + USE_MPA_PARAM;;
		}

		return defaultSwitches;
	}

	// method calculates mean need by the module.
	private String getWorkerFunctionParams() throws Exception
	{
		String params = " " + getParams();
		if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) )
		{
			params += PAIRED_PARAM;
		}

		if( !getInputFiles().isEmpty() && SeqUtil.isGzipped( getInputFiles().get( 0 ).getName() ) )
		{
			params += GZIP_PARAM;
		}
		return params;
	}

	private String defaultSwitches = null;

	/**
	 * {@link biolockj.Config} exe property for kraken2 executable: {@value #EXE_KRAKEN2}
	 */
	protected static final String EXE_KRAKEN2 = "exe.kraken2";

	/**
	 * Name of the kraken function used to assign taxonomy: {@value #FUNCTION_KRAKEN}
	 */
	protected static final String FUNCTION_KRAKEN = "runKraken2";

	/**
	 * {@link biolockj.Config} property must contain file path to Kraken kmer database directory:
	 * {@value #KRAKEN_DATABASE}
	 */
	protected static final String KRAKEN_DATABASE = "kraken2.db";

	/**
	 * File suffix added by BioLockJ to kraken output files (before translation): {@value #KRAKEN_FILE}
	 */
	protected static final String KRAKEN_FILE = "_kraken2_out" + TXT_EXT;

	private static final String DB_PARAM = "--db ";
	private static final String FASTA_PARAM = "--fasta-input ";
	private static final String FASTQ_PARAM = "--fastq-input ";
	private static final String GZIP_PARAM = "--gzip-compressed ";
	private static final String NUM_THREADS_PARAM = "--threads";
	private static final String OUTPUT_PARAM = "--output ";
	private static final String PAIRED_PARAM = "--paired ";
	private static final String REPORT_PARAM = "--report ";
	private static final String USE_MPA_PARAM = "--use-mpa-style ";
	private static final String USE_NAMES_PARAM = "--use-names ";
}
