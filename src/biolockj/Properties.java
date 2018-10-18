/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Jun 16, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import biolockj.exception.ConfigPathException;
import biolockj.util.BioLockJUtil;
import biolockj.util.RuntimeParamUtil;
import biolockj.util.SeqUtil;

/**
 * Load properties defined in the BioLockJ configuration file, including inherited properties from project.defaultProps
 */
public class Properties extends java.util.Properties
{

	/**
	 * Default constructor.
	 */
	public Properties()
	{
		super();
	}

	/**
	 * Constructor called when {@value biolockj.Config#PROJECT_DEFAULT_PROPS} contains a valid file-path
	 *
	 * @param defaultConfig Config built from {@value biolockj.Config#PROJECT_DEFAULT_PROPS} property
	 */
	public Properties( final Properties defaultConfig )
	{
		super( defaultConfig );
	}

	/**
	 * Load properties, adding escape characters where necessary.
	 *
	 * @param fis FileInputStream
	 * @throws IOException if unable to convert escape characters
	 */
	protected void load( final FileInputStream fis ) throws IOException
	{
		final Scanner in = new Scanner( fis );
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		while( in.hasNext() )
		{
			out.write( in.nextLine().replace( "\\", "\\\\" ).getBytes() );
			out.write( BioLockJ.RETURN.getBytes() );
		}
		in.close();
		final InputStream is = new ByteArrayInputStream( out.toByteArray() );
		super.load( is );
	}

	/**
	 * Copy the Config files to the pipeline root directory.
	 *
	 * @throws ConfigNotFoundException is pipeline root directory has not been set
	 * @throws ConfigPathException is pipeline root directory is defined, but the path is invalid
	 * @throws IOException if {@link org.apache.commons.io.FileUtils} is unable to copy the configFile
	 *
	 * public static void copyConfigToPipelineRootDir() throws ConfigNotFoundException, ConfigPathException, IOException
	 * { final File projectDir = Config.requireExistingDir( Config.INTERNAL_PIPELINE_DIR ); if( defaultConfigFiles !=
	 * null && !defaultConfigFiles.isEmpty() ) { Config.setConfigProperty( Config.INTERNAL_DEFAULT_CONFIG,
	 * defaultConfigFiles ); for( final File f: defaultConfigFiles ) { FileUtils.copyFileToDirectory( f, projectDir ); }
	 * }
	 * 
	 * final File newConfigFile = new File( projectDir.getAbsolutePath() + File.separator + configFile.getName() ); if(
	 * !configFile.equals( newConfigFile ) ) { FileUtils.copyFileToDirectory( configFile, projectDir ); configFile =
	 * newConfigFile; } }
	 */

	/**
	 * Instantiate {@link biolockj.Properties} via {@link #buildConfig(File)}
	 *
	 * @param file of {@link biolockj.Properties} file
	 * @return Properties Config properties loaded from files
	 * @throws Exception if unable to extract properties from filePath
	 */
	public static Properties loadProperties( final File file ) throws Exception
	{
		Log.debug( Properties.class, "Run loadProperties for Config: " + file.getAbsolutePath() );
		configFile = file;
		props = buildConfig( configFile );
		props.setProperty( Config.INTERNAL_BLJ_MODULE, BioLockJUtil.getCollectionAsString( getListedModules() ) );
		props.setProperty( Config.INTERNAL_DEFAULT_CONFIG,
				BioLockJUtil.getCollectionAsString( BioLockJUtil.getFilePaths( defaultConfigFiles ) ) );
		return props;
	}

	/**
	 * Read the properties defined in the required propFile and defaultProps (if included) to build Config.<br>
	 * Properties in propFile will override the defaultProps.
	 *
	 * @param propFile BioLockJ configuration file
	 * @param defaultProps Default BioLockJ configuration file
	 * @return {@link biolockj.Properties} instance
	 * @throws FileNotFoundException thrown if propFile is not a valid file path
	 * @throws IOException thrown if propFile or defaultProps cannot be parsed to read in properties
	 */
	public static Properties readProps( final File propFile, final Properties defaultProps )
			throws FileNotFoundException, IOException
	{
		if( propFile.exists() )
		{
			final FileInputStream in = new FileInputStream( propFile );
			final Properties tempProps = defaultProps == null ? new Properties(): new Properties( defaultProps );
			tempProps.load( in );
			in.close();
			return tempProps;
		}

		return null;
	}

	/**
	 * Recursive method handles nested default Config files. Default props are overridden by parent level props.
	 *
	 * @param configFile BioLockJ Configuration file
	 * @return {@link biolockj.Properties} instance
	 * @throws FileNotFoundException thrown if configFile is not a valid file
	 * @throws IOException thrown if configFile or defaultConfig cannot be parsed to read in properties
	 * @throws ConfigPathException if configFile or defaultConfig are defined, but not a valid file path
	 */
	protected static Properties buildConfig( final File configFile )
			throws FileNotFoundException, IOException, ConfigPathException
	{
		Log.debug( Properties.class, "Run buildConfig for Config: " + configFile.getAbsolutePath() );
		final File defaultConfig = getDefaultConfigProp( configFile );
		if( defaultConfig == null )
		{
			// Log.debug( Properties.class, "Found the base Config file: " + configFile.getAbsolutePath() );
			return readProps( configFile, null );
		}
		else
		{
			defaultConfigFiles.add( defaultConfig );
			return readProps( configFile, buildConfig( defaultConfig ) );
		}
	}

	/**
	 * Parse property file for the property {@value biolockj.Config#PROJECT_DEFAULT_PROPS}.<br>
	 * project.defaultProps=/app/biolockj_v1.0/resources/config/default/docker.properties
	 * 
	 * @param configFile BioLockJ Config file
	 * @return nested default prop file or null
	 * @throws FileNotFoundException thrown if configFile is not a valid file
	 * @throws IOException thrown if configFile or defaultConfig cannot be parsed to read in properties
	 * @throws ConfigPathException if the defaultConfig property is defined, but is not a valid file path
	 */
	protected static File getDefaultConfigProp( final File configFile )
			throws FileNotFoundException, IOException, ConfigPathException
	{
		final BufferedReader reader = SeqUtil.getFileReader( configFile );
		for( String line = reader.readLine(); line != null; line = reader.readLine() )
		{
			final StringTokenizer st = new StringTokenizer( line, "=" );
			if( st.countTokens() > 1 )
			{
				if( st.nextToken().trim().equals( Config.PROJECT_DEFAULT_PROPS ) )
				{
					final File defaultConfig = Config.getExistingFileObject( st.nextToken().trim() );
					reader.close();
					if( !defaultConfigFiles.contains( defaultConfig ) )
					{
						return defaultConfig;
					}

					return null;
				}
			}
		}
		reader.close();

		// Log.debug( Properties.class, "No default Config found in: " + configFile.getAbsolutePath() );

		// No more nested default Config files
		// If running Docker, add the Docker Config...
		if( RuntimeParamUtil.isDockerMode()
				&& !defaultConfigFiles.contains( Config.getExistingFileObject( DOCKER_CONFIG_PATH ) ) )
		{
			Log.info( Properties.class, "Import Docker Config: " + DOCKER_CONFIG_PATH );
			return Config.getExistingFileObject( DOCKER_CONFIG_PATH );
		}

		return null;
	}

	private static List<String> getListedModules() throws Exception
	{
		final List<String> modules = new ArrayList<>();
		final BufferedReader reader = SeqUtil.getFileReader( configFile );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( line.startsWith( Config.INTERNAL_BLJ_MODULE ) )
				{
					final String moduleName = line.trim().substring( line.indexOf( " " ) + 1 );
					Log.info( Properties.class, "Found configured BioModule: " + moduleName );
					modules.add( moduleName );
				}
			}
		}
		finally
		{
			reader.close();
		}

		return modules;
	}

	/**
	 * Path to Docker Config file: {@value #DOCKER_CONFIG_PATH}<br>
	 * Sets Docker properties as the default config.
	 */
	protected static final String DOCKER_CONFIG_PATH = "$BLJ/resources/config/default/docker.properties";

	private static File configFile = null;

	private static List<File> defaultConfigFiles = new ArrayList<>();
	private static Properties props = null;
	private static final long serialVersionUID = 2980376615128441545L;

}
