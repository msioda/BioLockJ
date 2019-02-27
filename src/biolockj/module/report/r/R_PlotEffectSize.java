/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 04, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report.r;

import java.util.List;
import biolockj.Config;
import biolockj.Constants;
import biolockj.exception.ConfigViolationException;
import biolockj.module.ScriptModule;
import biolockj.module.report.taxa.NormalizeTaxaTables;
import biolockj.util.BioLockJUtil;
import biolockj.util.RMetaUtil;

/**
 * This BioModule is used to run the R script used to generate OTU-metadata fold-change-barplots for each binary report
 * field. A pdf is created for each taxonomy level.
 * 
 * @blj.web_desc R Plot Effect Size
 */
public class R_PlotEffectSize extends R_Module implements ScriptModule
{
	/**
	 * At least one of the available plot types should NOT be disabled.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.getString( this, R_COLOR_HIGHLIGHT );
		
		// Use single "&" to ensure all config values saved to MASTER config
		if( Config.getBoolean( this, Constants.R_PLOT_EFFECT_SIZE_DISABLE_FC ) & Config.getBoolean( this, NO_COHENS_D )
				& Config.getBoolean( this, NO_R2 ) )
		{
			throw new ConfigViolationException( NO_COHENS_D,
					"When using " + this.getClass().getName() + " at least one of " + NO_COHENS_D + ", " + NO_R2
							+ ", or " + Constants.R_PLOT_EFFECT_SIZE_DISABLE_FC + " must not be true." );
		}

		if( !Config.getBoolean( this, Constants.R_PLOT_EFFECT_SIZE_DISABLE_FC )
				&& RMetaUtil.getBinaryFields( this ).isEmpty() )
		{
			throw new ConfigViolationException( Constants.R_PLOT_EFFECT_SIZE_DISABLE_FC,
					"Requires binary report fields" );
		}
	}

	/**
	 * Returns {@link #getStatPreReqs()} and if fold change plots are to be generated, add
	 * {@link biolockj.module.report.taxa.NormalizeTaxaTables}
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
	{
		final List<String> preReqs = getStatPreReqs();
		if( !Config.getBoolean( this, Constants.R_PLOT_EFFECT_SIZE_DISABLE_FC )
				&& !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_NORMAL_TAXA_COUNT_TABLE_INPUT_TYPE ) )
		{
			preReqs.add( NormalizeTaxaTables.class.getName() );
		}
		return preReqs;
	}

	private static final String NO_COHENS_D = "r_PlotEffectSize.disableCohensD";
	private static final String NO_R2 = "r_PlotEffectSize.disableRSquared";

}