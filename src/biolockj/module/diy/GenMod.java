/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date June 19, 2017
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module.diy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import biolockj.Config;
import biolockj.Log;
import biolockj.exception.ConfigPathException;
import biolockj.module.ScriptModuleImpl;



/**
 * This BioModule allows users to call in their own scripts into BLJ
 * 
 * @blj.web_desc Allows User made scripts into the BLJ pipeline
 */
public class GenMod extends ScriptModuleImpl {

	@Override
	public List<List<String>> buildScript( final List<File> files ) throws Exception {
		final List<List<String>> data = new ArrayList<>();
		final ArrayList<String> lines = new ArrayList<>();
		lines.add(getLauncher()+ transferScript() + getScriptParams());	
		data.add( lines );
		Log.info(GenMod.class,"Command ran: "+ data);
		return data;
	}
	

	
	@Override
	public void checkDependencies() throws Exception {
		Config.requireExistingFile(this, SCRIPT);	
	}
	
	protected String transferScript() throws ConfigPathException, IOException,Exception{
		File original = Config.requireExistingFile(this, SCRIPT);
		FileUtils.copyFileToDirectory(original, getModuleDir());
		File copy = new File(getModuleDir() + File.separator + original.getName());
		copy.setExecutable(true, false);
		Log.debug(GenMod.class,"Users script saved to: "+ copy.getAbsolutePath());
		return copy.getAbsolutePath();
		
	}
	
	protected String getLauncher() throws Exception{
		String launcher =Config.getString(this, LAUNCHER);
		if ( launcher != null) {
			launcher = Config.getExe(this, "exe." + launcher) +" ";
			Log.debug(GenMod.class,"Launcher used: " + launcher);
		}else{
			launcher="";
			Log.debug(GenMod.class,"No Launcher provided");
			}
		return launcher;
		
	}
	
	protected String getScriptParams() {
		String param =Config.getString(this, PARAM);
		if (param == null) {
			Log.debug(GenMod.class,"No param provided");
			return "";
		}
		
		Log.debug(GenMod.class,"param provided: "+ param);
		return " " + param;

	}
	
	/**
	 * {@value #LAUNCHER} type of Script used by User
	 */
	protected static final String LAUNCHER = "genMod.launcher";
	/**
	 *  {@value #SCRIPT} path to user Script
	 */
	protected static final String SCRIPT ="genMod.scriptPath";
	/**
	 *  {@value #PARAM} parameters for user script
	 */
	protected static final String PARAM = "genMod.param";
}
