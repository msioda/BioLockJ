# MAIN.R (MAIN_<Rmodule>.R)

# Return full path and name of this script
getModuleScript <- function() {
	initial.options = commandArgs(trailingOnly = FALSE)
	script.name <- sub("--file=", "", initial.options[grep("--file=", initial.options)])
	if( length( script.name ) == 0 ) {
		stop( "BioLockJ_Lib.R is not interactive - use RScript to execute." )
	}
	return( normalizePath( script.name ) )
}

scriptDir = dirname(getModuleScript())
moduleBaseName = sub("^MAIN_", "", basename(getModuleScript())) # retains the ".R" suffix

# get functions (BioLockJ_Lib.R) and initial setup that are common to all R modules
source( file.path( scriptDir, "BioLockJ_Lib.R" ) )

# Get the R code specific to this module and run its 'main' function.
source( file.path( scriptDir, moduleBaseName ) )
runProgram( getModuleScript() )

# save records of the process
reportStatus( getModuleScript() )
if (getProperty(name="r.saveRData", val=FALSE)){
	fname = paste0(moduleBaseName, "Data")
	save.image( file.path( scriptDir, fname ) )
}
sessionInfo()