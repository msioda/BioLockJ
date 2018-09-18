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
package biolockj.util.r;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import biolockj.Config;
import biolockj.Pipeline;
import biolockj.module.r.CalculateStats;
import biolockj.module.r.R_Module;
import biolockj.module.report.AddMetadataToOtuTables;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.RuntimeParamUtil;

/**
 * This class builds the base R script used by all standard R Script generating modules. Each script will call the
 * BaseScript via R's source command to make these standard methods and global variables available.
 * 
 */
public class BaseScript
{
	/**
	 * This method generates the base script R code required by all standard R modules.
	 *
	 * @param rModule R_Module
	 * @throws Exception if propagated by calling methods
	 */
	protected BaseScript( final R_Module rModule ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( rLinesInitScript( rModule ) );
		sb.append( rLinesSetLists() );
		sb.append( rFunctionGetValuesByName() );
		sb.append( rFunctionGetColIndexes() );
		sb.append( rFunctionGetPath() );
		sb.append( rFunctionGetFactorGroups() );
		R_Module.writeNewScript( getScriptPath( rModule ), sb.toString() );
	}

	/**
	 * This method returns R code for the function {@value #FUNCTION_GET_COL_INDEXES}(otuTable, attNames).<br>
	 * Extracts column indexes from table otuTable for the given vector of field names (attNames).
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>otuTable = A data.frame returned by read.table( otuFilePath ) for a taxonomy-level OTU table with metadata
	 * columns
	 * <li>attNames = Metadata field names (must be column names of otuTable)
	 * </ul>
	 *
	 * @return R code
	 */
	protected String rFunctionGetColIndexes()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_COL_INDEXES + " <- function( otuTable, attNames ) {" ) );
		sb.append( R_Module.getLine( "cols = vector( mode=\"integer\" )" ) );
		sb.append( R_Module.getLine( "if( !is.na(attNames) && length(attNames) > 0 ) {" ) );
		sb.append( R_Module.getLine( "for( i in 1:length(attNames) ) {" ) );
		sb.append( R_Module.getLine( "cols[i] = grep(TRUE, colnames(otuTable)==attNames[i])" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "return( cols )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * The method returns R code for the function {@value #FUNCTION_GET_FACTOR_GROUPS}( otuTable, metaCol, otuCol ). The
	 * OTU column values are split into groups based on their corresponding value in metaColFactorVals. The list
	 * returned contains one vector for each unique factor, each containing OTU abundance values related to that factor.
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>otuTable = A data.frame returned by read.table( otuFilePath ) for a taxonomy-level OTU table
	 * <li>metaColFactorVals = Metadata column values expressed as factors, created with R code like:
	 * as.factor(otuTable[,metaCol])
	 * <li>otuCol = otuTable myT column number with OTU counts/abundances
	 * </ul>
	 *
	 * @return R code
	 */
	protected String rFunctionGetFactorGroups()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_FACTOR_GROUPS + " <- function( otuTable, metaCol, otuCol ) {" ) );
		sb.append( R_Module.getLine( "vals = list()" ) );
		sb.append( R_Module.getLine( "options = levels( metaCol )" ) );
		sb.append( R_Module.getLine( "for( i in 1:length(options) ) {" ) );
		sb.append( R_Module.getLine( "vals[[i]] = otuTable[metaCol==options[i], otuCol]" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "return( vals )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function getPath(rootDir, name).
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>rootDir = Return path root directory
	 * <li>name = Base name of the output file
	 * </ul>
	 * <p>
	 *
	 * @return R code
	 * @throws Exception if unable to get the pipeline name from the Config
	 */
	protected String rFunctionGetPath() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_PATH + " <- function( rootDir, name ) {" ) );
		sb.append( R_Module.getLine( "return( paste0( rootDir,  \""
				+ Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + "_\", name ) )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * The method returns R code for the function getValuesByName(vals, name). R method extracts specified p-values as a
	 * vector from a data.frame that contain p-values for multiple fields.
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>vals = Values from the data frame
	 * <li>name = Metadata field name
	 * </ul>
	 *
	 * @return R code
	 */
	protected String rFunctionGetValuesByName()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_VALS_BY_NAME + " <- function( vals, name ) {" ) );
		sb.append( R_Module.getLine( "return( as.vector( vals[names(vals)==name] ) )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Initialize script, set directories at top of script to make it easy for users to update later.
	 *
	 * @param rModule R_Module
	 * @return R code
	 * @throws Exception if propagated by sub-routines
	 */
	protected String rLinesInitScript( final R_Module rModule ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( "rm( list=ls() )" ) );
		sb.append( R_Module.getLine( "sessionInfo()" ) );
		sb.append( R_Module
				.getLine( R_Module.R_BASE_DIR + " = \"" + RuntimeParamUtil.getBaseDir().getAbsolutePath() + "\"" ) );
		sb.append( R_Module.getLine( R_Module.R_TABLE_DIR + " = paste0( " + R_Module.R_BASE_DIR + ", \""
				+ modifyBasePath( ModuleUtil.getModule( AddMetadataToOtuTables.class.getName() ).getOutputDir() )
				+ "\" )" ) );
		sb.append( R_Module.getLine( R_Module.R_OUTPUT_DIR + " = paste0( " + R_Module.R_BASE_DIR + ", \""
				+ modifyBasePath( rModule.getOutputDir() ) + File.separator + "\" )" ) );
		sb.append( R_Module.getLine( R_Module.R_SCRIPT_DIR + " = paste0( " + R_Module.R_BASE_DIR + ", \""
				+ modifyBasePath( rModule.getScriptDir() ) + File.separator + "\" )" ) );
		sb.append( R_Module.getLine( R_Module.R_STATS_DIR + " = paste0( " + R_Module.R_BASE_DIR + ", \""
				+ modifyBasePath( ModuleUtil.getModule( CalculateStats.class.getName() ).getOutputDir() ) + "\" )" ) );
		sb.append( R_Module.getLine( R_Module.R_TEMP_DIR + " = paste0( " + R_Module.R_BASE_DIR + ", \""
				+ modifyBasePath( rModule.getTempDir() ) + File.separator + "\" )" ) );
		sb.append( R_Module.getLine( R_Module.ERROR_FILE_NAME + " = paste0( " + R_Module.R_SCRIPT_DIR + ", \""
				+ File.separator + rModule.getPrimaryScript().getName() + "_" + Pipeline.SCRIPT_FAILURES + "\" )" ) );
		sb.append( R_Module.getLine( "setwd( " + R_Module.R_OUTPUT_DIR + " )" ) );
		sb.append( R_Module.getLine( R_Module.NUM_META_COLS + " = " + MetaUtil.getFieldNames().size() ) );
		return sb.toString();
	}

	/**
	 * Set global lists that will be referenced throughout the script
	 *
	 * @return String representations of character vectors as comma separated lists
	 * @throws Exception if {@link biolockj.Config} properties are invalid or missing
	 */
	protected String rLinesSetLists() throws Exception
	{
		String allAtts = null;
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine(
				R_Module.OTU_LEVELS + " = " + convertToString( Config.getList( Config.REPORT_TAXONOMY_LEVELS ) ) ) );

		if( !RMetaUtil.getBinaryFields().isEmpty() )
		{
			sb.append(
					R_Module.getLine( R_Module.BINARY_ATTS + " = " + convertToString( RMetaUtil.getBinaryFields() ) ) );
			allAtts = R_Module.ALL_ATTS + " = c( " + R_Module.BINARY_ATTS;
		}

		if( !RMetaUtil.getNominalFields().isEmpty() )
		{
			sb.append( R_Module
					.getLine( R_Module.NOMINAL_ATTS + " = " + convertToString( RMetaUtil.getNominalFields() ) ) );
			allAtts = ( allAtts == null ? R_Module.ALL_ATTS + " = c( ": allAtts + ", " ) + R_Module.NOMINAL_ATTS;
		}

		if( !RMetaUtil.getNumericFields().isEmpty() )
		{
			sb.append( R_Module
					.getLine( R_Module.NUMERIC_ATTS + " = " + convertToString( RMetaUtil.getNumericFields() ) ) );
			allAtts = ( allAtts == null ? R_Module.ALL_ATTS + " = c( ": allAtts + ", " ) + R_Module.NUMERIC_ATTS;
		}

		sb.append( R_Module.getLine( allAtts + " )" ) );

		return sb.toString();
	}

	private String modifyBasePath( final File dir ) throws Exception
	{
		return dir.getAbsolutePath().replace( RuntimeParamUtil.getBaseDir().getAbsolutePath(), "" );
	}

	/**
	 * This method constructs a new BaseScript for the given rModule.
	 *
	 * @param rModule R_Module
	 * @throws Exception if any errors occur
	 */
	public static void buildScript( final R_Module rModule ) throws Exception
	{
		final File f = new File( getScriptPath( rModule ) );
		if( !f.exists() )
		{
			new BaseScript( rModule );
		}
	}

	/**
	 * Method converts a collection into an R vector via comma separated list
	 *
	 * @param data Collection of values
	 * @return R code
	 */
	public static String convertToString( final Collection<String> data )
	{
		final Iterator<String> it = data.iterator();
		final StringBuffer sb = new StringBuffer();
		if( !data.isEmpty() )
		{
			while( it.hasNext() )
			{
				sb.append( ( sb.length() == 0 ? "c( ": ", " ) + "\"" + it.next() + "\"" );
			}
			sb.append( " )" );
		}

		return sb.toString();
	}

	/**
	 * Return the path to the base script.
	 * 
	 * @param rModule R_Module BioModule
	 * @return Base script system path
	 */
	public static String getScriptPath( final R_Module rModule )
	{
		return rModule.getScriptDir().getAbsolutePath() + File.separator + BaseScript.class.getSimpleName()
				+ R_Module.R_EXT;
	}

	/**
	 * R function name to get otuTable column indexes based on column names: {@value #FUNCTION_GET_COL_INDEXES}
	 */
	public static final String FUNCTION_GET_COL_INDEXES = "getColIndexes";

	/**
	 * R function name to get otuTable column values as factors: {@value #FUNCTION_GET_FACTOR_GROUPS}
	 */
	public static final String FUNCTION_GET_FACTOR_GROUPS = "getFactorGroups";

	/**
	 * R function name to get full path with project name: {@value #FUNCTION_GET_PATH}
	 */
	public static final String FUNCTION_GET_PATH = "getPath";

	/**
	 * R function name to get vector values by element name: {@value #FUNCTION_GET_VALS_BY_NAME}
	 */
	public static final String FUNCTION_GET_VALS_BY_NAME = "getValuesByName";

}
