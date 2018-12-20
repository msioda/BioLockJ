package biolockj.module.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.util.OtuUtil;

/**
 * This BioModule compiles the counts from all OTU count files into a single summary OTU count file containing OTU
 * counts for the entire dataset.
 */
public class CompileOtuCounts extends JavaModuleImpl implements JavaModule
{

	@Override
	public boolean isValidInputModule( final BioModule previousModule ) throws Exception
	{
		return OtuUtil.outputHasOtuCountFiles( previousModule );
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
		final File otuCountFile = OtuUtil.getOtuCountFile( getOutputDir(), null, SUMMARY );
		Log.info( getClass(),
				"Build " + otuCountFile.getAbsolutePath() + " from " + otuCounts.size() + " unqiue OTU strings" );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( otuCountFile ) );
		try
		{
			for( final String otu: otuCounts.keySet() )
			{
				writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
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
	 * Compile OTU counts from the individual sample OTU count files
	 * 
	 * @param files Collection of OTU count files
	 * @return Map(OTU, count)
	 * @throws Exception if errors occur
	 */
	protected Map<String, Integer> compileOtuCounts( final Collection<File> files ) throws Exception
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
	private static final String SUMMARY = "summary";
}
