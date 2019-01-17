# Module script for: biolockj.module.r.BuildPvalHistograms

# save table output
# save favMetacols
# color by test type

# Add one histogram to the report
addHistogram <- function( v, title, xLabel, size, pvalCutoff=NULL, col="dodgerblue2" ) {
	if ( !all(is.nan( v )) && !all(is.na( v )) ) {
		hist( v, breaks=20, xlab=xLabel, main=title, cex.main=size, col=col, xlim=c(0,1))
	}
	if (!is.null(pvalCutoff)){
		abline(v=pvalCutoff, col=gray(.5), lty=2)
		mtext(at=pvalCutoff, side=3, text=pvalCutoff, col=gray(.5))
	}
}

# return the fraction of pvalues that are under a threshold, don't count NAs
calcSigFraction <- function(pvals, pvalCutoff){
	belowCutoff = sum(pvals < pvalCutoff, na.rm=TRUE)
	total = sum(!is.na(pvals))
	return( belowCutoff / total )
}

# Return main.cex parameter between 0.65 and 1.2 based on longest report field name
getCexMain<- function( labels=NULL ) {
	cexMax = 1.2
	cexMin = 0.65
	
	maxLabelSize = 1
	for( i in 1:length(labels) ) {
		if( nchar(labels[i]) > maxLabelSize ) maxLabelSize = nchar(labels[i])
	}
	
	size = 25/maxLabelSize
	if ( size > cexMax ) size = cexMax
	if ( size < cexMin ) size = cexMin
	
	return( size )
}


# Return name of statistical test used to generate P-Values
getTestName <- function( attName, isParametric ) {
	if( attName %in% getBinaryFields() && isParametric ) return ( "T-Test" )
	if( attName %in% getBinaryFields() && !isParametric ) return ( "Wilcox" )
	if( attName %in% getNominalFields() && isParametric ) return ( "ANOVA" )
	if( attName %in% getNominalFields() && !isParametric ) return ( "Kruskal" )
	if( attName %in% getNumericFields() && isParametric ) return ( "Pearson" )
	if( attName %in% getNumericFields() && !isParametric ) return ( "Kendall" )
}


# Return a color for each type of statistical test
getTestColor <- function(testName){
	colors = c("coral", "dodgerblue2", "darkgoldenrod1", "cornflowerblue", "tan1", "aquamarine3")
	names(colors) = c("T-Test", "Wilcox", "ANOVA", "Kruskal", "Pearson", "Kendall")
	return(colors[testName])
}

# This graphic can be printed with the histograms, 
# used for documentation, or just as a reference check when chaning the colors.
printColorCode <- function(){
	colors = getTestColor(c("T-Test", "Wilcox", "ANOVA", "Kruskal", "Pearson", "Kendall"))
	plot(1,1, ylim=c(0,4), xlim=c(.5,2.5), type="n", axes=FALSE, xlab="", ylab="")
	title(main="Color Coding", line=0)
	symbols(x=rep(1:2,3), y=rep(3:1, each=2), rectangles = matrix(ncol=2,byrow=TRUE,data=rep(c(1,1),6)), fg=colors, bg=colors, inches=FALSE, add=TRUE)
	text(x=rep(1:2,3), y=rep(3:1, each=2), labels=names(colors))
	text(x=1, y=3.5, labels="parametric", pos=3)
	text(x=2, y=3.5, labels="non-parametric", pos=3)
}


# Main function generates reports for each each report.taxonomyLevels
# Each taxonomy report includes 2 histograms for each report field (1 parametric, 1 non-parametric) 
main <- function() {
	pvalCutoff = getProperty("r.pvalCutoff", 0.05)
	
	for( otuLevel in getProperty("report.taxonomyLevels") ) {
		if( doDebug() ) sink( file.path( getModuleDir(), "temp", paste0("debug_BuildPvalHistograms_", otuLevel, ".log") ) )
		pdf( getPath( file.path(getModuleDir(), "output"), paste0(otuLevel, "_histograms.pdf") ) )
		par( mfrow=c(2, 2), las=1, mar=c(5,4,5,1)+.1 )
		parInputFile = getPipelineFile( paste0( otuLevel, "_parametricPvals.tsv" ) )
		if( doDebug() ) print( paste( "parInputFile:", parInputFile ) )
		parStats = read.table( parInputFile, check.names=FALSE, header=TRUE, sep="\t", row.names=1 )
		nonParInputFile = getPipelineFile( paste0( otuLevel, "_nonParametricPvals.tsv" ) )
		if( doDebug() ) print( paste( "nonParInputFile:", nonParInputFile ) )
		nonParStats = read.table( nonParInputFile, check.names=FALSE, header=TRUE, sep="\t", row.names=1  )
		size = getCexMain( colnames(parStats) )
		if( doDebug() ) print( paste("size = getCexMain( colnames(parStats) ):", size ) )
		
		# create ranks table
		ranks = data.frame(AttributeName=names(parStats),
											 Parametric.FractionPvalsUnderCutoff=rep(NA, ncol(parStats)),
											 NonParametric.FractionPvalsUnderCutoff=rep(NA, ncol(nonParStats)))
		row.names(ranks) = ranks$AttributeName
		for( attName in names(parStats) ) {
			ranks[attName,"Parametric.FractionPvalsUnderCutoff"] = calcSigFraction(pvals=parStats[, attName], pvalCutoff)
			ranks[attName,"NonParametric.FractionPvalsUnderCutoff"] = calcSigFraction(pvals=nonParStats[, attName], pvalCutoff)
		}
		# order attributes based on the fraction of tests with a p-value below <pvalCutoff>
		orderBy = apply(ranks[2:3],1, max, na.rm=TRUE)
		ranks = ranks[order(orderBy, decreasing=TRUE),]
		fname = getPath( file.path(getModuleDir(), "temp"), paste0(otuLevel, "_fractionBelow-", pvalCutoff, ".tsv") )
		write.table(ranks, file=fname, sep="\t", quote=FALSE, row.names=FALSE)
		
		# plot histograms in the same order they have in the ranks table
		for( attName in ranks$AttributeName ) {
			if( doDebug() ) print( paste("processing attribute:", attName) )
			stopifnot( attName %in% names(nonParStats) & attName %in% names(parStats) )
			# parametric
			parTestName = getTestName(attName, TRUE)
			xLabelPar = paste( parTestName, "P-Values" )
			addHistogram( v=parStats[, attName], title="",
										xLabel=xLabelPar, size=size, pvalCutoff=pvalCutoff, 
										col=getTestColor(parTestName) )
			title(main="Parametric", line=1.5)
			# nonParametric
			nonParTestName = getTestName(attName, FALSE)
			xLabelNonPar = paste( nonParTestName, "P-Values" )
			addHistogram( v=nonParStats[, attName], title="",
										xLabel=xLabelNonPar, size=size, pvalCutoff=pvalCutoff, 
										col=getTestColor(nonParTestName) )
			title(main="Non-Parametric", line=1.5)
			# shared title
			plotPointPerInch = (par("usr")[2] - par("usr")[1]) / par("pin")[1]
			shiftByPoints = par("mai")[2] * plotPointPerInch
			centerAt = par("usr")[1] - shiftByPoints
			mtext(text=attName, side=3, line=2.5, at=centerAt, adj=.5, xpd=NA, font=par("font.main"), cex=par("cex.main"))
		}
		# at the end, add the color code reference
		printColorCode()
		plotPlainText(paste0("Histograms are ordered by \nthe fraction of tests below ", 
												 pvalCutoff, "\nin the parametric or non-parametric test, \nwhichever was greater."))
		dev.off()
		
		if( doDebug() ) sink()
	}
}

