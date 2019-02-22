/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.parser.r16s;

import java.io.BufferedReader;
import java.io.File;
import biolockj.Config;
import biolockj.Constants;
import biolockj.Log;
import biolockj.module.implicit.parser.ParserModule;
import biolockj.module.implicit.parser.ParserModuleImpl;
import biolockj.node.OtuNode;
import biolockj.node.r16s.RdpNode;
import biolockj.util.BioLockJUtil;
import biolockj.util.MemoryUtil;

/**
 * This BioModule parses RDP output files to build standard OTU abundance tables.
 * 
 * @web_desc RDP Parser
 */
public class RdpParser extends ParserModuleImpl implements ParserModule
{
	/**
	 * Parse all {@link biolockj.module.classifier.r16s.RdpClassifier} reports in the input directory.<br>
	 * Build an {@link biolockj.node.r16s.RdpNode} for each line.<br>
	 * If {@link #isValid(OtuNode)},<br>
	 * <ol>
	 * <li>Create {@link biolockj.node.ParsedSample} for the {@link biolockj.node.r16s.RdpNode#getSampleId()} if not yet
	 * created.
	 * <li>Add the {@link biolockj.node.r16s.RdpNode#getCount()} (1) to {@link biolockj.node.ParsedSample} OTU count.
	 * </ol>
	 * <p>
	 * Sample QIIME report line (head 7A_1_reported.tsv):<br>
	 * FCABK7W:1:2105:21787:12788#/1 Root rootrank 1.0 Bacteria domain 1.0 Firmicutes phylum 1.0 Clostridia class 1.0
	 * Clostridiales order 1.0 Ruminococcaceae family 1.0 Faecalibacterium genus 1.0
	 */
	@Override
	public void parseSamples() throws Exception
	{
		for( final File file: getInputFiles() )
		{
			MemoryUtil.reportMemoryUsage( "Parse " + file.getAbsolutePath() );
			final BufferedReader reader = BioLockJUtil.getFileReader( file );
			try
			{
				for( String line = reader.readLine(); line != null; line = reader.readLine() )
				{
					addOtuNode( new RdpNode( file.getName().replace( Constants.PROCESSED, "" ), line ) );
				}
			}
			finally
			{
				if( reader != null )
				{
					reader.close();
				}
			}
		}
	}

	/**
	 * If {@link biolockj.node.r16s.RdpNode#getScore()} is above the
	 * {@link biolockj.Config}.{@value Constants#RDP_THRESHOLD_SCORE}, continue with the standard
	 * {@link biolockj.node.OtuNode} validation.
	 *
	 * @return true if {@link biolockj.node.OtuNode} is valid
	 */
	@Override
	protected boolean isValid( final OtuNode node )
	{
		try
		{
			if( ( (RdpNode) node ).getScore() >= Config.requirePositiveInteger( this, Constants.RDP_THRESHOLD_SCORE ) )
			{
				return super.isValid( node );
			}
		}
		catch( final Exception ex )
		{
			Log.error( getClass(), "Unable to verify if OTU node is valid! " + ex.getMessage(), ex );
		}
		return false;
	}

	/**
	 * Build the summary message to detail gaps in RDP report OTUs.
	 *
	 * @Override public String getSummary() { final StringBuffer sb = new StringBuffer(); try { int i = 0; for( final
	 * String gap: RdpNode.getGaps() ) { sb.append( "Taxonomy gap[" + ( i++ ) + "]: " + gap + RETURN ); } return
	 * sb.toString() + super.getSummary(); } catch( final Exception ex ) { Log.error( RdpParser.class, "Unable to
	 * produce module summary! " + ex.getMessage(), ex ); } return super.getSummary(); }
	 */

}
