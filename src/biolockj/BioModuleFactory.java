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

import java.util.*;
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
		final List<String> configModules = Config.requireList( Config.INTERNAL_BLJ_MODULE );
		final List<String> modules = new ArrayList<>();
		if( !Config.getBoolean( Constants.DISABLE_ADD_IMPLICIT_MODULES ) )
		{
			Log.info( getClass(), "Set required 1st module (for all pipelines): " + ImportMetadata.class.getName() );
			configModules.remove( ImportMetadata.class.getName() );
			modules.add( ImportMetadata.class.getName() );

			if( Config.getBoolean( SeqUtil.INTERNAL_MULTIPLEXED ) )
			{
				Log.info( getClass(),
						"Set required 2nd module (for multiplexed data): " + ModuleUtil.getDefaultDemultiplexer() );
				configModules.remove( ModuleUtil.getDefaultDemultiplexer() );
				modules.add( ModuleUtil.getDefaultDemultiplexer() );
			}

			if( Config.getBoolean( SeqUtil.INTERNAL_IS_MULTI_LINE_SEQ ) )
			{
				Log.info( getClass(), "Set required module (for multi seq-line fasta files ): "
						+ ModuleUtil.getDefaultFastaConverter() );
				configModules.remove( ModuleUtil.getDefaultFastaConverter() );
				modules.add( ModuleUtil.getDefaultFastaConverter() );
			}
		}

		for( final String module: configModules )
		{
			if( isImplicitModule( module ) && !Config.getBoolean( Constants.DISABLE_ADD_IMPLICIT_MODULES ) )
			{
				warn( "Ignoring configured module [" + module
						+ "] since implicit BioModules are added to the pipeline by the system if needed.  "
						+ "To override this behavior and ignore implicit designation, udpate project Config: ["
						+ Constants.DISABLE_ADD_IMPLICIT_MODULES + "=" + Config.TRUE + "]" );
			}
			else
			{
				modules.add( module );
			}
		}

		return modules;
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
			BioModule module = getModule( className );
			if( !Config.getBoolean( Constants.DISABLE_PRE_REQ_MODULES )  )
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

			if( !module.getPostRequisiteModules().isEmpty() )
			{
				for( final String mod: module.getPostRequisiteModules() )
				{
					if( !branchModules.contains( mod ) )
					{
						branchModules.add( addModule( mod ) );
					}
				}
			}
 //foundClassifier && 
			
			if( foundClassifier && branchClassifier )
			{
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
	
	private String addModule( String className )
	{
		if( className.startsWith( Constants.MODULE_CLASSIFIER_PACKAGE ) )
		{
			branchClassifier = true;
		}
		
		return className;
	}


	
	/**
	 * This method returns all module prerequisites (including prerequisites and post-requisites for the prerequisites).
	 * 
	 * @param module Current BioModule
	 * @return List of prerequisite module names
	 * @throws Exception if runtime errors occur
	 */
	protected List<String> getPreRequisites( final BioModule module ) throws Exception
	{
		if( --safteyCheck == 0 )
		{
			throw new Exception( "Too many calls ["+SAFE_MAX+"] to getPreRequisites( module )" );
		}
		final List<String> preReqs = new ArrayList<>();
		for( final String preReq: module.getPreRequisiteModules() )
		{
			final List<String> prePreReqs = getPreRequisites( getModule( preReq ) );
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
	

	private boolean requireCountMod() throws Exception
	{
		return !foundCountMod && Collections.disjoint( moduleCache, getCountModules() ) 
				&& Config.getBoolean( Config.REPORT_NUM_READS ) && SeqUtil.piplineHasSeqInput();
	}
	
	
	private int getCountModIndex() throws Exception
	{
		int i = -1;
		boolean addMod = requireCountMod();
		if( addMod )
		{
			
			if( ( moduleCache.size() == 1 ) || !moduleCache.get( 1 ).equals( ModuleUtil.getDefaultDemultiplexer() ) )
			{
				i = 1;
			}
			else if( moduleCache.get( 1 ).equals( ModuleUtil.getDefaultDemultiplexer() ) )
			{
				i = 2;
			}
		}
		
		Log.debug( getClass(), addMod ? "ADD count module at index: " + i : "No need to add count mdoule"  );
		
		return i;
	}

	private void insertConditionalModules() throws Exception
	{
		final List<String> finalModules = new ArrayList<>();
		int i = getCountModIndex();
		for( final String module: moduleCache )
		{
			if( finalModules.size() == i )
			{
				finalModules.add( SeqFileValidator.class.getName() );
				info( "Config property [ " + Config.REPORT_NUM_READS + "=" + Config.TRUE + " ] & [ "
						+ SeqUtil.INTERNAL_SEQ_TYPE + "=" + Config.requireString( SeqUtil.INTERNAL_SEQ_TYPE )
						+ " ] --> Adding module: " + SeqFileValidator.class.getName() );
			}
			if( requireGunzip( module ) )
			{
				info( "Qiime does not accept \"" + Constants.GZIP_EXT + "\" format, so adding required pre-req module: "
						+ Gunzipper.class.getName() + " before " + module );

				foundSeqMod = true;
				finalModules.add( Gunzipper.class.getName() );
			}
			else if( isSeqProcessingModule( module )  )
			{
				foundSeqMod = true;
			}

			finalModules.add( module );
		}

		moduleCache = finalModules;
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

	private boolean requireGunzip( final String module ) throws Exception
	{
		return !foundSeqMod && hasGzippedInput() && isSeqProcessingModule( module ) && module.toLowerCase().contains( "qiime" );
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
			registerModuleList();
		}
		final List<BioModule> modules = factory.buildModules();
		destroy();
		return modules;
	}

	/**
	 * Destroy the factory
	 */
	public static void destroy()
	{
		factory = null;
	}

	/**
	 * Register the complete list of modules to run.
	 * 
	 * @return List of Module Java class names
	 * @throws Exception if errors occur
	 */
	public static List<String> registerModuleList() throws Exception
	{
		factory = new BioModuleFactory();
		return factory.getModuleCache();
	}

	private void warn( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.warn( NextFlowUtil.class, msg );
		}
	}
	
	private BioModule getModule( final String moduleName ) throws Exception
	{
		return (BioModule) Class.forName( moduleName ).newInstance();
	}
	
	public List<String> getModuleCache()
	{
		return moduleCache;
	}
	
	private static final int SAFE_MAX = 10;
	private int safteyCheck = 0;
	private boolean foundClassifier = false;
	private boolean branchClassifier = false;
	private static BioModuleFactory factory = null;
	private boolean foundSeqMod = false;
	private boolean foundCountMod = false;
	private List<String> moduleCache = new ArrayList<>();
}
