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
import java.nio.charset.StandardCharsets;
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
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldGroup;
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

  private final Map<String, String> safeManifestNameToOriginalManifestNameMap = new HashMap<>();

  private final WebinCliContext context;
  private final WebinCliParameters parameters;
  private final ManifestReader<M> manifestReader;
  private final Validator<M, R> validator;
  private final XmlWriter<M, R> xmlWriter;

  private Collection<SubmissionBundle> submissionBundles;

  /**
   * Holds manifests whose submission bundles were not loaded. It is because they are either new or
   * have been changed since last validation.
   */
  private Collection<M> newOrModifiedManifests;

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

  /**
   * @param excludedManifestsNames Names of the manifests that are to be excluded from the list of
   *     read manifests.
   */
  public final void readManifest() {
    File manifestReportFile = getManifestReportFile();
    manifestReportFile.delete();

    try {
      manifestReader.readManifest(
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

  public final void validateSubmission(ManifestValidationPolicy validationPolicy)
      throws WebinCliException {
    Collection<M> manifestsToValidate = manifestReader.getManifests();
    if (validationPolicy == ManifestValidationPolicy.VALIDATE_UPDATED_MANIFESTS
        && !newOrModifiedManifests.isEmpty()) {
      manifestsToValidate = newOrModifiedManifests;
    } else {
      submissionBundles = new ArrayList<>(manifestsToValidate.size());
    }

    for (M manifest : manifestsToValidate) {
      File validationDir = createSubmissionDir(manifest, WebinCliConfig.VALIDATE_DIR);

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

      manifest.setReportFile(getValidationReportFile(validationDir));
      manifest.setProcessDir(createSubmissionDir(manifest, WebinCliConfig.PROCESS_DIR));
      manifest.setWebinAuthToken(getAuthTokenFromParam());
      manifest.setWebinRestUri(RemoteServiceUrlHelper.getWebinRestV1Url(getTestModeFromParam()));
      manifest.setBiosamplesUri(RemoteServiceUrlHelper.getBiosamplesUrl(getTestModeFromParam()));

      try {
        validationResponse = getValidator().validate(manifest);

        if (validationResponse != null
            && validationResponse.getStatus() == ValidationResponse.status.VALIDATION_SUCCESS) {
          prepareSubmissionBundles(manifest);
        }
      } catch (RuntimeException ex) {
        throw WebinCliException.systemError(ex, "Manifest name : " + manifest.getName());
      }

      if (validationResponse != null
          && validationResponse.getStatus() == ValidationResponse.status.VALIDATION_ERROR) {
        // It is important to notify the directory of validation reports as every manifest's
        // validation reports will be
        // written in it's separate directory. Not doing this will require going through every
        // manifest's validation
        // directory to find the one that's relevant.
        throw WebinCliException.validationError(
            "Manifest name : "
                + manifest.getName()
                + ". See reports for details : "
                + validationDir.getAbsolutePath());
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
          ignoreErrorsService.getIgnoreErrors(getContext().name(), manifest.getName()));
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
              .setWebinRestV1Uri(RemoteServiceUrlHelper.getWebinRestV1Url(getParameters().isTest()))
              .setCredentials(
                  getParameters().getWebinServiceUserName(), getParameters().getPassword())
              .build();

      String submissionAccountId = getParameters().getWebinServiceUserName();
      String studyId = manifest.getStudy() == null ? null : manifest.getStudy().getStudyId();
      String sampleId = manifest.getSample() == null ? null : manifest.getSample().getSraSampleId();

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

  private void prepareSubmissionBundles(M manifest) {
    File submitDir = createSubmissionDir(manifest, WebinCliConfig.SUBMIT_DIR);

    Path uploadDir =
        Paths.get(this.parameters.isTest() ? "webin-cli-test" : "webin-cli")
            .resolve(String.valueOf(this.context))
            .resolve(WebinCli.getSafeOutputDir(getFileSystemSafeSubmissionName(manifest)));

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

    SubmissionBundle sb =
        new SubmissionBundle(
            submitDir,
            uploadDir.toString(),
            uploadFileList,
            xmlFileList,
            calculateManifestFieldsMd5(manifestReader.getManifestFieldGroup(manifest)));

    submissionBundles.add(sb);

    if (getParameters().isSaveSubmissionBundleFile()) {
      SubmissionBundleHelper.write(sb, getSubmissionBundleFileDir(manifest));
    }
  }

  /**
   * First call to this method triggers submission bundle loading. Subsequence calls will just
   * return previous results.
   */
  public Collection<SubmissionBundle> getSubmissionBundles() {
    if (submissionBundles == null && getParameters().isSaveSubmissionBundleFile()) {
      Collection<SubmissionBundle> validatedSubmissionBundles =
          new ArrayList<>(manifestReader.getManifests().size());
      newOrModifiedManifests = new ArrayList<>(manifestReader.getManifests().size());

      manifestReader.getManifests().stream()
          .forEach(
              manifest -> {
                SubmissionBundle sb =
                    SubmissionBundleHelper.read(
                        calculateManifestFieldsMd5(manifestReader.getManifestFieldGroup(manifest)),
                        getSubmissionBundleFileDir(manifest));
                if (sb == null) {
                  // Null bundle means the manifest is either new or has been modified.
                  newOrModifiedManifests.add(manifest);
                } else {
                  validatedSubmissionBundles.add(sb);
                }
              });

      if (!validatedSubmissionBundles.isEmpty()) {
        submissionBundles = validatedSubmissionBundles;
      } else {
        // If all manifests are new or have been updated then it will essentially be a full
        // (re)validation.
        // Therefore, this list is no longer required.
        newOrModifiedManifests.clear();
      }
    }

    return submissionBundles;
  }

  /**
   * This should only be called after {@link WebinCliExecutor#getSubmissionBundles()} has been
   * invoked at least once.
   *
   * @return 'true' if updates were found in the manifest file.
   */
  public boolean isManifestFileUpdated() {
    // Submission bundle is not loaded for a new or modified manifest.
    // So if the loaded bundle count is less than read manifest count then it means there are either
    // new or modified
    // manifests read from the manifest file that have not yet been validated.
    return submissionBundles == null
        || submissionBundles.size() < manifestReader.getManifests().size();
  }

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

  public R getValidationResponse() {
    return validationResponse;
  }

  private File getSubmissionBundleFileDir(M manifest) {
    if (StringUtils.isBlank(getFileSystemSafeSubmissionName(manifest))) {
      throw WebinCliException.systemError(
          WebinCliMessage.EXECUTOR_INIT_ERROR.format("Missing submission name."));
    }

    return WebinCli.createOutputDir(
        parameters.getOutputDir(),
        String.valueOf(context),
        getFileSystemSafeSubmissionName(manifest));
  }

  private File getValidationReportFile(File manifestValidationDir) {
    return Paths.get(manifestValidationDir.getPath()).resolve(REPORT_FILE).toFile();
  }

  /**
   * @param manifest
   * @return Name of the given manifest after removing all the sensitive characters that may cause
   *     problems with file system from it.
   */
  private String getFileSystemSafeSubmissionName(M manifest) {
    if (manifest.getName() != null) {
      String safeName =
          WebinCli.getSafeOutputDir(manifest.getName().trim().replaceAll("\\s+", "_"));

      validateSafeManifestName(manifest, safeName);
    }

    return manifest.getName();
  }

  private void validateSafeManifestName(M manifest, String safeName) {
    String originalManifestName = safeManifestNameToOriginalManifestNameMap.get(safeName);
    if (originalManifestName == null) {
      safeManifestNameToOriginalManifestNameMap.put(safeName, manifest.getName());
    } else if (!originalManifestName.equals(manifest.getName())) {
      throw WebinCliException.userError(
          WebinCliMessage.EXECUTOR_DIRECTORY_MANIFEST_NAME_CONFLICT_ERROR.format(
              originalManifestName, manifest.getName()));
    }
  }

  private String getSubmissionAlias(M manifest) {
    String alias = "webin-" + context.name() + "-" + manifest.getName();
    return alias;
  }

  private String getSubmissionTitle(M manifest) {
    String title = context.getTitlePrefix() + ": " + manifest.getName();
    return title;
  }

  private String getAuthTokenFromParam() {
    return this.parameters.getWebinAuthToken();
  }

  private boolean getTestModeFromParam() {
    return this.parameters.isTest();
  }

  private File createSubmissionDir(M manifest, String dir) {
    if (StringUtils.isBlank(getFileSystemSafeSubmissionName(manifest))) {
      throw WebinCliException.systemError(
          WebinCliMessage.EXECUTOR_INIT_ERROR.format("Missing submission name."));
    }
    File newDir =
        WebinCli.createOutputDir(
            parameters.getOutputDir(),
            String.valueOf(context),
            getFileSystemSafeSubmissionName(manifest),
            dir);
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

  private String calculateManifestFieldsMd5(ManifestFieldGroup fieldGroup) {
    StringBuilder stringBuilder = new StringBuilder(8192);

    fieldGroup.forEach(field -> stringBuilder.append(field.getValue()));

    return FileUtils.calculateDigest(
        "MD5", stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
  }
}
