/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

public class WebinCliSubmissionTest {

    private static final File READS_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/reads/");
    private static final File GENOME_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/genome/");
    private static final File TRANSCRIPTOME_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/transcriptome/");
    private static final File SEQUENCE_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/sequence/");

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

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
        return genomeManifest("clone or isolate");
    }

    private ManifestBuilder genomeManifest(String assemblyType) {
    return new ManifestBuilder()
            .name()
            .field("ASSEMBLY_TYPE", assemblyType)
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

    private ManifestBuilder covid19GenomeManifest() {
        return new ManifestBuilder()
                .name()
                .field("ASSEMBLY_TYPE", "COVID-19 outbreak")
                .field("STUDY", "ERP011959")
                .field("SAMPLE", "ERS829308")
                .field("COVERAGE", "1.0")
                .field("PROGRAM", "prog-123")
                .field("PLATFORM", "ILLUMINA")
                .file("FASTA", "valid-covid19.fasta.gz")
                .file("CHROMOSOME_LIST", "valid-covid19-chromosome.list.gz");
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
                .field("DESCRIPTION", "Some transcriptome assembly description")
                .field("ASSEMBLY_TYPE", "isolate");
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
        WebinCli cli = webinCliBuilder.build(inputDir, manifest);
        assertThatThrownBy(cli::execute)
                .isInstanceOf(WebinCliException.class)
                .hasMessageContaining("Submission validation failed because of a user error");
        return cli;
    }

    @Test
    public void testReadsSubmissionCram() {
        ManifestBuilder manifest = readsManifest()
                .file("CRAM", "valid.cram");
        WebinCliBuilder.READS.build(READS_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testReadsSubmissionCramWithNoSubmissionBundleAndSubmissionXmlFilesSaving() {
        ManifestBuilder manifest = readsManifest()
            .file("CRAM", "valid.cram");
        WebinCliBuilder.READS.saveSubmissionBundleFile(false).saveSubmissionXmlFiles(false)
            .build(READS_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testReadsSubmissionCramNormalizePath() {
        ManifestBuilder manifest = readsManifest()
                .file("CRAM", "../" + READS_RESOURCE_DIR.getName() + "/valid.cram");
        WebinCliBuilder.READS.build(READS_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testReadsSubmissionCramWithInfo() {
        File infoFile = readsManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("CRAM", "valid.cram")
                .field("INFO", infoFile.getAbsolutePath());
        WebinCliBuilder.READS.build(READS_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testReadsSubmissionCramWithAscp() {
        ManifestBuilder manifest = readsManifest()
                .file("CRAM", "valid.cram");
        WebinCliBuilder.READS.ascp(true).build(READS_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testReadsSubmissionWithMultipleFastq() {
        ManifestBuilder manifest = readsManifest()
            .file("FASTQ", "10x/4fastq/I1.fastq.gz")
            .file("FASTQ", "10x/4fastq/R1.fastq.gz")
            .file("FASTQ", "10x/4fastq/R2.fastq.gz")
            .file("FASTQ", "10x/4fastq/R3.fastq.gz");
        WebinCliBuilder.READS.ascp(true).build(READS_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testGenomeSubmissionFlatFileAgp() {
        ManifestBuilder manifest = genomeManifest()
                .file("FLATFILE", "valid.flatfile.gz")
                .file("AGP", "valid.agp.gz");
        WebinCliBuilder.GENOME.build(GENOME_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testCovid19GenomeSubmissionRateLimitErrorIgnore() {
        WebinCliException ex = Assert.assertThrows(WebinCliException.class, () -> {
            //Submitting more than once within 24 hours will throw a rate limit error.
            for (int i = 0; i < 2; i++) {
                WebinCliBuilder.GENOME.build(GENOME_RESOURCE_DIR, covid19GenomeManifest()).execute();
            }
        });

        Assert.assertTrue(ex.getMessage().toLowerCase().contains("cannot submit more than 1 genome within 24 hours for one submission account"));

        WebinCliBuilder.GENOME.ignoreErrors(true).build(GENOME_RESOURCE_DIR, covid19GenomeManifest()).execute();
    }

    @Test
    public void testGenomeSubmissionFlatFileAgpWithInfo() {
        File infoFile = genomeManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("FLATFILE", "valid.flatfile.gz")
                .file("AGP", "valid.agp.gz")
                .field("INFO", infoFile.getAbsolutePath());
        WebinCliBuilder.GENOME.build(GENOME_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testGenomeSubmissionFlatFileWithFormatError() {
        ManifestBuilder manifest = genomeManifest()
                .file("FLATFILE", "invalid.flatfile.gz");
        WebinCli cli = assertWebinCliException(WebinCliBuilder.GENOME, GENOME_RESOURCE_DIR, manifest);
        new ReportTester(cli).textInFileReport("invalid.flatfile.gz",
                "ERROR: Invalid ID line format [ line: 1]");
    }

    @Test
    public void testGenomeSubmissionFastaWithOneSequenceError() {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path fastaFile = TempFileBuilder.gzip(
                inputDir, "test.fasta.gz", ">A\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n");
        ManifestBuilder manifest = genomeManifest()
                .file("FASTA", fastaFile);
        WebinCli cli = assertWebinCliException(WebinCliBuilder.GENOME, inputDir, manifest);
        new ReportTester(cli).textInSubmissionReport(
                "Invalid number of sequences : 1, Minimum number of sequences for CONTIG is: 2");
    }

    @Test
    public void testGenomeSubmissionFastaWithOneSequencePrimaryMetagenome() {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path fastaFile = TempFileBuilder.gzip(
                inputDir,"test.fasta.gz", ">A\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n");
        ManifestBuilder manifest = genomeManifest("primary metagenome")
                .file("FASTA", fastaFile);
    }

    @Test
    public void testSequenceSubmissionTab() {
        ManifestBuilder manifest = sequenceManifest()
                .file("TAB", "valid/ERT000003-EST.tsv.gz");
        WebinCliBuilder.SEQUENCE.build(SEQUENCE_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testSequenceSubmissionTabWithInfo() {
        File infoFile = sequenceManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("TAB", "valid/ERT000003-EST.tsv.gz")
                .field("INFO", infoFile.getAbsolutePath());
        WebinCliBuilder.SEQUENCE.build(SEQUENCE_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testSequenceSubmissionTabWithSampleInOrganismField() {
        File infoFile = sequenceManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("TAB", "valid/ERT000002_rRNA-with-sample-field.tsv.gz")
                .field("INFO", infoFile.getAbsolutePath());
                
        WebinCliBuilder.SEQUENCE.build(SEQUENCE_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testSequenceSubmissionTabWithInvalidSampleInOrganismField() {
        exceptionRule.expect(WebinCliException.class);
        File infoFile = sequenceManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("TAB", "valid/ERT000002_rRNA-with-invalid-sample-field.tsv.gz")
                .field("INFO", infoFile.getAbsolutePath());

        WebinCliBuilder.SEQUENCE.build(SEQUENCE_RESOURCE_DIR, manifest).execute();
    }
    
    @Test
    public void testSequenceSubmissionFlatFileWithFormatError() {
        ManifestBuilder manifest = sequenceManifest()
                .file("FLATFILE", "invalid.flatfile.gz");
        WebinCli cli = assertWebinCliException(WebinCliBuilder.SEQUENCE, GENOME_RESOURCE_DIR, manifest);
        new ReportTester(cli).textInFileReport("invalid.flatfile.gz",
                "ERROR: Invalid ID line format [ line: 1]");
    }

    @Test
    public void testTranscriptomeSubmissionFasta() {
        ManifestBuilder manifest = transcriptomeManifest()
                .file("FASTA", "valid/valid.fasta.gz");
        WebinCliBuilder.TRANSCRIPTOME.build(TRANSCRIPTOME_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testTranscriptomeSubmissionFastaWithInfo() {
        File infoFile = transcriptomeManifest().build();
        ManifestBuilder manifest = new ManifestBuilder()
                .file("FASTA", "valid/valid.fasta.gz")
                .field("INFO", infoFile.getAbsolutePath());
        WebinCliBuilder.TRANSCRIPTOME.build(TRANSCRIPTOME_RESOURCE_DIR, manifest).execute();
    }

    @Test
    public void testTranscriptomeSubmissionFlatFileWithFormatError() {
        ManifestBuilder manifest = transcriptomeManifest()
                .file("FLATFILE", "invalid.flatfile.gz");
        WebinCli cli = assertWebinCliException(WebinCliBuilder.TRANSCRIPTOME, GENOME_RESOURCE_DIR, manifest);
        new ReportTester(cli).textInFileReport("invalid.flatfile.gz",
                "ERROR: Invalid ID line format [ line: 1]");
    }

    @Test
    public void testSampleLookupErrorFormat() {
        String sample = "INVALID";

        ManifestBuilder manifest = new ManifestBuilder()
            .name()
            .field("ASSEMBLY_TYPE", "COVID-19 outbreak")
            .field("STUDY", "ERP011959")
            .field("SAMPLE", sample)
            .field("COVERAGE", "1.0")
            .field("PROGRAM", "prog-123")
            .field("PLATFORM", "ILLUMINA")
            .file("FASTA", "valid-covid19.fasta.gz")
            .file("CHROMOSOME_LIST", "valid-covid19-chromosome.list.gz");

        WebinCli cli = WebinCliBuilder.GENOME.build(GENOME_RESOURCE_DIR, manifest);

        assertThatThrownBy(cli::execute).isInstanceOf(WebinCliException.class)
            .hasFieldOrPropertyWithValue("errorType", WebinCliException.ErrorType.USER_ERROR);

        new ReportTester(cli).textInSubmissionReport("Unknown sample " + sample +
            " or the sample cannot be referenced by your submission account.");
    }

    @Test
    public void testStudyLookupErrorFormat() {
        String study = "INVALID";

        ManifestBuilder manifest = new ManifestBuilder()
            .name()
            .field("ASSEMBLY_TYPE", "COVID-19 outbreak")
            .field("STUDY", study)
            .field("SAMPLE", "ERS829308")
            .field("COVERAGE", "1.0")
            .field("PROGRAM", "prog-123")
            .field("PLATFORM", "ILLUMINA")
            .file("FASTA", "valid-covid19.fasta.gz")
            .file("CHROMOSOME_LIST", "valid-covid19-chromosome.list.gz");

        WebinCli cli = WebinCliBuilder.GENOME.build(GENOME_RESOURCE_DIR, manifest);

        assertThatThrownBy(cli::execute).isInstanceOf(WebinCliException.class)
            .hasFieldOrPropertyWithValue("errorType", WebinCliException.ErrorType.USER_ERROR);

        new ReportTester(cli).textInSubmissionReport("Unknown study " + study +
            " or the study cannot be referenced by your submission account.");
    }

    @Test
    public void testRunLookupErrorFormat() {
        String run = "INVALID";

        ManifestBuilder manifest = new ManifestBuilder()
            .name()
            .field("STUDY", "PRJEB20083")
            .field("RUN_REF", run)
            .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
            .field("DESCRIPTION", "Some sequence assembly description")
            .file("TAB", "valid/ERT000003-EST.tsv.gz");

        WebinCli cli = WebinCliBuilder.SEQUENCE.build(SEQUENCE_RESOURCE_DIR, manifest);

        assertThatThrownBy(cli::execute).isInstanceOf(WebinCliException.class)
            .hasFieldOrPropertyWithValue("errorType", WebinCliException.ErrorType.USER_ERROR);

        new ReportTester(cli).textInSubmissionReport("Unknown run " + run +
            " or the run cannot be referenced by your submission account. Runs must be submitted before they can be referenced in the submission.");
    }

    @Test
    public void testAnalysisLookupErrorFormat() {
        String analysis = "INVALID";

        ManifestBuilder manifest = new ManifestBuilder()
            .name()
            .field("STUDY", "PRJEB20083")
            .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
            .field("ANALYSIS_REF", analysis)
            .field("DESCRIPTION", "Some sequence assembly description")
            .file("TAB", "valid/ERT000003-EST.tsv.gz");

        WebinCli cli = WebinCliBuilder.SEQUENCE.build(SEQUENCE_RESOURCE_DIR, manifest);

        assertThatThrownBy(cli::execute).isInstanceOf(WebinCliException.class)
            .hasFieldOrPropertyWithValue("errorType", WebinCliException.ErrorType.USER_ERROR);

        new ReportTester(cli).textInSubmissionReport("Unknown analysis " + analysis +
            " or the analysis cannot be referenced by your submission account.");
    }
}
