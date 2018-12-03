# Module script for: biolockj.module.r.BuildPvalHistograms

# Add one histogram to the report
addHistogram <- function( v, title, xLabel, size ) {
   if ( !all(is.nan( v )) && !all(is.na( v )) ) {
      hist( v, breaks=20, xlab=xLabel, main=title, cex.main=size )
   }
}

# Return main.cex parameter between 0.65 and 1.2 based on longest report field name
getCexMain<- function( labels=NULL ) {
   cexMax = 1.2
   cexMin = 0.65
   if (returnMax){
      return(cexMax)
   }
   if (returnMin){
      return(cexMin)
   }
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


# Main function generates reports for each each report.taxonomyLevels
# Each taxonomy report includes 2 histograms for each report field (1 parametric, 1 non-parametric) 
main <- function() {

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
      for( i in 2:length(parStats) ) {
         parAttName = colnames(parStats)[i]
         if( doDebug() ) print( paste("parAttName = colnames(parStats)[i]:", parAttName) )
         nonParAttName = colnames(nonParStats)[i]
         if( doDebug() ) print( paste("nonParAttName = colnames(nonParStats)[i]:", nonParAttName) )
         stopifnot( parAttName == nonParAttName )
         xLabelPar = paste( getTestName(parAttName, TRUE), "P-Values" )
         addHistogram( parStats[, i], getPlotTitle("Parametric", parAttName), xLabelPar, size )
         xLabelNonPar = paste( getTestName(nonParAttName, FALSE), "P-Values" )
         addHistogram( nonParStats[, i], getPlotTitle("Non-Parametric", nonParAttName), xLabelNonPar, size )
      }
      dev.off()
      if( doDebug() ) sink()
   }
}

