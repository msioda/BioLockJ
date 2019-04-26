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
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.DockerUtil;
import biolockj.util.ModuleUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule uses RDP to assign taxonomy to 16s sequences.
 * 
 * @blj.web_desc RDP Classifier
 */
public class RdpClassifier extends ClassifierModuleImpl {

	/**
	 * Build bash script lines to classify unpaired reads with RDP. The inner list contains the bash script lines
	 * required to classify 1 sample (call java to run RDP jar on sample).
	 * <p>
	 * Example line: "java -jar $RDP_PATH t /database/silva128/rRNAClassifier.properties -o
	 * ./output/sample42.fasta_reported.tsv ./input/sample42.fasta"
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files ) {
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator
				+ SeqUtil.getSampleId( file.getName() ) + Constants.PROCESSED;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( FUNCTION_RDP + " " + file.getAbsolutePath() + " " + outputFile );
			data.add( lines );
		}

		return data;
	}

	@Override
	public void checkDependencies() throws Exception {
		super.checkDependencies();
		Config.requireString( this, RDP_JAR );
		getRuntimeParams( getClassifierParams(), null );
	}

	/**
	 * RDP uses java to run a JAR file, so no special command is required
	 */
	@Override
	public String getClassifierExe() throws Exception {
		return null;
	}

	/**
	 * Do not accept -t to define a database, since that instead requires the specific property: {@value #RDP_DB}
	 */
	@Override
	public List<String> getClassifierParams() throws Exception {
		final List<String> validParams = new ArrayList<>();
		for( final String param: Config.getList( this, RDP_PARAMS ) ) {
			if( param.startsWith( DB_PARAM ) ) {
				Log.warn( getClass(), "Ignoring " + DB_PARAM + " value: [ " + param + " ] set in Config property "
					+ RDP_PARAMS + "since this property must be explictily defined in " + RDP_DB );
			} else {
				validParams.add( param );
			}
		}

		return validParams;
	}

	@Override
	public File getDB() throws Exception {
		if( Config.getString( this, RDP_DB ) != null )
			return new File( Config.getSystemFilePath( Config.getString( this, RDP_DB ) ) );

		return null;
	}

	/**
	 * If paired reads found, add prerequisite: {@link biolockj.module.seq.PearMergeReads}.
	 */
	@Override
	public List<String> getPreRequisiteModules() throws Exception {
		final List<String> preReqs = new ArrayList<>();
		if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) ) {
			preReqs.add( ModuleUtil.getDefaultMergePairedReadsConverter() );
		}
		preReqs.addAll( super.getPreRequisiteModules() );
		return preReqs;
	}

	/**
	 * This method generates the required bash functions: {@value #FUNCTION_RDP}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_RDP + "() {" );
		lines.add( Config.getExe( this, Constants.EXE_JAVA ) + " " + getJavaParams() + JAVA_JAR_PARAM + " " + getJar()
			+ " " + getRuntimeParams( getClassifierParams(), null ) + getDbParam() + OUTPUT_PARAM + " $2 $1" );
		lines.add( "}" + RETURN );
		return lines;
	}

	private String getDbParam() throws Exception {
		if( getDB() == null ) return "";

		final String dbParam = DockerUtil.inDockerEnv()
			? getDB().getAbsolutePath().replace( getDB().getParentFile().getAbsolutePath(),
				DockerUtil.CONTAINER_DB_DIR )
			: getDB().getAbsolutePath();

		return DB_PARAM + " " + dbParam + " ";
	}

	private String getJar() throws Exception {
		return Config.requireString( this, RDP_JAR );
	}

	private String getJavaParams() throws Exception {
		return Config.getExeParams( this, Constants.EXE_JAVA );
	}

	/**
	 * Name of the RdpClassifier bash script function used to assign taxonomy: {@value #FUNCTION_RDP}
	 */
	protected static final String FUNCTION_RDP = "runRdp";

	/**
	 * {@link biolockj.Config} File property used to define an alternate RDP database file: {@value #RDP_DB}
	 */
	protected static final String RDP_DB = "rdp.db";

	/**
	 * {@link biolockj.Config} File property for RDP java executable JAR: {@value #RDP_JAR}
	 */
	protected static final String RDP_JAR = "rdp.jar";

	/**
	 * {@link biolockj.Config} List property for RDP java executable JAR runtime params: {@value #RDP_PARAMS}
	 */
	protected static final String RDP_PARAMS = "exe.rdpParams";

	private static final String DB_PARAM = "-t";
	private static final String JAVA_JAR_PARAM = "-jar";
	private static final String OUTPUT_PARAM = "-o";
}
