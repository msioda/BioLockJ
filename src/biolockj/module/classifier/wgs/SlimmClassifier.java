/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Apr 5, 2017
 * @disclaimer 	This code is free software; you can redistribute it and/or
 * 				modify it under the terms of the GNU General Public License
 * 				as published by the Free Software Foundation; either version 2
 * 				of the License, or (at your option) any later version,
 * 				provided that any use properly credits the author.
 * 				This program is distributed in the hope that it will be useful,
 * 				but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * 				GNU General Public License for more details at http://www.gnu.org *
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
import biolockj.util.SeqUtil;

/**
 * This class builds the scripts used to call SLIMM for classification of WGS data.
 */
public class SlimmClassifier extends ClassifierModuleImpl implements ClassifierModule
{
	/**
	 * Build the lines to use in bash script, first calling Bowtie & then Slimm.
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

			lines.add( bowtieExe + bowtieSwitches + "-x " + slimmRefGenomeIndex + " -U " + file.getAbsolutePath() + " "
					+ " 2> " + getTempDir().getAbsolutePath() + File.separator + fileId + "_alignmentReport.txt | "
					+ samToolsExe + " view -bS -> " + alignFile );

			lines.add( getClassifierExe() + slimmSwitches + "-m " + slimmDB.getAbsolutePath() + " -o "
					+ getOutputDir().getAbsolutePath() + File.separator + " " + alignFile );

			data.add( lines );
		}

		return data;
	}

	/**
	 * Build the lines to use in bash script for paired reads, first calling Bowtie & then Slimm.
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

			lines.add( bowtieExe + bowtieSwitches + "-x " + slimmRefGenomeIndex + " -1 " + file.getAbsolutePath()
					+ " -2 " + map.get( file ).getAbsolutePath() + " 2> " + getTempDir().getAbsolutePath()
					+ File.separator + fileId + "_alignmentReport.txt | " + samToolsExe + " view -bS -> " + alignFile );

			lines.add( getClassifierExe() + slimmSwitches + "-m " + slimmDB.getAbsolutePath() + " -o "
					+ getOutputDir().getAbsolutePath() + File.separator + " " + alignFile );

			data.add( lines );
		}

		return data;
	}

	/**
	 * Check that all SLIMM specific params are set and the bowtie params are valid.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();

		slimmDB = Config.requireExistingDir( DATABASE );
		slimmRefGenomeIndex = Config.requireString( REF_GENOME_INDEX );
		samToolsExe = Config.requireString( EXE_SAMTOOLS );
		bowtieExe = Config.requireString( EXE_BOWTIE );

		if( SeqUtil.isFastQ() )
		{
			inputTypeSwitch = "-q";
		}
		else
		{
			inputTypeSwitch = "-f";
		}

		setBowtieSwitches();
		slimmSwitches = getClassifierParams();

		if( bowtieSwitches.indexOf( "--mm " ) > -1 )
		{
			throw new Exception( "Invalid Bowtie2 option (--mm) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ hard codes this this value. " );
		}
		if( ( bowtieSwitches.indexOf( "-p " ) > -1 ) || ( bowtieSwitches.indexOf( "--threads " ) > -1 ) )
		{
			throw new Exception( "Invalid classifier option (-p or --threads) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ sets these values based on: " + Config.SCRIPT_NUM_THREADS );
		}
		if( ( bowtieSwitches.indexOf( "-q " ) > -1 ) || ( bowtieSwitches.indexOf( "-f " ) > -1 ) )
		{
			throw new Exception( "Invalid classifier option (-q or -f) found in property(" + EXE_BOWTIE_PARAMS
					+ "). BioLockJ derives this value by examinging one of the input files." );
		}
		if( ( bowtieSwitches.indexOf( "-1 " ) > -1 ) || ( slimmSwitches.indexOf( "-2 " ) > -1 )
				|| ( slimmSwitches.indexOf( "-U " ) > -1 ) )
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
		addHardCodedBowtieSwitches();
	}

	/**
	 * Set switches for calls to Slimm classifier.
	 */
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
	 * Add standard switches for call to Bowtie.
	 */
	private void addHardCodedBowtieSwitches() throws Exception
	{
		final Map<String, String> hardCodedSwitches = getHardCodedBowtieSwitches();
		for( final String key: hardCodedSwitches.keySet() )
		{
			String val = hardCodedSwitches.get( key ).trim();
			if( val.length() > 0 )
			{
				val = " " + val;
			}
			bowtieSwitches += key + val + " ";
		}
	}

	/**
	 * Format standard switches for call to Bowtie.
	 */
	private Map<String, String> getHardCodedBowtieSwitches() throws Exception
	{
		final Map<String, String> bowtieSwitches = new HashMap<>();
		bowtieSwitches.put( inputTypeSwitch, "" );
		bowtieSwitches.put( "-p",
				new Integer( Config.requirePositiveInteger( Config.SCRIPT_NUM_THREADS ) ).toString() );
		bowtieSwitches.put( "--mm", "" );
		return bowtieSwitches;
	}

	/**
	 * Add user configured switches for call to Bowtie.
	 */
	private void setBowtieSwitches() throws Exception
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
		if( Config.requireTaxonomy().size() == 1 )
		{
			slimmSwitches += "-r " + taxaLevelMap.get( Config.requireTaxonomy().iterator().next() ) + " ";
		}
	}

	private String bowtieExe = null;
	private String bowtieSwitches = null;
	private String inputTypeSwitch = null;
	private String samToolsExe = null;
	private File slimmDB = null;
	private String slimmRefGenomeIndex = null;
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

	public static final String SLIMM_DOMAIN_DELIM = "superkingdom";
	private static final String DATABASE = "slimm.db";
	private static final String EXE_BOWTIE = "exe.bowtie";
	private static final String EXE_BOWTIE_PARAMS = "exe.bowtieParams";
	private static final String EXE_SAMTOOLS = "exe.samtools";
	private static final String REF_GENOME_INDEX = "slimm.refGenomeIndex";
	private static final String[] singleDashParams = { "r", "c", "s", "u", "5", "3", "N", "L", "i", "k", "a", "D", "R",
			"I", "X", "t", "" };

}