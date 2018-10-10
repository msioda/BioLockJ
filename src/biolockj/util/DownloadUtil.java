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
package biolockj.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.ScriptModule;
import biolockj.module.r.R_Module;
import biolockj.module.report.AddMetadataToOtuTables;
import biolockj.module.report.Email;
import biolockj.module.report.JsonReport;

/**
 * This utility is used to validate the metadata to help ensure the format is valid R script input.
 */
public final class DownloadUtil
{
	// Prevent instantiation
	private DownloadUtil()
	{}

	/**
	 * If running on cluster, build scp command for user to download pipeline analysis.<br>
	 * <p>
	 * The pipeline analysis is extracted from the last module output directory (except Email).<br>
	 * Download target = ModuleUtil.getDownloadDir() on the local workstation.<br>
	 * Example download command: scp -rp johnDoe@myUniversity.usa.edu:"dir1, dir2, file1"
	 * /Users/johnDoe/projects/downloads
	 *
	 * @return String (scp command) or null
	 * @throws Exception if {@link biolockj.Config} parameters are missing or invalid
	 */
	public static String getDownloadCmd() throws Exception
	{
		final List<BioModule> modules = getDownloadModules();
		if( Config.isOnCluster() && modules != null && getDownloadDirPath() != null )
		{
			String status = "Complete";
			String targets = getSrc( MAIN_OUT );
			BigInteger downloadSize = FileUtils.sizeOfAsBigInteger( Log.getFile() );
			downloadSize = downloadSize.add( FileUtils.sizeOfAsBigInteger( SummaryUtil.getSummaryFile() ) );
			downloadSize = downloadSize.add( FileUtils.sizeOfAsBigInteger( BioLockJUtil.getMasterConfig() ) );

			if( modules.get( modules.size() - 1 ) instanceof R_Module )
			{
				final File runAll = makeRunAllScript( modules );
				downloadSize = downloadSize.add( FileUtils.sizeOfAsBigInteger( runAll ) );
			}

			for( final BioModule module: modules )
			{
				downloadSize = downloadSize.add( FileUtils.sizeOfAsBigInteger( module.getOutputDir() ) );
				if( module instanceof R_Module )
				{
					targets += " " + getSrc( ModuleUtil.getModuleNum( module ) + "*" + File.separator + "*"
							+ File.separator + getExts( (R_Module) module ) );
					downloadSize = downloadSize
							.add( FileUtils.sizeOfAsBigInteger( ( (ScriptModule) module ).getScriptDir() ) );
					downloadSize = downloadSize.add( FileUtils.sizeOfAsBigInteger( module.getTempDir() ) );
				}
				else
				{
					targets += " " + getSrc( ModuleUtil.getModuleNum( module ) + "*" + File.separator + "output"
							+ File.separator + "*" );
				}

				if( !ModuleUtil.isComplete( module ) )
				{
					status = "Failed";
				}
			}
			final String pipeRoot = Config.getExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath();
			final String label = status + ( getDownloadModules().size() > 1 ? " Modules --> ": " Module --> " );
			final String displaySize = FileUtils.byteCountToDisplaySize( downloadSize );
			final String cmd = "src=" + pipeRoot + "; out=" + getDownloadDirPath() + "; mkdir -p "
					+ getDest( BioModule.OUTPUT_DIR ) + "; mkdir " + getDest( BioModule.TEMP_DIR ) + "; scp -rp "
					+ getClusterUser() + "@" + Config.requireString( Email.CLUSTER_HOST ) + ":\"" + targets + "\" "
					+ DEST;

			return "Download " + label + " [" + displaySize + "]:" + RETURN + cmd;
		}

		return null;
	}

	/**
	 * Get validated {@link biolockj.Config}.{@value #DOWNLOAD_DIR} if running on cluster, otherwise return null
	 *
	 * @return String download directory file or null
	 * @throws Exception thrown if directory is defined but does not exist
	 */
	public static String getDownloadDirPath() throws Exception
	{
		String dir = Config.getString( DOWNLOAD_DIR );
		if( dir != null )
		{
			if( !dir.endsWith( File.separator ) )
			{
				dir = dir + File.separator;
			}

			return dir + Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getName();
		}

		return null;
	}

	/**
	 * If System.user.home is a file path (like /usr/home/johnDoe), return last directory name (johnDoe). Otherwise
	 * return System.user.home value.
	 *
	 * @return The userId
	 */
	protected static String getClusterUser()
	{
		String user = System.getProperty( "user.home" );
		if( user == null || user.trim().length() == 0 )
		{
			return null;
		}
		if( user.lastIndexOf( File.separator ) > 0 )
		{
			user = user.substring( user.lastIndexOf( File.separator ) + 1 );
		}

		return user;
	}

	/**
	 * Get the modules to download. Some modules are always included:
	 * <ul>
	 * <li>{@link biolockj.module.report.AddMetadataToOtuTables}
	 * <li>{@link biolockj.module.report.JsonReport}
	 * <li>Any module that implements {@link biolockj.module.r.R_Module} interface
	 * </ul>
	 * If R modules have run, build local output directory to be included in download via scp command.<br>
	 *
	 * @return BioModules to download
	 */
	protected static List<BioModule> getDownloadModules()
	{
		final List<BioModule> modules = new ArrayList<>();
		try
		{
			BioModule lastModule = null;
			final List<BioModule> rModules = new ArrayList<>();
			for( final BioModule module: Pipeline.getModules() )
			{
				if( ModuleUtil.hasExecuted( module ) && module instanceof JsonReport
						|| module instanceof AddMetadataToOtuTables )
				{
					modules.add( module );
				}
				else if( ModuleUtil.hasExecuted( module ) && module instanceof R_Module )
				{
					rModules.add( module );
				}

				if( ModuleUtil.hasExecuted( module ) && !( module instanceof Email ) )
				{
					lastModule = module;
				}
			}

			if( lastModule != null && !rModules.isEmpty() )
			{
				modules.addAll( rModules );
			}

			if( !modules.contains( lastModule ) )
			{
				modules.add( lastModule );
			}

			return modules;
		}
		catch( final Exception ex )
		{
			Log.warn( DownloadUtil.class, "Unable to find any executed modules to summarize: " + ex.getMessage() );
		}
		return null;
	}

	/**
	 * Get file extensions for each R module to include in scp download command
	 * 
	 * @param module R_Module
	 * @return REGEX to scp specific file estensions
	 * @throws Exception
	 */
	protected static String getExts( final R_Module module ) throws Exception
	{
		if( module.scpExtensions() == null || module.scpExtensions().isEmpty() )
		{
			return "*";
		}

		final StringBuffer sb = new StringBuffer();
		for( final String ext: module.scpExtensions() )
		{
			sb.append( sb.toString().isEmpty() ? "*.{": "," ).append( ext );
		}
		sb.append( "}" );
		return sb.toString();
	}

	/**
	 * This script allows a user to run all R scripts together from a single script.
	 * 
	 * @param modules BioModules
	 * @return File Run_All.R script
	 * @throws Exception if errors occur
	 */
	protected static File makeRunAllScript( final List<BioModule> modules ) throws Exception
	{
		final File script = new File( Config.getExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath()
				+ File.separator + RUN_ALL_SCRIPT );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( script ) );
		writer.write( "# Execute this script to run all R scripts on your local workstation" + RETURN );
		writer.write( R_Module.buildGetModuleScriptDirFunction() );

		for( final BioModule mod: modules )
		{
			if( mod instanceof R_Module )
			{
				writer.write( "source( file.path( " + R_Module.R_FUNCTION_GET_MOD_SCRIPT_DIR + "(), \""
						+ ( (R_Module) mod ).getPrimaryScript().getName() + "\" ) )" + RETURN );
			}
		}

		writer.write( "runAllScript = file.path( pipelineDir, \"" + RUN_ALL_SCRIPT + "\" )" + RETURN );
		writer.write( R_Module.METHOD_RUN_PROGRAM + "( runAllScript )" + RETURN );
		writer.write( R_Module.METHOD_REPORT_STATUS + "( runAllScript )" + RETURN );

		writer.close();

		return script;
	}

	private static String getDest( final String val )
	{
		return DEST + File.separator + val;
	}

	private static String getSrc( final String val )
	{
		return SOURCE + File.separator + val;
	}

	/**
	 * {@link biolockj.Config} String property: {@value #DOWNLOAD_DIR}<br>
	 * Sets the local directory targeted by the scp command.
	 */
	protected static final String DOWNLOAD_DIR = "project.downloadDir";

	/**
	 * List of file extensions download-able from pipeline root directory: {@value #MAIN_OUT}
	 */
	protected final static String MAIN_OUT = "*.{log,properties,txt,R}";

	private static final String DEST = "$out";
	private static final String RETURN = BioLockJ.RETURN;

	private static final String RUN_ALL_SCRIPT = "Run_All" + R_Module.R_EXT;
	private static final String SOURCE = "$src";

}
