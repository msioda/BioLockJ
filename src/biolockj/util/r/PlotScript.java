/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Oct 21, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util.r;

import java.io.File;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.Config;
import biolockj.module.r.R_Module;

/**
 * This utility generates the standard R code functions needed to display graphics in our PDF reports.
 */
public class PlotScript
{
	/**
	 * This is the top-level method called from a parent script that will print graphics to a PDF report.
	 * <p>
	 * Dependent BioLockJ Java-R methods called in this method:
	 * <ul>
	 * <li>{@link #rFunctionAddPlotLabel()}
	 * <li>{@link #rFunctionDisplayPval()}
	 * <li>{@link #rFunctionGetCexAxis()}
	 * <li>{@link #rFunctionGetColors()}
	 * <li>{@link #rFunctionGetColor()}
	 * <li>{@link #rFunctionGetLabels()}
	 * <li>{@link #rFunctionGetLas()}
	 * <li>{@link #rFunctionGetMaxAttLen()}
	 * <li>{@link #rFunctionPvalueTestName()}
	 * </ul>
	 *
	 * @param rModule R_Module
	 * @throws Exception if {@link biolockj.Config} property is missing or invalid
	 */
	protected PlotScript( final R_Module rModule ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( "source( \"" + BaseScript.getScriptPath( rModule ) + "\" )" ) );
		sb.append( R_Module.getLine( "HORIZONTAL = 1" ) );
		sb.append( R_Module.getLine( "VERTICAL = 3" ) );
		sb.append( rFunctionAddPlotLabel() );
		sb.append( rFunctionGetColors() );
		sb.append( rFunctionGetTitle() );
		sb.append( rFunctionDisplayPval() );
		sb.append( rFunctionGetCexAxis() );
		sb.append( rFunctionGetColor() );
		sb.append( rFunctionGetLabels() );
		sb.append( rFunctionGetLas() );
		sb.append( rFunctionGetMaxAttLen() );
		sb.append( rFunctionPvalueTestName() );
		R_Module.writeNewScript( getScriptPath( rModule ), sb.toString() );
	}

	/**
	 * Return the name of the non-parametric test used to calculate p-values for binary fields.
	 * 
	 * @return Test name
	 */
	protected String nonParametricBinaryTest()
	{
		return "Wilcox";
	}

	/**
	 * Return the name of the non-parametric test used to calculate p-values for nominal fields.
	 * 
	 * @return Test name
	 */
	protected String nonParametricNominalTest()
	{
		return "Kruskal";
	}

	/**
	 * Return the name of the non-parametric test used to calculate p-values for numeric fields.
	 * 
	 * @return Test name
	 */
	protected String nonParametricNumericTest()
	{
		return "Kendall";
	}

	/**
	 * Return the name of the parametric test used to calculate p-values for binary fields.
	 * 
	 * @return Test name
	 */
	protected String parametricBinaryTest()
	{
		return "T-Test";
	}

	/**
	 * Return the name of the parametric test used to calculate p-values for nominal fields.
	 * 
	 * @return Test name
	 */
	protected String parametricNominalTest()
	{
		return "ANOVA";
	}

	/**
	 * Return the name of the parametric test used to calculate p-values for numeric fields.
	 * 
	 * @return Test name
	 */
	protected String parametricNumericTest()
	{
		return "Pearson";
	}

	/**
	 * The method returns R code for the function {@value #FUNCTION_ADD_PLOT_LABEL}( label, size, color, las, side,
	 * rowIndex, colIndex ). This R function adds the label to the page using the mtext() function based on the
	 * orientation parameters. The PDF prints up to 4 plots per page, with the plot label oriented along the Y-axis of
	 * each plot.
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>label = Format this value in bold with R function bquote(bold(.( label )))
	 * <li>size = Set size of label via mtext(... cex=size, ...)
	 * <li>color = Set color of label via mtext(... col=color, ...)
	 * <li>las = Set text orientation via mtext(... las=las, ...) with options 1 (for HORIZONTAL) or 3 (for VERTICAL)
	 * <li>side = Set mtext(... side=side, ...) with options 2 (for LEFT_SIDE) or 3 (for TOP_SIDE)
	 * <li>rowIndex = Set mtext(... adj=rowIndex, ...) to adjust label orientation right to left
	 * <li>colIndex = Set mtext(... line=colIndex, ...) to adjust label orientation top to bottom
	 * </ul>
	 *
	 * @return R code
	 */
	protected String rFunctionAddPlotLabel()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine(
				FUNCTION_ADD_PLOT_LABEL + " <- function( label, size, color, las, side, rowIndex, colIndex ) {" ) );
		sb.append( R_Module.getLine(
				"mtext( bquote(bold(.( label ))), outer=TRUE, cex=size, side=side, las=las, line=colIndex, adj=rowIndex, col=color )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns the R code for {@value #FUNCTION_DISPLAY_PVAL} to format the pvalue.
	 * 
	 * @return Formatted pvalue
	 * @throws Exception if unable to format the pvalue
	 */
	protected String rFunctionDisplayPval() throws Exception
	{
		final String pvalFormat = Config.requireString( R_PVAL_FORMAT );
		final StringBuffer sb = new StringBuffer();
		String val = "pval";
		if( pvalFormat.endsWith( "f" ) )
		{
			val = "round(pval, " + pvalFormat.substring( pvalFormat.indexOf( "." ) + 1, pvalFormat.length() - 1 ) + ")";
		}

		sb.append( R_Module.getLine( FUNCTION_DISPLAY_PVAL + " <- function( pval ) {" ) );
		sb.append( R_Module.getLine( "return( paste( sprintf(\"" + pvalFormat + "\", " + val + ") ) )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Function returns a value to set cex.axis based on the number of characters in the x-axis labels
	 * 
	 * @return R code
	 * @throws Exception if Config errors occur
	 */
	protected String rFunctionGetCexAxis() throws Exception
	{
		final int width = Config.requirePositiveInteger( PlotScript.R_PLOT_WIDTH );
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_CEX_AXIS + " <- function( labels ) {" ) );
		sb.append( R_Module.getLine( "nchars = sum(nchar(labels)) + length(labels) - 1" ) );
		sb.append( R_Module.getLine( "if( nchars <=" + ( width - 1 ) + " ) {" ) );
		sb.append( R_Module.getLine( "return( 1 )" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "else if( nchars <=" + ( width + 6 ) + " ) {" ) );
		sb.append( R_Module.getLine( "return( 0.9 )" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "else if( nchars <=" + ( width + 13 ) + " ) {" ) );
		sb.append( R_Module.getLine( "return( 0.8 )" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "else if( nchars <=" + ( width + 20 ) + " ) {" ) );
		sb.append( R_Module.getLine( "return( 0.7 )" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "return( 0.65 )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * The method returns R code for the function {@value #FUNCTION_GET_COLOR}( v ). If any P-value in vector (v) is
	 * equal to or below the configured {@value #R_PVAL_CUTOFF} return {@value #R_HIGHLIGHT_COLOR}, otherwise return
	 * {@value #R_BASE_COLOR}.
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>v = Vector of P-values
	 * </ul>
	 * <p>
	 * Required {@link biolockj.Config} properties: {@value #R_PVAL_CUTOFF}, {@value #R_HIGHLIGHT_COLOR},
	 * {@value #R_BASE_COLOR}.
	 *
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} property is missing or invalid
	 */
	protected String rFunctionGetColor() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( "#return " + R_HIGHLIGHT_COLOR + " if any of the input values meet the "
				+ R_PVAL_CUTOFF + ", otherwise return " + R_BASE_COLOR ) );
		sb.append( R_Module.getLine( FUNCTION_GET_COLOR + " <- function( v ) {" ) );
		sb.append( R_Module.getLine( "for( i in 1:length(v) ) {" ) );
		sb.append( R_Module.getLine( "if( grepl(\"e\", v[i]) || !is.na(v[i]) && !is.nan(v[i]) && ( v[i] <= "
				+ Config.requirePositiveDoubleVal( R_PVAL_CUTOFF ) + " ) ) return( \""
				+ Config.requireString( R_HIGHLIGHT_COLOR ) + "\" )" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "return( \"" + Config.requireString( R_BASE_COLOR ) + "\" )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Get the color palette
	 * 
	 * @return R code
	 */
	protected String rFunctionGetColors()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_COLORS + " <- function( n ) {" ) );
		sb.append( R_Module.getLine( "return( get_palette(\"" + Config.getString( R_COLOR_PALETTE ) + "\", n) )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function {@value #FUNCTION_GET_LABELS}(labels).<br>
	 * Method returns the list of given labels as is if to be written horizontally. For vertical labels, return labels
	 * after trimming excessive characters for display purposes. The max length of vertical labels =
	 * {@value #VERT_LABEL_MAX}.
	 * <p>
	 * R Function parameter: labels = Vector of labels to print on plot axis
	 * <p>
	 * BioLockJ R function called by this method: getLas(labels)
	 *
	 * @return R code
	 * @throws Exception if Config error occurs
	 */
	protected String rFunctionGetLabels() throws Exception
	{
		final int width = Config.requirePositiveInteger( PlotScript.R_PLOT_WIDTH );
		final int max = width * 2 + 2;
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_LABELS + " <- function( labels ) {" ) );
		sb.append( R_Module.getLine( "if( " + FUNCTION_GET_CEX_AXIS + "(labels) == " + CEX_AXIS_MIN + " ) {" ) );
		sb.append( R_Module.getLine( "nchars = sum(nchar(labels)) + length(labels) - 1" ) );
		sb.append( R_Module.getLine( "maxSize = " + max + "/length(labels)" ) );
		sb.append( R_Module.getLine( "return( strtrim(labels, floor(maxSize) ) )" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "return( labels )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function {@value #FUNCTION_GET_LAS}(labels).<br>
	 * Method checks total number of characters for all labels to determine if they will fit written horizontally. If
	 * so, return global R script constant HORIZONTAL, otherwise, return global R script constant VERTICAL.
	 *
	 * @return R code
	 */
	protected String rFunctionGetLas()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_LAS + " <- function( labels ) {" ) );
		sb.append( R_Module.getLine( "nchars = sum(nchar(labels)) + length(labels) - 1" ) );
		sb.append( R_Module.getLine( "aveSize = sum(nchar(labels))/length(labels)" ) );
		sb.append( R_Module.getLine( "las = HORIZONTAL" ) );
		sb.append( R_Module.getLine( "if( (length(labels) > 5) && aveSize > 3 ) las = VERTICAL" ) );
		sb.append( R_Module.getLine( "return( las )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Return the length of the longest reportable metadata field name.
	 * 
	 * @return R code
	 */
	protected String rFunctionGetMaxAttLen()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_MAX_ATT_LEN + " <- function( v ) {" ) );
		sb.append( R_Module.getLine( "max = 0" ) );
		sb.append( R_Module.getLine( "for( i in 1:length(v) ) {" ) );
		sb.append( R_Module.getLine( "if( nchar(v[i]) > max ) max = nchar(v[i])" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "return( max )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function {@value #FUNCTION_GET_PLOT_TITLE}(line1, line2).<br>
	 * Build the title for the histogram based on the size of the field name and prefix.<br>
	 * If the title too long for 1 line, split it into 2 lines.
	 * 
	 * @return R Code
	 * @throws Exception if {@link biolockj.Config} property is missing or invalid
	 */
	protected String rFunctionGetTitle() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_GET_PLOT_TITLE + " <- function( line1, line2 ) {" ) );
		sb.append( R_Module.getLine(
				"if( (nchar(line1) + nchar(line2) ) > " + Config.requirePositiveInteger( R_PLOT_WIDTH ) + " ) {" ) );
		sb.append( R_Module.getLine( "return( paste0( line1, \"\\n\", line2 ) )" ) );
		sb.append( R_Module.getLine( "}" ) );
		sb.append( R_Module.getLine( "return( paste( line1, line2 ) )" ) );
		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function: {@value #FUNCTION_PVAL_TEST_NAME}( attName, isParametric )<br>
	 * R method returns the name of the statistical test used to generate the associated p-values.
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>attName = Metadata field name
	 * <li>isParametric = boolean
	 * </ul>
	 *
	 * @return R code
	 */
	protected String rFunctionPvalueTestName()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( R_Module.getLine( FUNCTION_PVAL_TEST_NAME + " <- function( attName, isParametric ) {" ) );
		if( !RMetaUtil.getBinaryFields().isEmpty() )
		{
			sb.append( R_Module.getLine( "if( attName %in% " + R_Module.BINARY_ATTS + " && isParametric ) return ( \""
					+ parametricBinaryTest() + "\" )" ) );
			sb.append( R_Module.getLine( "if( attName %in% " + R_Module.BINARY_ATTS + " && !isParametric ) return ( \""
					+ nonParametricBinaryTest() + "\" )" ) );
		}

		if( !RMetaUtil.getNominalFields().isEmpty() )
		{
			sb.append( R_Module.getLine( "if( attName %in% " + R_Module.NOMINAL_ATTS + " && isParametric ) return ( \""
					+ parametricNominalTest() + "\" )" ) );
			sb.append( R_Module.getLine( "if( attName %in% " + R_Module.NOMINAL_ATTS + " && !isParametric ) return ( \""
					+ nonParametricNominalTest() + "\" )" ) );
		}

		if( !RMetaUtil.getNumericFields().isEmpty() )
		{
			sb.append( R_Module.getLine( "if( attName %in% " + R_Module.NUMERIC_ATTS + " && isParametric ) return ( \""
					+ parametricNumericTest() + "\" )" ) );
			sb.append( R_Module.getLine( "if( attName %in% " + R_Module.NUMERIC_ATTS + " && !isParametric ) return ( \""
					+ nonParametricNumericTest() + "\" )" ) );
		}

		sb.append( R_Module.getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method constructs a new PlotScript for the given rModule.
	 *
	 * @param rModule R_Module
	 * @throws Exception if any errors occur
	 */
	public static void buildScript( final R_Module rModule ) throws Exception
	{
		new PlotScript( rModule );
	}

	/**
	 * Validate configuration file properties used to build the R report:
	 * <ul>
	 * <li>Require {@value #R_BASE_COLOR}
	 * <li>Require {@value #R_HIGHLIGHT_COLOR}
	 * <li>Require {@value #R_POINT_COLOR}
	 * <li>Require positive double {@value #R_PCH}
	 * <li>Require positive integer {@value #R_PLOT_WIDTH}
	 * <li>Require {@value #R_PVAL_CUTOFF}
	 * <li>Require {@value #R_PVAL_FORMAT}
	 * </ul>
	 * 
	 * @throws Exception if {@link biolockj.Config} property is missing or invalid
	 */
	public static void checkDependencies() throws Exception
	{
		Config.requireString( R_COLOR_PALETTE );
		Config.requireString( R_BASE_COLOR );
		Config.requireString( R_HIGHLIGHT_COLOR );
		Config.requireString( R_POINT_COLOR );
		Config.requireString( R_PCH );
		Config.requireString( R_PVAL_FORMAT );
		Config.requirePositiveInteger( PlotScript.R_PLOT_WIDTH );
		Config.requirePositiveDoubleVal( R_PVAL_CUTOFF );
	}

	/**
	 * Return {@link biolockj.Config}.{@value #R_PCH}.<br>
	 * If value is non-numeric, wrap the value in double quotes.
	 *
	 * @return PCH value as String
	 * @throws Exception if {@link biolockj.Config}.{@value #R_PCH} is undefined or invalid
	 */
	public static String getPch() throws Exception
	{
		String pch = Config.requireString( R_PCH );

		if( !NumberUtils.isNumber( pch ) )
		{
			pch = "\"" + pch + "\"";
		}
		return pch;
	}

	/**
	 * Get the PlotScript file path.
	 * 
	 * @param rModule R_Module
	 * @return File path of the rModule's PlotScript file
	 */
	public static String getScriptPath( final R_Module rModule )
	{
		return rModule.getScriptDir().getAbsolutePath() + File.separator + PlotScript.class.getSimpleName()
				+ R_Module.R_EXT;
	}

	/**
	 * R function used to add a label to the current plot {@value #FUNCTION_ADD_PLOT_LABEL}
	 */
	public static final String FUNCTION_ADD_PLOT_LABEL = "addPlotLabel";

	/**
	 * R function used to format the p-value for display {@value #FUNCTION_DISPLAY_PVAL}
	 */
	public static final String FUNCTION_DISPLAY_PVAL = "displayPval";

	/**
	 * R function used to return the cex.axis to adjust axis label size: {@value #FUNCTION_GET_CEX_AXIS}
	 */
	public static final String FUNCTION_GET_CEX_AXIS = "getCexAxis";

	/**
	 * R function returns the {@value #R_HIGHLIGHT_COLOR} for significant pvalues: {@value #FUNCTION_DISPLAY_PVAL}
	 */
	public static final String FUNCTION_GET_COLOR = "getColor";

	/**
	 * R function used to get plot colors {@value #FUNCTION_GET_COLORS}
	 */
	public static final String FUNCTION_GET_COLORS = "getColors";

	/**
	 * R function used to get a formatted list of labels for a list of reportable metadata fields
	 * {@value #FUNCTION_GET_LABELS}
	 */
	public static final String FUNCTION_GET_LABELS = "getLabels";

	/**
	 * R function used to return the las value for (vertical/horizontal) label orientation {@value #FUNCTION_GET_LAS}
	 */
	public static final String FUNCTION_GET_LAS = "getLas";

	/**
	 * R function used to find the length of the longest reportable metadata field: {@value #FUNCTION_GET_MAX_ATT_LEN}
	 */
	public static final String FUNCTION_GET_MAX_ATT_LEN = "getMaxAttLen";

	/**
	 * R function used to build the heading for the plot: {@value #FUNCTION_GET_PLOT_TITLE}
	 */
	public static final String FUNCTION_GET_PLOT_TITLE = "getPlotTitle";

	/**
	 * R function used to return the name of the pValue used for a given reportable metadata field:
	 * {@value #FUNCTION_PVAL_TEST_NAME}
	 */
	public static final String FUNCTION_PVAL_TEST_NAME = "pValueTestName";

	/**
	 * File extension of PDF report files generated by the R Script: {@value #PDF_EXT}
	 */
	public static final String PDF_EXT = ".pdf";

	/**
	 * {@link biolockj.Config} property: {@value #R_BASE_COLOR} defines the base plot label color
	 */
	public static final String R_BASE_COLOR = "r.colorBase";

	/**
	 * {@link biolockj.Config} property: {@value #R_COLOR_PALETTE} defines the color palette to use in plots
	 */
	public static final String R_COLOR_PALETTE = "r.colorPalette";

	/**
	 * {@link biolockj.Config} property: {@value #R_HIGHLIGHT_COLOR} defines the highlight color for significant labels
	 */
	public static final String R_HIGHLIGHT_COLOR = "r.colorHighlight";

	/**
	 * {@link biolockj.Config} property: {@value #R_PCH} defines the plot point base size and shape
	 */
	public static final String R_PCH = "r.pch";

	/**
	 * {@link biolockj.Config} Integer property: {@value #R_PLOT_WIDTH} defines the max number of characters on xy-axis
	 */
	public static final String R_PLOT_WIDTH = "r.plotWidth";

	/**
	 * {@link biolockj.Config} property: {@value #R_POINT_COLOR} defines the plot point color
	 */
	public static final String R_POINT_COLOR = "r.colorPoint";

	/**
	 * {@link biolockj.Config} Double property: {@value #R_PVAL_CUTOFF} defines the P-value cutoff for significance
	 */
	public static final String R_PVAL_CUTOFF = "r.pvalCutoff";

	/**
	 * {@link biolockj.Config} property: {@value #R_PVAL_FORMAT} defines the P-value display format used by R
	 * sprintf(...) function
	 */
	public static final String R_PVAL_FORMAT = "r.pValFormat";

	/**
	 * The maximum length of vertical labels: {@value #VERT_LABEL_MAX}
	 */
	public static final int VERT_LABEL_MAX = 5;

	/**
	 * R function {@value #FUNCTION_GET_LABELS} uses this value as the minimum cex.axis parameter value.
	 */
	protected static final double CEX_AXIS_MIN = 0.65;
}
