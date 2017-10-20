/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
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
package biolockj.module.classifier.r16s;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.BashScriptBuilder;
import biolockj.util.MetaUtil;

/**
 * QiimeClassifier is a superclass to several classes in the qiime package so hold shared methods
 * for these classes.  When called directly, OTUs have been picked so we add alphaDiversityMetrics
 * if configured and calls QIIME summary scripts.
 */
public class QiimeClassifier extends ClassifierModuleImpl implements ClassifierModule
{
	/**
	 * Build script that calls QIIME summary scripts.  If alphaDiversityMetrics are configured, add
	 * lines to add the metrics.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		final List<List<String>> data = new ArrayList<>();
		final List<String> lines = new ArrayList<>();

		final String line1 = SCRIPT_SUMMARIZE_TAXA + files.get( 0 ) + " -L " + getQiimeTaxaLevels() + " -o " + outDir
				+ OTU_DIR;

		final String line2 = SCRIPT_SUMMARIZE_BIOM + files.get( 0 ) + " -o " + outDir + OTU_SUMMARY_FILE;

		lines.add( line1 );
		lines.add( line2 );

		if( Config.getString( QIIME_ALPHA_DIVERSITY_METRICS ) != null )
		{
			final File newMapping = new File( outDir + MetaUtil.getMetadataFileName() );
			final String alphaLine1 = SCRIPT_CALC_ALPHA_DIVERSITY + files.get( 0 ) + " -m "
					+ Config.requireString( QIIME_ALPHA_DIVERSITY_METRICS ) + " -o " + outDir + ALPHA_DIVERSITY_TABLE; // + " -t " + outDir +TAXA_TREE ;

			final String alphaLine2 = SCRIPT_ADD_ALPHA_DIVERSITY + MetaUtil.getFile().getAbsolutePath() + " -i "
					+ outDir + ALPHA_DIVERSITY_TABLE + " -o " + newMapping;

			lines.add( alphaLine1 );
			lines.add( alphaLine2 );

			MetaUtil.setFile( newMapping );
		}

		data.add( lines );

		return data;
	}

	/**
	 * QIIME supports only unpaired reads, so if paired the mergeUtil must be used to merge the files
	 * so by the time this class is called, we are dealing with single fw reads, so call buildScripts,
	 * as used by single fw reads.
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		return buildScript( files );
	}

	/**
	 * Build bash scripts for input files.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		BashScriptBuilder.buildScripts( this, buildScript( getInputFiles() ), getInputFiles(),
				Config.requirePositiveInteger( Config.SCRIPT_BATCH_SIZE ) );
	}

	@Override
	public String getClassifierExe() throws Exception
	{
		return null;
	}

	/**
	 * Get program switches from the classifierParams prop value by adding correct number of dashes "-".
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
	 * Subclasses call this method to input files to only those named OTU_TABLE in dir (and its subdirs).
	 */
	@Override
	public void initInputFiles( final IOFileFilter ff, final IOFileFilter recursive ) throws Exception
	{
		if( getClass().getName().equals( QiimeClassifier.class.getName() ) )
		{
			Log.out.info( "QIIME Classifier getting files from "
					+ ( ( getInputDir() == null ) ? Config.INPUT_DIRS: getInputDir().getAbsolutePath() ) + " named: "
					+ OTU_TABLE );
			super.initInputFiles( new NameFileFilter( OTU_TABLE ), TrueFileFilter.INSTANCE );
		}
		else
		{
			super.initInputFiles( ff, recursive );
		}

	}

	/**
	 * Subclasses call this method to add lines to pick OTUs, first by adding labels and then by calling
	 * the configured OTU scirpt.
	 * @param lines
	 * @param fastaDir
	 * @param mapping
	 * @param outputDir
	 */
	protected void addPickOtuLines( final List<String> lines, final String fastaDir, final String mapping,
			final String outputDir ) throws Exception
	{
		final String fnaFile = outputDir + File.separator + COMBINED_FNA;
		lines.add( SCRIPT_ADD_LABELS + fastaDir + " -m " + mapping + " -c " + DEMUX_COLUMN + " -o " + outputDir );
		lines.add( pickOtuScript() + switches + "-i " + fnaFile + " -fo " + outputDir );
	}

	/**
	 * Subclasses call this method to check dependencies before picking OTUs.  Must verify
	 * conflicting params are not used.
	 * @throws Exception
	 */
	protected void checkOtuPickingDependencies() throws Exception
	{
		switches = getClassifierParams();
		if( ( switches.indexOf( "-i " ) > -1 ) || ( switches.indexOf( "--input_fp " ) > -1 ) )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-i or --input_fp) FOUND IN PROPERTY ("
					+ EXE_CLASSIFIER_PARAMS + "). PLEASE REMOVE.  INPUT DETERMINED BY: " + Config.INPUT_DIRS );
		}
		if( ( switches.indexOf( "-o " ) > -1 ) || ( switches.indexOf( "--output_dir " ) > -1 ) )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-o or --output_dir) FOUND IN PROPERTY ("
					+ EXE_CLASSIFIER_PARAMS + "). PLEASE REMOVE THIS VALUE FROM PROPERTY FILE. " );
		}
		if( ( switches.indexOf( "-a " ) > -1 ) || ( switches.indexOf( "-O " ) > -1 ) )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-a or -O) FOUND IN PROPERTY (" + EXE_CLASSIFIER_PARAMS
					+ "). BIOLOCKJ DERIVES THIS VALUE FROM: " + Config.SCRIPT_NUM_THREADS );
		}
		if( switches.indexOf( "-f " ) > -1 )
		{
			throw new Exception( "INVALID CLASSIFIER OPTION (-f or --force) FOUND IN PROPERTY (" + EXE_CLASSIFIER_PARAMS
					+ "). OUTPUT OPTIONS AUTOMATED BY BIOLOCKJ." );
		}

		addHardCodedSwitches();
	}

	//	/**
	//	 * Subclasses that pick OTUs call this method to initialize the QIIME Mapping file from the output dir
	//	 * of the previous executor.  If the previous executor output a new qiime mapping (via VALIDATED_MAPPING),
	//	 * update the config.metadata file.  If this is a re-run attempt this method overrides the configured
	//	 * metadata file in the prop file, with those found in the INPUT_DIR
	//	 * @param dir
	//	 * @throws Exception
	//	 */
	//	protected void initMappingFile( final File dir ) throws Exception
	//	{
	//		if( dir == null )
	//		{
	//			Log.out.info( "Restarting failed run with QIIME Pick OTU step so must configure QIIME mapping file: "
	//					+ Config.INPUT_METADATA );
	//			return;
	//		}
	//		final String searchTerm = "*" + VALIDATED_MAPPING;
	//		Log.out.info( "Get mapping file from " + ( ( dir == null ) ? Config.INPUT_DIRS: dir.getAbsolutePath() )
	//				+ " ending in: " + searchTerm );
	//		final IOFileFilter ff = new WildcardFileFilter( searchTerm );
	//		final Collection<File> files = FileUtils.listFiles( dir, ff, TrueFileFilter.INSTANCE );
	//		final int count = ( ( files == null ) ? 0: files.size() );
	//		if( count == 0 )
	//		{
	//			throw new Exception( "Unable to find QIIME mapping file ending in " + VALIDATED_MAPPING
	//					+ " in input directory: " + dir.getAbsolutePath() );
	//		}
	//		else if( count > 1 )
	//		{
	//			throw new Exception( "Too many QIIME mapping files (total=" + count + ") found ending in " + searchTerm
	//					+ " in input directory: " + dir.getAbsolutePath() );
	//		}
	//
	//		MetaUtil.setFile( files.iterator().next() );
	//	}

	/**
	 * Override basic function, details output to qsub/bash output.
	 */
	@Override
	protected void logVersion()
	{
		// handled in bash script
	}

	protected String pickOtuScript() throws Exception
	{
		throw new Exception( "pickOtuScript() must be overridden in subclass!" );
	}

	/**
	 * Typically we verify no duplicate file names are used, but for QIIME we may be combining
	 * multiple files with the same name, so we skip this impl for QIIME classifier.
	 */
	@Override
	protected void validateFileNameUnique( final Set<String> fileNames, final File f ) throws Exception
	{
		// Not needed for QIIME.  Multiple file named otu_table.biom & others exist.
	}

	/**
	 * Set the numThreads param.
	 * @throws Exception
	 */
	private void addHardCodedSwitches() throws Exception
	{
		switches += "-aO" + " " + Config.requirePositiveInteger( Config.SCRIPT_NUM_THREADS ) + " ";
	}

	/**
	 * Set the taxa level indicators based on config Config.getTaxonomy().
	 * @return
	 * @throws Exception
	 */
	private String getQiimeTaxaLevels() throws Exception
	{
		String levels = "";
		if( Config.requireTaxonomy().contains( Config.DOMAIN ) )
		{
			levels += "1";
		}
		if( Config.requireTaxonomy().contains( Config.PHYLUM ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "2";
		}
		if( Config.requireTaxonomy().contains( Config.CLASS ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "3";
		}
		if( Config.requireTaxonomy().contains( Config.ORDER ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "4";
		}
		if( Config.requireTaxonomy().contains( Config.FAMILY ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "5";
		}
		if( Config.requireTaxonomy().contains( Config.GENUS ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "6";
		}
		if( Config.requireTaxonomy().contains( Config.SPECIES ) )
		{
			levels += ( levels.isEmpty() ? "": "," ) + "7";
		}

		return levels;
	}

	private String switches = null;

	public static final String ALPHA_DIVERSITY_TABLE = "alphaDiversity.txt";
	public static final String BARCODE_SEQUENCE = "BarcodeSequence";
	public static final String COMBINED_FNA = "combined_seqs.fna";
	public static final String DEMUX_COLUMN = "InputFileName";
	public static final String DESCRIPTION = "Description";
	public static final String EXE_AWK = "exe.awk";
	public static final String EXE_VSEARCH = "exe.vsearch";
	public static final String EXE_VSEARCH_PARAMS = "exe.vsearchParams";
	public static final String LINKER_PRIMER_SEQUENCE = "LinkerPrimerSequence";

	public static final String OTU_DIR = "otu_by_taxa_level";
	public static final String OTU_SUMMARY_FILE = "otuSummary.txt";
	public static final String OTU_TABLE = "otu_table.biom";
	public static final String QIIME_ALPHA_DIVERSITY_METRICS = "qiime.alphaDiversityMetrics";

	public static final String QIIME_REMOVE_CHIMERAS = "qiime.removeChimeras";
	public static final String REP_SET = "rep_set";

	public static final String SCRIPT_ADD_ALPHA_DIVERSITY = "add_alpha_to_mapping_file.py -m ";
	public static final String SCRIPT_ADD_LABELS = "add_qiime_labels.py -n 1 -i ";

	public static final String SCRIPT_CALC_ALPHA_DIVERSITY = "alpha_diversity.py -i ";
	public static final String SCRIPT_FILTER_OTUS = "filter_otus_from_otu_table.py -i ";
	public static final String SCRIPT_SUMMARIZE_BIOM = "biom summarize-table -i ";
	public static final String SCRIPT_SUMMARIZE_TAXA = "summarize_taxa.py -a -i ";
	//public static final String VALIDATED_MAPPING = "_corrected.txt";
	protected static final String PICK_OTU_SCRIPT = "pick_de_novo_otus.py";

	// private static final String OTUS_TREE_97 = "97_otus.tree";
	// private static final String TAXA_TREE = "taxa.tre";
	// private static final String SCRIPT_FILTER_TREE = "filter_tree.py -i ";
	// private static final String DIFF_OTU_SUMMARY = "differential_otu_summary.txt";
	// private static final String SCRIPT_DIFF_ABUNDANCE = "differential_abundance.py -i ";
	// private static final String SCRIPT_COMP_CORE_MB = "compute_core_microbiome.py ";

}