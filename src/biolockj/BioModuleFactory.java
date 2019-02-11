/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org
 */
package biolockj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import biolockj.module.BioModule;
import biolockj.module.implicit.ImportMetadata;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.module.seq.*;
import biolockj.util.*;

/**
 * This class initializes pipeline modules, starting with those in the Config file and adding the prerequisite and
 * post-requisite modules.
 */
public class BioModuleFactory
{
	private BioModuleFactory() throws Exception
	{
		initModules();
	}

	/**
	 * This method returns all module post-requisites (including post-requisites of the post-requisites).
	 * 
	 * @param module Current BioModule
	 * @return List of post-requisite module names
	 * @throws Exception if runtime errors occur
	 */
	protected List<String> getPostRequisites( final BioModule module ) throws Exception
	{
		if( --safteyCheck == 0 )
		{
			throw new Exception( "Too many calls [" + SAFE_MAX + "] to getPostRequisites( module )" );
		}
		final List<String> postReqs = new ArrayList<>();
		for( final String postReq: module.getPostRequisiteModules() )
		{
			if( !postReqs.contains( postReq ) )
			{
				postReqs.add( postReq );
			}

			final List<String> postPostReqs = getPostRequisites( ModuleUtil.getModule( postReq ) );
			for( final String postPostReq: postPostReqs )
			{
				if( !postReqs.contains( postPostReq ) )
				{
					postReqs.add( postPostReq );
				}
			}
		}

		return postReqs;
	}

	/**
	 * This method returns all module prerequisites (including prerequisites of the prerequisites).
	 * 
	 * @param module Current BioModule
	 * @return List of prerequisite module names
	 * @throws Exception if runtime errors occur
	 */
	protected List<String> getPreRequisites( final BioModule module ) throws Exception
	{
		if( --safteyCheck == 0 )
		{
			throw new Exception( "Too many calls [" + SAFE_MAX + "] to getPreRequisites( module )" );
		}
		final List<String> preReqs = new ArrayList<>();
		for( final String preReq: module.getPreRequisiteModules() )
		{
			final List<String> prePreReqs = getPreRequisites( ModuleUtil.getModule( preReq ) );
			for( final String prePreReq: prePreReqs )
			{
				if( !preReqs.contains( prePreReq ) )
				{
					preReqs.add( prePreReq );
				}
			}

			if( !preReqs.contains( preReq ) )
			{
				preReqs.add( preReq );
			}
		}

		return preReqs;
	}

	private String addModule( final String className )
	{
		if( className.startsWith( Constants.MODULE_CLASSIFIER_PACKAGE ) )
		{
			branchClassifier = true;
		}

		return className;
	}

	/**
	 * The method returns the ordered list of BioModules required as part of the runtime {@link biolockj.Config}
	 * file.<br>
	 * Each line the begins with {@value biolockj.Config#INTERNAL_BLJ_MODULE} should be followed by the full class name
	 * of a Java class that implements the {@link biolockj.module.BioModule } interface.
	 * 
	 * @return List of BioModules
	 * @throws Exception thrown for invalid BioModules
	 */
	private List<BioModule> buildModules() throws Exception
	{
		final List<BioModule> bioModules = new ArrayList<>();
		for( final String className: moduleCache )
		{
			final BioModule module = (BioModule) Class.forName( className ).getDeclaredConstructor().newInstance();
			module.init();
			bioModules.add( module );
		}

		return bioModules;
	}

	/**
	 * Get the configured modules + implicit modules if configured.
	 */
	private List<String> getConfigModules() throws Exception
	{
		final List<String> configModules = Config.requireList( null, Constants.INTERNAL_BLJ_MODULE );
		final List<String> modules = new ArrayList<>();
		if( !Config.getBoolean( null, Constants.DISABLE_ADD_IMPLICIT_MODULES ) )
		{
			Log.info( getClass(), "Set required 1st module (for all pipelines): " + ImportMetadata.class.getName() );
			configModules.remove( ImportMetadata.class.getName() );
			modules.add( ImportMetadata.class.getName() );

			if( Config.getBoolean( null, SeqUtil.INTERNAL_MULTIPLEXED ) )
			{
				Log.info( getClass(),
						"Set required 2nd module (for multiplexed data): " + ModuleUtil.getDefaultDemultiplexer() );
				configModules.remove( ModuleUtil.getDefaultDemultiplexer() );
				modules.add( ModuleUtil.getDefaultDemultiplexer() );
			}

			if( Config.getBoolean( null, SeqUtil.INTERNAL_IS_MULTI_LINE_SEQ ) )
			{
				Log.info( getClass(), "Set required module (for multi seq-line fasta files ): "
						+ ModuleUtil.getDefaultFastaConverter() );
				configModules.remove( ModuleUtil.getDefaultFastaConverter() );
				modules.add( ModuleUtil.getDefaultFastaConverter() );
			}
		}

		for( final String module: configModules )
		{
			if( isImplicitModule( module ) && !Config.getBoolean( null, Constants.DISABLE_ADD_IMPLICIT_MODULES ) )
			{
				warn( "Ignoring configured module [" + module
						+ "] since implicit BioModules are added to the pipeline by the system if needed.  "
						+ "To override this behavior and ignore implicit designation, udpate project Config: ["
						+ Constants.DISABLE_ADD_IMPLICIT_MODULES + "=" + Constants.TRUE + "]" );
			}
			else
			{
				modules.add( module );
			}
		}

		return modules;
	}

	private int getCountModIndex() throws Exception
	{
		int i = -1;
		final boolean addMod = requireCountMod();
		if( addMod )
		{

			if( moduleCache.size() == 1 || !moduleCache.get( 1 ).equals( ModuleUtil.getDefaultDemultiplexer() ) )
			{
				i = 1;
			}
			else if( moduleCache.get( 1 ).equals( ModuleUtil.getDefaultDemultiplexer() ) )
			{
				i = 2;
			}
		}

		Log.debug( getClass(), addMod ? "ADD count module at index: " + i: "No need to add count mdoule" );

		return i;
	}

	private List<String> getCountModules()
	{
		final List<String> mods = new ArrayList<>();
		mods.add( RegisterNumReads.class.getName() );
		mods.add( SeqFileValidator.class.getName() );
		mods.add( TrimPrimers.class.getName() );
		mods.add( PearMergeReads.class.getName() );
		return mods;

	}

	private boolean hasGzippedInput() throws Exception
	{
		return !BioLockJUtil.getPipelineInputFiles().isEmpty()
				&& SeqUtil.isGzipped( BioLockJUtil.getPipelineInputFiles().iterator().next().getName() );
	}

	private void info( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( BioModuleFactory.class, msg );
		}
	}

	/**
	 * Register the complete list of Java class.getSimpleName() values for the configured modules.
	 * 
	 * @throws Exception
	 */
	private void initModules() throws Exception
	{
		final List<String> configModules = getConfigModules();
		List<String> branchModules = new ArrayList<>();

		for( final String className: configModules )
		{
			safteyCheck = SAFE_MAX;
			final BioModule module = ModuleUtil.getModule( className );
			if( !Config.getBoolean( null, Constants.DISABLE_PRE_REQ_MODULES ) )
			{
				for( final String mod: getPreRequisites( module ) )
				{
					if( !branchModules.contains( mod ) )
					{
						branchModules.add( addModule( mod ) );
					}
				}
			}

			if( !branchModules.contains( className ) )
			{
				branchModules.add( addModule( className ) );
			}

			safteyCheck = SAFE_MAX;
			if( !module.getPostRequisiteModules().isEmpty() )
			{
				for( final String mod: getPostRequisites( module ) )
				{
					if( !branchModules.contains( mod ) )
					{
						branchModules.add( addModule( mod ) );
					}
				}
			}

			if( foundClassifier && branchClassifier )
			{
				Log.info( getClass(), "Found another classifier: reset branch" );
				branchClassifier = false;
				foundClassifier = false;
				moduleCache.addAll( branchModules );
				branchModules = new ArrayList<>();
			}
			else if( branchClassifier )
			{
				foundClassifier = true;
				branchClassifier = false;
			}

		}

		if( !branchModules.isEmpty() )
		{
			moduleCache.addAll( branchModules );
		}

		insertConditionalModules();
	}

	private void insertConditionalModules() throws Exception
	{
		final List<String> finalModules = new ArrayList<>();
		final int i = getCountModIndex();
		for( final String module: moduleCache )
		{
			if( finalModules.size() == i )
			{
				finalModules.add( SeqFileValidator.class.getName() );
				info( "Config property [ " + Constants.REPORT_NUM_READS + "=" + Constants.TRUE + " ] & [ "
						+ SeqUtil.INTERNAL_SEQ_TYPE + "=" + Config.requireString( null, SeqUtil.INTERNAL_SEQ_TYPE )
						+ " ] --> Adding module: " + SeqFileValidator.class.getName() );
			}
			if( requireGunzip( module ) )
			{
				info( "Qiime does not accept \"" + Constants.GZIP_EXT + "\" format, so adding required pre-req module: "
						+ Gunzipper.class.getName() + " before " + module );

				foundSeqMod = true;
				finalModules.add( Gunzipper.class.getName() );
			}
			else if( isSeqProcessingModule( module ) )
			{
				foundSeqMod = true;
			}

			finalModules.add( module );
		}

		moduleCache = finalModules;
	}

	private boolean isImplicitModule( final String moduleName ) throws ClassNotFoundException
	{
		return moduleName.startsWith( Constants.MODULE_IMPLICIT_PACKAGE );
	}

	/**
	 * Check if module belongs to a package that processes sequences
	 */
	private boolean isSeqProcessingModule( final String name )
	{
		return name.startsWith( Constants.MODULE_SEQ_PACKAGE )
				|| name.startsWith( Constants.MODULE_CLASSIFIER_PACKAGE );
	}

	private boolean requireCountMod() throws Exception
	{
		return !foundCountMod && Collections.disjoint( moduleCache, getCountModules() )
				&& Config.getBoolean( null, Constants.REPORT_NUM_READS ) && SeqUtil.piplineHasSeqInput();
	}

	private boolean requireGunzip( final String module ) throws Exception
	{
		return !foundSeqMod && hasGzippedInput() && isSeqProcessingModule( module )
				&& module.toLowerCase().contains( "qiime" );
	}

	private void warn( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.warn( NextFlowUtil.class, msg );
		}
	}

	/**
	 * Build all modules for the pipeline.
	 * 
	 * @return List of BioModules
	 * @throws Exception if errors occur
	 */
	public static List<BioModule> buildPipeline() throws Exception
	{
		if( factory == null )
		{
			initFactory();
		}

		return factory.buildModules();
	}

	public static List<String> geModuleList()
	{
		return factory.moduleCache;
	}

	/**
	 * Register the complete list of modules to run.
	 * 
	 * @return List of Module Java class names
	 * @throws Exception if errors occur
	 */
	public static void initFactory() throws Exception
	{
		factory = new BioModuleFactory();
	}

	private boolean branchClassifier = false;
	private boolean foundClassifier = false;
	private final boolean foundCountMod = false;
	private boolean foundSeqMod = false;
	private List<String> moduleCache = new ArrayList<>();
	private int safteyCheck = 0;
	private static BioModuleFactory factory = null;
	private static final int SAFE_MAX = 10;
}
