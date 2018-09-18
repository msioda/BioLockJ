/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date May 15, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.r;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import biolockj.Config;
import biolockj.module.BioModule;
import biolockj.util.ModuleUtil;
import biolockj.util.r.BaseScript;
import biolockj.util.r.RMetaUtil;

/**
 * This BioModule is used to build the R script used to generate taxonomy statistics and plots.
 */
public class CalculateStats extends R_Module implements BioModule
{
	/**
	 * Validate configuration file properties used to build the R report:
	 * <ul>
	 * <li>super.checkDependencies()
	 * <li>Require {@value #R_ADJ_PVALS_SCOPE}
	 * <li>Require {@value #R_PVAL_ADJ_METHOD}
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getOtuTableFilter();
		Config.requireString( R_ADJ_PVALS_SCOPE );
		Config.requireString( R_PVAL_ADJ_METHOD );
	}

	/**
	 * Builds the R script used to generate taxonomy-level P-value and R^2 statistics.
	 * <p>
	 * Dependent BioLockJ Java-R methods called in this method:
	 * <ol>
	 * <li>{@link #rFunctionWilcoxTest()}
	 * <li>{@link #rFunctionCalculateStats()}
	 * <li>{@link #rMethodBuildSummaryTables()}
	 * <li>{@link #rFunctionAddNamedVectorElement()}
	 * <li>{@link #rLinesExecuteProgram()}
	 * </ol>
	 */
	@Override
	public void executeTask() throws Exception
	{
		super.executeTask();
		updatePrimaryScript( rFunctionWilcoxTest() );
		updatePrimaryScript( rFunctionCalculateStats() );
		updatePrimaryScript( rMethodBuildSummaryTables() );
		updatePrimaryScript( rFunctionAddNamedVectorElement() );
		updatePrimaryScript( rLinesExecuteProgram() );
	}

	/**
	 * Import the "coin" and "Kendall" libraries used in this script.
	 * 
	 * @return rCode
	 */
	@Override
	public List<String> getRequiredLibs()
	{
		return Arrays.asList( new String[] { "coin", "Kendall" } );
	}

	/**
	 * This method returns R code used to calculate the P-values for all data types.
	 * <p>
	 * BioLockJ Java-R functions called by this method:
	 * <ul>
	 * <li>calculateBinaryPvals()
	 * <li>calculateNominalPvals()
	 * <li>calculateNumericPvals()
	 * </ul>
	 *
	 * @return R code
	 * @throws Exception if propagated by the sub-methods
	 */
	protected String calculateStandardPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( OTU_NAMES + "[length(" + OTU_NAMES + ")+1] = names(" + OTU_TABLE + ")[" + OTU_COL + "]" ) );
		sb.append( getDebugLine( OTU_NAMES + "[ length(" + OTU_NAMES + ") ]" ) );
		sb.append( getDebugLine( "\"" + OTU_ABUNDANCE + "\"" ) );
		sb.append( RMetaUtil.getBinaryFields().isEmpty() ? "": calculateBinaryPvals() );
		sb.append( RMetaUtil.getNominalFields().isEmpty() ? "": calculateNominalPvals() );
		sb.append( RMetaUtil.getNumericFields().isEmpty() ? "": calculateNumericPvals() );
		return sb.toString();
	}

	/**
	 * Configure {@value #R_ADJ_PVALS_SCOPE} to set the p.adjust "n" parameter for multiple hypothesis testing:<br>
	 * <ul>
	 * <li>{@value #ADJ_PVAL_GLOBAL}: length = #p-values, all levels, all fields
	 * <li>{@value #ADJ_PVAL_ATTRIBUTE}:: length = #p-values, all levels, 1 field
	 * <li>{@value #ADJ_PVAL_TAXA}: length = all #p-values, 1 level, all fields
	 * <li>{@value #ADJ_PVAL_LOCAL}: length = #p-values in vector (R default)
	 * </ul>
	 * 
	 * The R default value is an empty string (as used by ADJ_PVAL_LOCAL).
	 *
	 * @return R code
	 * @throws Exception if propagated by child methods
	 */
	protected String getPvalAdjustedLength() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final String adjPvalLenMethod = Config.requireString( R_ADJ_PVALS_SCOPE );
		sb.append( getDebugLine( "\"" + OTU_NAMES + "\"" ) );
		if( adjPvalLenMethod.equalsIgnoreCase( ADJ_PVAL_GLOBAL ) )
		{
			sb.append( getLine( "attCount = length( " + ALL_ATTS + " )", true ) );
			sb.append( getLine( "numTaxaLevels = length( " + OTU_LEVELS + " )", true ) );
			pvalAdjustLen = ", n=( length(" + OTU_NAMES + ")*attCount*numTaxaLevels )";
		}
		else if( adjPvalLenMethod.equalsIgnoreCase( ADJ_PVAL_ATTRIBUTE ) )
		{
			sb.append( getLine( "numTaxaLevels = length( " + OTU_LEVELS + " )", true ) );
			pvalAdjustLen = ", n=( length(" + OTU_NAMES + ")*numTaxaLevels )";
		}
		else if( adjPvalLenMethod.equalsIgnoreCase( ADJ_PVAL_TAXA ) )
		{
			sb.append( getLine( "attCount = length( " + ALL_ATTS + " )", true ) );
			pvalAdjustLen = ", n=( length(" + OTU_NAMES + ")*attCount )";
		}

		return sb.toString();
	}

	/**
	 * Initialize the vectors that will hold report statistics.
	 * 
	 * @return rCode
	 */
	protected String initializeVecotrs()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( OTU_NAMES + " = vector( mode=\"character\" )" ) );
		sb.append( getLine( P_VALS_PAR + " = vector( mode=\"double\" )" ) );
		sb.append( getLine( P_VALS_NP + " = vector( mode=\"double\" )" ) );
		sb.append( getLine( R_SQUARED_VALS + " = vector( mode=\"double\" )" ) );
		sb.append( getLine( P_VALS_PAR_ADJ + " = vector( mode=\"double\" )" ) );
		sb.append( getLine( P_VALS_NP_ADJ + " = vector( mode=\"double\" )" ) );
		return sb.toString();
	}

	/**
	 * Initialize the vectors that will hold report statistics.
	 * <p>
	 * Dependent R function: {@value biolockj.util.r.BaseScript#FUNCTION_GET_VALS_BY_NAME}
	 * 
	 * @return rCode
	 * @throws Exception if R syntax or runtime errors occur
	 */
	protected String populateAdjustedPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "adjParDF = data.frame( vector(mode=\"double\", length=length(" + OTU_NAMES + ")) )" ) );
		sb.append( getLine( "adjNonParDF = data.frame( vector(mode=\"double\", length=length(" + OTU_NAMES + ")) )" ) );
		sb.append( getLine( "for( i in 1:length( " + ALL_ATTS + " ) ) {" ) );
		sb.append( getDebugLine( ALL_ATTS + "[i]" ) );
		sb.append( getLine(
				"parPvals = " + BaseScript.FUNCTION_GET_VALS_BY_NAME + "( " + P_VALS_PAR + ", " + ALL_ATTS + "[i] )",
				true ) );
		sb.append( getLine(
				"npPvals = " + BaseScript.FUNCTION_GET_VALS_BY_NAME + "( " + P_VALS_NP + ", " + ALL_ATTS + "[i] )",
				true ) );
		sb.append( getLine( "adjParDF[,i] = p.adjust( parPvals, method=\"" + Config.requireString( R_PVAL_ADJ_METHOD )
				+ "\"" + pvalAdjustLen + " )" ) );
		sb.append( getLine( "adjNonParDF[,i] = p.adjust( npPvals, method=\"" + Config.requireString( R_PVAL_ADJ_METHOD )
				+ "\"" + pvalAdjustLen + " )" ) );
		sb.append( getLine( "names(adjParDF)[i] = " + ALL_ATTS + "[i]" ) );
		sb.append( getLine( "names(adjNonParDF)[i] = " + ALL_ATTS + "[i]" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "# GET ADJUSTED P_VALS AS VECTOR", true ) );
		sb.append( getLine( P_VALS_PAR_ADJ + " = as.vector( as.matrix(adjParDF) )", true ) );
		sb.append( getLine( P_VALS_NP_ADJ + " = as.vector( as.matrix(adjNonParDF) )", true ) );
		return sb.toString();
	}

	/**
	 * Call p.adjust method on parametric and non-parametric pvalue vectors.
	 * 
	 * @return rCode
	 * @throws Exception if R syntax or runtime errors occur
	 */
	protected String populateGlobalAdjustedPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( P_VALS_PAR_ADJ + " = p.adjust( " + P_VALS_PAR + ", method=\""
				+ Config.requireString( R_PVAL_ADJ_METHOD ) + "\"" + pvalAdjustLen + " )" ) );
		sb.append( getLine( P_VALS_NP_ADJ + " = p.adjust( " + P_VALS_NP + ", method=\""
				+ Config.requireString( R_PVAL_ADJ_METHOD ) + "\"" + pvalAdjustLen + " )" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function: {@value #FUNCTION_ADD_V_ELEMENT}( v, name, value )<br>
	 * The R method adds a value to a vector and names the vector element.
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>v = Target vector
	 * <li>name = Vector element name
	 * <li>value = Vector element value
	 * </ul>
	 *
	 * @return R code
	 */
	protected String rFunctionAddNamedVectorElement()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( FUNCTION_ADD_V_ELEMENT + " <- function( v, name, value ) {" ) );
		sb.append( getLine( "v[length(v) + 1] = value" ) );
		sb.append( getLine( "names(v)[length(v)] = name" ) );
		sb.append( getLine( "return( v )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function: {@value #FUNCTION_CALCULATE_STATS}(otuTable, {@value #BINARY_COLS},
	 * {@value #NOMINAL_COLS}, {@value #NUMERIC_COLS})<br>
	 * This method builds the report spreadsheet containing [adjusted + unadjusted] [parametric + non-parametric]
	 * p-values and R^2 values.
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>{@value #OTU_TABLE} = Data.Frame returned by read.table( otuFilePath ) for a taxonomy-level OTU table
	 * <li>{@value #BINARY_COLS} = Vector of binary column indexes (possibly empty but never null)
	 * <li>{@value #NOMINAL_COLS} = Vector of categorical column indexes (possibly empty but never null)
	 * <li>{@value #NUMERIC_COLS} = Vector of numeric column indexes (possibly empty but never null)
	 * </ul>
	 * <p>
	 * BioLockJ R functions called by this method:
	 * <ul>
	 * <li>initializeVecotrs()
	 * <li>forEachOTU()
	 * <li>filterRareOTUs()
	 * <li>calculateStandardPvals()
	 * <li>calculateAdjustedPvals()
	 * </ul>
	 *
	 * @return R code
	 * @throws Exception if R script syntax or runtime error occurrs
	 */
	protected String rFunctionCalculateStats() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( FUNCTION_CALCULATE_STATS + " <- function(" + OTU_TABLE + ", " + BINARY_COLS + ", "
				+ NOMINAL_COLS + ", " + NUMERIC_COLS + " ) {" ) );
		sb.append( getLine( "# Loop through the OTUs to assign P-value & R^2 values" ) );
		sb.append( initializeVecotrs() );
		sb.append( forEachOtu( filterRareOtus( calculateStandardPvals() ) ) );
		sb.append( calculateAdjustedPvals() );
		sb.append( getLine( "reportStats = list(" + OTU_NAMES + ", " + P_VALS_PAR + ", " + P_VALS_NP + ", "
				+ P_VALS_PAR_ADJ + ", " + P_VALS_NP_ADJ + ", " + R_SQUARED_VALS + ")" ) );
		sb.append( getLine( "names(reportStats) = c( \"" + OTU + "\", \"" + P_VALS_PAR + "\", \"" + P_VALS_NP + "\", \""
				+ P_VALS_PAR_ADJ + "\", \"" + P_VALS_NP_ADJ + "\", \"" + R_SQUARED_VALS + "\" )" ) );
		sb.append( getLine( "return( reportStats )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function: {@value #FUNCTION_WILCOX_TEST}( x, y, ...)<br>
	 * Method executes Coin package wilcox_test for binary data on two p-value vectors.
	 *
	 * @return R code
	 */
	protected String rFunctionWilcoxTest()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( FUNCTION_WILCOX_TEST + ".default <- function( x, y, ... ) {" ) );
		sb.append( getLine(
				"data = data.frame( values = c(x, y), group = rep( c(\"x\", \"y\"), c(length(x), length(y)) ) )" ) );
		sb.append( getLine( "return( wilcox_test( values ~ group, data = data, ... ) )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * The method returns R code for the function main(). For each taxonomy level - {@value #OTU_LEVEL}:<br>
	 * <ol>
	 * <li>Output tsv file with parametric p-values for each reportable metadata field
	 * <li>Output tsv file with non-parametric p-values for each reportable metadata field
	 * <li>Output tsv file with adjusted parametric p-values for each reportable metadata field
	 * <li>Output tsv file with adjusted non-parametric p-values for each reportable metadata field
	 * <li>Output tsv file with R^2 values for each reportable metadata field
	 * </ol>
	 */
	@Override
	protected String rMainMethod() throws Exception
	{
		final StringBuffer rCode = new StringBuffer();
		rCode.append( rLinesReadOtuTable() );
		rCode.append( rLinesCreateAttColVectors() );
		rCode.append( getLine( "reportStats = " + FUNCTION_CALCULATE_STATS + "( " + OTU_TABLE + ", " + BINARY_COLS
				+ ", " + NOMINAL_COLS + ", " + NUMERIC_COLS + " )" ) );
		rCode.append( getLine( FUNCTION_BUILD_SUMMARY_TABLES + "( reportStats, " + OTU_LEVEL + " )" ) );

		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( METHOD_MAIN + " <- function() {" ) );
		sb.append( getLine( "stopifnot( " + FUNCTION_IMPORT_LIBS + "() )" ) );
		sb.append( forEachOtuLevel( enableDebug( rCode.toString(), OTU_LEVEL ) ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function {@value #FUNCTION_BUILD_SUMMARY_TABLES}(reportStats, otuLevel).<br>
	 * This function generates 1 table for each statistic column in the reportStats.
	 * <p>
	 * R Function parameters:<br>
	 * Parameter *reportStats* (type=R List): contains 6 columns and 1st row of headers = reported metadata field names:
	 * <ol>
	 * <li>otuNames
	 * <li>parametericPvals
	 * <li>nonParPvals
	 * <li>adjustedParametricPvals
	 * <li>adjustedNonParPvals
	 * <li>rSquaredVals
	 * </ol>
	 * <br>
	 * Parameter *otuLevel* (type=character): is the report taxonomy level name
	 * <p>
	 * Dependent R function: {@value biolockj.util.r.BaseScript#FUNCTION_GET_VALS_BY_NAME}
	 * 
	 * @return R code
	 * @throws Exception if errors occur
	 */
	protected String rMethodBuildSummaryTables() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( FUNCTION_BUILD_SUMMARY_TABLES + " <- function( reportStats, otuLevel ) {" ) );
		sb.append( getLine( "attNames = unique( names(reportStats[[2]]) )", true ) );
		sb.append( getLine( "if( length( attNames ) == 0 ) {" ) );
		sb.append( getLine( "print( paste( \"Insufficient data to calculate pvalues at OTU level:\", otuLevel ) )" ) );
		// sb.append( getLine( "stop(\"Insufficient data to calculate pvalues! See debug files in temp directory for
		// more info.\")" ) );
		sb.append( getLine( "} else {" ) );
		sb.append( getLine( "for( i in 2:length( reportStats ) ) {" ) );
		sb.append( getLine( "fileName = " + BaseScript.FUNCTION_GET_PATH + "(" + R_OUTPUT_DIR
				+ ", paste0( otuLevel, \"_\", names(reportStats)[i], \"" + TSV_EXT + "\" ) )", true ) );
		sb.append( getLine( "df = data.frame( vector( mode=\"double\", length=length( reportStats[[1]] ) ) )" ) );
		sb.append( getLine( "df[, 1] = reportStats[[1]]" ) );
		sb.append( getLine( "names(df)[1] = names( reportStats )[1]", true ) );
		sb.append( getLine( "for( j in 1:length( attNames ) ) {" ) );
		sb.append( getLine( "df[, length(df)+1] = " + BaseScript.FUNCTION_GET_VALS_BY_NAME
				+ "( reportStats[[i]], attNames[j] )" ) );
		sb.append( getLine( "names(df)[length(df)] = attNames[j]", true ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "write.table( df, file=fileName, sep=\"\\t\", row.names=FALSE )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();

	}

	/**
	 * Set element names in the adjusted p-value vectors with the metadata field name.
	 * 
	 * @return rCode
	 */
	protected String setAdjPvalVectorNames()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# Assign metadata field names to the adjusted p-value vector" ) );
		sb.append( getLine( "for( i in 1:length( " + ALL_ATTS + " ) ) {" ) );
		sb.append( getLine( "y = length( " + OTU_NAMES + " ) * i" ) );
		sb.append( getLine( "x = y - length( " + OTU_NAMES + " ) + 1" ) );
		sb.append( getLine(
				"names( " + P_VALS_PAR_ADJ + " )[x:y] = rep( " + ALL_ATTS + "[i], length( " + OTU_NAMES + " ) )" ) );
		sb.append( getLine(
				"names( " + P_VALS_NP_ADJ + " )[x:y] = rep( " + ALL_ATTS + "[i], length( " + OTU_NAMES + " ) )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns lines used to calculate adjusted p-values based on the {@value #R_ADJ_PVALS_SCOPE}.
	 * <p>
	 * BioLockJ Java functions called by this method:
	 * <ul>
	 * <li>getPvalAdjustedLength()
	 * <li>populateGlobalAdjustedPvals()
	 * <li>populateAdjustedPvals()
	 * <li>setAdjPvalVectorNames()
	 * </ul>
	 *
	 * @return R code
	 * @throws Exception if propagated from the child methods
	 */
	private String calculateAdjustedPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# CALCULATE ADJUSTED P_VALS", true ) );
		sb.append( getPvalAdjustedLength() );

		if( Config.requireString( R_ADJ_PVALS_SCOPE ).equalsIgnoreCase( ADJ_PVAL_GLOBAL ) )
		{
			sb.append( populateGlobalAdjustedPvals() );
		}
		else
		{
			sb.append( populateAdjustedPvals() );
			sb.append( setAdjPvalVectorNames() );
		}

		sb.append( getDebugLine( OTU_NAMES ) );
		sb.append( getDebugLine( P_VALS_PAR_ADJ ) );
		sb.append( getDebugLine( P_VALS_NP_ADJ ) );

		return sb.toString();
	}

	/**
	 * Method returns R code to calculate P-values for binary metadata fields.
	 * <p>
	 * BioLockJ R Script functions called by this method:
	 * <ul>
	 * <li>{@value #FUNCTION_ADD_V_ELEMENT}( pVals, attName, value)
	 * <li>{@value #FUNCTION_WILCOX_TEST}( v1, v2)
	 * </ul>
	 *
	 * @return R code
	 * @throws Exception if unable to write script lines
	 */
	private String calculateBinaryPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# CALCULATE BINARY P_VALS", true ) );
		sb.append( getLine( "for( metaCol in " + BINARY_COLS + " ) {" ) );
		sb.append( getDebugLine( "metaCol" ) );
		sb.append( getLine( "attName = names( " + OTU_TABLE + " )[metaCol]", true ) );
		sb.append( getLine( "att = as.factor( " + OTU_TABLE + "[,metaCol] )", true ) );
		sb.append( getLine( "vals = levels( att )", true ) );
		sb.append( getLine( "myLm = lm( " + OTU_ABUNDANCE + " ~ att, na.action=na.exclude )", true ) );
		sb.append( getLine( P_VALS_PAR + " = " + FUNCTION_ADD_V_ELEMENT + "( " + P_VALS_PAR + ", attName, t.test( "
				+ OTU_TABLE + "[att==vals[1], " + OTU_COL + "], " + OTU_TABLE + "[att==vals[2], " + OTU_COL
				+ "] )$p.value )", true ) );
		sb.append( getLine( P_VALS_NP + " = " + FUNCTION_ADD_V_ELEMENT + "( " + P_VALS_NP
				+ ", attName, pvalue( wilcox_test( " + OTU_TABLE + "[att==vals[1], " + OTU_COL + "], " + OTU_TABLE
				+ "[att==vals[2], " + OTU_COL + "] ) ) )", true ) );
		sb.append( getLine( R_SQUARED_VALS + " = " + FUNCTION_ADD_V_ELEMENT + "( " + R_SQUARED_VALS
				+ ", attName, summary( myLm )$r.squared )", true ) );
		sb.append( verifyPvals() );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Method returns R code to calculate P-values for categorical metadata fields.
	 * <p>
	 * Dependent R function: {@value biolockj.util.r.BaseScript#FUNCTION_GET_VALS_BY_NAME}
	 * 
	 * @return R code
	 * @throws Exception if unable to write script lines
	 */
	private String calculateNominalPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# CALCULATE NOMINAL P_VALS", true ) );
		sb.append( getLine( "for( metaCol in " + NOMINAL_COLS + " ) {" ) );
		sb.append( getDebugLine( "metaCol" ) );
		sb.append( getLine( "attName = names( " + OTU_TABLE + " )[metaCol]", true ) );
		sb.append( getLine( "att = as.factor( " + OTU_TABLE + "[,metaCol] )", true ) );
		sb.append( getLine( "vals = levels( att )", true ) );
		sb.append( getLine( "myLm = lm( " + OTU_ABUNDANCE + " ~ att, na.action=na.exclude )", true ) );
		sb.append( getLine( "myAnova = anova( myLm )", true ) );
		sb.append( getLine(
				P_VALS_PAR + " = " + FUNCTION_ADD_V_ELEMENT + "( " + P_VALS_PAR + ", attName, myAnova$\"Pr(>F)\"[1] )",
				true ) );
		sb.append( getLine( P_VALS_NP + " = " + FUNCTION_ADD_V_ELEMENT + "( " + P_VALS_NP + ", attName, kruskal.test( "
				+ OTU_ABUNDANCE + " ~ att, na.action=na.exclude )$p.value )", true ) );
		sb.append( getLine( R_SQUARED_VALS + " = " + FUNCTION_ADD_V_ELEMENT + "( " + R_SQUARED_VALS
				+ ", attName, summary( myLm )$r.squared )", true ) );
		sb.append( verifyPvals() );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Method returns R code to calculate P-values for numeric metadata fields.
	 * <p>
	 * Dependent R function: {@value biolockj.util.r.BaseScript#FUNCTION_GET_VALS_BY_NAME}
	 *
	 * @return R code
	 * @throws Exception if unable to write script lines
	 */
	private String calculateNumericPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# CALCULATE NUMERIC P_VALS", true ) );
		sb.append( getLine( "for( metaCol in " + NUMERIC_COLS + " ) {" ) );
		sb.append( getDebugLine( "metaCol" ) );
		sb.append( getLine( "attName = names( " + OTU_TABLE + " )[metaCol]", true ) );
		sb.append( getLine( "att = " + OTU_TABLE + "[,metaCol]", true ) );
		sb.append( getLine( P_VALS_PAR + " = " + FUNCTION_ADD_V_ELEMENT + "( " + P_VALS_PAR + ", attName, Kendall( "
				+ OTU_ABUNDANCE + ", att )$sl[1] )", true ) );
		sb.append( getLine( P_VALS_NP + " = " + FUNCTION_ADD_V_ELEMENT + "( " + P_VALS_NP + ", attName, cor.test( "
				+ OTU_ABUNDANCE + ", att )$p.value )", true ) );
		sb.append( getLine( R_SQUARED_VALS + " = " + FUNCTION_ADD_V_ELEMENT + "( " + R_SQUARED_VALS + ", attName, cor( "
				+ OTU_ABUNDANCE + ", att, use=\"na.or.complete\", method=\"kendall\" )^2 )", true ) );
		sb.append( verifyPvals() );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	private String verifyPvals()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "if( length( " + P_VALS_PAR + " ) == 0 || length( " + P_VALS_NP + " ) == 0 || length( "
				+ R_SQUARED_VALS + " ) == 0 )  {" ) );
		sb.append( getLine(
				"stop(\"Insufficient data to calculate pvalues!  See debug files in temp directory for more info.\")" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Get the stats file for the given fileType and otuLevel.
	 * 
	 * @param otuLevel OTU level
	 * @param fileType (parametric/non-parametric) (adjusted/non-adjusted)
	 * @return File with stats for fileType
	 * @throws Exception if CalculateStats not found
	 */
	public static File getStatsFile( final String otuLevel, final String fileType ) throws Exception
	{
		final BioModule mod = ModuleUtil.getModule( CalculateStats.class.getName() );
		final String fileName = mod.getOutputDir().getAbsolutePath() + File.separator
				+ Config.getString( Config.INTERNAL_PIPELINE_NAME ) + "_" + otuLevel + "_" + fileType
				+ R_Module.TSV_EXT;

		final File file = new File( fileName );
		if( file.exists() )
		{
			return file;
		}

		return null;
	}

	/**
	 * OTU column identifier: {@value #OTU}
	 */
	public static final String OTU = "OTU";

	/**
	 * Non parametric p-value identifier: {@value #P_VALS_NP}
	 */
	public static final String P_VALS_NP = "nonParametricPval";

	/**
	 * Non parametric adjusted p-value identifier: {@value #P_VALS_NP_ADJ}
	 */
	public static final String P_VALS_NP_ADJ = P_VALS_NP + "Adj";

	/**
	 * Parametric p-value identifier: {@value #P_VALS_PAR}
	 */
	public static final String P_VALS_PAR = "parametricPval";

	/**
	 * Parametric adjusted p-value identifier: {@value #P_VALS_PAR_ADJ}
	 */
	public static final String P_VALS_PAR_ADJ = P_VALS_PAR + "Adj";

	/**
	 * R^2 identifier: {@value #R_SQUARED_VALS}
	 */
	public static final String R_SQUARED_VALS = "rSquaredVals"; 

	/**
	 * This {@value #R_PVAL_ADJ_METHOD} option can be set in {@link biolockj.Config} file: {@value #ADJ_PVAL_ATTRIBUTE}
	 */
	protected static final String ADJ_PVAL_ATTRIBUTE = "ATTRIBUTE";

	/**
	 * This {@value #R_PVAL_ADJ_METHOD} option can be set in {@link biolockj.Config} file: {@value #ADJ_PVAL_GLOBAL}
	 */
	protected static final String ADJ_PVAL_GLOBAL = "GLOBAL";

	/**
	 * This {@value #R_PVAL_ADJ_METHOD} option can be set in {@link biolockj.Config} file: {@value #ADJ_PVAL_LOCAL}
	 */
	protected static final String ADJ_PVAL_LOCAL = "LOCAL";

	/**
	 * This {@value #R_PVAL_ADJ_METHOD} option can be set in {@link biolockj.Config} file: {@value #ADJ_PVAL_TAXA}
	 */
	protected static final String ADJ_PVAL_TAXA = "TAXA";

	/**
	 * R function used to add a named element to a vector: {@value #FUNCTION_ADD_V_ELEMENT}
	 */
	protected static final String FUNCTION_ADD_V_ELEMENT = "addNamedVectorElement";

	/**
	 * R function used to generate the primary BioModule output: {@value #FUNCTION_BUILD_SUMMARY_TABLES}
	 */
	protected static final String FUNCTION_BUILD_SUMMARY_TABLES = "buildSummaryTables";

	/**
	 * R function used to call the right statistical method for each reportable metadata field:
	 * {@value #FUNCTION_CALCULATE_STATS}
	 */
	protected static final String FUNCTION_CALCULATE_STATS = "calculateStats";

	/**
	 * R function used to apply the exact wilcox_test to a binary field: {@value #FUNCTION_BUILD_SUMMARY_TABLES}
	 */
	protected static final String FUNCTION_WILCOX_TEST = "wilcox_test";

	/**
	 * {@link biolockj.Config} String property: {@value #R_ADJ_PVALS_SCOPE} defines R p.adjust( n ) parameter. There are
	 * 4 supported options:
	 * <ol>
	 * <li>{@value #ADJ_PVAL_GLOBAL} n = number of all pVal tests for all fields and all taxonomy levels
	 * <li>{@value #ADJ_PVAL_ATTRIBUTE} n = number of all pVal tests for 1 field at all taxonomy levels
	 * <li>{@value #ADJ_PVAL_TAXA} n = number of all pVal tests for all fields at 1 taxonomy level
	 * <li>{@value #ADJ_PVAL_LOCAL} n = number of all pVal tests for 1 field at 1 taxonomy level
	 * </ol>
	 */
	protected static final String R_ADJ_PVALS_SCOPE = "rStats.pAdjustScope";

	/**
	 * {@link biolockj.Config} String property: {@value #R_PVAL_ADJ_METHOD} defines p.adjust( method ) parameter.
	 * p.adjust.methods = c("holm", "hochberg", "hommel", "bonferroni", "BH", "BY", "fdr", "none")
	 */
	protected static final String R_PVAL_ADJ_METHOD = "rStats.pAdjustMethod";

	private static final String OTU_ABUNDANCE = OTU_TABLE + "[,otuCol]";
	private static final String OTU_NAMES = "otuNames";
	private static String pvalAdjustLen = "";
}
