# Module script for: biolockj.module.r.BuildFoldChangePlots

### assumes library(BioLockJ_Lib.R)

### custom config options:
# r.FCplot.pvalType= # not yet supported
# r.FCplot.userOTUs=
# r.FCplot.maxBars=
###

addFoldChangePlot <- function(numGroupName, denGroupName, 
															numGroupVals, denGroupVals, title,
															pvals=NULL, pvalThresh=0.05, maxBars=30, userOTUs=NULL,
															numGroupColor="darkorange2", denGroupColor="burlywood1",
															centerAt0=TRUE, scale.fun="log2"){
	# numGroupName, denGroupName - Strings used in plot
	# numGroupVals, denGroupVals - each a data frame, where OTUs are column names and rows are samples
	##   these should have different row names (samples are from different groups) but matching column names.
	# title - string, binary attribute being plotted
	# pvals - named vector of pvalues, taken from calcStats (which test is configurable).
	##   names of pvals are OTU's, and should match num(den)GroupVals column names.
	# pvalThresh - OTUs that do not meet this value are not plotted (if pvals supplied)
	# maxBars - int, maximum number of bars to plot
	# userOTUs - string vector, set of OTUs of interest to the user.  If present, this overrides pvals.
	##   userOTUs may include OTUs that are not in the tables, only the intersect of both tables and userOTUs will be used.
	# numGroupColor, denGroupColor - color to use to fill bar for OTUs with neg/pos fold change
	# centerAt0 - boolean, should the xmin and xmax be set to the same value?
	# scale.fun - string (with quotes) giving the name of the function to use to scale
	##   the fold change values: probably log2 or log10.
	#
	# Select viable OTUs to plot
	sharedOTUs = intersect(names(numGroupVals), names(denGroupVals))
	if (!is.null(pvals) & is.null(userOTUs)){
		sigOTUs = names(pvals)[pvals <= pvalThresh]
		if (length(sigOTUs) == 0){
			stop(paste("Stop Plotting: Provided", length(pvals), "pvalues, \nwith", length(sigOTUs), "below the provided threshold:", pvalThresh))
		}
		plotOTUs = intersect(sigOTUs, sharedOTUs)
		comment=paste0("out of ", length(plotOTUs), " OTUs with p-value <= ", pvalThresh, ".")
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
	toPlot = data.frame(OTU=names(numGroupVals),
											foldChange = colMeans(numGroupVals) / colMeans(denGroupVals))
	toPlot$scaledFC = do.call(scale.fun, list(x=toPlot$foldChange))
	toPlot$color = NA
	toPlot$color[toPlot$foldChange > 1] = numGroupColor
	toPlot$color[toPlot$foldChange < 1] = denGroupColor
	#
	# select top [maxBars] most changed OTUs
	toPlot = toPlot[order(abs(toPlot$scaledFC), decreasing = T),] #highest abs on top
	maxBars = min(c(maxBars, nrow(toPlot)))
	toPlot = toPlot[1:maxBars,]
	#
	# order OTUs to plot
	toPlot = toPlot[order(toPlot$scaledFC),] #lowest values at top, barplot plots from bottom
	#
	# plot
	xmin = min(toPlot$scaledFC)
	xmax = max(toPlot$scaledFC)
	left = which(toPlot$scaledFC > 0) #where text goes on the left
	right = which(toPlot$scaledFC < 0)
	if (centerAt0){
		width = max(abs(c(xmin, xmax)))
		xmin = -width
		xmax = width
	}
	bp = barplot(toPlot$scaledFC, horiz=TRUE, plot=FALSE) # this one is not plotted, its just a reference
	# plot area and outer text
	plot(x=0, y=1, type='n', xlim=c(xmin, xmax), 
			 ylim=c(.9, max(bp)), # use .9 instead of 0, its an inelligant hack to remove added white space.
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
	# plot the bars
	bp = barplot(toPlot$scaledFC, horiz=TRUE, add=TRUE, col=toPlot$color, border="black")
	return(TRUE)
	}

main <- function(){
	# get config option for pvalThresh, maxBars, userOTUs, 
	pvalThresh = getProperty("r.pvalCutoff", 0.05)
	maxBars = getProperty("r.FCplot.maxBars", 45)
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
		adjNonParInputFile = getPipelineFile( paste0(otuLevel, "_adjNonParPvals.tsv") )
		if( doDebug() ) print( paste( "adjNonParInputFile:", adjNonParInputFile ) )
		nonParStats = read.table( adjNonParInputFile, check.names=FALSE, header=TRUE, sep="\t", row.names = 1)
		if( doDebug() ){ print( paste0("nonParStats has ", nrow(nonParStats), " rows and ", ncol(nonParStats), " columns."))}
		# 
		if( doDebug() ) print( paste( "Prepareing plot for each of ", length(getBinaryFields()), "binary attributes." ) )
		if (length(getBinaryFields()) == 0){
			plotPlainText("No binary attributes.")
		}
		for (biAtt in getBinaryFields()){
			if( doDebug() ) print( paste( "processing binary attribute:", biAtt ) )
			splitOTU = split(otuTable[row.names(meta),], f=meta[,biAtt])
			pvals = nonParStats[,biAtt]
			names(pvals) = row.names(nonParStats)
			# make plot
			tryCatch(expr={
				if( doDebug() ) print( paste("Calling addFoldChangePlot for otuLevel:", otuLevel, " and binary attribute:", biAtt) )
				complete = addFoldChangePlot(numGroupName=names(splitOTU)[2], denGroupName=names(splitOTU)[1], 
													numGroupVals=splitOTU[[2]], denGroupVals=splitOTU[[1]], title=biAtt,
													pvals = pvals, 
													pvalThresh = pvalThresh,
													maxBars = maxBars,
													userOTUs = userOTUs)
				if (complete) {
					print( paste("Calling addFoldChangePlot for otuLevel:", otuLevel, " and binary attribute:", biAtt))
				}
			}, error= function(err) {
				if( doDebug() ){print(err)}
				err=gsub("Error.*Stop Plotting:", "", err)
				plotPlainText(paste0("Failed to create plot for taxonomy level: ", otuLevel, 
														 " \nusing attribute: ", biAtt, " \n", err))
				})
		}
		dev.off()
		print( paste( "Saved fold change plot to file:", outFileName ) )
		if( doDebug() ) sink()
	}
}



