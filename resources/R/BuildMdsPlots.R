# Module script for: biolockj.module.r.BuildMdsPlots

# Import vegan library for distance plot support
# Main function generates 3 MDS plots for each attribute at each level in report.taxonomyLevels
main <- function() { 
   mdsAtts = getProperty( "rMds.reportFields", allAtts )
   importLibs( c( "vegan" ) )
   for( otuLevel in getProperty("report.taxonomyLevels") ) {
      if( r.debug ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildMdsPlots_", otuLevel, ".log") ) )
      pdf( paste0( getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_MDS.pdf" ) ) ) )
      par( mfrow=c(2, 2), las=1 )
      inputFile = list.files( pipelineDir, paste0(otuLevel, ".*_metaMerged.tsv"), full.names=TRUE, recursive=TRUE )

      if( r.debug ) print( paste( "inputFile = list.files( tableDir, paste0(otuLevel, .*, _metaMerged.tsv), full.names=TRUE ):", inputFile ) )
      if( length( inputFile ) == 0 ) { next }
      otuTable = read.table( inputFile, check.names=FALSE, na.strings=getProperty("metadata.nullValue", "NA"), comment.char=getProperty("metadata.commentChar", ""), header=TRUE, sep="\t" )
      mdsCols = getColIndexes( otuTable, mdsAtts )
      lastOtuCol = ncol(otuTable) - getProperty("internal.numMetaCols")
      myMDS = capscale( otuTable[,2:lastOtuCol]~1,distance=getProperty("rMds.distance") )
      pcoaFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(otuLevel, "_pcoa") ), ".tsv" )
      write.table( myMDS$CA$u, file=pcoaFileName, sep="\t" )
      eigenFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(otuLevel, "_eigenValues") ), ".tsv" )
      write.table( myMDS$CA$eig, file=eigenFileName, sep="\t")
      percentVariance = eigenvals(myMDS)/sum( eigenvals(myMDS) )
      for( metaCol in mdsCols )
      {
         att = as.factor(otuTable[,metaCol])
         attName = names(otuTable)[metaCol]
         vals = levels( att )
         for (x in 1:getProperty("rMds.numAxis")) {
            for (y in 2:getProperty("rMds.numAxis")) {
               if(x == y) {
                  break
               }
               plot(myMDS$CA$u[,x], myMDS$CA$u[,y], xlab=paste("Axis:", paste0( round(percentVariance[x]*100, 2), "%" ) ), ylab=paste("Axis:", paste0( round(percentVariance[y]*100, 2), "%" ) ), main=paste( "MDS", attName, otuLevel), cex=1.2, pch=ifelse(otuTable[,metaCol] == vals[2], 17, 16),col=ifelse(otuTable[,metaCol] == vals[2], rgb(0,0,1,0.75), rgb(1,0,0,0.75)) )
            }
         }
      }
      if( r.debug ) sink()
   }
}


