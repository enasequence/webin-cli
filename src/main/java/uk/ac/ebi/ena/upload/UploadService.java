package uk.ac.ebi.ena.upload;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface 
UploadService
{
    void connect( String userName, String password );
    void ftpDirectory( List<File> uploadFilesList, String uploadDir, Path inputDir );
    void disconnect();
    boolean isAvaliable();
}