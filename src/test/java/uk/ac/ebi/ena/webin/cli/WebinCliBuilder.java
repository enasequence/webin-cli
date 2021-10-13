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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class WebinCliBuilder {
  private final WebinCliContext context;
  private final Path outputDir = WebinCliTestUtils.createTempDir().toPath();
  private boolean validate = true;
  private boolean submit = true;
  private boolean ascp = false;
  private String AUTH_TOKEN="";
  private final static String AUTH_JSON="{\"authRealms\":[\"ENA\"],\"password\":\""+WebinCliTestUtils.getTestWebinPassword()+"\",\"username\":\""+WebinCliTestUtils.getTestWebinUsername()+"\"}";
  private final static String TEST_AUTH_URL="https://wwwdev.ebi.ac.uk/ena/submit/webin/auth/token";

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
    cmd.userName = WebinCliTestUtils.getTestWebinUsername();
    cmd.password = WebinCliTestUtils.getTestWebinPassword();
    cmd.test = true;
    cmd.validate = validate;
    cmd.submit = submit;
    cmd.ascp = ascp;
    return cmd;
  }

  private WebinCliParameters params(Path inputDir, ManifestBuilder manifestBuilder) {
    WebinCliParameters params = new WebinCliParameters();
    params.setContext(context);
    params.setInputDir(inputDir.toFile());
    params.setOutputDir(outputDir.toFile());
    params.setManifestFile(manifestBuilder.build(inputDir));
    params.setUsername(WebinCliTestUtils.getTestWebinUsername());
    params.setPassword(WebinCliTestUtils.getTestWebinPassword());
    params.setSubmissionAccount(WebinCliTestUtils.getTestWebinUsername());
    params.setWebinAuthToken(getAuthToken());
    params.setTest(true);
    params.setValidate(validate);
    params.setSubmit(submit);
    params.setAscp(ascp);

    return params;
  }

  public WebinCli build(File inputDir, ManifestBuilder manifestBuilder) {
    return build(inputDir.toPath(), manifestBuilder);
  }

  public WebinCli build(Path inputDir, ManifestBuilder manifestBuilder) {
    WebinCliCommand cmd = cmd(inputDir, manifestBuilder);
    return new WebinCli(WebinCliTestUtils.getTestWebinUsername(),getAuthToken(), cmd);
  }

  public WebinCli build(File inputDir, ManifestBuilder manifestBuilder, boolean ignoreErrors) {
    WebinCliParameters params = params(inputDir.toPath(), manifestBuilder);
    params.setIgnoreErrors(ignoreErrors);

    return new WebinCli(params);
  }

  private String getAuthToken(){

    if(StringUtils.isNotEmpty(AUTH_TOKEN)){
      return AUTH_TOKEN;
    }
    RestTemplate restTemplate=new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request =
            new HttpEntity<String>(AUTH_JSON, headers);
    ResponseEntity<String> response =
            restTemplate.postForEntity(TEST_AUTH_URL,request, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    AUTH_TOKEN = response.getBody();
    return AUTH_TOKEN;
  }
}
