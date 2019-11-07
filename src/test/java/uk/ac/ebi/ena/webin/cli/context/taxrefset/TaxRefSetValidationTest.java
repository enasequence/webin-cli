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
package uk.ac.ebi.ena.webin.cli.context.taxrefset;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutorBuilder;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TaxRefSetManifest;

import java.io.File;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getDefaultSample;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

public class TaxRefSetValidationTest {

  private static final File VALID_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/taxxrefset");

  private static ManifestBuilder manifestBuilder() {
    return new ManifestBuilder()
            .field("NAME", "test")
            .field("DESCRIPTION", "test_desc")
            .field("STUDY", "test")
            .field("TAXONOMY_SYSTEM", "test_TAXON")
            .field("TAXONOMY_SYSTEM_VERSION", "1")
            .field("CUSTOM_FIELD", "custom_field1:val1")
            .field("CUSTOM_FIELD", "custom_field2:val2");
  }

  private static final WebinCliExecutorBuilder<TaxRefSetManifest, ValidationResponse> executorBuilder =
      new WebinCliExecutorBuilder(TaxRefSetManifest.class, WebinCliExecutorBuilder.MetadataProcessorType.MOCK)
          .sample(getDefaultSample());

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidTab() {
    File[] files = VALID_DIR.listFiles((dir, name) -> name.endsWith(".tsv.gz"));
    assertThat(files.length).isEqualTo(1);
    String tsvFileName = files[0].getName();

    files = VALID_DIR.listFiles((dir, name) -> name.endsWith(".fa.gz"));
    assertThat(files.length).isEqualTo(1);
    String fastaFileName = files[0].getName();

    File manifestFile = manifestBuilder().file(TaxRefSetManifest.FileType.TAB, tsvFileName).file(TaxRefSetManifest.FileType.FASTA, fastaFileName).build();

    WebinCliExecutor<TaxRefSetManifest, ValidationResponse> executor = executorBuilder.build(manifestFile, VALID_DIR);
    executor.readManifest();
    executor.validateSubmission();
    assertThat(executor.getManifestReader().getManifest().files().get(TaxRefSetManifest.FileType.TAB))
            .size()
            .isOne();
    assertThat(executor.getManifestReader().getManifest().files().get(TaxRefSetManifest.FileType.FASTA))
            .size()
            .isOne();

  }
}
