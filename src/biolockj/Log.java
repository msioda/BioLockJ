package biolockj;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log
{

	private Log()
	{
	}

	public static void addMsg( final String msg )
	{
		logMesseges.add( msg );
	}

	public static File getFile()
	{
		return logFile;
	}

	public static String getName()
	{
		return logFile.getName();
	}

	public static String getPath()
	{
		return logFile.getAbsolutePath();
	}

	public static void initialize() throws Exception
	{
		logFile = new File( Config.requireExistingDir( Config.PROJECT_DIR ).getAbsolutePath() + File.separator
				+ Config.requireString( Config.PROJECT_NAME ) + ".log" );

		System.setProperty( LOG_FILE, getPath() );
		System.setProperty( LOG_LEVEL, getLogLevel() );
		System.setProperty( LOG_APPEND, String.valueOf( logFile.exists() ) );

		addMsg( "Set " + LOG_FILE + " = " + getPath() );
		addMsg( "Set " + LOG_LEVEL + " = " + getLogLevel() );
		addMsg( "Set " + LOG_APPEND + " = " + String.valueOf( logFile.exists() ) );

		out = LoggerFactory.getLogger( Log.class );

		for( final String m: Log.logMesseges )
		{
			Log.out.info( m );
		}

		logConfig();

		Log.out.info( LOG_SPACER );
		Log.out.info( "BioLockJ initialized" );
		Log.out.info( LOG_SPACER );

	}

	private static String getLogLevel() throws Exception
	{
		final String logLevel = Config.requireString( LOG_LEVEL_PROPERTY );
		if( !logLevel.equals( "DEBUG" ) && !logLevel.equals( "INFO" ) && !logLevel.equals( "WARN" )
				&& !logLevel.equals( "ERROR" ) )
		{
			throw new Exception( "Config property: " + LOG_LEVEL_PROPERTY
					+ "missing or invlid.  Please set to one of these valid options: " + "[DEBUG/INFO/WARN/ERROR]" );
		}
		return logLevel;
	}

	/**
	 * Log config file settings in welcome message.
	 * @throws Exception
	 */
	private static void logConfig() throws Exception
	{
		Log.out.info( Log.LOG_SPACER );
		Log.out.info( "BioLockJ Configuration" );
		Log.out.info( Log.LOG_SPACER );
		final Map<String, String> map = Config.getProperties();
		final Iterator<String> it = map.keySet().iterator();
		while( it.hasNext() )
		{
			final String key = it.next();
			Log.out.info( key + " = " + map.get( key ) );
		}
		Log.out.info( Log.LOG_SPACER );
	}

	public static final String LOG_SPACER = "========================================================================";
	public static final List<String> logMesseges = new ArrayList<>();
	public static Logger out = null;
	private static final String LOG_APPEND = "LOG_APPEND";
	private static final String LOG_FILE = "LOG_FILE";
	private static final String LOG_LEVEL = "LOG_LEVEL";
	private static final String LOG_LEVEL_PROPERTY = "project.logLevel";
	private static File logFile = null;
}
