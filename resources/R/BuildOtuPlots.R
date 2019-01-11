# Module script for: biolockj.module.r.BuildOtuPlots

# Output box-plot illustrating OTU-nominal metadata field relationship
# Print adjusted P-values in pot header
addBoxPlot <- function( otuColName, otuColVals, metaColName, metaColVals, barColors) #, otuTable, otuCol, metaCol, parPval, nonParPval
	{
	if( doDebug() ) print( paste( "Createing box plot for otu:", otuCol, "metadata column:", metaCol ) )
	metaColVals = as.factor( metaColVals )
	factors = split(otuColVals, f=metaColVals) # getFactorGroups( otuTable, metaColVals, otuCol )
	#barColors = getColors( length(factors) )
	# select some graphical parameters
	cexAxis = getCexAxis( levels(metaColVals) )
	if( doDebug() ) print( paste( "cexAxis = getCexAxis( levels(metaColVals) ):", cexAxis ) )
	labels = getBoxPlotLabels( levels(metaColVals) )
	if( doDebug() ) print( paste( c("X axis labels = getBoxPlotLabels( levels(metaColVals) ):", labels), collapse= " " ) )
	orient = getLas( levels(metaColVals) )
	if( doDebug() ) print( paste( "orient = getLas( levels(metaColVals) ):", orient ) )
	# draw plot
	boxplot( factors, outline=FALSE, names=labels, las=orient, col=barColors, 
					 pch=getProperty("r.pch"), ylab=otuColName, xlab="", main="", #cex=0.2, 
					 cex.axis=cexAxis ) #col.lab=color, col.main=color, cex.main=1, 
	stripchart( otuColVals ~ metaColVals, data=data.frame(otuColVals, metaColVals), 
							method="jitter", vertical=TRUE, pch=20, add=TRUE ) #ces=0.5, 
}

# Add text at the top of the plot in the margin
addPvalueNote <- function(parPval, nonParPval, cutoff=getProperty("r.pvalCutoff",-1)){
	mtext(text="Adjusted p-values:", line=1.4, 
				at=par("usr")[1], side=3, adj=0, cex=par("cex"))
	mtext(text=paste("Parametric:", parPval), line=0.2, 
				at=par("usr")[1], side=3, adj=0, cex=par("cex"), col=getColor(parPval))
	mtext(text=paste("Non-Parametric:", nonParPval), line=0.2, 
				at=par("usr")[2], side=3, adj=1, cex=par("cex"), col=getColor(nonParPval))
}


# Output scatter-plot illustrating OTU-numeric metadata field relationship
# Print adjusted P-values in pot header
addScatterPlot <- function( otuColName, otuColVals, metaColName, metaColVals ) #, parPval, nonParPval
{
	if( doDebug() ) print( paste( "Createing scatter plot for otu:", otuColName, "metadata column:", metaColName ) )
	# color = getColor( c(parPval, nonParPval) )
	# if( doDebug() ) print( paste( "color = getColor( c(parPval, nonParPval) ):", color ) )
	pointColors = getColors( length(metaColVals) )
	plot( metaColVals, otuColVals, pch=getProperty("r.pch"), 
				col=getProperty("r.colorPoint", "black"), #xaxt="n", col.lab=color, cex.main=1, col.main=color,
				ylab=otuColName, xlab="", main="")
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
		
		# metadata columns
		binaryCols = getBinaryFields() #getColIndexes( otuTable, getBinaryFields() )
		if( doDebug() ) print( paste( c("binaryCols:", binaryCols), collapse= ", " ) )
		nominalCols = getNominalFields() #getColIndexes( otuTable, getNominalFields() )
		if( doDebug() ) print( paste( c("nominalCols:", nominalCols), collapse= ", " ) )
		numericCols = getNumericFields() #getColIndexes( otuTable, getNumericFields() )
		if( doDebug() ) print( paste( c("numericCols:", numericCols), collapse= ", " ) )
		reportFields = names(otuTable)[(lastOtuCol+1):ncol(otuTable)]
		
		# adjusted parametric pvalues
		adjParInputFile = getPipelineFile( paste0(otuLevel, "_adjParPvals.tsv") )
		if( doDebug() ) print( paste( "adjParInputFile:", adjParInputFile ) )
		parStats = read.table( adjParInputFile, check.names=FALSE, 
													 header=TRUE, sep="\t", row.names = 1 )
		
		# adjusted non-parametric pvalues
		adjNonParInputFile = getPipelineFile( paste0(otuLevel, "_adjNonParPvals.tsv") )
		if( doDebug() ) print( paste( "adjNonParInputFile:", adjNonParInputFile ) )
		nonParStats = read.table( adjNonParInputFile, check.names=FALSE, 
															header=TRUE, sep="\t", row.names = 1 )
		
		# select colors for box plots so each box is different even across different metadata fields
		numBoxes = sapply(otuTable[,c(binaryCols, nominalCols)], function(x){length(levels(as.factor(x)))})
		boxColors = getColors( sum(numBoxes))
		metaColColors = split(boxColors, f=as.vector(mapply(x=names(numBoxes), each=numBoxes, rep)))
		
		
		# create empty ouptut file
		plotsPerOTU = length(reportFields)
		outputFile = getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_OTU_plots.pdf") )
		if( doDebug() ) print( paste( "CSaving plots to", outputFile ) )
		if (plotsPerOTU < 5 ) {
			pdf( outputFile, width = 7, height = 7)
			par( mfrow=c(2, 2) )
		}else{
			pdf( outputFile, paper="letter", width=7, height=10.5 )
			par( mfrow=c(3, 2) )
		}
		par(las=1, oma=c(0,1,5,0), mar=c(4, 4, 5, 2), cex=1)
		
		# if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of otuTable rows
		cutoffValue = getProperty("r.rareOtuThreshold", 1)
		if( cutoffValue < 1 ) {
			cutoffValue = cutoffValue * nrow(otuTable)
		}
		
		for( otuCol in names(otuTable)[1:lastOtuCol] ) {
			if( sum( otuTable[,otuCol] > 0 ) >=  cutoffValue ) {
				par( mfrow = par("mfrow") ) # step to next page, even if the last page is not full
				position = 1
				page = 1
				for( metaCol in reportFields)	{
					# get pvalues
					parPval = displayPval( parStats[ otuCol, metaCol ] )
					if( doDebug() ) print( paste("parPval:", parPval ) )
					nonParPval = displayPval( nonParStats[ otuCol, metaCol ] )
					if( doDebug() ) print( paste("nonParPval:", nonParPval ) )
					# add a plot
					if ( metaCol %in% binaryCols | metaCol %in% nominalCols){
						addBoxPlot( otuColName=otuCol, otuColVals=otuTable[,otuCol],
												metaColName=metaCol, metaColVals=otuTable[,metaCol], barColors=metaColColors[[metaCol]])
					}
					if ( metaCol %in% numericCols ) {
						addScatterPlot( otuColName=otuCol, otuColVals=otuTable[,otuCol],
														metaColName=metaCol, metaColVals=otuTable[,metaCol] )
					}
					addPvalueNote(parPval, nonParPval)
					mtext(metaCol, side=1, font=par("font.main"), cex=par("cex.main"), line=2.5, col=getColor(c(parPval, nonParPval)))
					# page title
					position = position + 1
					if (position == 2) { 
						title(main=otuCol, outer = TRUE, line=2)
						titlePart2 = ifelse( page == 1, paste( "taxonomic level:", otuLevel), paste("page", page))
						title(main=titlePart2, outer = TRUE, line=1)
						}
					if (position > prod(par("mfrow") ) ) {
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
