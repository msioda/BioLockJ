package biolockj;

public class Constants
{
	/**
	 * Captures the application start time
	 */
	public static final long APP_START_TIME = System.currentTimeMillis();

	public static final String AWS_EFS_DIR = "aws.efsDir";

	public static final String AWS_RAM = "aws.ram";

	public static final String AWS_STACK = "aws.stack";

	/**
	 * Name of the file created in the BioModule or {@value biolockj.Config##PROJECT_PIPELINE_DIR} root directory to
	 * indicate execution was successful: {@value #BLJ_COMPLETE}
	 */
	public static final String BLJ_COMPLETE = "biolockjComplete";
	/**
	 * Name of the file created in the {@value biolockj.Config#PROJECT_PIPELINE_DIR} root directory to indicate fatal
	 * application errors halted execution: {@value #BLJ_FAILED}
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
	 * If set to {@value biolockj.Config#TRUE}, implicit modules will not be added to the pipeline.
	 */
	public static final String DISABLE_ADD_IMPLICIT_MODULES = "project.disableAddImplicitModules";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #DISABLE_PRE_REQ_MODULES}<br>
	 * If set to {@value biolockj.Config#TRUE}, prerequisite modules will not be added to the pipeline.
	 */
	public static final String DISABLE_PRE_REQ_MODULES = "project.disableAddPreReqModules";

	public static final String DOCKER_IMG_VERSION = "docker.imgVersion";

	/**
	 * {@link biolockj.Config} String property: {@value #EXE_AWK}<br>
	 * Set command line executable awk.
	 */
	public static final String EXE_AWK = "exe.awk";
	/**
	 * {@link biolockj.Config} property for classifier program executable: {@value #EXE_CLASSIFIER}
	 */
	public static final String EXE_CLASSIFIER = "exe.classifier";
	/**
	 * {@link biolockj.Config} property for classifier program optional parameters: {@value #EXE_CLASSIFIER_PARAMS}
	 */
	public static final String EXE_CLASSIFIER_PARAMS = "exe.classifierParams";
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
	 * Boolean {@link biolockj.Config} property value option: {@value #FALSE}
	 */
	public static final String FALSE = "N";
	public static final String FATAL_ERROR_FILE_PREFIX = "BioLockJ_FATAL_ERROR_";
	/**
	 * Gzip compressed file extension constant: {@value #GZIP_EXT}
	 */
	public static final String GZIP_EXT = ".gz";
	public static final String HN2_FULL_REPORT = "fullReport";
	public static final String HN2_PATH_ABUND = "pAbund";

	public static final String HN2_PATHWAY_REPORT = "pathwayReport";
	public static final String HN2_TOTAL_PATH_COUNT = "Total_Pathway_Count";
	public static final String HN2_UNINTEGRATED_COUNT = "Unintegrated_Count";

	public static final String HN2_UNIQUE_PATH_COUNT = "Unique_Pathway_Count";
	public static final String HN2_UNMAPPED_COUNT = "Unmapped_Count";
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
	 * BioLockJ log file extension constant: {@value #LOG_EXT}
	 */
	public static final String LOG_EXT = ".log";

	public static final String MODULE_CLASSIFIER_PACKAGE = "biolockj.module.classifier";

	public static final String MODULE_IMPLICIT_PACKAGE = "biolockj.module.implicit";

	public static final String MODULE_R_PACKAGE = "biolockj.module.r";

	public static final String MODULE_SEQ_PACKAGE = "biolockj.module.seq";

	/**
	 * Included in the file name of each file output. One file per sample is output by the ParserModule.
	 */
	public static final String OTU_COUNT = "otuCount";

	/**
	 * BioLockJ PDF file extension constant: {@value #PDF_EXT}
	 */
	public static final String PDF_EXT = ".pdf";

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

	public static final String QIIME = "qiime";

	/**
	 * Qiime may find ambiguous taxa identified in various formats in different databases. The following accounts for
	 * Green Genes 13.8 and Silva 132: {@value #QIIME_AMBIGUOUS_TAXA}
	 */
	public static final String[] QIIME_AMBIGUOUS_TAXA = { "Ambiguous_taxa", "Other", "Unassigned" };

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
	 * Return character constant *backslash-n*
	 */
	public static final String RETURN = "\n";

	/**
	 * Semi-colon is used to separate each taxa {@value #SEPARATOR}
	 */
	public static final String SEPARATOR = "|";

	/**
	 * BioLockJ shell script file extension constant: {@value #SH_EXT}
	 */
	public static final String SH_EXT = ".sh";

	/**
	 * BioLockJ tab character constant: {@value #TAB_DELIM}
	 */
	public static final String TAB_DELIM = "\t";

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
	 * {@link biolockj.Config} property to define permission setttings when running chmod on pipeline root dir:
	 * {@value #PROJECT_PERMISSIONS}
	 */
	protected static final String PROJECT_PERMISSIONS = "project.permissions";

}
