package uk.ac.ebi.ena.upload;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

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
    private final static String SYSTEM_ERROR_CONNECT = "Failed to connect to the Webin file upload area.";
    private final static String USER_ERROR_NO_DIR = "The files have not been uploaded. The directory has not been created. " +
            "Please use the -validate and -upload options before using the -submit option.";
    private final static String USER_ERROR_NO_FILE = "The files have not been uploaded. " +
            "Please use the -validate and -upload options before using the -submit option.";
    private final static String SYSTEM_ERROR_CHECK = "Failed to check if files have been uploaded.";
    private final static String SYSTEM_ERROR_CREATE_DIR = "Failed to create directory in Webin file upload area.";
    private final static String SYSTEM_ERROR_CHANGE_DIR = "Failed to access directory in Webin file upload area.";
    private final static String SYSTEM_ERROR_UPLOAD_FILE = "Failed to upload files to Webin file upload area.";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when uploading files to Webin file upload area.";

    public void connectToFtp(String userName, String password) {
        try {
            ftpClient.connect(SERVER, FTP_PORT);
        } catch (IOException e) {
            throw new WebinCliException(SYSTEM_ERROR_CONNECT, WebinCliException.ErrorType.SYSTEM_ERROR);
        }
        try {
            if (!ftpClient.login(userName, password))
                throw new WebinCliException(WebinCli.AUTHENTICATION_ERROR, WebinCliException.ErrorType.USER_ERROR);
        } catch (IOException e) {
            throw new WebinCliException(SYSTEM_ERROR_CONNECT, WebinCliException.ErrorType.SYSTEM_ERROR);
        }
    }

    public void ftpDirectory(Path uploadDirectory, String context, String name) {
        if (context == null || context.isEmpty() || name == null || name.isEmpty())
            throw new WebinCliException(WebinCli.MISSING_CONTEXT, WebinCliException.ErrorType.USER_ERROR);
        try {
            ftpClient.enterLocalPassiveMode();
            if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE))
                throw new WebinCliException(SYSTEM_ERROR_OTHER, WebinCliException.ErrorType.SYSTEM_ERROR);
            FTPFile[] ftpFilesA = ftpClient.listDirectories();
            if (!Arrays.asList(ftpFilesA).stream()
                    .anyMatch(f -> context.equalsIgnoreCase(f.getName()))) {
                if (!ftpClient.makeDirectory(context))
                    throw new WebinCliException(SYSTEM_ERROR_CREATE_DIR, context, WebinCliException.ErrorType.SYSTEM_ERROR);
            }
            if (!ftpClient.changeWorkingDirectory(context))
                throw new WebinCliException(SYSTEM_ERROR_CHANGE_DIR, context, WebinCliException.ErrorType.SYSTEM_ERROR);
            ftpFilesA = ftpClient.listDirectories();
            if (!Arrays.asList(ftpFilesA).stream()
                    .anyMatch(f -> name.equalsIgnoreCase(f.getName()))) {
                if (!ftpClient.makeDirectory(name))
                    throw new WebinCliException(SYSTEM_ERROR_CREATE_DIR, name, WebinCliException.ErrorType.SYSTEM_ERROR);
                if (!ftpClient.changeWorkingDirectory(name))
                    throw new WebinCliException(SYSTEM_ERROR_CHANGE_DIR, name, WebinCliException.ErrorType.SYSTEM_ERROR);
            } else {
                if (!ftpClient.changeWorkingDirectory(name))
                    throw new WebinCliException(SYSTEM_ERROR_CHANGE_DIR, name, WebinCliException.ErrorType.SYSTEM_ERROR);
                FTPFile[] fileTodeleteA = ftpClient.listFiles();
                if (fileTodeleteA != null && fileTodeleteA.length > 0) {
                    for (FTPFile ftpFile: fileTodeleteA)
                        ftpClient.deleteFile(ftpFile.getName());
                }
            }
            List<Path> fileList = Files.list(uploadDirectory).map(Path::getFileName).filter(f -> !f.toFile().isHidden())
                    .collect(Collectors.toList());
            for (Path path: fileList) {
                FileInputStream fileInputStream = new FileInputStream(uploadDirectory + File.separator + path.toFile().getName());
                if (!ftpClient.storeFile(path.toFile().getName(), fileInputStream))
                    throw new WebinCliException(SYSTEM_ERROR_UPLOAD_FILE, WebinCliException.ErrorType.SYSTEM_ERROR);
            }
        } catch (IOException e) {
            throw new WebinCliException(SYSTEM_ERROR_OTHER, e.getMessage(), WebinCliException.ErrorType.SYSTEM_ERROR);
        }
    }

    public boolean checkFilesExistInUploadArea(Path validatedDirectory, String context, String name)  {
        if (context == null || context.isEmpty() || name == null || name.isEmpty())
            throw new WebinCliException(WebinCli.MISSING_CONTEXT, WebinCliException.ErrorType.USER_ERROR);
        try {
            List<FTPFile> ftpFileList = Arrays.asList(ftpClient.listDirectories());
            if (ftpFileList == null || ftpFileList.isEmpty())
                throw new WebinCliException(USER_ERROR_NO_FILE, WebinCliException.ErrorType.USER_ERROR);
            Optional<FTPFile> found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(context))
                    .findFirst();
            if (!found.isPresent())
                throw new WebinCliException(USER_ERROR_NO_FILE, WebinCliException.ErrorType.USER_ERROR);
            if (!ftpClient.changeWorkingDirectory(context))
                throw new WebinCliException(USER_ERROR_NO_DIR, WebinCliException.ErrorType.USER_ERROR);
            ftpFileList = Arrays.asList(ftpClient.listDirectories());
            if (ftpFileList == null || ftpFileList.isEmpty())
                throw new WebinCliException(USER_ERROR_NO_FILE, WebinCliException.ErrorType.USER_ERROR);
            found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst();
            if (!found.isPresent())
                throw new WebinCliException(USER_ERROR_NO_FILE, WebinCliException.ErrorType.USER_ERROR);
            if (!ftpClient.changeWorkingDirectory(name))
                throw new WebinCliException(USER_ERROR_NO_DIR, WebinCliException.ErrorType.USER_ERROR);
            ftpFileList = Arrays.asList(ftpClient.listFiles());
            if (ftpFileList == null || ftpFileList.isEmpty())
                throw new WebinCliException(USER_ERROR_NO_FILE, WebinCliException.ErrorType.USER_ERROR);
            List<Path> fileList = null;
            Stream<Path> stream = Files.list(validatedDirectory);
            fileList = stream.filter(f -> !f.toFile().isHidden())
                    .map(Path::getFileName)
                    .collect(Collectors.toList());
            for (Path path: fileList) {
                found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(path.getFileName().toString()))
                        .findFirst();
                if (!found.isPresent())
                    throw new WebinCliException(USER_ERROR_NO_FILE, WebinCliException.ErrorType.USER_ERROR);
            }
        } catch (IOException e) {
            throw new WebinCliException(SYSTEM_ERROR_CHECK, WebinCliException.ErrorType.SYSTEM_ERROR);
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
