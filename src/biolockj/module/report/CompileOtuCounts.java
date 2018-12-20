package biolockj.module.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.util.ModuleUtil;
import biolockj.util.OtuUtil;

public class CompileOtuCounts extends JavaModuleImpl implements JavaModule
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
	 * Input files are the output generated by the pipeline {@link biolockj.module.implicit.parser.ParserModule}
	 */
	@Override
	public List<File> getInputFiles() throws Exception
	{
		final IOFileFilter ff = new WildcardFileFilter(
				Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + "_" + OtuUtil.OTU_COUNT + "_*" );
		final List<File> files = new ArrayList<>(
				FileUtils.listFiles( ModuleUtil.getParserModule().getOutputDir(), ff, null ) );
		if( !files.isEmpty() )
		{
			Collections.sort( files );
		}
		return files;
	}

	/**
	 * Return the summary OTU count file.
	 * 
	 * @return File OTU summary count file
	 * @throws Exception if errors occur
	 */
	public File getSummaryCountFile() throws Exception
	{
		return new File( getOutputDir().getAbsolutePath() + File.separator
				+ Config.requireString( Config.INTERNAL_PIPELINE_NAME ) + "_" + SUMMARY_OTU_COUNT + TSV );
	}

	@Override
	public void runModule() throws Exception
	{
		buildSummaryOtuCountFile( compileOtuCounts( getInputFiles() ) );
	}

	/**
	 * Build Summary OTU count file for all samples.
	 * 
	 * @param otuCounts OTU-count mapping
	 * @throws Exception if errors occur
	 */
	protected void buildSummaryOtuCountFile( final Map<String, Integer> otuCounts ) throws Exception
	{
		Log.info( getClass(), "Build " + getSummaryCountFile() + " from " + otuCounts.size() + " unqiue OTU strings" );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getSummaryCountFile() ) );
		try
		{
			for( final String otu: otuCounts.keySet() )
			{
				writer.write( otu + TAB_DELIM + otuCounts.get( otu ) );
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

	/**
	 * Compile OTU counts from the individual sample OTU count files
	 * 
	 * @param files Collection of OTU count files
	 * @return Map<OTU, count>
	 * @throws Exception if errors occur
	 */
	public static Map<String, Integer> compileOtuCounts( final Collection<File> files ) throws Exception
	{
		final Map<String, Integer> combinedOtuCounts = new TreeMap<>();

		for( final File file: files )
		{
			final Map<String, Integer> otuCounts = OtuUtil.compileSampleOtuCounts( file );
			for( final String otu: otuCounts.keySet() )
			{
				final Integer count = otuCounts.get( otu );

				if( !combinedOtuCounts.keySet().contains( otu ) )
				{
					combinedOtuCounts.put( otu, 0 );
				}

				combinedOtuCounts.put( otu, combinedOtuCounts.get( otu ) + count );
			}

		}
		return combinedOtuCounts;
	}

	/**
	 * Included in the file name of the summary output file.
	 */
	public static final String SUMMARY_OTU_COUNT = "summaryOtuCount";
}