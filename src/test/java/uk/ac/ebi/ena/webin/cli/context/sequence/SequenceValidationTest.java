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
package uk.ac.ebi.ena.webin.cli.context.sequence;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getDefaultSample;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

import java.io.File;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutorBuilder;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest.FileType;

public class SequenceValidationTest {

  private static final File VALID_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/sequence/valid");

  private static ManifestBuilder manifestBuilder() {
    return new ManifestBuilder().field("STUDY", "test").field("NAME", "test");
  }

  private static final WebinCliExecutorBuilder<SequenceManifest, ValidationResponse>
      executorBuilder =
          new WebinCliExecutorBuilder(
                  SequenceManifest.class, WebinCliExecutorBuilder.MetadataProcessorType.MOCK)
              .sample(getDefaultSample());

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidTab() {
    File[] files =
        VALID_DIR.listFiles(
            (dir, name) -> (name.endsWith("tsv.gz") && !name.endsWith("field.tsv.gz")));
    assertThat(files.length).isGreaterThan(0);
    for (File file : files) {
      String fileName = file.getName();
      System.out.println("Testing valid tab file: " + fileName);
      File manifestFile = manifestBuilder().file(FileType.TAB, fileName).build();
      WebinCliExecutor<SequenceManifest, ValidationResponse> executor =
          executorBuilder.build(manifestFile, VALID_DIR);

      executor.readManifest();
      executor.validateSubmission();
      assertThat(executor.getManifestReader().getManifests().stream().findFirst().get().files().get(FileType.TAB))
          .size()
          .isOne();
    }
  }
}
