/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date May 15, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.r;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.util.ModuleUtil;

/**
 * This BioModule is used to build the R script used to generate taxonomy statistics and plots.
 */
public class CalculateStats extends R_Module implements BioModule
{
	/**
	 * Validate configuration file properties used to build the R report:
	 * <ul>
	 * <li>super.checkDependencies()
	 * <li>Require {@value #R_ADJ_PVALS_SCOPE}
	 * <li>Require {@value #R_PVAL_ADJ_METHOD}
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.requireString( R_ADJ_PVALS_SCOPE );
		Config.requireString( R_PVAL_ADJ_METHOD );
	}

	/**
	 * Return the set of file extensions available for download by {@link biolockj.util.DownloadUtil}
	 * 
	 * @return Set of file extensions
	 * @throws Exception if errors occur
	 */
	@Override
	public Set<String> scpExtensions() throws Exception
	{
		final TreeSet<String> set = (TreeSet<String>) super.scpExtensions();
		set.add( TSV_EXT.substring( 1 ) );
		return set;
	}

	/**
	 * Get the stats file for the given fileType and otuLevel.
	 * 
	 * @param otuLevel OTU level
	 * @param fileType (parametric/non-parametric) (adjusted/non-adjusted)
	 * @return File with stats for fileType
	 * @throws Exception if CalculateStats not found
	 */
	public static File getStatsFile( final String otuLevel, final String fileType ) throws Exception
	{
		final BioModule mod = ModuleUtil.getModule( CalculateStats.class.getName() );
		final String fileName = mod.getOutputDir().getAbsolutePath() + File.separator
				+ Config.getString( Config.INTERNAL_PIPELINE_NAME ) + "_" + otuLevel + "_" + fileType
				+ R_Module.TSV_EXT;
		
		Log.info( CalculateStats.class, "Find stats file: " + fileName );
		final File file = new File( fileName );
		if( file.exists() )
		{
			return file;
		}

		return null;
	}

	/**
	 * Non parametric p-value identifier: {@value #P_VALS_NP}
	 */
	public static final String P_VALS_NP = "nonParametricPvals";

	/**
	 * Non parametric adjusted p-value identifier: {@value #P_VALS_NP_ADJ}
	 */
	public static final String P_VALS_NP_ADJ = P_VALS_NP + "Adj";

	/**
	 * Parametric p-value identifier: {@value #P_VALS_PAR}
	 */
	public static final String P_VALS_PAR = "parametricPvals";

	/**
	 * Parametric adjusted p-value identifier: {@value #P_VALS_PAR_ADJ}
	 */
	public static final String P_VALS_PAR_ADJ = P_VALS_PAR + "Adj";

	/**
	 * R^2 identifier: {@value #R_SQUARED_VALS}
	 */
	public static final String R_SQUARED_VALS = "rSquaredVals";

	/**
	 * This {@value #R_PVAL_ADJ_METHOD} option can be set in {@link biolockj.Config} file: {@value #ADJ_PVAL_ATTRIBUTE}
	 */
	protected static final String ADJ_PVAL_ATTRIBUTE = "ATTRIBUTE";

	/**
	 * This {@value #R_PVAL_ADJ_METHOD} option can be set in {@link biolockj.Config} file: {@value #ADJ_PVAL_GLOBAL}
	 */
	protected static final String ADJ_PVAL_GLOBAL = "GLOBAL";

	/**
	 * This {@value #R_PVAL_ADJ_METHOD} option can be set in {@link biolockj.Config} file: {@value #ADJ_PVAL_LOCAL}
	 */
	protected static final String ADJ_PVAL_LOCAL = "LOCAL";

	/**
	 * This {@value #R_PVAL_ADJ_METHOD} option can be set in {@link biolockj.Config} file: {@value #ADJ_PVAL_TAXA}
	 */
	protected static final String ADJ_PVAL_TAXA = "TAXA";

	/**
	 * {@link biolockj.Config} String property: {@value #R_ADJ_PVALS_SCOPE} defines R p.adjust( n ) parameter. There are
	 * 4 supported options:
	 * <ol>
	 * <li>{@value #ADJ_PVAL_GLOBAL} n = number of all pVal tests for all fields and all taxonomy levels
	 * <li>{@value #ADJ_PVAL_ATTRIBUTE} n = number of all pVal tests for 1 field at all taxonomy levels
	 * <li>{@value #ADJ_PVAL_TAXA} n = number of all pVal tests for all fields at 1 taxonomy level
	 * <li>{@value #ADJ_PVAL_LOCAL} n = number of all pVal tests for 1 field at 1 taxonomy level
	 * </ol>
	 */
	protected static final String R_ADJ_PVALS_SCOPE = "rStats.pAdjustScope";

	/**
	 * {@link biolockj.Config} String property: {@value #R_PVAL_ADJ_METHOD} defines p.adjust( method ) parameter.
	 * p.adjust.methods = c("holm", "hochberg", "hommel", "bonferroni", "BH", "BY", "fdr", "none")
	 */
	protected static final String R_PVAL_ADJ_METHOD = "rStats.pAdjustMethod";
}
