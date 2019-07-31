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
 * Superclass for all BioLockJ exception used to ensure message uniformity.
 */
public class BioLockJException extends Exception {

	/**
	 * Simple default {@link Exception} implementation
	 *
	 * @param msg Exception message details
	 */
	public BioLockJException( final String msg ) {
		super( msg );
	}

	private static final long serialVersionUID = 243456830655360169L;
}
