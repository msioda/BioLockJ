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
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.*;

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
 * 
 * @blj.web_desc HumanN2 Classifier
 */
public class Humann2Classifier extends ClassifierModuleImpl implements ClassifierModule
{

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			File hn2InputSeq = file;
			final ArrayList<String> lines = new ArrayList<>();
			if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) )
			{
				lines.add( getPairedReadLine( file ) );
				hn2InputSeq = getMergedReadFile( file );
			}

			lines.add( FUNCTION_RUN_HN2 + " " + hn2InputSeq.getAbsolutePath() );

			// #copying metaphlan2 tables into metaphlan2_output
			// find . -iname *list.tsv -exec cp '{}' ./metaphlan2_output \;
			// #Merging metaphlan2 files
			// humann2_join_tables -i metaphlan2_output -o metaphlan2.tsv --file_name bugs_list

			data.add( lines );
		}

		final ArrayList<String> lines = new ArrayList<>();
		lines.add( FUNCTION_BUILD_SUMMARY_TABLES );
		data.add( lines );
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
	 * {@link biolockj.Config}.{@value #EXE_HUMANN2}{@value biolockj.Constants#PARAMS}. Also verify:
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
		getParams( EXE_HUMANN2_JOIN_PARAMS );
		getParams( EXE_HUMANN2_RENORM_PARAMS );
		PathwayUtil.verifyConfig( this );
	}

	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		pairedReads = null;
	}

	/**
	 * Get kraken executable command: {@value #EXE_HUMANN2}
	 */
	@Override
	public String getClassifierExe() throws Exception
	{
		return Config.getExe( this, EXE_HUMANN2 );
	}

	/**
	 * Obtain the humann2 runtime params
	 */
	@Override
	public List<String> getClassifierParams() throws Exception
	{
		final List<String> res = new ArrayList<>();

		for( final String val: Config.getList( this, EXE_HUMANN2_PARAMS ) )
		{
			if( val.startsWith( INPUT_PARAM ) || val.startsWith( LONG_INPUT_PARAM ) || val.startsWith( OUTPUT_PARAM )
					|| val.startsWith( LONG_OUTPUT_PARAM ) )
			{
				Log.warn( getClass(),
						"Ignore runtime option [ " + val + " ] set in Config property: " + EXE_HUMANN2_PARAMS
								+ " because this value is set by BioLockJ at runtime based on pipeline context" );
			}
			else if( val.startsWith( NUCL_DB_PARAM ) )
			{
				Log.warn( getClass(),
						"Ignore runtime option [ " + val + " ] set in Config property: " + EXE_HUMANN2_PARAMS
								+ " because this value is set by BioLockJ Config property: " + HN2_NUCL_DB );
			}
			else if( val.startsWith( PROT_DB_PARAM ) )
			{
				Log.warn( getClass(),
						"Ignore runtime option [ " + val + " ] set in Config property: " + EXE_HUMANN2_PARAMS
								+ " because this value is set by BioLockJ Config property: " + HN2_PROT_DB );
			}
			else
			{
				res.add( val );
			}
		}
		return res;
	}

	@Override
	public File getDB() throws Exception
	{
		final String nuclPath = Config.requireString( this, HN2_NUCL_DB );
		final String protPath = Config.requireString( this, HN2_PROT_DB );
		
		if( RuntimeParamUtil.runAws() )
		{
			return new File( DockerUtil.AWS_DB );
		}
		
		final File nuclDb = new File( Config.getSystemFilePath( nuclPath ) );
		final File protDb = new File( Config.getSystemFilePath( protPath ) );
		final File parentDir = BioLockJUtil.getCommonParent( nuclDb, protDb );
		Log.info( getClass(), "Found common database dir: " + parentDir.getAbsolutePath() );
		return parentDir;
	}

	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			sb.append( "HumanN2 nucleotide DB: " + getDbPath( HN2_NUCL_DB ) );
			sb.append( "HumanN2 protein DB: " + getDbPath( HN2_PROT_DB ) );
		}
		catch( final Exception ex )
		{
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( getClass(), msg );
		}

		return super.getSummary() + sb.toString();
	}

	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) )
		{
			lines.add( "function " + FUNCTION_CONCAT_PAIRED_READS + "() {" );
			lines.add(
					"cat $1 $2 > " + getTempSubDir( TEMP_MERGE_READ_DIR ).getAbsolutePath() + File.separator + "$3" );
			lines.add( "}" + RETURN );
		}

		lines.add( HN2_BASH_COMMENT );
		lines.add( "function " + FUNCTION_RUN_HN2 + "() {" );
		lines.add( getClassifierExe() + " " + getRuntimeParams() + INPUT_PARAM + " $1 " + OUTPUT_PARAM + " "
				+ getTempSubDir( FUNCTION_RUN_HN2 ) );
		lines.add( "}" + RETURN );

		lines.add( JOIN_BASH_COMMENT );
		lines.add( "function " + FUNCTION_JOIN_HN2_TABLES + "() {" );
		lines.add( getJoinTableCmd() + getParams( EXE_HUMANN2_JOIN_PARAMS ) + INPUT_PARAM + " $1 " + OUTPUT_PARAM
				+ " $2 " + FILE_NAME_PARAM + " $3" );
		lines.add( "}" + RETURN );

		lines.add( RENORM_BASH_COMMENT );
		lines.add( "function " + FUNCTION_RENORM_HN2_TABLES + "() {" );
		lines.add( getRenormTableCmd() + getParams( EXE_HUMANN2_RENORM_PARAMS ) + INPUT_PARAM + " $1 " + OUTPUT_PARAM
				+ " $2" );
		lines.add( "}" + RETURN );
		lines.addAll( getBuildSummaryFunction() );
		return lines;
	}

	/**
	 * Get formatted runtime parameters and {@value #SCRIPT_NUM_THREADS}
	 *
	 * @return Formatted runtime switches
	 * @throws Exception if errors occur
	 */
	protected String getRuntimeParams() throws Exception
	{
		return getRuntimeParams( getClassifierParams(), NUM_THREADS_PARAM ) + RM_STRATIFIED_OUTPUT + " " + NUCL_DB_PARAM
				+ " " + getDbPath( HN2_NUCL_DB ) + " " + PROT_DB_PARAM + " " + getDbPath( HN2_PROT_DB ) + " ";
	}

	private List<String> getBuildSummaryFunction() throws Exception
	{
		final List<String> lines = new ArrayList<>();
		lines.add( BUILD_SUMMARY_BASH_COMMENT );
		lines.add( "function " + FUNCTION_BUILD_SUMMARY_TABLES + "() {" );
		lines.add( "numStarted=1" );
		lines.add( "numComplete=0" );
		lines.add( "while [ $numStarted != $numComplete ]; do " );
		lines.add( "numStarted=$(ls \"" + getScriptDir().getAbsolutePath() + File.separator + "\"*"
				+ Constants.SCRIPT_STARTED + " | wc -l)" );
		lines.add( "numComplete=$(ls \"" + getScriptDir().getAbsolutePath() + File.separator + "\"*"
				+ Constants.SCRIPT_SUCCESS + " | wc -l)" );
		lines.add( "let \"numComplete++\"" );
		lines.add( "[ $numStarted != $numComplete ] && sleep 30" );
		lines.add( "done" );
		if( !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_ABUNDANCE ) )
		{
			lines.add( getJoinTableLine( HN2_PATH_ABUNDANCE ) );
			lines.add( getRenormTableLine( HN2_PATH_ABUNDANCE, Constants.HN2_PATH_ABUND_SUM ) );
		}
		if( !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_COVERAGE ) )
		{
			lines.add( getJoinTableLine( HN2_PATH_COVERAGE ) );
			lines.add( getRenormTableLine( HN2_PATH_COVERAGE, Constants.HN2_PATH_COVG_SUM ) );
		}
		if( !Config.getBoolean( this, Constants.HN2_DISABLE_GENE_FAMILIES ) )
		{
			lines.add( getJoinTableLine( HN2_GENE_FAMILIES ) );
			lines.add( getRenormTableLine( HN2_GENE_FAMILIES, Constants.HN2_GENE_FAM_SUM ) );
		}
		lines.add( "}" + RETURN );
		return lines;
	}


	/**
	 * Typically in Docker both databases are expected to have the same parent dir
	 * humann2.nuclDB=/db/chocophlan
	 * humann2.protDB=/db/uniref
	 */
	private String getDbPath( final String prop ) throws Exception
	{
		final File db = new File( Config.getSystemFilePath( Config.requireString( this, prop ) ) );
		return RuntimeParamUtil.isDockerMode() ? db.getAbsolutePath().replace( db.getParentFile().getAbsolutePath(), DockerUtil.CONTAINER_DB_DIR )
				: Config.requireExistingDir( this, prop ).getAbsolutePath();
	}

	private String getJoinTableCmd() throws Exception
	{
		return getClassifierExe() + JOIN_TABLE_CMD_SUFFIX;
	}

	private String getJoinTableLine( final String key ) throws Exception
	{
		return FUNCTION_JOIN_HN2_TABLES + " " + getTempSubDir( FUNCTION_RUN_HN2 ) + " "
				+ summaryFile( getTempSubDir( JOIN_OUT_DIR ), key ) + " " + key;
	}

	private File getMergedReadFile( final File file ) throws Exception
	{
		return new File( getTempSubDir( TEMP_MERGE_READ_DIR ).getAbsoluteFile() + File.separator
				+ SeqUtil.getSampleId( file.getName() ) + BioLockJUtil.fileExt( file ) );
	}

	private String getPairedReadLine( final File file ) throws Exception
	{
		return FUNCTION_CONCAT_PAIRED_READS + " " + file.getAbsolutePath() + " "
				+ getPairedReads().get( file ).getAbsolutePath() + " " + SeqUtil.getSampleId( file.getName() )
				+ BioLockJUtil.fileExt( file );
	}

	private Map<File, File> getPairedReads() throws Exception
	{
		if( pairedReads == null && Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) )
		{
			pairedReads = SeqUtil.getPairedReads( getInputFiles() );
		}

		return pairedReads;
	}

	private String getParams( final String property ) throws Exception
	{
		String params = " ";
		for( final String val: Config.getList( this, property ) )
		{
			if( val.startsWith( INPUT_PARAM ) || val.startsWith( LONG_INPUT_PARAM ) || val.startsWith( OUTPUT_PARAM )
					|| val.startsWith( LONG_OUTPUT_PARAM ) )
			{
				Log.warn( getClass(), "Ignore runtime option [ " + val + " ] set in Config property: " + property
						+ " because this value is set by BioLockJ at runtime based on pipeline context" );
			}
			else
			{
				params = val + " ";
			}
		}
		return params;
	}

	private String getRenormTableCmd() throws Exception
	{
		return getClassifierExe() + RENORM_TABLE_CMD_SUFFIX;
	}

	private String getRenormTableLine( final String input, final String output ) throws Exception
	{
		return FUNCTION_RENORM_HN2_TABLES + " " + summaryFile( getTempSubDir( JOIN_OUT_DIR ), input ) + " "
				+ getOutputDir().getAbsolutePath() + File.separator + PathwayUtil.getHn2ClassifierOutput( output );
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

	private String summaryFile( final File dir, final String key ) throws Exception
	{
		return dir + File.separator + Config.pipelineName() + "_" + key + TSV_EXT;
	}

	private Map<File, File> pairedReads = null;

	/**
	 * {@link biolockj.Config} exe property for humnan2 executable: {@value #EXE_HUMANN2}
	 */
	protected static final String EXE_HUMANN2 = "exe.humann2";

	/**
	 * {@link biolockj.Config} List property used to obtain the humann2_join_tables executable params
	 */
	protected static final String EXE_HUMANN2_JOIN_PARAMS = "exe.humann2JoinTableParams";

	/**
	 * {@link biolockj.Config} List property used to obtain the humann2 executable params
	 */
	protected static final String EXE_HUMANN2_PARAMS = "exe.humann2Params";

	/**
	 * {@link biolockj.Config} List property used to obtain the humann2_renorm_table executable params
	 */
	protected static final String EXE_HUMANN2_RENORM_PARAMS = "exe.humann2RenormTableParams";

	/**
	 * {@link biolockj.Config} Directory property must contain the nucleotide database: {@value #HN2_NUCL_DB}
	 */
	protected static final String HN2_NUCL_DB = "humann2.nuclDB";

	/**
	 * {@link biolockj.Config} Directory property must contain the protein nucleotide database: {@value #HN2_PROT_DB}
	 */
	protected static final String HN2_PROT_DB = "humann2.protDB";

	private static final String BUILD_SUMMARY_BASH_COMMENT = "# Wait until all worker scripts are complete to build summary tables";
	private static final String FILE_NAME_PARAM = "--file_name";
	private static final String FUNCTION_BUILD_SUMMARY_TABLES = "buildSummaryTables";
	private static final String FUNCTION_CONCAT_PAIRED_READS = "mergePairedReads";
	private static final String FUNCTION_JOIN_HN2_TABLES = "joinHn2Tables";
	private static final String FUNCTION_RENORM_HN2_TABLES = "renormHn2Tables";
	private static final String FUNCTION_RUN_HN2 = "runHn2";
	private static final String HN2_BASH_COMMENT = "# Run sample through HMP Unified Metabolic Analysis Network";
	private static final String HN2_GENE_FAMILIES = "genefamilies";
	private static final String HN2_PATH_ABUNDANCE = "pathabundance";
	private static final String HN2_PATH_COVERAGE = "pathcoverage";
	private static final String INPUT_PARAM = "-i";
	private static final String JOIN_BASH_COMMENT = "# Pool data of a given type output for each individual sample";
	private static final String JOIN_OUT_DIR = "joined_tables";
	private static final String JOIN_TABLE_CMD_SUFFIX = "_join_tables";
	private static final String LONG_INPUT_PARAM = "--input";
	private static final String LONG_OUTPUT_PARAM = "--output";
	private static final String NUCL_DB_PARAM = "--nucleotide-database";
	private static final String NUM_THREADS_PARAM = "--threads";
	private static final String OUTPUT_PARAM = "-o";
	private static final String PROT_DB_PARAM = "--protein-database";
	private static final String RENORM_BASH_COMMENT = "# Renormalize output summary tables" + RETURN
			+ "# Renorm unit options: counts/million (default) or relative abundance" + RETURN
			+ "# Renorm mode options: community (default) or levelwise";
	private static final String RENORM_TABLE_CMD_SUFFIX = "_renorm_table";
	private static final String RM_STRATIFIED_OUTPUT = "--remove-stratified-output";
	private static final String TEMP_MERGE_READ_DIR = "merged";
}
