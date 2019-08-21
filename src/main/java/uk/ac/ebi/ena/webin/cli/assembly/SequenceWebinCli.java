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
package uk.ac.ebi.ena.webin.cli.assembly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.webin.cli.*;
import uk.ac.ebi.ena.webin.cli.service.IgnoreErrorsService;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFileType;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.xml.XmlCreator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public abstract class
SequenceWebinCli<R extends SequenceManifestReaderEx, M extends Manifest> extends AbstractWebinCli<R>
{
    protected final static String ANALYSIS_XML = "analysis.xml";
    private static final String ERROR_FILE = "webin-cli.report";

    private static final Logger log = LoggerFactory.getLogger(SequenceWebinCli.class);

    public SequenceWebinCli(WebinCliContext context, WebinCliParameters parameters, R manifestReader) {
        super(context, parameters, manifestReader);
    }

    // TODO: remove
    @Override
    public R getManifestReader() {
        return super.getManifestReader();
    }

    private M getManifest() {
        SequenceManifestReaderEx<M> manifestReader = getManifestReader();
        if (manifestReader != null) {
            return manifestReader.getManifest();
        }
        return null;
    }

    // TODO: remove function
    @Override
    protected void readManifestForContext( )
    {
        getManifestReader().readManifest( getParameters().getInputDir().toPath(), getParameters().getManifestFile()  );
    }

    @Override protected void validateSubmissionForContext()
    {
        M manifest = getManifest();

        // TODO: move ignore errors to AbstractWebinCli::validate

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
    }

    @Override
    public void prepareSubmissionBundleForContext(Path uploadDir, List<File> uploadFileList, List<SubmissionBundle.SubmissionXMLFile> xmlFileList)
    {
        // TODO: move uploadFileList to AbstractWebinCli

        uploadFileList.addAll(getManifest().files().files());

        // TODO: move xmlCreator to AbstractWebinCli

        XmlCreator xmlCreator = getContext().createXmlCreator();

        Map<SubmissionBundle.SubmissionXMLFileType, String> xmls = xmlCreator.createXml(getManifest(), getParameters().getCenterName(), getSubmissionTitle(), getSubmissionAlias(), getParameters().getInputDir().toPath(), uploadDir);

        SubmissionXMLFileType analysisXmlType = SubmissionXMLFileType.ANALYSIS;
        String analysisXml = xmls.get(analysisXmlType);
        Path analysisFile = getSubmitDir().toPath().resolve( ANALYSIS_XML );

        try
        {
            Files.write( analysisFile, analysisXml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
        }
        catch(IOException ex) {
            throw WebinCliException.systemError( ex );
        }

        xmlFileList.add( new SubmissionXMLFile( analysisXmlType, analysisFile.toFile(), FileUtils.calculateDigest( "MD5", analysisFile.toFile() ) ) );
    }
}
