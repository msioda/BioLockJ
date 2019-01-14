package biolockj.module.report;

import java.io.*;
import java.util.*;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.r.CalculateStats;
import biolockj.node.JsonNode;
import biolockj.util.*;

/**
 * This BioModule is used to build a JSON file (summary.json) from pipeline OTU-metadata tables.
 */
public class JsonReport extends JavaModuleImpl implements JavaModule
{

	/**
	 * Module prerequisite: {@link biolockj.module.report.CompileOtuCounts}
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		final List<Class<?>> preReqs = super.getPreRequisiteModules();
		preReqs.add( CompileOtuCounts.class );
		return preReqs;
	}

	@Override
	public String getSummary() throws Exception
	{
		return super.getSummary() + summary;
	}

	/**
	 * Return TRUE if {biolockj.module.r.CalculateStats} is part of the pipeline.
	 * 
	 * @return boolean
	 * @throws Exception if errors occur
	 */
	public boolean hasStats() throws Exception
	{
		return ModuleUtil.moduleExists( CalculateStats.class.getName() )
				&& ModuleUtil.isComplete( ModuleUtil.getModule( CalculateStats.class.getName() ) );
	}

	@Override
	public boolean isValidInputModule( final BioModule previousModule ) throws Exception
	{
		return previousModule instanceof CompileOtuCounts;
	}

	/**
	 * Obtain parsed sample data, build root node, and create the jsonMap by passing both to buildMap(). Set ROOT_NODE
	 * #seqs with getRootCount(jsonMap), add stats info to the jsonNodes, and finally build the JSON file.
	 */
	@Override
	public void runModule() throws Exception
	{
		final JsonNode root = new JsonNode( ROOT_NODE, 0, null, null );
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = buildJsonMap( root );
		root.addCount( totalOtuCount );
		if( hasStats() )
		{
			summary += "with summary statistics";
			addStats( jsonMap, root );
		}

		writeJson( writeNodeAndChildren( root, false, jsonMap, 0 ) );

		summary = "Report generated " + numberOfNodes + " nodes " + summary;
	}

	/**
	 * Build JsonMap from the {@link biolockj.module.report.CompileOtuCounts} output directory.
	 * 
	 * @param rootNode Root JsonNode is top of the hierarchy
	 * @return Map(level, Set(JsonNode)) of nodes by level
	 * @throws Exception if errors occur
	 */
	protected LinkedHashMap<String, TreeSet<JsonNode>> buildJsonMap( final JsonNode rootNode ) throws Exception
	{
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = initJsonMap();
		final Map<String, Integer> otuCounts = OtuUtil.compileSampleOtuCounts( getInputFiles().get( 0 ) );
		Log.info( getClass(), "Build JSON Nodes for " + otuCounts.size() + " unique OTUs..." );
		for( final String otu: otuCounts.keySet() )
		{
			Log.debug( getClass(), "Add JSON otu " + otu );
			JsonNode parent = rootNode;
			final int otuCount = otuCounts.get( otu );
			final Map<String, String> taxaMap = TaxaUtil.getTaxaByLevel( otu );
			for( final String level: TaxaUtil.getTaxaLevels() )
			{
				final String taxa = taxaMap.get( level );
				JsonNode jsonNode = getNode( jsonMap.get( level ), taxa );

				if( jsonNode == null )
				{
					jsonNode = new JsonNode( taxa, otuCount, parent, level );
					numberOfNodes++;
				}
				else
				{
					jsonNode.addCount( otuCount );
				}

				jsonMap.get( level ).add( jsonNode );

				parent = jsonNode;
			}

			totalOtuCount += otuCount;
		}

		return jsonMap;
	}

	/**
	 * Add stats from {@link biolockj.module.r.CalculateStats} into all of the {@link biolockj.node.JsonNode}s.
	 * 
	 * @param jsonMap LinkedHashMap(level,Set(JsonNode))
	 * @param stats Stats file
	 * @param level {@link biolockj.Config}.{@value biolockj.util.TaxaUtil#REPORT_TAXONOMY_LEVELS}
	 * @return LinkedHashMap(level,Set(JsonNode))
	 * @throws Exception if errors occur
	 */
	protected LinkedHashMap<String, TreeSet<JsonNode>> updateNodeStats(
			final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final File stats, final String level )
			throws Exception
	{
		Log.info( getClass(), "Adding stats from: " + stats.getAbsolutePath() );
		final BufferedReader reader = BioLockJUtil.getFileReader( stats );
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
					Log.warn( getClass(),
							"Missing Taxa " + level + ": " + otu + " from CalculateStats: " + stats.getAbsolutePath() );
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
	 * Build lines of text output for the Json Report file.
	 * 
	 * @param node JsonNode is the parent node
	 * @param hasPeer boolean is true if node has peer nodes
	 * @param jsonMap LinkedHashMap(level,Set(JsonNode)) all nodes by level
	 * @param nodeLevel {@link biolockj.Config}.{@value biolockj.util.TaxaUtil#REPORT_TAXONOMY_LEVELS}
	 * @return Json Report lines
	 * @throws Exception if errors occur
	 */
	protected String writeNodeAndChildren( final JsonNode node, final boolean hasPeer,
			final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final int nodeLevel ) throws Exception
	{
		final String logBase = Config.getString( Config.REPORT_LOG_BASE );
		final String prefix = logBase == null ? "": "log" + logBase;

		final String taxaLevel = nodeLevel == 0 ? ROOT_NODE: TaxaUtil.getTaxaLevels().get( nodeLevel - 1 );

		final List<JsonNode> childNodes = getChildNodes( node, jsonMap, nodeLevel );

		final StringBuffer sb = new StringBuffer();
		sb.append( "{" + RETURN );
		sb.append( "\"" + OTU_NAME + "\": \"" + node.getTaxa() + "\"," + RETURN );
		sb.append( "\"" + OTU_LEVEL + "\": \"" + taxaLevel + "\"," + RETURN );
		sb.append( "\"" + NUM_SEQS + "\": " + node.getCount()
				+ ( node.getStats().isEmpty() && childNodes.isEmpty() ? "": "," ) + RETURN );

		if( !node.getStats().isEmpty() )
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
		Log.info( getClass(), "Adding stats to JSON nodes..." );
		for( final String level: TaxaUtil.getTaxaLevels() )
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

	private List<JsonNode> getChildNodes( final JsonNode node, final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap,
			final int nodeLevel ) throws Exception
	{
		final List<JsonNode> childNodes = new ArrayList<>();
		if( nodeLevel < TaxaUtil.getTaxaLevels().size() )
		{
			// getting all JsonNodes for the nodeLevel
			final Set<JsonNode> jsonNodes = jsonMap.get( TaxaUtil.getTaxaLevels().get( nodeLevel ) );

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
				if( jsonNode.getTaxa().equals( otu ) && jsonNode.getLevel().equals( level ) )
				{
					return jsonNode;
				}
			}
		}
		return null;
	}

	private JsonNode getNode( final Set<JsonNode> nodes, final String taxa ) throws Exception
	{
		for( final JsonNode node: nodes )
		{
			if( node.getTaxa().equals( taxa ) )
			{
				return node;
			}
		}
		return null;
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

	private LinkedHashMap<String, TreeSet<JsonNode>> initJsonMap() throws Exception
	{
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = new LinkedHashMap<>();
		for( final String level: TaxaUtil.getTaxaLevels() )
		{
			jsonMap.put( level, new TreeSet<JsonNode>() );
		}
		return jsonMap;
	}

	/**
	 * This method formats the JSON code to indent code blocks surround by curly-braces "{ }"
	 * 
	 * @param code JSON syntax markup
	 * @throws Exception if errors occur
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

	private static String getStatsType( final File file ) throws Exception
	{
		if( file.getName().endsWith( CalculateStats.P_VALS_PAR_ADJ + TSV_EXT ) )
		{
			return CalculateStats.P_VALS_PAR_ADJ;
		}
		else if( file.getName().endsWith( CalculateStats.P_VALS_PAR + TSV_EXT ) )
		{
			return CalculateStats.P_VALS_PAR;
		}
		if( file.getName().endsWith( CalculateStats.P_VALS_NP_ADJ + TSV_EXT ) )
		{
			return CalculateStats.P_VALS_NP_ADJ;
		}
		else if( file.getName().endsWith( CalculateStats.P_VALS_NP + TSV_EXT ) )
		{
			return CalculateStats.P_VALS_NP;
		}
		if( file.getName().endsWith( CalculateStats.R_SQUARED_VALS + TSV_EXT ) )
		{
			return CalculateStats.R_SQUARED_VALS;
		}

		throw new Exception( "Invalid Stats file: " + file.getAbsolutePath() );
	}

	private int numberOfNodes = 1; // root always created

	private String summary = "";
	private int totalOtuCount = 0;
	private static final int ADJ_NON_PAR_PVAL_INDEX = 3;
	private static final int ADJ_PAR_PVAL_INDEX = 2;
	private static final String CHILDREN = "children";
	private static final String JSON_SUMMARY = "otuSummary.json";
	private static final int NON_PAR_PVAL_INDEX = 1;
	private static final String NUM_SEQS = "numSeqs";
	private static final String OTU_LEVEL = "taxaLevel";
	private static final String OTU_NAME = "otu";
	private static final int PAR_PVAL_INDEX = 0;
	private static final int R2_PVAL_INDEX = 4;
	private static final String ROOT_NODE = "root";
}
