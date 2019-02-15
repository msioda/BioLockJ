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
package biolockj.module.classifier.wgs;

import java.io.File;
import java.util.*;
import biolockj.Config;
import biolockj.Constants;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.module.report.JsonReport;
import biolockj.util.*;

/**
 * This BioModule builds the scripts used to call SLIMM for classification of WGS data.
 */
public class SlimmClassifier extends ClassifierModuleImpl implements ClassifierModule
{

	/**
	 * Build 2 bash script lines per sample to classify unpaired WGS reads.
	 * <p>
	 * Example lines:
	 * <ul>
	 * <li>bowtie2 --no-unal --k 60 -p 8 -q --mm -x /database/slimm/5K/AB_5K -U ./input/sample42.fq.gz 2&gt;
	 * ./temp/sample42_alignmentReport.txt | samtools view -bS -&gt; ./temp/sample42.bam
	 * <li>slimm -m /database/slimm/5K/slimmDB_5K -o ./output/ ./temp/sample42.bam
	 * </ul>
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			final String id = SeqUtil.getSampleId( file.getName() );
			final ArrayList<String> lines = new ArrayList<>( 2 );
			lines.add( FUNCTION_ALIGN + " " + file.getAbsolutePath() + " " + getAlignReport( id ) + " "
					+ getAlignFile( id ) );
			lines.add( getClassifierExe() + " " + getAlignFile( id ) );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Build 2 bash script lines per sample to classify paired WGS reads.
	 * <p>
	 * Example lines:
	 * <ul>
	 * <li>bowtie2 --no-unal --k 60 -p 8 -q --mm -x /database/slimm/5K/AB_5K -1 ./input/sample42_R1.fq.gz -2
	 * ./input/sample42_R2.fq.gz 2&gt; ./temp/sample42_alignmentReport.txt | samtools view -bS -&gt; ./temp/sample42.bam
	 * <li>slimm -m /database/slimm/5K/slimmDB_5K -o ./output/ ./temp/sample42.bam
	 * </ul>
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		for( final File file: map.keySet() )
		{
			final String id = SeqUtil.getSampleId( file.getName() );
			final ArrayList<String> lines = new ArrayList<>( 2 );
			lines.add( FUNCTION_ALIGN + " " + file.getAbsolutePath() + " " + map.get( file ).getAbsolutePath() + " "
					+ getAlignReport( id ) + " " + getAlignFile( id ) );
			lines.add( FUNCTION_SLIMM + " " + getAlignFile( id ) );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Verify {@link biolockj.Config} properties
	 * <ul>
	 * <li>Require property {@value #DATABASE} is a valid file path
	 * <li>Require property {@value #REF_GENOME_INDEX} is a valid directory path
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getRuntimeSlimmParams();
		getRuntimeBowtieParams();

		if( ModuleUtil.getModule( this, JsonReport.class.getName(), true ) != null )
		{
			throw new Exception( "SLIMM does not return OTU tree so cannot run the JsonReport module." );
		}
	}

	/**
	 * Call SLIMM executabl
	 */
	@Override
	public String getClassifierExe() throws Exception
	{
		return Config.getExe( this, EXE_SLIMM );
	}

	@Override
	public List<String> getClassifierParams() throws Exception
	{
		return Config.getList( this, getExeParamName() );
	}

	/**
	 * This method generates the required bash function: {@value #FUNCTION_ALIGN}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();

		final String inputs = Config.getBoolean( this, SeqUtil.INTERNAL_PAIRED_READS ) ? " -1 $1 -2 $2": " -U $1";
		int index = Config.getBoolean( this, SeqUtil.INTERNAL_PAIRED_READS ) ? 3: 2;

		lines.add( "function " + FUNCTION_ALIGN + "() {" );
		lines.add( Config.getExe( this, EXE_BOWTIE2 ) + " " + getRuntimeBowtieParams() + inputs + " 2> $" + index++
				+ " | " + Config.getExe( this, EXE_SAMTOOLS ) + " view -bS -> $" + index );
		lines.add( "}" + RETURN );
		lines.add( "function " + FUNCTION_SLIMM + "() {" );
		lines.add( getClassifierExe() + " " + getRuntimeSlimmParams() + SLIMM_OUTPUT_PARAM
				+ getOutputDir().getAbsolutePath() + File.separator + " $1" );
		lines.add( "}" + RETURN );

		return lines;
	}

	private String getAlignFile( final String id ) throws Exception
	{
		return getTempDir().getAbsolutePath() + File.separator + id + ".bam";
	}

	private String getAlignReport( final String id ) throws Exception
	{
		return getTempDir().getAbsolutePath() + File.separator + id + "_alignmentReport" + TXT_EXT;
	}

	private String getDB() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			return Config.requireString( this, DATABASE );
		}

		return Config.requireExistingDir( this, DATABASE ).getAbsolutePath();
	}

	private String getInputTypeSwitch() throws Exception
	{
		if( SeqUtil.isFastQ() )
		{
			return "-q";
		}
		else
		{
			return "-f";
		}
	}

	private String getRuntimeBowtieParams() throws Exception
	{
		String bowtieSwitches = " " + BioLockJUtil.join( Config.getList( this, EXE_BOWTIE_PARAMS ) );

		if( bowtieSwitches.indexOf( "--mm " ) > -1 )
		{
			throw new Exception( "Invalid Bowtie2 option (--mm) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ hard codes this this value. " );
		}
		if( bowtieSwitches.indexOf( "-p " ) > -1 || bowtieSwitches.indexOf( "--threads " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-p or --threads) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ sets these values based on: " + SCRIPT_NUM_THREADS );
		}
		if( bowtieSwitches.indexOf( "-q " ) > -1 || bowtieSwitches.indexOf( "-f " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-q or -f) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ derives this value by examinging one of the input files." );
		}
		if( bowtieSwitches.indexOf( "-1 " ) > -1 || bowtieSwitches.indexOf( "-2 " ) > -1
				|| bowtieSwitches.indexOf( "-U " ) > -1 )
		{
			throw new Exception( "Invalid Bowtie2 option (-1 or -2 or -U) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ sets these values based on: " + Constants.INPUT_DIRS );
		}

		bowtieSwitches = getRuntimeParams( Config.getList( this, EXE_BOWTIE_PARAMS ), BOWTIE_NUM_THREADS_PARAM ) + "-x "
				+ Config.requireString( this, REF_GENOME_INDEX ) + " --mm " + getInputTypeSwitch() + " ";

		return bowtieSwitches;
	}

	private String getRuntimeSlimmParams() throws Exception
	{
		String slimmSwitches = " " + BioLockJUtil.join( getClassifierParams() );
		if( slimmSwitches.indexOf( "-o " ) > -1 )
		{
			throw new Exception( "Invalid SLIMM option (-o) found in property(" + getExeParamName()
					+ "). BioLockJ hard codes this value to: " + getOutputDir().getAbsolutePath() + File.separator );
		}
		if( slimmSwitches.indexOf( "-m " ) > -1 )
		{
			throw new Exception( "Invalid SLIMM option (-m) found in property(" + getExeParamName()
					+ "). BioLockJ sets these values based on: " + DATABASE );
		}
		if( slimmSwitches.indexOf( "-d " ) > -1 )
		{
			throw new Exception( "Invalid SLIMM option (-d) found in property(" + getExeParamName()
					+ "). BioLockJ sends individual input files to SLIMM from: " + getTempDir().getAbsolutePath() );
		}
		if( slimmSwitches.indexOf( "-r " ) > -1 )
		{
			throw new Exception( "Invalid SLIMM option (-r) found in property(" + getExeParamName()
					+ "). BioLockJ sets this value based on: " + TaxaUtil.REPORT_TAXONOMY_LEVELS );
		}

		slimmSwitches = getRuntimeParams( getClassifierParams(), null ) + "-m " + getDB() + " ";
		if( TaxaUtil.getTaxaLevels().size() == 1 )
		{
			slimmSwitches += "-r " + taxaLevelMap.get( TaxaUtil.getTaxaLevels().get( 0 ) ) + " ";
		}

		return slimmSwitches;
	}
	
	private String getExeParamName()
	{
		return EXE_SLIMM + Constants.PARAMS;
	}

	private final Map<String, String> taxaLevelMap = new HashMap<>();
	{
		taxaLevelMap.put( TaxaUtil.SPECIES, TaxaUtil.SPECIES );
		taxaLevelMap.put( TaxaUtil.GENUS, TaxaUtil.GENUS );
		taxaLevelMap.put( TaxaUtil.FAMILY, TaxaUtil.FAMILY );
		taxaLevelMap.put( TaxaUtil.ORDER, TaxaUtil.ORDER );
		taxaLevelMap.put( TaxaUtil.CLASS, TaxaUtil.CLASS );
		taxaLevelMap.put( TaxaUtil.PHYLUM, TaxaUtil.PHYLUM );
		taxaLevelMap.put( TaxaUtil.DOMAIN, SLIMM_DOMAIN_DELIM );
	}
	/**
	 * Override
	 * {@link biolockj.Config}.{@value biolockj.util.TaxaUtil#REPORT_TAXONOMY_LEVELS}.{@value biolockj.util.TaxaUtil#DOMAIN}
	 * value
	 */
	public static final String SLIMM_DOMAIN_DELIM = "superkingdom";
	/**
	 * {@link biolockj.Config} property to directory holding SLIMM database: {@value #DATABASE}
	 */
	protected static final String DATABASE = "slimm.db";
	/**
	 * {@link biolockj.Config} property to set bowtie2 parameters: {@value #EXE_BOWTIE_PARAMS}
	 */
	protected static final String EXE_BOWTIE_PARAMS = "exe.bowtie2Params";

	/**
	 * {@link biolockj.Config} property to set bowtie2 executable: {@value #EXE_BOWTIE2}
	 */
	protected static final String EXE_BOWTIE2 = "exe.bowtie2";

	/**
	 * {@link biolockj.Config} property to set samtools executable: {@value #EXE_SAMTOOLS}
	 */
	protected static final String EXE_SAMTOOLS = "exe.samtools";

	/**
	 * {@link biolockj.Config} property to call slimm executable: {@value #EXE_SLIMM}
	 */
	protected static final String EXE_SLIMM = "exe.slimm";

	/**
	 * Function name used to align sequences with bowtie2: {@value #FUNCTION_ALIGN}
	 */
	protected static final String FUNCTION_ALIGN = "bowTieAlign";

	/**
	 * Function name used to run SLIMM: {@value #FUNCTION_SLIMM}
	 */
	protected static final String FUNCTION_SLIMM = "runSlimm";

	/**
	 * {@link biolockj.Config} property to set SLIMM bowtie2 large reference index {@value #REF_GENOME_INDEX}
	 */
	protected static final String REF_GENOME_INDEX = "slimm.refGenomeIndex";

	private static final String BOWTIE_NUM_THREADS_PARAM = "-p";
	private static final String SLIMM_OUTPUT_PARAM = "-o";
}
