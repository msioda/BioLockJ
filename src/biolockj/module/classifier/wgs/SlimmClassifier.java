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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import biolockj.Config;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.module.report.JsonReport;
import biolockj.util.ModuleUtil;
import biolockj.util.RuntimeParamUtil;
import biolockj.util.SeqUtil;

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
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String alignFile = getTempDir().getAbsolutePath() + File.separator + fileId + ".bam";
			final ArrayList<String> lines = new ArrayList<>( 2 );
			lines.add( FUNCTION_ALIGN + " " + file.getAbsolutePath() + " " + getTempDir().getAbsolutePath()
					+ File.separator + fileId + "_alignmentReport.txt " + alignFile );
			lines.add( getClassifierExe() + slimmSwitches + "-m " + getDB() + " -o " + getOutputDir().getAbsolutePath()
					+ File.separator + " " + alignFile );
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
			final String fileId = SeqUtil.getSampleId( file.getName() );
			final String alignFile = getTempDir().getAbsolutePath() + File.separator + fileId + ".bam";
			final ArrayList<String> lines = new ArrayList<>( 2 );
			lines.add( FUNCTION_ALIGN + " " + file.getAbsolutePath() + " " + map.get( file ).getAbsolutePath() + " "
					+ getTempDir().getAbsolutePath() + File.separator + fileId + "_alignmentReport.txt " + alignFile );
			lines.add( getClassifierExe() + slimmSwitches + "-m " + getDB() + " -o " + getOutputDir().getAbsolutePath()
					+ File.separator + " " + alignFile );
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
		getDB();
		getRefGenomeIndex();
		registerDefaultSwitches();
		formatDerivedBowtieSwitches();

		if( ModuleUtil.getModule( JsonReport.class.getName() ) != null )
		{
			throw new Exception( "SLIMM does not return OTU tree so cannot run the JsonReport module." );
		}

	}

	@Override
	public String getClassifierParams() throws Exception
	{
		final List<String> singleDashParamList = new ArrayList<>();
		for( final String x: singleDashParams )
		{
			singleDashParamList.add( x );
		}

		String formattedSwitches = " ";
		final List<String> switches = Config.getList( EXE_CLASSIFIER_PARAMS );
		final Iterator<String> it = switches.iterator();
		while( it.hasNext() )
		{
			final String token = it.next();
			final StringTokenizer sToken = new StringTokenizer( token, " " );
			if( singleDashParamList.contains( sToken.nextToken() ) )
			{
				formattedSwitches += "-" + token + " ";
			}
			else
			{
				formattedSwitches += "--" + token + " ";
			}
		}

		return formattedSwitches;
	}

	/**
	 * This method generates the required bash function: {@value #FUNCTION_ALIGN}
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();

		final String inputs = Config.getBoolean( Config.INTERNAL_PAIRED_READS ) ? " -1 $1 -2 $2": " -U $1";
		int index = Config.getBoolean( Config.INTERNAL_PAIRED_READS ) ? 3: 2;

		lines.add( "function " + FUNCTION_ALIGN + "() {" );
		lines.add( Config.getExe( EXE_BOWTIE2 ) + getRuntimeBowtieParams() + "-x " + getRefGenomeIndex() + inputs
				+ " 2> " + "$" + index++ + " | " + Config.getExe( EXE_SAMTOOLS ) + " view -bS -> $" + index );
		lines.add( "}" );
		return lines;
	}

	/**
	 * Format derived switches for call to Bowtie.
	 */
	private String formatDerivedBowtieSwitches() throws Exception
	{
		String switches = "";
		final Map<String, String> hardCodedSwitches = getDerivedBowtieSwitches();
		for( final String key: hardCodedSwitches.keySet() )
		{
			String val = hardCodedSwitches.get( key ).trim();
			if( val.length() > 0 )
			{
				val = " " + val;
			}
			switches += key + val + " ";
		}

		return switches;
	}

	private String getDB() throws Exception
	{
		if( RuntimeParamUtil.isDockerMode() )
		{
			return Config.requireString( DATABASE );
		}

		return Config.requireExistingDir( DATABASE ).getAbsolutePath();
	}

	/**
	 * Get derived switches for call to Bowtie.
	 */
	private Map<String, String> getDerivedBowtieSwitches() throws Exception
	{
		final Map<String, String> bowtieSwitches = new HashMap<>();
		bowtieSwitches.put( getInputTypeSwitch(), "" );
		bowtieSwitches.put( "-p", getNumThreads().toString() );
		bowtieSwitches.put( "--mm", "" );
		return bowtieSwitches;
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

	private String getRefGenomeIndex() throws Exception
	{
		return Config.requireString( REF_GENOME_INDEX );
	}

	private String getRuntimeBowtieParams() throws Exception
	{
		String switches = bowtieSwitches;
		if( !switches.endsWith( " " ) )
		{
			switches += " ";
		}

		return switches + formatDerivedBowtieSwitches();
	}

	private void registerDefaultSwitches() throws Exception
	{
		setDefaultBowtieSwitches();
		slimmSwitches = getClassifierParams();

		if( bowtieSwitches.indexOf( "--mm " ) > -1 )
		{
			throw new Exception( "Invalid Bowtie2 option (--mm) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ hard codes this this value. " );
		}
		if( bowtieSwitches.indexOf( "-p " ) > -1 || bowtieSwitches.indexOf( "--threads " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-p or --threads) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ sets these values based on: " + getNumThreadsParam() );
		}
		if( bowtieSwitches.indexOf( "-q " ) > -1 || bowtieSwitches.indexOf( "-f " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-q or -f) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ derives this value by examinging one of the input files." );
		}
		if( bowtieSwitches.indexOf( "-1 " ) > -1 || slimmSwitches.indexOf( "-2 " ) > -1
				|| slimmSwitches.indexOf( "-U " ) > -1 )
		{
			throw new Exception( "Invalid Bowtie2 option (-1 or -2 or -U) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ sets these values based on: " + Config.INPUT_DIRS );
		}
		if( slimmSwitches.indexOf( "-o " ) > -1 )
		{
			throw new Exception( "Invalid SLIMM option (-o) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ hard codes this value to: " + getOutputDir().getAbsolutePath() + File.separator );
		}
		if( slimmSwitches.indexOf( "-m " ) > -1 )
		{
			throw new Exception( "Invalid SLIMM option (-m) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ sets these values based on: " + DATABASE );
		}
		if( slimmSwitches.indexOf( "-d " ) > -1 )
		{
			throw new Exception( "Invalid SLIMM option (-d) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ sends individual input files to SLIMM from: " + getTempDir().getAbsolutePath() );
		}
		if( slimmSwitches.indexOf( "-r " ) > -1 )
		{
			throw new Exception( "Invalid SLIMM option (-r) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ sets this value based on: " + Config.REPORT_TAXONOMY_LEVELS );
		}

		setSlimmRankSwitch();
		formatDerivedBowtieSwitches();
	}

	/**
	 * Add user configured switches for call to Bowtie.
	 */
	private void setDefaultBowtieSwitches() throws Exception
	{
		bowtieSwitches = " ";
		final List<String> switches = Config.getList( EXE_BOWTIE_PARAMS );
		for( final String next: switches )
		{
			bowtieSwitches += "--" + next + " ";
		}
	}

	/**
	 * Add single rank switch if only one taxonomy level configured in prop file.
	 */
	private void setSlimmRankSwitch() throws Exception
	{
		if( Config.getList( Config.REPORT_TAXONOMY_LEVELS ).size() == 1 )
		{
			slimmSwitches += "-r " + taxaLevelMap.get( Config.getList( Config.REPORT_TAXONOMY_LEVELS ).get( 0 ) ) + " ";
		}
	}

	private String bowtieSwitches = null;
	private String slimmSwitches = null;
	private final Map<String, String> taxaLevelMap = new HashMap<>();
	{
		taxaLevelMap.put( Config.SPECIES, Config.SPECIES );
		taxaLevelMap.put( Config.GENUS, Config.GENUS );
		taxaLevelMap.put( Config.FAMILY, Config.FAMILY );
		taxaLevelMap.put( Config.ORDER, Config.ORDER );
		taxaLevelMap.put( Config.CLASS, Config.CLASS );
		taxaLevelMap.put( Config.PHYLUM, Config.PHYLUM );
		taxaLevelMap.put( Config.DOMAIN, SLIMM_DOMAIN_DELIM );
	}
	/**
	 * Override {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}.{@value biolockj.Config#DOMAIN}
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
	 * Function name used to classify sequences with SLIMM: {@value #FUNCTION_ALIGN}
	 */
	protected static final String FUNCTION_ALIGN = "align";

	/**
	 * {@link biolockj.Config} property to set SLIMM bowtie2 large reference index {@value #REF_GENOME_INDEX}
	 */
	protected static final String REF_GENOME_INDEX = "slimm.refGenomeIndex";

	private static final String[] singleDashParams = { "r", "c", "s", "u", "5", "3", "N", "L", "i", "k", "a", "D", "R",
			"I", "X", "t", "" };

}
