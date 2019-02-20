# BioLockJ_Lib.R contains the library of functions shared by multiple BioLockJ R script modules. 

# Add value to vector v and assign name 
addNamedVectorElement <- function( v, name, value ) {
	v[length(v) + 1] = value
	names(v)[length(v)] = name
	return( v )
}

# Add a footer with title, level and page number
addPageFooter <- function (level, pageNum, multiPageSet=NULL){
	if (!is.null(multiPageSet)){
		pageString = paste(multiPageSet, pageNum, sep="-")
	}else{
		pageString = pageNum
	}
	pageNumberText = paste(displayLevel(level), paste("page", pageString), sep=" - ")
	mtext(pageNumberText, side=1, outer=TRUE, line=0, adj = 1)
}

# Add a page title
addPageTitle <- function (main, level=NULL, line=2){
	mtext(main, side=3, outer = TRUE, font=par("font.main"), cex=par("cex.main"), line=line)
	if (!is.null(level)){
		titlePart2 = displayLevel( level )
		title(main=titlePart2, outer = TRUE, line=(line-1))
	}
}

# Return P value formated with sprintf as defined in MASTER Config r.pValFormat, otherwise use %1.2g default
displayCalc <- function( pval ) {
	return( paste( sprintf(getProperty("r.pValFormat", "%1.2g"), pval) ) )
}

displayLevel <- function(level){
	return( str_to_title( paste(level,"Level") ) )
}

# Return TRUE if BioLock property r.debug=Y, otherwise return FALSE
doDebug <- function() {
	return( getProperty( "r.debug", FALSE ) )
}

# Return vector of binary fields or an empty vector
getBinaryFields <- function() {
	return( getProperty("R_internal.binaryFields", vector( mode="character" ) ) )
}

# Return countTable column indexes for the given colNames
getColIndexes <- function( countTable, colNames ) {
	cols = vector( mode="integer" )
	if( length(colNames) > 0 ) {
		for( i in 1:length(colNames) ) {
			cols[i] = grep(TRUE, colnames(countTable)==colNames[i])
		}
	}
	return( cols )
}

# Return r.colorHighlight if any of the input values meet the r.pvalCutoff, otherwise return r.colorBase
getColor <- function( v ) {
	for( i in 1:length(v) ) {
		if( grepl("e", v[i]) || !is.na(v[i]) && !is.nan(v[i]) && ( v[i] <= getProperty("r.pvalCutoff", 0.05) ) ) {
			return( getProperty("r.colorHighlight", "black") )
		} 
	}
	return( getProperty("r.colorBase", "black") )
}

# Return n colors using the palette defined in the MASTER Config
# If the r.colorPalette property is a palette name rather than a list of colors
# rearrange the colors so that very similar colors are not less likley to 
# be next to each other, thus less likely to be the alternatives in the same category.
getColors <- function( n, reorder=TRUE) {
	palette = getProperty("r.colorPalette", "npg")
	colors = get_palette( palette, n )
	
	if (length(palette) == 1 & reorder){
		flipFrom = (1:length(colors))[(1:length(colors)%%2)==0]
		flipTo = flipFrom[length(flipFrom):1]
		colors[flipTo] = colors[flipFrom]
	}
	return( colors )
}

# Select colors for each category so each category is different even across different metadata fields.
# Returns a named list (named for meta data columns) of named vecotors (named for levels in column)
getColorsByCategory <- function( metaTable ){
	categoricals = c(getBinaryFields(), getNominalFields())
	metaTable = metaTable[names(metaTable) %in% categoricals]
	if (ncol(metaTable) < 1){
		logInfo( "No categorical metadata fields.  Returning null." )
		return(NULL)
	}
	numBoxes = sapply(metaTable, function(x){length(levels(as.factor(x)))})
	boxColors = getColors( sum(numBoxes))
	logInfo ( paste( "Selected", length(boxColors), "colors to describe", ncol(metaTable), "categorical variables." ) )
	f = mapply(x=names(numBoxes), each=numBoxes, rep, SIMPLIFY = FALSE)
	metaColColors = split(boxColors, f=do.call(c, f))
	for (field in names(metaColColors)){
		names(metaColColors[[field]]) = levels(as.factor(metaTable[,field]))
	}
	return(metaColColors)
}

# Parse MASTER config for property value, if undefined return default defaultVal
# save all properties in propCache upon 1st request
getConfig <- function( name, defaultVal=NULL ) {
	if( is.null( propCache ) ) {
		propCache = read.properties( getMasterConfigFile() )
	}
	
	prop = propCache[[ name ]]
	
	if( is.null( prop ) ) {
		return( defaultVal )
	}
	
	if( str_trim( prop ) == "Y" ) {
		return( TRUE )
	}
	if( str_trim( prop ) == "N" ) {
		return( FALSE )
	}
	if( !is.na( as.numeric( prop ) ) && grepl( ",", prop ) ) {
		return( as.numeric( unlist( strsplit( prop, "," ) ) ) )
	}
	if( is.character( prop ) && grepl( ",", prop ) ) {
		return( str_trim( unlist( strsplit( prop, "," ) ) ) )
	}
	if( !is.na( as.numeric( prop ) ) ) {
		return( as.numeric( prop ) )
	}
	
	return( str_trim( prop ) )
}

# Return the data columns (no metadata)
getCountTable <- function( level ){
	fullTable = getCountMetaTable( level )
	if( is.null( fullTable ) ) return( NULL )
	lastCountCol = ncol(fullTable) - numMetaCols()
	return ( fullTable[1:lastCountCol] )
}

# Returns BioLockJ generated Count + Metadata Table with a standard format:
# 1. 1st column contains Sample IDs
# 2. Next group of columns contain numeric count data (derived from sample analysis)
# 3. Last group of columns contain the metadata columns ( call numMetaCols() to find out how many )
getCountMetaTable <- function( level=NULL ) {
	countMetaFile = pipelineFile( paste0( level, "_metaMerged.tsv$" ) )
	if( is.null( countMetaFile )  ) {
		logInfo( c( "BioLockJ_Lib.R getCountMetaTable(", level, ") found none!" ) )
		return( NULL )
	}
	
	countMetaTable = readBljTable( countMetaFile )
	if( nrow( countMetaTable ) == 0 ) {
		logInfo( c( "BioLockJ_Lib.R getCountMetaTable(", level, ") returned an empty table with header row:", colnames( countMetaTable ) ) )
		return( NULL )
	}
	
	logInfo( "Read count table", countMetaFile )
	return( countMetaTable )
}

# Return list, each record contains the count-data  associated with a unique value for the given nominal metadata field (metaCol)
getFactorGroups <- function( countMetaTable, metaCol, taxaCol ) {
	vals = list()
	options = levels( metaCol )
	for( i in 1:length(options) ) {
		vals[[i]] = countMetaTable[metaCol==options[i], taxaCol]
	}
	return( vals )
}

# Return the name of the R module level specific log file
getLogFile <- function( level ) {
	return( file.path( getTempDir(), paste0( moduleScriptName(), ".", level, ".log") ) )
}

# Return the name of the BioLockJ MASTER Config file
getMasterConfigFile <- function() {
	testDir = dirname( getModuleScript() )
	propFile = vector( mode="character" )
	while( length( propFile ) == 0 && testDir != "/" ) {
		propFile = list.files( testDir, "MASTER.*.properties", full.names=TRUE )
		testDir = dirname( testDir )
	}
	if( length( propFile ) == 0 ) {
		stop( "MASTER property file not found!" )
	}
	return( propFile )
}

# Return a data frame of the metadata from a biolockj data table with merged metadata.
getMetaData <- function( level ){
	fullTable = getCountMetaTable( level )
	if( is.null( fullTable ) ) return( NULL )
	firstMetaCol = ncol(fullTable) - numMetaCols() + 1
	return( fullTable[firstMetaCol:ncol(fullTable)] )
}

# If downloaded with scp, all files share 1 directory, so return getPipelineDir() 
# Otherwise, script path like: piplineDir/moduleDir/script/MAIN*.R, so return moduleDir (the dir 2 levels above script)  
getModuleDir <- function() {
	if( getPipelineDir() == dirname( getModuleScript() ) ) {
		return( getPipelineDir() )
	}
	return( dirname( dirname( getModuleScript() ) ) )
}

# Return vector of nominal fields or an empty vector
getNominalFields <- function() {
	return( getProperty("R_internal.nominalFields", vector( mode="character" ) ) )
}

# Return vector of numeric fields or an empty vector
getNumericFields <- function() {
	return( getProperty("R_internal.numericFields", vector( mode="character" ) ) )
}

# Get the temp dir for the current module, if it does not exist, create it.
getOutputDir <- function(){
	path = file.path( file.path(getModuleDir(), "output") )
	if ( !dir.exists(path)){
		dir.create( path )
	}
	return( path )
}

# Return file path of file in rootDir, with the pipeline name appended as a prefix to name
getPath <- function( rootDir, name ) {
	return( file.path( rootDir, paste0( basename( getPipelineDir() ), "_", name ) ) )
}

# Return the pipeline root directory
getPipelineDir <- function() {
	return( dirname( getMasterConfigFile() ) )
}

# Return property value from MASTER Config file, otherwise return the defaultVal
getProperty <- function( name, defaultVal=NULL ) {
	return ( suppressWarnings( getConfig( name, defaultVal ) ) )
}

# Return vector that includs all binary, nominal, and numeric fields or an empty vector
getReportFields <- function() {
	return( c( getBinaryFields(), getNominalFields(), getNumericFields() ) )
}

# Return the most recent stats file at the given level based on the suffix returned by statsFileSuffix()
getStatsTable <- function( level, parametric=NULL, adjusted=TRUE ) {
	statsFile = pipelineFile( paste0( level, ".*", statsFileSuffix( parametric, adjusted ), "$" ) )
	if( is.null( statsFile )  ) {
		logInfo( paste0( "BioLockJ_Lib.R function --> getStatsTable( level=", level, 
										 ", parametric=", parametric, ", adjusted=", adjusted, " ) returned NULL" ) )
		return( NULL )
	}
	
	statsTable = read.table( statsFile, header=TRUE, sep="\t", row.names=1, check.names=FALSE )
	if( nrow( statsTable ) == 0 ) {
		logInfo( paste0( "BioLockJ_Lib.R function --> getStatsTable( level=", level, ", parametric=", parametric, ", adjusted=", adjusted, " ) returned an empty table with only the header row:", colnames( statsTable ) ) )
		return( NULL )
	}
	
	logInfo( "Read stats table", statsFile )
	return( statsTable )
}

# Get the temp dir for the current module, if it does not exist, create it.
getTempDir <- function(){
	path = file.path( file.path(getModuleDir(), "temp") )
	if ( !dir.exists(path)){
		dir.create( path )
	}
	return( path )
}

# Return the name of statistical test used to generate P-Values for a given attribute
# If returnColors==TRUE, then return a color used for color-coding this test
# or a named color vector if the specific attribute or isParametric is not given.
getTestName <- function( field=NULL, isParametric=c(TRUE, FALSE), returnColors=FALSE ) {
	testOptions = data.frame(
		testName = c("T-Test", "Wilcox", "ANOVA", "Kruskal", "Pearson", "Kendall"),
		fieldType = c("binary", "binary", "nominal", "nominal", "numeric", "numeric" ),
		isParametric = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE),
		color = c("coral", "dodgerblue2", "darkgoldenrod1", "cornflowerblue", "tan1", "aquamarine3"),
		stringsAsFactors = FALSE
	)
	
	fieldType = c(unique(testOptions$fieldType))
	if (!is.null(field)){
		if( field %in% getBinaryFields() ) {fieldType = "binary"}
		if( field %in% getNominalFields() ) {fieldType = "nominal"}
		if( field %in% getNumericFields() ) {fieldType = "numeric"}
		if (length(fieldType) > 1){
			stop(paste("Cannot determine field type for attribute:", field))
		}
	}
	
	whichTest = which(testOptions$fieldType %in% fieldType & testOptions$isParametric %in% isParametric)
	
	if (returnColors){
		cols = testOptions[whichTest,"color"]
		names(cols) = testOptions[whichTest,"testName"]
		return(cols)
	}else{
		return(testOptions[whichTest,"testName"])
	}
}

# Return named vector values for the given name
getValuesByName <- function( vals, name ) {
	return( as.vector( vals[names(vals)==name] ) )
}

# Import libraries and abort program with descriptive error
importLibs <- function( libs ) {
	errors = vector( mode="character" )
	for( i in 1:length( libs ) ) {
		if ( !library( libs[i], logical.return=TRUE, character.only=TRUE ) ) {
			errors[ length( errors ) + 1 ] = paste0( "Missing R library, please run install.packages(\"", libs[i], "\") from within R" )
		}
	}   
	
	if( length( errors ) > 0 ) {
		writeErrors( errors )
	}
}

# Log the msg if Config property r.debug=Y, otherwise do nothing
# Append semicolon to label, unless it already exists
logInfo <- function( info, msg=NULL ) {
	if( doDebug() ) {
		if( is.null( msg ) ){
			msg = info
		} else {
			if( !endsWith( trimws( info ), ":" ) ) info = paste0( trimws( info ), ":" )
			msg = c( trimws( info ), msg )
		}
		cat( msg, "\n" )
	}
}

# Return number of metadata columns appended to sample count tables
numMetaCols <- function() {
	return( getProperty( "R_internal.numMetaCols" ) )
}


# Given a vector of files, return the most recently modified
pickLatestFile <- function( files ) {
	newestFile = NULL
	for( i in 1:length( files ) ) {
		if( !is.null( files[i] ) && (is.null( newestFile ) || file.info( files[i] )[ "mtime" ] > file.info( newestFile )[ "mtime" ]) ) { 
			newestFile = files[i] 
		}
	}
	return( newestFile )
}

# Return a file matching the pattern underwhere under the pipeline root directory
# If multiple results are found, return the most recent version
# If dir is undefined, look in pipeline dir
# If no results, check in Config property input.dirPaths
pipelineFile <- function( pattern, dir=getPipelineDir() ) {
	
	if( length( dir ) > 1 ) {
		for( i in 1:length( dir ) ) {
			inputDirResults = pipelineFile( pattern, dir[i] )
			if( !is.null( inputDirResults ) ) {
				return( inputDirResults )
			}
		}
		writeErrors( c( paste0( "pipelineFile(", pattern, ",", dir, ") returned NULL" ) ) )
	}
	
	logInfo( c( "Search:", dir, "for input files matching pattern:", pattern ) )
	results = list.files( dir, pattern, full.names=TRUE, recursive=TRUE )
	if( length( results ) == 0 ) {
		if( ! dir %in% getProperty("input.dirPaths") ) {
			return( pipelineFile( pattern, getProperty("input.dirPaths") ) ) 
		}
		else {
			return( NULL )
		}
	}
	
	return( pickLatestFile( results ) )
}

# Create an empty plot and add text. 
# Ideal when explaining why a plot is blank, or plot explanations within a plot document.
plotPlainText <- function(textToPrint, cex=1){
	plot(c(0, 1), c(0, 1), ann = F, bty = 'n', type = 'n', xaxt = 'n', yaxt = 'n', mar = c(0,0,0,0))
	text(labels=textToPrint, x = 0.5, y = 0.5, cex = cex, col = "black")
}

# Read a table using the biolockj standards
readBljTable <- function( file ){
	return( read.table( file, header=TRUE, sep="\t", row.names=1, na.strings=getProperty( "metadata.nullValue" ), 
		check.names=FALSE, comment.char=getProperty( "metadata.commentChar", "" ) ) )
}

# This method returns 1 of 5 possible CalculateStats.R output file suffix values
# Use TRUE/FALSE params to obtain the correct p-value statistics file
# To retrieve the r^2 effect size file, use: buildStatsFileSuffix()
statsFileSuffix <- function( parametric=NULL, adjusted=TRUE ) {
	if( is.null( parametric ) ) return( paste0( "rSquaredVals", ".tsv" ) )
	if( parametric && adjusted ) return( paste0( "adjParPvals", ".tsv" ) )
	if( parametric ) return( paste0( "parametricPvals", ".tsv" ) )
	if( adjusted ) return( paste0( "adjNonParPvals", ".tsv" ) )
	return( paste0( "nonParametricPvals", ".tsv" ) )
}

# Return number of metadata columns appended to sample count tables
taxaLevels <- function() {
	return( getProperty( "report.taxonomyLevels" ) )
}

# Import standard shared libraries
importLibs( c( "properties", "stringr", "ggpubr" ) )
propCache = NULL

