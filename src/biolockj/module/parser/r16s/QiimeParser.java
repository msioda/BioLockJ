/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Feb 9, 2017
 * @disclaimer 	This code is free software; you can redistribute it and/or
 * 				modify it under the terms of the GNU General Public License
 * 				as published by the Free Software Foundation; either version 2
 * 				of the License, or (at your option) any later version,
 * 				provided that any use properly credits the author.
 * 				This program is distributed in the hope that it will be useful,
 * 				but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * 				GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.parser.r16s;

import java.io.BufferedReader;
import java.io.File;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.parser.ParsedSample;
import biolockj.module.parser.ParserModule;
import biolockj.module.parser.ParserModuleImpl;
import biolockj.node.r16s.QiimeNode;
import biolockj.util.MetaUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.QiimeMapping;

/**
 * To see file format: > head otu_table_L2.txt
 *
 * # Constructed from biom file #OTU ID 3A.1 6A.1 120A.1 7A.1
 * k__Bacteria;p__Actinobacteria 419.0 26.0 90.0 70.0
 *
 */
public class QiimeParser extends ParserModuleImpl implements ParserModule
{
	/**
	 * QIIME doesn't support Config.INPUT_DEMULTIPLEX option
	 *
	 * @Exception thrown if Config.INPUT_DEMULTIPLEX == TRUE
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( Config.getBoolean( Config.INPUT_DEMULTIPLEX ) )
		{
			throw new Exception( "QIIME doesn't support multiplexed files.  Config must set: "
					+ Config.INPUT_DEMULTIPLEX + "=TRUE" );
		}
	}

	/**
	 * Utilize QiimeMapping to build QiimeMapping file.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		QiimeMapping.buildMapping( getTempDir(), getInputFiles().get( 0 ) );
		super.executeProjectFile();
	}

	/**
	 * Init input files to find the most specific taxa file, this will contain all the info
	 * for all taxa levels above it.
	 */
	@Override
	public void initInputFiles( final IOFileFilter ff, final IOFileFilter recursive ) throws Exception
	{
		final String searchTerm = getLowestTaxaLevelFileName();
		//final String searchTerm = OTU_TABLE_PREFIX + "*.txt";
		Log.out.info(
				"Search for taxa files with search term: " + searchTerm + " in: " + getInputDir().getAbsolutePath() );
		super.initInputFiles( new NameFileFilter( searchTerm ), TrueFileFilter.INSTANCE );
	}

	/**
	 * Parse each line of each file to extract lines used to build OtuNodes.  The addOtuNode()
	 * method is used to populate the otuNodes map.  Ignore lines starting with # symbol.
	 * Each line will have a single OTU path.
	 */
	@Override
	public void parseSamples() throws Exception
	{
		//int fileCount = 0;
		//for( final File file: getOrderedInputFiles() )
		//{
		final File file = getInputFiles().get( 0 );
		Log.out.info( "PARSE FILE: " + file.getName() );
		final BufferedReader reader = ModuleUtil.getFileReader( file );

		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			if( line.startsWith( "#" ) )
			{
				continue;
			}

			final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
			int index = 0;
			final String taxas = st.nextToken();
			while( st.hasMoreTokens() )
			{
				final Long count = Double.valueOf( st.nextToken() ).longValue();

				if( count > 0 )
				{
					final String id = QiimeMapping.getSampleIds().get( index++ );
					final QiimeNode node = new QiimeNode( id, taxas, count );
					if( isValid( node ) )
					{
						final ParsedSample sample = getParsedSample( node.getSampleId() );
						if( sample == null )
						{
							addParsedSample( new ParsedSample( node ) );
						}
						else
						{
							sample.addNode( node );
						}
					}
				}
			}
		}

		reader.close();
	}

	/**
	 * Get the output for the line, merged with its metadata.
	 */
	@Override
	protected String getMergedLine( final String line ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		final String sampleId = new StringTokenizer( line, TAB_DELIM ).nextToken();
		if( MetaUtil.getMetaFileFirstColValues().contains( sampleId ) )
		{
			sb.append( rFormat( line ) );
			for( final String attribute: MetaUtil.getAttributes( sampleId ) )
			{
				sb.append( TAB_DELIM ).append( rFormat( attribute ) );
			}
		}
		else
		{
			Log.out.warn( "Missing record for: " + sampleId + " in metadata: " + MetaUtil.getFile().getAbsolutePath() );
			return null;
		}

		if( mergeLineCount++ < 2 )
		{
			Log.out.info( "Example: Merge MetaUtil Line [" + sampleId + "] = " + sb.toString() );
		}

		return sb.toString();
	}

	/**
	 * Find the lowest taxa level.
	 * @return
	 * @throws Exception
	 */
	private String getLowestTaxaLevelFileName() throws Exception
	{
		String level = "";
		if( Config.requireTaxonomy().contains( Config.SPECIES ) )
		{
			level = "7";
		}
		else if( Config.requireTaxonomy().contains( Config.GENUS ) )
		{
			level = "6";
		}
		else if( Config.requireTaxonomy().contains( Config.FAMILY ) )
		{
			level = "5";
		}
		else if( Config.requireTaxonomy().contains( Config.ORDER ) )
		{
			level = "4";
		}
		else if( Config.requireTaxonomy().contains( Config.CLASS ) )
		{
			level = "3";
		}
		else if( Config.requireTaxonomy().contains( Config.PHYLUM ) )
		{
			level = "2";
		}
		else if( Config.requireTaxonomy().contains( Config.DOMAIN ) )
		{
			level = "1";
		}

		return OTU_TABLE_PREFIX + level + ".txt";
	}

	private Collection<File> getOrderedInputFiles() throws Exception
	{
		final TreeMap<Integer, File> map = new TreeMap<>();
		for( final File file: getInputFiles() )
		{
			final int x = Integer.valueOf( file.getName().replace( OTU_TABLE_PREFIX, "" ).replace( ".txt", "" ) );
			map.put( x, file );
		}

		return map.values();
	}

	private int mergeLineCount = 0;
	private static final String OTU_TABLE_PREFIX = "otu_table_L";
}
