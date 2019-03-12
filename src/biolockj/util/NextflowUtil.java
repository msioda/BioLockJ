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
	 * Call this method to build the Nextflow main.nf for the current pipeline.
	 * 
	 * @param modules Pipeline modules
	 * @return Nextflow main.nf file
	 * @throws Exception if errors occur
	 */
	public static File buildNextflowMain( final List<BioModule> modules ) throws Exception
	{
		Log.info( NextflowUtil.class, "Initialize AWS Cloud Manager" );
		final File template = buildInitialTemplate( asString( modules ) );
		writeNextflowMainNF( getNextflowLines( template ) );
		BioLockJUtil.deleteWithRetry( templateConfig(), 3 );
		Log.info( NextflowUtil.class, "Nextflow main.nf generated: " + getMainNf().getAbsolutePath() );
		return getMainNf();
	}

	/**
	 * Copy the initialized pipeline to EFS. The aws_manager can then be launched with updated $BLJ_PROJ = EFS parent
	 * directory.
	 * 
	 * @return File EFS Pipeline directory
	 * @throws Exception if errors occur copying the pipeline.
	 */
	public static File copyPipelineToEfs() throws Exception
	{
		final File efsPipeline = new File( EFS_DIR + File.separator + Config.pipelineName() );
		FileUtils.copyDirectory( new File( Config.pipelinePath() ), efsPipeline, true );
		if( !efsPipeline.exists() )
		{
			throw new Exception( "Unable to create EFS pipeline directory: " + efsPipeline.getAbsolutePath() );
		}
		return efsPipeline;
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
		final List<String> lines = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( template );
		try
		{
			String module = null;
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( module != null )
				{
					if( line.trim().equals( "}" ) )
					{
						module = null;
					}
					else if( line.contains( NF_CPUS ) )
					{
						final String numCpus = Config.getString( null, Config.getModuleProp( module, NF_CPUS ) );
						line = line.replace( NF_CPUS, numCpus );
					}
					else if( line.contains( NF_MEMORY ) )
					{
						String ram = Config.requireString( null, Config.getModuleProp( module, NF_MEMORY ) );
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
					}
				}
				else if( line.trim().startsWith( PROCESS ) )
				{
					module = line.replace( PROCESS, "" ).replaceAll( "{", "" ).trim();
				}
				else if( line.contains( NF_PIPELINE_NAME ) )
				{
					line = line.replace( NF_PIPELINE_NAME, Config.pipelineName() );
				}

				lines.add( line );
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		return lines;
	}

	private static String asString( final List<BioModule> modules ) throws Exception
	{
		String flatMods = "";
		for( final BioModule module: modules )
		{
			if( !( module instanceof ImportMetadata ) && !( module instanceof Email ) )
			{
				flatMods += ( flatMods.isEmpty() ? "": SEPARATOR ) + module.getClass().getSimpleName();
			}
		}

		return flatMods;
	}

	private static File buildInitialTemplate( final String modules ) throws Exception
	{
		Log.info( NextflowUtil.class, "Build Nextflow initial template: " + templateConfig().getAbsolutePath() );
		final String[] args = new String[ 3 ];
		args[ 0 ] = templateScript().getAbsolutePath();
		args[ 1 ] = templateConfig().getAbsolutePath();
		args[ 2 ] = modules;
		Job.submit( args );
		if( !templateConfig().exists() )
		{
			throw new Exception( "Unable to build template: " + templateConfig().getAbsolutePath() );
		}
		Log.info( NextflowUtil.class, "Template file generated: " + templateConfig().getAbsolutePath() );
		return templateConfig();
	}

	private static String getDockerImageLabel( final String moduleName ) throws Exception
	{
		return "'" + IMAGE + "_" + DockerUtil.getDockerUser( moduleName ) + "_" + DockerUtil.getImageName( moduleName )
				+ "_" + DockerUtil.getImageVersion( moduleName ) + "'";
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
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getMainNf() ) );
		try
		{
			boolean indent = false;
			for( final String line: lines )
			{
				if( line.trim().equals( "}" ) )
				{
					indent = !indent;
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

	private static final String EFS_DIR = "/mount/efs";
	private static final String IMAGE = "image";
	private static final String MAIN_NF = "main.nf";
	private static final String MAKE_NEXTFLOW_SCRIPT = "make_nextflow";
	private static final String NF_CPUS = "$" + ScriptModule.SCRIPT_NUM_THREADS;
	private static final String NF_DOCKER_IMAGE = "$nextflow.dockerImage";

	private static final String NF_MEMORY = "$" + Constants.AWS_RAM;
	private static final String NF_PIPELINE_NAME = "$pipeline.pipelineName";
	private static final String PROCESS = "process";
	private static final String SEPARATOR = ".";
}
