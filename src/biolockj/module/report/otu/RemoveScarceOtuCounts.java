package biolockj.module.report.otu;

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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.exception.ConfigFormatException;
import biolockj.module.JavaModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.*;

/**
 * This BioModule removes scarce OTUs not found in enough samples.<br>
 * The OTU must be found in a configurable percentage of samples.
 */
public class RemoveScarceOtuCounts extends OtuCountModule implements JavaModule
{

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( getScarceCutoff() > 1 )
		{
			throw new ConfigFormatException( getScarceProp(), "Required range 0.0 < " + getScarceProp() + " < 1.0 " );
		}
	}

	/**
	 * Set the number of hits field.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		ParserModuleImpl.setNumHitsFieldName( getMetaColName() + "_" + Constants.OTU_COUNT );
	}

	/**
	 * Produce summary message with min, max, mean, and median number of reads.
	 */
	@Override
	public String getSummary() throws Exception
	{
		String summary = "Remove rare OTUs found in less than " + getCutoff() + " samples" + RETURN;
		summary += "# Unique OTU removed:  " + uniqueOtuRemoved.size() + RETURN;
		summary += "# Total OTU removed:   " + totalOtuRemoved + RETURN;
		summary += SummaryUtil.getCountSummary( hitsPerSample, "OTUs" );
		sampleIds.removeAll( hitsPerSample.keySet() );
		if( !sampleIds.isEmpty() )
		{
			summary += "Removed empty samples: " + BioLockJUtil.getCollectionAsString( sampleIds );
		}
		hitsPerSample = null;
		return super.getSummary() + summary;
	}

	@Override
	public void runModule() throws Exception
	{
		sampleIds.addAll( MetaUtil.getSampleIds() );
		Log.info( getClass(), "Searching samples to remove OTUs found in less than " + getCutoff() + " samples." );
		final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts = OtuUtil.getSampleOtuCounts( getInputFiles() );

		final TreeSet<String> uniqueOtus = OtuUtil.findUniqueOtus( sampleOtuCounts );
		Log.info( getClass(),
				"Searching " + uniqueOtus.size() + " unique OTUs in " + sampleOtuCounts.size()
						+ " samples for OTUs found in less than the cutoff percentage [ " + getScarceCutoff() + " ] = "
						+ getCutoff() + " samples." );

		final TreeMap<String, TreeSet<String>> scarceTaxa = findScarceTaxa( sampleOtuCounts, uniqueOtus );
		final TreeMap<String, TreeSet<String>> scarceOtus = findScarceOtus( sampleOtuCounts, uniqueOtus, scarceTaxa );
		logScarceOtus( scarceOtus.keySet() );
		removeScarceOtuCounts( getUpdatedOtuCounts( sampleOtuCounts, scarceOtus ) );

		if( Config.getBoolean( this, Constants.REPORT_NUM_HITS ) )
		{
			MetaUtil.addColumn( getMetaColName() + "_" + Constants.OTU_COUNT, hitsPerSample, getOutputDir(), true );
		}
	}

	/**
	 * Find the scarce OTUs that contain the key values in scarceTaxa.
	 * 
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @param uniqueOtus TreeSet(OTU) contains all OTUs for all samples
	 * @param scarceTaxa TreeMap(taxa, TreeSet(SampleId)) contains scarce taxa and their associated samples
	 * @return TreeMap(OTU, TreeSet(SampleId)) contains scarce OTUs and their associated samples
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, TreeSet<String>> findScarceOtus(
			final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts, final TreeSet<String> uniqueOtus,
			final TreeMap<String, TreeSet<String>> scarceTaxa ) throws Exception
	{
		final TreeMap<String, TreeSet<String>> scarceOtus = new TreeMap<>();
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
	 * {@link biolockj.Config}.{@value biolockj.Constants#REPORT_SCARCE_CUTOFF}. Return a map of these scare taxa and a set of samples that
	 * need to remove them.
	 * 
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @param otus TreeSet of unique OTUs
	 * @return TreeMap(taxa, TreeSet(SampleIds))
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, TreeSet<String>> findScarceTaxa(
			final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts, final TreeSet<String> otus )
			throws Exception
	{
		final TreeMap<String, TreeSet<String>> scarceTaxa = new TreeMap<>();
		for( final String level: TaxaUtil.getTaxaLevels() )
		{
			final TreeMap<String, TreeSet<String>> scarceLevelTaxa = new TreeMap<>();
			final TreeSet<String> levelTaxa = TaxaUtil.findUniqueTaxa( otus, level );
			Log.debug( getClass(), "Checking level: " + level + " with " + levelTaxa.size() + " taxa" );
			for( final String taxa: levelTaxa )
			{
				final TreeMap<String, TreeMap<String, Integer>> levelTaxaCounts = TaxaUtil
						.getLevelTaxaCounts( sampleOtuCounts, level );
				final TreeSet<String> samplesWithTaxa = new TreeSet<>();
				for( final String sampleId: levelTaxaCounts.keySet() )
				{
					final TreeMap<String, Integer> taxaCounts = levelTaxaCounts.get( sampleId );
					Log.debug( getClass(), "Checking sampleId: " + sampleId + " with " + taxaCounts.size() + " " + level
							+ " taxa for: " + taxa );
					if( taxaCounts.keySet().contains( taxa ) )
					{
						samplesWithTaxa.add( sampleId );
					}
				}

				Log.debug( getClass(), taxa + " found in " + samplesWithTaxa.size() + " samples" );
				if( !samplesWithTaxa.isEmpty() && samplesWithTaxa.size() <= getCutoff() )
				{
					scarceLevelTaxa.put( OtuUtil.buildOtuTaxa( level, taxa ), samplesWithTaxa );
					scarceTaxa.put( OtuUtil.buildOtuTaxa( level, taxa ), samplesWithTaxa );
				}
			}

			Log.info( getClass(), "Found " + scarceLevelTaxa.size() + " scarce " + level + " taxa: "
					+ BioLockJUtil.getCollectionAsString( scarceLevelTaxa.keySet() ) );

		}

		return scarceTaxa;
	}

	/**
	 * Remove scarce OTUs from the sampleOtuCounts and return it.
	 * 
	 * @param sampleOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @param scarceOtus TreeMap(OTU, TreeSet(SampleId)) Scarce OTUs and the samples that list them
	 * @return TreeMap(SampleId, TreeMap(OTU, count)) sampleOtuCounts after scarce OTUs have been removed
	 * @throws Exception if errors occur
	 */
	protected TreeMap<String, TreeMap<String, Integer>> getUpdatedOtuCounts(
			final TreeMap<String, TreeMap<String, Integer>> sampleOtuCounts,
			final TreeMap<String, TreeSet<String>> scarceOtus ) throws Exception
	{
		for( final String badOtu: scarceOtus.keySet() )
		{
			for( final String sampleId: scarceOtus.get( badOtu ) )
			{
				if( sampleOtuCounts.get( sampleId ).get( badOtu ) != null )
				{
					uniqueOtuRemoved.add( badOtu );
					totalOtuRemoved += sampleOtuCounts.get( sampleId ).get( badOtu );
					sampleOtuCounts.get( sampleId ).remove( badOtu );
				}
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
		if( scarceOtus == null || scarceOtus.isEmpty() )
		{
			Log.info( getClass(), "No scarce OTUs detected!" );
			return;
		}
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
	 * @param updatedOtuCounts TreeMap(SampleId, TreeMap(OTU, count)) OTU counts for every sample
	 * @throws Exception if errors occur
	 */
	protected void removeScarceOtuCounts( final TreeMap<String, TreeMap<String, Integer>> updatedOtuCounts )
			throws Exception
	{
		for( final String sampleId: updatedOtuCounts.keySet() )
		{
			final TreeMap<String, Integer> otuCounts = updatedOtuCounts.get( sampleId );
			if( otuCounts != null && !otuCounts.isEmpty() )
			{
				final BufferedWriter writer = new BufferedWriter( new FileWriter(
						OtuUtil.getOtuCountFile( getOutputDir(), sampleId, getMetaColName().replace( "%", "" ) ) ) );
				try
				{
					Log.debug( getClass(), sampleId + " # unique OTUs: " + otuCounts.size() );
					Integer total = 0;
					for( final String otu: otuCounts.keySet() )
					{
						Log.debug( getClass(), sampleId + " checking OTU: " + otu );
						final Integer sampleCount = otuCounts.get( otu );
						if( sampleCount != null )
						{
							total += otuCounts.get( otu );
							writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
						}
					}

					hitsPerSample.put( sampleId, total.toString() );
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
	}

	private int getCutoff() throws Exception
	{
		if( cutoff == null )
		{
			cutoff = new Double( Math.ceil( MetaUtil.getSampleIds().size() * getScarceCutoff() ) ).intValue();
		}

		return cutoff;
	}

	private String getMetaColName() throws Exception
	{
		return "scarce" + new Double( getScarceCutoff() * 100 ).intValue() + "%";
	}

	private Double getScarceCutoff() throws Exception
	{
		return Config.requirePositiveDouble( this, getScarceProp() );
	}

	private String getScarceProp() throws Exception
	{
		if( prop == null )
		{
			prop = Config.getModuleProp( this, Constants.REPORT_SCARCE_CUTOFF );
		}
		return prop;
	}

	private File getScareOtuLogFile() throws Exception
	{
		return new File( getTempDir().getAbsolutePath() + File.separator + "scarceOtus" + TXT_EXT );
	}

	private Integer cutoff = null;
	private Map<String, String> hitsPerSample = new HashMap<>();
	private String prop = null;
	private final TreeSet<String> sampleIds = new TreeSet<>();
	private int totalOtuRemoved = 0;
	private final Set<String> uniqueOtuRemoved = new HashSet<>();
}