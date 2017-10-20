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
package biolockj.module.classifier.r16s.qiime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.r16s.QiimeClassifier;
import biolockj.util.BashScriptBuilder;
import biolockj.util.MetaUtil;

/**
 * Used de novo OTU picking in which all samples must be processed
 * as a single batch.
 */
public class DeNovoClassifier extends QiimeClassifier implements ClassifierModule
{
	/**
	 * Pick OTUs with de novo QIIME script and filter chimeras if qiime.removeChimeras=Y
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final List<String> lines = new ArrayList<>();
		final String inDir = getInputDir().getAbsolutePath();
		final String outputDir = getOutputDir().getAbsolutePath() + File.separator;

		addPickOtuLines( lines, inDir, MetaUtil.getFile().getAbsolutePath(), getOutputDir().getAbsolutePath() );

		if( removeChimeras )
		{
			final String otusToFilter = outputDir + "chimeras.fasta";
			final String line1 = vsearch + vsearchParams + "--uchime_ref " + outputDir + repSet + " --chimeras "
					+ otusToFilter + " --nonchimeras " + outputDir + "nochimeras.fasta";

			final String line2 = SCRIPT_FILTER_OTUS + outputDir + OTU_TABLE + " -e " + otusToFilter + " -o " + outputDir
					+ OTU_TABLE;

			lines.add( line1 );
			lines.add( line2 );
		}

		data.add( lines );
		return data;
	}

	/**
	 * If chimeras must be removed, verify vsearch params.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		checkOtuPickingDependencies();
		removeChimeras = Config.getBoolean( QIIME_REMOVE_CHIMERAS );
		if( removeChimeras )
		{
			vsearch = Config.requireString( EXE_VSEARCH );
			vsearchParams = getVsearchParams();
			if( !BashScriptBuilder.clusterModuleExists( vsearch ) )
			{
				Config.requireExistingFile( EXE_VSEARCH );
			}
		}
	}

	/**
	 * Call build scripts.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		BashScriptBuilder.buildScripts( this, buildScript( getInputFiles() ), "QIIME.DeNovoClassifier" );
	}

	@Override
	protected String pickOtuScript() throws Exception
	{
		return PICK_OTU_SCRIPT;
	}

	/**
	 * Get Vsearch params from the prop file and format switches for the bash script.
	 * @return
	 * @throws Exception
	 */
	private String getVsearchParams() throws Exception
	{
		String formattedSwitches = " ";
		final List<String> params = Config.requireList( EXE_VSEARCH_PARAMS );
		for( final String string: params )
		{
			formattedSwitches += "--" + string + " ";
		}

		return formattedSwitches;
	}

	private boolean removeChimeras = false;
	private String vsearch = null;
	private String vsearchParams = null;
	protected static final String PICK_OTU_SCRIPT = "pick_de_novo_otus.py";
	protected static final String repSet = REP_SET + File.separator + "*.fasta";
}