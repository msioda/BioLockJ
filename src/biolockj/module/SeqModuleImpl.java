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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import biolockj.Log;
import biolockj.util.BioLockJUtil;
import biolockj.util.SeqUtil;

/**
 * Superclass for SeqModules that take sequence files as input for pre-processing prior to classification.
 */
public abstract class SeqModuleImpl extends ScriptModuleImpl implements SeqModule
{
	/**
	 * Return {@link #getSeqFiles(Collection)} to filter standard module input files.
	 *
	 * @throws Exception thrown if any runtime error occurs
	 */
	@Override
	public List<File> getInputFiles() throws Exception
	{
		if( getFileCache().isEmpty() )
		{
			cacheInputFiles( getSeqFiles( findModuleInputFiles() ) );
		}

		return getFileCache();
	}

	@Override
	public List<File> getSeqFiles( final Collection<File> files ) throws Exception
	{
		return SeqUtil.getSeqFiles( files );
	}

	@Override
	public boolean isValidInputModule( final BioModule module )
	{
		return SeqUtil.isSeqModule( module );
	}
}
