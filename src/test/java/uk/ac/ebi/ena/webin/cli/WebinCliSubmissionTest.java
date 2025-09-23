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
package uk.ac.ebi.ena.webin.cli;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Assert;
import org.junit.Test;

public class WebinCliSubmissionTest {

  private enum WebinCliTestType {
    VALIDATE,
    SUBMIT
  }

  private static final WebinCliTestType TEST_TYPE = WebinCliTestType.SUBMIT;

  private static final File READS_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/reads/");
  private static final File GENOME_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/genome/");
  private static final File TRANSCRIPTOME_RESOURCE_DIR =
      getResourceDir("uk/ac/ebi/ena/webin/cli/transcriptome/");
  private static final File SEQUENCE_RESOURCE_DIR =
      getResourceDir("uk/ac/ebi/ena/webin/cli/sequence/");
  private static final File TAX_REF_SET_RESOURCE_DIR =
      getResourceDir("uk/ac/ebi/ena/webin/cli/taxxrefset/");

  private static void expectError(
      ThrowableAssert.ThrowingCallable shouldThrow, WebinCliException.ErrorType errorType) {
    assertThatThrownBy(shouldThrow)
        .isInstanceOf(WebinCliException.class)
        .hasFieldOrPropertyWithValue("errorType", errorType);
  }

  private static WebinCli webinCliForValidate(
      File resourceDir,
      WebinCliBuilder webinCliBuilder,
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<WebinCliBuilder> webinCliConfig) {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    metaManifestConfig.accept(manifestBuilder);
    filesManifestConfig.accept(manifestBuilder);
    webinCliConfig.accept(webinCliBuilder);
    webinCliBuilder.submit(false);
    return webinCliBuilder.build(resourceDir, manifestBuilder);
  }

  private static WebinCli webinCLiForValidateFiles(
      File resourceDir,
      WebinCliBuilder webinCliBuilder,
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<WebinCliBuilder> webinCliConfig) {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    metaManifestConfig.accept(manifestBuilder);
    filesManifestConfig.accept(manifestBuilder);
    webinCliConfig.accept(webinCliBuilder);
    webinCliBuilder.validateFiles(true);
    webinCliBuilder.submit(false);
    return webinCliBuilder.build(resourceDir, manifestBuilder);
  }

  private static WebinCli webinCliForSubmit(
      File resourceDir,
      WebinCliBuilder webinCliBuilder,
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<WebinCliBuilder> webinCliConfig) {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    metaManifestConfig.accept(manifestBuilder);
    filesManifestConfig.accept(manifestBuilder);
    webinCliConfig.accept(webinCliBuilder);
    webinCliBuilder.submit(true);
    return webinCliBuilder.build(resourceDir, manifestBuilder);
  }

  public static void testReads(
      Consumer<ManifestBuilder> metaManifestConfig, Consumer<ManifestBuilder> filesManifestConfig)
      throws Throwable {
    testReads(metaManifestConfig, filesManifestConfig, c -> {});
  }

  public static void testReads(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<WebinCliBuilder> webinCliConfig)
      throws Throwable {
    // Test -validateFiles option
    webinCLiForValidateFiles(
            READS_RESOURCE_DIR,
            WebinCliBuilder.createForReads(),
            metaManifestConfig,
            filesManifestConfig,
            webinCliConfig)
        .execute();

    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      webinCliForValidate(
              READS_RESOURCE_DIR,
              WebinCliBuilder.createForReads(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    } else {
      webinCliForSubmit(
              READS_RESOURCE_DIR,
              WebinCliBuilder.createForReads(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    }
  }

  public static void testReadError(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<ReportTester> reportTesterConfig,
      WebinCliException.ErrorType errorType) {
    // Test -validateFiles option
    {
      WebinCli webinCli =
          webinCLiForValidateFiles(
              READS_RESOURCE_DIR,
              WebinCliBuilder.createForReads(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(() -> webinCli.execute(), errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    }

    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      WebinCli webinCli =
          webinCliForValidate(
              READS_RESOURCE_DIR,
              WebinCliBuilder.createForReads(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(() -> webinCli.execute(), errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    } else {
      WebinCli webinCli =
          webinCliForSubmit(
              READS_RESOURCE_DIR,
              WebinCliBuilder.createForReads(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(() -> webinCli.execute(), errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    }
  }

  public static void testGenome(
      Consumer<ManifestBuilder> metaManifestConfig, Consumer<ManifestBuilder> filesManifestConfig)
      throws Throwable {
    testGenome(metaManifestConfig, filesManifestConfig, c -> {});
  }

  public static void testGenome(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<WebinCliBuilder> webinCliConfig)
      throws Throwable {
    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      webinCliForValidate(
              GENOME_RESOURCE_DIR,
              WebinCliBuilder.createForGenome(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    } else {
      webinCliForSubmit(
              GENOME_RESOURCE_DIR,
              WebinCliBuilder.createForGenome(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    }
  }

  public static void testGenomeError(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<ReportTester> reportTesterConfig,
      WebinCliException.ErrorType errorType) {
    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      WebinCli webinCli =
          webinCliForValidate(
              GENOME_RESOURCE_DIR,
              WebinCliBuilder.createForGenome(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(webinCli::execute, errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    } else {
      WebinCli webinCli =
          webinCliForSubmit(
              GENOME_RESOURCE_DIR,
              WebinCliBuilder.createForGenome(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(webinCli::execute, errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    }
  }

  public static void testSequence(
      Consumer<ManifestBuilder> metaManifestConfig, Consumer<ManifestBuilder> filesManifestConfig)
      throws Throwable {
    testSequence(metaManifestConfig, filesManifestConfig, c -> {});
  }

  public static void testSequence(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<WebinCliBuilder> webinCliConfig)
      throws Throwable {
    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      webinCliForValidate(
              SEQUENCE_RESOURCE_DIR,
              WebinCliBuilder.createForSequence(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    } else {
      webinCliForSubmit(
              SEQUENCE_RESOURCE_DIR,
              WebinCliBuilder.createForSequence(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    }
  }

  public static void testPolysample(
      Consumer<ManifestBuilder> metaManifestConfig, Consumer<ManifestBuilder> filesManifestConfig)
      throws Throwable {
    testPolysample(metaManifestConfig, filesManifestConfig, c -> {});
  }

  public static void testPolysample(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<WebinCliBuilder> webinCliConfig)
      throws Throwable {
    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      webinCliForValidate(
              SEQUENCE_RESOURCE_DIR,
              WebinCliBuilder.createForPolySample(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    } else {
      webinCliForSubmit(
              SEQUENCE_RESOURCE_DIR,
              WebinCliBuilder.createForPolySample(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    }
  }

  public static void testSequenceError(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<ReportTester> reportTesterConfig,
      WebinCliException.ErrorType errorType) {
    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      WebinCli webinCli =
          webinCliForValidate(
              SEQUENCE_RESOURCE_DIR,
              WebinCliBuilder.createForSequence(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(() -> webinCli.execute(), errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    } else {
      WebinCli webinCli =
          webinCliForSubmit(
              SEQUENCE_RESOURCE_DIR,
              WebinCliBuilder.createForSequence(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(() -> webinCli.execute(), errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    }
  }

  public static void testPolySampleError(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<ReportTester> reportTesterConfig,
      WebinCliException.ErrorType errorType) {
    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      WebinCli webinCli =
          webinCliForValidate(
              SEQUENCE_RESOURCE_DIR,
              WebinCliBuilder.createForPolySample(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(() -> webinCli.execute(), errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    } else {
      WebinCli webinCli =
          webinCliForSubmit(
              SEQUENCE_RESOURCE_DIR,
              WebinCliBuilder.createForPolySample(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(() -> webinCli.execute(), errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    }
  }

  public static void testTranscriptome(
      Consumer<ManifestBuilder> metaManifestConfig, Consumer<ManifestBuilder> filesManifestConfig)
      throws Throwable {
    testTranscriptome(metaManifestConfig, filesManifestConfig, c -> {});
  }

  public static void testTranscriptome(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<WebinCliBuilder> webinCliConfig)
      throws Throwable {
    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      webinCliForValidate(
              TRANSCRIPTOME_RESOURCE_DIR,
              WebinCliBuilder.createForTranscriptome(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    } else {
      webinCliForSubmit(
              TRANSCRIPTOME_RESOURCE_DIR,
              WebinCliBuilder.createForTranscriptome(),
              metaManifestConfig,
              filesManifestConfig,
              webinCliConfig)
          .execute();
    }
  }

  public static void testTranscriptomeError(
      Consumer<ManifestBuilder> metaManifestConfig,
      Consumer<ManifestBuilder> filesManifestConfig,
      Consumer<ReportTester> reportTesterConfig,
      WebinCliException.ErrorType errorType) {
    if (TEST_TYPE == WebinCliTestType.VALIDATE) {
      WebinCli webinCli =
          webinCliForValidate(
              TRANSCRIPTOME_RESOURCE_DIR,
              WebinCliBuilder.createForTranscriptome(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(() -> webinCli.execute(), errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    } else {
      WebinCli webinCli =
          webinCliForSubmit(
              TRANSCRIPTOME_RESOURCE_DIR,
              WebinCliBuilder.createForTranscriptome(),
              metaManifestConfig,
              filesManifestConfig,
              c -> {});
      expectError(() -> webinCli.execute(), errorType);
      reportTesterConfig.accept(new ReportTester(webinCli));
    }
  }

  // Manifest meta fields

  private void readsMetaManifest(ManifestBuilder manifestBuilder) {
    readsMetaManifest(manifestBuilder, WebinCliTestUtils.generateUniqueManifestName());
  }

  private void readsMetaManifest(ManifestBuilder manifestBuilder, String nameFieldValue) {
    manifestBuilder
        .field("NAME", nameFieldValue)
        .field("STUDY", "SRP052303")
        .field("SAMPLE", "ERS2554688")
        .field("PLATFORM", "ILLUMINA")
        .field("INSTRUMENT", "unspecifieD")
        .field("INSERT_SIZE", "1")
        .field("LIBRARY_NAME", "YOBA LIB")
        .field("LIBRARY_STRATEGY", "CLONEEND")
        .field("LIBRARY_SOURCE", "OTHER")
        .field("LIBRARY_SELECTION", "Inverse rRNA selection")
        .field("LIBRARY_CONSTRUCTION_PROTOCOL", "Protocol")
        .field("DESCRIPTION", "Some reads description");
  }

  private void genomeMetaManifest(ManifestBuilder manifestBuilder) {
    genomeMetaManifest(manifestBuilder, WebinCliTestUtils.generateUniqueManifestName());
  }

  private void genomeMAGManifest(ManifestBuilder manifestBuilder) {
    genomeMAGManifest(manifestBuilder, WebinCliTestUtils.generateUniqueManifestName());
  }

  private void genomeMAGManifest(ManifestBuilder manifestBuilder, String nameFieldValue) {
    manifestBuilder
        .field("NAME", nameFieldValue)
        .field("ASSEMBLY_TYPE", "Metagenome-Assembled Genome (MAG)")
        .field("COVERAGE", "45")
        .field("PROGRAM", "assembly")
        .field("PLATFORM", "fghgf")
        .field("MINGAPLENGTH", "34")
        .field("MOLECULETYPE", "genomic DNA")
        .field("SAMPLE", " SAMEA130950840")
        .field("STUDY", "PRJEB20083")
        .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
        .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
        .field("DESCRIPTION", "Some genome assembly description");
  }

  private void genomeMetaManifest(ManifestBuilder manifestBuilder, String nameFieldValue) {
    manifestBuilder
        .field("NAME", nameFieldValue)
        .field("ASSEMBLY_TYPE", "clone or isolate")
        .field("COVERAGE", "45")
        .field("PROGRAM", "assembly")
        .field("PLATFORM", "fghgf")
        .field("MINGAPLENGTH", "34")
        .field("MOLECULETYPE", "genomic DNA")
        .field("SAMPLE", "SAMN04526268")
        .field("STUDY", "PRJEB20083")
        .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
        .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
        .field("DESCRIPTION", "Some genome assembly description");
  }

  private void covid19GenomeMetaManifest(ManifestBuilder manifestBuilder) {
    manifestBuilder
        .field("NAME", WebinCliTestUtils.generateUniqueManifestName())
        .field("ASSEMBLY_TYPE", "COVID-19 outbreak")
        .field("STUDY", "ERP121228")
        .field("SAMPLE", "ERS5249578")
        .field("COVERAGE", "1.0")
        .field("PROGRAM", "prog-123")
        .field("PLATFORM", "ILLUMINA");
  }

  private void transcriptomeMetaManifest(ManifestBuilder manifestBuilder) {
    transcriptomeMetaManifest(manifestBuilder, WebinCliTestUtils.generateUniqueManifestName());
  }

  private void transcriptomeMetaManifest(ManifestBuilder manifestBuilder, String nameFieldValue) {
    manifestBuilder
        .field("NAME", nameFieldValue)
        .field("PROGRAM", "assembly")
        .field("PLATFORM", "fghgf")
        .field("SAMPLE", "SAMN04526268")
        .field("STUDY", "PRJEB20083")
        .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
        .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
        .field("DESCRIPTION", "Some transcriptome assembly description")
        .field("ASSEMBLY_TYPE", "isolate");
  }

  private void sequenceMetaManifest(ManifestBuilder manifestBuilder) {
    sequenceMetaManifest(manifestBuilder, WebinCliTestUtils.generateUniqueManifestName());
  }

  private void polySampleMetaManifest(ManifestBuilder manifestBuilder) {
    polySampleMetaManifest(manifestBuilder, WebinCliTestUtils.generateUniqueManifestName());
  }

  private void sequenceMetaManifest(ManifestBuilder manifestBuilder, String nameFieldValue) {
    manifestBuilder
        .field("NAME", nameFieldValue)
        .field("STUDY", "PRJEB20083")
        .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
        .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
        .field("DESCRIPTION", "Some sequence assembly description");
  }

  private void polySampleMetaManifest(ManifestBuilder manifestBuilder, String nameFieldValue) {
    manifestBuilder
        .field("NAME", nameFieldValue)
        .field("STUDY", "PRJEB20083")
        .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
        .field("ANALYSIS_TYPE", "SEQUENCE_SET")
        .field("ANALYSIS_PROTOCOL", "TEST")
        .field("DESCRIPTION", "Some sequence assembly description");
  }

  //

  private void lookupAndAssertGeneratedAlias(
      WebinCli webinCli, String submissionName, String xmlFileName, String expectedAlias)
      throws Throwable {
    webinCli.execute();

    Path generatedXml =
        webinCli
            .getParameters()
            .getOutputDir()
            .toPath()
            .resolve(webinCli.getParameters().getContext().toString())
            .resolve(submissionName)
            .resolve("submit")
            .resolve(xmlFileName + ".xml");

    String xml = new String(Files.readAllBytes(generatedXml));

    String expectedText = String.format("alias=\"%s\"", expectedAlias);

    Assert.assertTrue(xml.contains(expectedText));
  }

  private void executeAndAssertReceiptSuccess(WebinCli webinCli, int expectedSubmissionCount)
      throws Throwable {
    webinCli.execute();

    Path outputContextPath =
        webinCli
            .getParameters()
            .getOutputDir()
            .toPath()
            .resolve(webinCli.getParameters().getContext().toString());

    List<Path> outputSubmissionPaths = Files.list(outputContextPath).collect(Collectors.toList());

    Assert.assertEquals(expectedSubmissionCount, outputSubmissionPaths.size());

    for (Path outSubPath : outputSubmissionPaths) {
      Path receiptFile = outSubPath.resolve("submit").resolve("receipt.xml");

      Assert.assertTrue(receiptFile.toFile().exists());
      Assert.assertTrue(
          new String(Files.readAllBytes(receiptFile), StandardCharsets.UTF_8)
              .contains("success=\"true\""));
    }
  }

  private Path createReadsSampleJsonManifest(int submissionCount) throws IOException {
    if (submissionCount < 1) {
      submissionCount = 1;
    }

    Path manifestPath = Files.createTempFile("webin-cli-manifest-", ".txt");

    ObjectMapper objectMapper = new ObjectMapper();

    ArrayNode submissions = objectMapper.createArrayNode();
    for (int i = 0; i < submissionCount; i++) {
      ObjectNode submission = submissions.addObject();
      submission
          .put("NAME", WebinCliTestUtils.generateUniqueManifestName())
          .put("STUDY", "SRP052303")
          .put("PLATFORM", "ILLUMINA")
          .put("INSTRUMENT", "unspecified")
          .put("INSERT_SIZE", "1")
          .put("LIBRARY_NAME", "YOBA LIB")
          .put("LIBRARY_STRATEGY", "CLONEEND")
          .put("LIBRARY_SOURCE", "OTHER")
          .put("LIBRARY_SELECTION", "Inverse rRNA selection")
          .put("LIBRARY_CONSTRUCTION_PROTOCOL", "Protocol")
          .put("DESCRIPTION", "Some reads description")
          .put("FASTQ", "10x/4fastq/R1.fastq.gz");

      submission.set("SAMPLE", WebinCliTestUtils.createSampleJson());
    }

    objectMapper.writeValue(manifestPath.toFile(), submissions);

    return manifestPath;
  }

  // Reads

  @Test
  public void testReadsCram() throws Throwable {
    testReads(m -> readsMetaManifest(m), m -> m.file("CRAM", "valid.cram"));
  }

  @Test
  public void testReadsCramNormalizePath() throws Throwable {
    testReads(
        m -> readsMetaManifest(m),
        m -> m.file("CRAM", "../" + READS_RESOURCE_DIR.getName() + "/valid.cram"));
  }

  @Test
  public void testReadsCramWithInfo() throws Throwable {
    ManifestBuilder infoManifestBuilder = new ManifestBuilder();
    readsMetaManifest(infoManifestBuilder);
    File infoFile = infoManifestBuilder.build();
    testReads(m -> {}, m -> m.file("CRAM", "valid.cram").field("INFO", infoFile.getAbsolutePath()));
  }

  /**
   * This test may fail due to Aspera server problems on rare occassions. It is safe to disregard it
   * in such instances as long as the FTP tests continue to work.
   */
  @Test
  public void testReadsCramWithAscp() throws Throwable {
    testReads(m -> readsMetaManifest(m), m -> m.file("CRAM", "valid.cram"), c -> c.ascp(true));
  }

  @Test
  public void testReadsSingleFastq() throws Throwable {
    testReads(m -> readsMetaManifest(m), m -> m.file("FASTQ", "10x/4fastq/I1.fastq.gz"));
  }

  @Test
  public void testReadsMultipleFastq() throws Throwable {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    manifestBuilder.jsonFormat();
    readsMetaManifest(manifestBuilder);
    manifestBuilder.file("FASTQ", "10x/4fastq/I1.fastq.gz").attribute("READ_TYPE", "cell_barcode");
    manifestBuilder.file("FASTQ", "10x/4fastq/R1.fastq.gz").attribute("READ_TYPE", "umi_barcode");
    manifestBuilder
        .file("FASTQ", "10x/4fastq/R2.fastq.gz")
        .attribute("READ_TYPE", "feature_barcode");
    manifestBuilder
        .file("FASTQ", "10x/4fastq/R3.fastq.gz")
        .attribute("READ_TYPE", "sample_barcode");

    WebinCliBuilder webinCliBuilder = WebinCliBuilder.createForReads();
    webinCliBuilder.submit(true);
    webinCliBuilder.build(READS_RESOURCE_DIR, manifestBuilder).execute();
  }

  @Test
  public void testReadsGeneratedXmlAlias() throws Throwable {
    String name = "test-name";

    WebinCli webinCli =
        webinCliForValidate(
            READS_RESOURCE_DIR,
            WebinCliBuilder.createForReads(),
            m -> readsMetaManifest(m, name),
            m -> m.file("FASTQ", "10x/4fastq/I1.fastq.gz"),
            c -> {});

    lookupAndAssertGeneratedAlias(webinCli, name, "experiment", "webin-reads-" + name);
  }

  @Test
  public void testReadsMultiSubmission() throws Throwable {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    manifestBuilder.jsonFormat();

    // First submission
    readsMetaManifest(manifestBuilder);
    manifestBuilder.file("FASTQ", "10x/4fastq/R1.fastq.gz");

    // Second submission
    manifestBuilder.fieldGroup();
    readsMetaManifest(manifestBuilder);
    manifestBuilder.file("FASTQ", "10x/4fastq/R2.fastq.gz");

    // Third submission
    manifestBuilder.fieldGroup();
    readsMetaManifest(manifestBuilder);
    manifestBuilder.file("FASTQ", "10x/4fastq/R3.fastq.gz");

    WebinCliBuilder webinCliBuilder = WebinCliBuilder.createForReads();
    webinCliBuilder.submit(true);

    executeAndAssertReceiptSuccess(webinCliBuilder.build(READS_RESOURCE_DIR, manifestBuilder), 3);
  }

  @Test
  public void testReadsMultiSubmissionWithSampleJson() throws Throwable {
    WebinCliBuilder webinCliBuilder = WebinCliBuilder.createForReads();
    webinCliBuilder.submit(true);

    executeAndAssertReceiptSuccess(
        webinCliBuilder.build(
            READS_RESOURCE_DIR.toPath(), createReadsSampleJsonManifest(3).toFile()),
        3);
  }

  // Genome

  @Test
  public void testGenomeFlatFile() throws Throwable {
    testGenome(m -> genomeMetaManifest(m), m -> m.file("FLATFILE", "valid.flatfile.gz"));
  }

  @Test
  public void testGenomeFlatFileWithInfo() throws Throwable {
    ManifestBuilder infoManifestBuilder = new ManifestBuilder();
    genomeMetaManifest(infoManifestBuilder);
    File infoFile = infoManifestBuilder.build();
    testGenome(
        m -> {},
        m -> m.file("FLATFILE", "valid.flatfile.gz").field("INFO", infoFile.getAbsolutePath()));
  }

  @Test
  public void testCovid19Genome() throws Throwable {
    testGenome(
        m -> covid19GenomeMetaManifest(m),
        m ->
            m.file("FASTA", "valid-covid19.fasta.gz")
                .file("CHROMOSOME_LIST", "valid-covid19-chromosome.list.gz"),
        c -> c.ignoreErrors(true)); // cannot submit more than 1 genome within 24 hours
  }

  @Test
  public void testGenomeFlatFileWithFormatError() {
    String name = String.format("TEST%X", System.nanoTime());

    testGenomeError(
        m -> {
          genomeMetaManifest(m, name);
        },
        m -> m.file("FLATFILE", "invalid.flatfile.gz"),
        r ->
            r.textInFileReport(
                name, "invalid.flatfile.gz", "ERROR: Invalid ID line format [ line: 1]"),
        WebinCliException.ErrorType.VALIDATION_ERROR);
  }

  @Test
  public void testGenomeGeneratedXmlAlias() throws Throwable {
    String name = "test-name";

    WebinCli webinCli =
        webinCliForValidate(
            GENOME_RESOURCE_DIR,
            WebinCliBuilder.createForGenome(),
            m -> genomeMetaManifest(m, name),
            m -> m.file("FLATFILE", "valid.flatfile.gz"),
            c -> {});

    lookupAndAssertGeneratedAlias(webinCli, name, "analysis", "webin-genome-" + name);
  }

  @Test
  public void testGenomeMultiSubmission() throws Throwable {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    manifestBuilder.jsonFormat();

    // First submission
    genomeMetaManifest(manifestBuilder);
    manifestBuilder.file("FLATFILE", "valid.flatfile.gz");

    // Second submission
    manifestBuilder.fieldGroup();
    genomeMetaManifest(manifestBuilder);
    manifestBuilder.file("FLATFILE", "valid.flatfile.gz");

    // Third submission
    manifestBuilder.fieldGroup();
    genomeMetaManifest(manifestBuilder);
    manifestBuilder.file("FLATFILE", "valid.flatfile.gz");

    WebinCliBuilder webinCliBuilder = WebinCliBuilder.createForGenome();
    webinCliBuilder.submit(true);

    executeAndAssertReceiptSuccess(webinCliBuilder.build(GENOME_RESOURCE_DIR, manifestBuilder), 3);
  }

  @Test
  public void testGenomeMAGSubmission() throws Throwable {
    String name = "test-name";

    testGenomeError(
        m -> genomeMAGManifest(m, name),
        m -> m.file("FLATFILE", "valid.flatfile.gz"),
        r ->
            r.textInSubmissionReport(
                "test-name",
                "ERROR: Assembly type: MAG (METAGENOME-ASSEMBLED GENOME) cannot reference a sample having a metagenome taxonomy"),
        WebinCliException.ErrorType.VALIDATION_ERROR);
  }

  // Sequence

  @Test
  public void testSequenceTab() throws Throwable {
    testSequence(m -> sequenceMetaManifest(m), m -> m.file("TAB", "valid/ERT000003-EST.tsv.gz"));
  }

  @Test
  public void testSequenceTabWithSampleInOrganismField() throws Throwable {
    testSequence(
        m -> sequenceMetaManifest(m),
        m -> m.file("TAB", "valid/ERT000002_rRNA-with-sample-field.tsv.gz"));
  }

  @Test
  public void testSequenceTabWithInvalidSample() {
    String name = String.format("TEST%X", System.nanoTime());

    testSequenceError(
        m -> {
          sequenceMetaManifest(m, name);
        },
        m -> m.file("TAB", "invalid-sample.tsv.gz"),
        r ->
            r.textInFileReport(
                name, "invalid-sample.tsv.gz", "Organism name \"ERS000000\" is not submittable"),
        WebinCliException.ErrorType.VALIDATION_ERROR);
  }

  @Test
  public void testSequenceFlatFileWithFormatError() {
    String name = String.format("TEST%X", System.nanoTime());

    testSequenceError(
        m -> {
          sequenceMetaManifest(m, name);
        },
        m -> m.file("FLATFILE", "invalid.flatfile.gz"),
        r ->
            r.textInFileReport(
                name, "invalid.flatfile.gz", "ERROR: Invalid ID line format [ line: 1]"),
        WebinCliException.ErrorType.VALIDATION_ERROR);
  }

  @Test
  public void testSequenceGeneratedXmlAlias() throws Throwable {
    String name = "test-name";

    WebinCli webinCli =
        webinCliForValidate(
            SEQUENCE_RESOURCE_DIR,
            WebinCliBuilder.createForSequence(),
            m -> sequenceMetaManifest(m, name),
            m -> m.file("TAB", "valid/ERT000003-EST.tsv.gz"),
            c -> {});

    lookupAndAssertGeneratedAlias(webinCli, name, "analysis", "webin-sequence-" + name);
  }

  @Test
  public void testSequenceMultiSubmission() throws Throwable {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    manifestBuilder.jsonFormat();

    // First submission
    sequenceMetaManifest(manifestBuilder);
    manifestBuilder.file("TAB", "valid/ERT000003-EST.tsv.gz");

    // Second submission
    manifestBuilder.fieldGroup();
    sequenceMetaManifest(manifestBuilder);
    manifestBuilder.file("TAB", "valid/ERT000003-EST.tsv.gz");

    // Third submission
    manifestBuilder.fieldGroup();
    sequenceMetaManifest(manifestBuilder);
    manifestBuilder.file("TAB", "valid/ERT000003-EST.tsv.gz");

    WebinCliBuilder webinCliBuilder = WebinCliBuilder.createForSequence();
    webinCliBuilder.submit(true);

    executeAndAssertReceiptSuccess(
        webinCliBuilder.build(SEQUENCE_RESOURCE_DIR, manifestBuilder), 3);
  }

  // Transcriptome

  @Test
  public void testTranscriptomeFasta() throws Throwable {
    testTranscriptome(m -> transcriptomeMetaManifest(m), m -> m.file("FASTA", "valid.fasta.gz"));
  }

  @Test
  public void testTranscriptomeFastaWithInfo() throws Throwable {
    ManifestBuilder infoManifestBuilder = new ManifestBuilder();
    transcriptomeMetaManifest(infoManifestBuilder);
    File infoFile = infoManifestBuilder.build();
    testTranscriptome(
        m -> {}, m -> m.file("FASTA", "valid.fasta.gz").field("INFO", infoFile.getAbsolutePath()));
  }

  @Test
  public void testTranscriptomeFlatFileWithFormatError() {
    String name = String.format("TEST%X", System.nanoTime());

    testTranscriptomeError(
        m -> {
          transcriptomeMetaManifest(m, name);
        },
        m -> m.file("FLATFILE", "invalid.flatfile.gz"),
        r ->
            r.textInFileReport(
                name, "invalid.flatfile.gz", "ERROR: Invalid ID line format [ line: 1]"),
        WebinCliException.ErrorType.VALIDATION_ERROR);
  }

  @Test
  public void testTranscriptomeGeneratedXmlAlias() throws Throwable {
    String name = "test-name";

    WebinCli webinCli =
        webinCliForValidate(
            TRANSCRIPTOME_RESOURCE_DIR,
            WebinCliBuilder.createForTranscriptome(),
            m -> transcriptomeMetaManifest(m, name),
            m -> m.file("FASTA", "valid.fasta.gz"),
            c -> {});

    lookupAndAssertGeneratedAlias(webinCli, name, "analysis", "webin-transcriptome-" + name);
  }

  @Test
  public void testTranscriptomeMultiSubmission() throws Throwable {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    manifestBuilder.jsonFormat();

    // First submission
    transcriptomeMetaManifest(manifestBuilder);
    manifestBuilder.file("FASTA", "valid.fasta.gz");

    // Second submission
    manifestBuilder.fieldGroup();
    transcriptomeMetaManifest(manifestBuilder);
    manifestBuilder.file("FASTA", "valid.fasta.gz");

    // Third submission
    manifestBuilder.fieldGroup();
    transcriptomeMetaManifest(manifestBuilder);
    manifestBuilder.file("FASTA", "valid.fasta.gz");

    WebinCliBuilder webinCliBuilder = WebinCliBuilder.createForTranscriptome();
    webinCliBuilder.submit(true);

    executeAndAssertReceiptSuccess(
        webinCliBuilder.build(TRANSCRIPTOME_RESOURCE_DIR, manifestBuilder), 3);
  }

  // PolySample

  @Test
  public void testPolySampleFull() throws Throwable {
    testPolysample(
        this::polySampleMetaManifest,
        m -> {
          m.file("FASTA", "valid/valid.fasta.gz");
          m.file("SAMPLE_TSV", "valid/ERT000061-polysample-sample_tsv.tsv.gz");
          m.file("TAX_TSV", "valid/ERT000061-polysample-tax_tsv.tsv.gz");
        });
  }

  @Test
  public void testPolySampleTaxTsv() throws Throwable {
    testPolysample(
        this::polySampleMetaManifest,
        m -> {
          m.file("TAX_TSV", "valid/ERT000061-polysample-tax_tsv.tsv.gz");
        });
  }

  @Test
  public void testPolySampleFastaAndSampleTsv() throws Throwable {
    testPolysample(
        this::polySampleMetaManifest,
        m -> {
          m.file("FASTA", "valid/valid.fasta.gz");
          m.file("SAMPLE_TSV", "valid/ERT000061-polysample-sample_tsv.tsv.gz");
        });
  }

  @Test
  public void testPolySampleInvalidFileGroup() throws Throwable {
    testPolySampleError(
        m -> polySampleMetaManifest(m),
        m -> m.file("FASTA", "valid/valid.fasta.gz"),
        r ->
            r.textInManifestReport(
                "An invalid set of files has been specified. Expected data files are: [1 FASTA, 1 SAMPLE_TSV, 1 TAX_TSV] or [1 FASTA, 1 SAMPLE_TSV] or [1 TAX_TSV]"),
        WebinCliException.ErrorType.USER_ERROR);
  }

  //

  @Test
  public void testMultiSubmissionDuplicateName() throws Throwable {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    manifestBuilder.jsonFormat();

    // First submission
    readsMetaManifest(manifestBuilder, "TEST-1");
    manifestBuilder.file("FASTQ", "10x/4fastq/R1.fastq.gz");

    // Second submission
    manifestBuilder.fieldGroup();
    readsMetaManifest(manifestBuilder, "TEST-1");
    manifestBuilder.file("FASTQ", "10x/4fastq/R2.fastq.gz");

    // Third submission
    manifestBuilder.fieldGroup();
    readsMetaManifest(manifestBuilder, "TEST-3");
    manifestBuilder.file("FASTQ", "10x/4fastq/R3.fastq.gz");

    WebinCliBuilder webinCliBuilder = WebinCliBuilder.createForReads();
    webinCliBuilder.submit(true);

    WebinCli webinCli = webinCliBuilder.build(READS_RESOURCE_DIR, manifestBuilder);

    expectError(() -> webinCli.execute(), WebinCliException.ErrorType.USER_ERROR);
    new ReportTester(webinCli)
        .textInManifestReport(
            "ERROR: Invalid name: TEST-1. The name field must be unique within the manifest file.");
  }

  @Test
  public void testMultiSubmissionFileSystemSafeNameConflict() throws Throwable {
    ManifestBuilder manifestBuilder = new ManifestBuilder();
    manifestBuilder.jsonFormat();

    // First submission
    readsMetaManifest(manifestBuilder, "TEST:1");
    manifestBuilder.file("FASTQ", "10x/4fastq/R1.fastq.gz");

    // Second submission
    manifestBuilder.fieldGroup();
    readsMetaManifest(manifestBuilder, "TEST;1");
    manifestBuilder.file("FASTQ", "10x/4fastq/R2.fastq.gz");

    WebinCliBuilder webinCliBuilder = WebinCliBuilder.createForReads();
    webinCliBuilder.submit(false);

    WebinCli webinCli = webinCliBuilder.build(READS_RESOURCE_DIR, manifestBuilder);

    expectError(() -> webinCli.execute(), WebinCliException.ErrorType.USER_ERROR);
    new ReportTester(webinCli)
        .textInWebinCliReport(
            "A manifest name TEST;1 conflicts with another manifest name TEST:1 after adjusting the names when creating submission directories");
  }
}
