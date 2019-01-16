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
import biolockj.Log;
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
	public void checkDependencies() throws Exception
	{
		getKneadDataSwitches();
	}
	
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File seqFile: files )
		{
			final ArrayList<String> lines = new ArrayList<>();
			final String fileId = SeqUtil.getSampleId( seqFile.getName() );
			final String dirExt = SeqUtil.getReadDirectionSuffix( seqFile );
			lines.add( sanatize( seqFile ) );
			lines.add( copyToOutputDir( seqFile ) );
			
			data.add( lines );
		}

		return data;
	}

	/**
	 * This method generates the worker script function: {@value #FUNCTION_SANATIZE}.
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_SANATIZE + "() {" );
		lines.add( Config.getExe( EXE_KNEADDATA ) + " " + getKneadDataSwitches() + INPUT_PARAM + " $1 " 
				+ OUTPUT_PARAM + " " + getTempDir().getAbsolutePath() );
		lines.add( "}" );
		return lines;
	}
	
	/**
	 * Get formatted KneadData switches if provided in {@link biolockj.Config}
	 * properties: {@value #EXE_KNEADDATA_PARAMS} and {@value #SCRIPT_NUM_THREADS}.
	 *
	 * @return Formatted KneadData switches
	 * @throws Exception if errors occur
	 */
	private String getKneadDataSwitches() throws Exception
	{
		String formattedSwitches = "-t " + Config.requirePositiveInteger( SCRIPT_NUM_THREADS ) + " ";
		for( final String string: Config.getList( EXE_KNEADDATA_PARAMS ) )
		{
			formattedSwitches += "-" + string + " ";
		}

		return formattedSwitches;
	}

	private String sanatize( final File seqFile ) throws Exception
	{
		return FUNCTION_SANATIZE + " " + seqFile.getAbsolutePath();
	}
	
	private String copyToOutputDir( final File seqFile ) throws Exception
	{
		return "cp " + getSanatizedFileName( seqFile ) + " " + getOutputDir().getAbsolutePath();
	}
	
	private String getSanatizedFileName( final File seqFile ) throws Exception
	{
		return seqFile.getName();
	}

	/**
	 * Name of the bash function used to decompress gzipped files: {@value #FUNCTION_SANATIZE}
	 */
	protected static final String FUNCTION_SANATIZE = "sanatizeData";
	
	/**
	 * KneadData executable: {@value #EXE_KNEADDATA}
	 */
	protected static final String EXE_KNEADDATA = "exe.kneaddata";
	
	/**
	 * {@link biolockj.Config} property containing parameters for {@value #EXE_KNEADDATA}: {@value #EXE_KNEADDATA_PARAMS}
	 */
	protected static final String EXE_KNEADDATA_PARAMS = "exe.kneaddataParams";
	
	private static final String INPUT_PARAM = "--input";
	private static final String OUTPUT_PARAM = "-o";

}
