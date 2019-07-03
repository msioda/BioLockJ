/**
 * @UNCC Fodor Lab
 * @author Ivory Blakley
 * @email ieclabau@uncc.edu
 * @date May 27, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

import biolockj.module.BioModule;

/**
 * ValidationException is thrown at module runtime if the ValidationUtil throws
 * a non-BioLockJ error while validating module output.
 */
public class ValidationExecutionException extends BioLockJException {

	/**
	 * Default error message (not BioModule specific).
	 * 
	 * @param msg Error message
	 */
	public ValidationExecutionException( String msg ) {
		super( msg );
	}
	
	/**
	 * Generate BioModule specific error message.
	 * 
	 * @param module BioModule
	 */
	public ValidationExecutionException( BioModule module ) {
		super( buildMessage( module ) );
	}
	
	private static String buildMessage(BioModule module) {
		return "Error occurred validating the output of module " + module.getModuleDir().getName();
	}

	private static final long serialVersionUID = 9095697645997739998L;

}
