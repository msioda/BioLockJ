/**
 * @UNCC BINF 8380
 *
 * @author Michael Sioda
 * @date Oct 6, 2017
 */
package biolockj.module.classifier;

import java.io.File;
import java.util.List;
import biolockj.module.BioModule;

/**
 *
 */
public interface ClassifierModule extends BioModule
{

	public List<List<String>> buildScript( final List<File> files ) throws Exception;

	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception;

	public String getClassifierExe() throws Exception;

	public String getClassifierParams() throws Exception;

	public static final String EXE_CLASSIFIER = "exe.classifier";
	public static final String EXE_CLASSIFIER_PARAMS = "exe.classifierParams";
	public static final String PROCESSED = "_reported.tsv";
}
