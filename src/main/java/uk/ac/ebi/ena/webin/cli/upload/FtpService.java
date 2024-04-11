/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.upload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;

public class FtpService implements UploadService {
  private static final String SERVER = "webin2.ebi.ac.uk";
  private static final int FTP_PORT = 21;

  private static final Logger log = LoggerFactory.getLogger(FtpService.class);

  // FTP team has recommended that the client uses TLS 1.2. Java 8 should use TLS 1.2 by default
  // anyway but it is being
  // forced here just in case.
  private final FTPSClient ftpClient = new FTPSClient("TLSv1.2");

  private String username;
  private String password;

  private Path ftpServerWorkingDir;

  @Override
  public void connect(String userName, String password) throws WebinCliException {
    this.username = userName;
    this.password = password;

    ftpClient.setRemoteVerificationEnabled(false);
    ftpClient.setConnectTimeout(10_000);
    ftpClient.setDefaultTimeout(10_000);
    ftpClient.setDataTimeout(10_000);
    ftpClient.setControlKeepAliveTimeout(Duration.ofMinutes(1));
    ftpClient.setControlKeepAliveReplyTimeout(Duration.ofSeconds(5));

    connectToFtpServer();
  }

  // TODO verbose possible issues with file/folder permissions
  @Override
  public void upload(List<File> uploadFilesList, String uploadDir, Path inputDir)
      throws WebinCliException {
    if (null == uploadDir || uploadDir.isEmpty()) {
      throw WebinCliException.userError(WebinCliMessage.FTP_UPLOAD_DIR_ERROR.text());
    }

    try {
      changeToSubdir(Paths.get(uploadDir));

      FTPFile[] deleteFilesList =
          executeWithReconnect(
              () -> ftpClient.listFiles(),
              () -> log.warn("Retrying retrieving file list from FTP server."));

      if (deleteFilesList != null && deleteFilesList.length > 0) {
        for (FTPFile ftpFile : deleteFilesList) {
          executeWithReconnect(
              () -> ftpClient.deleteFile(ftpFile.getName()),
              () -> log.warn("Retrying file deletion on FTP server."));
        }
      }

      for (File localFile : uploadFilesList) {
        String fileName = localFile.toPath().getFileName().toString();

        storeFile(localFile.toPath(), fileName);
      }
    } catch (WebinCliException e) {
      throw e;
    } catch (Exception ex) {
      throw WebinCliException.systemError(ex, WebinCliMessage.FTP_SERVER_ERROR.text());
    }
  }

  @Override
  public void disconnect() {
    if (ftpClient.isConnected()) {
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

  private void connectToFtpServer() throws WebinCliException {
    reconnect();
  }

  private void reconnect() throws WebinCliException {
    log.info("Connecting to FTP server : {}", SERVER);

    try {
      RetryUtils.executeWithRetry(
          (RetryCallback<Void, Exception>)
              context -> {
                ftpClient.connect(SERVER, FTP_PORT);

                // As connect() method does not return anything. It is advised to check the reply
                // code after calling
                // it. Here we are only interested in logging the FTP reply if the return code is
                // either in the
                // negative reply range (4xx and 5xx) or protected reply range (6xx).
                if (ftpClient.getReplyCode() >= 400 && ftpClient.getReplyCode() <= 699) {
                  logLastFtpReply();
                }

                ftpClient.enterLocalPassiveMode();

                if (!ftpClient.login(username, password)) {
                  logLastFtpReply();
                  throw WebinCliException.userError(
                      WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format("FTP"));
                }

                if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
                  logLastFtpReply();
                  throw WebinCliException.systemError(WebinCliMessage.FTP_SERVER_ERROR.text());
                }

                // Upon reconnect change FTP server's working directory back to what it was before.
                if (ftpServerWorkingDir != null) {
                  if (!ftpClient.changeWorkingDirectory(
                      FileUtils.replaceIncompatibleFileSeparators(
                          ftpServerWorkingDir.toString()))) {
                    logLastFtpReply();
                    throw WebinCliException.systemError(
                        WebinCliMessage.FTP_CHANGE_DIR_ERROR.format(ftpServerWorkingDir));
                  }
                } else {
                  // FTP server's working directory is '/' after a fresh connection. So set this as
                  // current working directory.
                  ftpServerWorkingDir = Paths.get("/");
                }

                return null;
              },
          context -> log.warn("Retrying connecting to FTP server."),
          IOException.class);
    } catch (WebinCliException e) {
      throw e;
    } catch (Exception e) {
      throw WebinCliException.systemError(e, WebinCliMessage.FTP_CONNECT_ERROR.text());
    }
  }

  private void changeToSubdir(Path subdir) throws WebinCliException {
    for (int l = 0; l < subdir.getNameCount(); ++l) {
      String dir = subdir.subpath(l, l + 1).getFileName().toString();

      if (dir.equals(".")) {
        continue;
      }

      if (dir.equals("..")) {
        throw WebinCliException.systemError(WebinCliMessage.FTP_CHANGE_DIR_ERROR.format(dir));
      }

      try {
        FTPFile[] ftpDirs =
            executeWithReconnect(
                () -> ftpClient.listDirectories(),
                () -> log.warn("Retrying retrieving directory list from FTP server."));

        if (Stream.of(ftpDirs).noneMatch(f -> dir.equals(f.getName()))) {
          executeWithReconnect(
              () -> {
                if (!ftpClient.makeDirectory(dir)) {
                  logLastFtpReply();
                  throw WebinCliException.systemError(
                      WebinCliMessage.FTP_CREATE_DIR_ERROR.format(dir));
                }
                return null;
              },
              () -> log.warn("Retrying directory creation on FTP server."));
        }

        executeWithReconnect(
            () -> {
              if (!ftpClient.changeWorkingDirectory(dir)) {
                logLastFtpReply();
                throw WebinCliException.systemError(
                    WebinCliMessage.FTP_CHANGE_DIR_ERROR.format(dir));
              }

              ftpServerWorkingDir = ftpServerWorkingDir.resolve(dir);

              return null;
            },
            () -> log.warn("Retrying working directory change on FTP server."));

      } catch (WebinCliException e) {
        throw e;
      } catch (Exception ex) {
        throw WebinCliException.systemError(ex, WebinCliMessage.FTP_SERVER_ERROR.text());
      }
    }
  }

  private void storeFile(Path localFilePath, String remoteFileName) throws WebinCliException {
    log.info("Uploading file: {}", localFilePath);

    try {
      executeWithReconnect(
          () -> {
            // In case of a retry, the entire file will be re-uploaded from beginning. Hence, the
            // input stream
            // will need to be re-created as well.
            try (InputStream fileInputStream =
                new BufferedInputStream(Files.newInputStream(localFilePath))) {
              if (!ftpClient.storeFile(remoteFileName, fileInputStream)) {
                logLastFtpReply();
                throw WebinCliException.systemError(
                    WebinCliMessage.FTP_UPLOAD_ERROR.format(remoteFileName));
              }
            }

            return null;
          },
          () -> log.warn("Retrying file upload to FTP server."));
    } catch (WebinCliException ex) {
      throw ex;
    } catch (Exception ex) {
      throw WebinCliException.systemError(ex, WebinCliMessage.FTP_SERVER_ERROR.text());
    }
  }

  private void logLastFtpReply() {
    log.error(
        "Last received FTP Reply. ReplyCode : {}, ReplyStrings : {}",
        ftpClient.getReplyCode(),
        Arrays.toString(ftpClient.getReplyStrings()));
  }

  /**
   * In addition to retry, automatically connects and logs in to FTP server if there were connection
   * problems.
   *
   * @param retryCallable
   * @param retryLoggingRunnable
   * @return
   * @param <V>
   * @throws Exception
   */
  private <V> V executeWithReconnect(Callable<V> retryCallable, Runnable retryLoggingRunnable)
      throws Exception {
    return RetryUtils.executeWithRetry(
        (RetryCallback<V, Exception>)
            context -> {
              if (context.getLastThrowable() != null
                  && context.getLastThrowable() instanceof IOException) {
                reconnect();
              }

              return retryCallable.call();
            },
        context -> retryLoggingRunnable.run(),
        IOException.class);
  }
}
