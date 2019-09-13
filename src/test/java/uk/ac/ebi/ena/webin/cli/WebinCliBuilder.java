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

public class WebinCliBuilder {
  private final WebinCliContext context;
  private final Path outputDir = WebinCliTestUtils.createTempDir().toPath();
  private boolean validate = true;
  private boolean submit = true;
  private boolean ascp = false;

  public static final WebinCliBuilder READS = new WebinCliBuilder(WebinCliContext.reads);
  public static final WebinCliBuilder GENOME = new WebinCliBuilder(WebinCliContext.genome);
  public static final WebinCliBuilder TRANSCRIPTOME = new WebinCliBuilder(WebinCliContext.transcriptome);
  public static final WebinCliBuilder SEQUENCE = new WebinCliBuilder(WebinCliContext.sequence);

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

  private WebinCliCommand cmd(Path inputDir, ManifestBuilder manifestBuilder) {
    WebinCliCommand cmd = new WebinCliCommand();
    cmd.context = context;
    cmd.inputDir = inputDir.toFile();
    cmd.outputDir = outputDir.toFile();
    cmd.manifest = manifestBuilder.build(inputDir);
    cmd.userName = System.getenv("webin-cli-username");
    cmd.password = System.getenv("webin-cli-password");
    cmd.test = true;
    cmd.validate = validate;
    cmd.submit = submit;
    cmd.ascp = ascp;
    return cmd;
  }


  public WebinCli execute(File inputDir, ManifestBuilder manifestBuilder) {
    return execute(inputDir.toPath(), manifestBuilder);
  }

  public WebinCli execute(Path inputDir, ManifestBuilder manifestBuilder) {
    WebinCliCommand cmd = cmd(inputDir, manifestBuilder);
    WebinCli webinCli = new WebinCli(cmd);
    webinCli.execute();
    return webinCli;
  }

  public <T extends Exception> WebinCli executeThrows(Path inputDir, ManifestBuilder manifestBuilder, Class<T> exceptionClass, String ... messages) {
    WebinCliCommand cmd = cmd(inputDir, manifestBuilder);
    return executeThrows(cmd, exceptionClass, messages);
  }

  public static <T extends Exception> WebinCli executeThrows(WebinCliCommand cmd, Class<T> exceptionClass, String ... messages) {
    WebinCli cli = null;
    try {
      cli = new WebinCli(cmd);
      cli.execute();
    }
    catch (Exception ex) {
      if (exceptionClass.isInstance(ex)) {
        for (String message : messages) {
          assertThat(ex).hasMessageContaining(message);
        }
      }
    }
    return cli;
  }
}
