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
package biolockj.module.classifier;

import java.io.File;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Job;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.BioModuleImpl;
import biolockj.util.BashScriptBuilder;
import biolockj.util.SeqUtil;

/**
 * This is the Classifier superclass used by all WGS & 16S classifiers.
 */
public abstract class ClassifierModuleImpl extends BioModuleImpl implements ClassifierModule
{
	@Override
	public abstract List<List<String>> buildScript( final List<File> files ) throws Exception;

	@Override
	public abstract List<List<String>> buildScriptForPairedReads( final List<File> files ) throws Exception;

	/**
	 * Check dependencies as we read in generic classifier props.
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		classifierExe = getClassifierExe();
		classifierParams = getClassifierParams();
		SeqUtil.getInputSequenceType();
		logVersion();
	}

	/**
	 * Default classifier execution registers numReads for each sample (if report.numReads=Y)
	 * Then call abstract method for paired or single reads based on paried read prop.
	 * Finally Bash Scripts must be built for execution.
	 */
	@Override
	public void executeProjectFile() throws Exception
	{
		final List<File> files = getInputFiles();
		SeqUtil.registerNumReadsPerSample( files, getOutputDir() );
		final List<List<String>> data = Config.getBoolean( Config.INPUT_PAIRED_READS )
				? buildScriptForPairedReads( files ): buildScript( files );
		BashScriptBuilder.buildScripts( this, data, files, Config.requirePositiveInteger( Config.SCRIPT_BATCH_SIZE ) );
	}

	@Override
	public String getClassifierExe() throws Exception
	{

		if( ( classifierExe == null ) || classifierExe.isEmpty() )
		{
			classifierExe = Config.getString( EXE_CLASSIFIER );
			if( ( classifierExe == null ) || classifierExe.isEmpty() )
			{
				final String defaultProp = getClassifierType() + "." + EXE_CLASSIFIER.substring( 4 );
				classifierExe = Config.getString( defaultProp );
				if( ( classifierExe == null ) || classifierExe.isEmpty() )
				{
					Config.throwPropNotFoundException( EXE_CLASSIFIER );
				}
				Log.out.info( "Loading default classifier property: " + defaultProp );
			}

			if( !BashScriptBuilder.clusterModuleExists( classifierExe ) )
			{
				final File f = new File( classifierExe );
				if( !f.exists() )
				{
					Config.requireExistingFile( EXE_CLASSIFIER );
				}
			}
		}
		return classifierExe;
	}

	/**
	 * Get the basic classifier switches from the prop file.
	 * @return
	 * @throws Exception
	 */
	@Override
	public String getClassifierParams() throws Exception
	{
		if( ( classifierParams == null ) || classifierParams.isEmpty() )
		{
			classifierParams = " ";
			List<String> paramList = Config.getList( EXE_CLASSIFIER_PARAMS );
			if( ( paramList == null ) || paramList.isEmpty() )
			{
				final String defaultProp = getClassifierType() + "." + EXE_CLASSIFIER_PARAMS.substring( 4 );
				paramList = Config.getList( defaultProp );
				if( ( paramList != null ) && !paramList.isEmpty() )
				{
					Log.out.info( "Loading default classifier property: " + defaultProp );
				}
			}

			if( ( paramList != null ) && !paramList.isEmpty() )
			{
				final Iterator<String> it = paramList.iterator();
				while( it.hasNext() )
				{
					classifierParams += "--" + it.next() + " ";
				}
			}
		}

		return classifierParams;
	}

	/**
	 * Summary message.
	 */
	@Override
	public String getSummary()
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			final int numIn = getInputFiles().size();
			BigInteger inAvg = BigInteger.valueOf( 0L );
			for( final File f: getInputFiles() )
			{
				final BigInteger size = FileUtils.sizeOfAsBigInteger( f );
				inAvg = inAvg.add( size );

			}
			inAvg = inAvg.divide( BigInteger.valueOf( numIn ) );

			sb.append( "Input " + numIn + " " + SeqUtil.getInputSequenceType() + " files" + BioLockJ.RETURN );
			sb.append( "Mean output file size: " + FileUtils.byteCountToDisplaySize( inAvg ) + RETURN );
			return sb.toString() + super.getSummary();

		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to produce module summary for: " + getClass().getName() + " : " + ex.getMessage(),
					ex );
		}

		return super.getSummary();
	}

	/**
	 * Log the version info to the output file.
	 */
	protected void logVersion() throws Exception
	{
		logVersion( getClassifierExe(), "--version" );
	}

	private String getClassifierType() throws Exception
	{
		for( final BioModule bioModule: Config.getModules() )
		{
			if( bioModule.getClass().getSimpleName().toUpperCase().contains( RDP ) )
			{
				return RDP.toLowerCase();
			}
			if( bioModule.getClass().getSimpleName().toUpperCase().contains( QIIME ) )
			{
				return QIIME.toLowerCase();
			}
			if( bioModule.getClass().getSimpleName().toUpperCase().contains( KRAKEN ) )
			{
				return KRAKEN.toLowerCase();
			}
			if( bioModule.getClass().getSimpleName().toUpperCase().contains( METAPHLAN ) )
			{
				return METAPHLAN.toLowerCase();
			}
			if( bioModule.getClass().getSimpleName().toUpperCase().contains( SLIMM ) )
			{
				return SLIMM.toLowerCase();
			}
		}

		throw new Exception( "Classifier type undefined!  No configured module includes a classifier keyword [ " + RDP
				+ ", " + QIIME + ", " + KRAKEN + ", " + SLIMM + ", " + METAPHLAN + "]" );
	}

	/**
	 * Another method to output version to handle cases with a unique version switch param.
	 * @param programExe
	 * @param versionSwitch
	 */
	private void logVersion( final String programExe, final String versionSwitch )
	{
		try
		{
			final String[] cmd = new String[ 2 ];
			cmd[ 0 ] = programExe;
			cmd[ 1 ] = versionSwitch;
			Job.submit( cmd );
		}
		catch( final Exception ex )
		{
			Log.out.error( "Version not found: " + programExe + " " + versionSwitch, ex );
		}
	}

	private String classifierExe = null;
	private String classifierParams = null;
	private static final String KRAKEN = "KRAKEN";
	private static final String METAPHLAN = "METAPHLAN";
	private static final String QIIME = "QIIME";
	private static final String RDP = "RDP";
	private static final String SLIMM = "SLIMM";

}
