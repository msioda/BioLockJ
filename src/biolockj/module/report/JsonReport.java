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
import biolockj.util.BioLockJUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.OtuUtil;

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

	/**
	 * Add summary with unique OTU counts/level.
	 */
	@Override
	public String getSummary() throws Exception
	{
		return super.getSummary() + summary.toString();
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
		verifyInput();
		final JsonNode root = new JsonNode( ROOT_NODE, 0, null, null );
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = buildJsonMap( root );
		root.addCount( totalOtuCount );
		if( hasStats() )
		{
			addStats( jsonMap, root );
		}

		writeJson( writeNodeAndChildren( root, false, jsonMap, 0 ) );
	}

	protected LinkedHashMap<String, TreeSet<JsonNode>> buildJsonMap( final JsonNode rootNode ) throws Exception
	{
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = initJsonMap();

		final Map<String, Integer> otuCounts = OtuUtil.compileSampleOtuCounts( getInputFiles().get( 0 ) );
		Log.info( getClass(), "Build JSON Nodes for " + otuCounts.size() + " unique OTUs..." );
		final Map<String, Integer> levelCounts = initTotalLevelTaxaCountMap();
		final Map<String, Set<String>> uniqueOtus = initUniqueLevelTaxaCountMap();
		for( final String otu: otuCounts.keySet() )
		{
			Log.debug( getClass(), "Add JSON otu " + otu );
			JsonNode parent = rootNode;
			final Map<String, String> taxaMap = OtuUtil.getTaxaByLevel( otu );
			for( final String level: taxaMap.keySet() )
			{
				final String taxa = taxaMap.get( level );
				final int otuCount = otuCounts.get( otu );
				Log.debug( getClass(), "JSON level " + level + " - taxa:" + taxa + " - otuCount: " + otuCount );
				final JsonNode jsonNode = new JsonNode( taxa, otuCount, parent, level );
				jsonMap.get( level ).add( jsonNode );

				uniqueOtus.get( level ).add( taxa );

				totalOtuCount += otuCount;

				levelCounts.put( level, levelCounts.get( level ) + otuCount );

				parent = jsonNode;
			}
		}

		updateSummary( levelCounts, uniqueOtus );

		return jsonMap;
	}

	/**
	 * Add stats from {@link biolockj.module.r.CalculateStats} into all of the {@link biolockj.node.JsonNode}s.
	 * 
	 * @param jsonMap LinkedHashMap(level,Set(JsonNode))
	 * @param stats Stats file
	 * @param level {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}
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

	protected void verifyInput() throws Exception
	{
		if( getInputFiles().size() != 1 )
		{
			throw new Exception( CompileOtuCounts.class.getName() + " should have exactly 1 output file, but found "
					+ getInputFiles().size() );
		}
	}

	protected String writeNodeAndChildren( final JsonNode node, final boolean hasPeer,
			final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final int nodeLevel ) throws Exception
	{
		final String logBase = Config.getString( Config.REPORT_LOG_BASE );
		final String prefix = logBase == null ? "": "log" + logBase;

		final String taxaLevel = nodeLevel == 0 ? ROOT_NODE
				: Config.requireList( Config.REPORT_TAXONOMY_LEVELS ).get( nodeLevel - 1 );

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

	private boolean hasStats() throws Exception
	{
		return ModuleUtil.moduleExists( CalculateStats.class.getName() )
				&& ModuleUtil.isComplete( ModuleUtil.getModule( CalculateStats.class.getName() ) );
	}

	private LinkedHashMap<String, TreeSet<JsonNode>> initJsonMap() throws Exception
	{
		final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap = new LinkedHashMap<>();
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			jsonMap.put( level, new TreeSet<JsonNode>() );
		}
		return jsonMap;
	}

	private Map<String, Integer> initTotalLevelTaxaCountMap() throws Exception
	{
		final Map<String, Integer> levelCount = new LinkedHashMap<>();
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			levelCount.put( level, 0 );
		}
		return levelCount;
	}

	private Map<String, Set<String>> initUniqueLevelTaxaCountMap() throws Exception
	{
		final Map<String, Set<String>> otuCountMap = new LinkedHashMap<>();
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			otuCountMap.put( level, new HashSet<>() );
		}
		return otuCountMap;
	}

	private void updateSummary( final Map<String, Integer> levelCounts, final Map<String, Set<String>> uniqueOtus )
			throws Exception
	{
		for( final String level: Config.getList( Config.REPORT_TAXONOMY_LEVELS ) )
		{
			summary.append( "# " + level + " Unique/Total OTU: " + uniqueOtus.get( level ).size() + "/"
					+ levelCounts.get( level ) + RETURN );
		}
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
		if( file.getName().endsWith( CalculateStats.P_VALS_PAR + TSV ) )
		{
			return CalculateStats.P_VALS_PAR;
		}
		if( file.getName().endsWith( CalculateStats.P_VALS_NP + TSV ) )
		{
			return CalculateStats.P_VALS_NP;
		}
		if( file.getName().endsWith( CalculateStats.P_VALS_PAR_ADJ + TSV ) )
		{
			return CalculateStats.P_VALS_PAR_ADJ;
		}
		if( file.getName().endsWith( CalculateStats.P_VALS_NP_ADJ + TSV ) )
		{
			return CalculateStats.P_VALS_NP_ADJ;
		}
		if( file.getName().endsWith( CalculateStats.R_SQUARED_VALS + TSV ) )
		{
			return CalculateStats.R_SQUARED_VALS;
		}

		throw new Exception( "Invalid Stats file: " + file.getAbsolutePath() );
	}

	private final StringBuffer summary = new StringBuffer();

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
