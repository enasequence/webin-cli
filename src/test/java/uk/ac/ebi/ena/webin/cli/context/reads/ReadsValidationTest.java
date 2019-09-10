package uk.ac.ebi.ena.webin.cli.context.reads;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.FileType;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.QualityScore;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

public class ReadsValidationTest {

  private static final File RESOURCE_DIR = resourceDir("uk/ac/ebi/ena/webin/cli/rawreads");

  private static ManifestBuilder manifestBuilder() {
    return new ManifestBuilder()
        .field("STUDY", "test")
        .field("SAMPLE", "test")
        .field("PLATFORM", "ILLUMINA")
        .field("INSTRUMENT", "unspecified")
        .field("NAME", "test")
        .field("INSERT_SIZE", "1")
        .field("LIBRARY_STRATEGY", "CLONEEND")
        .field("LIBRARY_SOURCE", "OTHER")
        .field("LIBRARY_SELECTION", "Inverse rRNA selection");
  }

  private static final WebinCliExecutorBuilder<ReadsManifest, ReadsValidationResponse> executorBuilder =
      new WebinCliExecutorBuilder(ReadsManifest.class)
          .manifestMetadataProcessors(false);

  @Test
  public void
  fastqFileDoesNotExist() {
    /*
    TODO: check error messages
    ERROR: Invalid BAM file name. Could not read file: "missing".
    ERROR: Invalid BAM file suffix: "missing". Valid file suffixes are: .bam.
    ERROR: Invalid CRAM file name. Could not read file: "missing".
    ERROR: Invalid CRAM file suffix: "missing". Valid file suffixes are: .cram.
    ERROR: Invalid FASTQ file name. Could not read file: "missing".
    ERROR: Invalid FASTQ file suffix: "missing". Valid file suffixes are: .gz, .bz2.
     */

    for (FileType fileType : FileType.values()) {
      File manifestFile =
          manifestBuilder().file(fileType, "doesnotexisits.fastq.gz.bz2").build();

      assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
          .isInstanceOf(WebinCliException.class)
          .hasMessageStartingWith("Invalid manifest file");
    }
  }

  @Test
  public void
  fileIsDirectory() throws IOException {
    /*
    TODO: check error messages
    ERROR: Invalid BAM file name. Could not read file: "test5399168466889766446test".
    ERROR: Invalid BAM file suffix: "test5399168466889766446test". Valid file suffixes are: .bam.
    ERROR: Invalid CRAM file name. Could not read file: "test1251089343390577154test".
    ERROR: Invalid CRAM file suffix: "test1251089343390577154test". Valid file suffixes are: .cram.
    ERROR: Invalid FASTQ file name. Could not read file: "test4190692009155610561test".
    ERROR: Invalid FASTQ file suffix: "test4190692009155610561test". Valid file suffixes are: .gz, .bz2.
     */

    for (FileType fileType : FileType.values()) {
      File manifestFile =
          manifestBuilder().file(fileType, createOutputFolder()).build();

      assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
          .isInstanceOf(WebinCliException.class)
          .hasMessageStartingWith("Invalid manifest file");
    }
  }

  @Test
  public void
  fileNoPath() {
    /*
    TODO: check error messages
    ERROR: No data files have been specified. Expected data files are: [1-2 FASTQ] or [1 CRAM] or [1 BAM].
    ERROR: No data files have been specified. Expected data files are: [1-2 FASTQ] or [1 CRAM] or [1 BAM].
    ERROR: No data files have been specified. Expected data files are: [1-2 FASTQ] or [1 CRAM] or [1 BAM].
     */

    for (FileType fileType : FileType.values()) {
      File manifestFile =
          manifestBuilder().file(fileType, "").build();

      assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
          .isInstanceOf(WebinCliException.class)
          .hasMessageStartingWith("Invalid manifest file");
    }
  }

  @Test
  public void
  fileNonASCIIPath() throws IOException {
    /*
    TODO: check error messages
    ERROR: Invalid FASTQ file name. Could not read file: "FILE7663404904618886897Š.fq.gz".
    ERROR: Invalid FASTQ file name: "FILE7663404904618886897Š.fq.gz". File name should conform following regular expression: ^([\p{Alnum}]|\\|\]|\[|#|-|_|\.|,|\/|:|@|\+| |\(|\)|'|~|<|%|\?)+$.
     */

    URL url = ReadsWebinCliTest.class.getClassLoader()
        .getResource("uk/ac/ebi/ena/webin/cli/rawreads/MG23S_431.fastq.gz");
    File gz = new File(URLDecoder.decode(url.getFile(), "UTF-8"));

    Path file = Files
        .write(Files.createTempFile("FILE", "Š.fq.gz"), Files.readAllBytes(gz.toPath()),
            StandardOpenOption.TRUNCATE_EXISTING);
    File manifestFile =
        manifestBuilder().
            file(FileType.FASTQ, file).
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  incorrectQualityScore() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz")
            .field(QUALITY_SCORE, "PHRED_34")
            .build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  correctQualityScore() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz")
            .field(QUALITY_SCORE, "PHRED_33")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> ex = executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    assertThat(ex.getManifestReader().getManifest().getQualityScore()).isEqualTo(QualityScore.PHRED_33);
  }

  @Test
  public void
  incorrectBAM() {
    File manifestFile =
        manifestBuilder().file(FileType.BAM, "m54097_170904_165950.subreads.bam").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.BAM).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  correctFastq() {
    File manifestFile =
        manifestBuilder().file(FileType.FASTQ, "ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();
    executor.validateSubmission();
  }

  @Test
  public void
  correctBAM() {
    File manifestFile =
        manifestBuilder().file(FileType.BAM, "OUTO500m_MetOH_narG_OTU18.bam").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
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
            .file(FileType.FASTQ, "ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz")
            .file(FileType.FASTQ, "ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
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
            .file(FileType.FASTQ, "EP0_GTTCCTT_S1.txt.gz")
            .file(FileType.FASTQ, "EP0_GTTCCTT_S2.txt.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
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
            .file(FileType.FASTQ, "EP0_GTTCCTT_P1.txt.gz")
            .file(FileType.FASTQ, "EP0_GTTCCTT_P2.txt.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
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
            .file(FileType.FASTQ, "MG23S_431.fastq.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  correctOneFastqPaired() {
    File manifestFile =
        manifestBuilder().file(FileType.FASTQ, "EP0_GTTCCTT_0.txt.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
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
            .file(FileType.CRAM, "15194_1#135.cram")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  correctCram() {
    File manifestFile =
        manifestBuilder().file(FileType.CRAM, "18045_1#93.cram").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();
    executor.validateSubmission();
  }

  private File
  createOutputFolder() throws IOException {
    File output = File.createTempFile("test", "test");
    Assert.assertTrue(output.delete());
    Assert.assertTrue(output.mkdirs());
    return output;
  }
}
