/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Sep 23, 2017
 */
package biolockj.util.r;

import org.apache.commons.lang.math.NumberUtils;
import biolockj.Config;
import biolockj.Log;
import biolockj.util.MetaUtil;

/**
 * RScriptPlotUtil contains the code used call the appropriate methods for
 *
 */
public class PlotUtil extends RScript
{
	private PlotUtil()
	{
	}

	/**
	 * Each page should display graphs for only one OTU.
	 * This mehtod fills the extra space with empty plots (if needed).
	 *
	 * @return String lines of R code
	 */
	public static String addEmptyGraphToFillPage()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "while( ( " + PLOT_INDEX + " %% 4 ) != 0 )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "plot.new()" ) );
		sb.append( getLine( PLOT_INDEX + " = " + PLOT_INDEX + " + 1" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Adds box-plot, to compare OTU column data by metadata attribute factor levels (categories).
	 *
	 * Dependent R functions:
	 * 		1. displayPval(pval, label, lineAdj)
	 * 		2. addPlotLabel(label, size, color, las, side, rowIndex, colIndex)
	 * 		3. getColor(pVals)
	 * 	 	4. getFactorGroups(myT, attName, otuCol)
	 *
	 * sb.append( getLine( "i = ( i %% 4 ) + 1" ) );
	 *
	 * Set these values for parameters
	 * parametricPvals[i]
	 * parametricPvals[i]
	 *
	 * @return String - formatted R function: addBoxPlot(myT, otuCol, metaCol, reportList, i )
	 * @throws Exception
	 */
	public static String buildAddBoxPlotFunction() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "addBoxPlot <- function( myT, otuCol, metaCol, reportList, plotCount, otuCount )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "att = as.factor(myT[,metaCol])" ) );
		sb.append( getLine( "attName = names(myT)[metaCol]" ) );
		sb.append( getLine( "adjPv = reportList[[4]][names(reportList[[4]])==attName]" ) );
		sb.append( getLine( "adjNonPv = reportList[[5]][names(reportList[[5]])==attName]" ) );
		sb.append( getLine( "color = getColor( c( adjPv[" + OTU_INDEX + "], adjNonPv[" + OTU_INDEX + "] ) )" ) );
		sb.append(
				getLine( "boxplot( getFactorGroups(myT, att, otuCol), outline=FALSE, names=getLabels(levels(att)), las=getLas(levels(att)), pch="
						+ getPch( Config.requireString( R_PCH ) ) + ", ylab=\"\" )" ) );
		sb.append(
				getLine( "stripchart( myT[,otuCol] ~ att, data=data.frame(myT[,otuCol],att), method=\"jitter\", vertical=TRUE, pch="
						+ getPch( Config.requireString( R_PCH ) ) + ", col=\"" + Config.requireString( R_POINT_COLOR )
						+ "\", add=TRUE )" ) );
		sb.append( getLine( "displayPval( adjPv[" + OTU_INDEX + "], \"" + PAR_LABEL + "\", 2 )" ) );
		sb.append( getLine( "displayPval( adjNonPv[" + OTU_INDEX + "], \"" + NON_PAR_LABEL + "\", 1 )" ) );
		sb.append( getLine( "addPlotLabel( attName, " + Config.requirePositiveDoubleVal( R_ATT_LABEL_SIZE )
				+ ", color, VERTICAL, LEFT_SIDE, " + PLOT_ROW + "[(plotCount%%4)+1], " + PLOT_COL
				+ "[(plotCount%%4)+1] )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Builds histogram representation of vector (v), & formats the title based on size, color, and orientation params.
	 *
	 * @return String - formatted R function: addHistogram(v, label)
	 * @throws Exception
	 */
	public static String buildAddHistogramFunction() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "addHistogram <- function( v, label )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "if ( !is.nan( v ) && !is.na( v ) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "hist( v, breaks=" + Config.requirePositiveInteger( R_NUM_HISTAGRAM_BREAKS )
				+ ", ylab=\"\", xlab=\"\", main=\"\" )" ) );
		sb.append( getLine( "title( label, line=0.8 )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Plots OTU column data vs metadata attribute values on X-Y axis.
	 *
	 * Dependent R functions:
	 * 		1. displayPval(pval, label, lineAdj)
	 * 		2. addPlotLabel(label, size, color, las, side, rowIndex, colIndex)
	 * 		3. getColor(pVals)
	 *
	 *
	 * @return String - formatted R function: addNumericPlot(myT, otuCol, metaCol, reportList, i )
	 * @throws Exception
	 */
	public static String buildAddNumericPlotFunction() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "addNumericPlot <- function( myT, otuCol, metaCol, reportList, plotCount, otuCount )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "attName = names(myT)[metaCol]" ) );
		sb.append( getLine( "adjPv = reportList[[4]][names(reportList[[4]])==attName]" ) );
		sb.append( getLine( "adjNonPv = reportList[[5]][names(reportList[[5]])==attName]" ) );
		sb.append( getLine( "color = getColor( c( adjPv[" + OTU_INDEX + "], adjNonPv[" + OTU_INDEX + "] ) )" ) );
		sb.append( getLine( "plot( myT[,metaCol], myT[,otuCol], pch=" + getPch( Config.requireString( R_PCH ) )
				+ ", col=\"" + Config.requireString( R_POINT_COLOR ) + "\", xlab=\"\", ylab=\"\" )" ) );
		sb.append( getLine( "displayPval( adjPv[" + OTU_INDEX + "], \"" + PAR_LABEL + "\", 2 )" ) );
		sb.append( getLine( "displayPval( adjNonPv[" + OTU_INDEX + "], \"" + NON_PAR_LABEL + "\", 1 )" ) );
		sb.append( getLine( "addPlotLabel( attName, " + Config.requirePositiveDoubleVal( R_ATT_LABEL_SIZE )
				+ ", color, VERTICAL, LEFT_SIDE, " + PLOT_ROW + "[(plotCount%%4)+1], " + PLOT_COL
				+ "[(plotCount%%4)+1] )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Adds label to PDF using the label, size, color, and orientation parameters.
	 *
	 * @return String - formatted R function: addPlotLabel( label, size, color, las, side, rowIndex, colIndex )
	 */
	public static String buildAddPlotLabelFunction()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "addPlotLabel <- function( label, size, color, las, side, rowIndex, colIndex )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine(
				"mtext( bquote(bold(.( label ))), outer=TRUE, cex=size, side=side, las=las, line=colIndex, adj=rowIndex, col=color )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * P value format  handled by  sprintf() R function.  If ends with "f", round the float value.
	 *
	 * Dependent R functions:
	 * 		1. getColor(pVals)
	 *
	 * @throws Exception
	 * @return String - formatted R function: displayPval(pval, label, lineAdj)
	 */
	public static String buildDisplayPvalFunction() throws Exception
	{
		final String pvalFormat = Config.requireString( R_PVAL_FORMAT );
		final StringBuffer sb = new StringBuffer();
		String val = "pval";
		if( pvalFormat.endsWith( "f" ) )
		{
			final String x = pvalFormat.substring( pvalFormat.indexOf( "." ) + 1, pvalFormat.length() - 1 );
			val = "round(pval, " + x + ")";
		}

		sb.append( getLine( "displayPval <- function( pval, label, lineAdj )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "color = getColor( c( pval ) )" ) );
		sb.append( getLine( "title( paste( label, sprintf(\"" + pvalFormat + "\", " + val
				+ ") ), adj=1, line=lineAdj, col.main=color )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Method returns r.highlightColor if any value exceeds r.pvalCutoff, otherwise returns r.baseColor
	 *
	 * @return String - formatted R function: getColor(v)
	 * @throws Exception
	 */
	public static String buildGetColorFunction() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine(
				"#return r.highlightColor if any of the values meet the r.pvalCutoff, otherwise return r.baseColor" ) );
		sb.append( getLine( "getColor <- function( v )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "for( i in 1:length(v) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "if( v[i] <= " + Config.requirePositiveDoubleVal( R_PVAL_CUTOFF ) + " ) return( \""
				+ Config.requireString( R_HIGHLIGHT_COLOR ) + "\" )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "return( \"" + Config.requireString( R_BASE_COLOR ) + "\" )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Gets groups of values from an OTU column.  Groups data based on levels of metadata attribute.
	 *
	 * @return String - formatted R function: getFactorGroups(myT, att, otuCol)
	 */
	public static String buildGetFactorGroupsFunction()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "getFactorGroups <- function( myT, att, otuCol )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "vals = list()" ) );
		sb.append( getLine( "options = levels( att )" ) );
		sb.append( getLine( "for( i in 1:length(options) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "vals[[i]] = myT[att==options[i], otuCol]" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "return( vals )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	public static String buildMethods() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( buildPdfReportMethod() );
		sb.append( buildPlotPairedHistogramsFunction() );
		sb.append( buildDisplayPvalFunction() );
		sb.append( buildGetColorFunction() );
		sb.append( buildAddPlotLabelFunction() );
		sb.append( buildAddHistogramFunction() );
		sb.append( buildAddBoxPlotFunction() );
		sb.append( buildAddNumericPlotFunction() );
		sb.append( buildGetFactorGroupsFunction() );
		sb.append( rFunctionGetLas() );
		sb.append( rFunctionGetLabels() );
		return sb.toString();
	}

	/**
	 * Build the R Script code to output PDF Reports.
	 *
	 * Dependent R functions:
	 * 		1. plotPairedHistograms(parPvals, nonParVals)
	 *
	 * @return String lines of R code
	 * @throws Exception
	 */
	public static String buildPdfReportMethod() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "buildPdfReport <- function( myT, " + P_VALS_REPORT + ", " + BINARY_COLS + ", "
				+ NOMINAL_COLS + ", " + NUMERIC_COLS + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "plotPairedHistograms( " + P_VALS_REPORT + "[[2]], " + P_VALS_REPORT + "[[3]] )" ) );
		sb.append( getLine( PLOT_INDEX + " = 0" ) );
		sb.append( getLine( OTU_INDEX + " = 0" ) );

		//		final String addedAtts = getNumericCountAttributes();
		//		if( !addedAtts.isEmpty() )
		//		{
		//			sb.append(
		//					getLine( NUMERIC_COLS + " = " + NUMERIC_COLS + "[ !(" + NUMERIC_COLS + " %in% " + addedAtts + ") ]" ) );
		//		}

		sb.append( forEachOTU( filterRareOTUs( plotGraphics() ) ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Plot pairs of histogram distributions of parametric & non-parametric p-value values.
	 * The R function requires both input vectors to have the same number of p-values,
	 * found in the same order, and containing the same names.
	 *
	 * Dependent R functions:
	 * 		1. addHistogram(pvals)
	 * 		2. addPlotLabel(label, size, color, las, side, rowIndex, colIndex)
	 * 		3. getValuesByName(pValVector, attName)
	 *
	 * @throws Exception
	 * @return String - formatted R function: plotPairedHistograms(pValsPar, pValsNonPar)
	 */
	public static String buildPlotPairedHistogramsFunction() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "# Plot pairs of parametric & non-parametric p-value value histograms." ) );
		sb.append( getLine( "# Parameters must both be named double vectors of the same length" ) );
		sb.append( getLine( "# Dependent R functions: addHistogram( pvals ) & addPlotLabel( label, ... )" ) );

		sb.append( getLine( "plotPairedHistograms <- function( pValsPar, pValsNonPar )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "attNames = unique(names(pValsPar))" ) );
		sb.append( getLine( "for( i in 1:length(attNames) )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "addHistogram( getValuesByName( pValsPar, attNames[i] ), \"Parametric\" )" ) );
		sb.append( getLine( "addHistogram( getValuesByName( pValsNonPar, attNames[i] ), \"Non-Parametric\" )" ) );
		sb.append( getLine( "addPlotLabel( attNames[i], " + Config.requirePositiveDoubleVal( R_ATT_LABEL_SIZE ) + ", \""
				+ Config.requireString( R_BASE_COLOR ) + "\", VERTICAL, LEFT_SIDE, " + PLOT_ROW + "[ 3 - (i %% 2) ], "
				+ PLOT_COL + "[1] )" ) );

		sb.append( getLine( "if( i %% 2 == 1  )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine(
				"addPlotLabel( \"P-Value Distributions\", " + Config.requirePositiveDoubleVal( R_HEADING_SIZE ) + ", \""
						+ Config.requireString( R_BASE_COLOR ) + "\", HORIZONTAL, TOP_SIDE, CENTER_ADJ, 0 )" ) );
		sb.append( getLine( "}" ) );

		sb.append( getLine( "}" ) );

		sb.append( getLine( "if( length(attNames) %% 2 != 0  )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "plot.new()" ) );
		sb.append( getLine( "plot.new()" ) );
		sb.append( getLine( "}" ) );

		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Validate configuration file properties used in this utility.
	 *
	 * @throws Exception
	 */
	public static void checkDependencies() throws Exception
	{
		Config.requireString( R_PVAL_FORMAT );
		Config.requireString( R_PCH );
		Config.requireString( R_HIGHLIGHT_COLOR );
		Config.requireString( R_BASE_COLOR );
		Config.requireString( R_POINT_COLOR );
		Config.requirePositiveInteger( R_PLOT_WIDTH );
		Config.requirePositiveInteger( R_NUM_HISTAGRAM_BREAKS );
		Config.requirePositiveDoubleVal( R_ATT_LABEL_SIZE );
		Config.requirePositiveDoubleVal( R_HEADING_SIZE );
		Config.requirePositiveDoubleVal( R_PVAL_CUTOFF );
	}

	public static String getNumericCountAttributes() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		if( Config.getBoolean( Config.REPORT_NUM_READS ) )
		{
			sb.append( "c( \"" + MetaUtil.NUM_READS + "\"" );
		}
		if( Config.getBoolean( Config.REPORT_NUM_HITS ) )
		{
			sb.append( ( sb.toString().isEmpty() ? "c(": ", " ) + "\"" + MetaUtil.NUM_HITS + "\"" );
		}
		sb.append( sb.toString().isEmpty() ? "": ")" );
		return sb.toString();
	}

	/**
	 * R pch values range from #1-#25 or may contain certain string values
	 *
	 * @return String value of configured parm
	 * @throws Exception
	 */
	public static String getPch( String pchProp ) throws Exception
	{
		if( !NumberUtils.isNumber( pchProp ) )
		{
			pchProp = "\"" + pchProp + "\"";
		}
		return pchProp;
	}

	/**
	 * Get las value. Returns VERTICAL if over LAS_MAX characters across.
	 *
	 * @return
	 * @throws Exception
	 */
	public static String rFunctionGetLabels() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "getLabels <- function( labels )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "if( getLas(labels) == VERTICAL )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "return( strtrim(labels, " + VERT_LABEL_MAX + ") )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "return( labels )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Get las value. Returns VERTICAL if over LAS_MAX characters across.
	 *
	 * @return
	 * @throws Exception
	 */
	public static String rFunctionGetLas() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "getLas <- function( labels )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "nchars = sum(nchar(labels)) + length(labels) - 1" ) );
		sb.append( getLine( "las = HORIZONTAL" ) );
		sb.append( getLine( "if( nchars > " + Config.requirePositiveInteger( R_PLOT_WIDTH ) + " ) las = VERTICAL" ) );
		sb.append( getLine( "return( las )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Builds binary box pot for each binary attribute.
	 *
	 * Dependent R functions:
	 * 		1. addBoxPlot(myT, otuCol, metaCol, parametricPval, nonParametricPval)
	 *
	 * @return String lines of R code
	 */
	private static String addBinaryBoxPlots() throws Exception
	{
		Log.out.info( "Any binary fields? " + !MetaUtil.getBinaryFields().isEmpty() );
		if( !MetaUtil.getBinaryFields().isEmpty() )
		{
			final String rCode = "addBoxPlot( myT, " + OTU_COL + ", " + META_COL + ", " + P_VALS_REPORT + ", "
					+ PLOT_INDEX + ", " + OTU_INDEX + ")";
			return addPlots( BINARY_COLS, rCode );
		}
		return "";
	}

	/**
	 * Builds nominal (category) box pots for each nominal attribute.
	 *
	 * Dependent R functions:
	 * 		1. addBoxPlot(myT, otuCol, metaCol, parametricPval, nonParametricPval)
	 *
	 * @return String lines of R code
	 */
	private static String addNominalBoxPlots() throws Exception
	{
		Log.out.info( "Any nominal fields? " + !MetaUtil.getNominalFields().isEmpty() );
		if( !MetaUtil.getNominalFields().isEmpty() )
		{
			final String rCode = "addBoxPlot( myT, " + OTU_COL + ", " + META_COL + ", " + P_VALS_REPORT + ", "
					+ PLOT_INDEX + ", " + OTU_INDEX + ")";
			return addPlots( NOMINAL_COLS, rCode );
		}
		return "";
	}

	/**
	 * Builds numeric X-Y correlation pots for each binary attribute.
	 *
	 * Dependent R functions:
	 * 		1. addNumericPlot(myT, otuCol, metaCol, parametricPval, nonParametricPval)
	 *
	 * @return String lines of R code
	 */
	private static String addNumericPlots() throws Exception
	{
		Log.out.info( "Any numeric fields? " + !MetaUtil.getNumericFields().isEmpty() );
		if( !MetaUtil.getNumericFields().isEmpty() )
		{
			final String rCode = "addNumericPlot( myT, " + OTU_COL + ", " + META_COL + ", " + P_VALS_REPORT + ", "
					+ PLOT_INDEX + ", " + OTU_INDEX + ")";
			return addPlots( NUMERIC_COLS, rCode );
		}
		return "";
	}

	/**
	 * Builds plots passed in rCode for each attribute column in cols.
	 * If top of new page, add OTU Name center top.
	 *
	 * Dependent R functions:
	 * 		1. addPlotLabel(label, size, color, las, side, rowIndex, colIndex)
	 *
	 * @param cols - String comma separated list of metadata columns
	 * @param rCode - String line of R code that calls the appropriate plot function
	 * @return String lines of R code
	 */
	private static String addPlots( final String cols, final String rCode ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "for( " + META_COL + " in " + cols + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( rCode ) );
		sb.append( getLine( "if( " + PLOT_INDEX + " %% 4 == 0 ) addPlotLabel( names(myT)[" + OTU_COL + "], "
				+ Config.requirePositiveDoubleVal( R_HEADING_SIZE ) + ", \"" + Config.requireString( R_BASE_COLOR )
				+ "\", HORIZONTAL, TOP_SIDE, CENTER_ADJ, 0 )" ) );
		sb.append( getLine( PLOT_INDEX + " = " + PLOT_INDEX + " + 1" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Called by each taxonomy level to build level specific PDF report
	 *
	 * Dependent R functions:
	 * 		1. plotPairedHistograms(parPvals, nonParVals)
	 *
	 * @return String lines of R code
	 */
	private static String plotGraphics() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( OTU_INDEX + " = " + OTU_INDEX + " + 1" ) );
		sb.append( addBinaryBoxPlots() );
		sb.append( addNominalBoxPlots() );
		sb.append( addNumericPlots() );
		sb.append( addEmptyGraphToFillPage() );
		return sb.toString();
	}

	public static final String FUNCTION_BUILD_PDF_REPORT = "buildPdfReport";
	private static final String META_COL = "metaCol";
	private static final String NON_PAR_LABEL = "Non-Par Pval = ";
	private static final String OTU_INDEX = "otuCount";
	private static final String P_VALS_REPORT = "reportList";
	private static final String PAR_LABEL = "Par Pval = ";
	private static final String PLOT_INDEX = "plotCount";
	private static final String R_ATT_LABEL_SIZE = "r.attributeLabelSize";
	private static final String R_BASE_COLOR = "r.baseColor";
	private static final String R_HEADING_SIZE = "r.headingLabelSize";
	private static final String R_HIGHLIGHT_COLOR = "r.highlightColor";
	private static final String R_NUM_HISTAGRAM_BREAKS = "r.numHistogramBreaks";
	private static final String R_PCH = "r.pch";
	private static final String R_PLOT_WIDTH = "r.plotWidth";
	private static final String R_POINT_COLOR = "r.pointColor";
	private static final String R_PVAL_CUTOFF = "r.pvalCutoff";
	private static final String R_PVAL_FORMAT = "r.pValFormat";
	private static final int VERT_LABEL_MAX = 8;
}
