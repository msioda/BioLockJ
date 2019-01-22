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
package biolockj.module;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;
import biolockj.util.*;

/**
 * Superclass for Java BioModules that will be called in separate instances of the application.
 */
public abstract class ScriptModuleImpl extends BioModuleImpl implements ScriptModule
{
	@Override
	public abstract List<List<String>> buildScript( final List<File> files ) throws Exception;

	/**
	 * The default behavior is the same for paired or unpaired data. This method must be overridden to create separate
	 * scripts for paired datasets.
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		return buildScript( files );
	}

	/**
	 * Validate module dependencies:
	 * <ul>
	 * <li>Require {@link biolockj.Config}.{@value #SCRIPT_PERMISSIONS} exists
	 * <li>Require {@link biolockj.Config}.{@value #SCRIPT_BATCH_SIZE} is positive integer
	 * <li>Require {@link biolockj.Config}.{@value #SCRIPT_NUM_THREADS} is positive integer
	 * <li>Verify {@link biolockj.Config}.{@value #SCRIPT_TIMEOUT} is positive integer if set
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		Config.requireString( SCRIPT_PERMISSIONS );
		Config.requirePositiveInteger( SCRIPT_BATCH_SIZE );
		Config.requirePositiveInteger( SCRIPT_NUM_THREADS );
		Config.getPositiveInteger( SCRIPT_TIMEOUT );
	}

	/**
	 * 
	 * Build the nested list of bash script lines that will be used by {@link biolockj.util.BashScriptBuilder} to build
	 * the worker scripts. If running Docker, return {@link biolockj.util.DockerUtil#buildDockerScript()}, else
	 * pass{@link #getInputFiles()} to either {@link #buildScript(List)} or {@link #buildScriptForPairedReads(List)}
	 * based on {@link biolockj.Config}.{@value biolockj.Config#INTERNAL_PAIRED_READS}.
	 */
	@Override
	public void executeTask() throws Exception
	{
		if( DockerUtil.isDockerJavaModule( this ) )
		{
			BashScriptBuilder.buildScripts( this, DockerUtil.buildDockerScript(), 1 );
		}
		else
		{
			final List<List<String>> data = Config.getBoolean( Config.INTERNAL_PAIRED_READS )
					? buildScriptForPairedReads( getInputFiles() )
					: buildScript( getInputFiles() );
			BashScriptBuilder.buildScripts( this, data, Config.requireInteger( ScriptModule.SCRIPT_BATCH_SIZE ) );
		}
	}

	@Override
	public String[] getJobParams() throws Exception
	{
		return new String[] { ModuleUtil.getMainScript( this ).getAbsolutePath() };
	}

	/**
	 * Returns moduleDir/script which contains all scripts generated by the module.
	 */
	@Override
	public File getScriptDir()
	{
		return ModuleUtil.requireSubDir( this, SCRIPT_DIR );
	}

	/**
	 * Returns summary message to be displayed by Email module so must not contain confidential info. ModuleUtil
	 * provides summary metrics on output files
	 */
	@Override
	public String getSummary() throws Exception
	{
		return super.getSummary() + SummaryUtil.getScriptDirSummary( this );
	}

	/**
	 * Default behavior is for scripts to run indefinitely (no timeout).
	 */
	@Override
	public Integer getTimeout() throws ConfigFormatException
	{
		return Config.getPositiveInteger( SCRIPT_TIMEOUT );
	}

	/**
	 * Build the docker run command to launch a JavaModule in a Docker container.
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		if( DockerUtil.isDockerJavaModule( this ) )
		{
			return DockerUtil.buildRunDockerFunction( this );
		}

		return new ArrayList<>();
	}

	/**
	 * Return all collectionProperty values separated by a space. If numThreadsParam is not null, append the numThreads
	 * param and value.
	 * 
	 * @param params Runtime parameter
	 * @param numThreadsParam Number of threads parameter name
	 * @return all runtime parameters
	 * @throws Exception if errors occur
	 */
	protected String getRuntimeParams( final List<String> params, final String numThreadsParam ) throws Exception
	{
		return ( numThreadsParam == null ? "": numThreadsParam + " " + getNumThreads() )
				+ ( params == null || params.isEmpty() ? "": BioLockJUtil.join( params ) ) + " ";
	}

	private Integer getModuleNumThreads()
	{
		try
		{
			return Config.getPositiveInteger( getClass().getSimpleName() + NUM_THREADS );
		}
		catch( final Exception ex )
		{
			// FAIL SILENTLY
			// EXCEPTION ONLY INDICATES MODULE SPECIFIC HEADER DOES NOT EXIST
		}
		return null;
	}

	private Integer getNumThreads() throws ConfigFormatException, ConfigNotFoundException
	{
		if( getModuleNumThreads() != null )
		{
			return getModuleNumThreads();
		}

		return Config.requirePositiveInteger( SCRIPT_NUM_THREADS );
	}

}
