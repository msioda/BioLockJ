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
 * 				GNU General Public License for more details at http://www.gnu.org
 */
package biolockj;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.module.BioModule;
import biolockj.module.Email;
import biolockj.module.Metadata;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.SeqUtil;

/**
 * This is the main program used to control top level execution.
 */
public class BioLockJ
{

	public static boolean isRestarted()
	{
		return isRestarted;
	}

	/**
	 * The main method is the first method called when BioLockJ is run. Here we
	 * read property file, copy it to project directory, initialize Config
	 * and call runProgram().
	 *
	 * If the password param is given, the password is encrypted & stored to the
	 * prop file.
	 *
	 * @param args - args[0] path to property file - args[1] clear-text admin
	 *        email password
	 */
	public static void main( String[] args )
	{
		try
		{
			args = validateJavaParameters( args );

			System.out.println( "args[CONFIG_PARAM]: " + args[ CONFIG_PARAM ] );
			System.out.println( "args[OPTIONAL_PARAM]: " + args[ OPTIONAL_PARAM ] );

			if( changePassword( args[ OPTIONAL_PARAM ] ) )
			{
				System.out.println( "Encrypting and storing new admin email password!" );
				Email.encryptAndStoreEmailPassword( args[ CONFIG_PARAM ], args[ OPTIONAL_PARAM ] );
				System.exit( 0 );
			}

			Config.loadProperties( args[ CONFIG_PARAM ] );
			setProjectDir( args[ OPTIONAL_PARAM ] );

			if( !doRestart( args[ OPTIONAL_PARAM ] ) )
			{
				logWelcomeMsg();
				Config.copyConfig();
			}

			Log.initialize();
			initializeModules();
			runProgram();
		}
		catch( final Exception ex )
		{
			if( Log.out != null )
			{
				Log.out.error( "Error occurred running program! ", ex );
			}
			else
			{
				System.out.println( "FATAL APPLICATION ERROR - Log file = null: args: " + args );
				ex.printStackTrace();
			}
		}
	}

	protected static void initializeModules() throws Exception
	{
		Log.out.info( "Initializing BioLockJ Modules..." );
		File metadata = Config.getExistingFile( Config.INPUT_METADATA );
		Log.out.debug( "===> Initial metadata: " + metadata );

		final Set<String> ignoredFiles = Config.getSet( Config.INPUT_IGNORE_FILES );
		ignoredFiles.add( BioModule.BLJ_STARTED );
		ignoredFiles.add( BioModule.BLJ_COMPLETE );

		Config.setProperty( Config.INPUT_IGNORE_FILES, ignoredFiles );

		BioModule previousModule = null;
		for( final BioModule bioModule: Config.getModules() )
		{
			final String moduleName = Config.requireExistingDir( Config.PROJECT_DIR ).getAbsolutePath() + File.separator
					+ Config.getModules().indexOf( bioModule ) + "_" + bioModule.getClass().getSimpleName();

			bioModule.setModuleDir( moduleName );

			if( ModuleUtil.isIncomplete( bioModule ) )
			{
				Log.out.info( "Reset incomplete BioLockJ Module: " + bioModule.getClass().getName() );
				FileUtils.forceDelete( bioModule.getModuleDir() );
				bioModule.setModuleDir( moduleName );
			}

			if( Config.getModules().indexOf( bioModule ) == 0 )
			{
				bioModule.initInputFiles( TrueFileFilter.INSTANCE, null );
			}
			else
			{
				if( previousModule instanceof Metadata )
				{
					bioModule.initInputFiles( TrueFileFilter.INSTANCE, null );
				}
				else
				{
					bioModule.setInputDir( previousModule.getOutputDir() );
				}
			}

			if( ModuleUtil.getOutputMetadata( bioModule ) != null )
			{
				metadata = ModuleUtil.getOutputMetadata( bioModule );
			}

			if( !ModuleUtil.isComplete( bioModule ) )
			{
				bioModule.checkDependencies();
			}

			previousModule = bioModule;
		}

		if( metadata != null )
		{
			MetaUtil.setFile( metadata );
			MetaUtil.refresh();
		}
	}

	/**
	 * Output welcome message to the output file with BioLockJ version, lab citation,
	 * and freeware msg.
	 */
	protected static void logWelcomeMsg()
	{
		Log.addMsg( RETURN );
		Log.addMsg( Log.LOG_SPACER );
		Log.addMsg( "Launching BioLockJ " + BLJ_VERSION + " ~ Distributed by UNCC Fodor Lab @2017" );
		Log.addMsg( Log.LOG_SPACER );
		Log.addMsg( "This code is free software; you can redistribute and/or modify it" );
		Log.addMsg( "under the terms of the GNU General Public License as published by" );
		Log.addMsg( "the Free Software Foundation; either version 2 of the License, or" );
		Log.addMsg( "any later version, provided proper credit is given to the authors." );
		Log.addMsg( "This program is distributed in the hope that it will be useful," );
		Log.addMsg( "but WITHOUT ANY WARRANTY; without even the implied warranty of" );
		Log.addMsg( "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the" );
		Log.addMsg( "GNU General Public License for more details at http://www.gnu.org" );
		Log.addMsg( Log.LOG_SPACER );
	}

	/**
	 * Called by main(args[]) to check all of the executor dependencies, execute scripts,
	 * and then clean up by deleting temp dirs if needed.
	 * @throws Exception
	 */
	protected static void runProgram() throws Exception
	{
		try
		{
			if( Config.getBoolean( PROJECT_COPY_FILES ) )
			{
				SeqUtil.copyInputData();
			}

			for( final BioModule bioModule: Config.getModules() )
			{
				if( !ModuleUtil.isComplete( bioModule ) )
				{
					ModuleUtil.markStarted( bioModule );
					executeAndWaitForScriptsIfAny( bioModule );
					ModuleUtil.markComplete( bioModule );
				}
				else
				{
					Log.out.info( "Skip completed BioLockJ Module: " + bioModule.getClass().getName() );
				}
			}

			if( Config.getBoolean( PROJECT_DELETE_TEMP_FILES ) )
			{
				for( final BioModule bioModule: Config.getModules() )
				{
					if( ModuleUtil.subDirExists( bioModule, BioModule.TEMP_DIR ) )
					{
						Log.out.info( "Delete temp dir for BioLockJ Module: " + bioModule.getClass().getName() );
						FileUtils.forceDelete( ModuleUtil.requireSubDir( bioModule, BioModule.TEMP_DIR ) );
					}

				}
			}

			markProjectComplete();
		}
		catch( final Exception ex )
		{
			if( Log.out != null )
			{
				Log.out.error( "Error occurred during module execution! ", ex );
				for( final BioModule bioModule: Config.getModules() )
				{
					if( bioModule instanceof Email )
					{
						final Email emailMod = (Email) bioModule;
						emailMod.setError( ex );
						executeAndWaitForScriptsIfAny( bioModule );
					}
				}
			}
			else
			{
				System.out.println( "Error occurred during module execution!" );
				ex.printStackTrace();
			}
		}
	}

	protected static String[] validateJavaParameters( final String[] args ) throws Exception
	{
		final String[] params = new String[ 2 ];
		if( ( args == null ) || ( args.length < 1 ) || ( args.length > 2 ) )
		{
			throw new Exception( "BioLockJ accepts only 1-2 Java application parameters" + RETURN
					+ "Required Arg = path to config file" + RETURN
					+ "Optional Arg = [ new_email_password ] or [ restart_flag (\"restart\" or \"r\") ]" + RETURN
					+ RETURN + "PROGRAM TERMINATED!" );
		}

		File configFile = new File( args[ 0 ] );
		String optionalParam = null;
		if( ( args.length == 1 ) && ( !configFile.exists() || configFile.isDirectory() ) )
		{
			throw new Exception( configFile.getAbsolutePath() + " is not a valid file!" );
		}

		if( args.length == 2 )
		{
			optionalParam = args[ 1 ].toLowerCase();
			if( !configFile.exists() || configFile.isDirectory() )
			{
				optionalParam = args[ 0 ].toLowerCase();
				configFile = new File( args[ 1 ] );
				params[ 0 ] = configFile.getAbsolutePath();
				if( !configFile.exists() || configFile.isDirectory() )
				{
					throw new Exception( "Neither parameter is a valid file path [ " + args[ 0 ] + " / " + args[ 1 ]
							+ " ] does not exist!" );
				}
			}

			if( restartFlags.contains( optionalParam ) )
			{
				params[ 1 ] = Config.TRUE;
			}
			else
			{
				params[ 1 ] = optionalParam;
			}
		}

		params[ 0 ] = configFile.getAbsolutePath();
		params[ 1 ] = optionalParam;

		return params;
	}

	private static boolean changePassword( final String val )
	{
		return ( val != null ) && !restartFlags.contains( val );
	}

	private static boolean doRestart( final String val )
	{
		return ( val != null ) && restartFlags.contains( val );
	}

	/**
	 * Execute the Module scripts (if any).
	 *
	 * @param BioModule
	 * @throws Exception
	 */
	private static void executeAndWaitForScriptsIfAny( final BioModule m ) throws Exception
	{
		m.executeProjectFile();
		if( ModuleUtil.subDirExists( m, BioModule.SCRIPT_DIR ) )
		{
			Job.submit( m.getScriptDir(), m.getJobParams() );
			pollAndSpin( m );
		}
	}

	private static File getRestartDir() throws Exception
	{
		File restartDir = null;
		GregorianCalendar mostRecent = null;
		final FileFilter ff = new WildcardFileFilter( Config.requireString( Config.PROJECT_NAME ) + "*" );
		final File[] dirs = Config.requireExistingDir( PROJECTS_DIR ).listFiles( ff );

		for( final File d: dirs )
		{
			if( !d.isDirectory()
					|| ( d.getName().length() != ( Config.requireString( Config.PROJECT_NAME ).length() + 10 ) ) )
			{
				continue;
			}

			final String name = d.getName();
			final int len = name.length();
			final String year = name.substring( len - 9, len - 5 );
			final String mon = name.substring( len - 5, len - 2 );
			final String day = name.substring( len - 2 );
			final Date date = new SimpleDateFormat( "yyyyMMMdd" ).parse( year + mon + day );
			final GregorianCalendar projectDate = new GregorianCalendar();
			projectDate.setTime( date );

			// Value > 0 if projectDate has a more recent date than mostRecent
			if( ( mostRecent == null ) || ( projectDate.compareTo( mostRecent ) > 0 ) )
			{
				Log.addMsg( "Found previous run = " + d.getAbsolutePath() );
				restartDir = d;
				mostRecent = projectDate;
			}
		}

		if( restartDir == null )
		{
			throw new Exception(
					"Unalbe to locate restart directory in --> " + Config.requireExistingDir( PROJECTS_DIR ) );
		}

		if( isProjectComplete( restartDir ) )
		{
			throw new Exception( "RESTART FAILED!  Project ran successfully: " + restartDir.getAbsolutePath() );
		}

		isRestarted = true;

		Log.addMsg( RETURN );
		Log.addMsg( RETURN );
		Log.addMsg( Log.LOG_SPACER );
		Log.addMsg( Log.LOG_SPACER );
		Log.addMsg( RETURN );
		Log.addMsg( "RESTART PROJECT DIR --> " + restartDir.getAbsolutePath() );
		Log.addMsg( RETURN );
		Log.addMsg( Log.LOG_SPACER );
		Log.addMsg( Log.LOG_SPACER );
		Log.addMsg( RETURN );

		return restartDir;
	}

	private static boolean isProjectComplete( final File projDir ) throws Exception
	{
		final File f = new File( projDir.getAbsolutePath() + File.separator + BioModule.BLJ_COMPLETE );
		return f.exists();
	}

	private static void markProjectComplete() throws Exception
	{
		final File f = new File( Config.requireExistingDir( Config.PROJECT_DIR ).getAbsolutePath() + File.separator
				+ BioModule.BLJ_COMPLETE );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.exists() )
		{
			throw new Exception( "Unable to create " + f.getAbsolutePath() );
		}
	}

	/**
	 * Poll checks the Module's script dir for flag files indicating either
	 * SUCCESS or FAILURE.  Output message to output indicating num pass/fail.
	 * Exit if failures found and exitOnFailure flag set to Y.
	 *
	 * @param scriptFiles
	 * @param mainScript
	 * @return
	 * @throws Exception
	 */
	private static boolean poll( final File mainScript, final Collection<File> scriptFiles ) throws Exception
	{

		final File mainSuccess = new File( mainScript.getAbsolutePath() + "_" + SCRIPT_SUCCESS );
		final File mainFailed = new File( mainScript.getAbsolutePath() + "_" + SCRIPT_FAILED );

		// for bash scripts, remove main script from scriptFiles
		// for R, we will have only one script, so leave it in
		if( mainScript.getName().startsWith( BioModule.MAIN_SCRIPT_PREFIX ) )
		{
			scriptFiles.remove( mainScript );
			if( mainSuccess.exists() )
			{
				scriptFiles.remove( mainSuccess );
			}

			if( mainFailed.exists() )
			{
				scriptFiles.remove( mainFailed );
			}
		}

		File failure = null;
		int numSuccess = 0;
		int numStarted = 0;
		int numFailed = 0;
		for( final File f: scriptFiles )
		{
			final File testStarted = new File( f.getAbsolutePath() + "_" + SCRIPT_STARTED );
			if( testStarted.exists() )
			{
				numStarted++;
			}

			final File testSuccess = new File( f.getAbsolutePath() + "_" + SCRIPT_SUCCESS );
			if( testSuccess.exists() )
			{
				numSuccess++;
			}

			final File testFailure = new File( f.getAbsolutePath() + "_" + SCRIPT_FAILED );
			if( testFailure.exists() )
			{
				failure = testFailure;
				numFailed++;
			}

		}

		int numScripts = scriptFiles.size() - numSuccess - numFailed - numStarted;
		if( !mainScript.getName().endsWith( ".sh" ) ) // must be R script
		{
			numScripts = 1;
			numStarted = 1;
			if( mainSuccess.exists() )
			{
				numSuccess = 1;
			}
			if( mainFailed.exists() )
			{
				numFailed = 1;
			}
		}

		final int numRunning = numStarted - numSuccess - numFailed;
		final int numQueued = numScripts - numStarted;

		if( mainFailed.exists() )
		{
			failure = mainFailed;
		}

		final String logMsg = mainScript.getName() + " Status (Total=" + numScripts + "): Success=" + numSuccess
				+ "; Failed=" + numFailed + "; Running=" + numRunning + "; Queued=" + numQueued;

		if( !statusMsg.equals( logMsg ) )
		{
			statusMsg = logMsg;
			Log.out.info( logMsg );
		}
		else if( ( pollUpdateMeter++ % 10 ) == 0 )
		{
			Log.out.info( logMsg );
		}

		if( mainFailed.exists()
				|| ( Config.getBoolean( Config.SCRIPT_EXIT_ON_ERROR ) && ( failure != null ) && failure.exists() ) )
		{
			throw new Exception( "SCRIPT FAILED: " + failure.getAbsolutePath() );
		}

		return ( numSuccess + numFailed ) == numScripts;
	}

	/**
	 * This method calls poll to check status of scripts and then sleeps for pollTime seconds.
	 *
	 * @param mainScript
	 * @throws Exception
	 */
	private static void pollAndSpin( final BioModule m ) throws Exception
	{
		int numMinutes = 0;
		boolean finished = false;
		while( !finished )
		{
			finished = poll( ModuleUtil.getMainScript( m ),
					FileUtils.listFiles( m.getScriptDir(), TrueFileFilter.INSTANCE, null ) );
			if( !finished )
			{
				if( ( m.getTimeout() != null ) && ( m.getTimeout() > 0 ) && ( numMinutes++ >= m.getTimeout() ) )
				{
					throw new Exception( ModuleUtil.getMainScript( m ).getAbsolutePath() + " timed out after "
							+ numMinutes + " minutes." );
				}

				Thread.sleep( POLL_TIME * 1000 );
			}
		}
		pollUpdateMeter = 0;
	}

	/**
	 * This method creates the sub-dir under projects by attaching date-string to the project name
	 * unless restarting, in which case we send the most recent project with a matching name.
	 *
	 * @param String 2nd Java param - if restartFlag, set to existing project dir,
	 * otherwise, build new projectDir
	 * @throws Exception if projectDir already exists
	 */
	private static void setProjectDir( final String optionalParam ) throws Exception
	{
		if( doRestart( optionalParam ) )
		{
			Config.setAProperty( Config.PROJECT_DIR, getRestartDir().getAbsolutePath() );
		}
		else
		{
			final String year = String.valueOf( new GregorianCalendar().get( Calendar.YEAR ) );
			final String month = new GregorianCalendar().getDisplayName( Calendar.MONTH, Calendar.SHORT,
					Locale.ENGLISH );
			final String day = twoDigitVal( new GregorianCalendar().get( Calendar.DATE ) );
			final File projectDir = new File( Config.requireExistingDir( PROJECTS_DIR ).getAbsolutePath()
					+ File.separator + Config.requireString( Config.PROJECT_NAME ) + "_" + year + month + day );

			if( projectDir.exists() )
			{
				throw new Exception( "Project already exists with today's date: " + projectDir.getAbsolutePath()
						+ ".  Set restart flag to continue failed pipeline or provide a unique value for: "
						+ Config.PROJECT_NAME );
			}

			projectDir.mkdirs();
			Config.setAProperty( Config.PROJECT_DIR, projectDir.getAbsolutePath() );
		}
	}

	private static String twoDigitVal( final Integer input )
	{
		String val = input.toString();
		if( val.length() == 1 )
		{
			val = "0" + val;
		}
		return val;
	}

	public static final long APP_START_TIME = System.currentTimeMillis();
	public static final String BLJ_VERSION = "v.0.1_beta";
	public static final String INDENT = "   ";
	public static final String LOG_NORMAL = "_LogNormalCounts";
	public static final String NORMAL = "_NormalCounts";
	public static final String RETURN = "\n";
	public static final String SCRIPT_FAILED = "Failure";
	public static final String SCRIPT_STARTED = "Started";
	public static final String SCRIPT_SUCCESS = "Success";
	public static final String TAB_DELIM = "\t";
	private static final int CONFIG_PARAM = 0;
	private static boolean isRestarted = false;
	private static final int OPTIONAL_PARAM = 1;
	private static final int POLL_TIME = 60;
	private static int pollUpdateMeter = 0;
	private static final String PROJECT_COPY_FILES = "project.copyInput";
	private static final String PROJECT_DELETE_TEMP_FILES = "project.deleteTempFiles";
	private static final String PROJECTS_DIR = "project.rootDir";
	private static final List<String> restartFlags = new ArrayList<>();
	private static String statusMsg = "";

	static
	{
		restartFlags.add( "restart" );
		restartFlags.add( "r" );
	}
}