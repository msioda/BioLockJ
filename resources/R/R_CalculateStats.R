# Module script for: biolockj.module.report.r.R_CalculateStats

# There are 5 summary tables are output for each taxaLevels()
# 1. Parametric P-Value table
# 2. Non-parametric P-Value table 
# 3. Adjusted Parametric P-Value table
# 4. Adjusted Non-parametric P-Value table 
# 5. R^2 Value table
buildSummaryTables <- function( reportStats, level ) {
	attNames = unique( names(reportStats[[2]]) )
	for( i in 2:length( reportStats ) ) {
		df = data.frame( vector( mode="double", length=length( reportStats[[1]] ) ) )
		df[, 1] = reportStats[[1]]
		names(df)[1] = names( reportStats )[1]
		for( j in 1:length( attNames ) ) {
			df[, length(df)+1] = getValuesByName( reportStats[[i]], attNames[j] )
			names(df)[length(df)] = attNames[j]
		}
		fileName = getPath( getOutputDir(), paste0( level, "_", names(reportStats)[i] ) )
		logInfo( paste( "Saving output file", fileName ) )
		write.table( df, file=fileName, sep="\t", row.names=FALSE )
	}
}  


# Build list of reportStats for the taxonomy level specific taxaTable with the following columns:
# "OTU", "parametricPvals", "nonParametricPvals", "adjParPvals", "adjNonParPvals", "rSquaredVals"
# First, parametric & non-parametric p-values are calculated for each attribute type
# Then the p-values are adjusted using method defined in r.pAdjust
calculateStats <- function( taxaTable ) {
   # Loop through the OTUs to assign P-value & R^2 values
   otuNames = vector( mode="character" )
   parametricPvals = vector( mode="double" )
   nonParametricPvals = vector( mode="double" )
   rSquaredVals = vector( mode="double" )
   adjParPvals = vector( mode="double" )
   adjNonParPvals = vector( mode="double" )
   lastOtuCol = ncol(taxaTable) - numMetaCols() 

	binaryCols = getColIndexes( taxaTable, getBinaryFields() )
	nominalCols = getColIndexes( taxaTable, getNominalFields() )
	numericCols = getColIndexes( taxaTable, getNumericFields() )
	logInfo( "binaryCols", binaryCols )
	logInfo( "nominalCols", nominalCols )
	logInfo( "numericCols", numericCols )
	
	logInfo( "numMetaCols", numMetaCols() )
	logInfo( "ncol(taxaTable)", ncol(taxaTable) )
	logInfo( "lastOtuCol", lastOtuCol )

   # if r.rareOtuThreshold > 1, cutoffValue is an absolute threshold, otherwise it's a % of taxaTable rows
   cutoffValue = getProperty("r.rareOtuThreshold", 1)
   if( cutoffValue < 1 ) {
      cutoffValue = cutoffValue * nrow(taxaTable)
   }
   logInfo( "cutoffValue", cutoffValue )
   
   for( taxaCol in 1:lastOtuCol ) {
      if( sum( taxaTable[,taxaCol] > 0 ) >= cutoffValue ) {
         otuNames[length(otuNames)+1] = names(taxaTable)[taxaCol]         
         if( length( binaryCols ) > 0 ) {
            logInfo( "Calculate BINARY P_VALS" )
            for( metaCol in binaryCols ) {
               attName = names( taxaTable )[metaCol]
               logInfo( c( "Col# [", metaCol, "] =", attName ) )
               att = as.factor( taxaTable[,metaCol] )
               vals = levels( att )
               myLm = lm( taxaTable[,taxaCol] ~ att, na.action=na.exclude )
               parametricPvals = addNamedVectorElement( parametricPvals, attName, t.test( taxaTable[att==vals[1], taxaCol], taxaTable[att==vals[2], taxaCol] )$p.value )
               nonParametricPvals = addNamedVectorElement( nonParametricPvals, attName, pvalue( wilcox_test( taxaTable[att==vals[1], taxaCol], taxaTable[att==vals[2], taxaCol] ) ) )
               rSquaredVals = addNamedVectorElement( rSquaredVals, attName, summary( myLm )$r.squared )
            }
         }
         
         if( length( nominalCols ) > 0 ) {
         	logInfo( "Calculate NOMINAL P_VALS" )
            for( metaCol in nominalCols ) {
               attName = names( taxaTable )[metaCol]
               logInfo( c( "Col# [", metaCol, "] =", attName ) )
               att = as.factor( taxaTable[,metaCol] )
               vals = levels( att )
               myLm = lm( taxaTable[,taxaCol] ~ att, na.action=na.exclude )
               myAnova = anova( myLm )
               parametricPvals = addNamedVectorElement( parametricPvals, attName, myAnova$"Pr(>F)"[1] )
               nonParametricPvals = addNamedVectorElement( nonParametricPvals, attName, kruskal.test( taxaTable[,taxaCol] ~ att, na.action=na.exclude )$p.value )
               rSquaredVals = addNamedVectorElement( rSquaredVals, attName, summary( myLm )$r.squared )
            }
         }
         
         if( length( numericCols ) > 0 ) {
         	logInfo( "Calculate NUMERIC P_VALS" )
            for( metaCol in numericCols ) {
               attName = names( taxaTable )[metaCol]
               logInfo( c( "Col# [", metaCol, "] =", attName ) )
               att = as.numeric( taxaTable[,metaCol] )
               parametricPvals = addNamedVectorElement( parametricPvals, attName, Kendall( taxaTable[,taxaCol], att)$sl[1] )
			   nonParametricPvals = addNamedVectorElement( nonParametricPvals, attName, cor.test( taxaTable[,taxaCol], att, na.action=na.exclude )$p.value )
               rSquaredVals = addNamedVectorElement( rSquaredVals, attName, cor( taxaTable[,taxaCol], att, use="na.or.complete", method="kendall" )^2 )
            }
         }
      }
   }

   if( length( otuNames ) == 0 ) {
      logInfo( "No OTU Names found, verify empty vector", otuNames )
      return( NULL )
   }

   logInfo( "Calculate ADJUSTED P_VALS" )
   adjParDF = data.frame( vector(mode="double", length=length(otuNames)) )
   adjNonParDF = data.frame( vector(mode="double", length=length(otuNames)) )
   for( i in 1:length( getReportFields() ) ) {
      parPvals = getValuesByName( parametricPvals, getReportFields()[i] )
      npPvals = getValuesByName( nonParametricPvals, getReportFields()[i] )
      adjParDF[,i] = p.adjust( parPvals, method=getProperty("r_CalculateStats.pAdjustMethod"), n=getP_AdjustLen(otuNames) )
      adjNonParDF[,i] = p.adjust( npPvals, method=getProperty("r_CalculateStats.pAdjustMethod"), n=getP_AdjustLen(otuNames) )
      names(adjParDF)[i] = getReportFields()[i]
      names(adjNonParDF)[i] = getReportFields()[i]
   }

   logInfo( "Convert ADJUSTED P_VALS data.frame --> named vector" )
   adjParPvals = as.vector( as.matrix(adjParDF) )
   adjNonParPvals = as.vector( as.matrix(adjNonParDF) )
   for( i in 1:length( getReportFields() ) ) {
      y = length( otuNames ) * i
      x = y - length( otuNames ) + 1
      names( adjParPvals )[x:y] = rep( getReportFields()[i], length( otuNames ) )
      names( adjNonParPvals )[x:y] = rep( getReportFields()[i], length( otuNames ) )
   }
   logInfo( "otuNames", otuNames )
   logInfo( "adjParPvals:", adjParPvals )
   logInfo( "adjNonParPvals:", adjNonParPvals )
   reportStats = list( otuNames, parametricPvals, nonParametricPvals, adjParPvals, adjNonParPvals, rSquaredVals )
   names( reportStats ) = c( "OTU", statsFileSuffix( TRUE, FALSE ), statsFileSuffix( FALSE, FALSE ),
		statsFileSuffix( TRUE ), statsFileSuffix( FALSE ), statsFileSuffix() )
   return( reportStats )
}

# P Adjust Length depends on how many p-values to include in the adjustment
# LOCAL adjusts p-values for all OTUs for 1 attribute at 1 taxonomy level
# TAXA adjusts p-values for a all OTUs and all attributes at 1 taxonomy level
# ATTRIBUTE adjusts p-values for a all OTUs and all taxonomy levels for 1 attribute
# GLOBAL adjusts p-values for a all OTUs and all attributes and all taxonomy levels
getP_AdjustLen <- function( otuNames ) {
   if( getProperty("r_CalculateStats.pAdjustScope") == "GLOBAL" ) {
      return( length(otuNames) * length( taxaLevels() ) * length( getReportFields() ) )
   } else if( getProperty("r_CalculateStats.pAdjustScope") == "ATTRIBUTE" ) {
      return( length(otuNames) * length( taxaLevels() ) )
   } else if( getProperty("r_CalculateStats.pAdjustScope") == "TAXA" ) {
      return( length(otuNames) * length(getReportFields()) )
   } else {
      return( length(otuNames) )
   } 
}

# Main function imports coin and Kendall libraries 
# Generates the reportStats list with p-value and R^2 metrics 
# Outputs summary tables for each metric at each taxonomyLevel
main <- function() {
	importLibs( c( "coin", "Kendall" ) ) 
	for( level in taxaLevels() ) {
		if( doDebug() ) sink( file.path( getTempDir(), paste0("debug_CalculateStats_", level, ".log") ) )
		taxaTable = getTaxaTable( level )
		if( is.null( taxaTable ) ) { next }
		reportStats = calculateStats( taxaTable )
		if( is.null( reportStats ) ) {
			logInfo( c( level, "is empty, verify contents of table:", inputFile ) )
		} else {
			logInfo( "Building summary Tables ... " )
			buildSummaryTables( reportStats, level )
		}
	}
	logInfo( "Done!" )
	if( doDebug() ) sink()
}

# Method wilcox_test is from the coin package
# Calcualates exact pvalues without using heuristic algorithm for better precision
# Otherwise if ties are found the script may fail
wilcox_test.default <- function( x, y, ... ) {
   data = data.frame( values = c(x, y), group = rep( c("x", "y"), c(length(x), length(y)) ) )
   return( wilcox_test( values ~ group, data = data, ... ) )
}
