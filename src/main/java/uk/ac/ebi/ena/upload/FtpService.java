package uk.ac.ebi.ena.upload;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FtpService {
    private final static String SERVER = "webin.ebi.ac.uk";
    private final static int FTP_PORT = 21;
    private FTPClient ftpClient = new FTPClient() ;
    private String manifestDir;
    private final static String ERROR_01 = "Files have not been uploaded, please first run the -upload before submitting";
    private final static String ERROR_02 = "Error while checjing that files have been uploaded, please try again later";
    private final static String ERROR_03 = "Failed to create remote directory";
    private final static String ERROR_04 = "Failed to change to directory:";
    private final static String ERROR_05 = "Failed to connect to FTP server";
    private final static String ERROR_06 = "Failed to login to ftp server (username or password error";
    private final static String ERROR_07 = "Error occurred while attempting to verify that files have been uploaded";
    private final static String ERROR_08 = "context and/or name not supplied";
    private final static String ERROR_09 = "Failed to ftp submitted data file. FTP client returned false when store attempted";
    private final static String ERROR_10 = "Error occurred when attempting to ftp submitted data file";

    public FtpService(String manifestDir) {
        this.manifestDir = manifestDir;
    }

    public void connectToFtp(String userName, String password) throws FtpException {
        try {
            ftpClient.connect(SERVER, FTP_PORT);
        } catch (IOException e) {
            throw new FtpException(ERROR_05);
        }
        try {
            int count = 0;
            while (!ftpClient.login(userName, password)) {
                if (count == 3)
                    throw new FtpException(ERROR_06);
                Thread.sleep(3000);
                count++;
            }
        } catch (Exception e) {
            throw new FtpException(ERROR_05);
        }
    }

    public void ftpDirectory(Path validatedDirectory, String context, String name) throws FtpException {
        if (context == null || context.isEmpty() || name == null || name.isEmpty())
            throw new FtpException(ERROR_08);
        try {
            ftpClient.enterLocalPassiveMode();
            if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE))
                throw new FtpException(ERROR_10);
            FTPFile[] ftpFilesA = ftpClient.listDirectories();
            if (!Arrays.asList(ftpFilesA).stream()
                    .anyMatch(f -> context.equalsIgnoreCase(f.getName()))) {
                if (!ftpClient.makeDirectory(context))
                    throw new FtpException(ERROR_03 + context);
            }
            if (!ftpClient.changeWorkingDirectory(context))
                throw new FtpException(ERROR_04 + context);
            ftpFilesA = ftpClient.listDirectories();
            if (!Arrays.asList(ftpFilesA).stream()
                    .anyMatch(f -> name.equalsIgnoreCase(f.getName()))) {
                if (!ftpClient.makeDirectory(name))
                    throw new FtpException(ERROR_03 + name);
                if (!ftpClient.changeWorkingDirectory(name))
                    throw new FtpException(ERROR_04 + name);
            } else {
                if (!ftpClient.changeWorkingDirectory(name))
                    throw new FtpException(ERROR_04 + name);
                FTPFile[] fileTodeleteA = ftpClient.listFiles();
                if (fileTodeleteA != null && fileTodeleteA.length > 0) {
                    for (FTPFile ftpFile: fileTodeleteA)
                        ftpClient.deleteFile(ftpFile.getName());
                }
            }
            List<Path> fileList = Files.list(validatedDirectory).map(Path::getFileName).filter(f -> !f.toFile().isHidden())
                    .collect(Collectors.toList());
            for (Path path: fileList) {
                try (FileInputStream fileInputStream = new FileInputStream(validatedDirectory + File.separator + path.toFile().getName())) {
                    if (!ftpClient.storeFile(path.toFile().getName(), fileInputStream))
                        throw new FtpException(ERROR_09);
                } catch (IOException e) {
                    throw new FtpException(ERROR_10 + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new FtpException(ERROR_10 + e.getMessage());
        }
    }

    public boolean checkFilesExistInUploadArea(Path validatedDirectory, String context, String name) throws FtpException {
        if (context == null || context.isEmpty() || name == null || name.isEmpty())
            throw new FtpException(ERROR_08);
        try {
            List<FTPFile> ftpFileList = Arrays.asList(ftpClient.listDirectories());
            if (ftpFileList == null || ftpFileList.isEmpty())
                throw new FtpException(ERROR_01);
            Optional<FTPFile> found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(context))
                    .findFirst();
            if (!found.isPresent())
                throw new FtpException(ERROR_01);
            if (!ftpClient.changeWorkingDirectory(context))
                throw new FtpException(ERROR_02);
            ftpFileList = Arrays.asList(ftpClient.listDirectories());
            if (ftpFileList == null || ftpFileList.isEmpty())
                throw new FtpException(ERROR_01);
            found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst();
            if (!found.isPresent())
                throw new FtpException(ERROR_01);
            if (!ftpClient.changeWorkingDirectory(name))
                throw new FtpException(ERROR_02);
            ftpFileList = Arrays.asList(ftpClient.listFiles());
            if (ftpFileList == null || ftpFileList.isEmpty())
                throw new FtpException(ERROR_01);
            List<Path> fileList = null;
            Stream<Path> stream = Files.list(validatedDirectory);
            fileList = stream.filter(f -> !f.toFile().isHidden())
                    .map(Path::getFileName)
                    .collect(Collectors.toList());
            for (Path path: fileList) {
                found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(path.getFileName().toString()))
                        .findFirst();
                if (!found.isPresent())
                    throw new FtpException(ERROR_01);
            }
        } catch (Exception e) {
            throw new FtpException(ERROR_07);
        }
        return true;
    }

    public void disconnectFtp() {
        try {
            if (ftpClient != null && ftpClient.isConnected())
                ftpClient.disconnect();
        } catch (IOException e) {}
    }

    @Override
    protected void finalize() throws Throwable {
        disconnectFtp();
    }
}
