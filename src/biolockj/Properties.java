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

import java.io.*;
import java.util.*;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;
import biolockj.util.RuntimeParamUtil;

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
	 * Constructor called when {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS} contains a valid file-path
	 *
	 * @param defaultConfig Config built from {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS} property
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
			out.write( Constants.RETURN.getBytes() );
		}
		in.close();
		final InputStream is = new ByteArrayInputStream( out.toByteArray() );
		super.load( is );
	}

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
		props.setProperty( Constants.INTERNAL_BLJ_MODULE, BioLockJUtil.getCollectionAsString( getListedModules() ) );
		props.setProperty( Constants.INTERNAL_DEFAULT_CONFIG,
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
	 * @throws Exception if errors occur
	 */
	protected static Properties buildConfig( final File configFile ) throws Exception
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
	 * Parse property file for the property {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS}.<br>
	 * 
	 * @param configFile BioLockJ Config file
	 * @return nested default prop file or null
	 * @throws Exception if errors occur
	 */
	protected static File getDefaultConfigProp( final File configFile ) throws Exception
	{

		// No more nested default Config files
		// If running Docker, add the Docker Config...
		if( defaultConfigFiles.isEmpty() && RuntimeParamUtil.isDockerMode() )
		{
			Log.info( Properties.class, "Import Docker Config: " + DOCKER_CONFIG_PATH );
			return Config.getExistingFileObject( DOCKER_CONFIG_PATH );
		}

		final BufferedReader reader = BioLockJUtil.getFileReader( configFile );
		Log.info( Properties.class, "Import Config: " + configFile.getAbsolutePath() );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				final StringTokenizer st = new StringTokenizer( line, "=" );
				if( st.countTokens() > 1 )
				{
					if( st.nextToken().trim().equals( Constants.PIPELINE_DEFAULT_PROPS ) )
					{
						final String filePath = st.nextToken().trim();
						Log.info( Properties.class, "Import Default Config: " + filePath );
						File defaultConfig = null;
						if( RuntimeParamUtil.isDockerMode() && !filePath.endsWith( "docker.properties" )
								&& !filePath.endsWith( "standard.properties" ) )
						{
							Log.info( Properties.class, "Replace Default Config path with Docker /config path: " );
							defaultConfig = DockerUtil.getDockerVolumeFile( Constants.PIPELINE_DEFAULT_PROPS,
									DockerUtil.CONTAINER_CONFIG_DIR );
							Log.info( Properties.class, "New Default Config path: " + defaultConfig );
						}
						else
						{
							defaultConfig = Config.getExistingFileObject( filePath );
						}

						reader.close();
						if( !defaultConfigFiles.contains( defaultConfig ) )
						{
							return defaultConfig;
						}

						return null;
					}
				}
			}
		}
		finally
		{
			if( reader != null )
			{
				reader.close();
			}
		}

		return null;
	}

	private static List<String> getListedModules() throws Exception
	{
		final List<String> modules = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( configFile );
		try
		{
			for( String line = reader.readLine(); line != null; line = reader.readLine() )
			{
				if( line.startsWith( Constants.INTERNAL_BLJ_MODULE ) )
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

	private static File configFile = null;

	private static List<File> defaultConfigFiles = new ArrayList<>();

	/**
	 * Path to Docker Config file: {@value #DOCKER_CONFIG_PATH}<br>
	 * Sets Docker properties as the default config.
	 */
	private static final String DOCKER_CONFIG_PATH = "$BLJ/resources/config/default/docker.properties";
	private static Properties props = null;
	private static final long serialVersionUID = 2980376615128441545L;

}
