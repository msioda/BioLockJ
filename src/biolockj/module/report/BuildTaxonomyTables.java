package biolockj.module.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.util.MetaUtil;
import biolockj.util.OtuUtil;

public class BuildTaxonomyTables extends JavaModuleImpl implements JavaModule
{

	/**
	 * Execute {@link #validateModuleOrder()} to validate module configuration order.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		validateModuleOrder();
	}

	/**
	 * Return the taxonomy table file for the given level.
	 * 
	 * @param level {@link biolockj.Config.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}
	 * @return Taxonomy table file
	 * @throws Exception if errors occur building the File object
	 */
	public File getTaxonomyTable( final String level ) throws Exception
	{
		return new File( getOutputDir().getAbsolutePath() + File.separator
				+ Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + "_" + level + TSV );
	}

	@Override
	public void runModule() throws Exception
	{
		buildTaxonomyTables( OtuUtil.getSampleOtuCounts( getInputFiles() ) );
	}

	/**
	 * Build taxonomy tables from the sampleTaxaCounts.
	 * 
	 * @param sampleOtuCounts Map(SampleId, Map(OTU, count)) OTU counts for every sample
	 * @throws Exception if errors occur
	 */
	protected void buildTaxonomyTables( final Map<String, Map<String, Integer>> sampleOtuCounts ) throws Exception
	{
		final List<String> otus = OtuUtil.findUniqueOtus( sampleOtuCounts );

		report( "Print Sample OTU Counts", sampleOtuCounts );
		report( "Print Unique OTUs", otus );

		for( final String level: Config.requireList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			final List<String> levelTaxa = OtuUtil.findUniqueTaxa( otus, level );
			final TreeMap<String, Map<String, Integer>> levelTaxaCounts = OtuUtil.getLevelTaxaCounts( sampleOtuCounts,
					level );

			report( "Taxonomy Counts @" + level, levelTaxaCounts );

			Log.info( getClass(), "Building: " + getTaxonomyTable( level ).getAbsolutePath() );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( getTaxonomyTable( level ) ) );
			try
			{
				writer.write( MetaUtil.getID() );
				for( final String taxa: levelTaxa )
				{
					writer.write( TAB_DELIM + taxa );
				}
				writer.write( RETURN );

				for( final String sampleId: sampleOtuCounts.keySet() )
				{
					final Map<String, Integer> taxaCounts = levelTaxaCounts.get( sampleId );
					writer.write( sampleId );

					for( final String taxa: levelTaxa )
					{
						Integer count = 0;
						if( taxaCounts != null && taxaCounts.keySet().contains( taxa ) )
						{
							count = taxaCounts.get( taxa );
						}

						writer.write( TAB_DELIM + count );
						Log.debug( getClass(), sampleId + ":" + taxa + "=" + count );
					}

					writer.write( RETURN );
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
	}

	/**
	 * Validate {@link biolockj.module.implicit.parser.ParserModule} runs before this module.
	 * 
	 * @throws Exception if modules are out of order
	 */
	protected void validateModuleOrder() throws Exception
	{
		boolean foundSelf = false;
		boolean foundParser = false;
		for( final BioModule module: Pipeline.getModules() )
		{
			if( module.getClass().equals( getClass() ) )
			{
				foundSelf = true;
			}

			if( !foundParser )
			{
				foundParser = module instanceof ParserModule;
			}

			if( !foundParser && foundSelf )
			{
				throw new Exception( "ParserModule must run prior to " + getClass().getName() );
			}
		}
	}

	private void report( final String label, final Collection<String> col ) throws Exception
	{
		Log.warn( getClass(), label );
		for( final String item: col )
		{
			Log.warn( getClass(), item );
		}
	}

	private void report( final String label, final Map<String, Map<String, Integer>> map ) throws Exception
	{
		Log.warn( getClass(), label );
		for( final String id: map.keySet() )
		{
			final Map<String, Integer> innerMap = map.get( id );
			for( final String otu: innerMap.keySet() )
			{
				Log.warn( getClass(), id + ": " + otu + "=" + innerMap.get( otu ) );
			}
		}
	}

}