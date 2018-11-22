# BioLockJ_Lib.R contains the library of functions shared by multiple BioLockJ R script modules. 


addPlotLabel <- function( label, size, color, las, side, rowIndex, colIndex ) {
   mtext( bquote(bold(.( label ))), outer=TRUE, cex=size, side=side, las=las, line=colIndex, adj=rowIndex, col=color )
}


displayPval <- function( pval ) {
   return( paste( sprintf(getProperty("r.pValFormat", "%1.2g"), pval) ) )
}


getCexAxis <- function( labels ) {
   nchars = sum(nchar(labels)) + length(labels) - 1
   if( nchars < getProperty("r.plotWidth")) {
      return( 1 )
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


getColIndexes <- function( otuTable, attNames ) {
   cols = vector( mode="integer" )
   if( !is.na(attNames) && length(attNames) > 0 ) {
      for( i in 1:length(attNames) ) {
         cols[i] = grep(TRUE, colnames(otuTable)==attNames[i])
      }
   }
   return( cols )
}


#return r.colorHighlight if any of the input values meet the r.pvalCutoff, otherwise return r.colorBase
getColor <- function( v ) {
   for( i in 1:length(v) ) {
      if( grepl("e", v[i]) || !is.na(v[i]) && !is.nan(v[i]) && ( v[i] <= getProperty("r.pvalCutoff", 0.05) ) ) {
         return( getProperty("r.colorHighlight", "black") )
      } 
   }
   return( getProperty("r.colorBase", "black") )
}


getColors <- function( n ) {
   return( get_palette( getProperty("r.colorPalette", "npg"), n ) )
}


# Return a file matching the pattern underwhere under the pipeline root directory
getPipelineFile <- function( pattern ) {
   result = list.files( pipelineDir, pattern, full.names=TRUE, recursive=TRUE )
   if( length( result ) > 1 ){
      stop( paste( "Ambiguous file:", subDir, pattern ) )
   }

   return( result )
}

getFactorGroups <- function( otuTable, metaCol, otuCol ) {
   vals = list()
   options = levels( metaCol )
   for( i in 1:length(options) ) {
      vals[[i]] = otuTable[metaCol==options[i], otuCol]
   }
   return( vals )
}


getLabels <- function( labels ) {
   if( getCexAxis(labels) == cexAxisMin ) {
      nchars = sum(nchar(labels)) + length(labels) - 1
      maxSize = ((getProperty("r.plotWidth")*2)+2)/length(labels)
      return( strtrim(labels, floor(maxSize) ) )
   }
   return( labels )
}


getLas <- function( labels ) {
   HORIZONTAL = 1
   VERTICAL = 3
   nchars = sum(nchar(labels)) + length(labels) - 1
   aveSize = sum(nchar(labels))/length(labels)
   las = HORIZONTAL
   if( (length(labels) > 5) && aveSize > 3 ) las = VERTICAL
   return( las )
}


getMaxAttLen <- function( v ) {
   max = 0
   for( i in 1:length(v) ) {
      if( nchar(v[i]) > max ) max = nchar(v[i])
   }
   return( max )
}


getModuleDir <- function() {
	if( pipelineDir == dirname( getModuleScript() ) ){
		return( pipelineDir )
	}
	return( dirname( dirname( getModuleScript() ) ) )
}


getPath <- function( rootDir, name ) {
   return( file.path( rootDir, paste0( getProperty("internal.pipelineName"), "_", name ) ) )
}


getPlotTitle <- function( line1, line2 ) {
   if( (nchar(line1) + nchar(line2) ) > getProperty("r.plotWidth") ) {
      return( paste0( line1, "\n", line2 ) )
   }
   return( paste( line1, line2 ) )
}

getMasterProperties <- function() {
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


getProperty <- function( name, val=NULL ) {
   return ( suppressWarnings( parseConfig( name, val ) ) )
}
    

getValuesByName <- function( vals, name ) {
   return( as.vector( vals[names(vals)==name] ) )
}


importLibs <- function( libs ) {
   errors = vector( mode="character" )
   for( i in 1:length( libs ) ) {
      if ( !library( libs[i], logical.return=TRUE, character.only=TRUE ) ) {
         errors[ length( errors ) + 1 ] = paste( "Missing R library, please run install.packages(", libs[i], ")" )
      }
   }   

   if( length( errors ) > 0 ) {
      writeErrors( getModuleScript(), errors )
   }
}


parseConfig <- function( name, val=NULL ) {
   config = read.properties( configFile )
   prop = config[[ name ]]

   if( is.null( prop ) ) {
      return( val )
   }
   if( is.null( prop ) ) {
      return( NULL )
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


pValueTestName <- function( attName, isParametric ) {
   if( attName %in% binaryFields && isParametric ) return ( "T-Test" )
   if( attName %in% binaryFields && !isParametric ) return ( "Wilcox" )
   if( attName %in% nominalFields && isParametric ) return ( "ANOVA" )
   if( attName %in% nominalFields && !isParametric ) return ( "Kruskal" )
   if( attName %in% numericFields && isParametric ) return ( "Pearson" )
   if( attName %in% numericFields && !isParametric ) return ( "Kendall" )
}

reportStatus <- function( script ) {
   conn = file( paste0( script, "_Warnings" ), open="r" )
   warnings = readLines( conn )
   close( conn )
   errors = vector( mode="character" )
   if( length( errors ) > 0 ) {
      for ( i in 1:length( warnings ) ) {
         if( grepl( "Error", warnings[i] ) ) {
            errors[ length( errors ) + 1 ] = warnings[i]
         }
      }
   }

   if( file.exists( paste0( script, "_Warnings" ) ) ) {
         file.remove( paste0( script, "_Warnings" ) )
   }

   if( length( errors ) > 0 ) {
      writeErrors( script, errors )
   } else {
      file.create( paste0( script, "_Success" ) )
   }
}

runProgram <- function( script ) {
   log = file( paste0( script, "_Warnings" ), open="wt" )
   sink( log, type="message" )
   print( paste( "Pipeline dir: ", pipelineDir ) )
   try( main() )
   sink( type="message" )
   close( log )
}


writeErrors <- function( script, msgs ) {
   errorConn = file( paste0( script, "_Failures" ), open="wt" )
   writeLines( msgs, errorConn )
   close( errorConn )
   stop( paste( "Check error file to see runtime errors:", paste0( script, "_Failures" ) ) )
}


importLibs( c( "properties", "stringr", "ggpubr" ) )
configFile = getMasterProperties()
print( paste( "Importing Config:", configFile ) )
#print( read.properties( configFile ) )
pipelineDir = dirname( configFile )
r.debug = getProperty("r.debug", FALSE)
binaryFields = getProperty("internal.binaryFields", vector( mode="character" ) )
nominalFields = getProperty("internal.nominalFields", vector( mode="character" ) )
numericFields = getProperty("internal.numericFields", vector( mode="character" ) )
allAtts = c( binaryFields, nominalFields, numericFields )
cexAxisMin = 0.65
