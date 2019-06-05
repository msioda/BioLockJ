/**
 * @UNCC Fodor Lab
 * @author Ivory Blakley
 * @email ieclabau@uncc.edu
 * @date May 27, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import biolockj.module.BioModule;
import biolockj.util.BioLockJUtil;
import biolockj.Log;
import biolockj.exception.*;
import biolockj.Config;
import biolockj.Constants;

/**
 * This module measures attributes of the files in the output directory of the
 * previous module. If there is no expectation file, the module reports the
 * values for these attributes (can be specified with
 * {@link biolockj.Config}.{@value #REPORT_ON} and saves a file which can be
 * used as an expectation in future runs. If there is an expectation file
 * (specified using {@link biolockj.Config}.{@value #EXPECTATION_FILE}, the
 * values for the current outputs will be compared with the expected values and
 * the module will stop the pipeline if the files do not meet expectations. Set
 * {@value ALWAYS_PASS} to Y to report on the comparison but not stop the
 * pipeline.
 * 
 * @author Ivory
 *
 */
public class ValidationUtil
{

	public static void checkDependencies(BioModule module) throws Exception {
		if (!Config.getBoolean(module, DISABLE_VALIDATION)) {
			if (hasExp(module)) {
				getPrevSummaries(module);// TODO - capture these outputs? maybe log info?
			}
			// make sure configuration props are valid
			getReportSet(module);
			alwaysPass(module);
		}
	}
	
	public static void executeTask(BioModule module) throws Exception {
		if (!Config.getBoolean(module, DISABLE_VALIDATION)) {
			HashMap<String, FileSummary> prevOutput = new HashMap<String, FileSummary>();
			if (hasExp(module)) {
				prevOutput = getPrevSummaries(module);
			}

			final BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputFile(module)));
			writeRow(writer, getReportSet(module));

			File[] outputs = module.getOutputDir().listFiles();
			Log.debug(ValidationUtil.class,
					"Found [" + outputs.length + "] files in output dir of module [" + module + "].");
			int passingFiles = 0;
			for (File f : outputs) {
				FileSummary fs = new FileSummary(f);
				if (getReportSet(module).contains("md5") || hasExp(module) && getCompareSet(module).contains("md5")) {
					fs.calcMd5();
				}
				if (hasExp(module)) {
					String ekey = fileNameToKey(fs.getAtt(NAME));
					FileSummary expected = prevOutput.get(ekey);
					if (fs.compareToExpected(expected, getCompareSet(module)) == PASS) {
						passingFiles += 1;
					}
					prevOutput.remove(ekey);
				}
				ArrayList<String> row = new ArrayList<String>();
				for (String col : getReportSet(module)) {
					row.add(fs.getAtt(col));
				}
				writeRow(writer, row);
			}
			writer.close();
			if (prevOutput.size() > 0) {
				Log.warn(ValidationUtil.class, "Expectation file includes " + prevOutput.size()
						+ " files that were not verified in output of " + module.getClass().getSimpleName() + ".");
				for (String oldFileName : prevOutput.keySet()) {
					Log.warn(ValidationUtil.class, prevOutput.get(oldFileName).toString());
				}
				if (!alwaysPass(module)) {
					throw new ValidationException(module);
				}
			}
			if (hasExp(module) && !alwaysPass(module) && passingFiles < outputs.length) {
				Log.warn(ValidationUtil.class, "passingFiles: " + passingFiles);
				Log.warn(ValidationUtil.class, "outputs to validate: " + outputs.length);
				if (!alwaysPass(module)) {
					throw new ValidationException(module);
				}
			}
		}else {
			Log.debug(ValidationUtil.class, "Validation is turned off for module: " + ModuleUtil.displayID(module) + "_" + module.getClass().getSimpleName());
		}
	}
	
	public static File getValidationDir() {
		final File dir = new File( Config.pipelinePath() + File.separator + VALIDATION_FOLDER );
		if( !dir.isDirectory() ) {
			dir.mkdirs();
			Log.info( ValidationUtil.class, "Create directory: " + dir.getAbsolutePath() );
		}
		return dir;
	}
	
	private static boolean hasExp(BioModule module) throws Exception {
		return getExpectationFile(module) != null;
	}
	
	private static boolean alwaysPass(BioModule module) throws ConfigFormatException {
		return Config.getBoolean(module, ALWAYS_PASS);
	}
	
	private static void writeRow(BufferedWriter writer, ArrayList<String> row) throws Exception {
		try {
			writer.write(String.join(Constants.TAB_DELIM, row) + Constants.RETURN);
		} catch (Exception e) {
			writer.close();
			throw e;
		}
	}

	private static File getExpectationFile(BioModule module) throws Exception {
		File expectationFile = null;
		String expectationFilePath = Config.getString(module, EXPECTATION_FILE);
		if (expectationFilePath != null && !expectationFilePath.isEmpty()) {
			expectationFile = new File(expectationFilePath);
			if ( !expectationFile.exists()){
				throw new ConfigPathException(expectationFile);
			}
			if (expectationFile.isDirectory()) {
				expectationFile = new File(
						Config.getString(module, EXPECTATION_FILE) + File.separator + getOutputFileName(module));
				if (!expectationFile.exists()) {
					throw new ConfigPathException(expectationFile, "Could not find file: " + getOutputFileName(module) + " in directory "
							+ Config.getString(module, EXPECTATION_FILE)); 
				}
			}
		}
		return expectationFile;
	}

	
	private static List<String> getHeaders(BioModule module) throws ExpectationFileFormatException, Exception {
		List<String> headers = null;
		final List<List<String>> table = parseTableFile(module);
		final Iterator<List<String>> rows = table.iterator();
		headers = rows.next();
		if (headers == null || headers.isEmpty() ) {
			throw new ExpectationFileFormatException("No expectations in expectation file", getExpectationFile(module));
		}
		final String id = headers.get(0);
		if (!id.equals(NAME)) {
			throw new ExpectationFileFormatException(
					"First column in expectation file should be \"" + NAME + "\". Found first column: \"" + id + "\"].",
					getExpectationFile(module));
		}
		Log.debug(ValidationUtil.class, "Expectation file headers: " + headers);
		return headers;
	}

	
	private static HashMap<String, FileSummary> getPrevSummaries(BioModule module) throws Exception {
		HashMap<String, FileSummary> prevOutput = new HashMap<String, FileSummary>();
		List<String> headers = getHeaders(module);
		final List<List<String>> table = parseTableFile(module);
		int rowNum = 1;
		final Iterator<List<String>> rows = table.iterator();
		rows.next();//drop the header
		while (rows.hasNext()) {
			final List<String> row = rows.next();
			final String id = row.get(0);
			if (id == null || id.isEmpty() && !row.toString().isEmpty()) {
				throw new ExpectationFileFormatException( 
						"Row [" + rowNum + "] does not have a value in \"" + NAME + "\" column.", getExpectationFile(module));
			} else {
				FileSummary fs = new FileSummary(id);
				prevOutput.put(fileNameToKey(id), fs);
				for (String cf : getCompareSet(module)) {
					fs.setAtt(cf, row.get(headers.indexOf(cf)));
				}
			}
			rowNum++;
		}
		if (prevOutput == null || prevOutput.isEmpty() ) {
			Log.info(ValidationUtil.class, "Module " + ModuleUtil.displayID(module) + "_" + module.getClass().getSimpleName() + 
					" is expected to have not output.");
		}
		return prevOutput;
	}

	private static List<List<String>> parseTableFile(BioModule module) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		BufferedReader reader = null;
		try {
			reader = BioLockJUtil.getFileReader(getExpectationFile(module));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				final ArrayList<String> record = new ArrayList<>();
				final String[] cells = line.split(Constants.TAB_DELIM, -1);
				for (final String cell : cells)
					if (cell == null || cell.trim().isEmpty())
						record.add(null);
					else
						record.add(cell.trim());
				data.add(record);
			}
		} catch (final Exception ex) {
			throw new Exception("Error occurrred parsing expectation file!", ex);
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (final IOException ex) {
				Log.error(ValidationUtil.class, "Failed to close file reader", ex);
			}
		}
		return data;
	}



	private static ArrayList<String> getCompareSet(BioModule module) throws Exception {
		ArrayList<String> compareFeatures = new ArrayList<String>();
		ArrayList<String> configCompareOn = new ArrayList<String>(Config.getSet(module, COMPARE_ON));
		List<String> headers = getHeaders(module);
		if (configCompareOn == null || configCompareOn.isEmpty()) {
			compareFeatures = new ArrayList<String>(headers);
		} else {
			compareFeatures = configCompareOn;
		}
		// result of prior run of the pipeline, always ignore this.
		compareFeatures.remove(MATCHED_EXPECTATION);
		// name is always compared on by being the key between files.
		compareFeatures.remove(NAME);
		if (!availableAttributes.containsAll(compareFeatures)) {
			throw new ConfigViolationException("Available file attributes for validation are: " + availableAttributes);
		}
		if (!headers.containsAll(compareFeatures)) {
			throw new ConfigViolationException(COMPARE_ON, "Cannot compare on features that are not given in expectation file.");
		}
		Log.debug(ValidationUtil.class, "Comparing based on features: " + compareFeatures);
		return compareFeatures;
	}

	private static ArrayList<String> getReportSet(BioModule module) throws Exception {
		ArrayList<String> reportFeatures = new ArrayList<String>(Config.getSet(module, REPORT_ON));
		if (reportFeatures == null || reportFeatures.isEmpty()) {
			reportFeatures = availableAttributes;
		} else {
			if (!availableAttributes.containsAll(reportFeatures)) {
				reportFeatures.removeAll(availableAttributes);
				throw new ConfigViolationException(REPORT_ON, "Cannot report on: " 
						+ reportFeatures + "." + Constants.RETURN 
						+ "Options to report on include: " + availableAttributes.toString() + "."); 
			}
		}
		// ensure that this column is present and at the beginning.
		reportFeatures.remove(NAME);
		reportFeatures.add(0, NAME);
		// ensure that this column is present and at the end.
		reportFeatures.remove(MATCHED_EXPECTATION);
		reportFeatures.add(MATCHED_EXPECTATION);
		Log.debug(ValidationUtil.class, "Reporting on features: " + reportFeatures);
		return reportFeatures;
	}

	private static File getOutputFile(BioModule module) throws IOException {
		String outName = getValidationDir() + File.separator + getOutputFileName(module);
		File outFile = new File(outName);
		if (!outFile.exists()) {
			outFile.createNewFile();
		}
		return outFile;
	}

	private static String getOutputFileName(BioModule module) {
		return  ModuleUtil.displayID(module) + "_" + module.getClass().getSimpleName() + OUTPUT_FILE_SUFFIX;
	}

	/**
	 * If the file name contains a date, replace that with the word "DATE",
	 * otherwise return it unchanged.
	 * 
	 * @param fileName
	 * @return
	 */
	private static String fileNameToKey(String fileName) {
		String key = fileName.replaceAll("_[0-9]+_[0-9]{4}[A-Za-z]{3}[0-9]{2}", "_DATE");
		if (key.equals(fileName)) {
			key = fileName.replaceAll("_[0-9]{4}[A-Za-z]{3}[0-9]{2}", "_DATE");
		}
		return key;
	}

	/**
	 * {@link biolockj.Config} boolean property {@value #DISABLE_VALIDATION}.
	 * Disable validation for the all modules in the pipeline. Can be specified for
	 * individual modules to override pipeline setting.
	 */
	private static String DISABLE_VALIDATION = "validation.disableValidation";
	/**
	 * {@link biolockj.Config} boolean property. Even if the files do not meet
	 * expectation, just record this, do not halt the pipeline.
	 */
	protected static String ALWAYS_PASS = "validation.alwaysPass";
	/**
	 * {@link biolockj.Config} set property giving the file metrics to use in
	 * comparing to the expectation. Default is to use all metrics in the
	 * expectation file.
	 */
	protected static String COMPARE_ON = "validation.compareOn";
	/**
	 * {@link biolockj.Config} set property giving the file metrics to use report on
	 * as a comma-separate list of strings. Default is to use all currently
	 * available metrics.
	 */
	protected static String REPORT_ON = "validation.reportOn";
	/**
	 * {@link biolockj.Config} String property giving the file path that gives the
	 * expected values for file metrics. Probably generated by a previous run of the
	 * same pipeline.
	 */
	public static String EXPECTATION_FILE = "validation.expectationFile";
	/**
	 * The first column in an expectation file must be {@value #NAME}
	 */
	protected static String NAME = "name";
	/**
	 * The last column in a the output file, {@value #MATCHED_EXPECTATION},
	 * indicates if the referenced file met all expectations
	 * ({@value #FileSummary.status.PASS}}), or not
	 * ({@value #FileSummary.status.FAIL}}), or was not compared to any expectations
	 * ({@value #FileSummary.status.REPORT}}). The value "MATCHED_EXPECTATION" is
	 * hard-coded in the FileSummary.getAtt() method. If this is a column in the
	 * expectation file, it is ignored.
	 */
	protected static String MATCHED_EXPECTATION = "MATCHED_EXPECTATION";
	/**
	 * Append the String {@value #OUTPUT_FILE_SUFFIX} to the name of the previous
	 * module to get the output file name.
	 */
	protected static String OUTPUT_FILE_SUFFIX = "_validation.txt";
	private static String VALIDATION_FOLDER = "validation";
	
	private static String[] sa = {"name", "size", "sizeMB" , "sizeGB", "md5"};
	protected static ArrayList<String> availableAttributes = new ArrayList<String>( Arrays.asList(sa) );
	
	protected static String[] statusStrings = {"FAIL", "PASS", "REPORT"};
	protected static int FAIL = 0;
	protected static int PASS = 1;
	protected static int REPORT = 2;
	
	
	private static class FileSummary {

		private File file = null;
		
		private String name;
		
		/**
		 * The size of the file in bytes
		 */
		private long size = -1;
		
		/**
		 * The size rounded to the nearest MB (size/1e6).
		 */
		private long sizeMB = -1;
		
		/**
		 * The size rounded to the nearest GB (size/1e9).
		 */
		private long sizeGB = -1;
		
		private String md5 = null;
		
		private int validationStatus = ValidationUtil.REPORT;


		public FileSummary(File inFile) throws Exception {
			this.file = inFile;
			this.name = file.getName();
			this.size = file.getTotalSpace();
			sizeMB = size / 1000000L; 
			sizeGB = size / 1000000000L;
		}
		
		public FileSummary(String fileName) {
			name = fileName;
		}
		
		protected void calcMd5() throws IOException, NoSuchAlgorithmException {
			MessageDigest md = MessageDigest.getInstance("MD5");
			// InputStream is = Files.newInputStream(file.getAbsoluteFile());
			InputStream fis = new FileInputStream(file.getAbsoluteFile());
			byte[] bytes = new byte[1024];
			int numRead;
			do {
				numRead = fis.read(bytes);
				if (numRead > 0) {
					md.update(bytes, 0, numRead);
				}
			} while (numRead != -1);
			fis.close();
			byte[] digest = md.digest();

			String md5sum = "";
			for (int i = 0; i < digest.length; i++) {
				md5sum += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
			}
			md5 = md5sum;
		}

		public int compareToExpected(FileSummary other, Collection<String> comparisons) throws Exception {
			if (!ValidationUtil.availableAttributes.containsAll(comparisons)) {
				Log.error(this.getClass(), "Cannot do comparisons on: " + comparisons.removeAll(ValidationUtil.availableAttributes));
				throw new Exception();
			}
			if (other == null) {
				Log.warn(this.getClass(), "Cannot compare against a null.");
				validationStatus = ValidationUtil.FAIL;
				return ValidationUtil.FAIL;
			}
			int tested = 0;
			int passed = 0;
			
			for (String att:ValidationUtil.availableAttributes) {
				if (comparisons.contains(att)) {
					tested ++;
					try {
						if (other.getAtt(att).equals(getAtt(att))) {
							passed++;
						} else {
							mentionMismatch(att, other.getAtt(att), this.getAtt(att));
						}
					} catch (NullPointerException np) {
						Log.warn(this.getClass(), "Cannot compare missing attribute: " + att);
					}
					
				}
			}
			
			if ( tested == passed && passed == comparisons.size() ) {
				validationStatus = ValidationUtil.PASS;
				return ValidationUtil.PASS;
			}else {
				validationStatus = ValidationUtil.FAIL;
				Log.error(this.getClass(), "File " + name + " passed only " + passed + " out of " + comparisons.size() + " comparisons." ); 
				return ValidationUtil.FAIL;
			}
		}
		
		private void mentionMismatch(String attribute, Object expectedValue, Object foundValue) {
			Log.debug(this.getClass(), "File " + name + " failed " + attribute + " comparison.  Expected value: "
					+ expectedValue + "; found: " + foundValue + ".");
		}

		public String getName() {
			return name;
		}

		protected String getAtt(String col) throws Exception {
			switch (col) {
				case "name":
					return getName();
				case "size":
					return String.valueOf(size);
				case "sizeMB":
					return String.valueOf(sizeMB);	
				case "sizeGB":
					return String.valueOf(sizeGB);
				case "md5":
					return md5;
			}
			if (col.equals(ValidationUtil.MATCHED_EXPECTATION) )
			{
				return ValidationUtil.statusStrings[validationStatus];
			}
			return null;
		}
		
		protected void setAtt(String col, String val) throws Exception {
			Log.debug(this.getClass(), "Setting " + col + " to " + val);
			if (!ValidationUtil.availableAttributes.contains(col)) {
				throw new ConfigViolationException("Available file attributes for validation are: " + ValidationUtil.availableAttributes);
			}
			switch (col) {
				case "size":
					size = Long.parseLong(val);
					break;
				case "sizeMB":
					sizeMB = Long.parseLong(val);
					break;
				case "sizeGB":
					sizeGB = Long.parseLong(val);
					break;
				case "md5":
					md5 = val;
					break;
			}
		}
		

		@Override
		public String toString() {
			return "FileSummary [file=" + file + ", name=" + name + ", size=" + size
					+ ", sizeMB=" + sizeMB + ", sizeGB=" + sizeGB
					+ ", validationStatus=" + ValidationUtil.statusStrings[validationStatus] + "]";
		}
		
	}


}
