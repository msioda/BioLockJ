/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 02, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj;

/**
 * 
 * @author mike
 *
 */
public class Constants
{
	/**
	 * Captures the application start time
	 */
	public static final long APP_START_TIME = System.currentTimeMillis();

	/**
	 * {@link biolockj.Config} String property: AWS EFS directory set in Nextflow main.nf: {@value #AWS_EFS_DIR}
	 */
	public static final String AWS_EFS_DIR = "aws.efsDir";

	/**
	 * {@link biolockj.Config} String property: AWS memory set in Nextflow main.nf: {@value #AWS_RAM}
	 */
	public static final String AWS_RAM = "aws.ram";

	/**
	 * Name of the file created in the BioModule or {@value #PROJECT_PIPELINE_DIR} root directory to indicate execution
	 * was successful: {@value #BLJ_COMPLETE}
	 */
	public static final String BLJ_COMPLETE = "biolockjComplete";

	/**
	 * Name of the file created in the {@value #PROJECT_PIPELINE_DIR} root directory to indicate fatal application
	 * errors halted execution: {@value #BLJ_FAILED}
	 */
	public static final String BLJ_FAILED = "biolockjFailed";

	/**
	 * Name of the file created in the BioModule root directory to indicate execution has started: {@value #BLJ_STARTED}
	 */
	public static final String BLJ_STARTED = "biolockjStarted";

	/**
	 * URL to the BioLockJ WIKI
	 */
	public static final String BLJ_WIKI = "https://github.com/msioda/BioLockJ/wiki";
	
	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #CLASS}
	 */
	public static final String CLASS = "class";
	
	/**
	 * {@link biolockj.Config} Boolean property {@value #CLUSTER_RUN_JAVA_AS_SCRIPT} if set =
	 * {@value biolockj.Constants#TRUE} will run Java module as a script instead of running on the head node.
	 */
	public static final String CLUSTER_RUN_JAVA_AS_SCRIPT = "cluster.runJavaAsScriptModule";
	
	/**
	 * {@link biolockj.Config} String property: Java class name for default module used to demultiplex data:
	 * {@value #DEFAULT_MOD_DEMUX}
	 */
	public static final String DEFAULT_MOD_DEMUX = "project.defaultModuleDemultiplexer";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used to convert files into fasta:
	 * {@value #DEFAULT_MOD_FASTA_CONV} format
	 */
	public static final String DEFAULT_MOD_FASTA_CONV = "project.defaultModuleFastaConverter";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used combined paired read files:
	 * {@value #DEFAULT_MOD_SEQ_MERGER}
	 */
	public static final String DEFAULT_MOD_SEQ_MERGER = "project.defaultModuleSeqMerger";

	/**
	 * {@link biolockj.Config} String property: Java class name for default module used generate p-value and other
	 * stats: {@value #DEFAULT_STATS_MODULE}
	 */
	public static final String DEFAULT_STATS_MODULE = "project.defaultStatsModule";

	/**
	 * In an otu string for multiple levels, each separated by {@value #SEPARATOR}, each otu has a level prefix ending
	 * with {@value #DELIM_SEP}
	 */
	public static final String DELIM_SEP = "__";
	/**
	 * {@link biolockj.Config} Boolean property: {@value #DISABLE_ADD_IMPLICIT_MODULES}<br>
	 * If set to {@value #TRUE}, implicit modules will not be added to the pipeline.
	 */
	public static final String DISABLE_ADD_IMPLICIT_MODULES = "project.disableAddImplicitModules";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #DISABLE_PRE_REQ_MODULES}<br>
	 * If set to {@value #TRUE}, prerequisite modules will not be added to the pipeline.
	 */
	public static final String DISABLE_PRE_REQ_MODULES = "project.disableAddPreReqModules";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #DOMAIN}
	 */
	public static final String DOMAIN = "domain";

	/**
	 * {@link biolockj.Config} String property: {@value #EXE_AWK}<br>
	 * Set command line executable awk.
	 */
	public static final String EXE_AWK = "exe.awk";

	/**
	 * {@link biolockj.Config} String property {@value #EXE_DOCKER}<br>
	 * Set command line executable docker
	 */
	public static final String EXE_DOCKER = "exe.docker";

	/**
	 * {@link biolockj.Config} String property {@value #EXE_GZIP}<br>
	 * Set command line executable gzip
	 */
	public static final String EXE_GZIP = "exe.gzip";

	/**
	 * {@link biolockj.Config} property for java executable: {@value #EXE_JAVA}
	 */
	public static final String EXE_JAVA = "exe.java";
	
	/**
	 * {@link biolockj.Config} property to python executable
	 */
	public static final String EXE_PYTHON = "exe.python";
	
	/**
	 * Boolean {@link biolockj.Config} property value option: {@value #FALSE}
	 */
	public static final String FALSE = "N";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #FAMILY}
	 */
	public static final String FAMILY = "family";
	
	public static final String FATAL_ERROR_FILE_PREFIX = "BioLockJ_FATAL_ERROR_";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #GENUS}
	 */
	public static final String GENUS = "genus";
	/**
	 * Gzip compressed file extension constant: {@value #GZIP_EXT}
	 */
	public static final String GZIP_EXT = ".gz";

	/**
	 * {@link biolockj.Config} Boolean property to disable HumanN2 Gene Family report:
	 * {@value #HN2_DISABLE_GENE_FAMILIES}
	 */
	public static final String HN2_DISABLE_GENE_FAMILIES = "humann2.disableGeneFamilies";

	/**
	 * {@link biolockj.Config} Boolean property to disable HumanN2 Pathway Abundance report:
	 * {@value #HN2_DISABLE_PATH_ABUNDANCE}
	 */
	public static final String HN2_DISABLE_PATH_ABUNDANCE = "humann2.disablePathAbundance";

	/**
	 * {@link biolockj.Config} Boolean property to disable HumanN2 Pathway Coverage report:
	 * {@value #HN2_DISABLE_PATH_COVERAGE}
	 */
	public static final String HN2_DISABLE_PATH_COVERAGE = "humann2.disablePathCoverage";

	/**
	 * HumanN2 file suffix identifier for Gene Family Summary report: {@value #HN2_GENE_FAM_SUM}
	 */
	public static final String HN2_GENE_FAM_SUM = "geneFam";

	/**
	 * HumanN2 file suffix identifier for Pathway Abundance Summary report: {@value #HN2_PATH_ABUND_SUM}
	 */
	public static final String HN2_PATH_ABUND_SUM = "pAbund";

	/**
	 * HumanN2 file suffix identifier for Pathway Coverage Summary report: {@value #HN2_PATH_COVG_SUM}
	 */
	public static final String HN2_PATH_COVG_SUM = "pCovg";

	/**
	 * HumanN2 meta column to store the total pathway count/sample: {@value #HN2_TOTAL_PATH_COUNT}
	 */

	public static final String HN2_TOTAL_PATH_COUNT = "Total_Pathway_Count";

	/**
	 * HumanN2 meta column to store the unique pathway count/sample: {@value #HN2_TOTAL_PATH_COUNT}
	 */
	public static final String HN2_UNIQUE_PATH_COUNT = "Unique_Pathway_Count";


	public static final String HN2_UNMAPPED_COUNT = "Unmapped_Count";

	/**
	 * Standard indent = 4 spaces.
	 */
	public static final String INDENT = "    ";

	/**
	 * {@link biolockj.Config} List property: {@value #INPUT_DIRS}<br>
	 * Set sequence file directories
	 */
	public static final String INPUT_DIRS = "input.dirPaths";

	/**
	 * {@link biolockj.Config} List property: {@value #INPUT_IGNORE_FILES}<br>
	 * Set file names to ignore if found in {@value #INPUT_DIRS}
	 */
	public static final String INPUT_IGNORE_FILES = "input.ignoreFiles";

	/**
	 * Internal {@link biolockj.Config} List property: {@value #INTERNAL_ALL_MODULES}<br>
	 * List of all configured, implicit, and pre/post-requisite modules for the pipeline.<br>
	 * Example: biolockj.module.ImportMetadata, etc.
	 */
	public static final String INTERNAL_ALL_MODULES = "internal.allModules";
	
	/**
	 * Set BioModule tag in {@link biolockj.Config} file to include in pipeline: {@value #INTERNAL_BLJ_MODULE}<br>
	 * Example: #BioModule biolockj.module.ImportMetadata
	 */
	public static final String INTERNAL_BLJ_MODULE = "#BioModule";

	/**
	 * Internal {@link biolockj.Config} List property: {@value #INTERNAL_DEFAULT_CONFIG}<br>
	 * List of all nested default config files.<br>
	 */
	public static final String INTERNAL_DEFAULT_CONFIG = "internal.defaultConfig";
	
	/**
	 * {@link biolockj.Config} property used to limit classes that log debug statements when
	 * {@value #LOG_LEVEL_PROPERTY}={@value biolockj.Constants#TRUE}
	 */
	public static final String LIMIT_DEBUG_CLASSES = "project.limitDebugClasses";

	/**
	 * BioLockJ log file extension constant: {@value #LOG_EXT}
	 */
	public static final String LOG_EXT = ".log";

	/**
	 * {@link biolockj.Config} property used to set log sensitivity in
	 * <a href= "https://github.com/msioda/BioLockJ/blob/master/resources/log4j.properties?raw=true" target=
	 * "_top">log4j.properties</a><br>
	 * <i>log4j.rootLogger=${project.logLevel}, file, stdout</i>
	 * <ol>
	 * <li>DEBUG - Log all messages
	 * <li>INFO - Log info, warning and error messages
	 * <li>WARN - Log warning and error messages
	 * <li>ERROR - Log error messages only
	 * </ol>
	 */
	public static final String LOG_LEVEL_PROPERTY = "project.logLevel";

	/**
	 * Spacer used to improve log file readability
	 */
	public static final String LOG_SPACER = "========================================================================";

	/**
	 * Prefix added to the master Config file: {@value #MASTER_PREFIX}
	 */
	public static final String MASTER_PREFIX = "MASTER_";

	public static final String MODULE_CLASSIFIER_PACKAGE = "biolockj.module.classifier";

	public static final String MODULE_IMPLICIT_PACKAGE = "biolockj.module.implicit";
	

	public static final String MODULE_R_PACKAGE = "biolockj.module.r";

	/**
	 * Biolockj SEQ module package: {@value #MODULE_SEQ_PACKAGE}
	 */
	public static final String MODULE_SEQ_PACKAGE = "biolockj.module.seq";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #ORDER}
	 */
	public static final String ORDER = "order";

	/**
	 * Included in the file name of each file output. One file per sample is output by the ParserModule.
	 */
	public static final String OTU_COUNT = "otuCount";

	/**
	 * {@link biolockj.Config} property suffix for exe.* properties, used to set optional parameters: {@value #PARAMS}
	 */
	public static final String PARAMS = "Params";

	/**
	 * BioLockJ PDF file extension constant: {@value #PDF_EXT}
	 */
	public static final String PDF_EXT = ".pdf";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #PHYLUM}
	 */
	public static final String PHYLUM = "phylum";

	/**
	 * File suffix appended to processed samples in the module output directory: {@value #PROCESSED}
	 */
	public static final String PROCESSED = "_reported" + Constants.TSV_EXT;

	/**
	 * {@link biolockj.Config} property set to copy input files into pipeline root directory:
	 * {@value #PROJECT_COPY_FILES}
	 */
	public static final String PROJECT_COPY_FILES = "project.copyInput";

	/**
	 * {@link biolockj.Config} String property: {@value #PROJECT_DEFAULT_PROPS}<br>
	 * Set file path of default property file. Nested default properties are supported (so the default property file can
	 * also have a default, and so on).
	 */
	public static final String PROJECT_DEFAULT_PROPS = "project.defaultProps";

	/**
	 * {@link biolockj.Config} property set to delete {@link biolockj.module.BioModule#getTempDir()} files:
	 * {@value #PROJECT_DELETE_TEMP_FILES}
	 */
	public static final String PROJECT_DELETE_TEMP_FILES = "project.deleteTempFiles";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #PROJECT_ENV}<br>
	 * Options: {@value #PROJECT_ENV_CLUSTER}, {@value #PROJECT_ENV_AWS}, {@value #PROJECT_ENV_LOCAL}
	 */
	public static final String PROJECT_ENV = "project.env";

	/**
	 * {@link biolockj.Config} option for property: {@value #PROJECT_ENV}<br>
	 * Used to indicate running as an Amazon web service: {@value #PROJECT_ENV_AWS}
	 */
	public static final String PROJECT_ENV_AWS = "aws";

	/**
	 * {@link biolockj.Config} option for property: {@value #PROJECT_ENV}<br>
	 * Used to indicate running on the cluster: {@value #PROJECT_ENV_CLUSTER}
	 */
	public static final String PROJECT_ENV_CLUSTER = "cluster";

	/**
	 * {@link biolockj.Config} option for property: {@value #PROJECT_ENV}<br>
	 * Used to indicate running on a local machine (laptop, etc): {@value #PROJECT_ENV_LOCAL}
	 */
	public static final String PROJECT_ENV_LOCAL = "local";

	/**
	 * {@link biolockj.Config} String property: {@value #PROJECT_PIPELINE_DIR}<br>
	 * Stores the path of the pipeline root directory path set by the application runtime code.
	 */
	public static final String PROJECT_PIPELINE_DIR = "project.pipelineDir";

	/**
	 * QIIME application: {@value #QIIME}
	 */
	public static final String QIIME = "qiime";

	/**
	 * {@link biolockj.Config} list property to calculate alpha diversity metrics.<br>
	 * For complete list of skbio.diversity.alpha options, see
	 * <a href= "http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html" target=
	 * "_top">http://scikit-bio.org/docs/latest/generated/skbio.diversity.alpha.html</a><br>
	 * {@value #QIIME_ALPHA_DIVERSITY_METRICS}
	 */
	public static final String QIIME_ALPHA_DIVERSITY_METRICS = "qiime.alphaMetrics";

	/**
	 * Qiime may find ambiguous taxa identified in various formats in different databases. The following accounts for
	 * Green Genes 13.8 and Silva 132: "Ambiguous_taxa", "Other", "Unassigned"
	 */
	public static final String[] QIIME_AMBIGUOUS_TAXA = { "Ambiguous_taxa", "Other", "Unassigned" };

	/**
	 * File extension of BioLockJ generated R Scripts: {@value #R_EXT}
	 */
	public static final String R_EXT = ".R";

	/**
	 * {@link biolockj.Config} String property: {@value #RDP_THRESHOLD_SCORE}<br>
	 * RdpParser will ignore OTU assignments below the threshold score (0-100)
	 */
	public static final String RDP_THRESHOLD_SCORE = "rdp.minThresholdScore";
	
	/**
	 * {@link biolockj.Config} String property: {@value #REPORT_LOG_BASE}<br>
	 * Required to be set to "e" or "10" to build log normalized reports.
	 */
	public static final String REPORT_LOG_BASE = "report.logBase";

	/**
	 * {@link biolockj.Config} Positive Integer property {@value #REPORT_MIN_COUNT} defines the minimum table count
	 * allowed, if a count less that this value is found, it is set to 0.
	 */
	public static final String REPORT_MIN_COUNT = "report.minCount";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #REPORT_NUM_HITS}<br>
	 * If set to {@value #TRUE}, NUM_OTUS will be added to metadata file by
	 * {@link biolockj.module.implicit.parser.ParserModuleImpl} and included in R reports
	 */
	public static final String REPORT_NUM_HITS = "report.numHits";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #REPORT_NUM_READS}<br>
	 * If set to {@value #TRUE} and NUM_READS exists in metadata file, NUM_READS will be included in the R reports
	 */
	public static final String REPORT_NUM_READS = "report.numReads";

	/**
	 * {@link biolockj.Config} Positive Double property {@value #REPORT_SAMPLE_CUTOFF} defines minimum percentage of
	 * data columns must be non-zero to keep the sample.
	 */
	public static final String REPORT_SAMPLE_CUTOFF = "report.scarceSampleCutoff";

	/**
	 * {@link biolockj.Config} Positive Double property {@value #REPORT_SCARCE_CUTOFF} defines minimum percentage of
	 * samples that must contain a count value for it to be kept.
	 */
	public static final String REPORT_SCARCE_CUTOFF = "report.scarceCountCutoff";

	/**
	 * {@link biolockj.Config} List property: {@value #REPORT_TAXONOMY_LEVELS}<br>
	 * This property drives a lot of BioLockJ functionality and determines which taxonomy-levels are reported. Note,
	 * some classifiers do not identify {@value #SPECIES} level OTUs.<br>
	 * Options = {@value #DOMAIN}, {@value #PHYLUM}, {@value #CLASS}, {@value #ORDER}, {@value #FAMILY},
	 * {@value #GENUS}, {@value #SPECIES}
	 */
	public static final String REPORT_TAXONOMY_LEVELS = "report.taxonomyLevels";

	/**
	 * Return character constant *backslash-n*
	 */
	public static final String RETURN = "\n";

	/**
	 * Name of the script sub-directory: {@value #SCRIPT_DIR}
	 */
	public static final String SCRIPT_DIR = "script";

	/**
	 * File suffix appended to failed scripts: {@value #SCRIPT_FAILURES}
	 */
	public static final String SCRIPT_FAILURES = "Failures";
	
	/**
	 * File suffix appended to started script: {@value #SCRIPT_STARTED}
	 */
	public static final String SCRIPT_STARTED = "Started";
	
	/**
	 * File suffix appended to successful scripts: {@value #SCRIPT_SUCCESS}
	 */
	public static final String SCRIPT_SUCCESS = "Success";

	/**
	 * Semi-colon is used to separate each taxa {@value #SEPARATOR}
	 */
	public static final String SEPARATOR = "|";
	
	/**
	 * BioLockJ shell script file extension constant: {@value #SH_EXT}
	 */
	public static final String SH_EXT = ".sh";

	/**
	 * {@link biolockj.Config} option for {@value #REPORT_TAXONOMY_LEVELS}: {@value #SPECIES}
	 */
	public static final String SPECIES = "species";

	/**
	 * BioLockJ tab character constant: {@value #TAB_DELIM}
	 */
	public static final String TAB_DELIM = "\t";

	/**
	 * Included in the file name of each file output. One file per sample is output by the ParserModule.
	 */
	public static final String TAXA_TABLE = "taxaCount";

	/**
	 * Boolean {@link biolockj.Config} property value option: {@value #TRUE}
	 */
	public static final String TRUE = "Y";

	/**
	 * BioLockJ tab delimited text file extension constant: {@value #TSV_EXT}
	 */
	public static final String TSV_EXT = ".tsv";

	/**
	 * BioLockJ standard text file extension constant: {@value #TXT_EXT}
	 */
	public static final String TXT_EXT = ".txt";

	/**
	 * {@link biolockj.Config} property to define permission settings when running chmod on pipeline root dir:
	 * {@value #PROJECT_PERMISSIONS}
	 */
	protected static final String PROJECT_PERMISSIONS = "project.permissions";
}
