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

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.ScriptModule;
import biolockj.module.implicit.parser.wgs.Humann2Parser;
import biolockj.module.report.Email;
import biolockj.module.report.JsonReport;
import biolockj.module.report.humann2.AddMetadataToPathwayTables;
import biolockj.module.report.r.R_Module;
import biolockj.module.report.r.R_PlotEffectSize;
import biolockj.module.report.taxa.AddMetadataToTaxaTables;
import biolockj.module.report.taxa.BuildTaxaTables;

/**
 * This utility is used to validate the metadata to help ensure the format is valid R script input.
 */
public final class DownloadUtil
{
	// Prevent instantiation
	private DownloadUtil()
	{}

	/**
	 * If running on cluster, build command for user to download pipeline analysis.<br>
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
			final File pipeRoot = new File( Config.pipelinePath() );

			boolean hasRmods = false;
			final String status = ( BioLockJ.isPipelineComplete() ? "completed" : "failed" ) + " pipeline -->";
			final Set<File> files = new TreeSet<>();
			for( final BioModule module: modules )
			{
				Log.info( DownloadUtil.class, "Updating download list for " + module.getClass().getSimpleName() );
				if( module instanceof R_Module )
				{
					hasRmods = true;
					files.addAll( FileUtils.listFiles( module.getModuleDir(), TrueFileFilter.INSTANCE,
							getDirFilter( true ) ) );
				}
				else
				{
					files.addAll( FileUtils.listFiles( module.getModuleDir(), TrueFileFilter.INSTANCE,
							getDirFilter( false ) ) );
				}
			}

			if( hasRmods )
			{
				makeRunAllScript( modules );
			}


			files.addAll( Arrays.asList( pipeRoot.listFiles() ) );

			final List<File> downFiles = buildDownloadList( files );
			final String displaySize = FileUtils.byteCountToDisplaySize( getDownloadSize( downFiles ) );
			final String cmd = "rsync -v --times --files-from=:" + getDownloadListFile().getAbsolutePath() + " " + getClusterUser() + "@"
					+ Config.requireString( null, Email.CLUSTER_HOST ) + ":" + Config.pipelinePath() + " " + getDownloadDirPath();
			
			return "Download " + status + " [ " + displaySize + " ]:" + RETURN + cmd;
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
		String dir = Config.getString( null, DOWNLOAD_DIR );
		if( dir != null )
		{
			if( !dir.endsWith( File.separator ) )
			{
				dir = dir + File.separator;
			}

			return dir + Config.pipelineName();
		}

		return null;
	}

	/**
	 * Get the download list file.
	 * 
	 * @return File containing list of files to download
	 * @throws Exception if errors occur
	 */
	public static File getDownloadListFile() throws Exception
	{
		return new File( Config.pipelinePath() + File.separator + DOWNLOAD_LIST );
	}

	/**
	 * Get the total size of all files included for download.
	 * 
	 * @param files List of download files
	 * @return BigInteger total download size
	 * @throws Exception if errors occur
	 */
	protected static BigInteger getDownloadSize( List<File> files ) throws Exception
	{
		int i = 0;
		BigInteger downloadSize = BigInteger.valueOf( 0L );
		Log.info( DownloadUtil.class, "Calculatng download size.  Initial value: " + FileUtils.byteCountToDisplaySize( downloadSize ) );
		for( File file: files )
		{
			Log.info( DownloadUtil.class,  "File [  " + file.getName() + " ] STANDARD sizeOf( file ) = " +  FileUtils.sizeOf( file ) );
			Log.info( DownloadUtil.class, "File [  " + file.getName() + " ] = { " + FileUtils.sizeOfAsBigInteger( file ) + " } --> " + FileUtils.byteCountToDisplaySize( FileUtils.sizeOfAsBigInteger( file ) ) );
			downloadSize = downloadSize.add( FileUtils.sizeOfAsBigInteger( file ) );
			Log.info( DownloadUtil.class, "Total after iteration [ " + (i++) + "]: { " + downloadSize + " } --> " + FileUtils.byteCountToDisplaySize( downloadSize ) );
		}
		
		Log.info( DownloadUtil.class, "FINAL download size: { " + downloadSize  + " } --> " + FileUtils.byteCountToDisplaySize( downloadSize ) );
		return downloadSize;
	}


	private static File getRunAllRScript() throws Exception
	{
		return new File( Config.pipelinePath() + File.separator + RUN_ALL_SCRIPT );
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
	 * Get a directory name filter to include output and (optionally) script folders in file searches.
	 * 
	 * @param includeScript include the script directory
	 * @return a file name filter
	 * @throws Exception if errors occur
	 */
	protected static IOFileFilter getDirFilter( final boolean includeScript ) throws Exception
	{
		final ArrayList<String> dirFilter = new ArrayList<>();
		if( includeScript )
		{
			dirFilter.add( "script" );
		}
		dirFilter.add( "output" );
		return new NameFileFilter( dirFilter );
	}

	/**
	 * Get the modules to download. The following are downloaded if found in the pipeline:
	 * <ul>
	 * <li>{@link biolockj.module.report.taxa.AddMetadataToTaxaTables}
	 * <li>{@link biolockj.module.report.humann2.AddMetadataToPathwayTables}
	 * <li>{@link biolockj.module.report.JsonReport}
	 * <li>Any module that inherits from {@link biolockj.module.report.r.R_Module}
	 * </ul>
	 * 
	 * If pipeline contains {@link biolockj.module.report.r.R_PlotEffectSize} and
	 * {@link biolockj.Config}.{@value biolockj.Constants#R_PLOT_EFFECT_SIZE_DISABLE_FC}={@value biolockj.Constants#FALSE}
	 * include additional modules:
	 * <ul>
	 * <li>{@link biolockj.module.implicit.parser.wgs.Humann2Parser}
	 * <li>{@link biolockj.module.report.taxa.BuildTaxaTables}
	 * </ul>
	 * 
	 * @return BioModules to download
	 */
	protected static List<BioModule> getDownloadModules()
	{
		final List<BioModule> modules = new ArrayList<>();
		try
		{
			for( final BioModule module: Pipeline.getModules() )
			{
				final boolean includeRawCounts = ModuleUtil.moduleExists( R_PlotEffectSize.class.getName() )
						&& Config.getBoolean( module, Constants.R_PLOT_EFFECT_SIZE_DISABLE_FC );

				final boolean downloadableType = module instanceof JsonReport || module instanceof R_Module
						|| module instanceof AddMetadataToTaxaTables || module instanceof AddMetadataToPathwayTables
						|| includeRawCounts && ( module instanceof BuildTaxaTables || module instanceof Humann2Parser );

				if( ModuleUtil.hasExecuted( module ) && downloadableType )
				{
					modules.add( module );
				}
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
	 * This script allows a user to run all R scripts together from a single script.
	 * 
	 * @param modules BioModules
	 * @return File Run_All.R script
	 * @throws Exception if errors occur
	 */
	protected static File makeRunAllScript( final List<BioModule> modules ) throws Exception
	{

		final File pipeRoot = new File( Config.pipelinePath() );
		final File script = getRunAllRScript();
		final BufferedWriter writer = new BufferedWriter( new FileWriter( script ) );

		if( Config.getString( null, ScriptModule.SCRIPT_DEFAULT_HEADER ) != null )
		{
			writer.write( Config.getString( null, ScriptModule.SCRIPT_DEFAULT_HEADER ) + RETURN + RETURN );
		}

		writer.write( "# Use this script to locally run R modules." + RETURN );

		for( final BioModule mod: modules )
		{
			if( mod instanceof R_Module )
			{
				final String relPath = pipeRoot.toURI().relativize( ( (R_Module) mod ).getPrimaryScript().toURI() )
						.toString();
				// do not use exe.Rscript config option, this is a convenience for the users local system not for the
				// system where biolockj ran.
				writer.write( "Rscript " + relPath + RETURN );
			}
		}

		writer.close();

		return script;
	}

	/**
	 * Add files to {@value biolockj.util.DownloadUtil#DOWNLOAD_LIST} in pipeline root directory. 
	 * 
	 * @param files - files to add to the download list
	 * @return List of files for download
	 * @throws Exception if errors occur
	 */
	protected static List<File> buildDownloadList( final Collection<File> files ) throws Exception
	{
		final List<File> downFiles = new ArrayList<>();
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getDownloadListFile() ) );
		try
		{
			final File pipeRoot = new File( Config.pipelinePath() );
			for( final File file: files )
			{
				if( FileUtils.sizeOf( file ) != 0 && !file.isDirectory() && !file.getName().startsWith( "." ) )
				{
					downFiles.add(  file );
					final String relPath = pipeRoot.toURI().relativize( file.toURI() ).toString();
					writer.write( relPath + RETURN );
				}
			}
		}
		finally
		{
			if( writer != null ) writer.close();
		}

		return downFiles;
	}

	/**
	 * Name of the file holding the list of pipeline files to include when running {@link biolockj.util.DownloadUtil}
	 */
	public static final String DOWNLOAD_LIST = "downloadList.txt";

	/**
	 * {@link biolockj.Config} String property: {@value #DOWNLOAD_DIR}<br>
	 * Sets the local directory targeted by the scp command.
	 */
	protected static final String DOWNLOAD_DIR = "project.downloadDir";

	private static final String RETURN = Constants.RETURN;
	private static final String RUN_ALL_SCRIPT = "Run_All_R" + Constants.SH_EXT;
}
