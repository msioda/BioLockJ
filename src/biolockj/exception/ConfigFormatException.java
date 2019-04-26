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
 * ConfigFormatException is thrown if property is defined but the format is invalid.
 */
public class ConfigFormatException extends ConfigException {
    /**
     * ConfigFormatException is thrown if property is defined but the format is invalid.
     *
     * @param property {@link biolockj.Config} property name
     * @param msg Exception message details
     */
    public ConfigFormatException( final String property, final String msg ) {
        super( property, "Current value \"" + Config.getString( null, property )
            + "\" does not meet format requirements!" + Constants.RETURN + msg );
    }

    private static final long serialVersionUID = -5659243602699272132L;
}
