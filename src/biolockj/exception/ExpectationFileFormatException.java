package biolockj.exception;

import java.io.File;
import biolockj.Constants;
import biolockj.exception.ConfigException;
import biolockj.util.ValidationUtil;;

public class ExpectationFileFormatException extends Exception {


	public ExpectationFileFormatException(String msg, File expectationFile) {
		super(buildMessage(msg, expectationFile));
	}
	
	private static String buildMessage (String inMsg, File file) {
		return(inMsg + Constants.RETURN + 
				"Configuration property [" + ValidationUtil.EXPECTATION_FILE + "] points to file [" + file.getAbsolutePath() + "]." + Constants.RETURN 
				+ "Make corrections to [" + file.getName() + "] and restart pipeline.");
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5325863214513460460L;

}

