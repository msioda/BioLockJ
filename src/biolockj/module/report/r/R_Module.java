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
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
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

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.getPositiveInteger( this, R_TIMEOUT );
		Config.getBoolean( this, R_DEBUG );
		Config.getBoolean( this, R_SAVE_R_DATA );

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
		final List<String> preReqs = new ArrayList<>();
		if( !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_R_INPUT_TYPE ) )
		{
			preReqs.add( getMetaMergedModule() );
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
					final IOFileFilter ff = new WildcardFileFilter( "*" + LOG_EXT );
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
	 * Get correct meta-merged BioModule type for the give module. This is determined by examining previous configured
	 * modules to see what type of raw count tables are generated.
	 * 
	 * @return MetaMerged BioModule
	 * @throws Exception if errors occur
	 */
	protected String getMetaMergedModule() throws Exception
	{
		if( PathwayUtil.useHumann2RawCount( this ) )
		{
			return AddMetadataToPathwayTables.class.getName();
		}

		return AddMetadataToTaxaTables.class.getName();
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

	private String getErrors() throws Exception
	{
		final IOFileFilter ff = new WildcardFileFilter( "*" + Constants.SCRIPT_FAILURES );
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

	private File getFunctionLib() throws Exception
	{
		final File rFile = new File( getRTemplateDir() + R_FUNCTION_LIB );
		if( !rFile.exists() )
		{
			throw new Exception( "Missing R function library: " + rFile.getAbsolutePath() );
		}

		return rFile;
	}

	private String getModuleScriptName() throws Exception
	{
		return getClass().getSimpleName() + biolockj.Constants.R_EXT;
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
	protected static void writeScript( final BufferedWriter writer, final String rCode ) throws Exception
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
	 * {@link biolockj.Config} property {@value #EXE_RSCRIPT} defines the command line executable to call RScript
	 */
	protected static final String EXE_RSCRIPT = "exe.Rscript";

	/**
	 * {@link biolockj.Config} property {@value #P_VAL_CUTOFF} defines the p-value cutoff for significance
	 */
	protected static final String P_VAL_CUTOFF = "r.pvalCutoff";

	/**
	 * {@link biolockj.Config} property {@value #R_COLOR_BASE} defines the base label color
	 */
	protected static final String R_COLOR_BASE = "r.colorBase";

	/**
	 * {@link biolockj.Config} property {@value #R_COLOR_HIGHLIGHT} defines the highlight label color
	 */
	protected static final String R_COLOR_HIGHLIGHT = "r.colorHighlight";

	/**
	 * {@link biolockj.Config} property {@value #R_COLOR_PALETTE} defines the color palette for PDF plots
	 */
	protected static final String R_COLOR_PALETTE = "r.colorPalette";

	/**
	 * {@link biolockj.Config} property {@value #R_COLOR_POINT} defines the pch point colors for PDF plots
	 */
	protected static final String R_COLOR_POINT = "r.colorPoint";

	/**
	 * {@link biolockj.Config} boolean property {@value #R_DEBUG} sets the debug log function endabled
	 */
	protected static final String R_DEBUG = "r.debug";

	/**
	 * This library script contains helper functions used in the R scripts: {@value #R_FUNCTION_LIB}
	 */
	protected static final String R_FUNCTION_LIB = "BioLockJ_Lib.R";

	/**
	 * This main R script that sources helper libraries and calls modules main method function: {@value #R_MAIN_SCRIPT}
	 */
	protected static final String R_MAIN_SCRIPT = "BioLockJ_MAIN.R";

	/**
	 * {@link biolockj.Config} property {@value #R_PCH} defines the plot point shape for PDF plots
	 */
	protected static final String R_PCH = "r.pch";

	/**
	 * {@link biolockj.Config} Double property {@value #R_RARE_OTU_THRESHOLD} defines number OTUs needed to includ in
	 * reports
	 */
	protected static final String R_RARE_OTU_THRESHOLD = "r.rareOtuThreshold";

	/**
	 * {@link biolockj.Config} boolean property {@value #R_SAVE_R_DATA} enables the .RData file to save.
	 */
	protected static final String R_SAVE_R_DATA = "r.saveRData";

	/**
	 * {@link biolockj.Config} property {@value #R_TIMEOUT} defines the number of minutes before R script fails due to
	 * timeout. If undefined, no timeout is used.
	 */
	protected static final String R_TIMEOUT = "r.timeout";

	private static final String FUNCTION_RUN_R = "runScript";
	private static final String INDENT = "   ";
}
