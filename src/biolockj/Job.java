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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import biolockj.module.ScriptModule;

/**
 * {@link biolockj.module.ScriptModule}s that generate scripts will submit a main script to the OS for execution as a
 * {@link biolockj.Job}.
 */
public class Job
{
	private Job( final ScriptModule module ) throws Exception
	{
		Log.get( getClass() ).info( "[Run Command]: " + getArgsAsString( module.getJobParams() ) );
		final Runtime r = Runtime.getRuntime();
		final Process p = r.exec( module.getJobParams() );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s;
		while( ( s = br.readLine() ) != null )
		{
			if( !s.trim().isEmpty() )
			{
				Log.get( getClass() ).info( "[" + module.getClass().getSimpleName() + "] " + s );
			}
		}

		p.waitFor();
		p.destroy();
	}

	private Job( final String[] args ) throws Exception
	{
		Log.get( getClass() ).info( "[Run Command]: " + getArgsAsString( args ) );
		final Runtime r = Runtime.getRuntime();
		final Process p = r.exec( args );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s;
		while( ( s = br.readLine() ) != null )
		{
			if( !s.trim().isEmpty() )
			{
				Log.get( getClass() ).info( "[Process] " + s );
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

	/**
	 * Set file permissions by executing chmod {@value biolockj.module.ScriptModule#SCRIPT_PERMISSIONS} on generated
	 * bash scripts.
	 *
	 * @param dir Target directory
	 * @param permissions Set the chmod security bits (ex 764)
	 * @param recursive Boolean if chmod should add -R switch
	 * @throws Exception if chmod command command fails
	 */
	public static void setFilePermissions( final File dir, final String permissions, final boolean recursive ) throws Exception
	{
		final String[] args = new String[ 1 ];
		args[ 0 ] = "chmod " + ( recursive ? "-R" : "" ) + permissions + " " + dir.getAbsolutePath();
		submit( args );
//		for( final File f: dir.listFiles() )
//		{
//			final StringTokenizer st = new StringTokenizer(
//					"chmod " + Config.requireString( ScriptModule.SCRIPT_PERMISSIONS ) + " " + f.getAbsolutePath() );
//			final String[] args = new String[ st.countTokens() ];
//			for( int i = 0; i < args.length; i++ )
//			{
//				args[ i ] = st.nextToken();
//			}
//			submit( args );
//		}
	}

	/**
	 * This method is called by script generating {@link biolockj.module.ScriptModule}s to update the script
	 * file-permissions to ensure they are executable by the program. Once file permissions are set, the main script
	 * (passed in the args param) is executed. Calls {@link #setFilePermissions(File, String, boolean)} and {@link #submit(ScriptModule)}
	 *
	 * @param module ScriptModule that is submitting its main script as a Job
	 * @throws Exception if errors occur during execution
	 */
	public static void submit( final ScriptModule module ) throws Exception
	{
		setFilePermissions( module.getScriptDir(), Config.requireString( ScriptModule.SCRIPT_PERMISSIONS ), false );
		new Job( module );
	}

	/**
	 * Instantiates a new {@link biolockj.Job}.<br>
	 * String[] array used to control spacing between command/params.<br>
	 * As if executing on terminal args[0] args[1]... args[n-1] as one command.
	 *
	 * @param args Terminal command created from args (adds 1 space between each array element)
	 * @throws Exception if errors occur during execution
	 */
	public static void submit( final String[] args ) throws Exception
	{
		new Job( args );
	}

}
