# Execute this script to run all R scripts on your local workstation

# Return script directory path
getModuleScript <- function() {
	initial.options = commandArgs(trailingOnly = FALSE)
	script.name <- sub("--file=", "", initial.options[grep("--file=", initial.options)])
	if( length( script.name ) == 0 ) {
		stop( "BioLockJ_Lib.R is not interactive - use RScript to execute." )
	}
	return( normalizePath( script.name ) )
}

# The above method is part of a template.
# --------------------------------------------------------
# The following source lines are added based on pipeline modules.

