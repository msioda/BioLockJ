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

import biolockj.Constants;

/**
 * ConfigException is the superclass for all BioLockJ configuration file Exceptions used to ensure message uniformity.
 */
public class RuntimeParamException extends Exception {

	/**
	 * Print error message originating from {@link biolockj.util.RuntimeParamUtil} and print all runtime args.
	 * 
	 * @param msg Exception message
	 */
	public RuntimeParamException( final String msg ) {
		super( msg );
	}

	/**
	 * Call {@link #buildMessage(String, String, String)} to generate standard error message for
	 * {@link biolockj.util.RuntimeParamUtil} errors. Then print all runtime args.
	 *
	 * @param arg Arg name
	 * @param val Arg value
	 */
	public RuntimeParamException( final String arg, final String val ) {
		super( buildMessage( arg, val, null ) );

	}

	/**
	 * Call {@link #buildMessage(String, String, String)} to generate standard error message for
	 * {@link biolockj.util.RuntimeParamUtil} errorsr.
	 *
	 * @param arg Arg name
	 * @param val Arg value
	 * @param msg Exception message
	 */
	public RuntimeParamException( final String arg, final String val, final String msg ) {
		super( buildMessage( arg, val, msg ) );

	}

	/**
	 * Build error message and print all Java runtime args.
	 * 
	 * @param arg Arg name
	 * @param val Arg value
	 * @param msg Exception message
	 * @return Exception message that will be passed to superclass {@link java.lang.Exception} via super()
	 */
	protected static String buildMessage( final String arg, final String val, final String msg ) {
		final String displayVal = val == null ? "{undefined}": val;
		return "Pipeline failed to start due to invalid runtime parameter: [ " + arg + " = " + displayVal + " ] "
			+ ( msg == null || msg.trim().isEmpty() ? "": Constants.RETURN + msg );
	}

	private static final long serialVersionUID = 4511621216880299923L;
}
