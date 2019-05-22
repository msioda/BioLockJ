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
import biolockj.exception.ConfigNotFoundException;
import biolockj.exception.ConfigPathException;

/**
 * Interface for BioModules that use a reference database that is used by the DockerUtil to find the correct database
 * directory to map to the container /db volume.
 */
public interface DatabaseModule extends BioModule {

	/**
	 * Return database directory, if multiple databases are configured, they must share a common parent directory and
	 * the common parent directory is returned by this method.
	 *
	 * @return Database directory
	 * @throws ConfigPathException if path is defined but does not exists
	 * @throws ConfigNotFoundException if DB property is undefined and the default DB is not included in the module
	 * runtime env
	 */
	public File getDB() throws ConfigPathException, ConfigNotFoundException;

}
