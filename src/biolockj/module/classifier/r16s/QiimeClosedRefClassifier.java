/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Apr 5, 2017
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
import biolockj.Log;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.implicit.qiime.MergeOtuTables;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.util.*;

/**
 * This BioModule executes the QIIME script pick_closed_reference_otus.py on a FastA sequence files. Unlike open and de
 * novo OTU picking scripts, which require a single multiplexed file for cluster analysis, closed reference OTU picking
 * can be run in batches (output/batch_0, output/batch_1, output/batch_2, etc.). The program awk is used to split the
 * metadata into separate batch-specific QIIME mapping files.
 */
public class QiimeClosedRefClassifier extends QiimeClassifier implements ClassifierModule
{

	/**
	 * Create bash script lines to split up the QIIME mapping and fasta files into batches of size
	 * {@link biolockj.Config}.{@value biolockj.module.ScriptModule#SCRIPT_BATCH_SIZE}
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		Log.info( getClass(), "Processing " + ( files == null ? 0: files.size() ) + " files" );
		final List<List<String>> data = new ArrayList<>();
		List<String> lines = new ArrayList<>();
		if( RuntimeParamUtil.isDockerMode() || Config.requirePositiveInteger( SCRIPT_BATCH_SIZE ) >= files.size() )
		{
			Log.info( getClass(), "Batch size > # sequence files, so run all in 1 batch" );
			lines.addAll( getPickOtuLines( PICK_OTU_SCRIPT, getInputFileDir(), MetaUtil.getFile().getAbsolutePath(),
					getTempDir() ) );
			lines.add( copyBatchOtuTableToOutputDir( getTempDir(), null ) );
			data.add( lines );
		}
		else
		{
			Log.info( getClass(),
					"Pick closed ref OTUs in batches of size: " + Config.requirePositiveInteger( SCRIPT_BATCH_SIZE ) );
			int startIndex = 1;
			int batchNum = 0;
			int sampleCount = 0;
			for( final File f: files )
			{
				lines.add( "cp " + f.getAbsolutePath() + " " + getBatchFastaDir( batchNum ).getAbsolutePath() );
				if( doAddNextBatch( ++sampleCount ) )
				{
					data.add( getBatch( lines, batchNum++, startIndex ) );
					startIndex = startIndex + Config.requirePositiveInteger( SCRIPT_BATCH_SIZE );
					lines = new ArrayList<>();
				}
			}

			if( addFinalBatch( sampleCount ) )
			{
				data.add( getBatch( lines, batchNum, startIndex ) );
			}
		}

		return data;
	}

	/**
	 * Call {@link biolockj.module.implicit.qiime.QiimeClassifier} checkOtuPickingDependencies() method to verify OTU
	 * picking script parameters.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		checkOtuPickingDependencies();
	}

	/**
	 * Call {@link #buildScript(List)} to create bash script lines needed to batch jobs for closed ref OTU picking. Pass
	 * lines to {@link biolockj.util.BashScriptBuilder#buildScripts(biolockj.module.ScriptModule, List, int)} to create
	 * the bash scripts
	 */
	@Override
	public void executeTask() throws Exception
	{
		BashScriptBuilder.buildScripts( this, buildScript( getInputFiles() ), 1 );
	}

	/**
	 * If paired reads found, return prerequisite module: {@link biolockj.module.seq.PearMergeReads}.
	 */
	@Override
	public List<Class<?>> getPostRequisiteModules() throws Exception
	{
		final List<Class<?>> postReqs = new ArrayList<>();
		if( !RuntimeParamUtil.isDockerMode() && ( Config.getBoolean( Config.INTERNAL_MULTIPLEXED )
				|| BioLockJUtil.getBasicInputFiles().size() > Config.requireInteger( SCRIPT_BATCH_SIZE ) ) )
		{
			postReqs.add( MergeOtuTables.class );
		}

		postReqs.addAll( super.getPostRequisiteModules() );

		return postReqs;
	}

	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_CREATE_BATCH_MAPPING + "() {" );
		lines.add( Config.getExe( Config.EXE_AWK ) + " 'NR==1' " + MetaUtil.getFile().getAbsolutePath() + " > $1" );
		lines.add( Config.getExe( Config.EXE_AWK ) + " 'NR>'$2'&&NR<='$3 " + MetaUtil.getFile().getAbsolutePath()
				+ " >> $1" );
		lines.add( "}" );
		return lines;
	}

	/**
	 * The method returns 1 bash script line that will copy the batch
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#OTU_TABLE} from the batchDir to the output directory.
	 * 
	 * @param batchDir Batch directory
	 * @param batchNum Batch number used to name biom file, or null if processing 1 batch
	 * @return Bash script line to copy temp/otu_table.biom to output/otu_table.biom
	 */
	protected String copyBatchOtuTableToOutputDir( final File batchDir, final Integer batchNum )
	{
		final String fileName = batchNum == null ? OTU_TABLE: OTU_TABLE_PREFIX + "_" + batchNum + ".biom";
		return "cp " + batchDir.getAbsolutePath() + File.separator + OTU_TABLE + " " + getOutputDir().getAbsolutePath()
				+ File.separator + fileName;
	}

	/**
	 * Return true if the sample count indicates we have a full batch
	 *
	 * @param sampleCount Current number of samples processed so far
	 * @return boolean true if another batch is needed
	 * @throws Exception if batch size is undefined or invalid
	 */
	protected boolean doAddNextBatch( final int sampleCount ) throws Exception
	{
		if( sampleCount % Config.requirePositiveInteger( SCRIPT_BATCH_SIZE ) == 0 )
		{
			return true;
		}
		return false;
	}

	/**
	 * Method returns batch script lines for one batch.
	 * 
	 * @param lines Batch script lines to copy fasta to batch_#/fasta
	 * @param batchNum Batch number
	 * @param index Start index used to break up mapping file
	 * @return List of batch script lines used to process batch
	 * @throws Exception if unable to get batch script lines
	 */
	protected List<String> getBatch( final List<String> lines, final int batchNum, final int index ) throws Exception
	{
		final File batchDir = getBatchDir( batchNum );
		final String mapping = batchDir.getAbsolutePath() + File.separator + BATCH_MAPPING;
		final int endIndex = index + Config.requirePositiveInteger( SCRIPT_BATCH_SIZE );
		lines.add( FUNCTION_CREATE_BATCH_MAPPING + " " + mapping + " " + index + " " + endIndex );
		lines.addAll( getPickOtuLines( PICK_OTU_SCRIPT, getBatchFastaDir( batchNum ), mapping, batchDir ) );
		lines.add( copyBatchOtuTableToOutputDir( batchDir, batchNum ) );
		return lines;
	}

	/**
	 * Get the pipeline/module/temp/batch_# dir. Create it if it doesn't exist.
	 *
	 * @param batchNum Batch number
	 * @return Batch temp/batch_# directory
	 * @throws Exception if unable to created batch directory
	 */
	protected File getBatchDir( final int batchNum ) throws Exception
	{
		final File dir = new File( getTempDir().getAbsolutePath() + File.separator + "batch_" + batchNum );
		if( !dir.exists() )
		{
			dir.mkdirs();
		}

		return dir;
	}

	/**
	 * Get the pipeline/module/temp/batch_#/fasta directory. Create it if it doesn't exist.
	 *
	 * @param batchNum Batch number
	 * @return Batch temp/batch_#/fasta directory
	 * @throws Exception if unable to create the batch directory
	 */
	protected File getBatchFastaDir( final int batchNum ) throws Exception
	{
		final File f = new File( getBatchDir( batchNum ) + File.separator + SeqUtil.FASTA );
		if( !f.exists() )
		{
			f.mkdirs();
		}
		return f;
	}

	/**
	 * Return true if there is a batch of samples that need to be processed (if sampleCount did not reach the batch size
	 * yet)
	 *
	 * @param sampleCount Current number of samples processed so far
	 * @return boolean true if another batch is needed
	 * @throws Exception if batch size is undefined or invalid
	 */
	private boolean addFinalBatch( final int sampleCount ) throws Exception
	{
		if( sampleCount % Config.requirePositiveInteger( SCRIPT_BATCH_SIZE ) != 0 )
		{
			return true;
		}
		return false;
	}

	/**
	 * Name of each batch mapping file (each in its own batch directory)
	 */
	protected static final String BATCH_MAPPING = "batchMapping" + TSV_EXT;

	/**
	 * Name of the bash function that prepares a batch of seqs for processing: {@value #FUNCTION_CREATE_BATCH_MAPPING}
	 */
	protected static final String FUNCTION_CREATE_BATCH_MAPPING = "createBatchMapping";

	/**
	 * Closed reference OTU picking script
	 */
	protected static final String PICK_OTU_SCRIPT = "pick_closed_reference_otus.py";

}
