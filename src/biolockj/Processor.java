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
import biolockj.exception.ConfigPathException;
import biolockj.module.ScriptModule;
import biolockj.util.BioLockJUtil;

/**
 * {@link biolockj.module.ScriptModule}s that generate scripts will submit a main script to the OS for execution as a
 * {@link biolockj.Processor}.
 */
public class Processor {

	/**
	 * Class used to submit processes on their own Thread.
	 */
	public class SubProcess implements Runnable {
		/**
		 * Execute the command args in a separate thread and log output with label.
		 * 
		 * @param args Command args
		 * @param label Log label
		 */
		public SubProcess( final String[] args, final String label ) {
			this.args = args;
			this.label = label;
		}

		@Override
		public void run() {
			try {
				Log.info( getClass(), " initiailizing..." );
				new Processor().runJob( this.args, this.label );
			} catch( final Exception ex ) {
				Log.error( getClass(),
					"Problem occurring within SubProcess-" + this.label + " --> " + ex.getMessage() );
				ex.printStackTrace();
			}
		}

		private String[] args = null;
		private String label = null;
	}

	Processor() {}

	/**
	 * Execute the command args and log output with label.
	 * 
	 * @param args Command args
	 * @param label Log label
	 * @throws Exception if errors occur in the Processor
	 */
	public void runJob( final String[] args, final String label ) throws Exception {
		Log.info( getClass(), "[ " + label + " ]: " + getArgsAsString( args ) );
		final Process p = Runtime.getRuntime().exec( args );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String s;
		Log.info( getClass(), "[ " + label + " ] process started..." );
		while( ( s = br.readLine() ) != null ) {
			if( !s.trim().isEmpty() ) {
				Log.info( getClass(), "[ " + label + " ] " + s );
			}
		}
		p.waitFor();
		p.destroy();
		Log.info( getClass(), "[ " + label + " ] complete" );
	}

	/**
	 * Return the value of the bash variable from the runtime shell.
	 * 
	 * @param bashVar Bash variable name
	 * @return Bash env variable value or null if not found (or undefined)
	 * @throws Exception if errors occur running the script
	 */
	public static String getBashVar( final String bashVar ) throws Exception {
		if( bashVar == null ) return null;
		final String var = bashVar.startsWith( "$" ) || bashVar.equals( "~" ) ? bashVar: "$" + bashVar;
		final String[] args = new String[] { bashVarScript().getAbsolutePath(), var };
		final Process p = Runtime.getRuntime().exec( args );
		final BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
		String bashVarValue = null;
		String s;
		while( ( s = br.readLine() ) != null )
			if( s.startsWith( BLJ_GET_ENV_VAR_KEY ) ) {
				bashVarValue = s.replace( BLJ_GET_ENV_VAR_KEY, "" ).trim();
			}
		p.waitFor();
		p.destroy();
		return bashVarValue;
	}

	/**
	 * Instantiates a new {@link biolockj.Processor}.<br>
	 * String[] array used to control spacing between command/params.<br>
	 * As if executing on terminal args[0] args[1]... args[n-1] as one command.
	 *
	 * @param args Terminal command created from args (adds 1 space between each array element)
	 * @param label to associate with the process
	 */
	public static void runSubprocess( final String[] args, final String label ) {
		new Thread( new Processor().new SubProcess( args, label ) ).start();
	}

	/**
	 * Set file permissions by executing chmod {@value biolockj.module.ScriptModule#SCRIPT_PERMISSIONS} on generated
	 * bash scripts.
	 *
	 * @param path Target directory path
	 * @param permissions Set the chmod security bits (ex 764)
	 * @throws Exception if chmod command command fails
	 */
	public static void setFilePermissions( final String path, final String permissions ) throws Exception {
		final StringTokenizer st = new StringTokenizer( "chmod -R " + permissions + " " + path );
		final String[] args = new String[ st.countTokens() ];
		for( int i = 0; i < args.length; i++ ) {
			args[ i ] = st.nextToken();
		}

		submit( args, "Set File Privs" );
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
	public static void submit( final ScriptModule module ) throws Exception {
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
	 * @param label - Process label
	 * @throws Exception if errors occur during execution
	 */
	public static void submit( final String[] args, final String label ) throws Exception {
		new Processor().runJob( args, label );
	}

	private static File bashVarScript() throws Exception {
		final File script = new File( BioLockJUtil.getBljDir().getAbsolutePath() + File.separator + Constants.SCRIPT_DIR
			+ File.separator + BLJ_GET_ENV_VAR_SCRIPT );
		if( script.isFile() ) return script;
		throw new ConfigPathException( script );
	}

	private static String getArgsAsString( final String[] args ) {
		final StringBuffer sb = new StringBuffer();
		for( final String arg: args ) {
			sb.append( arg + " " );
		}
		return sb.toString();
	}

	private static final String BLJ_GET_ENV_VAR_KEY = "BLJ_GET_ENV_VAR";
	private static final String BLJ_GET_ENV_VAR_SCRIPT = "get_env_var";
}
