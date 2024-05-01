/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.context.transcriptome;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getDefaultStudy;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.ManifestValidationPolicy;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutorBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

public class TranscriptomeValidationTest {

  private static final File RESOURCE_DIR =
      WebinCliTestUtils.getResourceDir("uk/ac/ebi/ena/webin/cli/transcriptome");

  public static final String NAME = "test";

  private static ManifestBuilder manifestBuilder() {
    return new ManifestBuilder()
        .field("STUDY", "test")
        .field("SAMPLE", "test")
        .field("PROGRAM", "test")
        .field("PLATFORM", "test")
        .field("NAME", NAME);
  }

  private static final WebinCliExecutorBuilder<TranscriptomeManifest, ValidationResponse>
      executorBuilder =
          new WebinCliExecutorBuilder(
                  TranscriptomeManifest.class, WebinCliExecutorBuilder.MetadataProcessorType.MOCK)
              .sample(NAME, WebinCliTestUtils.getDefaultSample())
              .study(NAME, getDefaultStudy());

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidFasta() {
    File manifestFile =
        manifestBuilder()
            .file(TranscriptomeManifest.FileType.FASTA, "valid.fasta.gz")
            .field(TranscriptomeManifestReader.Field.ASSEMBLY_TYPE, "isolate")
            .build();
    WebinCliExecutor<TranscriptomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    executor.validateSubmission(ManifestValidationPolicy.VALIDATE_ALL_MANIFESTS);
    assertThat(
            executor
                .getManifestReader()
                .getManifests().stream().findFirst().get()
                .files()
                .get(TranscriptomeManifest.FileType.FASTA))
        .size()
        .isOne();
    assertGeneratedFiles(executor);
  }

  private void assertGeneratedFiles(WebinCliExecutor executor) {
    Path submissionDir = executor.getParameters().getOutputDir().toPath()
        .resolve(executor.getContext().toString())
        .resolve(NAME);

    File bundle = submissionDir.resolve("validate.json").toFile();
    AssertionsForClassTypes.assertThat(bundle.exists()).isTrue();
    AssertionsForClassTypes.assertThat(bundle.length()).isGreaterThan(0);

    for (String xmlFileName : Arrays.asList("analysis", "submission")) {
      File xmlFile = submissionDir.resolve("submit").resolve(xmlFileName + ".xml").toFile();
      AssertionsForClassTypes.assertThat(xmlFile.exists()).isTrue();
      AssertionsForClassTypes.assertThat(xmlFile.length()).isGreaterThan(0);
    }
  }
}
