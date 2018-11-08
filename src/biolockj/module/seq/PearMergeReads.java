/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.seq;

import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.ScriptModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.util.MetaUtil;
import biolockj.util.SeqUtil;

/**
 * This BioModule will merge forward and reverse fastq files using PEAR.<br>
 * For more informations, see the online PEAR manual:
 * <a href="https://sco.h-its.org/exelixis/web/software/pear/doc.html" target=
 * "_top">https://sco.h-its.org/exelixis/web/software/pear/doc.html</a>
 */
public class PearMergeReads extends ScriptModuleImpl implements ScriptModule
{
	/**
	 * Build the script lines for each sample as a nested list. PAIR program will be called once for each pair of files
	 * to output a single merged read.
	 *
	 * @param files List of module input files
	 * @return List of nested bash script lines.
	 * @throws Exception if error occurs generating bash script lines
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		final Set<File> keys = new TreeSet<>( map.keySet() );

		for( final File file: keys )
		{
			final List<String> lines = new ArrayList<>();
			lines.add( FUNCTION_PEAR_MERGE + " " + SeqUtil.getSampleId( file.getName() ) + " " + file.getAbsolutePath()
					+ " " + map.get( file ).getAbsolutePath() + " " + getTempDir().getAbsolutePath() + " "
					+ getOutputDir().getAbsolutePath() );

			// lines.add( Config.getExe( EXE_PEAR ) + " -f " + file.getAbsolutePath() + " -r "
			// + map.get( file ).getAbsolutePath() + " -o " + tempDir + sampleId + params );
			// lines.add( "mv " + tempDir + sampleId + ".assembled." + SeqUtil.FASTQ + " " + outDir + sampleId + "."
			// + SeqUtil.FASTQ );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Validate module dependencies
	 * <ol>
	 * <li>Verify input files are in fastq format
	 * <li>Verify matching paired reads are found
	 * <li>Validate required {@link biolockj.Config}.{@link #EXE_PEAR} property
	 * <li>If running a restarted pipeline that already merged reads, set paired indicator to false
	 * </ol>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		if( !SeqUtil.isFastQ() )
		{
			throw new Exception( "PAIRED READS CAN ONLY BE ASSEMBLED WITH <FASTQ> FILE INPUT" );
		}

		if( !Config.requireBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			throw new Exception( getClass().getName()
					+ " requires paired input data as a combined multiplexed file or as separate files named with "
					+ " matching sample IDs ending in the forward & reverse file suffix values: "
					+ Config.getString( Config.INPUT_FORWARD_READ_SUFFIX ) + " & "
					+ Config.getString( Config.INPUT_REVERSE_READ_SUFFIX ) );
		}

		// verify PEAR mod changes
		Config.getExe( EXE_PEAR );
		//
		// if( !RuntimeParamUtil.isDockerMode() && !BashScriptBuilder.clusterModuleExists( Config.getExe( EXE_PEAR ) ) )
		// {
		// Config.requireExistingFile( EXE_PEAR );
		// }
	}

	/**
	 * Set {@link biolockj.Config}.{@value biolockj.Config#INTERNAL_PAIRED_READS} = {@value biolockj.Config#FALSE} and
	 * register number of reads.
	 */
	@Override
	public void cleanUp() throws Exception
	{
		Config.setConfigProperty( Config.INTERNAL_PAIRED_READS, Config.FALSE );

		if( !MetaUtil.getFieldNames().contains( NUM_MERGED_READS ) )
		{
			Log.info( getClass(),
					"Counting # merged reads/sample for " + getOutputDir().listFiles().length + " files" );
			final Map<String, String> readsPerSample = new HashMap<>();
			for( final File f: getOutputDir().listFiles() )
			{
				if( !f.getName().equals( MetaUtil.getMetadataFileName() ) )
				{
					final long count = SeqUtil.countNumReads( f );
					Log.info( getClass(), "Num merged Reads for File:[" + f.getName() + "] ==> ID:["
							+ SeqUtil.getSampleId( f.getName() ) + "] = " + count );
					readsPerSample.put( SeqUtil.getSampleId( f.getName() ), Long.toString( count ) );
				}
			}

			MetaUtil.addColumn( NUM_MERGED_READS, readsPerSample, getOutputDir() );
		}
		else
		{
			Log.warn( getClass(), "Counts for # merged reads/sample already found in metadata, not re-counting "
					+ MetaUtil.getFile().getAbsolutePath() );
		}

		RegisterNumReads.setNumReadFieldName( NUM_MERGED_READS );
	}

	/**
	 * This method generates the required bash functions used by the worker scripts: {@value #FUNCTION_PEAR_MERGE}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_PEAR_MERGE + "() {" );
		lines.add( Config.getExe( EXE_PEAR ) + " -f $2 -r $3 -o $4" + File.separator + "$1 "
				+ getPearSwitches( Config.getList( EXE_PEAR_PARAMS ) ) );
		lines.add( "mv $4" + File.separator + "$1.assembled." + SeqUtil.FASTQ + " $5" + File.separator + "$1."
				+ SeqUtil.FASTQ );
		lines.add( "}" );
		return lines;
	}

	/**
	 * Get formatted pear switches as provided in prop file (if any).
	 *
	 * @param switches
	 * @return
	 * @throws Exception
	 */
	private String getPearSwitches( final List<String> switches ) throws Exception
	{
		String formattedSwitches = " -j " + Config.requirePositiveInteger( SCRIPT_NUM_THREADS ) + " ";
		for( final String string: switches )
		{
			formattedSwitches += "-" + string + " ";
		}

		return formattedSwitches;
	}

	/**
	 * Metadata column name for column that holds number of reads per sample after merging: {@value #NUM_MERGED_READS}
	 */
	public static final String NUM_MERGED_READS = "Num_Merged_Reads";

	/**
	 * {@link biolockj.Config} property {@value #EXE_PEAR} defines the command line PEAR executable
	 */
	protected static final String EXE_PEAR = "exe.pear";

	/**
	 * {@link biolockj.Config} property {@value #EXE_PEAR_PARAMS} is used to set the PEAR executable runtime parameters
	 */
	protected static final String EXE_PEAR_PARAMS = "exe.pearParams";

	/**
	 * {@link biolockj.Config} property {@value #FUNCTION_PEAR_MERGE} is used to set the PEAR executable runtime
	 * parameters
	 */
	/**
	 * Name of the bash function that merges files with PEAR: {@value #FUNCTION_PEAR_MERGE}
	 */
	protected static final String FUNCTION_PEAR_MERGE = "mergeReads";

}
