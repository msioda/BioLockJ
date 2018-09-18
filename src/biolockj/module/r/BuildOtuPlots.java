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
import biolockj.util.r.RMetaUtil;

/**
 * This BioModule is used to build the R script used to generate OTU-metadata box-plots and scatter-plots for each
 * report field and taxonomy level.
 */
public class BuildOtuPlots extends R_Module implements ScriptModule
{
	/**
	 * The method returns R code for the function addBoxPlot(otuTable, otuCol, metaCol, parPval, nonParPval ).<br>
	 * The plot output compares OTU abundance with the metadata field factor levels (categories).
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>otuTable = A data.frame returned by read.table( otuFilePath ) for a taxonomy-level OTU table
	 * <li>otuCol = OTU column number in otuTable
	 * <li>metaCol = Metadata column number in otuTable
	 * <li>parPval = Parametric P-value
	 * <li>nonParPval = Non-Parametric P-value
	 * </ul>
	 * <p>
	 * Required {@link biolockj.Config} properties: {@value biolockj.util.r.PlotScript#R_PCH},
	 * {@value biolockj.util.r.PlotScript#R_POINT_COLOR}.
	 * <p>
	 * BioLockJ R functions called by this method:
	 * <ul>
	 * <li>{@value biolockj.util.r.PlotScript#FUNCTION_GET_CEX_AXIS}
	 * <li>{@value biolockj.util.r.PlotScript#FUNCTION_GET_COLOR}
	 * <li>{@value biolockj.util.r.BaseScript#FUNCTION_GET_FACTOR_GROUPS}
	 * <li>{@value biolockj.util.r.PlotScript#FUNCTION_GET_LABELS}
	 * <li>{@value biolockj.util.r.PlotScript#FUNCTION_GET_LAS}
	 * <li>{@value biolockj.util.r.PlotScript#FUNCTION_GET_PLOT_TITLE}
	 * </ul>
	 * 
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} properties are missing or invalid
	 */
	public String buildAddBoxPlotFunction() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append(
				getLine( FUNCTION_ADD_BOX_PLOT + " <- function( otuTable, otuCol, metaCol, parPval, nonParPval )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "att = as.factor( otuTable[ ,metaCol] )" ) );
		sb.append( getLine( "attName = names(otuTable)[metaCol]", true ) );
		sb.append( getLine( "otuName = colnames(otuTable)[otuCol]", true ) );
		sb.append(
				getLine( "factors = " + BaseScript.FUNCTION_GET_FACTOR_GROUPS + "( otuTable, att, otuCol )", true ) );
		sb.append( getLine( "color = " + PlotScript.FUNCTION_GET_COLOR + "( c(parPval, nonParPval) )", true ) );
		sb.append( getLine( "barColors = " + PlotScript.FUNCTION_GET_COLORS + "( length(factors) )" ) );
		sb.append( getLine( "title = " + PlotScript.FUNCTION_GET_PLOT_TITLE + "( paste(\"" + PARAMETRIC_TITLE
				+ "\", parPval), paste(\"" + NON_PARAMETRIC_TITLE + "\", nonParPval) )", true ) );
		sb.append( getLine( "cexAxis = " + PlotScript.FUNCTION_GET_CEX_AXIS + "( levels(att) )", true ) );
		sb.append( getLine( "labels = " + PlotScript.FUNCTION_GET_LABELS + "( levels(att) )", true ) );
		sb.append( getLine( "orient = " + PlotScript.FUNCTION_GET_LAS + "( levels(att) )", true ) );
		sb.append( getLine( "boxplot( factors, outline=FALSE, names=labels, las=orient, col=barColors, pch="
				+ PlotScript.getPch()
				+ ", ylab=otuName, xlab=attName, main=title, col.lab=color, col.main=color, cex.main=1, cex.axis=cexAxis )" ) );
		sb.append( getLine(
				"stripchart( otuTable[ ,otuCol] ~ att, data=data.frame(otuTable[ ,otuCol], att), method=\"jitter\", vertical=TRUE, pch="
						+ PlotScript.getPch() + ", col=\"" + Config.requireString( PlotScript.R_POINT_COLOR )
						+ "\", add=TRUE )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * The method returns R code for the function addBoxPlot(otuTable, otuCol, metaCol, parPval, nonParPval).<br>
	 * The plot output compares OTU abundance with the metadata field factor levels (categories).
	 * <p>
	 * R Function parameters:
	 * <ul>
	 * <li>otuTable = A data.frame returned by read.table( otuFilePath ) for a taxonomy-level OTU table
	 * <li>otuCol = OTU column number in otuTable
	 * <li>metaCol = Metadata column number in otuTable
	 * <li>parPval = Parametric P-value
	 * <li>nonParPval = Non-Parametric P-value
	 * </ul>
	 * <p>
	 * Required {@link biolockj.Config} properties: {@value biolockj.util.r.PlotScript#R_PCH},
	 * {@value biolockj.util.r.PlotScript#R_POINT_COLOR}.
	 * <p>
	 * BioLockJ R functions called by this method:
	 * <ul>
	 * <li>{@value biolockj.util.r.PlotScript#FUNCTION_GET_COLOR}
	 * <li>{@value biolockj.util.r.BaseScript#FUNCTION_GET_FACTOR_GROUPS}
	 * <li>{@value biolockj.util.r.PlotScript#FUNCTION_GET_LABELS}
	 * <li>{@value biolockj.util.r.PlotScript#FUNCTION_GET_LAS}
	 * <li>{@value biolockj.util.r.PlotScript#FUNCTION_GET_PLOT_TITLE}
	 * </ul>
	 * 
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} properties are missing or invalid
	 */
	public String buildAddScatterPlotFunction() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine(
				FUNCTION_ADD_SCATTER_PLOT + " <- function( otuTable, otuCol, metaCol, parPval, nonParPval )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "attName = names(otuTable)[metaCol]", true ) );
		sb.append( getLine( "otuName = colnames(otuTable)[otuCol]", true ) );
		sb.append( getLine( "color = " + PlotScript.FUNCTION_GET_COLOR + "( c(parPval, nonParPval) )", true ) );
		sb.append( getLine( "pointColors = " + PlotScript.FUNCTION_GET_COLORS + "( length(otuTable[ ,metaCol]) )" ) );
		sb.append( getLine( "title = " + PlotScript.FUNCTION_GET_PLOT_TITLE + "( paste(\"" + PARAMETRIC_TITLE
				+ "\", parPval), paste(\"" + NON_PARAMETRIC_TITLE + "\", nonParPval) )", true ) );
		sb.append( getLine(
				"plot( otuTable[ ,metaCol], otuTable[ ,otuCol], pch=" + PlotScript.getPch() + ", col=pointColors, "
						+ "ylab=otuName, xlab=attName, main=title, col.lab=color, col.main=color, cex.main=1 )" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method builds the MAIN R script containing R functions to generate PDF reports.
	 * <p>
	 * Dependent BioLockJ Java methods called in this method:
	 * <ul>
	 * <li>{@link #buildAddBoxPlotFunction()}
	 * <li>{@link #buildAddScatterPlotFunction()}
	 * <li>{@link #rLinesExecuteProgram()}
	 * </ul>
	 */
	@Override
	public void executeTask() throws Exception
	{
		super.executeTask();
		PlotScript.buildScript( this );
		updatePrimaryScript( buildAddBoxPlotFunction() );
		updatePrimaryScript( buildAddScatterPlotFunction() );
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
	 * Method returns R code to loop through each column in cols and call the rFunctionSigniture. If top of new page,
	 * add OTU Name center top. Always increment the plot index.
	 * <p>
	 * Dependent PlotUtil R function: addPlotLabel(label, size, color, las, side, rowIndex, colIndex)
	 *
	 * @param cols - List of metadata column names
	 * @param rFunctionSigniture R code of plot function signature
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} properties are missing or invalid
	 */
	protected String addPlots( final String cols, final String rFunctionSigniture ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "for( " + META_COL + " in " + cols + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( PAR_PVAL + " = " + PlotScript.FUNCTION_DISPLAY_PVAL + "( " + PAR_STATS + "[ " + PAR_STATS
				+ "[colnames(" + PAR_STATS + ")[1]]==colnames(" + OTU_TABLE + ")[" + OTU_COL + "], colnames("
				+ PAR_STATS + ")==names(" + OTU_TABLE + ")[" + META_COL + "] ] )", true ) );
		sb.append( getLine( NP_PVAL + " = " + PlotScript.FUNCTION_DISPLAY_PVAL + "( " + NP_STATS + "[ " + NP_STATS
				+ "[colnames(" + NP_STATS + ")[1]]==colnames(" + OTU_TABLE + ")[" + OTU_COL + "], colnames(" + NP_STATS
				+ ")==names(" + OTU_TABLE + ")[" + META_COL + "] ] )", true ) );
		sb.append( getLine( rFunctionSigniture ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * This method is called once for each taxonomy level to build a taxonomy-level specific PDF report.<br>
	 * For each level, add binary, nominal, and numeric plots.
	 * <p>
	 * Dependent PlotUtil Java functions:
	 * <ul>
	 * <li>addBinaryBoxPlots()
	 * <li>addNominalBoxPlots()
	 * <li>addScatterPlots()
	 * </ul>
	 * 
	 * @return String lines of R code
	 * @throws Exception if processing errors occur
	 */
	protected String plotGraphics() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( addBinaryBoxPlots() );
		sb.append( addNominalBoxPlots() );
		sb.append( addScatterPlots() );
		return sb.toString();
	}

	/**
	 * Initialize the PDF report by generating the file name and par() function parameters to set the number of plots
	 * per page.
	 * 
	 * @return rCode
	 */
	protected String rLinesInitPdf()
	{
		int dim = 2;
		final int count = RMetaUtil.getBinaryFields().size() + RMetaUtil.getNominalFields().size()
				+ RMetaUtil.getNumericFields().size();
		if( count > 16 )
		{
			dim = 3;
		}

		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "pdf( paste0( " + BaseScript.FUNCTION_GET_PATH + "(" + R_OUTPUT_DIR + ", paste0("
				+ OTU_LEVEL + ", \"_OTU_plots" + PlotScript.PDF_EXT + "\" ) ) ) )" ) );
		sb.append( getLine( "par( mfrow=c(" + dim + ", " + dim + "), las=1 )" ) );
		return sb.toString();
	}

	/**
	 * The method returns R code for the function main(). For each taxonomy level - {@value #OTU_LEVEL}:<br>
	 * <ol>
	 * <li>Initialize a new PDF report
	 * <li>Read the parametric adjusted p-value spreadsheet
	 * <li>Read the non-parametric adjusted p-value spreadsheet
	 * <li>For each reportable metadata field, add a box-plot or scatterplot based on the data type
	 * </ol>
	 * <p>
	 * Dependent BioLockJ Java-R methods called in this method:
	 * <ul>
	 * <li>{@link biolockj.module.r.R_Module#forEachOtuLevel(String)}
	 * <li>{@link biolockj.module.r.R_Module#enableDebug(String, String)}
	 * <li>{@link #rLinesInitPdf()}
	 * <li>{@link biolockj.module.r.R_Module#rLinesReadOtuTable()}
	 * <li>{@link biolockj.module.r.R_Module#rLinesCreateAttColVectors()}
	 * <li>{@link biolockj.module.r.R_Module#rLinesReadStatsTable(String, String)}
	 * <li>{@link biolockj.module.r.R_Module#forEachOtu(String)}
	 * <li>{@link biolockj.module.r.R_Module#filterRareOtus(String)}
	 * <li>{@link #plotGraphics()}
	 * </ul>
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
		rCode.append( rLinesReadOtuTable() );
		rCode.append( rLinesCreateAttColVectors() );
		rCode.append( rLinesReadStatsTable( PAR_STATS, CalculateStats.P_VALS_PAR_ADJ + TSV_EXT ) );
		rCode.append( rLinesReadStatsTable( NP_STATS, CalculateStats.P_VALS_NP_ADJ + TSV_EXT ) );
		rCode.append( forEachOtu( filterRareOtus( plotGraphics() ) ) );
		rCode.append( getLine( "dev.off()" ) );

		sb.append( forEachOtuLevel( enableDebug( rCode.toString(), OTU_LEVEL ) ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * Method returns R code to add a box plot for each binary field.
	 * <p>
	 * Dependent R function: addBoxPlot(otuTable, otuCol, metaCol, parPval, nonParPval)
	 *
	 * @return R code
	 * @throws Exception if propagated by addPlots(...)
	 */
	private String addBinaryBoxPlots() throws Exception
	{
		if( !RMetaUtil.getBinaryFields().isEmpty() )
		{
			final String rCode = FUNCTION_ADD_BOX_PLOT + "( " + OTU_TABLE + ", " + OTU_COL + ", " + META_COL + ", "
					+ PAR_PVAL + ", " + NP_PVAL + ")";
			return addPlots( BINARY_COLS, rCode );
		}
		return "";
	}

	/**
	 * Method returns R code to add a box plot for each categorical field.
	 * <p>
	 * Dependent R function: addBoxPlot(otuTable, otuCol, metaCol, parPval, nonParPval)
	 *
	 * @return R code
	 * @throws Exception if propagated by addPlots(...)
	 */
	private String addNominalBoxPlots() throws Exception
	{
		if( !RMetaUtil.getNominalFields().isEmpty() )
		{
			final String rCode = FUNCTION_ADD_BOX_PLOT + "( " + OTU_TABLE + ", " + OTU_COL + ", " + META_COL + ", "
					+ PAR_PVAL + ", " + NP_PVAL + ")";
			return addPlots( NOMINAL_COLS, rCode );
		}
		return "";
	}

	/**
	 * Method returns R code to add a scatter plot for each numeric field.
	 * <p>
	 * Dependent R function: addScatterPlot(otuTable, otuCol, metaCol, parPval, nonParPval)
	 *
	 * @return R code
	 * @throws Exception if propagated by addPlots(...)
	 */
	private String addScatterPlots() throws Exception
	{
		if( !RMetaUtil.getNumericFields().isEmpty() )
		{
			final String rCode = FUNCTION_ADD_SCATTER_PLOT + "( " + OTU_TABLE + ", " + OTU_COL + ", " + META_COL + ", "
					+ PAR_PVAL + ", " + NP_PVAL + ")";
			return addPlots( NUMERIC_COLS, rCode );
		}
		return "";
	}

	/**
	 * Function name to add box plot to PDF: {@value #FUNCTION_ADD_BOX_PLOT}
	 */
	protected static final String FUNCTION_ADD_BOX_PLOT = "addBoxPlot";

	/**
	 * Function name to add scatter plot to PDF: {@value #FUNCTION_ADD_SCATTER_PLOT}
	 */
	protected static final String FUNCTION_ADD_SCATTER_PLOT = "addScatterPlot";

	/**
	 * Plot title prefix for non-parametric fields.
	 */
	protected static final String NON_PARAMETRIC_TITLE = "Nonparam. Pval:";

	/**
	 * Plot title prefix for parametric fields.
	 */
	protected static final String PARAMETRIC_TITLE = "Parametric Pval:";

	private static final String META_COL = "metaCol";
	private static final String NP_PVAL = "nonParPval";
	private static final String NP_STATS = "nonParStats";
	private static final String PAR_PVAL = "parPval";
	private static final String PAR_STATS = "parStats";
}
