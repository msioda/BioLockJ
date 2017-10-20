package biolockj.util.r;

import java.io.File;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;

public abstract class RScript
{

	/**
	 * Filter can be a rawCount value, or a percentage or rows value (if between 0 & 1).
	 * @return String - the filter "if( sum( myT[,OTU_COL] > 0 ) >= R_RARE_OTU_THRESHOLD)"
	 */
	public static String filterRareOTUs( final String code ) throws Exception
	{
		final String otuThreshold = Config.getPositiveDoubleVal( R_RARE_OTU_THRESHOLD );
		if( ( otuThreshold != null ) && !otuThreshold.trim().equals( "1" ) && ( otuThreshold.trim().length() > 0 ) )
		{
			String filterValue = null;
			if( Double.valueOf( otuThreshold ) < 1 )
			{
				filterValue = "( nrow(myT) * " + Double.valueOf( otuThreshold ).toString() + " )";
			}
			else
			{
				filterValue = Double.valueOf( otuThreshold ).toString();
			}
			final StringBuffer sb = new StringBuffer();
			sb.append( getLine( "# Filter rare OTUs as defined in: " + R_RARE_OTU_THRESHOLD ) );
			sb.append( getLine( "if( sum( myT[," + OTU_COL + "] > 0 ) >= " + filterValue + " )" ) );
			sb.append( getLine( "{" ) );
			sb.append( code );
			sb.append( getLine( "}" ) );
			return sb.toString();
		}

		return code;
	}

	public static String forEachOTU( final String code ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLine( "lastOtuCol = ncol(myT) - NUM_META_COLS" ) );
		sb.append( getLine( "for( " + OTU_COL + " in 2:lastOtuCol )" ) );
		sb.append( getLine( "{" ) );
		sb.append( code );
		sb.append( getLine( "}" ) );
		return sb.toString();
	}

	public static String getDebugLabel( final String printVal ) throws Exception
	{
		String output = "";
		if( Config.getBoolean( R_DEBUG ) )
		{
			output = getLine( "print( \"PRINT( " + printVal + " )\" )" );
		}
		return output;
	}

	public static String getDebugLine( final String printVal ) throws Exception
	{
		String output = "";
		if( Config.getBoolean( R_DEBUG ) )
		{
			output = getDebugLabel( printVal ) + getDebugValue( printVal );
		}
		return output;
	}

	public static String getDebugLine( final String line, final String printVal ) throws Exception
	{
		String output = "";
		if( Config.getBoolean( R_DEBUG ) )
		{
			output = getLine( "print( \"" + line.replaceAll( "\"", "" ) + "\" )" ) + getDebugValue( printVal );
		}
		return output;
	}

	public static String getDebugValue( final String printVal ) throws Exception
	{
		String output = "";
		if( Config.getBoolean( R_DEBUG ) )
		{
			output = getLine( "print( " + printVal + " )" ) + getLine( "print( \"" + Log.LOG_SPACER + "\" )" );
		}
		return output;
	}

	public static String getLine( final String val )
	{
		return val + BioLockJ.RETURN;
	}

	public static String getLine( final String line, final String printVal ) throws Exception
	{
		return getLine( line ) + getDebugLine( line, printVal );
	}

	public static final String ALL_ATTS = "allAtts";
	public static final String BINARY_ATTS = "binaryAtts";
	public static final String BINARY_COLS = "binaryCols";
	public static final String INPUT_DIR = "inputDir";
	public static final String NOMINAL_ATTS = "nominalAtts";
	public static final String NOMINAL_COLS = "nominalCols";
	public static final String NUMERIC_ATTS = "numericAtts";
	public static final String NUMERIC_COLS = "numericCols";
	public static final String OTU_COL = "otuCol";
	public static final String OTU_LEVELS = "otuLevels";
	public static final String OUTPUT_DIR = "outputDir";
	public static final String PLOT_COL = "plotCol";
	public static final String PLOT_ROW = "plotRow";
	public static final String R_BINARY_DATA = "r.binaryData";
	public static final String R_DATA = ".RData";
	public static final String R_DEBUG = "r.debug";
	public static final String R_ERROR = ".RError";
	public static final String R_FILTER_ATTRIBUTES = "r.filterAttributes";
	public static final String R_FILTER_NA_ATTRIBUTES = "r.filterNaAttributes";
	public static final String R_FILTER_OPERATORS = "r.filterOperators";
	public static final String R_FILTER_VALUES = "r.filterValues";
	public static final String R_LOG_DIR = "rLogs" + File.separator;
	public static final String R_NOMINAL_DATA = "r.nominalData";
	public static final String R_NUMERIC_DATA = "r.numericData";
	public static final String SCRIPT_DIR = "scriptDir";
	private static final String R_RARE_OTU_THRESHOLD = "r.rareOtuThreshold";
}
