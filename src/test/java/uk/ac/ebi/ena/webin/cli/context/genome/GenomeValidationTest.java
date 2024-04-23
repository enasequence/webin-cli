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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getDefaultSample;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

import java.io.File;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.*;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutorBuilder;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest.FileType;

public class GenomeValidationTest {

  private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/genome");

  private static final String NAME = "test";

  private static ManifestBuilder manifestBuilder() {
    return new ManifestBuilder()
        .field("STUDY", "test")
        .field("SAMPLE", "test")
        .field("ASSEMBLY_TYPE", "clone or isolate")
        .field("COVERAGE", "1")
        .field("PROGRAM", "test")
        .field("PLATFORM", "test")
        .field("NAME", NAME);
  }

  private static final WebinCliExecutorBuilder<GenomeManifest, ValidationResponse> executorBuilder =
      new WebinCliExecutorBuilder(
              GenomeManifest.class, WebinCliExecutorBuilder.MetadataProcessorType.MOCK)
          .sample(NAME, getDefaultSample());

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidFasta() {
    File manifestFile = manifestBuilder().file(FileType.FASTA, "valid.fasta.gz").build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTA).size()).isOne();
    executor.validateSubmission(false);
  }

  @Test
  public void testValidFlatFileWithSubmitterReferenceInManifest() {
    File manifestFile =
        manifestBuilder()
            .field(
                GenomeManifestReader.Field.ADDRESS,
                "Biologische Anstalt Helgoland, Alfred-Wegener-Institut, Helmholtz "
                    + "Zentrum für Polar- und Meeresforschung, Kurpromenade 27498 Helgoland, Germany")
            .field(GenomeManifestReader.Field.AUTHORS, "Kirstein   Ivan, Wichels Alfred..;")
            .file(FileType.FLATFILE, "valid.flatfile.gz")
            .build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FLATFILE).size()).isOne();
    executor.validateSubmission(false);
  }

  @Test
  public void testValidFlatFile() {
    File manifestFile = manifestBuilder().file(FileType.FLATFILE, "valid.flatfile.gz").build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FLATFILE).size()).isOne();
    executor.validateSubmission(false);
  }

  @Test
  public void testValidFastaAndAgp() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTA, "valid.fasta.gz")
            .file(FileType.AGP, "valid.agp.gz")
            .build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTA).size()).isOne();
    assertThat(submissionFiles.get(FileType.AGP).size()).isOne();
    executor.validateSubmission(false);
  }

  @Test
  public void testValidFlatFileAndAgp() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FLATFILE, "valid.flatfile.gz")
            .file(FileType.AGP, "valid.agp.gz")
            .build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FLATFILE).size()).isOne();
    assertThat(submissionFiles.get(FileType.AGP).size()).isOne();
    executor.validateSubmission(false);
  }

  @Test
  public void testValidFastaAndAgpAndChromosomeList() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTA, "valid.fasta.gz")
            .file(FileType.AGP, "valid.agp.gz")
            .file(FileType.CHROMOSOME_LIST, "valid_chromosome_list.txt.gz")
            .build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(3);
    assertThat(submissionFiles.get(FileType.FASTA).size()).isOne();
    assertThat(submissionFiles.get(FileType.AGP).size()).isOne();
    assertThat(submissionFiles.get(FileType.CHROMOSOME_LIST).size()).isOne();
    executor.validateSubmission(false);
  }

  @Test
  public void testValidFlatFileAndAgpAndChromosomeList() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FLATFILE, "valid.flatfile.gz")
            .file(FileType.AGP, "valid.agp.gz")
            .file(FileType.CHROMOSOME_LIST, "valid_chromosome_list.txt.gz")
            .build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(3);
    assertThat(submissionFiles.get(FileType.FLATFILE).size()).isOne();
    assertThat(submissionFiles.get(FileType.AGP).size()).isOne();
    assertThat(submissionFiles.get(FileType.CHROMOSOME_LIST).size()).isOne();
    executor.validateSubmission(false);
  }

  @Test
  public void testInvalidFasta() {
    File manifestFile = manifestBuilder().file(FileType.FASTA, "invalid.fasta.gz").build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    assertThatThrownBy(() -> executor.validateSubmission(false)).isInstanceOf(WebinCliException.class);

    new ReportTester(executor).textInSubmissionReport(NAME, "fasta file validation failed");
  }

  @Test
  public void testInvalidFlatFile() {
    File manifestFile = manifestBuilder().file(FileType.FLATFILE, "invalid.flatfile.gz").build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    assertThatThrownBy(() -> executor.validateSubmission(false)).isInstanceOf(WebinCliException.class);

    new ReportTester(executor).textInSubmissionReport(NAME, "flatfile file validation failed");
  }

  @Test
  public void testInvalidAgp() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTA, "valid.fasta.gz")
            .file(FileType.AGP, "invalid.agp.gz")
            .build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    assertThatThrownBy(() -> executor.validateSubmission(false)).isInstanceOf(WebinCliException.class);

    new ReportTester(executor).textInSubmissionReport(NAME, "Invalid number of columns");
  }

  @Test
  public void testInvalidSequencelessChromosomeList() {

    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTA, "valid.fasta.gz")
            .file(FileType.CHROMOSOME_LIST, "invalid_chromosome_list_sequenceless.txt.gz")
            .build();

    WebinCliExecutor<GenomeManifest, ValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    assertThatThrownBy(() -> executor.validateSubmission(false)).isInstanceOf(WebinCliException.class);

    new ReportTester(executor)
        .textInSubmissionReport(NAME, "Sequenceless chromosomes are not allowed in assembly");
  }
}
