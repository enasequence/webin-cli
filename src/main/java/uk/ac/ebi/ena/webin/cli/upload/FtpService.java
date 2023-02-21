/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.upload;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class FtpService implements UploadService {
    private final static String SERVER = "webin2.ebi.ac.uk";
    private final static int FTP_PORT = 21;
    private final FTPSClient ftpClient = new FTPSClient();

    private static final Logger log = LoggerFactory.getLogger(FtpService.class);

    @Override public void connect(String userName, String password) {
        try {
            ftpClient.setRemoteVerificationEnabled(false);
            ftpClient.setActivePortRange(40000, 50000);
            ftpClient.setConnectTimeout(10_000);
            ftpClient.setDefaultTimeout(10_000);
            ftpClient.setDataTimeout(10_000);

            log.info("Connecting to FTP server : {}", SERVER);

            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                ftpClient.connect(SERVER, FTP_PORT);
                return null;
            }, context -> log.warn("Retrying connecting to FTP server."), SocketException.class, IOException.class);
        } catch (Exception e) {
            throw WebinCliException.systemError(WebinCliMessage.FTP_CONNECT_ERROR.text(), e.getMessage());
        }

        try {
            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                if (!ftpClient.login(userName, password))
                    throw WebinCliException.userError(WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format("FTP"));
                return null;
            }, context -> log.warn("Retrying FTP server login."), IOException.class);
        } catch (WebinCliException e) {
            throw e;
        } catch (Exception e) {
            throw WebinCliException.systemError(WebinCliMessage.FTP_SERVER_ERROR.text(), e.getMessage());
        }
    }

    
    private void
    storeFile(Path local, Path remote) throws IOException
    {
        Path subdir = 1 == remote.getNameCount() ? Paths.get( "." ): remote.subpath( 0, remote.getNameCount() - 1 );

        log.info( "Uploading file: {}", local );

        int level = changeToSubdir( subdir );

        try {
            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                // In case of a retry, the entire file will be re-uploaded from beginning. Hence, the input stream
                // will need to be re-created as well.
                try (InputStream fileInputStream = new BufferedInputStream(Files.newInputStream(local))) {
                    if (!ftpClient.storeFile(remote.getFileName().toString(), fileInputStream))
                        throw WebinCliException.systemError(WebinCliMessage.FTP_UPLOAD_ERROR.format(remote.getFileName().toString()));
                }

                return null;
            }, context -> log.warn("Retrying file upload to FTP server."), IOException.class);

            for( int l = 0; l < level; ++l )
            {
                RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                    if( !ftpClient.changeToParentDirectory() )
                        throw WebinCliException.systemError( WebinCliMessage.FTP_CHANGE_DIR_ERROR.format("parent") );
                    return null;
                }, context -> log.warn("Retrying directory change on FTP server."), IOException.class);
            }
        } catch (WebinCliException ex) {
            throw ex;
        } catch (Exception ex) {
            throw WebinCliException.systemError( WebinCliMessage.FTP_SERVER_ERROR.text(), ex.getMessage());
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
                throw WebinCliException.systemError( WebinCliMessage.FTP_CHANGE_DIR_ERROR.format(dir) );
            }

            try {
                FTPFile[] ftpDirs = RetryUtils.executeWithRetry(
                    (RetryCallback<FTPFile[], Exception>) context -> ftpClient.listDirectories(),
                    context -> log.warn("Retrying retrieving directory list from FTP server."), IOException.class);

                if(Stream.of( ftpDirs ).noneMatch(f -> dir.equals( f.getName() ) ))
                {
                    RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                        if( !ftpClient.makeDirectory( dir ) )
                            throw WebinCliException.systemError( WebinCliMessage.FTP_CREATE_DIR_ERROR.format(dir) );
                        return null;
                    }, context -> log.warn("Retrying directory creation on FTP server."), IOException.class);
                }

                RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                    if( !ftpClient.changeWorkingDirectory( dir ) )
                        throw WebinCliException.systemError( WebinCliMessage.FTP_CHANGE_DIR_ERROR.format(dir) );
                    return null;
                }, context -> log.warn("Retrying changing working directory on FTP server."), IOException.class);

                level ++;
            } catch (WebinCliException e) {
                throw e;
            } catch (Exception ex) {
                throw WebinCliException.systemError(WebinCliMessage.FTP_SERVER_ERROR.text(), ex.getMessage());
            }
        }
        return level;
    }
    
    
    //TODO verbose possible issues with file/folder permissions
    @Override public void
    upload(List<File> uploadFilesList, String uploadDir, Path inputDir )
    {
        if( null == uploadDir || uploadDir.isEmpty() )
            throw WebinCliException.userError( WebinCliMessage.FTP_UPLOAD_DIR_ERROR.text());
        try 
        {
            ftpClient.enterLocalPassiveMode();

            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                if( !ftpClient.setFileType( FTP.BINARY_FILE_TYPE ) )
                    throw WebinCliException.systemError( WebinCliMessage.FTP_SERVER_ERROR.text() );
                return null;
            }, context -> log.warn("Retrying setting file type on FTP server."), IOException.class);
           
            changeToSubdir( Paths.get( uploadDir ) );

            FTPFile[] deleteFilesList = RetryUtils.executeWithRetry(
                (RetryCallback<FTPFile[], Exception>) context -> ftpClient.listFiles(),
                context -> log.warn("Retrying retrieving file list from FTP server."), IOException.class);

            if( deleteFilesList != null && deleteFilesList.length > 0 )
            {
                for( FTPFile ftpFile: deleteFilesList )
                    RetryUtils.executeWithRetry(
                        (RetryCallback<Boolean, Exception>) context -> ftpClient.deleteFile( ftpFile.getName()),
                        context -> log.warn("Retrying file deletion on FTP server."), IOException.class);
            }
            
            for( File file: uploadFilesList ) 
            {
                Path f = file.isAbsolute() ? file.toPath().startsWith( inputDir ) ? inputDir.relativize( file.toPath() ) 
                                                                                  : file.toPath().getFileName()
                                           : file.toPath();
                storeFile( file.toPath(), f );
            }
        } catch (WebinCliException e) {
            throw e;
        } catch (Exception ex) {
            throw WebinCliException.systemError(WebinCliMessage.FTP_SERVER_ERROR.text(), ex.getMessage());
        }
    }

    @Override public void 
    disconnect() 
    {
        try 
        {
            if(ftpClient.isConnected())
                ftpClient.disconnect();
        } catch (IOException e) {}
    }

    
    @Override
    protected void finalize() {
        disconnect();
    }


    @Override public boolean
    isAvailable()
    {
        return true;
    }
}
