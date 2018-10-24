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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.RuntimeParamUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule assigns taxonomy to WGS sequences and translates the results into mpa-format. Command line options are
 * defined in the online manual: <a href="http://ccb.jhu.edu/software/kraken/MANUAL.html" target=
 * "_top">http://ccb.jhu.edu/software/kraken/MANUAL.html</a>
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
			final String krakenOutput = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;
			final ArrayList<String> lines = new ArrayList<>( 1 );
			lines.add( FUNCTION_KRAKEN + " " + krakenOutput + " " + tempFile + " " + file.getAbsolutePath() );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Build bash script lines to classify paired WGS reads with Kraken. The inner list contains 1 bash script line
	 * used to classify 1 sample (2 files: forward and reverse reads).
	 * <p>
	 * Example lines:
	 * <ol>
	 * <li>kraken --mpa-format --paired --fasta-input --only-classified-output --threads 8 --db /database/kraken --output
	 * ./output/sample42_reported.tsv ./input/sample42_R1.fasta ./input/sample42_R2.fasta<br>	 * 
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
			final String krakenOutput = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;
			final ArrayList<String> lines = new ArrayList<>( 1 );
			lines.add( FUNCTION_KRAKEN + " " + krakenOutput + " " + tempFile + " " + file.getAbsolutePath() + " "
					+ map.get( file ).getAbsolutePath() );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Verify that none of the derived command line parameters are included in
	 * {@link biolockj.Config}.{@value biolockj.module.classifier.ClassifierModule#EXE_CLASSIFIER_PARAMS}. Also verify:
	 * <ul>
	 * <li>{@link biolockj.Config}.{@value #KRAKEN_DATABASE} is a valid file path valid parameters
	 * 
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getDB();
		getDefaultSwitches();
	}

	/**
	 * This method generates the required bash function: {@value #FUNCTION_KRAKEN}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final String inFiles = "$3" + ( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) ? " $4": "" );
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_KRAKEN + "() {" );
		lines.add( getClassifierExe() + getRuntimeParams() + "--report $1 " + "--output $2 " + inFiles );
		lines.add( "}" );
		return lines;
	}

	/**
	 * Format hard coded defaultSwitches to classifier defaultSwitches value.
	 * 
	 * @return Formatted Kraken runtime parameters
	 */
	private String formatHardCodedSwitches() throws Exception
	{
		String switches = "";
		final Map<String, String> hardCodedSwitches = getHardCodedKrakenSwitches();
		for( final String key: hardCodedSwitches.keySet() )
		{
			String val = hardCodedSwitches.get( key ).trim();
			if( val.length() > 0 )
			{
				val = " " + val;
			}
			switches += key + val + " ";
		}

		return switches;
	}

	private String getDB() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			return Config.requireString( KRAKEN_DATABASE );
		}

		return Config.requireExistingDir( KRAKEN_DATABASE ).getAbsolutePath();
	}

	private String getDefaultSwitches() throws Exception
	{
		if( defaultSwitches == null )
		{
			defaultSwitches = getClassifierParams();

			if( defaultSwitches.indexOf( "--fasta-input " ) > -1 )
			{
				defaultSwitches.replaceAll( "--fasta-input", "" );
			}
			if( defaultSwitches.indexOf( "--fastq-input " ) > -1 )
			{
				defaultSwitches.replaceAll( "--fastq-input", "" );
			}
			if( defaultSwitches.indexOf( "--use-names " ) > -1 )
			{
				defaultSwitches.replaceAll( "--use-names ", "" );
			}
			if( defaultSwitches.indexOf( "--use-mpa-style " ) > -1 )
			{
				defaultSwitches.replaceAll( "--use-mpa-style ", "" );
			}
			if( defaultSwitches.indexOf( "--threads " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (--threads) found in property(" + EXE_CLASSIFIER_PARAMS
						+ "). BioLockJ derives this value from property: " + getNumThreadsParam() );
			}
			if( defaultSwitches.indexOf( "--paired " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (--paired) found in property(" + EXE_CLASSIFIER_PARAMS
						+ "). BioLockJ derives this value by analyzing input sequence files" );
			}
			if( defaultSwitches.indexOf( "--output " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (--output) found in property(" + EXE_CLASSIFIER_PARAMS
						+ "). BioLockJ hard codes this file path based on sequence files names in: " + Config.INPUT_DIRS );
			}
			if( defaultSwitches.indexOf( "--db " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (--db) found in property(" + EXE_CLASSIFIER_PARAMS
						+ "). BioLockJ hard codes this directory path based on Config property: " + KRAKEN_DATABASE );
			}
			if( defaultSwitches.indexOf( "--help " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (--help) found in property(" + EXE_CLASSIFIER_PARAMS
						+ ")." );
			}
			if( defaultSwitches.indexOf( "--version " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (--version) found in property(" + EXE_CLASSIFIER_PARAMS
						+ ")." );
			}
			if( defaultSwitches.indexOf( "--report " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (--report) found in property(" + EXE_CLASSIFIER_PARAMS
						+ "). BioLockJ hard codes this value based on Sample IDs found in: " + Config.INPUT_DIRS );
			}
		}

		if( defaultSwitches == null )
		{
			defaultSwitches = "";
		}

		return defaultSwitches;
	}

	/**
	 * All calls to classifier requires setting number of threads, type of input files, set paired switch if needed, and
	 * finally, set the database parameter.
	 *
	 * @return hard-coded Kraken defaultSwitches
	 * @throws Exception if any validation fails
	 */
	private Map<String, String> getHardCodedKrakenSwitches() throws Exception
	{
		final Map<String, String> switches = new HashMap<>();
		switches.put( "--use-names", ""); //needed for output style expected by Kraken2Parser
		switches.put( "--use-mpa-style", ""); //needed for output style expected by Kraken2Parser
		switches.put( "--db", getDB() );
		switches.put( "--threads", getNumThreads().toString() );
//		switches.put( getInputSwitch(), "" );
		if( Config.requireBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			switches.put( "--paired", "" );
		}

		if( !getInputFiles().isEmpty() && getInputFiles().get( 0 ).getName().toLowerCase().endsWith( ".gz" ) )
		{
			if( getDefaultSwitches().indexOf( "--bzip2-compressed " ) > -1 )
			{
				Log.get( getClass() ).warn( "Setting: --bzip2-compressed based on user input" );
				switches.put( "--gzip-compressed", "" );
			}
			else
			{
				Log.get( getClass() ).warn( "Setting: --gzip-compressed" );
				switches.put( "--gzip-compressed", "" );
			}
		}

		return switches;
	}

	/**
	 * Set the input switch based reading a sample input file.
	 * Kraken2 does this automatically.
	 *
	 * @return file type switch
	 * @throws Exception
	 */
//	private String getInputSwitch() throws Exception
//	{
//		if( SeqUtil.isFastA() )
//		{
//			return "--fasta-input";
//		}
//		else
//		{
//			return "--fastq-input";
//		}
//	}

	private String getRuntimeParams() throws Exception
	{
		String switches = getDefaultSwitches();
		String hardCodedSwitches = formatHardCodedSwitches();
		if( !switches.endsWith( " " ) )
		{
			switches += " ";
		}
		if( !hardCodedSwitches.endsWith( " " ) )
		{
			hardCodedSwitches += " ";
		}

		return switches + hardCodedSwitches;
	}

	private String defaultSwitches = null;

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
	protected static final String KRAKEN_FILE = "_kraken2_out.txt";
}
