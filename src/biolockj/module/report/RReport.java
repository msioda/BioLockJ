/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 18, 2017
 * @disclaimer 	This code is free software; you can redistribute it and/or
 * 				modify it under the terms of the GNU General Public License
 * 				as published by the Free Software Foundation; either version 2
 * 				of the License, or (at your option) any later version,
 * 				provided that any use properly credits the author.
 * 				This program is distributed in the hope that it will be useful,
 * 				but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * 				GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.report;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModuleImpl;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.r.RScript;
import biolockj.util.r.RScriptBuilder;

/**
 * This class builds the R script.
 */
public class RReport extends BioModuleImpl
{

	/**
	 * Set the y-label based on if logNormal or not.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		Config.requireString( Config.REPORT_LOG_BASE );
		Config.requireExistingFile( Config.INPUT_METADATA );
		Config.requireString( EXE_RSCRIPT );
		Config.requirePositiveInteger( R_TIMEOUT );
		RScriptBuilder.checkDependencies();
	}

	/**
	 * Build the R script.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		initializeMainScript();
		initializeAttributes();
		RScriptBuilder.buildScript( this, true, true );
		prepDownload();
	}

	/**
	 * Run EXE_RSCRIPT command on the R Script
	 */
	@Override
	public String[] getJobParams() throws Exception
	{
		final String[] cmd = new String[ 2 ];
		cmd[ 0 ] = Config.requireString( EXE_RSCRIPT );
		cmd[ 1 ] = ModuleUtil.getMainScript( this ).getAbsolutePath();
		Log.out.info( "Executing Script: " + ModuleUtil.getMainScript( this ).getName() );
		return cmd;
	}

	/**
	 * Summary message.
	 */
	@Override
	public String getSummary()
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			final String rSpacer = "------------------------------------------";

			final IOFileFilter debugLogFF = new WildcardFileFilter(
					"*" + RScriptBuilder.DEBUG_LOG_PREFIX + "*" + RScriptBuilder.LOG_EXT );

			final IOFileFilter logTsvFF = new WildcardFileFilter(
					Config.getLogPrefix() + "*" + RScriptBuilder.TSV_EXT );

			final IOFileFilter logPdfFF = new WildcardFileFilter(
					Config.getLogPrefix() + "*" + RScriptBuilder.PDF_EXT );

			final IOFileFilter tsvFF = new WildcardFileFilter(
					RScriptBuilder.DEBUG_LOG_PREFIX + "*" + RScriptBuilder.TSV_EXT );

			final IOFileFilter pdfFF = new WildcardFileFilter(
					RScriptBuilder.DEBUG_LOG_PREFIX + "*" + RScriptBuilder.PDF_EXT );

			final IOFileFilter errorFF = new WildcardFileFilter( "*" + RScript.R_ERROR );

			final Collection<File> debugLogs = FileUtils.listFiles( getOutputDir(), debugLogFF, null );
			final Collection<File> logTsvs = FileUtils.listFiles( getOutputDir(), logTsvFF, null );
			final Collection<File> logPdfs = FileUtils.listFiles( getOutputDir(), logPdfFF, null );
			final Collection<File> tsvs = FileUtils.listFiles( getOutputDir(), tsvFF, null );
			final Collection<File> pdfs = FileUtils.listFiles( getOutputDir(), pdfFF, null );
			final Collection<File> errorFiles = FileUtils.listFiles( getOutputDir(), errorFF, null );

			tsvs.removeAll( logTsvs );
			pdfs.removeAll( logPdfs );

			final StringBuffer errors = new StringBuffer();
			if( !errorFiles.isEmpty() )
			{
				errors.append( BioLockJ.INDENT + rSpacer + RETURN );
				errors.append( BioLockJ.INDENT + "R Script Errors:" + RETURN );
				final BufferedReader r = ModuleUtil.getFileReader( errorFiles.iterator().next() );
				for( String line = r.readLine(); line != null; line = r.readLine() )
				{
					errors.append( BioLockJ.INDENT + line + RETURN );
				}
				errors.append( BioLockJ.INDENT + rSpacer + RETURN );
			}

			final File rScript = ModuleUtil.getMainScript( this );
			if( ( rScript == null ) || !rScript.exists() )
			{
				sb.append( "Failed to generate R Script" + RETURN );
			}
			else
			{
				sb.append( "R Script " + ( errors.toString().isEmpty() ? "successful": "failed" ) + ": "
						+ rScript.getAbsolutePath() + RETURN );
				sb.append( "Generated " + logTsvs.size() + " normalized TSV reprots" + RETURN );
				sb.append( "Generated " + logPdfs.size() + " normalized PDF reports" + RETURN );
				sb.append( "Generated " + logTsvs.size() + " " + Config.getLogPrefix() + "normalized TSV reprots"
						+ RETURN );
				sb.append( "Generated " + logTsvs.size() + " " + Config.getLogPrefix() + "normalized PDF reports"
						+ RETURN );
				sb.append( "Generated " + debugLogs.size() + " R Script [DEBUG] log files" + RETURN );
				sb.append( errors.toString() );
			}

			return sb.toString();
		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to produce module summary for: " + getClass().getName() + " : " + ex.getMessage(),
					ex );
		}

		return "Error occurred attempting to build BioModule summary!";
	}

	/**
	 * The R Script should run quickly, timeout = 10 minutes appears to work well.
	 */
	@Override
	public Integer getTimeout()
	{
		try
		{
			return Config.requirePositiveInteger( R_TIMEOUT );
		}
		catch( final Exception ex )
		{
			Log.out.error( ex.getMessage(), ex );
		}
		return null;
	}

	protected void prepDownload() throws Exception
	{
		final String downloadDir = Config.getDownloadDir();
		if( downloadDir == null )
		{
			return;
		}

		final File newScript = new File(
				getOutputDir().getAbsolutePath() + File.separator + ModuleUtil.getMainScript( this ).getName() );
		final BufferedReader reader = new BufferedReader( new FileReader( ModuleUtil.getMainScript( this ) ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newScript ) );
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			if( line.startsWith( RScript.OUTPUT_DIR + " =" ) )
			{
				line = RScript.OUTPUT_DIR + " = \"" + downloadDir + LOCAL + File.separator + "\"";
			}
			if( line.startsWith( RScript.SCRIPT_DIR + " =" ) )
			{
				line = RScript.SCRIPT_DIR + " = \"" + downloadDir + "\"";
			}
			if( line.startsWith( RScript.INPUT_DIR + " =" ) )
			{
				line = RScript.INPUT_DIR + " = \"" + downloadDir + TABLES + File.separator + "\"";
			}
			writer.write( line + RETURN );
		}

		reader.close();
		writer.close();

		final File localDir = new File( getOutputDir().getAbsolutePath() + File.separator + LOCAL );
		localDir.mkdirs();
		final File tableDir = new File( getOutputDir().getAbsolutePath() + File.separator + TABLES );
		tableDir.mkdirs();
		FileUtils.copyDirectory( getInputDir(), tableDir );
	}

	private void initializeAttributes() throws Exception
	{

		if( Config.getBoolean( Config.REPORT_NUM_READS ) )
		{
			if( MetaUtil.getAttributeNames().contains( MetaUtil.NUM_READS ) )
			{
				MetaUtil.addNumericField( MetaUtil.NUM_READS );
			}
			else
			{
				Log.out.warn( "Unable to report " + MetaUtil.NUM_READS + " - value not found in metadata: "
						+ MetaUtil.getFile().getAbsolutePath() );
			}
		}

		if( Config.getBoolean( Config.REPORT_NUM_HITS ) )
		{
			if( MetaUtil.getAttributeNames().contains( MetaUtil.NUM_HITS ) )
			{
				MetaUtil.addNumericField( MetaUtil.NUM_HITS );
			}
			else
			{
				Log.out.warn( "Unable to report " + MetaUtil.NUM_HITS + " - value not found in metadata: "
						+ MetaUtil.getFile().getAbsolutePath() );
			}
		}
	}

	private void initializeMainScript() throws Exception
	{
		final File f = new File(
				getScriptDir().getAbsolutePath() + File.separator + MAIN_SCRIPT_PREFIX + R_SCRIPT_NAME );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( f ) );
		writer.write( "# BioLockJ generated script for project: "
				+ Config.requireExistingDir( Config.PROJECT_DIR ).getName() );
		writer.close();
	}

	private static final String EXE_RSCRIPT = "exe.rScript";
	private static final String LOCAL = "local";
	private static final String R_SCRIPT_NAME = "BioLockJ.R";
	private static final String R_TIMEOUT = "r.timeout";
	private static final String TABLES = "tables";

}
