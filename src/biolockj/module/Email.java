/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Feb 9, 2017
 * @disclaimer 	This code is free software; you can redistribute it and/or
 * 				modify it under the terms of the GNU General Public License
 * 				as published by the Free Software Foundation; either version 2
 * 				of the License, or (at your option) any later version,
 * 				provided that any use properly credits the author.
 * 				This program is distributed in the hope that it will be useful,
 * 				but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 				MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * 				GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.module;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.math.NumberUtils;
import biolockj.BioLockJ;
import biolockj.Config;
import biolockj.Log;
import biolockj.util.ModuleUtil;

/**
 * A simple Mail utility to send notifications when job is complete with a status message
 * and summary details about failures and run time.
 */
public class Email extends BioModuleImpl implements BioModule
{
	@Override
	public void checkDependencies() throws Exception
	{
		Config.requireString( EMAIL_HOST );
		Config.requireString( EMAIL_PORT );
		Config.requireBoolean( EMAIL_SMTP_AUTH );
		Config.requireBoolean( EMAIL_START_TLS_ENABLE );
		Config.requireString( EMAIL_ENCRYPTED_PASSWORD );
		Config.getString( CLUSTER_HOST );

		new InternetAddress( Config.requireString( EMAIL_FROM ) ).validate();
		for( final String email: Config.requireList( EMAIL_TO ) )
		{
			new InternetAddress( email ).validate();
		}
	}

	@Override
	public void executeProjectFile() throws Exception
	{
		status = getStatus();
		Transport.send( getMimeMessage() );
		Log.out.info( "EMAIL SENT!" );
	}

	/**
	 * As the last module, no summary is output.
	 */
	@Override
	public String getSummary()
	{
		return null;
	}

	/**
	 * As the last module, no summary is output.
	 */
	public void setError( final Exception ex )
	{
		pipelineFailure = ex;
	}

	private String convertToHtml( final String text ) throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		return "<font face=\"monaco\" size=\"10px\">" + sb.toString() + "</font>";
	}

	private String decrypt( final String property )
	{
		String decryptedPassword = null;
		try
		{
			final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );
			final SecretKey key = keyFactory.generateSecret( new PBEKeySpec( PASSWORD ) );
			final Cipher pbeCipher = Cipher.getInstance( "PBEWithMD5AndDES" );
			pbeCipher.init( Cipher.DECRYPT_MODE, key, new PBEParameterSpec( SALT, 20 ) );
			decryptedPassword = new String( pbeCipher.doFinal( base64Decode( property ) ), "UTF-8" );
		}
		catch( final Exception ex )
		{
			Log.out.error( ex.getMessage(), ex );
		}

		return decryptedPassword;

	}

	private Multipart getContent( final String emailText ) throws Exception
	{
		final Multipart multipart = new MimeMultipart();
		final BodyPart mimeMsg = new MimeBodyPart();
		mimeMsg.setText( emailText );
		multipart.addBodyPart( mimeMsg );
		return multipart;
	}

	/**
	 * Example download command:
	 *
	 * scp -r -p msioda@hpc.uncc.edu:/users/msioda/data/metagenomics/* /Users/msioda/projects/downloads
	 *
	 * @return download msg
	 */
	private String getDownloadCmd() throws Exception
	{
		String user = System.getProperty( "user.home" );
		final String downloadDir = Config.getDownloadDir();
		final String downloadMod = Config.getString( Config.CLUSTER_DOWNLOAD_MODULE );
		if( ( downloadMod != null ) && ( downloadDir != null ) )
		{
			String label = "Download Reports";
			BioModule m = ModuleUtil.requireModule( downloadMod );
			if( !ModuleUtil.isComplete( m ) )
			{
				m = ModuleUtil.getFailedModule();
				label = "Download Failed Module";
			}

			final File logFileCopy = new File( m.getOutputDir().getAbsolutePath() + File.separator + Log.getName() );
			FileUtils.copyFile( Log.getFile(), logFileCopy );

			if( user.lastIndexOf( File.separator ) > 0 )
			{
				user = user.substring( user.lastIndexOf( File.separator ) + 1 );
			}

			final String downloadSize = FileUtils
					.byteCountToDisplaySize( FileUtils.sizeOfAsBigInteger( m.getOutputDir() ) );
			label = label + " [" + downloadSize + "]:";
			final String cmd = "scp -r -p " + user + "@" + Config.requireString( CLUSTER_HOST ) + ":"
					+ m.getOutputDir().getAbsolutePath() + File.separator + " " + downloadDir;
			return label + RETURN + cmd;
		}

		return null;
	}

	/**
	 * Summary of run-times to be output to output file & to be included in summary email.
	 * @return
	 */
	private String getEmailText() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		sb.append( getLabel( "Project Name" ) + Config.requireString( Config.PROJECT_NAME ) + RETURN );
		sb.append( getLabel( "Pipeline Status" ) + "Pipeline " + ( BioLockJ.isRestarted() ? "(restart) ": "" )
				+ status.toLowerCase() + "!" + RETURN );
		sb.append( getLabel( "Pipeline Runtime" )
				+ ModuleUtil.getRunTime( System.currentTimeMillis() - BioLockJ.APP_START_TIME ) + RETURN );
		sb.append( getLabel( "Pipeline Root Dir" ) + Config.requireExistingDir( Config.PROJECT_DIR ).getAbsolutePath()
				+ RETURN );

		Exception emailError = null;
		try
		{
			final String downloadCmd = getDownloadCmd();
			if( downloadCmd != null )
			{
				final StringBuffer cmd = new StringBuffer();
				cmd.append( SPACER + SPACER + RETURN );
				cmd.append( downloadCmd + RETURN );
				sb.append( cmd );
			}
		}
		catch( final Exception ex )
		{
			emailError = ex;
			Log.out.error( "Unable to build download command!", ex );
		}

		sb.append( SPACER + SPACER + RETURN );
		int i = 0;
		for( final BioModule m: Config.getModules() )
		{
			sb.append( getLabel( "Module[" + ( i++ ) + "]" ) + m.getClass().getName() + RETURN );
			if( m == this )
			{
				sb.append( getLabel( "Status" ) + "email sent" + RETURN );
				if( emailError != null )
				{
					sb.append( getExceptionOutput( emailError ) );
				}
			}
			else if( ModuleUtil.isComplete( m ) && ModuleUtil.hasExecuted( m ) )
			{
				sb.append( getLabel( "Status" ) + "complete" + RETURN );
				sb.append( getLabel( "Runtime" ) + ModuleUtil.getRunTime( m ) + RETURN );
			}
			else if( ModuleUtil.isComplete( m ) && !ModuleUtil.hasExecuted( m ) )
			{
				sb.append( getLabel( "Status" ) + "complete (prior to restart)" + RETURN );
				sb.append( getLabel( "Runtime" ) + "N/A" + RETURN );
			}
			else if( ModuleUtil.isIncomplete( m ) )
			{
				sb.append( getLabel( "Status" ) + "failed" + RETURN );
				sb.append( getLabel( "Runtime" ) + ModuleUtil.getRunTime( m ) + RETURN );
				sb.append( getExceptionOutput( pipelineFailure ) );
			}
			else
			{
				sb.append( getLabel( "Status" ) + "incomplete" + RETURN );
				sb.append( getLabel( "Runtime" ) + "N/A" + RETURN );
			}

			String summary = m.getSummary();
			if( ( summary != null ) && !summary.isEmpty() )
			{
				sb.append( SPACER + RETURN );
				if( !summary.endsWith( RETURN ) )
				{
					summary += RETURN;
				}
				sb.append( summary );
			}

			sb.append( SPACER + SPACER + RETURN );
		}

		Log.out.info( sb.toString() );

		sb.append( RETURN + "Regards," + RETURN + "BioLockJ Admin" );

		return sb.toString();
	}

	private String getExceptionOutput( final Exception ex )
	{
		if( ex == null )
		{
			return "Error message not found!" + RETURN;
		}
		final StringBuffer sb = new StringBuffer();
		sb.append( SPACER + RETURN + getLabel( "Exception" ) + ex.getMessage() + RETURN );
		for( final StackTraceElement ste: ex.getStackTrace() )
		{
			sb.append( TAB_DELIM + ste.toString() + RETURN );
		}

		return sb.toString();
	}

	private Multipart getHtmlContent( final String emailText ) throws Exception
	{
		final Multipart multipart = new MimeMultipart();
		final BodyPart mimeMsg = new MimeBodyPart();
		mimeMsg.setText( convertToHtml( emailText ) );
		multipart.addBodyPart( mimeMsg );
		Log.out.info( RETURN + RETURN + emailText + RETURN + RETURN );
		return multipart;
	}

	private Message getMimeMessage() throws Exception
	{
		final Message message = new MimeMessage( getSession() );
		message.setFrom( new InternetAddress( Config.requireString( EMAIL_FROM ) ) );
		message.addRecipients( Message.RecipientType.TO, InternetAddress.parse( getRecipients() ) );
		message.setSubject( "BioLockJ " + Config.requireString( Config.PROJECT_NAME ) + " " + status );
		message.setContent( getContent( getEmailText() ), "text/plain; charset=utf-8" );
		return message;
	}

	private String getRecipients() throws Exception
	{
		final StringBuffer addys = new StringBuffer();
		for( final String to: Config.requireList( EMAIL_TO ) )
		{
			if( addys.length() != 0 )
			{
				addys.append( "," );
			}
			addys.append( to );
		}

		return addys.toString();
	}

	private Session getSession() throws Exception
	{
		String startTls = "false";
		String smtpAuth = "false";
		if( Config.requireBoolean( EMAIL_SMTP_AUTH ) )
		{
			smtpAuth = "true";
		}
		if( Config.requireBoolean( EMAIL_START_TLS_ENABLE ) )
		{
			startTls = "true";
		}

		final Properties props = new Properties();
		props.put( EMAIL_SMTP_AUTH, smtpAuth );
		props.put( EMAIL_START_TLS_ENABLE, startTls );
		props.put( EMAIL_HOST, Config.requireString( EMAIL_HOST ) );
		props.put( EMAIL_PORT, Config.requireString( EMAIL_PORT ) );

		final Session session = Session.getInstance( props, new Authenticator()
		{
			@Override
			protected PasswordAuthentication getPasswordAuthentication()
			{
				try
				{
					return new PasswordAuthentication( Config.requireString( EMAIL_FROM ),
							decrypt( Config.requireString( EMAIL_ENCRYPTED_PASSWORD ) ) );
				}
				catch( final Exception ex )
				{
					Log.out.error( "Unable to build PasswordAuthentication due to missing/invalid properties: "
							+ EMAIL_FROM + " or " + EMAIL_ENCRYPTED_PASSWORD + " : " + ex.getMessage(), ex );
				}

				return null;
			}
		} );

		return session;
	}

	private String getStatus() throws Exception
	{
		final File[] dirs = Config.requireExistingDir( Config.PROJECT_DIR )
				.listFiles( (FileFilter) DirectoryFileFilter.DIRECTORY );
		for( final File d: dirs )
		{
			final String prefix = d.getName().substring( 0, 1 );
			if( NumberUtils.isNumber( prefix ) && !d.getName().contains( Email.class.getSimpleName() ) )
			{
				if( FileUtils.directoryContains( d,
						new File( d.getAbsolutePath() + File.separator + BioModule.BLJ_STARTED ) ) )
				{
					failedModule = d.getName();
					Log.out.warn( "Module Failed: " + failedModule );
					return BioLockJ.SCRIPT_FAILED;
				}

			}
		}

		return BioLockJ.SCRIPT_SUCCESS;
	}

	private Message getVersitileMimeMessage() throws Exception
	{
		final MimeMessage mimeMessage = new MimeMessage( getSession() );
		mimeMessage.setFrom( new InternetAddress( Config.requireString( EMAIL_FROM ) ) );
		mimeMessage.addRecipients( Message.RecipientType.TO, InternetAddress.parse( getRecipients() ) );
		mimeMessage.setSubject( "BioLockJ " + Config.requireString( Config.PROJECT_NAME ) + " " + status );

		final String emailText = getEmailText();

		final MimeBodyPart wrapper = new MimeBodyPart();

		final MimeMultipart cover = new MimeMultipart( "alternative" );
		final BodyPart textPart = new MimeBodyPart();
		textPart.setContent( getContent( emailText ), "text/plain; charset=utf-8" );
		textPart.setDisposition( Part.INLINE );
		cover.addBodyPart( textPart );

		//HTML
		final BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent( getHtmlContent( emailText ), "text/html; charset=utf-8" );
		htmlPart.setDisposition( Part.INLINE );
		cover.addBodyPart( htmlPart );

		wrapper.setContent( cover );

		final MimeMultipart content = new MimeMultipart( "related" );
		mimeMessage.setContent( content );
		content.addBodyPart( wrapper );
		return mimeMessage;
	}

	/**
	 * Used to obtain a new encrypted password hash when the admin email password is set.
	 * @param password
	 * @throws Exception
	 */
	public static void encryptAndStoreEmailPassword( final String propFile, final String password ) throws Exception
	{
		final String encryptedPassword = encrypt( password );
		final PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout( new PropertiesConfiguration() );
		layout.load( new InputStreamReader( new FileInputStream( propFile ) ) );
		final Config props = Config.readProps( new File( propFile ), null );
		props.setProperty( EMAIL_ENCRYPTED_PASSWORD, encryptedPassword );
		layout.save( new FileWriter( propFile, false ) );
		System.out.println( "CONFIG FILE UPDATED WITH ENCRYPTED PASSWORD: " + encryptedPassword );
	}

	private static byte[] base64Decode( final String encodedPassword ) throws IOException
	{
		return Base64.getDecoder().decode( encodedPassword );
	}

	private static String base64Encode( final byte[] bytes )
	{
		return Base64.getEncoder().encodeToString( bytes );
	}

	private static String encrypt( final String password ) throws GeneralSecurityException, UnsupportedEncodingException
	{
		final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "PBEWithMD5AndDES" );
		final SecretKey key = keyFactory.generateSecret( new PBEKeySpec( PASSWORD ) );
		final Cipher pbeCipher = Cipher.getInstance( "PBEWithMD5AndDES" );
		pbeCipher.init( Cipher.ENCRYPT_MODE, key, new PBEParameterSpec( SALT, 20 ) );
		return base64Encode( pbeCipher.doFinal( password.getBytes( "UTF-8" ) ) );
	}

	private static String getLabel( final String label )
	{
		return label + ":  ";
		//		final int bufferSize = LABEL_SPACE - label.length();
		//		String buffer = "";
		//		while( buffer.length() < bufferSize )
		//		{
		//			buffer = buffer + " ";
		//		}
		//		return label + ":" + buffer;
	}

	private String failedModule = null;
	private Exception pipelineFailure = null;
	private String status = null;
	protected static final String CLUSTER_HOST = "cluster.host";
	protected static final String EMAIL_ENCRYPTED_PASSWORD = "mail.encryptedPassword";
	protected static final String EMAIL_FROM = "mail.from";
	protected static final String EMAIL_HOST = "mail.smtp.host";
	protected static final String EMAIL_PORT = "mail.smtp.port";
	protected static final String EMAIL_SMTP_AUTH = "mail.smtp.auth";
	protected static final String EMAIL_START_TLS_ENABLE = "mail.smtp.starttls.enable";
	protected static final String EMAIL_TO = "mail.to";
	//private static int LABEL_SPACE = 16;
	private static final char[] PASSWORD = "enfldsgbnlsngdlksdsgm".toCharArray();
	private static final byte[] SALT = { (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, (byte) 0xde, (byte) 0x33,
			(byte) 0x10, (byte) 0x12, };
	private static final String SPACER = "---------------------------------------------------------------------";
}
