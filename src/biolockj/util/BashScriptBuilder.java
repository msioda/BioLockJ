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
import java.util.*;
import java.util.regex.Matcher;
import biolockj.*;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;
import biolockj.module.*;
import biolockj.module.report.r.R_Module;

/**
 * This utility class generates the bash script files using the lines provided and {@link biolockj.Config} script
 * properties.
 */
public class BashScriptBuilder {

	// Prevents instantiation
	private BashScriptBuilder() {}

	/**
	 * Build the MIAN script.
	 * 
	 * @param module ScriptModule
	 * @throws Exception if any error occurs
	 */
	public static void buildMainScript( final ScriptModule module ) throws Exception {
		if( workerScripts.isEmpty() )
			throw new Exception( "No worker scripts created for module: " + module.getClass().getName() );

		final List<String> mainScriptLines = initMainScript( module );
		for( final File worker: workerScripts )
			mainScriptLines.add( getMainScriptExecuteWorkerLine( worker.getAbsolutePath() ) );

		mainScriptLines
			.add( Constants.RETURN + "touch " + getMainScriptPath( module ) + "_" + Constants.SCRIPT_SUCCESS );
		createScript( getMainScriptPath( module ), mainScriptLines );
		workerScripts.clear();
	}

	/**
	 * This method builds the bash scripts required for the given module.<br>
	 * Standard local/cluster pipelines include: 1 MAIN script, 1+ worker-scripts.<br>
	 * Docker R_Modules include: 1 MAIN scirpt, 0 worker-scripts - MAIN.R run by MAIN.sh<br>
	 * Docker *non-R_Modules* include: 1 MAIN scirpt, 1+ worker-scripts - MAIN.sh runs workers<br>
	 * AWS Docker R_Modules include: 0 MAIN scirpts, 0 worker-scripts - MAIN.R run by nextflow<br>
	 * AWS Docker *non-R_Modules* include: 0 MAIN scirpts, 1+ worker-scripts MAIN.sh runs workers<br>
	 * 
	 * @param module ScriptModule
	 * @param data Bash script lines
	 * @throws Exception if any error occurs
	 */
	public static void buildScripts( final ScriptModule module, final List<List<String>> data ) throws Exception {
		if( data == null || data.size() < 1 )
			throw new Exception( "Cannot build empty scripts for: " + module.getClass().getName() );
		buildWorkerScripts( module, data );
		if( !DockerUtil.inAwsEnv() ) buildMainScript( module );
		Processor.setFilePermissions( module.getScriptDir().getAbsolutePath(),
			Config.requireString( null, ScriptModule.SCRIPT_PERMISSIONS ) );
	}

	/**
	 * Create bash worker script function: executeLine<br>
	 * Capture status code of the payload script line. Call scriptFailed function to capture all failure info (if any
	 * occur).
	 * 
	 * @return Bash script lines
	 */
	protected static List<String> buildExecuteFunction() {
		final List<String> lines = new ArrayList<>();
		lines.add( "function " + FUNCTION_EXECUTE_LINE + "() {" );
		lines.add( "${1}" );
		lines.add( "statusCode=$?" );
		lines.add( "[ ${statusCode} != 0 ] && " + FUNCTION_SCRIPT_FAILED + " ${1} ${2} ${statusCode}" );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * Create bash worker script function: scriptFailed<br>
	 * Failure details written to the failure indicator file:<br>
	 * <ol>
	 * <li>$1 Script payload line to execute
	 * <li>$2 Script line number
	 * <li>$3 Script line failure status code
	 * </ol>
	 * 
	 * @param path Script path
	 * @return Bash script lines
	 */
	protected static List<String> buildScriptFailureFunction( final String path ) {
		final List<String> lines = new ArrayList<>();
		lines.add( "function " + FUNCTION_SCRIPT_FAILED + "() {" );
		lines.add( "echo \"Line #${2} failure status code [ ${3} ]:  ${1}\" >> \"" + path + "_" +
			Constants.SCRIPT_FAILURES + "\"" );
		lines.add( "exit ${3}" );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * Create the script. Leading zeros added if needed so all worker scripts have same number of digits. Print the
	 * worker script as DEBUG to the log file.
	 * 
	 * @param scriptPath Worker script path
	 * @param lines Ordered bash script lines to write to the scriptPath
	 * @return File bash script
	 * @throws Exception if I/O errors occur
	 */
	protected static File createScript( final String scriptPath, final List<String> lines ) throws Exception {
		Log.info( BashScriptBuilder.class, "Create new script: " + scriptPath );
		final File workerScript = new File( scriptPath );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( workerScript ) );
		try {
			writeScript( writer, lines );
		} finally {
			writer.close();
		}
		return workerScript;
	}

	/**
	 * Call executeLine function in the worker script
	 * 
	 * @param workerScriptPath Worker script path
	 * @return bash script line
	 */
	protected static String getMainScriptExecuteWorkerLine( final String workerScriptPath ) {
		final StringBuffer line = new StringBuffer();
		if( DockerUtil.inDockerEnv() ) line.append( DockerUtil.SPAWN_DOCKER_CONTAINER + " " );
		else if( Config.isOnCluster() ) line.append( FUNCTION_RUN_JOB + " " );
		line.append( workerScriptPath );
		return FUNCTION_EXECUTE_LINE + " \"" + line.toString() + "\" ${LINENO}";
	}

	/**
	 * Pass each line and the current line number to: executeLine
	 *
	 * @param lines Basic script lines generated by a BioModule.
	 * @return List of bash script lines
	 */
	protected static List<String> getWorkerScriptLines( final List<String> lines ) {
		final List<String> out = new ArrayList<>();
		for( final String line: lines )
			out.add( FUNCTION_EXECUTE_LINE + " \"" + line + "\" ${LINENO}" );
		return out;
	}

	/**
	 * Build the file path for the numbered worker script. Leading zeros added if needed so all worker scripts have same
	 * number of digits.
	 * 
	 * @param module ScriptModule is the module that owns the scripts
	 * @param workerId Worker ID
	 * @return Absolute path of next worker script
	 */
	protected static String getWorkerScriptPath( final ScriptModule module, final String workerId ) {
		if( DockerUtil.inAwsEnv() && module instanceof R_Module ) return getMainScriptPath( module );
		return module.getScriptDir().getAbsolutePath() + File.separator + ModuleUtil.displayID( module ) + "." +
			workerId + "_" + module.getClass().getSimpleName() + Constants.SH_EXT;
	}

	/**
	 * Create the ScriptModule main script that calls all worker scripts.
	 *
	 * @param module ScriptModule script directory is where main script is written
	 * @return List of lines for the main script that has the prefix
	 * {@value biolockj.module.ScriptModule#MAIN_SCRIPT_PREFIX}
	 * @throws Exception if error occurs writing the file
	 */
	protected static List<String> initMainScript( final ScriptModule module ) throws Exception {
		final List<String> lines = new ArrayList<>();
		final String scriptPath = getMainScriptPath( module );
		if( Config.getString( module, ScriptModule.SCRIPT_DEFAULT_HEADER ) != null )
			lines.add( Config.getString( module, ScriptModule.SCRIPT_DEFAULT_HEADER ) + RETURN );
		lines.add( PIPE_DIR + "=\"" + Config.pipelinePath() + "\"" + RETURN );
		lines.add( "# BioLockJ " + BioLockJUtil.getVersion() + " " + scriptPath + RETURN );
		lines.add( "touch " + scriptPath + "_" + Constants.SCRIPT_STARTED + RETURN );
		lines.add( "cd " + module.getScriptDir().getAbsolutePath() + RETURN );

		if( DockerUtil.inDockerEnv() ) lines.addAll( DockerUtil.buildSpawnDockerContainerFunction( module ) );
		else if( Config.isOnCluster() ) {
			lines.add( "# Submit job script" );
			lines.add( "function " + FUNCTION_RUN_JOB + "() {" );
			lines.add( Config.requireString( module, CLUSTER_BATCH_COMMAND ) + " $1" );
			lines.add( "}" );
		}

		lines.addAll( buildScriptFailureFunction( scriptPath ) );
		lines.addAll( buildExecuteFunction() );
		return lines;
	}

	/**
	 * Create the numbered worker scripts. Leading zeros added if needed so all worker scripts names are the same
	 * length. If run on cluster and cluster.jobHeader is defined, add cluster.jobHeader as header for worker scripts.
	 * Otherwise, use the same script.defaultHeader as used in the MAIN script.
	 *
	 *
	 * @param module ScriptModule script directory is where main script is written
	 * @param scriptPath Worker script absolute file path
	 * @return Lines for the worker script
	 * @throws Exception if required properties are undefined
	 */
	protected static List<String> initWorkerScript( final ScriptModule module, final String scriptPath )
		throws Exception {
		final List<String> lines = new ArrayList<>();
		final String header = Config.getString( module, SCRIPT_JOB_HEADER );
		final String defaultHeader = Config.getString( module, ScriptModule.SCRIPT_DEFAULT_HEADER );

		if( Config.isOnCluster() && header != null ) lines.add( header + RETURN );
		else if( defaultHeader != null ) lines.add( defaultHeader + RETURN );

		lines.add( PIPE_DIR + "=\"" + Config.pipelinePath() + "\"" + RETURN );
		lines.add( "# BioLockJ." + BioLockJUtil.getVersion() + " " + scriptPath + " #samples/batch=" +
			getBatchSize( module ) + RETURN );
		lines.add( "touch " + scriptPath + "_" + Constants.SCRIPT_STARTED + RETURN );
		lines.addAll( loadModules( module ) );

		final List<String> workerFunctions = module.getWorkerScriptFunctions();
		if( workerFunctions != null && !workerFunctions.isEmpty() ) lines.addAll( workerFunctions );
		lines.addAll( buildScriptFailureFunction( scriptPath ) );
		lines.addAll( buildExecuteFunction() );
		return lines;
	}

	/**
	 * This method formats the bash script to indent if statement code blocks
	 * 
	 * @param writer BufferedWriter writes to the file
	 * @param scriptLines List or shell script lines
	 * @throws IOException if errors occur writing the new script
	 */
	protected static void writeScript( final BufferedWriter writer, final List<String> scriptLines )
		throws IOException {
		int indentCount = 0;
		final List<String> lines = new ArrayList<>();
		try {
			for( final String line: scriptLines )
				if( !line.trim().startsWith( PIPE_DIR ) && line.contains( Config.pipelinePath() ) ) lines
					.add( line.replaceAll( Config.pipelinePath(), Matcher.quoteReplacement( "${" + PIPE_DIR + "}" ) ) );
				else lines.add( line );

			for( String line: lines ) {
				if( line.trim().equals( "fi" ) || line.trim().equals( "}" ) || line.trim().equals( "elif" ) ||
					line.trim().equals( "else" ) || line.trim().equals( "done" ) ) indentCount--;

				int i = 0;
				while( i++ < indentCount )
					line = Constants.INDENT + line;
				writer.write( line + RETURN );
				Log.debug( BashScriptBuilder.class, line );

				if( line.trim().endsWith( "{" ) || line.trim().equals( "elif" ) || line.trim().equals( "else" ) ||
					line.trim().startsWith( "if" ) && line.trim().endsWith( "then" ) ||
					line.trim().startsWith( "while" ) && line.trim().endsWith( "do" ) ) indentCount++;
			}
		} finally {
			if( writer != null ) writer.close();
		}

	}

	private static void buildWorkerScripts( final ScriptModule module, final List<List<String>> data )
		throws Exception {
		workerScripts.clear();
		final int count = getBatchSize( module );
		int numWorkerScripts = count == 0 ? 1: new Double( new Double( data.size() ) / new Double( count ) ).intValue();
		numWorkerScripts = numWorkerScripts == 0 ? 1: numWorkerScripts;

		final int digits = new Integer( numWorkerScripts ).toString().length();
		final List<String> subScriptLines = new ArrayList<>();
		int scriptCount = 0;
		int sampleCount = 0;
		int samplesInScript = 0;
		String workerScriptPath = null;

		for( final List<String> lines: data ) {
			if( lines.size() == 0 )
				throw new Exception( " Worker script is empty for: " + module.getClass().getName() );

			if( samplesInScript == 0 ) {
				subScriptLines.clear();
				workerScriptPath = getWorkerScriptPath( module, getWorkerId( scriptCount++, digits ) );
				subScriptLines.addAll( initWorkerScript( module, workerScriptPath ) );
			}

			subScriptLines.addAll( getWorkerScriptLines( lines ) );

			if( ++sampleCount == data.size() || count > 0 && ++samplesInScript == count ) {
				if( !( module instanceof JavaModule ) )
					subScriptLines.add( "touch " + workerScriptPath + "_" + Constants.SCRIPT_SUCCESS );
				workerScripts.add( createScript( workerScriptPath, subScriptLines ) );
				samplesInScript = 0;
			}
		}

		Log.info( BashScriptBuilder.class, Constants.LOG_SPACER );
		Log.info( BashScriptBuilder.class,
			workerScripts.size() + " WORKER scripts created for: " + module.getClass().getName() );
		Log.info( BashScriptBuilder.class, Constants.LOG_SPACER );
	}

	private static int getBatchSize( final ScriptModule module ) throws ConfigNotFoundException, ConfigFormatException {
		if( batchSize == null ) batchSize = Config.requirePositiveInteger( module, ScriptModule.SCRIPT_BATCH_SIZE );
		return batchSize;
	}

	private static String getMainScriptPath( final ScriptModule module ) {
		return new File( module.getScriptDir().getAbsolutePath() + File.separator + BioModule.MAIN_SCRIPT_PREFIX +
			module.getModuleDir().getName() + Constants.SH_EXT ).getAbsolutePath();
	}

	private static String getWorkerId( final int scriptNum, final int digits ) {
		return String.format( "%0" + digits + "d", scriptNum );
	}

	/**
	 * Return lines to script that load cluster modules based on {@link biolockj.Config}.{@value #CLUSTER_MODULES}
	 *
	 * @param module ScriptModule
	 * @return Bash Script lines to load cluster modules
	 */
	private static List<String> loadModules( final ScriptModule module ) {
		final List<String> lines = new ArrayList<>();
		for( final String clusterMod: Config.getList( module, CLUSTER_MODULES ) )
			lines.add( "module load " + clusterMod );

		if( !lines.isEmpty() ) lines.add( "" );
		final String prologue = Config.getString( module, CLUSTER_PROLOGUE );
		if( prologue != null ) lines.add( prologue + RETURN );

		return lines;
	}

	/**
	 * {@link biolockj.Config} String property: {@value #CLUSTER_BATCH_COMMAND}<br>
	 * Terminal command used to submit jobs on the cluster.
	 */
	protected static final String CLUSTER_BATCH_COMMAND = "cluster.batchCommand";

	/**
	 * {@link biolockj.Config} List property: {@value #CLUSTER_MODULES}<br>
	 * List of cluster modules to load at start of worker scripts.
	 */
	protected static final String CLUSTER_MODULES = "cluster.modules";

	/**
	 * One parameter of the {@link biolockj.Config} String property {@value #SCRIPT_JOB_HEADER} to set number of cores.
	 */
	protected static final List<String> CLUSTER_NUM_PROCESSORS = Arrays.asList( "procs", "ppn" );

	/**
	 * {@link biolockj.Config} String property: {@value #CLUSTER_PROLOGUE}<br>
	 * To run at the start of every script after loading cluster modules (if any)
	 */
	protected static final String CLUSTER_PROLOGUE = "cluster.prologue";

	/**
	 * {@link biolockj.Config} String property: {@value #SCRIPT_JOB_HEADER}<br>
	 * Header written at top of worker scripts
	 */
	protected static final String SCRIPT_JOB_HEADER = "cluster.jobHeader";
	private static Integer batchSize = null;
	private static final String FUNCTION_EXECUTE_LINE = "executeLine";

	private static final String FUNCTION_RUN_JOB = "runJob";

	private static final String FUNCTION_SCRIPT_FAILED = "scriptFailed";
	private static final String PIPE_DIR = "pipeDir";
	private static final String RETURN = Constants.RETURN;
	private static final List<File> workerScripts = new ArrayList<>();
}
