package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class 
SubmissionBundle
{
    public enum
    PAYLOAD_TYPE
    {
        ANALYSIS,
        RUN
    };
    
    private PAYLOAD_TYPE payloadType;
    private File         submitDirectory;
    private String       uploadDirectory;
    private List<File>   uploadFileList = Collections.emptyList();
    private File         xmlFile;
    private String       centerName;
    

    public 
    SubmissionBundle( File         submitDirectory, 
                      String       uploadDirectory, 
                      List<File>   uploadFileList, 
                      File         xmlFile, 
                      PAYLOAD_TYPE payloadType, 
                      String       centerName )
    {
        this.submitDirectory = submitDirectory;
        this.uploadDirectory = uploadDirectory;
        this.uploadFileList = uploadFileList;
        this.xmlFile = xmlFile;
        this.payloadType = payloadType;
        this.centerName = centerName;
    }

    
    public PAYLOAD_TYPE
    getPayloadType()
    {
        return payloadType;
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


    public File
    getXMLFile()
    {
        return xmlFile;
    }


    public String
    getCenterName()
    {
        return centerName;
    }
}
