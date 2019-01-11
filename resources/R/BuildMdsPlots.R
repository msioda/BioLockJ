# Module script for: biolockj.module.r.BuildMdsPlots

# Import vegan library for distance plot support
# Main function generates 3 MDS plots for each attribute at each level in report.taxonomyLevels
main <- function() { 
	importLibs( c( "vegan" ) )
	numAxis = getProperty("rMds.numAxis")
	mdsAtts = getProperty( "rMds.reportFields", c( getBinaryFields(), getNominalFields() )  )
	for( otuLevel in getProperty("report.taxonomyLevels") ) {
		if( doDebug() ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildMdsPlots_", otuLevel, ".log") ) )
		outputFile = paste0( getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_MDS.pdf" ) ) )
		if( doDebug() ) print( paste( "Saving plots to ", outputFile ) )
		if (numAxis < 4 ) {
			pdf( outputFile, width = 7, height = 7)
			par( mfrow=c(2, 2) )
		}else{
			pdf( outputFile, paper="letter", width=7, height=10.5 )
			par( mfrow=c(3, 2) )
		}
		par(las=1, oma=c(0,1,4,0), mar=c(5, 4, 2, 2))
		inputFile = getPipelineFile( paste0(otuLevel, ".*_metaMerged.tsv") )
		if( doDebug() ) print( paste( "inputFile = list.files( tableDir, paste0(otuLevel, .*, _metaMerged.tsv), full.names=TRUE ):", inputFile ) )
		if( length( inputFile ) == 0 ) { next }
		otuTable = read.table( inputFile, check.names=FALSE, na.strings=getProperty("metadata.nullValue", "NA"), 
													 comment.char=getProperty("metadata.commentChar", ""), header=TRUE, sep="\t", row.names=1 )
		mdsCols = getColIndexes( otuTable, mdsAtts )
		lastOtuCol = ncol(otuTable) - getProperty("internal.numMetaCols")
		myMDS = capscale( otuTable[,1:lastOtuCol]~1, distance=getProperty("rMds.distance") )
		pcoaFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(otuLevel, "_pcoa") ), ".tsv" )
		if( doDebug() ) print( paste( "Saving table to ", pcoaFileName ) )
		write.table( myMDS$CA$u, file=pcoaFileName, sep="\t" )
		eigenFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(otuLevel, "_eigenValues") ), ".tsv" )
		write.table( myMDS$CA$eig, file=eigenFileName, sep="\t")
		percentVariance = as.numeric(eigenvals(myMDS)/sum( eigenvals(myMDS) ) ) * 100
		colors = getColors(2)
		for( metaCol in mdsCols ){
			metaColVals = otuTable[,metaCol]
			par(mfrow = par("mfrow"))
			att = as.factor(metaColVals)
			attName = names(otuTable)[metaCol]
			vals = levels( att )
			colorKey = getMdsColors( otuTable, metaCol )
			position = 1
			page = 1
			for (x in 1:(numAxis-1)) {
				for (y in (x+1):numAxis) {
					plot( myMDS$CA$u[,x], myMDS$CA$u[,y], main=paste(y, "vs", x),
								xlab=getMdsLabel( x, percentVariance[x] ),
								ylab=getMdsLabel( y, percentVariance[y] ),
								cex=1.2, pch=getProperty("r.pch"), col=colorKey[otuTable[,metaCol]] )
					position = position + 1
					if ( position == 2 ){ 
						# put this plot at the upper right position
						# that puts the legend in a nice white space, and it makes axis 1 in line with itself in two plots (same for axis3)
						plotRelativeVariance(percentVariance)
						legend(x="topright", title=attName,
									 legend = paste0(names(colorKey), " (n=", table(metaColVals)[names(colorKey)], ")"), 
									 col=colorKey, pch=getProperty("r.pch"), bty="n")
						# this is the best time to add the page title
						title(main="Multidimensional Scaling Plots", outer = TRUE, line=1.2, cex=1.5)
						titlePart2 = ifelse( page == 1, paste( "taxonomic level:", otuLevel), paste("page", page))
						title(main=titlePart2, outer = TRUE, line=0, cex=.5)
						position = position + 1
					}
					if (position > prod(par("mfrow") ) ) {
						position = 1
						page = page + 1
					}
				}
			}
		}
		plotPlainText(paste("Each set of", page, ifelse(page==1, "page", "pages"), "is identical.\nOnly the color scheme/legend changes."))
		if( doDebug() ) sink()
		dev.off()
	}
}

getMdsLabel <- function( axisNum, variance ) { 
	return( paste0("Axis ", axisNum, " (", paste0( round( variance ), "%)" ) ) )
}

getMdsColors <- function( otuTable,  metaCol ) { 
	tableVals = as.factor( otuTable[,metaCol] )
	factors = levels( tableVals )
	colors = getColors( length( factors ) )
	names(colors) = levels( tableVals )
	return( colors )
}


plotRelativeVariance <- function(percentVariance){
	numBars = min(c(length(percentVariance), 6)) # arbitrary choice, don't show more than 6
	numBars = max(numBars, getProperty("rMds.numAxis"))
	heights = percentVariance[1:numBars]
	bp = barplot(heights, col="dodgerblue1", ylim=c(0,100), names=1:numBars,
							 xlab="axis", ylab="variance explained per axis", main="")
	text(x=bp, y=heights, labels = paste(round(heights), "%"), pos=3, xpd=TRUE)
}
