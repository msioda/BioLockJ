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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.exception.ConfigFormatException;
import biolockj.module.JavaModule;
import biolockj.module.ScriptModule;
import biolockj.module.classifier.ClassifierModuleImpl;

/**
 * This utility class generates the bash script files using the lines provided and {@link biolockj.Config} script
 * properties.
 */
public class BashScriptBuilder
{
	// Prevent instantiation
	private BashScriptBuilder()
	{}

	/**
	 * Build the MIAN script.
	 * 
	 * @param module ScriptModule
	 * @throws Exception if any error occurs
	 */
	public static void buildMainScript( final ScriptModule module ) throws Exception
	{
		if( workerScripts.isEmpty() )
		{
			throw new Exception( "No worker scripts created for module: " + module.getClass().getName() );
		}

		int scriptCount = 0;
		final int digits = new Integer( workerScripts.size() - 1 ).toString().length();
		final List<String> mainScriptLines = initMainScript( module );
		for( final File worker: workerScripts )
		{
			mainScriptLines.addAll(
					getMainScriptLines( module, worker.getAbsolutePath(), getWorkerId( scriptCount++, digits ) ) );
		}

		mainScriptLines.addAll( closeScript( getMainScriptPath( module ) ) );
		createScript( getMainScriptPath( module ), mainScriptLines );
		workerScripts.clear();
	}

	/**
	 * This method builds the bash scripts (MAIN + worker scripts) for the given module.
	 * 
	 * @param module ScriptModule
	 * @param data Bash script lines
	 * @param batchSize Number of samples to process per worker script
	 * @throws Exception if any error occurs
	 */
	public static void buildScripts( final ScriptModule module, final List<List<String>> data, int batchSize )
			throws Exception
	{
		verifyConfig( module );

		if( data == null || data.size() < 1 )
		{
			throw new Exception( "Cannot build empty scripts for: " + module.getClass().getName() );
		}

		if( RuntimeParamUtil.isDockerMode() )
		{
			batchSize = data.size();
		}

		buildWorkerScripts( module, data, batchSize );
		buildMainScript( module );
	}

	/**
	 * Returns TRUE if cluster module configured in {@link biolockj.Config} property file.
	 *
	 * @param clusterModuleName Name of cluster module
	 * @return TRUE if clusterModuleName found in {@link biolockj.Config}.{@value #CLUSTER_MODULES}
	 * @throws Exception if property {@value #CLUSTER_MODULES} is missing or invalid
	 */
	public static boolean clusterModuleExists( final String clusterModuleName ) throws Exception
	{
		if( clusterModuleName != null )
		{
			for( final String module: Config.getSet( CLUSTER_MODULES ) )
			{
				if( exists( Config.requireString( CLUSTER_BATCH_COMMAND ) ) && module.contains( clusterModuleName ) )
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Get lines to build the correct indicator file based on script status.
	 *
	 * @param scriptPath Path to script file
	 * 
	 * @return List of bash script lines to close out the script
	 * @throws IOException if error occurs writing the file
	 */
	protected static List<String> closeScript( final String scriptPath ) throws IOException
	{
		final List<String> lines = new ArrayList<>();
		lines.add( "if [ $" + ERROR_DETECTED + " == false ]; then" );
		lines.add( "touch " + scriptPath + "_" + Pipeline.SCRIPT_SUCCESS );
		lines.add( "else" );
		lines.add( "exit 1" );
		lines.add( "fi" );
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
	protected static File createScript( final String scriptPath, final List<String> lines ) throws Exception
	{
		Log.get( BashScriptBuilder.class ).info( "Create Script: " + scriptPath );
		final File workerScript = new File( scriptPath );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( workerScript ) );
		try
		{
			writeScript( writer, lines );
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}

		printScriptAsDebug( scriptPath );
		return workerScript;
	}

	/**
	 * Wrap lines in the MAIN script that call a worker script in control code.
	 * 
	 * @param module ScriptModule
	 * @param workerScriptPath Worker script path
	 * @param workerId Worker script ID
	 * @return List of wrapped MAIN script lines
	 * @throws Exception if I/O errors occur
	 */
	protected static List<String> getMainScriptLines( final ScriptModule module, final String workerScriptPath,
			final String workerId ) throws Exception
	{
		final List<String> lines = new ArrayList<>();
		final StringBuffer cmd = new StringBuffer();

		if( DockerUtil.isDockerScriptModule( module ) )
		{
			cmd.append( DockerUtil.runDockerContainer( DockerUtil.getContainerName( module, workerId ) ) + " "
					+ workerScriptPath );
		}
		else if( Config.isOnCluster() )
		{
			cmd.append( FUNCTION_RUN_JOB + " " + workerScriptPath );
		}
		else // is Docker JavaModule
		{
			cmd.append( workerScriptPath );
		}

		lines.add( "if [ $" + ERROR_DETECTED + " == false ]; then" );
		lines.add( LINE + "=\"" + cmd + "\"" );
		lines.addAll( wrapScriptLine( workerScriptPath.toString(), false ) );
		lines.add( "fi" );
		return lines;
	}

	/**
	 * Lines are wrapped in control statements to allow exit of script based on a single failure, or to simply track if
	 * any failure occur (if exitOnFailure=N). Exit codes are capture in the event of a failure to saved to the
	 * FAIL_FILE.
	 *
	 * @param scriptPath Path to script file
	 * @param lines Basic script lines generated by a BioModule.
	 * @return List of wrapped worker script lines
	 * @throws Exception if I/O errors occur
	 */
	protected static List<String> getWorkerScriptLines( final String scriptPath, final List<String> lines )
			throws Exception
	{
		final List<String> wrappedLines = new ArrayList<>();
		wrappedLines.add( ERROR_ON_PREVIOUS_LINE + "=false" );
		boolean firstLine = true;
		for( final String line: lines )
		{
			if( Config.getBoolean( ScriptModule.SCRIPT_EXIT_ON_ERROR ) )
			{
				wrappedLines.add( "if [ " + ( firstLine ? "": "$" + ERROR_ON_PREVIOUS_LINE + " == false ] && [ " ) + "$"
						+ ERROR_DETECTED + " == false ]; then" );
			}
			else if( !firstLine )
			{
				wrappedLines.add( "if [ $" + ERROR_ON_PREVIOUS_LINE + " == false ]; then" );
			}

			wrappedLines.add( LINE + "=\"" + line + "\"" );
			wrappedLines.addAll( wrapScriptLine( scriptPath, true ) );
			wrappedLines.add( !firstLine || Config.getBoolean( ScriptModule.SCRIPT_EXIT_ON_ERROR ) ? "fi": "" );
			firstLine = false;
		}

		return wrappedLines;
	}

	/**
	 * Build the file path for the numbered worker script. Leading zeros added if needed so all worker scripts have same
	 * number of digits.
	 * 
	 * @param module ScriptModule is the module that owns the scripts
	 * @param workerId Worker ID
	 * @return Absolute path of next worker script
	 * @throws Exception if I/O errors occur
	 */
	protected static String getWorkerScriptPath( final ScriptModule module, final String workerId ) throws Exception
	{
		String modIndex = new Integer( Pipeline.getModules().indexOf( module ) ).toString();
		while( modIndex.length() < Integer.valueOf( Pipeline.getModules().size() ).toString().length() )
		{
			modIndex = "0" + modIndex;
		}

		final String modPrefix = new File( getMainScriptPath( module ) ).getName()
				.replaceAll( ScriptModule.MAIN_SCRIPT_PREFIX, "" );

		final String jobName = modIndex + "." + workerId + modPrefix.substring( modIndex.length() );

		return module.getScriptDir().getAbsolutePath() + File.separator + jobName;
	}

	/**
	 * Create the ScriptModule main script that calls all worker scripts.
	 *
	 * @param module ScriptModule script directory is where main script is written
	 * @return List of lines for the main script that has the prefix
	 * {@value biolockj.module.ScriptModule#MAIN_SCRIPT_PREFIX}
	 * @throws Exception if error occurs writing the file
	 */
	protected static List<String> initMainScript( final ScriptModule module ) throws Exception
	{
		final List<String> lines = new ArrayList<>();
		if( Config.getString( ScriptModule.SCRIPT_DEFAULT_HEADER ) != null )
		{
			lines.add( Config.getString( ScriptModule.SCRIPT_DEFAULT_HEADER ) );
		}

		lines.add( "# BioLockJ " + Log.BLJ_VERSION + " " + getMainScriptPath( module ) );

		if( DockerUtil.isDockerScriptModule( module ) )
		{
			lines.add( "### This script can spawn multiple Docker containers for parallel processing ###" + RETURN );
			lines.addAll( DockerUtil.buildRunDockerFunction( module ) );
		}
		else if( Config.isOnCluster() )
		{
			lines.add( "### This script can submit multiple worker scripts for parallel processing ###" + RETURN );
			lines.add( "cd " + ModuleUtil.requireSubDir( module, QSUB_DIR ).getAbsolutePath() );
			lines.add( "function " + FUNCTION_RUN_JOB + "() {" );
			lines.add( Config.requireString( CLUSTER_BATCH_COMMAND ) + " $1" );
			lines.add( "}" );
		}

		lines.add( "touch " + getMainScriptPath( module ) + "_" + Pipeline.SCRIPT_STARTED );
		lines.add( ERROR_DETECTED + "=false" );
		lines.add( FAIL_FILE + "=" + getMainScriptPath( module ) + "_" + Pipeline.SCRIPT_FAILURES );
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
			throws Exception
	{
		final List<String> lines = new ArrayList<>();
		if( Config.isOnCluster() && getJobHeader( module ) != null )
		{
			lines.add( getJobHeader( module ) );
		}
		else if( Config.getString( ScriptModule.SCRIPT_DEFAULT_HEADER ) != null )
		{
			lines.add( Config.getString( ScriptModule.SCRIPT_DEFAULT_HEADER ) );
		}

		lines.add( "#BioLockJ." + Log.BLJ_VERSION + " " + scriptPath + " | batch size = "
				+ new Integer( Config.requirePositiveInteger( ScriptModule.SCRIPT_BATCH_SIZE ) ).toString() );
		lines.add( "touch " + scriptPath + "_" + Pipeline.SCRIPT_STARTED );
		lines.addAll( loadModules() );

		final List<String> bashFunctions = module.getWorkerScriptFunctions();
		if( bashFunctions != null && !bashFunctions.isEmpty() )
		{
			lines.addAll( bashFunctions );
		}

		lines.add( FAIL_FILE + "=" + scriptPath + "_" + Pipeline.SCRIPT_FAILURES );
		lines.add( ERROR_DETECTED + "=false" );
		return lines;
	}

	/**
	 * If property {@value biolockj.Config#PROJECT_ENV} = cluster, require property {@value #CLUSTER_BATCH_COMMAND},
	 * otherwise exit this method.
	 * <p>
	 * If running on the cluster and property {@value #CLUSTER_VALIDATE_PARAMS} = {@value biolockj.Config#TRUE}:
	 * <ol>
	 * <li>Require property {@value #SCRIPT_JOB_HEADER}
	 * <li>Require property {@value #CLUSTER_BATCH_COMMAND} sets #cores param (ppn or procs) =
	 * {@value biolockj.module.ScriptModule#SCRIPT_NUM_THREADS} or
	 * {@value biolockj.module.ScriptModule#CLASSIFIER_NUM_THREADS}
	 * </ol>
	 * <p>
	 * Current format #1 (2018) for UNCC HPC Cluster: #PBS -l procs=1,mem=8GB,walltime=8:00:00<br>
	 * Current format #2 (2018) for UNCC HPC Cluster: #PBS -l ppn=1,mem=8GB,walltime=8:00:00<br>
	 *
	 * @param module ScriptModuel
	 * @throws Exception if any validation fails
	 */
	protected static void verifyConfig( final ScriptModule module ) throws Exception
	{

		if( !Config.isOnCluster() )
		{
			return;
		}

		Config.requireString( CLUSTER_BATCH_COMMAND );

		if( !Config.getBoolean( CLUSTER_VALIDATE_PARAMS ) )
		{
			Log.get( BashScriptBuilder.class ).warn( CLUSTER_VALIDATE_PARAMS + "=" + Config.FALSE
					+ " --  Will NOT enforce cluster #cores  = " + getNumThreadsParam( module ) );
			return;
		}

		final String jobScriptHeader = getJobHeader( module );

		if( !jobScriptHeader.startsWith( "#PBS" ) )
		{
			throw new ConfigFormatException( getJobHeaderParam( module ), "Must begin with \"#PBS\"" );
		}

		if( !hasNumCoresParam( jobScriptHeader ) )
		{
			Log.get( BashScriptBuilder.class )
					.warn( getJobHeaderParam( module ) + " does not have #cores param defined" );
			return;
		}

		final StringTokenizer st = new StringTokenizer( jobScriptHeader, "," );
		while( st.hasMoreTokens() )
		{
			String token = st.nextToken();
			if( hasNumCoresParam( token ) ) // value like "#PBS -l procs=1"
			{
				boolean foundNumCores = false;
				token = token.substring( token.indexOf( "-l " ) + 3 ).trim();
				final StringTokenizer pToken = new StringTokenizer( token, "=" );
				while( pToken.hasMoreTokens() )
				{
					token = pToken.nextToken().trim();
					if( !foundNumCores )
					{
						if( hasNumCoresParam( token ) )
						{
							foundNumCores = true;
						}
					}
					else
					{
						if( Integer.valueOf( token ) != getNumThreads( module ) )
						{
							throw new ConfigFormatException( getJobHeaderParam( module ),
									"Inconsistent config values. " + getNumThreadsParam( module ) + "="
											+ getNumThreads( module ) + " & " + getJobHeaderParam( module ) + "="
											+ jobScriptHeader + " --> (#" + CLUSTER_NUM_PROCESSORS + "="
											+ Integer.valueOf( token ) + ")" );
						}
						break;
					}
				}
			}
		}
	}

	/**
	 * This method requires that the variable ${@value #LINE} holds an executable shell statement.<br>
	 * Wrapping code:
	 * <ol>
	 * <li>Save executing line number to ${@value #LINE_NUM}
	 * <li>Execute script line ${@value #LINE}.
	 * <li>Capture ${@value #EXIT_CODE} to determine execution status
	 * <li>If the ${@value #LINE} failed:
	 * <ul>
	 * <li>Set {@value #ERROR_DETECTED}=true
	 * <li>If isWorker, set {@value #ERROR_ON_PREVIOUS_LINE}=true
	 * </ul>
	 * </ol>
	 * 
	 * @param scriptPath Path to script file
	 * @param isWorker Set true if building worker script
	 * @return List of wrapped lines
	 */
	protected static List<String> wrapScriptLine( final String scriptPath, final boolean isWorker )
	{
		final List<String> lines = new ArrayList<>();
		lines.add( LINE_NUM + "=$LINENO && $" + LINE );
		lines.add( EXIT_CODE + "=$?" );
		lines.add( "if [ $" + EXIT_CODE + " != 0 ]; then" );
		lines.add( "if [ $" + ERROR_DETECTED + " == false ]; then" );
		lines.add( "touch $" + FAIL_FILE );
		lines.add( "fi" );
		lines.add( "echo \"Failure code [ $" + EXIT_CODE + " ] on Line [ $" + LINE_NUM + " ]:  $" + LINE + "\" >> $"
				+ FAIL_FILE );
		if( isWorker )
		{
			lines.add( ERROR_ON_PREVIOUS_LINE + "=true" );
		}
		lines.add( ERROR_DETECTED + "=true" );
		lines.add( "fi" );
		return lines;
	}

	/**
	 * This method formats the bash script to indent if statement code blocks
	 * 
	 * @param writer BufferedWriter writes to the file
	 * @param lines List or shell script lines
	 * @throws Exception if I/O errors occur
	 */
	protected static void writeScript( final BufferedWriter writer, final List<String> lines ) throws Exception
	{
		int indentCount = 0;

		for( final String line: lines )
		{
			if( line.trim().equals( "fi" ) || line.trim().equals( "}" ) || line.equals( "elif" )
					|| line.equals( "else" ) )
			{
				indentCount--;
			}

			int i = 0;
			while( i++ < indentCount )
			{
				writer.write( INDENT );
			}

			writer.write( line + RETURN );

			if( line.endsWith( "{" ) || line.equals( "elif" ) || line.equals( "else" )
					|| line.startsWith( "if" ) && line.endsWith( "then" ) )
			{
				indentCount++;
			}
		}

		writer.close();
	}

	private static void buildWorkerScripts( final ScriptModule module, final List<List<String>> data,
			final int batchSize ) throws Exception
	{
		workerScripts.clear();
		int numWorkerScripts = batchSize == 0 ? 1: new Integer( data.size() / batchSize );
		numWorkerScripts = numWorkerScripts == 0 ? 1: numWorkerScripts;

		final int digits = new Integer( numWorkerScripts ).toString().length();
		final List<String> subScriptLines = new ArrayList<>();
		int scriptCount = 0;
		int sampleCount = 0;
		int samplesInScript = 0;
		String workerScriptPath = null;

		for( final List<String> lines: data )
		{
			if( lines.size() == 0 )
			{
				throw new Exception(
						" Worker script cannot be written with zero lines: " + module.getClass().getName() );
			}

			if( samplesInScript == 0 )
			{
				final String workerId = getWorkerId( scriptCount++, digits );
				subScriptLines.clear();
				workerScriptPath = getWorkerScriptPath( module, workerId );
				subScriptLines.addAll( initWorkerScript( module, workerScriptPath ) );
			}

			subScriptLines.addAll( getWorkerScriptLines( workerScriptPath, lines ) );

			if( ++sampleCount == data.size() || batchSize > 0 && ++samplesInScript == batchSize )
			{
				if( !( module instanceof JavaModule ) )
				{
					subScriptLines.addAll( closeScript( workerScriptPath ) );
				}
				workerScripts.add( createScript( workerScriptPath, subScriptLines ) );
				samplesInScript = 0;
			}
		}

		Log.get( BashScriptBuilder.class ).info( Log.LOG_SPACER );
		Log.get( BashScriptBuilder.class )
				.info( workerScripts.size() + " WORKER scripts created for: " + module.getClass().getName() );
		Log.get( BashScriptBuilder.class ).info( Log.LOG_SPACER );
	}

	/**
	 * Convenience methods that validates property exists.
	 *
	 * @param propName Name of {@link biolockj.Config} property
	 * @return
	 */
	private static boolean exists( final String propName )
	{
		if( propName != null && propName.trim().length() > 0 )
		{
			return true;
		}

		return false;
	}

	private static String getJobHeader( final ScriptModule module ) throws Exception
	{
		return isClassifier( module ) && Config.getString( CLASSIFIER_JOB_HEADER ) != null
				? Config.getString( CLASSIFIER_JOB_HEADER )
				: Config.requireString( SCRIPT_JOB_HEADER );
	}

	private static String getJobHeaderParam( final ScriptModule module ) throws Exception
	{
		return isClassifier( module ) && Config.getString( CLASSIFIER_JOB_HEADER ) != null ? CLASSIFIER_JOB_HEADER
				: SCRIPT_JOB_HEADER;
	}

	private static String getMainScriptPath( final ScriptModule scriptModule ) throws Exception
	{
		return new File( scriptModule.getScriptDir().getAbsolutePath() + File.separator
				+ ScriptModule.MAIN_SCRIPT_PREFIX + scriptModule.getModuleDir().getName() + ".sh" ).getAbsolutePath();
	}

	private static Integer getNumThreads( final ScriptModule module ) throws Exception
	{
		return isClassifier( module ) ? ClassifierModuleImpl.getNumThreads()
				: Config.requirePositiveInteger( ScriptModule.SCRIPT_NUM_THREADS );
	}

	private static String getNumThreadsParam( final ScriptModule module ) throws Exception
	{
		return isClassifier( module ) ? ClassifierModuleImpl.getNumThreadsParam(): ScriptModule.SCRIPT_NUM_THREADS;
	}

	private static String getWorkerId( final int scriptNum, final int digits )
	{
		return String.format( "%0" + digits + "d", scriptNum );
	}

	private static boolean hasNumCoresParam( final String param ) throws Exception
	{
		if( param.indexOf( "-l " ) > 0 )
		{
			final String paramsNoSpaces = param.substring( param.indexOf( "-l " ) + 3 ).trim();
			if( paramsNoSpaces.length() < 1 )
			{
				throw new ConfigFormatException( SCRIPT_JOB_HEADER,
						"Param resource_list defined via \"-l\" but incomplete!" );
			}
			if( paramsNoSpaces.contains( " " ) )
			{
				throw new ConfigFormatException( SCRIPT_JOB_HEADER, "Must not contain spaces!" );
			}

			for( final String paramName: CLUSTER_NUM_PROCESSORS )
			{
				if( paramsNoSpaces.contains( paramName ) )
				{
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isClassifier( final ScriptModule module ) throws Exception
	{
		return module.getClass().getPackage().getName().startsWith( "biolockj.module.classifier" )
				|| module.getClass().getName().toLowerCase().endsWith( "classifier" );
	}

	/**
	 * Return lines to script that load cluster modules based on {@link biolockj.Config}.{@value #CLUSTER_MODULES}
	 *
	 * @return Bash Script lines to load cluster modules
	 * @throws Exception if {@value #CLUSTER_MODULES} undefined or invalid
	 */
	private static List<String> loadModules() throws Exception
	{
		final List<String> lines = new ArrayList<>();
		for( final String module: Config.getSet( CLUSTER_MODULES ) )
		{
			lines.add( "module load " + module );
		}
		return lines;
	}

	/**
	 * Print bash script lines to BioLockJ log file for debug purposes.
	 *
	 * @param file
	 */
	private static void printScriptAsDebug( final String filePath )
	{
		Log.get( BashScriptBuilder.class ).debug( "PRINT FILE => " + filePath );
		try
		{
			final BufferedReader in = SeqUtil.getFileReader( new File( filePath ) );
			String line;
			while( ( line = in.readLine() ) != null )
			{
				Log.get( BashScriptBuilder.class ).debug( line );
			}
			in.close();
		}
		catch( final Exception ex )
		{
			Log.get( BashScriptBuilder.class ).error( "Error occurred printing script to log file: " + filePath, ex );
		}
	}

	/**
	 * Name of the module directory where cluster job output/error files are to be created: {@value #QSUB_DIR}
	 */
	public static final String QSUB_DIR = "qsub";

	/**
	 * {@link biolockj.Config} property to use in cluster jobHeader .<br>
	 * : {@value #CLASSIFIER_JOB_HEADER}<br>
	 * Header written at top of QIIME OTU picking worker scripts
	 */
	protected static final String CLASSIFIER_JOB_HEADER = "cluster.classifierHeader";

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
	protected static final List<String> CLUSTER_NUM_PROCESSORS = Arrays.asList( new String[] { "procs", "ppn" } );

	/**
	 * {@link biolockj.Config} Boolean property: {@value #CLUSTER_VALIDATE_PARAMS}<br>
	 * If set to {@value biolockj.Config#TRUE}, validate {@value #SCRIPT_JOB_HEADER} param cluster number of processors
	 * = {@value biolockj.module.ScriptModule#SCRIPT_NUM_THREADS} or
	 * {@value biolockj.module.ScriptModule#CLASSIFIER_NUM_THREADS}
	 */
	protected static final String CLUSTER_VALIDATE_PARAMS = "cluster.validateParams";

	/**
	 * Script variable set true if any script error occurs: {@value #ERROR_DETECTED}
	 */
	protected static final String ERROR_DETECTED = "errorDetected";

	/**
	 * Script variable set true if a script errors on previous line: {@value #ERROR_ON_PREVIOUS_LINE}
	 */
	protected static final String ERROR_ON_PREVIOUS_LINE = "errorOnPreviousLine";

	/**
	 * Script variable to store exit code of previous statement: {@value #EXIT_CODE}
	 */
	protected static final String EXIT_CODE = "exitCode";

	/**
	 * Script variable to store path of file where error messages will be saved: {@value #FAIL_FILE}
	 */
	protected static final String FAIL_FILE = "failureFile";

	/**
	 * Main script function to submit jobs on the cluster
	 */
	protected static final String FUNCTION_RUN_JOB = "runJob";

	/**
	 * Script variable to store the next script line to execute: {@value #LINE}
	 */
	protected static final String LINE = "line";

	/**
	 * Script variable to store the line number of the current executable statement: {@value #LINE}
	 */
	protected static final String LINE_NUM = "lineNum";

	/**
	 * {@link biolockj.Config} String property: {@value #SCRIPT_JOB_HEADER}<br>
	 * Header written at top of worker scripts
	 */
	protected static final String SCRIPT_JOB_HEADER = "cluster.jobHeader";

	private static final String INDENT = "    ";
	private static final String RETURN = BioLockJ.RETURN;
	private static final List<File> workerScripts = new ArrayList<>();

}
