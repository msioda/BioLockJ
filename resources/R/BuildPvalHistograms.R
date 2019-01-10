# Module script for: biolockj.module.r.BuildPvalHistograms

# save table output
# save favMetacols
# color by test type

# Add one histogram to the report
addHistogram <- function( v, title, xLabel, size, pvalCutoff=NULL, col="dodgerblue2" ) {
   if ( !all(is.nan( v )) && !all(is.na( v )) ) {
      hist( v, breaks=20, xlab=xLabel, main=title, cex.main=size, col=col, xlim=c(0,1))
   }
	if (!is.null(pvalCutoff)){
		abline(v=pvalCutoff, col=gray(.5), lty=2)
		text(x=pvalCutoff, y=0, pos=1, labels = pvalCutoff, col=gray(.5))
	}
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


# Return name of statistical test used to generate P-Values
getTestName <- function( attName, isParametric ) {
   if( attName %in% getBinaryFields() && isParametric ) return ( "T-Test" )
   if( attName %in% getBinaryFields() && !isParametric ) return ( "Wilcox" )
   if( attName %in% getNominalFields() && isParametric ) return ( "ANOVA" )
   if( attName %in% getNominalFields() && !isParametric ) return ( "Kruskal" )
   if( attName %in% getNumericFields() && isParametric ) return ( "Pearson" )
   if( attName %in% getNumericFields() && !isParametric ) return ( "Kendall" )
}


# Return a color for each type of statistical test
getTestColor <- function(testName){
	colors = c("coral", "dodgerblue2", "darkgoldenrod1", "cornflowerblue", "tan1", "aquamarine3")
	names(colors) = c("T-Test", "Wilcox", "ANOVA", "Kruskal", "Pearson", "Kendall")
	return(colors[testName])
}


# Main function generates reports for each each report.taxonomyLevels
# Each taxonomy report includes 2 histograms for each report field (1 parametric, 1 non-parametric) 
main <- function() {
   pvalCutoff = getProperty("r.pvalCutoff", 0.05)
   favMetaCols = c()

   for( otuLevel in getProperty("report.taxonomyLevels") ) {
      if( doDebug() ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildPvalHistograms_", otuLevel, ".log") ) )
      pdf( getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_histograms.pdf") ) )
      par( mfrow=c(2, 2), las=1 )
      parInputFile = getPipelineFile( paste0( otuLevel, "_parametricPvals.tsv" ) )
      if( doDebug() ) print( paste( "parInputFile:", parInputFile ) )
      parStats = read.table( parInputFile, check.names=FALSE, header=TRUE, sep="\t" )
      nonParInputFile = getPipelineFile( paste0( otuLevel, "_nonParametricPvals.tsv" ) )
      if( doDebug() ) print( paste( "nonParInputFile:", nonParInputFile ) )
      nonParStats = read.table( nonParInputFile, check.names=FALSE, header=TRUE, sep="\t" )
      size = getCexMain( colnames(parStats) )
      if( doDebug() ) print( paste("size = getCexMain( colnames(parStats) ):", size ) )
      ranks = data.frame(parAttName=2:length(parStats), 
         ratioPvalsUnderCutoff.Par=2:length(parStats), 
         ratioPvalsUnderCutoff.NonPar=2:length(parStats))
      for( i in 2:length(parStats) ) {
         parAttName = colnames(parStats)[i]
         if( doDebug() ) print( paste("parAttName = colnames(parStats)[i]:", parAttName) )
         nonParAttName = colnames(nonParStats)[i]
         if( doDebug() ) print( paste("nonParAttName = colnames(nonParStats)[i]:", nonParAttName) )
         stopifnot( parAttName == nonParAttName )
         parTestName = getTestName(parAttName, TRUE)
         xLabelPar = paste( parTestName, "P-Values" )
         addHistogram( parStats[, i], getPlotTitle("Parametric", parAttName), xLabelPar, size, col=getTestColor(parTestName) )
         nonParTestName = getTestName(nonParAttName, FALSE)
         xLabelNonPar = paste( nonParTestName, "P-Values" )
         addHistogram( nonParStats[, i], getPlotTitle("Non-Parametric", parAttName), xLabelNonPar, size, col=getTestColor(nonParTestName) )
         ranks[i-1,"parAttName"] = parAttName
         ranks[i-1,"ratioPvalsUnderCutoff.Par"] = sum(parStats[, i] < pvalCutoff, na.rm=TRUE)/sum(!is.na(parStats[, i]))
         ranks[i-1,"ratioPvalsUnderCutoff.NonPar"] = sum(nonParStats[, i] < pvalCutoff, na.rm=TRUE)/sum(!is.na(nonParStats[, i]))
      }
      dev.off()
      ranks = ranks[order(ranks$ratioPvalsUnderCutoff.Par, decreasing=TRUE),]
      fname = getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_ratioBelow", pvalCutoff, ".tsv") )
      write.table(ranks, file=fname, sep="\t", quote=FALSE, row.names=FALSE)
      favMetaCols = unique(c(favMetaCols, ranks[1:33,"parAttName"]))
      if( doDebug() ) sink()
   }
   fnameFavCols = getPath( file.path(getModuleDir(), "output"), paste0("favoriteMetaCols.txt") )
   writeLines(paste(favMetaCols, collapse=", "), con=fnameFavCols)
}

