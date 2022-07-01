/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.submit;

import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliConfig;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class
SubmissionBundle implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String version;
    private final List<SubmissionXMLFile> xmlFileList;
    private final File submitDir;
    private final String uploadDir;
    private final List<File> uploadFileList;
    private final List<Long> uploadFileSize;
    private final String manifestMd5;
    private final File submissionBundleFile;


    public boolean
    equals( Object other )
    {
        if( other instanceof SubmissionBundle )
        {
            SubmissionBundle sb = (SubmissionBundle)other;
            return //this.version.equals( sb.version )
               /* && */ this.xmlFileList.equals( sb.xmlFileList )
                && this.submitDir.equals( sb.submitDir )
                && this.uploadDir.equals( sb.uploadDir )
                && this.uploadFileList.equals( sb.uploadFileList )
                && this.uploadFileSize.equals( sb.uploadFileSize )
                && this.manifestMd5.equals( sb.manifestMd5);
        }
        return false;
    }



    public enum
    SubmissionXMLFileType
    {
        AIO_SUBMISSION,
        SUBMISSION,
        ANALYSIS,
        RUN,
        EXPERIMENT
    }


    public static class
    SubmissionXMLFile implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private final File file;
        private final SubmissionXMLFileType type;
        private final String md5;

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

        public String
        getXml() {
            try {
                return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            }
            catch (IOException ex) {
                throw new RuntimeException(ex);
            }
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
    SubmissionBundle( File submitDir,
                      String uploadDir,
                      List<File> uploadFileList,
                      List<SubmissionXMLFile> xmlFileList,
                      String manifestMd5 ) {
        this.version = getVersion();
        this.submitDir = submitDir;
        this.uploadDir = uploadDir;
        this.uploadFileList = uploadFileList;
        this.uploadFileSize = uploadFileList.stream().sequential().map( f -> f.length() ).collect( Collectors.toList() );
        this.xmlFileList = xmlFileList;
        this.manifestMd5 = manifestMd5;
        this.submissionBundleFile = getSubmissionBundleFile(submitDir);
    }


    public String
    getManifestMd5()
    {
        return this.manifestMd5;
    }


    private String
    getVersion()
    {
        return WebinCli.class.getPackage().getImplementationVersion();
    }


    public File
    getSubmitDir()
    {
        return submitDir;
    }


    public String
    getUploadDir()
    {
        return uploadDir;
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

    public SubmissionXMLFile
    getXMLFile(SubmissionXMLFileType fileType)
    {
        return xmlFileList.stream().filter(file -> file.getType().equals(fileType)).findFirst().get();
    }


    public void
    validate( ValidationResult result )
    {
        String current = getVersion();
        if( null != current && !current.equals( this.version ) )
            result.add(ValidationMessage.info( "Program version has changed" ) );

        SubmissionXMLFile submissionFile = null;

        try {
            submissionFile = getXMLFile(SubmissionXMLFileType.AIO_SUBMISSION);
            if( !submissionFile.getFile().exists() ) {
                result.add( ValidationMessage.info( "Generated xml file not found: " + submissionFile.getFile() ) );
            }

            if( !submissionFile.getMd5().equalsIgnoreCase( FileUtils.calculateDigest( "MD5", submissionFile.getFile() ) ) ) {
                result.add(ValidationMessage.info("Generated xml file has changed: " + submissionFile.getFile()));
            }
        } catch( NoSuchElementException ex ) {
            // no need to do anything.
        } catch( Exception ex ) {
            result.add(ValidationMessage.info("Error reading generated xml file: " + submissionFile.getFile() + " " + ex.getMessage() ) );
        }

        for( int index = 0; index < uploadFileList.size(); index ++ )
        {
            File file = uploadFileList.get( index );
            Long fileSize = uploadFileSize.get( index );

            if( !file.exists() || file.isDirectory() )
            {
                result.add( ValidationMessage.info("Error reading file: " + file.getPath() ) );
                continue;
            }

            if( file.length() != fileSize )
                result.add( ValidationMessage.info("Error confirming length for: " + file.getPath() + ", expected: " + fileSize + " got: " + file.length() ) );

        }
    }

    public static SubmissionBundle read(File submitDir, String manifestMd5) {
        return new SubmissionBundleHelper( getSubmissionBundleFile( submitDir) ).read( manifestMd5 ) ;
    }

    public void write() {
        new SubmissionBundleHelper( this.submissionBundleFile ).write( this );
    }

    private static File getSubmissionBundleFile(File submitDir) {
        return new File( submitDir, WebinCliConfig.SUBMISSION_BUNDLE_FILE_SUFFIX);
    }
}
