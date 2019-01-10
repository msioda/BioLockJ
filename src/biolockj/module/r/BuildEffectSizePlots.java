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
package biolockj.module.r;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import biolockj.Config;
import biolockj.module.ScriptModule;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.util.ModuleUtil;

/**
 * This BioModule is used to run the R script used to generate OTU-metadata fold-change-barplots for each binary report
 * field. A pdf is created for each taxonomy level.
 */
public class BuildEffectSizePlots extends R_Module implements ScriptModule
{
	/**
	 * Add prerequisite module: {@link biolockj.module.r.CalculateStats}.
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		final List<Class<?>> preReqs = super.getPreRequisiteModules();
		preReqs.add( CalculateStats.class );
		return preReqs;
	}

	/**
	 * Return the set of file extensions available for download by {@link biolockj.util.DownloadUtil} Add
	 * {@value #PDF_EXT} to super class set.
	 * 
	 * @return Set of file extensions
	 * @throws Exception if errors occur
	 */
	@Override
	public Set<String> scpExtensions() throws Exception
	{
		final TreeSet<String> set = (TreeSet<String>) super.scpExtensions();
		set.add( PDF_EXT.substring( 1 ) );
		return set;
	}

	@Override
	public void executeTask() throws Exception
	{
		// TODO Auto-generated method stub
		super.executeTask();
		Config.setConfigProperty( Config.INTERNAL_PARSER_MODULE,
				ModuleUtil.getParserModule().getOutputDir().toString() );
	}

}