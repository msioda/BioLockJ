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
import biolockj.exception.ConfigFormatException;
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

			lines.add( FUNCTION_RUN_HN2 + " " + hn2InputSeq.getAbsolutePath() );
			lines.add( FUNCTION_JOIN_HN2_TABLES + " " + getTempSubDir( TEMP_HN2_OUTPUT_DIR ).getAbsoluteFile() + " "
					+ outputPath( PATH_ABUNDANCE ) + PATH_ABUNDANCE );

			for( final String unit: Config.getSet( HN2_RENORM_UNITS ) )
			{
				lines.add( FUNCTION_RENORM_HN2_TABLES + " " + outputPath( PATH_ABUNDANCE ) + getRenormPath( unit, null )
						+ RENORM_UNITS_OPTION_CPM );

				for( final String mode: Config.getSet( HN2_RENORM_MODES ) )
				{
					lines.add( FUNCTION_RENORM_HN2_TABLES + " " + outputPath( PATH_ABUNDANCE )
							+ getRenormPath( unit, mode ) + unit + " " + mode );
				}

			}

			// #copying metaphlan tables into metaphlan_output
			// find . -iname *list.tsv -exec cp '{}' ./metaphlan_output \;
			//
			// #Merging metaphlan files
			// humann2_join_tables -i metaphlan_output -o metaphlan.tsv --file_name bugs_list

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
	 * <li>{@link biolockj.Config}.{@value #HN2_NUCL_DB} is a valid directory
	 * <li>{@link biolockj.Config}.{@value #HN2_PROT_DB} is a valid directory
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getRuntimeParams();
		validateRenormModes();
		validateRenormUnits();
	}

	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		pairedReads = null;
	}

	/**
	 * This method generates the required bash functions: {@value #FUNCTION_CONCAT_PAIRED_READS} and
	 * {@value #FUNCTION_RUN_HN2}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			lines.add( "function " + FUNCTION_CONCAT_PAIRED_READS + "() {" );
			lines.add(
					"cat $1 $2 > " + getTempSubDir( TEMP_MERGE_READ_DIR ).getAbsolutePath() + File.separator + "$3" );
			lines.add( "}" );
		}

		lines.add( HN2_BASH_COMMENT );
		lines.add( "function " + FUNCTION_RUN_HN2 + "() {" );
		lines.add( getClassifierExe() + " " + getRuntimeParams() + INPUT_PARAM + "$1 " + NUCL_DB_PARAM + getNuclDB()
				+ " " + PROT_DB_PARAM + getProtDB() + " " + OUTPUT_PARAM
				+ getTempSubDir( TEMP_HN2_OUTPUT_DIR ).getAbsoluteFile() );
		lines.add( "}" + RETURN );

		lines.add( HN2_BASH_COMMENT );
		lines.add( "function " + FUNCTION_JOIN_HN2_TABLES + "() {" );
		lines.add( getJoinTableCmd() + " " + INPUT_PARAM + "$1 " + OUTPUT_PARAM + "$2 " + FILE_NAME_PARAM + "$3" );
		lines.add( "}" + RETURN );

		lines.add( HN2_BASH_COMMENT );
		lines.add( "function " + FUNCTION_RENORM_HN2_TABLES + "() {" );
		lines.add( "if [ ${#4} -gt 0 ]; then" );
		lines.add( getJoinTableCmd() + " " + INPUT_PARAM + "$1 " + OUTPUT_PARAM + "$2 " + RENORM_UNITS_PARAM + "$3 "
				+ RENORM_MODE_PARAM + "$4" );
		lines.add( "else" );
		lines.add( getJoinTableCmd() + " " + INPUT_PARAM + "$1 " + OUTPUT_PARAM + "$2 " + RENORM_UNITS_PARAM + "$3" );
		lines.add( "fi" + RETURN );

		lines.add( "}" );

		return lines;
	}

	/**
	 * Get formatted KneadData switches if provided in {@link biolockj.Config} properties:
	 * {@value #EXE_CLASSIFIER_PARAMS} and {@value #SCRIPT_NUM_THREADS}.
	 *
	 * @return Formatted KneadData switches
	 * @throws Exception if errors occur
	 */
	protected String getRuntimeParams() throws Exception
	{
		return getRuntimeParams( getClassifierParams(), NUM_THREADS_PARAM ) + NUCL_DB_PARAM + " " + getNuclDB() + " "
				+ PROT_DB_PARAM + " " + getProtDB() + " " + OUTPUT_PARAM + " " + getOutputDir().getAbsolutePath();
	}

	private String getJoinTableCmd() throws Exception
	{
		return getClassifierExe() + JOIN_TABLE_CMD_SUFFIX;
	}

	private String getJoinTableLine( final String key ) throws Exception
	{
		return FUNCTION_JOIN_HN2_TABLES + " " + key;
	}

	private File getMergedReadFile( final File file ) throws Exception
	{
		return new File( getTempSubDir( TEMP_MERGE_READ_DIR ).getAbsoluteFile() + File.separator
				+ SeqUtil.getSampleId( file.getName() ) + BioLockJUtil.fileExt( file ) );
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

	private String getRenormPath( final String units, final String mode ) throws Exception
	{
		return outputPath( PATH_ABUNDANCE + "_" + units + ( mode == null ? "": "_" + mode ) );
	}

	private String getRenormTableCmd() throws Exception
	{
		return getClassifierExe() + RENORM_TABLE_CMD_SUFFIX;
	}

	private File getTempSubDir( final String name ) throws Exception
	{
		final File dir = new File( getTempDir().getAbsolutePath() + File.separator + name );
		if( !dir.exists() )
		{
			dir.mkdirs();
		}
		return dir;
	}

	private String outputPath( final String key ) throws Exception
	{
		return getOutputDir().getAbsolutePath() + File.separator + SUMMARY_PREFIX + key + TSV_EXT + " ";
	}

	private void validateRenormModes() throws Exception
	{
		for( final String val: Config.getSet( HN2_RENORM_MODES ) )
		{
			if( !val.equals( RENORM_MODE_OPTION_COMMUNITY ) && !val.equals( RENORM_MODE_OPTION_LEVELWISE ) )
			{
				throw new ConfigFormatException( HN2_RENORM_MODES, val + " is an invalid option.  Valid options = { "
						+ RENORM_MODE_OPTION_COMMUNITY + ", " + RENORM_MODE_OPTION_LEVELWISE + " }" );
			}
		}
	}

	private void validateRenormUnits() throws Exception
	{
		for( final String val: Config.getSet( HN2_RENORM_UNITS ) )
		{
			if( !val.equals( RENORM_UNITS_OPTION_CPM ) && !val.equals( RENORM_UNITS_OPTION_RELAB ) )
			{
				throw new ConfigFormatException( HN2_RENORM_UNITS, val + " is an invalid option.  Valid options = { "
						+ RENORM_UNITS_OPTION_CPM + ", " + RENORM_UNITS_OPTION_RELAB + " }" );
			}
		}
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
	 * {@link biolockj.Config} Directory property may contain protein nucleotide database files: {@value #HN2_PROT_DB}
	 */
	protected static final String HN2_PROT_DB = "humann2.protDB";

	/**
	 * {@link biolockj.Config} Directory property may contain protein nucleotide database files: {@value #HN2_PROT_DB}
	 */
	protected static final String HN2_RENORM_MODES = "humann2.renormMode";

	/**
	 * {@link biolockj.Config} Directory property may contain protein nucleotide database files: {@value #HN2_PROT_DB}
	 */
	protected static final String HN2_RENORM_UNITS = "humann2.renormUnits";

	/**
	 * HumanN2 command suffix used to join sample abundance tables together: {@value #JOIN_TABLE_CMD_SUFFIX}
	 */
	protected static final String JOIN_TABLE_CMD_SUFFIX = "_join_tables";

	/**
	 * HumanN2 command suffix used to renormalize joined sample abundance tables together:
	 * {@value #RENORM_TABLE_CMD_SUFFIX}
	 */
	protected static final String RENORM_TABLE_CMD_SUFFIX = "_renorm_table ";

	private static final String FILE_NAME_PARAM = "--file_name ";
	private static final String HN2_BASH_COMMENT = "# Run sample through HMP Unified Metabolic Analysis Network";
	private static final String INPUT_PARAM = "-i ";

	private static final String JOIN_BASH_COMMENT = "";
	private static final String NUCL_DB_PARAM = "--nucleotide-database ";
	private static final String NUM_THREADS_PARAM = "--threads";
	private static final String OUTPUT_PARAM = "-o ";
	private static final String PATH_ABUNDANCE = "pathabundance";
	private static final String PROT_DB_PARAM = "--protein-database ";
	private static final String RENORM_BASH_COMMENT = "";
	private static final String RENORM_MODE_OPTION_COMMUNITY = "community";
	private static final String RENORM_MODE_OPTION_LEVELWISE = "levelwise";
	private static final String RENORM_MODE_PARAM = "-m ";
	private static final String RENORM_UNITS_OPTION_CPM = "cpm";
	private static final String RENORM_UNITS_OPTION_RELAB = "relab";
	private static final String RENORM_UNITS_PARAM = "--units ";
	private static final String SUMMARY_PREFIX = "summary_";
	private static final String TEMP_HN2_OUTPUT_DIR = "humann2_cmd_output";
	private static final String TEMP_MERGE_READ_DIR = "merged";
}
