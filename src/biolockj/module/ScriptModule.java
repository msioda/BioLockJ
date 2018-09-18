/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module;

import java.io.File;
import java.util.List;
import biolockj.exception.ConfigFormatException;

/**
 * Classes that implement this interface are <br>
 * 
 */
public interface ScriptModule extends BioModule
{

	/**
	 * Method returns a nested list of bash script lines to classify samples containing forward reads only. The inner
	 * list contains the bash script lines required to classify 1 sample.
	 *
	 * @param files Files in the input directory that contain only forward reads
	 * @return Nested list of bash scripts lines
	 * @throws Exception if unable to generate script lines
	 */
	public List<List<String>> buildScript( final List<File> files ) throws Exception;

	/**
	 * Method returns a nested list of bash script lines to classify samples containing paired reads.<br>
	 * The inner list contains the bash script lines required to classify 1 sample.
	 *
	 * @param files Files in the input directory that contain only paired reads
	 * @return Nested list of bash scripts lines
	 * @throws Exception if unable to generate the script lines
	 */
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception;

	/**
	 * Many ScriptModules generate bash or R scripts for the Operating System to run as a {@link biolockj.Job}.<br>
	 * The Job creates a {@link java.lang.Process} using the parameters supplied by this method and executed in the
	 * {@link java.lang.Runtime} environment.<br>
	 * Parameters typically contain the full script path and script parameters, if needed.
	 *
	 * @return {@link java.lang.Runtime#exec(String)} parameters
	 * @throws Exception if unable to build the job parameters
	 */
	public String[] getJobParams() throws Exception;

	/**
	 * ScriptModules that generate scripts to complete their task, create script files in this directory.<br>
	 * The main script must begin with prefix {@value #MAIN_SCRIPT_PREFIX} and there must only be one main script.<br>
	 * The main script executes, or submits to the job queue, each of the other scripts in this directory.<br>
	 * The # subscripts generated by the ScriptModule depends upon {@value #SCRIPT_BATCH_SIZE}.<br>
	 *
	 * @return File directory containing all ScriptModule scripts
	 */
	public File getScriptDir();

	/**
	 * ScriptModule that run scripts can opt to set a timeout (such as used by the R script).
	 *
	 * @return Number of minutes before script is cancelled due to timeout
	 * @throws ConfigFormatException if the {@value #SCRIPT_TIMEOUT} value is invalid
	 */
	public Integer getTimeout() throws ConfigFormatException;

	/**
	 * Method returns bash functions to add to the worker scripts.<br>
	 * The default behavior is that no bash functions are needed.
	 * 
	 * @return Bash script lines for the functions
	 * @throws Exception if unable to generate script lines
	 */
	public List<String> getWorkerScriptFunctions() throws Exception;

	/**
	 * {@link biolockj.Config} Integer property: {@value #CLASSIFIER_NUM_THREADS}<br>
	 * Used to reserve cluster resources and passed to classifier applications call that accepts a numThreads parameter.
	 */
	public static final String CLASSIFIER_NUM_THREADS = "cluster.numClassifierThreads";

	/**
	 * {@link biolockj.Config} Integer property: {@value #SCRIPT_BATCH_SIZE}<br>
	 * Set number of samples to process per script (if parallel processing)
	 */
	public static final String SCRIPT_BATCH_SIZE = "script.batchSize";

	/**
	 * {@link biolockj.Config} List property: {@value #SCRIPT_DEFAULT_HEADER}<br>
	 * Store default script header for MAIN script and locally run WORKER scripts.
	 */
	public static final String SCRIPT_DEFAULT_HEADER = "script.defaultHeader";

	/**
	 * Name of the script sub-directory: {@value #SCRIPT_DIR}
	 */
	public static final String SCRIPT_DIR = "script";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #SCRIPT_EXIT_ON_ERROR}<br>
	 * If set to {@value biolockj.Config#TRUE}, pipeline will fail on any script error.
	 */
	public static final String SCRIPT_EXIT_ON_ERROR = "script.exitOnError";

	/**
	 * {@link biolockj.Config} Integer property: {@value #SCRIPT_NUM_THREADS}<br>
	 * Used to reserve cluster resources and passed to any external application call that accepts a numThreads
	 * parameter.
	 */
	public static final String SCRIPT_NUM_THREADS = "script.numThreads";

	/**
	 * {@link biolockj.Config} String property: {@value #SCRIPT_PERMISSIONS}<br>
	 * Used as chmod permission parameter (ex: 774)
	 */
	public static final String SCRIPT_PERMISSIONS = "script.permissions";

	/**
	 * {@link biolockj.Config} Integer property: {@value #SCRIPT_TIMEOUT}<br>
	 * Sets # of minutes before worker scripts times out.
	 */
	public static final String SCRIPT_TIMEOUT = "script.timeout";
}
