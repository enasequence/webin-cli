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
package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.webin.cli.context.SubmissionXmlWriter;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.service.IgnoreErrorsService;
import uk.ac.ebi.ena.webin.cli.service.RatelimitService;
import uk.ac.ebi.ena.webin.cli.service.models.RateLimitResult;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundleHelper;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.utils.RemoteServiceUrlHelper;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;

public class WebinCliExecutor<M extends Manifest, R extends ValidationResponse> {

  private static final String REPORT_FILE = "webin-cli.report";

  private static final Logger log = LoggerFactory.getLogger(WebinCliExecutor.class);

  private final WebinCliContext context;
  private final WebinCliParameters parameters;
  private final ManifestReader<M> manifestReader;
  private final Validator<M, R> validator;
  private final XmlWriter<M, R> xmlWriter;

  private Collection<SubmissionBundle> submissionBundles;

  protected R validationResponse;

  public WebinCliExecutor(
      WebinCliContext context,
      WebinCliParameters parameters,
      ManifestReader<M> manifestReader,
      XmlWriter<M, R> xmlWriter,
      Validator<M, R> validator) {
    this.context = context;
    this.parameters = parameters;
    this.manifestReader = manifestReader;
    this.xmlWriter = xmlWriter;
    this.validator = validator;
  }

  public final void readManifest() {
    File manifestReportFile = getManifestReportFile();
    manifestReportFile.delete();

    try {
      manifestReader
          .readManifest(
              getParameters().getInputDir().toPath(),
              getParameters().getManifestFile(),
              manifestReportFile);
    } catch (WebinCliException ex) {
      throw ex;
    } catch (Exception ex) {
      throw WebinCliException.systemError(
          ex, WebinCliMessage.EXECUTOR_INIT_ERROR.format(ex.getMessage()));
    }

    if (manifestReader == null || !manifestReader.getValidationResult().isValid()) {
      throw WebinCliException.userError(
          WebinCliMessage.MANIFEST_READER_INVALID_MANIFEST_FILE_ERROR.format(
              manifestReportFile.getPath()));
    }
  }

  public final void validateSubmission() {
    for (M manifest : manifestReader.getManifests()) {
      File validationDir = createSubmissionDir(manifest, WebinCliConfig.VALIDATE_DIR);
      File processDir = createSubmissionDir(manifest, WebinCliConfig.PROCESS_DIR);

      setIgnoreErrors(manifest);

      checkGenomeSubmissionRatelimit(manifest);

      if (!manifest.getFiles().get().isEmpty()) {
        for (SubmissionFile subFile : (List<SubmissionFile>) manifest.getFiles().get()) {
          subFile.setReportFile(
              Paths.get(validationDir.getPath())
                  .resolve(subFile.getFile().getName() + ".report")
                  .toFile());
        }
      }

      manifest.setReportFile(getSubmissionReportFile(validationDir));
      manifest.setProcessDir(processDir);
      manifest.setWebinAuthToken(getAuthTokenFromParam());
      manifest.setWebinRestUri(RemoteServiceUrlHelper.getWebinRestV1Url(getTestModeFromParam()));
      manifest.setBiosamplesUri(RemoteServiceUrlHelper.getBiosamplesUrl(getTestModeFromParam()));

      try {
        validationResponse = getValidator().validate(manifest);
      } catch (RuntimeException ex) {
        throw WebinCliException.systemError(ex);
      }
      if (validationResponse != null
          && validationResponse.getStatus() == ValidationResponse.status.VALIDATION_ERROR) {
        throw WebinCliException.validationError("");
      }
    }
  }

  private void setIgnoreErrors(M manifest) {
    // if ignore errors is already set to true then do nothing.
    if (manifest.isIgnoreErrors()) {
      return;
    }

    manifest.setIgnoreErrors(false);
    try {
      IgnoreErrorsService ignoreErrorsService =
          new IgnoreErrorsService.Builder()
              .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(getParameters().isTest()))
              .setCredentials(
                  getParameters().getWebinServiceUserName(), getParameters().getPassword())
              .build();

      manifest.setIgnoreErrors(
          ignoreErrorsService.getIgnoreErrors(getContext().name(), getSubmissionName(manifest)));
    } catch (RuntimeException ex) {
      log.warn(WebinCliMessage.IGNORE_ERRORS_SERVICE_SYSTEM_ERROR.text());
    }
  }

  private void checkGenomeSubmissionRatelimit(M manifest) {
    if (!(manifest instanceof GenomeManifest) || manifest.isIgnoreErrors()) {
      return;
    }

    RateLimitResult ratelimit;
    try {
      RatelimitService ratelimitService =
          new RatelimitService.Builder()
              .setWebinRestV1Uri(
                  RemoteServiceUrlHelper.getWebinRestV1Url(getParameters().isTest()))
              .setCredentials(
                  getParameters().getWebinServiceUserName(), getParameters().getPassword())
              .build();

      String submissionAccountId = getParameters().getWebinServiceUserName();
      String studyId = manifest.getStudy() == null ? null : manifest.getStudy().getStudyId();
      String sampleId =
          manifest.getSample() == null ? null : manifest.getSample().getSraSampleId();

      ratelimit =
          ratelimitService.ratelimit(getContext().name(), submissionAccountId, studyId, sampleId);
    } catch (RuntimeException ex) {
      throw WebinCliException.systemError(
          ex, WebinCliMessage.RATE_LIMIT_SERVICE_SYSTEM_ERROR.text());
    }
    if (ratelimit.isRateLimited()) {
      throw WebinCliException.userError(
          WebinCliMessage.CLI_GENOME_RATELIMIT_ERROR_WITH_ANALYSIS_ID.format(
              ratelimit.getLastSubmittedAnalysisId()));
    }
  }

  public final void prepareSubmissionBundles() {
    submissionBundles = new ArrayList<>(manifestReader.getManifests().size());

    for (M manifest : manifestReader.getManifests()) {
      File submitDir = createSubmissionDir(manifest, WebinCliConfig.SUBMIT_DIR);

      Path uploadDir =
          Paths.get(this.parameters.isTest() ? "webin-cli-test" : "webin-cli")
              .resolve(String.valueOf(this.context))
              .resolve(WebinCli.getSafeOutputDir(getSubmissionName(manifest)));

      Map<SubmissionBundle.SubmissionXMLFileType, String> xmls = new HashMap<>();

      xmls.putAll(
          new SubmissionXmlWriter()
              .createXml(
                  getValidationResponse(),
                  getParameters().getCenterName(),
                  WebinCli.getVersionForSubmission(parameters.getWebinSubmissionTool()),
                  getManifestFileContent(),
                  calculateManifestFileMd5()));

      // Calculate MD5 checksum of data files so it can be written into the XML.
      List<SubmissionFile> submissionFiles = manifest.files().get();
      submissionFiles.forEach(file -> file.setMd5(FileUtils.calculateDigest("MD5", file.getFile())));

      xmls.putAll(
          xmlWriter.createXml(
              manifest,
              getValidationResponse(),
              getParameters().getCenterName(),
              getSubmissionTitle(manifest),
              getSubmissionAlias(manifest),
              getParameters().getInputDir().toPath(),
              uploadDir));

      List<SubmissionBundle.SubmissionXMLFile> xmlFileList =
          xmls.entrySet().stream()
              .map(
                  entry -> {
                    String xmlFileName = entry.getKey().name().toLowerCase() + ".xml";
                    Path xmlFilePath = submitDir.toPath().resolve(xmlFileName);

                    return new SubmissionBundle.SubmissionXMLFile(
                        entry.getKey(), xmlFilePath.toFile(), entry.getValue());
                  })
              .collect(Collectors.toList());

      List<SubmissionBundle.SubmissionUploadFile> uploadFileList = new ArrayList<>();

      submissionFiles.forEach(
          file ->
              uploadFileList.add(
                  new SubmissionBundle.SubmissionUploadFile(
                      file.getFile(),
                      file.getFile().length(),
                      FileUtils.getLastModifiedTime(file.getFile()),
                      file.getMd5())));

      SubmissionBundle sb = new SubmissionBundle(submitDir, uploadDir.toString(), uploadFileList, xmlFileList, manifest);
      if (getParameters().isSaveSubmissionBundleFile()) {
        SubmissionBundleHelper.write(sb, getSubmissionBundleFileDir(manifest));

        submissionBundles.add(sb);
      }
    }
  }

  public Collection<SubmissionBundle> getSubmissionBundles() {
    if (submissionBundles == null && getParameters().isSaveSubmissionBundleFile()) {
      submissionBundles = manifestReader.getManifests().stream()
          .map(manifest -> SubmissionBundleHelper.read(manifest, getSubmissionBundleFileDir(manifest)))
          .collect(Collectors.toList());
    }

    return submissionBundles;
  }

  // TODO: remove
  public WebinCliContext getContext() {
    return context;
  }

  public ManifestReader<M> getManifestReader() {
    return manifestReader;
  }

  public Validator<M, R> getValidator() {
    return validator;
  }

  public XmlWriter<M, R> getXmlWriter() {
    return xmlWriter;
  }

  private File getSubmissionBundleFileDir(M manifest) {
    if (StringUtils.isBlank(getSubmissionName(manifest))) {
      throw WebinCliException.systemError(
          WebinCliMessage.EXECUTOR_INIT_ERROR.format("Missing submission name."));
    }

    return WebinCli.createOutputDir(
        parameters.getOutputDir(), String.valueOf(context), getSubmissionName(manifest));
  }

  private File getSubmissionReportFile(File manifestValidationDir) {
    return Paths.get(manifestValidationDir.getPath()).resolve(REPORT_FILE).toFile();
  }

  public File getManifestReportFile() {
    File outputDir = getParameters().getOutputDir();

    if (outputDir == null || !outputDir.isDirectory()) {
      throw WebinCliException.systemError(
          WebinCliMessage.CLI_INVALID_REPORT_DIR_ERROR.format(outputDir));
    }

    return new File(
        outputDir,
        Paths.get(getParameters().getManifestFile().getName()).getFileName().toString()
            + WebinCliConfig.REPORT_FILE_SUFFIX);
  }

  public WebinCliParameters getParameters() {
    return this.parameters;
  }

  private String getSubmissionName(M manifest) {
    String name = manifest.getName();
    if (name != null) {
      return name.trim().replaceAll("\\s+", "_");
    }
    return name;
  }

  private String getSubmissionAlias(M manifest) {
    String alias = "webin-" + context.name() + "-" + getSubmissionName(manifest);
    return alias;
  }

  private String getSubmissionTitle(M manifest) {
    String title = context.getTitlePrefix() + ": " + getSubmissionName(manifest);
    return title;
  }

  public R getValidationResponse() {
    return validationResponse;
  }

  private String getAuthTokenFromParam() {
    return this.parameters.getWebinAuthToken();
  }

  private boolean getTestModeFromParam() {
    return this.parameters.isTest();
  }

  private File createSubmissionDir(M manifest, String dir) {
    if (StringUtils.isBlank(getSubmissionName(manifest))) {
      throw WebinCliException.systemError(
          WebinCliMessage.EXECUTOR_INIT_ERROR.format("Missing submission name."));
    }
    File newDir =
        WebinCli.createOutputDir(
            parameters.getOutputDir(), String.valueOf(context), getSubmissionName(manifest), dir);
    if (!FileUtils.emptyDirectory(newDir)) {
      throw WebinCliException.systemError(
          WebinCliMessage.EXECUTOR_EMPTY_DIRECTORY_ERROR.format(newDir));
    }
    return newDir;
  }

  private String getManifestFileContent() {
    try {
      return new String(Files.readAllBytes(getParameters().getManifestFile().toPath()));
    } catch (IOException ioe) {
      throw WebinCliException.userError(
          "Exception thrown while reading manifest file", ioe.getMessage());
    }
  }

  private String calculateManifestFileMd5() {
    return FileUtils.calculateDigest("MD5", parameters.getManifestFile());
  }
}
