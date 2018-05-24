package uk.ac.ebi.ena.submit;

import java.io.File;
import java.util.Collections;
import java.util.List;

import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFileType;

public class 
SubmissionBundle
{
    public enum
    SubmissionXMLFileType
    {
        ANALYSIS,
        RUN,
        EXPERIMENT
    };
    
    
    public static class 
    SubmissionXMLFile
    {
        public final File        file;
        public final SubmissionXMLFileType type;
        
        public 
        SubmissionXMLFile( SubmissionXMLFileType type, File file )
        {
            this.type = type;
            this.file = file;
        }
        
        public String
        toString()
        {
            return String.format( "%s|%s", type, file );
        }
    }
    
    
    private List<SubmissionXMLFile> xmlFileList;
    private File              submitDirectory;
    private String            uploadDirectory;
    private List<File>        uploadFileList = Collections.emptyList();
    private String            centerName;
    

    public 
    SubmissionBundle( File              submitDirectory, 
                      String            uploadDirectory, 
                      List<File>        uploadFileList, 
                      List<SubmissionXMLFile> xmlFileList, 
                      String            centerName )
    {
        this.submitDirectory = submitDirectory;
        this.uploadDirectory = uploadDirectory;
        this.uploadFileList = uploadFileList;
        this.xmlFileList = xmlFileList;
        this.centerName = centerName;
    }

    
    public File
    getSubmitDirectory()
    {
        return submitDirectory;
    }


    public String
    getUploadDirectory()
    {
        return uploadDirectory;
    }


    public List<File>
    getUploadFileList()
    {
        return uploadFileList;
    }


    public List<SubmissionXMLFile>
    getXMLFileList()
    {
        return xmlFileList;
    }


    public String
    getCenterName()
    {
        return centerName;
    }
}
