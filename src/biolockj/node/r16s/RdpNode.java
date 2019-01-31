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
package biolockj.node.r16s;

import java.util.StringTokenizer;
import biolockj.Config;
import biolockj.Constants;
import biolockj.module.implicit.parser.r16s.RdpParser;
import biolockj.node.OtuNode;
import biolockj.node.OtuNodeImpl;
import biolockj.util.TaxaUtil;

/**
 * This class represents one line of {@link biolockj.module.classifier.r16s.RdpClassifier} output. RDP outputs the
 * sequence header and taxonomy assignments for all levels in one line. Each line represents a single read. Each level
 * is followed by a confidence score ranging from 0-1, which can be obtained via {@link #getScore()}.
 * <p>
 * Sample output line from RDP classifier: <br>
 * FCABK7W:1:2105:21787:12788#/1 Root rootrank 1.0 Bacteria domain 1.0 Firmicutes phylum 1.0 Clostridia class 1.0
 * Clostridiales order 1.0 Ruminococcaceae family 1.0 Faecalibacterium genus 1.0
 */
public class RdpNode extends OtuNodeImpl implements OtuNode
{
	/**
	 * Constructor called one line of RDP output.
	 *
	 * @param id Sample ID
	 * @param line RDP Classifier output line
	 * @throws Exception if propagated from {@link #buildRdpNode(String, String)}
	 */
	public RdpNode( final String id, final String line ) throws Exception
	{
		buildRdpNode( id, line );
	}

	/**
	 * Return lowest RDP confidence percentage (0-100) above the configured threshold value. If no valid levels are
	 * found, return 0.
	 *
	 * @return Integer between 0 and 100
	 */
	public int getScore()
	{
		return score;
	}

	/**
	 * Build the OtuNode by extracting the OTU names for each level from the line. Only OTUs with score above the
	 * threshold are included. Score is set to the lowest score above
	 * {@value biolockj.module.implicit.parser.r16s.RdpParser#RDP_THRESHOLD_SCORE}. If id = null, extract the ID from
	 * the 1st token in the line. Remove single and double quotes if found around OTU name.
	 *
	 * @param id Sample ID
	 * @param line RDP Classifier output line
	 * @throws Exception if required properties are invalid or undefined
	 */
	protected void buildRdpNode( final String id, final String line ) throws Exception
	{
		if( line == null || id == null || line.isEmpty() || id.isEmpty() )
		{
			return;
		}

		final StringTokenizer st = new StringTokenizer( line, Constants.TAB_DELIM );
		setSampleId( id );
		setLine( line );
		setCount( 1 );

		// skip the header, every line has at least 1 token, so next line will never throw Exception
		st.nextToken();

		while( st.hasMoreTokens() )
		{
			String taxa = getTaxaName( st.nextToken() );
			while( st.hasMoreTokens() && ( taxa.equals( "-" ) || taxa.equals( "" ) ) )
			{
				taxa = getTaxaName( st.nextToken() );
			}

			final String level = st.hasMoreTokens() ? st.nextToken().trim(): null;
			final Integer nextScore = st.hasMoreTokens() ? calculateScore( st.nextToken().trim() ): null;

			if( level == null || nextScore == null
					|| nextScore < Config.requirePositiveInteger( RdpParser.RDP_THRESHOLD_SCORE ) )
			{
				return;
			}

			score = nextScore;
			addTaxa( taxa, level );

		}
	}

	/**
	 * Returns default score = 0 if no valid levels are found, otherwise return a score between 1 - 100.
	 *
	 * @return the lowest score above the RDP (between 0 and 100)
	 * @Exception if scoreString has an invalid format
	 */
	private Integer calculateScore( String scoreString ) throws Exception
	{
		if( scoreString.equals( "1" ) || scoreString.equals( "1.0" ) )
		{
			return 100;
		}
		else if( scoreString.equals( "0" ) || scoreString.equals( "0.0" ) )
		{
			return 0;
		}
		else if( !scoreString.startsWith( "0." ) )
		{
			throw new Exception( rangeError( scoreString ) );
		}

		scoreString = scoreString.replace( "0.", "" );
		if( scoreString.length() == 1 )
		{
			scoreString += "0";
		}

		final Integer thisScore = Integer.parseInt( scoreString );
		if( thisScore < 0 || thisScore > 100 )
		{
			throw new Exception( rangeError( scoreString ) );
		}

		return thisScore;
	}

	private String getTaxaName( final String taxa ) throws Exception
	{
		return taxa.replaceAll( "'", "" ).replaceAll( "\"", "" ).trim();
	}

	private String rangeError( final String score ) throws Exception
	{
		return "Invalid RDP confidence score | Required range [ 0.0 <= score <= 1.0 ] ---> Actual score = " + score;
	}

	private int score = 0;

	// Override default taxonomy level delimiters set in OtuNodeImpl
	static
	{
		DOMAIN_DELIM = TaxaUtil.DOMAIN;
		CLASS_DELIM = TaxaUtil.CLASS;
		FAMILY_DELIM = TaxaUtil.FAMILY;
		GENUS_DELIM = TaxaUtil.GENUS;
		ORDER_DELIM = TaxaUtil.ORDER;
		PHYLUM_DELIM = TaxaUtil.PHYLUM;
		SPECIES_DELIM = TaxaUtil.SPECIES;
	}
}
