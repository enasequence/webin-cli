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
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Creates the validator and reads the manifest file without using the command line parser. */
public class ValidatorBuilder<T extends SequenceWebinCli> {
  private final Class<T> validatorClass;
  private boolean createOutputDirs = true;
  private boolean manifestMetadataProcessors = true;
  private boolean manifestValidateMandatory = true;
  private boolean manifestValidateFileExists = true;
  private boolean manifestValidateFileCount = true;
  private Study study;
  private Sample sample;

  private static final File TEMP_INPUT_DIR = WebinCliTestUtils.createTempDir();

  public ValidatorBuilder(Class<T> validatorClass) {
    this.validatorClass = validatorClass;
  }

  public ValidatorBuilder createOutputDirs(boolean createOutputDirs) {
    this.createOutputDirs = createOutputDirs;
    return this;
  }

  public ValidatorBuilder manifestMetadataProcessors(boolean metadataServiceActive) {
    this.manifestMetadataProcessors = metadataServiceActive;
    return this;
  }

  public ValidatorBuilder manifestValidateMandatory(boolean manifestValidateMandatory) {
    this.manifestValidateMandatory = manifestValidateMandatory;
    return this;
  }

  public ValidatorBuilder manifestValidateFileExists(boolean manifestValidateFileExists) {
    this.manifestValidateFileExists = manifestValidateFileExists;
    return this;
  }

  public ValidatorBuilder manifestValidateFileCount(boolean manifestValidateFileCount) {
    this.manifestValidateFileCount = manifestValidateFileCount;
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

  private T createValidator(File inputDir) {
    T validator;
    try {
      validator = validatorClass.newInstance();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    validator.setTestMode(true);
    validator.setInputDir(inputDir);
    validator.setValidationDir(WebinCliTestUtils.createTempDir());
    validator.setProcessDir(WebinCliTestUtils.createTempDir());
    validator.setSubmitDir(WebinCliTestUtils.createTempDir());
    return validator;
  }

  private WebinCliParameters createParameters(File manifestFile, File inputDir) {
    WebinCliParameters parameters = new WebinCliParameters();
    parameters.setManifestFile(manifestFile);
    parameters.setInputDir(inputDir);
    parameters.setOutputDir(WebinCliTestUtils.createTempDir());
    parameters.setUsername(System.getenv("webin-cli-username"));
    parameters.setPassword(System.getenv("webin-cli-password"));
    parameters.setTestMode(true);
    parameters.setCreateOutputDirs(createOutputDirs);
    parameters.setManifestMetadataProcessors(manifestMetadataProcessors);
    parameters.setManifestValidateMandatory(manifestValidateMandatory);
    parameters.setManifestValidateFileExist(manifestValidateFileExists);
    parameters.setManifestValidateFileCount(manifestValidateFileCount);
    return parameters;
  }

  public T readManifest(File manifestFile) {
    return readManifest(manifestFile, TEMP_INPUT_DIR);
  }

  public T readManifest(File manifestFile, File inputDir) {
    T validator = createValidator(inputDir);
    WebinCliParameters parameters = createParameters(manifestFile, validator.getInputDir());
    validator.readManifest(parameters);
    if (sample != null) {
      validator.getManifestReader().getManifest().setSample(sample);
    }
    if (study != null) {
      validator.getManifestReader().getManifest().setStudy(study);
    }
    return validator;
  }

  public T readManifestThrows(File manifestFile, WebinCliMessage message) {
    return readManifestThrows(manifestFile, TEMP_INPUT_DIR, message);
  }

  public T readManifestThrows(File manifestFile, File inputDir, WebinCliMessage message) {
    T validator = createValidator(inputDir);
    WebinCliParameters parameters = createParameters(manifestFile, validator.getInputDir());
    assertThatThrownBy(
            () -> validator.readManifest(parameters),
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
