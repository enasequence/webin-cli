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
package uk.ac.ebi.ena.webin.cli.context.genome;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.context.genome.GenomeManifestReader.ASSEMBLY_TYPE_PRIMARY_METAGENOME;
import static uk.ac.ebi.ena.webin.cli.context.genome.GenomeManifestReader.Field;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;

public class GenomeManifestReaderTest {
  private static GenomeManifestReader createManifestReader() {
    WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    return new GenomeManifestReader(parameters, new MetadataProcessorFactory(parameters));
  }

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidManifestWithoutAssemblyType() {
    GenomeManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(Field.PLATFORM, " illumina")
            .field(ManifestReader.Fields.NAME, " SOME-FANCY-NAME")
            .field(Field.DESCRIPTION, " description")
            .file("FASTA", TempFileBuilder.empty("fasta"))
            .field(ManifestReader.Fields.SUBMISSION_TOOL, "ST-001")
            .field(ManifestReader.Fields.SUBMISSION_TOOL_VERSION, "STV-001")
            .build());

    GenomeManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    Assert.assertEquals("illumina", manifest.getPlatform());
    Assert.assertEquals("SOME-FANCY-NAME", manifest.getName());
    assertThat(manifest.files().files()).size().isOne();
    Assert.assertEquals("description", manifest.getDescription());
    Assert.assertEquals("ST-001", manifest.getSubmissionTool());
    Assert.assertEquals("STV-001", manifest.getSubmissionToolVersion());
    Assert.assertNull(manifest.getAssemblyType());
  }

  @Test
  public void testValidManifestWithAssemblyType() {
    GenomeManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(Field.PLATFORM, " illumina")
            .field(ManifestReader.Fields.NAME, " SOME-FANCY-NAME")
            .field(Field.DESCRIPTION, " description")
            .field(Field.ASSEMBLY_TYPE, ASSEMBLY_TYPE_PRIMARY_METAGENOME)
            .file("FASTA", TempFileBuilder.empty("fasta"))
            .field(ManifestReader.Fields.SUBMISSION_TOOL, "ST-001")
            .field(ManifestReader.Fields.SUBMISSION_TOOL_VERSION, "STV-001")
            .build());

    GenomeManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    Assert.assertEquals("illumina", manifest.getPlatform());
    Assert.assertEquals("SOME-FANCY-NAME", manifest.getName());
    assertThat(manifest.files().files()).size().isOne();
    Assert.assertEquals("description", manifest.getDescription());
    Assert.assertEquals("ST-001", manifest.getSubmissionTool());
    Assert.assertEquals("STV-001", manifest.getSubmissionToolVersion());
    Assert.assertEquals(ASSEMBLY_TYPE_PRIMARY_METAGENOME, manifest.getAssemblyType());
  }

  // This tests checks and ensures that both FASTA and FLATFILE file types cannot be specified in
  // the manifest
  // at the same time.
  @Test
  public void testInvalidManifestWithBothFastaAndFlatfile() {
    String expectedErrorMessage =
        "An invalid set of files has been specified. Expected data files are: [1 FASTA] or [1 FASTA, 1 CHROMOSOME_LIST, 0-1 UNLOCALISED_LIST] or [1 FLATFILE] or [1 FLATFILE, 1 CHROMOSOME_LIST, 0-1 UNLOCALISED_LIST].";

    String[][] invalidFileTypeGroups = {
      {"FASTA", "FLATFILE"},
      {"FASTA", "FLATFILE", "CHROMOSOME_LIST"},
      {"FASTA", "FLATFILE", "CHROMOSOME_LIST", "UNLOCALISED_LIST"}
    };

    for (String[] invalidFileTypeGroup : invalidFileTypeGroups) {
      GenomeManifestReader manifestReader = createManifestReader();

      List<String> msgs = new ArrayList<>();

      manifestReader.addListener(
          validationMessage -> {
            msgs.add(validationMessage.getMessage());
          });

      ManifestBuilder manifestBuilder =
          new ManifestBuilder()
              .field(Field.PLATFORM, " illumina")
              .field(ManifestReader.Fields.NAME, " SOME-FANCY-NAME")
              .field(Field.DESCRIPTION, " description")
              .field(ManifestReader.Fields.SUBMISSION_TOOL, "ST-001")
              .field(ManifestReader.Fields.SUBMISSION_TOOL_VERSION, "STV-001");

      for (String fileType : invalidFileTypeGroup) {
        manifestBuilder.file(fileType, "file");
      }

      manifestReader.readManifest(Paths.get("."), manifestBuilder.build());

      Assert.assertFalse(manifestReader.getValidationResult().isValid());
      Assert.assertTrue(msgs.contains(expectedErrorMessage));
    }
  }
}
