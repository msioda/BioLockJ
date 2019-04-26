/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 18, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.r;

import java.util.List;
import biolockj.Config;

/**
 * This BioModule is used to build the R script used to generate p-value histograms for each report field and each
 * taxonomy level configured.
 * 
 * @blj.web_desc R Plot P-value Histograms
 */
public class R_PlotPvalHistograms extends R_Module {
	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		Config.requireString( this, P_VAL_CUTOFF );
	}

	/**
	 * Returns {@link #getStatPreReqs()}
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		return getStatPreReqs();
	}

}
