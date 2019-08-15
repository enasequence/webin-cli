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
package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class WebinCliSubmissionTest {
  private final WebinCliBuilder reads =
      new WebinCliBuilder(WebinCliContext.reads).validate(true).submit(true).ascp(false);
  private final WebinCliBuilder genome =
      new WebinCliBuilder(WebinCliContext.genome).validate(true).submit(true).ascp(false);
  private final WebinCliBuilder transcriptome =
      new WebinCliBuilder(WebinCliContext.transcriptome).validate(true).submit(true).ascp(false);
  private final WebinCliBuilder sequence =
      new WebinCliBuilder(WebinCliContext.sequence).validate(true).submit(true).ascp(false);

  private ManifestBuilder readsManifest() {
    return new ManifestBuilder()
        .field("NAME", WebinCliTestUtils.createName())
        .field("STUDY", "SRP052303")
        .field("SAMPLE", "ERS2554688")
        .field("PLATFORM", "ILLUMINA")
        .field("INSTRUMENT", "unspecifieD")
        .field("INSERT_SIZE", "1")
        .field("LIBRARY_NAME", "YOBA LIB")
        .field("LIBRARY_STRATEGY", "CLONEEND")
        .field("LIBRARY_SOURCE", "OTHER")
        .field("LIBRARY_SELECTION", "Inverse rRNA selection")
        .field("DESCRIPTION", "Some reads description");
  }

  private ManifestBuilder genomeManifest() {
    return genomeManifest(WebinCliTestUtils.createName());
  }

  private ManifestBuilder genomeManifest(String name) {
    return new ManifestBuilder()
        .field("ASSEMBLYNAME", name)
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

  private ManifestBuilder transcriptomeManifest() {
    return transcriptomeManifest(WebinCliTestUtils.createName());
  }

  private ManifestBuilder transcriptomeManifest(String name) {
    return new ManifestBuilder()
        .field("ASSEMBLYNAME", name)
        .field("PROGRAM", "assembly")
        .field("PLATFORM", "fghgf")
        .field("SAMPLE", "SAMN04526268")
        .field("STUDY", "PRJEB20083")
        .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
        .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
        .field("DESCRIPTION", "Some transcriptome assembly description");
  }

  private ManifestBuilder sequenceManifest() {
    return sequenceManifest(WebinCliTestUtils.createName());
  }

  private ManifestBuilder sequenceManifest(String name) {
    return new ManifestBuilder()
        .field("NAME", name)
        .field("STUDY", "PRJEB20083")
        .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
        .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
        .field("DESCRIPTION", "Some sequence assembly description");
  }

  private static Path copy(String resource, Path inputDir) {
    return WebinCliTestUtils.createTempFileFromResource(resource, inputDir);
  }

  private static void assertExecuteThrowsUserError(
      WebinCliBuilder webinCliBuilder, Path inputDir, Path outputDir, ManifestBuilder manifest) {
    assertThatExceptionOfType(WebinCliException.class)
        .isThrownBy(() -> webinCliBuilder.execute(inputDir, outputDir, manifest))
        .withMessageContaining("Submission validation failed because of a user error");
  }

  private static void assertExecuteThrowsUserError(
      WebinCliBuilder webinCliBuilder,
      Path inputDir,
      Path outputDir,
      ManifestBuilder manifest,
      String message) {
    assertThatExceptionOfType(WebinCliException.class)
        .isThrownBy(() -> webinCliBuilder.execute(inputDir, outputDir, manifest))
        .withMessageContaining("Submission validation failed because of a user error")
        .withMessageContaining(message);
  }

  private static void assertReportContains(
      WebinCliContext context, String name, Path outputDir, Path dataFile, String message) {
    Path reportFile =
        outputDir
            .resolve(context.name())
            .resolve(name)
            .resolve("validate")
            .resolve(dataFile.getFileName().toString() + ".report");
    assertThat(WebinCliTestUtils.readFile(reportFile)).contains(message);
  }

  @Test
  public void testReadsSubmissionCramWithInfo() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path cramFile = copy("uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", inputDir);
    File infoFile = readsManifest().build(inputDir);

    ManifestBuilder manifest =
        new ManifestBuilder().file("CRAM", cramFile).field("INFO", infoFile.getAbsolutePath());
    reads.execute(inputDir, manifest);
  }

  @Test
  public void testReadsSubmissionCram() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path cramFile = copy("uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", inputDir);

    ManifestBuilder manifest = readsManifest().file("CRAM", cramFile);
    reads.execute(inputDir, manifest);
  }

  @Test
  public void testReadsSubmissionCramWithAscp() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path cramFile = copy("uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", inputDir);

    ManifestBuilder manifest = readsManifest().file("CRAM", cramFile);
    new WebinCliBuilder(WebinCliContext.reads)
        .validate(true)
        .submit(true)
        .ascp(true)
        .execute(inputDir, manifest);
  }

  @Test
  public void testGenomeSubmissionFlatFileAgpWithInfo() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path flatFile = copy("uk/ac/ebi/ena/webin/cli/assembly/valid_flatfile.dat.gz", inputDir);
    Path agpFile = copy("uk/ac/ebi/ena/webin/cli/assembly/valid_agp.agp.gz", inputDir);

    File infoFile = genomeManifest().build(inputDir);

    ManifestBuilder manifest =
        new ManifestBuilder()
            .file("FLATFILE", flatFile)
            .file("AGP", agpFile)
            .field("INFO", infoFile.getAbsolutePath());
    genome.execute(inputDir, manifest);
  }

  @Test
  public void testGenomeSubmissionFlatFileAgp() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path flatFile = copy("uk/ac/ebi/ena/webin/cli/assembly/valid_flatfile.dat.gz", inputDir);
    Path agpFile = copy("uk/ac/ebi/ena/webin/cli/assembly/valid_agp.agp.gz", inputDir);

    ManifestBuilder manifest = genomeManifest().file("FLATFILE", flatFile).file("AGP", agpFile);
    genome.execute(inputDir, manifest);
  }

  @Test
  public void testGenomeSubmissionFlatFileWithFormatError() {
    String name = WebinCliTestUtils.createName();
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path outputDir = WebinCliTestUtils.createTempDir().toPath();

    Path flatFile = copy("uk/ac/ebi/ena/webin/cli/assembly/invalid_flatfile.dat.gz", inputDir);
    ManifestBuilder manifest = genomeManifest(name).file("FLATFILE", flatFile);

    assertExecuteThrowsUserError(genome, inputDir, outputDir, manifest);
    assertReportContains(
        WebinCliContext.genome,
        name,
        outputDir,
        flatFile,
        "ERROR: Invalid ID line format [ line: 1]");
  }

  @Test
  public void testGenomeSubmissionFastaWithOneSequenceError() {
    String name = WebinCliTestUtils.createName();
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path outputDir = WebinCliTestUtils.createTempDir().toPath();
    Path fastaFile =
        WebinCliTestUtils.createTempFile(
            "test.fasta.gz", inputDir, true, ">A\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n");

    ManifestBuilder manifest = genomeManifest(name).file("FASTA", fastaFile);

    assertExecuteThrowsUserError(
        genome,
        inputDir,
        outputDir,
        manifest,
        "Invalid number of sequences : 1, Minimum number of sequences for CONTIG is: 2");

    // TODO: error is missing from report file
    // assertReportContains(WebinCliContext.genome, name, outputDir, fastaFile, "ERROR: Invalid
    // number of sequences : 1, Minimum number of sequences for CONTIG is: 2");
  }

  @Test
  public void testGenomeSubmissionFastaWithOneSequencePrimaryMetagenome() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path fastaFile =
        WebinCliTestUtils.createTempFile(
            "test.fasta.gz", inputDir, true, ">A\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n");

    ManifestBuilder manifest =
        genomeManifest().file("FASTA", fastaFile).field("ASSEMBLY_TYPE", "primary metagenome");
    genome.execute(inputDir, manifest);
  }

  @Test
  public void testSequenceSubmissionTabWithInfo() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path tabFile = copy("uk/ac/ebi/ena/webin/cli/template/valid/ERT000003-EST.tsv.gz", inputDir);
    File infoFile = sequenceManifest().build(inputDir);

    ManifestBuilder manifest =
        new ManifestBuilder().file("TAB", tabFile).field("INFO", infoFile.getAbsolutePath());
    sequence.execute(inputDir, manifest);
  }

  @Test
  public void testSequenceSubmissionTab() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path tabFile = copy("uk/ac/ebi/ena/webin/cli/template/valid/ERT000003-EST.tsv.gz", inputDir);

    ManifestBuilder manifest = sequenceManifest().file("TAB", tabFile);
    sequence.execute(inputDir, manifest);
  }

  @Test
  public void testSequenceSubmissionFlatFileWithFormatError() {
    String name = WebinCliTestUtils.createName();
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path outputDir = WebinCliTestUtils.createTempDir().toPath();

    Path flatFile = copy("uk/ac/ebi/ena/webin/cli/assembly/invalid_flatfile.dat.gz", inputDir);

    ManifestBuilder manifest = sequenceManifest(name).file("FLATFILE", flatFile);
    assertExecuteThrowsUserError(sequence, inputDir, outputDir, manifest);
    assertReportContains(
        WebinCliContext.sequence,
        name,
        outputDir,
        flatFile,
        "ERROR: Invalid ID line format [ line: 1]");
  }

  @Test
  public void testTranscriptomeSubmissionFastaWithInfo() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path fastaFile =
        copy("uk/ac/ebi/ena/webin/cli/transcriptome/valid/valid_fasta.fasta.gz", inputDir);
    File infoFile = transcriptomeManifest().build(inputDir);

    ManifestBuilder manifest =
        new ManifestBuilder().file("FASTA", fastaFile).field("INFO", infoFile.getAbsolutePath());
    transcriptome.execute(inputDir, manifest);
  }

  @Test
  public void testTranscriptomeSubmissionFasta() {
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path fastaFile =
        copy("uk/ac/ebi/ena/webin/cli/transcriptome/valid/valid_fasta.fasta.gz", inputDir);

    ManifestBuilder manifest = transcriptomeManifest().file("FASTA", fastaFile);
    transcriptome.execute(inputDir, manifest);
  }

  @Test
  public void testTranscriptomeSubmissionFlatFileWithFormatError() {
    String name = WebinCliTestUtils.createName();
    Path inputDir = WebinCliTestUtils.createTempDir().toPath();
    Path outputDir = WebinCliTestUtils.createTempDir().toPath();

    Path flatFile = copy("uk/ac/ebi/ena/webin/cli/assembly/invalid_flatfile.dat.gz", inputDir);
    ManifestBuilder manifest = transcriptomeManifest(name).file("FLATFILE", flatFile);

    assertExecuteThrowsUserError(transcriptome, inputDir, outputDir, manifest);
    assertReportContains(
        WebinCliContext.transcriptome,
        name,
        outputDir,
        flatFile,
        "ERROR: Invalid ID line format [ line: 1]");
  }
}
