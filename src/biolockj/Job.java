/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
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
package biolockj;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * ProcessUtil enables the Java program to execute scripts on thos emailHost OS.
 */
public class Job
{
	private Job( final String[] args ) throws Exception
	{
		Log.out.info( "[BioLockJ Execute Command]: " + getArgsAsString( args ) );
		final Runtime r = Runtime.getRuntime();
		final Process p = r.exec( args );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s;
		while( ( s = br.readLine() ) != null )
		{
			if( !s.trim().isEmpty() )
			{
				Log.out.info( "[BioLockJ Process Output] " + s );
			}

		}

		p.waitFor();
		p.destroy();
	}

	private String getArgsAsString( final String[] args )
	{
		final StringBuffer sb = new StringBuffer();
		for( final String arg: args )
		{
			sb.append( arg + " " );
		}

		return sb.toString();
	}

	public static void submit( final File dir, final String[] args ) throws Exception
	{
		setFilePermissions( dir );
		new Job( args );
	}

	public static void submit( final String[] args ) throws Exception
	{
		new Job( args );
	}

	/**
	 * Populate Job args[] by tokenizing command and appending script absolute path.
	 *
	 * @param String command
	 * @param File script
	 * @return String[] args
	 */
	private static String[] getArgs( final String command, final File script )
	{
		final StringTokenizer st = new StringTokenizer( command + " " + script.getAbsolutePath() );
		final String[] args = new String[ st.countTokens() ];
		for( int i = 0; i < args.length; i++ )
		{
			args[ i ] = st.nextToken();
		}

		return args;
	}

	/**
	 * Execute SCRIPT_CHMOD_COMMAND to ensure the new bash scripts are executable.
	 * @param scriptDir
	 * @throws Exception
	 */
	private static void setFilePermissions( final File scriptDir ) throws Exception
	{
		final File[] listOfFiles = scriptDir.listFiles();
		for( final File file: listOfFiles )
		{
			if( file.isFile() && !file.getName().startsWith( "." ) )
			{
				submit( getArgs( Config.requireString( Config.SCRIPT_CHMOD_COMMAND ), file ) );
			}
		}
	}

}
