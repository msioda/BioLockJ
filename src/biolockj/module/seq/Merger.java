/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
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
package biolockj.module.seq;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.BioModuleImpl;
import biolockj.util.BashScriptBuilder;
import biolockj.util.SeqUtil;

/**
 * This class will merge forward & reverse fastQ files.
 */
public class Merger extends BioModuleImpl implements BioModule
{
	/**
	 * Verify pear props are valid and inputType is fastQ.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		final String pear = Config.requireString( EXE_PEAR );

		if( !SeqUtil.isFastQ() )
		{
			throw new Exception( "PAIRED READS CAN ONLY BE ASSEMBLED WITH <FASTQ> FILE INPUT" );
		}

		if( !Config.getBoolean( Config.INPUT_PAIRED_READS ) )
		{
			throw new Exception( getClass().getName() + " requires " + Config.INPUT_PAIRED_READS + "=" + Config.TRUE );
		}

		if( !BashScriptBuilder.clusterModuleExists( pear ) )
		{
			Config.requireExistingFile( EXE_PEAR );
		}
	}

	/**
	 * Create lines for the bash scripts.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		BashScriptBuilder.buildScripts( this, getMergeLines( getInputFiles() ), fwReads,
				Config.requirePositiveInteger( Config.SCRIPT_BATCH_SIZE ) );
	}

	/**
	 * Get merge file lines for the bash script.
	 * @param files
	 * @return
	 * @throws Exception
	 */
	private List<List<String>> getMergeLines( final List<File> files ) throws Exception
	{
		Log.out.info( "Generating merge files from " + ( ( files == null ) ? 0: files.size() ) + " total files." );

		final List<List<String>> data = new ArrayList<>();
		final Map<File, File> map = SeqUtil.getPairedReads( files );
		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		final Set<File> keys = new TreeSet<>( map.keySet() );
		final String params = getPearSwitches( Config.getList( EXE_PEAR_PARAMS ) );

		for( final File file: keys )
		{
			final List<String> lines = new ArrayList<>();
			fwReads.add( file );
			final String sampleId = SeqUtil.getSampleId( file.getName() );
			lines.add( Config.requireString( EXE_PEAR ) + " -f " + file.getAbsolutePath() + " -r "
					+ map.get( file ).getAbsolutePath() + " -o " + tempDir + sampleId + params );
			lines.add( "mv " + tempDir + sampleId + ".assembled." + SeqUtil.FASTQ + " " + outDir + sampleId + "."
					+ SeqUtil.FASTQ );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Get formatted pear switches as provided in prop file (if any).
	 * @param switches
	 * @return
	 * @throws Exception
	 */
	private String getPearSwitches( final List<String> switches ) throws Exception
	{
		String formattedSwitches = " ";
		for( final String string: switches )
		{
			formattedSwitches += "-" + string + " ";
		}

		return formattedSwitches;
	}

	private final List<File> fwReads = new ArrayList<>();
	private static final String EXE_PEAR = "exe.pear";
	private static final String EXE_PEAR_PARAMS = "exe.pearParams";

}
