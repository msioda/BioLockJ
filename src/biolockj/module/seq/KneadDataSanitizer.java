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
import java.util.*;
import biolockj.Config;
import biolockj.module.ScriptModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.util.SeqUtil;

/**
 * This BioModule runs biobakery kneaddata program to remove contaminated DNA.<br>
 * Multiple contaminent DNA databases can be used to filter reads simultaniously.<br>
 * Common contaminents include Human, Viral, and Plasmid DNA.<br>
 */
public class KneadDataSanitizer extends ScriptModuleImpl implements ScriptModule
{

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File seqFile: files )
		{
			if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) && !SeqUtil.isForwardRead( seqFile.getName() ) )
			{
				continue;
			}
			
			final ArrayList<String> lines = new ArrayList<>();
			
			if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
			{
				lines.add( sanatize( seqFile, SeqUtil.getPairedReads( files ).get( seqFile ) ) );
			}
			else 
			{
				lines.add( sanatize( seqFile, null ) );
			}

			lines.addAll( copyToOutputDir( SeqUtil.getSampleId( seqFile.getName() ) ) );

			data.add( lines );
		}

		return data;
	}

	@Override
	public void checkDependencies() throws Exception
	{
		getKneadDataSwitches();
	}

	/**
	 * This method generates the worker script function: {@value #FUNCTION_SANATIZE}.
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_SANATIZE + "() {" );
		lines.add( Config.getExe( EXE_KNEADDATA ) + " " + getKneadDataSwitches() + OUTPUT_FILE_PREFIX_PARAM + " $1 "
			+ INPUT_PARAM + " $2 " + ( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) ? INPUT_PARAM + " $3 " : "" ) 
				+ OUTPUT_PARAM + " " + getTempDir().getAbsolutePath() );
		lines.add( "}" );
		return lines;
	}

	private List<String> copyToOutputDir( final String sampleId ) throws Exception
	{
		List<String> lines = new ArrayList<>();
		String fileSuffix = "." + Config.requireString( SeqUtil.INTERNAL_SEQ_TYPE );
		if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			String fwSuffix = Config.requireString( Config.INPUT_FORWARD_READ_SUFFIX );
			String rvSuffix = Config.requireString( Config.INPUT_REVERSE_READ_SUFFIX );
			File fwOutFile = new File( getOutputDir().getAbsolutePath() + File.separator + sampleId + fwSuffix + fileSuffix );
			File rvOutFile = new File( getOutputDir().getAbsolutePath() + File.separator + sampleId + rvSuffix + fileSuffix );
			lines.add( "mv " + getSanatizedFile( sampleId, false ).getAbsolutePath() + " " + fwOutFile.getAbsolutePath() );
			lines.add( "mv " + getSanatizedFile( sampleId, true ).getAbsolutePath() + " " + rvOutFile.getAbsolutePath() );
		}
		else
		{
			File outFile = new File( getOutputDir().getAbsolutePath() + File.separator + sampleId + fileSuffix );
			lines.add( "mv " + getSanatizedFile( sampleId, null ).getAbsolutePath() + " " + outFile.getAbsolutePath() );
		}

		return lines;
	}
	
	
	/**
	 * Get formatted KneadData switches if provided in {@link biolockj.Config} properties:
	 * {@value #EXE_KNEADDATA_PARAMS} and {@value #SCRIPT_NUM_THREADS}.
	 *
	 * @return Formatted KneadData switches
	 * @throws Exception if errors occur
	 */
	private String getKneadDataSwitches() throws Exception
	{
		final List<String> switches = Config.getList( EXE_KNEADDATA_PARAMS );
		String formattedSwitches = "-t " +  getNumThreads() + " " + getDbParams();
		
		final List<String> singleDashParams = getSingleDashParams();
		
		final Iterator<String> it = switches.iterator();
		while( it.hasNext() )
		{
			final String param = it.next();
			final StringTokenizer sToken = new StringTokenizer( param, " " );
			if( singleDashParams.contains( sToken.nextToken() ) )
			{
				formattedSwitches += "-" + param + " ";
			}
			else
			{
				formattedSwitches += "--" + param + " ";
			}
		}

		return formattedSwitches;
	}
	
	private List<String> getSingleDashParams() throws Exception
	{
		final List<String> params = new ArrayList<>();
		for( final String param: singleDashParams )
		{
			params.add( param );
		}
		return params;
	}
	
	// TODO --> add comma separated list with no spaces
	private String getDbParams() throws Exception
	{
		String params = "";
		for( String db: Config.requireList( KNEAD_DBS ) )
		{
			params += DB_PARAM + " " + db + " ";
		}
		
		return params; 
	}

	private File getSanatizedFile( final String sampleId, Boolean isRvRead ) throws Exception
	{
		String suffix = KNEADDATA_SUFFIX;
		if( Config.getBoolean( Config.INTERNAL_PAIRED_READS ) )
		{
			suffix += isRvRead ? RV_OUTPUT_SUFFIX : FW_OUTPUT_SUFFIX;
		}
		 
		suffix += "." + Config.requireString( SeqUtil.INTERNAL_SEQ_TYPE );
		return new File( getTempDir().getAbsolutePath() + File.separator + sampleId + suffix );
	}

	private String sanatize( final File seqFile, final File rvRead ) throws Exception
	{
		return FUNCTION_SANATIZE + " " + SeqUtil.getSampleId( seqFile.getName() ) + " " + seqFile.getAbsolutePath()
			+ ( rvRead == null ? "" : " " + rvRead.getAbsolutePath());
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
	 * {@link biolockj.Config} required property to the contaminent databases {@value #KNEAD_DBS}:
	 */
	protected static final String KNEAD_DBS = "kneaddata.dbs";
	
	
	/**
	 * Name of the bash function used to decompress gzipped files: {@value #FUNCTION_SANATIZE}
	 */
	protected static final String FUNCTION_SANATIZE = "sanatizeData";

	private static final String FW_OUTPUT_SUFFIX = "_paired_1";
	private static final String RV_OUTPUT_SUFFIX = "_paired_2";
	private static final String KNEADDATA_SUFFIX = "_kneaddata";
	private static final String INPUT_PARAM = "-i";
	private static final String OUTPUT_PARAM = "-o";
	private static final String DB_PARAM = "-db";
	private static final String OUTPUT_FILE_PREFIX_PARAM = "--output-prefix";
	private static final String[] singleDashParams = { "p", "q", "v" };

}
