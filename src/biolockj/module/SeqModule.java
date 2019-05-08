/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jan 20, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Classes that implement this interface requires sequence files for input.<br>
 */
public interface SeqModule extends ScriptModule {

	/**
	 * Return only sequence files for sample IDs found in the metadata file.<br>
	 * If {@link biolockj.Config}.{@value biolockj.util.MetaUtil#META_REQUIRED} = {@value biolockj.Constants#TRUE}, an
	 * error is thrown to list the files that cannot be matched to a metadata row.
	 * 
	 * @param files Module input files
	 * @return Module sequence files
	 * @throws Exception if no input files are found
	 */
	public List<File> getSeqFiles( Collection<File> files ) throws Exception;

}
