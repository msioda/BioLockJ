/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.classifier;

import java.io.File;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.exception.ConfigFormatException;
import biolockj.exception.ConfigNotFoundException;
import biolockj.module.BioModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.util.BashScriptBuilder;
import biolockj.util.RuntimeParamUtil;

/**
 * This is the superclass for all WGS and 16S biolockj.module.classifier BioModules.
 */
public abstract class ClassifierModuleImpl extends ScriptModuleImpl implements ClassifierModule
{
	/**
	 * Validate module dependencies:
	 * <ul>
	 * <li>Call {@link #getClassifierExe()} to verify the executable
	 * <li>Call {@link #getClassifierParams()} to verify the runtime parameters are valid
	 * <li>Call {@link #validateModuleOrder()} to validate module configuration order.
	 * <li>Call {@link biolockj.module.ScriptModule#checkDependencies() } to validate script dependencies.
	 * </ul>
	 */
	@Override
	public void checkDependencies() throws Exception
	{
		classifierExe = getClassifierExe();
		classifierParams = getClassifierParams();
		getNumThreads();
		validateModuleOrder();
		super.checkDependencies();
	}

	/**
	 * Get the classifier executable command from the Config. Verify command is either loaded as a cluster module or is
	 * a valid file path. To simplify the Config, default values for exe.classifier can be defined by replacing "exe"
	 * with a classifier type (rdp, qiime, kraken, metaphlan, or slimm). For example, if exe.classifer is missing or
	 * blank and getClassifierType() returns "rdp" the property rdp.classifier will be used in place of exe.classifier.
	 * This allows all classifiers to be defined in a file such as standard.properties.
	 *
	 * @return Classifier command to use in bash scripts
	 * @throws Exception if the executable is undefined or invalid
	 */
	@Override
	public String getClassifierExe() throws Exception
	{
		if( classifierExe == null || classifierExe.isEmpty() )
		{
			classifierExe = Config.getString( EXE_CLASSIFIER );
			if( classifierExe == null || classifierExe.isEmpty() )
			{
				final String defaultProp = getClassifierType() + "." + EXE_CLASSIFIER.substring( 4 );

				classifierExe = Config.requireString( defaultProp );
				Log.info( getClass(), "Loading default classifier property: " + defaultProp + " = " + classifierExe );

				if( classifierExe == null || classifierExe.isEmpty() )
				{
					throw new ConfigNotFoundException( EXE_CLASSIFIER );
				}
			}

			if( !BashScriptBuilder.clusterModuleExists( classifierExe ) && !RuntimeParamUtil.isDockerMode() )
			{
				final File f = new File( Config.getSystemFilePath( classifierExe ) );
				if( !f.exists() )
				{
					Config.requireExistingFile( EXE_CLASSIFIER );
				}
			}
		}

		return classifierExe;
	}

	/**
	 * Get optional classifier switches by appending "--" to each value in the list parameter if defined in the Config
	 * file. Classifiers that use a single "-" must be override this method. To simplify the Config, default values for
	 * exe.classifier can be defined by replacing "exe" with a classifier type (rdp, qiime, kraken, metaphlan, or
	 * slimm). For example, if exe.classifer is missing or blank and getClassifierType() returns "rdp" the property
	 * rdp.classifier will be used in place of exe.classifier. This allows all classifiers to be defined in a file such
	 * as standard.properties.
	 *
	 * @return String containing formatted switches to append to the executable
	 * @throws Exception if defined but invalid
	 */
	@Override
	public String getClassifierParams() throws Exception
	{
		if( classifierParams == null || classifierParams.isEmpty() )
		{
			classifierParams = " ";
			List<String> paramList = Config.getList( EXE_CLASSIFIER_PARAMS );
			if( paramList == null || paramList.isEmpty() )
			{
				final String defaultProp = getClassifierType() + "." + EXE_CLASSIFIER_PARAMS.substring( 4 );
				paramList = Config.getList( defaultProp );
				if( paramList != null && !paramList.isEmpty() )
				{
					Log.info( getClass(), "Loading default classifier property: " + defaultProp );
				}
			}

			if( paramList != null && !paramList.isEmpty() )
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
	 * This method returns the corresponding Parser module associated with the classifier. The Parser module name is
	 * built using the same prefix used in the Classifier module
	 * <p>
	 * For example RdpClassifier, will return the RdpParser.
	 */
	@Override
	public List<Class<?>> getPostRequisiteModules() throws Exception
	{
		final List<Class<?>> postReqs = super.getPostRequisiteModules();
		final String type = getClassifierType().substring( 0, 1 ).toUpperCase() + getClassifierType().substring( 1 );
		postReqs.add( Class
				.forName( ParserModule.class.getPackage().getName() + "." + getSeqType() + "." + type + "Parser" ) );
		return postReqs;
	}

	/**
	 * This method extends the basic summary by adding metrics on the input files. Prints the number and type of
	 * sequence files and the mean output file size.
	 */
	@Override
	public String getSummary() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		try
		{
			final int numIn = getInputFiles().size();
			if( numIn < 1 )
			{
				return null;
			}

			BigInteger inAvg = BigInteger.valueOf( 0L );
			for( final File f: getInputFiles() )
			{
				final BigInteger size = FileUtils.sizeOfAsBigInteger( f );
				inAvg = inAvg.add( size );

			}
			inAvg = inAvg.divide( BigInteger.valueOf( numIn ) );

			sb.append( "# Input files: " + BioLockJ.RETURN );
			sb.append( "Mean input file size: " + FileUtils.byteCountToDisplaySize( inAvg ) + RETURN );
			return sb.toString() + super.getSummary();

		}
		catch( final Exception ex )
		{
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.get( getClass() ).warn( msg );
		}

		return super.getSummary() + sb.toString();
	}

	/**
	 * This method returns the classifier class name in lower case, after "classifier" is removed.<br>
	 * The remaining text should uniquely identify the name of the program.<br>
	 * The basic deployment will return one of: (rdp, qiime, kraken, metaphlan, or slimm).<br>
	 * <p>
	 * The purpose of this method is to allow users to configure multiple classifiers in a default properties file.<br>
	 * Instead of setting the property {@value biolockj.module.classifier.ClassifierModule#EXE_CLASSIFIER} in the
	 * {@link biolockj.Config} file, leave this value blank and configure the default properties file one time with:
	 * "rdp.classifier", "qiime.classifier", "kraken.classifier", "metaphlan.classifier", and "slimm.classifier".<br>
	 * The same approach should be taken with
	 * {@value biolockj.module.classifier.ClassifierModule#EXE_CLASSIFIER_PARAMS}.
	 *
	 * @return String rdp, qiime, kraken, metaphlan, slimm
	 */
	protected String getClassifierType()
	{
		return getClass().getSimpleName().toLowerCase().replaceAll( "classifier", "" );
	}

	/**
	 * Validate {@link biolockj.module.classifier} modules run after {@link biolockj.module.seq} modules.
	 * 
	 * @throws Exception if modules are out of order
	 */
	protected void validateModuleOrder() throws Exception
	{
		boolean found = false;
		for( final BioModule module: Pipeline.getModules() )
		{
			if( found )
			{
				if( module.getClass().getName().contains( "biolockj.module.seq" ) )
				{
					throw new Exception( "Invalid BioModule configuration order! " + module.getClass().getName()
							+ " must run before any " + getClass().getPackage().getName() + " BioModule." );
				}
			}

			if( module.getClass().equals( getClass() ) )
			{
				found = true;
			}
		}
	}

	private String getSeqType()
	{
		if( getClass().getName().startsWith( "biolockj.module.classifier.wgs" ) )
		{
			return "wgs";
		}

		return "r16s";

	}

	/**
	 * Get classifier specific number of threads property value, if defined. Otherwise, return the standard number of
	 * threads property value.
	 * 
	 * @return Positive integer value
	 * @throws ConfigFormatException if property is not a positive integer
	 * @throws ConfigNotFoundException if properties are undefined
	 */
	public static Integer getNumThreads() throws ConfigFormatException, ConfigNotFoundException
	{
		if( Config.getPositiveInteger( CLASSIFIER_NUM_THREADS ) != null )
		{
			return Config.getPositiveInteger( CLASSIFIER_NUM_THREADS );
		}

		return Config.requirePositiveInteger( SCRIPT_NUM_THREADS );
	}

	/**
	 * Get classifier number of threads property name.
	 * 
	 * @return Number of threads property name
	 * @throws ConfigFormatException if property is not a positive integer
	 */
	public static String getNumThreadsParam() throws ConfigFormatException
	{
		if( Config.getPositiveInteger( CLASSIFIER_NUM_THREADS ) != null )
		{
			return CLASSIFIER_NUM_THREADS;
		}

		return SCRIPT_NUM_THREADS;
	}

	private String classifierExe = null;
	private String classifierParams = null;
}
