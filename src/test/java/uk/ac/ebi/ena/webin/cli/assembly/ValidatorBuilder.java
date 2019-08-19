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

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.webin.cli.*;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Creates the validator and reads the manifest file without using the command line parser. */
public class ValidatorBuilder<T extends SequenceWebinCli> {
  private final Class<T> validatorClass;
  private boolean manifestMetadataProcessors = true;
  private Study study;
  private Sample sample;

  public ValidatorBuilder(Class<T> validatorClass) {
    this.validatorClass = validatorClass;
  }

  public ValidatorBuilder manifestMetadataProcessors(boolean metadataServiceActive) {
    this.manifestMetadataProcessors = metadataServiceActive;
    return this;
  }

  public ValidatorBuilder study(Study study) {
    this.study = study;
    return this;
  }

  public ValidatorBuilder sample(Sample sample) {
    this.sample = sample;
    return this;
  }

  private T createValidator(File inputDir, WebinCliParameters parameters) {
    T validator = WebinCliContext.createValidator(validatorClass, parameters);
    validator.setInputDir(inputDir);
    validator.setValidationDir(WebinCliTestUtils.createTempDir());
    validator.setProcessDir(WebinCliTestUtils.createTempDir());
    validator.setSubmitDir(WebinCliTestUtils.createTempDir());
    return validator;
  }

  private WebinCliParameters createParameters(File manifestFile, File inputDir) {
    WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
    parameters.setManifestFile(manifestFile);
    parameters.setInputDir(inputDir);
    parameters.setOutputDir(WebinCliTestUtils.createTempDir());
    parameters.setMetadataProcessorsActive(manifestMetadataProcessors);
    return parameters;
  }

  public T readManifest(File manifestFile, File inputDir) {
    T validator = createValidator(inputDir, createParameters(manifestFile, inputDir));
    validator.readManifest();
    if (sample != null) {
      validator.getManifestReader().getManifest().setSample(sample);
    }
    if (study != null) {
      validator.getManifestReader().getManifest().setStudy(study);
    }
    return validator;
  }

  public T readManifestThrows(File manifestFile, File inputDir, WebinCliMessage message) {
    T validator = createValidator(inputDir, createParameters(manifestFile, inputDir));
    assertThatThrownBy(
            () -> validator.readManifest(),
            "Expected WebinCliException to be thrown: " + message.key())
        .isInstanceOf(WebinCliException.class);
    assertThat(
            validator
                .getManifestReader()
                .getValidationResult()
                .count(message.key(), Severity.ERROR))
        .isGreaterThanOrEqualTo(1);
    return validator;
  }
}
