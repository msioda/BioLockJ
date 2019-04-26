/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module;

/**
 * Classes that implement this interface are pure Java modules.<br>
 */
public interface JavaModule extends ScriptModule {

    /**
     * This method will create the success flag file so the next module can run.
     *
     * @throws Exception thrown if any runtime error occurs
     */
    public void moduleComplete() throws Exception;

    /**
     * This method will create the fail flag file so the pipeline can move on..
     *
     * @throws Exception thrown if any runtime error occurs
     */
    public void moduleFailed() throws Exception;

    /**
     * This method executes the modules primary Java routine.
     *
     * @throws Exception thrown if any runtime error occurs
     */
    public void runModule() throws Exception;
}
