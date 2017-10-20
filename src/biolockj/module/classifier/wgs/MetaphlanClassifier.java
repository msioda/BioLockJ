/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
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
import java.util.List;
import java.util.Map;
import biolockj.Config;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.SeqUtil;

/**
 * This class builds the scripts used to call Metaphlan for classification of WGS data.
 */
public class MetaphlanClassifier extends ClassifierModuleImpl implements ClassifierModule
{
	/**
	 * Build scripts that use python to execute Metaphlan scripts.
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
			lines.add( pythonExe + " " + getClassifierExe() + switches + file.getAbsolutePath() + " --bowtie2out "
					+ bowtie2Out + " > " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Build scripts for paried reads that use python to execute Metaphlan scripts.
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
			lines.add( pythonExe + " " + getClassifierExe() + switches + file.getAbsolutePath() + ","
					+ map.get( file ).getAbsolutePath() + " --bowtie2out " + bowtie2Out + " > " + outputFile );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Verify python exe is found and only valid params are configured in prop file.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		pythonExe = Config.requireString( EXE_PYTHON );
		switches = getClassifierParams();

		if( switches.indexOf( "--input_type " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--input_type) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ derives this value by examinging one of the input files." );
		}
		if( switches.indexOf( "--nproc " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--nproc) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ derives this value from property: " + Config.SCRIPT_NUM_THREADS );
		}
		if( switches.indexOf( "--bowtie2out " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--bowtie2out) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ outputs bowtie2out files to MetaphlanClassifier/temp." );
		}
		if( switches.indexOf( "-t rel_ab_w_read_stats " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-t rel_ab_w_read_stats). BioLockJ hard codes this "
					+ "option for MetaPhlAn so must not be included in the property file." );
		}
		if( switches.indexOf( "--tax_lev " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (--tax_lev) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ sets this value based on: " + Config.REPORT_TAXONOMY_LEVELS );
		}
		if( switches.indexOf( "-s " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-s) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). SAM output not supported.  BioLockJ outputs .tsv files." );
		}
		if( switches.indexOf( "-o " ) > -1 )
		{
			throw new Exception( "Invalid classifier option (-o) found in property(" + EXE_CLASSIFIER_PARAMS
					+ "). BioLockJ outputs results to: " + getOutputDir().getAbsolutePath() + File.separator );
		}

		setRankSwitch();
		addHardCodedSwitches();

	}

	/**
	 * All Metaphlan queries will set --input_type, --nproc, and -t
	 * @return
	 * @throws Exception
	 */
	protected Map<String, String> getMetaphlanHardCodedSwitches() throws Exception
	{
		final Map<String, String> metaphlanSwitches = new HashMap<>();
		metaphlanSwitches.put( "--input_type", SeqUtil.getInputSequenceType() );
		metaphlanSwitches.put( "--nproc",
				new Integer( Config.requirePositiveInteger( Config.SCRIPT_NUM_THREADS ) ).toString() );
		metaphlanSwitches.put( "-t", "rel_ab_w_read_stats" );
		return metaphlanSwitches;
	}

	/**
	 * Add getMetaphlanHardCodedSwitches() to switches value.
	 * @throws Exception
	 */
	private void addHardCodedSwitches() throws Exception
	{
		final Map<String, String> hardCodedSwitches = getMetaphlanHardCodedSwitches();
		for( final String key: hardCodedSwitches.keySet() )
		{
			String val = hardCodedSwitches.get( key ).trim();
			if( val.length() > 0 )
			{
				val = " " + val;
			}
			switches += key + val + " ";
		}
	}

	/**
	 * Set the rankSwitch based on the configured Config.getTaxonomy() in the prop file if only one
	 * taxonomy level is to be reported.
	 * @throws Exception
	 */
	private void setRankSwitch() throws Exception
	{
		if( Config.requireTaxonomy().size() == 1 )
		{
			switches += "--tax_lev " + taxaLevelMap.get( Config.requireTaxonomy().iterator().next() ) + " ";
		}
	}

	private String pythonExe = null;
	private String switches = null;
	private final Map<String, String> taxaLevelMap = new HashMap<>();
	{
		taxaLevelMap.put( Config.SPECIES, METAPHLAN_SPECIES );
		taxaLevelMap.put( Config.GENUS, METAPHLAN_GENUS );
		taxaLevelMap.put( Config.FAMILY, METAPHLAN_FAMILY );
		taxaLevelMap.put( Config.ORDER, METAPHLAN_ORDER );
		taxaLevelMap.put( Config.CLASS, METAPHLAN_CLASS );
		taxaLevelMap.put( Config.PHYLUM, METAPHLAN_PHYLUM );
		taxaLevelMap.put( Config.DOMAIN, METAPHLAN_DOMAIN );
	}

	private static final String bowtie2ext = ".bowtie2.bz2";
	private static final String EXE_PYTHON = "exe.python";
	private static final String METAPHLAN_CLASS = "c";
	private static final String METAPHLAN_DOMAIN = "k";
	private static final String METAPHLAN_FAMILY = "f";
	private static final String METAPHLAN_GENUS = "g";
	private static final String METAPHLAN_ORDER = "o";
	private static final String METAPHLAN_PHYLUM = "p";
	private static final String METAPHLAN_SPECIES = "s";
}