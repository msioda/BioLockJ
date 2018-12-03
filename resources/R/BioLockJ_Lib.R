# BioLockJ_Lib.R contains the library of functions shared by multiple BioLockJ R script modules. 

# Add value to vector v and assign name 
addNamedVectorElement <- function( v, name, value ) {
   v[length(v) + 1] = value
   names(v)[length(v)] = name
   return( v )
}

# Return P value formated with sprintf as defined in MASTER Config r.pValFormat, otherwise use %1.2g default
displayPval <- function( pval ) {
   return( paste( sprintf(getProperty("r.pValFormat", "%1.2g"), pval) ) )
}

# Return TRUE if BioLock Config indicates debug files should be generated for R scripts
doDebug <- function() {
  return( getProperty("r.debug", FALSE) )
}

# Return vector that includs all binary, nominal, and numeric fields or an empty vector
getReportFields <- function() {
   return( c( getBinaryFields(), getNominalFields(), getNumericFields() ) )
}

# Return vector of binary fields or an empty vector
getBinaryFields <- function() {
   return( getProperty("internal.binaryFields", vector( mode="character" ) ) )
}

# Return otuTable column indexes for the given colNames
getColIndexes <- function( otuTable, colNames ) {
   cols = vector( mode="integer" )
   if( length(colNames) > 0 ) {
      for( i in 1:length(colNames) ) {
         cols[i] = grep(TRUE, colnames(otuTable)==colNames[i])
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
getColors <- function( n ) {
   return( get_palette( getProperty("r.colorPalette", "npg"), n ) )
}

# Return vector of nominal fields or an empty vector
getNominalFields <- function() {
   return( getProperty("internal.nominalFields", vector( mode="character" ) ) )
}

# Return vector of numeric fields or an empty vector
getNumericFields <- function() {
   return( getProperty("internal.numericFields", vector( mode="character" ) ) )
}

# Return a file matching the pattern underwhere under the pipeline root directory
# If multiple results are found, return the most recent version
getPipelineFile <- function( pattern ) {
   results = list.files( getPipelineDir(), pattern, full.names=TRUE, recursive=TRUE )
   returnFile = NULL
   for( i in 1:length( results ) ) {
      if( is.null( returnFile ) || file.info( results[i] )[ "mtime" ] > file.info( returnFile )[ "mtime" ] ) { 
         returnFile = results[i] 
      }
   }

   return( returnFile )
}

# Return list, each record contains the OTUs associated with a unique value for the given nominal metadata field (metaCol)
getFactorGroups <- function( otuTable, metaCol, otuCol ) {
   vals = list()
   options = levels( metaCol )
   for( i in 1:length(options) ) {
      vals[[i]] = otuTable[metaCol==options[i], otuCol]
   }
   return( vals )
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

# If downloaded with scp, all files share 1 directory, so return getPipelineDir() 
# Otherwise, script path like: piplineDir/moduleDir/script/MAIN*.R, so return moduleDir (the dir 2 levels above script)  
getModuleDir <- function() {
	if( getPipelineDir() == dirname( getModuleScript() ) ){
		return( getPipelineDir() )
	}
	return( dirname( dirname( getModuleScript() ) ) )
}

# Return file path of file in rootDir, with the pipeline name appended as a prefix to name
getPath <- function( rootDir, name ) {
   return( file.path( rootDir, paste0( getProperty("internal.pipelineName"), "_", name ) ) )
}

# Return the pipeline root directory
getPipelineDir <- function() {
	return( dirname( getMasterConfigFile() ) )
}

# Return the PDF plot label based on r.plotWidth, standar.properties default assumes 4 plots/page 
getPlotTitle <- function( line1, line2 ) {
   if( (nchar(line1) + nchar(line2) ) > getProperty("r.plotWidth") ) {
      return( paste0( line1, "\n", line2 ) )
   }
   return( paste( line1, line2 ) )
}

# Return property value from MASTER Config file, otherwise return the defaultVal
getProperty <- function( name, defaultVal=NULL ) {
   return ( suppressWarnings( parseConfig( name, defaultVal ) ) )
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
			errors[ length( errors ) + 1 ] = paste( "Missing R library, please run install.packages(", libs[i], ")" )
		}
	}   
	
	if( length( errors ) > 0 ) {
		writeErrors( getModuleScript(), errors )
	}
}

# Parse MASTER config for property value, if undefined return default defaultVal
parseConfig <- function( name, defaultVal=NULL ) {
   config = read.properties( getMasterConfigFile() )
   prop = config[[ name ]]

   if( is.null( prop ) ) {
      return( defaultVal )
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

# Create status indicator file by appending suffix of _Success or _Failure if any error messages logged
# BioLockJ Java program checks for existance of new file named as R script + status indicator
# Messages logged to _Warning file are generally spurious so file is deleted after processing
# Save MAIN script R data if r.saveRData configured and print session info
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

   if ( getProperty( "r.saveRData", FALSE ) ){
      save.image( paste0( sub("^MAIN_", "", script ), "Data" ) )
   }

   sessionInfo()
}




# MAIN R scripts all must call this method to wrap execution in sink() to catch error messages
# Call reportStatus( script ) when executin is complete
runProgram <- function( script ) {
   log = file( paste0( script, "_Warnings" ), open="wt" )
   sink( log, type="message" )
   print( paste( "Run script:", script, "in pipeline dir:", getPipelineDir() ) )
   try( main() )
   sink( type="message" )
   close( log )
   reportStatus( script )
}

# Method writes error msgs to the script _Failures file and aborts R program
writeErrors <- function( script, msgs ) {
   errorConn = file( paste0( script, "_Failures" ), open="wt" )
   writeLines( msgs, errorConn )
   close( errorConn )
   stop( paste( "Check error file to see runtime errors:", paste0( script, "_Failures" ) ) )
}

# Import standard shared libraries
importLibs( c( "properties", "stringr", "ggpubr" ) )
