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
import biolockj.*;
import biolockj.exception.ConfigFormatException;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.ScriptModule;

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
			mainScriptLines.add( getMainScriptExecuteWorkerLine( module, worker.getAbsolutePath(),
					getWorkerId( scriptCount++, digits ) ) );
		}

		mainScriptLines
				.add( Constants.RETURN + "touch " + getMainScriptPath( module ) + "_" + Constants.SCRIPT_SUCCESS );
		createScript( getMainScriptPath( module ), mainScriptLines );
		workerScripts.clear();
	}

	/**
	 * This method builds the bash scripts (MAIN + worker scripts) for the given module.
	 * 
	 * @param module ScriptModule
	 * @param data Bash script lines
	 * @throws Exception if any error occurs
	 */
	public static void buildScripts( final ScriptModule module, final List<List<String>> data ) throws Exception
	{
		if( data == null || data.size() < 1 )
		{
			throw new Exception( "Cannot build empty scripts for: " + module.getClass().getName() );
		}

		verifyConfig( module );
		setBatchSize( module, data );

		buildWorkerScripts( module, data );
		buildMainScript( module );
	}

	/**
	 * Create bash worker script function: {@value #FUNCTION_EXECUTE}.<br>
	 * Failure details written to the failure script file if any occur.
	 * <ol>
	 * <li>$1 Script payload line to execute
	 * <li>$2 Script line number
	 * </ol>
	 * 
	 * @param script Execute script
	 * @return Bash script lines
	 * @throws Exception if errors occur
	 */
	protected static List<String> buildExecuteFunction( final String script ) throws Exception
	{
		final List<String> lines = new ArrayList<>();
		lines.add( "function " + FUNCTION_EXECUTE + "() {" );
		lines.add( "$1" );
		lines.add( "statusCode=$?" );
		lines.add( "if [ $statusCode != 0 ]; then" );
		lines.add( "echo \"Failure code [ $statusCode ] on Line [ $2 ]:  $1\" >> \"" + script + "_"
				+ Constants.SCRIPT_FAILURES + "\"" );
		lines.add( "exit $statusCode" );
		lines.add( "fi" );
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
	protected static File createScript( final String scriptPath, final List<String> lines ) throws Exception
	{
		Log.info( BashScriptBuilder.class, "Create Script: " + scriptPath );
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
	 * Call {@value #FUNCTION_EXECUTE} on the worker script
	 * 
	 * @param module ScriptModule
	 * @param workerScriptPath Worker script path
	 * @param workerId Worker script ID
	 * @return bash script line
	 * @throws Exception if errors occur
	 */
	protected static String getMainScriptExecuteWorkerLine( final ScriptModule module, final String workerScriptPath,
			final String workerId ) throws Exception
	{
		final StringBuffer line = new StringBuffer();

		if( RuntimeParamUtil.isDockerMode() )
		{
			line.append( DockerUtil.SPAWN_DOCKER_CONTAINER + " " );
		}
		else if( Config.isOnCluster() )
		{
			line.append( FUNCTION_RUN_JOB + " " );
		}

		line.append( workerScriptPath );

		return FUNCTION_EXECUTE + " \"" + line.toString() + "\" $LINENO";

	}

	/**
	 * Pass each line and the current line number to {@value #FUNCTION_EXECUTE}
	 *
	 * @param lines Basic script lines generated by a BioModule.
	 * @return List of bash script lines
	 * @throws Exception if errors occur
	 */
	protected static List<String> getWorkerScriptLines( final List<String> lines ) throws Exception
	{
		final List<String> wrappedLines = new ArrayList<>();
		for( final String line: lines )
		{
			wrappedLines.add( FUNCTION_EXECUTE + " \"" + line + "\" $LINENO" );
		}
		wrappedLines.add( "" );
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
		final String modId = ModuleUtil.displayID( module );

		final String modPrefix = new File( getMainScriptPath( module ) ).getName()
				.replaceAll( BioModule.MAIN_SCRIPT_PREFIX, "" );

		final String jobName = modId + "." + workerId + modPrefix.substring( 2 );

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

		if( Config.getString( module, ScriptModule.SCRIPT_DEFAULT_HEADER ) != null )
		{
			lines.add( Config.getString( module, ScriptModule.SCRIPT_DEFAULT_HEADER ) + RETURN );
		}

		lines.add( "# BioLockJ " + BioLockJUtil.getVersion() + " " + getMainScriptPath( module ) + RETURN );
		lines.add( "touch " + getMainScriptPath( module ) + "_" + Constants.SCRIPT_STARTED + RETURN );
		lines.add( "cd " + module.getScriptDir().getAbsolutePath() + RETURN );

		if( RuntimeParamUtil.isDockerMode() )
		{
			lines.addAll( DockerUtil.buildSpawnDockerContainerFunction( module ) );
		}
		else if( Config.isOnCluster() )
		{
			lines.add( "# Submit job script" );
			lines.add( "function " + FUNCTION_RUN_JOB + "() {" );
			lines.add( Config.requireString( module, CLUSTER_BATCH_COMMAND ) + " $1" );
			lines.add( "}" );
		}

		lines.addAll( buildExecuteFunction( getMainScriptPath( module ) ) );
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
		final String header = Config.getString( module, SCRIPT_JOB_HEADER );
		if( Config.isOnCluster() && header != null )
		{
			lines.add( header + RETURN );
		}
		else if( Config.getString( module, ScriptModule.SCRIPT_DEFAULT_HEADER ) != null )
		{
			lines.add( Config.getString( module, ScriptModule.SCRIPT_DEFAULT_HEADER ) + RETURN );
		}

		lines.add(
				"#BioLockJ." + BioLockJUtil.getVersion() + " " + scriptPath + " | batch size = " + batchSize + RETURN );

		lines.add( "touch " + scriptPath + "_" + Constants.SCRIPT_STARTED + RETURN );
		lines.addAll( loadModules( module ) );

		final List<String> bashFunctions = module.getWorkerScriptFunctions();
		if( bashFunctions != null && !bashFunctions.isEmpty() )
		{
			lines.addAll( bashFunctions );
		}

		lines.addAll( buildExecuteFunction( scriptPath ) );
		return lines;
	}

	/**
	 * If property {@value biolockj.Constants#PROJECT_ENV} = cluster, require property {@value #CLUSTER_BATCH_COMMAND},
	 * otherwise exit this method.
	 * <p>
	 * If running on the cluster and property {@value #CLUSTER_VALIDATE_PARAMS} = {@value biolockj.Constants#TRUE}:
	 * <ol>
	 * <li>Require property {@value #SCRIPT_JOB_HEADER}
	 * <li>Require property {@value #CLUSTER_BATCH_COMMAND} sets #cores param (ppn or procs) =
	 * {@link biolockj.module.ScriptModule#SCRIPT_NUM_THREADS} or
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

		Config.requireString( module, CLUSTER_BATCH_COMMAND );

		if( !Config.getBoolean( null, CLUSTER_VALIDATE_PARAMS ) )
		{
			Log.warn( BashScriptBuilder.class, CLUSTER_VALIDATE_PARAMS + "=" + Constants.FALSE
					+ " --  Will NOT enforce cluster #cores  = " + ScriptModule.SCRIPT_NUM_THREADS );
			// + getNumThreadsParam( module ) );
			return;
		}
		final String jobHeaderParam = Config.getModuleProp( module, SCRIPT_JOB_HEADER );
		final String jobScriptHeader = Config.requireString( module, jobHeaderParam );

		if( !jobScriptHeader.startsWith( "#PBS" ) )
		{
			throw new ConfigFormatException( SCRIPT_JOB_HEADER, "Must begin with \"#PBS\"" );
		}

		if( !hasNumCoresParam( jobScriptHeader ) )
		{
			Log.warn( BashScriptBuilder.class, SCRIPT_JOB_HEADER + " does not have #cores param defined" );
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

						final String numThreadsParam = Config.getModuleProp( module, ScriptModule.SCRIPT_NUM_THREADS );
						final int numThreads = Config.requirePositiveInteger( module, ScriptModule.SCRIPT_NUM_THREADS );
						if( Integer.valueOf( token ) != numThreads )
						{
							throw new ConfigFormatException( jobHeaderParam,
									"Inconsistent config values. " + numThreadsParam + "=" + numThreads + " & "
											+ jobHeaderParam + "=" + jobScriptHeader + " --> (#"
											+ CLUSTER_NUM_PROCESSORS + "=" + Integer.valueOf( token ) + ")" );
						}
						break;
					}
				}
			}
		}
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
			if( line.trim().equals( "fi" ) || line.trim().equals( "}" ) || line.trim().equals( "elif" )
					|| line.trim().equals( "else" ) || line.trim().equals( "done" ) )
			{
				indentCount--;
			}

			int i = 0;
			while( i++ < indentCount )
			{
				writer.write( Constants.INDENT );
			}

			writer.write( line + RETURN );

			if( line.trim().endsWith( "{" ) || line.trim().equals( "elif" ) || line.trim().equals( "else" )
					|| line.trim().startsWith( "if" ) && line.trim().endsWith( "then" )
					|| line.trim().startsWith( "while" ) && line.trim().endsWith( "do" ) )
			{
				indentCount++;
			}
		}

		writer.close();
	}

	private static void buildWorkerScripts( final ScriptModule module, final List<List<String>> data ) throws Exception
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
				subScriptLines.clear();
				workerScriptPath = getWorkerScriptPath( module, getWorkerId( scriptCount++, digits ) );
				subScriptLines.addAll( initWorkerScript( module, workerScriptPath ) );
			}

			subScriptLines.addAll( getWorkerScriptLines( lines ) );

			if( ++sampleCount == data.size() || batchSize > 0 && ++samplesInScript == batchSize )
			{
				if( !( module instanceof JavaModule ) )
				{
					subScriptLines.add( "touch " + workerScriptPath + "_" + Constants.SCRIPT_SUCCESS );
				}
				workerScripts.add( createScript( workerScriptPath, subScriptLines ) );
				samplesInScript = 0;
			}
		}

		Log.info( BashScriptBuilder.class, Log.LOG_SPACER );
		Log.info( BashScriptBuilder.class,
				workerScripts.size() + " WORKER scripts created for: " + module.getClass().getName() );
		Log.info( BashScriptBuilder.class, Log.LOG_SPACER );
	}

	private static String getMainScriptPath( final ScriptModule scriptModule ) throws Exception
	{
		return new File( scriptModule.getScriptDir().getAbsolutePath() + File.separator + BioModule.MAIN_SCRIPT_PREFIX
				+ scriptModule.getModuleDir().getName() + Constants.SH_EXT ).getAbsolutePath();
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

	/**
	 * Return lines to script that load cluster modules based on {@link biolockj.Config}.{@value #CLUSTER_MODULES}
	 *
	 * @param module ScriptModule
	 * @return Bash Script lines to load cluster modules
	 * @throws Exception if {@value #CLUSTER_MODULES} undefined or invalid
	 */
	private static List<String> loadModules( final ScriptModule module ) throws Exception
	{
		final List<String> lines = new ArrayList<>();
		for( final String clusterMod: Config.getList( module, CLUSTER_MODULES ) )
		{
			lines.add( "module load " + clusterMod );
		}

		if( !lines.isEmpty() )
		{
			lines.add( "" );
		}
		final String prologue = Config.getString( module, CLUSTER_PROLOGUE );
		if( prologue != null )
		{
			lines.add( prologue + RETURN );
		}

		return lines;
	}

	/**
	 * Print bash script lines to BioLockJ log file for debug purposes.
	 *
	 * @param filePath String
	 */
	private static void printScriptAsDebug( final String filePath )
	{
		Log.debug( BashScriptBuilder.class, "PRINT FILE => " + filePath );
		try
		{
			final BufferedReader in = BioLockJUtil.getFileReader( new File( filePath ) );
			String line;
			while( ( line = in.readLine() ) != null )
			{
				Log.debug( BashScriptBuilder.class, line );
			}
			in.close();
		}
		catch( final Exception ex )
		{
			Log.error( BashScriptBuilder.class, "Error occurred printing script to log file: " + filePath, ex );
		}
	}

	private static void setBatchSize( final ScriptModule module, final List<List<String>> data ) throws Exception
	{
		if( DockerUtil.isBljManager() )
		{
			batchSize = data.size();
		}
		else
		{
			batchSize = Config.requirePositiveInteger( module, ScriptModule.SCRIPT_BATCH_SIZE );
		}
	}

	/**
	 * {@link biolockj.Config} Boolean property {@value #CLUSTER_RUN_JAVA_AS_SCRIPT} if set =
	 * {@value biolockj.Constants#TRUE} will run Java module as a script instead of running on the head node.
	 */
	public static final String CLUSTER_RUN_JAVA_AS_SCRIPT = "cluster.runJavaAsScriptModule";

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
	 * {@link biolockj.Config} String property: {@value #CLUSTER_PROLOGUE}<br>
	 * To run at the start of every script after loading cluster modules (if any)
	 */
	protected static final String CLUSTER_PROLOGUE = "cluster.prologue";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #CLUSTER_VALIDATE_PARAMS}<br>
	 * If set to {@value biolockj.Constants#TRUE}, validate {@value #SCRIPT_JOB_HEADER} param cluster number of
	 * processors = {@value biolockj.module.ScriptModule#SCRIPT_NUM_THREADS} or
	 * {@link biolockj.module.ScriptModule#SCRIPT_NUM_THREADS}
	 */
	protected static final String CLUSTER_VALIDATE_PARAMS = "cluster.validateParams";

	/**
	 * Worker script function to execute 1 line of a worker script.
	 */
	protected static final String FUNCTION_EXECUTE = "execute";

	/**
	 * Main script function to submit jobs on the cluster
	 */
	protected static final String FUNCTION_RUN_JOB = "runJob";

	/**
	 * {@link biolockj.Config} String property: {@value #SCRIPT_JOB_HEADER}<br>
	 * Header written at top of worker scripts
	 */
	protected static final String SCRIPT_JOB_HEADER = "cluster.jobHeader";

	private static int batchSize = 0;
	private static final String RETURN = Constants.RETURN;
	private static final List<File> workerScripts = new ArrayList<>();
}
