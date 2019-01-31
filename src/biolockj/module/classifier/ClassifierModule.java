/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 2, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.classifier;

import java.util.List;
import biolockj.Constants;
import biolockj.module.SeqModule;

/**
 * Classifier {@link biolockj.module.BioModule}s build one or more bash scripts to call the application on sequence
 * files. Contains methods to build bash scripts for taxonomic assignment of paired and unpaired reads. Contains methods
 * to get classifier command (loaded module, executable file path, or in the $USER $PATH) and optional command line
 * parameters, read from {@link biolockj.Config}.{@value #EXE_CLASSIFIER_PARAMS}
 */
public interface ClassifierModule extends SeqModule
{
	/**
	 * Get the executable from {@link biolockj.Config} {@value #EXE_CLASSIFIER} required to classify the samples.
	 *
	 * @return Command to execute classifier program
	 * @throws Exception if the classifier program undefined or invalid
	 */
	public String getClassifierExe() throws Exception;

	/**
	 * Get optional list of parameters from {@link biolockj.Config} {@value #EXE_CLASSIFIER_PARAMS} to append whenever
	 * the classifier executable is called.
	 *
	 * @return Runtime parameters
	 * @throws Exception thrown if parameters defined are invalid
	 */
	public List<String> getClassifierParams() throws Exception;

	/**
	 * {@link biolockj.Config} property for classifier program executable: {@value #EXE_CLASSIFIER}
	 */
	public static final String EXE_CLASSIFIER = "exe.classifier";

	/**
	 * {@link biolockj.Config} property for classifier program optional parameters: {@value #EXE_CLASSIFIER_PARAMS}
	 */
	public static final String EXE_CLASSIFIER_PARAMS = "exe.classifierParams";

	/**
	 * File suffix appended to processed samples in the module output directory: {@value #PROCESSED}
	 */
	public static final String PROCESSED = "_reported" + Constants.TSV_EXT;
}
