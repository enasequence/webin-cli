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

import java.io.File;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

import static org.assertj.core.api.Assertions.assertThat;

public class TranscriptomeAssemblyValidationTest {

  private static final File VALID_DIR =
      WebinCliTestUtils.resourceDir("uk/ac/ebi/ena/webin/cli/transcriptome/valid");

  private static ManifestBuilder manifestBuilder() {
    return new ManifestBuilder()
        .field("STUDY", "test")
        .field("SAMPLE", "test")
        .field("PROGRAM", "test")
        .field("PLATFORM", "test")
        .field("NAME", "test");
  }

  private static final WebinCliExecutorBuilder<TranscriptomeManifest> executorBuilder =
      new WebinCliExecutorBuilder(TranscriptomeManifest.class)
          .manifestMetadataProcessors(false)
          .sample(WebinCliTestUtils.getDefaultSample());

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidFasta() {
    File[] files = VALID_DIR.listFiles((dir, name) -> name.endsWith(".fasta.gz"));
    assertThat(files.length).isGreaterThan(0);
    for (File file : files) {
      String fileName = file.getName();
      // System.out.println(fileName);
      File manifestFile =
          manifestBuilder().file(TranscriptomeManifest.FileType.FASTA, fileName).build();
      WebinCliExecutor<TranscriptomeManifest> executor =
          executorBuilder.readManifest(manifestFile, VALID_DIR);
      executor.validateSubmission();
      assertThat(
              executor
                  .getManifestReader()
                  .getManifest()
                  .files()
                  .get(TranscriptomeManifest.FileType.FASTA))
          .size()
          .isOne();
    }
  }
}
