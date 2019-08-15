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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getDefaultSample;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.resourceDir;

import java.io.File;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest.FileType;

public class GenomeAssemblyValidationTest {

  private static final File RESOURCE_DIR = resourceDir("uk/ac/ebi/ena/webin/cli/assembly");

  private static ManifestBuilder manifestBuilder() {
    return new ManifestBuilder()
            .field("STUDY", "test")
            .field("SAMPLE", "test")
            .field("COVERAGE", "1")
            .field("PROGRAM", "test")
            .field("PLATFORM", "test")
            .field("NAME", "test");
  }

  private static final ValidatorBuilder<GenomeAssemblyWebinCli> validatorBuilder =
          new ValidatorBuilder(GenomeAssemblyWebinCli.class)
                  .manifestMetadataProcessors(false)
                  .sample(getDefaultSample());

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidFasta() {
    File manifestFile =
        manifestBuilder().file(FileType.FASTA, "valid_fasta.fasta.gz").build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = validator.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTA).size()).isOne();
    validator.validate();
  }

  @Test
  public void testValidFlatFileWithSubmitterReferenceInManifest() {
    File manifestFile =
        manifestBuilder()
            .field(
                GenomeAssemblyManifestReader.Field.ADDRESS,
                "Biologische Anstalt Helgoland, Alfred-Wegener-Institut, Helmholtz "
                    + "Zentrum für Polar- und Meeresforschung, Kurpromenade 27498 Helgoland, Germany")
            .field(GenomeAssemblyManifestReader.Field.AUTHORS, "Kirstein   Ivan, Wichels Alfred..;")
            .file(FileType.FLATFILE, "valid_flatfile.dat.gz")
            .build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = validator.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FLATFILE).size()).isOne();
    validator.validate();
  }

  @Test
  public void testValidFlatFile() {
    File manifestFile =
        manifestBuilder().file(FileType.FLATFILE, "valid_flatfile.dat.gz").build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = validator.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FLATFILE).size()).isOne();
    validator.validate();
  }

  @Test
  public void testValidFastaAndAgp() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTA, "valid_fasta.fasta.gz")
            .file(FileType.AGP, "valid_agp.agp.gz")
            .build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = validator.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTA).size()).isOne();
    assertThat(submissionFiles.get(FileType.AGP).size()).isOne();
    validator.validate();
  }

  @Test
  public void testValidFlatFileAndAgp() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FLATFILE, "valid_flatfile.dat.gz")
            .file(FileType.AGP, "valid_agp.agp.gz")
            .build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = validator.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FLATFILE).size()).isOne();
    assertThat(submissionFiles.get(FileType.AGP).size()).isOne();
    validator.validate();
  }

  @Test
  public void testValidFastaAndAgpAndChromosomeList() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTA, "valid_fasta.fasta.gz")
            .file(FileType.AGP, "valid_agp.agp.gz")
            .file(FileType.CHROMOSOME_LIST, "valid_chromosome_list.txt.gz")
            .build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = validator.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(3);
    assertThat(submissionFiles.get(FileType.FASTA).size()).isOne();
    assertThat(submissionFiles.get(FileType.AGP).size()).isOne();
    assertThat(submissionFiles.get(FileType.CHROMOSOME_LIST).size()).isOne();
    validator.validate();
  }

  @Test
  public void testValidFlatFileAndAgpAndChromosomeList() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FLATFILE, "valid_flatfile.dat.gz")
            .file(FileType.AGP, "valid_agp.agp.gz")
            .file(FileType.CHROMOSOME_LIST, "valid_chromosome_list.txt.gz")
            .build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = validator.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(3);
    assertThat(submissionFiles.get(FileType.FLATFILE).size()).isOne();
    assertThat(submissionFiles.get(FileType.AGP).size()).isOne();
    assertThat(submissionFiles.get(FileType.CHROMOSOME_LIST).size()).isOne();
    validator.validate();
  }

  @Test
  public void testInvalidFasta() {
    File manifestFile =
        manifestBuilder().file(FileType.FASTA, "invalid_fasta.fasta.gz").build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    assertThatThrownBy(validator::validate)
        .isInstanceOf(WebinCliException.class)
        .hasMessageContaining("fasta file validation failed");
  }

  @Test
  public void testInvalidFlatFile() {
    File manifestFile =
        manifestBuilder().file(FileType.FLATFILE, "invalid_flatfile.dat.gz").build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    assertThatThrownBy(validator::validate)
        .isInstanceOf(WebinCliException.class)
        .hasMessageContaining("flatfile file validation failed");
  }

  @Test
  public void testInvalidAgp() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTA, "valid_fasta.fasta.gz")
            .file(FileType.AGP, "invalid_agp.agp.gz")
            .build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    assertThatThrownBy(validator::validate)
        .isInstanceOf(WebinCliException.class)
        .hasMessageContaining("agp file validation failed");
  }

  @Test
  public void testInvalidSequencelessChromosomeList() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTA, "valid_fasta.fasta.gz")
            .file(FileType.CHROMOSOME_LIST, "invalid_chromosome_list_sequenceless.txt.gz")
            .build();

    GenomeAssemblyWebinCli validator =
        validatorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    assertThatThrownBy(validator::validate)
        .isInstanceOf(WebinCliException.class)
        .hasMessageContaining("Sequenceless chromosomes are not allowed in assembly");
  }
}
