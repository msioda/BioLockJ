# Module script for: biolockj.module.report.r.R_PlotOtus

# Output box-plot illustrating OTU-nominal metadata field relationship
# Print adjusted P-values in pot header
addBoxPlot <- function( item, taxaVals, metaVals, barColors)
{
   metaVals = as.factor( metaVals )
   factors = split( taxaVals, metaVals ) 
   cexAxis = getCexAxis( levels(metaVals) )
   labels = getBoxPlotLabels( levels(metaVals) )
   orient = getLas( levels(metaVals) )

   boxplot( factors, outline=FALSE, names=labels, las=orient, col=barColors, pch=getProperty("r.pch"),
                ylab=item, xlab="", cex.axis=cexAxis )
   stripchart( taxaVals ~ metaVals, data=data.frame(taxaVals, metaVals), method="jitter",
                     vertical=TRUE, pch=getProperty("r.pch"), add=TRUE )
}

plotHeading <- function(parPval, nonParPval, r2, att) {
   HEAD_1 = 0.2; HEAD_2 = 1.4; LEFT = 0; RIGHT = 1; TOP = 3;

   title1_A = paste( "Adj.", getTestName(att, TRUE), "P-value: ", displayCalc(parPval) )
   title1_B = bquote( paste( R^2, ": ", .( displayCalc(parPval) ) ) )
   title2 = paste( "Adj.", getTestName(att, FALSE), "P-value: ", displayCalc(nonParPval) )

   mtext( title1_A, TOP, HEAD_1, col=getColor( parPval ), cex=0.75, adj=LEFT )
   mtext( title1_B, TOP, HEAD_1, cex=0.75, adj=RIGHT )
   mtext( title2, TOP, HEAD_2, col=getColor( nonParPval ), cex=0.75, adj=LEFT )
}

addScatterPlot <- function( item, taxaVals, metaVals )
{
   #cols = getColors( length(metaVals) )
   cols = getProperty("r.colorPoint", "black")
   plot( metaVals, taxaVals, pch=getProperty("r.pch"), col=cols, ylab=item, xlab="" )
}

# Return nominal group names truncated for display on X-axis of box plot
getBoxPlotLabels <- function( labels ) {
   if( getCexAxis(labels) == getCexAxis(returnMin=TRUE) ) {
      nchars = sum(nchar(labels)) + length(labels) - 1
      maxSize = ((getProperty("r.plotWidth")*2)+2)/length(labels)
      return( strtrim(labels, floor(maxSize) ) )
   }
   return( labels )
}

# Return plot cex.axis parameter to set x-y axis text size based on total number of characters in all labels
getCexAxis <- function( labels=NULL, returnMax=FALSE, returnMin=FALSE) {
   cexAxisMax = 1
   cexAxisMin = 0.65
   if (returnMax){
      return(cexAxisMax)
   }
   if (returnMin){
      return(cexAxisMin)
   }
   nchars = sum(nchar(labels)) + length(labels) - 1
   if( nchars < getProperty("r.plotWidth") ) return( cexAxisMax )
   if( nchars < (getProperty("r.plotWidth") +7 ) ) return( 0.9 )
   if( nchars < (getProperty("r.plotWidth") + 15 ) ) return( 0.8 )
   if( nchars < (getProperty("r.plotWidth") + 24 ) ) return( 0.7 )

   return( cexAxisMin )
}

# Return plot las parameter value to horizontal orientation by default.
# Return las vertical orientation only if more than 5 lables found with an average lable length > 3 chars.
getLas <- function( labels ) {
   HORIZONTAL = 1; VERTICAL = 3
   nchars = sum(nchar(labels)) + length(labels) - 1
   aveSize = sum(nchar(labels))/length(labels)
   if( (length(labels) > 5) && aveSize > 3 ) return( VERTICAL )
   return( HORIZONTAL )
}

# Called by BioLockJ_Lib.R runProgram() to execute this script
main <- function() {

   for( level in taxaLevels() ) {

      countTable = getCountTable( level )
      metaTable = getMetaData( level )
      if( is.null(countTable) || is.null(metaTable) ) { next }
      if( doDebug() ) sink( file.path( getTempDir(), paste0(moduleScriptName(), level, ".log") ) ) 

      binaryCols = getBinaryFields()
      nominalCols = getNominalFields()
      numericCols = getNumericFields()

      logInfo( "binaryCols", binaryCols )
      logInfo( "nominalCols", nominalCols )
      logInfo( "numericCols", numericCols )

      reportCols = getReportFields()

      parStats = getStatsTable( level, TRUE )
      nonParStats = getStatsTable( level, FALSE )
      r2Stats = getStatsTable( level )

      metaColColors = getColorsByCategory( metaTable )

      outputFile = getPath( getOutputDir(), paste0(level, "_OTU_plots.pdf") )

      if( length( reportCols ) < 5 ) {
         pdf( outputFile, width = 7, height = 7)
         par( mfrow=c(2, 2) )
      }else{
         pdf( outputFile, paper="letter", width=7, height=10.5 )
         par( mfrow=c(3, 2) )
      }
      par(las=1, oma=c(1.2,1,4.5,0), mar=c(5, 4, 3, 2), cex=1)
      multiPageSet = 0

      # if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of countTable rows
      cutoffValue = getProperty("r.rareOtuThreshold", 1)
      if( cutoffValue < 1 ) cutoffValue = cutoffValue * nrow(countTable)

      for( item in names(countTable) ) {
         multiPageSet = multiPageSet + 1
         if( sum( countTable[,item] > 0 ) >=  cutoffValue ) {
            par( mfrow = par("mfrow") ) # step to next pageNum, even if the last page is not full
            position = 1
            pageNum = 1
            taxaVals = countTable[,item]

            for( meta in reportCols ) {
               metaVals = metaTable[,meta]
               if( meta %in% binaryCols || meta %in% nominalCols ) {
                  logInfo( c( "Plot Box-Plot [", item, "~", meta, "]" ) )
                  addBoxPlot( item, taxaVals, metaVals, metaColColors[[meta]] )
               }
               else if( meta %in% numericCols ) {
                  logInfo( c( "Plot Scatter-Plot [", item, "~", meta, "]" ) )
                  addScatterPlot( item, taxaVals, metaVals )
               }

               plotHeading( parStats[ item, meta ], nonParStats[ item, meta ], r2Stats[ item, meta], meta )
               mtext( meta, side=1, font=1, cex=1, line=2.5 )
               position = position + 1

               if(position == 2) {
                  addPageTitle( item )
                  if ( length(reportCols) > prod( par("mfrow") ) ) {
                     addPageFooter( level, pageNum, multiPageSet )
                  }else{
                     addPageFooter( level, multiPageSet )
                  }
               }
               if( position > prod( par("mfrow") ) ) {
                  position = 1
                  pageNum = pageNum + 1
               }
            }
         }
      }
      dev.off()
      if( doDebug() ) sink()
   }
}
