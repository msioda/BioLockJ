package biolockj;

public class Constants
{

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
	 * Name of the file created in the BioModule or {@value biolockj.Config##PROJECT_PIPELINE_DIR} root directory to
	 * indicate execution was successful: {@value #BLJ_COMPLETE}
	 */
	public static final String BLJ_COMPLETE = "biolockjComplete";

	/**
	 * Captures the application start time
	 */
	public static final long APP_START_TIME = System.currentTimeMillis();

	/**
	 * URL to the BioLockJ WIKI
	 */
	public static final String BLJ_WIKI = "https://github.com/msioda/BioLockJ/wiki";

	/**
	 * Gzip compressed file extension constant: {@value #GZIP_EXT}
	 */
	public static final String GZIP_EXT = ".gz";

	/**
	 * BioLockJ log file extension constant: {@value #LOG_EXT}
	 */
	public static final String LOG_EXT = ".log";

	/**
	 * BioLockJ PDF file extension constant: {@value #PDF_EXT}
	 */
	public static final String PDF_EXT = ".pdf";

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

	public static final String FATAL_ERROR_FILE_PREFIX = "BioLockJ_FATAL_ERROR_";

}
