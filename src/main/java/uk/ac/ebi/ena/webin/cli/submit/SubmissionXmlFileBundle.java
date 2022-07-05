package uk.ac.ebi.ena.webin.cli.submit;

import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliConfig;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class SubmissionXmlFileBundle extends SubmissionBundle {
    private final String version;

    private final File submissionBundleFile;

    public SubmissionXmlFileBundle( File submitDir,
                             String uploadDir,
                             List<File> uploadFileList,
                             List<SubmissionBundle.SubmissionXMLFile> xmlFileList,
                             String manifestMd5 ) {
        super(submitDir, uploadDir, uploadFileList, xmlFileList, manifestMd5);
        this.version = getVersion();
        this.submissionBundleFile = new File( submitDir, WebinCliConfig.SUBMISSION_BUNDLE_FILE_SUFFIX);

        computeChecksums();
        writeXmls();
    }

    private String getVersion() {
        return WebinCli.class.getPackage().getImplementationVersion();
    }

    public File getSubmissionBundleFile() {
        return submissionBundleFile;
    }

    public void validate( ValidationResult result ) {
        String current = getVersion();
        if( null != current && !current.equals( this.version ) ) {
            result.add(ValidationMessage.info("Program version has changed"));
        }

        for( SubmissionXMLFile file : getXMLFileList() ) {
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

        for( int index = 0; index < uploadFileList.size(); index ++ ) {
            File file = uploadFileList.get( index );
            Long fileSize = uploadFileSize.get( index );

            if( !file.exists() || file.isDirectory() ) {
                result.add( ValidationMessage.info("Error reading file: " + file.getPath() ) );
                continue;
            }

            if( file.length() != fileSize ) {
                result.add(ValidationMessage.info("Error confirming length for: " + file.getPath() + ", expected: " + fileSize + " got: " + file.length()));
            }
        }
    }

    private void computeChecksums() {
        xmlFileList.forEach(xmlFile -> {
            String md5 = FileUtils.calculateDigest("MD5", new ByteArrayInputStream(
                xmlFile.getXmlContent().getBytes(StandardCharsets.UTF_8)));

            xmlFile.setMd5(md5);
        });
    }

    private void writeXmls() {
        xmlFileList.forEach(xmlFile -> {
            String xmlFileName = xmlFile.getType().name().toLowerCase() + ".xml";
            Path xmlFilePath = getSubmitDir().toPath().resolve( xmlFileName );

            try {
                Files.write( xmlFilePath, xmlFile.getXmlContent().getBytes( StandardCharsets.UTF_8 ),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
            } catch(IOException ex) {
                throw WebinCliException.systemError( ex );
            }
        });
    }
}
