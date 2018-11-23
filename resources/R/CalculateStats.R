# Module script for: biolockj.module.r.CalculateStats

# Add value to vector v and assign name 
addNamedVectorElement <- function( v, name, value ) {
   v[length(v) + 1] = value
   names(v)[length(v)] = name
   return( v )
}

# There are 5 summary tables are output for each report.taxonomyLevels
# 1. Parametric P-Value table
# 2. Nonparameteric P-Value table 
# 3. Adjusted Parametric P-Value table
# 4. Adjusted Nonparameteric P-Value table 
# 5. R^2 Value table
buildSummaryTables <- function( reportStats, otuLevel ) {
   attNames = unique( names(reportStats[[2]]) )
   for( i in 2:length( reportStats ) ) {
      fileName = getPath( file.path(getModuleDir(), "output"), paste0( otuLevel, "_", names(reportStats)[i], ".tsv" ) )
      df = data.frame( vector( mode="double", length=length( reportStats[[1]] ) ) )
      df[, 1] = reportStats[[1]]
      names(df)[1] = names( reportStats )[1]
      for( j in 1:length( attNames ) ) {
         df[, length(df)+1] = getValuesByName( reportStats[[i]], attNames[j] )
         names(df)[length(df)] = attNames[j]
      }
      write.table( df, file=fileName, sep="\t", row.names=FALSE )
   }
}

# Build list of reportStats for the taxonomy level specific otuTable with the following columns:
# "OTU", "parametricPvals", "nonParametricPvals", "adjParPvals", "adjNonParPvals", "rSquaredVals"
# First, parametric & nonparametric p-values are calculated for each attribute type
# Then the p-values are adjusted based on r.pAdjust
calculateStats <- function(otuTable, binaryCols, nominalCols, numericCols ) {
   # Loop through the OTUs to assign P-value & R^2 values
   otuNames = vector( mode="character" )
   parametricPvals = vector( mode="double" )
   nonParametricPvals = vector( mode="double" )
   rSquaredVals = vector( mode="double" )
   adjParPvals = vector( mode="double" )
   adjNonParPvals = vector( mode="double" )
   lastOtuCol = ncol(otuTable) - getProperty("internal.numMetaCols")

   if( r.debug ) print( paste( "internal.numMetaCols:", getProperty("internal.numMetaCols") ) )
   if( r.debug ) print( paste( "ncol(otuTable):", ncol(otuTable) ) )
   if( r.debug ) print( paste( "lastOtuCol:", lastOtuCol ) )

   # if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of otuTable rows
   cutoffValue = getProperty("r.rareOtuThreshold", 1)
   if( cutoffValue < 1 ) {
      cutoffValue = cutoffValue * nrow(otuTable)
   }
   if( r.debug ) print( paste( "cutoffValue:", cutoffValue ) )
   
   for( otuCol in 2:lastOtuCol ) {
      if( sum( otuTable[,otuCol] > 0 ) >= cutoffValue ) {
         otuNames[length(otuNames)+1] = names(otuTable)[otuCol]
         if( r.debug ) print( paste0( "otuNames[", length(otuNames), "]: ", otuNames[ length(otuNames) ] ) )
         
         if( length( binaryCols ) > 0 ) {
            if( r.debug ) print( "Calculate BINARY P_VALS" )
            for( metaCol in binaryCols ) {
               if( r.debug ) print( paste( "metaCol:", metaCol ) )
               attName = names( otuTable )[metaCol]
               if( r.debug ) print( paste( "attName = names( otuTable )[metaCol]:", attName ) )
               att = as.factor( otuTable[,metaCol] )
               if( r.debug ) print( paste( c("att = otuTable[,metaCol]:", att), collapse= " " ) )
               vals = levels( att )
               if( r.debug ) print( paste( c("vals = levels( att ):", vals), collapse= " " ) )
               myLm = lm( otuTable[,otuCol] ~ att, na.action=na.exclude )
               if( r.debug ) print( "myLm = lm( otuTable[,otuCol] ~ att, na.action=na.exclude )" )
               if( r.debug ) print( myLm )
               parametricPvals = addNamedVectorElement( parametricPvals, attName, t.test( otuTable[att==vals[1], otuCol], otuTable[att==vals[2], otuCol] )$p.value )
               if( r.debug ) print( paste( "Add parametricPval:", parametricPvals[length(parametricPvals)] ) )
               nonParametricPvals = addNamedVectorElement( nonParametricPvals, attName, pvalue( wilcox_test( otuTable[att==vals[1], otuCol], otuTable[att==vals[2], otuCol] ) ) )
               if( r.debug ) print( paste( "Add nonParametricPval:", nonParametricPvals[length(nonParametricPvals)] ) )
               rSquaredVals = addNamedVectorElement( rSquaredVals, attName, summary( myLm )$r.squared )
               if( r.debug ) print( paste( "Add rSquaredVal:", rSquaredVals[length(rSquaredVals)] ) )

            }
         }
         
         if( length( nominalCols ) > 0 ) {
            if( r.debug ) print( "Calculate NOMINAL P_VALS" )
            for( metaCol in nominalCols ) {
               if( r.debug ) print( paste( "metaCol:", metaCol ) )
               attName = names( otuTable )[metaCol]
               if( r.debug ) print( paste( "attName = names( otuTable )[metaCol]:", attName ) )
               att = as.factor( otuTable[,metaCol] )
               if( r.debug ) print( paste( c("att = otuTable[,metaCol]:", att), collapse= " " ) )
               vals = levels( att )
               if( r.debug ) print( paste( c("vals = levels( att ):", vals), collapse= " " ) )
               myLm = lm( otuTable[,otuCol] ~ att, na.action=na.exclude )
               if( r.debug ) print( "myLm = lm( otuTable[,otuCol] ~ att, na.action=na.exclude )" )
               if( r.debug ) print( myLm )
               myAnova = anova( myLm )
               if( r.debug ) print( "myAnova = anova( myLm )" )
               if( r.debug ) print( myAnova )
               parametricPvals = addNamedVectorElement( parametricPvals, attName, myAnova$"Pr(>F)"[1] )
               if( r.debug ) print( paste( "Add parametricPval:", parametricPvals[length(parametricPvals)] ) )
               nonParametricPvals = addNamedVectorElement( nonParametricPvals, attName, kruskal.test( otuTable[,otuCol] ~ att, na.action=na.exclude )$p.value )
               if( r.debug ) print( paste( "Add nonParametricPval:", nonParametricPvals[length(nonParametricPvals)] ) )
               rSquaredVals = addNamedVectorElement( rSquaredVals, attName, summary( myLm )$r.squared )
               if( r.debug ) print( paste( "Add rSquaredVal:", rSquaredVals[length(rSquaredVals)] ) )

            }
         }
         
         if( length( numericCols ) > 0 ) {
            if( r.debug ) print( "Calculate NUMERIC P_VALS" )
            for( metaCol in numericCols ) {
               if( r.debug ) print( paste( "metaCol:", metaCol ) )
               attName = names( otuTable )[metaCol]
               if( r.debug ) print( paste( "attName = names( otuTable )[metaCol]:", attName ) )
               att = otuTable[,metaCol]
               if( r.debug ) print( paste( c("att = otuTable[,metaCol]:", att), collapse= " " ) )
               parametricPvals = addNamedVectorElement( parametricPvals, attName, Kendall( otuTable[,otuCol], att )$sl[1] )
               if( r.debug ) print( paste( "Add parametricPval:", parametricPvals[length(parametricPvals)] ) )
               nonParametricPvals = addNamedVectorElement( nonParametricPvals, attName, cor.test( otuTable[,otuCol], att )$p.value )
               if( r.debug ) print( paste( "Add nonParametricPval:", nonParametricPvals[length(nonParametricPvals)] ) )
               rSquaredVals = addNamedVectorElement( rSquaredVals, attName, cor( otuTable[,otuCol], att, use="na.or.complete", method="kendall" )^2 )
               if( r.debug ) print( paste( "Add rSquaredVal:", rSquaredVals[length(rSquaredVals)] ) )
            }
         }
      }
   }

   if( length( otuNames ) == 0 ) {
      if( r.debug ) print( "No OTU Names found, should print empty vector below:"  )
      if( r.debug ) print( otuNames )
      return( NULL )
   }

   if( r.debug ) print( "Calculate ADJUSTED P_VALS" )
   adjParDF = data.frame( vector(mode="double", length=length(otuNames)) )
   adjNonParDF = data.frame( vector(mode="double", length=length(otuNames)) )
   for( i in 1:length( allAtts ) ) {
      if( r.debug ) print( paste( "allAtts[i]:", allAtts[i] ) )
      parPvals = getValuesByName( parametricPvals, allAtts[i] )
      if( r.debug ) print( paste( c("parPvals:", parPvals), collapse= " " ) )
      npPvals = getValuesByName( nonParametricPvals, allAtts[i] )
      if( r.debug ) print( paste( c("npPvals:", npPvals), collapse= " " ) )
      adjParDF[,i] = p.adjust( parPvals, method=getProperty("rStats.pAdjustMethod"), n=getP_AdjustLen(otuNames) )
      adjNonParDF[,i] = p.adjust( npPvals, method=getProperty("rStats.pAdjustMethod"), n=getP_AdjustLen(otuNames) )
      names(adjParDF)[i] = allAtts[i]
      names(adjNonParDF)[i] = allAtts[i]
   }

   if( r.debug ) print( "Convert ADJUSTED P_VALS data.frame --> named vector" )
   adjParPvals = as.vector( as.matrix(adjParDF) )
   adjNonParPvals = as.vector( as.matrix(adjNonParDF) )
   for( i in 1:length( allAtts ) ) {
      y = length( otuNames ) * i
      x = y - length( otuNames ) + 1
      names( adjParPvals )[x:y] = rep( allAtts[i], length( otuNames ) )
      names( adjNonParPvals )[x:y] = rep( allAtts[i], length( otuNames ) )
   }
   if( r.debug ) print( paste( c("otuNames:", otuNames), collapse= " " ) )
   if( r.debug ) print( paste( c("adjParPvals:", adjParPvals), collapse= " " ) )
   if( r.debug ) print( paste( c("adjNonParPvals:", adjNonParPvals), collapse= " " ) )
   reportStats = list( otuNames, parametricPvals, nonParametricPvals, adjParPvals, adjNonParPvals, rSquaredVals )
   names(reportStats) = c( "OTU", "parametricPvals", "nonParametricPvals", "adjParPvals", "adjNonParPvals", "rSquaredVals" )
   return( reportStats )
}

# P Adjust Length depends on how many p-values to include in the adjustment
# LOCAL adjusts p-values for all OTUs for 1 attribute at 1 taxonomy level
# TAXA adjusts p-values for a all OTUs and all attributes at 1 taxonomy level
# ATTRIBUTE adjusts p-values for a all OTUs and all taxonomy levels for 1 attribute
# GLOBAL adjusts p-values for a all OTUs and all attributes and all taxonomy levels
getP_AdjustLen <- function( otuNames ) {
   if( getProperty("rStats.pAdjustScope") == "GLOBAL" ) {
      return( length(otuNames) * length( getProperty("report.taxonomyLevels") ) * length(allAtts) )
   } else if( getProperty("rStats.pAdjustScope") == "ATTRIBUTE" ) {
      return( length(otuNames) * length( getProperty("report.taxonomyLevels") ) )
   } else if( getProperty("rStats.pAdjustScope") == "TAXA" ) {
      return( length(otuNames) * length(allAtts) )
   } else {
      return( length(otuNames) )
   } 
}


# Main function imports libraries and for each report.taxonomyLevels:
# Generates the reportStats list with p-value and R^2 metrics
# Outputs summary tables for each metric at each taxonomyLevel
main <- function() {
   importLibs( c( "coin", "Kendall" ) ) 
   for( otuLevel in getProperty("report.taxonomyLevels") ) {
      if( r.debug ) sink( file.path( getModuleDir(), "temp", paste0("debug_CalculateStats_", otuLevel, ".log") ) )
      inputFile = getPipelineFile( paste0(otuLevel, ".*_metaMerged.tsv") )
      if( r.debug ) print( paste( "inputFile:", inputFile ) )
      if( length( inputFile ) == 0 ) { next }
      otuTable = read.table( inputFile, check.names=FALSE, na.strings=getProperty("metadata.nullValue", "NA"), comment.char=getProperty("metadata.commentChar", ""), header=TRUE, sep="\t" )
      binaryCols = getColIndexes( otuTable, binaryFields )
      if( r.debug ) print( paste( c("binaryCols:", binaryCols), collapse= " " ) )
      nominalCols = getColIndexes( otuTable, nominalFields )
      if( r.debug ) print( paste( c("nominalCols:", nominalCols), collapse= " " ) )
      numericCols = getColIndexes( otuTable, numericFields )
      if( r.debug ) print( paste( c("numericCols:", numericCols), collapse= " " ) )
      reportStats = calculateStats( otuTable, binaryCols, nominalCols, numericCols )
      if( is.null( reportStats ) ) {
         if( r.debug ) print( paste( otuLevel, "is empty, verify contents of table:", inputFile ) )
      } else {
         buildSummaryTables( reportStats, otuLevel )
      }
   }
   if( r.debug ) sink()
}

# wilcox_test is from the coin package
# calcualates exact pvalues without using heuristic algorithm for better precision
# otherwise many ties are found in typical datasets causing script to fail
wilcox_test.default <- function( x, y, ... ) {
   data = data.frame( values = c(x, y), group = rep( c("x", "y"), c(length(x), length(y)) ) )
   return( wilcox_test( values ~ group, data = data, ... ) )
}
