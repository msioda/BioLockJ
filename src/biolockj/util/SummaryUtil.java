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
import java.util.stream.Collectors;
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
public class SummaryUtil {

	/**
	 * Called when pipeline fails to add summary details to summary file, if possible.
	 */
	public static void addSummaryFooterForFailedPipeline() {
		String summaryFile = "";
		try {
			summaryFile = getSummaryFile().getAbsolutePath();
			saveSummary( getFooter() );
		} catch( final Exception ex ) {
			Log.error( SummaryUtil.class, "Unable to update summary file: " + summaryFile, ex );
		}
	}

	/**
	 * Print the application name *bigly* with ASCII art :-)
	 * 
	 * @return Beautiful artwork
	 */
	public static String display_ASCII_Status() {
		return biolockj_ASCII_Art() + RETURN
			+ ( BioLockJ.isPipelineComplete() ? complete_ASCII_Art(): failed_ASCII_Art() );
	}

	/**
	 * Return the min/max/mean/median summary stats for the given metadata numeric column
	 * 
	 * @param map Map(sampleId,count)
	 * @param label Context label
	 * @param addTotal Boolean if should include total count
	 * @return Summary lines
	 * @throws Exception if errors occur
	 */
	public static String getCountSummary( final Map<String, String> map, final String label, final boolean addTotal )
		throws Exception {
		final int pad = getPad( label );
		String msg = BioLockJUtil.addTrailingSpaces( "# Samples:", pad )
			+ BioLockJUtil.formatNumericOutput( new Integer( map.size() ).longValue(), false ) + RETURN;

		if( !map.isEmpty() ) {
			final TreeSet<Long> vals = new TreeSet<>(
				map.values().stream().map( Long::parseLong ).collect( Collectors.toSet() ) );

			msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (min):", pad )
				+ BioLockJUtil.formatNumericOutput( vals.first(), false ) + RETURN;
			msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (median):", pad )
				+ BioLockJUtil.formatNumericOutput( Long.valueOf( SummaryUtil.getMedian( vals, false ) ), false )
				+ RETURN;
			msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (mean):", pad )
				+ BioLockJUtil.formatNumericOutput( Long.valueOf( SummaryUtil.getMean( vals, false ) ), false )
				+ RETURN;
			msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (max):", pad )
				+ BioLockJUtil.formatNumericOutput( vals.last(), false ) + RETURN;

			Long sum = 0L;
			for( final long val: vals ) {
				sum += val;
			}

			if( addTotal ) {
				msg += BioLockJUtil.addTrailingSpaces( "# " + label + " (total):", pad )
					+ BioLockJUtil.formatNumericOutput( sum, false ) + RETURN;
			}
			if( !vals.first().equals( vals.last() ) ) {
				final Set<String> minSamples = new HashSet<>();
				final Set<String> maxSamples = new HashSet<>();
				for( final String id: map.keySet() ) {
					if( map.get( id ).equals( vals.first().toString() ) ) {
						minSamples.add( id );
					}
					if( map.get( id ).equals( vals.last().toString() ) ) {
						maxSamples.add( id );
					}
				}

				msg += BioLockJUtil.addTrailingSpaces( "IDs w/ min " + label + ":", pad ) + minSamples + RETURN;
				msg += BioLockJUtil.addTrailingSpaces( "IDs w/ max " + label + ":", pad ) + maxSamples + RETURN;
			}
		}

		return msg;
	}

	/**
	 * Print the final lines of the summary with overall status, runtime, and the download scp command if applicable.
	 * 
	 * @return Summary info
	 * @throws Exception if any error occurs
	 */
	public static String getFooter() throws Exception {
		final long duration = System.currentTimeMillis() - Constants.APP_START_TIME;
		final StringBuffer sb = new StringBuffer();
		sb.append( getLabel( PIPELINE_NAME ) + "   " + Config.pipelineName() + RETURN );
		sb.append( getLabel( RUNTIME_ENV ) + "     " + getRuntimeEnv() + RETURN );
		sb.append( getLabel( PIPELINE_STATUS ) + " " + Pipeline.getStatus().toLowerCase() + "!" + RETURN );
		sb.append( getLabel( PIPELINE_RUNTIME ) + getRunTime( duration ) + RETURN );
		sb.append( getLabel( PIPELINE_OUTPUT ) + "    " + Config.pipelinePath() + RETURN );
		sb.append( getLabel( INPUT_CONFIG ) + "    " + Config.getConfigFilePath() + RETURN );
		sb.append( getLabel( MASTER_CONFIG ) + "   " + MasterConfigUtil.getPath() + RETURN );
		sb.append( getLabel( FINAL_META ) + "  " + ( MetaUtil.exists() ? MetaUtil.getPath(): "N/A" ) + RETURN );

		final String downloadCmd = downloadCmd();
		if( downloadCmd != null ) {
			sb.append( EXT_SPACER + RETURN );
			sb.append( downloadCmd + RETURN );
		}

		sb.append( EXT_SPACER + RETURN );
		return sb.toString();
	}

	/**
	 * Build a summary of the input files for the given module
	 * 
	 * @param module BioModule to summarize
	 * @return module input summary
	 */
	public static String getInputSummary( final BioModule module ) {
		final StringBuffer sb = new StringBuffer();
		try {
			final int numIn = module.getInputFiles().size();
			if( numIn < 1 ) return null;

			Long inAvg = 0L;
			for( final File f: module.getInputFiles() ) {
				inAvg += FileUtils.sizeOf( f );
			}

			inAvg = new Double( inAvg / numIn ).longValue();

			sb.append( "# Input files: " + numIn + Constants.RETURN );
			sb.append( "Mean Input File Size: " + FileUtils.byteCountToDisplaySize( inAvg ) + RETURN );
		} catch( final Exception ex ) {
			final String msg = "Unable to produce module input summary for: " + module.getClass().getName() + " : "
				+ ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( SummaryUtil.class, msg );
			ex.printStackTrace();
		}

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
	public static String getMean( final Collection<?> vals, final boolean isDouble ) {
		if( isDouble ) {
			final Collection<Double> doubleVals = (Collection<Double>) vals;
			return Double.valueOf( doubleVals.stream().mapToDouble( i -> i ).sum() / doubleVals.size() ).toString();
		}

		final Collection<Long> longVals = (Collection<Long>) vals;
		return Long
			.valueOf( Double.valueOf( longVals.stream().mapToLong( i -> i ).sum() / longVals.size() ).longValue() )
			.toString();
	}

	/**
	 * Return the median value
	 * 
	 * @param vals Collection of values
	 * @param isDouble if the Collection holds Double values
	 * @return median value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getMedian( final Collection<?> vals, final boolean isDouble ) {
		if( vals.size() == 1 ) return vals.iterator().next().toString();

		List data = null;
		if( isDouble ) {
			data = new ArrayList<>( (Collection<Double>) vals );
		} else {
			data = new ArrayList<>( (Collection<Long>) vals );
		}

		Collections.sort( data );

		final int middle = data.size() / 2;
		if( data.size() % 2 == 1 ) return data.get( new Double( data.size() / 2 ).intValue() ).toString();
		else if( isDouble )
			return Double.valueOf( ( (Double) data.get( middle - 1 ) + (Double) data.get( middle ) ) / 2 ).toString();
		else return Long.valueOf( ( (Long) data.get( middle - 1 ) + (Long) data.get( middle ) ) / 2 ).toString();
	}

	/**
	 * Return duration module ran based on modified data of started file, formatted for display (as hours, minutes,
	 * seconds).
	 *
	 * @param module BioModule
	 * @return Formatted module runtime
	 */
	public static String getModuleRunTime( final BioModule module ) {
		final File started = new File(
			module.getModuleDir().getAbsolutePath() + File.separator + Constants.BLJ_STARTED );
		return getRunTime( System.currentTimeMillis() - started.lastModified() );
	}

	/**
	 * Return summary of {@link biolockj.module.BioModule} output directory, with metrics:
	 * <ul>
	 * <li>Number of output files
	 * <li>Mean output file size
	 * <li>Path of new metadata file if any created
	 * </ul>
	 *
	 * @param module BioModule to summarize
	 * @return Summary of module output directory
	 */
	public static String getOutputDirSummary( final BioModule module ) {
		final StringBuffer sb = new StringBuffer();
		try {

			if( module.getOutputDir().listFiles().length == 0 ) return "# Files Output:  0" + RETURN;
			final Collection<File> outFiles = FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE,
				HiddenFileFilter.VISIBLE );
			int count = outFiles.size();
			BigInteger outAvg = FileUtils.sizeOfAsBigInteger( module.getOutputDir() );
			final File newMeta = module.getMetadata();
			if( newMeta.isFile() && module.getOutputDir().listFiles().length > 1 ) {
				count--;
				outAvg = outAvg.subtract( FileUtils.sizeOfAsBigInteger( newMeta ) );
			}

			if( count == 0 ) {
				outAvg = BigInteger.valueOf( 0 );
			} else {
				outAvg = outAvg.divide( BigInteger.valueOf( count ) );
			}

			sb.append( "# Files Output:  " + count + RETURN );
			sb.append( "Mean Output File Size:  " + FileUtils.byteCountToDisplaySize( outAvg ) + RETURN );
			sb.append( newMeta.isFile() ? "New metadata: " + newMeta.getAbsolutePath() + RETURN : "" );

		} catch( final Exception ex ) {
			final String msg = "Unable to produce module output summary for: " + module.getClass().getName() + " : "
				+ ex.getMessage();
			sb.append( msg + RETURN );
			Log.error( SummaryUtil.class, msg, ex );
		}

		return sb.toString();
	}

	/**
	 * Summary count label padding as label length + 18
	 * 
	 * @param label Label
	 * @return padding length
	 */
	public static int getPad( final String label ) {
		return label.length() + 15;
	}

	/**
	 * Get runtime message (formatted as hours, minutes, seconds) based on startTime passed.
	 *
	 * @param duration Milliseconds of run time
	 * @return Formatted runtime as XX hours : XX minutes: XX seconds
	 */
	public static String getRunTime( final long duration ) {
		final String format = String.format( "%%0%dd", 2 );
		long elapsedTime = duration / 1000;
		if( elapsedTime < 0 ) {
			elapsedTime = 0;
		}
		final String hours = String.format( format, elapsedTime / 3600 );
		final String minutes = String.format( format, elapsedTime % 3600 / 60 );
		String seconds = String.format( format, elapsedTime % 60 );
		if( hours.equals( "00" ) && minutes.equals( "00" ) && seconds.equals( "00" ) ) {
			seconds = "01";
		}
		return hours + " hours : " + minutes + " minutes : " + seconds + " seconds";
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
	 * @param module ScriptModule to summarize
	 * @return Summary of module script directory
	 */
	public static String getScriptDirSummary( final ScriptModule module ) {
		final StringBuffer sb = new StringBuffer();
		try {

			final File mainScript = module.getMainScript();
			if( mainScript == null && !DockerUtil.inAwsEnv() )
				return "Module MAIN script not found in -->" + module.getScriptDir().getAbsolutePath() + RETURN;

			final IOFileFilter ff0 = new WildcardFileFilter( "*" + Constants.SH_EXT );
			final IOFileFilter ffStarted = new WildcardFileFilter(
				"*" + Constants.SH_EXT + "_" + Constants.SCRIPT_STARTED );
			final IOFileFilter ffSuccess = new WildcardFileFilter(
				"*" + Constants.SH_EXT + "_" + Constants.SCRIPT_SUCCESS );
			final IOFileFilter ffFailed = new WildcardFileFilter(
				"*" + Constants.SH_EXT + "_" + Constants.SCRIPT_FAILURES );

			final Collection<File> scripts = FileUtils.listFiles( module.getScriptDir(), ff0, null );
			final Collection<File> scriptsStarted = FileUtils.listFiles( module.getScriptDir(), ffStarted, null );
			final Collection<File> scriptsFailed = FileUtils.listFiles( module.getScriptDir(), ffFailed, null );
			final Collection<File> scriptsSuccess = FileUtils.listFiles( module.getScriptDir(), ffSuccess, null );

			if( mainScript != null ) {
				final File mainSuccess = new File(
					module.getMainScript().getAbsolutePath() + "_" + Constants.SCRIPT_SUCCESS );
				final File mainFail = new File(
					module.getMainScript().getAbsolutePath() + "_" + Constants.SCRIPT_FAILURES );
				final File mainStarted = new File(
					module.getMainScript().getAbsolutePath() + "_" + Constants.SCRIPT_STARTED );

				scripts.remove( module.getMainScript() );
				scriptsFailed.remove( mainFail );
				scriptsSuccess.remove( mainSuccess );
				scriptsStarted.remove( mainStarted );
			}

			final Map<String, Long> longestScripts = new HashMap<>();
			final Map<String, Long> shortestScripts = new HashMap<>();
			long totalRunTime = 0L;
			final int numCompleted = scriptsSuccess.size() + scriptsFailed.size();
			long maxDuration = 0L;
			long minDuration = Long.MAX_VALUE;

			final long oneMinute = BioLockJUtil.minutesToMillis( 1 );

			for( final File script: scripts ) {
				final File started = new File( script.getAbsolutePath() + "_" + Constants.SCRIPT_STARTED );
				File finish = new File( script.getAbsolutePath() + "_" + Constants.SCRIPT_SUCCESS );
				if( !finish.isFile() ) {
					finish = new File( script.getAbsolutePath() + "_" + Constants.SCRIPT_FAILURES );
				} else {
					final long duration = finish.lastModified() - started.lastModified();
					Log.debug( SummaryUtil.class, script.getName() + " duration: " + duration );
					totalRunTime += duration;
					if( duration > oneMinute && duration > maxDuration ) {
						longestScripts.clear();
						longestScripts.put( script.getName(), duration );
						maxDuration = duration;
					} else if( duration > oneMinute && duration == maxDuration ) {
						longestScripts.put( script.getName(), duration );
					}

					if( duration > oneMinute && duration < minDuration ) {
						shortestScripts.clear();
						shortestScripts.put( script.getName(), duration );
						minDuration = duration;
					} else if( duration > oneMinute && duration == minDuration ) {
						shortestScripts.put( script.getName(), duration );
					}
				}
			}

			final Set<String> removeItems = new HashSet<>();
			for( final String name: shortestScripts.keySet() ) {
				if( longestScripts.keySet().contains( name ) ) {
					removeItems.add( name );
				}
			}

			for( final String name: removeItems ) {
				shortestScripts.remove( name );
				longestScripts.remove( name );
			}

			final Long avgRunTime = numCompleted > 0 ? totalRunTime / numCompleted: null;
			final int numInc = scriptsStarted.size() - numCompleted;
			if( mainScript != null ) {
				sb.append( "Main Script:  " + mainScript.getAbsolutePath() + RETURN );
			}
			sb.append( "Executed " + scriptsStarted.size() + "/" + scripts.size() + " worker scripts [" );
			sb.append( scriptsSuccess.size() + " successful" );
			sb.append( scriptsFailed.isEmpty() ? "": "; " + scriptsFailed.size() + " failed" );
			sb.append( numInc > 0 ? "; " + numInc + " incomplete": "" );
			sb.append( "]" + RETURN );

			if( avgRunTime != null ) {
				sb.append( "Average worker script runtime: " + getScriptRunTime( avgRunTime ) + RETURN );
			}
			if( !shortestScripts.isEmpty() ) {
				sb.append( "Shortest running scripts [" + getScriptRunTime( minDuration ) + "] --> "
					+ shortestScripts.keySet() + RETURN );
			}
			if( !longestScripts.isEmpty() ) {
				sb.append( "Longest running scripts [" + getScriptRunTime( maxDuration ) + "] --> "
					+ longestScripts.keySet() + RETURN );
			}

			for( final File failureScript: scriptsFailed ) {
				sb.append( "Script Failed:" + failureScript.getAbsolutePath() + RETURN );
				final BufferedReader reader = BioLockJUtil.getFileReader( failureScript );
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					sb.append( line + RETURN );
				}
			}
		} catch( final Exception ex ) {
			final String msg = "Unable to produce module script summary for: " + module.getClass().getName() + " : "
				+ ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( SummaryUtil.class, msg );
			ex.printStackTrace();
		}

		return sb.toString();
	}

	/**
	 * Pipeline execution summary.
	 * 
	 * @return Pipeline summary
	 */
	public static String getSummary() {
		final StringBuffer sb = new StringBuffer();
		try {
			final File summary = getSummaryFile();
			if( !summary.isFile() ) {
				sb.append( "NO SUMMARY FOUND" );
			} else {
				final BufferedReader reader = BioLockJUtil.getFileReader( summary );
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					sb.append( line + RETURN );
				}
				reader.close();
			}
		} catch( final Exception ex ) {
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
	 */
	public static File getSummaryFile() {
		return new File( Config.pipelinePath() + File.separator + SUMMARY_FILE );
	}

	/**
	 * Report exception details in the summary
	 *
	 * @param ex Exception thrown causing application runtime failure
	 * @throws Exception if unable to save the updates summary
	 */
	public static void reportFailure( final Exception ex ) throws Exception {
		final StringBuffer sb = new StringBuffer();
		sb.append( getDashes( 60 ) + RETURN + getLabel( "Exception" ) );
		if( ex == null ) {
			sb.append( "Error message not found!" + RETURN );
		} else {
			sb.append( ex.getMessage() + RETURN );
			if( ex.getStackTrace() != null & ex.getStackTrace().length > 0 ) {
				for( final StackTraceElement ste: ex.getStackTrace() ) {
					sb.append( Constants.TAB_DELIM + ste.toString() + RETURN );
				}
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
	public static void reportSuccess( final BioModule module ) throws Exception {
		final StringBuffer sb = new StringBuffer();
		final File summaryFile = getSummaryFile();
		if( module == null ) {
			sb.append( getFooter() );
		} else {
			Log.info( SummaryUtil.class,
				"Update BioModule summary [ " + module.getClass().getName() + " ] " + summaryFile.getAbsolutePath() );
			Integer modNum = 0;
			if( !summaryFile.isFile() ) {
				sb.append( getHeading() );
			} else {
				resetModuleSummary( module );
				modNum = getModuleNumber();
			}

			String gap = "  ";
			if( modNum.toString().length() == 2 ) {
				gap += " ";
			}
			final String modLabel = getLabel( MODULE + "[" + modNum + "]" ) + module.getClass().getName();
			final String runtime = getLabel( RUN_TIME ) + gap + getModuleRunTime( module );
			sb.append( modLabel + RETURN );
			sb.append( runtime + RETURN );

			final String summary = module.getSummary();
			if( summary != null && !summary.isEmpty() ) {
				sb.append( getDashes( Math.max( modLabel.length(), runtime.length() ) ) + RETURN + summary
					+ ( summary.endsWith( RETURN ) ? "": RETURN ) );
			}
			sb.append( EXT_SPACER + RETURN );
		}

		saveSummary( sb.toString() );
	}

	/**
	 * Update the number of attempts in the summary file (called from restart)
	 * 
	 * @throws Exception if unable to update the summary file
	 */
	public static void updateNumAttempts() throws Exception {
		final File summary = getSummaryFile();
		if( summary.exists() ) {
			FileUtils.copyFile( getSummaryFile(), getTempFile() );
			getSummaryFile().delete();
			// BioLockJUtil.deleteWithRetry( getSummaryFile(), 10 );
			final BufferedReader reader = BioLockJUtil.getFileReader( getTempFile() );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( getSummaryFile() ) );
			try {
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					if( line.startsWith( NUM_ATTEMPTS ) ) {
						final String count = line.substring( getLabel( NUM_ATTEMPTS ).length() ).trim();
						final Integer num = Integer.valueOf( count ) + 1;
						writer.write( line.replace( count, num.toString() ) + RETURN );
					} else {
						writer.write( line + RETURN );
					}
				}
				getTempFile().delete();
				// BioLockJUtil.deleteWithRetry( getTempFile(), 10 );
			} finally {
				if( reader != null ) {
					reader.close();
				}
				writer.close();
			}
		}
	}

	/**
	 * Read the summary file to find the last completed module number and return the next number.
	 * 
	 * @return Next module number
	 * @throws Exception if unable to determine the module number
	 */
	protected static Integer getModuleNumber() throws Exception {
		Integer num = null;
		final BufferedReader reader = BioLockJUtil.getFileReader( getSummaryFile() );
		try {
			if( getSummaryFile().isFile() ) {
				for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
					final String label = MODULE + "[";
					if( line.startsWith( label ) && line.indexOf( "]" ) > 0 ) {
						num = Integer.valueOf( line.substring( label.length(), line.indexOf( "]" ) ) );
					}
				}
			}
		} finally {
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
	protected static String getScriptRunTime( final Long duration ) {
		if( duration == null ) return "N/A";

		final String format = String.format( "%%0%dd", 2 );
		long elapsedTime = duration / 1000;
		if( elapsedTime < 0 ) {
			elapsedTime = 0;
		}
		String hours = String.format( format, elapsedTime / 3600 );
		String minutes = String.format( format, elapsedTime % 3600 / 60 );

		if( hours.equals( "00" ) ) {
			hours = "";
		} else if( hours.equals( "01" ) ) {
			hours = "1 hour";
		} else {
			hours += " hours";
		}

		if( hours.isEmpty() && minutes.equals( "00" ) ) {
			minutes = "<1 minute";
		} else if( minutes.equals( "00" ) ) {
			minutes = "";
		} else if( hours.isEmpty() && minutes.equals( "01" ) ) {
			minutes = "1 minute";
		} else if( minutes.equals( "01" ) ) {
			minutes = " : 1 minute";
		} else if( hours.isEmpty() ) // and minutes > 01
		{
			minutes += " minutes";
		} else // hours not empty & minutes > 01
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
	protected static void resetModuleSummary( final BioModule module ) throws Exception {
		FileUtils.copyFile( getSummaryFile(), getTempFile() );
		getSummaryFile().delete();
		// BioLockJUtil.deleteWithRetry( getSummaryFile(), 10 );
		final BufferedReader reader = BioLockJUtil.getFileReader( getTempFile() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getSummaryFile() ) );
		try {

			boolean foundMod = false;
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				final String label = MODULE + "[";
				if( foundMod ) {
					break;
				} else if( line.startsWith( EXCEPTION_LABEL )
					|| line.startsWith( label ) && line.endsWith( module.getClass().getName() ) ) {
					foundMod = true;
				} else {
					writer.write( line + RETURN );
				}
			}
			getTempFile().delete();
			// FileUtils.forceDelete( getTempFile() );
		} finally {
			if( reader != null ) {
				reader.close();
			}
			writer.close();
		}
	}

	/**
	 * Save the summary generated to the pipeline root directory.
	 * 
	 * @param summary Pipeline summary
	 * @throws Exception if file processing errors occur
	 */
	protected static void saveSummary( final String summary ) throws Exception {
		final File f = getSummaryFile();
		final FileWriter writer = new FileWriter( f, f.isFile() );
		writer.write( summary );
		writer.close();
		Log.info( SummaryUtil.class, "Summary updated" );
	}

	private static String biolockj_ASCII_Art() {
		final StringBuffer sb = new StringBuffer();
		final String spacer = getSpaces( 10 );
		sb.append( spacer + "" + RETURN );
		return sb.toString();
	}

	private static String complete_ASCII_Art() {
		final StringBuffer sb = new StringBuffer();
		final String spacer = getSpaces( 5 );
		sb.append( spacer + "" + RETURN );
		return sb.toString();
	}

	private static String downloadCmd() throws Exception {
		if( downloadCommand == null ) {
			downloadCommand = DownloadUtil.getDownloadCmd();
		}
		return downloadCommand;
	}

	private static String failed_ASCII_Art() {
		final StringBuffer sb = new StringBuffer();
		final String spacer = getSpaces( 23 );
		sb.append( spacer + "" + RETURN );
		return sb.toString();
	}

	private static String getDashes( final int len ) {
		return getSpacer( "-", len );
	}

	private static String getHeading() {
		final StringBuffer sb = new StringBuffer();
		sb.append( RETURN + EXT_SPACER + RETURN + getLabel( PIPELINE_NAME ) + "  " + Config.pipelineName() + RETURN );
		sb.append( getLabel( INPUT_CONFIG ) + "   " + Config.getConfigFilePath() + RETURN );
		sb.append( getLabel( NUM_MODULES ) + "      " + Pipeline.getModules().size() + RETURN );
		sb.append( getLabel( NUM_ATTEMPTS ) + "     1" + RETURN );
		sb.append( EXT_SPACER + RETURN );
		return sb.toString();
	}

	/**
	 * Simple method to format labels in email body. Adds a colon and 2 spaces to label.
	 *
	 * @param label String
	 * @return Formatted label
	 */
	private static String getLabel( final String label ) {
		return label + ":  ";
	}

	private static String getRuntimeEnv() {
		if( runtimeEnv != null ) return runtimeEnv;
		String clusterHost = null;
		runtimeEnv = "localhost";
		try {
			clusterHost = Config.isOnCluster() ? Config.requireString( null, Constants.CLUSTER_HOST ): null;
			if( clusterHost != null ) {
				runtimeEnv = clusterHost;
			}

			final String hostName = Processor.submit( "hostname", "Query Host" );
			if( hostName != null ) {
				runtimeEnv = hostName;
			}

		} catch( final Exception ex ) {
			Log.error( SummaryUtil.class, "Failed to determine runtime environment host", ex );
		}

		if( DockerUtil.inAwsEnv() ) return "AWS-Nextflow/Docker @" + runtimeEnv;
		if( DockerUtil.inDockerEnv() ) return "Docker @" + runtimeEnv;
		if( Config.isOnCluster() && clusterHost != null && runtimeEnv != clusterHost )
			return "CLUSTER [ head@" + clusterHost + " --> compute@" + runtimeEnv + " ]";
		return "@" + runtimeEnv;
	}

	private static String getSpacer( final String val, final int len ) {
		String spacer = "";
		for( int i = 0; i < len; i++ ) {
			spacer += val;
		}
		return spacer;
	}

	private static String getSpaces( final int len ) {
		return getSpacer( " ", len );
	}

	private static File getTempFile() {
		return new File( Config.pipelinePath() + File.separator + TEMP_SUMMARY_FILE );
	}

	private static String downloadCommand = null;
	private static final String EXCEPTION_LABEL = "Exception:";
	private static final String EXT_SPACER = getDashes( 154 );
	private static final String FINAL_META = "Final Metadata";
	private static final String INPUT_CONFIG = "Input Config";
	private static final String MASTER_CONFIG = "Master Config";
	private static final String MODULE = "Module";
	private static final String NUM_ATTEMPTS = "# Attempts";
	private static final String NUM_MODULES = "# Modules";
	private static final String PIPELINE_NAME = "Pipeline Name";
	private static final String PIPELINE_OUTPUT = "Pipeline Dir";
	private static final String PIPELINE_RUNTIME = "Pipeline Runtime";
	private static final String PIPELINE_STATUS = "Pipeline Status";
	private static final String RETURN = Constants.RETURN;
	private static final String RUN_TIME = "Runtime";
	private static final String RUNTIME_ENV = "Runtime Env";
	private static String runtimeEnv = null;
	private static final String SUMMARY_FILE = "summary" + Constants.TXT_EXT;
	private static final String TEMP_SUMMARY_FILE = ".tempSummary" + Constants.TXT_EXT;
}
