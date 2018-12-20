/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.JavaModuleImpl;
import biolockj.node.OtuNode;
import biolockj.node.ParsedSample;
import biolockj.util.*;

/**
 * Parser {@link biolockj.module.BioModule}s read {@link biolockj.module.classifier.ClassifierModule} output to build
 * standardized OTU count tables. This class provides the default abstract implementation.
 */
public abstract class ParserModuleImpl extends JavaModuleImpl implements ParserModule
{
	@Override
	public void addParsedSample( final ParsedSample newSample ) throws Exception
	{
		for( final ParsedSample sample: parsedSamples )
		{
			if( sample.getSampleId().equals( newSample.getSampleId() ) )
			{
				throw new Exception( "Attempt to add duplicate sample! " + sample.getSampleId() );
			}
		}

		parsedSamples.add( newSample );
	}

	@Override
	public void buildOtuCountFiles() throws Exception
	{
		for( final ParsedSample sample: parsedSamples )
		{
			final Map<String, Integer> otuCounts = sample.getOtuCounts();
			
			if( otuCounts != null )
			{
				File outputFile = OtuUtil.getOtuCountFile( getOutputDir(), sample.getSampleId(), null );
				Log.info( getClass(), "Build output sample: " + sample.getSampleId() + " has " + otuCounts.size() + " OTUs --> " 
						+ outputFile.getAbsolutePath() );
				final BufferedWriter writer = new BufferedWriter( new FileWriter( outputFile ) );
				int numHits = 0;
				try
				{
					for( final String otu: otuCounts.keySet() )
					{
						numHits += otuCounts.get( otu );
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

				hitsPerSample.put( sample.getSampleId(), String.valueOf( numHits ) );
			}
		}
	}

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
	 * Getter for depricatedOtuCountFields
	 * 
	 * @return depricatedOtuCountFields
	 */
	public Set<String> getDepricatedOtuCountFields()
	{
		return depricatedOtuCountFields;
	}

	/**
	 * Getter for otuCountField
	 * 
	 * @return otuCountField
	 */
	public String getOtuCountField()
	{
		return otuCountField;
	}

	@Override
	public ParsedSample getParsedSample( final String sampleId )
	{
		for( final ParsedSample sample: parsedSamples )
		{
			if( sample.getSampleId().equals( sampleId ) )
			{
				return sample;
			}
		}
		return null;
	}

	@Override
	public abstract void parseSamples() throws Exception;

	/**
	 * Parsers execute a task with 3 core functions:
	 * <ol>
	 * <li>{@link #parseSamples()} - generates {@link biolockj.node.ParsedSample}s
	 * <li>{@link #buildOtuCountFiles()} - builds OTU tree tables from the {@link biolockj.node.ParsedSample}s
	 * </ol>
	 */
	@Override
	public void runModule() throws Exception
	{
		MemoryUtil.reportMemoryUsage( "About to parse samples" );
		parseSamples();

		Log.debug( getClass(), "# Samples parsed: " + parsedSamples.size() );

		if( parsedSamples.isEmpty() )
		{
			throw new Exception( "Parser failed to produce output!" );
		}

		buildOtuCountFiles();

		if( Config.getBoolean( Config.REPORT_NUM_HITS ) )
		{
			MetaUtil.addColumn( NUM_OTUS, hitsPerSample, getOutputDir() );

		}
	}

	/**
	 * When a module modifies the number of hits, the new counts must replace the old count fields.
	 * 
	 * @param name Name of new number of hits metadata field
	 * @throws Exception if null value passed
	 */
	public void setNumHitsFieldName( final String name ) throws Exception
	{
		if( name == null )
		{
			throw new Exception( "Null name value passed to ParserModuleImpl.setNumHitsFieldName(name)" );
		}
		else if( otuCountField != null && otuCountField.equals( name ) )
		{
			Log.warn( getClass(), "Num Hits field already set to: " + otuCountField );
		}
		else
		{
			if( otuCountField != null )
			{
				depricatedOtuCountFields.add( otuCountField );
			}

			depricatedOtuCountFields.remove( name );
			otuCountField = name;
		}
	}

	/**
	 * Some {@link biolockj.module.classifier.ClassifierModule}s can include taxonomy level identifiers without an OTU
	 * name in the sample report files. This method verifies the node exists, has a valid sample ID, and that no emptry
	 * String OTU names are reported.
	 *
	 * @param node OtuNode build from 1 line of a {@link biolockj.module.classifier.ClassifierModule} output file
	 * @return boolean if {@link biolockj.node.OtuNode} is valid
	 */
	protected boolean isValid( final OtuNode node ) throws Exception
	{
		if( node != null && node.getSampleId() != null && node.getCount() != null && !node.getTaxaMap().isEmpty() )
		{
			for( final String level: node.getTaxaMap().keySet() )
			{
				final String otu = node.getTaxaMap().get( level );
				if( Config.getList( Config.REPORT_TAXONOMY_LEVELS ).contains( level ) && otu != null
						&& !otu.trim().isEmpty() )
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Validate {@link biolockj.module.implicit.parser} modules run after {@link biolockj.module.classifier} and
	 * {@link biolockj.module.seq} modules.
	 * 
	 * @throws Exception if modules are out of order
	 */
	protected void validateModuleOrder() throws Exception
	{
		boolean found = false;
		for( final BioModule module: Pipeline.getModules() )
		{
			if( found )
			{
				if( module.getClass().getName().startsWith( "biolockj.module.seq" )
						|| module.getClass().getName().startsWith( "biolockj.module.classifier" ) )
				{
					throw new Exception( "Invalid BioModule configuration order! " + module.getClass().getName()
							+ " must run before any " + getClass().getPackage().getName() + " BioModule." );
				}
			}
			else if( module.getClass().equals( getClass() ) )
			{
				found = true;
			}
		}
	}

	/**
	 * Get RegisterNumReads instance from the pipeline registry.
	 * 
	 * @return RegisterNumReads BioModule
	 * @throws Exception if errors occur
	 */
	public static ParserModuleImpl getModule() throws Exception
	{
		return (ParserModuleImpl) ModuleUtil.getParserModule();
	}

	private final Set<String> depricatedOtuCountFields = new HashSet<>();
	private final Map<String, String> hitsPerSample = new HashMap<>();
	private String otuCountField = NUM_OTUS;

	private final TreeSet<ParsedSample> parsedSamples = new TreeSet<>();

	/**
	 * Metadata column name for column that holds number of OTU hits after any {@link biolockj.module.implicit.parser}
	 * module executes: {@value #NUM_OTUS}
	 */
	public static final String NUM_OTUS = "NUM_OTU";

}
