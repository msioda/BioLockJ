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
package biolockj.module.implicit.qiime;

import java.io.*;
import java.util.*;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.module.classifier.r16s.QiimeOpenRefClassifier;
import biolockj.module.seq.PearMergeReads;
import biolockj.util.*;

/**
 * This BioModule generates the bash script used to create QIIME summary scripts, taxonomy-level reports, and add alpha
 * diversity metrics (if configured) to the metadata file.<br>
 * For a complete list of available metrics, see:
 * <a href= "http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html" target=
 * "_top">http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html</a><br>
 */
public class QiimeClassifier extends ClassifierModuleImpl implements ClassifierModule
{

	/**
	 * Generate bash script lines to summarize QIIME results, build taxonomy reports, and add alpha diversity metrics.
	 * <p>
	 * The QiimeClassifier script begins with the following QIIME scripts:
	 * <ol>
	 * <li>{@value #SCRIPT_PRINT_CONFIG} - logs version/environment info
	 * <li>{@value #SCRIPT_SUMMARIZE_TAXA} - processes {@value #OTU_TABLE} to create taxonomy-level reports in the
	 * output directory
	 * <li>{@value #SCRIPT_SUMMARIZE_BIOM} - processes {@value #OTU_TABLE} to create summary file:
	 * output/{@value #OTU_SUMMARY_FILE}
	 * </ol>
	 * <p>
	 * If {@link biolockj.Config}.{@value #QIIME_ALPHA_DIVERSITY_METRICS} are defined, add lines to run additional
	 * scripts:
	 * <ol>
	 * <li>{@value #SCRIPT_CALC_ALPHA_DIVERSITY} - calculates {@value #QIIME_ALPHA_DIVERSITY_METRICS} on
	 * {@value #OTU_TABLE} to create output/{@value #ALPHA_DIVERSITY_TABLE}
	 * <li>{@value #SCRIPT_ADD_ALPHA_DIVERSITY} - adds {@value #ALPHA_DIVERSITY_TABLE} data to
	 * {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_FILE_PATH}
	 * <li>
	 * </ol>
	 * <p>
	 * For complete list of skbio.diversity.alpha options, see
	 * <a href= "http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html" target=
	 * "_top">http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html</a>
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final List<List<String>> data = new ArrayList<>();
		final List<String> lines = new ArrayList<>();
		lines.add( SCRIPT_PRINT_CONFIG + " -t" );
		lines.add( SCRIPT_SUMMARIZE_TAXA + " -a --" + SUMMARIZE_TAXA_SUPPRESS_BIOM + " -i " + files.get( 0 ) + " -L "
				+ getQiimeTaxaLevels() + " -o " + outDir );
		lines.add( SCRIPT_SUMMARIZE_BIOM + " -i " + files.get( 0 ) + " -o " + tempDir + OTU_SUMMARY_FILE );
		if( Config.getString( QIIME_ALPHA_DIVERSITY_METRICS ) != null )
		{
			final File newMapping = new File( tempDir + MetaUtil.getMetadataFileName() );
			lines.add( SCRIPT_CALC_ALPHA_DIVERSITY + " -i " + files.get( 0 ) + " -m " + getAlphaDiversityMetrics() 
				+ " -o " + tempDir + ALPHA_DIVERSITY_TABLE );

			lines.add( SCRIPT_ADD_ALPHA_DIVERSITY + " -m " + MetaUtil.getFile().getAbsolutePath() + " -i " + tempDir
					+ ALPHA_DIVERSITY_TABLE + " -o " + newMapping );
			MetaUtil.setFile( newMapping );
		}

		data.add( lines );

		return data;
	}
	
	private String getAlphaDiversityMetrics() throws Exception
	{
		StringBuffer sb = new StringBuffer();
		Iterator<String> metrics = Config.requireList( QIIME_ALPHA_DIVERSITY_METRICS ).iterator();
		sb.append( metrics.next() );
		while( metrics.hasNext() )
		{
			sb.append( "," ).append( metrics.next() );
		}
		return sb.toString();
	}

	/**
	 * QIIME does not support paired reads
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		throw new Exception(
				"QIIME does not support paired reads!  Prerequisite BioModule " + PearMergeReads.class.getName()
						+ " should be added your pipeline Config: " + Config.getConfigFileName() );
	}

	@Override
	public void checkDependencies() throws Exception
	{
		if( getClass().equals( QiimeClassifier.class ) )
		{
			boolean foundOtuPickingModule = false;
			for( final BioModule module: Pipeline.getModules() )
			{
				if( module.getClass().getPackage().equals( QiimeOpenRefClassifier.class.getPackage() ) )
				{
					foundOtuPickingModule = true;
				}
			}

			if( !foundOtuPickingModule )
			{
				throw new Exception( "QIIME pipelines require an OTU Picking module from package: "
						+ QiimeOpenRefClassifier.class.getPackage() );
			}
		}

		super.checkDependencies();
	}

	/**
	 * The cleanUp operation builds a new metadata file if alpha diversity metrics were generated by this module. The
	 * script {@value #SCRIPT_ADD_ALPHA_DIVERSITY} outputs {@value #ALPHA_DIV_NULL_VALUE} for null values which must be
	 * replaced by {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_NULL_VALUE} if any are found.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		final List<String> metrics = Config.getList( QIIME_ALPHA_DIVERSITY_METRICS );
		if( ModuleUtil.isComplete( this ) || !getClass().equals( QiimeClassifier.class ) || metrics.isEmpty()
				|| Config.requireString( MetaUtil.META_NULL_VALUE ).equals( ALPHA_DIV_NULL_VALUE ) )
		{
			return; // nothing to do
		}

		MetaUtil.refreshCache(); // to get the new alpha metric fields
		final BufferedReader reader = BioLockJUtil.getFileReader( MetaUtil.getFile() );
		MetaUtil.setFile(
				new File( getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getMetadataFileName() ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( MetaUtil.getFile() ) );
		final int numCols = MetaUtil.getFieldNames().size() - metrics.size() * 3;

		try
		{
			final String headers = reader.readLine();
			writer.write( headers + RETURN );

			Log.warn( getClass(), "Build new METADATA FILE with headers: " + headers );

			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, TAB_DELIM );
				writer.write( st.nextToken() ); // write ID col
				for( int i = 0; i < numCols; i++ ) // write all standard columns
				{
					writer.write( TAB_DELIM + st.nextToken() );
				}

				// replace any N/A values with configured MetaUtil.META_ULL_VALUE
				while( st.hasMoreTokens() )
				{
					String token = st.nextToken();
					if( token.equals( ALPHA_DIV_NULL_VALUE ) )
					{
						token = Config.requireString( MetaUtil.META_NULL_VALUE );
					}
					writer.write( TAB_DELIM + token );
				}
				writer.write( RETURN );
			}
		}
		finally
		{
			reader.close();
			writer.close();
			MetaUtil.refreshCache(); // to print out the new alpha metric fields
		}
	}

	/**
	 * QIIME does not use this method, instead specific scripts are called based on OTU picking method.
	 *
	 * @return null
	 */
	@Override
	public String getClassifierExe() throws Exception
	{
		return null;
	}

	/**
	 * Get optional list of parameters from {@link biolockj.Config}
	 * {@value biolockj.module.classifier.ClassifierModule#EXE_CLASSIFIER_PARAMS} to append whenever the OTU picking
	 * script is used.
	 */
	@Override
	public String getClassifierParams() throws Exception
	{
		String formattedSwitches = " ";
		final List<String> switches = Config.getList( EXE_CLASSIFIER_PARAMS );
		final Iterator<String> it = switches.iterator();
		while( it.hasNext() )
		{
			final String token = it.next();
			final StringTokenizer sToken = new StringTokenizer( token, " " );
			if( sToken.nextToken().length() == 1 )
			{
				formattedSwitches += "-" + token + " ";
			}
			else
			{
				formattedSwitches += "--" + token + " ";
			}
		}

		return formattedSwitches;
	}

	/**
	 * Subclasses of QiimeClassifier add post-requisite module: {@link biolockj.module.implicit.qiime.QiimeClassifier}.
	 * Only the QiimeClassifier itself adds the QiimeParser as a post-requisite module.
	 */
	@Override
	public List<Class<?>> getPostRequisiteModules() throws Exception
	{
		final List<Class<?>> postReqs = new ArrayList<>();
		if( !getClass().equals( QiimeClassifier.class ) )
		{
			postReqs.add( QiimeClassifier.class );
		}
		else
		{
			postReqs.addAll( super.getPostRequisiteModules() );
		}

		return postReqs;
	}

	/**
	 * If paired reads found, add prerequisite module: {@link biolockj.module.seq.PearMergeReads}. If sequences are not
	 * fasta format, add prerequisite module: {@link biolockj.module.seq.AwkFastaConverter}. Subclasses of
	 * QiimeClassifier add prerequisite module: {@link biolockj.module.implicit.qiime.BuildQiimeMapping}.
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		final List<Class<?>> preReqs = super.getPreRequisiteModules();
		if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			preReqs.add( Class.forName( BioModuleFactory.getDefaultMergePairedReadsConverter() ) );
		}
		if( !SeqUtil.isFastA() )
		{
			preReqs.add( Class.forName( BioModuleFactory.getDefaultFastaConverter() ) );
		}
		if( !getClass().equals( QiimeClassifier.class ) )
		{
			preReqs.add( BuildQiimeMapping.class );
		}
		return preReqs;
	}

	/**
	 * This method extends the classifier summary by adding the Qiime OTU summary metrics.
	 */
	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			final String endString = "Counts/sample detail:";
			final File otuSummary = new File( getTempDir().getAbsolutePath() + File.separator + OTU_SUMMARY_FILE );
			if( otuSummary.exists() )
			{
				final BufferedReader reader = BioLockJUtil.getFileReader( otuSummary );
				sb.append( "OTU Summary" + RETURN );
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					if( line.trim().equals( endString ) )
					{
						break;
					}
					if( !line.trim().isEmpty() )
					{
						sb.append( line + RETURN );
					}
				}

				reader.close();
			}
			return super.getSummary() + sb.toString();
		}
		catch( final Exception ex )
		{
			Log.warn( getClass(), "Unable to complete module summary: " + ex.getMessage() );
		}

		return super.getSummary();
	}

	/**
	 * Subclasses call this method to check dependencies before picking OTUs to validate
	 * {@link biolockj.Config}.{@value biolockj.module.classifier.ClassifierModule#EXE_CLASSIFIER_PARAMS}
	 *
	 * @throws Exception if
	 * {@link biolockj.Config}.{@value biolockj.module.classifier.ClassifierModule#EXE_CLASSIFIER_PARAMS} contains
	 * invalid parameters
	 */
	protected void checkOtuPickingDependencies() throws Exception
	{
		switches = getClassifierParams();
		if( switches.indexOf( "-i " ) > -1 || switches.indexOf( "--input_fp " ) > -1 )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-i or --input_fp) FOUND IN PROPERTY ("
					+ EXE_CLASSIFIER_PARAMS + "). PLEASE REMOVE.  INPUT DETERMINED BY: " + Config.INPUT_DIRS );
		}
		if( switches.indexOf( "-o " ) > -1 || switches.indexOf( "--output_dir " ) > -1 )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-o or --output_dir) FOUND IN PROPERTY ("
					+ EXE_CLASSIFIER_PARAMS + "). PLEASE REMOVE THIS VALUE FROM PROPERTY FILE. " );
		}
		if( switches.indexOf( "-a " ) > -1 || switches.indexOf( "-O " ) > -1 )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-a or -O) FOUND IN PROPERTY (" + EXE_CLASSIFIER_PARAMS
					+ "). BIOLOCKJ DERIVES THIS VALUE FROM: " + getNumThreadsParam() );
		}
		if( switches.indexOf( "-f " ) > -1 )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-f or --force) FOUND IN PROPERTY (" + EXE_CLASSIFIER_PARAMS
					+ "). OUTPUT OPTIONS AUTOMATED BY BIOLOCKJ." );
		}

		addHardCodedSwitches();
	}

	/**
	 * Module input directories are set to the previous module output directory.<br>
	 * To ensure we use the correct path, get path from {@link #getInputFiles()}
	 *
	 * @return File directory containing module input files
	 * @throws Exception if propagated by {@link #getInputFiles()}
	 */
	protected File getInputFileDir() throws Exception
	{
		final String inDir = getInputFiles().get( 0 ).getAbsolutePath();
		final int i = inDir.indexOf( File.separator + getInputFiles().get( 0 ).getName() );
		final File dir = new File( inDir.substring( 0, i ) );
		if( !dir.exists() )
		{
			throw new Exception( "Module input directory not found! --> " + dir.getAbsolutePath() );
		}

		return dir;
	}

	/**
	 * Subclasses call this method to add OTU picking lines by calling {@value #SCRIPT_ADD_LABELS} via OTU picking
	 * script. Sleep for 5 seconds before running so that freshly created batch fasta files and mapping files can be
	 * found on the file system.
	 *
	 * @param otuPickingScript QIIME script
	 * @param fastaDir Fasta File directory
	 * @param mapping File-path of mapping file
	 * @param outputDir Directory to output {@value #COMBINED_FNA}
	 * @return 2 script lines for the bash script
	 */
	protected List<String> getPickOtuLines( final String otuPickingScript, final File fastaDir, final String mapping,
			final File outputDir )
	{
		final List<String> lines = new ArrayList<>();
		lines.add( "sleep 5s" );
		lines.add( SCRIPT_ADD_LABELS + " -n 1 -i " + fastaDir.getAbsolutePath() + " -m " + mapping + " -c "
				+ DEMUX_COLUMN + " -o " + outputDir.getAbsolutePath() );
		final String fnaFile = outputDir + File.separator + COMBINED_FNA;
		lines.add( otuPickingScript + switches + "-i " + fnaFile + " -fo " + outputDir );
		return lines;
	}

	/**
	 * Get Vsearch params from the prop file and format switches for the bash script.
	 *
	 * @return Formatted vsearch parameters, if any, else an empty string
	 * @throws Exception if errors occur
	 */
	protected String getVsearchParams() throws Exception
	{
		String formattedSwitches = " ";
		final List<String> params = Config.requireList( EXE_VSEARCH_PARAMS );
		for( final String string: params )
		{
			formattedSwitches += "--" + string + " ";
		}

		formattedSwitches += "--threads " + getNumThreads() + " ";

		return formattedSwitches;
	}

	/**
	 * Typically we verify no duplicate file names are used, but for QIIME we may be combining multiple files with the
	 * same name ({@value #OTU_TABLE}), so QiimeClassifier skips this validation.
	 */
	@Override
	protected void validateFileNameUnique( final Set<String> fileNames, final File file ) throws Exception
	{
		// Not needed for QIIME. Multiple file named otu_table.biom & others exist.
	}

	/**
	 * Set the number of threads used in QIIME scripts as defined in
	 * {@link biolockj.Config}.{@value biolockj.Config#SCRIPT_NUM_THREADS}.
	 *
	 * @throws Exception if errors occur
	 */
	private void addHardCodedSwitches() throws Exception
	{
		switches += "-aO" + " " + getNumThreads() + " ";
	}

	/**
	 * Set the taxonomy level indicators.
	 *
	 * @return QIIME taxonomy level numbers as a comma separated list
	 */
	private String getQiimeTaxaLevels()
	{
		String levels = "";
		if( TaxaUtil.getTaxaLevels().contains( TaxaUtil.DOMAIN ) )
		{
			levels += "1";
		}
		if( TaxaUtil.getTaxaLevels().contains( TaxaUtil.PHYLUM ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "2";
		}
		if( TaxaUtil.getTaxaLevels().contains( TaxaUtil.CLASS ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "3";
		}
		if( TaxaUtil.getTaxaLevels().contains( TaxaUtil.ORDER ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "4";
		}
		if( TaxaUtil.getTaxaLevels().contains( TaxaUtil.FAMILY ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "5";
		}
		if( TaxaUtil.getTaxaLevels().contains( TaxaUtil.GENUS ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "6";
		}
		if( TaxaUtil.getTaxaLevels().contains( TaxaUtil.SPECIES ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "7";
		}

		return levels;
	}

	private String switches = null;

	/**
	 * File produced by QIIME {@value #SCRIPT_CALC_ALPHA_DIVERSITY} script: {@value #ALPHA_DIVERSITY_TABLE}
	 */
	public static final String ALPHA_DIVERSITY_TABLE = "alphaDiversity" + TXT_EXT;

	/**
	 * Multiplexed fasta file produced by QIIME {@value #SCRIPT_ADD_LABELS} script: {@value #COMBINED_FNA}
	 */
	public static final String COMBINED_FNA = "combined_seqs.fna";

	/**
	 * QIIME mapping column created by {@link biolockj.module.implicit.qiime.BuildQiimeMapping} that stores the name of
	 * the original fasta file associated with the sample: {@value #DEMUX_COLUMN}
	 */
	public static final String DEMUX_COLUMN = "BioLockJFileName";

	/**
	 * {@link biolockj.Config} property for vsearch exectuable used for chimera detection: {@value #EXE_VSEARCH}
	 */
	public static final String EXE_VSEARCH = "exe.vsearch";

	/**
	 * {@link biolockj.Config} property for {@value #EXE_VSEARCH} parameters (such as alternate reference database
	 * path): {@value #EXE_VSEARCH_PARAMS}
	 */
	public static final String EXE_VSEARCH_PARAMS = "exe.vsearchParams";

	/**
	 * File produced by QIIME {@value #SCRIPT_SUMMARIZE_BIOM} script: {@value #OTU_SUMMARY_FILE}
	 */
	public static final String OTU_SUMMARY_FILE = "otuSummary" + TXT_EXT;

	/**
	 * File produced by OTU picking scripts holding read taxonomy assignments: {@value #OTU_TABLE}
	 */
	public static final String OTU_TABLE = "otu_table.biom";

	/**
	 * OTU table prefix: {@value #OTU_TABLE_PREFIX}
	 */
	public static final String OTU_TABLE_PREFIX = "otu_table";

	/**
	 * {@link biolockj.Config} list property to calculate alpha diversity metrics.<br>
	 * For complete list of skbio.diversity.alpha options, see
	 * <a href= "http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html" target=
	 * "_top">http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html</a><br>
	 * {@value #QIIME_ALPHA_DIVERSITY_METRICS}
	 */
	public static final String QIIME_ALPHA_DIVERSITY_METRICS = "qiime.alphaMetrics";

	/**
	 * {@link biolockj.Config} boolean property to indicate if {@value #EXE_VSEARCH} is needed for chimera removal:
	 * {@value #QIIME_REMOVE_CHIMERAS}
	 */
	public static final String QIIME_REMOVE_CHIMERAS = "qiime.removeChimeras";

	/**
	 * QIIME script to add {@value #ALPHA_DIVERSITY_TABLE} to the metadata file: {@value #SCRIPT_ADD_ALPHA_DIVERSITY}
	 */
	public static final String SCRIPT_ADD_ALPHA_DIVERSITY = "add_alpha_to_mapping_file.py";

	/**
	 * QIIME script that produces {@value #COMBINED_FNA}, the multiplexed fasta file: {@value #SCRIPT_ADD_LABELS}
	 */
	public static final String SCRIPT_ADD_LABELS = "add_qiime_labels.py";

	/**
	 * QIIME script that creates alpha diversity metrics file in output/{@value #ALPHA_DIVERSITY_TABLE}:
	 * {@value #SCRIPT_CALC_ALPHA_DIVERSITY}
	 */
	public static final String SCRIPT_CALC_ALPHA_DIVERSITY = "alpha_diversity.py";

	/**
	 * QIIME script used to remove chimeras detected by {@value #EXE_VSEARCH}: {@value #SCRIPT_FILTER_OTUS}
	 */
	public static final String SCRIPT_FILTER_OTUS = "filter_otus_from_otu_table.py";

	/**
	 * QIIME script to print environment configuration to qsub output file: {@value #SCRIPT_PRINT_CONFIG}
	 */
	public static final String SCRIPT_PRINT_CONFIG = "print_qiime_config.py";

	/**
	 * Produces output/{@value #OTU_SUMMARY_FILE} summarizing dataset: {@value #SCRIPT_SUMMARIZE_BIOM}
	 */
	public static final String SCRIPT_SUMMARIZE_BIOM = "biom summarize-table";

	/**
	 * QIIME script used to produce taxonomy-level reports in the module output directory:
	 * {@value #SCRIPT_SUMMARIZE_TAXA}
	 */
	public static final String SCRIPT_SUMMARIZE_TAXA = "summarize_taxa.py";

	/**
	 * Value output by {@value #SCRIPT_CALC_ALPHA_DIVERSITY} for null values: {@value #ALPHA_DIV_NULL_VALUE}
	 */
	protected static final String ALPHA_DIV_NULL_VALUE = "N/A";

	/**
	 * Directory created by {@value biolockj.module.classifier.r16s.QiimeDeNovoClassifier#PICK_OTU_SCRIPT} and
	 * {@value biolockj.module.classifier.r16s.QiimeOpenRefClassifier#PICK_OTU_SCRIPT}: {@value #REP_SET}
	 */
	protected static final String REP_SET = "rep_set";

	/**
	 * QIIME script {@value #SCRIPT_SUMMARIZE_TAXA} parameter used to suppress the output of biom files. BioLockJ
	 * BioLockJ parsers expect clear text files in the module output directory, so the biom files must be excluded.
	 */
	protected static final String SUMMARIZE_TAXA_SUPPRESS_BIOM = "suppress_biom_table_output";

	// OTHER SCRIPT THAT MAY BE ADDED IN THE FUTURE
	// public static final String VALIDATED_MAPPING = "_corrected.txt";
	// private static final String OTUS_TREE_97 = "97_otus.tree";
	// private static final String TAXA_TREE = "taxa.tre";
	// private static final String SCRIPT_FILTER_TREE = "filter_tree.py -i ";
	// private static final String DIFF_OTU_SUMMARY = "differential_otu_summary.txt";
	// private static final String SCRIPT_DIFF_ABUNDANCE = "differential_abundance.py -i ";
	// private static final String SCRIPT_COMP_CORE_MB = "compute_core_microbiome.py ";

}
