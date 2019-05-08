/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.File;
import java.util.*;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.*;
import biolockj.module.BioModule;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.module.report.taxa.AddMetadataToTaxaTables;

/**
 * This utility is used to validate the metadata to help ensure the format is valid R script input.
 */
public final class RMetaUtil {
	// Prevent instantiation
	private RMetaUtil() {}

	/**
	 * Classify and verify the R filter and reportable metadata fields listed in the {@link biolockj.Config} file.<br>
	 * All metadata fields are reported unless specific fields are listed in:
	 * {@link biolockj.Config}.{@value #R_REPORT_FIELDS}. Each field is classified as binary, nominal, or numeric which
	 * determines the R statistical test used and the type of plot generated.
	 * <ul>
	 * <li>Fields with exactly 2 unique non-null values are classified as binary data unless specified in:
	 * {@link biolockj.Config}.{@value #R_NUMERIC_FIELDS}
	 * <li>Fields with numeric data are classified as numeric data unless specified in:
	 * {@link biolockj.Config}.{@value #R_NOMINAL_FIELDS}
	 * <li>Fields with nominal or mixed nominal/numeric data are classified as nominal data.
	 * </ul>
	 * <p>
	 * If {@link biolockj.Config}.{@value biolockj.Constants#REPORT_NUM_READS} = {@value biolockj.Constants#TRUE}, add
	 * the last read count field in the metadata file as a numeric field store in {link
	 * biolockj.module.implicit.RegisterNumReads#getNumReadFieldName()}
	 * 
	 * <p>
	 * If {@link biolockj.Config}.{@value biolockj.Constants#REPORT_NUM_HITS} = {@value biolockj.Constants#TRUE}, add
	 * the {@link biolockj.module.implicit.parser.ParserModuleImpl#getOtuCountField()} field as a numeric field.
	 * <p>
	 * Perform validations:
	 * <ul>
	 * <li>Verify reportable metadata fields exist and contain at least 2 unique values
	 * <li>Verify numeric fields contain all numeric data
	 * </ul>
	 * <p>
	 * Save MASTER {@link biolockj.Config} properties to store lists of binary, nominal, and numeric fields
	 * 
	 * @param module BioModule
	 * @throws Exception if {@link biolockj.Config} lists invalid metadata fields or metadata filed has less than 2
	 * unique values
	 */
	public static void classifyReportableMetadata( final BioModule module ) throws Exception {
		Log.info( RMetaUtil.class, "Validate reportable metadata fields: " + MetaUtil.getPath() );

		Set<String> rScriptFields = Config.getSet( module, R_REPORT_FIELDS );
		binaryFields = new TreeSet<>();
		mdsFields = new TreeSet<>();
		nominalFields = new TreeSet<>();
		numericFields = new TreeSet<>();

		nominalFields.addAll( Config.getList( module, R_NOMINAL_FIELDS ) );
		numericFields.addAll( Config.getList( module, R_NUMERIC_FIELDS ) );
		mdsFields.addAll( Config.getList( module, R_MDS_REPORT_FIELDS ) );

		final List<String> excludeFields = Config.getList( module, R_EXCLUDE_FIELDS );

		nominalFields.removeAll( excludeFields );
		numericFields.removeAll( excludeFields );
		mdsFields.removeAll( excludeFields );
		rScriptFields.removeAll( excludeFields );
		rScriptFields.addAll( nominalFields );
		rScriptFields.addAll( numericFields );

		verifyMetadataFieldsExist( module, R_EXCLUDE_FIELDS, excludeFields );
		verifyMetadataFieldsExist( module, R_REPORT_FIELDS, rScriptFields );
		verifyMetadataFieldsExist( module, R_NUMERIC_FIELDS, numericFields );
		verifyMetadataFieldsExist( module, R_NOMINAL_FIELDS, nominalFields );
		verifyMetadataFieldsExist( module, R_MDS_REPORT_FIELDS, mdsFields );

		if( !DockerUtil.isDirectMode() ) {
			Log.debug( RMetaUtil.class,
				"List override nominal fields: " + BioLockJUtil.getCollectionAsString( nominalFields ) );
			Log.debug( RMetaUtil.class,
				"List override numeric fields: " + BioLockJUtil.getCollectionAsString( numericFields ) );
		}

		if( reportAllFields( module ) ) {
			rScriptFields.addAll( getMetaCols( module ) );
			rScriptFields.removeAll( excludeFields );
			if( Config.getString( module, MetaUtil.META_BARCODE_COLUMN ) != null ) {
				rScriptFields.remove( Config.getString( module, MetaUtil.META_BARCODE_COLUMN ) );
			}
			if( MetaUtil.hasColumn( Config.getString( module, MetaUtil.META_FILENAME_COLUMN ) ) ) {
				rScriptFields.remove( Config.getString( module, MetaUtil.META_FILENAME_COLUMN ) );
			}

			// remove BLJ generated fields since inclusion depends upon the report.num* properties
			if( ParserModuleImpl.getOtuCountField() != null ) {
				rScriptFields.remove( ParserModuleImpl.getOtuCountField() );
			}

			if( ParserModuleImpl.getDepricatedOtuCountFields() != null ) {
				rScriptFields.removeAll( ParserModuleImpl.getDepricatedOtuCountFields() );
			}

			if( RegisterNumReads.getNumReadFieldName() != null ) {
				rScriptFields.remove( RegisterNumReads.getNumReadFieldName() );
			}

			if( RegisterNumReads.getDepricatedReadFields() != null ) {
				rScriptFields.removeAll( RegisterNumReads.getDepricatedReadFields() );
			}

			if( !DockerUtil.isDirectMode() ) {
				Log.debug( RMetaUtil.class, "List R fields BEFORE chekcing for Qiime alpha metrics: "
					+ BioLockJUtil.getCollectionAsString( rScriptFields ) );
			}
			if( hasQiimeMapping() ) {
				rScriptFields.remove( Constants.QIIME_DESC_COL );
				rScriptFields.remove( Constants.QIIME_DEMUX_COL );
				rScriptFields.remove( Constants.QIIME_LINKER_PRIMER_SEQ_COL );
				rScriptFields.remove( Constants.QIIME_BARCODE_SEQ_COL );
			}
		}

		final boolean reportReads = Config.getBoolean( module, Constants.REPORT_NUM_READS );
		final boolean reportHits = Config.getBoolean( module, Constants.REPORT_NUM_HITS );
		rScriptFields = updateNumericData( RegisterNumReads.getNumReadFieldName(), rScriptFields, reportReads );
		rScriptFields = updateNumericData( ParserModuleImpl.getOtuCountField(), rScriptFields, reportHits );
		rScriptFields = updateNumericData( AddMetadataToTaxaTables.HIT_RATIO, rScriptFields, reportHits );

		if( reportAllFields( module ) && !DockerUtil.isDirectMode() ) {
			Log.info( RMetaUtil.class, "R_Modules will report on the all [" + rScriptFields.size()
				+ "] metadata fields since Config property: " + R_REPORT_FIELDS + " is undefined." );

		}

		for( final String field: rScriptFields ) {
			final Set<String> data = MetaUtil.getUniqueFieldValues( field, true );
			final int count = data.size();
			if( !DockerUtil.isDirectMode() ) {
				Log.info( RMetaUtil.class, "Metadata field [" + field + "] has " + count + " unique non-null values." );
			}

			if( count < 2 ) throw new Exception(
				"Metadata field [" + field + "] is invalid!  Statistical tests require at least 2 unique values." );

			if( numericFields.contains( field ) ) {
				verifyNumericData( field, data );
			} else if( count == 2 ) {
				binaryFields.add( field );
				numericFields.remove( field );
				nominalFields.remove( field );
			}
			// undefined field, so must assign as nominal or numeric
			else if( !nominalFields.contains( field ) ) {
				boolean foundNumeric = false;
				boolean foundNominal = false;

				for( final String val: data ) {
					if( NumberUtils.isNumber( val ) ) {
						foundNumeric = true;
					} else {
						foundNominal = true;
					}
				}

				if( foundNominal && !foundNumeric ) // all nominal
				{
					nominalFields.add( field );
					if( !DockerUtil.isDirectMode() ) {
						Log.debug( RMetaUtil.class, "Assign as nominal field: " + field );
					}

				} else if( foundNominal && foundNumeric ) // mixed nominal/numeric
				{
					nominalFields.add( field );
					if( !DockerUtil.isDirectMode() ) {
						Log.warn( RMetaUtil.class, "Metadata field [" + field + "] has both numeric and "
							+ "non-numeric data so will be classified as nominal data" );
					}
				} else if( !foundNominal && foundNumeric ) // all numeric
				{
					numericFields.add( field );
					if( !DockerUtil.isDirectMode() ) {
						Log.debug( RMetaUtil.class, "Assign as numeric field: " + field );
					}
				}
			}
		}

		if( updateRConfig( module ) && !MasterConfigUtil.saveMasterConfig() )
			throw new Exception( "Failed to update MASTER config with latest \"R_internal\" Config" );

		if( !DockerUtil.isDirectMode() ) {
			Log.info( RMetaUtil.class, Constants.LOG_SPACER );
			Log.info( RMetaUtil.class, "Reportable metadata field validations complete for: " + rScriptFields );
			Log.info( RMetaUtil.class, Constants.LOG_SPACER );
		}
	}

	/**
	 * Get the {@link biolockj.Config}.{@value #BINARY_FIELDS} fields containing only 2 non-numeric values.
	 *
	 * @param module is the Calling BioModule
	 * @return Set of column names with only 2 non-numeric values
	 * @throws Exception if unable to assing binary fields
	 */
	public static Set<String> getBinaryFields( final BioModule module ) throws Exception {
		if( binaryFields == null ) {
			classifyReportableMetadata( module );
		}
		return binaryFields;
	}

	/**
	 * Method analyzes the file name to determine if the file could be output from the BioModule
	 * {@link biolockj.module.report.taxa.AddMetadataToTaxaTables}
	 * 
	 * @param file Ambiguous file
	 * @return TRUE if named like a meta merged file
	 */
	public static boolean isMetaMergeTable( final File file ) {
		return file.getName().endsWith( AddMetadataToTaxaTables.META_MERGED );
	}

	/**
	 * The override property: {@link biolockj.Config}.{@value #R_REPORT_FIELDS} can be used to list the metadata
	 * reportable fields for use in the R modules. If undefined, report all fields.
	 * 
	 * @param module BioModule
	 * @return true if {@value #R_REPORT_FIELDS} is empty
	 */
	public static boolean reportAllFields( final BioModule module ) {
		return Config.getSet( module, R_REPORT_FIELDS ).isEmpty();
	}

	/**
	 * Get updated R config props
	 * 
	 * @param module BioModule
	 * @return map of R props by data type
	 * @throws Exception if errors occur
	 */
	public static boolean updateRConfig( final BioModule module ) throws Exception {
		final Integer numCols = Config.getPositiveInteger( module, RMetaUtil.NUM_META_COLS );
		final Integer numMetaCols = getMetaCols( module ).size();

		if( numCols != null && numCols == numMetaCols || numMetaCols == 0 ) {
			Log.info( RMetaUtil.class, "R Config unchanged..." );
			return false;
		}

		Config.setConfigProperty( NUM_META_COLS, numMetaCols.toString() );
		Log.info( RMetaUtil.class, "Set " + NUM_META_COLS + " = " + numMetaCols );

		if( !binaryFields.isEmpty() ) {
			final String val = BioLockJUtil.getCollectionAsString( binaryFields );
			String rData = null;
			if( Config.getString( null, BINARY_FIELDS ) != null ) {
				rData = BioLockJUtil.getCollectionAsString( Config.getSet( null, BINARY_FIELDS ) );
			}

			if( rData == null || !val.equals( rData ) ) {
				Log.info( RMetaUtil.class, "Set " + BINARY_FIELDS + " = " + val );
				Config.setConfigProperty( BINARY_FIELDS, val );
			}

		}

		if( !nominalFields.isEmpty() ) {
			final String val = BioLockJUtil.getCollectionAsString( nominalFields );
			String rData = null;
			if( Config.getString( null, NOMINAL_FIELDS ) != null ) {
				rData = BioLockJUtil.getCollectionAsString( Config.getSet( null, NOMINAL_FIELDS ) );
			}

			if( rData == null || !val.equals( rData ) ) {
				Log.info( RMetaUtil.class, "Set " + NOMINAL_FIELDS + " = " + val );
				Config.setConfigProperty( NOMINAL_FIELDS, val );
			}
		}

		if( !numericFields.isEmpty() ) {
			final String val = BioLockJUtil.getCollectionAsString( numericFields );
			String rData = null;
			if( Config.getString( null, NUMERIC_FIELDS ) != null ) {
				rData = BioLockJUtil.getCollectionAsString( Config.getSet( null, NUMERIC_FIELDS ) );
			}

			if( rData == null || !val.equals( rData ) ) {
				Log.info( RMetaUtil.class, "Set " + NUMERIC_FIELDS + " = " + val );
				Config.setConfigProperty( NUMERIC_FIELDS, val );
			}
		}

		return true;
	}

	/**
	 * This method verifies the fields given exist in the metadata file.
	 * 
	 * @param module Source BioModule calling this utility
	 * @param prop Config property name
	 * @param fields Set of metadata fields
	 * @throws Exception if the metadata column does not exist
	 */
	public static void verifyMetadataFieldsExist( final BioModule module, final String prop,
		final Collection<String> fields ) throws Exception {
		for( final String field: fields ) {
			if( !MetaUtil.getFieldNames().contains( field ) && !isQiimeMetric( module, field ) )
				throw new Exception( "Config property [ " + prop + "] contians a field [" + field
					+ "] not found in metadata: " + MetaUtil.getPath() );
		}
	}

	private static List<String> getMetaCols( final BioModule module ) throws Exception {
		final List<String> cols = new ArrayList<>();
		for( final String field: MetaUtil.getFieldNames() ) {
			if( !isQiimeMetric( module, field ) ) {
				cols.add( field );
			}
		}
		return cols;
	}

	private static boolean hasQiimeMapping() {
		for( final BioModule module: Pipeline.getModules() ) {
			if( module.getClass().getName().toLowerCase().contains( Constants.QIIME ) ) {
				Log.debug( RMetaUtil.class, "Found Qiime Module: " + module.getClass().getName() );
				return true;
			}
		}

		return false;
	}

	private static boolean isQiimeMetric( final BioModule module, final String field ) {
		final Set<String> alphaDivMetrics = Config.getSet( module, Constants.QIIME_ALPHA_DIVERSITY_METRICS );
		if( !alphaDivMetrics.isEmpty() ) {
			for( final String metric: alphaDivMetrics ) {
				if( field.equals( metric + QIIME_ALPHA_METRIC_SUFFIX ) ) {
					Log.info( RMetaUtil.class,
						"Metadata validation of field(" + field + ") --> found QIIME metric: " + metric );
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isValidNumericField( final String field ) throws Exception {
		if( field != null && MetaUtil.getFieldNames().contains( field ) ) {
			final int count = MetaUtil.getUniqueFieldValues( field, true ).size();
			if( count > 1 ) return true;
			Log.warn( RMetaUtil.class, "Metadata field [" + field + "] has only " + count
				+ " unique values.  R modules will not include this field because statistical tests require at least 2 unique values." );
		}
		return false;
	}

	private static Set<String> updateNumericData( final String field, final Set<String> rScriptFields,
		final boolean doUpdate ) throws Exception {
		if( doUpdate && isValidNumericField( field ) ) {
			rScriptFields.add( field );
			numericFields.add( field );
		} else {
			rScriptFields.remove( field );
			numericFields.remove( field );
		}

		return rScriptFields;
	}

	private static void verifyNumericData( final String field, final Set<String> data ) throws Exception {
		for( final String val: data ) {
			if( !NumberUtils.isNumber( val ) ) throw new Exception( "Invalid Config! " + R_NUMERIC_FIELDS
				+ " contains field [" + field + "] with non-numeric data [" + val + "]" );
		}
	}

	/**
	 * {@link biolockj.Config} Internal List property: {@value #BINARY_FIELDS}<br>
	 * Binary fields contain only 2 unique non-NA options
	 */
	protected static final String BINARY_FIELDS = "R_internal.binaryFields";

	/**
	 * Name of R script variable with metadata column count
	 */
	protected static final String NUM_META_COLS = "R_internal.numMetaCols";

	/**
	 * {@link biolockj.Config} List property: {@value #R_EXCLUDE_FIELDS}<br>
	 * R reports must contain at least one valid nominal or numeric metadata field.
	 */
	protected static final String R_EXCLUDE_FIELDS = "r.excludeFields";

	/**
	 * {@link biolockj.Config} List property: {@value #R_MDS_REPORT_FIELDS}<br>
	 * Fields listed here must exist in the metadata file.
	 */
	protected static final String R_MDS_REPORT_FIELDS = "r_PlotMds.reportFields";

	/**
	 * {@link biolockj.Config} List property: {@value #R_NOMINAL_FIELDS}<br>
	 * Override default property type by explicitly listing it as nominal.
	 */
	protected static final String R_NOMINAL_FIELDS = "r.nominalFields";

	/**
	 * {@link biolockj.Config} List property: {@value #R_NUMERIC_FIELDS}<br>
	 * Override default property type by explicitly listing it as numeric.
	 */
	protected static final String R_NUMERIC_FIELDS = "r.numericFields";

	/**
	 * {@link biolockj.Config} List property: {@value #R_REPORT_FIELDS}<br>
	 * R reports must contain at least one valid field.
	 */
	protected static final String R_REPORT_FIELDS = "r.reportFields";

	private static Set<String> binaryFields = null;
	private static Set<String> mdsFields = null;
	private static final String NOMINAL_FIELDS = "R_internal.nominalFields";
	private static Set<String> nominalFields = null;
	private static final String NUMERIC_FIELDS = "R_internal.numericFields";
	private static Set<String> numericFields = null;
	private static final String QIIME_ALPHA_METRIC_SUFFIX = "_alpha";
}
