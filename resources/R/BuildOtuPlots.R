# Module script for: biolockj.module.r.BuildOtuPlots

# Box-plot nominal fields by categorical OTU abundance
addBoxPlot <- function( otuTable, otuCol, metaCol, parPval, nonParPval )
{
   att = as.factor( otuTable[ ,metaCol] )
   attName = names(otuTable)[metaCol]
   if( r.debug ) print( paste( "attName = names( otuTable )[metaCol]:", attName ) )
   otuName = colnames(otuTable)[otuCol]
   if( r.debug ) print( paste( "otuName = colnames(otuTable)[otuCol]:", otuName ) )
   factors = getFactorGroups( otuTable, att, otuCol )
   if( r.debug ) print( paste( c("factors = getFactorGroups( otuTable, att, otuCol ):", factors), collapse= " " ) )
   color = getColor( c(parPval, nonParPval) )
   if( r.debug ) print( paste( "color = getColor( c(parPval, nonParPval) ):", color ) )
   barColors = getColors( length(factors) )
   title = getPlotTitle( paste("Parametric Pval:", parPval), paste("Nonparam. Pval:", nonParPval) )
   if( r.debug ) print( paste( "title = getPlotTitle( paste(Parametric Pval:, parPval), paste(Nonparam. Pval:, nonParPval) ):", title ) )
   cexAxis = getCexAxis( levels(att) )
   if( r.debug ) print( paste( "cexAxis = getCexAxis( levels(att) ):", cexAxis ) )
   labels = getLabels( levels(att) )
   if( r.debug ) print( paste( c("labels = getLabels( levels(att) ):", labels), collapse= " " ) )
   orient = getLas( levels(att) )
   if( r.debug ) print( paste( "orient = getLas( levels(att) ):", orient ) )
   boxplot( factors, outline=FALSE, names=labels, las=orient, col=barColors, pch=getProperty("r.pch"), cex=0.2, ylab=otuName, xlab=attName, main=title, col.lab=color, col.main=color, cex.main=1, cex.axis=cexAxis )
   stripchart( otuTable[ ,otuCol] ~ att, data=data.frame(otuTable[ ,otuCol], att), method="jitter", vertical=TRUE, pch=20, ces=0.5, add=TRUE )
}

# Plot numeric field vs OTU abundance
addScatterPlot <- function( otuTable, otuCol, metaCol, parPval, nonParPval )
{
   attName = names(otuTable)[metaCol]
   if( r.debug ) print( paste( "attName = names( otuTable )[metaCol]:", attName ) )
   otuName = colnames(otuTable)[otuCol]
   if( r.debug ) print( paste( "otuName = colnames(otuTable)[otuCol]:", otuName ) )
   color = getColor( c(parPval, nonParPval) )
   if( r.debug ) print( paste( "color = getColor( c(parPval, nonParPval) ):", color ) )
   pointColors = getColors( length(otuTable[ ,metaCol]) )
   title = getPlotTitle( paste("Parametric Pval:", parPval), paste("Nonparam. Pval:", nonParPval) )
   if( r.debug ) print( paste( "title = getPlotTitle( paste(Parametric Pval:, parPval), paste(Nonparam. Pval:, nonParPval) ):", title ) )
   plot( otuTable[ ,metaCol], otuTable[ ,otuCol], pch=getProperty("r.pch"), col=pointColors, ylab=otuName, xlab=attName, main=title, col.lab=color, col.main=color, cex.main=1 )
}

main <- function() {

   for( otuLevel in getProperty("report.taxonomyLevels") ) {
      if( r.debug ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildOtuPlots_", otuLevel, ".log") ) )
      pdf( getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_OTU_plots.pdf") ) )
      par( mfrow=c(2, 2), las=1 )

      inputFile = getPipelineFile( paste0(otuLevel, ".*_metaMerged.tsv") )
      if( r.debug ) print( paste( "inputFile:", inputFile ) )
      if( length( inputFile ) == 0 ) { next }
      otuTable = read.table( inputFile, check.names=FALSE, na.strings=getProperty("metadata.nullValue", "NA"), comment.char=getProperty("metadata.commentChar", ""), header=TRUE, sep="\t" )
      rownames(otuTable) = otuTable[, 1]
      lastOtuCol = ncol(otuTable) - getProperty("internal.numMetaCols")
      
      binaryCols = getColIndexes( otuTable, binaryFields )
      if( r.debug ) print( paste( c("binaryCols:", binaryCols), collapse= " " ) )
      nominalCols = getColIndexes( otuTable, nominalFields )
      if( r.debug ) print( paste( c("nominalCols:", nominalCols), collapse= " " ) )
      numericCols = getColIndexes( otuTable, numericFields )
      if( r.debug ) print( paste( c("numericCols:", numericCols), collapse= " " ) )

      adjParInputFile = getPipelineFile( paste0(otuLevel, "_adjParPvals.tsv") )
      if( r.debug ) print( paste( "adjParInputFile:", adjParInputFile ) )
      parStats = read.table( adjParInputFile, check.names=FALSE, header=TRUE, sep="\t" )
      
      adjNonParInputFile = getPipelineFile( paste0(otuLevel, "_adjNonParPvals.tsv") )
      if( r.debug ) print( paste( "adjNonParInputFile:", adjNonParInputFile ) )
      nonParStats = read.table( adjNonParInputFile, check.names=FALSE, header=TRUE, sep="\t" )
      

      # if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of otuTable rows
      cutoffValue = getProperty("r.rareOtuThreshold", 1)
      if( cutoffValue < 1 ) {
         cutoffValue = cutoffValue * nrow(otuTable)
      }

      for( otuCol in 2:lastOtuCol ) {
         if( sum( otuTable[,otuCol] > 0 ) >=  cutoffValue ) {
            for( metaCol in binaryCols )
            {
               parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] )
               if( r.debug ) print( paste("parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] ):", parPval ) )
               nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] )
               if( r.debug ) print( paste("nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] ):", nonParPval ) )
               addBoxPlot( otuTable, otuCol, metaCol, parPval, nonParPval )
            }
            for( metaCol in nominalCols )
            {
               parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] )
               if( r.debug ) print( paste("parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] ):", parPval ) )
               nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] )
               if( r.debug ) print( paste("nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] ):", nonParPval ) )
               addBoxPlot( otuTable, otuCol, metaCol, parPval, nonParPval )
            }
            for( metaCol in numericCols )
            {
               parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] )
               if( r.debug ) print( paste("parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] ):", parPval ) )
               nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] )
               if( r.debug ) print( paste("nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] ):", nonParPval ) )
               addScatterPlot( otuTable, otuCol, metaCol, parPval, nonParPval )
            }
         }
      }
      dev.off()
      if( r.debug ) sink()
   }
}
