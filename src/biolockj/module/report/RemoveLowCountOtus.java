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
import org.apache.commons.io.FileUtils;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.util.*;

/**
 * This BioModule
 */
public class RemoveLowCountOtus extends JavaModuleImpl implements JavaModule
{

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		Config.requirePositiveInteger( MIN_OTU_COUNT );
	}

	/**
	 * Set {@value #NUM_OTUS} as the number of hits field.
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
		return OtuUtil.outputHasOtuCountFiles( previousModule );
	}

	/**
	 * 
	 */
	@Override
	public void runModule() throws Exception
	{
		final Map<String, Map<String, Integer>> sampleOtuCounts = OtuUtil.getSampleOtuCounts( getInputFiles() );
		removeLowCounts( sampleOtuCounts );
		updateMetadata();
	}

	/**
	 * Remove OTUs below the {@link biolockj.Config}.{@value #MIN_OTU_COUNT}
	 * 
	 * @param sampleOtuCounts Map(SampleId, Map(OTU, count)) OTU counts for every sample
	 * @throws Exception if errors occur
	 */
	protected void removeLowCounts( final Map<String, Map<String, Integer>> sampleOtuCounts ) throws Exception
	{
		Log.debug( getClass(), "Build low count files for total # files: " + sampleOtuCounts.size() );
		for( final String sampleId: sampleOtuCounts.keySet() )
		{
			Log.debug( getClass(), "Check for low OTU counts in: " + sampleId );
			int numOtus = 0;
			final Map<String, Integer> otuCounts = sampleOtuCounts.get( sampleId );

			final Set<String> validOtus = new HashSet<>( otuCounts.keySet() );

			for( final String otu: otuCounts.keySet() )
			{
				if( otuCounts.get( otu ) < Config.requirePositiveInteger( MIN_OTU_COUNT ) )
				{
					Log.debug( getClass(), sampleId + ": must remove: " + otu + "=" + otuCounts.get( otu ) );
					validOtus.remove( otu );
				}
				else
				{
					numOtus += otuCounts.get( otu );
				}
			}

			if( validOtus.isEmpty() )
			{
				badSamples.add( sampleId );
			}
			else
			{
				hitsPerSample.put( sampleId, String.valueOf( numOtus ) );
			}

			if( validOtus.size() == otuCounts.size() )
			{
				FileUtils.copyFileToDirectory( getFileMap().get( sampleId ), getOutputDir() );
			}
			else if( !badSamples.contains( sampleId ) )
			{
				final Set<String> badOtus = new HashSet<>( otuCounts.keySet() );
				badOtus.removeAll( validOtus );

				Log.warn( getClass(),
						"Removed low OTU counts below " + MIN_OTU_COUNT + "="
								+ Config.requirePositiveInteger( MIN_OTU_COUNT ) + " --> "
								+ BioLockJUtil.getCollectionAsString( badOtus ) );

				final File otuFile = OtuUtil.getOtuCountFile( getOutputDir(), sampleId, getIdString() );
				final BufferedWriter writer = new BufferedWriter( new FileWriter( otuFile ) );
				try
				{
					for( final String otu: validOtus )
					{
						Log.debug( getClass(),
								sampleId + ":otuCounts.size(): " + otuCounts.size() + "=" + otuCounts.get( otu ) );
						writer.write( otu + TAB_DELIM + otuCounts.get( otu ) + RETURN );
					}
				}
				finally
				{
					if( writer != null )
					{
						writer.close();
					}

					getFileMap().put( sampleId, otuFile );
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
		String metaColName = getMetaColName();
		if( badSamples.isEmpty() )
		{
			Log.info( getClass(), "All samples have OTUs that meet minimum read threshold - none will be ommitted..." );
			MetaUtil.addColumn( metaColName, hitsPerSample, getOutputDir() );
			return;
		}
		else
		{
			Log.warn( getClass(), "Removing samples below with only low count OTUs: "
					+ BioLockJUtil.printLongFormList( badSamples ) );
		}

		final File newMapping = new File(
				getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() );

		Log.info( getClass(), "Current metadata file: " + MetaUtil.getFile().getAbsolutePath() );
		Log.info( getClass(),
				"Building metadata file: " + newMapping.getAbsolutePath() + " with new col: " + metaColName );
		final BufferedReader reader = BioLockJUtil.getFileReader( MetaUtil.getFile() );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( newMapping ) );
		try
		{
			String line = reader.readLine();
			writer.write( line + TAB_DELIM + metaColName + RETURN );
			Log.info( getClass(), "Adding col header: " + metaColName );
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

	private Map<String, File> getFileMap() throws Exception
	{
		if( fileMap == null )
		{
			fileMap = new HashMap<>();
			for( final File f: getInputFiles() )
			{
				fileMap.put( OtuUtil.getSampleId( f ), f );
			}
		}
		return fileMap;
	}

	private String getIdString() throws Exception
	{
		return MIN_OTU + Config.requirePositiveInteger( MIN_OTU_COUNT );
	}

	private String getMetaColName() throws Exception
	{
		if( otuColName == null )
		{
			otuColName = ModuleUtil.getSystemMetaCol( this, ParserModuleImpl.NUM_OTUS + "_" + getIdString() );
		}

		return otuColName;
	}

	private final Set<String> badSamples = new HashSet<>();
	private Map<String, File> fileMap = null;
	private final Map<String, String> hitsPerSample = new HashMap<>();
	private String otuColName = null;

	/**
	 * {@link biolockj.Config} Positive Integer property {@value #MIN_OTU_COUNT} defines the minimum number of OTUs
	 * allowed, if a count less that this value is found, it is set to 0.
	 */
	protected static final String MIN_OTU_COUNT = "removeLowCounts.minOtuCount";

	private static final String MIN_OTU = "minOtuCount";

}
