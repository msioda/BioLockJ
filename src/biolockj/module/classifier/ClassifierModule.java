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
import biolockj.exception.ConfigViolationException;
import biolockj.module.SeqModule;

/**
 * Classifier {@link biolockj.module.BioModule}s build one or more bash scripts to call the application on sequence
 * files. Contains methods to build bash scripts for taxonomic assignment of paired and unpaired reads. Contains methods
 * to get classifier command (loaded module, executable file path, or in the $USER $PATH) and optional command line
 * parameters.}
 */
public interface ClassifierModule extends SeqModule {
	/**
	 * Get the executable required to classify your samples.
	 *
	 * @return Command to execute classifier program
	 * @throws ConfigViolationException if the classifier ".exe" property violates buiness rules
	 */
	public String getClassifierExe() throws ConfigViolationException;

	/**
	 * Get optional list of parameters to append whenever the classifier executable is called.
	 *
	 * @return Runtime parameters
	 * @throws Exception thrown if parameters defined are invalid
	 */
	public List<String> getClassifierParams() throws Exception;
}
