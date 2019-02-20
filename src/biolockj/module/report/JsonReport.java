package biolockj.module.report;

import java.io.*;
import java.util.*;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.JavaModule;
import biolockj.module.JavaModuleImpl;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.wgs.Humann2Classifier;
import biolockj.module.report.otu.CompileOtuCounts;
import biolockj.module.report.r.R_CalculateStats;
import biolockj.node.JsonNode;
import biolockj.util.*;

/**
 * This BioModule is used to build a JSON file (summary.json) compiled from all OTUs in the dataset.
 * @web_desc Json Report
 */
public class JsonReport extends JavaModuleImpl implements JavaModule
{

	/**
	 * Module prerequisite: {@link biolockj.module.report.otu.CompileOtuCounts}
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception
	{
		final List<String> preReqs = new ArrayList<>();
		if( !pipelineInputContainsOtuSummary() )
		{
			preReqs.add( CompileOtuCounts.class.getName() );
		}

		preReqs.addAll( super.getPreRequisiteModules() );
		return preReqs;
	}

	@Override
	public String getSummary() throws Exception
	{
		return super.getSummary() + summary;
	}

	@Override
	public boolean isValidInputModule( final BioModule module )
	{
		return module instanceof CompileOtuCounts;
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
		root.addCount( totalTaxaCount );
		if( hasStats() )
		{
			summary += "with summary statistics";
			addStats( jsonMap, root );
		}

		writeJson( writeNodeAndChildren( root, false, jsonMap, 0 ) );

		summary = "Report generated " + numberOfNodes + " nodes " + summary;
	}

	/**
	 * Build JsonMap from the {@link biolockj.module.report.otu.CompileOtuCounts} output directory.
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

			totalTaxaCount += otuCount;
		}

		return jsonMap;
	}

	/**
	 * Check pipeline input to see if OTU summary file is the only pipeline input file.
	 *
	 * @return TRUE if pipeline input
	 * @throws Exception if errors occur
	 */
	protected boolean pipelineInputContainsOtuSummary() throws Exception
	{
		final Iterator<File> it = BioLockJUtil.getPipelineInputFiles().iterator();
		while( it.hasNext() )
		{
			if( it.next().getName().endsWith( getInputFileSuffix() ) )
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Add stats from {@link biolockj.module.report.r.R_CalculateStats} into all of the {@link biolockj.node.JsonNode}s.
	 *
	 * @param jsonMap LinkedHashMap(level,Set(JsonNode))
	 * @param stats Stats file
	 * @param level {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * @param label Label to use in node statistics
	 * @return LinkedHashMap(level,Set(JsonNode))
	 * @throws Exception if errors occur
	 */
	protected LinkedHashMap<String, TreeSet<JsonNode>> updateNodeStats(
			final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final File stats, final String level,
			final String label ) throws Exception
	{
		Log.info( getClass(), "Adding " + label + " stats from: " + stats.getAbsolutePath() );
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
							jsonNode.updateStats( label + "_" + columnNames.get( ++i ), Double.parseDouble( token ) );
						}
					}
				}
				else
				{
					Log.warn( getClass(), "Missing Taxa " + level + ": " + otu + " from R_CalculateStats: "
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
	 * Build lines of text output for the Json Report file.
	 *
	 * @param node JsonNode is the parent node
	 * @param hasPeer boolean is true if node has peer nodes
	 * @param jsonMap LinkedHashMap(level,Set(JsonNode)) all nodes by level
	 * @param nodeLevel {@link biolockj.Config}.{@value biolockj.Constants#REPORT_TAXONOMY_LEVELS}
	 * @return Json Report lines
	 * @throws Exception if errors occur
	 */
	protected String writeNodeAndChildren( final JsonNode node, final boolean hasPeer,
			final LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final int nodeLevel ) throws Exception
	{
		final String logBase = Config.getString( this, Constants.REPORT_LOG_BASE );
		final String prefix = logBase == null ? "": "log" + logBase;

		final String taxaLevel = nodeLevel == 0 ? ROOT_NODE: TaxaUtil.getTaxaLevels().get( nodeLevel - 1 );

		final List<JsonNode> childNodes = getChildNodes( node, jsonMap, nodeLevel );

		final StringBuffer sb = new StringBuffer();
		sb.append( "{" + RETURN );
		sb.append( "\"" + TAXA + "\": \"" + node.getTaxa() + "\"," + RETURN );
		sb.append( "\"" + OTU_LEVEL + "\": \"" + taxaLevel + "\"," + RETURN );
		sb.append( "\"" + NUM_SEQS + "\": " + node.getCount()
				+ ( node.getStats().isEmpty() && childNodes.isEmpty() ? "": "," ) + RETURN );

		if( !node.getStats().isEmpty() )
		{
			for( final Iterator<String> stats = node.getStats().keySet().iterator(); stats.hasNext(); )
			{
				final String stat = stats.next();
				final String name = stat.startsWith( R_CalculateStats.getSuffix(null, false) ) ? stat
						: prefix + "(" + stat + ")";
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
	 * Statistics can be found for all taxonomy levels in the output from
	 * {@link biolockj.module.report.r.R_CalculateStats}, which is also the JsonReport inputDir() since it must be
	 * configured as the
	 *
	 * @param jsonMap jsonMap (key=level)
	 * @param root JsonNode
	 * @throws Exception if unable to parse report files
	 */
	private void addStats( LinkedHashMap<String, TreeSet<JsonNode>> jsonMap, final JsonNode root ) throws Exception
	{
		Log.info( getClass(), "Adding stats to JSON nodes..." );
		for( final String level: TaxaUtil.getTaxaLevels() )
		{
			final Map<String, File> statReports = getAllStatReports( level );
			for( final String name: statReports.keySet() )
			{
				final File file = statReports.get( name );
				if( file != null )
				{
					jsonMap = updateNodeStats( jsonMap, file, level, name );
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

	private String getInputFileSuffix() throws Exception
	{
		return CompileOtuCounts.SUMMARY + Constants.OTU_COUNT + TSV_EXT;
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
	 * Get the taxonomy level reports from {@link biolockj.module.report.r.R_CalculateStats} for the given level
	 *
	 * @param level String
	 * @return File log normalized report
	 * @throws Exception if unable to obtain the report due to propagated exceptions
	 */
	private Map<String, File> getAllStatReports( final String level ) throws Exception
	{
		Map<String, File> statReports = new LinkedHashMap<>();
		final ClassifierModule classifier = ModuleUtil.getClassifier( this, false );
		if( classifier != null && classifier instanceof Humann2Classifier )
		{
			if( !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_ABUNDANCE ) )
			{
				Map<String, File> reports = getStatReports( Constants.HN2_PATH_ABUND_SUM + "_" + level );
				for( String key : reports.keySet() )
				{
					statReports.put( key, reports.get( key ) );
				}
			}
			if( !Config.getBoolean( this, Constants.HN2_DISABLE_PATH_COVERAGE ) )
			{
				Map<String, File> reports = getStatReports( Constants.HN2_PATH_COVG_SUM + "_" + level );
				for( String key : reports.keySet() )
				{
					statReports.put( key, reports.get( key ) );
				}
			}
			if( !Config.getBoolean( this, Constants.HN2_DISABLE_GENE_FAMILIES ) )
			{
				Map<String, File> reports = getStatReports( Constants.HN2_GENE_FAM_SUM + "_" + level );
				for( String key : reports.keySet() )
				{
					statReports.put( key, reports.get( key ) );
				}
			}
		}
		else
		{
			statReports = getStatReports( level );
		}
		
		return statReports;
	}
	
	private Map<String, File> getStatReports( final String type ) throws Exception
	{
		final Map<String, File> statReports = new LinkedHashMap<>();

		final ClassifierModule classifier = ModuleUtil.getClassifier( this, false );
		if( classifier != null && classifier instanceof Humann2Classifier )
		{
			statReports.put( "parPval", R_CalculateStats.getStatsFile( this, type, true, false ) );
			statReports.put( "nonParPval", R_CalculateStats.getStatsFile( this, type, false, false ) );
			statReports.put( "adjParPval", R_CalculateStats.getStatsFile( this, type, true, true ) );
			statReports.put( "adjNonParPval", R_CalculateStats.getStatsFile( this, type, false, true ) );
			statReports.put( "rSquared", R_CalculateStats.getStatsFile( this, type, false, false ) );
		}
		else
		{
			
		}
		
		return statReports;
	}
	

	private boolean hasStats() throws Exception
	{
		return ModuleUtil.getModule( this, R_CalculateStats.class.getName(), false ) != null;
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
	 * @param code JSON syntax mark-up
	 * @throws Exception if errors occur
	 */
	private void writeJson( final String code ) throws Exception
	{
		final BufferedWriter writer = new BufferedWriter(
				new FileWriter( new File( getOutputDir().getAbsolutePath() + File.separator + JSON_SUMMARY ) ) );
		try
		{
			int indentCount = 0;
			final StringTokenizer st = new StringTokenizer( code, Constants.RETURN );
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
					writer.write( Constants.TAB_DELIM );
				}

				writer.write( line + Constants.RETURN );

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

	private int numberOfNodes = 1; // root always created

	private String summary = "";
	private int totalTaxaCount = 0;
	private static final String CHILDREN = "children";
	private static final String JSON_SUMMARY = "otuSummary.json";
	private static final String NUM_SEQS = "numSeqs";
	private static final String OTU_LEVEL = "taxaLevel";
	private static final String ROOT_NODE = "root";
	private static final String TAXA = "taxa";
}
