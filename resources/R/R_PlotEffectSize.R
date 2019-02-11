# Module script for: biolockj.module.report.r.R_PlotEffectSize

### assumes library(BioLockJ_Lib.R)

### NOTE: The header printed on the reference table serves as built-in documentation for this module. 
# See calcBarSizes

### custom config options:
# r_PlotEffectSize.parametricPval: Y/N should the parametric (vs the nonParametric) pvalue be used
# r_PlotEffectSize.useAdjustedPvals=
# r_PlotEffectSize.excludePvalAbove=
# r_PlotEffectSize.taxa=
# r_PlotEffectSize.maxNumTaxa=
# r_PlotEffectSize.cohensD=Y
# r_PlotEffectSize.rSquared=Y
# r_PlotEffectSize.foldChange=N
### also uses:
# r.pvalCutoff
# R_internal.numMetaCols
# internal.parserModule # if doing fold change
###

getMetaData() <- function(){
	return( NULL )
}

# The main method is designed to integrate this module with BiolockJ.  
# It handles pulling data from other modules and options from the BiolockJ properties.
main <- function(){
	# get config option for pvalStar, pvalIncludeBar, maxBars, userOTUs, 
	useParametric = getProperty("r_PlotEffectSize.parametricPval", FALSE)
	useAdjustedPs = getProperty("r_PlotEffectSize.useAdjustedPvals", FALSE)
	pvalStar = getProperty("r.pvalCutoff", 0.05)
	pvalIncludeBar = getProperty("r_PlotEffectSize.excludePvalAbove", 1)
	maxBars = getProperty("r_PlotEffectSize.maxNumTaxa", 40)
	userOTUs = getProperty("r_PlotEffectSize.taxa", NULL)
	doFoldChange = getProperty("r_PlotEffectSize.foldChange", FALSE) 
	doCohensD = getProperty("r_PlotEffectSize.cohensD", FALSE) 
	doRSquared = getProperty("r_PlotEffectSize.rSquared", FALSE) 
	
	# get metadata
	meta = getMetaData()
	
	for (level in taxaLevels() ) {
		if( doDebug() ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildEffectSizePlots_", level, ".log") ) )
		logInfo( "Processing level", level )
		# make a new pdf output file, specify page size
		outFileName = getPath( file.path(getModuleDir(), "output"), paste0(level, "_OTU-EffectSizePlots.pdf") )
		logInfo( "Creating file", outFileName )
		height=10
		pdf(file=outFileName, paper="letter", width=7.5, height=height, onefile=TRUE)
		par(mar=c(6, 5, 2, 5), oma=c(0,0,0,0))
		p = par(no.readonly = TRUE)
		# use this to reset par to the values it has as of right now
		resetPar <- function(){ 
			par(p)
		}

		# get normalized OTU vals
		countMetaTable = getCountMetaTable( level )
		if( is.null( countMetaTable ) ) { next }
		
		lastCountCol = ncol(countMetaTable) - numMetaCols()
		countMetaTable = countMetaTable[1:lastCountCol]
		logInfo( c("countMetaTable has", nrow(countMetaTable), "rows and", ncol(countMetaTable), "columns.") )
		
		# get pvals from calc stats
		pvalTable = getStatsTable( level, useParametric, useAdjustedPs )
		logInfo( c( "pvalTable has", nrow(pvalTable), "rows and", ncol(pvalTable), "columns." ) )
		
		# get r-squared values from calc stats
		if (doRSquared){
			r2Table = getStatsTable( level )
			logInfo( c( "r-squared table has", nrow(r2Table), "rows and", ncol(r2Table), "columns" ) )
		}
		 
		if ( doCohensD ){
			logInfo( c( "Preparing effect size plot for each of", length(getBinaryFields()), "report fields." ) )
		}
		
		# Get the raw counts from the parser and calc simple relative abundance---only if a parser module is given.
		# Don't bother with this unless we will use it, ie: there are binary attributes and we plan to do fold change
		if (!is.null(getProperty("internal.parserModule")) & length(getBinaryFields()) > 0 & doFoldChange){
			logInfo( "Preparing fold change plot for each of", length(getBinaryFields()), "report fields." ) )
			logInfo( "retrieving raw counts..." )
			relAbundance = normalize(readRawCounts(level)[[1]])
		}else{
			relAbundance=NULL
		}
		
		logInfo( c( "Preparing plot for each of", length(getReportFields()), "report fields. ", length(getBinaryFields()), "are binary attributes." ) )
		if (length(getReportFields()) == 0){
			plotPlainText( "No reportable fields." )
		}
		for (reportField in getReportFields()){
			isBinaryAtt = reportField %in% getBinaryFields()
			logInfo( "Processing report field", c( reportField, ", which", ifelse(isBinaryAtt, "is", "is not"), "a binary attribute." ) )

			pvals = pvalTable[,reportField]
			names(pvals) = row.names(pvalTable)
			
			# rSquared piggy-backs on effects size for selection and ordering, 
			# so IF both are plotted, they are ploted in the same order.
			# Even if it is not a binary attribute, the normalizedPvals should have AT LEAST 2 tables
			if (doCohensD | doRSquared){ 
	
				r2vals=r2Table[,reportField]
				names(r2vals) = row.names(r2Table)
				normalizedPvals = split(countMetaTable[row.names(meta),], f=meta[,reportField])

				saveRefTable = NULL
				if (doCohensD & isBinaryAtt){
					saveRefTable=getPath( file.path(getModuleDir(), "temp"), paste(level, reportField, "effectSize.tsv", sep="_") )
				}

				logInfo( "CohensD", c( "Calling calcBarSizes for level:", level, "and binary attribute:", reportField ) )
				calculations = calcBarSizes( c("CohensD","rSquared"), r2vals, normalizedPvals[[2]], normalizedPvals[[1]],
					names(normalizedPvals)[2], names(normalizedPvals)[1], pvals, pvalIncludeBar, userOTUs, maxBars,
					orderByColumn=ifelse(doCohensD & isBinaryAtt, "CohensD", "rSquared"), saveRefTable)
				#
				if (doRSquared){ # does not need to be a binary attribute
					resetPar()
					complete = drawPlot(toPlot=calculations[["toPlot"]][,c("pvalue","rSquared")], 
						barSizeColumn="rSquared",
															xAxisLab="r-squared", title=reportField,
															pvalStar=pvalStar, starColor=getProperty("r.colorHighlight", "red"), 
															comments=calculations[["comments"]][c(1,3)])
					if (complete) {
						logInfo( c("Completed r-squared plot for level:", level, "and report field:", reportField) )
					}
				}
				#
				if (doCohensD & isBinaryAtt){
					logInfo( c( "Effect size: Calling drawPlot for level:", level,  "and binary attribute:", reportField ) )
					resetPar()
					complete = drawPlot(toPlot=calculations[["toPlot"]], barSizeColumn="CohensD",
							xAxisLab="Effect Size (Cohen's d)", title=reportField,
							pvalStar=pvalStar, starColor=getProperty("r.colorHighlight", "red"),
							xAxisLab2 = calculations[["xAxisLab2"]],
							comments=calculations[["comments"]][c(1,2)])
					if (complete) {
						logInfo( c( "Completed CohensD plot for level:", level, "and binary attribute:", reportField ) )
					}
				}
				
			}
			#
			if (doFoldChange & !is.null(relAbundance)){
				# relAbundance vals
				splitRelAbund = split(relAbundance[row.names(meta),], f=meta[,reportField])
				logInfo( c ("Fold change: Calling calcBarSizes for level:", level, "and binary attribute:", reportField ) )
				calculations = calcBarSizes(effectType = "foldChange",
																		numGroupVals=splitRelAbund[[2]], denGroupVals=splitRelAbund[[1]],
																		numGroupName=names(splitRelAbund)[2], denGroupName=names(splitRelAbund)[1],
																		pvals=pvals, pvalIncludeBar=pvalIncludeBar, userOTUs=userOTUs, maxBars=maxBars,
																		saveRefTable=getPath( file.path(getModuleDir(), "temp"), paste(level, biAtt,"foldChange.tsv", sep="_") ) )
				logInfo( c( "Fold change: Calling drawPlot for level:", level, "and binary attribute:", reportField ) )
				resetPar()
				complete = drawPlot(toPlot=calculations[["toPlot"]], barSizeColumn="foldChange",
														xAxisLab="Fold Change", title=biAtt,
														pvalStar=pvalStar, starColor=getProperty("r.colorHighlight", "red"),
														xAxisLab2 = calculations[["xAxisLab2"]],
														comments=calculations[["comments"]])
				if (complete) {
					logInfo( c( "Completed effect size plot for level:", level, "and binary attribute:", reportField ) )
				}
				
			}
		}
		dev.off()
		logInfo( "Saved plot(s) to file", outFileName )
		if( doDebug() ) sink()
	}
}
	

# Read the counts from the parser module; this requires the BiolockJ pipeline environment.
# returns a list with each level as an element.
readRawCounts <- function(){
	parserModule = getProperty("internal.parserModule")
	if (is.null(parserModule)){
		stop("No parser module found in master properties (internal.parserModule).")
		}
	parserOutput = file.path(parserModule, "output")
	counts = list()
	names(counts) = taxaLevels()
	for (level in taxaLevels() ){
		fileName = dir(path=parserOutput, pattern=level)
		if (length(fileName) > 1){
			stop(paste("Ambiguous file for", level))
		}
		if (length(fileName) < 1){
			logInfo("No file found for", level)
		}
		counts[level] = read.table( inputFile, check.names=FALSE, na.strings=getProperty("metadata.nullValue", "NA"), 
			comment.char=getProperty("metadata.commentChar", ""), header=TRUE, sep="\t", row.names=1)
	}
	return(counts)
}


# normalize otu counts by simple relative abundance
normalize <- function(countMetaTable){
	# countMetaTable - data frame with a row for each sample and a column for each OTU
	normFactor = rowSums(countMetaTable)
	normed = countMetaTable/normFactor
	return(normed)
}

# returns a list of 2:
#  toPlot - a dataframe with values to plot and info for each bar
#  comment - a string(s) that should be included in the plot to inform the user about this step
calcBarSizes <- function(numGroupVals, denGroupVals, numGroupName, denGroupName,
						effectType="CohensD", r2vals=NULL, pvals=NULL, pvalIncludeBar=0.05, maxBars=30, userOTUs=NULL,
						saveRefTable=NULL, scale.fun="log2", orderByColumn=effectType[1]){
	# numGroupVals, denGroupVals - each a data frame, where OTUs are column names and rows are samples
	##   these should have different row names (samples are from different groups) but matching column names.
	# numGroupName, denGroupName - Strings used in comments (and later in plot)
	# effectType - what type of impact should be calculated
	##   CohensD (default) is the difference of the means devided by the pooled standard deviation
	##   foldChange is the ratio of the means
	##   rSquared is taken from the calculate stats module
	# r2vals - (optional) named vector of r-squared values from a statistical test to use for bar sizes.
	# pvals - named vector of pvalues, taken from calcStats (which test is configurable).
	##   names of pvals are OTU's, and should match num(den)GroupVals column names.
	# pvalIncludeBar - OTUs that do not meet this value are not plotted (if pvals supplied)
	# maxBars - int, maximum number of bars to plot
	# userOTUs - string vector, set of OTUs of interest to the user.  If present, this overrides pvals.
	##   userOTUs may include OTUs that are not in the tables, only the intersect of both tables and userOTUs will be used.
	# saveRefTable - file name to save a reference table corresponding to the plot
	# scale.fun - string (with quotes) giving the name of the function to use to scale
	##   the bar values values: probably log2 or log10.
	# orderByColumn - the name of the column to use in ordering the final output rows.
	#
	# Keep a running header of documentation
	header = "Plot Reference"
	# Select viable OTUs to plot
	viableOTUs = selectViableOTUs(group1=names(numGroupVals), group2= names(denGroupVals), 
																pvals=pvals, pvalIncludeBar=pvalIncludeBar, userOTUs=userOTUs)
	plotOTUs = viableOTUs[["plotOTUs"]]
	comments = viableOTUs[["comment"]]
	numGroupVals = numGroupVals[plotOTUs]
	denGroupVals = denGroupVals[plotOTUs]
	#
	# assemble data frame of plot values.
	numGroupN = sapply(numGroupVals, function(x){sum(!is.na(x))})
	denGroupN = sapply(denGroupVals, function(x){sum(!is.na(x))})
	numMeans = colMeans(numGroupVals, na.rm=TRUE)
	denMeans = colMeans(denGroupVals, na.rm=TRUE)
	toPlot = data.frame(OTU=plotOTUs, row.names=plotOTUs,
											numMeans = numMeans, denMeans = denMeans,
											infUp = numMeans > 0 & denMeans == 0, #In the table, these are Inf
											infDown = numMeans == 0 & denMeans > 0) #In the table, these are 0
	header = c(header, "<group name>.mean: the mean value for each group.")
	header = c(header, paste("infUp: was the OTU flagged for having all-0-counts only in the", denGroupName, "group."))
	header = c(header, paste("infDown: was the OTU flagged for having all-0-counts only in the", numGroupName, "group."))
	if (!is.null(pvals)){
		toPlot$pvalue = pvals[row.names(toPlot)]
		header = c(header, paste0("pvalue: the p-value used to determine if the OTU was included (if under ", pvalIncludeBar,
															") and if OTU got a star (if under <pvalStar>thresholds controlled by r_PlotEffectSize.excludePvalAbove and r.pvalCutoff properties respectively.",
															" See also: r_PlotEffectSize.parametricPval property"))
	}
	xAxisLab2="" #just make sure it exists; it is defined in if statements
	if ("CohensD" %in% effectType){
		numGroupSD = sapply(numGroupVals, sd, na.rm=TRUE)
		denGroupSD = sapply(denGroupVals, sd, na.rm=TRUE)
		pooledSD = mapply(calc2GroupPooledSD, 
											group1.n=numGroupN, group2.n=denGroupN, 
											group1.sd=numGroupSD, group2.sd=denGroupSD, 
											USE.NAMES = TRUE)
		toPlot$CohensD = (denMeans - numMeans) / pooledSD
		xAxisLab2 = paste0("difference of the means, ", denGroupName, " (n=", max(numGroupN), ") minus ", numGroupName, " (n=", max(denGroupN), "), over pooled sd")
		if (length(effectType) > 1){
			comments = c(comments, paste("Cohen's d is", xAxisLab2))
		}
		header = c(header, paste("CohensD:", xAxisLab2))
	}
	if ("foldChange" %in% effectType){
		toPlot$foldChange = numMeans / denMeans
		toPlot$scaledFC = do.call(scale.fun, list(x=toPlot$foldChange))
		xAxisLab2 = paste0(numGroupName, " (n=", max(numGroupN), ") relative to ", denGroupName, " (n=", max(denGroupN), ")")
		if (length(effectType) > 1){
			comments = c(comments, paste("Fold change is", xAxisLab2, "on a", scale.fun, "scale."))
		}
		header = c(header, paste("foldChange:", xAxisLab2))
		header = c(header, paste("scaledFC:", xAxisLab2, "on a", scale.fun, "scale."))
	}
	if ("rSquared" %in% effectType){
		if (is.null(r2vals)){
			stop("r2vals must be supplied.")
		}else{
			toPlot$rSquared = r2vals[row.names(toPlot)]
			xAxisLab2 = ""
			r2comment = "r-squared values are taken from the CalculateStats module."
			if (length(effectType) > 1){
				comments = c(comments, r2comment)
			}
			header = c(header, paste("rSquared:", r2comment))
		}
	}
	#
	# select top [maxBars] most changed OTUs
	# cases where one group is all-zeros is treated at most changed
	toPlot = toPlot[order(abs(toPlot[,orderByColumn]), decreasing = T),] #highest abs on top
	maxBars = min(c(maxBars, nrow(toPlot)))
	toPlot$plotPriority = 1:nrow(toPlot)
	header = c(header, paste0('plotPriority: the rank of this OTU when determineing the "most changed" using abs(',orderByColumn,'); number of OTUs plotted can configured with r_PlotEffectSize.maxNumTaxa or over-riden using r_PlotEffectSize.taxa.'))
	toPlot$includeInPlot = toPlot$plotPriority <= maxBars
	comments[1] = paste0("Showing top ", maxBars, " most changed OTUs ", viableOTUs[["comment"]])
	header = c(header, "includeInPlot: will this otu be included in the plot.")
	#
	# order OTUs to plot
	ordNames = row.names(toPlot)[order(toPlot[,orderByColumn])]
	toPlot = toPlot[ordNames,] #lowest values at top, barplot plots from bottom
	#
	# save a table the user can reference
	if (!is.null(saveRefTable)){
		toPrint=toPlot
		names(toPrint)[2:3] = paste(c(numGroupName, denGroupName), "mean", sep=".")
		header = c(header, "plot.location: vertical location of the bar in the plot")
		toPrint$plot.location = NA
		toPrint[1:sum(toPrint$includeInPlot),"plot.location"] = sum(toPrint$includeInPlot):1
		toPrint = toPrint[order(toPrint$plot.location),]
		header = paste("#", header)
		writeLines(header, con=saveRefTable)
		suppressWarnings(write.table(toPrint, file=saveRefTable, quote=FALSE, sep="\t", row.names = FALSE, append = TRUE))
		logInfo( "Saved reference table to", saveRefTable )
	}
	#
	# get rid of the rows that will not be plotted
	toPlot = toPlot[toPlot$includeInPlot,]
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


calcPooledSD <- function(group.n, group.sd){
	# group.n - the number of samples in each group
	# group.sh - the within-group standard deviation for each group
	# formula taken from https://en.wikipedia.org/wiki/Pooled_variance#Computation
	group.var = group.sd^2
	pooled.var = sum( (group.n - 1) * group.var) / sum(group.n - 1)
	return(sqrt(pooled.var))
}

calc2GroupPooledSD <- function(group1.n, group2.n, group1.sd, group2.sd){
	return(calcPooledSD(group.n=c(group1.n, group2.n), group.sd=c(group1.sd, group2.sd)))
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
			logInfo( "Argument fixedBarHeightInches should be a numeric value >= 0." )
		}
		if (inchesToRemove > 0){
			mais = par("mai")
			mais[1] = mais[1] + inchesToRemove
			par(mai=mais)
		}else{
			logInfo( c( "Not enough space in plot for", nrow(toPlot), "bars with", fixedBarHeightInches, 
				" for each bar. Bar widths will be set to fit the space.") )
		}
	}
	
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
	# logInfo(c("xmin, xmax:", xmin, ",", xmax))
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
	if (!is.null(pvalStar) & !is.null(toPlot$pvalue)){
		starOTUs = row.names(toPlot)[toPlot$pvalue <= pvalStar]
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



