/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Mar 18, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

/**
 * ConfigNotFoundException is thrown if a required {@link biolockj.Config} property is undefined.
 */
public class ConfigNotFoundException extends ConfigException
{
	/**
	 * ConfigNotFoundException is thrown if a required {@link biolockj.Config} property is undefined.
	 *
	 * @param propertyName {@link biolockj.Config} property name
	 */
	public ConfigNotFoundException( final String propertyName )
	{
		super( propertyName, "Not Found" );
	}

	private static final long serialVersionUID = 1677388490031911310L;

}
