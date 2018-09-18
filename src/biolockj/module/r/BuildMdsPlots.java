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
import biolockj.util.MetaUtil;
import biolockj.util.r.BaseScript;
import biolockj.util.r.PlotScript;
import biolockj.util.r.RMetaUtil;

/**
 * This BioModule is used to build the R script used to generate MDS plots for each report field and each taxonomy level
 * configured.
 */
public class BuildMdsPlots extends R_Module implements ScriptModule
{
	/**
	 * Require {@link biolockj.Config}.{@value #R_MDS_NUM_AXIS} set to integer greater than 2
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getNumLabels();
		PlotScript.checkDependencies();
		if( Config.requirePositiveInteger( R_MDS_NUM_AXIS ) < 2 )
		{
			throw new Exception( "Config property [" + R_MDS_NUM_AXIS + "] must be > 2" );
		}
	}

	@Override
	public void executeTask() throws Exception
	{
		super.executeTask();
		PlotScript.buildScript( this );
		updatePrimaryScript( rFunctionGetOutliers() );
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

	/**
	 * Import the "coin" and "Kendall" libraries used in this script.
	 * 
	 * @return rCode
	 */
	@Override
	public List<String> getRequiredLibs()
	{
		return Arrays.asList( new String[] { "ggpubr", "vegan" } );
	}

	/**
	 * If MDS atts are not defined, return all metadata reportable attributes.
	 * 
	 * @return MDS fields as comma separated list
	 */
	protected String getMdsAtts()
	{
		if( RMetaUtil.getMdsFields().isEmpty() )
		{
			return ALL_ATTS;
		}

		return BaseScript.convertToString( RMetaUtil.getMdsFields() );
	}

	/**
	 * Get the number of points to label on MDS plots
	 * 
	 * @return Number of points to label
	 * @throws Exception if {@value #R_MDS_OUTLIERS} is undefined or invalid
	 */
	protected int getNumLabels() throws Exception
	{
		final String numOutliers = Config.requireString( R_MDS_OUTLIERS );
		if( numOutliers.contains( "." ) )
		{
			final Double val = Double.valueOf( numOutliers );
			if( val < 0.0 || val > 1.0 )
			{
				throw new Exception( "Config property [" + R_MDS_OUTLIERS
						+ "] accepts values between 0.0 - 1.0 if providing a percentage value" );
			}

			return new Long( Math.round( val * MetaUtil.getSampleIds().size() ) ).intValue();
		}
		else
		{
			final Integer val = Integer.valueOf( numOutliers );
			if( val < 0 || val > MetaUtil.getSampleIds().size() )
			{
				throw new Exception( "Config property [" + R_MDS_OUTLIERS + "] accepts values between 0 - "
						+ MetaUtil.getSampleIds().size() + " (the total #samples) if providing an integer value" );
			}

			return val;
		}
	}
	
	/**
	 * Build R function to create MDS labels
	 * @return R code
	 * @throws Exception if errors occur
	 */
	protected String rFunctionGetMdsLabels() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( R_FUNCTION_GET_OUTLIER_LABELS + " <- function( df, outLabels ) {" ) );
		sb.append( getLine( "labels = c()" ) );
		sb.append( getLine( "for( i in 1:length(row.names(df)) ) {" ) );
		sb.append( getLine( "if( row.names(df)[i] %in% outLabels ) {" ) );
		sb.append( getLine( "labels[i] = row.names(df)[i]" ) );
		sb.append( getLine( "} else {" ) );
		sb.append( getLine( "labels[i] = \"\"" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "return( labels )" ) );
		sb.append( getLine( "}" ) );

		return sb.toString();
	}

	/**
	 * Get the labels for the MDS plot outliers.<br>
	 * Calculated by adding distance between given points and all other points.
	 * 
	 * @return R code
	 * @throws Exception if errors occur
	 */
	protected String rFunctionGetOutliers() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( R_FUNCTION_GET_OUTLIERS + " <- function( df ) {" ) );
		sb.append( getLine( "rownames(df) = df[, which(colnames(df)==\"" + MetaUtil.getID() + "\")]" ) );
		sb.append( getLine( "vd = vegdist( df, diag=TRUE)" ) );
		sb.append( getLine( "rss = sort( rowSums(as.matrix( vd )), decreasing=TRUE )" ) );
		sb.append( getLine( "return( names(rss)[1:" + getNumLabels() + " ] )" ) );
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
		final int allAtts = RMetaUtil.getNominalFields().size() + RMetaUtil.getNumericFields().size()
				+ RMetaUtil.getBinaryFields().size();
		final int count = RMetaUtil.getMdsFields().isEmpty() ? allAtts: RMetaUtil.getMdsFields().size();
		if( count > 16 )
		{
			dim = 3;
		}

		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "pdf( paste0( " + BaseScript.FUNCTION_GET_PATH + "(" + R_OUTPUT_DIR + ", paste0("
				+ OTU_LEVEL + ", \"_MDS" + PlotScript.PDF_EXT + "\" ) ) ) )" ) );
		sb.append( getLine( "par( mfrow=c(" + dim + ", " + dim + "), las=1 )" ) );
		return sb.toString();
	}

	/**
	 * CODE IN PROGRESS Method builds an MDS PDF file with plots for each taxonomy level.
	 *
	 * @return R code
	 * @throws Exception if errors occur
	 */
	protected String rLinesPlotMds() throws Exception
	{
		final int numAxis = Config.requirePositiveInteger( R_MDS_NUM_AXIS );
		final StringBuffer sb = new StringBuffer();
		sb.append( rLinesInitPdf() );
		sb.append( rLinesReadOtuTable() );
		sb.append( getLine(
				MDS_COLS + " = " + BaseScript.FUNCTION_GET_COL_INDEXES + "( " + OTU_TABLE + ", " + MDS_ATTS + " )" ) );
		sb.append( getLine( "lastOtuCol = ncol(" + OTU_TABLE + ") - " + NUM_META_COLS ) );
		sb.append( getLine( "myMDS = capscale( " + OTU_TABLE + "[,2:lastOtuCol]~1,distance=\""
				+ Config.requireString( R_MDS_DISTANCE ) + "\" )" ) );
		sb.append( getLine( "pcoaFileName = paste0( " + BaseScript.FUNCTION_GET_PATH + "(" + R_TEMP_DIR + ", paste0("
				+ OTU_LEVEL + ", \"_pcoa\") ), \"" + TSV_EXT + "\" )" ) );
		sb.append( getLine( "write.table( myMDS$CA$u, file=pcoaFileName, sep=\"\\t\" )" ) );
		sb.append( getLine( "eigenFileName = paste0( " + BaseScript.FUNCTION_GET_PATH + "(" + R_TEMP_DIR + ", paste0("
				+ OTU_LEVEL + ", \"_eigenValues\") ), \"" + TSV_EXT + "\" )" ) );
		sb.append( getLine( "write.table( myMDS$CA$eig, file=eigenFileName, sep=\"\\t\")" ) );
		sb.append( getLine( "percentVariance = eigenvals(myMDS)/sum( eigenvals(myMDS) )" ) );
		sb.append( getLine( "outlierTable = data.frame()" ) );
		sb.append( getLine( "for( metaCol in " + MDS_COLS + " )" ) );
		sb.append( getLine( "{" ) );
		sb.append( getLine( "att = as.factor(" + OTU_TABLE + "[,metaCol])" ) );
		sb.append( getLine( "attName = names(" + OTU_TABLE + ")[metaCol]" ) );
		sb.append( getLine( "vals = levels( att )" ) );
		sb.append( getLine( "for (x in 1:" + numAxis + ") {" ) );
		sb.append( getLine( "for (y in 2:" + numAxis + ") {" ) );
		sb.append( getLine( "if(x == y) {" ) );
		sb.append( getLine( "break" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "title=paste( " + OTU_LEVEL + ", attName, \"MDS\" )" ) );
		sb.append( getLine( "xLab=paste(\"Axis:\", paste0( round(percentVariance[x]*100, 2), \"%\" ) )" ) );
		sb.append( getLine( "yLab=paste(\"Axis:\", paste0( round(percentVariance[y]*100, 2), \"%\" ) )" ) );
		// sb.append( getLine( "colors=ifelse(" + OTU_TABLE + "[,metaCol] == vals[2], rgb(0,0,1,0.75), rgb(1,0,0,0.75))
		// )" ) );
		sb.append( getLine( "colors=" + PlotScript.FUNCTION_GET_COLORS + "(length(" + OTU_TABLE + ")" ) );
		sb.append( getLine( "df = data.frame(myMDS$CA$u[,x], myMDS$CA$u[,y])" ) );

		sb.append( getLine( "outLabels = " + R_FUNCTION_GET_OUTLIERS + "(df)" ) );

		sb.append( getLine( "if( length(outlierTable) <= " + numAxis + ") { " ) );
		sb.append( getLine( "outlierTable[, length(outlierTable) + 1] = outLabels" ) );
		sb.append( getLine( "colnames(outlierTable)[length(outlierTable)] = paste(xLab, \"x\", yLab)" ) );
		sb.append( getLine( "}" ) );

		sb.append( getLine( "labels = " + R_FUNCTION_GET_OUTLIER_LABELS + "( df, outLabels )" ) );
		sb.append( getLine( "lim = 1.5 * max(df[,1], df[,2])" ) );
		sb.append( getLine(
				"myPlot = plot(df, xlim=c(-1 * lim, lim), ylim=c(-1 * lim, lim), xlab=xLab, ylab=yLab, main=title, cex=2.0, pch="
						+ PlotScript.getPch() + ", col=colors )" ) );
		sb.append( getLine( "with(df, text(df, labels=labels, pos = 4))" ) );
		sb.append( getLine( "" ) );
		sb.append( getLine( "" ) );
		sb.append( getLine( "" ) );
		sb.append( getLine( "" ) );

		// pcoa12=ordiplot(myPCoA,choices = c(1, 2), type = "none",cex.lab=1.5)
		// points(myPCoA, "sites",col=c(adjustcolor( "blue", alpha.f = 0.5),adjustcolor( "orange", alpha.f =
		// 0.5))[factor(otuTable$SEX)],pch=16,cex=2)
		// ordiellipse(myPCoA, otuTable$SEX, kind="se", conf=0.95, lwd=4, draw = "lines",
		// col="orange",show.groups=1,label=T,font=2,cex=3)
		// ordiellipse(myPCoA, otuTable$SEX, kind="se", conf=0.95, lwd=4, draw = "lines",
		// col="blue",show.groups=0,label=T,font=2,cex=3)
		// legend("topleft",c("Male", "Female"),col=c("orange","blue"),pch=16,bty = "n",cex=1.5)

		// TO-DO // 1. Add Legend // 2. Handle nominal data (currently only supports binary data
		// legend("topright", inset=c(-0.9,0),
		// c(paste0(attName,"=",vals[1]), paste0(attName,"=",vals[2])),
		// pch=c(16, 17), cex = 1.1,
		// col=c(rgb(1,0,0,0.75), rgb(0,0,1,0.75)))

		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * The method returns R code for the function main(). For each taxonomy level - {@value #OTU_LEVEL}:<br>
	 * and calls {@link #rLinesPlotMds()} for each report taxonomy level to plot MDS values for configured metadata
	 * attributes.
	 * <p>
	 * BioLockJ R methods and loop wrappers called by this method:
	 * <ul>
	 * <li>{@link biolockj.module.r.R_Module#forEachOtuLevel(String)}
	 * <li>{@link biolockj.module.r.R_Module#enableDebug(String, String)}
	 * <li>{@link #rLinesPlotMds()}
	 * </ul>
	 *
	 * @return R code
	 * @throws Exception if {@link biolockj.Config} properties are missing or invalid
	 */
	@Override
	protected String rMainMethod() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "source(\"" + PlotScript.getScriptPath( this ) + "\")" + RETURN ) );
		sb.append( getLine( METHOD_MAIN + " <- function() {" ) );
		sb.append( getLine( "stopifnot( " + FUNCTION_IMPORT_LIBS + "() )" ) );
		sb.append( getLine( MDS_ATTS + " = " + getMdsAtts() ) );
		sb.append( forEachOtuLevel( enableDebug( rLinesPlotMds(), OTU_LEVEL ) ) );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	/**
	 * R function name: {@value #R_FUNCTION_GET_OUTLIER_LABELS}
	 */
	protected static final String R_FUNCTION_GET_OUTLIER_LABELS = "getMdsLabels";

	/**
	 * R function name: {@value #R_FUNCTION_GET_OUTLIERS}
	 */
	protected static final String R_FUNCTION_GET_OUTLIERS = "getOutliers";

	/**
	 * {@link biolockj.Config} property: {@value #R_MDS_DISTANCE} defines the distance index to use in the capscale
	 * command.
	 */
	protected static final String R_MDS_DISTANCE = "rMds.distance";

	/**
	 * {@link biolockj.Config} Integer property: {@value #R_MDS_NUM_AXIS} defines the number of MDS axis to report
	 */
	protected static final String R_MDS_NUM_AXIS = "rMds.numAxis";

	/**
	 * {@link biolockj.Config} Numeric property: {@value #R_MDS_OUTLIERS} defines the number of outliers to label. If an
	 * integer, label that number, if a float value, label top percentage of outliers.
	 */
	protected static final String R_MDS_OUTLIERS = "rMds.outliers";

	private static final String MDS_ATTS = "mdsAtts";
	private static final String MDS_COLS = "mdsCols";
}
