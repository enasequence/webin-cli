/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.submit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliConfig;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationOrigin;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

public class SubmissionBundleHelper {
    private static final Logger log = LoggerFactory.getLogger(SubmissionBundleHelper.class);
    
    public static SubmissionBundle read(String manifestMd5, File submitDir) {
        File submissionBundleFile = new File( submitDir, WebinCliConfig.SUBMISSION_BUNDLE_FILE_SUFFIX);
        try( ObjectInputStream os = new ObjectInputStream( new FileInputStream(submissionBundleFile) ) ) {
            SubmissionBundle sb = (SubmissionBundle) os.readObject();
            
            if( null != manifestMd5 && !manifestMd5.equals( sb.getManifestMd5() ) ) {
                log.info(WebinCliMessage.SUBMISSION_BUNDLE_REVALIDATE_SUBMISSION.text());
                return null;
            }

            readXmls(sb);

            ValidationResult result = new ValidationResult(
                    new ValidationOrigin("submission bundle", submissionBundleFile.getAbsolutePath()));
            validate(sb, result);

            // TODO: potentially dangerous comparison
            if(result.count(ValidationMessage.Severity.INFO) > 0) {
                log.info(WebinCliMessage.SUBMISSION_BUNDLE_REVALIDATE_SUBMISSION.text());
                return null;
            }
            
            return sb;
            
        } catch( ClassNotFoundException | IOException e ) {
            // Submission bundle could not be read.
            log.info(WebinCliMessage.SUBMISSION_BUNDLE_VALIDATE_SUBMISSION.text());
            return null;
        }
    }

    public static void write( SubmissionBundle sb, File submitDir ) {
        File submissionBundleFile = new File( submitDir, WebinCliConfig.SUBMISSION_BUNDLE_FILE_SUFFIX);
        try( ObjectOutputStream os = new ObjectOutputStream( new FileOutputStream(submissionBundleFile) ) ) {
            computeXmlFilesChecksums(sb);

            os.writeObject( sb );
            os.flush();

            writeXmls(sb);
        } catch( IOException ex ) {
            throw WebinCliException.systemError(ex, WebinCliMessage.SUBMISSION_BUNDLE_FILE_ERROR.format(submissionBundleFile));
        }
    }

    private static void validate( SubmissionBundle sb, ValidationResult result ) {
        String current = WebinCli.getVersion();
        if( null != current && !current.equals( sb.getVersion() ) ) {
            result.add(ValidationMessage.info("Program version has changed"));
        }

        for( SubmissionBundle.SubmissionXMLFile file : sb.getXMLFileList() ) {
            if( !file.getFile().exists() ) {
                result.add( ValidationMessage.info( "Generated xml file not found: " + file.getFile() ) );
            }

            try {
                if( !file.getMd5().equalsIgnoreCase( FileUtils.calculateDigest( "MD5", file.getFile() ) ) ) {
                    result.add(ValidationMessage.info("Generated xml file has changed: " + file.getFile()));
                }
            } catch( Exception ex ) {
                result.add(ValidationMessage.info("Error reading generated xml file: " + file.getFile() + " " + ex.getMessage() ) );
            }
        }

        List<File> uploadFileList = sb.getUploadFileList();
        for( int index = 0; index < uploadFileList.size(); index ++ ) {
            File file = uploadFileList.get( index );
            Long fileSize = sb.getUploadFileSize().get( index );

            if( !file.exists() || file.isDirectory() ) {
                result.add( ValidationMessage.info("Error reading file: " + file.getPath() ) );
                continue;
            }

            if( file.length() != fileSize ) {
                result.add(ValidationMessage.info("Error confirming length for: " + file.getPath() + ", expected: " + fileSize + " got: " + file.length()));
            }
        }
    }

    private static void computeXmlFilesChecksums(SubmissionBundle sb) {
        sb.getXMLFileList().forEach(xmlFile -> {
            String md5 = FileUtils.calculateDigest("MD5", xmlFile.getXmlContent().getBytes(StandardCharsets.UTF_8));

            xmlFile.setMd5(md5);
        });
    }

    private static void readXmls(SubmissionBundle sb) {
        sb.getXMLFileList().forEach(xmlFile -> {
            try {
                xmlFile.setXmlContent(new String(Files.readAllBytes(xmlFile.getFile().toPath()), StandardCharsets.UTF_8));
            } catch(IOException ex) {
                throw WebinCliException.systemError( ex );
            }
        });
    }

    private static void writeXmls(SubmissionBundle sb) {
        sb.getXMLFileList().forEach(xmlFile -> {
            try {
                Files.write( xmlFile.getFile().toPath(), xmlFile.getXmlContent().getBytes( StandardCharsets.UTF_8 ),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
            } catch(IOException ex) {
                throw WebinCliException.systemError( ex );
            }
        });
    }
}
