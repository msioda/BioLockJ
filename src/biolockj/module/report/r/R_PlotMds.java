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

import biolockj.Config;
import biolockj.module.ScriptModule;

/**
 * This BioModule is used to build the R script used to generate MDS plots for each report field and each taxonomy level
 * configured.
 * 
 * @blj.web_desc R Plot MDS
 */
public class R_PlotMds extends R_Module implements ScriptModule {

    /**
     * Require {@link biolockj.Config}.{@value #R_MDS_NUM_AXIS} set to integer greater than 2
     */
    @Override
    public void checkDependencies() throws Exception {
        super.checkDependencies();
        Config.requireString( this, R_MDS_DISTANCE );
        Config.getString( this, R_COLOR_BASE );
        Config.getString( this, R_COLOR_HIGHLIGHT );
        Config.getString( this, R_COLOR_PALETTE );
        Config.getString( this, R_PCH );
        if( Config.requirePositiveInteger( this, R_MDS_NUM_AXIS ) < 2 )
            throw new Exception( "Config property [" + R_MDS_NUM_AXIS + "] must be > 2" );
    }

    /**
     * {@link biolockj.Config} property: {@value #R_MDS_DISTANCE} defines the distance index to use in the capscale
     * command.
     */
    protected static final String R_MDS_DISTANCE = "r_PlotMds.distance";

    /**
     * {@link biolockj.Config} Integer property: {@value #R_MDS_NUM_AXIS} defines the number of MDS axis to report
     */
    protected static final String R_MDS_NUM_AXIS = "r_PlotMds.numAxis";

    /**
     * {@link biolockj.Config} List property: {@value #R_MDS_REPORT_FIELDS}<br>
     * List metadata fields to generate MDS ordination plots.
     */
    protected static final String R_MDS_REPORT_FIELDS = "r_PlotMds.reportFields";

}
