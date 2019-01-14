/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 20, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.seq;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.Config;
import biolockj.Log;
import biolockj.module.ScriptModule;
import biolockj.module.ScriptModuleImpl;
import biolockj.util.SeqUtil;

/**
 * This BioModule uses awk and gzip to convert input sequence files into a decompressed fasta file format.
 */
public class AwkFastaConverter extends ScriptModuleImpl implements ScriptModule
{

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception
	{
		final List<List<String>> data = new ArrayList<>();
		final boolean isMultiLine = Config.getBoolean( Config.INTERNAL_IS_MULTI_LINE_SEQ );
		final String tempDir = getTempDir().getAbsolutePath() + File.separator;
		final String outDir = getOutputDir().getAbsolutePath() + File.separator;
		// final String ext = "." + Config.requireString( SeqUtil.INTERNAL_SEQ_TYPE );
		final String ext = "." + ( isMultiLine ? SeqUtil.FASTA: Config.requireString( SeqUtil.INTERNAL_SEQ_TYPE ) );
		for( final File f: files )
		{
			final ArrayList<String> lines = new ArrayList<>();
			final String fileId = SeqUtil.getSampleId( f.getName() );
			final String dirExt = SeqUtil.getReadDirectionSuffix( f );

			String filePath = f.getAbsolutePath();

			if( SeqUtil.isGzipped( f.getName() ) )
			{
				filePath = ( SeqUtil.isFastQ() || isMultiLine ? tempDir: outDir ) + fileId + dirExt + ext;
				lines.add( unzip( f, filePath ) );
			}

			if( Config.getBoolean( Config.INTERNAL_IS_MULTI_LINE_SEQ ) )
			{
				lines.add( convert454( filePath, fileId + dirExt, outDir ) );
			}
			else if( SeqUtil.isFastQ() )
			{
				lines.add( convert2fastA( filePath, fileId + dirExt, outDir ) );
			}

			if( !SeqUtil.isGzipped( f.getName() ) && SeqUtil.isFastA() )
			{
				Log.warn( getClass(), "Remove this BioModule from Config:  "
						+ " Files are already in decompressed FastA format!  It is unnecessary to make duplicate files..." );
				lines.add( copyToOutputDir( filePath, fileId + dirExt + ext ) );
			}

			data.add( lines );
		}

		return data;
	}

	/**
	 * Set {@link biolockj.Config}.{@value biolockj.util.SeqUtil#INTERNAL_SEQ_TYPE} =
	 * {@value biolockj.util.SeqUtil#FASTA}<br>
	 * Set {@link biolockj.Config}.{@value biolockj.util.SeqUtil#INTERNAL_SEQ_HEADER_CHAR} =
	 * {@link biolockj.util.SeqUtil#FASTA_HEADER_DEFAULT_DELIM}
	 * 
	 * @throws Exception if errors occur
	 */
	@Override
	public void cleanUp() throws Exception
	{
		super.cleanUp();
		Config.setConfigProperty( SeqUtil.INTERNAL_SEQ_TYPE, SeqUtil.FASTA );
		Config.setConfigProperty( SeqUtil.INTERNAL_SEQ_HEADER_CHAR, SeqUtil.FASTA_HEADER_DEFAULT_DELIM );
	}

	/**
	 * This method generates the required bash functions used by the module scripts.
	 * <ul>
	 * <li>{@value #FUNCTION_CONVERT_TO_FASTA}
	 * <li>{@value #FUNCTION_GUNZIP}
	 * </ul>
	 */
	@Override
	public List<String> getWorkerScriptFunctions() throws Exception
	{
		final List<String> lines = super.getWorkerScriptFunctions();
		lines.add( "function " + FUNCTION_CONVERT_TO_FASTA + "() {" );
		lines.add( "cat $1 | " + Config.getExe( Config.EXE_AWK )
				+ " '{ if(NR%4==1) { printf( \">%s \\n\",substr($0,2) ); } else if(NR%4==2) print; }' > $2 " );
		lines.add( "}" );
		if( hasGzipped() )
		{
			lines.add( "function " + FUNCTION_GUNZIP + "() {" );
			lines.add( Config.getExe( Config.EXE_GZIP ) + " -cd $1 > $2" );
			lines.add( "}" );
		}

		if( Config.getBoolean( Config.INTERNAL_IS_MULTI_LINE_SEQ ) )
		{
			lines.add( "function " + FUNCTION_CONVERT_454 + "() {" );
			lines.add( "cat $1 | " + Config.getExe( Config.EXE_AWK )
					+ " '{if(substr($0,0,1) == \">\" ) { printf(\"\\n%s\\n\", $0); } else printf;}' > $2 " );
			lines.add( "}" );
		}
		return lines;
	}

	/**
	 * Build script line to decompress gzipped sequence file.
	 * 
	 * @param file Gzipped file
	 * @param targetPath Output file name
	 * @return Bash Script line to gunzip file
	 */
	protected String unzip( final File file, final String targetPath )
	{
		return FUNCTION_GUNZIP + " " + file.getAbsolutePath() + " " + targetPath;
	}

	private String convert2fastA( final String filePath, final String fileId, final String outDir ) throws Exception
	{
		return FUNCTION_CONVERT_TO_FASTA + " " + filePath + " " + outDir + fileId + "." + SeqUtil.FASTA;
	}

	private String convert454( final String filePath, final String fileId, final String outDir ) throws Exception
	{
		return FUNCTION_CONVERT_454 + " " + filePath + " " + outDir + fileId + "." + SeqUtil.FASTA;
	}

	private String copyToOutputDir( final String source, final String target ) throws Exception
	{
		return "cp " + source + " " + getOutputDir().getAbsolutePath() + File.separator + target;
	}

	private boolean hasGzipped() throws Exception
	{
		for( final File f: getInputFiles() )
		{
			if( SeqUtil.isGzipped( f.getName() ) )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Name of the bash function used to conver 454 format to BioLockJ friendly Illumina format:
	 * {@value #FUNCTION_CONVERT_454}
	 */
	protected static final String FUNCTION_CONVERT_454 = "convert454";

	/**
	 * Name of the bash function that converts the file format to Fasta: {@value #FUNCTION_CONVERT_TO_FASTA}
	 */
	protected static final String FUNCTION_CONVERT_TO_FASTA = "convertToFastA";

	/**
	 * Name of the bash function used to decompress gzipped files: {@value #FUNCTION_GUNZIP}
	 */
	protected static final String FUNCTION_GUNZIP = "decompressGzip";

}
