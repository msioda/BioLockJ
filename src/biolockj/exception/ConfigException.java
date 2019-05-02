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

import biolockj.Config;
import biolockj.Constants;

/**
 * ConfigException is the superclass for all BioLockJ configuration file Exceptions used to ensure message uniformity.
 */
public abstract class ConfigException extends Exception {
	/**
	 * Abstract {@link biolockj.Config} exception calls super to instantiate using the superclass
	 * {@link java.lang.Exception} implementation
	 *
	 * @param msg Exception message details
	 */
	public ConfigException( final String msg ) {
		super( msg );
	}

	/**
	 * Abstract {@link biolockj.Config} exception calls {@link #buildMessage(String, String)} to generate a standard
	 * error message for Configuration file errors, passing an empty string for the msg parameter.
	 *
	 * @param property {@link biolockj.Config} property name
	 * @param msg Exception type
	 */
	public ConfigException( final String property, final String msg ) {
		super( buildMessage( property, msg ) );
	}

	/**
	 * Build a standard error message for Configuration file errors.
	 * 
	 * @param property Property name
	 * @param msg Failure details
	 * @return Exception message that will be passed to superclass {@link java.lang.Exception} via super()
	 */
	protected static String buildMessage( final String property, final String msg ) {
		String val = Config.getString( null, property );
		if( val == null ) {
			val = "{undefined}";
		}
		return "[ " + property + "=" + val + " ] " + Constants.RETURN + msg + Constants.RETURN
			+ "Restart pipeline after updating the property value in: " + Config.getConfigFilePath() + Constants.RETURN
			+ "RESTART SHORTCUT CMD ---> \"blj_rerun " + Config.getConfigFilePath() + "\"";
	}

	private static final long serialVersionUID = 3479702562753539290L;
}
