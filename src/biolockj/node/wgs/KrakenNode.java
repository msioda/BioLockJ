/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 16, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.node.wgs;

import java.util.StringTokenizer;
import biolockj.BioLockJ;
import biolockj.Constants;
import biolockj.Log;
import biolockj.node.OtuNode;
import biolockj.node.OtuNodeImpl;

/**
 * This class represents one line of {@link biolockj.module.classifier.wgs.KrakenClassifier} output. Kraken outputs the
 * sequence header and taxonomy assignments for all levels in one line. Each line represents a single read.
 * <p>
 * Sample mpa-output line from Kraken: <br>
 * FCC6MMAACXX:8:1101:1968:2100#GTATTCTC/1
 * d__Bacteria|p__Bacteroidetes|c__Bacteroidia|o__Bacteroidales|f__Bacteroidaceae|g__Bacteroides|s__Bacteroides_vulgatus
 */
public class KrakenNode extends OtuNodeImpl implements OtuNode
{

	/**
	 * Constructor called for each line of Kraken output.<br>
	 * Builds the OtuNode by extracting the OTU names for each taxonomy level found in the line.
	 *
	 * @param id Sample ID
	 * @param line Kraken mpa-output line
	 * @throws Exception if required properties are invalid or undefined
	 */
	public KrakenNode( final String id, final String line ) throws Exception
	{
		final StringTokenizer st = new StringTokenizer( line, Constants.TAB_DELIM );
		if( st.countTokens() == 2 )
		{
			st.nextToken(); // skip the header
			setSampleId( id );
			setLine( line );
			setCount( 1 );

			final StringTokenizer taxas = new StringTokenizer( st.nextToken(), KRAKEN_DELIM );
			while( taxas.hasMoreTokens() )
			{
				final String token = taxas.nextToken();
				final String levelDelim = token.substring( 0, 3 );
				final String taxa = token.substring( 3 );
				addTaxa( taxa, delimToLevelMap().get( levelDelim ) );
			}
		}
		else
		{
			while( st.hasMoreTokens() )
			{
				Log.warn( getClass(), "Extra Kraken token [ more than expected 2! ]: " + st.nextToken() );
			}

			throw new Exception( "Invalid Record = (" + ( Log.doDebug() ? line: id ) + ")" + BioLockJ.RETURN
					+ "Kraken output must have exactly 2 tab delimited columns per line. " );
		}
	}

	private static final String KRAKEN_DELIM = "\\|";
}
