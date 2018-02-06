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

    public FtpService(String manifestDir) {
        this.manifestDir = manifestDir;
    }

    public void connectToFtp(String userName, String password) throws FtpException {
        try {
            ftpClient.connect(SERVER, FTP_PORT);
        } catch (IOException e) {
            throw new FtpException(" - Failed to connect to FTP server");
        }
        try {
            int count = 0;
            while (!ftpClient.login(userName, password)) {
                if (count == 3)
                    throw new FtpException(" - Failed to login to ftp server (username or password error)");
                Thread.sleep(3000);
                count++;
            }
        } catch (Exception e) {
            throw new FtpException(" - Failed to login to ftp server (IO error)");
        }
    }

    public void ftpDirectory(Path validatedDirectory, String context, String name) throws FtpException {
        if (context == null || context.isEmpty() || name == null || name.isEmpty())
            throw new FtpException(" - name not supplied.");
        try {
            ftpClient.enterLocalPassiveMode();
            if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE))
                throw new FtpException(" - Failed to set binary file type on FTP server.");
            FTPFile[] ftpFilesA = ftpClient.listDirectories();
            if (!Arrays.asList(ftpFilesA).stream()
                    .anyMatch(f -> context.equalsIgnoreCase(f.getName()))) {
                if (!ftpClient.makeDirectory(context))
                    throw new FtpException(" - Failed to create remote directory: " + context);
            }
            if (!ftpClient.changeWorkingDirectory(context))
                throw new FtpException(" - Failed to change to directory: " + context);
            ftpFilesA = ftpClient.listDirectories();
            if (!Arrays.asList(ftpFilesA).stream()
                    .anyMatch(f -> name.equalsIgnoreCase(f.getName()))) {
                if (!ftpClient.makeDirectory(name))
                    throw new FtpException(" - Failed to create remote directory: " + name);
                if (!ftpClient.changeWorkingDirectory(name))
                    throw new FtpException(" - Failed to change to directory: " + name);
            } else {
                if (!ftpClient.changeWorkingDirectory(name))
                    throw new FtpException(" - Failed to change to directory: " + name);
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
                        throw new FtpException("Failed to ftp submitted data file. FTP client returned false when store attempted.");
                } catch (IOException e) {
                    throw new FtpException(" - IO Exception when attempting to ftp files file names: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new FtpException(" - Error occurred when attempting to ftp submitted data file: " + e.getMessage());
        }
    }

    public boolean checkFilesExistInUploadArea(Path validatedDirectory, String context, String name) throws FtpException {
        if (context == null || context.isEmpty() || name == null || name.isEmpty())
            throw new FtpException(" - name not supplied.");
        try {
            List<FTPFile> ftpFileList = Arrays.asList(ftpClient.listDirectories());
            if (ftpFileList == null || ftpFileList.isEmpty())
                throw new FtpException(" - Files have not been uploaded, please first run the -upload before submitting.");
            Optional<FTPFile> found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(context))
                    .findFirst();
            if (!found.isPresent())
                throw new FtpException(" - Files have not been uploaded, please first run the -upload before submitting.");
            if (!ftpClient.changeWorkingDirectory(context))
                throw new FtpException(" - Error while checjing that files have been uploaded, please try again later.");
            ftpFileList = Arrays.asList(ftpClient.listDirectories());
            if (ftpFileList == null || ftpFileList.isEmpty())
                throw new FtpException(" - Files have not been uploaded, please first run the -upload before submitting.");
            found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst();
            if (!found.isPresent())
                throw new FtpException(" - Files have not been uploaded, please first run the -upload before submitting.");
            if (!ftpClient.changeWorkingDirectory(name))
                throw new FtpException(" - Error while checjing that files have been uploaded, please try again later.");
            ftpFileList = Arrays.asList(ftpClient.listFiles());
            if (ftpFileList == null || ftpFileList.isEmpty())
                throw new FtpException(" - Files have not been uploaded, please first run the -upload before submitting.");
            List<Path> fileList = null;
            Stream<Path> stream = Files.list(validatedDirectory);
            fileList = stream.filter(f -> !f.toFile().isHidden())
                    .map(Path::getFileName)
                    .collect(Collectors.toList());
            for (Path path: fileList) {
                found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(path.getFileName().toString()))
                        .findFirst();
                if (!found.isPresent())
                    throw new FtpException(" - Files have not been uploaded, please first run the -upload before submitting.");
            }
        } catch (Exception e) {
            throw new FtpException(" - Error occurred while attempting to verify that files have been uploaded.");
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
