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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class FtpService implements UploadService {
    private final static String SERVER = "webin2.ebi.ac.uk";
    private final static int FTP_PORT = 21;

    private static final Logger log = LoggerFactory.getLogger(FtpService.class);

    private final FTPSClient ftpClient = new FTPSClient();

    private String username;
    private String password;

    private String lastWorkingDirectory;

    @Override
    public void connect(String userName, String password) {

        this.username = userName;
        this.password = password;

        ftpClient.setRemoteVerificationEnabled(false);
        ftpClient.setActivePortRange(40000, 50000);
        ftpClient.setConnectTimeout(10_000);
        ftpClient.setDefaultTimeout(10_000);
        ftpClient.setDataTimeout(10_000);

        connect();
        login();
        setFileTypeAsBinary();
    }

    //TODO verbose possible issues with file/folder permissions
    @Override
    public void upload(List<File> uploadFilesList, String uploadDir, Path inputDir ) {
        if( null == uploadDir || uploadDir.isEmpty() ) {
            throw WebinCliException.userError(WebinCliMessage.FTP_UPLOAD_DIR_ERROR.text());
        }

        try {
            // Set given upload directory as last working directory so that if a reconnect happens then the upload
            // directory can automatically become current working directory.
            //lastWorkingDirectory = uploadDir;

            changeToSubdir( Paths.get( uploadDir ) );

            FTPFile[] deleteFilesList = executeWithReconnect(
                () -> ftpClient.listFiles(),
                () -> log.warn("Retrying retrieving file list from FTP server."));

            if( deleteFilesList != null && deleteFilesList.length > 0 ) {
                for( FTPFile ftpFile: deleteFilesList ) {
                    executeWithReconnect(
                        () -> ftpClient.deleteFile(ftpFile.getName()),
                        () -> log.warn("Retrying file deletion on FTP server."));
                }
            }

            for( File file: uploadFilesList ) {
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

    @Override
    public void disconnect() {
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
    public boolean isAvailable() {
        return true;
    }

    @Override
    protected void finalize() {
        disconnect();
    }

    private void connect() {
        try {
            log.info("Connecting to FTP server : {}", SERVER);

            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                ftpClient.connect(SERVER, FTP_PORT);
                ftpClient.enterLocalPassiveMode();
                return null;
            }, context -> log.warn("Retrying connecting to FTP server."), IOException.class);
        } catch (Exception e) {
            throw WebinCliException.systemError(e, WebinCliMessage.FTP_CONNECT_ERROR.text());
        }
    }

    private void login() {
        try {
            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                if (context.getLastThrowable() != null && context.getLastThrowable() instanceof IOException) {
                    // Testing has showed that even in the event of a broken connection, ftpsClient.isConnected()
                    // continues to return true. So need to check the status here before attempting to connect again.
                    connect();
                }

                if (!ftpClient.login(username, password)) {
                    logErrorFtpReply();
                    throw new FTPAuthenticationFailureException();
                }

                return null;
            }, context -> log.warn("Retrying FTP server login."), IOException.class, FTPAuthenticationFailureException.class);
        } catch (FTPAuthenticationFailureException e) {
            throw WebinCliException.userError(WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format("FTP"));
        } catch (Exception e) {
            throw WebinCliException.systemError(e, WebinCliMessage.FTP_SERVER_ERROR.text());
        }
    }

    private void setFileTypeAsBinary() {
        try {
            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                if (context.getLastThrowable() != null && context.getLastThrowable() instanceof IOException) {
                    connect();
                    login();
                }

                if( !ftpClient.setFileType( FTP.BINARY_FILE_TYPE ) ) {
                    logErrorFtpReply();
                    throw WebinCliException.systemError(WebinCliMessage.FTP_SERVER_ERROR.text());
                }

                return null;
            }, context -> log.warn("Retrying setting file type on FTP server."), IOException.class);
        } catch (WebinCliException e) {
            throw e;
        } catch (Exception e) {
            throw WebinCliException.systemError(e, WebinCliMessage.FTP_SERVER_ERROR.text());
        }
    }

    private void changeToLastWorkingDirectory() {
        if (lastWorkingDirectory == null) {
            return;
        }

        try {
            RetryUtils.executeWithRetry((RetryCallback<Void, Exception>) context -> {
                if (context.getLastThrowable() != null && context.getLastThrowable() instanceof IOException) {
                    connect();
                    login();
                    setFileTypeAsBinary();
                }

                if( !ftpClient.changeWorkingDirectory(lastWorkingDirectory)) {
                    logErrorFtpReply();
                    throw WebinCliException.systemError(WebinCliMessage.FTP_CHANGE_DIR_ERROR.format(lastWorkingDirectory));
                }

                return null;
            }, context -> log.warn("Retrying working directory change on FTP server."), IOException.class);
        } catch (WebinCliException e) {
            throw e;
        } catch (Exception e) {
            throw WebinCliException.systemError(e, WebinCliMessage.FTP_SERVER_ERROR.text());
        }
    }

    private void reconnect() {
        connect();
        login();
        setFileTypeAsBinary();
        changeToLastWorkingDirectory();
    }

    /**
     * In addition to retry, automatically connects and logs in to FTP server if there were connection problems.
     *
     * @param retryCallable
     * @param retryLoggingRunnable
     * @return
     * @param <V>
     * @throws Exception
     */
    private <V> V executeWithReconnect(Callable<V> retryCallable, Runnable retryLoggingRunnable) throws Exception {
        return RetryUtils.executeWithRetry((RetryCallback<V, Exception>) context -> {
            if (context.getLastThrowable() != null && context.getLastThrowable() instanceof IOException) {
                reconnect();
            }

            return retryCallable.call();
        }, context -> retryLoggingRunnable.run(), IOException.class);
    }

    private void logErrorFtpReply() {
        log.error("FTP error. ReplyCode : {}, ReplyStrings : {}",
            ftpClient.getReplyCode(), Arrays.toString(ftpClient.getReplyStrings()));
    }

    private int changeToSubdir( Path subdir ) throws IOException {
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
                FTPFile[] ftpDirs = executeWithReconnect(
                    () -> ftpClient.listDirectories(),
                    () -> log.warn("Retrying retrieving directory list from FTP server."));

                if(Stream.of( ftpDirs ).noneMatch(f -> dir.equals( f.getName() ) ))
                {
                    executeWithReconnect(() -> {
                        if( !ftpClient.makeDirectory( dir ) ) {
                            logErrorFtpReply();
                            throw WebinCliException.systemError(WebinCliMessage.FTP_CREATE_DIR_ERROR.format(dir));
                        }
                        return null;
                    }, () -> log.warn("Retrying directory creation on FTP server."));
                }

                executeWithReconnect(() -> {
                    if( !ftpClient.changeWorkingDirectory( dir ) ) {
                        logErrorFtpReply();
                        throw WebinCliException.systemError(WebinCliMessage.FTP_CHANGE_DIR_ERROR.format(dir));
                    }

                    // No need to call ftpClient.printWorkingDirectory() and worry about it failing.
                    // Just append the directory name at the end of the path.
                    if (lastWorkingDirectory == null) {
                        lastWorkingDirectory = dir;
                    } else {
                        lastWorkingDirectory = lastWorkingDirectory + "/" + dir;
                    }

                    return null;
                }, () -> log.warn("Retrying working directory change on FTP server."));

                level ++;
            } catch (WebinCliException e) {
                throw e;
            } catch (Exception ex) {
                throw WebinCliException.systemError(ex, WebinCliMessage.FTP_SERVER_ERROR.text());
            }
        }
        return level;
    }

    private void storeFile(Path local, Path remote) throws IOException {
        Path subdir = 1 == remote.getNameCount() ? Paths.get( "." ): remote.subpath( 0, remote.getNameCount() - 1 );

        log.info( "Uploading file: {}", local );

        int level = changeToSubdir( subdir );

        try {
            executeWithReconnect(() -> {
                // In case of a retry, the entire file will be re-uploaded from beginning. Hence, the input stream
                // will need to be re-created as well.
                try (InputStream fileInputStream = new BufferedInputStream(Files.newInputStream(local))) {
                    if (!ftpClient.storeFile(remote.getFileName().toString(), fileInputStream)) {
                        logErrorFtpReply();
                        throw WebinCliException.systemError(WebinCliMessage.FTP_UPLOAD_ERROR.format(remote.getFileName().toString()));
                    }
                }

                return null;
            }, () -> log.warn("Retrying file upload to FTP server."));

            // go back up same number of levels to where we were before we moved to the directory where
            // the file was stored at.

            StringBuilder navigateUpPath = new StringBuilder("");

            Path beforeStorePath = Paths.get(lastWorkingDirectory);
            for( int l = 0; l < level; ++l ) {
                navigateUpPath.append("../");
                beforeStorePath = beforeStorePath.getParent();
            }

            String beforeStorePathStr = beforeStorePath.toString();

            if (navigateUpPath.length() > 0) {
                executeWithReconnect(() -> {
                    if(!ftpClient.changeWorkingDirectory(navigateUpPath.toString())) {
                        logErrorFtpReply();
                        throw WebinCliException.systemError(
                            WebinCliMessage.FTP_CHANGE_DIR_ERROR.format(navigateUpPath.toString()));
                    }

                    lastWorkingDirectory = beforeStorePathStr;

                    return null;
                }, () -> log.warn("Retrying working directory change on FTP server."));
            }
        } catch (WebinCliException ex) {
            throw ex;
        } catch (Exception ex) {
            throw WebinCliException.systemError(ex, WebinCliMessage.FTP_SERVER_ERROR.text());
        }
    }

    private static class FTPAuthenticationFailureException extends RuntimeException {
        public FTPAuthenticationFailureException() {
        }

        public FTPAuthenticationFailureException(String message) {
            super(message);
        }

        public FTPAuthenticationFailureException(String message, Throwable cause) {
            super(message, cause);
        }

        public FTPAuthenticationFailureException(Throwable cause) {
            super(cause);
        }

        public FTPAuthenticationFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
