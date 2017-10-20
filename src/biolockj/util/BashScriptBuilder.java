/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
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
package biolockj.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;

/**
 * This class generates the bash scripts based on the lines provided by the metagenomeClassifier.
 */
public class BashScriptBuilder
{
	/**
	 * Scripts are build for the Module based on the data lines provided.
	 * @param bioModule
	 * @param data - classifier lines for the bash script
	 * @param files - failure flag files, just used for their names.
	 * @throws Exception
	 */
	public static void buildScripts( final BioModule bioModule, final List<List<String>> data, final List<File> files )
			throws Exception
	{
		verifyConfig();

		final int count = ( files == null ) ? 0: files.size();
		Log.out.info( bioModule.getClass().getSimpleName() + " Building bash scripts: # Sequence Files =" + data.size()
				+ "; If failures occur, failure indicator file name = "
				+ ( ( files == null ) ? failMessage + "": ( count + " unique names (based on SampleID)" ) ) );

		final BufferedWriter allWriter = new BufferedWriter( new FileWriter( createMainScript( bioModule ), true ) );
		int scriptCount = 0;
		int sampleCount = 0;
		int samplesInScript = 0;
		final int digits = new Integer( count / scriptBatchSize ).toString().length();
		boolean needNewScript = false;
		File subScript = null;
		BufferedWriter subScriptWriter = null;

		for( final List<String> lines: data )
		{
			if( lines.size() == 0 )
			{
				throw new Exception( bioModule.getClass().getSimpleName() + " has no lines in "
						+ ModuleUtil.getMainScript( bioModule ) + " subscript!" );
			}
			if( ( subScript == null ) || needNewScript )
			{
				subScript = createSubScript( bioModule, allWriter, scriptCount++, digits );
				subScriptWriter = new BufferedWriter( new FileWriter( subScript, true ) );
			}

			String failMsg = failMessage;
			if( count > 0 )
			{
				failMsg = files.get( sampleCount ).getName();
			}

			addDependantLinesToScript( subScriptWriter,
					ModuleUtil.requireSubDir( bioModule, BioModule.FAILURE_DIR ).getAbsolutePath() + File.separator,
					failMsg, lines );
			needNewScript = needNewScript( ++samplesInScript, ++sampleCount, data.size() );
			if( needNewScript || ( sampleCount == data.size() ) )
			{
				samplesInScript = 0;
				closeScript( subScriptWriter, subScript );
				printFile( subScript );
			}
		}

		closeScript( allWriter, ModuleUtil.getMainScript( bioModule ) );
		Log.out.info( Log.LOG_SPACER );
		Log.out.info( bioModule.getClass().getSimpleName() + " Bash scripts successfully generated" );
		Log.out.info( Log.LOG_SPACER );
	}

	public static void buildScripts( final BioModule bioModule, final List<List<String>> data, final List<File> files,
			final int size ) throws Exception
	{
		scriptBatchSize = size;
		buildScripts( bioModule, data, files );
	}

	/**
	 * Some BioLockJExecutors do not have a line for each file, these pass a single fail message.
	 * @param bioModule
	 * @param data
	 * @param msg
	 * @throws Exception
	 */
	public static void buildScripts( final BioModule bioModule, final List<List<String>> data, final String msg )
			throws Exception
	{
		scriptBatchSize = Config.requirePositiveInteger( Config.SCRIPT_BATCH_SIZE );
		failMessage = msg;
		buildScripts( bioModule, data, new ArrayList<File>() );
		failMessage = "failure";
	}

	/**
	 * Boolean return TRUE if module exists in prop file.
	 * @param val
	 * @return
	 */
	public static boolean clusterModuleExists( final String val ) throws Exception
	{
		if( val == null )
		{
			return false;
		}

		for( final String module: Config.getSet( CLUSTER_MODULES ) )
		{
			if( exists( Config.requireString( CLUSTER_BATCH_COMMAND ) ) && module.contains( val ) )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Lines are wrapped in control statements to allow exit of script based on a single failure,
	 * or to simply track if any failure occur (if exitOnFailure=N).  Exit codes are capture in the
	 * event of a failure to be attached to the failureFile ( 0KB flag file).
	 * @param writer
	 * @param failureFile
	 * @param lines
	 * @throws Exception
	 */
	private static void addDependantLinesToScript( final BufferedWriter writer, final String failPath, String failMsg,
			final List<String> lines ) throws Exception
	{
		writer.write( ERROR_ON_PREVIOUS_LINE + "=false" + RETURN );
		boolean firstLine = true;
		boolean indent = false;
		for( final String line: lines )
		{
			if( Config.getBoolean( Config.SCRIPT_EXIT_ON_ERROR ) )
			{
				indent = true;
				writer.write( "if [[ " + ( firstLine ? "": "$" + ERROR_ON_PREVIOUS_LINE + " == false && " ) + "$"
						+ ERROR_DETECTED + " == false ]]; then" + RETURN );
			}
			else if( !firstLine )
			{
				indent = true;
				writer.write( "if [[ $" + ERROR_ON_PREVIOUS_LINE + " == false ]]; then" + RETURN );
			}

			final String[] parts = line.split( "\\s" );
			if( parts[ 0 ].endsWith( ".py" ) )
			{
				failMsg = parts[ 0 ];
			}

			writer.write( ( indent ? INDENT: "" ) + line + RETURN );
			writer.write( ( indent ? INDENT: "" ) + EXIT_CODE + "=$?" + RETURN );
			writer.write( ( indent ? INDENT: "" ) + "if [[ $" + EXIT_CODE + " != \"0\" ]]; then" + RETURN );
			writer.write( ( indent ? INDENT: "" ) + INDENT + ERROR_ON_PREVIOUS_LINE + "=true" + RETURN );
			writer.write( ( indent ? INDENT: "" ) + INDENT + ERROR_DETECTED + "=true" + RETURN );
			writer.write( ( indent ? INDENT: "" ) + INDENT + FAILURE_CODE + "=$" + EXIT_CODE + RETURN );
			writer.write( ( indent ? INDENT: "" ) + INDENT + "touch " + failPath + failMsg + "_"
					+ BioLockJ.SCRIPT_FAILED + "_exitCode_$" + EXIT_CODE + RETURN );
			writer.write( ( indent ? INDENT: "" ) + "fi" + RETURN );
			writer.write( indent ? "fi" + RETURN: "" );
			firstLine = false;
		}
	}

	/**
	 * Close the script, set the status message, close the writer.
	 * @param writer
	 * @param script
	 * @throws Exception
	 */
	private static void closeScript( final BufferedWriter writer, final File script ) throws Exception
	{
		writer.write( "if [[ $" + ERROR_DETECTED + " == false ]]; then" + RETURN );
		writer.write( INDENT + "touch " + script + "_" + BioLockJ.SCRIPT_SUCCESS + RETURN );
		writer.write( "else" + RETURN );
		writer.write( INDENT + "touch " + script + "_" + BioLockJ.SCRIPT_FAILED + RETURN );
		writer.write(
				INDENT + "touch " + script + "_" + BioLockJ.SCRIPT_FAILED + "_failureCode_$" + FAILURE_CODE + RETURN );
		writer.write( INDENT + "exit 1" + RETURN );
		writer.write( "fi" + RETURN );
		writer.close();
	}

	/**
	 * Create the main script for the Module.
	 *
	 * @param BioModule
	 * @return File mainScript
	 * @throws Exception
	 */
	private static File createMainScript( final BioModule m ) throws Exception
	{
		final File f = new File( m.getScriptDir().getAbsolutePath() + File.separator + BioModule.MAIN_SCRIPT_PREFIX
				+ m.getModuleDir().getName() + ".sh" );

		final BufferedWriter writer = new BufferedWriter( new FileWriter( f ) );

		writer.write( "# BioLockJ " + BioLockJ.BLJ_VERSION + " " + f.getName() + RETURN );
		writer.write( "### This script submits multiple subscripts for parallel processing ###" + RETURN + RETURN );
		writer.write( "cd " + ModuleUtil.requireSubDir( m, QSUB_DIR ).getAbsolutePath() + RETURN );
		writer.write( ERROR_DETECTED + "=false" + RETURN );
		writer.write( FAILURE_CODE + "=0" + RETURN );
		writer.close();
		return f;
	}

	/**
	 * Create the numbered subscript (a worker script)
	 * @param bioModule
	 * @param allWriter
	 * @param countNum
	 * @param digits
	 * @return
	 * @throws Exception
	 */
	private static File createSubScript( final BioModule bioModule, final BufferedWriter allWriter, final int countNum,
			final int digits ) throws Exception
	{
		final String main = ModuleUtil.getMainScript( bioModule ).getName().replaceAll( BioModule.MAIN_SCRIPT_PREFIX,
				"" );
		final String jobName = main.substring( 0, 1 ) + "." + String.format( "%0" + digits + "d", countNum )
				+ main.substring( 1 );
		final File script = new File( bioModule.getScriptDir().getAbsolutePath() + File.separator + jobName );
		Log.out.info( bioModule.getClass().getSimpleName() + " Create Sub Script: " + script.getAbsolutePath() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( script ) );

		final String executeCommand = ( Config.getBoolean( Config.SCRIPT_RUN_ON_CLUSTER )
				? ( Config.requireString( CLUSTER_BATCH_COMMAND ) + " " ): "" ) + script.getAbsolutePath();

		writer.write( Config.requireString( CLUSTER_PARAMS ) + RETURN );
		writer.write( "#BioLockJ." + BioLockJ.BLJ_VERSION + " " + jobName + " - batch size: "
				+ new Integer( Config.requirePositiveInteger( Config.SCRIPT_BATCH_SIZE ) ).toString() + RETURN
				+ RETURN );
		writer.write( "touch " + script + "_" + BioLockJ.SCRIPT_STARTED + RETURN );
		loadModules( writer );
		writer.write( ERROR_DETECTED + "=false" + RETURN );
		writer.write( FAILURE_CODE + "=0" + RETURN );
		writer.close();

		allWriter.write( "if [[ $" + ERROR_DETECTED + " == false ]]; then" + RETURN );
		allWriter.write( INDENT + executeCommand + RETURN );
		allWriter.write( INDENT + EXIT_CODE + "=$?" + RETURN );
		allWriter.write( INDENT + "if [[ $" + EXIT_CODE + " != \"0\" ]]; then" + RETURN );
		allWriter.write( INDENT + INDENT + ERROR_DETECTED + "=true" + RETURN );
		allWriter.write( INDENT + INDENT + FAILURE_CODE + "=$" + EXIT_CODE + RETURN );
		allWriter.write( INDENT + "fi" + RETURN );
		allWriter.write( "fi" + RETURN );
		return script;
	}

	/**
	 * Convenience methods that validates property exists, checking for null in code
	 * is less readable
	 *
	 * @param val
	 * @return
	 */
	private static boolean exists( final String val )
	{
		if( ( val != null ) && ( val.trim().length() > 0 ) )
		{
			return true;
		}

		return false;
	}

	/**
	 * Adds cluster modules to script.
	 * @param writer
	 * @throws Exception
	 */
	private static void loadModules( final BufferedWriter writer ) throws Exception
	{
		for( final String module: Config.getSet( CLUSTER_MODULES ) )
		{
			writer.write( "module load " + module + RETURN );
		}
	}

	/**
	 * Determine if a new script is needed - do #samples = batch size?
	 * @param samplesInScript
	 * @param sampleCount
	 * @param totalSampleCount
	 * @return
	 */
	private static boolean needNewScript( final int samplesInScript, final int sampleCount, final int totalSampleCount )
	{
		if( ( scriptBatchSize > 0 ) && ( samplesInScript == scriptBatchSize ) && ( sampleCount < totalSampleCount ) )
		{
			return true;
		}

		return false;
	}

	/**
	 * Print the bash script to the output file.
	 * @param file
	 */
	private static void printFile( final File file )
	{
		Log.out.debug( "BashScriptUtil PRINT FILE => " + file.getAbsolutePath() );
		try
		{
			final BufferedReader in = new BufferedReader( new FileReader( file ) );
			String line;
			while( ( line = in.readLine() ) != null )
			{
				Log.out.debug( line );
			}
			in.close();

		}
		catch( final Exception ex )
		{
			Log.out.error( "BashScriptUtil Error occurred printing DEBUG for file: " + file, ex );
		}
	}

	/**
	 * Validate cluster params num threads matches, numThreads defined in prop file.
	 * Format for UNCC HPC Cluster: #PBS -l procs=1,mem=8GB
	 * @throws Exception
	 */
	private static void verifyConfig() throws Exception
	{
		Config.requirePositiveInteger( Config.SCRIPT_NUM_THREADS );

		if( !Config.getBoolean( Config.SCRIPT_RUN_ON_CLUSTER ) )
		{
			return;
		}

		Config.requireString( CLUSTER_BATCH_COMMAND );

		if( !Config.getBoolean( CLUSTER_VALIDATE_PARAMS ) )
		{
			Log.out.warn( CLUSTER_VALIDATE_PARAMS + "=N so we will not enfore cluster #" + CLUSTER_NUM_PROCESSORS
					+ " = " + Config.SCRIPT_NUM_THREADS );
			return;
		}

		final String clusterParams = Config.requireString( CLUSTER_PARAMS );

		final StringTokenizer st = new StringTokenizer( clusterParams, "," );
		while( st.hasMoreTokens() )
		{
			String token = st.nextToken();
			if( token.contains( CLUSTER_NUM_PROCESSORS ) )
			{
				final StringTokenizer pToken = new StringTokenizer( token, "=" );
				while( pToken.hasMoreTokens() )
				{
					token = pToken.nextToken().trim();
					if( !token.contains( CLUSTER_NUM_PROCESSORS ) ) // only check right size of equation "="
					{
						if( Integer.valueOf( token ) != Config.requirePositiveInteger( Config.SCRIPT_NUM_THREADS ) )
						{
							throw new Exception( "Inconsistant config values. " + Config.SCRIPT_NUM_THREADS + "="
									+ Config.requirePositiveInteger( Config.SCRIPT_NUM_THREADS ) + " & "
									+ CLUSTER_PARAMS + "=" + clusterParams + " --> (#" + CLUSTER_NUM_PROCESSORS + "="
									+ Integer.valueOf( token ) + ")" );
						}
						break;
					}
				}
			}
		}
	}

	public static final String QSUB_DIR = "qsub";
	protected static final String CLUSTER_BATCH_COMMAND = "cluster.batchCommand";
	protected static final String CLUSTER_MODULES = "cluster.modules";
	protected static final String CLUSTER_NUM_PROCESSORS = "procs";
	protected static final String CLUSTER_PARAMS = "cluster.params";
	protected static final String CLUSTER_VALIDATE_PARAMS = "cluster.validateParams";
	protected static final String ERROR_DETECTED = "errorDetected";
	protected static final String ERROR_ON_PREVIOUS_LINE = "errorOnPreviousLine";
	protected static final String EXIT_CODE = "exitCode";
	protected static String failMessage = "failure";
	protected static final String FAILURE_CODE = "failureCode";
	protected static int scriptBatchSize = 0;
	private static final String INDENT = BioLockJ.INDENT;
	private static final String RETURN = BioLockJ.RETURN;
}
