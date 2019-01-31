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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.Config;
import biolockj.Log;
import biolockj.Pipeline;
import biolockj.module.BioModule;
import biolockj.module.implicit.RegisterNumReads;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.module.implicit.qiime.BuildQiimeMapping;
import biolockj.module.implicit.qiime.QiimeClassifier;
import biolockj.module.report.taxa.AddMetadataToTaxaTables;

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
	 * last read count field in the metadata file as a numeric field store in {link
	 * biolockj.module.implicit.RegisterNumReads#getNumReadFieldName()}
	 * 
	 * <p>
	 * If {@link biolockj.Config}.{@value biolockj.Config#REPORT_NUM_HITS} = {@value biolockj.Config#TRUE}, add the
	 * {@link biolockj.module.implicit.parser.ParserModuleImpl#getOtuCountField()} field as a numeric field.
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
		Log.info( RMetaUtil.class, "Validate reportable metadata fields: " + MetaUtil.getFile().getAbsolutePath() );

		final Set<String> rScriptFields = Config.getSet( R_REPORT_FIELDS );
		binaryFields.clear();
		mdsFields.clear();
		nominalFields.clear();
		numericFields.clear();

		nominalFields.addAll( Config.getList( R_NOMINAL_FIELDS ) );
		numericFields.addAll( Config.getList( R_NUMERIC_FIELDS ) );
		mdsFields.addAll( Config.getList( MDS_REPORT_FIELDS ) );
		
		final List<String> excludeFields = Config.getList( R_EXCLUDE_FIELDS );

		nominalFields.removeAll( excludeFields );
		numericFields.removeAll( excludeFields );
		mdsFields.removeAll( excludeFields );
		rScriptFields.addAll( nominalFields );
		rScriptFields.addAll( numericFields );

		final List<String> metaFields = MetaUtil.getFieldNames();

		verifyMetadataFieldsExist( R_EXCLUDE_FIELDS, excludeFields );
		verifyMetadataFieldsExist( R_REPORT_FIELDS, rScriptFields );
		verifyMetadataFieldsExist( R_NUMERIC_FIELDS, numericFields );
		verifyMetadataFieldsExist( R_NOMINAL_FIELDS, nominalFields );
		verifyMetadataFieldsExist( MDS_REPORT_FIELDS, mdsFields );

		if( !RuntimeParamUtil.isDirectMode() )
		{
			Log.debug( RMetaUtil.class,
					"List override nominal fields: " + BioLockJUtil.getCollectionAsString( nominalFields ) );
			Log.debug( RMetaUtil.class,
					"List override numeric fields: " + BioLockJUtil.getCollectionAsString( numericFields ) );
		}

		if( reportAllFields() )
		{
			rScriptFields.addAll( MetaUtil.getFieldNames() );
			rScriptFields.removeAll( excludeFields );
			if( Config.getString( MetaUtil.META_BARCODE_COLUMN ) != null )
			{
				rScriptFields.remove( Config.getString( MetaUtil.META_BARCODE_COLUMN ) );
			}
			if( MetaUtil.hasColumn( Config.getString( MetaUtil.META_FILENAME_COLUMN ) ) )
			{
				rScriptFields.remove( Config.getString( MetaUtil.META_FILENAME_COLUMN ) );
			}

			// remove BLJ generated fields since inclusion depends upon the report.num* properties
			if( ParserModuleImpl.getOtuCountField() != null )
			{
				rScriptFields.remove( ParserModuleImpl.getOtuCountField() );
			}

			if( ParserModuleImpl.getDepricatedOtuCountFields() != null )
			{
				rScriptFields.removeAll( ParserModuleImpl.getDepricatedOtuCountFields() );
			}

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

		if( Config.getBoolean( Config.REPORT_NUM_READS )
				&& isValidNumericField( metaFields, RegisterNumReads.getNumReadFieldName() ) )
		{
			rScriptFields.add( RegisterNumReads.getNumReadFieldName() );
			numericFields.add( RegisterNumReads.getNumReadFieldName() );
		}
		else
		{
			rScriptFields.remove( RegisterNumReads.getNumReadFieldName() );
			numericFields.remove( RegisterNumReads.getNumReadFieldName() );
		}

		if( Config.getBoolean( Config.REPORT_NUM_HITS )
				&& isValidNumericField( metaFields, ParserModuleImpl.getOtuCountField() ) )
		{
			rScriptFields.add( ParserModuleImpl.getOtuCountField() );
			numericFields.add( ParserModuleImpl.getOtuCountField() );
		}
		else
		{
			rScriptFields.remove( ParserModuleImpl.getOtuCountField() );
			numericFields.remove( ParserModuleImpl.getOtuCountField() );
		}

		if( Config.getBoolean( Config.REPORT_NUM_HITS )
				&& isValidNumericField( metaFields, AddMetadataToTaxaTables.HIT_RATIO ) )
		{
			rScriptFields.add( AddMetadataToTaxaTables.HIT_RATIO );
			numericFields.add( AddMetadataToTaxaTables.HIT_RATIO );
		}
		else
		{
			rScriptFields.remove( AddMetadataToTaxaTables.HIT_RATIO );
			numericFields.remove( AddMetadataToTaxaTables.HIT_RATIO );
		}

		if( reportAllFields() && !RuntimeParamUtil.isDirectMode() )
		{
			Log.info( RMetaUtil.class, "R_Modules will report on the all [" + rScriptFields.size()
					+ "] metadata fields since Config property: " + R_REPORT_FIELDS + " is undefined." );

		}

		for( final String field: rScriptFields )
		{
			final Set<String> data = MetaUtil.getUniqueFieldValues( field, true );
			final int count = data.size();
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
					if( !RuntimeParamUtil.isDirectMode() )
					{
						Log.debug( RMetaUtil.class, "Assign as nominal field: " + field );
					}

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
					if( !RuntimeParamUtil.isDirectMode() )
					{
						Log.debug( RMetaUtil.class, "Assign as numeric field: " + field );
					}
				}
			}
		}

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
	 * Get updated R config props
	 * 
	 * @return map of R props by data type
	 * @throws Exception if errors occur
	 */
	public static Map<String, String> getUpdatedRConfig() throws Exception
	{
		final Map<String, String> props = new HashMap<>();
		final Integer numCols = Config.getPositiveInteger( RMetaUtil.NUM_META_COLS );
		final Integer numMetaCols = new Integer( MetaUtil.getFieldNames().size() );

		if( numCols != null && numCols == numMetaCols )
		{
			Log.info( RMetaUtil.class, "R Config unchanged..." );
			return props;
		}

		props.put( NUM_META_COLS, numMetaCols.toString() );
		Log.info( RMetaUtil.class, "Set " + NUM_META_COLS + " = " + numMetaCols );

		if( !binaryFields.isEmpty() )
		{
			final String val = BioLockJUtil.getCollectionAsString( binaryFields );
			if( Config.getString( BINARY_FIELDS ) == null || !val.equals( Config.getString( BINARY_FIELDS ) ) )
			{
				Log.info( RMetaUtil.class, "Set " + BINARY_FIELDS + " = " + val );
				props.put( BINARY_FIELDS, val );
			}

		}
		if( !nominalFields.isEmpty() )
		{
			final String val = BioLockJUtil.getCollectionAsString( nominalFields );
			if( Config.getString( NOMINAL_FIELDS ) == null || !val.equals( Config.getString( NOMINAL_FIELDS ) ) )
			{
				Log.info( RMetaUtil.class, "Set " + NOMINAL_FIELDS + " = " + val );
				props.put( NOMINAL_FIELDS, val );
			}
		}
		if( !numericFields.isEmpty() )
		{
			final String val = BioLockJUtil.getCollectionAsString( numericFields );
			if( Config.getString( NUMERIC_FIELDS ) == null || !val.equals( Config.getString( NUMERIC_FIELDS ) ) )
			{
				Log.info( RMetaUtil.class, "Set " + NUMERIC_FIELDS + " = " + val );
				props.put( NUMERIC_FIELDS, val );
			}
		}

		return props;
	}

	/**
	 * Check module for metadata merged output in hte module output directory.
	 * 
	 * @param module BioModule
	 * @return TRUE if module contains metaMerged.tsv files
	 * @throws Exception if errors occur
	 */
	public static boolean isMetaMergeModule( final BioModule module ) throws Exception
	{
		final Collection<File> files = SeqUtil.removeIgnoredAndEmptyFiles(
				FileUtils.listFiles( module.getOutputDir(), HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE ) );

		if( files.isEmpty() )
		{
			throw new Exception( module.getClass().getSimpleName() + " has no output!" );
		}

		for( final File f: files )
		{
			if( isMetaMergeTable( f ) )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Method analyzes the file name to determine if the file could be output from the BioModule
	 * {@link biolockj.module.report.taxa.AddMetadataToTaxaTables}
	 * 
	 * @param file Ambiguous file
	 * @return TRUE if named like a meta merged file
	 * @throws Exception if errors occur
	 */
	public static boolean isMetaMergeTable( final File file ) throws Exception
	{
		return file.getName().endsWith( AddMetadataToTaxaTables.META_MERGED );
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
	public static void verifyMetadataFieldsExist( final String prop, final Collection<String> fields ) throws Exception
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

	private static boolean hasQiimeMapping() throws Exception
	{
		for( final BioModule module: Pipeline.getModules() )
		{
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

	private static boolean isValidNumericField( final List<String> metaFields, final String field ) throws Exception
	{
		if( field != null && metaFields.contains( field ) )
		{
			final int count = MetaUtil.getUniqueFieldValues( field, true ).size();
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
	protected static final String NUM_META_COLS = "R_internal.numMetaCols";

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

	private static final String BINARY_FIELDS = "R_internal.binaryFields";
	private static final Set<String> binaryFields = new TreeSet<>();
	private static final Set<String> mdsFields = new TreeSet<>();
	private static final String NOMINAL_FIELDS = "R_internal.nominalFields";
	private static final Set<String> nominalFields = new TreeSet<>();
	private static final String NUMERIC_FIELDS = "R_internal.numericFields";
	private static final Set<String> numericFields = new TreeSet<>();
}
