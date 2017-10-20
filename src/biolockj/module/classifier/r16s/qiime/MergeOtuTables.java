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
package biolockj.module.classifier.r16s.qiime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.r16s.QiimeClassifier;
import biolockj.util.BashScriptBuilder;

/**
 *
 */
public class MergeOtuTables extends QiimeClassifier implements ClassifierModule
{
	/**
	 * Input files are all of the otu_table.biom files output by the ClosedRefClassifier.
	 * The QiimeClassifer superclass will be called next & wil expect a single otu_table.biom file.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		String tables = "";
		for( final File f: files )
		{
			tables += ( tables.isEmpty() ? "": "," ) + f.getAbsolutePath();
		}

		final List<List<String>> data = new ArrayList<>();
		final List<String> lines = new ArrayList<>();
		final String outputFile = getOutputDir().getAbsolutePath() + File.separator + OTU_TABLE;
		lines.add( SCRIPT_MERGE_OTU_TABLES + tables + " -o " + outputFile );
		data.add( lines );

		return data;
	}

	/**
	 * Call build scripts to merge OTU tables via QIIME script: merge_otu_tables.py
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		BashScriptBuilder.buildScripts( this, buildScript( getInputFiles() ), SCRIPT_MERGE_OTU_TABLES );
	}

	private static final String SCRIPT_MERGE_OTU_TABLES = "merge_otu_tables.py -i ";
}
