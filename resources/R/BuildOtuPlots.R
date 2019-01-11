# Module script for: biolockj.module.r.BuildOtuPlots

# Output box-plot illustrating OTU-nominal metadata field relationship
# Print adjusted P-values in pot header
addBoxPlot <- function( otuTable, otuCol, metaCol, parPval, nonParPval )
{
	att = as.factor( otuTable[ ,metaCol] )
	attName = names(otuTable)[metaCol]
	if( doDebug() ) print( paste( "attName = names( otuTable )[metaCol]:", attName ) )
	otuName = colnames(otuTable)[otuCol]
	if( doDebug() ) print( paste( "otuName = colnames(otuTable)[otuCol]:", otuName ) )
	factors = getFactorGroups( otuTable, att, otuCol )
	if( doDebug() ) print( paste( c("factors = getFactorGroups( otuTable, att, otuCol ):", factors), collapse= " " ) )
	color = getColor( c(parPval, nonParPval) )
	if( doDebug() ) print( paste( "color = getColor( c(parPval, nonParPval) ):", color ) )
	barColors = getColors( length(factors) )
	title = getPlotTitle( paste("Adj. Par. Pval:", parPval), paste("Adj. Npr. Pval:", nonParPval) )
	if( doDebug() ) print( paste( "title = getPlotTitle( paste(Adj. Par. Pval:, parPval), paste(Adj. Npr. Pval:, nonParPval) ):", title ) )
	cexAxis = getCexAxis( levels(att) )
	if( doDebug() ) print( paste( "cexAxis = getCexAxis( levels(att) ):", cexAxis ) )
	labels = getBoxPlotLabels( levels(att) )
	if( doDebug() ) print( paste( c("X axis labels = getBoxPlotLabels( levels(att) ):", labels), collapse= " " ) )
	orient = getLas( levels(att) )
	if( doDebug() ) print( paste( "orient = getLas( levels(att) ):", orient ) )
	boxplot( factors, outline=FALSE, names=labels, las=orient, col=barColors, pch=getProperty("r.pch"), cex=0.2, ylab=otuName, xlab=attName, main=title, col.lab=color, col.main=color, cex.main=1, cex.axis=cexAxis )
	stripchart( otuTable[ ,otuCol] ~ att, data=data.frame(otuTable[ ,otuCol], att), method="jitter", vertical=TRUE, pch=20, ces=0.5, add=TRUE )
}

# Output scatter-plot illustrating OTU-numeric metadata field relationship
# Print adjusted P-values in pot header
addScatterPlot <- function( otuTable, otuCol, metaCol, parPval, nonParPval )
{
	attName = names(otuTable)[metaCol]
	if( doDebug() ) print( paste( "attName = names( otuTable )[metaCol]:", attName ) )
	otuName = colnames(otuTable)[otuCol]
	if( doDebug() ) print( paste( "otuName = colnames(otuTable)[otuCol]:", otuName ) )
	color = getColor( c(parPval, nonParPval) )
	if( doDebug() ) print( paste( "color = getColor( c(parPval, nonParPval) ):", color ) )
	pointColors = getColors( length(otuTable[ ,metaCol]) )
	title = getPlotTitle( paste("Adj. Par. Pval:", parPval), paste("Adj. Npr. Pval:", nonParPval) )
	if( doDebug() ) print( paste( "title = getPlotTitle( paste(Adj. Par. Pval:, parPval), paste(Adj. Npr. Pval:, nonParPval) ):", title ) )
	plot( otuTable[ ,metaCol], otuTable[ ,otuCol], pch=getProperty("r.pch"), col=getProperty("r.colorPoint", "black"), ylab=otuName, xlab=attName, main=title, col.lab=color, col.main=color, cex.main=1 )
}

# Return nominal group names truncated for display on X-axis of box plot
getBoxPlotLabels <- function( labels ) {
	if( getCexAxis(labels) == getCexAxis(returnMin=T) ) {
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
	
	for( otuLevel in getProperty("report.taxonomyLevels") ) {
		if( doDebug() ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildOtuPlots_", otuLevel, ".log") ) )

		# get input
		inputFile = getPipelineFile( paste0(otuLevel, ".*_metaMerged.tsv") )
		if( doDebug() ) print( paste( "inputFile:", inputFile ) )
		if( length( inputFile ) == 0 ) { next }
		otuTable = read.table( inputFile, check.names=FALSE, na.strings=getProperty("metadata.nullValue", "NA"), 
													 comment.char=getProperty("metadata.commentChar", ""), header=TRUE, sep="\t", row.names=1 )
		lastOtuCol = ncol(otuTable) - getProperty("internal.numMetaCols")
		
		binaryCols = getColIndexes( otuTable, getBinaryFields() )
		if( doDebug() ) print( paste( c("binaryCols:", binaryCols), collapse= " " ) )
		nominalCols = getColIndexes( otuTable, getNominalFields() )
		if( doDebug() ) print( paste( c("nominalCols:", nominalCols), collapse= " " ) )
		numericCols = getColIndexes( otuTable, getNumericFields() )
		if( doDebug() ) print( paste( c("numericCols:", numericCols), collapse= " " ) )
		
		adjParInputFile = getPipelineFile( paste0(otuLevel, "_adjParPvals.tsv") )
		if( doDebug() ) print( paste( "adjParInputFile:", adjParInputFile ) )
		parStats = read.table( adjParInputFile, check.names=FALSE, header=TRUE, sep="\t" )
		
		adjNonParInputFile = getPipelineFile( paste0(otuLevel, "_adjNonParPvals.tsv") )
		if( doDebug() ) print( paste( "adjNonParInputFile:", adjNonParInputFile ) )
		nonParStats = read.table( adjNonParInputFile, check.names=FALSE, header=TRUE, sep="\t" )
		
		# create empty ouptut file
		plotsPerOTU = sum(c(length(binaryCols), length(nominalCols), length(numericCols)))
		outputFile = getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_OTU_plots.pdf") )
		if( doDebug() ) print( paste( "CSaving plots to", outputFile ) )
		if (plotsPerOTU < 5 ) {
			pdf( outputFile, width = 7, height = 7)
			par( mfrow=c(2, 2) )
		}else{
			pdf( outputFile, paper="letter", width=7, height=10.5 )
			par( mfrow=c(3, 2) )
		}
		par(las=1, oma=c(0,1,4,0), mar=c(5, 4, 2, 2))
		
		# if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of otuTable rows
		cutoffValue = getProperty("r.rareOtuThreshold", 1)
		if( cutoffValue < 1 ) {
			cutoffValue = cutoffValue * nrow(otuTable)
		}
		
		if (testing){lastOtuCol = 12} ####!!!! just for testing !!!!
		for( otuCol in 1:lastOtuCol ) {
			if( sum( otuTable[,otuCol] > 0 ) >=  cutoffValue ) {
				for( metaCol in binaryCols )
				{
					parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] )
					if( doDebug() ) print( paste("parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] ):", parPval ) )
					nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] )
					if( doDebug() ) print( paste("nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] ):", nonParPval ) )
					addBoxPlot( otuTable, otuCol, metaCol, parPval, nonParPval )
				}
				for( metaCol in nominalCols )
				{
					parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] )
					if( doDebug() ) print( paste("parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] ):", parPval ) )
					nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] )
					if( doDebug() ) print( paste("nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] ):", nonParPval ) )
					addBoxPlot( otuTable, otuCol, metaCol, parPval, nonParPval )
				}
				for( metaCol in numericCols )
				{
					parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] )
					if( doDebug() ) print( paste("parPval = displayPval( parStats[ parStats[colnames(parStats)[1]]==colnames(otuTable)[otuCol], colnames(parStats)==names(otuTable)[metaCol] ] ):", parPval ) )
					nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] )
					if( doDebug() ) print( paste("nonParPval = displayPval( nonParStats[ nonParStats[colnames(nonParStats)[1]]==colnames(otuTable)[otuCol], colnames(nonParStats)==names(otuTable)[metaCol] ] ):", nonParPval ) )
					addScatterPlot( otuTable, otuCol, metaCol, parPval, nonParPval )
				}
			}
		}
		dev.off()
		if( doDebug() ) sink()
	}
}
