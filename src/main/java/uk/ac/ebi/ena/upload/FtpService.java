/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.upload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import uk.ac.ebi.ena.rawreads.VerboseLogger;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class FtpService implements UploadService, VerboseLogger {
    private final static String SERVER = "webin.ebi.ac.uk";
    private final static int FTP_PORT = 21;
    private FTPClient ftpClient = new FTPClient() ;
    private final static String SYSTEM_ERROR_CONNECT = "Failed to connect to the Webin file upload area.";
    private final static String SYSTEM_ERROR_CREATE_DIR = "Failed to create directory in webin.ebi.ac.uk file upload area.";
    private final static String SYSTEM_ERROR_CHANGE_DIR = "Failed to access directory in webin.ebi.ac.uk file upload area.";
    private final static String SYSTEM_ERROR_UPLOAD_FILE = "Failed to upload files to webin.ebi.ac.uk file upload area.";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when uploading files to webin.ebi.ac.uk file upload area.";

    /* (non-Javadoc)
     * @see uk.ac.ebi.ena.upload.UploadService#connectToFtp(java.lang.String, java.lang.String)
     */
    @Override public void connect(String userName, String password) {
        try {
            ftpClient.connect(SERVER, FTP_PORT);
        } catch (IOException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_CONNECT);
        }
        try {
            if (!ftpClient.login(userName, password))
                throw WebinCliException.createUserError(WebinCli.AUTHENTICATION_ERROR);
        } catch (IOException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_CONNECT);
        }
    }

    
    void   
    storeFile( Path local, Path remote ) throws IOException
    {
        Path subdir = 1 == remote.getNameCount() ? Paths.get( "." ): remote.subpath( 0, remote.getNameCount() - 1 );
        try( InputStream fileInputStream = new BufferedInputStream( Files.newInputStream( local ) ) )    
        {
            printfToConsole( "Uploading file: %s\n", local );
            int level = changeToSubdir( subdir );       
            if( !ftpClient.storeFile( remote.getFileName().toString(), fileInputStream ) )
                throw WebinCliException.createSystemError( SYSTEM_ERROR_UPLOAD_FILE, "Unable to transfer " + remote.getFileName().toString() );
            
            for( int l = 0; l < level; ++l )
            {
                if( !ftpClient.changeToParentDirectory() )
                    throw WebinCliException.createSystemError( SYSTEM_ERROR_CHANGE_DIR, "Unable to change to parent directory" );
            }
        }
    }


    private int
    changeToSubdir( Path subdir ) throws IOException
    {
        int level = 0;
        for( int l = 0; l < subdir.getNameCount(); ++l )
        {
            String dir = subdir.subpath( l, l + 1 ).getFileName().toString();
            if( dir.equals( "." ) )
                continue;

            if( dir.equals( ".." ) )
            {
                throw WebinCliException.createSystemError( SYSTEM_ERROR_CHANGE_DIR, dir );
            }
            
            if( !Stream.of( ftpClient.listDirectories() ).anyMatch( f -> dir.equals( f.getName() ) ) )
            {
                if( !ftpClient.makeDirectory( dir ) )
                    throw WebinCliException.createSystemError( SYSTEM_ERROR_CREATE_DIR, dir );
            }
            
            if( !ftpClient.changeWorkingDirectory( dir ) )
                throw WebinCliException.createSystemError( SYSTEM_ERROR_CHANGE_DIR, dir );

            level ++;
        }
        return level;
    }
    
    
    //TODO verbose possible issues with file/folder permissions
    /* (non-Javadoc)
     * @see uk.ac.ebi.ena.upload.UploadService#ftpDirectory(java.util.List, java.lang.String, java.nio.file.Path)
     */
    @Override public void 
    ftpDirectory( List<File> uploadFilesList, String uploadDir, Path inputDir ) 
    {
        if( null == uploadDir || uploadDir.isEmpty() )
            throw WebinCliException.createUserError( WebinCli.MISSING_CONTEXT );
        try 
        {
            ftpClient.enterLocalPassiveMode();
            if( !ftpClient.setFileType( FTP.BINARY_FILE_TYPE ) )
                throw WebinCliException.createSystemError( SYSTEM_ERROR_OTHER );
           
            changeToSubdir( Paths.get( uploadDir ) );
            
            FTPFile[] fileTodeleteA = ftpClient.listFiles();
            if( fileTodeleteA != null && fileTodeleteA.length > 0 ) 
            {
                for( FTPFile ftpFile: fileTodeleteA )
                    ftpClient.deleteFile( ftpFile.getName() );
            }
            
            for( File file: uploadFilesList ) 
            {
                Path f = file.isAbsolute() ? file.toPath().startsWith( inputDir ) ? inputDir.relativize( file.toPath() ) 
                                                                                  : file.toPath().getFileName()
                                           : file.toPath();
                storeFile( file.toPath(), f );
            }
        } catch( IOException e ) 
        {
            throw WebinCliException.createSystemError( SYSTEM_ERROR_OTHER, e.getMessage() );
        }
    }
    
    
     /* (non-Javadoc)
     * @see uk.ac.ebi.ena.upload.UploadService#disconnectFtp()
     */
    @Override public void 
    disconnect() 
    {
        try 
        {
            if( ftpClient != null && ftpClient.isConnected() )
                ftpClient.disconnect();
        } catch (IOException e) {}
    }

    
    @Override
    protected void finalize() throws Throwable {
        disconnect();
    }


    @Override public boolean
    isAvaliable()
    {
        return true;
    }
}
