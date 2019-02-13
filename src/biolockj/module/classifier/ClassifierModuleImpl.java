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
import biolockj.BioLockJ;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.BioModule;
import biolockj.module.SeqModuleImpl;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.util.ModuleUtil;

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

	@Override
	public abstract String getClassifierExe() throws Exception;

	@Override
	public abstract List<String> getClassifierParams() throws Exception;

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

			sb.append( "# Input files: " + numIn + Constants.RETURN );
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
	 * The basic deployment will return one of: (rdp, qiime, kraken, metaphlan2, humann2, or slimm).<br>
	 * <p>
	 * The purpose of this method is to allow users to configure multiple classifiers in a default properties file.<br>
	 * Instead of setting the property {@value biolockj.Constants#EXE_CLASSIFIER} in the
	 * {@link biolockj.Config} file, leave this value blank and configure the default properties file one time with:
	 * "rdp.classifier", "qiime.classifier", "kraken.classifier", etc.<br>
	 *
	 * @return String - options { rdp, qiime, kraken, kraken2, metaphlan2, humann2, slimm }
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
	 * Validate that no {@link biolockj.module.seq} modules run after this classifier unless a new classifier branch is
	 * started.
	 * 
	 * @throws Exception if modules are out of order
	 */
	protected void validateModuleOrder() throws Exception
	{
		for( final BioModule module: ModuleUtil.getModules( this, true ) )
		{
			if( module.equals( ModuleUtil.getClassifier( this, true ) ) )
			{
				break;
			}
			else if( module.getClass().getName().startsWith( Constants.MODULE_SEQ_PACKAGE ) )
			{
				throw new Exception( "Invalid BioModule configuration order! " + module.getClass().getName()
						+ " must run before the ParserModule." );
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
}
