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

import static org.assertj.core.api.Assertions.assertThat;

/** Creates the validator and reads the manifest file without using the command line parser. */
public class WebinCliBuilder {
  private final WebinCliContext context;
  private boolean validate = true;
  private boolean submit = true;
  private boolean ascp = false;

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

  public WebinCli execute(Path inputDir, ManifestBuilder manifestBuilder) {
    return execute(
            inputDir, WebinCliTestUtils.createTempDir().toPath(), manifestBuilder.build(inputDir));
  }

  public WebinCli execute(Path inputDir, Path outputDir, ManifestBuilder manifestBuilder) {
    return execute(inputDir, outputDir, manifestBuilder.build(inputDir));
  }

  public WebinCli execute(Path inputDir, Path outputDir, File manifest) {
    WebinCliCommand parameters = new WebinCliCommand();
    parameters.context = context;
    parameters.inputDir = inputDir.toFile();
    parameters.outputDir = outputDir.toFile();
    parameters.manifest = manifest;
    parameters.userName = System.getenv("webin-cli-username");
    parameters.password = System.getenv("webin-cli-password");
    parameters.test = true;
    parameters.validate = validate;
    parameters.submit = submit;
    parameters.ascp = ascp;
    WebinCli webinCli = new WebinCli();
    webinCli.execute(parameters);
    return webinCli;
  }

  public static void assertError(WebinCli webinCli) {
    assertThat(webinCli.getException()).isNotNull();
  }

  public static void assertError(WebinCli webinCli, String message) {
    assertThat(webinCli.getException()).hasMessageContaining(message);
  }
}
