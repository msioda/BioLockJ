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
		final Properties props = buildConfig( file );
		props.setProperty( Constants.INTERNAL_BLJ_MODULE,
			BioLockJUtil.getCollectionAsString( getListedModules( file ) ) );
		props.setProperty( Constants.INTERNAL_DEFAULT_CONFIG,
			BioLockJUtil.getCollectionAsString( BioLockJUtil.getFilePaths( regConf ) ) );
		return props;
	}

	/**
	 * Recursive method handles nested default Config files. Default props are overridden by parent level props.<br>
	 * Standard properties are always imported 1st: {@value biolockj.Constants#STANDARD_CONFIG_PATH}<br>
	 * Docker properties are always imported 2nd if in a Docker container:
	 * {@value biolockj.Constants#DOCKER_CONFIG_PATH}<br>
	 * Then nested default Config files defined by property: {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS}<br>
	 * The project Config file is read last to ensure properties are not overridden in the default Config files.
	 * 
	 * @param propFile BioLockJ Configuration file
	 * @return Properties including default props
	 * @throws Exception if errors occur
	 */
	protected static Properties buildConfig( final File propFile ) throws Exception {
		Log.info( Properties.class,
			"Import All Config Properties for --> Top Level Pipeline Properties File: " + propFile.getAbsolutePath() );
		Properties defaultProps = null;
		final File standConf = BioLockJUtil.getLocalFile( Constants.STANDARD_CONFIG_PATH );
		if( standConf != null && !regConf.contains( propFile ) ) {
			defaultProps = readProps( standConf, null );
		}

		final File dockConf = DockerUtil.inDockerEnv() ? BioLockJUtil.getLocalFile( Constants.DOCKER_CONFIG_PATH )
			: null;
		if( dockConf != null && !regConf.contains( dockConf ) ) {
			defaultProps = readProps( dockConf, defaultProps );
		}

		for( final File pipelineDefaultConfig: getNestedDefaultProps( propFile ) ) {
			if( !regConf.contains( pipelineDefaultConfig ) ) {
				defaultProps = readProps( pipelineDefaultConfig, defaultProps );
			}
		}

		Properties pops = readProps( propFile, defaultProps );
		//report( pops, propFile, true );
		
		return pops;
	}

	/**
	 * Parse property file for the property {@value biolockj.Constants#PIPELINE_DEFAULT_PROPS}.<br>
	 * 
	 * @param propFile BioLockJ Config file
	 * @return nested default prop file or null
	 * @throws Exception if errors occur
	 */
	protected static File getDefaultConfig( final File propFile ) throws Exception {
		final BufferedReader reader = BioLockJUtil.getFileReader( propFile );
		try {
			for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
				final StringTokenizer st = new StringTokenizer( line, "=" );
				if( st.countTokens() > 1 ) {
					if( st.nextToken().trim().equals( Constants.PIPELINE_DEFAULT_PROPS ) )
						return BioLockJUtil.getLocalFile( st.nextToken().trim() );
				}
			}
		} finally {
			if( reader != null ) {
				reader.close();
			}
		}
		return null;
	}

	/**
	 * Read the properties defined in the required propFile and defaultProps (if included) to build Config.<br>
	 * Properties in propFile will override the defaultProps.
	 *
	 * @param propFile BioLockJ configuration file
	 * @param defaultProps Default properties
	 * @return {@link biolockj.Properties} instance
	 * @throws FileNotFoundException thrown if propFile is not a valid file path
	 * @throws IOException thrown if propFile or defaultProps cannot be parsed to read in properties
	 */
	protected static Properties readProps( final File propFile, final Properties defaultProps )
		throws FileNotFoundException, IOException {
		if( propFile.exists() ) {
			regConf.add( propFile );
			Log.info( Properties.class, "LOAD CONFIG [ #" + ++loadOrder + " ]: ---> " + propFile.getAbsolutePath() );
			final FileInputStream in = new FileInputStream( propFile );
			final Properties tempProps = defaultProps == null ? new Properties(): new Properties( defaultProps );
			tempProps.load( in );
			in.close();
			return tempProps;
		}

		return null;
	}

	private static List<String> getListedModules( final File file ) throws Exception {
		final List<String> modules = new ArrayList<>();
		final BufferedReader reader = BioLockJUtil.getFileReader( file );
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

	private static List<File> getNestedDefaultProps( final File propFile ) throws Exception {
		final List<File> configFiles = new ArrayList<>();
		File defConfig = null;
		do {
			defConfig = getDefaultConfig( propFile );
			if( defConfig == null || regConf.contains( defConfig ) || configFiles.contains( defConfig ) ) {
				break;
			}
			configFiles.add( defConfig );
		} while( true );
		Collections.reverse( configFiles );
		return configFiles;
	}


	@SuppressWarnings("unused")
	private static void report( final Properties properties, final File config, boolean projectConfigOnly ) {
		Log.debug( Properties.class, " ---------- Report [ " + config.getAbsolutePath() + " ] ------------> " );
		if( projectConfigOnly ) {
			for( final Object key: properties.keySet() ) {
				Log.debug( Config.class, "Project Config: " + key + "=" + properties.getProperty( (String) key ) );
			}
		} else {
			final Enumeration<?> en = properties.propertyNames();
			while( en.hasMoreElements() ) {
				final String key = en.nextElement().toString();
				Log.debug( Properties.class, key + " = " + properties.getProperty( key ) );
			}
		}
		
		Log.debug( Properties.class, " ----------------------------------------------------------------------------------" );
	}

	private static int loadOrder = -1;
	private static List<File> regConf = new ArrayList<>();
	private static final long serialVersionUID = 2980376615128441545L;
}
