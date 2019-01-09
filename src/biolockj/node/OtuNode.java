/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 2, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.node;

import java.util.Map;

/**
 * Classes that implement this interface store taxonomy assignment info for one sequence, as output by a classifier.
 * {@link biolockj.module.implicit.parser.ParserModule}s read {@link biolockj.module.classifier.ClassifierModule} output
 * to build standardized OTU abundance tables. Each line of classifier output is parsed for the OTU name, OTU count, and
 * sample ID. Note that if data is demultiplexed, the Sample ID is extracted from the file name instead of the sequence
 * header.
 */
public interface OtuNode
{
	/**
	 * Add {@link biolockj.node.OtuNode} taxa names for the
	 * {@link biolockj.Config}.{@value biolockj.util.TaxaUtil#REPORT_TAXONOMY_LEVELS} level mapped by the classifier specific
	 * levelDelim parameter: .
	 * 
	 * @param taxa Classifier Taxa name
	 * @param levelDelim Classifier specific taxonomy level indicator
	 * @throws Exception if errors occur adding the taxa
	 */
	public void addTaxa( final String taxa, final String levelDelim ) throws Exception;

	/**
	 * Get a map of the {@link biolockj.module.classifier.ClassifierModule} taxonomy delimiters to
	 * {@link biolockj.Config}.{@value biolockj.util.TaxaUtil#REPORT_TAXONOMY_LEVELS}
	 * 
	 * @return Map(delim, level) map
	 */
	public Map<String, String> delimToLevelMap();

	/**
	 * Gets the OTU count.
	 *
	 * @return Number of reads in node sampleId with node OTU assignment
	 */
	public int getCount();

	/**
	 * Get the line from classifier output file used to create this OtuNode.
	 *
	 * @return Line of classifier output
	 */
	public String getLine();

	/**
	 * Build the OTU name from the taxaMap.
	 * 
	 * @return OTU name
	 */
	public String getOtuName();

	/**
	 * Gets the sample ID to which the read belongs
	 *
	 * @return Sample ID
	 */
	public String getSampleId();

	/**
	 * Gets the map holding level-specific OTU names
	 *
	 * @return OTU map (key=level, value=OTU)
	 * @throws Exception if errors occur building map
	 */
	public Map<String, String> getTaxaMap() throws Exception;

	/**
	 * Set the number of reads for a sample ID that have this OTU assignment.
	 *
	 * @param count Number of reads having this OTU assignment
	 */
	public void setCount( int count );

	/**
	 * Set the classifier report line parsed to build this OTU node.
	 *
	 * @param line Classifier report line
	 */
	public void setLine( String line );

	/**
	 * Set the sample ID, parsed from file name or sequence header, to which this taxonomy assignment belongs.
	 *
	 * @param sampleId ID of sample to which this taxonomy assignment belongs
	 */
	public void setSampleId( final String sampleId );
}
