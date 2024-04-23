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
package uk.ac.ebi.ena.webin.cli.context.reads;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

import java.io.File;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.ReportTester;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutorBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.FileType;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

public class ReadsValidationTest {

  private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/reads");

  private static final String NAME = "test";

  private static ManifestBuilder manifestBuilder() {
    return new ManifestBuilder()
        .field("STUDY", "test")
        .field("SAMPLE", "test")
        .field("PLATFORM", "ILLUMINA")
        .field("INSTRUMENT", "unspecified")
        .field("NAME", NAME)
        .field("INSERT_SIZE", "1")
        .field("LIBRARY_STRATEGY", "CLONEEND")
        .field("LIBRARY_SOURCE", "OTHER")
        .field("LIBRARY_SELECTION", "Inverse rRNA selection");
  }

  private static WebinCliExecutorBuilder<ReadsManifest, ReadsValidationResponse> executorBuilder =
      new WebinCliExecutorBuilder(
          ReadsManifest.class, WebinCliExecutorBuilder.MetadataProcessorType.MOCK);

  @Test
  public void invalidBAM() {
    File manifestFile = manifestBuilder().file(FileType.BAM, "invalid.bam").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.BAM).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission(false))
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");

    new ReportTester(executor)
        .textInFileReport( NAME, "webin-cli", "Submitted files must contain a minimum of 1 sequence read");
  }

  @Test
  public void validBAM() {
    File manifestFile = manifestBuilder().file(FileType.BAM, "valid.bam").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.BAM).size()).isOne();
    executor.validateSubmission(false);
  }

  @Test
  public void invaliFastq() {
    File manifestFile = manifestBuilder().file(FileType.FASTQ, "invalid.fastq.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission(false))
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");

    new ReportTester(executor).textInFileReport(NAME, "webin-cli", "Sequence header must start with @");
  }

  @Test
  public void validFastq() {
    File manifestFile = manifestBuilder().file(FileType.FASTQ, "valid.fastq.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();
    executor.validateSubmission(false);
  }

  @Test
  public void validPairedFastqTwoFiles() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "valid_paired_1.fastq.gz")
            .file(FileType.FASTQ, "valid_paired_2.fastq.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);
    executor.validateSubmission(false);
    assertThat(executor.getValidationResponse().isPaired());
  }

  @Test
  public void validPairedFastqOneFile() {
    File manifestFile =
        manifestBuilder().file(FileType.FASTQ, "valid_paired_single_fastq.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();
    executor.validateSubmission(false);
    assertThat(executor.getValidationResponse().isPaired());
  }

  @Test
  public void invalidPairedFastqTwoFiles() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "invalid_not_paired_1.fastq.gz")
            .file(FileType.FASTQ, "invalid_not_paired_2.fastq.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);

    assertThatThrownBy(() -> executor.validateSubmission(false))
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");

    new ReportTester(executor)
        .textInSubmissionReport(NAME,
            "Detected paired fastq submission with less than 20% of paired reads");
  }

  @Test
  public void uracilFastq() {
    File manifestFile = manifestBuilder().file(FileType.FASTQ, "uracil-bases.fastq.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();

    executor.validateSubmission(false);
  }

  @Test
  public void invalidCram() {
    File manifestFile = manifestBuilder().file(FileType.CRAM, "invalid.cram").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission(false))
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");

    new ReportTester(executor)
        .textInFileReport(NAME, "webin-cli", "Submitted files must contain a minimum of 1 sequence read");
  }

  @Test
  public void validCram() {
    File manifestFile = manifestBuilder().file(FileType.CRAM, "valid.cram").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifests().stream().findFirst().get().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();
    executor.validateSubmission(false);
  }
}
