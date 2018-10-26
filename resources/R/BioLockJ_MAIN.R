
# MAIN.R (MAIN_<Rmodule>.R)

# Return full path to this script, use this to derive file paths and name for this module
getModuleScript <- function() {
	initial.options = commandArgs(trailingOnly = FALSE)
	script.name <- sub("--file=", "", initial.options[grep("--file=", initial.options)])
	if( length( script.name ) == 0 ) {
		stop( "BioLockJ_Lib.R is not interactive - use RScript to execute." )
	}
	return( normalizePath( script.name ) )
}
scriptDir = dirname(getModuleScript())
moduleBaseName = sub("^MAIN_", "", basename(getModuleScript()))
moduleBaseName = sub(".R$", "", moduleBaseName)

# get functions (BioLockJ_Lib.R) and initial setup that are common to all R modules
source( file.path( scriptDir, "BioLockJ_Lib.R" ) )
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

# Get the R code specific to this module and run its 'main' function.
source( file.path( scriptDir, paste0(moduleBaseName, ".R") ) )
runProgram( getModuleScript() )

# save records of the process
reportStatus( getModuleScript() )
if (getProperty(name="r.saveRData", val=T)){
	fname = paste0("moduleBaseName", ".RData")
	save.image( file.path( scriptDir, fname ) )
}
sessionInfo()