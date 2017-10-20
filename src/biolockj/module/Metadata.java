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
package biolockj.module;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.Log;
import biolockj.util.MetaUtil;

/**
 * The metadataUtil helps access and modify data in the metadata file.
 */
public class Metadata extends BioModuleImpl implements BioModule
{
	@Override
	public void checkDependencies() throws Exception
	{
		Config.requireString( MetaUtil.INPUT_NULL_VALUE );
		Config.requireExistingFile( Config.INPUT_METADATA );
		inputDelim = Config.requireString( INPUT_COL_DELIM );
		if( inputDelim.equals( "\\t" ) )
		{
			inputDelim = TAB_DELIM;
		}
	}

	@Override
	public void executeProjectFile() throws Exception
	{
		Log.out.info( "Importing metadata (delim=" + Config.requireString( INPUT_COL_DELIM ) + "): "
				+ MetaUtil.getFile().getAbsolutePath() );

		final BufferedReader reader = new BufferedReader( new FileReader( MetaUtil.getFile() ) );
		MetaUtil.setFile(
				new File( getOutputDir().getAbsolutePath() + File.separator + MetaUtil.getFile().getName() ) );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( MetaUtil.getFile() ) );

		try
		{
			int lineNum = 0;
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				writer.write( getRow( line, ( lineNum++ == 0 ) ) );
			}
		}
		finally
		{
			reader.close();
			writer.close();
		}

		MetaUtil.refresh();
	}

	/**
	 * Summary message.
	 */
	@Override
	public String getSummary()
	{
		try
		{
			final StringBuffer sb = new StringBuffer();
			sb.append( "Current Metadata:  " + MetaUtil.getFile().getAbsolutePath() + RETURN );
			sb.append( "# Samples in metadata file:  " + ( MetaUtil.getMetaFileFirstColValues().size() - 1 ) + RETURN );
			sb.append( "# Attributes in metadata file:  " + MetaUtil.getAttributeNames().size() + RETURN );
			return sb.toString();
		}
		catch( final Exception ex )
		{
			Log.out.error( "Unable to produce module summary for: " + getClass().getName() + " : " + ex.getMessage(),
					ex );
		}
		return null;
	}

	private boolean cellInQuotes( final String cell )
	{
		if( !quoteEnded() || ( !inputDelim.equals( TAB_DELIM ) && cell.startsWith( "\"" ) && !cell.endsWith( "\"" ) ) )
		{
			return true;
		}

		return false;
	}

	private String getQuotedValue( String cell )
	{
		quotedText = quotedText + cell;
		cell = quotedText;
		if( cell.endsWith( "\"" ) )
		{
			quotedText = "";
		}
		else
		{
			quotedText = quotedText + inputDelim;
		}
		return cell;
	}

	private String getRow( final String line, final boolean isHeader ) throws Exception
	{
		final String[] cells = line.split( inputDelim, -1 );
		int colNum = 1;

		final StringBuffer sb = new StringBuffer();
		for( String cell: cells )
		{
			cell = cell.trim();

			if( cellInQuotes( cell ) )
			{
				cell = getQuotedValue( cell );
				if( !quoteEnded() )
				{
					continue;
				}
			}

			if( isHeader )
			{
				verifyHeader( cell, colNames, colNum );
				if( colNames.isEmpty() )
				{
					cell = MetaUtil.formatMetaId( cell );
				}
				colNames.add( cell );
			}
			else if( cell.isEmpty() )
			{
				cell = Config.requireString( MetaUtil.INPUT_NULL_VALUE );
				Log.out.debug( "====> Set Row#[" + rowNum + "] - Column#[" + colNum + "] = " + cell );
			}

			if( colNum++ > 1 )
			{
				sb.append( TAB_DELIM );
			}

			sb.append( cell );
		}
		rowNum++;
		return sb.toString() + RETURN;
	}

	private boolean quoteEnded()
	{
		if( quotedText.equals( "" ) )
		{
			return true;
		}
		return false;
	}

	private void verifyHeader( final String cell, final List<String> colNames, final int colNum ) throws Exception
	{
		if( cell.isEmpty() )
		{
			throw new Exception(
					"MetaUtil column names must not be null. Column #" + colNum + " must be given a name!" );
		}
		else if( colNames.contains( cell ) )
		{
			int j = 1;
			String dup = null;
			for( final String name: colNames )
			{
				if( name.equals( cell ) )
				{
					dup = name;
					break;
				}
				j++;
			}

			throw new Exception( "MetaUtil file column names must be unique.  Column #" + colNum
					+ " is a duplicate of Column #" + j + " - duplicate name = [" + dup + "]" );
		}
	}

	private final List<String> colNames = new ArrayList<>();

	private String quotedText = "";
	private int rowNum = 0;
	private static final String INPUT_COL_DELIM = "input.columnDelim";

	private static String inputDelim = null;
}