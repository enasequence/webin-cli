package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class 
SubmissionBundle
{
    private File       submitDirectory;
    private String     uploadDirectory;
    private List<File> uploadFileList = Collections.emptyList();
    private File       xmlFile;


    public 
    SubmissionBundle( File submitDirectory, String uploadDirectory, List<File> uploadFileList, File xmlFile )
    {
        this.submitDirectory = submitDirectory;
        this.uploadDirectory = uploadDirectory;
        this.uploadFileList = uploadFileList;
        this.xmlFile = xmlFile;
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
}
