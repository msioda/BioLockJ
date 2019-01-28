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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.Config;
import biolockj.module.BioModule;
import biolockj.module.ScriptModule;
import biolockj.module.report.taxa.BuildTaxaTables;

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
	public TreeSet<String> scpExtensions() throws Exception
	{
		final TreeSet<String> set = super.scpExtensions();
		set.add( PDF_EXT.substring( 1 ) );
		return set;
	}
	
	

	@Override
	public void executeTask() throws Exception
	{
		// TODO Auto-generated method stub
		super.executeTask();
		if( Config.getBoolean( "r.plotEffectSize.foldChange" ) )
		{

			
	
			Config.setConfigProperty( "internal.parserModule", getTaxaTablePath() );
			
		}
	}
	
	
	private  String getTaxaTablePath() throws Exception
	{
		final IOFileFilter ff = new WildcardFileFilter( "*" + BuildTaxaTables.class.getSimpleName() );
		Collection<File> files = FileUtils.listFiles( Config.getExistingDir( Config.PROJECT_PIPELINE_DIR ),
				ff, HiddenFileFilter.VISIBLE );
		
		if( !files.isEmpty() )
		{
			File f = files.iterator().next();
			return f.getAbsolutePath() + File.separator + BioModule.OUTPUT_DIR;
		}

		
		return "";
	}

}