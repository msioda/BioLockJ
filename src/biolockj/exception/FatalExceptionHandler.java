/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Mar 18, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import biolockj.*;
import biolockj.util.*;

/**
 * FatalExceptionHandler saves logs somewhere the user can access if failures occur before the log file is generated.
 */
public class FatalExceptionHandler {

	/**
	 * Return the existing error log, or build a new log if not yet created.
	 * 
	 * @return Error log file
	 */
	public static File getErrorLog() {
		if( errorLog != null ) return errorLog;
		final File dir = getErrorLogDir();
		if( dir == null ) return null;
		final String suffix = getErrorLogSuffix();
		int i = 0;
		File file = new File(
			dir.getAbsolutePath() + File.separator + FATAL_ERROR_FILE_PREFIX + suffix + Constants.LOG_EXT );
		while( file.exists() ) {
			file = new File( dir.getAbsolutePath() + File.separator + FATAL_ERROR_FILE_PREFIX + suffix + "_"
				+ new Integer( ++i ).toString() + Constants.LOG_EXT );
		}
		errorLog = file;
		return errorLog;
	}

	/**
	 * Print the {@link biolockj.Log} messages and the exception stack trace info to the $USER $HOME directory.
	 * 
	 * @param args Java runtime args
	 * @param ex Fatal application exception
	 */
	public static void logFatalError( final String[] args, final Exception ex ) {
		if( Log.getFile() != null && Log.getFile().isFile() ) {
			setFailedStatus();
			SummaryUtil.addSummaryFooterForFailedPipeline();
		} else {
			createErrorLog();
		}

		logFatalException( args, ex );

		if( getErrorLog() != null ) {
			dumpLogs( getLogs() );
		} else if( !RuntimeParamUtil.isDebugMode() ) {
			printLogsOnScreen( getLogs() );
		} else {
			System.out.println(
				"Log file not found or created.  System in DEBUG mode, so logs should already be on screen." );
		}
	}

	private static void createErrorLog() {
		Log.warn( FatalExceptionHandler.class, "Pipeline failed before pipeline directory or log were created!" );
		final File errFile = getErrorLog();
		if( errFile == null ) {
			System.out.println( "Failed to build error log error log" );
			return;
		}
		String hostPath = errFile.getAbsolutePath();
		if( DockerUtil.inDockerEnv() ) {
			if( hostPath.startsWith( DockerUtil.CONTAINER_OUTPUT_DIR ) ) {
				hostPath = hostPath.replace( DockerUtil.CONTAINER_OUTPUT_DIR,
					RuntimeParamUtil.getDockerHostPipelineDir() );
			} else if( hostPath.startsWith( DockerUtil.DOCKER_ROOT_HOME ) ) {
				hostPath = hostPath.replace( DockerUtil.DOCKER_ROOT_HOME, RuntimeParamUtil.getDockerHostHomeDir() );
			}
		}

		Log.warn( FatalExceptionHandler.class, "Local file-system error log path: " + errFile.getAbsolutePath() );
		if( !hostPath.equals( errFile.getAbsolutePath() ) ) {
			Log.warn( FatalExceptionHandler.class, "Host file-system error log path: " + hostPath );
		}
	}

	private static void dumpLogs( final List<String> lines ) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter( getErrorLog() ) );
			for( final String line: lines ) {
				writer.write( line );
			}
		} catch( final Exception ex ) {
			System.out.println( "Failed to write to: " + getErrorLog().getAbsolutePath() );
			ex.printStackTrace();
		} finally {
			try {
				if( writer != null ) {
					writer.close();
				}
			} catch( final IOException ex3 ) {
				System.out.println( "Failed to close writer for: " + getErrorLog().getAbsolutePath() );
			}
		}
	}

	private static File getErrorLogDir() {
		File dir = RuntimeParamUtil.getBaseDir();
		if( dir == null || !dir.isDirectory() ) {
			dir = RuntimeParamUtil.getHomeDir();
		}
		if( dir == null || !dir.isDirectory() ) {
			final String path = Config.replaceEnvVar( "${HOME}" );
			if( path != null && !path.isEmpty() ) {
				dir = new File( path );
			}
		}

		if( dir == null || !dir.isDirectory() ) {
			Log.warn( FatalExceptionHandler.class, "Unable to find $BLJ_PROJ or $HOME dirs" );
			return null;
		}
		Log.warn( FatalExceptionHandler.class, "Save Error File to: " + dir.getAbsolutePath() );
		return dir;
	}

	private static String getErrorLogSuffix() {
		if( DockerUtil.isDirectMode() ) return RuntimeParamUtil.getDirectModuleDir();
		else if( Config.pipelineName() != null ) return Config.pipelineName();
		else if( RuntimeParamUtil.getConfigFile() != null ) return RuntimeParamUtil.getConfigFile().getName();
		return "Unknown_Config";
	}

	private static List<String> getLogs() {
		final List<String> lines = new ArrayList<>();
		for( final String[] m: Log.getMsgs() ) {
			if( m[ 0 ].equals( Log.DEBUG ) ) {
				lines.add( "[ " + Log.DEBUG + " ] " + m[ 1 ] + Constants.RETURN );
			}
			if( m[ 0 ].equals( Log.INFO ) ) {
				lines.add( "[ " + Log.INFO + " ] " + m[ 1 ] + Constants.RETURN );
			}
			if( m[ 0 ].equals( Log.WARN ) ) {
				lines.add( "[ " + Log.WARN + " ] " + m[ 1 ] + Constants.RETURN );
			}
			if( m[ 0 ].equals( Log.ERROR ) ) {
				lines.add( "[ " + Log.ERROR + " ] " + m[ 1 ] + Constants.RETURN );
			}
		}
		return lines;
	}

	private static void logFatalException( final String[] args, final Exception ex ) {
		if( Log.getFile() != null ) {
			Log.error( FatalExceptionHandler.class, Constants.LOG_SPACER );
			Log.error( FatalExceptionHandler.class, Constants.RETURN + "FATAL APPLICATION ERROR " + ( args == null ? ""
				: " -->" + Constants.RETURN + " Program args: " + RuntimeParamUtil.getRuntimeArgs() ), ex );
			Log.error( FatalExceptionHandler.class, Constants.LOG_SPACER );
			ex.printStackTrace();
			Log.error( FatalExceptionHandler.class, Constants.LOG_SPACER );
			Log.error( FatalExceptionHandler.class, BioLockJ.getHelpInfo() );
			Log.error( FatalExceptionHandler.class, Constants.LOG_SPACER );
		}

	}

	private static void printLogsOnScreen( final List<String> lines ) {
		for( final String line: lines ) {
			System.out.println( line );
		}
	}

	private static void setFailedStatus() {
		try {
			BioLockJUtil.createFile( Config.pipelinePath() + File.separator + Constants.BLJ_FAILED );
		} catch( final Exception ex ) {
			Log.error( FatalExceptionHandler.class,
				"Pipeline root directory not found, cannot save Pipeline Status File: " + Constants.BLJ_FAILED );
		}
	}

	private static File errorLog = null;
	private static final String FATAL_ERROR_FILE_PREFIX = "BioLockJ_FATAL_ERROR_";
}
