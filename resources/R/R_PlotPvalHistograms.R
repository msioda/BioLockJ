# Module script for: biolockj.module.report.r.R_PlotPvalHistograms

# save table output
# save favMetacols
# color by test type

# Add one histogram to the report
addHistogram <- function( v, xLabel, size, pvalCutoff=NULL, col ) {
   if ( !all(is.nan( v )) && !all(is.na( v )) ) {
      hist( v, breaks = seq(0, 1, 0.05), xlab=xLabel, main="", cex.main=size, col=col, xlim=c(0,1))
      if (!is.null(pvalCutoff)){
         abline(v=pvalCutoff, col=gray(.5), lty=2)
         mtext(at=pvalCutoff, side=3, text=pvalCutoff, col=gray(.5))
      }
   }else{
      plotPlainText("All values are NA or NaN.")
   }
}

# return the fraction of pvalues that are under a threshold, don't count NAs
calcSigFraction <- function(pvals, pvalCutoff){
   belowCutoff = sum(pvals < pvalCutoff, na.rm=TRUE)
   total = sum(!is.na(pvals))
   return( belowCutoff / total )
}

# Return main.cex parameter between 0.65 and 1.2 based on longest report field name
getCexMain<- function( labels=NULL ) {
   cexMax = 1.2
   cexMin = 0.65

   maxLabelSize = 1
   for( i in 1:length(labels) ) {
      if( nchar(labels[i]) > maxLabelSize ) maxLabelSize = nchar(labels[i])
   }

   size = 25/maxLabelSize
   if ( size > cexMax ) size = cexMax
   if ( size < cexMin ) size = cexMin

   return( size )
}


# This graphic can be printed with the histograms, 
# used for documentation, or just as a reference check when chaning the colors.
printColorCode <- function(){
   parColors = getTestName(isParametric = TRUE, returnColors = TRUE)
   nonParColors = getTestName(isParametric = FALSE, returnColors = TRUE)
   colors = c(parColors, nonParColors)
   plot(1,1, ylim=c(0,4), xlim=c(.5,2.5), type="n", axes=FALSE, xlab="", ylab="")
   title(main="Color Coding", line=0)
   x=rep(1:2, each=3)
   y=rep(3:1, 2)
   symbols(x=x, y=y, rectangles = matrix(ncol=2,byrow=TRUE,data=rep(c(1,1),6)), fg=colors, bg=colors, inches=FALSE, add=TRUE)
   text(x=x, y=y, labels=names(colors))
   text(x=1, y=3.5, labels="parametric", pos=3)
   text(x=2, y=3.5, labels="non-parametric", pos=3)
}


# Main function generates reports for each each taxaLevels()
# Each taxonomy report includes 2 histograms for each report field (1 parametric, 1 non-parametric) 
main <- function() {
   pvalCutoff = getProperty("r.pvalCutoff", 0.05)

   for( level in taxaLevels() ) {
      if( doDebug() ) sink( file.path( getTempDir(), paste0("debug_BuildPvalHistograms_", level, ".log") ) )

      # create empty pdf
     pdf( getPath( getOutputDir(), paste0(level, "_histograms.pdf") ) )
      par( mfrow=c(2, 2), las=1, mar=c(5,4,5,1)+.1 )


      parStats = getStatsTable( level, TRUE, FALSE )
      nonParStats = getStatsTable( level, FALSE, FALSE )
      size = getCexMain( colnames(parStats) )
      logInfo( "size = getCexMain( colnames(parStats) )", size )

      # create ranks table
      ranks = data.frame(AttributeName=names(parStats),
                                  Parametric.FractionPvalsUnderCutoff=rep(NA, ncol(parStats)),
                                  NonParametric.FractionPvalsUnderCutoff=rep(NA, ncol(nonParStats)))
      row.names(ranks) = ranks$AttributeName
      for( field in names(parStats) ) {
         ranks[field,"Parametric.FractionPvalsUnderCutoff"] = calcSigFraction(pvals=parStats[, field], pvalCutoff)
         ranks[field,"NonParametric.FractionPvalsUnderCutoff"] = calcSigFraction(pvals=nonParStats[, field], pvalCutoff)
      }
      # order attributes based on the fraction of tests with a p-value below <pvalCutoff>
      orderBy = apply(ranks[2:3],1, max, na.rm=TRUE)
      ranks = ranks[order(orderBy, decreasing=TRUE),]
      fname = getPath( getTempDir(), paste0(level, "_fractionBelow-", pvalCutoff, ".tsv") )
      write.table(ranks, file=fname, sep="\t", quote=FALSE, row.names=FALSE)

      # plot histograms in the same order they have in the ranks table
      for( field in ranks$AttributeName ) {

         if( ! field %in% getReportFields() ){ next }

         logInfo("processing attribute:", field )
         stopifnot( field %in% names(nonParStats) & field %in% names(parStats) )

         parTestName = getTestName(field, isParametric=TRUE)
         xLabelPar = paste( parTestName, "P-Values" )
         addHistogram( v=parStats[, field],
                              xLabel=xLabelPar, size=size, pvalCutoff=pvalCutoff, 
                              col=getTestName(field, isParametric=TRUE, returnColors=TRUE) )

         nonParTestName = getTestName(field, isParametric=FALSE)
         xLabelNonPar = paste( nonParTestName, "P-Values" )
         addHistogram( v=nonParStats[, field],
                              xLabel=xLabelNonPar, size=size, pvalCutoff=pvalCutoff, 
                              col=getTestName(field, isParametric=FALSE, returnColors=TRUE) )

         # shared title
         plotPointPerInch = (par("usr")[2] - par("usr")[1]) / par("pin")[1]
         shiftByPoints = par("mai")[2] * plotPointPerInch
         centerAt = par("usr")[1] - shiftByPoints
         mtext(text=field, side=3, line=2.5, at=centerAt, adj=.5, xpd=NA, font=par("font.main"), cex=par("cex.main"))

      }
      # at the end, add the color code reference
      printColorCode()
      plotPlainText(paste0("Histograms are ordered by \nthe fraction of tests below ", 
                                     pvalCutoff, "\nin the parametric or non-parametric test, \nwhichever was greater."))
      dev.off()

      if( doDebug() ) sink()
   }
}
