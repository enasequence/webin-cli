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
import org.apache.commons.net.ftp.FTPConnectionClosedException;
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
import java.util.concurrent.Callable;
import java.util.stream.Stream;


public class FtpService implements UploadService {
    private final static String SERVER = "webin2.ebi.ac.uk";
    private final static int FTP_PORT = 21;

    /**
     * Originally, retries were only added for connectivity/network related issues that can occur during usage of this client.
     * But due to random unexplained FTP server errors that sometimes did not re-occur during subsequent submissions, it was
     * decided that all FTP operations will be changed to attempt retry in all error cases.
     */
    private final FTPSClient ftpClient = new FTPSClient();

    private static final Logger log = LoggerFactory.getLogger(FtpService.class);

    private String username;
    private String password;

    @Override public void connect(String userName, String password) {

        this.username = userName;
        this.password = password;

        ftpClient.setRemoteVerificationEnabled(false);
        ftpClient.setActivePortRange(40000, 50000);
        ftpClient.setConnectTimeout(10_000);
        ftpClient.setDefaultTimeout(10_000);
        ftpClient.setDataTimeout(10_000);

        connectAndLogin();
    }


    private void connectAndLogin() {
        connect();
        login();
    }


    private void connect() {
        try {
            log.info("Connecting to FTP server : {}", SERVER);

            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                ftpClient.connect(SERVER, FTP_PORT);
                return null;
            }, context -> log.warn("Retrying connecting to FTP server."), Exception.class);
        } catch (Exception e) {
            throw WebinCliException.systemError(e, WebinCliMessage.FTP_CONNECT_ERROR.text());
        }
    }


    private void login() {
        try {
            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                if ((context.getLastThrowable() != null && context.getLastThrowable() instanceof FTPConnectionClosedException)
                    ||
                    (context.getLastThrowable() != null && context.getLastThrowable() instanceof SocketException
                        && ((SocketException)context.getLastThrowable()).getMessage().contains("Connection or outbound has closed"))) {
                    connect();
                }

                if (!ftpClient.login(username, password))
                    throw WebinCliException.userError(WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format("FTP"));
                return null;
            }, context -> log.warn("Retrying FTP server login."), Exception.class);
        } catch (WebinCliException e) {
            throw e;
        } catch (Exception e) {
            throw WebinCliException.systemError(e, WebinCliMessage.FTP_SERVER_ERROR.text());
        }
    }


    /**
     * In addition to retry, automatically connects and logs in to FTP server if it is found that existing connection
     * is terminated.
     *
     * @param retryCallable
     * @param retryLoggingRunnable
     * @return
     * @param <V>
     * @throws Exception
     */
    private <V> V executeWithRetryReconnectRelogin(Callable<V> retryCallable, Runnable retryLoggingRunnable) throws Exception {
        return RetryUtils.executeWithRetry((RetryCallback<V, Exception>) context -> {
            if (
                (context.getLastThrowable() != null && context.getLastThrowable() instanceof FTPConnectionClosedException)
                ||
                (context.getLastThrowable() != null && context.getLastThrowable() instanceof SocketException
                    && ((SocketException)context.getLastThrowable()).getMessage().contains("Connection or outbound has closed"))) {
                connectAndLogin();
            }

            return retryCallable.call();
        }, context -> retryLoggingRunnable.run(), Exception.class);
    }

    
    private void
    storeFile(Path local, Path remote) throws IOException
    {
        Path subdir = 1 == remote.getNameCount() ? Paths.get( "." ): remote.subpath( 0, remote.getNameCount() - 1 );

        log.info( "Uploading file: {}", local );

        int level = changeToSubdir( subdir );

        try {
            executeWithRetryReconnectRelogin(() -> {
                // In case of a retry, the entire file will be re-uploaded from beginning. Hence, the input stream
                // will need to be re-created as well.
                try (InputStream fileInputStream = new BufferedInputStream(Files.newInputStream(local))) {
                    if (!ftpClient.storeFile(remote.getFileName().toString(), fileInputStream))
                        throw WebinCliException.systemError(WebinCliMessage.FTP_UPLOAD_ERROR.format(remote.getFileName().toString()));
                }

                return null;
            }, () -> log.warn("Retrying file upload to FTP server."));

            for( int l = 0; l < level; ++l )
            {
                executeWithRetryReconnectRelogin(() -> {
                    if( !ftpClient.changeToParentDirectory() )
                        throw WebinCliException.systemError( WebinCliMessage.FTP_CHANGE_DIR_ERROR.format("parent") );
                    return null;
                }, () -> log.warn("Retrying directory change on FTP server."));
            }
        } catch (WebinCliException ex) {
            throw ex;
        } catch (Exception ex) {
            throw WebinCliException.systemError(ex, WebinCliMessage.FTP_SERVER_ERROR.text());
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
                FTPFile[] ftpDirs = executeWithRetryReconnectRelogin(
                    () -> ftpClient.listDirectories(),
                    () -> log.warn("Retrying retrieving directory list from FTP server."));

                if(Stream.of( ftpDirs ).noneMatch(f -> dir.equals( f.getName() ) ))
                {
                    executeWithRetryReconnectRelogin(() -> {
                        if( !ftpClient.makeDirectory( dir ) )
                            throw WebinCliException.systemError( WebinCliMessage.FTP_CREATE_DIR_ERROR.format(dir) );
                        return null;
                    }, () -> log.warn("Retrying directory creation on FTP server."));
                }

                executeWithRetryReconnectRelogin(() -> {
                    if( !ftpClient.changeWorkingDirectory( dir ) )
                        throw WebinCliException.systemError( WebinCliMessage.FTP_CHANGE_DIR_ERROR.format(dir) );
                    return null;
                }, () -> log.warn("Retrying changing working directory on FTP server."));

                level ++;
            } catch (WebinCliException e) {
                throw e;
            } catch (Exception ex) {
                throw WebinCliException.systemError(ex, WebinCliMessage.FTP_SERVER_ERROR.text());
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

            executeWithRetryReconnectRelogin(() -> {
                if( !ftpClient.setFileType( FTP.BINARY_FILE_TYPE ) )
                    throw WebinCliException.systemError( WebinCliMessage.FTP_SERVER_ERROR.text() );
                return null;
            }, () -> log.warn("Retrying setting file type on FTP server."));
           
            changeToSubdir( Paths.get( uploadDir ) );

            FTPFile[] deleteFilesList = executeWithRetryReconnectRelogin(
                () -> ftpClient.listFiles(),
                () -> log.warn("Retrying retrieving file list from FTP server."));

            if( deleteFilesList != null && deleteFilesList.length > 0 )
            {
                for( FTPFile ftpFile: deleteFilesList )
                    executeWithRetryReconnectRelogin(
                        () -> ftpClient.deleteFile( ftpFile.getName()),
                        () -> log.warn("Retrying file deletion on FTP server."));
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
            throw WebinCliException.systemError(ex, WebinCliMessage.FTP_SERVER_ERROR.text());
        }
    }

    @Override public void 
    disconnect() 
    {
        if(ftpClient.isConnected()) {
            try {
                ftpClient.logout();
            } catch (IOException e) {
            } finally {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                }
            }
        }
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
