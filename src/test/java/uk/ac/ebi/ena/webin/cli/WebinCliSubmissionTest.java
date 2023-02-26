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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

import java.io.File;
import java.util.function.Consumer;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

public class WebinCliSubmissionTest {

    private enum WebinCliTestType {
        VALIDATE,
        SUBMIT
    }

    private static final WebinCliTestType TEST_TYPE = WebinCliTestType.SUBMIT;

    private static final File READS_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/reads/");
    private static final File GENOME_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/genome/");
    private static final File TRANSCRIPTOME_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/transcriptome/");
    private static final File SEQUENCE_RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/sequence/");

    private static void expectUserError(ThrowableAssert.ThrowingCallable shouldThrow) {
        assertThatThrownBy(shouldThrow)
                .isInstanceOf(WebinCliException.class)
                .hasFieldOrPropertyWithValue("errorType", WebinCliException.ErrorType.USER_ERROR);
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
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig) {
        testReads(metaManifestConfig, filesManifestConfig, c -> {
        });
    }

    public static void testReads(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig,
            Consumer<WebinCliBuilder> webinCliConfig) {
        // Test -validateFiles option
        webinCLiForValidateFiles(READS_RESOURCE_DIR, WebinCliBuilder.READS, metaManifestConfig, filesManifestConfig, webinCliConfig).execute();

        if (TEST_TYPE == WebinCliTestType.VALIDATE) {
            webinCliForValidate(READS_RESOURCE_DIR, WebinCliBuilder.READS, metaManifestConfig, filesManifestConfig, webinCliConfig).execute();
        } else {
            webinCliForSubmit(READS_RESOURCE_DIR, WebinCliBuilder.READS, metaManifestConfig, filesManifestConfig, webinCliConfig).execute();
        }
    }

    public static void testReadsUserError(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig,
            Consumer<ReportTester> reportTesterConfig) {
        // Test -validateFiles option
        {
            WebinCli webinCli = webinCLiForValidateFiles(READS_RESOURCE_DIR, WebinCliBuilder.READS, metaManifestConfig, filesManifestConfig, c -> {
            });
            expectUserError(() -> webinCli.execute());
            reportTesterConfig.accept(new ReportTester(webinCli));
        }

        if (TEST_TYPE == WebinCliTestType.VALIDATE) {
            WebinCli webinCli = webinCliForValidate(READS_RESOURCE_DIR, WebinCliBuilder.READS, metaManifestConfig, filesManifestConfig, c -> {
            });
            expectUserError(() -> webinCli.execute());
            reportTesterConfig.accept(new ReportTester(webinCli));
        } else {
            WebinCli webinCli = webinCliForSubmit(READS_RESOURCE_DIR, WebinCliBuilder.READS, metaManifestConfig, filesManifestConfig, c -> {
            });
            expectUserError(() -> webinCli.execute());
            reportTesterConfig.accept(new ReportTester(webinCli));
        }
    }

    public static void testGenome(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig) {
        testGenome(metaManifestConfig, filesManifestConfig, c -> {
        });
    }

    public static void testGenome(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig,
            Consumer<WebinCliBuilder> webinCliConfig) {
        if (TEST_TYPE == WebinCliTestType.VALIDATE) {
            webinCliForValidate(GENOME_RESOURCE_DIR, WebinCliBuilder.GENOME, metaManifestConfig, filesManifestConfig, webinCliConfig).execute();
        } else {
            webinCliForSubmit(GENOME_RESOURCE_DIR, WebinCliBuilder.GENOME, metaManifestConfig, filesManifestConfig, webinCliConfig).execute();
        }
    }

    public static void testGenomeUserError(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig,
            Consumer<ReportTester> reportTesterConfig) {
        if (TEST_TYPE == WebinCliTestType.VALIDATE) {
            WebinCli webinCli = webinCliForValidate(GENOME_RESOURCE_DIR, WebinCliBuilder.GENOME, metaManifestConfig, filesManifestConfig, c -> {
            });
            expectUserError(() -> webinCli.execute());
            reportTesterConfig.accept(new ReportTester(webinCli));
        } else {
            WebinCli webinCli = webinCliForSubmit(GENOME_RESOURCE_DIR, WebinCliBuilder.GENOME, metaManifestConfig, filesManifestConfig, c -> {
            });
            expectUserError(() -> webinCli.execute());
            reportTesterConfig.accept(new ReportTester(webinCli));
        }
    }

    public static void testSequence(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig) {
        testSequence(metaManifestConfig, filesManifestConfig, c -> {
        });
    }

    public static void testSequence(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig,
            Consumer<WebinCliBuilder> webinCliConfig) {
        if (TEST_TYPE == WebinCliTestType.VALIDATE) {
            webinCliForValidate(SEQUENCE_RESOURCE_DIR, WebinCliBuilder.SEQUENCE, metaManifestConfig, filesManifestConfig, webinCliConfig).execute();
        } else {
            webinCliForSubmit(SEQUENCE_RESOURCE_DIR, WebinCliBuilder.SEQUENCE, metaManifestConfig, filesManifestConfig, webinCliConfig).execute();
        }
    }

    public static void testSequenceUserError(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig,
            Consumer<ReportTester> reportTesterConfig) {
        if (TEST_TYPE == WebinCliTestType.VALIDATE) {
            WebinCli webinCli = webinCliForValidate(SEQUENCE_RESOURCE_DIR, WebinCliBuilder.SEQUENCE, metaManifestConfig, filesManifestConfig, c -> {
            });
            expectUserError(() -> webinCli.execute());
            reportTesterConfig.accept(new ReportTester(webinCli));
        } else {
            WebinCli webinCli = webinCliForSubmit(SEQUENCE_RESOURCE_DIR, WebinCliBuilder.SEQUENCE, metaManifestConfig, filesManifestConfig, c -> {
            });
            expectUserError(() -> webinCli.execute());
            reportTesterConfig.accept(new ReportTester(webinCli));
        }
    }

    public static void testTranscriptome(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig) {
        testTranscriptome(metaManifestConfig, filesManifestConfig, c -> {
        });
    }

    public static void testTranscriptome(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig,
            Consumer<WebinCliBuilder> webinCliConfig) {
        if (TEST_TYPE == WebinCliTestType.VALIDATE) {
            webinCliForValidate(TRANSCRIPTOME_RESOURCE_DIR, WebinCliBuilder.TRANSCRIPTOME, metaManifestConfig, filesManifestConfig, webinCliConfig).execute();
        } else {
            webinCliForSubmit(TRANSCRIPTOME_RESOURCE_DIR, WebinCliBuilder.TRANSCRIPTOME, metaManifestConfig, filesManifestConfig, webinCliConfig).execute();
        }
    }

    public static void testTranscriptomeUserError(
            Consumer<ManifestBuilder> metaManifestConfig,
            Consumer<ManifestBuilder> filesManifestConfig,
            Consumer<ReportTester> reportTesterConfig) {
        if (TEST_TYPE == WebinCliTestType.VALIDATE) {
            WebinCli webinCli = webinCliForValidate(TRANSCRIPTOME_RESOURCE_DIR, WebinCliBuilder.TRANSCRIPTOME, metaManifestConfig, filesManifestConfig, c -> {
            });
            expectUserError(() -> webinCli.execute());
            reportTesterConfig.accept(new ReportTester(webinCli));
        } else {
            WebinCli webinCli = webinCliForSubmit(TRANSCRIPTOME_RESOURCE_DIR, WebinCliBuilder.TRANSCRIPTOME, metaManifestConfig, filesManifestConfig, c -> {
            });
            expectUserError(() -> webinCli.execute());
            reportTesterConfig.accept(new ReportTester(webinCli));
        }
    }

    // Manifest meta fields
    //

    private void readsMetaManifest(ManifestBuilder manifestBuilder) {
        manifestBuilder
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

    private void genomeMetaManifest(ManifestBuilder manifestBuilder) {
        manifestBuilder
                .name()
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
                .name()
                .field("ASSEMBLY_TYPE", "COVID-19 outbreak")
                .field("STUDY", "ERP011959")
                .field("SAMPLE", "ERS829308")
                .field("COVERAGE", "1.0")
                .field("PROGRAM", "prog-123")
                .field("PLATFORM", "ILLUMINA");
    }

    private void transcriptomeMetaManifest(ManifestBuilder manifestBuilder) {
        manifestBuilder
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

    private void sequenceMetaManifest(ManifestBuilder manifestBuilder) {
        manifestBuilder
                .name()
                .field("STUDY", "PRJEB20083")
                .field("RUN_REF", "ERR2836762, ERR2836753, SRR8083599")
                .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
                .field("DESCRIPTION", "Some sequence assembly description");
    }

    // Reads
    //

    @Test
    public void testReadsCram() {
        testReads(m -> readsMetaManifest(m), m -> m.file("CRAM", "valid.cram"));
    }

    @Test
    public void testReadsCramNormalizePath() {
        testReads(
                m -> readsMetaManifest(m),
                m -> m.file("CRAM", "../" + READS_RESOURCE_DIR.getName() + "/valid.cram"));
    }

    @Test
    public void testReadsCramWithInfo() {
        ManifestBuilder infoManifestBuilder = new ManifestBuilder();
        readsMetaManifest(infoManifestBuilder);
        File infoFile = infoManifestBuilder.build();
        testReads(m -> {
        }, m -> m.file("CRAM", "valid.cram").field("INFO", infoFile.getAbsolutePath()));
    }

    @Test
    public void testReadsCramWithAscp() {
        testReads(m -> readsMetaManifest(m), m -> m.file("CRAM", "valid.cram"), c -> c.ascp(true));
    }

    @Test
    public void testReadsSingleFastq() {
        testReads(m -> readsMetaManifest(m), m -> m.file("FASTQ", "10x/4fastq/I1.fastq.gz"));
    }

    @Test
    public void testReadsMultipleFastq() {
        testReads(m -> readsMetaManifest(m), m -> m.file("FASTQ", "10x/4fastq/I1.fastq.gz")
                .file("FASTQ", "10x/4fastq/R1.fastq.gz")
                .file("FASTQ", "10x/4fastq/R2.fastq.gz")
                .file("FASTQ", "10x/4fastq/R3.fastq.gz"));
    }

    // Genome
    //

    @Test
    public void testGenomeFlatFileAgp() {
        testGenome(m -> genomeMetaManifest(m), m -> m
                .file("FLATFILE", "valid.flatfile.gz")
                .file("AGP", "valid.agp.gz"));
    }

    @Test
    public void testGenomeFlatFileAgpWithInfo() {
        ManifestBuilder infoManifestBuilder = new ManifestBuilder();
        genomeMetaManifest(infoManifestBuilder);
        File infoFile = infoManifestBuilder.build();
        testGenome(
                m -> {
                },
                m -> m
                        .file("FLATFILE", "valid.flatfile.gz")
                        .file("AGP", "valid.agp.gz")
                        .field("INFO", infoFile.getAbsolutePath()));
    }

    @Test
    public void testCovid19Genome() {
        testGenome(m -> covid19GenomeMetaManifest(m), m -> m
                        .file("FASTA", "valid-covid19.fasta.gz")
                        .file("CHROMOSOME_LIST", "valid-covid19-chromosome.list.gz"),
                c -> c.ignoreErrors(true)); // cannot submit more than 1 genome within 24 hours
    }

    @Test
    public void testGenomeFlatFileWithFormatError() {
        testGenomeUserError(m -> genomeMetaManifest(m),
                m -> m.file("FLATFILE", "invalid.flatfile.gz"),
                r -> r.textInFileReport("invalid.flatfile.gz",
                        "ERROR: Invalid ID line format [ line: 1]")
        );
    }

    @Test
    public void testSequenceTab() {
        testSequence(m -> sequenceMetaManifest(m),
                m -> m.file("TAB", "valid/ERT000003-EST.tsv.gz"));
    }

    @Test
    public void testSequenceTabWithSampleInOrganismField() {
        testSequence(m -> sequenceMetaManifest(m),
                m -> m.file("TAB", "valid/ERT000002_rRNA-with-sample-field.tsv.gz"));
    }

    @Test
    public void testSequenceTabWithInvalidSample() {
        testSequenceUserError(m -> sequenceMetaManifest(m),
                m -> m.file("TAB", "invalid-sample.tsv.gz"),
                r -> r.textInFileReport("invalid-sample.tsv.gz",
                        "Organism name \"ERS000000\" is not submittable"));
    }

    @Test
    public void testSequenceFlatFileWithFormatError() {
        testSequenceUserError(m -> sequenceMetaManifest(m),
                m -> m.file("FLATFILE", "invalid.flatfile.gz"),
                r -> r.textInFileReport("invalid.flatfile.gz",
                        "ERROR: Invalid ID line format [ line: 1]"));
    }

    @Test
    public void testTranscriptomeFasta() {
        testTranscriptome(m -> transcriptomeMetaManifest(m),
                m -> m.file("FASTA", "valid.fasta.gz"));
    }

    @Test
    public void testTranscriptomeFastaWithInfo() {
        ManifestBuilder infoManifestBuilder = new ManifestBuilder();
        transcriptomeMetaManifest(infoManifestBuilder);
        File infoFile = infoManifestBuilder.build();
        testTranscriptome(m -> {
        }, m -> m
                .file("FASTA", "valid.fasta.gz")
                .field("INFO", infoFile.getAbsolutePath()));
    }

    @Test
    public void testTranscriptomeFlatFileWithFormatError() {
        testTranscriptomeUserError(m -> transcriptomeMetaManifest(m),
                m -> m.file("FLATFILE", "invalid.flatfile.gz"),
                r -> r.textInFileReport("invalid.flatfile.gz",
                        "ERROR: Invalid ID line format [ line: 1]"));
    }
}
