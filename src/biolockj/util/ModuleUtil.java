package biolockj.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.parser.ParserModule;

/**
 *
 */
public class ModuleUtil
{
	public static BioModule getFailedModule() throws Exception
	{
		for( final BioModule m: Config.getModules() )
		{
			if( isIncomplete( m ) && hasExecuted( m ) )
			{
				Log.out.warn( "Found failed module: " + m.getClass().getName() );
				return m;
			}
		}
		return null;
	}

	public static String getFailureDirSummary( final BioModule m )
	{
		try
		{
			if( ( getSubDir( m, BioModule.FAILURE_DIR ) == null )
					|| ( getSubDir( m, BioModule.FAILURE_DIR ).listFiles().length == 0 ) )
			{
				return "";
			}

			final StringBuffer sb = new StringBuffer();

			int x = 0;
			for( final File f: getSubDir( m, BioModule.FAILURE_DIR ).listFiles() )
			{
				sb.append( "Script Failure[" + ( x++ ) + "]" + f.getName() + BioLockJ.RETURN );
			}

			return sb.toString();
		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to produce module failureDir summary for: " + m.getClass().getName() + " : "
					+ ex.getMessage(), ex );
		}

		return "";
	}

	/**
	 * Get a BufferedReader for standard text file or gzipped file.
	 * @param File
	 * @return BufferedReader
	 * @throws Exception
	 */
	public static BufferedReader getFileReader( final File file ) throws Exception
	{
		return file.getName().toLowerCase().endsWith( ".gz" )
				? new BufferedReader( new InputStreamReader( new GZIPInputStream( new FileInputStream( file ) ) ) )
				: new BufferedReader( new FileReader( file ) );
	}

	public static File getMainScript( final BioModule m ) throws Exception
	{
		for( final File f: m.getScriptDir().listFiles() )
		{
			if( f.getName().startsWith( BioModule.MAIN_SCRIPT_PREFIX )
					&& !f.getName().endsWith( BioLockJ.SCRIPT_FAILED )
					&& !f.getName().endsWith( BioLockJ.SCRIPT_SUCCESS ) )
			{
				return f;
			}
		}
		return null;
	}

	public static BioModule getModule( final String name ) throws Exception
	{
		for( final BioModule m: Config.getModules() )
		{
			if( m.getClass().getName().equals( name ) )
			{
				return m;
			}
		}

		return null;
	}

	public static String getOutputDirSummary( final BioModule m )
	{
		try
		{
			if( ( getSubDir( m, BioModule.OUTPUT_DIR ) == null )
					|| ( getSubDir( m, BioModule.OUTPUT_DIR ).listFiles().length == 0 ) )
			{
				return "";
			}

			final StringBuffer sb = new StringBuffer();
			File newMeta = null;
			int count = m.getOutputDir().listFiles().length;

			BigInteger outAvg = FileUtils.sizeOfAsBigInteger( m.getOutputDir() );

			if( MetaUtil.exists() )
			{
				newMeta = new File(
						m.getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getFile().getName() );

				if( newMeta.exists() )
				{
					count--;
					outAvg = outAvg.subtract( FileUtils.sizeOfAsBigInteger( newMeta ) );
				}
				else
				{
					newMeta = null;
				}

			}
			outAvg = outAvg.divide( BigInteger.valueOf( count ) );

			sb.append( "Generated " + count + " output files: " + BioLockJ.RETURN );
			sb.append( "Mean output file size: " + FileUtils.byteCountToDisplaySize( outAvg ) + BioLockJ.RETURN );
			if( newMeta != null )
			{
				sb.append( "Generated new metadata: " + newMeta.getAbsolutePath() );
			}

			return sb.toString();
		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to produce module outputDir summary for: " + m.getClass().getName() + " : "
					+ ex.getMessage(), ex );
		}

		return "";
	}

	public static File getOutputMetadata( final BioModule m ) throws Exception
	{
		if( isComplete( m ) && ( getSubDir( m, BioModule.OUTPUT_DIR ) != null ) )
		{
			final File metadata = new File( getSubDir( m, BioModule.OUTPUT_DIR ).getAbsolutePath() + File.separator
					+ MetaUtil.getMetadataFileName() );

			if( metadata.exists() )
			{
				return metadata;
			}
		}

		return null;
	}

	public static String getRunTime( final BioModule m )
	{
		final String runTime = moduleRunTime.get( m.getClass().getName() );
		if( ( runTime == null ) && hasExecuted( m ) )
		{
			return getRunTime( System.currentTimeMillis() - startTime );

		}
		else if( runTime == null )
		{
			return "N/A";
		}
		return runTime;
	}

	/**
	 * Get runtime message based on startTime passed.
	 * @return
	 */
	//public static String getRunTime( final long start, final long end )
	public static String getRunTime( final long duration )
	{
		final String format = String.format( "%%0%dd", 2 );
		long elapsedTime = ( duration ) / 1000;
		if( elapsedTime < 0 )
		{
			elapsedTime = 0;
		}
		final String hours = String.format( format, elapsedTime / 3600 );
		final String minutes = String.format( format, ( elapsedTime % 3600 ) / 60 );
		String seconds = String.format( format, elapsedTime % 60 );
		if( hours.equals( "00" ) && minutes.equals( "00" ) && seconds.equals( "00" ) )
		{
			seconds = "01";
		}

		return hours + " hours : " + minutes + " minutes : " + seconds + " seconds";
	}

	public static String getScriptDirSummary( final BioModule m )
	{
		try
		{
			if( ( getSubDir( m, BioModule.SCRIPT_DIR ) == null ) || ( getMainScript( m ) == null )
					|| ( getSubDir( m, BioModule.SCRIPT_DIR ).listFiles().length == 0 ) )
			{
				return "";
			}

			final IOFileFilter ff0 = new WildcardFileFilter( "*.sh" );
			final IOFileFilter ffStarted = new WildcardFileFilter( "*.sh_" + BioLockJ.SCRIPT_STARTED );
			final IOFileFilter ffSuccess = new WildcardFileFilter( "*.sh_" + BioLockJ.SCRIPT_SUCCESS );
			final IOFileFilter ffFailed = new WildcardFileFilter( "*.sh_" + BioLockJ.SCRIPT_FAILED );

			final Collection<File> scripts = FileUtils.listFiles( m.getScriptDir(), ff0, null );
			final Collection<File> scriptsStarted = FileUtils.listFiles( m.getScriptDir(), ffStarted, null );
			final Collection<File> scriptsFailed = FileUtils.listFiles( m.getScriptDir(), ffFailed, null );
			final Collection<File> scriptsSuccess = FileUtils.listFiles( m.getScriptDir(), ffSuccess, null );

			final File mainSuccess = new File( getMainScript( m ).getAbsolutePath() + "_" + BioLockJ.SCRIPT_SUCCESS );

			final File mainFail = new File( getMainScript( m ).getAbsolutePath() + "_" + BioLockJ.SCRIPT_FAILED );

			scripts.remove( getMainScript( m ) );
			scriptsFailed.remove( mainFail );
			scriptsSuccess.remove( mainSuccess );
			File longestScript = null;
			Long avgRunTime = 0L;
			Long maxDuration = 0L;
			Long numCompleted = 0L;
			for( final File script: scripts )
			{
				final File started = new File( script.getAbsolutePath() + "_" + BioLockJ.SCRIPT_STARTED );
				File finish = new File( script.getAbsolutePath() + "_" + BioLockJ.SCRIPT_SUCCESS );
				if( !finish.exists() )
				{
					finish = new File( script.getAbsolutePath() + "_" + BioLockJ.SCRIPT_FAILED );
				}
				if( finish.exists() )
				{
					numCompleted++;
					final long duration = finish.lastModified() - started.lastModified();
					avgRunTime += duration;
					if( duration > maxDuration )
					{
						longestScript = script;
						maxDuration = duration;
					}
				}
			}

			if( numCompleted > 0 )
			{
				avgRunTime = avgRunTime / numCompleted;
			}
			else
			{
				avgRunTime = null;
			}

			final StringBuffer sb = new StringBuffer();
			sb.append( "Main Script: " + getMainScript( m ).getAbsolutePath() + BioLockJ.RETURN );
			sb.append( "Executed " + scriptsStarted.size() + "/" + scripts.size() );
			sb.append( " sub-scripts [" + scriptsSuccess.size() + " succeessful" );
			sb.append( ( ( scriptsFailed.size() > 0 ) ? ( "; " + scriptsFailed.size() + " failed" ): "" ) + "] "
					+ BioLockJ.RETURN );

			if( avgRunTime != null )
			{
				sb.append( "Average runtime: " + getScriptRunTime( avgRunTime ) + BioLockJ.RETURN );
			}
			if( longestScript != null )
			{
				sb.append( "Longest running script [" + getScriptRunTime( maxDuration ) + "] --> "
						+ longestScript.getAbsolutePath() + BioLockJ.RETURN );
			}

			return sb.toString();
		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to produce module scriptDir summary for: " + m.getClass().getName() + " : "
					+ ex.getMessage(), ex );
		}

		return "";
	}

	public static File getSubDir( final BioModule m, final String name )
	{
		final File dir = new File( m.getModuleDir().getAbsolutePath() + File.separator + name );
		if( !dir.exists() )
		{
			return null;
		}
		return dir;
	}

	public static boolean hasExecuted( final BioModule m )
	{
		return executedModules.contains( m.getClass().getName() );
	}

	public static boolean isComplete( final BioModule m ) throws Exception
	{
		final File f = new File( m.getModuleDir().getAbsolutePath() + File.separator + BioModule.BLJ_COMPLETE );
		return f.exists();
	}

	public static boolean isIncomplete( final BioModule m ) throws Exception
	{
		final File f = new File( m.getModuleDir().getAbsolutePath() + File.separator + BioModule.BLJ_STARTED );
		return f.exists();
	}

	public static void markComplete( final BioModule m ) throws Exception
	{
		final File f = new File( m.getModuleDir().getAbsolutePath() + File.separator + BioModule.BLJ_COMPLETE );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.exists() )
		{
			throw new Exception( "Unable to create " + f.getAbsolutePath() );
		}
		final File startFile = new File( m.getModuleDir().getAbsolutePath() + File.separator + BioModule.BLJ_STARTED );
		startFile.delete();
		Log.out.info( Log.LOG_SPACER );
		Log.out.info( "FINISHED " + m.getClass().getName() );
		Log.out.info( Log.LOG_SPACER );
		moduleRunTime.put( m.getClass().getName(), getRunTime( System.currentTimeMillis() - startTime ) );
	}

	public static void markStarted( final BioModule m ) throws Exception
	{
		startTime = System.currentTimeMillis();
		executedModules.add( m.getClass().getName() );
		final File f = new File( m.getModuleDir().getAbsolutePath() + File.separator + BioModule.BLJ_STARTED );
		final FileWriter writer = new FileWriter( f );
		writer.close();
		if( !f.exists() )
		{
			throw new Exception( "Unable to create " + f.getAbsolutePath() );
		}
		Log.out.info( Log.LOG_SPACER );
		Log.out.info( "STARTING " + m.getClass().getName() );
		Log.out.info( Log.LOG_SPACER );
	}

	public static boolean moduleExists( final String className ) throws Exception
	{
		return getModule( className ) != null;
	}

	public static ClassifierModule requireClassifierModule() throws Exception
	{
		for( final BioModule m: Config.getModules() )
		{
			if( m instanceof ClassifierModule )
			{
				return (ClassifierModule) m;
			}
		}

		throw new Exception( "Unable to find Classifier Module" );
	}

	public static BioModule requireModule( final String name ) throws Exception
	{
		final BioModule bioModule = getModule( name );
		if( bioModule == null )
		{
			throw new Exception( "Unable to find module: " + name );
		}

		return bioModule;
	}

	public static ParserModule requireParserModule() throws Exception
	{
		for( final BioModule m: Config.getModules() )
		{
			if( m instanceof ParserModule )
			{
				return (ParserModule) m;
			}
		}

		throw new Exception( "Unable to find required ParserModule" );
	}

	public static BioModule requirePreviousModule( final String name ) throws Exception
	{
		BioModule previousModule = null;
		for( final BioModule m: Config.getModules() )
		{
			if( ( previousModule != null ) && m.getClass().getName().equals( name ) )
			{
				return previousModule;
			}
			previousModule = m;
		}

		throw new Exception( "Unable to find previous module for: " + name );
	}

	public static File requireSubDir( final BioModule m, final String name )
	{
		final File dir = new File( m.getModuleDir().getAbsolutePath() + File.separator + name );
		if( !dir.exists() )
		{
			dir.mkdirs();
			Log.out.info( "Create directory: " + dir.getAbsolutePath() );
		}
		return dir;
	}

	/**
	 * Utility method to remove quotes from String.
	 * @param inString
	 * @return

	public static String stripQuotes( String inString )
	{
		final StringBuffer buff = new StringBuffer();
		for( int x = 0; x < inString.length(); x++ )
		{
			final char c = inString.charAt( x );
			if( c != '\"' )
			{
				buff.append( c );
			}
		}
	
		return buff.toString().trim();
	} */

	public static boolean subDirExists( final BioModule m, final String name )
	{
		final File dir = new File( m.getModuleDir().getAbsolutePath() + File.separator + name );
		return dir.exists();
	}

	private static String getScriptRunTime( final Long duration )
	{
		if( duration == null )
		{
			return "N/A";
		}

		final String format = String.format( "%%0%dd", 2 );
		long elapsedTime = ( duration ) / 1000;
		if( elapsedTime < 0 )
		{
			elapsedTime = 0;
		}
		final String hours = String.format( format, elapsedTime / 3600 );
		final String minutes = String.format( format, ( elapsedTime % 3600 ) / 60 );

		if( hours.equals( "00" ) )
		{

			return minutes + " minutes";
		}

		return hours + " hours : " + minutes + " minutes";
	}

	private static Set<String> executedModules = new HashSet<>();
	private static Map<String, String> moduleRunTime = new HashMap<>();
	private static long startTime = 0L;
}
