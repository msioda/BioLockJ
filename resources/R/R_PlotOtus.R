# Module script for: biolockj.module.report.r.R_PlotOtus

# Output box-plot illustrating OTU-nominal metadata field relationship
# Print adjusted P-values in pot header
addBoxPlot <- function( item, taxaVals, metaVals, barColors ) {
   metaVals = as.factor( metaVals )
   factors = split( taxaVals, metaVals )
   cexAxis = getCexAxis( levels( metaVals ) )
   labels = getBoxPlotLabels( levels( metaVals ) )
   orient = getLas( levels( metaVals ) )
   pch = getProperty("r.pch")
   pointCol = getProperty( "r.colorPoint", "black" )
   data = data.frame(taxaVals, metaVals)
   boxplot( factors, outline=FALSE, names=labels, las=orient, col=barColors, pch=pch, ylab=item, xlab="", cex.axis=cexAxis )
   stripchart( taxaVals ~ metaVals, method="jitter", data=data, vertical=TRUE, col=pointCol, pch=pch, add=TRUE )             
}

# Display plot heading with 2 pvalues + R^2 effect size
plotHeading <- function( parPval, nonParPval, r2, field ) {
   HEAD_1 = 0.2; HEAD_2 = 1.4; LEFT = 0; RIGHT = 1; TOP = 3;
   title1 = paste( "Adj.", getTestName( field ), "P-value:", displayCalc( parPval ) )
   title2 = paste( "Adj.", getTestName( field, FALSE ), "P-value:", displayCalc( nonParPval ) )
   mtext( title1, TOP, HEAD_1, col=displayCol( parPval ), cex=0.75, adj=LEFT )
   mtext( displayR2( r2 ), TOP, HEAD_1, cex=0.75, adj=RIGHT )
   mtext( title2, TOP, HEAD_2, col=displayCol( nonParPval ), cex=0.75, adj=LEFT )
}

# Scatter-plot for numeric fields 
addScatterPlot <- function( item, taxaVals, metaVals ) {
   plot( metaVals, taxaVals, pch=getProperty( "r.pch" ), col=getProperty( "r.colorPoint", "black" ), ylab=item, xlab="" )
}

# Return nominal group names truncated for display on X-axis of box plot
getBoxPlotLabels <- function( labels ) {
   if( getCexAxis( labels ) == getCexAxis( returnMin=TRUE ) ) {
      nchars = sum( nchar( labels ) ) + length( labels ) - 1
      maxSize = ( ( r.plotWidth * 2 ) + 2 )/length( labels )
      return( strtrim( labels, floor( maxSize ) ) )
   }
   return( labels )
}

# Return plot cex.axis parameter to set x-y axis text size based on total number of characters in all labels
getCexAxis <- function( labels=NULL, returnMax=FALSE, returnMin=FALSE) {
   cexAxisMax = 1
   cexAxisMin = 0.65
   if ( returnMax ) return( cexAxisMax )
   if ( returnMin ) return( cexAxisMin )
   
   nchars = sum(nchar(labels)) + length(labels) - 1
   if( nchars < r.plotWidth ) return( cexAxisMax )
   if( nchars < ( r.plotWidth +7 ) ) return( 0.9 )
   if( nchars < ( r.plotWidth + 15 ) ) return( 0.8 )
   if( nchars < ( r.plotWidth + 24 ) ) return( 0.7 )

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
	buildPlots()
	sigOnly <<- TRUE
	buildPlots()
}

# Key method used to build plots
buildPlots <- function() {

	logInfo( c( "sigOnly value", sigOnly ) )
	for( level in taxaLevels() ) {

      countTable = getCountTable( level )
      metaTable = getMetaData( level )
      if( is.null(countTable) || is.null(metaTable) ) { next }
      
      if( doDebug() ) sink( getLogFile( paste0( level, ifelse( sigOnly, "_significant", "" ) ) ) )
	  
      binaryCols = getBinaryFields()
      nominalCols = getNominalFields()
      numericCols = getNumericFields()
      logInfo( "binaryCols", binaryCols )
      logInfo( "nominalCols", nominalCols )
      logInfo( "numericCols", numericCols )
      
      parStats = getStatsTable( level, TRUE )
      nonParStats = getStatsTable( level, FALSE )
      r2Stats = getStatsTable( level )
      metaColColors = getColorsByCategory( metaTable )

	  outputFile = getPath( getOutputDir(), paste0( level, ifelse( sigOnly, "_significant", "" ), "_OTU_plots.pdf" ) )
      pdf( outputFile, paper="letter", width=7, height=10.5 )
      par(mfrow=c(3, 2), las=1, oma=c(1.2,1,4.5,0), mar=c(5, 4, 3, 2), cex=1)
      logInfo( "default par(mfrow)", par("mfrow") )
      par( mfrow = par("mfrow") ) 
      logInfo( "new par(mfrow)", par("mfrow") )
      pageNum = 0
      position = 0
      pvalCutoff = getProperty("r.pvalCutoff")

      # if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of countTable rows
      cutoffValue = getProperty("r.rareOtuThreshold", 1)
      if( cutoffValue < 1 ) cutoffValue = cutoffValue * nrow( countTable )

      for( item in names( countTable ) ) {
         if( sum( countTable[,item] > 0 ) >=  cutoffValue ) {
         
            taxaVals = countTable[,item]
            foundValidPvals = FALSE

            for( meta in getReportFields() ) {
            		metaVals = metaTable[,meta]
            		parPval = parStats[item, meta]
               	nonParPval = nonParStats[item, meta]
               	
               	if( sigOnly && min( parPval, nonParPval ) > pvalCutoff ) { next } 
               	
               	# Every new item starts a new page
               	if( !sigOnly && !foundValidPvals ) {
                     foundValidPvals = TRUE
                     par( mfrow = par("mfrow") ) 
                     position = 0
                  } 
            
               	if( meta %in% binaryCols || meta %in% nominalCols ) {
                  	logInfo( c( "Add Box-Plot [", item, "~", meta, "]" ) )
                  	addBoxPlot( item, taxaVals, metaVals, metaColColors[[meta]] )
               	}
               	else {
                  	logInfo( c( "Add Scatter-Plot [", item, "~", meta, "]" ) )
                  	addScatterPlot( item, taxaVals, metaVals )
               	}

              	plotHeading( parPval, nonParPval, r2Stats[item, meta], meta )
               	mtext( meta, side=1, font=1, cex=1, line=2.5, col=displayCol( c(parPval, nonParPval) ) )
               	position = position + 1

			   	if( position == 1 ) {
			   		pageNum = pageNum + 1
            			addHeaderFooter( item, level, pageNum )
               	} else if( position > prod( par("mfrow") ) ) {
            			position = 1
            			pageNum = pageNum + 1
            			addHeaderFooter( item, level, pageNum )
               	}
            }
         }
      }
      dev.off()
      if( doDebug() ) sink()
   }
}

# Always use base color for significant only plot, otherwise use highlight colors for significant plots
displayCol <- function( vals ) {
	if( sigOnly ) return( getProperty("r.colorBase", "black") )
	return( getColor( vals ) )
}

# Add page title + footer with page number
addHeaderFooter <- function( item, level, pageNum ) {
	if( !sigOnly ) addPageTitle( item )
	addPageNumber( pageNum )
	addPageFooter( paste0( ifelse( sigOnly, "Significant ", "" ), "Taxa Plots [ ", str_to_title( level ), " ]" ) )
}

sigOnly = FALSE
r.plotWidth = 23