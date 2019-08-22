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
package uk.ac.ebi.ena.webin.cli.manifest;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorParameters;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/** Creates the validator and reads the manifest file without using the command line parser. */
public class ManifestReaderTester<M extends Manifest> {
  private final Class<ManifestReader<M>> manifestReaderClass;
  private boolean metadataProcessorsActive = true;
  private boolean manifestValidateMandatory = true;
  private boolean manifestValidateFileExist = true;
  private boolean manifestValidateFileCount = true;

  public ManifestReaderTester( Class<ManifestReader<M>> manifestReaderClass) {
      this.manifestReaderClass = manifestReaderClass;
  }

  public ManifestReaderTester metadataProcessorsActive(boolean metadataProcessorsActive) {
    this.metadataProcessorsActive = metadataProcessorsActive;
    return this;
  }

  public ManifestReaderTester manifestValidateMandatory(boolean manifestValidateMandatory) {
    this.manifestValidateMandatory = manifestValidateMandatory;
    return this;
  }

  public ManifestReaderTester manifestValidateFileExist(boolean manifestValidateFileExist) {
    this.manifestValidateFileExist = manifestValidateFileExist;
    return this;
  }

  public ManifestReaderTester manifestValidateFileCount(boolean manifestValidateFileCount) {
    this.manifestValidateFileCount = manifestValidateFileCount;
    return this;
  }

  private ManifestReader<M> create() {
        return WebinCliContext.createManifestReader(manifestReaderClass,
              new ManifestReaderParameters() {
                public boolean isManifestValidateMandatory() {
                  return manifestValidateMandatory;
                }

                public boolean isManifestValidateFileExist() {
                  return manifestValidateFileExist;
                }

                public boolean isManifestValidateFileCount() {
                  return manifestValidateFileCount;
                }
              },
              new MetadataProcessorFactory(
                  new MetadataProcessorParameters() {
                    public boolean isMetadataProcessorsActive() {
                      return metadataProcessorsActive;
                    }

                    public String getUsername() {
                      return System.getenv("webin-cli-username");
                    }

                    public String getPassword() {
                      return System.getenv("webin-cli-password");
                    }

                    public boolean isTestMode() {
                      return true;
                    }
                  }));

  }

  public ManifestReader<M> test(ManifestBuilder manifest) {
    return test(manifest, WebinCliTestUtils.createTempDir());
  }

  public ManifestReader<M> test(ManifestBuilder manifest, File inputDir) {
    return test(manifest, inputDir.toPath());
  }

  public ManifestReader<M> test(ManifestBuilder manifest, Path inputDir) {
      ManifestReader<M> reader = create();
    reader.readManifest(inputDir, manifest.build(inputDir));
    return reader;
  }

  public ManifestReader<M> testError(ManifestBuilder manifest, WebinCliMessage message) {
    return testError(manifest, WebinCliTestUtils.createTempDir(), message);
  }

  public ManifestReader<M> testError(ManifestBuilder manifest, File inputDir, WebinCliMessage message) {
  ManifestReader<M> reader = test(manifest, inputDir);
    assertThat(reader.getValidationResult().count(message.key(), Severity.ERROR))
        .isGreaterThanOrEqualTo(1);
    return reader;
  }
}
