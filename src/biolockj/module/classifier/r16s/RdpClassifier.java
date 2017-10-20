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
package biolockj.module.classifier.r16s;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.classifier.ClassifierModule;
import biolockj.module.classifier.ClassifierModuleImpl;
import biolockj.util.SeqUtil;

/**
 * RdpClassifier is used to build RDP classifier bash scripts
 */
public class RdpClassifier extends ClassifierModuleImpl implements ClassifierModule
{
	/**
	 * Call RDP jar with specified params.
	 */
	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		for( final File file: files )
		{
			final String outputFile = getOutputDir().getAbsolutePath() + File.separator
					+ SeqUtil.getSampleId( file.getName() ) + PROCESSED;
			final ArrayList<String> lines = new ArrayList<>();
			lines.add( javaExe + " -jar " + getClassifierExe() + getClassifierParams() + "-o " + outputFile + " "
					+ file.getAbsolutePath() );
			data.add( lines );
		}

		return data;
	}

	/**
	 * Paired reads must use mergeUtil & then call standard buildScripts() method.
	 */
	@Override
	public List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception
	{
		return buildScript( files );
	}

	/**
	 * The only unique RDP dependency is on Java.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		super.checkDependencies();
		javaExe = Config.requireString( EXE_JAVA );
	}

	/**
	 * RDP does not supply a version call.
	 */
	@Override
	protected void logVersion() throws Exception
	{
		Log.out.warn( "Version unavailable for: " + getClassifierExe() );
	}

	private String javaExe;
	protected static final String EXE_JAVA = "exe.java";

}
