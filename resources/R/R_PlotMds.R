# Module script for: biolockj.module.report.r.R_PlotMds

# Import vegan library for distance plot support
# Main function generates numAxis MDS plots for each field at each level in taxaLevels()
main <- function() { 
   importLibs( c( "vegan" ) )
   mdsFields = getProperty( "r_PlotMds.reportFields", c( getBinaryFields(), getNominalFields() )  )

   for( level in taxaLevels() ) {

      countTable = getCountTable( level )
      metaTable = getMetaData( level )
      if( is.null(countTable) || is.null(metaTable) ) { next }
      if( doDebug() ) sink( getLogFile( level ) )
      logInfo( "mdsFields", mdsFields )
      
      myMDS = capscale( countTable~1, distance=getProperty("r_PlotMds.distance") )
      numAxis = min( c( getProperty("r_PlotMds.numAxis"), ncol(myMDS$CA$u) ) )
      metaColColors = getColorsByCategory( metaTable )

      pcoaFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(level, "_pcoa") ), ".tsv" )
      write.table( cbind(id=row.names(myMDS$CA$u), as.data.frame(myMDS$CA$u)), file=pcoaFileName, col.names=TRUE, row.names=FALSE, sep="\t" )
      logInfo( "Save PCoA table", pcoaFileName )
      
      eigenFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(level, "_eigenValues") ), ".tsv" )
      write.table( data.frame(mds=names(myMDS$CA$eig), eig=myMDS$CA$eig), file=eigenFileName, col.names=FALSE, row.names=FALSE, sep="\t")
      logInfo( "Save Eigen value table", pcoaFileName )
      
      outputFile = paste0( getPath( getOutputDir(), paste0(level, "_MDS.pdf" ) ) )
      pdf( outputFile, paper="letter", width=7.5, height=10.5 )
      par( mfrow=c(3, 2), las=1, oma=c(1,0,2,1), mar=c(5, 4, 2, 2), cex=0.95 )
      perVariance = as.numeric(eigenvals(myMDS)/sum( eigenvals(myMDS) ) ) * 100
      pageNum = 0
      
      for( field in mdsFields ){
		par(mfrow = par("mfrow"), cex = par("cex"))
      position = 1

		metaColVals = as.character(metaTable[,field])
		colorKey = metaColColors[[field]]
		
		logInfo( "metaColVals", metaColVals )
		logInfo( c( "Using colors: ", paste(colorKey, "for", names(colorKey), collapse= ", ")) )
         
		for( x in 1: (numAxis-1) ) {
			for( y in (x+1): numAxis ) {

               plot( myMDS$CA$u[,x], myMDS$CA$u[,y], main=paste("Axes", x, "vs", y),
                        xlab=getMdsLabel( x, perVariance[x] ),
                        ylab=getMdsLabel( y, perVariance[y] ),
                        cex=1.2, pch=getProperty("r.pch", 20), col=colorKey[metaColVals] )
               
               if( position == 1 || position > prod( par("mfrow") ) ) {
					position = 1
                  	pageNum = pageNum + 1
                  	addHeaderFooter( field, level, pageNum )
               	}
               
               position = position + 1
               
               if( position == 2 ) { 
					plotRelativeVariance( field, metaColVals, perVariance, level, numAxis, colorKey )
					position = position + 1
               }
            }
         }
      }
      if( doDebug() ) sink()
      dev.off()
   }
}

# Add page title + footer with page number
addHeaderFooter <- function( field, level, pageNum ) {
	addPageTitle( field )
	addPageNumber( pageNum )
	addPageFooter( paste( "Multidimensional Scaling [", str_to_title( level ), "]" ) )
}

# Get variance plot label as percentage
getMdsLabel <- function( axisNum, variance ) { 
   return( paste0("Axis ", axisNum, " ( ", paste0( round( variance ), "% )" ) ) )
}

# This plot is always put in the upper right corner of the page
plotRelativeVariance <- function( field, metaColVals, perVariance, level, numAxis, colorKey ){
   numBars = min( c(length(perVariance), maxInLegend) )
   numBars = max(numBars, numAxis)
   heights = perVariance[1:numBars]
   bp = barplot(heights, col="dodgerblue1", ylim=c(0,100), names=1:numBars,
                      xlab="Axis", ylab="Variance" )
   labels = round(heights)
   near0 = which(labels < 1)
   labels[near0] = "<1"
   if( numBars <= maxInLegend ) labels = paste( labels, "%" )
   text(x=bp, y=heights, labels = labels, pos=3, xpd=TRUE)
   title( str_to_title( level ) )

	legendKey = colorKey
	legendLabels = paste0(names(legendKey), " (n=", table(metaColVals)[names(legendKey)], ")")
	legendKey = legendKey[ order(table(metaColVals)[names(colorKey)]) ]
	
	if (length(colorKey) > (maxInLegend + 1) ){
		legendKey = c( colorKey[ 1:maxInLegend], NA)
	    numDropped = length(colorKey) - length(legendKey) + 1
	    legendLabels = c(legendLabels[1:maxInLegend], paste("(", numDropped, "other labels )"))
	}
	legend( "topright", title=field, legend=legendLabels, cex=0.8, col=legendKey, pch=getProperty("r.pch", 20), bty="n" )
}

maxInLegend = 6