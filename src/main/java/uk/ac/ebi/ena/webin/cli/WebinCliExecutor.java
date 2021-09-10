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

import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.service.IgnoreErrorsService;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;

public class
WebinCliExecutor<M extends Manifest, R extends ValidationResponse>
{
    private final WebinCliContext context;
    private final WebinCliParameters parameters;
    private final ManifestReader<M> manifestReader;
    private final Validator<M,R> validator;
    private final XmlWriter<M, R> xmlWriter;

    private File validationDir;
    private File processDir;
    private File submitDir;
    protected R validationResponse;

    private static final String REPORT_FILE = "webin-cli.report";

    private static final Logger log = LoggerFactory.getLogger(WebinCliExecutor.class);


    public WebinCliExecutor(WebinCliContext context, WebinCliParameters parameters, ManifestReader<M> manifestReader, XmlWriter<M, R> xmlWriter, Validator<M,R> validator) {
        this.context = context;
        this.parameters = parameters;
        this.manifestReader = manifestReader;
        this.xmlWriter = xmlWriter;
        this.validator = validator;
    }

    public final void readManifest() {
        this.validationDir = WebinCli.createOutputDir(parameters.getOutputDir(), ".");

        File manifestReportFile = getManifestReportFile();
        manifestReportFile.delete();

        try {
            getManifestReader().readManifest(
                    getParameters().getInputDir().toPath(),
                    getParameters().getManifestFile(),
                    getManifestReportFile());
        } catch (WebinCliException ex) {
            throw ex;
        } catch (Exception ex) {
            throw WebinCliException.systemError(ex, WebinCliMessage.EXECUTOR_INIT_ERROR.format(ex.getMessage()));
        }

        if (manifestReader == null || !manifestReader.getValidationResult().isValid()) {
            throw WebinCliException.userError( WebinCliMessage.MANIFEST_READER_INVALID_MANIFEST_FILE_ERROR.format(manifestReportFile.getPath()) );
        }
    }

    public final void validateSubmission() {
        this.validationDir = createSubmissionDir(WebinCliConfig.VALIDATE_DIR );
        this.processDir = createSubmissionDir(WebinCliConfig.PROCESS_DIR );

        M manifest = getManifestReader().getManifest();

        setIgnoreErrors(manifest);

        if(!manifest.getFiles().get().isEmpty()) {
            for (SubmissionFile subFile : (List<SubmissionFile>) manifest.getFiles().get()) {
                subFile.setReportFile(Paths.get(getValidationDir().getPath()).resolve(subFile.getFile().getName() + ".report").toFile());
            }
        }
        manifest.setReportFile(getSubmissionReportFile());
        manifest.setProcessDir(getProcessDir());
        manifest.setAuthToken(getAuthTokenFromParam());
        manifest.setTestMode(getTestModeFromParam());

        try {
            validationResponse = getValidator().validate(manifest);
        } catch (RuntimeException ex) {
            throw WebinCliException.systemError(ex);
        }
        if(validationResponse != null && validationResponse.getStatus() == ValidationResponse.status.VALIDATION_ERROR) {
            throw WebinCliException.validationError("");
        }
    }

    private void setIgnoreErrors(M manifest) {
        manifest.setIgnoreErrors(false);
        try {
            IgnoreErrorsService ignoreErrorsService = new IgnoreErrorsService.Builder()
                    .setCredentials(getParameters().getWebinServiceUserName(),
                            getParameters().getPassword())
                    .setTest(getParameters().isTest())
                    .build();

            manifest.setIgnoreErrors(ignoreErrorsService.getIgnoreErrors(getContext().name(), getSubmissionName()));
        }
        catch (RuntimeException ex) {
            log.warn(WebinCliMessage.IGNORE_ERRORS_SERVICE_SYSTEM_ERROR.text());
        }
    }

    public final void prepareSubmissionBundle() {
        this.submitDir = createSubmissionDir(WebinCliConfig.SUBMIT_DIR );

        List<File> uploadFileList = new ArrayList<>();
        List< SubmissionBundle.SubmissionXMLFile > xmlFileList = new ArrayList<>();

        Path uploadDir = Paths
                .get( this.parameters.isTest() ? "webin-cli-test" : "webin-cli" )
                .resolve( String.valueOf( this.context ) )
                .resolve( WebinCli.getSafeOutputDir( getSubmissionName() ) );

        uploadFileList.addAll(getManifestReader().getManifest().files().files());

        Map<SubmissionBundle.SubmissionXMLFileType, String> xmls =
                xmlWriter.createXml(
                        getManifestReader().getManifest(),
                        getValidationResponse(),
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
            throw WebinCliException.systemError(WebinCliMessage.EXECUTOR_INIT_ERROR.format("Missing submission name."));
        }
        File newDir = WebinCli.createOutputDir( parameters.getOutputDir(), String.valueOf( context ), getSubmissionName(), dir);
        if (!FileUtils.emptyDirectory(newDir)) {
            throw WebinCliException.systemError(WebinCliMessage.EXECUTOR_EMPTY_DIRECTORY_ERROR.format(newDir));
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

    public Validator<M,R> getValidator() {
        return validator;
    }

    public XmlWriter<M, R> getXmlWriter() {
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

    public File
    getSubmissionReportFile() {
        return Paths.get(getValidationDir().getPath()).resolve(REPORT_FILE).toFile();
    }

    public File getManifestReportFile() {
      File manifestFile = getParameters().getManifestFile();

      if (this.validationDir == null || !this.validationDir.isDirectory()) {
        throw WebinCliException.systemError(
                WebinCliMessage.CLI_INVALID_REPORT_DIR_ERROR.format(manifestFile.getName()));
      }
      return new File(
              this.validationDir,
              Paths.get(manifestFile.getName()).getFileName().toString()
                      + WebinCliConfig.REPORT_FILE_SUFFIX);
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

    public R getValidationResponse() {
        return validationResponse;
    }
    
    
    private String getAuthTokenFromParam(){
        return this.parameters.getAuthToken();
    }

    private boolean getTestModeFromParam(){
        return this.parameters.isTest();
    }
}
