# Module script for: biolockj.module.r.BuildFoldChangePlots

### assumes library(BioLockJ_Lib.R)

### NOTE: The header printed on the reference table serves as built-in documentation for this module. 
# See saveRefTableWithHeader

### custom config options:
# r.FCplot.pvalType=
# r.FCplot.pvalIncludeBar=
# r.FCplot.userOTUs=
# r.FCplot.maxBars=
### also uses:
# r.pvalCutoff
# internal.numMetaCols
# internal.parserModule # if doing fold change
###



# The main method is designed to integrate this module with BiolockJ.  
# It handles pulling data from other modules and options from the BiolockJ properties.
main <- function(){
	# get config option for pvalStar, pvalIncludeBar, maxBars, userOTUs, 
	pvalStar = getProperty("r.pvalCutoff", 0.05)
	pvalIncludeBar = getProperty("r.FCplot.pvalIncludeBar", 1)
	maxBars = getProperty("r.FCplot.maxBars", 40)
	userOTUs = getProperty("r.FCplot.userOTUs", NULL)
	scale.fun="log2"
	# get metadata
	if( doDebug() ) print( paste( "metadata file:", getMetaDataFile() ) )
	meta = getMetaData()
	#
	for (otuLevel in getProperty("report.taxonomyLevels") ) {
		if( doDebug() ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildFoldChangePlots_", otuLevel, ".log") ) )
		if( doDebug() ) print( paste( "processing otuLevel:", otuLevel ) )
		# make a new pdf output file, specify page size
		outFileName = getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_OTU-foldChangePlots.pdf") )
		if( doDebug() ) print( paste( "Creating file:", outFileName ) )
		height=10
		pdf(file=outFileName, paper="letter", width=7.5, height=height, onefile=TRUE)
		# make some logical choices about par() based on maxBars and paper size
		if (maxBars < 20 & height >=10){
			par(mfrow=c(2,1))
		}else{
			par(mfrow=c(1,1))
		}
		par(mar=c(6, 5, 2, 5), oma=c(0,0,0,0))
		#
		# get normalized OTU vals
		# inputFile = getPipelineFile( paste0(otuLevel, ".*_norm.tsv") ) <-- might revert to this in future
		inputFile = getPipelineFile( paste0(otuLevel, ".*_metaMerged.tsv") )
		if( doDebug() ) print( paste( "inputFile:", inputFile ) )
		otuTable = read.table( inputFile, check.names=FALSE, na.strings=getProperty("metadata.nullValue", "NA"),
													 comment.char=getProperty("metadata.commentChar", ""), header=TRUE, sep="\t", row.names=1)
		#otuTable = normalize(readRawCounts(otuLevel)[[1]])
		if( doDebug() ){ print( paste0("otuTable has ", nrow(otuTable), " rows and ", ncol(otuTable), " columns."))}
		lastOtuCol = ncol(otuTable) - getProperty("internal.numMetaCols")
		otuTable = otuTable[1:lastOtuCol]
		#
		# Might undo log-scale to get fold change values, then optionally scale them in some way.
		logBase = getProperty("report.logBase", NULL) # move this to top of main where we get properties
		if (!is.null(logBase)){
			otuTable = logBase^otuTable
			if( doDebug() ) print( paste( "Input values are on a ", logBase, " scale, this has been reversed." ) )
		}
		#
		# get pvals from calc stats, maybe add config option to specify which pvals to get
		pvalFileIdentifier = getProperty(name = "r.FCplot.pvalType", "_adjNonParPvals.tsv")
		pvalFile = getPipelineFile( paste0(otuLevel, pvalFileIdentifier) )
		if( doDebug() ) print( paste( "p-value file:", pvalFile ) )
		pvalTable = read.table( pvalFile, check.names=FALSE, header=TRUE, sep="\t", row.names = 1)
		if( doDebug() ){ print( paste0("pvalTable has ", nrow(pvalTable), " rows and ", ncol(pvalTable), " columns."))}
		# 
		if( doDebug() ) print( paste( "Prepareing plot for each of ", length(getBinaryFields()), "binary attributes." ) )
		if (length(getBinaryFields()) == 0){
			plotPlainText("No binary attributes.")
		}
		for (biAtt in getBinaryFields()){
			if( doDebug() ) print( paste( "processing binary attribute:", biAtt ) )
			splitOTU = split(otuTable[row.names(meta),], f=meta[,biAtt])
			pvals = pvalTable[,biAtt]
			names(pvals) = row.names(pvalTable)
			par(mar=c(6, 5, 2, 5), oma=c(0,0,0,0)) # this is changed within the plot function, so reset before each plot
			# make plot
			tryCatch(expr={
				if( doDebug() ) print( paste("Calling addFoldChangePlot for otuLevel:", otuLevel, " and binary attribute:", biAtt) )
				calculations = calcBarSizes(numGroupVals=splitOTU[[2]], denGroupVals=splitOTU[[1]], 
																		numGroupName=names(splitOTU)[2], denGroupName=names(splitOTU)[1],
																		pvals=pvals, pvalIncludeBar=pvalIncludeBar, userOTUs=userOTUs, maxBars=maxBars, 
																		saveRefTable=getPath( file.path(getModuleDir(), "temp"), paste(otuLevel, biAtt,"OTU-foldChangeTable.tsv", sep="_") ),
																		scale.fun=scale.fun)
				complete = drawPlot(toPlot=calculations[["toPlot"]], barSizeColumn="scaledFC",
								 xAxisLab=paste0(scale.fun, "(fold change)"), title=biAtt,
								 pvalStar=pvalStar, starColor=getProperty("r.colorHighlight", "red"), 
								 xAxisLab2 = calculations[["xAxisLab2"]], 
								 comments=calculations[["comments"]])
				if (complete) {
					print( paste("Completed plot for otuLevel:", otuLevel, " and binary attribute:", biAtt))
				}
			}, error= function(err) {
				if( doDebug() ){print(err)}
				origErr = as.character(err)
				trimmedErr=gsub("Error.*Stop Plotting:", "", origErr) # error messages that I create in addFoldChangePlot start with "Stop Plotting"
				msg = paste0("Failed to create plot for taxonomy level: ", otuLevel, 
										 "\nusing attribute: ", biAtt)
				if (doDebug() | nchar(trimmedErr) < nchar(origErr)){
					msg = paste0(msg, "\n", trimmedErr)
				}
				plotPlainText(msg)
			})
		}
		dev.off()
		print( paste( "Saved fold change plot to file:", outFileName ) )
		if( doDebug() ) sink()
	}
}

# Read the counts from the parser module; this requires the BiolockJ pipeline environment.
# returns a list with each otuLevel as an element.
readRawCounts <- function(otuLevels=getProperty("report.taxonomyLevels")){
	parserModule = getProperty("internal.parserModule")
	parserOutput = file.path(parserModule, "output")
	counts = list()
	names(counts) = otuLevels
	for (level in otuLevels){
		fileName = dir(path=parserOutput, pattern=level)
		if (length(fileName) > 1){
			stop(paste("Ambiguous file for", level))
		}
		if (length(fileName) < 1){
			print(paste("no file found for", level))
		}
		counts[level] = read.table( inputFile, check.names=FALSE, na.strings=getProperty("metadata.nullValue", "NA"), 
																comment.char=getProperty("metadata.commentChar", ""), header=TRUE, sep="\t", row.names=1)
	}
	return(counts)
}


# normalize otu counts by simple relative abundance
normalize <- function(otuTable){
	# otuTable - data frame with a row for each sample and a column for each OTU
	normFactor = rowSums(otuTable)
	normed = otuTable/normFactor
	return(normed)
}


# returns a list of 2:
#  toPlot - a dataframe with values to plot and info for each bar
#  comment - a string(s) that should be included in the plot to inform the user about this step
calcBarSizes <- function(numGroupVals, denGroupVals, 
												 numGroupName, denGroupName,
												 pvals=NULL, pvalIncludeBar=0.05, maxBars=30, userOTUs=NULL,
												 saveRefTable=NULL, scale.fun="log2"){
	# numGroupVals, denGroupVals - each a data frame, where OTUs are column names and rows are samples
	##   these should have different row names (samples are from different groups) but matching column names.
	# numGroupName, denGroupName - Strings used in plot
	# pvals - named vector of pvalues, taken from calcStats (which test is configurable).
	##   names of pvals are OTU's, and should match num(den)GroupVals column names.
	# pvalIncludeBar - OTUs that do not meet this value are not plotted (if pvals supplied)
	# maxBars - int, maximum number of bars to plot
	# userOTUs - string vector, set of OTUs of interest to the user.  If present, this overrides pvals.
	##   userOTUs may include OTUs that are not in the tables, only the intersect of both tables and userOTUs will be used.
	# saveRefTable - file name to save a reference table corresponding to the plot
	# scale.fun - string (with quotes) giving the name of the function to use to scale
	##   the bar values values: probably log2 or log10.
	#
	# Select viable OTUs to plot
	viableOTUs = selectViableOTUs(group1=names(numGroupVals), group2= names(denGroupVals), 
																pvals=pvals, pvalIncludeBar=pvalIncludeBar, userOTUs=userOTUs)
	plotOTUs = viableOTUs[["plotOTUs"]]
	comment = viableOTUs[["comment"]]
	numGroupVals = numGroupVals[plotOTUs]
	denGroupVals = denGroupVals[plotOTUs]
	#
	# assemble data frame of plot values. Calc and scale fold changes
	numMeans = colMeans(numGroupVals, na.rm=TRUE)
	denMeans = colMeans(denGroupVals, na.rm=TRUE)
	toPlot = data.frame(OTU=plotOTUs, row.names=plotOTUs,
											numMeans = numMeans, denMeans = denMeans,
											foldChange = numMeans / denMeans,
											infUp = numMeans > 0 & denMeans == 0, #In the table, these are Inf
											infDown = numMeans == 0 & denMeans > 0) #In the table, these are 0
	toPlot$scaledFC = do.call(scale.fun, list(x=toPlot$foldChange))
	toPlot$pvalue = pvals[row.names(toPlot)]
	#
	# select top [maxBars] most changed OTUs
	# cases where one group is all-zeros is treated at most changed
	toPlot = toPlot[order(abs(toPlot$scaledFC), decreasing = T),] #highest abs on top
	maxBars = min(c(maxBars, nrow(toPlot)))
	toPlot$plotPriority = 1:nrow(toPlot)
	toPlot$includeInPlot = toPlot$plotPriority <= maxBars
	#
	# order OTUs to plot
	ordNames = row.names(toPlot)[order(toPlot$scaledFC)]
	toPlot = toPlot[ordNames,] #lowest values at top, barplot plots from bottom
	#
	# save a table the user can reference
	if (!is.null(saveRefTable)){
		headerArgsList = list(numGroupName=numGroupName, denGroupName=denGroupName, 
													scale.fun=scale.fun, pvalIncludeBar=pvalIncludeBar, pvalStar=pvalStar)
		saveRefTableWithHeader(saveRefTable=saveRefTable, toPrint=toPlot, 
													 ordNames=ordNames, 
													 headerArgsList=headerArgsList)
	}
	#
	# get rid of the rows that will not be plotted
	toPlot = toPlot[toPlot$includeInPlot,]
	xAxisLab2 = paste0(numGroupName, " (n=", nrow(numGroupVals), ") relative to ", denGroupName, " (n=", nrow(denGroupVals), ")")
	comments = paste0("Showing top ", maxBars, " most changed OTUs ", comment)
	return(list(toPlot=toPlot, comments=comments, xAxisLab2=xAxisLab2))
}



# Select the OTU's that qualify to be in the plot
selectViableOTUs <- function(group1, group2, pvals=NULL, pvalIncludeBar=NULL, userOTUs=NULL){
	# group1 - OTUs that are viable for group 1
	# group2 - OTUs that are viable for group 2
	# pvals - named vector of pvalues, taken from calcStats (which test is configurable).
	##   names of pvals are OTU's, and should match num(den)GroupVals column names.
	# pvalIncludeBar - OTUs that do not meet this value are not plotted (if pvals supplied)
	# userOTUs - string vector, set of OTUs of interest to the user.  If present, this overrides pvals.
	##   userOTUs may include OTUs that are not in the tables, only the intersect of both tables and userOTUs will be used.
	#
	sharedOTUs = intersect(group1, group2)
	if (!is.null(pvals) & is.null(userOTUs)){
		sigOTUs = names(pvals)[pvals <= pvalIncludeBar]
		if (length(sigOTUs) == 0){
			stop(paste("Stop Plotting: Provided", length(pvals), "pvalues, \nwith", length(sigOTUs), "below the provided threshold:", pvalIncludeBar))
		}
		plotOTUs = intersect(sigOTUs, sharedOTUs)
		comment=paste0("out of ", length(plotOTUs), " OTUs with p-value <= ", pvalIncludeBar, ".")
	}else if (!is.null(userOTUs)){
		plotOTUs = intersect(userOTUs, sharedOTUs)
		comment=paste0("out of ", length(plotOTUs), " user-supplied OTUs.")
	}else{
		plotOTUs = sharedOTUs
		comment=paste0("out of ", length(plotOTUs), " reported OTUs.")
	}
	if(is.null(plotOTUs) | length(plotOTUs) == 0){
		stop("Stop Plotting: No qualifying OTUs to plot.")
	}
	return(list(plotOTUs=plotOTUs, comment=comment))
}



# The header printed on the reference table serves as built-in documentation for this method.
saveRefTableWithHeader <- function(saveRefTable, toPrint, skipColumns=NULL,
																	 ordNames=NULL,
																	 headerArgsList=NULL){
	# saveRefTable - file name to save the table to
	# toPrint - data frame of values to save
	# skipColumns - colunns to NOT include in the saved file
	# ordNames, infUp, infDown ---- soon to be removed argument
	# headerArgsList - named list with values that might be used in the header
	header = "Fold Change Plot Reference"
	if (!is.null(skipColumns)){
		keepCols = setdiff(names(toPrint), skipColumns)
		toPrint = toPrint[,keepCols]
	}
	header = c(header, "<group name>.mean: the mean value for each group used to calculate the fold change.",
						 paste("foldChange: ratio of the mean of one group over the other,", headerArgsList$numGroupName, "over", headerArgsList$denGroupName), 
						 paste0("scaledFC: the ", headerArgsList$scale.fun, "-scaled fold change, this is the bar length shown in the plot"),
						 'plotPriority: the rank of this OTU when determineing the "most changed" using abs(scaledFC); number of OTUs plotted can configured with r.FCplot.maxBars or over-riden using r.FCplot.userOTUs.')
	names(toPrint)[2:3] = paste(c(headerArgsList$numGroupName, headerArgsList$denGroupName), "mean", sep=".")
	header = c(header, "plot.location: vertical location of the bar in the plot")
	toPrint$plot.location = NA
	toPrint[ordNames,"plot.location"] = nrow(toPrint):1
	toPrint = toPrint[order(toPrint$plot.location),]
	header = c(header, paste0("pvalue: the p-value used to determine if the OTU was included (if under ", 
														headerArgsList$pvalIncludeBar,") and if OTU got a star (if under ", headerArgsList$pvalStar, 
														"),  thresholds controlled by r.FCplot.pvalIncludeBar and r.pvalCutoff properties respectively.",
														" See also: r.FCplot.pvalType property"))
	header = c(header, paste("infUp: was the fold change flagged for having all-0-counts only in the", headerArgsList$denGroupName, "group."))
	header = c(header, paste("infDown: was the fold change flagged for having all-0-counts only in the", headerArgsList$numGroupName, "group."))
	header = paste("#", header)
	writeLines(header, con=saveRefTable)
	suppressWarnings(write.table(toPrint, file=saveRefTable, quote=FALSE, sep="\t", row.names = FALSE, append = TRUE))
	print(paste("Saved reference table to", saveRefTable))
}


# If this function reaches the end, it returns TRUE.
drawPlot <- function(toPlot, barSizeColumn, xAxisLab=barSizeColumn, title="Impact per OTU",
										 numGroupColor="darkorange2", denGroupColor="burlywood1",
										 pvalStar=NULL, starColor="red",
										 xAxisLab2=NULL, comments=NULL,
										 fixedBarHeightInches=0, # this should pretty much always be 0
										 centerAt0=TRUE ){
	# toPlot - data frame of values to plot. 
	#  columns in toPlot should include: columns specified by barSizeColumn; 
	#  and optionally: infUp, infDown, pvals, color, OTU
	#  If OTU column is present, that will used for labels, otherwise row.names will be used
	# barSizeColumn - character (or potentially integer) giving the column from toPlot to be used to create bars, currently supports length 1.
  # title - main title for the plot
	# numGroupColor, denGroupColor - color to use for bars to the left and right (respectively) of the 0 line.
	# pvalStar - OTUs that meet this cuttoff get a star.
	# starColor - color to use for significance star
	# xAxisLab - label to use for x-axis
	# xAxisLab2 - string to plot below the x-axis label (added explaination of x-axis)
	# comments - string(s) to add to bottom of the plot to inform the user
	# fixedBarHeightInches - vertical space (in inches) to allow for each bar, the total plot region is adjusted to fit this.
	##   If fixedBarHeightInches is set to 0, then it is set equal to the number of inches per 'line' in the margins.
	# centerAt0 - boolean, should the xmin and xmax be set to the same value? This is almost always true.
	#
	# Check required values in toPlot
	if (is.null(toPlot[,barSizeColumn])){ stop(paste('Input data frame [toPlot] must include a barSizeColumn ("', barSizeColumn, '") column.')) }
	# Set optional columns in toPlot
	if (is.null(toPlot$infUp)){ 
		toPlot$infUp=FALSE
		toPlot$infUp[toPlot[,barSizeColumn] == Inf] = TRUE
		}
	if (is.null(toPlot$infDown)){
		toPlot$infDown=FALSE
		toPlot$infDown[toPlot[,barSizeColumn] == -Inf] = TRUE
		}
	if (is.null(toPlot$color)){
		toPlot$color = NA
		toPlot$color[toPlot[,barSizeColumn] > 0] = numGroupColor
		toPlot$color[toPlot[,barSizeColumn] < 0] = denGroupColor
	}
	if (is.null(toPlot$OTU)){ toPlot$OTU = row.names(toPlot)}
	#
	# determine the direction for each bar (or where "Inf" should be written)
	left = which(toPlot[,barSizeColumn] > 0) #where text goes on the left
	right = which(toPlot[,barSizeColumn] < 0)
	# cases where one group is all-zeros is plotted as 0 
	toPlot[(toPlot$infUp | toPlot$infDown), barSizeColumn] = 0 # bar gets a space, but no visible bar is plotted.
	#
	# determine plot size based on number of bars to plot, and lower lines needed for axis label and comments
	if (!is.null(fixedBarHeightInches)){
		# set mar[1] to fit the axis labels and comments
		linesNeededBelow = 1.5 + length(xAxisLab) + length(xAxisLab2) + length(comments) + ifelse(length(comments)>0, 1, 0) # allow a one line gap between axis labels and comments
		mars = par("mar")
		mars[1] = linesNeededBelow
		par(mar=mars)
		if (fixedBarHeightInches == 0 ) { # set bar width + space to be the same size as one "line" of the margin
			fixedBarHeightInches = max(par("mai") / par("mar"), na.rm=TRUE) # 4 vals should all be the same but any could be /0
		}
		if (fixedBarHeightInches > 0 ) { # set bar width + space to this many inches
			plotRegionHeightInches = dev.size()[2]
			plotMarginsHeightInches = par("mai")[1] + par("mai")[3] + par("omi")[1] + par("omi")[3]
			inchesForBars = nrow(toPlot) * fixedBarHeightInches
			inchesToRemove = plotRegionHeightInches - plotMarginsHeightInches - inchesForBars
		}else{
			print("Argument fixedBarHeightInches should be a numeric value >= 0.")
		}
		if (inchesToRemove > 0){
			mais = par("mai")
			mais[1] = mais[1] + inchesToRemove
			par(mai=mais)
		}else{
			print(paste0("Not enough space in plot for ", nrow(toPlot), " bars with ", 
									 fixedBarHeightInches, " for each bar. Bar widths will be set to fit the space."))
		}
	}
	#
	# determine plot dimensions
	xmin = min(toPlot[,barSizeColumn])
	xmax = max(toPlot[,barSizeColumn])
	if (centerAt0){
		width = max(abs(c(xmin, xmax)))
		if (width == 0){width = 1}
		xmin = -width
		xmax = width
	}else{
		xmin = min(0, xmin)
		xmax = max(0, xmax)
	}
	barWidth=1
	barSpace=.2
	bp = barplot(toPlot[,barSizeColumn], horiz=TRUE, plot=FALSE, width = barWidth, space = barSpace) # this one is not plotted, its just a reference
	row.names(bp) = row.names(toPlot)
	# check xlims
	# print(paste("xmin, xmax: ", xmin, ", ", xmax))
	#
	# plot area and outer text
	plot(x=0, y=1, type='n', 
			 xlim=c(xmin, xmax), 
			 ylim=c(0, max(bp)+(barWidth/2)+barSpace),
			 xlab="", ylab="", axes=FALSE)
	title(main=title, line=1)
	lowerLine = 2
	mtext(text=xAxisLab, side=1, line=lowerLine)
	if (!is.null(xAxisLab2)){
		lowerLine = lowerLine + 1
		mtext(text=xAxisLab2, side=1, line=lowerLine)
	}
	lowerLine = lowerLine + 2
	for (comment in comments){
		mtext(comment, side=1, line=lowerLine, adj=0)
		lowerLine = lowerLine + 1
	}
	#
	# x-axis, axis lines and inner text
	# The vertical lines should match the axis tick marks, and they should be behind the bars,
	# and they should not be drawn where text will be drawn.
	ax = axis(side=1)
	abline(v=0)
	vertRef = c(0, bp, par("usr")[4]) #bottom of plot, bar midpoints, top
	vertMidLine = (vertRef[length(right)+1] + vertRef[length(right)+2])/2
	if (length(left)==0){ vertMidLine = par("usr")[4] }
	if (length(right)==0){ vertMidLine = par("usr")[3] }
	if (length(left) > 0){
		segments(x0=ax[ax>0], y0=vertMidLine, y1=par("usr")[4], col=gray(.8), lwd=.5)
		text(x=0, y=bp[left], labels=toPlot$OTU[left], pos=2, xpd=TRUE)
	}
	if (length(right) > 0){
		segments(x0=ax[ax<0], y0=par("usr")[3], y1=vertMidLine, col=gray(.8), lwd=.5)
		text(x=0, y=bp[right], labels=toPlot$OTU[right], pos=4, xpd=TRUE)
	}
	if (sum(toPlot$infUp) > 0){
		text(x=0, y=bp[toPlot$infUp,], labels="Inf", pos=4, xpd=TRUE)
	}
	if (sum(toPlot$infDown) > 0){
		text(x=0, y=bp[toPlot$infDown,], labels="-Inf", pos=2, xpd=TRUE)
	}
	#
	# plot the bars
	bp = barplot(toPlot[,barSizeColumn], horiz=TRUE, width = barWidth, space = barSpace,
							 add=TRUE, col=toPlot$color, border="black")
	row.names(bp) = row.names(toPlot)
	#
	# plot the stars
	if (!is.null(pvalStar) & !is.null(toPlot$pvals)){
		starOTUs = row.names(toPlot)[toPlot$pvals <= pvalStar]
		starChar = "*"
		if ( length(starOTUs) > 0 ){
			starBarGap = 0.03 * par("usr")[2]
			xPlusGap = toPlot[starOTUs,barSizeColumn] + ifelse(toPlot[starOTUs,barSizeColumn] > 0, starBarGap, (-1 * starBarGap))
			points(x=xPlusGap, y=bp[starOTUs,], pch=starChar, col=starColor, xpd=TRUE)
		}
		mtext(paste0("(", starChar, ") p-value <= ", pvalStar), side=3, line=0, adj=0)
	}
	return(TRUE)
}



