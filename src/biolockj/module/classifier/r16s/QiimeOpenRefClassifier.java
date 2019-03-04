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
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.util.MetaUtil;

/**
 * This module runs the QIIME pick_open_reference_otus.py script on FastA sequence files in a single script so it is
 * important to allocate sufficient job resources if running in a clustered environment.
 * 
 * @blj.web_desc QIIME Open Reference Classifier
 */
public class QiimeOpenRefClassifier extends QiimeClassifier implements ClassifierModule
{
	/**
	 * Return bash script lines to pick open reference OTUs by passing the newest biom file in the temp directory [via
	 * bash code "$(ls -t $tempDir/*.biom | head -1)"] to {@link biolockj.module.implicit.qiime.QiimeClassifier}
	 * getPickOtuLines() method.
	 * <p>
	 * Several biom files are generated by this script, the script will select biomFile = <br>
	 * If property {@value biolockj.module.implicit.qiime.QiimeClassifier#QIIME_REMOVE_CHIMERAS} =
	 * {@value biolockj.Constants#TRUE}, use vsearch to identify chimeras and call
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#SCRIPT_FILTER_OTUS} to remove them from
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#OTU_TABLE}
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final List<String> lines = new ArrayList<>();

		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final String outputDir = getOutputDir().getAbsolutePath() + File.separator;
		final String biomFile = "$(ls -t " + tempDir + "*.biom | head -1)";

		lines.addAll( getPickOtuLines( PICK_OTU_SCRIPT, getInputFileDir(), MetaUtil.getPath(), getTempDir() ) );

		if( Config.getBoolean( this, QIIME_REMOVE_CHIMERAS ) )
		{
			final String otusToFilter = tempDir + "chimeras.fasta";
			lines.add( Config.getExe( this, EXE_VSEARCH ) + getVsearchParams() + "--uchime_ref " + tempDir + REP_SET
					+ ".fna" + " --chimeras " + otusToFilter + " --nonchimeras " + tempDir + "nochimeras.fasta" );
			lines.add(
					SCRIPT_FILTER_OTUS + " -i " + biomFile + " -e " + otusToFilter + " -o " + outputDir + OTU_TABLE );
		}
		else
		{
			lines.add( "cp " + biomFile + " " + outputDir + OTU_TABLE );
		}

		data.add( lines );
		return data;
	}

	/**
	 * Call {@link biolockj.module.implicit.qiime.QiimeClassifier} checkOtuPickingDependencies() method to verify OTU
	 * picking script parameters. If not in Docker mode and property
	 * {@value biolockj.module.implicit.qiime.QiimeClassifier#QIIME_REMOVE_CHIMERAS} = {@value biolockj.Constants#TRUE},
	 * verify {@value biolockj.module.implicit.qiime.QiimeClassifier#EXE_VSEARCH_PARAMS}.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		getParams();

		if( Config.getBoolean( this, QIIME_REMOVE_CHIMERAS ) )
		{
			getVsearchParams();
		}
	}

	/**
	 * Open reference OTU picking script: {@value #PICK_OTU_SCRIPT}
	 */
	public static final String PICK_OTU_SCRIPT = "pick_open_reference_otus.py";

}
