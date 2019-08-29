/**
 * @UNCC Fodor Lab
 * @author Shan Sun
 * @email ssun5@uncc.edu
 * @date Aug 12, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.assembly;

import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.exception.*;
import biolockj.module.SeqModuleImpl;
import biolockj.module.seq.PearMergeReads;
import biolockj.util.SeqUtil;

/**
 * This BioModule builds the bash scripts used to assemble WGS sequences with MetaSPAdes, bin contigs with Metabat2 and
 * check quality with checkM.
 *
 * @blj.web_desc Genome Assembly
 */
public class GenomeAssembly extends SeqModuleImpl {

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		throw new BioLockJException( ERR_MSG );
	}

	/**
	 * MetaSPAdes doesn't require parameters besides input files and output directory. Default threads is 16 and memory
	 * is 250G<br>
	 * Verify no invalid runtime params are passed and add rankSwitch if needed.<br>
	 * 
	 * @return runtime parameters
	 * @throws Exception if errors occur
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		for( final File file: map.keySet() ) {
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String outputDir = getOutputDir().getAbsolutePath() + File.separator + fileId + "_assembly";
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( FUNCTION_ASSEMBLY + " " + file.getAbsolutePath() + " " + map.get( file ).getAbsolutePath() +
				" " + outputDir );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Samples must be paired reads since metaspades currently doesn't support single reads.
	 */
	@Override
	public void checkDependencies() throws Exception {
		if( !hasPairedReadInput() ) throw new BioLockJException( ERR_MSG );
		getWorkerScriptFunctions();
	}

	/**
	 * Build function to call metaspades, metabat, and Checkm
	 * 
	 * $1 - forward read $2 - reverse
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception {
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_ASSEMBLY + "() {" );
		lines.add(
			getMetaspadesExe() + " " + getMetaspadeParams() + " -1 $1 -2 $2 " + OUTPUT_PARAM + " $3" + ASSEMBLY_DIR );
		lines.add( getMetabatExe() + " " + getMetabatParams() + " " + INPUT_PARAM + " $3" + CONTIG_DIR + " " +
			OUTPUT_PARAM + " $3" + METAS_BIN_DIR );
		lines.add( getCheckmExe() + " lineage_wf " + FILE_PARAM + " $3" + CHECKM_OUTPUT + " " + getCheckmParams() +
			" $3" + BINS_DIR + " $3" + SCG_DIR + " " );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * Get standard Checkm runtime parameters.
	 * 
	 * @return checkm runtime params
	 * @throws ConfigException if properties are missing or invalid
	 */
	protected String getCheckmParams() throws ConfigException {
		return getRuntimeParams( Config.getList( this, EXE_CHECKM_PARAMS ), NUM_THREADS_PARAM );
	}

	/**
	 * Get Metabat runtime parameters.
	 * 
	 * @return metabat runtime params
	 * @throws ConfigException if properties are missing or invalid
	 */
	protected String getMetabatParams() throws ConfigException {
		return getRuntimeParams( Config.getList( this, EXE_METABAT2_PARAMS ), null );
	}

	/**
	 * Get Metaspade runtime parameters.
	 * 
	 * @return metaspade runtime parameters
	 * @throws ConfigException if properties are missing or invalid
	 */
	protected String getMetaspadeParams() throws ConfigException {
		return TEMP_DIR_PARAM + " " + getTempDir().getAbsolutePath() + " " +
			getRuntimeParams( Config.getList( this, EXE_METASPADES_PARAMS ), NUM_THREADS_PARAM );
	}

	private String getCheckmExe() throws ConfigViolationException {
		return Config.getExe( this, EXE_CHECKM );
	}

	private String getMetabatExe() throws ConfigViolationException {
		return Config.getExe( this, EXE_METABAT2 );
	}

	private String getMetaspadesExe() throws ConfigViolationException {
		return Config.getExe( this, EXE_METASPADES2 );
	}

	private boolean hasPairedReadInput() throws Exception {
		for( final String module: Config.requireList( this, Constants.INTERNAL_BLJ_MODULE ) )
			if( module.equals( PearMergeReads.class.getName() ) ) return false;
			else if( module.equals( getClass().getName() ) ) break;
		return SeqUtil.hasPairedReads();
	}

	/**
	 * {@link biolockj.Config} exe property for checkm executable: {@value #EXE_CHECKM}
	 */
	protected static final String EXE_CHECKM = "exe.checkm";

	/**
	 * {@link biolockj.Config} List property used to obtain the checkm runtime parameters: {@value #EXE_CHECKM_PARAMS}
	 */
	protected static final String EXE_CHECKM_PARAMS = "exe.checkmParams";

	/**
	 * {@link biolockj.Config} exe property for metabat2 executable: {@value #EXE_METABAT2}
	 */
	protected static final String EXE_METABAT2 = "exe.metabat2";

	/**
	 * {@link biolockj.Config} List property used to obtain the metabat runtime parameters:
	 * {@value #EXE_METABAT2_PARAMS}
	 */
	protected static final String EXE_METABAT2_PARAMS = "exe.metabat2Params";

	/**
	 * {@link biolockj.Config} List property used to obtain the metaspade runtime parameters:
	 * {@value #EXE_METASPADES_PARAMS}
	 */
	protected static final String EXE_METASPADES_PARAMS = "exe.metaspade2Params";

	/**
	 * {@link biolockj.Config} exe property for metaspades executable: {@value #EXE_METASPADES2}
	 */
	protected static final String EXE_METASPADES2 = "exe.metaspades2";

	/**
	 * Name of the metaspades function used to assemble reads: {@value #FUNCTION_ASSEMBLY}
	 */
	protected static final String FUNCTION_ASSEMBLY = "runAssembly";

	/**
	 * {@link biolockj.Config} Positive integer property defines total metabat memory per sample.
	 */
	protected static final String METABAT_MEMORY = "metabat.memory";

	/**
	 * {@link biolockj.Config} Positive integer property defines total metaspades memory per sample.
	 */
	protected static final String METASPADES_MEMORY = "metaspades.memory";

	private static final String ASSEMBLY_DIR = File.separator + "assembly";
	private static final String BINS_DIR = File.separator + "bins";
	private static final String CHECKM_OUTPUT = File.separator + "CheckM" + Constants.TXT_EXT;
	private static final String CONTIG_DIR = ASSEMBLY_DIR + File.separator + "contigs." + Constants.FASTA;
	private static final String ERR_MSG = "Metaspades requires paired reads as input!";
	private static final String FILE_PARAM = "-f";
	private static final String INPUT_PARAM = "-i";
	private static final String METAS_BIN_DIR = BINS_DIR + File.separator + "bin";
	private static final String NUM_THREADS_PARAM = "-t";
	private static final String OUTPUT_PARAM = "-o";
	private static final String SCG_DIR = File.separator + "SCG";
	private static final String TEMP_DIR_PARAM = "--tmp-dir";
}
