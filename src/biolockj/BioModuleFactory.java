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
import java.util.ArrayList;
import java.util.List;
import biolockj.module.BioModule;
import biolockj.module.classifier.r16s.RdpClassifier;
import biolockj.module.classifier.wgs.KrakenClassifier;
import biolockj.module.implicit.Demultiplexer;
import biolockj.module.implicit.ImportMetadata;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.module.seq.AwkFastaConverter;
import biolockj.module.seq.Gunzipper;
import biolockj.module.seq.PearMergeReads;
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
		final List<String> listedModules = Config.requireList( Config.INTERNAL_BLJ_MODULE );

		info( "Set required 1st module (for all pipelines): " + ImportMetadata.class.getName() );

		bioModules.add( getModule( ImportMetadata.class.getName() ) );

		if( Config.getBoolean( Config.INTERNAL_MULTIPLEXED ) )
		{
			info( "Set required 2nd module (for multiplexed data): " + Demultiplexer.class.getName() );
			bioModules.add( getModule( getDefaultDemultiplexer() ) );
		}

		if( Config.getBoolean( Config.REPORT_NUM_READS ) )
		{
			info( "Config property [ " + Config.REPORT_NUM_READS + "=" + Config.TRUE + " ] --> Adding module: "
					+ RegisterNumReads.class.getName() );
			bioModules.add( getModule( RegisterNumReads.class.getName() ) );
		}

		for( int i = 0; i < listedModules.size(); i++ )
		{
			final BioModule module = getModule( listedModules.get( i ) );

			if( bioModules.contains( module ) )
			{
				warn( module.getClass().getName()
						+ " is skipped where configured since it has already been added to the pipeline." );
			}
			else if( isImplicitModule( module.getClass().getName() ) && !Config.getBoolean( ALLOW_IMPLICIT_MODULES ) )
			{

				warn( "Ignoring configured module [" + module.getClass().getName()
						+ "] since implicit BioModules are added to the pipeline by the system if needed.  "
						+ "To override this behavior and ignore implicit designation, udpate project Config: ["
						+ ALLOW_IMPLICIT_MODULES + "=" + Config.TRUE + "]" );

			}
			else
			{
				safetyCount = 0;
				for( final String preReqModule: getPreRequisites( module ) )
				{
					if( !moduleInList( bioModules, preReqModule ) )
					{
						info( "Add pre-req module: " + preReqModule );
						bioModules.add( getModule( preReqModule ) );
					}
				}

				// Check in case one of the pre-reqs added the current module as a post-req
				if( !moduleInList( bioModules, module.getClass().getName() ) )
				{
					info( "Add module: " + module.getClass().getName() );
					bioModules.add( module );
				}

				safetyCount = 0;
				for( final String postReqModule: getPostRequisites( module ) )
				{
					if( !moduleInList( bioModules, postReqModule ) )
					{
						info( "Add post-req module: " + postReqModule );
						bioModules.add( getModule( postReqModule ) );
					}
				}
			}
		}

		final BioModule firstSeqProcMod = getFirstSeqProcessingModule( bioModules );
		if( firstSeqProcMod.getClass().getName().contains( "qiime" ) )
		{
			for( final File file: SeqUtil.getPipelineInputFiles() )
			{
				if( file.getName().toLowerCase().endsWith( ".gz" ) )
				{
					final List<BioModule> tempModules = bioModules;
					bioModules.clear();
					for( final BioModule mod: tempModules )
					{
						if( mod.getClass().getName().equals( firstSeqProcMod.getClass().getName() ) )
						{
							info( "Qiime does not accept .gz format, so adding pre-req module: "
									+ Gunzipper.class.getName() + " before " + firstSeqProcMod.getClass().getName() );
							bioModules.add( getModule( Gunzipper.class.getName() ) );
						}

						bioModules.add( mod );
					}

					break;
				}
			}
		}

		return bioModules;
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
		final List<Class<?>> modPostReqs = module.getPostRequisiteModules();
		if( modPostReqs != null )
		{
			for( final Class<?> modClass: modPostReqs )
			{
				final List<String> postReqPreReqModules = getPreRequisites( getModule( modClass.getName() ) );
				for( final String preReq: postReqPreReqModules )
				{
					if( !postReqs.contains( preReq ) )
					{
						postReqs.add( preReq );
					}
				}

				postReqs.add( modClass.getName() );

				final List<String> postReqPostReqModules = getPostRequisites( getModule( modClass.getName() ) );
				for( final String postReq: postReqPostReqModules )
				{
					if( !postReqs.contains( postReq ) )
					{
						postReqs.add( postReq );
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
		final List<Class<?>> modPreReqs = module.getPreRequisiteModules();
		if( modPreReqs != null )
		{
			for( final Class<?> modClass: modPreReqs )
			{
				final List<String> preReqPreReqModules = getPreRequisites( getModule( modClass.getName() ) );
				for( final String preReq: preReqPreReqModules )
				{
					if( !preReqs.contains( preReq ) )
					{
						preReqs.add( preReq );
					}
				}

				preReqs.add( modClass.getName() );

				final List<String> preReqPostReqModules = getPostRequisites( getModule( modClass.getName() ) );
				for( final String postReq: preReqPostReqModules )
				{
					if( !preReqs.contains( postReq ) )
					{
						preReqs.add( postReq );
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
		if( Class.forName( moduleName ).getPackage().getName()
				.startsWith( ImportMetadata.class.getPackage().getName() ) )
		{
			return true;
		}

		return false;
	}

	private static BioModule getFirstSeqProcessingModule( final List<BioModule> bioModules ) throws Exception
	{
		for( final BioModule module: bioModules )
		{
			if( module.getClass().getName().startsWith( PearMergeReads.class.getPackage().getName() )
					|| module.getClass().getName().startsWith( RdpClassifier.class.getPackage().getName() )
					|| module.getClass().getName().startsWith( KrakenClassifier.class.getPackage().getName() ) )
			{
				info( "Identify 1st sequence processing BioModule: " + module.getClass().getName() );
				return module;
			}
		}

		return null;
	}

	private static BioModule getModule( final String moduleName ) throws Exception
	{
		try
		{
			return (BioModule) Class.forName( moduleName ).newInstance();
		}
		catch( final Exception ex )
		{
			throw new Exception( "Module does not exist (check your spelling): " + moduleName );
		}
	}

	private static void info( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.get( Pipeline.class ).info( msg );
		}
	}

	private static boolean moduleInList( final List<BioModule> bioModules, final String moduleName )
	{
		for( final BioModule module: bioModules )
		{
			if( module.getClass().getName().equals( moduleName ) )
			{
				return true;
			}
		}
		return false;
	}

	private static void verifySafetyCount( final BioModule module ) throws Exception
	{
		if( safetyCount++ > 50 )
		{
			throw new Exception(
					"Circular Logic Error!  BioModule [ " + module.getClass().getName() + " ] has a pre-requisite "
							+ "module that refers back to the original module as a post-requisite (or vice versa)." );
		}
	}

	private static void warn( final String msg ) throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.get( Pipeline.class ).warn( msg );
		}
	}
	
	/**
	 * Get the Java Class name for the default Demultiplexer module
	 * @return Demultiplexer module Java class name
	 */
	public static String getDefaultDemultiplexer() 
	{
		if( Config.getString( DEFAULT_MOD_DEMUX ) != null )
		{
			return Config.getString( DEFAULT_MOD_DEMUX ); 
		}
		return Demultiplexer.class.getName() ;
	}
	
	/**
	 * Get the Java Class name for the default Fasta converter module
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
	 * {@link biolockj.Config} Boolean property: {@value #ALLOW_IMPLICIT_MODULES}<br>
	 * If set to {@value biolockj.Config#TRUE}, implicit modules can be directly configured. Otherwise, these modules
	 * can only by automatically added to pipelines by BioLockJ.
	 */
	protected static final String ALLOW_IMPLICIT_MODULES = "project.allowImplicitModules";
	
	/**
	 * {@link biolockj.Config} String property: Java class name for default module used to convert files into fasta
	 * format
	 */
	protected static final String DEFAULT_MOD_FASTA_CONV = "project.defaultModuleFastaConverter";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used combined paired read files
	 */
	protected static final String DEFAULT_MOD_SEQ_MERGER = "project.defaultModuleSeqMerger";


	/**
	 * {@link biolockj.Config} String property: Java class name for default module used to demultiplex data
	 */
	protected static final String DEFAULT_MOD_DEMUX = "project.defaultModuleDemultiplexer";

	private static int safetyCount = 0;
}
