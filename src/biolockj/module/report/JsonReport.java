package biolockj.module.report;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.commons.lang.builder.EqualsBuilder;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.BioModuleImpl;
import biolockj.module.parser.ParsedSample;
import biolockj.module.parser.ParserModule;
import biolockj.util.ModuleUtil;

public class JsonReport extends BioModuleImpl
{
	private static class JsonNode implements Serializable, Comparable<JsonNode>
	{
		@Override
		public int compareTo( final JsonNode node )
		{
			return name.compareTo( node.name );
		}

		@Override
		public boolean equals( final Object o )
		{
			if( ( o != null ) && ( o instanceof JsonNode ) )
			{
				if( o == this )
				{
					return true;
				}
				if( ( (JsonNode) o ).parent != null )
				{
					if( parent == null )
					{
						return false;
					}
					return new EqualsBuilder().append( name, ( (JsonNode) o ).name )
							.append( parent, ( (JsonNode) o ).parent ).isEquals();
				}
				return new EqualsBuilder().append( name, ( (JsonNode) o ).name ).isEquals();
			}

			return false;
		}

		@Override
		public int hashCode()
		{
			if( parent == null )
			{
				return name.hashCode();
			}
			return ( name + parent ).hashCode();
		}

		String name;
		Long numSequences;
		JsonNode parent;
		HashMap<String, Double> stats = new LinkedHashMap<>();

		private static final long serialVersionUID = 7967794387383764650L;

	}

	/**
	 * Verify Config params & that RReport is the previous module.
	 *
	 * @Exception if modules out of order or config is missing/invalid
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		Config.requireString( Config.REPORT_LOG_BASE );
		ModuleUtil.requireModule( RReport.class.getName() );
		ModuleUtil.requireParserModule();
	}

	/**
	 * Obtain parsed sample data, build root node, & create the jsonMap by passing both
	 * to buildMap().  Set ROOT_NODE #sequences with getRootCount(jsonMap), add stats info
	 * to the jsonNodes, & finally build the JSON file.
	 *
	 * @Exception if propagated by sub-methods
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		final BioModule parserMod = ModuleUtil.requireParserModule();
		final BioModule rMod = ModuleUtil.requireModule( RReport.class.getName() );
		if( !ModuleUtil.isComplete( rMod ) || !ModuleUtil.isComplete( parserMod ) )
		{
			throw new Exception(
					"BioLockJ Modules Out of Order --> Prerequisite modules have not successfully completed!  "
							+ parserMod.getClass().getName() + " & " + RReport.class.getName() + " incomplete" );
		}

		final Set<ParsedSample> parsedSamples = getParsedSamples();
		final JsonNode root = buildNode( ROOT_NODE, 0L, null );
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = buildJsonMap( root, parsedSamples );
		root.numSequences = getRootCount( jsonMap );
		addStats( jsonMap, root );
		writeJSON( jsonMap, root );
	}

	/**
	 * Stats can be found for all taxonomy levels in the RReport output, which is also
	 * the JsonReport inputDir() since it must be configured as the
	 *
	 * @param LinkedHashMap<String, TreeSet<JsonNode>> jsonMap (key=level)
	 * @param root
	 * @throws Exception if unable to parse report files
	 */
	private void addStats( final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final JsonNode root )
			throws Exception
	{
		Log.out.info( "Adding stats (pvals, r-squared, etc) to JSON nodes..." );
		for( final String level: Config.requireTaxonomy() )
		{
			final File statsReport = getTaxaLevelReport( level );
			final BufferedReader reader = new BufferedReader( new FileReader( statsReport ) );
			final List<String> columnNames = Arrays
					.asList( reader.readLine().replaceAll( "\"", "" ).split( TAB_DELIM ) );

			Log.out.debug( "addStats(" + level + ") using report: " + statsReport.getAbsolutePath() );

			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line.replaceAll( "\"", "" ), TAB_DELIM );
				final String otu = st.nextToken();

				Log.out.debug( "addStats(" + level + ") otu: " + otu + "   [length=" + otu.length() + "]" );

				int i = 0;
				final JsonNode jsonNode = getJsonNode( otu, jsonMap.get( level ) );
				if( jsonNode != null )
				{
					while( st.hasMoreTokens() )
					{
						jsonNode.stats.put( columnNames.get( ++i ), Double.parseDouble( st.nextToken() ) );
						Log.out.debug( "addStats(" + level + ") for: " + columnNames.get( i ) );
					}
				}
				else
				{
					Log.out.error(
							"Could not find " + level + " OTU: " + otu + " from " + statsReport.getAbsolutePath() );
				}
			}

			reader.close();
		}
	}

	/**
	 * Build the JSON-Map key=level, value = Set<OtuNode>
	 * The value
	 *
	 * @param JsonNode rootNode
	 * @param Set<ParsedSample> parsedSamples
	 * @return LinkedHashMap<String, TreeSet<JsonNode>> jsonMap
	 * @throws Exception
	 */
	private LinkedHashMap<String, TreeSet<JsonNode>> buildJsonMap( final JsonNode rootNode,
			final Set<ParsedSample> parsedSamples ) throws Exception
	{
		Log.out.info( "Build JSON for " + parsedSamples.size() + " samples..." );
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = new LinkedHashMap<>();

		//		//for each sample...
		//		for( final ParsedSample sample: parsedSamples )
		//		{
		//			Log.out.info( "Process Sample ID: " + id );
		//			final Iterator<OtuNode> it = otuNodeMap.get( id ).iterator();
		//			while( it.hasNext() )
		//			{
		//				JsonNode parent = rootNode;
		//				final OtuNode otuNode = it.next();
		//				for( final String level: Config.requireTaxonomy() )
		//				{
		//					final String otu = otuNode.getOtu( level );
		//					if( otu != null )
		//					{
		//						final long count = otuNode.getCount();
		//						if( jsonMap.get( level ) == null )
		//						{
		//							jsonMap.put( level, new TreeSet<JsonNode>() );
		//						}
		//						JsonNode jsonNode = getJsonNode( otu, jsonMap.get( level ) );
		//
		//						if( jsonNode == null )
		//						{
		//							jsonNode = buildNode( otu, count, parent );
		//							jsonMap.get( level ).add( jsonNode );
		//						}
		//						else
		//						{
		//							jsonNode.numSequences += count;
		//						}
		//
		//						parent = jsonNode;
		//					}
		//				}
		//			}
		//		}

		return jsonMap;
	}

	/**
	 * The ParserModule has already parsed & cached the classifier report data.
	 * If running a restarted pipeline, this cache will need to be rebuilt.
	 *
	 * @return Set<ParsedSample>
	 * @throws Exception thrown if unable to obtain parsed sample data
	 */
	private Set<ParsedSample> getParsedSamples() throws Exception
	{
		final ParserModule parser = ModuleUtil.requireParserModule();
		if( !ModuleUtil.hasExecuted( parser ) )
		{
			Log.out.info( parser.getClass().getName()
					+ " has not executed so must be executing a pipeline restart --> attempting to rebuild ParsedSample cache..." );
			parser.parseSamples();
		}
		if( !parser.getParsedSamples().isEmpty() )
		{
			return parser.getParsedSamples();
		}

		throw new Exception( "Unalbe to obtain cached parsed sample data!" );
	}

	/**
	 * The root count = sum of sequence counts for OTUs in top level taxonomy.
	 * Also log (INFO) the #sequences at each taxonomy level.
	 *
	 * @param LinkedHashMap<String, TreeSet<JsonNode>> jsonMap (key=level)
	 * @return long count
	 * @throws Exception thrown if propagated from Config.requireTaxonomy()
	 */
	private long getRootCount( final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap ) throws Exception
	{
		Log.out.info( Log.LOG_SPACER );
		Log.out.info( "JSON OTU Report" );
		Log.out.info( Log.LOG_SPACER );
		long rootCount = 0L;
		final LinkedHashMap<String, Long> countMap = new LinkedHashMap<>();
		for( final String level: Config.requireTaxonomy() )
		{
			long totalSeqs = 0L;
			for( final JsonNode jsonNode: jsonMap.get( level ) )
			{
				if( jsonNode == null )
				{
					throw new Exception( "No JSON Nodes as level: " + level );
				}

				if( jsonNode.parent == null )
				{
					throw new Exception( "No parent found: " + level + " : " + jsonNode.name );
				}

				if( jsonNode.numSequences == null )
				{
					throw new Exception( "numSequences is null! --> " + level + " : " + jsonNode.name );
				}

				totalSeqs += jsonNode.numSequences;
				Log.out.info( level + " : " + jsonNode.name + " (parent=" + jsonNode.parent.name + ") = "
						+ jsonNode.numSequences );
			}
			countMap.put( level, totalSeqs );
			if( ( rootCount == 0L ) && ( totalSeqs > 0 ) )
			{
				rootCount = totalSeqs; // sets top level count
			}
		}

		for( final String level: Config.requireTaxonomy() )
		{
			Log.out.info( "Seq Total [" + level + "] = " + countMap.get( level ) );
		}

		return rootCount;
	}

	/**
	 * Get the approriate level specific report from the RReport outputDir.
	 *
	 * @param String level
	 * @return File log normalized report
	 * @throws Exception if unable to obtain the report due to propagated excptions
	 */
	private File getTaxaLevelReport( final String level ) throws Exception
	{
		for( final File f: ModuleUtil.requireModule( RReport.class.getName() ).getOutputDir().listFiles() )
		{
			if( f.getName().equals( Config.getLogPrefix() + level + ".tsv" ) )
			{
				return f;
			}
		}
		return null;
	}

	/**
	 * Generate the JSON_SUMMARY file by recursively calling writeNodeAndChildren()
	 *
	 * @param jsonMap contains all JsonNodes, key=level
	 * @param rootNode is the ROOT_NODE & must contain children to build hierarchy
	 * @throws Exception thrown if unable to create the new file
	 */
	private void writeJSON( final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final JsonNode rootNode )
			throws Exception
	{
		final BufferedWriter writer = new BufferedWriter(
				new FileWriter( new File( getOutputDir().getAbsolutePath() + File.separator + JSON_SUMMARY ) ) );

		writeNodeAndChildren( writer, rootNode, jsonMap, 0 );
		writer.close();
	}

	private void writeNodeAndChildren( final BufferedWriter writer, final JsonNode node,
			final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final int nodeLevel ) throws Exception
	{
		final String taxaLevel = ( nodeLevel == 0 ) ? ROOT_NODE: Config.requireTaxonomy().get( nodeLevel - 1 );
		writer.write( "{" + RETURN );
		writer.write( "\"" + OTU_NAME + "\": \"" + node.name + "\"" + RETURN );
		writer.write( "\"" + OTU_LEVEL + "\": \"" + taxaLevel + "\"," + RETURN );
		writer.write( "\"" + NUM_SEQS + "\": " + node.numSequences + "," + RETURN );

		if( node.stats.size() > 0 )
		{
			for( final String s: node.stats.keySet() )
			{
				writer.write( "\"-log" + Config.requireString( Config.REPORT_LOG_BASE ) + "(" + s + ")\": "
						+ node.stats.get( s ) + "," + RETURN );
			}
		}

		if( nodeLevel < Config.requireTaxonomy().size() )
		{
			final List<JsonNode> childNodes = new ArrayList<>();
			final Iterator<JsonNode> jsonNodes = jsonMap.get( Config.requireTaxonomy().get( nodeLevel ) ).iterator();

			while( jsonNodes.hasNext() )
			{
				final JsonNode innerNode = jsonNodes.next();
				// if ( innerNode == node )
				if( innerNode.parent.equals( node ) )
				{
					childNodes.add( innerNode );
				}
			}

			if( childNodes.size() > 0 )
			{
				writer.write( ",\"" + CHILDREN + "\": [" + RETURN );
				for( final Iterator<JsonNode> children = childNodes.iterator(); children.hasNext(); )
				{
					writeNodeAndChildren( writer, children.next(), jsonMap, nodeLevel + 1 );
					if( children.hasNext() )
					{
						writer.write( "," );
					}
				}
				writer.write( "]" + RETURN );
			}
		}

		writer.write( "}" + RETURN );
	}

	private static JsonNode buildNode( final String name, final Long count, final JsonNode parent )
	{
		final JsonNode node = new JsonNode();
		node.name = name;
		node.numSequences = count;
		node.parent = parent;
		return node;
	}

	private static JsonNode getJsonNode( final String otu, final Set<JsonNode> jsonNodes )
	{
		for( final JsonNode jsonNode: jsonNodes )
		{
			if( jsonNode.name.equals( otu ) )
			{
				return jsonNode;
			}
		}
		return null;
	}

	private static final String CHILDREN = "children";
	private static final String JSON_SUMMARY = "summary.json";
	private static final String NUM_SEQS = "numSeqs";
	private static final String OTU_LEVEL = "taxaLevel";
	private static final String OTU_NAME = "otu";
	private static final String ROOT_NODE = "root";
}