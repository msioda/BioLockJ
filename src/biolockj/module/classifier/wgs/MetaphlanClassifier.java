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
package biolockj.module.classifier.wgs;

import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.BioLockJUtil;
import biolockj.util.SeqUtil;
import biolockj.util.TaxaUtil;

/**
 * This BioModule builds the bash scripts used to execute metaphlan2.py to classify WGS sequences.
 */
public class MetaphlanClassifier extends ClassifierModuleImpl implements ClassifierModule
{
	/**
	 * Build bash script lines to classify unpaired WGS reads with Metaphlan. The inner list contains 1 bash script line
	 * per sample.
	 * <p>
	 * Example line:<br>
	 * python /app/metaphlan2.py --nproc 8 -t rel_ab_w_read_stats --input_type fasta ./input/sample42.fasta --bowtie2out
	 * ./temp/sample42.bowtie2.bz2 &gt; ./output/sample42_reported.tsv
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;
			final String bowtie2Out = getTempDir().getAbsolutePath() + File.separator + fileId + bowtie2ext;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( FUNCTION_RUN_METAPHLAN + " " + file.getAbsolutePath() + " " + bowtie2Out + " " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Build bash script lines to classify paired WGS reads with Metaphlan. The inner list contains 1 bash script line
	 * per sample.
	 * <p>
	 * Example line:<br>
	 * python /app/metaphlan2.py --nproc 8 -t rel_ab_w_read_stats --input_type fasta ./input/sample42_R1.fasta,
	 * ./input/sample42_R2.fasta --bowtie2out ./temp/sample42.bowtie2.bz2 &gt; ./output/sample42_reported.tsv
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		for( final File file: map.keySet() )
		{
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator + fileId + PROCESSED;
			final String bowtie2Out = getTempDir().getAbsolutePath() + File.separator + fileId + bowtie2ext;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( FUNCTION_RUN_METAPHLAN + " " + file.getAbsolutePath() + "," + map.get( file ).getAbsolutePath()
					+ " " + bowtie2Out + " " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Verify none of the derived command line parameters are included in
	 * {@link biolockj.Config}.{@value biolockj.module.classifier.ClassifierModule#EXE_CLASSIFIER_PARAMS}
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getDefaultSwitches();
	}

	/**
	 * This method generates the required bash function: {@value #FUNCTION_RUN_METAPHLAN}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_RUN_METAPHLAN + "() {" );
		lines.add( Config.getExe( EXE_PYTHON ) + " " + getClassifierExe() + " " + getDefaultSwitches()
				+ "$1 --bowtie2out $2 > $3" );
		lines.add( "}" );
		return lines;
	}

	/**
	 * Metaphlan queries require standard parameters: --input_type, --nproc, -t<br>
	 * Verify no invalid runtime params are passed and add rankSwitch if needed.<br>
	 * Set the rankSwitch based on the {@link biolockj.Config}.{@value biolockj.util.TaxaUtil#REPORT_TAXONOMY_LEVELS} if
	 * only one taxonomy level is to be reported, otherwise report all levels.
	 * 
	 * @return runtime parameters
	 * @throws Exception if errors occur
	 */
	protected String getDefaultSwitches() throws Exception
	{
		if( defaultSwitches == null )
		{
			final String params = BioLockJUtil.join( getClassifierParams() );

			if( params.indexOf( "--input_type " ) > -1 )
			{
				throw new Exception(
						"Invalid classifier option (--input_type) found in property(" + EXE_CLASSIFIER_PARAMS
								+ "). BioLockJ derives this value by examinging one of the input files." );
			}
			if( params.indexOf( NUM_THREADS_PARAM ) > -1 )
			{
				throw new Exception( "Ignoring nvalid classifier option (" + NUM_THREADS_PARAM + ") found in property("
						+ EXE_CLASSIFIER_PARAMS + "). BioLockJ derives this value from property: " + NUM_THREADS );
			}
			if( params.indexOf( "--bowtie2out " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (--bowtie2out) found in property("
						+ EXE_CLASSIFIER_PARAMS + "). BioLockJ outputs bowtie2out files to MetaphlanClassifier/temp." );
			}
			if( params.indexOf( "-t rel_ab_w_read_stats " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (-t rel_ab_w_read_stats). BioLockJ hard codes this "
						+ "option for MetaPhlAn so must not be included in the property file." );
			}
			if( params.indexOf( "--tax_lev " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (--tax_lev) found in property(" + EXE_CLASSIFIER_PARAMS
						+ "). BioLockJ sets this value based on: " + TaxaUtil.REPORT_TAXONOMY_LEVELS );
			}
			if( params.indexOf( "-s " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (-s) found in property(" + EXE_CLASSIFIER_PARAMS
						+ "). SAM output not supported.  BioLockJ outputs " + TSV_EXT + " files." );
			}
			if( params.indexOf( "-o " ) > -1 )
			{
				throw new Exception( "Invalid classifier option (-o) found in property(" + EXE_CLASSIFIER_PARAMS
						+ "). BioLockJ outputs results to: " + getOutputDir().getAbsolutePath() + File.separator );
			}

			defaultSwitches = getRuntimeParams( getClassifierParams(), NUM_THREADS_PARAM );

			if( TaxaUtil.getTaxaLevels().size() == 1 )
			{
				defaultSwitches += "--tax_lev " + taxaLevelMap.get( TaxaUtil.getTaxaLevels().get( 0 ) ) + " ";
			}

			defaultSwitches += "--input_type " + Config.requireString( SeqUtil.INTERNAL_SEQ_TYPE )
					+ " -t rel_ab_w_read_stats ";
		}

		return defaultSwitches;
	}

	private String defaultSwitches = null;

	private final Map<String, String> taxaLevelMap = new HashMap<>();

	{
		taxaLevelMap.put( TaxaUtil.SPECIES, METAPHLAN_SPECIES );
		taxaLevelMap.put( TaxaUtil.GENUS, METAPHLAN_GENUS );
		taxaLevelMap.put( TaxaUtil.FAMILY, METAPHLAN_FAMILY );
		taxaLevelMap.put( TaxaUtil.ORDER, METAPHLAN_ORDER );
		taxaLevelMap.put( TaxaUtil.CLASS, METAPHLAN_CLASS );
		taxaLevelMap.put( TaxaUtil.PHYLUM, METAPHLAN_PHYLUM );
		taxaLevelMap.put( TaxaUtil.DOMAIN, METAPHLAN_DOMAIN );
	}

	/**
	 * File suffix for intermediate bowtie2 index created by metaphlan
	 */
	protected static final String bowtie2ext = ".bowtie2.bz2";

	/**
	 * {@link biolockj.Config} property to python executable
	 */
	protected static final String EXE_PYTHON = "exe.python";

	/**
	 * Function name of the translate function used to classify sequences with Metaphlan.
	 */
	protected static final String FUNCTION_RUN_METAPHLAN = "runMetaphlan";

	private static final String METAPHLAN_CLASS = "c";
	private static final String METAPHLAN_DOMAIN = "k";
	private static final String METAPHLAN_FAMILY = "f";
	private static final String METAPHLAN_GENUS = "g";
	private static final String METAPHLAN_ORDER = "o";
	private static final String METAPHLAN_PHYLUM = "p";
	private static final String METAPHLAN_SPECIES = "s";
	private static final String NUM_THREADS_PARAM = "--nproc";
}
