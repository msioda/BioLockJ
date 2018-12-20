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
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.module.implicit.parser.r16s.RdpParser;
import biolockj.node.OtuNode;
import biolockj.node.OtuNodeImpl;

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
		final StringTokenizer st = new StringTokenizer( line, BioLockJ.TAB_DELIM );
		setSampleId( id );
		setLine( line );
		setCount( 1 );

		st.nextToken(); // skip the header

		while( st.hasMoreTokens() )
		{
			String taxa = st.nextToken().replaceAll( "'", "" ).replaceAll( "\"", "" ).trim();
			while( taxa.trim().equals( "-" ) || taxa.trim().equals( "" ) )
			{
				taxa = st.nextToken().replaceAll( "'", "" ).replaceAll( "\"", "" );
			}

			final String level = st.nextToken();
			final int nextScore = calculateScore( st.nextToken() );

			if( Config.getList( Config.REPORT_TAXONOMY_LEVELS ).contains( level )
					&& nextScore >= Config.requirePositiveInteger( RdpParser.RDP_THRESHOLD_SCORE ) )
			{
				score = nextScore;
				addTaxa( taxa, level );
			}

			// #Code commented out, used to require unbroken taxa chain from top level to current level
			// # for example, if phylum defined but gaps exist before genus defined, genus is ignore due to broken taxa
			// tree
			// if( getValidLevelDelims().contains( level )
			// && ( nextScore >= Config.requirePositiveInteger( Config.RDP_THRESHOLD_SCORE ) ) )
			// {
			// score = nextScore;
			// addOtu( level, taxa );
			// }
			// // all lower levels will fall below RDP threshold - so exit the loop
			// else if( getValidLevelDelims().contains( level ) )
			// {
			// break;
			// }
		}
	}

	/**
	 * Returns default score = 0 if no valid levels are found, otherwise return a score between 1 - 100.
	 *
	 * @return the lowest score above the RDP (between 0 and 100)
	 * @Exception if scoreString has an invalid format
	 */
	private int calculateScore( String scoreString ) throws Exception
	{
		int thisScore = 0;
		scoreString = scoreString.trim();

		if( scoreString.equals( "1" ) || scoreString.equals( "1.0" ) )
		{
			thisScore = 100;
		}
		else
		{
			if( scoreString.equals( "0" ) )
			{
				scoreString = "0.0";
			}

			if( !scoreString.startsWith( "0." ) )
			{
				throw new Exception( "Unexpected score string: " + scoreString );
			}

			scoreString = scoreString.replace( "0.", "" );
			if( !scoreString.equals( "0" ) && scoreString.length() == 1 )
			{
				scoreString = scoreString + "0";
			}

			thisScore = Integer.parseInt( scoreString );
		}

		if( thisScore < 0 || thisScore > 100 )
		{
			throw new Exception( "Unexpected score: " + score );
		}

		return thisScore;
	}

	private int score = 0;

	// Override default taxonomy level delimiters set in OtuNodeImpl
	static
	{
		DOMAIN_DELIM = Config.DOMAIN;
		CLASS_DELIM = Config.CLASS;
		FAMILY_DELIM = Config.FAMILY;
		GENUS_DELIM = Config.GENUS;
		ORDER_DELIM = Config.ORDER;
		PHYLUM_DELIM = Config.PHYLUM;
		SPECIES_DELIM = Config.SPECIES;
	}

	/*
	 * Add the OTU at the given level but only if a valid level returned by getLevels(). OTU names must be added in
	 * order (top to bottom). It is not uncommon for classifiers to omit a level due to incomplete information. If
	 * missing levels are found while traversing the level hierarchy (from top to bottom), the previous level OTU will
	 * be used to fill in any gaps.
	 * 
	 * @param String levelDelim
	 * 
	 * @param String OTU name
	 * 
	 * @Exception thrown if propagated from getValidLevelDelims()
	 * 
	 * @Override public void addOtu( final String levelDelim, final String otu ) { if( ( levelDelim == null ) || ( otu
	 * == null ) || levelDelim.trim().isEmpty() || otu.trim().isEmpty() ) { return; } debug( "\n\n ADD OTU " +
	 * getSampleId() + "> " + levelDelim + ": " + otu + " ||| line = " + getLine() ); String prevOtu = null; for( final
	 * String level: getValidLevelDelims() ) { String levelOtu = getOtuMap().get( level ); if( levelOtu == null ) // OTU
	 * at current level is MISSING { if( level.equals( levelDelim ) ) // Set OTU for current level { debug(
	 * "Adding OTU --> " + levelDelim + " : " + otu ); getOtuMap().put( levelDelim, otu ); break; } else if( prevOtu !=
	 * null ) // OTU above current level exists { // Inherit parent OTU to fill in missing level if(
	 * inheritOtuForSkippedLevels ) { levelOtu = prevOtu; getOtuMap().put( level, prevOtu ); } // Register missing OTU
	 * else { addGap( getLine() ); // if we want to cut off the hierarchy containing gaps, add the break // break; //
	 * return without setting target OTU } } } prevOtu = levelOtu; } }
	 */

	// /**
	// * Used to output special cases, such as only lines containing "Chloroplast".
	// * Can be useful when debugging specific taxonomy issues. To set key value
	// * in this example, populate the class variable DEBUG_KEY="Chloroplast".
	// *
	// * @param String out
	// */
	// private void debug( final String out )
	// {
	// if( doDebug() )
	// {
	// Log.out.warn( out );
	// }
	//
	// }
	//
	// private boolean doDebug()
	// {
	// return ( DEBUG_KEY != null ) && getLine().contains( DEBUG_KEY );
	// }
	//
	// public static Set<String> getGaps()
	// {
	// return gaps;
	// }
	//
	// private static void addGap( final String line ) throws Exception
	// {
	// final StringBuffer sb = new StringBuffer();
	// final String[] cells = line.split( "\\s" );
	// for( final String level: getValidLevelDelims() )
	// {
	// if( line.contains( level ) )
	// {
	// for( int i = 0; i < cells.length; i++ )
	// {
	// final String cell = cells[ i ];
	// if( level.equals( cell ) )
	// {
	// sb.append( level ).append( ": " ).append( cells[ i - 1 ] ).append( " " );
	// }
	// }
	// }
	// else
	// {
	// sb.append( level ).append( ": {MISSING}" ).append( " " );
	// }
	// }
	//
	// gaps.add( sb.toString() );
	// }

	// private static final String DEBUG_KEY = null;
	// private static TreeSet<String> gaps = new TreeSet<>();
	// private static final boolean inheritOtuForSkippedLevels = false;
}
