package biolockj;

public class Constants
{

	/**
	 * Captures the application start time
	 */
	public static final long APP_START_TIME = System.currentTimeMillis();
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
	 * {@link biolockj.Config} Boolean property: {@value #DISABLE_ADD_IMPLICIT_MODULES}<br>
	 * If set to {@value biolockj.Config#TRUE}, implicit modules will not be added to the pipeline.
	 */
	public static final String DISABLE_ADD_IMPLICIT_MODULES = "project.disableAddImplicitModules";

	/**
	 * {@link biolockj.Config} Boolean property: {@value #DISABLE_PRE_REQ_MODULES}<br>
	 * If set to {@value biolockj.Config#TRUE}, prerequisite modules will not be added to the pipeline.
	 */
	public static final String DISABLE_PRE_REQ_MODULES = "project.disableAddPreReqModules";

	public static final String FATAL_ERROR_FILE_PREFIX = "BioLockJ_FATAL_ERROR_";

	/**
	 * Gzip compressed file extension constant: {@value #GZIP_EXT}
	 */
	public static final String GZIP_EXT = ".gz";

	/**
	 * BioLockJ log file extension constant: {@value #LOG_EXT}
	 */
	public static final String LOG_EXT = ".log";

	public static final String MODULE_CLASSIFIER_PACKAGE = "biolockj.module.classifier";
	public static final String MODULE_IMPLICIT_PACKAGE = "biolockj.module.implicit";

	public static final String MODULE_POST_REQS = "postReqs";

	public static final String MODULE_PRE_REQS = "preReqs";

	public static final String MODULE_SEQ_PACKAGE = "biolockj.module.seq";

	/**
	 * BioLockJ PDF file extension constant: {@value #PDF_EXT}
	 */
	public static final String PDF_EXT = ".pdf";

	public static final String PIPELINE_IMPLICIT = "pipeline.implicit";

	public static final String PIPELINE_REQUIRE_FASTA = "pipeline.requireFasta";

	public static final String PIPELINE_REQUIRE_MERGED = "pipeline.requireMerged";

	/**
	 * {@link biolockj.Config} property set to copy input files into pipeline root directory:
	 * {@value #PROJECT_COPY_FILES}
	 */
	public static final String PROJECT_COPY_FILES = "project.copyInput";

	/**
	 * {@link biolockj.Config} property set to delete {@link biolockj.module.BioModule#getTempDir()} files:
	 * {@value #PROJECT_DELETE_TEMP_FILES}
	 */
	public static final String PROJECT_DELETE_TEMP_FILES = "project.deleteTempFiles";

	/**
	 * Return character constant *backslash-n*
	 */
	public static final String RETURN = "\n";

	/**
	 * BioLockJ shell script file extension constant: {@value #SH_EXT}
	 */
	public static final String SH_EXT = ".sh";

	/**
	 * BioLockJ tab character constant: {@value #TAB_DELIM}
	 */
	public static final String TAB_DELIM = "\t";

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
