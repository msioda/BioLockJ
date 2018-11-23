/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 20, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.Config;

/**
 * This class has a main() method as it is designed to be called as a stand-alone class.<br>
 * This utility updates the JavaDocs to add additional header options.<br>
 * These headers are added to almost every class when the JavaDoc command is run so an automated solution was needed.
 */
public class UpdateJavaDocs
{

	/**
	 * Main method updates the JavaDocs to replace undesirable auto-generated text.
	 * 
	 * @param args Runtime parameters (none)
	 */
	public static void main( final String[] args )
	{
		BufferedReader reader = null;
		try
		{
			final Collection<File> files = FileUtils.listFiles( new File( Config.getSystemFilePath( PATH ) ),
					HiddenFileFilter.VISIBLE, HiddenFileFilter.VISIBLE );
			final List<String> lines = new ArrayList<>();
			for( final File file: files )
			{
				lines.clear();
				System.out.println( "Reading: " + file.getAbsolutePath() );
				reader = BioLockJUtil.getFileReader( file );
				boolean updatedFile = false;
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					if( file.getName().equals( INDEX ) && line.replaceAll( "\"", "" ).equals( BAD_INDEX_TITLE ) )
					{
						System.out.println( "Modify " + file.getAbsolutePath() + " change: " + BAD_INDEX_TITLE );
						lines.add( NEW_INDEX_TITLE );
						updatedFile = true;
					}
					else if( file.getName().equals( OVERVIEW_SUMMARY )
							&& line.replaceAll( "\"", "" ).equals( BAD_OVERVIEW_TITLE ) )
					{
						System.out.println( "Modify " + file.getAbsolutePath() + " change: " + BAD_OVERVIEW_TITLE );
						lines.add( NEW_OVERVIEW_TITLE );
						reader.readLine(); // skips next line
						updatedFile = true;
					}
					else if( line.endsWith( TARGET ) )
					{
						System.out.println( "Modify " + file.getAbsolutePath() + " change: " + TARGET );
						lines.add( line );
						lines.add( NEWLINE_1 );
						lines.add( NEWLINE_2 );
						updatedFile = true;
					}
					else if( line.endsWith( TARGET_2 ) )
					{
						System.out.println( "Modify " + file.getAbsolutePath() + " change: " + TARGET_2 );
						lines.add( line );
						lines.add( NEWLINE_1 );
						lines.add( NEWLINE_2 );
						updatedFile = true;
					}
					else
					{
						lines.add( line );
					}
				}

				if( reader != null )
				{
					reader.close();
				}

				if( updatedFile )
				{
					updateFile( file, lines );
				}
			}

			System.out.println( "JavaDoc Update Successful!" );
		}
		catch( final Exception ex )
		{
			System.out.println( "ERROR: " + ex.getMessage() );
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				if( reader != null )
				{
					reader.close();
				}
			}
			catch( final Exception ex )
			{
				ex.printStackTrace();
			}
		}
	}

	private static void updateFile( final File file, final List<String> lines ) throws Exception
	{
		final String filePath = file.getAbsolutePath();
		FileUtils.forceDelete( file );
		BioLockJUtil.deleteWithRetry( file, 5 );
		System.out.println( "Deleted: " + filePath );
		final BufferedWriter writer = new BufferedWriter( new FileWriter( filePath ) );
		try
		{
			for( final String line: lines )
			{
				writer.write( line + RETURN );
			}
			updatedFiles.add( file.getAbsolutePath() );
			System.out.println( "Created a new: " + filePath );
		}
		finally
		{
			if( writer != null )
			{
				writer.close();
			}
		}
	}

	static final String BAD_INDEX_TITLE = "<title>Generated Documentation (Untitled)</title>";
	static final String BAD_OVERVIEW_TITLE = "<h1 class=title>BioLockJ</h1>";
	static final String INDEX = "index.html";
	static final String NEW_INDEX_TITLE = "<title>BioLockJ Documentation</title>";
	static final String NEW_OVERVIEW_TITLE = "<h1 class=\"title\">BioLockJ API Documentation</h1></div><table border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"Text added after JavaDoc generation\"><tr><th><div style=\"font-weight:500\">&nbsp;&nbsp;&nbsp;&nbsp;Please visit the <a href=\"https://github.com/msioda/BioLockJ/wiki/\" target=\"_top\">BioLockJ Wiki</a> for the general user guide.</div></th></tr></table>";
	static final String NEWLINE_1 = "<li>&nbsp;&nbsp;</li>";
	static final String NEWLINE_2 = "<li><a href=\"https://github.com/msioda/BioLockJ/wiki\" target=\"_top\"><strong>RETURN to WIKI</strong></a></li>";
	static final String OVERVIEW_SUMMARY = "overview-summary.html";
	static final String PATH = "$BLJ/docs/";
	static final String RETURN = "\n";
	static final String TARGET = "help-doc.html\">Help</a></li>";
	static final String TARGET_2 = "<li class=\"navBarCell1Rev\">Help</li>";
	static List<String> updatedFiles = new ArrayList<>();

}
