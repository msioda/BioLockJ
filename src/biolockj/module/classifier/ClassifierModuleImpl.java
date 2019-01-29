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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.SeqModuleImpl;
import biolockj.module.implicit.parser.ParserModule;

/**
 * This is the superclass for all WGS and 16S biolockj.module.classifier BioModules.
 */
public abstract class ClassifierModuleImpl extends SeqModuleImpl implements ClassifierModule
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
		getClassifierExe();
		getClassifierParams();
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
		if( classifierExe == null )
		{
			classifierExe = Config.getString( EXE_CLASSIFIER );
			if( classifierExe == null || classifierExe.isEmpty() )
			{
				final String defaultProp = getClassifierType() + "." + EXE_CLASSIFIER.substring( 4 );

				classifierExe = Config.getString( defaultProp );
				if( classifierExe != null && !classifierExe.isEmpty() )
				{
					Log.info( getClass(), "Using default classifier property: " + defaultProp + " = " + classifierExe );
				}
				else
				{
					classifierExe = getClassifierType();
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
	 * rdp.classifier will be used in place of exe.classifier. This allows all classifiers to be defined in
	 * standard.properties.
	 *
	 * @return List of classifier runtime parameters
	 * @throws Exception if runtime errors occur
	 */
	@Override
	public List<String> getClassifierParams() throws Exception
	{
		if( classifierParams == null )
		{
			String prop = EXE_CLASSIFIER_PARAMS;
			classifierParams = Config.getList( prop );
			if( classifierParams.isEmpty() )
			{
				prop = getClassifierType() + "." + EXE_CLASSIFIER_PARAMS.substring( 4 );
				classifierParams = Config.getList( prop );
				if( !classifierParams.isEmpty() )
				{
					Log.info( getClass(), "Loading default classifier property: " + prop + " = " + classifierParams );
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
	public List<String> getPostRequisiteModules() throws Exception
	{
		final List<String> postReqs = new ArrayList<>();
		final String type = getClassifierType().substring( 0, 1 ).toUpperCase() + getClassifierType().substring( 1 );
		postReqs.add( ParserModule.class.getPackage().getName() + "." + getSeqType() + "." + type + "Parser" );
		postReqs.addAll( super.getPostRequisiteModules() );
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

			sb.append( "# Input files: " + numIn + BioLockJ.RETURN );
			sb.append( "Mean input file size: " + FileUtils.byteCountToDisplaySize( inAvg ) + RETURN );
			return sb.toString() + super.getSummary();

		}
		catch( final Exception ex )
		{
			final String msg = "Unable to complete module summary: " + ex.getMessage();
			sb.append( msg + RETURN );
			Log.warn( getClass(), msg );
		}

		return super.getSummary() + sb.toString();
	}

	/**
	 * This method returns the classifier class name in lower case, after "classifier" is removed.<br>
	 * The remaining text should uniquely identify the name of the program.<br>
	 * The basic deployment will return one of: (rdp, qiime, kraken, metaphlan, humann2, or slimm).<br>
	 * <p>
	 * The purpose of this method is to allow users to configure multiple classifiers in a default properties file.<br>
	 * Instead of setting the property {@value biolockj.module.classifier.ClassifierModule#EXE_CLASSIFIER} in the
	 * {@link biolockj.Config} file, leave this value blank and configure the default properties file one time with:
	 * "rdp.classifier", "qiime.classifier", "kraken.classifier", etc.<br>
	 *
	 * @return String - options { rdp, qiime, kraken, kraken2, metaphlan, humann2, slimm }
	 */
	protected String getClassifierType()
	{
		String type = getClass().getSimpleName().toLowerCase().replaceAll( "classifier", "" );
		if( type.startsWith( "qiime" ) )
		{
			type = "qiime";
		}

		return type;
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

	private String classifierExe = null;
	private List<String> classifierParams = null;
}
