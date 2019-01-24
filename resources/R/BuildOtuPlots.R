# Module script for: biolockj.module.r.BuildOtuPlots

# Output box-plot illustrating OTU-nominal metadata field relationship
# Print adjusted P-values in pot header
addBoxPlot <- function( otuColName, otuColVals, metaColName, metaColVals, barColors) 
{
	if( doDebug() ) print( paste( "Createing box plot for otu:", otuColName, "metadata column:", metaColName ) )
	metaColVals = as.factor( metaColVals )
	factors = split(otuColVals, f=metaColVals) 
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
addPvalueNote <- function(parPval, nonParPval, r2, 
													cutoff=getProperty("r.pvalCutoff",-1),
													attName=NULL){
	lineNumbers = c(0.2, 1.4, 2.6)
	names(lineNumbers) = c("low", "mid", "top")
	parTestName=""
	nonParTestName=""
	if (!is.null(attName)){
		parTestName = paste0(" (", getTestName(attName, isParametric = TRUE), ")")
		nonParTestName = paste0(" (", getTestName(attName, isParametric = FALSE), ")")
	}
	mtext(text=paste("r-squared:", round(r2, 3)), 
				line=lineNumbers["top"], 
				at=par("usr")[1], side=3, adj=0, cex=par("cex"))
	mtext(text=paste0("adjusted parametric", parTestName, " p-value: ", displayPval(parPval)), 
				line=lineNumbers["mid"], col=getColor(parPval), 
				at=par("usr")[1], side=3, adj=0, cex=par("cex"))
	mtext(text=paste0("adjusted non-parametric", nonParTestName, " p-value: ", displayPval(nonParPval)), 
				line=lineNumbers["low"], col=getColor(nonParPval), 
				at=par("usr")[1], side=3, adj=0, cex=par("cex"))
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

# error handler for tryCatch in main that wraps each OTU plot set
errorHandlerOtuPlots <- function(err, otuLevel, otuCol){
	if( doDebug() ){print(err)}
	msg = paste0("Failed to create plot for taxonomy level: ", otuLevel, 
							 "\nusing OTU column: ", otuCol)
	if( doDebug() ){print(msg)}
	plotPlainText(msg)
}

# error handler for tryCatch in main that wraps each meta data column plot
errorHandlerOtuSubPlots <- function(err, otuLevel, otuCol, metaCol){
	if( doDebug() ){print(err)}
	msg = paste0("Failed to create plot for \ntaxonomy level: ", otuLevel, 
							 "\nOTU column: ", otuCol,
							 "\nmeta data column: ", metaCol)
	if( doDebug() ){print(msg)}
	plotPlainText(msg)
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
		if( doDebug() ) print( paste( "binaryCols:", paste0(binaryCols, collapse= ", ") ) )
		nominalCols = getNominalFields() #getColIndexes( otuTable, getNominalFields() )
		if( doDebug() ) print( paste( "nominalCols:", paste0(nominalCols, collapse= ", ") ) )
		numericCols = getNumericFields() #getColIndexes( otuTable, getNumericFields() )
		if( doDebug() ) print( paste( "numericCols:", paste0(numericCols, collapse= ", ") ) )
		reportFields = getReportFields()
		
		# adjusted parametric pvalues
		adjParInputFile = getPipelineFile( buildStatsFileSuffix(parametric=TRUE, adjusted=TRUE, level=otuLevel) )
		if( doDebug() ) print( paste( "adjParInputFile:", adjParInputFile ) )
		parStats = read.table( adjParInputFile, check.names=FALSE, 
													 header=TRUE, sep="\t", row.names = 1 )
		
		# adjusted non-parametric pvalues
		adjNonParInputFile = getPipelineFile( buildStatsFileSuffix(parametric=FALSE, adjusted=TRUE, level=otuLevel) )
		if( doDebug() ) print( paste( "adjNonParInputFile:", adjNonParInputFile ) )
		nonParStats = read.table( adjNonParInputFile, check.names=FALSE, 
															header=TRUE, sep="\t", row.names = 1 )
		
		# r-squared values
		rSquareFile = getPipelineFile( buildStatsFileSuffix(parametric=NA, level=otuLevel) )
		r2Stats = read.table( rSquareFile, check.names=FALSE, 
													header=TRUE, sep="\t", row.names = 1 )
		
		# # select colors for box plots so each box is different even across different metadata fields
		# numBoxes = sapply(otuTable[,c(binaryCols, nominalCols)], function(x){length(levels(as.factor(x)))})
		# boxColors = getColors( sum(numBoxes))
		# metaColColors = split(boxColors, f=as.vector(mapply(x=names(numBoxes), each=numBoxes, rep)))
		metaColColors = getColorsByCategory()
		
		# create empty ouptut file
		plotsPerOTU = length(reportFields)
		outputFile = getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_OTU_plots.pdf") )
		if( doDebug() ) print( paste( "Saving plots to", outputFile ) )
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
			tryCatch(expr={
				if( sum( otuTable[,otuCol] > 0 ) >=  cutoffValue ) {
					par( mfrow = par("mfrow") ) # step to next page, even if the last page is not full
					position = 1
					page = 1
					for( metaCol in reportFields)	{
						
						tryCatch(expr={
						# add a plot
						if ( metaCol %in% binaryCols | metaCol %in% nominalCols){
							addBoxPlot( otuColName=otuCol, otuColVals=otuTable[,otuCol],
													metaColName=metaCol, metaColVals=otuTable[,metaCol], barColors=metaColColors[[metaCol]])
						}
						if ( metaCol %in% numericCols ) {
							addScatterPlot( otuColName=otuCol, otuColVals=otuTable[,otuCol],
															metaColName=metaCol, metaColVals=otuTable[,metaCol] )
						}
						# add p-values
						addPvalueNote(parPval = nonParStats[ otuCol, metaCol ], 
													nonParPval = nonParStats[ otuCol, metaCol ], 
													r2=r2Stats[ otuCol, metaCol], attName=metaCol)
						# Add plot title
						mtext(metaCol, side=1, font=par("font.main"), cex=par("cex.main"), line=2.5) #col=getColor(c(parPval, nonParPval))
						}, error = function(err) {
							errorHandlerOtuSubPlots(err, otuLevel=otuLevel, otuCol=otuCol, metaCol=metaCol)
						})
						
						# page title
						position = position + 1
						if (position == 2) { 
							#title(main=otuCol, outer = TRUE, line=2)
							mtext(otuCol, side=3, outer = TRUE, font=par("font.main"), cex=par("cex.main"), line=2)
							titlePart2 = ifelse( page == 1, paste( "taxonomic level:", otuLevel), paste("page", page))
							title(main=titlePart2, outer = TRUE, line=1)
						}
						if (position > prod(par("mfrow") ) ) {
							position = 1
							page = page + 1
						}
					}
				}
			}, error = function(err) {
				errorHandlerOtuPlots(err, otuLevel=otuLevel, otuCol=otuCol)
			})
		}
		dev.off()
		if( doDebug() ) sink()
	}
}
