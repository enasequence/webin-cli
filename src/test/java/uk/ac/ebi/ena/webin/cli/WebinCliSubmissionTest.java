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

import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.resourceDir;

import org.junit.Test;

public class WebinCliSubmissionTest {

    private static final File READS_RESOURCE_DIR = resourceDir("uk/ac/ebi/ena/webin/cli/reads/");
    private static final File GENOME_RESOURCE_DIR = resourceDir("uk/ac/ebi/ena/webin/cli/genome/");
    private static final File TRANSCRIPTOME_RESOURCE_DIR = resourceDir("uk/ac/ebi/ena/webin/cli/transcriptome/");
    private static final File SEQUENCE_RESOURCE_DIR = resourceDir("uk/ac/ebi/ena/webin/cli/sequence/");

    private ManifestBuilder readsManifest() {
        return new ManifestBuilder()
                .name()
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
        return new ManifestBuilder()
                .name()
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
        return new ManifestBuilder()
                .name()
                .field("PROGRAM", "assembly")
                .field("PLATFORM", "fghgf")
                .field("SAMPLE", "SAMN04526268")
                .field("STUDY", "PRJEB20083")
                .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
                .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
                .field("DESCRIPTION", "Some transcriptome assembly description");
    }

    private ManifestBuilder sequenceManifest() {
        return new ManifestBuilder()
                .name()
                .field("STUDY", "PRJEB20083")
                .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
                .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
                .field("DESCRIPTION", "Some sequence assembly description");
    }

    private static WebinCli assertWebinCliException(
            WebinCliBuilder webinCliBuilder, File inputDir, ManifestBuilder manifest) {
        return assertWebinCliException(webinCliBuilder, inputDir.toPath(), manifest);
    }

    private static WebinCli assertWebinCliException(
            WebinCliBuilder webinCliBuilder, Path inputDir, ManifestBuilder manifest) {
        return webinCliBuilder.executeThrows(inputDir, manifest, WebinCliException.class,
                "Submission validation failed because of a user error");
    }

    @Test
    public void testReadsSubmissionCram() {
        ManifestBuilder manifest = readsManifest()
                .file("CRAM", "18045_1#93.cram");
        WebinCliBuilder.READS.execute(READS_RESOURCE_DIR, manifest);
    }

    @Test
    public void testReadsSubmissionCramWithInfo() {
        File infoFile = readsManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("CRAM", "18045_1#93.cram")
                .field("INFO", infoFile.getAbsolutePath());
        WebinCliBuilder.READS.execute(READS_RESOURCE_DIR, manifest);
    }

    @Test
    public void testReadsSubmissionCramWithAscp() {
        ManifestBuilder manifest = readsManifest()
                .file("CRAM", "18045_1#93.cram");
        WebinCliBuilder.READS.ascp(true).execute(READS_RESOURCE_DIR, manifest);
    }

    @Test
    public void testGenomeSubmissionFlatFileAgp() {
        ManifestBuilder manifest = genomeManifest()
                .file("FLATFILE", "valid_flatfile.dat.gz")
                .file("AGP", "valid_agp.agp.gz");
        WebinCliBuilder.GENOME.execute(GENOME_RESOURCE_DIR, manifest);
    }

    @Test
    public void testGenomeSubmissionFlatFileAgpWithInfo() {
        File infoFile = genomeManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("FLATFILE", "valid_flatfile.dat.gz")
                .file("AGP", "valid_agp.agp.gz")
                .field("INFO", infoFile.getAbsolutePath());
        WebinCliBuilder.GENOME.execute(GENOME_RESOURCE_DIR, manifest);
    }

    @Test
    public void testGenomeSubmissionFlatFileWithFormatError() {
        ManifestBuilder manifest = genomeManifest()
                .file("FLATFILE", "invalid_flatfile.dat.gz");
        WebinCli cli = assertWebinCliException(WebinCliBuilder.GENOME, GENOME_RESOURCE_DIR, manifest);
        WebinCliTestUtils.assertReportContains(cli,
                "invalid_flatfile.dat.gz",
                "ERROR: Invalid ID line format [ line: 1]");
    }

    @Test
    public void testGenomeSubmissionFastaWithOneSequenceError() {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path fastaFile = WebinCliTestUtils.createTempFile(
                "test.fasta.gz", inputDir, true, ">A\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n");
        ManifestBuilder manifest = genomeManifest()
                .file("FASTA", fastaFile);
        WebinCli cli = assertWebinCliException(WebinCliBuilder.GENOME, inputDir, manifest);
        WebinCliTestUtils.assertReportContains(
                cli,
                "webin-cli",
                "Invalid number of sequences : 1, Minimum number of sequences for CONTIG is: 2");
    }

    @Test
    public void testGenomeSubmissionFastaWithOneSequencePrimaryMetagenome() {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path fastaFile = WebinCliTestUtils.createTempFile(
                "test.fasta.gz", inputDir, true, ">A\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n");
        ManifestBuilder manifest = genomeManifest()
                .file("FASTA", fastaFile)
                .field("ASSEMBLY_TYPE", "primary metagenome");
        WebinCliBuilder.GENOME.execute(inputDir, manifest);
    }


    @Test
    public void testSequenceSubmissionTab() {
        ManifestBuilder manifest = sequenceManifest()
                .file("TAB", "valid/ERT000003-EST.tsv.gz");
        WebinCliBuilder.SEQUENCE.execute(SEQUENCE_RESOURCE_DIR, manifest);
    }

    @Test
    public void testSequenceSubmissionTabWithInfo() {
        File infoFile = sequenceManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("TAB", "valid/ERT000003-EST.tsv.gz")
                .field("INFO", infoFile.getAbsolutePath());
        WebinCliBuilder.SEQUENCE.execute(SEQUENCE_RESOURCE_DIR, manifest);
    }

    @Test
    public void testSequenceSubmissionFlatFileWithFormatError() {
        ManifestBuilder manifest = sequenceManifest()
                .file("FLATFILE", "invalid_flatfile.dat.gz");
        WebinCli cli = assertWebinCliException(WebinCliBuilder.SEQUENCE, GENOME_RESOURCE_DIR, manifest);
        WebinCliTestUtils.assertReportContains(
                cli,
                "invalid_flatfile.dat.gz",
                "ERROR: Invalid ID line format [ line: 1]");
    }

    @Test
    public void testTranscriptomeSubmissionFasta() {
        ManifestBuilder manifest = transcriptomeManifest()
                .file("FASTA", "valid/valid_fasta.fasta.gz");
        WebinCliBuilder.TRANSCRIPTOME.execute(TRANSCRIPTOME_RESOURCE_DIR, manifest);
    }

    @Test
    public void testTranscriptomeSubmissionFastaWithInfo() {
        File infoFile = transcriptomeManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("FASTA", "valid/valid_fasta.fasta.gz")
                .field("INFO", infoFile.getAbsolutePath());
        WebinCliBuilder.TRANSCRIPTOME.execute(TRANSCRIPTOME_RESOURCE_DIR, manifest);
    }

    @Test
    public void testTranscriptomeSubmissionFlatFileWithFormatError() {
        ManifestBuilder manifest = transcriptomeManifest()
                .file("FLATFILE", "invalid_flatfile.dat.gz");
        WebinCli cli = assertWebinCliException(WebinCliBuilder.TRANSCRIPTOME, GENOME_RESOURCE_DIR, manifest);
        WebinCliTestUtils.assertReportContains(
                cli,
                "invalid_flatfile.dat.gz",
                "ERROR: Invalid ID line format [ line: 1]");
    }
}
