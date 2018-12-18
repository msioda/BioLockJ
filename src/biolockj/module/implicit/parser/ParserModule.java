/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Sep 22, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.implicit.parser;

import java.io.File;
import biolockj.module.JavaModule;
import biolockj.node.ParsedSample;

/**
 * This interface defines the required methods to parse ClassifierModule output. Each BioLockJ ClassifierModule
 * implementation must have a corresponding ParserModule that understands the classifier output file format to extract
 * the OTUs assigned to each sample. Parser BioModules read ClassifierModule output to build a single OTU count files
 * for each sample using a standard format.
 */
public interface ParserModule extends JavaModule
{
	/**
	 * Each sample with taxonomic assignments from a {@link biolockj.module.classifier.ClassifierModule} is parsed by
	 * {@link #parseSamples()} to produce a {@link biolockj.node.ParsedSample}. As each file is processed, this method
	 * is called to add the {@link biolockj.node.ParsedSample} to the Set returned by {@link #getParsedSamples()}
	 *
	 * @param parsedSample ParsedSample
	 * @throws Exception if unable to add the parsed sample
	 */
	public void addParsedSample( ParsedSample parsedSample ) throws Exception;

	/**
	 * After {@link #parseSamples()} completes, this method builds the OTU count files.
	 *
	 * @throws Exception if error occurs while building OTU tables
	 */
	public void buildOtuCountFiles() throws Exception;

	/**
	 * Get the name of the OTU count file output for the given sampleId
	 * 
	 * @param sampleId Sample ID
	 * @return OTU count file
	 * @throws Exception if errors occur
	 */
	public File getOtuCountFile( String sampleId ) throws Exception;

	/**
	 * After {@link #parseSamples()} completes, this method can be called to get a {@link biolockj.node.ParsedSample} by
	 * its sample ID.
	 *
	 * @param sampleId Sample ID
	 * @return ParsedSample with the given sampleId
	 */
	public ParsedSample getParsedSample( String sampleId );

	/**
	 * Parse {@link biolockj.module.classifier.ClassifierModule} output to build {@link biolockj.node.ParsedSample}s.
	 *
	 * @throws Exception if error occurs while parsing classifier reports
	 */
	public void parseSamples() throws Exception;

}
