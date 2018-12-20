# Module script for: biolockj.module.r.BuildFoldChangePlots

### assumes library(BioLockJ_Lib.R)

### custom config options:
# r.FCplot.pvalType=
# r.FCplot.pvalIncludeBar=
# r.FCplot.userOTUs=
# r.FCplot.maxBars=
###

addFoldChangePlot <- function(numGroupName, denGroupName, 
															numGroupVals, denGroupVals, title,
															pvals=NULL, pvalIncludeBar=0.05, pvalStar=NULL,
															maxBars=30, userOTUs=NULL, fixedBarHeightInches=0,
															numGroupColor="darkorange2", denGroupColor="burlywood1",
															centerAt0=TRUE, scale.fun="log2"){
	# numGroupName, denGroupName - Strings used in plot
	# numGroupVals, denGroupVals - each a data frame, where OTUs are column names and rows are samples
	##   these should have different row names (samples are from different groups) but matching column names.
	# title - string, binary attribute being plotted
	# pvals - named vector of pvalues, taken from calcStats (which test is configurable).
	##   names of pvals are OTU's, and should match num(den)GroupVals column names.
	# pvalIncludeBar - OTUs that do not meet this value are not plotted (if pvals supplied)
	# pvalStar - OTUs that meet this cuttoff get a star.
	# maxBars - int, maximum number of bars to plot
	# userOTUs - string vector, set of OTUs of interest to the user.  If present, this overrides pvals.
	##   userOTUs may include OTUs that are not in the tables, only the intersect of both tables and userOTUs will be used.
	# fixedBarHeightInches - vertical space (in inches) to allow for each bar, the total plot region is adjusted to fit this.
	##   If fixedBarHeightInches is set to 0, then it is set equal to the number of inches per 'line' in the margins.
	# numGroupColor, denGroupColor - color to use to fill bar for OTUs with neg/pos fold change
	# centerAt0 - boolean, should the xmin and xmax be set to the same value?
	# scale.fun - string (with quotes) giving the name of the function to use to scale
	##   the fold change values: probably log2 or log10.
	#
	# Select viable OTUs to plot
	sharedOTUs = intersect(names(numGroupVals), names(denGroupVals))
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
	numGroupVals = numGroupVals[plotOTUs]
	denGroupVals = denGroupVals[plotOTUs]
	#
	# assemble data frame of plot values. Calc and scale fold changes
	numMeans = colMeans(numGroupVals, na.rm=TRUE)
	denMeans = colMeans(denGroupVals, na.rm=TRUE)
	infUp = plotOTUs[which(numMeans > 0 & denMeans == 0)] #In the table, these are Inf
	infDown = plotOTUs[which(numMeans == 0 & denMeans > 0)] #In the table, these are 0
	toPlot = data.frame(OTU=plotOTUs, row.names=plotOTUs,
											foldChange = numMeans / denMeans)
	toPlot$scaledFC = do.call(scale.fun, list(x=toPlot$foldChange))
	toPlot$color = NA
	toPlot$color[toPlot$foldChange > 1] = numGroupColor
	toPlot$color[toPlot$foldChange < 1] = denGroupColor
	#
	# select top [maxBars] most changed OTUs
	# cases where one group is all-zeros is treated at most changed
	toPlot = toPlot[order(abs(toPlot$scaledFC), decreasing = T),] #highest abs on top
	maxBars = min(c(maxBars, nrow(toPlot)))
	toPlot = toPlot[1:maxBars,]
	infUp = intersect(infUp, row.names(toPlot))
	infDown = intersect(infDown, row.names(toPlot))
	#
	# order OTUs to plot
	# cases where one group is all-zeros is plotted as 0 
	toPlot = toPlot[order(toPlot$scaledFC),] #lowest values at top, barplot plots from bottom
	toPlot[c(infUp,infDown), "scaledFC"] = 0 # bar gets a space, but no visible bar is plotted.
	#
	# determine plot size based on number of bars to plot
	if (!is.null(fixedBarHeightInches)){
		if (fixedBarHeightInches == 0 ) { # set bar width + space to be the same size as one "line" of the margin
			fixedBarHeightInches = max(par("mai") / par("mar")) # 4 vals should all be the same but any could be /0
		}
		if (fixedBarHeightInches > 0 ) { # set bar width + space to this many inches
			plotRegionHeightInches = par("fin")[2]
			plotMarginsHeightInches = par("mai")[1] + par("mai")[3]
			inchesForBars = maxBars * fixedBarHeightInches
			inchesToRemove = plotRegionHeightInches - plotMarginsHeightInches - inchesForBars
		}else{
			print("Argument fixedBarHeightInches should be a numeric value >= 0.")
		}
		if (inchesToRemove > 0){
			par(omi=c(inchesToRemove, 0, 0, 0))
		}else{
			print(paste0("Not enough space in plot for ", maxBars, " bars with ", fixedBarHeightInches, " for each bar. Bar widths will be set to fit the space."))
		}
	}
	#
	# determine plot dimensions
	xmin = min(toPlot$scaledFC)
	xmax = max(toPlot$scaledFC)
	left = which(toPlot$foldChange > 1) #where text goes on the left
	right = which(toPlot$foldChange < 1)
	if (centerAt0){
		width = max(abs(c(xmin, xmax)))
		if (width == 0){width = 1}
		xmin = -width
		xmax = width
	}
	barWidth=1
	barSpace=.2
	bp = barplot(toPlot$scaledFC, horiz=TRUE, plot=FALSE, width = barWidth, space = barSpace) # this one is not plotted, its just a reference
	row.names(bp) = row.names(toPlot)
	# check xlims
	if ( doDebug() ) print(paste("xmin, xmax: ", xmin, ", ", xmax))
	# plot area and outer text
	plot(x=0, y=1, type='n', 
			 xlim=c(xmin, xmax), 
			 ylim=c(0, max(bp)+(barWidth/2)+barSpace),
			 xlab="", ylab="", axes=FALSE)
	title(main=title, line=1)
	# text(paste("greater in", numGroupName), x=mean(c(0,par("usr")[2])), y=par("usr")[4], xpd=NA)
	# text(paste("greater in", denGroupName), x=mean(c(0,par("usr")[1])), y=par("usr")[4], xpd=NA)
	mtext(text=paste0(scale.fun, "(fold change)"), side=1, line=2)
	mtext(text=paste(numGroupName, "relative to", denGroupName), side=1, line=3)
	mtext(paste0("Showing top ", maxBars, " most changed OTUs ", comment), side=1, line=5, adj=0)
	# Axis and background lines and inner text
	# The vertical lines should match the axis tick marks, they should be behind the bars,
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
	if (length(infUp) > 0){
		text(x=0, y=bp[infUp,], labels="Inf", pos=4, xpd=TRUE)
	}
	if (length(infDown) > 0){
		text(x=0, y=bp[infDown,], labels="-Inf", pos=2, xpd=TRUE)
	}
	# plot the bars
	bp = barplot(toPlot$scaledFC, horiz=TRUE, width = barWidth, space = barSpace,
							 add=TRUE, col=toPlot$color, border="black")
	row.names(bp) = row.names(toPlot)
	# plot the stars
	if (!is.null(pvalStar) & !is.null(pvals)){
		starOTUs = intersect(names(pvals)[pvals <= pvalStar], row.names(toPlot))
		starColor = getProperty("r.colorHighlight", "red")
		starChar = "*"
		if ( length(starOTUs) > 0 ){
			starBarGap = 0.03 * par("usr")[2]
			xPlusGap = toPlot[starOTUs,"scaledFC"] + ifelse(toPlot[starOTUs,"foldChange"] > 1, starBarGap, (-1 * starBarGap))
			points(x=xPlusGap, y=bp[starOTUs,], pch=starChar, col=starColor, xpd=TRUE)
		}
		mtext(paste0("(", starChar, ") p-value <= ", pvalStar), side=3, line=0, adj=0)
	}
	# done
	return(TRUE)
	}

main <- function(){
	# get config option for pvalStar, pvalIncludeBar, maxBars, userOTUs, 
	pvalStar = getProperty("r.pvalCutoff", 0.05)
	pvalIncludeBar = getProperty("r.FCplot.pvalIncludeBar", 1)
	maxBars = getProperty("r.FCplot.maxBars", 40)
	userOTUs = getProperty("r.FCplot.userOTUs", NULL)
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
		if( doDebug() ){ print( paste0("otuTable has ", nrow(otuTable), " rows and ", ncol(otuTable), " columns."))}
		lastOtuCol = ncol(otuTable) - getProperty("internal.numMetaCols")
		otuTable = otuTable[1:lastOtuCol]
		#
		# # Might undo log-scale to get fold change values, then optionally scale them in some way.
		# logbase = getProperty("report.logBase")
		# if (!is.null(logbase)){
		# 	otuTable = logbase^otuTable
		# }
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
			par(oma=c(0,0,0,0)) # this is changed within the plot function, so reset before each plot
			# make plot
			tryCatch(expr={
				if( doDebug() ) print( paste("Calling addFoldChangePlot for otuLevel:", otuLevel, " and binary attribute:", biAtt) )
				complete = addFoldChangePlot(numGroupName=names(splitOTU)[2], denGroupName=names(splitOTU)[1], 
													numGroupVals=splitOTU[[2]], denGroupVals=splitOTU[[1]], title=biAtt,
													pvals = pvals, 
													pvalIncludeBar = pvalIncludeBar,
													pvalStar = pvalStar,
													maxBars = maxBars,
													userOTUs = userOTUs)
				if (complete) {
					print( paste("Calling addFoldChangePlot for otuLevel:", otuLevel, " and binary attribute:", biAtt))
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



