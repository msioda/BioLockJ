# Module script for: biolockj.module.r.BuildOtuPlots

# Output box-plot illustrating OTU-nominal metadata field relationship
# Print adjusted P-values in pot header
addBoxPlot <- function( taxa, taxaVals, metaVals, barColors) 
{
	metaVals = as.factor( metaVals )
	factors = split( taxaVals, metaVals ) 
	cexAxis = getCexAxis( levels(metaVals) )
	logInfo( "cexAxis = getCexAxis( levels(metaVals) )", cexAxis )
	labels = getBoxPlotLabels( levels(metaVals) )
	logInfo( "X axis labels = getBoxPlotLabels( levels(metaVals) )", labels )
	orient = getLas( levels(metaVals) )
	logInfo( "orient = getLas( levels(metaVals) )", orient )

	boxplot( factors, outline=FALSE, names=labels, las=orient, col=barColors, pch=getProperty("r.pch"), 
		ylab=taxa, xlab="", cex.axis=cexAxis )
	stripchart( taxaVals ~ metaVals, data=data.frame(taxaVals, metaVals), method="jitter", 
		vertical=TRUE, pch=20, add=TRUE )
}

# Add text at the top of the plot in the margin
addPvalueNote <- function(parPval, nonParPval, r2, attName=NULL){
	lineNumbers = c(0.2, 1.4, 2.6)
	names(lineNumbers) = c("low", "mid", "top")
	parTestName = getTestName(attName, TRUE)
	nonParTestName = getTestName(attName, FALSE)

	logInfo( "OTU Label CEX size: ", par("cex") )
	mtext(text=paste("R^2:", round(r2, 3)), line=lineNumbers["top"], 
				at=par("usr")[1], side=3, adj=0, cex=par("cex"))
	mtext(text=paste( parTestName, "Adjusted P-value:  ", displayPval(parPval) ), 
				line=lineNumbers["mid"], col=getColor(parPval), at=par("usr")[1], side=3, adj=0.5, cex=par("cex"))
	mtext(text=paste( nonParTestName, "Adjusted P-value:  ", displayPval(nonParPval) ), 
				line=lineNumbers["low"], col=getColor(nonParPval), at=par("usr")[1], side=3, adj=0.5, cex=par("cex"))
}


# Output scatter-plot illustrating OTU-numeric metadata field relationship
# Print adjusted P-values in pot header
addScatterPlot <- function( taxa, taxaVals, metaVals )
{
	pointColors = getColors( length(metaVals) )
	plot( metaVals, taxaVals, pch=getProperty("r.pch"), col=getProperty("r.colorPoint", "black"), 
				ylab=taxa, xlab="" )
}

# Return nominal group names truncated for display on X-axis of box plot
getBoxPlotLabels <- function( labels ) {
	if( getCexAxis(labels) == getCexAxis(returnMin=TRUE) ) {
		nchars = sum(nchar(labels)) + length(labels) - 1
		maxSize = ((getProperty("r.plotWidth")*2)+2)/length(labels)
		return( strtrim(labels, floor(maxSize) ) )
	}
	return( labels )
}

# Return plot cex.axis parameter to set x-y axis text size based on total number of characters in all labels
getCexAxis <- function( labels=NULL, returnMax=FALSE, returnMin=FALSE) {
	cexAxisMax = 1
	cexAxisMin = 0.65
	if (returnMax){
		return(cexAxisMax)
	}
	if (returnMin){
		return(cexAxisMin)
	}
	nchars = sum(nchar(labels)) + length(labels) - 1
	if( nchars < getProperty("r.plotWidth")) {
		return( cexAxisMax )
	}
	else if( nchars < (getProperty("r.plotWidth")+7) ) {
		return( 0.9 )
	}
	else if( nchars < (getProperty("r.plotWidth")+15) ) {
		return( 0.8 )
	}
	else if( nchars < (getProperty("r.plotWidth")+24) ) {
		return( 0.7 )
	}
	return( cexAxisMin )
}

# Return plot las parameter value to horizontal orientation by default.
# Return las vertical orientation only if more than 5 lables found with an average lable length > 3 chars.
getLas <- function( labels ) {
	HORIZONTAL = 1
	VERTICAL = 3
	nchars = sum(nchar(labels)) + length(labels) - 1
	aveSize = sum(nchar(labels))/length(labels)
	las = HORIZONTAL
	if( (length(labels) > 5) && aveSize > 3 ) las = VERTICAL
	return( las )
}

# Called by BioLockJ_Lib.R runProgram() to execute this script
main <- function() {
	
	for( level in taxaLevels() ) {
		if( doDebug() ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildOtuPlots_", level, ".log") ) )
		
		taxaTable = getTaxaTable( level )
		if( is.null( taxaTable ) ) { next }
		
		lastOtuCol = ncol(taxaTable) - numMetaCols()
		binaryCols = getColIndexes( taxaTable, getBinaryFields() )
		logInfo( "binaryCols", binaryCols )
		nominalCols = getColIndexes( taxaTable, getNominalFields() )
		logInfo( "nominalCols", nominalCols )
		numericCols = getColIndexes( taxaTable, getNumericFields() )
		logInfo( "numericCols", numericCols )
		reportCols = c( binaryCols, nominalCols, numericCols )
		logInfo( "reportCols", reportCols )
		
		parStats = getStatsTable( level, TRUE )
		nonParStats = getStatsTable( level, FALSE )
		r2Stats = getStatsTable( level )
		metaColColors = getColors( length( reportCols ) )
		
		plotsPerOTU = length( reportCols )
		outputFile = getPath( file.path(getModuleDir(), "output"), paste0(level, "_OTU_plots.pdf") )

		if (plotsPerOTU < 5 ) {
			pdf( outputFile, width = 7, height = 7)
			par( mfrow=c(2, 2) )
		}else{
			pdf( outputFile, paper="letter", width=7, height=10.5 )
			par( mfrow=c(3, 2) )
		}
		par(las=1, oma=c(0,1,5,0), mar=c(4, 4, 5, 2), cex=1)
		
		# if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of taxaTable rows
		cutoffValue = getProperty("r.rareOtuThreshold", 1)
		if( cutoffValue < 1 ) cutoffValue = cutoffValue * nrow(taxaTable)
		
		for( taxaCol in 1:lastOtuCol ) {
			if( sum( taxaTable[,taxaCol] > 0 ) >=  cutoffValue ) {
				par( mfrow = par("mfrow") ) # step to next page, even if the last page is not full
				position = 1
				page = 1
				taxa = colnames(taxaTable)[taxaCol]
				taxaVals = taxaTable[,taxaCol]
				logInfo( paste( "Taxa Name[col#] =", taxa, "[", taxaCol, "]" ), taxaVals ) 

				for( metaCol in reportCols )	{
					meta = colnames(taxaTable)[metaCol]
					metaVals = taxaTable[,metaCol]
					logInfo( paste( "Meta Name[col#] =", meta, "[", metaCol, "]" ), metaVals ) 
					
					if( metaCol %in% binaryCols || metaCol %in% nominalCols ) {
						logInfo( c( "Plot Box-Plot [", taxa, "~", meta, "]" ) )
						addBoxPlot( taxa, taxaVals, metaVals, "#90F6FF" )
						logInfo( "Basic plot complete!" )
					}
					else if( metaCol %in% numericCols ) {
						logInfo( c( "Plot Scatter-Plot [", taxa, "~", meta, "]" ) )
						addScatterPlot( taxa, taxaVals, metaVals )
						logInfo( "Basic plot complete!" )
					}

					addPvalueNote( nonParStats[ taxa, meta ], nonParStats[ taxa, meta ], r2Stats[ taxa, meta], meta )
					mtext(meta, side=1, font=par("font.main"), cex=par("cex.main"), line=2.5)
					
					# page title
					position = position + 1
					if(position == 2) { 
						#title(main=taxaCol, outer = TRUE, line=2)
						mtext(taxaCol, side=3, outer = TRUE, font=par("font.main"), cex=par("cex.main"), line=2)
						titlePart2 = ifelse( page == 1, paste( "taxonomic level:", level), paste("page", page))
						title(main=titlePart2, outer = TRUE, line=1)
					}
					if(position > prod( par("mfrow") ) ) {
						position = 1
						page = page + 1
					}
				}
			}
		}
		dev.off()
		if( doDebug() ) sink()
	}
}
