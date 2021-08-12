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
package uk.ac.ebi.ena.webin.cli.manifest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.listener.MessageCounter;

/** Creates the validator and reads the manifest file without using the command line parser. */
public class ManifestReaderTester<M extends Manifest> {
  private final Class<ManifestReader<M>> manifestReaderClass;

  public ManifestReaderTester(Class<ManifestReader<M>> manifestReaderClass) {
    this.manifestReaderClass = manifestReaderClass;
  }

  private ManifestReader<M> create() {
    return new ManifestReaderBuilder(manifestReaderClass, WebinCliTestUtils.getTestWebinCliParameters()).build();
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

  public ManifestReader<M> testError(
      ManifestBuilder manifest, File inputDir, WebinCliMessage message) {
      MessageCounter counter = MessageCounter.regex(
              ValidationMessage.Severity.ERROR,
              message.regex());
      ManifestReader<M> reader = create();
      reader.addListener(counter);
      reader.readManifest(inputDir.toPath(), manifest.build(inputDir));
     assertThat(counter.getCount()).isGreaterThanOrEqualTo(1);
    return reader;
  }
}
