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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.ebi.ena.webin.cli.WebinCli.getSafeOutputDir;
import static uk.ac.ebi.ena.webin.cli.WebinCli.getSafeOutputDirs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.Test;

public class WebinCliTest {

  @Test
  public void testGetSafeOutputDir() {
    assertThat("A_aZ").isEqualTo(getSafeOutputDir("A aZ"));
    assertThat("A_a_Z").isEqualTo(getSafeOutputDir("A a Z"));
    assertThat("A_a_Z").isEqualTo(getSafeOutputDir("A  a   Z"));
    assertThat("AaZ").isEqualTo(getSafeOutputDir("AaZ"));
    assertThat("A_AA").isEqualTo(getSafeOutputDir("A&AA"));
    assertThat("A.AA").isEqualTo(getSafeOutputDir("A.AA"));
    assertThat("A-AA").isEqualTo(getSafeOutputDir("A-AA"));
    assertThat("A_AA").isEqualTo(getSafeOutputDir("A_____AA"));
    assertThat("AA").isEqualTo(getSafeOutputDir("_____AA"));
    assertThat("AA").isEqualTo(getSafeOutputDir("AA_____"));
    assertThat("_").isEqualTo(getSafeOutputDir("_______"));
    assertThat(".").isEqualTo(getSafeOutputDir("."));
  }

  @Test
  public void testGetSafeOutputDirs() {
    assertThat(".").isEqualTo(getSafeOutputDirs(".", "E_vermicularis_upd")[0]);
    assertThat("E_vermicularis_upd").isEqualTo(getSafeOutputDirs(".", "E_vermicularis_upd")[1]);
    assertThat("AaZ").isEqualTo(getSafeOutputDirs("AaZ", "AaZ")[0]);
    assertThat("A.AA").isEqualTo(getSafeOutputDirs("AaZ", "A.AA")[1]);
  }

  @Test
  public void testInputDirIsAFileError() {
    WebinCliCommand cmd = new WebinCliCommand();
    cmd.userName = WebinCliTestUtils.getTestWebinUsername();
    cmd.password = WebinCliTestUtils.getTestWebinPassword();
    cmd.inputDir = TempFileBuilder.file("test").toFile();
    cmd.outputDir = WebinCliTestUtils.createTempDir();

    assertThatThrownBy(() -> new WebinCli(cmd))
        .isInstanceOf(WebinCliException.class)
        .hasMessage(WebinCliMessage.CLI_INPUT_PATH_NOT_DIR.format(cmd.inputDir.getAbsoluteFile()));
  }

  @Test
  public void testOutputDirIsAFileError() {
    WebinCliCommand cmd = new WebinCliCommand();
    cmd.userName = WebinCliTestUtils.getTestWebinUsername();
    cmd.password = WebinCliTestUtils.getTestWebinPassword();
    cmd.inputDir = WebinCliTestUtils.createTempDir();
    cmd.outputDir = TempFileBuilder.file("test").toFile();

    assertThatThrownBy(() -> new WebinCli(cmd))
        .isInstanceOf(WebinCliException.class)
        .hasMessage(
            WebinCliMessage.CLI_OUTPUT_PATH_NOT_DIR.format(cmd.outputDir.getAbsoluteFile()));
  }

  @Test
  public void testInputDirMissingError() {
    WebinCliCommand cmd = new WebinCliCommand();
    cmd.userName = WebinCliTestUtils.getTestWebinUsername();
    cmd.password = WebinCliTestUtils.getTestWebinPassword();
    cmd.inputDir = new File(UUID.randomUUID().toString());
    cmd.outputDir = WebinCliTestUtils.createTempDir();

    assertThatThrownBy(() -> new WebinCli(cmd))
        .isInstanceOf(WebinCliException.class)
        .hasMessage(WebinCliMessage.CLI_INPUT_PATH_NOT_DIR.format(cmd.inputDir.getName()));
  }

  @Test
  public void testOutputDirMissingError() throws IOException {
    WebinCliCommand cmd = new WebinCliCommand();
    cmd.userName = WebinCliTestUtils.getTestWebinUsername();
    cmd.password = WebinCliTestUtils.getTestWebinPassword();
    cmd.inputDir = WebinCliTestUtils.createTempDir();
    cmd.outputDir = Files.createTempFile("file", ".temp").toFile();

    assertThatThrownBy(() -> new WebinCli(cmd))
        .isInstanceOf(WebinCliException.class)
        .hasMessage(WebinCliMessage.CLI_OUTPUT_PATH_NOT_DIR.format(cmd.outputDir.toString()));
  }

  @Test
  public void testPrintManifestHelpGenome() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    WebinCli.printManifestHelp(WebinCliContext.genome, new PrintStream(os));
    System.out.println(os.toString());
    assertThat(os.toString().replaceAll("\r", ""))
        .isEqualTo(
            "\nManifest fields for 'genome' context:\n"
                + "\n"
                + "┌────────────────────┬───────────┬─────────────────────────────────────────────┐\n"
                + "│Field               │Cardinality│Description                                  │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│NAME (ASSEMBLYNAME) │Mandatory  │Unique genome assembly name                  │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│STUDY               │Mandatory  │Study accession or name                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SAMPLE              │Mandatory  │Sample accession or name                     │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│ASSEMBLY_TYPE       │Mandatory  │Assembly type:                               │\n"
                + "│                    │           │* clone or isolate                           │\n"
                + "│                    │           │* primary metagenome                         │\n"
                + "│                    │           │* binned metagenome                          │\n"
                + "│                    │           │* Metagenome-Assembled Genome (MAG)          │\n"
                + "│                    │           │* Environmental Single-Cell Amplified Genome │\n"
                + "│                    │           │(SAG)                                        │\n"
                + "│                    │           │* COVID-19 outbreak                          │\n"
                + "│                    │           │* clinical isolate assembly                  │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│COVERAGE            │Mandatory  │Sequencing coverage                          │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│PROGRAM             │Mandatory  │Assembly program                             │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│PLATFORM            │Mandatory  │Sequencing platform                          │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│DESCRIPTION         │Optional   │Genome assembly description                  │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│MINGAPLENGTH        │Optional   │Minimum gap length                           │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│MOLECULETYPE        │Optional   │Molecule type:                               │\n"
                + "│                    │           │* genomic DNA                                │\n"
                + "│                    │           │* genomic RNA                                │\n"
                + "│                    │           │* viral cRNA                                 │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│RUN_REF             │Optional   │Run accession or name as a comma-separated   │\n"
                + "│                    │           │list                                         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│ANALYSIS_REF        │Optional   │Analysis accession or name as a              │\n"
                + "│                    │           │comma-separated list                         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│TPA                 │Optional   │Third party annotation:                      │\n"
                + "│                    │           │* yes                                        │\n"
                + "│                    │           │* no                                         │\n"
                + "│                    │           │* true                                       │\n"
                + "│                    │           │* false                                      │\n"
                + "│                    │           │* Y                                          │\n"
                + "│                    │           │* N                                          │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│AUTHORS             │Optional   │For submission brokers only. Submitter's     │\n"
                + "│                    │           │names as a comma-separated list              │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│ADDRESS             │Optional   │For submission brokers only. Submitter's     │\n"
                + "│                    │           │address                                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SUBMISSION_TOOL     │Optional   │Name of third-party or developed tool used to│\n"
                + "│                    │           │submit to ENA                                │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SUBMISSION_TOOL_VERS│Optional   │Version number of the third-party or         │\n"
                + "│ION                 │           │developed tool used to submit to ENA         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│FASTA               │0-1 files  │Fasta file                                   │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│FLATFILE            │0-1 files  │Flat file                                    │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│CHROMOSOME_LIST     │0-1 files  │Chromosome list file                         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│UNLOCALISED_LIST    │0-1 files  │Unlocalised sequence list file               │\n"
                + "└────────────────────┴───────────┴─────────────────────────────────────────────┘\n"
                + "\n"
                + "Data files for 'genome' context:\n"
                + "\n"
                + "┌──────────────────────────────┬───────────┬───────────┬───────────┬───────────┐\n"
                + "│Data files                    │FASTA      │FLATFILE   │CHROMOSOME_│UNLOCALISED│\n"
                + "│                              │           │           │LIST       │_LIST      │\n"
                + "├──────────────────────────────┼───────────┼───────────┼───────────┼───────────┤\n"
                + "│Sequences in an annotated flat│           │1          │           │           │\n"
                + "│file.                         │           │           │           │           │\n"
                + "├──────────────────────────────┼───────────┼───────────┼───────────┼───────────┤\n"
                + "│Sequences in a fasta file. No │1          │0-1        │           │           │\n"
                + "│chromosomes. An optional      │           │           │           │           │\n"
                + "│annotated flat file.          │           │           │           │           │\n"
                + "├──────────────────────────────┼───────────┼───────────┼───────────┼───────────┤\n"
                + "│Sequences in an annotated flat│           │1          │1          │0-1        │\n"
                + "│file. A list of chromosomes.  │           │           │           │           │\n"
                + "│An optional list of           │           │           │           │           │\n"
                + "│unlocalised sequences.        │           │           │           │           │\n"
                + "├──────────────────────────────┼───────────┼───────────┼───────────┼───────────┤\n"
                + "│Sequences in a fasta file. A  │1          │0-1        │1          │0-1        │\n"
                + "│list of chromosomes. An       │           │           │           │           │\n"
                + "│optional optional annotated   │           │           │           │           │\n"
                + "│flat file and an optional list│           │           │           │           │\n"
                + "│of unlocalised sequences.     │           │           │           │           │\n"
                + "└──────────────────────────────┴───────────┴───────────┴───────────┴───────────┘\n");
  }

  @Test
  public void testPrintManifestHelpTranscriptome() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    WebinCli.printManifestHelp(WebinCliContext.transcriptome, new PrintStream(os));
    System.out.println(os.toString());
    assertThat(os.toString().replaceAll("\r", ""))
        .isEqualTo(
            "\n"
                + "Manifest fields for 'transcriptome' context:\n"
                + "\n"
                + "┌────────────────────┬───────────┬─────────────────────────────────────────────┐\n"
                + "│Field               │Cardinality│Description                                  │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│NAME (ASSEMBLYNAME) │Mandatory  │Unique transcriptome assembly name           │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│STUDY               │Mandatory  │Study accession or name                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SAMPLE              │Mandatory  │Sample accession or name                     │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│PROGRAM             │Mandatory  │Assembly program                             │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│PLATFORM            │Mandatory  │Sequencing platform                          │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│ASSEMBLY_TYPE       │Mandatory  │Assembly type:                               │\n"
                + "│                    │           │* isolate                                    │\n"
                + "│                    │           │* metatranscriptome                          │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│RUN_REF             │Optional   │Run accession or name as a comma-separated   │\n"
                + "│                    │           │list                                         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│ANALYSIS_REF        │Optional   │Analysis accession or name as a              │\n"
                + "│                    │           │comma-separated list                         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│DESCRIPTION         │Optional   │Transcriptome assembly description           │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│TPA                 │Optional   │Third party annotation:                      │\n"
                + "│                    │           │* yes                                        │\n"
                + "│                    │           │* no                                         │\n"
                + "│                    │           │* true                                       │\n"
                + "│                    │           │* false                                      │\n"
                + "│                    │           │* Y                                          │\n"
                + "│                    │           │* N                                          │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│AUTHORS             │Optional   │For submission brokers only. Submitter's     │\n"
                + "│                    │           │names as a comma-separated list              │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│ADDRESS             │Optional   │For submission brokers only. Submitter's     │\n"
                + "│                    │           │address                                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SUBMISSION_TOOL     │Optional   │Name of third-party or developed tool used to│\n"
                + "│                    │           │submit to ENA                                │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SUBMISSION_TOOL_VERS│Optional   │Version number of the third-party or         │\n"
                + "│ION                 │           │developed tool used to submit to ENA         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│FASTA               │0-1 files  │Fasta file                                   │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│FLATFILE            │0-1 files  │Flat file                                    │\n"
                + "└────────────────────┴───────────┴─────────────────────────────────────────────┘\n"
                + "\n"
                + "Data files for 'transcriptome' context:\n"
                + "\n"
                + "┌──────────────────────────────┬───────────────────────┬───────────────────────┐\n"
                + "│Data files                    │FASTA                  │FLATFILE               │\n"
                + "├──────────────────────────────┼───────────────────────┼───────────────────────┤\n"
                + "│Sequences in a fasta file.    │1                      │                       │\n"
                + "├──────────────────────────────┼───────────────────────┼───────────────────────┤\n"
                + "│Sequences in an annotated flat│                       │1                      │\n"
                + "│file.                         │                       │                       │\n"
                + "└──────────────────────────────┴───────────────────────┴───────────────────────┘\n");
  }

  @Test
  public void testPrintManifestHelpSequence() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    WebinCli.printManifestHelp(WebinCliContext.sequence, new PrintStream(os));
    System.out.println(os.toString());
    assertThat(os.toString().replaceAll("\r", ""))
        .isEqualTo(
            "\n"
                + "Manifest fields for 'sequence' context:\n"
                + "\n"
                + "┌────────────────────┬───────────┬─────────────────────────────────────────────┐\n"
                + "│Field               │Cardinality│Description                                  │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│NAME                │Mandatory  │Unique sequence submission name              │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│STUDY               │Mandatory  │Study accession or name                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│RUN_REF             │Optional   │Run accession or name as a comma-separated   │\n"
                + "│                    │           │list                                         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│ANALYSIS_REF        │Optional   │Analysis accession or name as a              │\n"
                + "│                    │           │comma-separated list                         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│DESCRIPTION         │Optional   │Sequence submission description              │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│AUTHORS             │Optional   │For submission brokers only. Submitter's     │\n"
                + "│                    │           │names as a comma-separated list              │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│ADDRESS             │Optional   │For submission brokers only. Submitter's     │\n"
                + "│                    │           │address                                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SUBMISSION_TOOL     │Optional   │Name of third-party or developed tool used to│\n"
                + "│                    │           │submit to ENA                                │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SUBMISSION_TOOL_VERS│Optional   │Version number of the third-party or         │\n"
                + "│ION                 │           │developed tool used to submit to ENA         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│TAB                 │0-1 files  │Tabulated file                               │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│FLATFILE            │0-1 files  │Flat file                                    │\n"
                + "└────────────────────┴───────────┴─────────────────────────────────────────────┘\n"
                + "\n"
                + "Data files for 'sequence' context:\n"
                + "\n"
                + "┌──────────────────────────────┬───────────────────────┬───────────────────────┐\n"
                + "│Data files                    │TAB                    │FLATFILE               │\n"
                + "├──────────────────────────────┼───────────────────────┼───────────────────────┤\n"
                + "│Annotated sequences in a comma│1                      │                       │\n"
                + "│separated file.               │                       │                       │\n"
                + "├──────────────────────────────┼───────────────────────┼───────────────────────┤\n"
                + "│Annotated sequences in a flat │                       │1                      │\n"
                + "│file.                         │                       │                       │\n"
                + "└──────────────────────────────┴───────────────────────┴───────────────────────┘\n");
  }

  @Test
  public void testPrintManifestHelpReads() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    WebinCli.printManifestHelp(WebinCliContext.reads, new PrintStream(os));
    System.out.println(os.toString());
    assertThat(os.toString().replaceAll("\r", ""))
        .isEqualTo(
            "\n"
                + "Manifest fields for 'reads' context:\n"
                + "\n"
                + "┌────────────────────┬───────────┬─────────────────────────────────────────────┐\n"
                + "│Field               │Cardinality│Description                                  │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│NAME                │Mandatory  │Unique sequencing experiment name            │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│STUDY               │Mandatory  │Study accession or name                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SAMPLE              │Mandatory  │Sample accession or name                     │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│INSTRUMENT          │Mandatory  │Sequencing instrument:                       │\n"
                + "│                    │           │* HiSeq X Five                               │\n"
                + "│                    │           │* HiSeq X Ten                                │\n"
                + "│                    │           │* Illumina Genome Analyzer                   │\n"
                + "│                    │           │* Illumina Genome Analyzer II                │\n"
                + "│                    │           │* Illumina Genome Analyzer IIx               │\n"
                + "│                    │           │* Illumina HiScanSQ                          │\n"
                + "│                    │           │* Illumina HiSeq 1000                        │\n"
                + "│                    │           │* Illumina HiSeq 1500                        │\n"
                + "│                    │           │* Illumina HiSeq 2000                        │\n"
                + "│                    │           │* Illumina HiSeq 2500                        │\n"
                + "│                    │           │* Illumina HiSeq 3000                        │\n"
                + "│                    │           │* Illumina HiSeq 4000                        │\n"
                + "│                    │           │* Illumina HiSeq X                           │\n"
                + "│                    │           │* Illumina iSeq 100                          │\n"
                + "│                    │           │* Illumina MiSeq                             │\n"
                + "│                    │           │* Illumina MiniSeq                           │\n"
                + "│                    │           │* Illumina NovaSeq 6000                      │\n"
                + "│                    │           │* Illumina NovaSeq X                         │\n"
                + "│                    │           │* Illumina NovaSeq X Plus                    │\n"
                + "│                    │           │* NextSeq 500                                │\n"
                + "│                    │           │* NextSeq 550                                │\n"
                + "│                    │           │* NextSeq 1000                               │\n"
                + "│                    │           │* NextSeq 2000                               │\n"
                + "│                    │           │* MinION                                     │\n"
                + "│                    │           │* GridION                                    │\n"
                + "│                    │           │* PromethION                                 │\n"
                + "│                    │           │* Onso                                       │\n"
                + "│                    │           │* PacBio RS                                  │\n"
                + "│                    │           │* PacBio RS II                               │\n"
                + "│                    │           │* Revio                                      │\n"
                + "│                    │           │* Sequel                                     │\n"
                + "│                    │           │* Sequel II                                  │\n"
                + "│                    │           │* Sequel IIe                                 │\n"
                + "│                    │           │* BGISEQ-50                                  │\n"
                + "│                    │           │* BGISEQ-500                                 │\n"
                + "│                    │           │* MGISEQ-2000RS                              │\n"
                + "│                    │           │* 454 GS                                     │\n"
                + "│                    │           │* 454 GS 20                                  │\n"
                + "│                    │           │* 454 GS FLX                                 │\n"
                + "│                    │           │* 454 GS FLX+                                │\n"
                + "│                    │           │* 454 GS FLX Titanium                        │\n"
                + "│                    │           │* 454 GS Junior                              │\n"
                + "│                    │           │* Ion Torrent Genexus                        │\n"
                + "│                    │           │* Ion Torrent PGM                            │\n"
                + "│                    │           │* Ion Torrent Proton                         │\n"
                + "│                    │           │* Ion Torrent S5                             │\n"
                + "│                    │           │* Ion Torrent S5 XL                          │\n"
                + "│                    │           │* Ion GeneStudio S5                          │\n"
                + "│                    │           │* Ion GeneStudio S5 Plus                     │\n"
                + "│                    │           │* Ion GeneStudio S5 Prime                    │\n"
                + "│                    │           │* AB 3730xL Genetic Analyzer                 │\n"
                + "│                    │           │* AB 3730 Genetic Analyzer                   │\n"
                + "│                    │           │* AB 3500xL Genetic Analyzer                 │\n"
                + "│                    │           │* AB 3500 Genetic Analyzer                   │\n"
                + "│                    │           │* AB 3130xL Genetic Analyzer                 │\n"
                + "│                    │           │* AB 3130 Genetic Analyzer                   │\n"
                + "│                    │           │* AB 310 Genetic Analyzer                    │\n"
                + "│                    │           │* DNBSEQ-T7                                  │\n"
                + "│                    │           │* DNBSEQ-G400                                │\n"
                + "│                    │           │* DNBSEQ-G50                                 │\n"
                + "│                    │           │* DNBSEQ-G400 FAST                           │\n"
                + "│                    │           │* Element AVITI                              │\n"
                + "│                    │           │* UG 100                                     │\n"
                + "│                    │           │* Sentosa SQ301                              │\n"
                + "│                    │           │* GENIUS                                     │\n"
                + "│                    │           │* Genapsys Sequencer                         │\n"
                + "│                    │           │* GS111                                      │\n"
                + "│                    │           │* GenoCare 1600                              │\n"
                + "│                    │           │* GenoLab M                                  │\n"
                + "│                    │           │* FASTASeq 300                               │\n"
                + "│                    │           │* Tapestri                                   │\n"
                + "│                    │           │* unspecified                                │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│LIBRARY_SOURCE      │Mandatory  │Source material:                             │\n"
                + "│                    │           │* GENOMIC                                    │\n"
                + "│                    │           │* GENOMIC SINGLE CELL                        │\n"
                + "│                    │           │* TRANSCRIPTOMIC                             │\n"
                + "│                    │           │* TRANSCRIPTOMIC SINGLE CELL                 │\n"
                + "│                    │           │* METAGENOMIC                                │\n"
                + "│                    │           │* METATRANSCRIPTOMIC                         │\n"
                + "│                    │           │* SYNTHETIC                                  │\n"
                + "│                    │           │* VIRAL RNA                                  │\n"
                + "│                    │           │* OTHER                                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│LIBRARY_SELECTION   │Mandatory  │Method used to select or enrich the source   │\n"
                + "│                    │           │material:                                    │\n"
                + "│                    │           │* RANDOM                                     │\n"
                + "│                    │           │* PCR                                        │\n"
                + "│                    │           │* RANDOM PCR                                 │\n"
                + "│                    │           │* RT-PCR                                     │\n"
                + "│                    │           │* HMPR                                       │\n"
                + "│                    │           │* MF                                         │\n"
                + "│                    │           │* repeat fractionation                       │\n"
                + "│                    │           │* size fractionation                         │\n"
                + "│                    │           │* MSLL                                       │\n"
                + "│                    │           │* cDNA                                       │\n"
                + "│                    │           │* cDNA_randomPriming                         │\n"
                + "│                    │           │* cDNA_oligo_dT                              │\n"
                + "│                    │           │* PolyA                                      │\n"
                + "│                    │           │* Oligo-dT                                   │\n"
                + "│                    │           │* Inverse rRNA                               │\n"
                + "│                    │           │* Inverse rRNA selection                     │\n"
                + "│                    │           │* ChIP                                       │\n"
                + "│                    │           │* ChIP-Seq                                   │\n"
                + "│                    │           │* MNase                                      │\n"
                + "│                    │           │* DNase                                      │\n"
                + "│                    │           │* Hybrid Selection                           │\n"
                + "│                    │           │* Reduced Representation                     │\n"
                + "│                    │           │* Restriction Digest                         │\n"
                + "│                    │           │* 5-methylcytidine antibody                  │\n"
                + "│                    │           │* MBD2 protein methyl-CpG binding domain     │\n"
                + "│                    │           │* CAGE                                       │\n"
                + "│                    │           │* RACE                                       │\n"
                + "│                    │           │* MDA                                        │\n"
                + "│                    │           │* padlock probes capture method              │\n"
                + "│                    │           │* other                                      │\n"
                + "│                    │           │* unspecified                                │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│LIBRARY_STRATEGY    │Mandatory  │Sequencing technique:                        │\n"
                + "│                    │           │* WGS                                        │\n"
                + "│                    │           │* WGA                                        │\n"
                + "│                    │           │* WXS                                        │\n"
                + "│                    │           │* RNA-Seq                                    │\n"
                + "│                    │           │* ssRNA-seq                                  │\n"
                + "│                    │           │* snRNA-seq                                  │\n"
                + "│                    │           │* miRNA-Seq                                  │\n"
                + "│                    │           │* ncRNA-Seq                                  │\n"
                + "│                    │           │* FL-cDNA                                    │\n"
                + "│                    │           │* EST                                        │\n"
                + "│                    │           │* Hi-C                                       │\n"
                + "│                    │           │* ATAC-seq                                   │\n"
                + "│                    │           │* WCS                                        │\n"
                + "│                    │           │* RAD-Seq                                    │\n"
                + "│                    │           │* CLONE                                      │\n"
                + "│                    │           │* POOLCLONE                                  │\n"
                + "│                    │           │* AMPLICON                                   │\n"
                + "│                    │           │* CLONEEND                                   │\n"
                + "│                    │           │* FINISHING                                  │\n"
                + "│                    │           │* ChIP-Seq                                   │\n"
                + "│                    │           │* MNase-Seq                                  │\n"
                + "│                    │           │* DNase-Hypersensitivity                     │\n"
                + "│                    │           │* Bisulfite-Seq                              │\n"
                + "│                    │           │* CTS                                        │\n"
                + "│                    │           │* MRE-Seq                                    │\n"
                + "│                    │           │* MeDIP-Seq                                  │\n"
                + "│                    │           │* MBD-Seq                                    │\n"
                + "│                    │           │* Tn-Seq                                     │\n"
                + "│                    │           │* VALIDATION                                 │\n"
                + "│                    │           │* FAIRE-seq                                  │\n"
                + "│                    │           │* SELEX                                      │\n"
                + "│                    │           │* RIP-Seq                                    │\n"
                + "│                    │           │* ChIA-PET                                   │\n"
                + "│                    │           │* Synthetic-Long-Read                        │\n"
                + "│                    │           │* Targeted-Capture                           │\n"
                + "│                    │           │* Tethered Chromatin Conformation Capture    │\n"
                + "│                    │           │* NOMe-seq                                   │\n"
                + "│                    │           │* ChM-Seq                                    │\n"
                + "│                    │           │* GBS                                        │\n"
                + "│                    │           │* Ribo-seq                                   │\n"
                + "│                    │           │* OTHER                                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│DESCRIPTION         │Optional   │Experiment description                       │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│PLATFORM            │Optional   │Sequencing platform:                         │\n"
                + "│                    │           │* ILLUMINA                                   │\n"
                + "│                    │           │* PACBIO_SMRT                                │\n"
                + "│                    │           │* OXFORD_NANOPORE                            │\n"
                + "│                    │           │* BGISEQ                                     │\n"
                + "│                    │           │* LS454                                      │\n"
                + "│                    │           │* ION_TORRENT                                │\n"
                + "│                    │           │* CAPILLARY                                  │\n"
                + "│                    │           │* DNBSEQ                                     │\n"
                + "│                    │           │* ELEMENT                                    │\n"
                + "│                    │           │* ULTIMA                                     │\n"
                + "│                    │           │* VELA_DIAGNOSTICS                           │\n"
                + "│                    │           │* GENAPSYS                                   │\n"
                + "│                    │           │* GENEMIND                                   │\n"
                + "│                    │           │* TAPESTRI                                   │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│LIBRARY_CONSTRUCTION│Optional   │Protocol used to construct the sequencing    │\n"
                + "│_PROTOCOL           │           │library                                      │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│LIBRARY_NAME        │Optional   │Library name                                 │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│INSERT_SIZE         │Optional   │Insert size for paired reads                 │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SUBMISSION_TOOL     │Optional   │Name of third-party or developed tool used to│\n"
                + "│                    │           │submit to ENA                                │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│SUBMISSION_TOOL_VERS│Optional   │Version number of the third-party or         │\n"
                + "│ION                 │           │developed tool used to submit to ENA         │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│FASTQ               │0-10 files │Fastq file                                   │\n"
                + "│                    │           │READ_TYPE attribute:                         │\n"
                + "│                    │           │* single                                     │\n"
                + "│                    │           │* paired                                     │\n"
                + "│                    │           │* cell_barcode                               │\n"
                + "│                    │           │* umi_barcode                                │\n"
                + "│                    │           │* feature_barcode                            │\n"
                + "│                    │           │* sample_barcode                             │\n"
                + "│                    │           │* spatial_barcode                            │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│BAM                 │0-1 files  │BAM file                                     │\n"
                + "├────────────────────┼───────────┼─────────────────────────────────────────────┤\n"
                + "│CRAM                │0-1 files  │CRAM file                                    │\n"
                + "└────────────────────┴───────────┴─────────────────────────────────────────────┘\n"
                + "\n"
                + "Data files for 'reads' context:\n"
                + "\n"
                + "┌──────────────────────────────┬───────────────┬───────────────┬───────────────┐\n"
                + "│Data files                    │FASTQ          │BAM            │CRAM           │\n"
                + "├──────────────────────────────┼───────────────┼───────────────┼───────────────┤\n"
                + "│Single or paired sequence     │1-10           │               │               │\n"
                + "│reads in multiple fastq files.│               │               │               │\n"
                + "├──────────────────────────────┼───────────────┼───────────────┼───────────────┤\n"
                + "│Sequence reads in a CRAM file.│               │               │1              │\n"
                + "├──────────────────────────────┼───────────────┼───────────────┼───────────────┤\n"
                + "│Sequence reads in a BAM file. │               │1              │               │\n"
                + "└──────────────────────────────┴───────────────┴───────────────┴───────────────┘\n");
  }
}
