package biolockj.util.r;

import biolockj.Config;
import biolockj.util.MetaUtil;

public class StatsUtil extends RScript
{

	private StatsUtil()
	{
	}

	public static String buildMethods() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( rFunctionWilcoxTest() );
		sb.append( rFunctionCalculateStats() );
		sb.append( rMethodBuildSummaryTable() );
		sb.append( rFunctionAddNamedVectorElement() );
		return sb.toString();
	}

	/**
	* Validate configuration file properties used in this utility.
	*
	* @throws Exception
	*/
	public static void checkDependencies() throws Exception
	{
		Config.requireString( R_ADJ_PVALS_LENGTH );
		Config.requireString( R_PVAL_ADJ_METHOD );
	}

	/**
	 * Adds a value to a vector and names the vector element.
	 *
	 * @return String - formatted R function: addNamedVectorElement(myVector, index, value, name)
	 */
	public static String rFunctionAddNamedVectorElement()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "addNamedVectorElement <- function( myVector, name, value )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "myVector[length(myVector) + 1] = value" ) );
		sb.append( getLine( "names(myVector)[length(myVector)] = name" ) );
		sb.append( getLine( "return( myVector )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	* Populate output vectors for reportAttributes
	*
	* @throws Exception
	*/
	public static String rFunctionCalculateStats() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( FUNCTION_CALCULATE_STATS + " <- function(myT, " + BINARY_COLS + ", " + NOMINAL_COLS + ", "
				+ NUMERIC_COLS + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "# Loop through the OTUs to assign P & R^2 values" ) );
		sb.append( initializeVecotrs() );
		sb.append( forEachOTU( filterRareOTUs( calculateNormalPvals() ) ) );
		sb.append( calculateAdjustedPvals() );
		sb.append( getLine( "reportList = list(" + OTU_NAMES + ", " + P_VALS_PAR + ", " + P_VALS_NP + ", "
				+ P_VALS_PAR_ADJ + ", " + P_VALS_NP_ADJ + ", " + R_SQUARED_VALS + ")" ) );
		sb.append( getLine(
				"names(reportList) = c( \"OTU\", \"parPval\", \"nonParPval\", \"adjParPval\", \"adjNonParPval\", \"rSquared\" )" ) );
		sb.append( getLine( "return( reportList )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Executes Coin package wilcox_test for binary data on two p-value vectors.
	 *
	 * @return String - formatted R function: getColor(v)
	 */
	public static String rFunctionWilcoxTest()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "wilcox_test.default <- function(x, y, ...)" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine(
				"data = data.frame(values = c(x, y), group = rep(c(\"x\", \"y\" ), c(length(x), length(y))))" ) );
		sb.append( getLine( "return( wilcox_test(values ~ group, data = data, ...) )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Builds the summary table that will be written to .tsv spreadsheet.
	 * Parameter 1 (vectorNames): contains each type of calculations:
	 * parametericPvals, nonParPvals, adjustedParametricPvals, adjustedNonParPvals.
	 * Parameter 2 (vectors): contains one p-value vector per reported attribute.
	 *
	 * Dependent R functions:
	 * 		1. getValuesByName(pValVector, attName)
	 *
	 * @return String - formatted R function: buildSummaryTable(reportList)
	 */
	public static String rMethodBuildSummaryTable()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( FUNCTION_BUILD_SUMMARY_TABLE + " <- function( reportList, fileName )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "df = data.frame( vector(mode=\"double\", length=length(reportList[[1]])) )" ) );
		sb.append( getLine( "attNames = unique(names(reportList[[2]]))" ) );
		sb.append( getLine( "df[, 1] = reportList[[1]]" ) );
		sb.append( getLine( "names(df)[1] = names(reportList)[1]" ) );
		sb.append( getLine( "for( i in 2:length(reportList) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "for( j in 1:length(attNames) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "df[, length(df)+1] = getValuesByName(reportList[[i]], attNames[j])" ) );
		sb.append( getLine( "names(df)[length(df)] = paste(names(reportList)[i], \"_\", attNames[j], sep=\"\" )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "write.table( df, file=fileName, sep=\"\\t\", row.names=FALSE )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	private static String calculateAdjustedPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# CALCULATE ADJUSTED P_VALS" ) );
		sb.append( getPvalAdjustedLength() );

		if( Config.requireString( R_ADJ_PVALS_LENGTH ).equalsIgnoreCase( ADJ_PVAL_GLOBAL ) )
		{
			sb.append( populateGlobalAdjustedPvals() );
		}
		else
		{
			sb.append( populateAdjustedPvals() );
			sb.append( setAdjPvalVectorNames() );
		}

		if( Config.getBoolean( R_DEBUG ) )
		{
			sb.append( getDebugLine( P_VALS_PAR_ADJ ) );
			sb.append( getDebugLine( P_VALS_NP_ADJ ) );
			sb.append( getDebugLine( OTU_NAMES ) );
		}
		return sb.toString();
	}

	private static String calculateBinaryPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# CALCULATE BINARY P_VALS" ) );
		sb.append( getLine( "for( metaCol in " + BINARY_COLS + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getDebugLine( "metaCol" ) );
		sb.append( getLine( "attName = names(myT)[metaCol]", "attName" ) );
		sb.append( getLine( "att = as.factor(myT[,metaCol])", "att" ) );
		sb.append( getLine( "vals = levels( att )", "vals" ) );
		sb.append( getLine( "myLm = lm( " + OTU_ABUNDANCE + " ~ att, na.action=na.exclude )", "myLm" ) );
		sb.append( getLine(
				P_VALS_PAR + " = addNamedVectorElement( " + P_VALS_PAR + ", attName, t.test( myT[att==vals[1], "
						+ OTU_COL + "], myT[att==vals[2], " + OTU_COL + "] )$p.value )",
				P_VALS_PAR ) );
		sb.append( getLine( P_VALS_NP + " = addNamedVectorElement( " + P_VALS_NP
				+ ", attName, pvalue( wilcox_test( myT[att==vals[1], " + OTU_COL + "], myT[att==vals[2], " + OTU_COL
				+ "] ) ) )", P_VALS_NP ) );
		sb.append( getLine( R_SQUARED_VALS + " = addNamedVectorElement( " + R_SQUARED_VALS
				+ ", attName, summary( myLm )$r.squared )", R_SQUARED_VALS ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	private static String calculateNominalPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# CALCULATE NOMINAL P_VALS" ) );
		sb.append( getLine( "for( metaCol in " + NOMINAL_COLS + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getDebugLine( "metaCol" ) );
		sb.append( getLine( "attName = names(myT)[metaCol]", "attName" ) );
		sb.append( getLine( "att = as.factor(myT[,metaCol])", "att" ) );
		sb.append( getLine( "vals = levels( att )", "vals" ) );
		sb.append( getLine( "myLm = lm( " + OTU_ABUNDANCE + " ~ att, na.action=na.exclude )", "myLm" ) );
		sb.append( getLine( "myAnova = anova( myLm )", "myAnova" ) );
		sb.append(
				getLine( P_VALS_PAR + " = addNamedVectorElement( " + P_VALS_PAR + ", attName, myAnova$\"Pr(>F)\"[1] )",
						P_VALS_PAR ) );
		sb.append( getLine( P_VALS_NP + " = addNamedVectorElement( " + P_VALS_NP + ", attName, kruskal.test( "
				+ OTU_ABUNDANCE + " ~ att, na.action=na.exclude )$p.value )", P_VALS_NP ) );
		sb.append( getLine( R_SQUARED_VALS + " = addNamedVectorElement( " + R_SQUARED_VALS
				+ ", attName, summary( myLm )$r.squared )", R_SQUARED_VALS ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	* Populate output vectors for reportAttributes
	*
	* @throws Exception
	*/
	private static String calculateNormalPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( OTU_NAMES + "[length(" + OTU_NAMES + ")+1] = names(myT)[" + OTU_COL + "]",
				OTU_NAMES + "[length(" + OTU_NAMES + ")]" ) );

		sb.append( getDebugLine( "\"" + OTU_ABUNDANCE + "\"", OTU_ABUNDANCE ) );

		if( !MetaUtil.getBinaryFields().isEmpty() )
		{
			sb.append( calculateBinaryPvals() );
		}

		if( !MetaUtil.getNominalFields().isEmpty() )
		{
			sb.append( calculateNominalPvals() );
		}

		if( !MetaUtil.getNumericFields().isEmpty() )
		{
			sb.append( calculateNumericPvals() );
		}

		return sb.toString();
	}

	private static String calculateNumericPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# CALCULATE NUMERIC P_VALS" ) );
		sb.append( getLine( "for( metaCol in " + NUMERIC_COLS + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getDebugLine( "metaCol" ) );
		sb.append( getLine( "attName = names(myT)[metaCol]", "attName" ) );
		sb.append( getLine( "att = myT[,metaCol]", "att" ) );
		sb.append( getLine( P_VALS_PAR + " = addNamedVectorElement( " + P_VALS_PAR + ", attName, Kendall( "
				+ OTU_ABUNDANCE + ", att )$sl[1] )", P_VALS_PAR ) );
		sb.append( getLine( P_VALS_NP + " = addNamedVectorElement( " + P_VALS_NP + ", attName, cor.test( "
				+ OTU_ABUNDANCE + ", att )$p.value )", P_VALS_NP ) );
		sb.append( getLine( R_SQUARED_VALS + " = addNamedVectorElement( " + R_SQUARED_VALS + ", attName, cor( "
				+ OTU_ABUNDANCE + ", att, use=\"na.or.complete\", method=\"kendall\" )^2 )", R_SQUARED_VALS ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	* Several options (configured in R_ADJ_PVALS_LENGTH) to adjust p-values
	* for multiple hypothesis testing:
	*
	* ADJ_PVAL_GLOBAL: length =  #p-values, all levels, all attributes
	* ADJ_PVAL_ATTRIBUTE: length = #p-values, all levels, 1 attribute
	* ADJ_PVAL_TAXA: length = all #p-values, 1 level, all attributes
	* ADJ_PVAL_LOCAL: length = #p-values in vector (R default), n param omitted
	*
	* Save pvalAdjustLen to n param to p.adjust( v, n=? ), default value is
	* an empty string (empty for ADJ_PVAL_LOCAL)
	*
	* @return R Script lines creating the count variables
	* @throws Exception
	*/
	private static String getPvalAdjustedLength() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final String adjPvalLenMethod = Config.requireString( R_ADJ_PVALS_LENGTH );
		sb.append( getLine( NUM_OTUS + " = length(" + OTU_NAMES + ")", NUM_OTUS ) );
		sb.append( getDebugLine( "\"" + OTU_NAMES + "\"", OTU_NAMES ) );
		if( adjPvalLenMethod.equalsIgnoreCase( ADJ_PVAL_GLOBAL ) )
		{
			sb.append( getLine( "attCount = " + getStandardNumericAttCount(), "attCount" ) );
			sb.append( getLine( "numTaxaLevels = length( " + OTU_LEVELS + " )", "numTaxaLevels" ) );
			pvalAdjustLen = ", n=(" + NUM_OTUS + "*attCount*numTaxaLevels)";
		}
		else if( adjPvalLenMethod.equalsIgnoreCase( ADJ_PVAL_ATTRIBUTE ) )
		{
			sb.append( getLine( "numTaxaLevels = length( " + OTU_LEVELS + " )", "numTaxaLevels" ) );
			pvalAdjustLen = ", n=(" + NUM_OTUS + "*numTaxaLevels)";
		}
		else if( adjPvalLenMethod.equalsIgnoreCase( ADJ_PVAL_TAXA ) )
		{
			sb.append( getLine( "attCount = " + getStandardNumericAttCount(), "attCount" ) );
			pvalAdjustLen = ", n=(" + NUM_OTUS + "*attCount)";
		}

		return sb.toString();
	}

	private static String getStandardNumericAttCount() throws Exception
	{
		String val = "length( " + ALL_ATTS + " )";
		int i = 0;
		if( Config.getBoolean( MetaUtil.NUM_READS ) )
		{
			i++;
		}
		if( Config.getBoolean( MetaUtil.NUM_HITS ) )
		{
			i++;
		}

		if( i > 0 )
		{
			val = val + " - " + i;
		}

		return val;
	}

	private static String initializeVecotrs() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# CALCULATE NOMINAL P_VALS" ) );
		sb.append( getLine( OTU_NAMES + " = vector( mode=\"character\" )" ) );
		sb.append( getLine( P_VALS_PAR + " = vector( mode=\"double\" )" ) );
		sb.append( getLine( P_VALS_NP + " = vector( mode=\"double\" )" ) );
		sb.append( getLine( R_SQUARED_VALS + " = vector( mode=\"double\" )" ) );
		sb.append( getLine( P_VALS_PAR_ADJ + " = vector( mode=\"double\" )" ) );
		sb.append( getLine( P_VALS_NP_ADJ + " = vector( mode=\"double\" )" ) );
		return sb.toString();
	}

	private static String populateAdjustedPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "adjParDF = data.frame( vector(mode=\"double\", length=length(" + OTU_NAMES + ")) )" ) );
		sb.append( getLine( "adjNonParDF = data.frame( vector(mode=\"double\", length=length(" + OTU_NAMES + ")) )" ) );
		sb.append( getLine( "for( i in 1:length( " + ALL_ATTS + " ) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getDebugLine( ALL_ATTS + "[i]" ) );
		sb.append( getLine( "parPvals = getValuesByName( " + P_VALS_PAR + ", " + ALL_ATTS + "[i] )", "parPvals" ) );
		sb.append( getLine( "npPvals = getValuesByName( " + P_VALS_NP + ", " + ALL_ATTS + "[i] )", "npPvals" ) );
		sb.append( getLine( "adjParDF[,i] = p.adjust( parPvals, method=\"" + Config.requireString( R_PVAL_ADJ_METHOD )
				+ "\"" + pvalAdjustLen + " )" ) );
		sb.append( getLine( "adjNonParDF[,i] = p.adjust( npPvals, method=\"" + Config.requireString( R_PVAL_ADJ_METHOD )
				+ "\"" + pvalAdjustLen + " )" ) );
		sb.append( getLine( "names(adjParDF)[i] = " + ALL_ATTS + "[i]" ) );
		sb.append( getLine( "names(adjNonParDF)[i] = " + ALL_ATTS + "[i]" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "# GET ADJUSTED P_VALS AS VECTOR" ) );
		sb.append( getLine( P_VALS_PAR_ADJ + " = as.vector( as.matrix(adjParDF) )", P_VALS_PAR_ADJ ) );
		sb.append( getLine( P_VALS_NP_ADJ + " = as.vector( as.matrix(adjNonParDF) )", P_VALS_NP_ADJ ) );
		return sb.toString();
	}

	private static String populateGlobalAdjustedPvals() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( P_VALS_PAR_ADJ + " = p.adjust( " + P_VALS_PAR + ", method=\""
				+ Config.requireString( R_PVAL_ADJ_METHOD ) + "\"" + pvalAdjustLen + " )" ) );
		sb.append( getLine( P_VALS_NP_ADJ + " = p.adjust( " + P_VALS_NP + ", method=\""
				+ Config.requireString( R_PVAL_ADJ_METHOD ) + "\"" + pvalAdjustLen + " )" ) );
		return sb.toString();
	}

	private static String setAdjPvalVectorNames()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# Assign metadata attribute names to the adjusted p-value vector" ) );
		sb.append( getLine( "for( i in 1:length( " + ALL_ATTS + " ) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "y = " + NUM_OTUS + " * i" ) );
		sb.append( getLine( "x = y - " + NUM_OTUS + " + 1" ) );
		sb.append( getLine( "names( " + P_VALS_PAR_ADJ + " )[x:y] = rep( " + ALL_ATTS + "[i], " + NUM_OTUS + " )" ) );
		sb.append( getLine( "names( " + P_VALS_NP_ADJ + " )[x:y] = rep( " + ALL_ATTS + "[i], " + NUM_OTUS + " )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	public static final String FUNCTION_BUILD_SUMMARY_TABLE = "buildSummaryTable";
	public static final String FUNCTION_CALCULATE_STATS = "calculateStats";

	private static final String ADJ_PVAL_ATTRIBUTE = "ATTRIBUTE";
	private static final String ADJ_PVAL_GLOBAL = "GLOBAL";
	private static final String ADJ_PVAL_TAXA = "TAXA";
	private static final String NUM_OTUS = "NUM_OTUS";
	private static final String OTU_ABUNDANCE = "myT[,otuCol]";
	private static final String OTU_NAMES = "otuNames";
	private static final String P_VALS_NP = "pValsNonPar";
	private static final String P_VALS_NP_ADJ = P_VALS_NP + "Adj";
	private static final String P_VALS_PAR = "pValsPar";
	private static final String P_VALS_PAR_ADJ = P_VALS_PAR + "Adj";
	private static String pvalAdjustLen = "";
	private static final String R_ADJ_PVALS_LENGTH = "r.adjPvalsScope";
	private static final String R_PVAL_ADJ_METHOD = "r.pAdjMethod";
	private static final String R_SQUARED_VALS = "rSquaredVals";
}
