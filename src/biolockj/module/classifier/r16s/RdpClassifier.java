/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.classifier.r16s;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.BioModuleFactory;
import biolockj.Config;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.SeqUtil;

/**
 * This BioModule uses RDP to assign taxonomy to 16s sequences.
 */
public class RdpClassifier extends ClassifierModuleImpl implements ClassifierModule
{

	/**
	 * Build bash script lines to classify unpaired reads with RDP. The inner list contains the bash script lines
	 * required to classify 1 sample (call java to run RDP jar on sample).
	 * <p>
	 * Example line: "java -jar $RDP_PATH t /database/silva128/rRNAClassifier.properties -o
	 * ./output/sample42.fasta_reported.tsv ./input/sample42.fasta"
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator
					+ SeqUtil.getSampleId( file.getName() ) + PROCESSED;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( FUNCTION_RDP + " " + file.getAbsolutePath() + " " + outputFile );
			data.add( lines );
		}

		return data;
	}

	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getRuntimeParams( getClassifierParams(), null );
	}

	/**
	 * If paired reads found, return prerequisite module: {@link biolockj.module.seq.PearMergeReads}.
	 */
	@Override
	public List<Class<?>> getPreRequisiteModules() throws Exception
	{
		final List<Class<?>> preReqs = super.getPreRequisiteModules();
		if( Config.getBoolean( SeqUtil.INTERNAL_PAIRED_READS ) )
		{
			preReqs.add( Class.forName( BioModuleFactory.getDefaultMergePairedReadsConverter() ) );
		}

		return preReqs;
	}

	/**
	 * This method generates the required bash functions: {@value #FUNCTION_RDP}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_RDP + "() {" );
		lines.add( Config.getExe( EXE_JAVA ) + " " + JAVA_JAR_PARAM + " " + getClassifierExe() + " "
				+ getRuntimeParams( getClassifierParams(), null ) + OUTPUT_PARAM + " $2 $1" );
		lines.add( "}" );
		return lines;
	}

	/**
	 * {@link biolockj.Config} property for java executable: {@value #EXE_JAVA}
	 */
	protected static final String EXE_JAVA = "exe.java";

	/**
	 * Name of the RdpClassifier bash script function used to assign taxonomy: {@value #FUNCTION_RDP}
	 */
	protected static final String FUNCTION_RDP = "runRdp";

	private static final String JAVA_JAR_PARAM = "-jar";
	private static final String OUTPUT_PARAM = "-o";

}
