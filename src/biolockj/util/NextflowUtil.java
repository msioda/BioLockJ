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
	public static File buildNextFlowMain( final List<BioModule> modules ) throws Exception
	{
		Log.info( NextflowUtil.class, "Running AWS Cloud Manager [ INIT MODE ]"  );
		final File template = buildInitialTemplate( flattenList( modules ) );
		writeNextFlowMainNF( getNextFlowLines( template ) );
		BioLockJUtil.deleteWithRetry( getNextflowTempConfig(), 3 );
		Log.info( NextflowUtil.class, "Nextflow main.nf generated: " + getNextflowMainConfig().getAbsolutePath() );
		return getNextflowMainConfig();
	}

	/**
	 * Build the main.nf lines from the template file by replacing several parameters.
	 * 
	 * @param template Generated .main.nf template
	 * @return List of lines to save in final main.nf
	 * @throws Exception if errors occur
	 */
	protected static List<String> getNextFlowLines( final File template ) throws Exception
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
						final String ram = Config.getString( null, Config.getModuleProp( module, NF_MEMORY ) );
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
				else if( line.contains( NF_EFS_DIR ) )
				{
					final File efsDir = Config.requireExistingDir( null, Constants.AWS_EFS_DIR );
					line = line.replace( NF_EFS_DIR, efsDir.getAbsolutePath() );
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

	private static File buildInitialTemplate( final String modules ) throws Exception
	{
		final File tempFile = getNextflowTempConfig();
		Log.info( NextflowUtil.class, "Building template file: " + tempFile.getAbsolutePath() );
		final String[] args = new String[ 3 ];
		args[ 0 ] = buildTemplateScript().getAbsolutePath();
		args[ 1 ] = tempFile.getAbsolutePath();
		args[ 2 ] = modules;
		Job.submit( args );
		if( !tempFile.exists() )
		{
			throw new Exception( "Unable to build template: " + tempFile.getAbsolutePath() );
		}
		Log.info( NextflowUtil.class, "Template file generated: " + tempFile.getAbsolutePath() );

		return tempFile;
	}

	private static File buildTemplateScript() throws Exception
	{
		return new File( BioLockJUtil.getBljDir().getAbsolutePath() + File.separator + Constants.SCRIPT_DIR
				+ File.separator + MAKE_NEXTFLOW_SCRIPT );
	}

	private static String flattenList( final List<BioModule> modules ) throws Exception
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

	private static String getDockerImageLabel( final String moduleName ) throws Exception
	{
		return "'" + IMAGE + "_" + DockerUtil.getDockerUser( moduleName ) + "_" + DockerUtil.getImageName( moduleName )
				+ "_" + DockerUtil.getImageVersion( moduleName ) + "'";
	}

	/**
	 * Get the Nextflow main.nf file path.  
	 * 
	 * @return Nextflow main.nf
	 * @throws Exception if File I/O Errors occur
	 */
	public static File getNextflowMainConfig() throws Exception
	{
		return new File( Config.pipelinePath() + File.separator + MAIN_NF );
	}
	
	private static File getNextflowTempConfig() throws Exception
	{
		return new File( Config.pipelinePath() + File.separator + "." + MAIN_NF );
	}

	private static void writeNextFlowMainNF( final List<String> lines ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getNextflowMainConfig() ) );
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

	private static final String IMAGE = "image";
	private static final String MAIN_NF = "main.nf";
	private static final String MAKE_NEXTFLOW_SCRIPT = "make_nextflow";
	private static final String NF_CPUS = "$" + ScriptModule.SCRIPT_NUM_THREADS;
	private static final String NF_DOCKER_IMAGE = "$nextflow.dockerImage";

	private static final String NF_EFS_DIR = "$" + Constants.AWS_EFS_DIR;
	private static final String NF_MEMORY = "$" + Constants.AWS_RAM;
	private static final String NF_PIPELINE_NAME = "$project.pipelineName";
	private static final String PROCESS = "process";
	private static final String SEPARATOR = ".";
}
