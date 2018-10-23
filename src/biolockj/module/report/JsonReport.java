package biolockj.module.report;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.r.CalculateStats;
import biolockj.node.JsonNode;
import biolockj.node.OtuNode;
import biolockj.node.ParsedSample;
import biolockj.util.ModuleUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule is used to build a JSON file (summary.json) from pipeline OTU-metadata tables.
 */
public class JsonReport extends JavaModuleImpl implements JavaModule
{
	@Override
	public void checkDependencies() throws Exception
	{
		ModuleUtil.requireParserModule();
	}

	@Override
	public List<File> getInputFiles() throws Exception
	{
		return new ArrayList<>();
		// nothing to do
	}

	/**
	 * Module prerequisites include:
	 * <ul>
	 * <li>{@link biolockj.module.report.AddMetadataToOtuTables}
	 * <li>{@link biolockj.module.r.CalculateStats}
	 * </ul>
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		final List<Class<?>> preReqs = super.getPreRequisiteModules();
		preReqs.add( AddMetadataToOtuTables.class );
		preReqs.add( CalculateStats.class );
		return preReqs;
	}

	/**
	 * Add summary with unique OTU counts/level, and for each level, give # reads with a gap in the taxonomy assignment.
	 */
	@Override
	public String getSummary() throws Exception
	{
		final Map<String, Long> gaps = new HashMap<>();
		for( final Map<String, String> otus: brokenOtuNetworks )
		{
			final int size = otus.size();
			int count = 0;
			for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
			{
				if( otus.get( level ) != null )
				{
					count++;
				}
				else if( count < size )
				{
					if( gaps.get( level ) == null )
					{
						gaps.put( level, 0L );
					}

					gaps.put( level, gaps.get( level ) + 1 );
				}
			}
		}

		final StringBuffer sb = new StringBuffer();
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			final Long count = gaps.get( level );
			if( count != null )
			{
				sb.append( "# OTU Gaps [ " + level + " ]: " + count + RETURN );
			}
		}

		return super.getSummary() + summary.toString() + sb.toString();
	}

	/**
	 * Obtain parsed sample data, build root node, and create the jsonMap by passing both to buildMap(). Set ROOT_NODE
	 * #seqs with getRootCount(jsonMap), add stats info to the jsonNodes, and finally build the JSON file.
	 */
	@Override
	public void runModule() throws Exception
	{
		final JsonNode root = new JsonNode( ROOT_NODE, 0L, null, null );
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = buildJsonMap( root );
		root.addCount( getRootCount( jsonMap ) );
		addStats( jsonMap, root );
		writeJson( writeNodeAndChildren( root, false, jsonMap, 0 ) );
	}

	/**
	 * Return list of OTUs that passed the filters in {@link biolockj.module.r.CalculateStats} which has generated
	 * statistics. Key=level, Value=OTUs.
	 * 
	 * @return Map of valid OTUs by level
	 * @throws Exception if errors occur
	 */
	protected Map<String, Set<String>> getValidOtus() throws Exception
	{
		final Map<String, Set<String>> validOtuMap = new HashMap<>();
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			final File statsFile = CalculateStats.getStatsFile( level, CalculateStats.P_VALS_PAR );
			final Set<String> validOtus = new HashSet<>();
			if( statsFile != null )
			{
				final BufferedReader reader = SeqUtil.getFileReader( statsFile );
				try
				{
					reader.readLine(); // skip column headers
					for( String line = reader.readLine(); line != null; line = reader.readLine() )
					{
						final StringTokenizer st = new StringTokenizer( line.replaceAll( "\"", "" ), TAB_DELIM );
						validOtus.add( st.nextToken().trim() );
					}
				}
				finally
				{
					if( reader != null )
					{
						reader.close();
					}
				}
			}

			validOtuMap.put( level, validOtus );
		}

		return validOtuMap;
	}

	/**
	 * Statistics can be found for all taxonomy levels in the output from {@link biolockj.module.r.CalculateStats},
	 * which is also the JsonReport inputDir() since it must be configured as the
	 *
	 * @param LinkedHashMap jsonMap (key=level)
	 * @param root JsonNode
	 * @throws Exception if unable to parse report files
	 */
	private void addStats( LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final JsonNode root ) throws Exception
	{
		Log.get( getClass() ).info( "Adding stats to JSON nodes..." );
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			final File[] statReports = getStatReports( level );
			for( final File stats: statReports )
			{
				if( stats != null )
				{
					jsonMap = updateNodeStats( jsonMap, stats, level );
				}
			}
		}
	}

	/**
	 * Build the JSON Map [key=level, value = TreeSet<JsonNode>].<br>
	 * 
	 * 
	 * @param JsonNode rootNode
	 * @return jsonMap of JsonNodes for each taxonomy level
	 * @throws Exception if errors occur
	 */
	private LinkedHashMap<String, TreeSet<JsonNode>> buildJsonMap( final JsonNode rootNode ) throws Exception
	{
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = new LinkedHashMap<>();
		final Set<ParsedSample> parsedSamples = ModuleUtil.requireParserModule().getParsedSamples();
		Log.get( getClass() ).info( "Build JSON Nodes for " + parsedSamples.size() + " samples..." );
		
		
		final Map<String, Set<String>> validOtus = getValidOtus();

		// for each sample...
		for( final ParsedSample sample: parsedSamples )
		{
			Log.get( getClass() ).debug( "Build JSON Node for Sample ID[ " + sample.getSampleId() + " ] with #Hits:"
					+ sample.getNumHits() + " | #OTU Nodes: " + sample.getOtuNodes().size() );

			// for each sample OtuNode
			for( final OtuNode node: sample.getOtuNodes() )
			{
				final Map<String, String> otuBranch = node.getOtuMap();
				boolean foundGap = false;
				final StringBuffer gaps = new StringBuffer();
				String parentOtu = null;
				String parentLevel = null;
				final Iterator<String> levels = Config.getList( Config.REPORT_TAXONOMY_LEVELS ).iterator();
				while( levels.hasNext() )
				{
					final String level = levels.next();
					final String otu = otuBranch.get( level );
					if( otu != null && validOtus.get( level ).contains( otu ) )
					{
						// missing levels only become a gap if OTUs are found below the gap
						if( !gaps.toString().isEmpty() )
						{
							foundGap = true;
						}

						// create jsonMap level entry
						if( jsonMap.get( level ) == null )
						{
							jsonMap.put( level, new TreeSet<JsonNode>() );
						}

						// create JsonNode for OTU at the given level
						JsonNode jsonNode = getJsonNode( otu, jsonMap.get( level ), level );
						if( jsonNode == null )
						{
							JsonNode parent = getParent( parentOtu, parentLevel, jsonMap );
							if( parent == null )
							{
								parent = rootNode;
							}
							jsonNode = new JsonNode( otu, 0L, parent, level );
							jsonMap.get( level ).add( jsonNode );
						}

						jsonNode.addCount( node.getCount() );
						parentOtu = otu;
						parentLevel = level;
					}
					else if( parentOtu != null ) // OTU = null: Tree has a gap, so do not check remaining nodes
					{
						gaps.append( ( gaps.toString().isEmpty() ? "": ", " ) + level );

					}
				}

				if( foundGap )
				{
					brokenOtuNetworks.add( otuBranch );
					node.report();
					Log.get( getClass() )
							.warn( "Missing OTU Levels [ " + sample.getSampleId() + " ]: " + gaps.toString() );
				}
			}
		}

		return jsonMap;
	}

	private List<JsonNode> getChildNodes( final JsonNode node, final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap,
			final int nodeLevel ) throws Exception
	{
		final List<JsonNode> childNodes = new ArrayList<>();
		if( nodeLevel < Config.getList( Config.REPORT_TAXONOMY_LEVELS ).size() )
		{
			// getting all JsonNodes for the nodeLevel
			final Set<JsonNode> jsonNodes = jsonMap
					.get( Config.getList( Config.REPORT_TAXONOMY_LEVELS ).get( nodeLevel ) );

			if( jsonNodes != null )
			{
				for( final JsonNode innerNode: jsonNodes )
				{
					if( innerNode.getParent().equals( node ) )
					{
						childNodes.add( innerNode );
					}
				}
			}
		}
		return childNodes;
	}

	private JsonNode getJsonNode( final String otu, final Set<JsonNode> jsonNodes, final String level )
	{
		if( jsonNodes != null )
		{
			for( final JsonNode jsonNode: jsonNodes )
			{
				if( jsonNode.getOtu().equals( otu ) && jsonNode.getLevel().equals( level ) )
				{
					return jsonNode;
				}
			}
		}
		return null;
	}

	private JsonNode getParent( final String parentOtu, final String parentLevel,
			final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap )
	{
		if( parentLevel != null && parentOtu != null && jsonMap != null && !parentLevel.isEmpty()
				&& !parentOtu.isEmpty() && !jsonMap.isEmpty() && jsonMap.get( parentLevel ) != null )
		{
			for( final JsonNode node: jsonMap.get( parentLevel ) )
			{
				if( node.getOtu().equals( parentOtu ) )
				{
					return node;
				}
			}
		}
		return null;
	}

	/**
	 * The root count = sum of sequence counts for OTUs in top level taxonomy.<br>
	 * Also log #sequences at each taxonomy level.
	 *
	 * @param LinkedHashMap jsonMap (key=level)
	 * @return long count
	 * @throws Exception if parent is missing for any node
	 */
	private long getRootCount( final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap ) throws Exception
	{
		Log.get( getClass() ).info( Log.LOG_SPACER );
		Log.get( getClass() ).info( "JSON OTU Report" );
		Log.get( getClass() ).info( Log.LOG_SPACER );
		long rootCount = 0L;
		final LinkedHashMap<String, Long> countMap = new LinkedHashMap<>();
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			long totalSeqs = 0L;
			int numOtus = 0;
			if( jsonMap.get( level ) != null )
			{
				for( final JsonNode jsonNode: jsonMap.get( level ) )
				{
					numOtus++;
					if( jsonNode.getParent() == null )
					{
						throw new Exception( "No parent found: " + level + " : " + jsonNode.getOtu() );
					}

					totalSeqs += jsonNode.getCount();
					Log.get( getClass() ).info( level + ":" + jsonNode.getOtu() + " [ parent:"
							+ jsonNode.getParent().getOtu() + " ] = " + jsonNode.getCount() );
				}
			}

			countMap.put( level, totalSeqs );
			if( rootCount == 0L && totalSeqs > 0 )
			{
				rootCount = totalSeqs; // sets top level count
			}

			summary.append( "# Unique OTU [ " + level + " ]: " + numOtus + RETURN );
		}

		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			summary.append( "# Total OTU [ " + level + " ]: " + countMap.get( level ) + RETURN );
		}

		return rootCount;
	}

	/**
	 * Get the taxonomy level reports from {@link biolockj.module.r.CalculateStats} for the given level
	 *
	 * @param String level
	 * @return File log normalized report
	 * @throws Exception if unable to obtain the report due to propagated exceptions
	 */
	private File[] getStatReports( final String level ) throws Exception
	{
		final File[] statReports = new File[ 5 ];
		statReports[ PAR_PVAL_INDEX ] = CalculateStats.getStatsFile( level, CalculateStats.P_VALS_PAR );
		statReports[ NON_PAR_PVAL_INDEX ] = CalculateStats.getStatsFile( level, CalculateStats.P_VALS_NP );
		statReports[ ADJ_PAR_PVAL_INDEX ] = CalculateStats.getStatsFile( level, CalculateStats.P_VALS_PAR_ADJ );
		statReports[ ADJ_NON_PAR_PVAL_INDEX ] = CalculateStats.getStatsFile( level, CalculateStats.P_VALS_NP_ADJ );
		statReports[ R2_PVAL_INDEX ] = CalculateStats.getStatsFile( level, CalculateStats.R_SQUARED_VALS );
		return statReports;
	}

	private LinkedHashMap<String, TreeSet<JsonNode>> updateNodeStats(
			final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final File stats, final String level )
			throws Exception
	{
		Log.get( getClass() ).info( "Adding stats from: " + stats.getAbsolutePath() );
		final BufferedReader reader = SeqUtil.getFileReader( stats );
		try
		{
			final List<String> columnNames = Arrays
					.asList( reader.readLine().replaceAll( "\"", "" ).split( TAB_DELIM ) );

			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line.replaceAll( "\"", "" ), TAB_DELIM );
				final String otu = st.nextToken().trim();

				int i = 0;
				final JsonNode jsonNode = getJsonNode( otu, jsonMap.get( level ), level );
				if( jsonNode != null )
				{
					while( st.hasMoreTokens() )
					{
						final String token = st.nextToken();
						if( NumberUtils.isNumber( token ) )
						{
							final double val = Double.parseDouble( token );
							final String statName = getStatsType( stats ) + "_" + columnNames.get( ++i );
							jsonNode.updateStats( statName, val );
						}
					}
				}
				else
				{
					Log.get( getClass() )
							.debug( "Missing OTU " + level + ": " + otu
									+ " (likely due to OTU network gap), as found in CalculateStats output: "
									+ stats.getAbsolutePath() );
				}
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		return jsonMap;
	}

	/**
	 * This method formats the JSON code to indent code blocks surround by curly-braces "{ }"
	 * 
	 * @param writer BufferedWriter writes to the file
	 * @param code JSON code
	 * @throws Exception if I/O errors occur
	 */
	private void writeJson( final String code ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter(
				new FileWriter( new File( getOutputDir().getAbsolutePath() + File.separator + JSON_SUMMARY ) ) );
		try
		{
			int indentCount = 0;
			final StringTokenizer st = new StringTokenizer( code, BioLockJ.RETURN );
			while( st.hasMoreTokens() )
			{
				final String line = st.nextToken();
				if( line.startsWith( "}" ) )
				{
					indentCount--;
				}

				int i = 0;
				while( i++ < indentCount )
				{
					writer.write( BioLockJ.TAB_DELIM );
				}

				writer.write( line + BioLockJ.RETURN );

				if( line.endsWith( "{" ) )
				{
					indentCount++;
				}
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

	private String writeNodeAndChildren( final JsonNode node, final boolean hasPeer,
			final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final int nodeLevel ) throws Exception
	{
		final String logBase = Config.getString( Config.REPORT_LOG_BASE );
		final String prefix = logBase == null ? "": "log" + logBase;

		final String taxaLevel = nodeLevel == 0 ? ROOT_NODE
				: Config.getList( Config.REPORT_TAXONOMY_LEVELS ).get( nodeLevel - 1 );

		final List<JsonNode> childNodes = getChildNodes( node, jsonMap, nodeLevel );

		final StringBuffer sb = new StringBuffer();
		sb.append( "{" + RETURN );
		sb.append( "\"" + OTU_NAME + "\": \"" + node.getOtu() + "\"," + RETURN );
		sb.append( "\"" + OTU_LEVEL + "\": \"" + taxaLevel + "\"," + RETURN );
		sb.append( "\"" + NUM_SEQS + "\": " + node.getCount() + "," + RETURN );

		if( node.getStats().size() > 0 )
		{
			for( final Iterator<String> stats = node.getStats().keySet().iterator(); stats.hasNext(); )
			{
				final String stat = stats.next();
				final String name = stat.startsWith( CalculateStats.R_SQUARED_VALS ) ? stat: prefix + "(" + stat + ")";
				sb.append( "\"" + name + "\": " + node.getStats().get( stat ) );
				sb.append( ( stats.hasNext() || !childNodes.isEmpty() ? ",": "" ) + RETURN );
			}
		}

		if( !childNodes.isEmpty() )
		{
			sb.append( "\"" + CHILDREN + "\": [" + RETURN );
			for( final Iterator<JsonNode> children = childNodes.iterator(); children.hasNext(); )
			{
				sb.append( writeNodeAndChildren( children.next(), children.hasNext(), jsonMap, nodeLevel + 1 ) );
			}
		}

		sb.append( "}" + ( hasPeer ? ",": nodeLevel != 0 ? " ]": "" ) + RETURN );
		return sb.toString();
	}

	private static String getStatsType( final File file ) throws Exception
	{
		if( file.getName().endsWith( CalculateStats.P_VALS_PAR + CalculateStats.TSV_EXT ) )
		{
			return CalculateStats.P_VALS_PAR;
		}
		if( file.getName().endsWith( CalculateStats.P_VALS_NP + CalculateStats.TSV_EXT ) )
		{
			return CalculateStats.P_VALS_NP;
		}
		if( file.getName().endsWith( CalculateStats.P_VALS_PAR_ADJ + CalculateStats.TSV_EXT ) )
		{
			return CalculateStats.P_VALS_PAR_ADJ;
		}
		if( file.getName().endsWith( CalculateStats.P_VALS_NP_ADJ + CalculateStats.TSV_EXT ) )
		{
			return CalculateStats.P_VALS_NP_ADJ;
		}
		if( file.getName().endsWith( CalculateStats.R_SQUARED_VALS + CalculateStats.TSV_EXT ) )
		{
			return CalculateStats.R_SQUARED_VALS;
		}

		throw new Exception( "Invalid Stats file: " + file.getAbsolutePath() );
	}

	private final Set<Map<String, String>> brokenOtuNetworks = new HashSet<>();
	private final StringBuffer summary = new StringBuffer();
	private static final int ADJ_NON_PAR_PVAL_INDEX = 3;
	private static final int ADJ_PAR_PVAL_INDEX = 2;
	private static final String CHILDREN = "children";
	private static final String JSON_SUMMARY = "summary.json";
	private static final int NON_PAR_PVAL_INDEX = 1;
	private static final String NUM_SEQS = "numSeqs";
	private static final String OTU_LEVEL = "taxaLevel";
	private static final String OTU_NAME = "otu";
	private static final int PAR_PVAL_INDEX = 0;
	private static final int R2_PVAL_INDEX = 4;
	private static final String ROOT_NODE = "root";
}
