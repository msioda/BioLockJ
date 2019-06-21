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
package biolockj.module.implicit.parser.wgs;

import biolockj.node.OtuNode;
import biolockj.node.wgs.Kraken2Node;

/**
 * This BioModules parses KrakenClassifier output reports to build standard OTU abundance tables.
 * 
 * @blj.web_desc Kraken2 Parser
 */
public class Kraken2Parser extends KrakenParser {

	/**
	 * Construct a new Kraken2Node
	 * 
	 * @return Kraken2Node
	 */
	@Override
	protected OtuNode getNode( final String id, final String line ) throws Exception {
		return new Kraken2Node( id, line );
	}
}
