/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 11, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.ScriptModule;
import biolockj.module.implicit.ImportMetadata;
import biolockj.module.report.Email;

/**
 * This utility builds the Nextflow main.nf used in AWS pipelines.
 */
public class NextflowUtil
{
	/**
	 * Save a copy of the Nextflow log file to the Pipeline root directory
	 * @throws Exception if errors occur
	 */
	public static void saveNextflowLog() throws Exception
	{
		FileUtils.copyFileToDirectory( new File( NF_LOG ), new File( Config.pipelinePath() + File.separator + "nextflow.log" ) );
	}

	/**
	 * Call this method to build the Nextflow main.nf for the current pipeline.
	 * 
	 * @param modules Pipeline modules
	 * @throws Exception if errors occur
	 */
	public static void startNextflow( final List<BioModule> modules ) throws Exception
	{
		final File template = buildInitialTemplate( asString( modules ) );
		writeNextflowMainNF( getNextflowLines( template ) );
		Log.info( NextflowUtil.class, "Nextflow main.nf generated: " + getMainNf().getAbsolutePath() );
		startService();
		pollAndSpin();
		Log.info( NextflowUtil.class, "Nextflow service sub-process started!" );
	}
	
	private static void pollAndSpin() throws Exception
	{
		Log.info( NextflowUtil.class, "Poll " + NF_LOG + " every 15 seconds until the status message \"" + NF_INIT_FLAG + "\" is logged" );
		int numSecs = 0;
		boolean finished = false;
		while( !finished )
		{
			finished = poll();
			if( !finished )
			{
				if( numSecs > NF_TIMEOUT )
				{
					throw new Exception( "Nextflow initialization timed out after " + numSecs + " seconds." );
				}
				Log.info( NextflowUtil.class, "Nextflow initializing..." );
				Thread.sleep( 15 * 1000 );
				numSecs += 15;
			}
			else
			{
				Log.info( NextflowUtil.class, "Nextflow initialization complete!" );
			}
		}
	}
	
	private static boolean poll() throws Exception
	{
		final File nfLog = new File( NF_LOG );
		if( nfLog.exists() )
		{
			final BufferedReader reader = BioLockJUtil.getFileReader( nfLog );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					if( line.contains( NF_INIT_FLAG ) )
					{
						return true;
					}
				}
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
			}
		}
		else
		{
			Log.info( NextflowUtil.class, "Nextflow log file \"" + NF_LOG + "\" has not been created yet..." );
		}
		return false;
	}
	
	
	private static void startService() throws Exception
	{
		final String reportBase = Config.pipelinePath() + File.separator + Config.pipelineName() + "_";
		final String[] args = new String[ 11 ];
		args[ 0 ] = NEXTFLOW_CMD;
		args[ 1 ] = "run";
		args[ 2 ] = "-work-dir";
		args[ 3 ] = S3_DIR + Config.requireString( null, Constants.AWS_S3 ) + File.separator + "nextflow";
		args[ 4 ] = "-with-trace";
		args[ 5 ] = reportBase + "nextflow_trace.tsv";
		args[ 6 ] = "-with-timeline";
		args[ 7 ] = reportBase + "nextflow_timeline.html";
		args[ 8 ] = "-with-dag";
		args[ 9 ] = reportBase + "nextflow_diagram.html";
		args[ 10 ] = getMainNf().getAbsolutePath();
		Processor.runSubprocess( args, "Nextflow" );
	}

	/**
	 * Get the Nextflow main.nf file path.
	 * 
	 * @return Nextflow main.nf
	 * @throws Exception if File I/O Errors occur
	 */
	public static File getMainNf() throws Exception
	{
		return new File( Config.pipelinePath() + File.separator + MAIN_NF );
	}

	/**
	 * Build the main.nf lines from the template file by replacing several parameters.
	 * 
	 * @param template Generated .main.nf template
	 * @return List of lines to save in final main.nf
	 * @throws Exception if errors occur
	 */
	protected static List<String> getNextflowLines( final File template ) throws Exception
	{
		Log.info( NextflowUtil.class, "BUILD main.nf from the template: " + template.getAbsolutePath() );
		final List<String> lines = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( template );
		try
		{
			ScriptModule module = null;
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				String onDemandLabel = null;
				Log.info( NextflowUtil.class, "READ LINE: " + line );
				if( line.trim().startsWith( PROCESS ) )
				{
					Log.info( NextflowUtil.class, "Found module PROCESS declaration: " + line );
					module = getModule( line.replaceAll( PACKAGE_SEPARATOR, "\\." ).replace( PROCESS, "" ).replaceAll( "\\{", "" ).trim() );
					Log.info( NextflowUtil.class, "START module BLOCK for: " + module.getClass().getName() );
					line = line.replaceAll( PACKAGE_SEPARATOR, "_" );
				}
				else if( module != null )
				{
					if( line.contains( NF_CPUS ) )
					{
						String prop = Config.getModuleProp( module, NF_CPUS.substring( 1 ) );
						line = line.replace( NF_CPUS, Config.getString( module, prop ) );
					}
					else if( line.contains( NF_MEMORY ) )
					{
						String prop = Config.getModuleProp( module, NF_MEMORY.substring( 1 ) );
						String ram = Config.requireString( module, prop );
						if( !ram.startsWith( "'" ) )
						{
							ram = "'" + ram;
						}
						if( !ram.endsWith( "'" ) )
						{
							ram = ram + "'";
						}
						line = line.replace( NF_MEMORY, ram );
					}
					else if( line.contains( NF_DOCKER_IMAGE ) )
					{
						line = line.replace( NF_DOCKER_IMAGE, getDockerImageLabel( module ) );
						if( Config.requireString( module, EC2_ACQUISITION_STRATEGY ).toUpperCase().equals( ON_DEMAND ) )
						{
							onDemandLabel = "    label '" + ON_DEMAND + "'";
						}
					}
					else if( line.contains( MODULE_SCRIPT ) )
					{
						Log.info( NextflowUtil.class, "Found worker line: " + line );
						line = line.replace( MODULE_SCRIPT, module.getScriptDir().getAbsolutePath() );
					}
					else if( line.trim().equals( "}" ) )
					{
						Log.info( NextflowUtil.class, "END module BLOCK: " + module.getClass().getName() );
						module = null;
					}
				}

				Log.info( NextflowUtil.class, "ADD LINE: " + line );
				lines.add( line );
				if( onDemandLabel != null )
				{
					lines.add( onDemandLabel );
					Log.info( NextflowUtil.class, "ADD LINE: " + onDemandLabel );
				}
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}
		Log.info( NextflowUtil.class, "# Lines in main.nf: " + lines.size() );
		return lines;
	}
	
	private static ScriptModule getModule( final String className ) throws Exception
	{
		Log.info( NextflowUtil.class, "Calling getModule( " + className + " )" );
		for( BioModule module: Pipeline.getModules() )
		{
			if( module.getClass().getName().equals( className ) )
			{
				if( usedModules.contains( module.getID() ) )
				{
					Log.info( NextflowUtil.class, "Skip module [ ID = " + module.getID() + " ] in since it was already used, look for another module of type: " + module.getClass().getName() );
				}
				else
				{
					Log.info( NextflowUtil.class, "getModule( " + className + " ) RETURN module [ ID = " + module.getID() + " ] --> " + module.getClass().getName() );
					usedModules.add( module.getID() );
					return (ScriptModule) module;
				}
			}
		}
		return null;
	}

	private static String asString( final List<BioModule> modules ) throws Exception
	{
		String flatMods = "";
		for( final BioModule module: modules )
		{
			if( !( module instanceof ImportMetadata ) && !( module instanceof Email ) )
			{
				flatMods += ( flatMods.isEmpty() ? "": MODULE_SEPARATOR ) + module.getClass().getName().replaceAll( "\\.", PACKAGE_SEPARATOR );
			}
		}

		return flatMods;
	}

	private static File buildInitialTemplate( final String modules ) throws Exception
	{
		Log.info( NextflowUtil.class, "Build Nextflow initial template: " + templateConfig().getAbsolutePath() );
		Log.info( NextflowUtil.class, "Nextflow modules: " + modules );

		final String[] args = new String[ 3 ];
		args[ 0 ] = templateScript().getAbsolutePath();
		args[ 1 ] = templateConfig().getAbsolutePath();
		args[ 2 ] = modules;
		Processor.submit( args );
		if( !templateConfig().exists() )
		{
			throw new Exception( "Nextflow Template file is not found at path: " + templateConfig().getAbsolutePath() );
		}
		Log.info( NextflowUtil.class, "Nextflow Template file created: " + templateConfig().getAbsolutePath() );
		return templateConfig();
	}

	private static String getDockerImageLabel( final BioModule module ) throws Exception
	{
		return "'" + IMAGE + "_" + DockerUtil.getDockerUser( module ) + "_" + DockerUtil.getImageName( module )
				+ "_" + DockerUtil.getImageVersion( module ) + "'";
	}

	private static File templateConfig() throws Exception
	{
		return new File( Config.pipelinePath() + File.separator + ".template_" + MAIN_NF );
	}

	private static File templateScript() throws Exception
	{
		return new File( BioLockJUtil.getBljDir().getAbsolutePath() + File.separator + Constants.SCRIPT_DIR
				+ File.separator + MAKE_NEXTFLOW_SCRIPT );
	}

	private static void writeNextflowMainNF( final List<String> lines ) throws Exception
	{
		Log.debug( NextflowUtil.class, "Create " + getMainNf().getAbsolutePath() + " with # lines = " + lines.size() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getMainNf() ) );
		try
		{
			boolean indent = false;
			for( String line: lines )
			{
				if( line.trim().equals( "}" ) )
				{
					indent = !indent;
				}
				if( indent ) 
				{
					line = "    " + line;
				}
				writer.write( line + Constants.RETURN );
				if( line.trim().endsWith( "{" ) )
				{
					indent = !indent;
				}
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}
	}

	private static final int NF_TIMEOUT = 180;
	private static final String NF_LOG = File.separator + ".nextflow.log";
	private static final String NF_INIT_FLAG = "Session await";
	private static final Set<Integer> usedModules = new HashSet<>();
	private static final String ON_DEMAND = "DEMAND";
	private static final String EC2_ACQUISITION_STRATEGY = "aws.ec2AcquisitionStrategy";
	private static final String IMAGE = "image";
	private static final String MAIN_NF = "main.nf";
	private static final String MODULE_SCRIPT = "BLJ_MODULE_SUB_DIR";
	private static final String MAKE_NEXTFLOW_SCRIPT = "make_nextflow";
	private static final String MODULE_SEPARATOR = ".";
	private static final String NEXTFLOW_CMD = "nextflow";
	private static final String NF_CPUS = "$" + ScriptModule.SCRIPT_NUM_THREADS;
	private static final String NF_DOCKER_IMAGE = "$nextflow.dockerImage";
	private static final String NF_MEMORY = "$" + Constants.AWS_RAM;
	private static final String PACKAGE_SEPARATOR = "_:_";
	private static final String PROCESS = "process";
	private static final String S3_DIR = "s3://";
}
