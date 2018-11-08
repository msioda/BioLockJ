# Module script for: biolockj.module.r.BuildPvalHistograms

# Add one histogram to the report
addHistogram <- function( v, title, xLabel, size ) {
   if ( !all(is.nan( v )) && !all(is.na( v )) ) {
      if ( size > 1.2 ) size = 1.2
      hist( v, breaks=20, xlab=xLabel, main=title, cex.main=size, col=getColors(20) )
   }
}

# Mian function generates reports for each each report.taxonomyLevels
# Each taxonomy report includes 1 histogram for each report attribute  
main <- function() {

   for( otuLevel in getProperty("report.taxonomyLevels") ) {
      if( r.debug ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildPvalHistograms_", otuLevel, ".log") ) )
      pdf( getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_histograms.pdf") ) )
      par( mfrow=c(2, 2), las=1 )
      parInputFile = getPipelineFile( paste0( otuLevel, "_parametricPvals.tsv" ) )
      if( r.debug ) print( paste( "parInputFile:", parInputFile ) )
      parStats = read.table( parInputFile, check.names=FALSE, header=TRUE, sep="\t" )
      nonParInputFile = getPipelineFile( paste0( otuLevel, "_nonParametricPvals.tsv" ) )
      if( r.debug ) print( paste( "nonParInputFile:", nonParInputFile ) )
      nonParStats = read.table( nonParInputFile, check.names=FALSE, header=TRUE, sep="\t" )
      size = 25/getMaxAttLen( colnames(parStats) )
      if( r.debug ) print( paste("size = 25/getMaxAttLen( colnames(parStats) ):", size ) )
      for( i in 2:length(parStats) ) {
         parAttName = colnames(parStats)[i]
         if( r.debug ) print( paste("parAttName = colnames(parStats)[i]:", parAttName) )
         nonParAttName = colnames(nonParStats)[i]
         if( r.debug ) print( paste("nonParAttName = colnames(nonParStats)[i]:", nonParAttName) )
         stopifnot( parAttName == nonParAttName )
         xLabelPar = paste( pValueTestName(parAttName, TRUE), "P-Values" )
         addHistogram( parStats[, i], getPlotTitle("Parametric", parAttName), xLabelPar, size )
         xLabelNonPar = paste( pValueTestName(nonParAttName, FALSE), "P-Values" )
         addHistogram( nonParStats[, i], getPlotTitle("Non-Parametric", nonParAttName), xLabelNonPar, size )
      }
      dev.off()
      if( r.debug ) sink()
   }
}
