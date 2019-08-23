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

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Creates the validator and reads the manifest file without using the command line parser. */
public class WebinCliExecutorBuilder<M extends Manifest> {
  private final Class<M> manifestClass;
  private boolean manifestMetadataProcessors = true;
  private Study study;
  private Sample sample;

  public WebinCliExecutorBuilder(Class<M> manifestClass) {
    this.manifestClass = manifestClass;
  }

  public WebinCliExecutorBuilder manifestMetadataProcessors(boolean metadataServiceActive) {
    this.manifestMetadataProcessors = metadataServiceActive;
    return this;
  }

  public WebinCliExecutorBuilder study(Study study) {
    this.study = study;
    return this;
  }

  public WebinCliExecutorBuilder sample(Sample sample) {
    this.sample = sample;
    return this;
  }

  private WebinCliExecutor<M> createExecutor(WebinCliParameters parameters) {
    return WebinCliContext.createExecutor(manifestClass, parameters);
  }

  private WebinCliParameters createParameters(File manifestFile, File inputDir) {
    WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
    parameters.setManifestFile(manifestFile);
    parameters.setInputDir(inputDir);
    parameters.setMetadataProcessorsActive(manifestMetadataProcessors);
    return parameters;
  }

  public WebinCliExecutor<M> readManifest(File manifestFile, File inputDir) {
    WebinCliExecutor<M> executor = createExecutor(createParameters(manifestFile, inputDir));
    executor.readManifest();
    if (sample != null) {
      executor.getManifestReader().getManifest().setSample(sample);
    }
    if (study != null) {
      executor.getManifestReader().getManifest().setStudy(study);
    }
    return executor;
  }

  public WebinCliExecutor<M> readManifestThrows(File manifestFile, File inputDir, WebinCliMessage message) {
    WebinCliExecutor<M> executor = createExecutor(createParameters(manifestFile, inputDir));
    assertThatThrownBy(
            () -> executor.readManifest(),
            "Expected WebinCliException to be thrown: " + message.key())
        .isInstanceOf(WebinCliException.class);
    assertThat(
            executor
                .getManifestReader()
                .getValidationResult()
                .count(message.key(), Severity.ERROR))
        .isGreaterThanOrEqualTo(1);
    return executor;
  }
}
