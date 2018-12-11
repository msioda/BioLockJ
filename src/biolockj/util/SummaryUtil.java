/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date May 29, 2018
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
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.ScriptModule;

/**
 * This module builds an execution summary for the pipeline which is printed to the log file and is be sent to the user
 * if the Email module is configured.
 */
public class SummaryUtil
{

	/**
	 * Print the final lines of the summary with overall status, runtime, and the download scp command if applicable.
	 * 
	 * @return Summary info
	 * @throws Exception if any error occurs
	 */
	public static String getFooter() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLabel( PIPELINE_NAME ) + "   " + Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + RETURN );
		sb.append( getLabel( PIPELINE_STATUS ) + " " + Pipeline.getStatus().toLowerCase() + "!" + RETURN );
		sb.append( getLabel( PIPELINE_RUNTIME )
				+ ModuleUtil.getRunTime( System.currentTimeMillis() - BioLockJ.APP_START_TIME ) + RETURN );
		sb.append( getLabel( PIPELINE_CONFIG ) + " " + Config.getConfigFilePath() + RETURN );
		sb.append( getLabel( PIPELINE_OUTPUT ) + " "
				+ Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath() + RETURN );

		final String downloadCmd = DownloadUtil.getDownloadCmd();
		if( downloadCmd != null )
		{
			sb.append( SPACER_2X + RETURN );
			sb.append( downloadCmd + RETURN );
		}

		sb.append( SPACER_2X + RETURN );
		return sb.toString();
	}

	/**
	 * Return the mean value
	 * 
	 * @param vals Collection of values
	 * @param isDouble if the Collection holds Double values
	 * @return mean value
	 */
	@SuppressWarnings("unchecked")
	public static String getMean( final Collection<?> vals, final boolean isDouble )
	{
		if( isDouble )
		{
			final Collection<Double> doubleVals = (Collection<Double>) vals;
			return Double.valueOf( doubleVals.stream().mapToDouble( i -> i ).sum() / doubleVals.size() ).toString();
		}

		final Collection<Long> longVals = (Collection<Long>) vals;
		return Long.valueOf( longVals.stream().mapToLong( i -> i ).sum() / longVals.size() ).toString();
	}

	/**
	 * Return the median value
	 * 
	 * @param vals Collection of values
	 * @param isDouble if the Collection holds Double values
	 * @return median value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getMedian( final Collection<?> vals, final boolean isDouble )
	{
		if( vals.size() == 1 )
		{
			return vals.iterator().next().toString();
		}

		List data = null;
		if( isDouble )
		{
			data = new ArrayList<>( (Collection<Double>) vals );
		}
		else
		{
			data = new ArrayList<>( (Collection<Long>) vals );
		}

		Collections.sort( data );

		final int middle = data.size() / 2;
		if( data.size() % 2 == 1 )
		{
			return data.get( data.size() / 2 ).toString();
		}
		else if( isDouble )
		{
			return Double.valueOf( ( (Double) data.get( middle - 1 ) + (Double) data.get( middle ) ) / 2 ).toString();
		}
		else
		{
			return Long.valueOf( ( (Long) data.get( middle - 1 ) + (Long) data.get( middle ) ) / 2 ).toString();
		}
	}

	/**
	 * Return summary of {@link biolockj.module.BioModule} output directory, with metrics:
	 * <ul>
	 * <li>Number of output files
	 * <li>Mean output file size
	 * <li>Path of new metadata file if any created
	 * </ul>
	 *
	 * @param bioModule BioModule to summarize
	 * @return Summary of bioModule output directory
	 */
	public static String getOutputDirSummary( final BioModule bioModule )
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			if( bioModule.getOutputDir().listFiles().length == 0 )
			{
				return "# Files Output: 0" + RETURN;
			}

			final Collection<File> outFiles = FileUtils.listFiles( bioModule.getOutputDir(), HiddenFileFilter.VISIBLE,
					HiddenFileFilter.VISIBLE );
			int count = outFiles.size();

			BigInteger outAvg = FileUtils.sizeOfAsBigInteger( bioModule.getOutputDir() );

			File newMeta = new File(
					bioModule.getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );

			if( newMeta.exists() && bioModule.getOutputDir().listFiles().length > 1 )
			{
				count--;
				outAvg = outAvg.subtract( FileUtils.sizeOfAsBigInteger( newMeta ) );
			}
			else if( !newMeta.exists() )
			{
				newMeta = null;
			}

			if( count == 0 )
			{
				outAvg = BigInteger.valueOf( 0 );
			}
			else
			{
				outAvg = outAvg.divide( BigInteger.valueOf( count ) );
			}

			sb.append( "# Files Output: " + count + RETURN );
			sb.append( "Mean File Size: " + FileUtils.byteCountToDisplaySize( outAvg ) + RETURN );
			sb.append( newMeta == null ? "": "New metadata:   " + newMeta.getAbsolutePath() + RETURN );

		}
		catch( final Exception ex )
		{
			final String msg = "Unable to produce module outputDir summary for: " + bioModule.getClass().getName()
					+ " : " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( SummaryUtil.class, msg );
		}

		return sb.toString();
	}

	/**
	 * Return summary of the {@link biolockj.module.ScriptModule} script directory with metrics:
	 * <ul>
	 * <li>Print main script name
	 * <li>Number of worker scripts run
	 * <li>Number of worker scripts successful/failed/incomplete
	 * <li>Average worker script run time
	 * <li>Longest running worker script names/duration
	 * <li>Longest running workers script names/duration
	 * </ul>
	 *
	 * @param scriptModule ScriptModule to summarize
	 * @return Summary of bioModule script directory
	 */
	public static String getScriptDirSummary( final ScriptModule scriptModule )
	{
		final StringBuffer sb = new StringBuffer();
		try
		{

			if( ModuleUtil.getSubDir( scriptModule, ScriptModule.SCRIPT_DIR ) == null )
			{
				return "";
			}

			if( ModuleUtil.getMainScript( scriptModule ) == null
					|| scriptModule.getScriptDir().listFiles().length == 0 )
			{
				return "No scripts found!" + RETURN;
			}

			final IOFileFilter ff0 = new WildcardFileFilter( "*.sh" );
			final IOFileFilter ffStarted = new WildcardFileFilter( "*.sh_" + Pipeline.SCRIPT_STARTED );
			final IOFileFilter ffSuccess = new WildcardFileFilter( "*.sh_" + Pipeline.SCRIPT_SUCCESS );
			final IOFileFilter ffFailed = new WildcardFileFilter( "*.sh_" + Pipeline.SCRIPT_FAILURES );

			final Collection<File> scripts = FileUtils.listFiles( scriptModule.getScriptDir(), ff0, null );
			final Collection<File> scriptsStarted = FileUtils.listFiles( scriptModule.getScriptDir(), ffStarted, null );
			final Collection<File> scriptsFailed = FileUtils.listFiles( scriptModule.getScriptDir(), ffFailed, null );
			final Collection<File> scriptsSuccess = FileUtils.listFiles( scriptModule.getScriptDir(), ffSuccess, null );

			final File mainSuccess = new File(
					ModuleUtil.getMainScript( scriptModule ).getAbsolutePath() + "_" + Pipeline.SCRIPT_SUCCESS );

			final File mainFail = new File(
					ModuleUtil.getMainScript( scriptModule ).getAbsolutePath() + "_" + Pipeline.SCRIPT_FAILURES );

			final File mainStarted = new File(
					ModuleUtil.getMainScript( scriptModule ).getAbsolutePath() + "_" + Pipeline.SCRIPT_STARTED );

			scripts.remove( ModuleUtil.getMainScript( scriptModule ) );
			scriptsFailed.remove( mainFail );
			scriptsSuccess.remove( mainSuccess );
			scriptsStarted.remove( mainStarted );

			final Map<String, Long> longestScripts = new HashMap<>();
			final Map<String, Long> shortestScripts = new HashMap<>();
			long totalRunTime = 0L;
			final int numCompleted = scriptsSuccess.size() + scriptsFailed.size();
			long maxDuration = 0L;
			long minDuration = Long.MAX_VALUE;

			final long oneMinute = 1000 * 60;

			for( final File script: scripts )
			{
				final File started = new File( script.getAbsolutePath() + "_" + Pipeline.SCRIPT_STARTED );
				File finish = new File( script.getAbsolutePath() + "_" + Pipeline.SCRIPT_SUCCESS );
				if( !finish.exists() )
				{
					finish = new File( script.getAbsolutePath() + "_" + Pipeline.SCRIPT_FAILURES );
				}
				if( finish.exists() )
				{
					final long duration = finish.lastModified() - started.lastModified();
					Log.debug( SummaryUtil.class, script.getName() + " duration: " + duration );
					totalRunTime += duration;
					if( duration > oneMinute && duration > maxDuration )
					{
						longestScripts.clear();
						longestScripts.put( script.getName(), duration );
						maxDuration = duration;
					}
					else if( duration > oneMinute && duration == maxDuration )
					{
						longestScripts.put( script.getName(), duration );
					}

					if( duration > oneMinute && duration < minDuration )
					{
						shortestScripts.clear();
						shortestScripts.put( script.getName(), duration );
						minDuration = duration;
					}
					else if( duration > oneMinute && duration == minDuration )
					{
						shortestScripts.put( script.getName(), duration );
					}
				}
			}

			final Set<String> removeItems = new HashSet<>();
			for( final String name: shortestScripts.keySet() )
			{
				if( longestScripts.keySet().contains( name ) )
				{
					removeItems.add( name );
				}
			}

			for( final String name: removeItems )
			{
				shortestScripts.remove( name );
				longestScripts.remove( name );
			}

			final Long avgRunTime = numCompleted > 0 ? totalRunTime / numCompleted: null;
			final int numInc = scriptsStarted.size() - numCompleted;
			sb.append( "Main Script: " + ModuleUtil.getMainScript( scriptModule ).getAbsolutePath() + RETURN );
			sb.append( "Executed " + scriptsStarted.size() + "/" + scripts.size() + " worker scripts [" );
			sb.append( scriptsSuccess.size() + " successful" );
			sb.append( scriptsFailed.isEmpty() ? "": "; " + scriptsFailed.size() + " failed" );
			sb.append( numInc > 0 ? "; " + numInc + " incomplete": "" );
			sb.append( "] " + RETURN );

			if( avgRunTime != null )
			{
				sb.append( "Average worker script runtime: " + getScriptRunTime( avgRunTime ) + RETURN );
			}
			if( !shortestScripts.isEmpty() )
			{
				sb.append( "Shortest running scripts [" + getScriptRunTime( minDuration ) + "] --> "
						+ shortestScripts.keySet() + RETURN );
			}
			if( !longestScripts.isEmpty() )
			{
				sb.append( "Longest running scripts [" + getScriptRunTime( maxDuration ) + "] --> "
						+ longestScripts.keySet() + RETURN );
			}

			for( final File failureScript: scriptsFailed )
			{
				sb.append( "Script Failed:" + failureScript.getAbsolutePath() + RETURN );
				final BufferedReader reader = BioLockJUtil.getFileReader( failureScript );
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					sb.append( line + RETURN );
				}
			}
		}
		catch( final Exception ex )
		{
			final String msg = "Unable to produce module scriptDir summary for: " + scriptModule.getClass().getName()
					+ " : " + ex.getMessage();
			Log.warn( SummaryUtil.class, msg );
			sb.append( msg + RETURN );
		}

		return sb.toString();
	}

	/**
	 * Pipeline execution summary.
	 * 
	 * @return Pipeline summary
	 */
	public static String getSummary()
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			final File summary = getSummaryFile();
			if( !summary.exists() )
			{
				sb.append( "NO SUMMARY FOUND" );
			}
			else
			{
				final BufferedReader reader = BioLockJUtil.getFileReader( summary );
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					sb.append( line + RETURN );
				}
				reader.close();
			}
		}
		catch( final Exception ex )
		{
			final String msg = "Error occurred creating the pipeline summary: " + ex.getMessage();
			sb.append( RETURN + msg );
			Log.error( SummaryUtil.class, msg, ex );
		}
		return sb.toString();
	}

	/**
	 * Getter for the summary file.
	 * 
	 * @return summary file
	 * @throws Exception if unable to find the file dir
	 */
	public static File getSummaryFile() throws Exception
	{
		return new File( Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath() + File.separator
				+ SUMMARY_FILE );
	}

	/**
	 * Report exception details in the summary
	 *
	 * @param ex Exception
	 * @throws Exception if unable to save the updates summary
	 */
	public static void reportFailure( final Exception ex ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( SPACER + RETURN + getLabel( "Exception" ) );
		if( ex == null )
		{
			sb.append( "Error message not found!" + RETURN );
		}
		else
		{
			sb.append( ex.getMessage() + RETURN );
		}

		if( ex.getStackTrace() != null & ex.getStackTrace().length > 0 )
		{
			for( final StackTraceElement ste: ex.getStackTrace() )
			{
				sb.append( TAB_DELIM + ste.toString() + RETURN );
			}
		}

		saveSummary( sb.toString() );
	}

	/**
	 * After each module completes, this method is called to track the execution summary.<br>
	 * If module is null, the pipeline is complete/successful.
	 * 
	 * @param module Completed module
	 * @throws Exception if unable to build the summary
	 */
	public static void reportSuccess( final BioModule module ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final File summaryFile = getSummaryFile();
		Log.info( SummaryUtil.class, "Update " + summaryFile.getAbsolutePath() );
		if( module == null )
		{
			sb.append( getFooter() );
		}
		else
		{
			Integer modNum = 0;
			if( !summaryFile.exists() )
			{
				sb.append( getHeading() );
			}
			else
			{
				resetModuleSummary( module );
				modNum = getModuleNumber();
			}

			String gap = "  ";
			if( modNum.toString().length() == 2 )
			{
				gap += " ";
			}

			sb.append( getLabel( MODULE + "[" + modNum + "]" ) + module.getClass().getName() + RETURN );
			sb.append( getLabel( RUN_TIME ) + gap + ModuleUtil.getModuleRunTime( module ) + RETURN );
			Log.info( SummaryUtil.class, "Add BioModule summary " + module.getClass().getName() );
			final String summary = module.getSummary();
			if( summary != null && !summary.isEmpty() )
			{
				sb.append( SPACER + RETURN + summary + ( summary.endsWith( RETURN ) ? "": RETURN ) );
			}
			sb.append( SPACER_2X + RETURN );
		}

		saveSummary( sb.toString() );
	}

	/**
	 * Update the number of attempts in the summary file (called from restart)
	 * 
	 * @throws Exception if unable to update the summary file
	 */
	public static void updateNumAttempts() throws Exception
	{
		final File summary = getSummaryFile();
		if( summary.exists() )
		{
			FileUtils.copyFile( getSummaryFile(), getTempFile() );
			BioLockJUtil.deleteWithRetry( getSummaryFile(), 10 );
			final BufferedReader reader = BioLockJUtil.getFileReader( getTempFile() );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( getSummaryFile() ) );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					if( line.startsWith( NUM_ATTEMPTS ) )
					{
						final String count = line.substring( getLabel( NUM_ATTEMPTS ).length() ).trim();
						final Integer num = Integer.valueOf( count ) + 1;
						writer.write( line.replace( count, num.toString() ) + RETURN );
					}
					else
					{
						writer.write( line + RETURN );
					}
				}
				BioLockJUtil.deleteWithRetry( getTempFile(), 10 );
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
				if( writer != null )
				{
					writer.close();
				}
			}
		}
	}

	/**
	 * Read the summary file to find the last completed module number and return the next number.
	 * 
	 * @return Next module number
	 * @throws Exception if unable to determine the module number
	 */
	protected static Integer getModuleNumber() throws Exception
	{
		Integer num = null;
		final BufferedReader reader = BioLockJUtil.getFileReader( getSummaryFile() );
		try
		{
			if( getSummaryFile().exists() )
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					final String label = MODULE + "[";
					if( line.startsWith( label ) && line.indexOf( "]" ) > 0 )
					{
						num = Integer.valueOf( line.substring( label.length(), line.indexOf( "]" ) ) );
					}
				}
			}
		}
		finally
		{
			reader.close();
		}

		return num == null ? 0: num + 1;
	}

	/**
	 * Get the script runtime in hours and minutes
	 * 
	 * @param duration in milliseconds
	 * @return Formatted runtime description
	 */
	protected static String getScriptRunTime( final Long duration )
	{
		if( duration == null )
		{
			return "N/A";
		}

		final String format = String.format( "%%0%dd", 2 );
		long elapsedTime = duration / 1000;
		if( elapsedTime < 0 )
		{
			elapsedTime = 0;
		}
		String hours = String.format( format, elapsedTime / 3600 );
		String minutes = String.format( format, elapsedTime % 3600 / 60 );

		if( hours.equals( "00" ) )
		{
			hours = "";
		}
		else if( hours.equals( "01" ) )
		{
			hours = "1 hour";
		}
		else
		{
			hours += " hours";
		}

		if( hours.isEmpty() && minutes.equals( "00" ) )
		{
			minutes = "<1 minute";
		}
		else if( minutes.equals( "00" ) )
		{
			minutes = "";
		}
		else if( hours.isEmpty() && minutes.equals( "01" ) )
		{
			minutes = "1 minute";
		}
		else if( minutes.equals( "01" ) )
		{
			minutes = " : 1 minute";
		}
		else if( hours.isEmpty() ) // and minutes > 01
		{
			minutes += " minutes";
		}
		else // hours not empty & minutes > 01
		{
			minutes = " : " + minutes + " minutes";
		}

		return hours + minutes;
	}

	/**
	 * Modules can be forced to reset to incomplete status. In this scenario, this method will remove the summary for
	 * completed modules that are rerun.
	 * 
	 * @param module Rerun module
	 * @throws Exception if unable to reset the summary
	 */
	protected static void resetModuleSummary( final BioModule module ) throws Exception
	{
		FileUtils.copyFile( getSummaryFile(), getTempFile() );
		BioLockJUtil.deleteWithRetry( getSummaryFile(), 10 );
		final BufferedReader reader = BioLockJUtil.getFileReader( getTempFile() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getSummaryFile() ) );
		try
		{

			boolean foundMod = false;
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final String label = MODULE + "[";
				if( foundMod )
				{
					break;
				}
				else if( line.startsWith( EXCEPTION_LABEL )
						|| line.startsWith( label ) && line.endsWith( module.getClass().getName() ) )
				{
					foundMod = true;
				}
				else
				{
					writer.write( line + RETURN );
				}
			}
			FileUtils.forceDelete( getTempFile() );
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
			if( writer != null )
			{
				writer.close();
			}
		}
	}

	/**
	 * Save the summary generated to the pipeline root directory.
	 * 
	 * @param summary Pipeline summary
	 * @throws Exception if file processing errors occur
	 */
	protected static void saveSummary( final String summary ) throws Exception
	{
		final File f = getSummaryFile();
		Log.info( SummaryUtil.class, "Update summary: " + f.getAbsolutePath() );
		final boolean append = f.exists();
		Log.debug( SummaryUtil.class, "File exists?: " + append );
		final FileWriter writer = new FileWriter( f, append );
		writer.write( summary );
		writer.close();
		Log.info( SummaryUtil.class, "Save complete!" );
	}

	private static String getHeading() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( RETURN + SPACER + RETURN + getLabel( PIPELINE_NAME ) + "  "
				+ Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + RETURN );
		sb.append( getLabel( PIPELINE_CONFIG ) + Config.getConfigFilePath() + RETURN );
		sb.append( getLabel( NUM_MODULES ) + "      " + Pipeline.getModules().size() + RETURN );
		sb.append( getLabel( NUM_ATTEMPTS ) + "     1" + RETURN );
		sb.append( SPACER_2X + RETURN );
		return sb.toString();
	}

	/**
	 * Simple method to format labels in email body. Adds a colon and 2 spaces to label.
	 *
	 * @param label
	 * @return Formatted label
	 */
	private static String getLabel( final String label )
	{
		return label + ":  ";
	}

	private static File getTempFile() throws Exception
	{
		return new File( Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ).getAbsolutePath() + File.separator
				+ TEMP_SUMMARY_FILE );
	}

	/**
	 * Summary label BioModule header: {@value #MODULE}
	 */
	protected static final String MODULE = "Module";

	/**
	 * Summary label for module/pipeline runtime: {@value #RUN_TIME}
	 */
	protected static final String RUN_TIME = "Runtime";

	/**
	 * Summary label for module/pipeline status: {@value #STATUS}
	 */
	protected static final String STATUS = "Status";

	/**
	 * Name of the summary file created in pipeline root directory: {@value #SUMMARY_FILE}
	 */
	protected static final String SUMMARY_FILE = "summary.txt";

	/**
	 * Name of the temp file created in pipeline root directory: {@value #TEMP_SUMMARY_FILE}
	 */
	protected static final String TEMP_SUMMARY_FILE = "tempSummary.txt";

	private static final String EXCEPTION_LABEL = "Exception:";
	private static final String NUM_ATTEMPTS = "# Attempts";
	private static final String NUM_MODULES = "# Modules";
	private static final String PIPELINE_CONFIG = "Pipeline Config";
	private static final String PIPELINE_NAME = "Pipeline Name";
	private static final String PIPELINE_OUTPUT = "Pipeline Output";
	private static final String PIPELINE_RUNTIME = "Pipeline Runtime";
	private static final String PIPELINE_STATUS = "Pipeline Status";
	private static final String RETURN = BioLockJ.RETURN;
	private static final String SPACER = "---------------------------------------------------------------------";
	private static final String SPACER_2X = SPACER + SPACER;
	private static final String TAB_DELIM = BioLockJ.TAB_DELIM;
}
