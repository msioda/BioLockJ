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

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import biolockj.module.BioModule;
import biolockj.module.SeqModule;
import biolockj.module.classifier.r16s.RdpClassifier;
import biolockj.module.implicit.*;
import biolockj.module.r.CalculateStats;
import biolockj.module.seq.*;
import biolockj.util.BioLockJUtil;
import biolockj.util.RuntimeParamUtil;
import biolockj.util.SeqUtil;

/**
 * This class initializes pipeline modules, startign with those in the Config file and adding the prerequisite and
 * post-requisite modules.
 */
public class BioModuleFactory
{

	/**
	 * The method returns the ordered list of BioModules required as part of the runtime {@link biolockj.Config}
	 * file.<br>
	 * Each line the begins with {@value biolockj.Config#INTERNAL_BLJ_MODULE} should be followed by the full class name
	 * of a Java class that implements the {@link biolockj.module.BioModule } interface.
	 * 
	 * @return List of BioModules
	 * @throws Exception thrown for invalid BioModules
	 */
	public static List<BioModule> buildModules() throws Exception
	{
		final List<BioModule> bioModules = new ArrayList<>();
		final List<String> configModules = Config.requireList( Config.INTERNAL_BLJ_MODULE );

		if( !Config.getBoolean( Config.ALLOW_IMPLICIT_MODULES ) )
		{
			info( "Set required 1st module (for all pipelines): " + ImportMetadata.class.getName() );
			bioModules.addAll( constructModule( ImportMetadata.class.getName() ) );
			configModules.remove( ImportMetadata.class.getName() );

			if( Config.getBoolean( SeqUtil.INTERNAL_MULTIPLEXED ) )
			{
				info( "Set required 2nd module (for multiplexed data): " + getDefaultDemultiplexer() );
				bioModules.addAll( constructModule( getDefaultDemultiplexer() ) );
				configModules.remove( getDefaultDemultiplexer() );
			}

			if( Config.getBoolean( SeqUtil.INTERNAL_IS_MULTI_LINE_SEQ ) )
			{
				bioModules.addAll( constructModule( getDefaultFastaConverter() ) );
				configModules.remove( getDefaultFastaConverter() );
			}

			if( SeqUtil.piplineHasSeqInput() && Config.getBoolean( Config.REPORT_NUM_READS )
					&& !Config.getBoolean( SeqUtil.INTERNAL_PAIRED_READS )
					&& !configModules.contains( SeqFileValidator.class.getName() )
					&& !configModules.contains( TrimPrimers.class.getName() ) )
			{
				info( "Config property [ " + Config.REPORT_NUM_READS + "=" + Config.TRUE + " ] & [ "
						+ SeqUtil.INTERNAL_SEQ_TYPE + "=" + Config.requireString( SeqUtil.INTERNAL_SEQ_TYPE )
						+ " ] --> Adding module: " + RegisterNumReads.class.getName() );

				bioModules.addAll( constructModule( RegisterNumReads.class.getName() ) );
				configModules.remove( RegisterNumReads.class.getName() );
			}
		}

		for( int i = 0; i < configModules.size(); i++ )
		{
			final String moduleName = configModules.get( i );
			final BioModule module = getModule( moduleName );

			if( isImplicitModule( moduleName ) && !Config.getBoolean( Config.ALLOW_IMPLICIT_MODULES ) )
			{
				warn( "Ignoring configured module [" + moduleName
						+ "] since implicit BioModules are added to the pipeline by the system if needed.  "
						+ "To override this behavior and ignore implicit designation, udpate project Config: ["
						+ Config.ALLOW_IMPLICIT_MODULES + "=" + Config.TRUE + "]" );
			}
			else
			{
				List<String> preReqs = new ArrayList<>();
				if( !Config.getBoolean( DISABLE_PRE_REQ_MODULES ) )
				{
					safetyCount = 0;
					preReqs = getPreRequisites( module );
					preReqs.removeAll( moduleMap.values() );
					for( final String preReqModule: preReqs )
					{
						info( "Add pre-req module: " + preReqModule );
						bioModules.addAll( constructModule( preReqModule ) );
					}
				}

				// Check in case one of the pre-reqs added the current module as a post-req
				if( !preReqs.contains( moduleName ) )
				{
					info( "Add module: " + moduleName );
					bioModules.addAll( constructModule( moduleName ) );
				}

				safetyCount = 0;
				for( final String postReqModule: getPostRequisites( module ) )
				{
					if( !moduleMap.values().contains( postReqModule ) )
					{
						info( "Add post-req module: " + postReqModule );
						bioModules.addAll( constructModule( postReqModule ) );
					}
				}
			}
		}

		moduleMap = null;
		return bioModules;
	}

	/**
	 * Get the Java Class name for the default Demultiplexer module
	 * 
	 * @return Demultiplexer module Java class name
	 */
	public static String getDefaultDemultiplexer()
	{
		if( Config.getString( DEFAULT_MOD_DEMUX ) != null )
		{
			return Config.getString( DEFAULT_MOD_DEMUX );
		}
		return Demultiplexer.class.getName();
	}

	/**
	 * Get the Java Class name for the default Fasta converter module
	 * 
	 * @return Fasta converter module Java class name
	 */
	public static String getDefaultFastaConverter()
	{
		if( Config.getString( DEFAULT_MOD_FASTA_CONV ) != null )
		{
			return Config.getString( DEFAULT_MOD_FASTA_CONV );
		}
		return AwkFastaConverter.class.getName();
	}

	/**
	 * Get the Java Class name for the default Merge paired read module
	 * 
	 * @return Merge paired read module Java class name
	 */
	public static String getDefaultMergePairedReadsConverter()
	{
		if( Config.getString( DEFAULT_MOD_SEQ_MERGER ) != null )
		{
			return Config.getString( DEFAULT_MOD_SEQ_MERGER );
		}
		return PearMergeReads.class.getName();
	}

	/**
	 * Get the Java Class name for the default Merge paired read module
	 * 
	 * @return Merge paired read module Java class name
	 */
	public static String getDefaultStatsModule()
	{
		if( Config.getString( DEFAULT_STATS_MODULE ) != null )
		{
			return Config.getString( DEFAULT_STATS_MODULE );
		}
		return CalculateStats.class.getName();
	}

	/**
	 * This method returns all module post-requisites (including prerequisites and post-requisites for the
	 * post-requisites).
	 * 
	 * @param module Current BioModule
	 * @return List of post-requisite module names
	 * @throws Exception if runtime errors occur
	 */
	protected static List<String> getPostRequisites( final BioModule module ) throws Exception
	{
		verifySafetyCount( module );
		final List<String> postReqs = new ArrayList<>();
		final List<String> modPostReqs = module.getPostRequisiteModules();
		if( modPostReqs != null )
		{
			for( final String postReq: modPostReqs )
			{
				final List<String> prePostReqs = getPreRequisites( getModule( postReq ) );
				for( final String preReq: prePostReqs )
				{
					if( !postReqs.contains( preReq ) )
					{
						postReqs.add( preReq );
					}
				}

				postReqs.add( postReq );

				final List<String> postPostReqMs = getPostRequisites( getModule( postReq ) );
				for( final String postPostReq: postPostReqMs )
				{
					if( !postReqs.contains( postPostReq ) )
					{
						postReqs.add( postPostReq );
					}
				}
			}
		}

		return postReqs;
	}

	/**
	 * This method returns all module prerequisites (including prerequisites and post-requisites for the prerequisites).
	 * 
	 * @param module Current BioModule
	 * @return List of prerequisite module names
	 * @throws Exception if runtime errors occur
	 */
	protected static List<String> getPreRequisites( final BioModule module ) throws Exception
	{
		verifySafetyCount( module );
		final List<String> preReqs = new ArrayList<>();
		final List<String> modPreReqs = module.getPreRequisiteModules();
		if( modPreReqs != null )
		{
			for( final String preReq: modPreReqs )
			{
				final List<String> prePreReqs = getPreRequisites( getModule( preReq ) );
				for( final String prePreReq: prePreReqs )
				{
					if( !preReqs.contains( prePreReq ) )
					{
						preReqs.add( prePreReq );
					}
				}

				preReqs.add( preReq );

				final List<String> postPreReqs = getPostRequisites( getModule( preReq ) );
				for( final String postPreReq: postPreReqs )
				{
					if( !preReqs.contains( postPreReq ) )
					{
						preReqs.add( postPreReq );
					}
				}
			}
		}

		return preReqs;
	}

	/**
	 * This method returns TRUE if in given module name belongs to the {@link biolockj.module.implicit} package or one
	 * of its sub-packages.
	 * 
	 * @param moduleName BioModule full Java class name
	 * @return boolean TRUE if in the {@link biolockj.module.implicit}
	 * @throws ClassNotFoundException if the moduleName is not a valid class name
	 */
	protected static boolean isImplicitModule( final String moduleName ) throws ClassNotFoundException
	{
		return Class.forName( moduleName ).getPackage().getName()
				.startsWith( ImportMetadata.class.getPackage().getName() );
	}

	private static List<BioModule> constructModule( final String moduleName ) throws Exception
	{
		final List<BioModule> bioModules = new ArrayList<>();
		try
		{
			int id = moduleMap.size();
			final Constructor<?> constructor = Class.forName( moduleName ).getDeclaredConstructor();

			if( !foundFirstModuleToProcessSeqs && isSeqProcessingModule( moduleName ) && SeqUtil.piplineHasSeqInput() )
			{
				foundFirstModuleToProcessSeqs = true;
				final boolean isQiime = moduleName.toLowerCase().contains( "qiime" );
				final File file = BioLockJUtil.getPipelineInputFiles().iterator().next();
				if( isQiime && SeqUtil.isGzipped( file.getName() ) )
				{
					info( "Qiime does not accept \"" + BioLockJ.GZIP_EXT
							+ "\" format, so adding required pre-req module: " + Gunzipper.class.getName() + " before "
							+ moduleName );

					final String gz = Gunzipper.class.getName();
					final BioModule mod = (BioModule) constructor.newInstance();
					mod.init();
					bioModules.add( mod );
					moduleMap.put( id, gz );
					id = moduleMap.size();
				}
			}

			final BioModule mod = (BioModule) constructor.newInstance();
			mod.init();

			if( mod instanceof SeqModule )
			{
				foundFirstModuleToProcessSeqs = true;
			}

			bioModules.add( mod );
			moduleMap.put( id, moduleName );
		}
		catch( final Exception ex )
		{
			throw new Exception(
					"Module does not exist!  Check your spelling & verify module hasn't moved to a new package: "
							+ moduleName );
		}
		return bioModules;
	}

	private static BioModule getModule( final String moduleName ) throws Exception
	{
		return (BioModule) Class.forName( moduleName ).newInstance();
	}

	private static void info( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( Pipeline.class, msg );
		}
	}

	private static boolean isSeqProcessingModule( final String name ) throws Exception
	{
		final String seqPackage = PearMergeReads.class.getPackage().getName();
		final String classifierPackage = RdpClassifier.class.getPackage().getName().replaceAll( "r16s", "" );
		return name.startsWith( seqPackage ) || name.startsWith( classifierPackage );
	}

	private static void verifySafetyCount( final BioModule module ) throws Exception
	{
		if( safetyCount++ > 50 )
		{
			throw new Exception(
					"Circular Logic Error!  BioModule [ " + module.getClass().getName() + " ] has a prerequisitee "
							+ "module that refers back to the original module as a post-requisite (or vice versa)." );
		}
	}

	private static void warn( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.warn( Pipeline.class, msg );
		}
	}

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used to demultiplex data:
	 * {@value #DEFAULT_MOD_DEMUX}
	 */
	protected static final String DEFAULT_MOD_DEMUX = "project.defaultModuleDemultiplexer";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used to convert files into fasta:
	 * {@value #DEFAULT_MOD_FASTA_CONV} format
	 */
	protected static final String DEFAULT_MOD_FASTA_CONV = "project.defaultModuleFastaConverter";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used combined paired read files:
	 * {@value #DEFAULT_MOD_SEQ_MERGER}
	 */
	protected static final String DEFAULT_MOD_SEQ_MERGER = "project.defaultModuleSeqMerger";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used generate p-value and other
	 * stats: {@value #DEFAULT_STATS_MODULE}
	 */
	protected static final String DEFAULT_STATS_MODULE = "project.defaultStatsModule";


	/**
	 * {@link biolockj.Config} Boolean property: {@value #DISABLE_PRE_REQ_MODULES}<br>
	 * If set to {@value biolockj.Config#TRUE}, prerequisite modules will not be added to the pipeline.
	 */
	protected static final String DISABLE_PRE_REQ_MODULES = "project.disablePreReqModules";

	private static boolean foundFirstModuleToProcessSeqs = false;
	private static Map<Integer, String> moduleMap = new HashMap<>();
	private static int safetyCount = 0;
}
