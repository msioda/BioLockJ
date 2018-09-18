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
	 * Build the {@link biolockj.node.OtuNode} if level is configured in:
	 * {@link biolockj.Config}.{@value biolockj.Config#REPORT_TAXONOMY_LEVELS}.
	 *
	 * @param otu Classifier OTU name
	 * @param levelDelim Classifier specific taxonomy level indicator
	 */
	public void buildOtuNode( final String otu, final String levelDelim );

	/**
	 * Gets the OTU count.
	 *
	 * @return Number of reads in node sampleId with node OTU assignment
	 */
	public Long getCount();

	/**
	 * Get the line from classifier output file used to create this OtuNode.
	 *
	 * @return Line of classifier output
	 */
	public String getLine();

	/**
	 * Gets the map holding level-specific OTU names
	 *
	 * @return OTU map (key=level, value=OTU)
	 */
	public Map<String, String> getOtuMap();

	/**
	 * Gets the sample ID to which the read belongs
	 *
	 * @return Sample ID
	 */
	public String getSampleId();

	/**
	 * Print level name and OTU name for each taxonomy level populated.
	 */
	public void report();

	/**
	 * Set the number of reads for a sample ID that have this OTU assignment.
	 *
	 * @param count Number of reads having this OTU assignment
	 */
	public void setCount( Long count );

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
