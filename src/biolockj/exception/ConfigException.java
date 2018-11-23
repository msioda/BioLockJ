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

import biolockj.BioLockJ;
import biolockj.Config;

/**
 * ConfigException is the superclass for all BioLockJ configuration file Exceptions used to ensure message uniformity.
 */
public abstract class ConfigException extends Exception
{
	/**
	 * Abstract {@link biolockj.Config} exception calls super to instantiate using the superclass
	 * {@link java.lang.Exception} implementation
	 *
	 * @param msg Exception message details
	 */
	public ConfigException( final String msg )
	{
		super( msg );
	}

	/**
	 * Abstract {@link biolockj.Config} exception calls {@link #buildMessage(String, String, String)} to generate a
	 * standard error message for Configuration file errors, passing an empty string for the msg parameter.
	 *
	 * @param propertyName {@link biolockj.Config} property name
	 * @param type Exception type
	 */
	public ConfigException( final String propertyName, final String type )
	{
		super( buildMessage( propertyName, type, "" ) );
	}

	/**
	 * Abstract {@link biolockj.Config} exception calls {@link #buildMessage(String, String, String)} to generate a
	 * standard error message for Configuration file errors.
	 *
	 * @param propertyName {@link biolockj.Config} property name
	 * @param type Exception type
	 * @param msg Exception message details
	 */
	public ConfigException( final String propertyName, final String type, final String msg )
	{
		super( buildMessage( propertyName, type, msg ) );
	}

	/**
	 * Build a standard error message for Configuration file errors.
	 * 
	 * @param propertyName Property name
	 * @param type Error type
	 * @param msg Exception details
	 * @return Exception message that will be passed to superclass {@link java.lang.Exception} via super()
	 */
	protected static String buildMessage( final String propertyName, String type, String msg )
	{
		if( type != null && !type.isEmpty() )
		{
			type += " " + type;
		}
		if( !msg.isEmpty() )
		{
			msg = msg + BioLockJ.RETURN;
		}

		String val = Config.getString( propertyName );
		if( val == null )
		{
			val = "{undefined}";
		}

		return "Config" + type + " Exception [ " + propertyName + "=" + val + " ] " + BioLockJ.RETURN + msg
				+ "Restart pipeline after updating application inputs or Config " + propertyName + " value in: "
				+ Config.getConfigFileName();
	}

	private static final long serialVersionUID = 3479702562753539290L;
}
