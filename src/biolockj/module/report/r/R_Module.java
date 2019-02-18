/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 18, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.r;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.*;
import biolockj.module.ScriptModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.module.report.humann2.AddMetadataToPathwayTables;
import biolockj.module.report.taxa.AddMetadataToTaxaTables;
import biolockj.util.*;

/**
 * This BioModule is the superclass for R script generating modules.
 */
public abstract class R_Module extends ScriptModuleImpl implements ScriptModule
{
	/**
	 * Run R script in docker.
	 * 
	 * @return Bash script lines for the docker script
	 * @throws Exception if errors occur
	 */
	public List<List<String>> buildDockerBashScript() throws Exception
	{
		final List<List<String>> dockerScriptLines = new ArrayList<>();
		final List<String> innerList = new ArrayList<>();
		innerList.add( FUNCTION_RUN_R + " " + getPrimaryScript().getAbsolutePath() );
		dockerScriptLines.add( innerList );
		return dockerScriptLines;
	}

	/**
	 * Not needed for R script modules.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		return null;
	}

	/**
	 * Validate configuration file properties used to build the R report:
	 * <ol>
	 * <li>Get positive integer {@link biolockj.Config}.{@value #R_TIMEOUT}
	 * <li>Get positive integer {@link biolockj.Config}.{@value #R_PLOT_WIDTH}
	 * </ol>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.getPositiveInteger( this, R_TIMEOUT );
		Config.requirePositiveInteger( this, R_PLOT_WIDTH );
		Config.requirePositiveDouble( this, P_VAL_CUTOFF );
	}

	/**
	 * Builds an R script by calling sub-methods to builds the BaseScript and creates the MAIN script shell that sources
	 * the BaseScript, calls runProgram(), reportStatus() and main() which can only be implemented in a subclass.<br>
	 */
	@Override
	public void executeTask() throws Exception
	{
		writePrimaryScript();

		if( RuntimeParamUtil.isDockerMode() )
		{
			BashScriptBuilder.buildScripts( this, buildDockerBashScript() );
		}
	}

	/**
	 * Get the function library script
	 * 
	 * @return Function library script
	 * @throws Exception if errors occur
	 */
	public File getFunctionLib() throws Exception
	{
		final File rFile = new File( getRTemplateDir() + R_FUNCTION_LIB );
		if( !rFile.exists() )
		{
			throw new Exception( "Missing R function library: " + rFile.getAbsolutePath() );
		}

		return rFile;
	}

	/**
	 * If running Docker, run the Docker bash script, otherwise:<br>
	 * Run {@link biolockj.Config}.{@value #EXE_RSCRIPT} command on the generated R Script:
	 * {@link ScriptModuleImpl#getMainScript()}.
	 */
	@Override
	public String[] getJobParams() throws Exception
	{
		Log.info( getClass(), "Run MAIN Script: " + getMainScript().getName() );
		if( RuntimeParamUtil.isDockerMode() )
		{
			return super.getJobParams();
		}
		else
		{
			final String[] cmd = new String[ 2 ];
			cmd[ 0 ] = Config.getExe( this, EXE_RSCRIPT );
			cmd[ 1 ] = getMainScript().getAbsolutePath();
			return cmd;
		}

	}

	/**
	 * Get the main R script
	 * 
	 * @return Main R script
	 * @throws Exception if errors occur
	 */
	public File getMainR() throws Exception
	{
		final File rFile = new File( getRTemplateDir() + R_MAIN_SCRIPT );
		if( !rFile.exists() )
		{
			throw new Exception( "Missing R function library: " + rFile.getAbsolutePath() );
		}

		return rFile;
	}

	/**
	 * Get the Module script
	 * 
	 * @return Module R script
	 * @throws Exception if errors occur
	 */
	public File getModuleScript() throws Exception
	{
		final File rFile = new File( getRTemplateDir() + getModuleScriptName() );
		if( !rFile.exists() )
		{
			throw new Exception( "Missing R module script: " + rFile.getAbsolutePath() );
		}

		return rFile;
	}

	/**
	 * Require combined count-metadata tables as input.
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
	{
		numInit++;
		final List<String> preReqs = new ArrayList<>();
		if( !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_R_INPUT_TYPE ) )
		{
			if( isHumanN2() )
			{
				preReqs.add( AddMetadataToPathwayTables.class.getName() );
			}
			else
			{
				preReqs.add( AddMetadataToTaxaTables.class.getName() );
			}

		}
		preReqs.addAll( super.getPreRequisiteModules() );
		return preReqs;
	}

	/**
	 * Get the primary R script
	 * 
	 * @return File (R script)
	 * @throws Exception if errors occur
	 */
	public File getPrimaryScript() throws Exception
	{
		return new File(
				getScriptDir().getAbsolutePath() + File.separator + MAIN_SCRIPT_PREFIX + getModuleScriptName() );
	}

	/**
	 * Produce summary file counts for each file extension in the output directory and the number of log files in the
	 * temp directory. Any R Script errors detected during execution will also be printed. also contain details of R
	 * script errors, if any.
	 */
	@Override
	public String getSummary() throws Exception
	{
		try
		{
			final StringBuffer sb = new StringBuffer();
			final Map<String, Integer> map = new HashMap<>();
			for( final File file: getOutputDir().listFiles() )
			{
				if( !file.isFile() )
				{
					continue;
				}
				else if( file.getName().indexOf( "." ) > -1 )
				{
					final String ext = file.getName().substring( file.getName().lastIndexOf( "." ) + 1 );
					if( map.get( ext ) == null )
					{
						map.put( ext, 0 );
					}

					map.put( ext, map.get( ext ) + 1 );
				}
				else // no extension
				{
					if( map.get( "none" ) == null )
					{
						map.put( "none", 0 );
					}

					map.put( "none", map.get( "none" ) + 1 );
				}
			}

			final File rScript = getPrimaryScript();
			if( rScript == null || !rScript.exists() )
			{
				sb.append( "Failed to generate R Script!" + RETURN );
			}
			else
			{
				sb.append( getClass().getSimpleName() + ( getErrors().isEmpty() ? " successful": " failed" ) + ": "
						+ rScript.getAbsolutePath() + RETURN );

				for( final String ext: map.keySet() )
				{
					sb.append( "Generated " + map.get( ext ) + " " + ext + " files" + RETURN );
				}

				if( Config.getBoolean( this, R_DEBUG ) )
				{
					final IOFileFilter ff = new WildcardFileFilter( "*" + DEBUG_LOG_PREFIX + "*" + LOG_EXT );
					final Collection<File> debugLogs = FileUtils.listFiles( getTempDir(), ff, null );
					sb.append( "Generated " + debugLogs.size() + " log files" + RETURN );
				}

				sb.append( getErrors() );
			}

			return sb.toString();
		}
		catch( final Exception ex )
		{
			Log.warn( getClass(), "Unable to produce R_Module summary: " + ex.getMessage() );
		}

		return "Error occurred attempting to build R_Module summary!";
	}

	/**
	 * The R Script should run quickly, timeout = 10 minutes appears to work well.
	 */
	@Override
	public Integer getTimeout()
	{
		try
		{
			return Config.getPositiveInteger( this, R_TIMEOUT );
		}
		catch( final Exception ex )
		{
			Log.error( getClass(), ex.getMessage(), ex );
		}
		return null;
	}

	/**
	 * This method generates the bash function that calls the R script: runScript.
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_RUN_R + "() {" );
		lines.add( Config.getExe( this, EXE_RSCRIPT ) + " $1" );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * Add {@link biolockj.module.report.r.R_CalculateStats} to standard {@link #getPreRequisiteModules()}
	 * 
	 * @return Statistics Module prerequisite if needed
	 * @throws Exception if errors occur determining eligibility
	 */
	protected List<String> getStatPreReqs() throws Exception
	{
		final List<String> preReqs = super.getPreRequisiteModules();
		if( !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_STATS_TABLE_INPUT_TYPE ) )
		{
			preReqs.add( ModuleUtil.getDefaultStatsModule() );
		}
		return preReqs;
	}

	/**
	 * Initialize the R script by creating the MAIN R script that calls source on the BaseScript and adds the R code for
	 * the runProgarm() method.
	 *
	 * @throws Exception if unable to build the R script stub
	 */
	protected void writePrimaryScript() throws Exception
	{
		getTempDir();
		getOutputDir();
		FileUtils.copyFile( getMainR(), getPrimaryScript() );
		FileUtils.copyFileToDirectory( getFunctionLib(), getScriptDir() );
		FileUtils.copyFileToDirectory( getModuleScript(), getScriptDir() );
	}

	private Integer getClosestIndex( final List<Integer> indexes, final Integer target ) throws Exception
	{
		Integer hit = null;
		for( final Integer i: indexes )
		{
			if( i < target )
			{
				hit = i;
			}
		}
		return hit;
	}

	private String getErrors() throws Exception
	{
		final IOFileFilter ff = new WildcardFileFilter( "*" + Pipeline.SCRIPT_FAILURES );
		final Collection<File> scriptsFailed = FileUtils.listFiles( getScriptDir(), ff, null );
		if( scriptsFailed.isEmpty() )
		{
			return "";
		}
		final String rSpacer = "-------------------------------------";
		final StringBuffer errors = new StringBuffer();

		errors.append( INDENT + rSpacer + RETURN );
		errors.append( INDENT + "R Script Errors:" + RETURN );
		final BufferedReader reader = BioLockJUtil.getFileReader( scriptsFailed.iterator().next() );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				errors.append( INDENT + line + RETURN );
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}
		errors.append( INDENT + rSpacer + RETURN );
		return errors.toString();
	}

	private String getModuleScriptName() throws Exception
	{
		return getClass().getSimpleName() + R_EXT;
	}

	private Boolean isHumanN2() throws Exception
	{
		Log.debug( getClass(), "Check to see if R Module inherits Hn2 or Tax tables..." );
		final List<String> mods = Config.requireList( this, Constants.INTERNAL_BLJ_MODULE );
		final List<Integer> classifiers = new ArrayList<>();
		final List<Integer> parsers = new ArrayList<>();
		final List<Integer> hn2Classifiers = new ArrayList<>();
		final List<Integer> otherRModules = new ArrayList<>();
		final List<Integer> thisClass = new ArrayList<>();
		for( int i = 0; i < mods.size(); i++ )
		{
			final boolean isParser = mods.get( i ).toLowerCase().contains( "parser" );
			final boolean isClassifier = mods.get( i ).toLowerCase().contains( "classifier" );
			final boolean isHn2 = isClassifier && mods.get( i ).toLowerCase().contains( "humann2" );
			if( mods.get( i ).equals( getClass().getName() ) )
			{
				thisClass.add( i );
			}
			else if( mods.get( i ).startsWith( "R_" ) )
			{
				otherRModules.add( i );
			}
			else if( isHn2 )
			{
				hn2Classifiers.add( i );
			}
			else if( isClassifier )
			{
				classifiers.add( i );
			}
			else if( isParser )
			{
				parsers.add( i );
			}
		}

		final boolean hasPathwayInputs = BioLockJUtil
				.pipelineInputType( BioLockJUtil.PIPELINE_HUMANN2_COUNT_TABLE_INPUT_TYPE );
		final boolean hasTaxaInputs = BioLockJUtil
				.pipelineInputType( BioLockJUtil.PIPELINE_TAXA_COUNT_TABLE_INPUT_TYPE );
		Log.debug( getClass(), "hasPathwayInputs: " + hasPathwayInputs );
		Log.debug( getClass(), "hasTaxaInputs: " + hasTaxaInputs );
		if( hn2Classifiers.isEmpty() && classifiers.isEmpty() )
		{
			if( hasPathwayInputs && !hasTaxaInputs )
			{
				Log.debug( getClass(), "No classifier modules --> hasPathwayInputs && !hasTaxaInputs: return( TRUE )" );
				return true;
			}
			Log.debug( getClass(), "No classifier modules --> !hasPathwayInputs || hasTaxaInputs: return( FALSE )" );
			return false;
		}
		if( hn2Classifiers.isEmpty() )
		{
			Log.debug( getClass(), "No HN2 classifiers configured: return( FALSE )" );
			return false;
		}
		if( classifiers.isEmpty() )
		{
			Log.debug( getClass(), "No standard classifiers configured: return( TRUE )" );
			return true;
		}

		final Integer rIndex = thisClass.get( thisClass.size() == 1 ? 0: numInit - 1 );
		final Integer hn2Index = getClosestIndex( hn2Classifiers, rIndex );
		final Integer classifierIndex = getClosestIndex( classifiers, rIndex );
		final Integer parserIndex = getClosestIndex( parsers, rIndex );
		Log.debug( getClass(), "rIndex: " + ( rIndex == null ? "N/A": rIndex ) );
		Log.debug( getClass(), "hn2Index: " + ( hn2Index == null ? "N/A": hn2Index ) );
		Log.debug( getClass(), "classifierIndex: " + ( classifierIndex == null ? "N/A": classifierIndex ) );
		Log.debug( getClass(), "parserIndex: " + ( parserIndex == null ? "N/A": parserIndex ) );

		if( hn2Index == null )
		{
			Log.debug( getClass(), "No HN2 classifiers BEFORE R-module index so return( FALSE ): " + rIndex );
			return false;
		}
		boolean useHn2 = classifierIndex == null || classifierIndex < hn2Index;
		useHn2 = useHn2 && ( parserIndex == null || parserIndex < hn2Index );
		Log.debug( getClass(), "Final assessment --> use HumanN2 tables?  return( " + useHn2 + " ) " );
		return useHn2;
	}

	/**
	 * Get the BioLockJ resource R directory.
	 * 
	 * @return System file path
	 * @throws Exception if errors occur
	 */
	public static String getRTemplateDir() throws Exception
	{
		return BioLockJUtil.getBljDir().getAbsolutePath() + File.separator + "resources" + File.separator + "R"
				+ File.separator;
	}

	/**
	 * This method generates an R script with the given rCode saved to the given path.
	 *
	 * @param path Path to new R Script
	 * @param rCode R script code
	 * @throws Exception if I/O errors occur
	 */
	public static void writeNewScript( final String path, final String rCode ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( path ) );
		writeScript( writer, rCode );
	}

	/**
	 * This method formats the rCode to indent code blocks surround by curly-braces "{ }"
	 * 
	 * @param writer BufferedWriter writes to the file
	 * @param rCode R code
	 * @throws Exception if I/O errors occur
	 */
	private static void writeScript( final BufferedWriter writer, final String rCode ) throws Exception
	{
		int indentCount = 0;
		final StringTokenizer st = new StringTokenizer( rCode, Constants.RETURN );
		while( st.hasMoreTokens() )
		{
			final String line = st.nextToken();

			if( line.equals( "}" ) )
			{
				indentCount--;
			}

			int i = 0;
			while( i++ < indentCount )
			{
				writer.write( INDENT );
			}

			writer.write( line + Constants.RETURN );

			if( line.endsWith( "{" ) )
			{
				indentCount++;
			}
		}

		writer.close();
	}

	/**
	 * Method name in BioLockJ_Lib.R to report status of script: {@value #METHOD_REPORT_STATUS}
	 */
	public static final String METHOD_REPORT_STATUS = "reportStatus";

	/**
	 * Method name in BioLockJ_Lib.R to run script program: {@value #METHOD_RUN_PROGRAM}
	 */
	public static final String METHOD_RUN_PROGRAM = "runProgram";

	/**
	 * This directory is added to the MAIN script: {@value #MODULE_DIR}
	 */
	public static final String MODULE_DIR = "moduleDir";

	/**
	 * File extension for R save.image() command output: {@value #R_DATA_EXT}
	 */
	public static final String R_DATA_EXT = ".RData";

	/**
	 * File extension of BioLockJ generated R Scripts: {@value #R_EXT}
	 */
	public static final String R_EXT = ".R";

	/**
	 * R function to get module script file path: {@value #R_FUNCTION_GET_MOD_SCRIPT}
	 */
	public static final String R_FUNCTION_GET_MOD_SCRIPT = "getModuleScript";

	/**
	 * This library script contains helper functions used in the R scripts: {@value #R_FUNCTION_LIB}
	 */
	public static final String R_FUNCTION_LIB = "BioLockJ_Lib.R";

	/**
	 * This main R script that sources helper libraries and calls modules main method function: {@value #R_MAIN_SCRIPT}
	 */
	public static final String R_MAIN_SCRIPT = "BioLockJ_MAIN.R";

	/**
	 * {@link biolockj.Config} boolean property {@value #R_SAVE_R_DATA} enables the .RData file to save.
	 */
	public static final String R_SAVE_R_DATA = "r.saveRData";

	/**
	 * {@link biolockj.Config} property {@value #EXE_RSCRIPT} defines the command line executable to call RScript
	 */
	protected static final String EXE_RSCRIPT = "exe.Rscript";

	/**
	 * {@link biolockj.Config} property {@value #P_VAL_CUTOFF} defines the p-value cutoff for significance
	 */
	protected static final String P_VAL_CUTOFF = "r.pvalCutoff";

	/**
	 * {@link biolockj.Config} boolean property {@value #R_DEBUG} sets the debug log function endabled
	 */
	protected static final String R_DEBUG = "r.debug";

	/**
	 * {@link biolockj.Config} Integer property {@value #R_PLOT_WIDTH} sets the plot width in the PDF reports.
	 */
	protected static final String R_PLOT_WIDTH = "r.plotWidth";

	/**
	 * {@link biolockj.Config} property {@value #R_TIMEOUT} defines the number of minutes before R script fails due to
	 * timeout. If undefined, no timeout is used.
	 */
	protected static final String R_TIMEOUT = "r.timeout";

	private static final String DEBUG_LOG_PREFIX = "debug_";

	private static final String FUNCTION_RUN_R = "runScript";
	private static final String INDENT = "   ";
	private static int numInit = 0;
}
