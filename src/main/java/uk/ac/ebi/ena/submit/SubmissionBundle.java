/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.submit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliReporter;

public class 
SubmissionBundle implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private String                  version;
    private List<SubmissionXMLFile> xmlFileList;
    private File                    submitDirectory;
    private String                  uploadDirectory;
    private List<File>              uploadFileList = Collections.emptyList();
    private List<Long>              uploadFileSize = Collections.emptyList();
    transient private File          manifest_file;
    private String                  manifest_md5;
    private String                  centerName;


    public boolean
    equals( Object other )
    {
        if( other instanceof SubmissionBundle )
        {
            SubmissionBundle sb = (SubmissionBundle)other;
            return //this.version.equals( sb.version ) 
               /* && */ this.xmlFileList.equals( sb.xmlFileList )
                && this.submitDirectory.equals( sb.submitDirectory ) 
                && this.uploadDirectory.equals( sb.uploadDirectory )
                && this.uploadFileList.equals( sb.uploadFileList )
                && this.uploadFileSize.equals( sb.uploadFileSize )
                && this.manifest_md5.equals( sb.manifest_md5 )
                && this.centerName.equals( sb.centerName );
        }
        return false;
    }
    
    
    
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
    
    
    public 
    SubmissionBundle( File              submitDirectory, 
                      String            uploadDirectory, 
                      List<File>        uploadFileList, 
                      List<SubmissionXMLFile> xmlFileList, 
                      String            centerName,
                      String            manifest_md5 ) throws NoSuchAlgorithmException, IOException
    {
        this.version = getVersion();
        this.submitDirectory = submitDirectory;
        this.uploadDirectory = uploadDirectory;
        
        this.uploadFileList = uploadFileList;
        this.uploadFileSize = uploadFileList.stream().sequential().map( f -> f.length() ).collect( Collectors.toList() );
        
        this.xmlFileList = xmlFileList;
        this.centerName = centerName;
        
        this.manifest_md5 = manifest_md5;
    }

    
    public void 
    setManifestMd5( String manifest_md5 )
    {
        this.manifest_md5 = manifest_md5;
    }
    
 
    public String 
    getManifestMd5()
    {
        return this.manifest_md5;
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
            result.append( WebinCliReporter.createValidationMessage( Severity.INFO, "Program version has changed" ) );

        if( null != this.manifest_file )
        {
            try
            {
                String current_md5 = FileUtils.calculateDigest( "MD5", this.manifest_file );
                if( null != current_md5 && !current_md5.equals( this.manifest_md5 ) )
                    result.append( WebinCliReporter.createValidationMessage( Severity.INFO, "Manifest has changed" ) );
                
            } catch( IOException | NoSuchAlgorithmException e )
            {
                result.append( WebinCliReporter.createValidationMessage( Severity.INFO, "Unable to confirm manifest checksum" ) );
            }
        }
        
        for( SubmissionXMLFile file : getXMLFileList() )
        {
            try
            {
                if( !file.getMd5().equalsIgnoreCase( FileUtils.calculateDigest( "MD5", file.getFile() ) ) ) {
                    result.append(WebinCliReporter.createValidationMessage(Severity.INFO, "Generated xml file has changed: " + file.getFile()));
                }
            } catch( FileNotFoundException e )
            {
                result.append( WebinCliReporter.createValidationMessage( Severity.INFO, "Generated xml file not found: " + file.getFile() ) );
            } catch( NoSuchAlgorithmException | IOException e )
            {
                result.append( WebinCliReporter.createValidationMessage( Severity.INFO, "Error reading generated xml file: " + file.getFile() + " " + e.getMessage() ) );
            }            
        }

        
        for( int index = 0; index < uploadFileList.size(); index ++ )
        {
            File file = uploadFileList.get( index );
            Long file_sz = uploadFileSize.get( index );
           
            if( !file.exists() || file.isDirectory() )
            {
                result.append( WebinCliReporter.createValidationMessage( Severity.INFO, "Error reading file: " + file.getPath() ) );
                continue;
            }
            
            if( file.length() != file_sz )
                result.append( WebinCliReporter.createValidationMessage( Severity.INFO, "Error confirming length for: " + file.getPath() + ", expected: " + file_sz + " got: " + file.length() ) );
        
        }
        return result;
    }
}
