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
import java.util.ArrayList;
import java.util.List;
import biolockj.Constants;
import biolockj.Log;
import biolockj.exception.*;
import biolockj.module.*;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.util.ModuleUtil;
import biolockj.util.SummaryUtil;

/**
 * This is the superclass for all WGS and 16S biolockj.module.classifier BioModules.
 */
public abstract class ClassifierModuleImpl extends SeqModuleImpl implements ClassifierModule, DatabaseModule {

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
	public void checkDependencies() throws Exception {
		getClassifierExe();
		getClassifierParams();
		validateModuleOrder();
		super.checkDependencies();
	}

	@Override
	public abstract String getClassifierExe() throws ConfigException;

	@Override
	public abstract List<String> getClassifierParams() throws ConfigException;

	@Override
	public abstract File getDB() throws ConfigPathException, ConfigNotFoundException;

	/**
	 * This method returns the corresponding Parser module associated with the classifier. The Parser module name is
	 * built using the same prefix used in the Classifier module
	 * <p>
	 * For example RdpClassifier, will return the RdpParser.
	 */
	@Override
	public List<String> getPostRequisiteModules() throws Exception {
		final List<String> postReqs = new ArrayList<>();
		final String type = getClassifierType().substring( 0, 1 ).toUpperCase() + getClassifierType().substring( 1 );
		postReqs.add( ParserModule.class.getPackage().getName() + "." + getSeqType() + "." + type + "Parser" );
		postReqs.addAll( super.getPostRequisiteModules() );
		return postReqs;
	}

	@Override
	public String getSummary() throws Exception {
		return super.getSummary() + SummaryUtil.getInputSummary( this );
	}

	/**
	 * This method returns the classifier class name in lower case, after "classifier" is removed.<br>
	 * The remaining text should uniquely identify the name of the program.<br>
	 * The basic deployment will return one of: (rdp, qiime, kraken, metaphlan2, or humann2).<br>
	 *
	 * @return String - options { rdp, qiime, kraken, kraken2, metaphlan2, or humann2 }
	 */
	protected String getClassifierType() {
		String type = getClass().getSimpleName().toLowerCase().replaceAll( "classifier", "" );
		if( type.startsWith( Constants.QIIME ) ) type = Constants.QIIME;

		return type;
	}

	/**
	 * DB directory path getter
	 * 
	 * @return DB directory
	 */
	protected File getDbCache() {
		return this.dbCache;
	}

	/**
	 * Set DB cache directory path on 1st access
	 * 
	 * @param db Database file
	 */
	protected void setDbCache( final File db ) {
		if( db != null ) Log.info( getClass(), "Set DB cache: " + db.getAbsolutePath() );
		this.dbCache = db;
	}

	/**
	 * Validate that no {@link biolockj.module.seq} modules run after this classifier unless a new classifier branch is
	 * started.
	 * 
	 * @throws Exception if modules are out of order
	 */
	protected void validateModuleOrder() throws Exception {
		for( final BioModule module: ModuleUtil.getModules( this, true ) )
			if( module.equals( ModuleUtil.getClassifier( this, true ) ) ) break;
			else if( module.getClass().getName().startsWith( Constants.MODULE_SEQ_PACKAGE ) )
				throw new Exception( "Invalid BioModule configuration order! " + module.getClass().getName() +
					" must run before the ParserModule." );
	}

	private String getSeqType() {
		if( getClass().getName().startsWith( "biolockj.module.classifier.wgs" ) ) return "wgs";

		return "r16s";

	}

	private File dbCache = null;
}
