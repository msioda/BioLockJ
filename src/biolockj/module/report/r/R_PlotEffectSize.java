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
import biolockj.exception.ConfigViolationException;
import biolockj.module.ScriptModule;
import biolockj.module.report.taxa.BuildTaxaTables;
import biolockj.util.BioLockJUtil;
import biolockj.util.ModuleUtil;

/**
 * This BioModule is used to run the R script used to generate OTU-metadata fold-change-barplots for each binary report
 * field. A pdf is created for each taxonomy level.
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
		final boolean allDisabled = Config.getBoolean( this, NO_FOLD_CHANGE ) & Config.getBoolean( this, NO_COHENS_D )
				& Config.getBoolean( this, NO_R2 );
		if( allDisabled )
		{
			throw new ConfigViolationException( NO_COHENS_D,
					"When using " + this.getClass().getName() + " at least one of " + NO_COHENS_D + ", " + NO_R2
							+ ", or " + NO_FOLD_CHANGE + " must not be true." );
		}
	}

	/**
	 * Returns {@link #getStatPreReqs()} and if fold change plots are to be generated, ensures
	 * the correct metaMerged module/modules are generated
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
	{
		final List<String> preReqs = getStatPreReqs();
		if( !Config.getBoolean( this, NO_FOLD_CHANGE ) && !BioLockJUtil.pipelineInputType( BioLockJUtil.PIPELINE_R_INPUT_TYPE ) )
		{
			preReqs.add( ModuleUtil.getMetaMergedModule( this ) );
		}
		return preReqs;
	}

	private final String NO_COHENS_D = "r.plotEffectSize.disableCohensD";
	private final String NO_FOLD_CHANGE = "r.plotEffectSize.disableFoldChange";
	private final String NO_R2 = "r.plotEffectSize.disableRSquared";

}