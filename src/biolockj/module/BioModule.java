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

/**
 * Classes that implement this interface are eligible to be included in a BioLockJ pipeline.<br>
 * Use the <b>#BioModule</b> tag with the class name in the {@link biolockj.Config} file to include a module.<br>
 * The {@link biolockj.Pipeline} class executes BioModules in the order provided in the {@link biolockj.Config}
 * file.<br>
 * <p>
 * <b>BioModule Directory Structure</b><br>
 * <table summary="BioModule Directories" cellpadding="4">
 * <tr>
 * <th>Directory</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>output</td>
 * <td>Contains all module output files</td>
 * </tr>
 * <tr>
 * <td>temp</td>
 * <td>Holds intermediate files generated by the module, but are not to be passed on to the next module. This directory
 * will deleted after pipeline execution if {@value biolockj.BioLockJ#PROJECT_DELETE_TEMP_FILES} =
 * {@value biolockj.Config#TRUE}</td>
 * </tr>
 * </table>
 */
public interface BioModule
{

	/**
	 * During pipeline initialization, all configured BioModules will run this method to validate dependencies.
	 *
	 * @throws Exception thrown if missing or invalid dependencies are found
	 */
	public void checkDependencies() throws Exception;

	/**
	 * This method executes after execution to update Config modified by the module or other cleanup operations.
	 *
	 * @throws Exception thrown if any runtime error occurs
	 */
	public void cleanUp() throws Exception;

	/**
	 * This is the main method called when it is time for the BioModule to complete its task.
	 *
	 * @throws Exception thrown if the module is unable to complete is task
	 */
	public void executeTask() throws Exception;

	/**
	 * Some BioModules may be added to a pipeline multiple times so must be identified by an ID.<br>
	 * This is the same value as the directory folder prefix when run.<br>
	 * The 1st module ID is 0 (or 00 if there are more than 10 modules.
	 * 
	 * @return Module ID
	 */
	public String getID();

	/**
	 * Each BioModule takes the previous BioModule output as input:<br>
	 * BioModule[ n ].getInputFiles() = BioModule[ n - 1 ].getOutputDir().listFiles()<br>
	 * Special cases:<br>
	 * <ul>
	 * <li>The 1st BioModule return all files in {@value biolockj.Config#INPUT_DIRS}<br>
	 * <li>If previous BioModule BioModule[ n - 1 ] is a MetadataModule, forward it's input + output file: BioModule[ n
	 * ].getInputFiles() = BioModule[ n -1 ].getInputFiles() + BioModule[ n -1 ].getOutputFiles()
	 * </ul>
	 * 
	 * @return Input files
	 * @throws Exception if unable to obtain input files
	 */
	public List<File> getInputFiles() throws Exception;

	/**
	 * Each BioModule generates sub-directory under $DOCKER_PROJ
	 *
	 * @return BioModule root directory
	 */
	public File getModuleDir();

	/**
	 * Output files destined as input for the next BioModule is created in this directory.
	 *
	 * @return File directory containing the primary BioModule output
	 */
	public File getOutputDir();

	/**
	 * {@link biolockj.Pipeline} calls this method when building the list of pipeline BioModules to execute. Any
	 * BioModules returned by this method will be added to the pipeline after the current BioModule. If multiple
	 * post-requisites are found, the modules will be added in the order listed.
	 * 
	 * @return List of BioModules
	 * @throws Exception if invalid Class names are returned as post-requisites
	 */
	public List<Class<?>> getPostRequisiteModules() throws Exception;

	/**
	 * {@link biolockj.Pipeline} calls this method when building the list of pipeline BioModules to execute. Any
	 * BioModules returned by this method will be added to the pipeline before the current BioModule. If multiple
	 * prerequisites are returned, the modules will be added in the order listed.
	 * 
	 * @return List of BioModules
	 * @throws Exception if invalid Class names are returned as prerequisites
	 */
	public List<Class<?>> getPreRequisiteModules() throws Exception;

	/**
	 * Gets the BioModule execution summary, this is sent as part of the notification email, if configured.<br>
	 * Summary should not include data content, to avoid unintentional publication of confidential information.<br>
	 * However, meta-data such as number/size of files can be helpful during debug.<br>
	 *
	 * @return Summary of BioModule execution
	 * @throws Exception if any error occurs
	 */
	public String getSummary() throws Exception;

	/**
	 * Contains intermediate files generated by the module but not used by the next BioModule.<br>
	 * The files may contain supplementary information or data that may be helpful during debug or recovery.<br>
	 * If {@value biolockj.BioLockJ#PROJECT_DELETE_TEMP_FILES} = {@value biolockj.Config#TRUE}, successful pipelines
	 * delete this directory.<br>
	 *
	 * @return File directory of files typically not useful long term
	 */
	public File getTempDir();

	/**
	 * Initialize a new module by passing a unique ID.
	 * 
	 * @param id 2-digit number starting at 00 to identify modules
	 * @throws Exception if errors occur
	 */
	public void init( String id ) throws Exception;

	/**
	 * BioModules {@link #getInputFiles()} method typically, but not always, return the previousModule output files.
	 * This method checks the output directory from the previous module to check for input deemed acceptable by the
	 * current module. The conditions coded in this method will be checked on each previous module in the pipeline until
	 * acceptable input is found. If no previous module produced acceptable input, the files under
	 * {@link biolockj.Config}.{@value biolockj.Config#INPUT_DIRS} are returned.<br>
	 * <br>
	 * This method can be overridden by modules that need input files generated prior to the previous module.
	 * 
	 * @param previousModule BioModule that ran before the current BioModule
	 * @return boolean TRUE if the previousModule output is acceptable input for the current BioModule
	 * @throws Exception if unexpected errors occur
	 */
	public boolean isValidInputModule( BioModule previousModule ) throws Exception;

	/**
	 * Script prefix appended to start of file name to indicate the main script in the script directory. BioModules that
	 * generate scripts must create exactly one main script.
	 */
	public static final String MAIN_SCRIPT_PREFIX = "MAIN_";

	/**
	 * Name of the output sub-directory
	 */
	public static final String OUTPUT_DIR = "output";

	/**
	 * Name of the temporary sub-directory
	 */
	public static final String TEMP_DIR = "temp";
}
