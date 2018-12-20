package biolockj.module.report;

/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
import java.io.*;
import java.util.*;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.*;

/**
 * This BioModule removes scarce OTUs not found in enough samples.<br>
 * The OTU must be found in a configurable percentage of samples.
 */
public class RemoveScarceOtus extends JavaModuleImpl implements JavaModule
{

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.requirePositiveDouble( MIN_OTU_THRESHOLD );
	}

	/**
	 * Set the number of hits field.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		ParserModuleImpl.getModule().setNumHitsFieldName( getMetaColName() );
	}

	@Override
	public boolean isValidInputModule( final BioModule previousModule ) throws Exception
	{
		return OtuUtil.outputhasOtuCountFiles( previousModule );
	}

	@Override
	public void runModule() throws Exception
	{
		Log.info( getClass(), "Searching samples to remove OTUs found in less than " + getCutoff() + " samples." );
		final Map<String, Map<String, Integer>> sampleOtuCounts = OtuUtil.getSampleOtuCounts( getInputFiles() );

		final List<String> uniqueOtus = OtuUtil.findUniqueOtus( sampleOtuCounts );
		Log.info( getClass(),
				"Searching " + uniqueOtus.size() + " unique OTUs in " + sampleOtuCounts.size()
						+ " samples for OTUs found in less than the cutoff percentage [ "
						+ Config.requirePositiveDouble( MIN_OTU_THRESHOLD ) + " ] = " + getCutoff() + " samples." );

		final Map<String, Set<String>> scarceTaxa = findScarceTaxa( sampleOtuCounts, uniqueOtus );
		final Map<String, Set<String>> scarceOtus = findScarceOtus( sampleOtuCounts, uniqueOtus, scarceTaxa );
		logScarceOtus( scarceOtus.keySet() );
		removeScarceOtus( getUpdatedOtuCounts( sampleOtuCounts, scarceOtus ) );
		updateMetadata();
	}

	/**
	 * Find the scarce OTUs that contain the key values in scarceTaxa.
	 * 
	 * @param sampleOtuCounts Map(SampleId, Map(OTU, count)) OTU counts for every sample
	 * @param uniqueOtus List(OTU) contains all OTUs for all samples
	 * @param scarceTaxa Map(taxa, Set(SampleId)) contains scarce taxa and their associated samples
	 * @return Map(OTU, Set(SampleId)) contains scarce OTUs and their associated samples
	 * @throws Exception if errors occur
	 */
	protected Map<String, Set<String>> findScarceOtus( final Map<String, Map<String, Integer>> sampleOtuCounts,
			final List<String> uniqueOtus, final Map<String, Set<String>> scarceTaxa ) throws Exception
	{
		final Map<String, Set<String>> scarceOtus = new HashMap<>();
		for( final String otu: uniqueOtus )
		{
			for( final String taxa: scarceTaxa.keySet() )
			{
				if( otu.contains( taxa ) )
				{
					scarceOtus.put( otu, scarceTaxa.get( taxa ) );
				}
			}
		}

		return scarceOtus;
	}

	/**
	 * Find scarce taxa found in less samples than the cutoff percentage:
	 * {@link biolockj.Config}.{@value #MIN_OTU_THRESHOLD}. Return a map of these scare taxa and a set of samples that
	 * need to remove them.
	 * 
	 * @param sampleOtuCounts Map(SampleId, Map(OTU, count)) OTU counts for every sample
	 * @param otus Set of unique OTUs
	 * @return Map(taxa, Set(SampleIds))
	 * @throws Exception if errors occur
	 */
	protected Map<String, Set<String>> findScarceTaxa( final Map<String, Map<String, Integer>> sampleOtuCounts,
			final List<String> otus ) throws Exception
	{
		final Map<String, Set<String>> scarceTaxa = new HashMap<>();
		for( final String level: Config.requireList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			final List<String> levelTaxa = OtuUtil.findUniqueTaxa( otus, level );
			Log.debug( getClass(), "Checking level: " + level + " with " + levelTaxa.size() + " taxa" );
			for( final String taxa: levelTaxa )
			{
				final Map<String, Map<String, Integer>> levelTaxaCounts = OtuUtil.getLevelTaxaCounts( sampleOtuCounts,
						level );
				final Set<String> samplesWithTaxa = new HashSet<>();
				for( final String sampleId: levelTaxaCounts.keySet() )
				{
					final Map<String, Integer> taxaCounts = levelTaxaCounts.get( sampleId );
					Log.debug( getClass(), "Checking sampleId: " + sampleId + " with " + taxaCounts.size() + " " + level
							+ " taxa for: " + taxa );
					if( taxaCounts.keySet().contains( taxa ) )
					{
						samplesWithTaxa.add( sampleId );
					}
				}

				if( !samplesWithTaxa.isEmpty() && samplesWithTaxa.size() <= getCutoff() )
				{
					scarceTaxa.put( OtuUtil.buildOtuTaxa( level, taxa ), samplesWithTaxa );
				}
				else if( !samplesWithTaxa.isEmpty() )
				{
					Log.debug( getClass(),
							"Valid " + level + " taxa: " + BioLockJUtil.getCollectionAsString( samplesWithTaxa ) );
				}
			}

			final int numScarceOtus = scarceTaxa.get( level ) == null ? 0: scarceTaxa.get( level ).size();
			Log.warn( getClass(), "Found " + numScarceOtus + " scarce " + level + " taxa: "
					+ BioLockJUtil.getCollectionAsString( scarceTaxa.get( level ) ) );
		}

		return scarceTaxa;
	}

	/**
	 * Remove scarce OTUs from the sampleOtuCounts and return it.
	 * 
	 * @param sampleOtuCounts Map(SampleId, Map(OTU, count)) OTU counts for every sample
	 * @param scarceOtus Map(OTU, Set(SampleId)) Scarce OTUs and the samples that list them
	 * @return Map(SampleId, Map(OTU, count)) sampleOtuCounts after scarce OTUs have been removed
	 * @throws Exception if errors occur
	 */
	protected Map<String, Map<String, Integer>> getUpdatedOtuCounts(
			final Map<String, Map<String, Integer>> sampleOtuCounts, final Map<String, Set<String>> scarceOtus )
			throws Exception
	{
		for( final String badOtu: scarceOtus.keySet() )
		{
			for( final String sampleId: scarceOtus.get( badOtu ) )
			{
				sampleOtuCounts.get( sampleId ).remove( badOtu );
			}
		}

		return sampleOtuCounts;

	}

	/**
	 * Save a list of scarce OTUs to the module temp directory.
	 * 
	 * @param scarceOtus OTUs found in too few samples
	 * @throws Exception if errors occur
	 */
	protected void logScarceOtus( final Set<String> scarceOtus ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter( new FileWriter( getScareOtuLogFile() ) );
		try
		{
			for( final String otu: scarceOtus )
			{
				writer.write( otu + RETURN );
			}
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}

		Log.info( getClass(),
				"Found " + scarceOtus.size() + " scarce OTUs saved to --> " + getScareOtuLogFile().getAbsolutePath() );
	}

	/**
	 * Output OTU count files with the updatedOtuCounts
	 * 
	 * @param updatedOtuCounts Map(SampleId, Map(OTU, count)) OTU counts for every sample
	 * @throws Exception if errors occur
	 */
	protected void removeScarceOtus( final Map<String, Map<String, Integer>> updatedOtuCounts ) throws Exception
	{
		for( final String sampleId: updatedOtuCounts.keySet() )
		{
			Log.debug( getClass(), "removeScarceOtus Checking sampleId: " + sampleId );
			final BufferedWriter writer = new BufferedWriter( new FileWriter(
					OtuUtil.getOtuCountFile( getOutputDir(), sampleId, getIdString().replace( "%", "" ) ) ) );
			try
			{
				final Map<String, Integer> otuCounts = updatedOtuCounts.get( sampleId );
				if( otuCounts == null || otuCounts.isEmpty() )
				{
					badSamples.add( sampleId );
					Log.warn( getClass(), sampleId + " has no valid OTUs after removing scarce OTUS below "
							+ MIN_OTU_THRESHOLD + "=" + Config.requirePositiveDouble( MIN_OTU_THRESHOLD ) );
				}
				else
				{
					Log.debug( getClass(), "removeScarceOtus Found: " + otuCounts.size() );
					Integer total = 0;
					for( final String otu: otuCounts.keySet() )
					{
						Log.debug( getClass(), "removeScarceOtus Checking OTU: " + otu );
						final Integer sampleCount = otuCounts.get( otu );
						Log.debug( getClass(),
								"removeScarceOtus otuCounts key #1: " + otuCounts.keySet().iterator().next() );
						if( sampleCount != null )
						{
							total += otuCounts.get( otu );
							writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
							Log.debug( getClass(), "removeScarceOtus Updated OTU count: " + otuCounts.get( otu ) );
						}
					}

					hitsPerSample.put( sampleId, total.toString() );
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
	 * Remove the invalid samples from the metadata file and add new NumOtus field.
	 *
	 * @throws Exception if errors occur
	 */
	protected void updateMetadata() throws Exception
	{
		if( badSamples.isEmpty() )
		{
			Log.info( getClass(),
					"All samples have valid OTUs that meet minimum read threshold - none will be ommitted..." );
			MetaUtil.addColumn( getMetaColName(), hitsPerSample, getOutputDir() );
			return;
		}
		else
		{
			Log.warn( getClass(), "Removing samples containing nothing but low count OTUs that have been removed: "
					+ BioLockJUtil.printLongFormList( badSamples ) );
		}

		final File newMapping = new File(
				getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );
		final BufferedReader reader = BioLockJUtil.getFileReader( MetaUtil.getFile() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newMapping ) );
		try
		{
			String line = reader.readLine();
			writer.write( line + TAB_DELIM + getMetaColName() + RETURN );

			for( line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
				final String id = st.nextToken();
				if( !badSamples.contains( id ) )
				{
					writer.write( line + TAB_DELIM + hitsPerSample.get( id ) + RETURN );
				}
			}
		}
		finally
		{
			reader.close();
			writer.close();
		}

		MetaUtil.setFile( newMapping );
		MetaUtil.refreshCache();
	}

	private int getCutoff() throws Exception
	{
		final Double cutoff = MetaUtil.getSampleIds().size() * Config.requirePositiveDouble( MIN_OTU_THRESHOLD );
		return cutoff.intValue();
	}

	private String getIdString() throws Exception
	{
		return MIN_OTU + "_" + new Double( Config.requirePositiveDouble( MIN_OTU_THRESHOLD ) * 100 ).intValue() + "%";
	}

	private String getMetaColName() throws Exception
	{
		if( otuColName == null )
		{
			otuColName = ModuleUtil.getSystemMetaCol( this, ParserModuleImpl.NUM_OTUS + "_" + getIdString() );
		}

		return otuColName;
	}

	private File getScareOtuLogFile() throws Exception
	{
		return new File( getTempDir().getAbsolutePath() + File.separator + "scarceOtus.txt" );
	}

	private final Set<String> badSamples = new HashSet<>();
	private final Map<String, String> hitsPerSample = new HashMap<>();
	private String otuColName = null;
	/**
	 * {@link biolockj.Config} Positive Double property {@value #MIN_OTU_THRESHOLD} defines minimum percentage of
	 * samples that must contain an OTU for it to be kept.
	 */
	protected static final String MIN_OTU_THRESHOLD = "removeScarceOtus.minOtuThreshold";

	private static final String MIN_OTU = "minOtuPer";

}