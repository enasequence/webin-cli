package uk.ac.ebi.ena.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;

public class 
FileUtils 
{

	public static BufferedReader 
	getBufferedReader( File file ) throws FileNotFoundException, IOException 
	{
		if( file.getName().matches( "^.+\\.gz$" ) || file.getName().matches( "^.+\\.gzip$" ) ) 
		{
			GZIPInputStream gzip = new GZIPInputStream( new FileInputStream( file ) );
			return new BufferedReader( new InputStreamReader( gzip ) );
			
		} else if( file.getName().matches( "^.+\\.bz2$" ) || file.getName().matches( "^.+\\.bzip2$" ) ) 
		{
			BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream( new FileInputStream( file ) );
			return new BufferedReader( new InputStreamReader( bzIn ) );
			
		} else 
		{
			return new BufferedReader( new FileReader(file ) );
		}
	}
	
	
	public static boolean 
	isZipped( String fileName )
	{
		if( fileName.matches( "^.+\\.gz$" ) || fileName.matches( "^.+\\.gzip$" ) || fileName.matches( "^.+\\.bz2$" ) || fileName.matches( "^.+\\.bzip2$" ) )
		{
			return true;
		}
		return false;
	}
	
	
	public static File  
	gZipFile( File file ) throws IOException 
	{
        if( isZipped( file.getName() ) )
        	return file;
        
        File result = new File( file.getParentFile(), file.getName() + ".gz" );
		byte[] buffer = new byte[ 1024 ];
		try( OutputStream gzos = new GZIPOutputStream( new BufferedOutputStream( new FileOutputStream( result ) ) );
		     InputStream  in = new BufferedInputStream( new FileInputStream( file ) ) )
		{
			int len;
			while( ( len = in.read( buffer ) ) > 0 ) 
				gzos.write( buffer, 0, len );
		}
		
		file.delete();
		return result;
	}
	
	
	static public String
	md5CheckSum( String file ) throws NoSuchAlgorithmException, IOException
	{
	    return calculateDigest( "MD5", new File( file ) );
	}
	
	
    static public String 
    calculateDigest( String digest_name, 
                     File   file ) throws IOException, NoSuchAlgorithmException 
    {
        MessageDigest digest = MessageDigest.getInstance( digest_name );
        byte[] buf = new byte[ 4096 ];
        int  read  = 0;
        try( BufferedInputStream is = new BufferedInputStream( new FileInputStream( file ) ) )
        {
            while( ( read = is.read( buf ) ) > 0 )
                digest.update( buf, 0, read );
            
            byte[] message_digest = digest.digest();
            BigInteger value = new BigInteger( 1, message_digest );
            return String.format( String.format( "%%0%dx", message_digest.length << 1 ), value );
        }
    }
    
	
	public static boolean 
	emptyDirectory( File dir )
	{
	    if( dir.exists() )
	    {
	        File[] files = dir.listFiles();
	        for( int i = 0; i < files.length; i++ ) 
	        {
	            if( files[ i ].isDirectory() ) 
	            {
	            	emptyDirectory( files[ i ] );
	            } else 
	            {
	                files[ i ].delete();
	            }
	        }
	    }
	    return dir.listFiles().length == 0;
	}


	@Deprecated public static File 
	createReportFile( File reportDir, String submittedFile ) throws ValidationEngineException 
	{
		try 
		{
			Path submittedFilePath = Paths.get( submittedFile );
			if( !Files.exists( submittedFilePath ) )
				throw new ValidationEngineException( "File " + submittedFile + " does not exist" );
			
			String reportFile = reportDir + File.separator + submittedFilePath.getFileName().toString() + ".report";
			
			Path reportPath = Paths.get( reportFile );
			if( Files.exists( reportPath ) )
				Files.delete( reportPath );
			
			Files.createFile( reportPath );
			return new File( reportFile );
			
		} catch (IOException e) 
		{
			throw new ValidationEngineException( "Unable to create report file." );
		}
	}
	
	
	public static String
	formatMessage( Severity severity, String message ) 
	{
        StringWriter str = new StringWriter();
        ValidationMessage<?> validationMessage = new ValidationMessage<>( severity, ValidationMessage.NO_KEY );
        try 
        {
        	validationMessage.setMessage( message );
        	validationMessage.writeMessage( str );
        	return str.toString();
        } catch ( IOException e )
        {
        	return e.toString();
        }
	}


	public static void 
	writeReport( File reportFile, Severity severity, String message )
	{
		try 
		{
			Files.write( reportFile.toPath(), 
					     formatMessage( severity, message ).getBytes( StandardCharsets.UTF_8 ),
					     StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.SYNC );
			
		} catch( IOException e ) 
		{
			//ignore;
		}
	}

	
	public static void 
	writeReport( File reportFile, ValidationResult validationResult ) 
	{
		try
		{
			StringWriter writer = new StringWriter();
			validationResult.writeMessages( writer );
            Files.write( reportFile.toPath(), 
            		     writer.toString().getBytes( StandardCharsets.UTF_8 ),
            		     StandardOpenOption.APPEND, StandardOpenOption.SYNC, StandardOpenOption.CREATE );
		} catch( IOException e ) 
		{
			//ignore;
		}
	}

	
	public static void 
	writeReport( File reportFile, ValidationResult validationResult, String targetOrigin ) 
	{
		try
		{
			StringWriter writer = new StringWriter();
			validationResult.writeMessages( writer, targetOrigin );
            Files.write( reportFile.toPath(), 
            		     writer.toString().getBytes( StandardCharsets.UTF_8 ), 
            		     StandardOpenOption.APPEND, StandardOpenOption.SYNC, StandardOpenOption.CREATE );
		} catch( IOException e ) 
		{
			//ignore;
		}
	}

	
	public static void 
	writeReport( File reportFile, Collection<ValidationMessage<Origin>> validationMessagesList ) 
	{
		try 
		{
			StringWriter writer = new StringWriter();
			for( ValidationMessage<?> validationMessage: validationMessagesList )
				validationMessage.writeMessage( writer );
            
			Files.write( reportFile.toPath(), 
					     writer.toString().getBytes( StandardCharsets.UTF_8 ), 
					     StandardOpenOption.APPEND, StandardOpenOption.SYNC, StandardOpenOption.CREATE );
			
		} catch( IOException e )
		{
			//ignore;
		}
	}


	public static void 
	writeReport( File reportFile, Collection<ValidationMessage<Origin>> validationMessagesList, String targetOrigin ) 
	{
		try 
		{
			StringWriter writer = new StringWriter();
			for( ValidationMessage<?> validationMessage: validationMessagesList )
			{   if(Severity.ERROR.equals(validationMessage.getSeverity()))
				validationMessage.writeMessage( writer,ValidationMessage.TEXT_TIME_MESSAGE_FORMATTER_TRAILING_LINE_END, targetOrigin );
			}
            
			Files.write( reportFile.toPath(), 
					     writer.toString().getBytes( StandardCharsets.UTF_8 ), 
					     StandardOpenOption.APPEND, StandardOpenOption.SYNC, StandardOpenOption.CREATE );
			
		} catch( IOException e )
		{
			//ignore;
		}
	}
}