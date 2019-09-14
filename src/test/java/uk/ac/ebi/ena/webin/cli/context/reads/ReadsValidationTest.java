package uk.ac.ebi.ena.webin.cli.context.reads;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.assertReportContains;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.resourceDir;
import static uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader.Field.QUALITY_SCORE;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutorBuilder;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.FileType;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.QualityScore;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

public class ReadsValidationTest {

  private static final File RESOURCE_DIR = resourceDir("uk/ac/ebi/ena/webin/cli/reads");

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
  public void
  dataFileDoesNotExist() {

    for (FileType fileType : FileType.values()) {
      File manifestFile =
          manifestBuilder().file(fileType, "doesnotexisits.fastq.gz.bz2").build();

      assertThatThrownBy(() -> executorBuilder.build(manifestFile, RESOURCE_DIR).readManifest())
          .isInstanceOf(WebinCliException.class)
          .hasMessageStartingWith("Invalid manifest file");

      assertReportContains(executorBuilder.getParameters().getOutputDir().toString(), manifestFile.getName() + ".report",
          "ERROR: Invalid " + fileType.name() + " file name");
    }
  }

  @Test
  public void
  dataFileIsDirectory() throws IOException {

    for (FileType fileType : FileType.values()) {
      File manifestFile =
          manifestBuilder().file(fileType, createOutputFolder()).build();

      assertThatThrownBy(() -> executorBuilder.build(manifestFile, RESOURCE_DIR).readManifest())
          .isInstanceOf(WebinCliException.class)
          .hasMessageStartingWith("Invalid manifest file");

      assertReportContains(executorBuilder.getParameters().getOutputDir().toString(), manifestFile.getName() + ".report",
          "ERROR: Invalid " + fileType.name() + " file name");
    }
  }

  @Test
  public void
  dataFileNoPath() {

    for (FileType fileType : FileType.values()) {
      File manifestFile =
          manifestBuilder().file(fileType, "").build();

      assertThatThrownBy(() -> executorBuilder.build(manifestFile, RESOURCE_DIR).readManifest())
          .isInstanceOf(WebinCliException.class)
          .hasMessageStartingWith("Invalid manifest file");

      assertReportContains(executorBuilder.getParameters().getOutputDir().toString(), manifestFile.getName() + ".report",
          "ERROR: No data files have been specified");
    }
  }

  @Test
  public void
  dataFileNonASCIIPath() throws IOException {

    URL url = ReadsValidationTest.class.getClassLoader()
        .getResource("uk/ac/ebi/ena/webin/cli/reads/invalid.fastq.gz");
    File gz = new File(URLDecoder.decode(url.getFile(), "UTF-8"));

    Path file = Files
        .write(Files.createTempFile("FILE", "Š.fq.gz"), Files.readAllBytes(gz.toPath()),
            StandardOpenOption.TRUNCATE_EXISTING);
    File manifestFile =
        manifestBuilder().
            file(FileType.FASTQ, file).
            build();

    assertThatThrownBy(() -> executorBuilder.build(manifestFile, RESOURCE_DIR).readManifest())
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");

    assertReportContains(executorBuilder.getParameters().getOutputDir().toString(), manifestFile.getName() + ".report",
        "File name should conform following regular expression");
  }

  @Test
  public void
  incorrectQualityScore() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "valid.fastq.gz")
            .field(QUALITY_SCORE, "PHRED_34")
            .build();

    assertThatThrownBy(() -> executorBuilder.build(manifestFile, RESOURCE_DIR).readManifest())
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  correctQualityScore() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "valid.fastq.gz")
            .field(QUALITY_SCORE, "PHRED_33")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
            executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    assertThat(executor.getManifestReader().getManifest().getQualityScore()).isEqualTo(QualityScore.PHRED_33);
  }

  @Test
  public void
  incorrectBAM() {
    File manifestFile =
        manifestBuilder().file(FileType.BAM, "invalid.bam").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.BAM).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  correctBAM() {
    File manifestFile =
        manifestBuilder().file(FileType.BAM, "valid.bam").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.BAM).size()).isOne();
    executor.validateSubmission();
  }

  @Test
  public void
  sameFilePairedFastq() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "valid.fastq.gz")
            .file(FileType.FASTQ, "valid.fastq.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  correctTwoFastq() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "valid_paired_1.fastq.gz")
            .file(FileType.FASTQ, "valid_paired_2.fastq.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);
    executor.validateSubmission();
    assertThat(executor.getValidationResponse().isPaired());
  }

  @Test
  public void
  invalidTwoFastqNotPaired() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "invalid_not_paired_1.fastq.gz")
            .file(FileType.FASTQ, "invalid_not_paired_2.fastq.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  invalidOneFastq() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "invalid.fastq.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  correctFastq() {
    File manifestFile =
        manifestBuilder().file(FileType.FASTQ, "valid.fastq.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();
    executor.validateSubmission();
  }

  @Test
  public void
  correctOneFastqPaired() {
    File manifestFile =
        manifestBuilder().file(FileType.FASTQ, "valid_paired_single_fastq.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();
    executor.validateSubmission();
    assertThat(executor.getValidationResponse().isPaired());
  }

  @Test
  public void
  incorrectCram() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.CRAM, "invalid.cram")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  validCram() {
    File manifestFile =
        manifestBuilder().file(FileType.CRAM, "valid.cram").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();
    executor.validateSubmission();
  }

  // TODO: move to readtools
  @Test
  public void
  dataFileReportWritten() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.BAM, "invalid.bam")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);
    executor.readManifest();
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.BAM).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");

    for (SubmissionFile sf: executor.getManifestReader().getManifest().getFiles().get()) {
      assertReportContains(sf.getReportFile().toPath(), "ERROR: File contains no valid reads");
    }
  }

  private File
  createOutputFolder() throws IOException {
    File output = File.createTempFile("test", "test");
    Assert.assertTrue(output.delete());
    Assert.assertTrue(output.mkdirs());
    return output;
  }
}
