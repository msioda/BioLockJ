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

import java.util.Arrays;
import java.util.List;
import biolockj.Config;
import biolockj.module.ScriptModule;
import biolockj.util.r.BaseScript;
import biolockj.util.r.PlotScript;

/**
 * This BioModule is used to build the R script used to generate p-value histograms for each report field and each
 * taxonomy level configured.
 */
public class BuildPvalHistograms extends R_Module implements ScriptModule
{

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		PlotScript.checkDependencies();
	}

	@Override
	public void executeTask() throws Exception
	{
		super.executeTask();
		PlotScript.buildScript( this );
		updatePrimaryScript( rMethodAddHistogram() );
		updatePrimaryScript( rLinesExecuteProgram() );
	}

	/**
	 * Add prerequisite module: {@link biolockj.module.r.CalculateStats}.
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		final List<Class<?>> preReqs = super.getPreRequisiteModules();
		preReqs.add( CalculateStats.class );
		return preReqs;
	}

	@Override
	public List<String> getRequiredLibs()
	{
		return Arrays.asList( new String[] { "ggpubr" } );
	}

	/**
	 * Initialize the PDF form by setting the name and par() function parameters.
	 * 
	 * @return rCode
	 */
	protected String rLinesInitPdf()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "pdf( " + BaseScript.FUNCTION_GET_PATH + "(" + R_OUTPUT_DIR + ", paste0(" + OTU_LEVEL
				+ ", \"_histograms" + PlotScript.PDF_EXT + "\" ) ) )" ) );
		sb.append( getLine( "par( mfrow=c(2, 2), las=1 )" ) );
		return sb.toString();
	}

	/**
	 * The method returns R code for the function main(). For each taxonomy level - {@value #OTU_LEVEL}:<br>
	 * <ol>
	 * <li>Initialize a new 2x2 PDF report
	 * <li>Read the parametric p-value spreadsheet
	 * <li>Read the non-parametric p-value spreadsheet
	 * <li>For each metadata field, add a parametric and non-parametric histogram
	 * </ol>
	 * <p>
	 * Dependent BioLockJ Java-R methods called in this method:
	 * <ul>
	 * <li>{@link biolockj.util.r.PlotScript#FUNCTION_GET_PLOT_TITLE}
	 * <li>{@link #rLinesInitPdf()}
	 * <li>{@link #rMethodAddHistogram()}
	 * <li>{@link biolockj.module.r.R_Module#rLinesReadStatsTable(String, String)}
	 * <li>{@link biolockj.util.r.PlotScript#FUNCTION_PVAL_TEST_NAME}
	 * <li>{@link biolockj.util.r.PlotScript#FUNCTION_GET_MAX_ATT_LEN}
	 * </ul>
	 * 
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} property is missing or invalid
	 */
	@Override
	protected String rMainMethod() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "source(\"" + PlotScript.getScriptPath( this ) + "\")" + RETURN ) );
		sb.append( getLine( METHOD_MAIN + " <- function() {" ) );
		sb.append( getLine( "stopifnot( " + FUNCTION_IMPORT_LIBS + "() )" ) );
		final StringBuffer rCode = new StringBuffer();
		rCode.append( rLinesInitPdf() );
		rCode.append( rLinesReadStatsTable( "parStats", CalculateStats.P_VALS_PAR + TSV_EXT ) );
		rCode.append( rLinesReadStatsTable( "nonParStats", CalculateStats.P_VALS_NP + TSV_EXT ) );
		rCode.append( getLine( "size = " + ( Config.requirePositiveInteger( PlotScript.R_PLOT_WIDTH ) + 2 ) + "/"
				+ PlotScript.FUNCTION_GET_MAX_ATT_LEN + "(colnames(parStats))", true ) );
		rCode.append( getLine( "for( i in 2:length(parStats) ) {" ) );
		rCode.append( getLine( "parAttName = colnames(parStats)[i]", true ) );
		rCode.append( getLine( "nonParAttName = colnames(nonParStats)[i]", true ) );
		rCode.append( getLine( "stopifnot( parAttName == nonParAttName )" ) );
		rCode.append( getLine( "xLabelPar = paste( " + PlotScript.FUNCTION_PVAL_TEST_NAME + "(parAttName, TRUE), \""
				+ X_LABEL + "\" )" ) );
		rCode.append( getLine( METHOD_ADD_HISTOGRAM + "( parStats[, i], " + PlotScript.FUNCTION_GET_PLOT_TITLE + "(\""
				+ PARAMETRIC_TITLE + "\", parAttName), xLabelPar, size )" ) );
		rCode.append( getLine( "xLabelNonPar = paste( " + PlotScript.FUNCTION_PVAL_TEST_NAME
				+ "(nonParAttName, FALSE), \"" + X_LABEL + "\" )" ) );
		rCode.append( getLine( METHOD_ADD_HISTOGRAM + "( nonParStats[, i], " + PlotScript.FUNCTION_GET_PLOT_TITLE
				+ "(\"" + NON_PARAMETRIC_TITLE + "\", nonParAttName), xLabelNonPar, size )" ) );
		rCode.append( getLine( "}" ) );
		rCode.append( getLine( "dev.off()" ) );

		sb.append( forEachOtuLevel( enableDebug( rCode.toString(), OTU_LEVEL ) ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method returns R code for the function addHistogram(v, title, xLabel, size).<br>
	 * The histogram shows the distribution of vector values (v).
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>v = Vector of data values used to build histogram
	 * <li>title = Histogram main value
	 * <li>xLabel = Histogram xlab value
	 * <li>size = Histogram cex.main value
	 * </ul>
	 *
	 * @return R code
	 */
	protected String rMethodAddHistogram()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( METHOD_ADD_HISTOGRAM + " <- function( v, title, xLabel, size ) {" ) );
		sb.append( getLine( "if ( !all(is.nan( v )) && !all(is.na( v )) ) {" ) );
		sb.append( getLine( "if ( size > 1.2 ) size = 1.2" ) );
		sb.append( getLine( "hist( v, breaks=20, xlab=xLabel, main=title, cex.main=size, col="
				+ PlotScript.FUNCTION_GET_COLORS + "(20) )" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * R function plots a histogram of OTU p-values for one reportable metadata field: {@value #METHOD_ADD_HISTOGRAM}
	 */
	protected static final String METHOD_ADD_HISTOGRAM = "addHistogram";

	/**
	 * Histogram title prefix for non-parametric fields: {@value #NON_PARAMETRIC_TITLE}
	 */
	protected static final String NON_PARAMETRIC_TITLE = "Non-Parametric";

	/**
	 * Histogram title prefix for parametric fields: {@value #PARAMETRIC_TITLE}
	 */
	protected static final String PARAMETRIC_TITLE = "Parametric";

	/**
	 * Histogram x-label suffix: {@value #X_LABEL}
	 */
	protected static final String X_LABEL = "P-Values";
}
