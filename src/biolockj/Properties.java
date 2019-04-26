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
import biolockj.exception.ConfigPathException;
import biolockj.util.BioLockJUtil;
import biolockj.util.DockerUtil;

/**
 * Load properties defined in the BioLockJ configuration file, including inherited properties from project.defaultProps
 */
public class Properties extends java.util.Properties {

	/**
	 * Default constructor.
	 */
	public Properties() {
		super();
	}

	/**
	 * Constructor called when {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS} contains a valid file-path
	 *
	 * @param defaultConfig Config built from {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS} property
	 */
	public Properties( final Properties defaultConfig ) {
		super( defaultConfig );
	}

	/**
	 * Load properties, adding escape characters where necessary.
	 *
	 * @param fis FileInputStream
	 * @throws IOException if unable to convert escape characters
	 */
	protected void load( final FileInputStream fis ) throws IOException {
		final Scanner in = new Scanner( fis );
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		while( in.hasNext() ) {
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
	public static Properties loadProperties( final File file ) throws Exception {
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
		throws FileNotFoundException, IOException {
		if( propFile.exists() ) {
			if( defaultProps == null ) Log.info( Properties.class, "LOAD CONFIG[ #"+ loadOrder++  +" ]: ---> " + propFile.getAbsolutePath() );
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
	 * @param propFile BioLockJ Configuration file
	 * @return {@link biolockj.Properties} instance
	 * @throws Exception if errors occur
	 */
	protected static Properties buildConfig( final File propFile ) throws Exception {
		final File defaultConfig = getDefaultConfig( propFile );
		if( defaultConfig == null ) {
			final File basicConfig = getBasicConfig();
			if( basicConfig != null ) return readProps( propFile, buildConfig( basicConfig ) );
			return readProps( propFile, null );
		}

		return readProps( propFile, buildConfig( defaultConfig ) );
	}

	/**
	 * Recursive method handles nested default Config files. Default props are overridden by parent level props.
	 *
	 * @return {@link biolockj.Properties} instance
	 * @throws Exception if errors occur
	 */
	protected static File getBasicConfig() throws Exception {
		File basicConfig = null;
		if( DockerUtil.inDockerEnv() ) {
			basicConfig = Config.getExistingFileObject( DOCKER_CONFIG_PATH );
			if( basicConfig == null || !basicConfig.isFile() || defaultConfigFiles.contains( basicConfig ) ) {
				basicConfig = null;
			}
		}
		if( basicConfig == null ) {
			basicConfig = Config.getExistingFileObject( STANDARD_CONFIG_PATH );
			if( basicConfig == null || !basicConfig.isFile() || defaultConfigFiles.contains( basicConfig ) ) {
				basicConfig = null;
			}
		}
		if( basicConfig != null ) {
			defaultConfigFiles.add( basicConfig );
			Log.info( Properties.class, "Adding Basic Config: " + basicConfig.getAbsolutePath() );
			return basicConfig;
		}
		return null;
	}
	
	

	/**
	 * Parse property to find default config property: {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS}.<br>
	 * 
	 * @param propFile BioLockJ Config file
	 * @return nested default prop file or null
	 * @throws Exception if errors occur
	 */
	protected static File getDefaultConfig( final File propFile ) throws Exception {
		File defaultConfig = null;
		final Properties defaultProps = readProps( propFile, null );
		if( defaultProps != null && !defaultProps.isEmpty() ) {
			final Object defaultConfigPath = defaultProps.get( Constants.PIPELINE_DEFAULT_PROPS );
			if( defaultConfigPath != null && !( (String) defaultConfigPath ).isEmpty() ) {
				defaultConfig = getLocalFile( (String) defaultConfigPath );
			}
		}
		if( defaultConfig == null || defaultConfigFiles.contains( defaultConfig ) ) return null;
		if( !defaultConfig.isFile() )
			throw new ConfigPathException( Constants.PIPELINE_DEFAULT_PROPS, ConfigPathException.FILE );
		defaultConfigFiles.add( defaultConfig );
		return defaultConfig;
	}

	private static List<String> getListedModules() throws Exception {
		final List<String> modules = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( configFile );
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				if( line.startsWith( Constants.BLJ_MODULE_TAG ) ) {
					final String moduleName = line.trim().substring( line.indexOf( " " ) + 1 );
					Log.info( Properties.class, "Detected configured BioModule: " + moduleName );
					modules.add( moduleName );
				}
			}
		} finally {
			reader.close();
		}

		return modules;
	}

	private static File getLocalFile( final String path ) throws Exception {
		if( DockerUtil.inDockerEnv() && !path.equals( DOCKER_CONFIG_PATH ) && !path.equals( STANDARD_CONFIG_PATH ) )
			return new File( DockerUtil.getDockerVolumePath( path, DockerUtil.CONTAINER_CONFIG_DIR ) );
		return new File( Config.getSystemFilePath( path ) );
	}

	private static int loadOrder = 0;
	private static File configFile = null;
	private static Set<File> defaultConfigFiles = new HashSet<>();
	private static final String DOCKER_CONFIG_PATH = "$BLJ/resources/config/default/docker.properties";
	private static Properties props = null;
	private static final long serialVersionUID = 2980376615128441545L;
	private static final String STANDARD_CONFIG_PATH = "$BLJ/resources/config/default/standard.properties";

}
