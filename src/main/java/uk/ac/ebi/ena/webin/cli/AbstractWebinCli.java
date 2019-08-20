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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.ena.webin.cli.logger.ValidationMessageLogger;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.reporter.ValidationMessageReporter;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;

public abstract class
AbstractWebinCli<M extends ManifestReader> implements WebinCliValidator
{
    private final WebinCliContext context;
    private final WebinCliParameters parameters;
    private final M manifestReader;

    private File validationDir;
    private File processDir;
    private File submitDir;

    public AbstractWebinCli(WebinCliContext context, WebinCliParameters parameters, M manifestReader) {
        this.context = context;
        this.parameters = parameters;
        this.manifestReader = manifestReader;
    }

    protected abstract void readManifestForContext();

    protected abstract void validateSubmissionForContext();

    protected abstract void prepareSubmissionBundleForContext(
            Path uploadDir,
            List<File> uploadFileList,
            List<SubmissionBundle.SubmissionXMLFile> xmlFileList);

    @Override
    public final void readManifest() {
        this.validationDir = WebinCli.createOutputDir(parameters.getOutputDir(), ".");

        File manifestFile = getParameters().getManifestFile();
        File manifestReportFile = getReportFile(manifestFile.getName());
        manifestReportFile.delete();

        try {
            readManifestForContext();
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

    @Override
    public final void validateSubmission() {
        this.validationDir = createSubmissionDir(WebinCliConfig.VALIDATE_DIR );
        this.processDir = createSubmissionDir(WebinCliConfig.PROCESS_DIR );

        validateSubmissionForContext();
    }

    @Override
    public final void prepareSubmissionBundle() {
        this.submitDir = createSubmissionDir(WebinCliConfig.SUBMIT_DIR );

        List<File> uploadFileList = new ArrayList<>();
        List< SubmissionBundle.SubmissionXMLFile > xmlFileList = new ArrayList<>();

        Path uploadDir = Paths
                .get( this.parameters.isTestMode() ? "webin-cli-test" : "webin-cli" )
                .resolve( String.valueOf( this.context ) )
                .resolve( WebinCli.getSafeOutputDir( getSubmissionName() ) );

        prepareSubmissionBundleForContext(uploadDir, uploadFileList, xmlFileList);

        SubmissionBundle submissionBundle = new SubmissionBundle(
                getSubmitDir(),
                uploadDir.toString(),
                uploadFileList,
                xmlFileList,
                getParameters().getCenterName(),
                calculateManifestMd5() );

        submissionBundle.write();
    }

    @Override
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

    public M getManifestReader() {
        return manifestReader;
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
        String name = manifestReader.getName();
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

    protected File
    getReportFile( String filename )
    {
        return WebinCli.getReportFile( this.validationDir, filename, WebinCliConfig.REPORT_FILE_SUFFIX );
    }
}
