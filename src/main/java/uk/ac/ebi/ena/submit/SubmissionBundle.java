package uk.ac.ebi.ena.submit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.WebinCli;

public class 
SubmissionBundle implements Serializable
{
    private static final long serialVersionUID = 1L;


    public enum
    SubmissionXMLFileType
    {
        ANALYSIS,
        RUN,
        EXPERIMENT
    };
    
    
    public static class 
    SubmissionXMLFile implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private File                  file;
        private SubmissionXMLFileType type;
        private String                md5;
        
        
        public 
        SubmissionXMLFile( SubmissionXMLFileType type, File file, String md5sum )
        {
            this.type = type;
            this.file = file;
            this.md5 = md5sum;
        }
        
        
        public String
        toString()
        {
            return String.format( "%s|%s|%s", type, file, md5 );
        }

        
        public File
        getFile()
        {
            return file;
        }

        
        public SubmissionXMLFileType
        getType()
        {
            return type;
        }
        
        
        public String
        getMd5()
        {
            return md5;
        }
    }
    
    
    private List<SubmissionXMLFile> xmlFileList;
    private File              submitDirectory;
    private String            uploadDirectory;
    private List<File>        uploadFileList = Collections.emptyList();
    private String            centerName;
    private String            version;
    
    
    public 
    SubmissionBundle( File              submitDirectory, 
                      String            uploadDirectory, 
                      List<File>        uploadFileList, 
                      List<SubmissionXMLFile> xmlFileList, 
                      String            centerName )
    {
        this.version = getVersion();
        this.submitDirectory = submitDirectory;
        this.uploadDirectory = uploadDirectory;
        this.uploadFileList = uploadFileList;
        this.xmlFileList = xmlFileList;
        this.centerName = centerName;
    }

    
    private String
    getVersion()
    {
        return WebinCli.class.getPackage().getImplementationVersion();
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
    
    
    public ValidationResult
    validate( ValidationResult result )
    {
        result = null != result ? result : new ValidationResult();
 
        String current = getVersion();
        if( null != current && !current.equals( this.version ) )
            result.append( new ValidationMessage<>( Severity.ERROR, "Incorrect version" ) );

        for( SubmissionXMLFile file : getXMLFileList() )
        {
            try
            {
                if( !file.getMd5().equalsIgnoreCase( FileUtils.calculateDigest( "MD5", file.getFile() ) ) )
                    result.append( new ValidationMessage<>( Severity.ERROR, "Unable to vaildate checksum for file " + file.getFile() ) );
            } catch( FileNotFoundException e )
            {
                result.append( new ValidationMessage<>( Severity.ERROR, "Unable to vaildate checksum for file " + file.getFile() + " " + "File not found" ) );
            } catch( NoSuchAlgorithmException | IOException e )
            {
                result.append( new ValidationMessage<>( Severity.ERROR, "Unable to vaildate checksum for file " + file.getFile() + " " + e.getMessage() ) );
            }            
        }

        return result;
    }
}