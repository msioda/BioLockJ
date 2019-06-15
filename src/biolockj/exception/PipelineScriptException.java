/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Mar 03, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

import biolockj.module.ScriptModule;

/**
 * PipelineScriptException is thrown by {@link biolockj.util.BashScriptBuilder} if errors occur writing MASTER & WORKER
 * scripts.
 */
public class PipelineScriptException extends Exception {

	/**
	 * Create standard error to throw writing MASTER & WORKER bash scripts.
	 *
	 * @param module ScriptModule
	 * @param isWorker If the error is thrown while writing a worker script
	 * @param msg Exception message details
	 */
	public PipelineScriptException( final ScriptModule module, final boolean isWorker, final String msg ) {
		super( "Error writing " + ( isWorker ? "WORKER": "MAIN" ) + " script for: " + module.getClass().getName() +
			" --> " + msg );
	}

	/**
	 * Create standard error to throw if problems occur generating pipeline bash scripts.
	 *
	 * @param module ScriptModule
	 * @param msg Exception message details
	 */
	public PipelineScriptException( final ScriptModule module, final String msg ) {
		super( "Error writing script for: " + module.getClass().getName() + " --> " + msg );
	}

	/**
	 * Create a standard exception message.
	 *
	 * @param msg Exception message details
	 */
	public PipelineScriptException( final String msg ) {
		super( msg );
	}

	private static final long serialVersionUID = 3153279611111591414L;

}
