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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.exception.ConfigFormatException;
import biolockj.module.BioModule;
import biolockj.module.ScriptModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.module.report.AddMetadataToOtuTables;
import biolockj.util.BashScriptBuilder;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.RuntimeParamUtil;
import biolockj.util.SeqUtil;
import biolockj.util.r.BaseScript;
import biolockj.util.r.RMetaUtil;

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
	 * <li>Verify query fields are valid via {@link #getOtuTableFilter()}
	 * </ol>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.getPositiveInteger( R_TIMEOUT );
		getOtuTableFilter();
	}

	/**
	 * Builds an R script by calling sub-methods to builds the BaseScript and creates the MAIN script shell that sources
	 * the BaseScript, calls runProgram(), reportStatus() and main() which can only be implemented in a subclass.<br>
	 * 
	 * <ol>
	 * <li>Call {@link biolockj.util.r.RMetaUtil#classifyReportableMetadata()}
	 * <li>Call {@link #initializePrimaryScript()}
	 * <li>Call {@link #rMethodImportLibs(List)}( {@link #getRequiredLibs()} )
	 * <li>Call {@link #updatePrimaryScript(String)}( {@link #rMainMethod()} )
	 * <li>Call {@link #updatePrimaryScript(String)}( {@link #rMethodReportStatus()} )
	 * <li>Call {@link #updatePrimaryScript(String)}( {@link #rMethodRunProgram()} )
	 * <li>Call {@link biolockj.util.r.BaseScript#buildScript(R_Module)}
	 * </ol>
	 */
	@Override
	public void executeTask() throws Exception
	{
		RMetaUtil.classifyReportableMetadata();
		initializePrimaryScript();
		updatePrimaryScript( rMainMethod() );
		updatePrimaryScript( rMethodImportLibs( getRequiredLibs() ) );
		updatePrimaryScript( rMethodReportStatus() );
		updatePrimaryScript( rMethodRunProgram() );
		BaseScript.buildScript( this );

		if( RuntimeParamUtil.isDockerMode() )
		{
			BashScriptBuilder.buildScripts( this, buildDockerBashScript(), 1 );
		}

	}

	/**
	 * If running Docker, run the Docker bash script, otherwise:<br>
	 * Run {@link biolockj.Config}.{@value #EXE_RSCRIPT} command on the generated R Script:
	 * {@link biolockj.util.ModuleUtil#getMainScript(BioModule)}.
	 */
	@Override
	public String[] getJobParams() throws Exception
	{
		Log.get( getClass() ).info( "Executing Script: " + ModuleUtil.getMainScript( this ).getName() );
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
		return new File( getScriptDir().getAbsolutePath() + File.separator + getMainRscriptPrefix()
				+ getClass().getSimpleName() + R_EXT );
		//
		// if( RuntimeParamUtil.isDockerMode() )
		// {
		// return ModuleUtil.getMainScript( this );
		// }
		// else
		// {
		// return new File( getScriptDir().getAbsolutePath() + File.separator + getMainRscriptPrefix()
		// + getClass().getSimpleName() + R_EXT );
		// }
	}

	/**
	 * Get the list of require libs that is passed to {@link #rMethodImportLibs} to import required R libraries
	 * 
	 * @return rCode
	 */
	public abstract List<String> getRequiredLibs();

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
			Log.get( getClass() ).warn( "Unable to produce R_Module summary: " + ex.getMessage() );
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
			Log.get( getClass() ).error( ex.getMessage(), ex );
		}
		return null;
	}

	/**
	 * This method generates the bash function: {@value #FUNCTION_RUN_R}
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
	 * The method returns the rCode wrapped in a sink() block if {@value #R_DEBUG} is enabled.<br>
	 * The log file is saved the the rModule temp directory with prefix {@value #DEBUG_LOG_PREFIX} and the suffix
	 * {@value #LOG_EXT}.
	 *
	 * @param rCode R code
	 * @param otuLevel OTU level
	 * @return R code wrapped in a sink() block
	 * @throws ConfigFormatException if {@link biolockj.Config} properties are invalid or missing
	 */
	protected String enableDebug( final String rCode, final String otuLevel ) throws ConfigFormatException
	{
		final StringBuffer sb = new StringBuffer();
		if( Config.getBoolean( R_DEBUG ) )
		{
			sb.append( getLine( "rLog = paste0(" + R_TEMP_DIR + ", \"" + DEBUG_LOG_PREFIX + getClass().getSimpleName()
					+ "_\", " + otuLevel + ", \"" + LOG_EXT + "\" )" ) );
			sb.append( getLine( "sink( rLog )" ) );
		}

		sb.append( rCode );

		if( Config.getBoolean( R_DEBUG ) )
		{
			sb.append( getLine( "sink()" ) );
		}

		return sb.toString();
	}

	/**
	 * This method wraps the R code input in an if statement to filter out rare OTUs. Rare OTUs are filtered out of the
	 * R reports as defined in property {@link biolockj.Config}.{@value #R_RARE_OTU_THRESHOLD}. If
	 * {@value #R_RARE_OTU_THRESHOLD} is between 0-1, it is considered a minimum percentage. If
	 * {@value #R_RARE_OTU_THRESHOLD} is greater than 1, it is a specific numeric value. OTUs not found in the number or
	 * percentage of samples configured will be omitted from the report.
	 *
	 * @param code R code to wrap in filter if-statement code block
	 * @return R code wrapped in the if-statement used to filter rare OTUs
	 * @throws Exception if {@link biolockj.Config}.{@value #R_RARE_OTU_THRESHOLD} is missing or invalid
	 */
	protected String filterRareOtus( final String code ) throws Exception
	{
		final Double otuThreshold = Config.getPositiveDoubleVal( R_RARE_OTU_THRESHOLD );
		if( otuThreshold != null && otuThreshold.doubleValue() != 1 )
		{
			String filterValue = null;
			if( Double.valueOf( otuThreshold ) < 1 )
			{
				filterValue = "( nrow(" + OTU_TABLE + ") * " + Double.valueOf( otuThreshold ).toString() + " )";
			}
			else
			{
				filterValue = Double.valueOf( otuThreshold ).toString();
			}
			final StringBuffer sb = new StringBuffer();
			sb.append( getLine( "# Filter rare OTUs as defined in: " + R_RARE_OTU_THRESHOLD ) );
			sb.append( getLine( "if( sum( " + OTU_TABLE + "[," + OTU_COL + "] > 0 ) >= " + filterValue + " ) {" ) );
			sb.append( code );
			sb.append( getLine( "}" ) );
			return sb.toString();
		}

		return code;
	}

	/**
	 * This method wraps in the R code input in a for-loop that iterates through all OTU column indexes by incrementing
	 * {@value #OTU_COL} from 2:lastOtuCol. The R code will then be executed for each OTU.
	 *
	 * @param code R code to wrap in for loop
	 * @return R code
	 */
	protected String forEachOtu( final String code )
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "lastOtuCol = ncol(" + OTU_TABLE + ") - " + NUM_META_COLS ) );
		sb.append( getLine( "for( " + OTU_COL + " in 2:lastOtuCol ) {" ) );
		sb.append( code );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * The method returns R code to run rCode for each taxonomy level configured.
	 *
	 * @param rCode R code
	 * @return R code wrapped in a for loop
	 */
	protected String forEachOtuLevel( final String rCode )
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "for( " + OTU_LEVEL + " in " + OTU_LEVELS + " ) {" ) );
		sb.append( rCode );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Build the otuTable filter after verifying the required {@link biolockj.Config} properties.
	 *
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} properties are invalid or missing
	 */
	protected List<String> getOtuTableFilter() throws Exception
	{
		RMetaUtil.verifyMetadataFieldsExist( R_FILTER_FIELDS, Config.getSet( R_FILTER_FIELDS ) );
		RMetaUtil.verifyMetadataFieldsExist( R_FILTER_NA_FIELDS, Config.getSet( R_FILTER_NA_FIELDS ) );

		final List<String> queryFilters = new ArrayList<>();
		final Iterator<String> nalIt = Config.getList( R_FILTER_NA_FIELDS ).iterator();
		final Iterator<String> attIt = Config.getList( R_FILTER_FIELDS ).iterator();
		final Iterator<String> opIt = Config.getList( R_FILTER_OPERATORS ).iterator();
		final Iterator<String> valIt = Config.getList( R_FILTER_VALUES ).iterator();

		if( Config.getList( R_FILTER_FIELDS ).size() != Config.getList( R_FILTER_OPERATORS ).size()
				|| Config.getList( R_FILTER_FIELDS ).size() != Config.getList( R_FILTER_VALUES ).size() )
		{
			throw new Exception(
					"CONFIG FILE ERROR >>> Each of these Config properties must be lists of the equal length [ "
							+ R_FILTER_FIELDS + ", " + R_FILTER_OPERATORS + ", " + R_FILTER_VALUES + " ]"
							+ "Current lengths = [ " + Config.getList( R_FILTER_FIELDS ).size() + ", " + " ]"
							+ Config.getList( R_FILTER_OPERATORS ).size() + ", "
							+ Config.getList( R_FILTER_VALUES ).size() );
		}

		while( attIt.hasNext() )
		{
			final String att = attIt.next();
			String val = valIt.next();
			if( !RMetaUtil.getNumericFields().contains( att ) && !att.startsWith( "\"" ) && !att.startsWith( "'" ) )
			{
				val = "\"" + val + "\"";
			}

			final String filter = "(" + OTU_TABLE + "$'" + att + "' " + opIt.next() + " " + val + " )";
			Log.get( getClass() ).info( "Adding filter: " + filter );
			queryFilters.add( filter );
		}

		while( nalIt.hasNext() )
		{
			queryFilters.add( "!is.na(" + OTU_TABLE + "$'" + nalIt.next() + "')" );
		}

		return queryFilters;
	}

	/**
	 * Initialize the R script by creating the MAIN R script that calls source on the BaseScript and adds the R code for
	 * the runProgarm() method.
	 *
	 * @throws Exception if unable to build the R script stub
	 */
	protected void initializePrimaryScript() throws Exception
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getPrimaryScript() ) );
		writer.write( "# BioLockJ generated script for project: "
				+ Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getName() + RETURN );
		writer.write( "source( \"" + BaseScript.getScriptPath( this ) + "\" )" + RETURN );
		writer.close();
	}

	/**
	 * This method initializes the R Report by parsing the OTU abundance table for column indexes.
	 *
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} properties are invalid or missing
	 */
	protected String rLinesCreateAttColVectors() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		if( !RMetaUtil.getBinaryFields().isEmpty() )
		{
			sb.append( getLine( BINARY_COLS + " = " + BaseScript.FUNCTION_GET_COL_INDEXES + "( " + OTU_TABLE + ", "
					+ BINARY_ATTS + " )", true ) );
		}
		else
		{
			sb.append( getLine( BINARY_COLS + " = c()", true ) );
		}

		if( !RMetaUtil.getNominalFields().isEmpty() )
		{
			sb.append( getLine( NOMINAL_COLS + " = " + BaseScript.FUNCTION_GET_COL_INDEXES + "( " + OTU_TABLE + ", "
					+ NOMINAL_ATTS + " )", true ) );
		}
		else
		{
			sb.append( getLine( NOMINAL_COLS + " = c()", true ) );
		}
		if( !RMetaUtil.getNumericFields().isEmpty() )
		{
			sb.append( getLine( NUMERIC_COLS + " = " + BaseScript.FUNCTION_GET_COL_INDEXES + "( " + OTU_TABLE + ", "
					+ NUMERIC_ATTS + " )", true ) );
		}
		else
		{
			sb.append( getLine( NUMERIC_COLS + " = c()", true ) );
		}

		// sb.append( getLine(
		// "# Instantiate references to each report field for use with mixed linear models (in case added later)" )
		// );
		// for( final String att: RMetaUtil.getBinaryFields() )
		// {
		// sb.append(
		// getLine( "binaryAtt_" + att.replaceAll( " ", "_" ) + " = factor( " + OTU_TABLE + "$'" + att + "' )",
		// true ) );
		// }
		// for( final String att: RMetaUtil.getNominalFields() )
		// {
		// sb.append( getLine(
		// "nominalAtt_" + att.replaceAll( " ", "_" ) + " = factor( " + OTU_TABLE + "$'" + att + "' )",
		// true ) );
		// }
		// for( final String att: RMetaUtil.getNumericFields() )
		// {
		// sb.append( getLine( "numericAtt_" + att.replaceAll( " ", "_" ) + " = " + OTU_TABLE + "$'" + att + "'",
		// true ) );
		// }
		return sb.toString();
	}

	/**
	 * Add table filter if configured in prop file. Examples:<br>
	 * otuTable = otuTable[ otuTable$Location != "OffTargetOrganoid" ]<br>
	 * otuTable = otuTable[ otuTable$Location == "OffTargetOrganoid" | otuTable$Location == "OffTarget", ]
	 *
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} properties are invalid or missing
	 */
	protected String rLineSetPreFilter() throws Exception
	{
		String preFilter = "";
		final List<String> queryFilters = getOtuTableFilter();
		if( queryFilters != null && !queryFilters.isEmpty() )
		{
			final Iterator<String> it = queryFilters.iterator();
			String prevFilter = "";
			final StringBuffer sb = new StringBuffer();
			sb.append( OTU_TABLE + " = " + OTU_TABLE + "[ " + it.next() );
			while( it.hasNext() )
			{
				final String filter = it.next();
				final StringTokenizer st = new StringTokenizer( filter, " " );
				// if same field with multiple != must use OR condition
				if( prevFilter.contains( "!=" ) && prevFilter.contains( st.nextToken() ) )
				{
					sb.append( " | " + filter );
				}
				else
				{
					sb.append( " & " + filter );
				}

				prevFilter = filter;
			}
			sb.append( ", ]" );
			preFilter = getLine( sb.toString() );
		}
		return preFilter;
	}

	/**
	 * Wrap call to main() with sink() method to capture errors/warnings in the error file. Output success/failure flag
	 * files and write errors to error report.
	 *
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} properties are invalid or missing
	 */
	protected String rLinesExecuteProgram() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( METHOD_RUN_PROGRAM + "()" ) );
		sb.append( getLine( METHOD_REPORT_STATUS + "()" ) );
		if( Config.getBoolean( R_SAVE_R_DATA ) )
		{
			sb.append( getLine( "save.image( paste0( " + R_OUTPUT_DIR + ", \"" + getClass().getSimpleName() + R_DATA_EXT
					+ "\" ) )" ) );
		}
		return sb.toString();
	}

	/**
	 * This method is used to read in the reports output by {@link biolockj.module.report.AddMetadataToOtuTables}
	 * 
	 * @return rCode
	 * @throws Exception if any error occurs
	 */
	protected String rLinesReadOtuTable() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "inputFile = list.files( " + R_TABLE_DIR + ", paste0(" + OTU_LEVEL + ", \".*\", \""
				+ AddMetadataToOtuTables.META_MERGED + "\"), full.names=\"TRUE\" )", true ) );
		sb.append( getLine( "if( length( inputFile ) == 0 ) { next }" ) );
		sb.append( getLine( OTU_TABLE + " = read.table( inputFile, check.names=FALSE, na.strings=\""
				+ Config.requireString( MetaUtil.META_NULL_VALUE ) + "\", comment.char=\"" + MetaUtil.getCommentChar()
				+ "\", header=TRUE, sep=\"\\t\" )" ) );
		sb.append( getLine( "rownames(" + OTU_TABLE + ") = " + OTU_TABLE + "[, which(colnames(" + OTU_TABLE + ")==\""
				+ MetaUtil.getID() + "\") ]" ) );
		sb.append( rLineSetPreFilter() );
		return sb.toString();
	}

	/**
	 * Each R script must build a function called main() which is called from the standard runProgram() method which
	 * wraps the method in error-handling code.
	 * 
	 * @return R code for the function main()
	 * @throws Exception if unable to build this required method
	 */
	protected abstract String rMainMethod() throws Exception;

	/**
	 * If libs is not null and not empty, return the function to import the libraries given.
	 * 
	 * @param libs List of R libraries
	 * @return rCode
	 */
	protected String rMethodImportLibs( final List<String> libs )
	{
		final StringBuffer sb = new StringBuffer();
		if( libs != null && !libs.isEmpty() )
		{
			final StringBuffer rLibs = new StringBuffer();
			for( final String lib: libs )
			{
				if( !rLibs.toString().isEmpty() )
				{
					rLibs.append( " && " );
				}
				rLibs.append( "library( " + lib + ", logical.return=TRUE )" );
			}

			sb.append( getLine( FUNCTION_IMPORT_LIBS + " <- function() {" ) );
			sb.append( getLine( "return( " + rLibs.toString() + " )" ) );
			sb.append( getLine( "}" ) );
		}

		return sb.toString();
	}

	/**
	 * This method returns R code for the function {@value #METHOD_REPORT_STATUS}.<br>
	 * At the end of the R script we check the sink() file for error messages, if none, we declare SUCCESS.
	 *
	 * @return R code
	 * @throws Exception if errors occur
	 */
	protected String rMethodReportStatus() throws Exception
	{
		final String scriptName = getPrimaryScript().getName();
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( METHOD_REPORT_STATUS + " <- function() {" ) );
		sb.append( getLine( "successFlag = paste0( " + R_SCRIPT_DIR + ", \"" + File.separator + scriptName + "_"
				+ Pipeline.SCRIPT_SUCCESS + "\" )" ) );
		sb.append( getLine( "conn = file( " + ERROR_FILE_NAME + ", open=\"r\" )" ) );
		sb.append( getLine( "errors = readLines( conn )" ) );
		sb.append( getLine( "close( conn )" ) );
		sb.append( getLine( "foundError = FALSE" ) );
		sb.append( getLine( "if( length( errors ) > 0 ) {" ) );
		sb.append( getLine( "for ( i in 1:length( errors ) ) {" ) );
		sb.append( getLine( "if( grepl( \"Error\", errors[i] ) ) {" ) );
		sb.append( getLine( "foundError = TRUE" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "if( !foundError ){" ) );
		sb.append( getLine( "file.create( successFlag )" ) );
		sb.append( getLine( "if( file.exists(" + ERROR_FILE_NAME + ") ) {" ) );
		sb.append( getLine( "file.remove( " + ERROR_FILE_NAME + " )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method adds R code to the Primary R script in the rModule script directory.
	 * 
	 * @param rCode R script code
	 * @throws Exception if I/O errors occur
	 */
	protected void updatePrimaryScript( final String rCode ) throws Exception
	{
		if( rCode != null && !rCode.isEmpty() )
		{
			final BufferedWriter writer = new BufferedWriter( new FileWriter( getPrimaryScript(), true ) );
			writeScript( writer, rCode );
		}
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

	private String getMainRscriptPrefix() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			return ModuleUtil.getModuleNum( this ) + ".0_";
		}
		return MAIN_SCRIPT_PREFIX;
	}

	/**
	 * If {@link biolockj.Config}.{@value #R_DEBUG}={@value biolockj.Config#TRUE}, wrap printVal with a print statement
	 * to log the text to the R debug file.
	 *
	 * @param printVal Value to log
	 * @return R code
	 * @throws Exception if {@link biolockj.Config}.{@value #R_DEBUG} value is invalid
	 */
	public static String getDebugLabel( final String printVal ) throws Exception
	{
		if( Config.getBoolean( R_DEBUG ) )
		{
			return getLine( "print( \"" + printVal.replaceAll( "\"", "" ) + "\" )" );
		}
		return "";
	}

	/**
	 * If {@link biolockj.Config}.{@value #R_DEBUG}={@value biolockj.Config#TRUE}, wrap printVal with a print statement
	 * to log the text to the R debug file and print the R code output.
	 *
	 * @param line R script line to debug
	 * @return R code
	 * @throws Exception if {@link biolockj.Config}.{@value #R_DEBUG} value is invalid
	 */
	public static String getDebugLine( final String line ) throws Exception
	{
		if( Config.getBoolean( R_DEBUG ) )
		{
			String varName = line;
			if( line.trim().startsWith( "#" ) || line.trim().startsWith( "\"" ) )
			{
				return getDebugLabel( line );
			}

			if( line.indexOf( "=" ) > 0 )
			{
				varName = line.substring( 0, line.indexOf( "=" ) ).trim();
			}

			return getDebugLabel( line ) + getDebugValue( varName );
		}

		return "";
	}

	/**
	 * Return the value with the return escape character: {@link biolockj.BioLockJ#RETURN}
	 *
	 * @param line Line of R code
	 * @return R code with a newline escape char
	 */
	public static String getLine( final String line )
	{
		return line + BioLockJ.RETURN;
	}

	/**
	 * Return the value with the return escape character: {@link biolockj.BioLockJ#RETURN}
	 *
	 * @param line Line of R code
	 * @param debug Set true to log line to debug log
	 * @return R code with a newline escape char
	 * @throws Exception if unable to find the debug Config
	 */
	public static String getLine( final String line, final boolean debug ) throws Exception
	{
		return getLine( line ) + ( debug ? getDebugLine( line ): "" );

	}

	/**
	 * This method is used to read in the reports output by {@link biolockj.module.r.CalculateStats}
	 * 
	 * @param tableName name of the stats table to build
	 * @param suffix file suffix
	 * @return rCode
	 * @throws Exception if any error occurs
	 */
	public static String rLinesReadStatsTable( final String tableName, final String suffix ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "inputFile = list.files( " + R_STATS_DIR + ", " + BaseScript.FUNCTION_GET_PATH
				+ "( \"\", paste0(" + OTU_LEVEL + ", \"_" + suffix + "\") ), full.names=\"TRUE\" )", true ) );
		sb.append( getLine( "if( length( inputFile ) == 0 ) { next }" ) );
		sb.append( getLine( tableName + " = read.table( inputFile, check.names=FALSE, header=TRUE, sep=\"\\t\" )" ) );
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
	 * This method returns R code for the function runProgram(). Primary method, wraps the call to main() in a sink()
	 * call to catch error messages, if any.
	 *
	 * @return R code
	 */
	protected static String rMethodRunProgram()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( METHOD_RUN_PROGRAM + " <- function() {" ) );
		sb.append( getLine( "errFile = file( " + ERROR_FILE_NAME + ", open=\"wt\" )" ) );
		sb.append( getLine( "sink( errFile, type=\"message\" )" ) );
		sb.append( getLine( "try( " + METHOD_MAIN + "() )" ) );
		sb.append( getLine( "sink( type=\"message\" )" ) );
		sb.append( getLine( "close( errFile )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * If {@link biolockj.Config}.{@value #R_DEBUG}={@value biolockj.Config#TRUE}, wrap printVal with a print statement
	 * to log the text to the R debug file and add a Log spacer.
	 *
	 * @param printVal Value to log
	 * @return R code
	 * @throws Exception if {@link biolockj.Config}.{@value #R_DEBUG} value is invalid
	 */
	private static String getDebugValue( final String printVal ) throws Exception
	{
		if( Config.getBoolean( R_DEBUG ) )
		{
			return getLine( "print( " + printVal + " )" );
		}
		return "";
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
	 * Global R script vector containing all report metadata fields: {@value #ALL_ATTS}
	 */
	public static final String ALL_ATTS = "allAtts";

	/**
	 * Global R script vector containing all report metadata binary fields: {@value #BINARY_ATTS}
	 */
	public static final String BINARY_ATTS = "binaryAtts";

	/**
	 * Global R script vector containing all report binary metadata fields: {@value #BINARY_COLS}
	 */
	public static final String BINARY_COLS = "binaryCols";

	/**
	 * Variable name of the BioLockJ R error file.
	 */
	public static final String ERROR_FILE_NAME = "errorFile";

	/**
	 * {@link biolockj.Config} property {@value #EXE_RSCRIPT} defines the command line executable to call RScript
	 */
	public static final String EXE_RSCRIPT = "exe.Rscript";

	/**
	 * Name of the function used to execute an R script: {@value #FUNCTION_RUN_R}
	 */
	public static final String FUNCTION_RUN_R = "runScript";

	/**
	 * Global R script vector containing all report metadata nominal fields: {@value #NOMINAL_ATTS}
	 */
	public static final String NOMINAL_ATTS = "nominalAtts";

	/**
	 * Defines the number of metadata columns in our merged OTU-metadata tables.
	 */
	public static final String NUM_META_COLS = "numMetaCols";

	/**
	 * Global R script vector containing all report metadata numeric fields: {@value #NUMERIC_ATTS}
	 */
	public static final String NUMERIC_ATTS = "numericAtts";

	/**
	 * Variable name holding one of the {@value #OTU_LEVELS} (taxonomy levels): {@value #OTU_LEVEL}
	 */
	public static final String OTU_LEVEL = "otuLevel";

	/**
	 * Name of taxonomy levels variable in R script methods: {@value #OTU_LEVELS}
	 */
	public static final String OTU_LEVELS = "otuLevels";

	/**
	 * Name of variable that stores read.table() output
	 */
	public static final String OTU_TABLE = "otuTable";

	/**
	 * Global R script variable containing path to R Script root file directory: {@value #R_BASE_DIR}
	 */
	public static final String R_BASE_DIR = "baseDir";

	/**
	 * File extension of BioLockJ generated R Scripts: {@value #R_EXT}
	 */
	public static final String R_EXT = ".R";

	/**
	 * Directory name where output generated by downloaded R scripts is output: {@value #R_LOCAL_DIR_NAME}
	 */
	public static final String R_LOCAL_DIR_NAME = "local";

	/**
	 * Global R script variable containing path to R Script output file directory: {@value #R_OUTPUT_DIR}
	 */
	public static final String R_OUTPUT_DIR = "outputDir";

	/**
	 * Global R script variable containing path to R Script directory: {@value #R_SCRIPT_DIR}
	 */
	public static final String R_SCRIPT_DIR = "scriptDir";

	/**
	 * Global R script variable containing path to R Script directory containing pvalue reports: {@value #R_STATS_DIR}
	 */
	public static final String R_STATS_DIR = "statsDir";

	/**
	 * Global R script variable containing path to R Script input file directory: {@value #R_TABLE_DIR}
	 */
	public static final String R_TABLE_DIR = "tableDir";

	/**
	 * Global R script variable containing path to R Script temporary directory: {@value #R_TEMP_DIR}
	 */
	public static final String R_TEMP_DIR = "tempDir";

	/**
	 * File extension of TSV (tab-separated text) files read or generated by R Script: {@value #TSV_EXT}
	 */
	public static final String TSV_EXT = ".tsv";

	/**
	 * R DEBUG log file prefix name: {@value #DEBUG_LOG_PREFIX}
	 */
	protected static final String DEBUG_LOG_PREFIX = "debug_";

	/**
	 * R function used to import required libraries: {@value #FUNCTION_IMPORT_LIBS}
	 */
	protected static final String FUNCTION_IMPORT_LIBS = "importLibs";

	/**
	 * The standard indentation size in the R script is 4 spaces.
	 */
	protected static final String INDENT = "   ";

	/**
	 * File extension of R Script log file: {@value #LOG_EXT}
	 */
	protected static final String LOG_EXT = ".log";

	/**
	 * Name of the main method for any BioLockJ R Script.
	 */
	protected static final String METHOD_MAIN = "main";

	/**
	 * R method name for reportStatus: {@value #METHOD_REPORT_STATUS}
	 */
	protected static final String METHOD_REPORT_STATUS = "reportStatus";

	/**
	 * R method name: {@value #METHOD_RUN_PROGRAM}
	 */
	protected static final String METHOD_RUN_PROGRAM = "runProgram";

	/**
	 * Global R script vector containing all report nominal metadata fields: {@value #NOMINAL_COLS}
	 */
	protected static final String NOMINAL_COLS = "nominalCols";

	/**
	 * Global R script vector containing all report numeric metadata fields: {@value #NUMERIC_COLS}
	 */
	protected static final String NUMERIC_COLS = "numericCols";

	/**
	 * Name of OTU column index variable incremented in R wrapper code-blocks {@link #filterRareOtus(String)} and
	 * {@link #forEachOtu(String)} : {@value #OTU_COL}
	 */
	protected static final String OTU_COL = "otuCol";

	/**
	 * File extension of R Script data file: {@value #R_DATA_EXT}
	 */
	protected static final String R_DATA_EXT = ".RData";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #R_DEBUG}<br>
	 * If enabled, an R script debug log file is created for each taxonomy level.
	 */
	protected static final String R_DEBUG = "r.debug";

	/**
	 * {@link biolockj.Config} List property: {@value #R_FILTER_FIELDS}<br>
	 * Ordered list of filter fields to include in R-wrapper code-block, to filter rows using their corresponding
	 * {@value #R_FILTER_OPERATORS} and {@value #R_FILTER_VALUES}.
	 */
	protected static final String R_FILTER_FIELDS = "r.filterFields";

	/**
	 * {@link biolockj.Config} List property: {@value #R_FILTER_NA_FIELDS}<br>
	 * Ordered list of fields to omit rows with NA value in filter-block R-code wrapper.
	 */
	protected static final String R_FILTER_NA_FIELDS = "r.filterNaFields";

	/**
	 * {@link biolockj.Config} List property: {@value #R_FILTER_OPERATORS}<br>
	 * Ordered list of logical operators to use in R-wrapper code-block along with their corresponding
	 * {@value #R_FILTER_FIELDS} and {@value #R_FILTER_VALUES}.
	 */
	protected static final String R_FILTER_OPERATORS = "r.filterOperators";

	/**
	 * {@link biolockj.Config} List property: {@value #R_FILTER_VALUES}<br>
	 * Ordered list of filter values use in R-wrapper code-block along with their corresponding
	 * {@value #R_FILTER_OPERATORS} and {@value #R_FILTER_FIELDS}.
	 */
	protected static final String R_FILTER_VALUES = "r.filterValues";

	/**
	 * {@link biolockj.Config} Numeric property: {@value #R_RARE_OTU_THRESHOLD}<br>
	 * If value is between 0-1, it is considered a minimum percentage. If value is greater than 1, it is a hard
	 * numerical minimum. OTUs not found in the number or percentage of samples configured will be ommitted from the
	 * report.
	 */
	protected static final String R_RARE_OTU_THRESHOLD = "r.rareOtuThreshold";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #R_SAVE_R_DATA} can be enable to call save.image to save the R
	 * session used to run the R script.
	 */
	protected static final String R_SAVE_R_DATA = "r.saveRData";

	/**
	 * {@link biolockj.Config} property {@value #R_TIMEOUT} defines the number of minutes before R script fails due to
	 * timeout. If undefined, no timeout is used.
	 */
	protected static final String R_TIMEOUT = "r.timeout";

}
