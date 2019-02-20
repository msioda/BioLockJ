/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 20, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.seq;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.Constants;
import biolockj.module.SeqModule;
import biolockj.module.SeqModuleImpl;
import biolockj.util.SeqUtil;

/**
 * This BioModule runs biobakery kneaddata program to remove contaminated DNA.<br>
 * Multiple contaminent DNA databases can be used to filter reads simultaniously.<br>
 * Common contaminents include Human, Viral, and Plasmid DNA.<br>
 * @web_desc Knead Data Sanitizer
 */
public class KneadData extends SeqModuleImpl implements SeqModule
{

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File seqFile: files )
		{
			if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS )
					&& !SeqUtil.isForwardRead( seqFile.getName() ) )
			{
				continue;
			}

			final ArrayList<String> lines = new ArrayList<>();

			if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) )
			{
				lines.add( sanatize( seqFile, SeqUtil.getPairedReads( files ).get( seqFile ) ) );
			}
			else
			{
				lines.add( sanatize( seqFile, null ) );
			}

			lines.addAll( buildScriptLinesToMoveValidSeqsToOutputDir( SeqUtil.getSampleId( seqFile.getName() ) ) );

			data.add( lines );
		}

		return data;
	}

	@Override
	public void checkDependencies() throws Exception
	{
		if( !SeqUtil.isFastQ() )
		{
			throw new Exception( getClass().getName() + " requires FASTQ format!" );
		}
		getParams();
	}

	/**
	 * This method generates the worker script function: {@value #FUNCTION_SANATIZE}.
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_SANATIZE + "() {" );
		lines.add( Config.getExe( this, EXE_KNEADDATA ) + " " + getParams() + OUTPUT_FILE_PREFIX_PARAM + " $1 "
				+ INPUT_PARAM + " $2 "
				+ ( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) ? INPUT_PARAM + " $3 ": "" ) + OUTPUT_PARAM
				+ " " + getTempDir().getAbsolutePath() );
		lines.add( "}" + RETURN );
		return lines;
	}

	/**
	 * Move 1 file named /"Sample_ID.fastq/" if module input consists of forward reads only.<br>
	 * If module input contains paired reads, move 2 files named /"Sample_ID_paired_1.fastq/" and
	 * /"Sample_ID_paired_2.fastq/" to the module output directory (after renaming them to BioLockJ standards).
	 *
	 * @param sampleId Sample ID
	 * @return Script lines to move the file or files
	 * @throws Exception if errors occur building lines
	 */
	protected List<String> buildScriptLinesToMoveValidSeqsToOutputDir( final String sampleId ) throws Exception
	{
		final List<String> lines = new ArrayList<>();
		final String fileSuffix = fastqExt();
		if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) )
		{
			final String fwSuffix = Config.requireString( this, Constants.INPUT_FORWARD_READ_SUFFIX );
			final String rvSuffix = Config.requireString( this, Constants.INPUT_REVERSE_READ_SUFFIX );
			final File fwOutFile = new File(
					getOutputDir().getAbsolutePath() + File.separator + sampleId + fwSuffix + fileSuffix );
			final File rvOutFile = new File(
					getOutputDir().getAbsolutePath() + File.separator + sampleId + rvSuffix + fileSuffix );
			lines.add(
					"mv " + getSanatizedFile( sampleId, false ).getAbsolutePath() + " " + fwOutFile.getAbsolutePath() );
			lines.add(
					"mv " + getSanatizedFile( sampleId, true ).getAbsolutePath() + " " + rvOutFile.getAbsolutePath() );
		}
		else
		{
			final File outFile = new File( getOutputDir().getAbsolutePath() + File.separator + sampleId + fileSuffix );
			lines.add( "mv " + getSanatizedFile( sampleId, null ).getAbsolutePath() + " " + outFile.getAbsolutePath() );
		}

		return lines;
	}

	/**
	 * Return sanitized sequence data file.
	 *
	 * @param sampleId Sample ID
	 * @param isRvRead Boolean TRUE to return the file containing reverse reads
	 * @return File with sanitized sequences
	 * @throws Exception if errors occur
	 */
	protected File getSanatizedFile( final String sampleId, final Boolean isRvRead ) throws Exception
	{
		String suffix = "";
		if( Config.getBoolean( this, Constants.INTERNAL_PAIRED_READS ) )
		{
			suffix += isRvRead ? RV_OUTPUT_SUFFIX: FW_OUTPUT_SUFFIX;
		}

		return new File( getTempDir().getAbsolutePath() + File.separator + sampleId + suffix + fastqExt() );
	}

	private String fastqExt() throws Exception
	{
		return "." + Constants.FASTQ;
	}

	private String getParams() throws Exception
	{
		String params = getRuntimeParams( Config.getList( this, EXE_KNEADDATA_PARAMS ), NUM_THREADS_PARAM );
		for( final File db: Config.requireExistingDirs( this, KNEAD_DBS ) )
		{
			params += DB_PARAM + " " + db.getAbsolutePath() + " ";
		}

		return params;
	}

	private String sanatize( final File seqFile, final File rvRead ) throws Exception
	{
		return FUNCTION_SANATIZE + " " + SeqUtil.getSampleId( seqFile.getName() ) + " " + seqFile.getAbsolutePath()
				+ ( rvRead == null ? "": " " + rvRead.getAbsolutePath() );
	}

	/**
	 * KneadData executable: {@value #EXE_KNEADDATA}
	 */
	protected static final String EXE_KNEADDATA = "exe.kneaddata";

	/**
	 * {@link biolockj.Config} property containing parameters for {@value #EXE_KNEADDATA}:
	 * {@value #EXE_KNEADDATA_PARAMS}
	 */
	protected static final String EXE_KNEADDATA_PARAMS = "exe.kneaddataParams";

	/**
	 * Name of the bash function used to decompress gzipped files: {@value #FUNCTION_SANATIZE}
	 */
	protected static final String FUNCTION_SANATIZE = "sanatizeData";

	/**
	 * {@link biolockj.Config} required property to the contaminent databases {@value #KNEAD_DBS}:
	 */
	protected static final String KNEAD_DBS = "kneaddata.dbs";

	private static final String DB_PARAM = "-db";
	private static final String FW_OUTPUT_SUFFIX = "_paired_1";
	private static final String INPUT_PARAM = "-i";
	private static final String NUM_THREADS_PARAM = "-t";
	private static final String OUTPUT_FILE_PREFIX_PARAM = "--output-prefix";
	private static final String OUTPUT_PARAM = "-o";
	private static final String RV_OUTPUT_SUFFIX = "_paired_2";
}
