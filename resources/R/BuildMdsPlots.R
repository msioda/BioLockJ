# Module script for: biolockj.module.r.BuildMdsPlots

# Import vegan library for distance plot support
# Main function generates 3 MDS plots for each attribute at each level in report.taxonomyLevels
main <- function() { 
	importLibs( c( "vegan" ) )
	numAxis = getProperty("rMds.numAxis")
	mdsAtts = getProperty( "rMds.reportFields", c( getBinaryFields(), getNominalFields() )  )
	metaColColors = getColorsByCategory()
	for( otuLevel in getProperty("report.taxonomyLevels") ) {
		if( doDebug() ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildMdsPlots_", otuLevel, ".log") ) )
		# get input data
		inputFile = getPipelineFile( paste0(otuLevel, ".*_metaMerged.tsv") )
		if( doDebug() ) print( paste( "inputFile = list.files( tableDir, paste0(otuLevel, .*, _metaMerged.tsv), full.names=TRUE ):", inputFile ) )
		if( length( inputFile ) == 0 ) { next }
		otuTable = read.table( inputFile, check.names=FALSE, na.strings=getProperty("metadata.nullValue", "NA"), 
													 comment.char=getProperty("metadata.commentChar", ""), header=TRUE, sep="\t", row.names=1 )
		lastOtuCol = ncol(otuTable) - getProperty("internal.numMetaCols")
		#
		# calculate PCoA
		myMDS = capscale( otuTable[,1:lastOtuCol]~1, distance=getProperty("rMds.distance") )
		#
		# save PCoA table
		pcoaFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(otuLevel, "_pcoa") ), ".tsv" )
		if( doDebug() ) print( paste( "Saving PCoA table to ", pcoaFileName ) )
		write.table( cbind(id=row.names(myMDS$CA$u), as.data.frame(myMDS$CA$u)), file=pcoaFileName, col.names=TRUE, row.names=FALSE, sep="\t" )
		# save eigen values
		eigenFileName = paste0( getPath( file.path(getModuleDir(), "temp"), paste0(otuLevel, "_eigenValues") ), ".tsv" )
		if( doDebug() ) print( paste( "Saving PCoA table to ", pcoaFileName ) )
		write.table( data.frame(mds=names(myMDS$CA$eig), eig=myMDS$CA$eig), file=eigenFileName, col.names=FALSE, row.names=FALSE, sep="\t")
		#
		# Make plots
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
		percentVariance = as.numeric(eigenvals(myMDS)/sum( eigenvals(myMDS) ) ) * 100
		for (attName in mdsAtts){
			metaColVals = as.character(otuTable[,attName])
			par(mfrow = par("mfrow"))
			att = as.factor(metaColVals)
			colorKey = metaColColors[[attName]]
			if( doDebug() ) print( paste( "Using colors: ", paste(colorKey, collapse = ", "), "for", paste(names(colorKey), collapse = ", "), "respectively." ) )
			position = 1
			page = 1
			numAxis = min(c(numAxis, ncol(myMDS$CA$u)))
			for (x in 1:(numAxis-1)) {
				for (y in (x+1):numAxis) {
					if (position > prod(par("mfrow") ) ) {
						position = 1
						page = page + 1
					}
					plot( myMDS$CA$u[,x], myMDS$CA$u[,y], main=paste(y, "vs", x),
								xlab=getMdsLabel( x, percentVariance[x] ),
								ylab=getMdsLabel( y, percentVariance[y] ),
								cex=1.2, pch=getProperty("r.pch"), col=colorKey[metaColVals] )
					position = position + 1
					if ( position == 2 ){ 
						# put this plot at the upper right position
						# that puts the legend in a nice white space, and it makes axis 1 in line with itself in two plots (same for axis3)
						plotRelativeVariance(percentVariance, numAxis)
						legend(x="topright", title=attName,
									 legend = paste0(names(colorKey), " (n=", table(metaColVals)[names(colorKey)], ")"), 
									 col=colorKey, pch=getProperty("r.pch"), bty="n")
						# this is the best time to add the page title
						title(main="Multidimensional Scaling Plots", outer = TRUE, line=1.2, cex=1.5)
						titlePart2 = ifelse( page == 1, paste( "taxonomic level:", otuLevel), paste(attName, "page", page))
						title(main=titlePart2, outer = TRUE, line=0, cex=.5)
						position = position + 1
					}
				}
			}
		}
		plotPlainText(paste("Each", paste0(page, "-page"), "set is identical.\nOnly the color scheme/legend changes."))
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
							 xlab="axis", ylab="variance explained per axis", main="")
	labels = round(heights)
	near0 = which(labels < 1)
	labels[near0] = "<1"
	text(x=bp, y=heights, labels = paste(labels, "%"), pos=3, xpd=TRUE)
}
