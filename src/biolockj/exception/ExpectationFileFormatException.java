package biolockj.exception;

import java.io.File;
import biolockj.Constants;
import biolockj.util.ValidationUtil;

/**
 * TODO : Add info
 */
public class ExpectationFileFormatException extends BioLockJException {

	/**
	 * 
	 * TODO : Add info
	 * 
	 * @param msg Error message info
	 * @param file Problem file
	 */
	public ExpectationFileFormatException( final String msg, final File file ) {
		super( buildMessage( msg, file ) );
	}

	private static String buildMessage( final String msg, final File file ) {
		return msg + Constants.RETURN + "Configuration property [" + ValidationUtil.EXPECTATION_FILE +
			"] points to file [" + file.getAbsolutePath() + "]." + Constants.RETURN + "Make corrections to [" +
			file.getName() + "] and restart pipeline.";
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5325863214513460460L;

}
