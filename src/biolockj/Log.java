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
package biolockj;

import java.io.File;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import biolockj.util.*;

/**
 * Simple Logging Facade for Java (SLF4J) implementation using Log4J<br>
 * Log4J Configuration:
 * <a href="https://github.com/msioda/BioLockJ/blob/master/resources/log4j.properties?raw=true" target=
 * "_top">log4j.properties</a><br>
 * Every Java class that prints to the Java log file, will do so by calling:
 * <ul>
 * <li>{@link biolockj.Log#out(Class)}.{@link #out(Class)}.debug( logMessage )
 * <li>{@link biolockj.Log#out(Class)}.{@link #out(Class)}.info( logMessage )
 * <li>{@link biolockj.Log#out(Class)}.{@link #out(Class)}.warn( logMessage )
 * <li>{@link biolockj.Log#out(Class)}.{@link #out(Class)}.error( logMessage, exception )
 * </ul>
 */
public class Log
{

	private Log()
	{}

	/**
	 * Print log level DEBUG message.<br>
	 * Do not print {@link biolockj.util.MetaUtil} debug since these will always print since the Logger isn't
	 * initialized yet and logMesseges will print them as INFO after Log file initializes.
	 * 
	 * @param loggingClass Logging class
	 * @param msg Message to log
	 */
	public static void debug( final Class<?> loggingClass, final String msg )
	{
		if( suppressLogs )
		{
			return;
		}
		if( isInitialized() && !debugClasses().isEmpty() && !debugClasses().contains( loggingClass.getName() ) )
		{
			if( !gaveDebugWarning )
			{
				gaveDebugWarning = true;
				warn( loggingClass, "DEBUG DISABLED!  Because the [ " + LIMIT_DEBUG_CLASSES
						+ " ] property is enabled, \"Debug\" log output is only written for clasess defined in Config property: "
						+ LIMIT_DEBUG_CLASSES + " ---> " + debugClasses() );
			}
			return;
		}

		if( logFile == null )
		{
			if( !MetaUtil.class.equals( loggingClass ) )
			{
				logMesseges.add( msg );
			}
		}
		else
		{
			out( loggingClass ).debug( msg );
		}
	}

	/**
	 * Return TRUE if {@value #LOG_LEVEL_PROPERTY} = DEBUG.
	 * 
	 * @return TRUE if DEBUG
	 * @throws Exception if errors occur
	 */
	public static boolean doDebug() throws Exception
	{
		return Config.requireString( null, LOG_LEVEL_PROPERTY ).toUpperCase().equals( "DEBUG" );
	}

	/**
	 * Boolean toggle to enable/disable all log messages
	 * 
	 * @param enable
	 */
	public static void enableLogs( final boolean enable )
	{
		suppressLogs = !enable;
	}

	/**
	 * Print log level ERROR message without exception stacktrace
	 * 
	 * @param myClass Logging class
	 * @param msg Message to log
	 */
	public static void error( final Class<?> myClass, final String msg )
	{
		if( suppressLogs )
		{
			return;
		}
		error( myClass, msg, null );
	}

	/**
	 * Print log level ERROR message.
	 * 
	 * @param myClass Logging class
	 * @param msg Message to log
	 * @param exception Cause of the error
	 */
	public static void error( final Class<?> myClass, final String msg, final Exception exception )
	{
		if( suppressLogs )
		{
			return;
		}
		if( logFile == null )
		{
			logMesseges.add( msg );
		}
		else if( exception != null )
		{
			out( myClass ).error( msg, exception );
		}
		else
		{
			out( myClass ).error( msg );
		}
	}

	/**
	 * Returns the log file.
	 *
	 * @return File logFile
	 */
	public static File getFile()
	{
		return logFile;
	}

	/**
	 * Get the logMesseges created prior to creating the actual {@link #LOG_FILE}
	 * 
	 * @return logMesseges
	 */
	public static List<String> getMsgs()
	{
		return logMesseges;
	}

	/**
	 * Print log level INFO message.
	 * 
	 * @param myClass Logging class
	 * @param msg Message to log
	 */
	public static void info( final Class<?> myClass, final String msg )
	{
		if( suppressLogs )
		{
			return;
		}
		if( logFile == null )
		{
			logMesseges.add( msg );
		}
		else
		{
			out( myClass ).info( msg );
		}
	}

	/**
	 * Called by {@link biolockj.BioLockJ#main(String[]) BioLockJ.main()} after {@link biolockj.Config} is initialized.
	 * Create Java Log4J log file in {@link biolockj.Config}.{@value biolockj.Config#PROJECT_PIPELINE_DIR}, named after
	 * the project.
	 * <ul>
	 * <li>Set and store {@link #LOG_FILE} and {@link #LOG_APPEND} in {@link System} properties to be used by
	 * <a href="https://github.com/msioda/BioLockJ/blob/master/resources/log4j.properties?raw=true" target=
	 * "_top">log4j.properties</a>
	 * <li>Print cached messages generated prior to Log file initialization to log file as INFO
	 * <li>Print {@link biolockj.Config} property values to log file as INFO
	 * </ul>
	 *
	 * @param name Name of the log file
	 * @throws Exception if unable to create the log file or print {@link biolockj.Config} properties
	 */
	public static void initialize( final String name ) throws Exception
	{
		logFile = new File( Config.pipelinePath() + File.separator + name + Constants.LOG_EXT );
		System.setProperty( LOG_FILE, logFile.getAbsolutePath() );
		System.setProperty( LOG_LEVEL_PROPERTY, validateLogLevel() );
		System.setProperty( LOG_APPEND, String.valueOf( logFile.exists() ) );
		System.setProperty( LOG_FORMAT,
				RuntimeParamUtil.isDirectMode() && !Config.isOnCluster() ? DIRECT_FORMAT: DEFAULT_FORMAT );

		if( !RuntimeParamUtil.isDirectMode() )
		{
			logWelcomeMsg();
			for( final String m: Log.logMesseges )
			{
				Log.info( Log.class, m );
			}

			Log.info( Log.class, "Set " + LOG_FILE + " = " + logFile.getAbsolutePath() );
			Log.info( Log.class, "Set " + LOG_LEVEL_PROPERTY + " = " + validateLogLevel() );
			Log.info( Log.class, "Set " + LOG_APPEND + " = " + String.valueOf( logFile.exists() ) );
			Log.info( Log.class, "Set " + LOG_FORMAT + " = " + DIRECT_FORMAT );
			logConfig();
			Log.info( Log.class, LOG_SPACER );
			Log.info( Log.class, "Java Logger initialized" );
			Log.info( Log.class, LOG_SPACER );
		}
	}

	/**
	 * Returns the Logger for the callingClass. Each class has its own Logger since the callingClass name can be
	 * appended to the log file without impacting performance.
	 *
	 * @param callingClass Java class in need of Logger
	 * @return Logger for callingClass
	 */
	public static Logger out( final Class<?> callingClass )
	{
		if( loggers.get( callingClass.getName() ) == null )
		{
			loggers.put( callingClass.getName(), LoggerFactory.getLogger( callingClass ) );
		}

		return loggers.get( callingClass.getName() );
	}

	/**
	 * Print log level WARN message.
	 * 
	 * @param myClass Logging class
	 * @param msg Message to log
	 */
	public static void warn( final Class<?> myClass, final String msg )
	{
		if( suppressLogs )
		{
			return;
		}
		if( logFile == null )
		{
			logMesseges.add( msg );
		}
		else
		{
			out( myClass ).warn( msg );
		}
	}

	/**
	 * Prints {@link biolockj.Config} properties to the Java log file.
	 */
	protected static void logConfig()
	{
		Log.info( Log.class, Log.LOG_SPACER );
		Log.info( Log.class, "Pipeline Project Config File: " + Config.getConfigFilePath() );
		Log.info( Log.class, "Pipeline Default Config Files: " + Config.getList( null, Constants.INTERNAL_DEFAULT_CONFIG ) );
		Log.info( Log.class, Log.LOG_SPACER );
		Log.info( Log.class, "===> List All Configured Properties:" );
		Log.info( Log.class, Log.LOG_SPACER );
		final Map<String, String> map = Config.getProperties();
		final Iterator<String> it = map.keySet().iterator();
		while( it.hasNext() )
		{
			final String key = it.next();
			Log.info( Log.class, key + " = " + map.get( key ) );
		}
	}

	/**
	 * Print welcome message.<br>
	 * Message includes BioLockJ version {@link biolockj.util.BioLockJUtil#getVersion()}, lab citation, and freeware
	 * blurb.
	 * 
	 * @throws Exception if errors occur
	 */
	protected static void logWelcomeMsg() throws Exception
	{
		Log.info( Log.class, LOG_SPACER );
		Log.info( Log.class,
				"Launching BioLockJ " + BioLockJUtil.getVersion() + " ~ Distributed by UNCC Fodor Lab @2019" );
		Log.info( Log.class, LOG_SPACER );
		Log.info( Log.class, "This code is free software; you can redistribute and/or modify it" );
		Log.info( Log.class, "under the terms of the GNU General Public License as published by" );
		Log.info( Log.class, "the Free Software Foundation; either version 2 of the License, or" );
		Log.info( Log.class, "any later version, provided proper credit is given to the authors." );
		Log.info( Log.class, "This program is distributed in the hope that it will be useful," );
		Log.info( Log.class, "but WITHOUT ANY WARRANTY; without even the implied warranty of" );
		Log.info( Log.class, "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the" );
		Log.info( Log.class, "GNU General Public License for more details at http://www.gnu.org" );
		Log.info( Log.class, LOG_SPACER );
	}

	/**
	 * Validate log level is configured to one of the valid Log4J options: DEBUG, INFO, WARN, ERROR
	 *
	 * @return valid logLevel (DEBUG, INFO, WARN, ERROR)
	 * @throws Exception if {@link biolockj.Config}.{@value #LOG_LEVEL_PROPERTY} parameter is missing or invalid
	 */
	protected static String validateLogLevel() throws Exception
	{
		final String logLevel = Config.requireString( null, LOG_LEVEL_PROPERTY ).toUpperCase();
		if( !logLevel.equals( "DEBUG" ) && !logLevel.equals( "INFO" ) && !logLevel.equals( "WARN" )
				&& !logLevel.equals( "ERROR" ) )
		{
			throw new Exception( "Config property: " + LOG_LEVEL_PROPERTY
					+ "missing or invlid.  Please configure a valid option: " + "[DEBUG/INFO/WARN/ERROR]" );
		}
		return logLevel;
	}

	/**
	 * Some classes should always print DEBUG. This limitation is designed to avoid unwanted BioModule DEBUG in your
	 * pipeline.
	 * 
	 * @return Set of debug classes
	 */
	private static Set<String> debugClasses()
	{
		if( debugClasses == null )
		{
			debugClasses = Config.getSet( null, LIMIT_DEBUG_CLASSES );
			if( !debugClasses.isEmpty() )
			{
				debugClasses.add( BioLockJ.class.getName() );
				debugClasses.add( BioModuleFactory.class.getName() );
				debugClasses.add( Config.class.getName() );
				debugClasses.add( Log.class.getName() );
				debugClasses.add( Pipeline.class.getName() );
				debugClasses.add( Properties.class.getName() );
				debugClasses.add( DockerUtil.class.getName() );
			}
		}
		return debugClasses;
	}

	private static boolean isInitialized()
	{
		return logFile != null && logFile.exists();
	}

	/**
	 * {@link biolockj.Config} property used to limit classes that log debug statements when
	 * {@value #LOG_LEVEL_PROPERTY}={@value biolockj.Constants#TRUE}
	 */
	public static final String LIMIT_DEBUG_CLASSES = "project.limitDebugClasses";

	/**
	 * {@link biolockj.Config} property used to set log sensitivity in
	 * <a href= "https://github.com/msioda/BioLockJ/blob/master/resources/log4j.properties?raw=true" target=
	 * "_top">log4j.properties</a><br>
	 * <i>log4j.rootLogger=${project.logLevel}, file, stdout</i>
	 * <ol>
	 * <li>DEBUG - Log all messages
	 * <li>INFO - Log info, warning and error messages
	 * <li>WARN - Log warning and error messages
	 * <li>ERROR - Log error messages only
	 * </ol>
	 */
	public static final String LOG_LEVEL_PROPERTY = "project.logLevel";

	/**
	 * Spacer used to improve log file readability
	 */
	public static final String LOG_SPACER = "========================================================================";

	/**
	 * Standard BioLockJ log format includes date, time, severity type, and calling class before the msg. Example:
	 * 2018-05-27 19:53:04 INFO MetaUtil:
	 */
	protected static final String DEFAULT_FORMAT = "%d{yyyy-MM-dd HH:mm:ss} %p %c{1}: %m%n";

	/**
	 * Direct BioLockJ log prefix includes only the msg. Example: 2018-05-27 19:53:04 INFO MetaUtil:
	 */
	protected static final String DIRECT_FORMAT = "%m%n";

	/**
	 * Set in {@link #initialize(String)} to true only if executing pipeline restart.<br>
	 * Used by <a href= "https://github.com/msioda/BioLockJ/blob/master/resources/log4j.properties?raw=true" target=
	 * "_top">log4j.properties</a><br>
	 * <i>log4j.appender.file.Append=${LOG_APPEND}</i>
	 */
	protected static final String LOG_APPEND = "LOG_APPEND";

	/**
	 * Set in {@link #initialize(String)} to file path of pipeline Java log file<br>
	 * Used by <a href= "https://github.com/msioda/BioLockJ/blob/master/resources/log4j.properties?raw=true" target=
	 * "_top">log4j.properties</a><br>
	 * <i>log4j.appender.file.File=${LOG_FILE}</i>
	 */
	protected static final String LOG_FILE = "LOG_FILE";

	/**
	 * Set in {@link #initialize(String)} to proper layout based on pipeline is direct or note<br>
	 * Used by <a href= "https://github.com/msioda/BioLockJ/blob/master/resources/log4j.properties?raw=true" target=
	 * "_top">log4j.properties</a><br>
	 * <i>log4j.appender.file.layout.ConversionPattern=${LOG_FORMAT}</i>
	 */
	protected static final String LOG_FORMAT = "LOG_FORMAT";
	private static Set<String> debugClasses = null;
	private static boolean gaveDebugWarning = false;
	private static File logFile = null;
	private static Map<String, Logger> loggers = new HashMap<>();
	private static final List<String> logMesseges = new ArrayList<>();
	private static boolean suppressLogs = false;
}
