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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.service.IgnoreErrorsService;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationOrigin;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationReport;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebinCliExecutor<M extends Manifest, R extends ValidationResponse>
    implements ValidationReportProvider {
  private final WebinCliContext context;
  private final WebinCliParameters parameters;
  private final ManifestReader<M> manifestReader;
  private final Validator<M, R> validator;
  private final XmlWriter<M, R> xmlWriter;

  private final File validationDir;
  private final File processDir;
  private final File submitDir;

  private final File submissionBundleFile;
  private final File submissionReportFile;

  private final ValidationReport submissionReport;
  private final Map<String, ValidationReport> submissionFileReport = new HashMap<>();
  private final ValidationReport submissionReportOfManifestOrigin;
  private final ValidationReport submissionReportOfBundleOrigin;


  protected R validationResponse;

  private static final String REPORT_FILE = "webin-cli.report";

  private static final Logger log = LoggerFactory.getLogger(WebinCliExecutor.class);

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

    if (StringUtils.isBlank(getSubmissionName())) {
      throw WebinCliException.systemError(
              WebinCliMessage.EXECUTOR_INIT_ERROR.format("Missing submission name."));
    }

    this.validationDir = createOutputDir(WebinCliConfig.VALIDATE_DIR);
    this.processDir = createOutputDir(WebinCliConfig.PROCESS_DIR);
    this.submitDir = createOutputDir(WebinCliConfig.SUBMIT_DIR);

    this.submissionBundleFile = createSubmissionBundleFile();
    this.submissionReportFile = createSubmissionReportFile();

    this.submissionReport = createSubmissionReport();
    this.submissionReportOfManifestOrigin = createSubmissionReportOfManifestOrigin(this.submissionReport);
    this.submissionReportOfBundleOrigin = createSubmissionReportOfBundleOrigin(this.submissionReport);

  }

  protected File createSubmissionBundleFile() {
    return new File( this.submitDir, WebinCliConfig.SUBMISSION_BUNDLE_FILE_SUFFIX);
  }

  protected File createSubmissionReportFile() {
    return Paths.get(getValidationDir().getPath()).resolve(REPORT_FILE).toFile();
  }

  protected File createSubmissionReportFile(File file) {
    return Paths.get(getValidationDir().getPath()).resolve(file.getName() + ".report").toFile();
  }

  protected ValidationReport createSubmissionReport() {
    ValidationReport.Builder reportBuilder = ValidationReport.builder();
    if (getParameters().isReportFile()) {
      reportBuilder.file(submissionReportFile);
    }
    reportBuilder.listener(getParameters().getMessageListeners());
    return reportBuilder.build();
  }

  protected ValidationReport createSubmissionReportOfManifestOrigin(ValidationReport submissionReport) {
    return ValidationReport.builder()
            .parent(submissionReport)
            .origin(new ValidationOrigin("manifest file", parameters.getManifestFile().getAbsolutePath()))
            .build();
  }

  protected ValidationReport createSubmissionReportOfBundleOrigin(ValidationReport submissionReport) {
    return ValidationReport.builder()
            .parent(submissionReport)
            .origin(new ValidationOrigin("submission bundle file", submissionBundleFile.getAbsolutePath()))
            .build();
  }

  @Override
  public ValidationReport getSubmissionReport() {
    return submissionReport;
  }

  @Override
  public ValidationReport getSubmissionReportOfBundleOrigin() {
    return submissionReportOfBundleOrigin;
  }

  @Override
  public ValidationReport getSubmissionReportOfManifestOrigin() {
    return submissionReportOfManifestOrigin;
  }

  @Override
  public ValidationReport getSubmissionFileReport(File file) {
    String name = file.getName();
    if (!submissionFileReport.containsKey(name)) {
      ValidationReport.Builder reportBuilder = ValidationReport.builder();
      if (getParameters().isReportFile()) {
        reportBuilder.file(createSubmissionReportFile(file));
      }
      reportBuilder.listener(getParameters().getMessageListeners());
      ValidationReport report = reportBuilder.build();
      submissionFileReport.put(name, report);
    }
    return submissionFileReport.get(name);
  }

  public final void readManifest() {
    try {
      getManifestReader()
          .readManifest(getParameters().getInputDir().toPath(), getParameters().getManifestFile(), this);
    } catch (WebinCliException ex) {
      throw ex;
    } catch (Exception ex) {
      throw WebinCliException.systemError(
          ex, WebinCliMessage.EXECUTOR_INIT_ERROR.format(ex.getMessage()));
    }

    if (!getSubmissionReportOfManifestOrigin().isValid()) {
      throw WebinCliException.userError(
          WebinCliMessage.MANIFEST_READER_INVALID_MANIFEST_FILE_ERROR.format(
              submissionReportFile.getPath()));
    }
  }

  public void validateSubmission() {
    M manifest = getManifestReader().getManifest();

    setIgnoreErrors(manifest);

    manifest.setValidationReport(getSubmissionReport());
    manifest.setProcessDir(getProcessDir());

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

  private void setIgnoreErrors(M manifest) {
    manifest.setIgnoreErrors(false);
    try {
      IgnoreErrorsService ignoreErrorsService =
          new IgnoreErrorsService.Builder()
              .setCredentials(
                  getParameters().getWebinServiceUserName(), getParameters().getPassword())
              .setTest(getParameters().isTest())
              .build();

      manifest.setIgnoreErrors(
          ignoreErrorsService.getIgnoreErrors(getContext().name(), getSubmissionName()));
    } catch (RuntimeException ex) {
      log.warn(WebinCliMessage.IGNORE_ERRORS_SERVICE_SYSTEM_ERROR.text());
    }
  }

  public final void prepareSubmissionBundle() {

    List<File> uploadFileList = new ArrayList<>();
    List<SubmissionBundle.SubmissionXMLFile> xmlFileList = new ArrayList<>();

    Path uploadDir =
        Paths.get(this.parameters.isTest() ? "webin-cli-test" : "webin-cli")
            .resolve(String.valueOf(this.context))
            .resolve(WebinCli.getSafeOutputDir(getSubmissionName()));

    uploadFileList.addAll(getManifestReader().getManifest().files().files());

    Map<SubmissionBundle.SubmissionXMLFileType, String> xmls =
        xmlWriter.createXml(
            getManifestReader().getManifest(),
            getValidationResponse(),
            getParameters().getCenterName(),
            getSubmissionTitle(),
            getSubmissionAlias(),
            getParameters().getInputDir().toPath(),
            uploadDir);

    xmls.forEach(
        (fileType, xml) -> {
          String xmlFileName = fileType.name().toLowerCase() + ".xml";
          Path xmlFile = getSubmitDir().toPath().resolve(xmlFileName);

          try {
            Files.write(
                xmlFile,
                xml.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.SYNC);
          } catch (IOException ex) {
            throw WebinCliException.systemError(ex);
          }

          xmlFileList.add(
              new SubmissionBundle.SubmissionXMLFile(
                  fileType, xmlFile.toFile(), FileUtils.calculateDigest("MD5", xmlFile.toFile())));
        });

    SubmissionBundle submissionBundle =
        new SubmissionBundle(
            getSubmitDir(),
            uploadDir.toString(),
            uploadFileList,
            xmlFileList,
            getParameters().getCenterName(),
            calculateManifestMd5());

    submissionBundle.write();
  }

  public SubmissionBundle readSubmissionBundle() {
    return SubmissionBundle.read(getSubmitDir(), calculateManifestMd5(), submissionReportOfBundleOrigin);
  }

  private File createOutputDir(String outputDirName) {
    File outputDir =
            WebinCli.getOutputDir(
                    parameters.getOutputDir(), String.valueOf(context), getSubmissionName(), outputDirName);
    WebinCli.createOutputDir(outputDir);
    if (!FileUtils.recursiveFileDelete(outputDir)) {
      throw WebinCliException.systemError(
          WebinCliMessage.EXECUTOR_EMPTY_DIRECTORY_ERROR.format(outputDir));
    }
    return outputDir;
  }

  private String calculateManifestMd5() {
    return FileUtils.calculateDigest("MD5", parameters.getManifestFile());
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

  public File getValidationDir() {
    return validationDir;
  }

  public File getProcessDir() {
    return processDir;
  }

  public File getSubmitDir() {
    return submitDir;
  }

  public WebinCliParameters getParameters() {
    return this.parameters;
  }

  public String getSubmissionName() {
    String name = manifestReader.getManifest().getName();
    if (name != null) {
      return name.trim().replaceAll("\\s+", "_");
    }
    return name;
  }

  public String getSubmissionAlias() {
    String alias = "webin-" + context.name() + "-" + getSubmissionName();
    return alias;
  }

  public String getSubmissionTitle() {
    String title = context.getTitlePrefix() + ": " + getSubmissionName();
    return title;
  }

  public R getValidationResponse() {
    return validationResponse;
  }
}
