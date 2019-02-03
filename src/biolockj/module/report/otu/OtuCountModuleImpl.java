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
package biolockj.module.report.otu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import biolockj.module.BioModule;
import biolockj.module.JavaModuleImpl;
import biolockj.util.OtuUtil;

/**
 * TBD
 */
public abstract class OtuCountModuleImpl extends JavaModuleImpl implements OtuCountModule
{

	@Override
	public List<File> getInputFiles() throws Exception
	{
		if( getFileCache().isEmpty() )
		{
			final List<File> files = new ArrayList<>();
			for( final File f: super.findModuleInputFiles() )
			{
				if( OtuUtil.isOtuFile( f ) )
				{
					files.add( f );
				}
			}
			cacheInputFiles( files );
		}
		return getFileCache();
	}

	@Override
	public boolean isValidInputModule( final BioModule module )
	{
		return OtuUtil.isOtuModule( module );
	}

}
