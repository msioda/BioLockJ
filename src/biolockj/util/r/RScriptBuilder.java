/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 18, 2017
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
package biolockj.util.r;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.module.BioModule;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;

/**
 * This class builds the R script.
 */
public class RScriptBuilder extends RScript
{

	public static void buildScript( final BioModule bioModule, final boolean normalReportFlag,
			final boolean logNormalReportFlag ) throws Exception
	{
		if( !normalReportFlag && !logNormalReportFlag )
		{
			throw new Exception( "RScriptUtil.buildReports() must contain at least one TRUE parameter." );
		}

		final StringBuffer sb = new StringBuffer();
		sb.append( rLinesTopOfScript( bioModule ) );
		sb.append( rMethodMain( true, true ) );
		sb.append( rMethodBuildReports() );
		sb.append( rFunctionGetValuesByName() );
		sb.append( rFunctionGetColIndexes() );
		sb.append( rFunctionGetPath() );
		sb.append( rMethodImportPackage( bioModule ) );
		sb.append( StatsUtil.buildMethods() );
		sb.append( PlotUtil.buildMethods() );
		sb.append( rLinesCallMainAndReportStatus() );
		writeScript( bioModule, sb.toString() );
	}

	/**
	 * Validate configuration file properties used in this utility.
	 *
	 * @throws Exception
	 */
	public static void checkDependencies() throws Exception
	{
		requireQueryFilters();
		Config.requireString( Config.REPORT_LOG_BASE );
		Config.requireString( MetaUtil.INPUT_NULL_VALUE );
		Config.requireString( MetaUtil.INPUT_COMMENT );
		Config.requireString( Config.PROJECT_NAME );
		Config.requireDoubleVal( R_TOP_ROW_ADJ );
		Config.requireDoubleVal( R_BOTTOM_ROW_ADJ );
		Config.requireDoubleVal( R_RIGHT_COL_ADJ );
		Config.requireDoubleVal( R_LEFT_COL_ADJ );
		Config.requireTaxonomy();
		if( Config.getSet( RScript.R_NOMINAL_DATA ).isEmpty() && Config.getSet( RScript.R_NUMERIC_DATA ).isEmpty() )
		{
			throw new Exception(
					"R Reports require either: " + RScript.R_NOMINAL_DATA + " or " + RScript.R_NUMERIC_DATA );
		}

		PlotUtil.checkDependencies();
		StatsUtil.checkDependencies();
	}

	/**
	 * Extracts column indexes from table for the given vector of attribute names.
	 *
	 * @return String - formatted R function: getColIndexes(myT, attNames)
	 */
	public static String rFunctionGetColIndexes()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "getColIndexes <- function( myT, attNames )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "cols = vector( mode=\"integer\" )" ) );
		sb.append( getLine( "for( i in 1:length(attNames) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "cols[i] = grep( attNames[i], colnames(myT) )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "return( cols )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	public static String rFunctionGetPath() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "getPath <- function( rootDir, name, fileSuffix )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "if ( grepl( \"" + BioLockJ.LOG_NORMAL + "\", fileSuffix ) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "return( paste( rootDir, \"" + Config.getLogPrefix() + "\", name, sep=\"\" ) )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "return( paste( rootDir, name, sep=\"\" ) )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Extracts specified p-values from a vector that contain p-values for multiple attributes.
	 * (renamed from "getPvalsForAtt" )
	 *
	 * @return String - formatted R function: getValuesByName(v, name)
	 */
	public static String rFunctionGetValuesByName()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "getValuesByName <- function( v, name )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "return( as.vector( v[grep(name, names(v))] ) )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	private static String convertToString( final Collection<String> data )
	{
		final Iterator<String> it = data.iterator();
		final StringBuffer sb = new StringBuffer();
		if( !data.isEmpty() )
		{
			while( it.hasNext() )
			{
				sb.append( ( ( sb.length() == 0 ) ? "c( ": ", " ) + "\"" + it.next() + "\"" );
			}
			sb.append( " )" );
		}

		return sb.toString();
	}

	/**
	 * Build the query filters based on prop file values.
	 *
	 * @return List<String> query filters
	 * @throws Exception
	 */
	private static List<String> requireQueryFilters() throws Exception
	{
		final List<String> queryFilters = new ArrayList<>();
		final Iterator<String> nalIt = Config.getSet( RScript.R_FILTER_NA_ATTRIBUTES ).iterator();
		final Iterator<String> attIt = Config.getList( RScript.R_FILTER_ATTRIBUTES ).iterator();
		final Iterator<String> opIt = Config.getList( RScript.R_FILTER_OPERATORS ).iterator();
		final Iterator<String> valIt = Config.getList( RScript.R_FILTER_VALUES ).iterator();

		if( ( Config.getList( RScript.R_FILTER_ATTRIBUTES ).size() != Config.getList( RScript.R_FILTER_OPERATORS )
				.size() )
				|| ( Config.getList( RScript.R_FILTER_ATTRIBUTES ).size() != Config.getList( RScript.R_FILTER_VALUES )
						.size() ) )
		{
			throw new Exception(
					"CONFIG FILE ERROR >>> THESE PROPERTIES MUST BE COMMA DELIMITED LISTS OF THE SAME LENGTH [ "
							+ RScript.R_FILTER_ATTRIBUTES + ", " + RScript.R_FILTER_OPERATORS + ", "
							+ RScript.R_FILTER_VALUES + " ]" + "CURRENT LENGTHS = [ "
							+ Config.getList( RScript.R_FILTER_ATTRIBUTES ).size() + ", " + " ]"
							+ Config.getList( RScript.R_FILTER_OPERATORS ).size() + ", "
							+ Config.getList( RScript.R_FILTER_VALUES ).size() );
		}

		while( attIt.hasNext() )
		{
			queryFilters.add( "(myT$'" + attIt.next() + "' " + opIt.next() + " " + valIt.next() + " )" );
		}

		while( nalIt.hasNext() )
		{
			queryFilters.add( "!is.na(myT$'" + nalIt.next() + "')" );
		}

		return queryFilters;
	}

	private static String rInitReport() throws Exception
	{
		final StringBuffer sb = new StringBuffer();

		if( Config.getBoolean( R_DEBUG ) )
		{
			sb.append( getLine( R_LOG + " = paste( getPath( " + OUTPUT_DIR + ", paste( \"" + DEBUG_LOG_PREFIX + "\", "
					+ OTU_LEVEL + ", sep=\"\"), " + INPUT_FILE_SUFFIX + "), \"" + LOG_EXT + "\", sep=\"\" )" ) );
			sb.append( getLine( "sink( " + R_LOG + " )" ) );
		}

		sb.append( getLine( "pdf( paste( getPath(" + OUTPUT_DIR + ", " + OTU_LEVEL + ", " + INPUT_FILE_SUFFIX + "), \""
				+ PDF_EXT + "\", sep=\"\" ) )" ) );
		sb.append( getLine( "par( mfrow=c(2, 2), mar=c(3, 4, 4, 2), xpd=FALSE, oma=c(2, 2, 3, 1), las=1 )" ) );
		sb.append( getLine(
				"inputFile = paste( " + INPUT_DIR + ", " + OTU_LEVEL + ", " + INPUT_FILE_SUFFIX + ", sep=\"\" )",
				"inputFile" ) );
		sb.append( getLine( "myT = read.table( inputFile, check.names=FALSE, na.strings=\""
				+ Config.requireString( MetaUtil.INPUT_NULL_VALUE ) + "\", comment.char=\""
				+ Config.requireString( MetaUtil.INPUT_COMMENT ) + "\", header=TRUE, sep=\"\\t\" )" ) );

		sb.append( rLineSetPreFilter() );

		if( !MetaUtil.getBinaryFields().isEmpty() )
		{
			sb.append( getLine( BINARY_COLS + " = getColIndexes( myT, " + BINARY_ATTS + " )", BINARY_COLS ) );
		}
		if( !MetaUtil.getNominalFields().isEmpty() )
		{
			sb.append( getLine( NOMINAL_COLS + " = getColIndexes( myT, " + NOMINAL_ATTS + " )", NOMINAL_COLS ) );
		}
		if( !MetaUtil.getNumericFields().isEmpty() )
		{
			sb.append( getLine( NUMERIC_COLS + " = getColIndexes( myT, " + NUMERIC_ATTS + " )", NUMERIC_COLS ) );
		}

		sb.append( getLine(
				"# Instantiate references to each report attribute for use with mixed linear models (if needed)" ) );
		for( final String att: MetaUtil.getBinaryFields() )
		{
			sb.append( getLine( att.replaceAll( " ", "_" ) + " = factor( myT$'" + att + "' )",
					att.replaceAll( " ", "_" ) ) );
		}
		for( final String att: MetaUtil.getNominalFields() )
		{
			sb.append( getLine( att.replaceAll( " ", "_" ) + " = factor( myT$'" + att + "' )",
					att.replaceAll( " ", "_" ) ) );
		}
		for( final String att: MetaUtil.getNumericFields() )
		{
			sb.append( getLine( att.replaceAll( " ", "_" ) + " = myT$'" + att + "'", att.replaceAll( " ", "_" ) ) );
		}
		return sb.toString();
	}

	/**
	 * Wrap call to main() with sink() method to capture errors/warnings in the error file.
	 * Output success/failure flag files & write errors to error report.
	 *
	 * @return R script lines
	 */
	private static String rLinesCallMainAndReportStatus() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "importPackage( \"coin\" )" ) );
		sb.append( getLine( "importPackage( \"Kendall\" )" ) );
		sb.append( getLine( "errFile = file( " + ERROR_FILE_NAME + ", open=\"wt\" )" ) );
		sb.append( getLine( "sink( errFile, type=\"message\" )" ) );
		sb.append( getLine( "try( main() )" ) );
		sb.append( getLine( "sink( type=\"message\" )" ) );
		sb.append( getLine( "close( errFile )" ) );
		sb.append( getLine( "conn = file( " + ERROR_FILE_NAME + ", open=\"r\" )" ) );
		sb.append( getLine( "errors = readLines( conn )" ) );
		sb.append( getLine( "close( conn )" ) );
		sb.append( getLine( "if( length( errors ) > 0 )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "for ( i in 1:length( errors ) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "if( grepl( \"Error\", errors[i] ) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "file.create( " + FAILED_SCRIPT_NAME + " )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "if( !file.exists( " + FAILED_SCRIPT_NAME + " ) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "file.create( " + SUCCESS_SCRIPT_NAME + " )" ) );
		sb.append( getLine( "file.remove( " + ERROR_FILE_NAME + " )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "save.image( " + R_IMAGE_FILE_NAME + " )" ) );
		return sb.toString();
	}

	/**
	 * Add table filter if configured in prop file.
	 *
	 * @return
	 */
	private static String rLineSetPreFilter() throws Exception
	{
		String preFilter = "";
		final List<String> queryFilters = requireQueryFilters();
		if( ( queryFilters != null ) && !queryFilters.isEmpty() )
		{
			final StringBuffer sb = new StringBuffer();
			sb.append( "myT = myT[ " );
			for( final String filter: queryFilters )
			{
				sb.append( ( ( sb.length() == 0 ) ? filter: " && " ) + filter );
			}
			sb.append( ", ]" );
			preFilter = getLine( sb.toString() );
		}
		return preFilter;
	}

	/**
	 * Initialize script, set directories at top of script to make it easy for users to update.
	 *
	 * @return R script lines
	 * @throws Exception
	 */
	private static String rLinesInitScript( final BioModule bioModule ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "sessionInfo()" ) );
		sb.append( getLine( "rm( list=ls() )" ) );
		sb.append(
				getLine( OUTPUT_DIR + " = \"" + bioModule.getOutputDir().getAbsolutePath() + File.separator + "\"" ) );
		sb.append(
				getLine( SCRIPT_DIR + " = \"" + bioModule.getScriptDir().getAbsolutePath() + File.separator + "\"" ) );
		sb.append( getLine( INPUT_DIR + " = \"" + bioModule.getInputDir().getAbsolutePath() + File.separator + "\"" ) );
		sb.append( getLine( "setwd( " + OUTPUT_DIR + " )" ) );
		return sb.toString();
	}

	/**
	 * Set the constant values referenced in the script
	 * reports status (SUCCESS/FAILURE) by generating empty flag file, and save the rImage R_DATA file.
	 *
	 * @param String - name of the R script
	 * @return String - formatted top level script for module
	 * @throws Exception
	 */
	private static String rLinesSetConstants( final String scriptName ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "NUM_META_COLS = " + MetaUtil.getAttributeNames().size() ) );
		sb.append( getLine( "HORIZONTAL = 1" ) );
		sb.append( getLine( "VERTICAL = 3" ) );
		sb.append( getLine( "LEFT_SIDE = 2" ) );
		sb.append( getLine( "TOP_SIDE = 3" ) );
		sb.append( getLine( "CENTER_ADJ = 0.5" ) );
		sb.append( getLine( ERROR_FILE_NAME + " = paste( " + OUTPUT_DIR + ", \""
				+ Config.requireString( Config.PROJECT_NAME ) + R_ERROR + "\", sep=\"\" )" ) );
		sb.append( getLine( IMPORT_ERROR_FILE_NAME + " = paste( " + OUTPUT_DIR + ", \""
				+ Config.requireString( Config.PROJECT_NAME ) + "_import" + R_ERROR + "\", sep=\"\" )" ) );
		sb.append( getLine( R_IMAGE_FILE_NAME + " = paste( " + OUTPUT_DIR + ", \""
				+ Config.requireString( Config.PROJECT_NAME ) + R_DATA + "\", sep=\"\" )" ) );
		sb.append( getLine( "FAILED_SCRIPT_NAME = paste( " + SCRIPT_DIR + ", \"" + scriptName + "_"
				+ BioLockJ.SCRIPT_FAILED + "\", sep=\"\" )" ) );
		sb.append( getLine( "SUCCESS_SCRIPT_NAME = paste( " + SCRIPT_DIR + ", \"" + scriptName + "_"
				+ BioLockJ.SCRIPT_SUCCESS + "\", sep=\"\" )" ) );

		return sb.toString();
	}

	/**
	 * Set static lists that will be referenced throughout the script
	 *
	 * @return String representations of character vectors as comma separated lists
	 * @throws Exception
	 */
	private static String rLinesSetLists() throws Exception
	{
		final StringBuffer sb = new StringBuffer();

		sb.append( getLine( OTU_LEVELS + " = " + convertToString( Config.requireTaxonomy() ) ) );

		String allAtts = null;
		if( !MetaUtil.getBinaryFields().isEmpty() )
		{
			sb.append( getLine( BINARY_ATTS + " = " + convertToString( MetaUtil.getBinaryFields() ) ) );
			allAtts = ALL_ATTS + " = c( " + BINARY_ATTS;
		}

		if( !MetaUtil.getNominalFields().isEmpty() )
		{
			sb.append( getLine( NOMINAL_ATTS + " = " + convertToString( MetaUtil.getNominalFields() ) ) );
			allAtts = ( ( allAtts == null ) ? ( ALL_ATTS + " = c( " ): allAtts + ", " ) + NOMINAL_ATTS;
		}

		if( !MetaUtil.getNumericFields().isEmpty() )
		{
			sb.append( getLine( NUMERIC_ATTS + " = " + convertToString( MetaUtil.getNumericFields() ) ) );
			allAtts = ( ( allAtts == null ) ? ( ALL_ATTS + " = c( " ): allAtts + ", " ) + NUMERIC_ATTS;
		}

		sb.append( getLine( allAtts + " )" ) );

		sb.append( getLine( PLOT_ROW + " = c( " + Config.requireDoubleVal( R_TOP_ROW_ADJ ) + ", "
				+ Config.requireDoubleVal( R_TOP_ROW_ADJ ) + ", " + Config.requireDoubleVal( R_BOTTOM_ROW_ADJ ) + ", "
				+ Config.requireDoubleVal( R_BOTTOM_ROW_ADJ ) + " )" ) );

		sb.append( getLine( PLOT_COL + " = c( " + Config.requireDoubleVal( R_LEFT_COL_ADJ ) + ", "
				+ Config.requireDoubleVal( R_RIGHT_COL_ADJ ) + ", " + Config.requireDoubleVal( R_LEFT_COL_ADJ ) + ", "
				+ Config.requireDoubleVal( R_RIGHT_COL_ADJ ) + " )" ) );

		return sb.toString();
	}

	/**
	 * Top level script, imports required libraries, calls the main script, sinks errors/warnings to errorFile,
	 * reports status (SUCCESS/FAILURE) by generating empty flag file, and save the rImage R_DATA file.
	 *
	 * Dependent R functions:
	 * 		1. main()
	 *
	 * @param BioModule - module building the R script
	 * @return String - formatted top level script for module
	 * @throws Exception
	 */
	private static String rLinesTopOfScript( final BioModule bioModule ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( rLinesInitScript( bioModule ) );
		sb.append( rLinesSetConstants( ModuleUtil.getMainScript( bioModule ).getName() ) );
		sb.append( rLinesSetLists() );
		return sb.toString();
	}

	/**
	 * Initialize the R structures for all attributes.
	 * @throws Exception
	 */
	private static String rMethodBuildReports() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "buildReports <- function( " + INPUT_FILE_SUFFIX + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "for( " + OTU_LEVEL + " in " + OTU_LEVELS + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( rInitReport() );

		sb.append( getLine( "reportList = " + StatsUtil.FUNCTION_CALCULATE_STATS + "(myT, " + BINARY_COLS + ", "
				+ NOMINAL_COLS + ", " + NUMERIC_COLS + " )" ) );

		sb.append( getLine( StatsUtil.FUNCTION_BUILD_SUMMARY_TABLE + "( reportList, file=paste( getPath(" + OUTPUT_DIR
				+ ", " + OTU_LEVEL + ", " + INPUT_FILE_SUFFIX + " ), \"" + TSV_EXT + "\", sep=\"\" ) )" ) );

		sb.append( getLine( PlotUtil.FUNCTION_BUILD_PDF_REPORT + "(myT, reportList, " + BINARY_COLS + ", "
				+ NOMINAL_COLS + ", " + NUMERIC_COLS + ")" ) );

		if( Config.getBoolean( R_DEBUG ) )
		{
			sb.append( getLine( "sink()" ) );
		}

		sb.append( getLine( "dev.off()" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Import required libraries.
	 *
	 * @return R script lines
	 */
	private static String rMethodImportPackage( final BioModule bioModule )
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "importPackage <- function( name )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "if( !( eval(name) %in% installed.packages()[,\"Package\"] ) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "install.packages( eval(name), lib=" + SCRIPT_DIR + " )" ) );
		sb.append( getLine( "library( eval(name), lib.loc=" + SCRIPT_DIR + ", character.only=TRUE )" ) );
		sb.append( getLine( "} else {" ) );
		sb.append( getLine( "library( eval(name), character.only=TRUE )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );

		return sb.toString();
	}

	private static String rMethodMain( final boolean buildNormalReport, final boolean buildLogNormalReport )
			throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( RScript.getLine( "main <- function()" ) );
		sb.append( RScript.getLine( "{" ) );
		if( buildNormalReport )
		{
			sb.append( RScript.getLine( "buildReports( \"" + BioLockJ.NORMAL + MetaUtil.META_MERGED_SUFFIX + "\" )" ) );
		}
		if( buildLogNormalReport )
		{
			sb.append( RScript
					.getLine( "buildReports( \"" + BioLockJ.LOG_NORMAL + MetaUtil.META_MERGED_SUFFIX + "\" )" ) );
		}
		sb.append( RScript.getLine( "}" ) );
		return sb.toString();
	}

	private static void writeScript( final BioModule bioModule, final String script ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( ModuleUtil.getMainScript( bioModule ) ) );
		int indentCount = 0;
		final StringTokenizer st = new StringTokenizer( script, BioLockJ.RETURN );
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
				writer.write( BioLockJ.INDENT );
			}

			writer.write( line + BioLockJ.RETURN );

			if( line.equals( "{" ) )
			{
				indentCount++;
			}
		}
		writer.close();
	}

	public static final String DEBUG_LOG_PREFIX = "debug_";
	public static final String LOG_EXT = ".log";
	public static final String PDF_EXT = ".pdf";
	public static final String TSV_EXT = ".tsv";
	private static final String ERROR_FILE_NAME = "ERROR_FILE_NAME";
	private static final String FAILED_SCRIPT_NAME = "FAILED_SCRIPT_NAME";
	private static final String IMPORT_ERROR_FILE_NAME = "IMPORT_ERROR_FILE_NAME";
	private static final String INPUT_FILE_SUFFIX = "inputFileSuffix";
	private static final String OTU_LEVEL = "otuLevel";
	private static final String R_BOTTOM_ROW_ADJ = "r.bottomRowAdj";
	private static final String R_IMAGE_FILE_NAME = "R_IMAGE_FILE_NAME";
	private static final String R_LEFT_COL_ADJ = "r.leftColAdj";
	private static final String R_LOG = "rLog";
	private static final String R_RIGHT_COL_ADJ = "r.rightColAdj";
	private static final String R_TOP_ROW_ADJ = "r.topRowAdj";
	private static final String SUCCESS_SCRIPT_NAME = "SUCCESS_SCRIPT_NAME";
}
