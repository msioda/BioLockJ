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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.module.implicit.qiime.BuildQiimeMapping;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.module.report.AddMetadataToOtuTables;

/**
 * This utility is used to validate the metadata to help ensure the format is valid R script input.
 */
public final class RMetaUtil
{
	// Prevent instantiation
	private RMetaUtil()
	{}

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
	 * If {@link biolockj.Config}.{@value biolockj.Config#REPORT_NUM_READS} = {@value biolockj.Config#TRUE}, add the
	 * last read count field in the metadata file as a numeric field:
	 * <ul>
	 * <li>{@value biolockj.module.implicit.RegisterNumReads#NUM_READS}
	 * <li>{@value biolockj.module.seq.TrimPrimers#NUM_TRIMMED_READS}
	 * <li>{@value biolockj.module.seq.SeqFileValidator#NUM_VALID_READS}
	 * <li>{@value biolockj.module.seq.Rarefier#NUM_RAREFIED_READS}
	 * </ul>
	 * <p>
	 * If {@link biolockj.Config}.{@value biolockj.Config#REPORT_NUM_HITS} = {@value biolockj.Config#TRUE}, add the
	 * {@value biolockj.module.implicit.parser.ParserModuleImpl#NUM_HITS} field as a numeric field.
	 * <p>
	 * Perform validations:
	 * <ul>
	 * <li>Verify reportable metadata fields exist and contain at least 2 unique values
	 * <li>Verify numeric fields contain all numeric data
	 * </ul>
	 * <p>
	 * Save MASTER {@link biolockj.Config} properties to store lists of binary, nominal, and numeric fields
	 * 
	 * @throws Exception if {@link biolockj.Config} lists invalid metadata fields or metadata filed has less than 2
	 * unique values
	 */
	public static void classifyReportableMetadata() throws Exception
	{
		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( RMetaUtil.class, "Validating Metadata R fields in: " + MetaUtil.getFile().getAbsolutePath() );
		}
		final Set<String> rScriptFields = new HashSet<>();
		binaryFields.clear();
		mdsFields.clear();
		nominalFields.clear();
		numericFields.clear();

		nominalFields.addAll( Config.getSet( R_NOMINAL_FIELDS ) );
		numericFields.addAll( Config.getSet( R_NUMERIC_FIELDS ) );
		mdsFields.addAll( Config.getSet( MDS_REPORT_FIELDS ) );

		final Set<String> excludeFields = Config.getSet( R_EXCLUDE_FIELDS );

		nominalFields.removeAll( excludeFields );
		numericFields.removeAll( excludeFields );
		mdsFields.removeAll( excludeFields );

		verifyMetadataFieldsExist( R_EXCLUDE_FIELDS, excludeFields );
		verifyMetadataFieldsExist( R_REPORT_FIELDS, Config.getSet( R_REPORT_FIELDS ) );
		verifyMetadataFieldsExist( R_NUMERIC_FIELDS, numericFields );
		verifyMetadataFieldsExist( R_NOMINAL_FIELDS, nominalFields );
		verifyMetadataFieldsExist( MDS_REPORT_FIELDS, mdsFields );

		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( RMetaUtil.class, "List override nominalFields BEFORE checking for generated columns: "
					+ BioLockJUtil.getCollectionAsString( nominalFields ) );
		}

		rScriptFields.addAll( Config.getSet( R_REPORT_FIELDS ) );

		if( reportAllFields() )
		{
			rScriptFields.addAll( MetaUtil.getFieldNames() );
			rScriptFields.removeAll( excludeFields );
			if( Config.getString( MetaUtil.META_BARCODE_COLUMN ) != null )
			{
				rScriptFields.remove( Config.getString( MetaUtil.META_BARCODE_COLUMN ) );
			}

			// remove BLJ generated fields since inclusion depends upon the report.num* properties
			rScriptFields.remove( ParserModuleImpl.NUM_HITS );
			if( RegisterNumReads.getNumReadFieldName() != null )
			{
				rScriptFields.remove( RegisterNumReads.getNumReadFieldName() );
			}

			if( RegisterNumReads.getDepricatedReadFields() != null )
			{
				rScriptFields.removeAll( RegisterNumReads.getDepricatedReadFields() );
			}

			if( !RuntimeParamUtil.isDirectMode() )
			{
				Log.debug( RMetaUtil.class, "List R fields BEFORE chekcing for Qiime alpha metrics: "
						+ BioLockJUtil.getCollectionAsString( rScriptFields ) );
			}
			if( hasQiimeMapping() )
			{
				rScriptFields.remove( BuildQiimeMapping.DESCRIPTION );
				rScriptFields.remove( BuildQiimeMapping.DEMUX_COLUMN );
				rScriptFields.remove( BuildQiimeMapping.LINKER_PRIMER_SEQUENCE );
				rScriptFields.remove( BuildQiimeMapping.BARCODE_SEQUENCE );
			}
		}

		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( RMetaUtil.class, "List R fields AFTER checking for generated columnss: "
					+ BioLockJUtil.getCollectionAsString( rScriptFields ) );
		}

		if( Config.getBoolean( Config.REPORT_NUM_READS )
				&& isValidNumericField( RegisterNumReads.getNumReadFieldName() ) )
		{
			rScriptFields.add( RegisterNumReads.getNumReadFieldName() );
			numericFields.add( RegisterNumReads.getNumReadFieldName() );
		}
		else
		{
			rScriptFields.remove( RegisterNumReads.getNumReadFieldName() );
			numericFields.remove( RegisterNumReads.getNumReadFieldName() );
		}

		if( Config.getBoolean( Config.REPORT_NUM_HITS ) && isValidNumericField( ParserModuleImpl.NUM_HITS ) )
		{
			rScriptFields.add( ParserModuleImpl.NUM_HITS );
			numericFields.add( ParserModuleImpl.NUM_HITS );
		}
		else
		{
			rScriptFields.remove( ParserModuleImpl.NUM_HITS );
			numericFields.remove( ParserModuleImpl.NUM_HITS );
		}

		if( Config.getBoolean( Config.REPORT_NUM_HITS ) && isValidNumericField( AddMetadataToOtuTables.HIT_RATIO ) )
		{
			rScriptFields.add( AddMetadataToOtuTables.HIT_RATIO );
			numericFields.add( AddMetadataToOtuTables.HIT_RATIO );
		}
		else
		{
			rScriptFields.remove( AddMetadataToOtuTables.HIT_RATIO );
			numericFields.remove( AddMetadataToOtuTables.HIT_RATIO );
		}

		if( reportAllFields() && !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( RMetaUtil.class, "R_Modules will report on the all [" + rScriptFields.size()
					+ "] metadata fields since Config property: " + R_REPORT_FIELDS + " is undefined." );

		}

		for( final String field: rScriptFields )
		{
			final Set<String> data = getUniqueValues( field );
			final int count = countUniqueValues( field );
			if( !RuntimeParamUtil.isDirectMode() )
			{
				Log.info( RMetaUtil.class, "Metadata field [" + field + "] has " + count + " unique non-null values." );
			}

			if( count < 2 )
			{
				throw new Exception( "Metadata field [" + field
						+ "] is invalid!  Statistical tests require at least 2 unique values." );
			}

			if( numericFields.contains( field ) )
			{
				verifyNumericData( field, data );
			}
			else if( count == 2 )
			{
				binaryFields.add( field );
				numericFields.remove( field );
				nominalFields.remove( field );
			}
			// undefined field, so must assign as nominal or numeric
			else if( !nominalFields.contains( field ) )
			{
				boolean foundNumeric = false;
				boolean foundNominal = false;

				if( !RuntimeParamUtil.isDirectMode() )
				{
					Log.warn( RMetaUtil.class, "Assigning numeric/nominal field status for: " + field );
				}

				for( final String val: data )
				{
					if( NumberUtils.isNumber( val ) )
					{
						foundNumeric = true;
					}
					else
					{
						foundNominal = true;
					}
				}

				if( foundNominal && !foundNumeric ) // all nominal
				{
					nominalFields.add( field );
				}
				else if( foundNominal && foundNumeric ) // mixed nominal/numeric
				{
					nominalFields.add( field );
					if( !RuntimeParamUtil.isDirectMode() )
					{
						Log.warn( RMetaUtil.class, "Metadata field [" + field + "] has both numeric and "
								+ "non-numeric data so will be classified as nominal data" );
					}
				}
				else if( !foundNominal && foundNumeric ) // all numeric
				{
					numericFields.add( field );
				}
			}
		}

		updateProps();

		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( RMetaUtil.class, Log.LOG_SPACER );
			Log.info( RMetaUtil.class, "Reportable metadata field validations complete for: " + rScriptFields );
			Log.info( RMetaUtil.class, Log.LOG_SPACER );
		}
	}

	/**
	 * Get the {@link biolockj.Config}.{@value #R_NOMINAL_FIELDS} fields containing only 2 non-numeric values.
	 *
	 * @return Set of column names with only 2 non-numeric values
	 */
	public static Set<String> getBinaryFields()
	{
		return binaryFields;
	}

	/**
	 * Get the {@link biolockj.Config}.{@value #MDS_REPORT_FIELDS} fields.
	 *
	 * @return Set of MDS fields
	 */
	public static Set<String> getMdsFields()
	{
		return mdsFields;
	}

	/**
	 * Get the {@link biolockj.Config}.{@value #R_NOMINAL_FIELDS} fields.
	 *
	 * @return Set of Nominal (category) fields
	 */
	public static Set<String> getNominalFields()
	{
		return nominalFields;
	}

	/**
	 * Get the {@link biolockj.Config}.{@value #R_NUMERIC_FIELDS} fields.
	 *
	 * @return Set of Numeric fields
	 */
	public static Set<String> getNumericFields()
	{
		return numericFields;
	}

	/**
	 * The override property: {@link biolockj.Config}.{@value #R_REPORT_FIELDS} can be used to list the metadata
	 * reportable fields for use in the R modules. If undefined, report all fields.
	 * 
	 * @return true if {@value #R_REPORT_FIELDS} is empty
	 */
	public static boolean reportAllFields()
	{
		return Config.getSet( R_REPORT_FIELDS ).isEmpty();
	}

	/**
	 * This method verifies the fields given exist in the metadata file.
	 * 
	 * @param prop Config property name
	 * @param fields Set of metadata fields
	 * @throws Exception if the metadata column does not exist
	 */
	public static void verifyMetadataFieldsExist( final String prop, final Set<String> fields ) throws Exception
	{
		for( final String field: fields )
		{
			if( !MetaUtil.getFieldNames().contains( field ) && !isQiimeMetric( field ) )
			{

				throw new Exception( "Config property [ " + prop + "] contians a field [" + field
						+ "] not found in metadata: " + MetaUtil.getFile().getAbsolutePath() );
			}
		}
	}

	private static int countUniqueValues( final String att ) throws Exception
	{
		return getUniqueValues( att ).size();
	}

	private static String getSetAsString( final Set<String> set )
	{
		final StringBuffer sb = new StringBuffer();
		for( final String val: set )
		{
			sb.append( sb.toString().isEmpty() ? val: "," + val );
		}

		return sb.toString();
	}

	private static Set<String> getUniqueValues( final String att ) throws Exception
	{
		final Set<String> vals = new HashSet<>( MetaUtil.getFieldValues( att ) );
		vals.remove( Config.requireString( MetaUtil.META_NULL_VALUE ) );
		return vals;
	}

	private static boolean hasQiimeMapping() throws Exception
	{
		for( final BioModule module: Pipeline.getModules() )
		{
			Log.debug( RMetaUtil.class, "Looking for Qiime: " + module.getClass().getName() );
			if( module.getClass().getName().toLowerCase().contains( "qiime" ) )
			{
				Log.debug( RMetaUtil.class, "Found Qiime Module: " + module.getClass().getName() );
				return true;
			}
		}

		return false;
	}

	private static boolean isQiimeMetric( final String field )
	{
		final Set<String> alphaDivMetrics = Config.getSet( QiimeClassifier.QIIME_ALPHA_DIVERSITY_METRICS );
		if( !alphaDivMetrics.isEmpty() )
		{
			for( final String metric: alphaDivMetrics )
			{
				if( field.indexOf( metric ) > -1 )
				{
					Log.info( RMetaUtil.class,
							"Metadata validation of field(" + field + ") --> found QIIME metric: " + metric );
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isValidNumericField( final String field ) throws Exception
	{
		if( field != null && MetaUtil.getFieldNames().contains( field ) )
		{
			final int count = countUniqueValues( field );
			if( count > 1 )
			{
				return true;
			}
			else
			{
				Log.warn( RMetaUtil.class, "Metadata field [" + field + "] has only " + count
						+ " unique values.  R modules will not include this field because statistical tests require at least 2 unique values." );
			}
		}

		return false;
	}

	private static void updateProps() throws Exception
	{
		final Map<String, String> props = new HashMap<>();
		if( !binaryFields.isEmpty() )
		{
			props.put( BINARY_FIELDS, getSetAsString( binaryFields ) );
		}
		if( !nominalFields.isEmpty() )
		{
			props.put( NOMINAL_FIELDS, getSetAsString( nominalFields ) );
		}
		if( !numericFields.isEmpty() )
		{
			props.put( NUMERIC_FIELDS, getSetAsString( numericFields ) );
		}

		props.put( NUM_META_COLS, new Integer( MetaUtil.getFieldNames().size() ).toString() );

		BioLockJUtil.updateMasterConfig( props );
	}

	private static void verifyNumericData( final String field, final Set<String> data ) throws Exception
	{
		for( final String val: data )
		{
			if( !NumberUtils.isNumber( val ) )
			{
				throw new Exception( "Invalid Config! " + R_NUMERIC_FIELDS + " contains field [" + field
						+ "] with non-numeric data [" + val + "]" );
			}
		}
	}

	/**
	 * {@link biolockj.Config} List property: {@value #MDS_REPORT_FIELDS}<br>
	 * List metadata fields to generate MDS ordination plots.
	 */
	protected static final String MDS_REPORT_FIELDS = "rMds.reportFields";

	/**
	 * Name of R script variable with metadata column count
	 */
	protected static final String NUM_META_COLS = "internal.numMetaCols";

	/**
	 * {@link biolockj.Config} List property: {@value #R_EXCLUDE_FIELDS}<br>
	 * R reports must contain at least one valid nominal or numeric metadata field.
	 */
	protected static final String R_EXCLUDE_FIELDS = "r.excludeFields";

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

	private static final String BINARY_FIELDS = "internal.binaryFields";
	private static final Set<String> binaryFields = new TreeSet<>();
	private static final Set<String> mdsFields = new TreeSet<>();
	private static final String NOMINAL_FIELDS = "internal.nominalFields";
	private static final Set<String> nominalFields = new TreeSet<>();
	private static final String NUMERIC_FIELDS = "internal.numericFields";
	private static final Set<String> numericFields = new TreeSet<>();
}
