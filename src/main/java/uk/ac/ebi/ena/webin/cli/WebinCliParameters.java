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

import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorParameters;
import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.AnalysisProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.RunProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.SampleProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.SampleXmlProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.StudyProcessor;

public class WebinCliParameters implements MetadataProcessorParameters {
  private String submissionAccount;
  private String webinAuthToken;
  private WebinCliContext context;
  private File manifestFile;
  private String username;
  private String password;
  private File outputDir;
  private File inputDir = new File(".");
  private String centerName;
  private WebinSubmissionTool webinSubmissionTool = WebinSubmissionTool.WEBIN_CLI;
  private boolean validate;
  private boolean quick;
  private boolean submit;
  private boolean test;
  private boolean ascp;
  private boolean ignoreErrors;

  private SampleProcessor sampleProcessor;
  private StudyProcessor studyProcessor;
  private SampleXmlProcessor sampleXmlProcessor;
  private RunProcessor runProcessor;
  private AnalysisProcessor analysisProcessor;

  public WebinCliParameters() {}

  public void setSubmissionAccount(String submissionAccount) {
    this.submissionAccount = submissionAccount;
  }

  public String getWebinAuthToken() {
    return webinAuthToken;
  }

  public void setWebinAuthToken(String webinAuthToken) {
    this.webinAuthToken = webinAuthToken;
  }

  public WebinCliContext getContext() {
    return context;
  }

  public void setContext(WebinCliContext context) {
    this.context = context;
  }

  public File getManifestFile() {
    return manifestFile;
  }

  public void setManifestFile(File manifestFile) {
    this.manifestFile = manifestFile;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public File getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(File outputDir) {
    this.outputDir = outputDir;
  }

  public File getInputDir() {
    return inputDir;
  }

  public void setInputDir(File inputDir) {
    this.inputDir = inputDir;
  }

  public String getCenterName() {
    return centerName;
  }

  public void setCenterName(String centerName) {
    this.centerName = centerName;
  }

  public WebinSubmissionTool getWebinSubmissionTool() {
    return webinSubmissionTool;
  }

  public void setWebinSubmissionTool(WebinSubmissionTool webinSubmissionTool) {
    this.webinSubmissionTool = webinSubmissionTool;
  }

  public boolean isValidate() {
    return validate;
  }

  public void setValidate(boolean validate) {
    this.validate = validate;
  }

  public boolean isQuick() {
    return quick;
  }

  public void setQuick(boolean quick) {
    this.quick = quick;
  }

  public boolean isSubmit() {
    return submit;
  }

  public void setSubmit(boolean submit) {
    this.submit = submit;
  }

  public boolean isTest() {
    return test;
  }

  public void setTest(boolean test) {
    this.test = test;
  }

  public boolean isAscp() {
    return ascp;
  }

  public void setAscp(boolean ascp) {
    this.ascp = ascp;
  }

  public boolean isIgnoreErrors() {
    return ignoreErrors;
  }

  public void setIgnoreErrors(boolean ignoreErrors) {
    this.ignoreErrors = ignoreErrors;
  }

  public SampleProcessor getSampleProcessor() {
    return sampleProcessor;
  }

  public void setSampleProcessor(SampleProcessor sampleProcessor) {
    this.sampleProcessor = sampleProcessor;
  }

  public StudyProcessor getStudyProcessor() {
    return studyProcessor;
  }

  public void setStudyProcessor(StudyProcessor studyProcessor) {
    this.studyProcessor = studyProcessor;
  }

  public SampleXmlProcessor getSampleXmlProcessor() {
    return sampleXmlProcessor;
  }

  public void setSampleXmlProcessor(SampleXmlProcessor sampleXmlProcessor) {
    this.sampleXmlProcessor = sampleXmlProcessor;
  }

  public RunProcessor getRunProcessor() {
    return runProcessor;
  }

  public void setRunProcessor(RunProcessor runProcessor) {
    this.runProcessor = runProcessor;
  }

  public AnalysisProcessor getAnalysisProcessor() {
    return analysisProcessor;
  }

  public void setAnalysisProcessor(AnalysisProcessor analysisProcessor) {
    this.analysisProcessor = analysisProcessor;
  }

  public String getWebinServiceUserName() {
    return username;
  }

  public String getFileUploadServiceUserName() {
    // Use su-Webin-N, mg-Webin-N, or Webin-N for FTP and Aspera authentication.
    return (username.startsWith("su-Webin") || username.startsWith("mg-Webin"))
        ? username
        : submissionAccount;
  }
}
