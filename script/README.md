# Organization of scripts

All scripts should fall into one of these three tiers.
This is intended to make the code easier to read and maintain and to make it virtually impossible to create a circular dependency.

### Utility Scripts
 * end in "_functions"; ie blj_functions
 * has no dependencies, cannot call any other script
 * can be used any other script
 * Defines only functions (the functions may define variables, but this script cannot directly call any functions)
 * Use by `source`'ing

### Library Scripts
 * end in "_lib"; ie blj_user_arg_lb
 * can depend on utilities, but nothing else
 * cannot call scripts
 * Defines only functions (the functions may define variables, but only when called)
 * Use by `source`'ing
 * Some libraries are used by scripts, some by developers, some by users.

### Launch Script 
 * biolockj, launch_aws, launch_docker, launch_java
 * can depend on any Utility, or any library
 * may call other scripts
 * Use by launching: `$(launch_java)` --thus creating a new subshell.
 * narrative structure.  

Launch scripts have a narrative structure. A main method is defined at the top and called at the bottom.  The main method should read like english.  All of the code outside of the main method is essentially the library of narrative chunks given in the order in which they are called in main.  
