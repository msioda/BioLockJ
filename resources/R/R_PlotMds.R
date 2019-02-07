# Module script for: biolockj.module.report.r.R_PlotMds

# Import vegan library for distance plot support
# Main function generates 3 MDS plots for each attribute at each level in taxaLevels()
main <- function() { 
   importLibs( c( "vegan" ) )
   numAxis = getProperty("r_PlotMds.numAxis")
   mdsFields = getProperty( "r_PlotMds.reportFields", c( getBinaryFields(), getNominalFields() )  )

   for( level in taxaLevels() ) {

      countTable = getCountTable( level )
      metaTable = getMetaData( level )
      if( is.null(countTable) || is.null(metaTable) ) { next }
      if( doDebug() ) sink( getLogFile( level ) )
      
      myMDS = capscale( countTable~1, distance=getProperty("r_PlotMds.distance") )
      metaColColors = getColorsByCategory( metaTable )

      pcoaFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(level, "_pcoa") ), ".tsv" )
      write.table( cbind(id=row.names(myMDS$CA$u), as.data.frame(myMDS$CA$u)), file=pcoaFileName, col.names=TRUE, row.names=FALSE, sep="\t" )
      logInfo( "Save PCoA table", pcoaFileName )
      
      eigenFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(level, "_eigenValues") ), ".tsv" )
      write.table( data.frame(mds=names(myMDS$CA$eig), eig=myMDS$CA$eig), file=eigenFileName, col.names=FALSE, row.names=FALSE, sep="\t")
      logInfo( "Save Eigen value table", pcoaFileName )
      
      # Make plots
      outputFile = paste0( getPath( getOutputDir(), paste0(level, "_MDS.pdf" ) ) )
      pdf( outputFile, paper="letter", width=7.5, height=10.5 )
      par(mfrow=c(3, 2), las=1, oma=c(1,0,2,1), mar=c(5, 4, 2, 2), cex=.95)
      percentVariance = as.numeric(eigenvals(myMDS)/sum( eigenvals(myMDS) ) ) * 100
      pageNum = 0
      
      for( field in mdsFields ){
        logInfo( "mdsFields", mdsFields )
        pageNum = pageNum + 1
         metaColVals = as.character(metaTable[,field])
         logInfo( "metaColVals", metaColVals )
         par(mfrow = par("mfrow"), cex = par("cex"))
         att = as.factor(metaColVals)
         colorKey = metaColColors[[field]]
         logInfo( c( "Using colors: ", paste(colorKey, "for", names(colorKey), collapse= ", ")) )
         position = 1
         pageNum = 1
         numAxis = min(c(numAxis, ncol(myMDS$CA$u)))
         for (x in 1:(numAxis-1)) {
            for (y in (x+1):numAxis) {
               if (position > prod(par("mfrow") ) ) {
                  position = 1
                  pageNum = pageNum + 1
               }
               pch=getProperty("r.pch", 20)
               plot( myMDS$CA$u[,x], myMDS$CA$u[,y], main=paste("Axes", x, "vs", y),
                        xlab=getMdsLabel( x, percentVariance[x] ),
                        ylab=getMdsLabel( y, percentVariance[y] ),
                        cex=1.2, pch=pch, col=colorKey[metaColVals] )
               position = position + 1
               if ( position == 2 ){ 
                  addPageTitle( field, line=1 )
                  addPageNumber( pageNum )
                  addPageFooter( "Multidimensional Scaling" )
                  # put this plot at the upper right position
                  # that puts the legend in a nice white space, and it makes axis 1 in line with itself in two plots (same for axis3)
                  plotRelativeVariance(percentVariance, numAxis)
                  position = position + 1
                  title( displayLevel( level ) )
                  # Add legend
                  legendKey = colorKey
                  legendLabels = paste0(names(legendKey), " (n=", table(metaColVals)[names(legendKey)], ")")
                  legendKey = legendKey[ order(table(metaColVals)[names(colorKey)]) ]
                  maxInLegend = 6
                  if (length(colorKey) > (maxInLegend + 1)){
                    legendKey = c( colorKey[ 1:maxInLegend], NA)
                    numDropped = length(colorKey) - length(legendKey) + 1
                    legendLabels = c(legendLabels[1:maxInLegend], paste("(", numDropped, "other labels )"))
                  }
                  legend(x="topright", title=field, legend = legendLabels, col=legendKey, pch=pch, bty="n")
               }
            }
         }
      }
      if( doDebug() ) sink()
      dev.off()
   }
}

getMdsLabel <- function( axisNum, variance ) { 
   return( paste0("Axis ", axisNum, " (", paste0( round( variance ), "%)" ) ) )
}

plotRelativeVariance <- function(percentVariance, numAxis){
   numBars = min(c(length(percentVariance), 6)) # arbitrary choice, don't show more than 6
   numBars = max(numBars, numAxis)
   heights = percentVariance[1:numBars]
   bp = barplot(heights, col="dodgerblue1", ylim=c(0,100), names=1:numBars,
                      xlab="Axis", ylab="Variance" )
   labels = round(heights)
   near0 = which(labels < 1)
   labels[near0] = "<1"
   if (numBars <= 6){ labels = paste(labels, "%") }
   text(x=bp, y=heights, labels = labels, pos=3, xpd=TRUE)
}
