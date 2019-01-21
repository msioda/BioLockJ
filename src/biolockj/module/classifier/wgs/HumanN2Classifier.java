/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 20, 2017
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
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.RuntimeParamUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule runs biobakery humann2 program to generate the HMP Unified Metabolic Analysis Network<br>
 * HUMAnN is a pipeline for efficiently and accurately profiling the presence/absence and abundance of microbial
 * pathways in a community from metagenomic or metatranscriptomic sequencing data (typically millions of short DNA/RNA
 * reads). This process, referred to as functional profiling, aims to describe the metabolic potential of a microbial
 * community and its members. More generally, functional profiling answers the question "What are the microbes in my
 * community-of-interest doing (or capable of doing)?
 * 
 * For more information, please review the BioBakery instruction manual:
 * <a href= "https://bitbucket.org/biobakery/humann2" target="_top">https://bitbucket.org/biobakery/humann2</a><br>
 */
public class HumanN2Classifier extends ClassifierModuleImpl implements ClassifierModule
{

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			File hn2InputSeq = file;
			final ArrayList<String> lines = new ArrayList<>();
			if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
			{
				lines.add( getMergeReadLine( file ) );
				hn2InputSeq = getMergedReadFile( file );
			}

			lines.add( getHumanN2Line( hn2InputSeq ) );
			data.add( lines );
		}

		return data;
	}

	@Override
	public List<List<String>> buildScriptForPairedReads( List<File> files ) throws Exception
	{
		files = new ArrayList<>( getPairedReads().keySet() );
		Collections.sort( files );
		return buildScript( files );
	}

	/**
	 * Verify that none of the derived command line parameters are included in
	 * {@link biolockj.Config}.{@value biolockj.module.classifier.ClassifierModule#EXE_CLASSIFIER_PARAMS}. Also verify:
	 * <ul>
	 * <li>{@link biolockj.Config}.{@value #NUCL_DB} is a valid directory
	 * <li>{@link biolockj.Config}.{@value #PROT_DB} is a valid directory
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getRuntimeParams();

	}

	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		pairedReads = null;
	}

	/**
	 * This method generates the required bash functions: {@value #FUNCTION_TRANSLATE} and {@value #FUNCTION_KRAKEN}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			lines.add( "function " + FUNCTION_CONCAT_PAIRED_READS + "() {" );
			lines.add( "cat $1 $2 > " + getMergeDir().getAbsolutePath() + File.separator + "$3" );
			lines.add( "}" );
		}

		lines.add( "function " + FUNCTION_RUN_HN2 + "() {" );
		lines.add( getClassifierExe() + getRuntimeParams() + INPUT_PARAM + " $1" );
		lines.add( "}" );

		return lines;
	}

	/**
	 * Get formatted KneadData switches if provided in {@link biolockj.Config} properties:
	 * {@value #EXE_KNEADDATA_PARAMS} and {@value #SCRIPT_NUM_THREADS}.
	 *
	 * @return Formatted KneadData switches
	 * @throws Exception if errors occur
	 */
	protected String getRuntimeParams() throws Exception
	{
		return getRuntimeParams( getClassifierParams(), NUM_THREADS_PARAM ) + NUCL_DB_PARAM + " " + getNuclDB() + " "
				+ PROT_DB_PARAM + " " + getProtDB() + " " + OUTPUT_PARAM + " " + getOutputDir().getAbsolutePath();
	}

	private String getHumanN2Line( final File file ) throws Exception
	{
		return FUNCTION_RUN_HN2 + " " + INPUT_PARAM + " " + file.getAbsolutePath() + " " + NUCL_DB_PARAM + " "
				+ getNuclDB() + " " + PROT_DB_PARAM + " " + getProtDB() + " " + OUTPUT_PARAM + " "
				+ getOutputDir().getAbsolutePath();
	}

	private File getMergeDir() throws Exception
	{
		final File dir = new File( getTempDir().getAbsolutePath() + File.separator + TEMP_MERGE_READ_DIR );
		if( !dir.exists() )
		{
			dir.mkdirs();
		}
		return dir;
	}

	private File getMergedReadFile( final File file ) throws Exception
	{
		return new File( getMergeDir().getAbsoluteFile() + File.separator + SeqUtil.getSampleId( file.getName() )
				+ BioLockJUtil.fileExt( file ) );
	}

	private String getMergeReadLine( final File file ) throws Exception
	{
		return FUNCTION_CONCAT_PAIRED_READS + " " + file.getAbsolutePath() + " "
				+ getPairedReads().get( file ).getAbsolutePath() + " " + SeqUtil.getSampleId( file.getName() )
				+ BioLockJUtil.fileExt( file );
	}

	private String getNuclDB() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			return Config.requireString( HN2_NUCL_DB );
		}

		return Config.requireExistingDir( HN2_NUCL_DB ).getAbsolutePath();
	}

	private Map<File, File> getPairedReads() throws Exception
	{
		if( pairedReads == null && Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			pairedReads = SeqUtil.getPairedReads( getInputFiles() );
		}

		return pairedReads;
	}

	private String getProtDB() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			return Config.requireString( HN2_PROT_DB );
		}

		return Config.requireExistingDir( HN2_PROT_DB ).getAbsolutePath();
	}

	private Map<File, File> pairedReads = null;

	/**
	 * Name of the function used to concatenate forward and reverse read into a single file to use as input into humann2
	 * command: {@value #FUNCTION_CONCAT_PAIRED_READS}
	 */
	protected static final String FUNCTION_CONCAT_PAIRED_READS = "mergePairedReads";

	/**
	 * Name of the function used to join HumanN2 output for all of the samples initial HumanN2 analysis:
	 * {@value #FUNCTION_JOIN_HN2_TABLES}
	 */
	protected static final String FUNCTION_JOIN_HN2_TABLES = "joinHn2Tables";

	/**
	 * Name of the function used to renormalize HumanN2 tables: {@value #FUNCTION_RENORM_HN2_TABLES}
	 */
	protected static final String FUNCTION_RENORM_HN2_TABLES = "renormHn2Tables";

	/**
	 * Name of the function used to run initial HumanN2 analysis: {@value #FUNCTION_RUN_HN2}
	 */
	protected static final String FUNCTION_RUN_HN2 = "runHn2";

	/**
	 * {@link biolockj.Config} Directory property may contain multiple nucleotide database files: {@value #HN2_NUCL_DB}
	 */
	protected static final String HN2_NUCL_DB = "humann2.nuclDB";

	/**
	 * {@link biolockj.Config} Directory property may contain protein nucleotide database files:
	 * {@value #HN2_PROT_DB2_NUCL_DB}
	 */
	protected static final String HN2_PROT_DB = "humann2.protDB";

	/**
	 * Renormalize HumanN2 table --mode option: {@value #RENORM_MODE_OPTION_COMMUNITY}
	 */
	protected static final String RENORM_MODE_OPTION_COMMUNITY = "community";

	/**
	 * Renormalize HumanN2 table --mode option: {@value #RENORM_MODE_OPTION_LEVELWISE}
	 */
	protected static final String RENORM_MODE_OPTION_LEVELWISE = "levelwise";

	/**
	 * Renormalize HumanN2 table --units option "Copies Per Million": {@value #HN2_RENORM_OPTION_CPM}
	 */
	protected static final String RENORM_UNITS_OPTION_CPM = "cpm";

	/**
	 * Renormalize HumanN2 table --units option "Relative Abundance": {@value #RENORM_UNITS_OPTION_RELAB}
	 */
	protected static final String RENORM_UNITS_OPTION_RELAB = "relab";
	private static final String INPUT_PARAM = "-i";
	private static final String NUCL_DB_PARAM = "--nucleotide-database";
	private static final String NUM_THREADS_PARAM = "--threads";
	private static final String OUTPUT_PARAM = "-o";
	private static final String PROT_DB_PARAM = "--protein-database";
	private static final String RENORM_MODE_PARAM = "-m";
	private static final String RENORM_UNITS_PARAM = "--units";
	private static final String TEMP_MERGE_READ_DIR = "merged";
}
