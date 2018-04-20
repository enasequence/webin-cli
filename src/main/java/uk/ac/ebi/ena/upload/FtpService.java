package uk.ac.ebi.ena.upload;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
            throw WebinCliException.createSystemError(SYSTEM_ERROR_CONNECT);
        }
        try {
            if (!ftpClient.login(userName, password))
                throw WebinCliException.createUserError(WebinCli.AUTHENTICATION_ERROR);
        } catch (IOException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_CONNECT);
        }
    }

    public void ftpDirectory(List<File> uploadFilesList, String context, String name) {
        if (context == null || context.isEmpty() || name == null || name.isEmpty())
            throw WebinCliException.createUserError(WebinCli.MISSING_CONTEXT);
        try {
            ftpClient.enterLocalPassiveMode();
            if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE))
                throw WebinCliException.createSystemError(SYSTEM_ERROR_OTHER);
            FTPFile[] ftpFilesA = ftpClient.listDirectories();
            if (!Arrays.asList(ftpFilesA).stream()
                    .anyMatch(f -> context.equalsIgnoreCase(f.getName()))) {
                if (!ftpClient.makeDirectory(context))
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_CREATE_DIR, context);
            }
            if (!ftpClient.changeWorkingDirectory(context))
                throw WebinCliException.createSystemError(SYSTEM_ERROR_CHANGE_DIR, context);
            ftpFilesA = ftpClient.listDirectories();
            if (!Arrays.asList(ftpFilesA).stream()
                    .anyMatch(f -> name.equalsIgnoreCase(f.getName()))) {
                if (!ftpClient.makeDirectory(name))
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_CREATE_DIR, name);
                if (!ftpClient.changeWorkingDirectory(name))
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_CHANGE_DIR, name);
            } else {
                if (!ftpClient.changeWorkingDirectory(name))
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_CHANGE_DIR, name);
                FTPFile[] fileTodeleteA = ftpClient.listFiles();
                if (fileTodeleteA != null && fileTodeleteA.length > 0) {
                    for (FTPFile ftpFile: fileTodeleteA)
                        ftpClient.deleteFile(ftpFile.getName());
                }
            }
            for (File file: uploadFilesList) {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    if (!ftpClient.storeFile(file.getName(), fileInputStream))
                        throw WebinCliException.createSystemError(SYSTEM_ERROR_UPLOAD_FILE);
                }
            }
        } catch (IOException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_OTHER, e.getMessage());
        }
    }

    public boolean doFilesExistInUploadArea(List<File> uploadFilesList, String context, String assemblyName)  {
        if (context == null || context.isEmpty() || assemblyName == null || assemblyName.isEmpty())
            throw WebinCliException.createUserError(WebinCli.MISSING_CONTEXT);
        try {
            List<FTPFile> ftpFileList = Arrays.asList(ftpClient.listDirectories());
            if (ftpFileList == null || ftpFileList.isEmpty())
                return false;
            Optional<FTPFile> found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(context))
                    .findFirst();
            if (!found.isPresent())
                return false;
            if (!ftpClient.changeWorkingDirectory(context))
                throw WebinCliException.createSystemError(SYSTEM_ERROR_CHANGE_DIR);
            ftpFileList = Arrays.asList(ftpClient.listDirectories());
            if (ftpFileList == null || ftpFileList.isEmpty())
                return false;
            found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(assemblyName)).findFirst();
            if (!found.isPresent())
                return false;
            if (!ftpClient.changeWorkingDirectory(assemblyName))
                throw WebinCliException.createSystemError(SYSTEM_ERROR_CHANGE_DIR);
            ftpFileList = Arrays.asList(ftpClient.listFiles());
            if (ftpFileList == null || ftpFileList.isEmpty())
                return false;
            for (File fileName: uploadFilesList) {
                found = ftpFileList.stream().filter(f -> f.getName().equalsIgnoreCase(fileName.getName()))
                        .findFirst();
                if (!found.isPresent())
                    return false;
            }
        } catch (IOException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_CHECK);
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
