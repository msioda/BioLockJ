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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
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
	 * Call this method to build the Nextflow main.nf for the current pipeline.
	 * 
	 * @param modules Pipeline modules
	 * @throws Exception if errors occur
	 */
	public static void startNextflow( final List<BioModule> modules ) throws Exception
	{
		Log.info( NextflowUtil.class, "Initialize AWS Cloud Manager" );
		final File template = buildInitialTemplate( asString( modules ) );
		Log.info( NextflowUtil.class, "Generated template: " + template.getAbsolutePath() );
		writeNextflowMainNF( getNextflowLines( template ) );
		BioLockJUtil.deleteWithRetry( templateConfig(), 5 );
		Log.info( NextflowUtil.class, "Nextflow main.nf generated: " + getMainNf().getAbsolutePath() );
		startService();
		Log.info( NextflowUtil.class, "Nextflow service sub-process started!" );
		//return getMainNf();
	}
	
	private static void startService() throws Exception
	{
		final String[] args = new String[ 3 ];
		args[ 0 ] = NEXTFLOW_CMD;
		args[ 1 ] = "run";
		args[ 2 ] = getMainNf().getAbsolutePath();
		Processor.runSubprocess( args, "Nextflow-main" );
		
		
		
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
		Log.info( NextflowUtil.class, "Calling getNextflowLines()" );
		final List<String> lines = new ArrayList<>();

		final BufferedReader reader = BioLockJUtil.getFileReader( template );
		try
		{
			String moduleName = null;
			BioModule module = null;
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				String onDemandLabel = null;
				Log.info( NextflowUtil.class, "READ line: " + line );
				if( moduleName != null )
				{
					module = getShellModule( moduleName );

					if( line.trim().equals( "}" ) )
					{
						moduleName = null;
						module = null;
					}
					if( line.trim().startsWith( WORKER_FLAG ) && line.contains( module.getClass().getSimpleName() ) )
					{
						Log.info( NextflowUtil.class, "Found worker: " + line );
						String moduleFlag = moduleName.replaceAll( "\\.", PACKAGE_SEPARATOR );
						Log.info( NextflowUtil.class, "Replace moduleFlag: " + moduleFlag + " with " + module.getClass().getSimpleName() );
						line = line.replace( moduleFlag, module.getClass().getSimpleName() );
					}
					if( line.contains( NF_CPUS ) )
					{
						String prop = Config.getModuleProp( module, NF_CPUS.substring( 1 ) );
						line = line.replace( NF_CPUS, Config.getString( module, prop ) );
					}
					if( line.contains( NF_MEMORY ) )
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
					if( line.contains( NF_DOCKER_IMAGE ) )
					{
						line = line.replace( NF_DOCKER_IMAGE, getDockerImageLabel( module ) );
						if( Config.requireString( module, EC2_ACQUISITION_STRATEGY ).toUpperCase().equals( ON_DEMAND ) )
						{
							onDemandLabel = "    label '" + ON_DEMAND + "'";
						}
					}
				}
				if( line.trim().startsWith( PROCESS ) )
				{
					Log.info( NextflowUtil.class, "Found module on line: " + line );
					line = line.replaceAll( PACKAGE_SEPARATOR, "\\." );
					moduleName = line.replace( PROCESS, "" ).replaceAll( "\\{", "" ).trim();
					line = line.replace( moduleName, convertModuleName( moduleName ) );
				}
				if( line.contains( NF_PIPELINE_NAME ) )
				{
					line = line.replace( NF_PIPELINE_NAME, Config.pipelineName() );
				}

				Log.info( NextflowUtil.class, "Add line: " + line );
				lines.add( line );
				if( onDemandLabel != null )
				{
					lines.add( onDemandLabel );
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
		Log.info( NextflowUtil.class, "Return: " + lines.size() + " total lines" );
		return lines;
	}
	
	private static BioModule getShellModule( final String className ) throws Exception
	{
		return (BioModule) Class.forName( className ).getDeclaredConstructor().newInstance();
	}
	
	private static String convertModuleName( String name ) throws Exception
	{
		String val = "";
		StringTokenizer st = new StringTokenizer( name, "." );
		while( st.hasMoreTokens() )
		{
			val += st.nextToken();
			if( st.hasMoreTokens() )
			{
				val += "_";
			}
		}
		return val;
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
		Log.info( NextflowUtil.class, "Using module list: " + modules );

		final String[] args = new String[ 3 ];
		args[ 0 ] = templateScript().getAbsolutePath();
		args[ 1 ] = templateConfig().getAbsolutePath();
		args[ 2 ] = modules;
		Processor.submit( args );
		if( !templateConfig().exists() )
		{
			throw new Exception( "Nextflow Template failed to build: " + templateConfig().getAbsolutePath() );
		}
		Log.info( NextflowUtil.class, "Nextflow Template successfully created: " + templateConfig().getAbsolutePath() );
		return templateConfig();
	}

	private static String getDockerImageLabel( final BioModule module ) throws Exception
	{
		return "'" + IMAGE + "_" + DockerUtil.getDockerUser( module ) + "_" + DockerUtil.getImageName( module )
				+ "_" + DockerUtil.getImageVersion( module ) + "'";
	}

	private static File templateConfig() throws Exception
	{
		return new File( Config.pipelinePath() + File.separator + "." + MAIN_NF );
	}

	private static File templateScript() throws Exception
	{
		return new File( BioLockJUtil.getBljDir().getAbsolutePath() + File.separator + Constants.SCRIPT_DIR
				+ File.separator + MAKE_NEXTFLOW_SCRIPT );
	}

	private static void writeNextflowMainNF( final List<String> lines ) throws Exception
	{
		Log.info( NextflowUtil.class, "Exec writeNextflowMainNF() - total # lines: " + lines.size() );
		Log.info( NextflowUtil.class, "getMainNf(): " + getMainNf() );
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

	private static final String ON_DEMAND = "DEMAND";
	private static final String EC2_ACQUISITION_STRATEGY = "aws.ec2AcquisitionStrategy";
	private static final String IMAGE = "image";
	private static final String MAIN_NF = "main.nf";
	private static final String MAKE_NEXTFLOW_SCRIPT = "make_nextflow";
	private static final String WORKER_FLAG = "val worker from Channel.watchPath";
	private static final String MODULE_SEPARATOR = ".";
	private static final String NEXTFLOW_CMD = "nextflow";
	private static final String NF_CPUS = "$" + ScriptModule.SCRIPT_NUM_THREADS;
	private static final String NF_DOCKER_IMAGE = "$nextflow.dockerImage";
	private static final String NF_MEMORY = "$" + Constants.AWS_RAM;
	private static final String NF_PIPELINE_NAME = "$pipeline.pipelineName";
	private static final String PACKAGE_SEPARATOR = "_:_";
	private static final String PROCESS = "process";
}
