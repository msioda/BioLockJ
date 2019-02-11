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
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.*;
import biolockj.util.*;

/**
 * Superclass for Java BioModules that will be called in separate instances of the application.
 */
public abstract class JavaModuleImpl extends ScriptModuleImpl implements JavaModule
{
	/**
	 * Java script only require 2 lines, one to run the blj_config to update our $PATH and gain access to environment
	 * variables, and then the direct call to the BioLockJ.jar.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final ArrayList<String> lines = new ArrayList<>();

		lines.add( "java" + getSource() + " " + BioLockJUtil.getDirectModuleParam( this ) + " "
				+ RuntimeParamUtil.BASE_DIR_FLAG + " " + RuntimeParamUtil.getBaseDir().getAbsolutePath() + " "
				+ RuntimeParamUtil.CONFIG_FLAG + " " + Config.getConfigFilePath() );

		data.add( lines );
		return data;
	}

	/**
	 * JavaModules run pure Java code.<br>
	 * If in Docker mode and not in Direct mode, execute {@link biolockj.module.ScriptModule#executeTask()} to build the
	 * bash script.<br>
	 * If not in Docker mode AND on the cluster AND
	 * {@link biolockj.Config}.{@value biolockj.util.BashScriptBuilder#CLUSTER_RUN_JAVA_AS_SCRIPT}={@value biolockj.Config#TRUE}
	 * execute {@link biolockj.module.ScriptModule#executeTask()} to build the bash script<br>
	 * Otherwise, execute {@link #runModule()} to run the Java code to execute module functionality.
	 */
	@Override
	public void executeTask() throws Exception
	{
		final boolean buildClusterScript = !RuntimeParamUtil.isDockerMode() && Config.isOnCluster()
				&& Config.getBoolean( this, BashScriptBuilder.CLUSTER_RUN_JAVA_AS_SCRIPT );
		final boolean buildDockerSciprt = RuntimeParamUtil.isDockerMode() && !RuntimeParamUtil.isDirectMode();
		if( buildClusterScript || buildDockerSciprt )
		{
			super.executeTask();
		}
		else
		{
			runModule();
		}

	}

	/**
	 * If module is a {@link biolockj.module.SeqModule} input must contain sequence data.
	 */
	@Override
	public boolean isValidInputModule( final BioModule module )
	{
		if( this instanceof SeqModule )
		{
			return SeqUtil.isSeqModule( module );
		}
		return super.isValidInputModule( module );
	}

	@Override
	public void moduleComplete() throws Exception
	{
		markStatus( Pipeline.SCRIPT_SUCCESS );
		Log.info( getClass(), "Direct module complete!  Terminate direct application instance." );
	}

	@Override
	public void moduleFailed() throws Exception
	{
		markStatus( Pipeline.SCRIPT_FAILURES );
		Log.info( getClass(), "Direct module failed!  Terminate direct application instance." );
	}

	@Override
	public abstract void runModule() throws Exception;

	/**
	 * Get the program source (either the jar path or main class biolockj.BioLockJ);
	 * 
	 * @return java source parameter (either Jar or main class with classpath)
	 * @throws Exception if unable to determine source
	 */
	protected final String getSource() throws Exception
	{
		final File source = new File(
				JavaModuleImpl.class.getProtectionDomain().getCodeSource().getLocation().toURI() );
		String javaString = null;
		if( source.exists() && source.isFile() )
		{
			javaString = " -jar " + source.getAbsolutePath();
		}
		else if( source.exists() && source.isDirectory() )
		{
			final String lib = source.getAbsolutePath().replace( "bin", "lib/*" );
			javaString = " -cp " + source.getAbsolutePath() + ":" + lib + " " + BioLockJ.class.getName();
		}

		if( javaString != null )
		{
			Log.debug( getClass(), "BioLockJ Java source code for java command: " + javaString );
			return javaString;
		}

		throw new Exception( "Cannot find BioLockJ program source: " + source.getAbsolutePath() );
	}

	/**
	 * This method sets the module status by saving the indicator file to the module root dir.
	 * 
	 * @param status Success or Failures
	 * @throws Exception if unable to set the status
	 */
	protected void markStatus( final String status ) throws Exception
	{
		File statusIndicator = null;
		File script = null;
		final Collection<File> files = FileUtils.listFiles( getScriptDir(), HiddenFileFilter.VISIBLE, null );
		for( final File file: files )
		{
			final String key = ".0_" + getClass().getSimpleName() + SH_EXT;
			if( file.getName().endsWith( key + "_" + status ) )
			{
				statusIndicator = file;
			}

			if( file.getName().endsWith( key ) )
			{
				script = file;
			}
		}

		if( script == null )
		{
			final String msg = "Cannot find DIRECT script in:  " + getScriptDir().getAbsolutePath();
			Log.warn( getClass(), msg );
			throw new Exception( msg );
		}
		else
		{
			if( statusIndicator == null )
			{
				final String path = script.getAbsolutePath() + "_" + status;
				Log.info( getClass(), "Saving file: " + path );
				final File file = new File( path );
				final FileWriter writer = new FileWriter( file );
				writer.close();
			}
			else
			{
				final String msg = "Program status already set: " + statusIndicator.getAbsolutePath();
				Log.warn( getClass(), msg );
				throw new Exception( msg );
			}
		}
	}
}
