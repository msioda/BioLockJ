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
package biolockj.module.r;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.ScriptModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.module.report.AddMetadataToOtuTables;
import biolockj.util.BashScriptBuilder;
import biolockj.util.BioLockJUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.RMetaUtil;
import biolockj.util.RuntimeParamUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule is the superclass for R script generating modules.
 */
public abstract class R_Module extends ScriptModuleImpl implements ScriptModule
{
	/**
	 * Return the set of file extensions available for download by {@link biolockj.util.DownloadUtil} 
	 * Add {@value #TSV_EXT} to super class set.
	 * 
	 * @return Set of file extensions
	 * @throws Exception if errors occur
	 */
	public Set<String> scpExtensions() throws Exception
	{
		final TreeSet<String> set = new TreeSet<>();
		set.add( LOG_EXT.substring( 1 ) );
		set.add( R_EXT.substring( 1 ) );
		
		if( Config.getBoolean( R_Module.R_SAVE_R_DATA ) )
		{
			set.add( R_DATA_EXT.substring( 1 ) );
		}

		return set;
	}
	

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
	 * Build the R module primary script.
	 *
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} properties are invalid or missing
	 */
	public String buildPrimaryScript() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( buildGetModuleScriptDirFunction() + RETURN );
		sb.append( "source( file.path( " + R_FUNCTION_GET_MOD_SCRIPT_DIR + "(), \"" + R_FUNCTION_LIB + "\" ) )" + RETURN );
		sb.append( "source( file.path( " + R_FUNCTION_GET_MOD_SCRIPT_DIR + "(), \"" + getModuleScriptName() + "\" ) )"
				+ RETURN );
		sb.append( "moduleScript = file.path( " + R_FUNCTION_GET_MOD_SCRIPT_DIR + "(), \"" + getMainScriptName()
				+ "\" )" + RETURN );
		sb.append( METHOD_RUN_PROGRAM + "( moduleScript )" + RETURN );
		sb.append( METHOD_REPORT_STATUS + "( moduleScript )" + RETURN );
		if( Config.getBoolean( R_SAVE_R_DATA ) )
		{
			sb.append( "save.image( file.path( getModuleDir(), \"output\", \"" + getClass().getSimpleName() + R_DATA_EXT
					+ "\" ) )" + RETURN );
		}
		sb.append( "sessionInfo()" + RETURN );

		return sb.toString();
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
		Config.getPositiveInteger( R_TIMEOUT );
		Config.requirePositiveInteger( R_PLOT_WIDTH );
	}

	/**
	 * Builds an R script by calling sub-methods to builds the BaseScript and creates the MAIN script shell that sources
	 * the BaseScript, calls runProgram(), reportStatus() and main() which can only be implemented in a subclass.<br>
	 * 
	 * <ol>
	 * <li>Call {@link biolockj.util.RMetaUtil#classifyReportableMetadata()}
	 * <li>Call {@link #writePrimaryScript()}
	 * </ol>
	 */
	@Override
	public void executeTask() throws Exception
	{
		RMetaUtil.classifyReportableMetadata();
		writePrimaryScript();

		if( RuntimeParamUtil.isDockerMode() )
		{
			BashScriptBuilder.buildScripts( this, buildDockerBashScript(), 1 );
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
	 * {@link biolockj.util.ModuleUtil#getMainScript(BioModule)}.
	 */
	@Override
	public String[] getJobParams() throws Exception
	{
		Log.info( getClass(), "Executing Script: " + ModuleUtil.getMainScript( this ).getName() );
		if( RuntimeParamUtil.isDockerMode() )
		{
			return new String[] { ModuleUtil.getMainScript( this ).getAbsolutePath() };
		}
		else
		{
			final String[] cmd = new String[ 2 ];
			cmd[ 0 ] = Config.getExe( EXE_RSCRIPT );
			cmd[ 1 ] = ModuleUtil.getMainScript( this ).getAbsolutePath();
			return cmd;
		}
	}

	/**
	 * Get the main R script prefix, typically {@value #MAIN_R_SCRIPT} except when running in Docker mode, which instead
	 * uses a numeric prefix since {@value #MAIN_R_SCRIPT} will be reserved for the MAIN bash script.
	 * 
	 * @return MAIN script prefix
	 * @throws Exception if errors occur
	 */
	public String getMainRscriptPrefix() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			return ModuleUtil.getModuleNum( this ) + ".0_";
		}
		return MAIN_SCRIPT_PREFIX;
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
	 * All R modules require combined OTU-metadata tables.
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		final List<Class<?>> preReqs = super.getPreRequisiteModules();
		preReqs.add( AddMetadataToOtuTables.class );
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
		return new File( getScriptDir().getAbsolutePath() + File.separator + getMainScriptName() );
	}

	/**
	 * Get the BioLockJ resource R directory.
	 * 
	 * @return System file path
	 * @throws Exception if errors occur
	 */
	public String getRTemplateDir() throws Exception
	{
		return BioLockJUtil.getSource().getAbsolutePath() + File.separator + "resources" + File.separator + "R"
				+ File.separator;
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
				else if( file.getName().indexOf( "." ) > 0 )
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

			final File rScript = ModuleUtil.getMainScript( this );
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

				if( Config.getBoolean( R_DEBUG ) )
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
			return Config.getPositiveInteger( R_TIMEOUT );
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
		lines.add( Config.getExe( EXE_RSCRIPT ) + " $1" );
		lines.add( "}" );
		return lines;
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
		FileUtils.copyFileToDirectory( getFunctionLib(), getScriptDir() );
		FileUtils.copyFileToDirectory( getModuleScript(), getScriptDir() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getPrimaryScript() ) );
		writer.write( buildPrimaryScript() );
		writer.close();
	}

	private String getErrors() throws Exception
	{
		final IOFileFilter ff = new WildcardFileFilter( "*" + R_EXT + "_" + Pipeline.SCRIPT_FAILURES );
		final Collection<File> scriptsFailed = FileUtils.listFiles( getScriptDir(), ff, null );
		if( scriptsFailed.isEmpty() )
		{
			return "";
		}
		final String rSpacer = "-------------------------------------";
		final StringBuffer errors = new StringBuffer();

		errors.append( INDENT + rSpacer + RETURN );
		errors.append( INDENT + "R Script Errors:" + RETURN );
		final BufferedReader reader = SeqUtil.getFileReader( scriptsFailed.iterator().next() );
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

	private String getMainScriptName() throws Exception
	{
		return getMainRscriptPrefix() + getModuleScriptName();
	}

	private String getModuleScriptName() throws Exception
	{
		return getClass().getSimpleName() + R_EXT;
	}

	public static String buildGetModuleScriptDirFunction() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( RETURN );
		sb.append( "# Return script directory path" + RETURN );
		sb.append( R_FUNCTION_GET_MOD_SCRIPT_DIR + " <- function() {" + RETURN );
		sb.append( "   initial.options = commandArgs(trailingOnly = FALSE)" + RETURN );
		sb.append( "   script.name <- sub(\"--file=\", \"\", initial.options[grep(\"--file=\", initial.options)])"
				+ RETURN );
		sb.append( "   if( length( script.name ) == 0 ) {" + RETURN );
		sb.append( "       stop( \"BioLockJ_Lib.R is not interactive - use RScript to execute.\" )" + RETURN );
		sb.append( "   }" + RETURN );
		sb.append( "   return( dirname( script.name ) )" + RETURN );
		sb.append( "}" + RETURN );
		return sb.toString();
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
		final StringTokenizer st = new StringTokenizer( rCode, BioLockJ.RETURN );
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

			writer.write( line + BioLockJ.RETURN );

			if( line.endsWith( "{" ) )
			{
				indentCount++;
			}
		}

		writer.close();
	}

	/**
	 * R function to get module script dir path: {@value #R_FUNCTION_GET_MOD_SCRIPT_DIR}
	 */
	public static String R_FUNCTION_GET_MOD_SCRIPT_DIR = "getModuleScriptDir";

	/**
	 * This directory is added to the MAIN script: {@value #MODULE_DIR}
	 */
	public static final String MODULE_DIR = "moduleDir";

	/**
	 * File extension of BioLockJ generated R Scripts: {@value #R_EXT}
	 */
	public static final String R_EXT = ".R";

	/**
	 * This library script contains helper functions used in the R scripts: {@value #R_FUNCTION_LIB}
	 */
	public static final String R_FUNCTION_LIB = "BioLockJ_Lib.R";

	/**
	 * File extension of TSV (tab-separated text) files read or generated by R_Modules: {@value #TSV_EXT}
	 */
	public static final String TSV_EXT = ".tsv";
	
	/**
	 * Method name in BioLockJ_Lib.R to report status of script: {@value #METHOD_REPORT_STATUS}
	 */
	public static final String METHOD_REPORT_STATUS = "reportStatus";
	
	
	/**
	 * Method name in BioLockJ_Lib.R to run script program: {@value #METHOD_RUN_PROGRAM}
	 */
	public static final String METHOD_RUN_PROGRAM = "runProgram";
	
	/**
	 * File extension of PDF file generated by plot building R_Modules: {@value #PDF_EXT}
	 */
	protected static final String PDF_EXT = ".pdf";
	

	/**
	 * {@link biolockj.Config} property {@value #EXE_RSCRIPT} defines the command line executable to call RScript
	 */
	protected static final String EXE_RSCRIPT = "exe.Rscript";

	/**
	 * {@link biolockj.Config} boolean property {@value #R_DEBUG} sets the debug log function endabled
	 */
	protected static final String R_DEBUG = "r.debug";

	/**
	 * {@link biolockj.Config} Integer property {@value #R_PLOT_WIDTH} sets the plot width in the PDF reports.
	 */
	protected static final String R_PLOT_WIDTH = "r.plotWidth";

	/**
	 * {@link biolockj.Config} boolean property {@value #R_SAVE_R_DATA} enables the .RData file to save.
	 */
	public static final String R_SAVE_R_DATA = "r.saveRData";

	/**
	 * {@link biolockj.Config} property {@value #R_TIMEOUT} defines the number of minutes before R script fails due to
	 * timeout. If undefined, no timeout is used.
	 */
	protected static final String R_TIMEOUT = "r.timeout";

	private static final String DEBUG_LOG_PREFIX = "debug_";
	private static final String FUNCTION_RUN_R = "runScript";
	private static final String INDENT = "   ";
	private static final String LOG_EXT = ".log";
	
	
	/**
	 * File extension for R save.image() command output: {@value #R_DATA_EXT}
	 */
	public static final String R_DATA_EXT = ".RData";
}
