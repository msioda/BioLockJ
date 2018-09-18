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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.r.R_Module;
import biolockj.module.report.AddMetadataToOtuTables;
import biolockj.module.report.Email;
import biolockj.module.report.JsonReport;
import biolockj.util.r.BaseScript;
import biolockj.util.r.PlotScript;

/**
 * This utility is used to validate the metadata to help ensure the format is valid R script input.
 */
public final class DownloadUtil
{
	// Prevent instantiation
	private DownloadUtil()
	{}

	/**
	 * Save a copy of the module R scripts into their output directory after changing any cluster pipeline root
	 * directory paths found in the script with the download direction from the Config File
	 * 
	 * @param module R_Module
	 * @throws Exception if any errors occur
	 */
	public static void copyLocalScripts( final R_Module module ) throws Exception
	{
		final IOFileFilter ff = new WildcardFileFilter( "*" + R_Module.R_EXT );
		final Collection<File> scripts = FileUtils.listFiles( module.getScriptDir(), ff, null );

		for( final File script: scripts )
		{
			final File newScript = new File(
					module.getOutputDir().getAbsolutePath() + File.separator + script.getName() );

			Log.get( DownloadUtil.class ).info(
					"Copy localized version of R script to output dir for download: " + newScript.getAbsolutePath() );

			final BufferedReader reader = SeqUtil.getFileReader( script );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( newScript ) );
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				String newLine = line;
				if( line.startsWith( R_Module.R_BASE_DIR ) )
				{
					newLine = R_Module.R_BASE_DIR + " = \"" + Config.requireString( DOWNLOAD_DIR ) + "\"";
				}

				if( line.startsWith( "source" ) && line.contains( BaseScript.class.getSimpleName() + R_Module.R_EXT ) )
				{
					newLine = "source(\"" + getDownloadDirPath() + File.separator + BaseScript.class.getSimpleName()
							+ R_Module.R_EXT + "\" )";
				}
				else if( line.startsWith( "source" )
						&& line.contains( PlotScript.class.getSimpleName() + R_Module.R_EXT ) )
				{
					newLine = "source(\"" + getDownloadDirPath() + File.separator + PlotScript.class.getSimpleName()
							+ R_Module.R_EXT + "\" )";
				}
				else if( line.startsWith( R_Module.R_OUTPUT_DIR ) || line.startsWith( R_Module.R_TEMP_DIR ) )
				{
					newLine = line.substring( 0, line.indexOf( "," ) + 1 ) + " \""
							+ Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getName() + File.separator
							+ R_Module.R_LOCAL_DIR_NAME + File.separator + "\" )";
				}
				else if( line.startsWith( R_Module.R_TABLE_DIR ) || line.startsWith( R_Module.R_STATS_DIR )
						|| line.startsWith( R_Module.R_SCRIPT_DIR ) )
				{
					newLine = line.substring( 0, line.indexOf( "," ) + 1 ) + " \""
							+ Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getName() + "\" )";
				}

				if( !line.equals( newLine ) )
				{
					Log.get( DownloadUtil.class ).info( script.getName() + ": replace: [ " + line + BioLockJ.RETURN
							+ " ] with [" + newLine + " ]" );
				}

				writer.write( newLine + R_Module.RETURN );
			}

			reader.close();
			writer.close();
		}
	}

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
			if( modules.get( modules.size() - 1 ) instanceof R_Module )
			{
				makeRunAllScript( modules );
			}

			String status = null;
			String targets = Log.getFile().getAbsolutePath() + " " + SummaryUtil.getSummaryFile().getAbsolutePath();
			BigInteger downloadSize = BigInteger.valueOf( 0L );
			for( final BioModule module: modules )
			{
				if( module instanceof R_Module )
				{
					copyLocalScripts( (R_Module) module );
				}

				if( status == null && !ModuleUtil.isComplete( module ) )
				{
					status = "Failed ";
				}

				final BigInteger size = FileUtils.sizeOfAsBigInteger( module.getOutputDir() );
				downloadSize = downloadSize.add( size );
				targets = targets + " " + module.getOutputDir().getAbsolutePath() + File.separator + "*";
			}

			final BigInteger logSize = FileUtils.sizeOfAsBigInteger( Log.getFile() );
			downloadSize = downloadSize.add( logSize );
			Log.get( DownloadUtil.class ).info( "Log Size: " + FileUtils.byteCountToDisplaySize( logSize ) );
			Log.get( DownloadUtil.class )
					.info( "Full Download Size: " + FileUtils.byteCountToDisplaySize( downloadSize ) );

			final BigInteger summarySize = FileUtils.sizeOfAsBigInteger( SummaryUtil.getSummaryFile() );
			downloadSize = downloadSize.add( summarySize );
			Log.get( DownloadUtil.class ).info( "Summary Size: " + FileUtils.byteCountToDisplaySize( summarySize ) );
			Log.get( DownloadUtil.class )
					.info( "Full Download Size: " + FileUtils.byteCountToDisplaySize( downloadSize ) );

			final String label = getDownloadModules().size() > 1 ? "Modules --> ": "Module --> ";
			final String displaySize = FileUtils.byteCountToDisplaySize( downloadSize );
			final String cmd = "mkdir " + getDownloadDirPath() + "; scp -rp " + getClusterUser() + "@"
					+ Config.requireString( Email.CLUSTER_HOST ) + ":\"" + targets + "\" ";

			return "Download " + ( status == null ? "Completed ": status ) + label + " [" + displaySize + "]:"
					+ BioLockJ.RETURN + cmd + getDownloadDirPath() + File.separator;
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
	 * Create the output directory for local output in the download module.
	 * 
	 * @param module R_Module
	 * @throws Exception if unable to create local directories
	 */
	protected static void createLocalOutputDir( final R_Module module ) throws Exception
	{
		final File f = new File( module.getOutputDir().getAbsolutePath() + File.separator + R_Module.R_LOCAL_DIR_NAME );
		f.mkdir();
		if( !f.exists() )
		{
			throw new Exception( "Unable to build directory: " + f.getAbsolutePath() );
		}
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
				createLocalOutputDir( (R_Module) rModules.get( rModules.size() - 1 ) );
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
			Log.get( SummaryUtil.class ).warn( "Unable to find any executed modules to summarize: " + ex.getMessage() );
		}
		return null;
	}

	private static void makeRunAllScript( final List<BioModule> modules ) throws Exception
	{
		final List<String> lines = new ArrayList<>();
		lines.add( "# Execute this script to run all R scripts on your local workstation" );
		for( final BioModule mod: modules )
		{
			if( !( mod instanceof AddMetadataToOtuTables ) && !( mod instanceof JsonReport ) )
			{
				lines.add( "source(\"" + getDownloadDirPath() + File.separator + R_Module.MAIN_SCRIPT_PREFIX
						+ mod.getClass().getSimpleName() + R_Module.R_EXT + "\")" );
			}
		}

		final String path = modules.get( modules.size() - 1 ).getOutputDir().getAbsolutePath() + File.separator
				+ RUN_ALL_SCRIPT;
		final File script = new File( path );

		final BufferedWriter writer = new BufferedWriter( new FileWriter( script ) );
		for( final String line: lines )
		{
			writer.write( line + BioLockJ.RETURN );
		}
		writer.close();
	}

	/**
	 * {@link biolockj.Config} String property: {@value #DOWNLOAD_DIR}<br>
	 * Sets the local directory targeted by the scp command.
	 */
	protected static final String DOWNLOAD_DIR = "project.downloadDir";

	private static final String RUN_ALL_SCRIPT = "Run_All" + R_Module.R_EXT;

}
