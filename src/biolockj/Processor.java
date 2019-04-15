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
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import biolockj.module.ScriptModule;

/**
 * {@link biolockj.module.ScriptModule}s that generate scripts will submit a main script to the OS for execution as a
 * {@link biolockj.Processor}.
 */
public class Processor
{
	/**
	 * Class used to submit processes on their own Thread.
	 */
	public class SubProcess implements Runnable
	{
		private String[] args = null;
		private String label = null;
		
		/**
		 * Execute the command args in a separate thread and log output with label.
		 * 
		 * @param args Command args
		 * @param label Log label
		 * @throws Exception if errors occur in the Processor
		 */
		public SubProcess( final String[] args, final String label )
		{
			this.args = args;
			this.label = label;
		}

		@Override
		public void run()
		{
			try
			{
				Log.info( getClass(), "SubProcess initiailizing..." );
				new Processor().runJob( args, "SubProcess-" + label );
			} 
			catch( Exception ex )
			{
				Log.error( getClass(), "Problem occurring within SubProcess-" + label + " --> " + ex.getMessage() );
				ex.printStackTrace();
			}
		}
	}
	
	private Processor()
	{};

	/**
	 * Execute the command args and log output with label.
	 * 
	 * @param args Command args
	 * @param label Log label
	 * @throws Exception if errors occur in the Processor
	 */
	public void runJob( final String[] args, final String label ) throws Exception
	{
		Log.info( getClass(), "[Run Command]: " + getArgsAsString( args ) );
		final Runtime r = Runtime.getRuntime();
		final Process p = r.exec( args );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s;
		Log.info( getClass(), "BioLockJ process-" + label + " process started..." );
		while( ( s = br.readLine() ) != null )
		{
			if( !s.trim().isEmpty() )
			{
				Log.info( getClass(), "[" + label + "] " + s );
			}
		}
		Log.info( getClass(), "BioLockJ process-" + label + " has no further output" );
		p.waitFor();
		p.destroy();
		Log.info( getClass(), "BioLockJ process-" + label + " has been destroyed" );
	}

	/**
	 * Set file permissions by executing chmod {@value biolockj.module.ScriptModule#SCRIPT_PERMISSIONS} on generated
	 * bash scripts.
	 *
	 * @param path Target directory path
	 * @param permissions Set the chmod security bits (ex 764)
	 * @throws Exception if chmod command command fails
	 */
	public static void setFilePermissions( final String path, final String permissions ) throws Exception
	{
		final StringTokenizer st = new StringTokenizer( "chmod -R " + permissions + " " + path );
		final String[] args = new String[ st.countTokens() ];
		for( int i = 0; i < args.length; i++ )
		{
			args[ i ] = st.nextToken();
		}

		submit( args );
	}

	/**
	 * This method is called by script generating {@link biolockj.module.ScriptModule}s to update the script
	 * file-permissions to ensure they are executable by the program. Once file permissions are set, the main script
	 * (passed in the args param) is executed. Calls {@link #setFilePermissions(String, String)} and
	 * {@link #submit(ScriptModule)}
	 *
	 * @param module ScriptModule that is submitting its main script as a Processor
	 * 
	 * @throws Exception if errors occur during execution
	 */
	public static void submit( final ScriptModule module ) throws Exception
	{
		setFilePermissions( module.getScriptDir().getAbsolutePath(),
				Config.requireString( module, ScriptModule.SCRIPT_PERMISSIONS ) );
		new Processor().runJob( module.getJobParams(), module.getClass().getSimpleName() );
	}

	/**
	 * Instantiates a new {@link biolockj.Processor}.<br>
	 * String[] array used to control spacing between command/params.<br>
	 * As if executing on terminal args[0] args[1]... args[n-1] as one command.
	 *
	 * @param args Terminal command created from args (adds 1 space between each array element)
	 * @throws Exception if errors occur during execution
	 */
	public static void submit( final String[] args ) throws Exception
	{
		new Processor().runJob( args, "Process" );
	}
	
	/**
	 * Instantiates a new {@link biolockj.Processor}.<br>
	 * String[] array used to control spacing between command/params.<br>
	 * As if executing on terminal args[0] args[1]... args[n-1] as one command.
	 *
	 * @param args Terminal command created from args (adds 1 space between each array element)
	 * @param label to associate with the process
	 * @throws Exception if errors occur during execution
	 */
	public static void runSubprocess( final String[] args, String label ) throws Exception
	{
		new Thread( new Processor().new SubProcess( args, label ) ).start();
	}

	private static String getArgsAsString( final String[] args )
	{
		final StringBuffer sb = new StringBuffer();
		for( final String arg: args )
		{
			sb.append( arg + " " );
		}

		return sb.toString();
	}
}