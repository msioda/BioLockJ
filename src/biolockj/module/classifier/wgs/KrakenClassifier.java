/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
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
import biolockj.util.SeqUtil;

/**
 * This class builds the Kraken classifier scripts.
 */
public class KrakenClassifier extends ClassifierModuleImpl implements ClassifierModule
{
	/**
	 * Build scripts that outputs initial Kraken classification files to tempDir.
	 * Next, call kraken-translate and output results to outputDir.
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

			final ArrayList<String> lines = new ArrayList<>( 2 );

			lines.add( getClassifierExe() + switches + "--output " + tempFile + " " + file.getAbsolutePath() );
			lines.add( getClassifierExe() + "-translate --db "
					+ Config.requireExistingDir( KRAKEN_DATABASE ).getAbsolutePath() + " --mpa-format " + tempFile
					+ " > " + krakenOutput );

			data.add( lines );
		}

		return data;
	}

	/**
	 * Build scripts for paired reads that outputs initial Kraken classification files to tempDir.
	 * Next, call kraken-translate and output results to outputDir.
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		final String db = Config.requireExistingDir( KRAKEN_DATABASE ).getAbsolutePath();
		for( final File file: map.keySet() )
		{
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String tempFile = getTempDir().getAbsolutePath() + File.separator + fileId + KRAKEN_FILE;
			final String krakenOutput = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;

			final ArrayList<String> lines = new ArrayList<>( 2 );

			lines.add( getClassifierExe() + " --db " + db + switches + "--output " + tempFile + " "
					+ file.getAbsolutePath() + " " + map.get( file ).getAbsolutePath() );

			lines.add(
					getClassifierExe() + "-translate --db " + db + " --mpa-format " + tempFile + " > " + krakenOutput );

			data.add( lines );
		}

		return data;
	}

	/**
	 * Verify the input switches are valid and the kraken database exists.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.requireExistingDir( KRAKEN_DATABASE );
		switches = getClassifierParams();

		if( switches.indexOf( "--fasta-input " ) > -1 )
		{
			switches.replaceAll( "--fasta-input", "" );
		}
		if( switches.indexOf( "--fastq-input " ) > -1 )
		{
			switches.replaceAll( "--fastq-input", "" );
		}
		if( switches.indexOf( "--threads " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--threads) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ derives this value from property: " + Config.SCRIPT_NUM_THREADS );
		}
		if( switches.indexOf( "--paired " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--paired) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ derives this value from property: " + Config.INPUT_PAIRED_READS );
		}
		if( switches.indexOf( "--output " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--output) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ hard codes this value based on Sample IDs found in: " + Config.INPUT_DIRS );
		}
		if( switches.indexOf( "--db " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--db) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ hard codes this value based on Sample IDs found in: " + KRAKEN_DATABASE );
		}

		addHardCodedSwitches();
	}

	/**
	 * Add hard coded switches to classifier switches value.
	 * @throws Exception
	 */
	private void addHardCodedSwitches() throws Exception
	{
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
	}

	/**
	 * All calls to classifier requires setting number of threads, type of input files,
	 * set paired switch if needed, and finally, set the database param.
	 * @return
	 * @throws Exception
	 */
	private Map<String, String> getHardCodedKrakenSwitches() throws Exception
	{
		final Map<String, String> krakenSwitches = new HashMap<>();
		krakenSwitches.put( "--db", Config.requireExistingDir( KRAKEN_DATABASE ).getAbsolutePath() );
		krakenSwitches.put( "--threads",
				new Integer( Config.requirePositiveInteger( Config.SCRIPT_NUM_THREADS ) ).toString() );
		krakenSwitches.put( getInputSwitch(), "" );
		if( Config.getBoolean( Config.INPUT_PAIRED_READS ) )
		{
			krakenSwitches.put( "--paired", "" );
		}

		Log.out.warn( Log.LOG_SPACER );
		Log.out.warn( Log.LOG_SPACER );
		Log.out.warn( Log.LOG_SPACER );
		Log.out.warn( "LOOKING FOR .gz FILES!" );
		Log.out.warn( Log.LOG_SPACER );
		Log.out.warn( Log.LOG_SPACER );
		for( final File f: getInputFiles() )
		{
			Log.out.warn( "setting kraken switch, list input: " + f.getAbsolutePath() );
		}

		Log.out.warn( "setting kraken switch, list input: " + getInputFiles().get( 0 ).getName().toLowerCase() );
		Log.out.warn( Log.LOG_SPACER );
		Log.out.warn( Log.LOG_SPACER );
		Log.out.warn( Log.LOG_SPACER );

		if( getInputFiles().get( 0 ).getName().toLowerCase().endsWith( ".gz" ) )
		{
			Log.out.warn( "Setting: --gzip-compressed" );
			krakenSwitches.put( "--gzip-compressed", "" );
		}

		return krakenSwitches;
	}

	/**
	 * Set the input switch based on inputSequenceType.
	 * @return
	 * @throws Exception
	 */
	private String getInputSwitch() throws Exception
	{
		if( SeqUtil.isFastA() )
		{
			return "--fasta-input";
		}
		else
		{
			return "--fastq-input";
		}
	}

	//private File krakenDatabaseDir = null;
	private String switches = null;
	private static final String KRAKEN_DATABASE = "kraken.db";
	private static final String KRAKEN_FILE = "_kraken.txt";
}