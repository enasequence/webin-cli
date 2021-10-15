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
import java.nio.file.Path;

public class WebinCliBuilder {
  private final WebinCliContext context;
  private final Path outputDir = WebinCliTestUtils.createTempDir().toPath();
  private boolean validate = true;
  private boolean submit = true;
  private boolean ascp = false;
  private boolean ignoreErrors = false;

  public static final WebinCliBuilder READS = new WebinCliBuilder(WebinCliContext.reads);
  public static final WebinCliBuilder GENOME = new WebinCliBuilder(WebinCliContext.genome);
  public static final WebinCliBuilder TRANSCRIPTOME = new WebinCliBuilder(WebinCliContext.transcriptome);
  public static final WebinCliBuilder SEQUENCE = new WebinCliBuilder(WebinCliContext.sequence);

  private static String submissionAccount;
  private static String authToken;

  public WebinCliBuilder(WebinCliContext context) {
    this.context = context;
  }

  public WebinCliBuilder validate(boolean validate) {
    this.validate = validate;
    return this;
  }

  public WebinCliBuilder submit(boolean submit) {
    this.submit = submit;
    return this;
  }

  public WebinCliBuilder ascp(boolean ascp) {
    this.ascp = ascp;
    return this;
  }

  public WebinCliBuilder ignoreErrors(boolean ignoreErrors) {
    this.ignoreErrors = ignoreErrors;
    return this;
  }

  private WebinCliCommand cmd(Path inputDir, ManifestBuilder manifestBuilder) {
    WebinCliCommand cmd = new WebinCliCommand();
    cmd.context = context;
    cmd.inputDir = inputDir.toFile();
    cmd.outputDir = outputDir.toFile();
    cmd.manifest = manifestBuilder.build(inputDir);
    cmd.userName = WebinCliTestUtils.getTestWebinUsername();
    cmd.password = WebinCliTestUtils.getTestWebinPassword();
    cmd.test = true;
    cmd.validate = validate;
    cmd.submit = submit;
    cmd.ascp = ascp;
    return cmd;
  }

  public WebinCli build(File inputDir, ManifestBuilder manifestBuilder) {
    return build(inputDir.toPath(), manifestBuilder);
  }

  public WebinCli build(Path inputDir, ManifestBuilder manifestBuilder) {
    WebinCliCommand cmd = cmd(inputDir, manifestBuilder);
    if (submissionAccount == null) {
      submissionAccount = WebinCli.getSubmissionAccount(cmd);
    }
    if (authToken == null) {
      authToken = WebinCli.getAuthToken(cmd);
    }
    WebinCliParameters parameters = WebinCli.initParameters(submissionAccount, authToken, cmd);
    parameters.setIgnoreErrors(ignoreErrors);
    return new WebinCli(parameters);
  }
}
