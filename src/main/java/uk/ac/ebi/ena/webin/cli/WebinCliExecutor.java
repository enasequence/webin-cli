/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.webin.cli.logger.ValidationMessageLogger;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.reporter.ValidationMessageReporter;
import uk.ac.ebi.ena.webin.cli.service.IgnoreErrorsService;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;

public class
WebinCliExecutor<M extends Manifest>
{
    private final WebinCliContext context;
    private final WebinCliParameters parameters;
    private final ManifestReader<M> manifestReader;
    private final XmlWriter<M> xmlWriter;

    private File validationDir;
    private File processDir;
    private File submitDir;
    protected ValidationResponse validationResponse;
    //TODO : move it to a common place if used in multiple places
    private static final String ERROR_FILE = "webin-cli.report";

    private static final Logger log = LoggerFactory.getLogger(WebinCliExecutor.class);


    public WebinCliExecutor(WebinCliContext context, WebinCliParameters parameters, ManifestReader<M> manifestReader, XmlWriter<M> xmlWriter) {
        this.context = context;
        this.parameters = parameters;
        this.manifestReader = manifestReader;
        this.xmlWriter = xmlWriter;
    }

    protected void validateSubmissionForContext(){
        M manifest = getManifestReader().getManifest();

        if(!manifest.getFiles().get().isEmpty()) {
            for (SubmissionFile subFile : (List<SubmissionFile>) manifest.getFiles().get()) {
                subFile.setReportFile(Paths.get(getValidationDir().getPath()).resolve(subFile.getFile().getName() + ".report").toFile());
            }
        }
        manifest.setReportFile(Paths.get(getValidationDir().getPath()).resolve(ERROR_FILE).toFile());
        manifest.setProcessDir(getProcessDir());
        ValidationResponse response;
        try {
            response = new SubmissionValidator().validate(manifest);
        } catch (RuntimeException ex) {
            throw WebinCliException.systemError(ex);
        }
        if(response != null && response.getStatus() == ValidationResponse.status.VALIDATION_ERROR) {
            throw WebinCliException.validationError("");
        }
        validationResponse = response;
    }

    public final void readManifest() {
        this.validationDir = WebinCli.createOutputDir(parameters.getOutputDir(), ".");

        File manifestFile = getParameters().getManifestFile();
        File manifestReportFile = getReportFile(manifestFile.getName());
        manifestReportFile.delete();

        try {
            getManifestReader().readManifest( getParameters().getInputDir().toPath(), getParameters().getManifestFile()  );
        } catch (WebinCliException ex) {
            throw ex;
        } catch (Exception ex) {
            throw WebinCliException.systemError(ex, WebinCliMessage.Cli.INIT_ERROR.format(ex.getMessage()));
        } finally {
            if (manifestReader != null && !manifestReader.getValidationResult().isValid()) {
                ValidationMessageLogger.log(manifestReader.getValidationResult());
                try (ValidationMessageReporter reporter = new ValidationMessageReporter(manifestReportFile)) {
                    reporter.write(manifestReader.getValidationResult());
                }
            }
        }

        if (manifestReader == null || !manifestReader.getValidationResult().isValid()) {
            throw WebinCliException.userError( WebinCliMessage.Manifest.INVALID_MANIFEST_FILE_ERROR.format(manifestReportFile.getPath()) );
        }
    }

    public final void validateSubmission() {
        this.validationDir = createSubmissionDir(WebinCliConfig.VALIDATE_DIR );
        this.processDir = createSubmissionDir(WebinCliConfig.PROCESS_DIR );

        M manifest = getManifestReader().getManifest();

        manifest.setIgnoreErrors(false);
        try {
            IgnoreErrorsService ignoreErrorsService = new IgnoreErrorsService.Builder()
                    .setCredentials(getParameters().getUsername(),
                            getParameters().getPassword())
                    .setTest(getParameters().isTestMode())
                    .build();

            manifest.setIgnoreErrors(ignoreErrorsService.getIgnoreErrors(getContext().name(), getSubmissionName()));
        }
        catch (RuntimeException ex) {
            log.warn(WebinCliMessage.Service.IGNORE_ERRORS_SERVICE_SYSTEM_ERROR.format());
        }

        validateSubmissionForContext();
    }

    public final void prepareSubmissionBundle() {
        this.submitDir = createSubmissionDir(WebinCliConfig.SUBMIT_DIR );

        List<File> uploadFileList = new ArrayList<>();
        List< SubmissionBundle.SubmissionXMLFile > xmlFileList = new ArrayList<>();

        Path uploadDir = Paths
                .get( this.parameters.isTestMode() ? "webin-cli-test" : "webin-cli" )
                .resolve( String.valueOf( this.context ) )
                .resolve( WebinCli.getSafeOutputDir( getSubmissionName() ) );

        uploadFileList.addAll(getManifestReader().getManifest().files().files());

        Map<SubmissionBundle.SubmissionXMLFileType, String> xmls =
                xmlWriter.createXml(
                        getManifestReader().getManifest(),
                        validationResponse,
                        getParameters().getCenterName(),
                        getSubmissionTitle(),
                        getSubmissionAlias(),
                        getParameters().getInputDir().toPath(), uploadDir);

        xmls.forEach((fileType, xml) -> {
            String xmlFileName = fileType.name().toLowerCase() + ".xml";
            Path xmlFile = getSubmitDir().toPath().resolve( xmlFileName );

            try
            {
                Files.write( xmlFile, xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
            }
            catch(IOException ex) {
                throw WebinCliException.systemError( ex );
            }

            xmlFileList.add( new SubmissionBundle.SubmissionXMLFile( fileType, xmlFile.toFile(), FileUtils.calculateDigest( "MD5", xmlFile.toFile() ) ) );
        });

        SubmissionBundle submissionBundle = new SubmissionBundle(
                getSubmitDir(),
                uploadDir.toString(),
                uploadFileList,
                xmlFileList,
                getParameters().getCenterName(),
                calculateManifestMd5() );

        submissionBundle.write();
    }

    public SubmissionBundle readSubmissionBundle() {
        return SubmissionBundle.read(getSubmitDir(), calculateManifestMd5());
    }

    private File createSubmissionDir(String dir) {
        if (StringUtils.isBlank(getSubmissionName())) {
            throw WebinCliException.systemError(WebinCliMessage.Cli.INIT_ERROR.format("Missing submission name."));
        }
        File newDir = WebinCli.createOutputDir( parameters.getOutputDir(), String.valueOf( context ), getSubmissionName(), dir);
        if (!FileUtils.emptyDirectory(newDir)) {
            throw WebinCliException.systemError(WebinCliMessage.Cli.EMPTY_DIRECTORY_ERROR.format(newDir));
        }
        return newDir;
    }

    private String calculateManifestMd5() {
        return FileUtils.calculateDigest( "MD5", parameters.getManifestFile());
    }


    // TODO: remove
    public WebinCliContext getContext() {
        return context;
    }

    public ManifestReader<M> getManifestReader() {
        return manifestReader;
    }

    public XmlWriter<M> getXmlWriter() {
        return xmlWriter;
    }

    public File getValidationDir() {
        return validationDir;
    }

    public File getProcessDir() {
        return processDir;
    }

    public File getSubmitDir() {
        return submitDir;
    }

    public WebinCliParameters
    getParameters()
    {
        return this.parameters;
    }

    public String
    getSubmissionName()
    {
        String name = manifestReader.getManifest().getName();
        if (name != null) {
            return name.trim().replaceAll("\\s+", "_");
        }
        return name;
    }

    public String
    getSubmissionAlias() {
        String alias = "webin-" + context.name() + "-" + getSubmissionName();
        return alias;
    }

    public String
    getSubmissionTitle() {
        String title = context.getTitlePrefix() + ": " + getSubmissionName();
        return title;
    }

    protected File
    getReportFile( String filename )
    {
        return WebinCli.getReportFile( this.validationDir, filename, WebinCliConfig.REPORT_FILE_SUFFIX );
    }
}