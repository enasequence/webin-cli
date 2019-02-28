/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.entity.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GenomeAssemblyValidationTest {

    private File defaultInputDir = new File(this.getClass().getClassLoader().getResource(
            "uk/ac/ebi/ena/assembly/valid_fasta.fasta.gz").getFile()).getParentFile();
    private File tempInputDir = WebinCliTestUtils.createTempDir();
    private File validationDir = WebinCliTestUtils.createTempDir();
    private File submitDir = WebinCliTestUtils.createTempDir();

    private class ManifestBuilder {
        private String manifest;
        private final File inputDir;

        ManifestBuilder(File inputDir) {
            manifest =
                    "STUDY test\n" +
                    "SAMPLE test\n" +
                    "COVERAGE 1\n" +
                    "PROGRAM test\n" +
                    "PLATFORM test\n" +
                    "NAME test\n";
            this.inputDir = inputDir;
        }

        ManifestBuilder field(String field, String value) {
            manifest += field + "\t" + value + "\n";
            return this;
        }

        ManifestBuilder file(FileType fileType, String fileName) {
            return field(fileType.name(), fileName);
        }

        ManifestBuilder fasta(String fileName) {
            return file(FileType.FASTA, fileName);
        }

        ManifestBuilder flatfile(String fileName) {
            return file(FileType.FLATFILE, fileName);
        }

        ManifestBuilder agp(String fileName) {
            return file(FileType.AGP, fileName);
        }

        ManifestBuilder chromosomeList(String fileName) {
            return file(FileType.CHROMOSOME_LIST, fileName);
        }

        ManifestBuilder unlocalisedList(String fileName) {
            return file(FileType.UNLOCALISED_LIST, fileName);
        }

        ManifestBuilder gzipTempFile(FileType fileType, String fileName) {
            return file(fileType, WebinCliTestUtils.createEmptyGzippedTempFile(
                    fileName, inputDir.toPath()).getFileName().toString());
        }

        ManifestBuilder gzipTempFasta(String fileName) {
            return gzipTempFile(FileType.FASTA, fileName);
        }

        ManifestBuilder gzipTempFlatfile(String fileName) {
            return gzipTempFile(FileType.FLATFILE, fileName);
        }

        ManifestBuilder gzipTempAgp(String fileName) {
            return gzipTempFile(FileType.AGP, fileName);
        }

        ManifestBuilder gzipTempChromosomeList(String fileName) {
            return gzipTempFile(FileType.CHROMOSOME_LIST, fileName);
        }

        ManifestBuilder gzipTempUnlocalisedList(String fileName) {
            return gzipTempFile(FileType.UNLOCALISED_LIST, fileName);
        }

        ManifestBuilder gzipTempFasta() {
            return gzipTempFile(FileType.FASTA, ".fasta.gz");
        }

        ManifestBuilder gzipTempFlatfile() {
            return gzipTempFile(FileType.FLATFILE, ".dat.gz");
        }

        ManifestBuilder gzipTempAgp() {
            return gzipTempFile(FileType.AGP, ".agp.gz");
        }

        ManifestBuilder gzipTempChromosomeList() {
            return gzipTempFile(FileType.CHROMOSOME_LIST, ".txt.gz");
        }

        ManifestBuilder gzipTempUnlocalisedList() {
            return gzipTempFile(FileType.UNLOCALISED_LIST, ".txt.gz");
        }

        ManifestBuilder gzipTempFiles(
                boolean fasta,
                boolean flatfile,
                boolean agp,
                boolean chromosomeList,
                boolean unlocalisedList) {
            if (fasta) gzipTempFile(FileType.FASTA, ".fasta.gz");
            if (flatfile) gzipTempFile(FileType.FLATFILE, ".dat.gz");
            if (agp) gzipTempFile(FileType.AGP, ".agp.gz");
            if (chromosomeList) gzipTempFile(FileType.CHROMOSOME_LIST, ".txt.gz");
            if (unlocalisedList) gzipTempFile(FileType.UNLOCALISED_LIST, ".txt.gz");
            return this;
        }

        ManifestBuilder assemblyType(String value) {
            return field("ASSEMBLY_TYPE", value);
        }

        Path build() {
            // System.out.println("Manifest:\n" + manifest);
            return WebinCliTestUtils.createTempFile(manifest);
        }

        @Override
        public String toString() {
            return "ManifestBuilder{" +
                    "manifest='" + manifest + '\'' +
                    '}';
        }
    }

    @Before
    public void
    before() {
        Locale.setDefault(Locale.UK);
    }

    private GenomeAssemblyWebinCli createValidator(File inputDir) {
        GenomeAssemblyWebinCli cli = new GenomeAssemblyWebinCli();
        cli.setTestMode(true);
        cli.setInputDir(inputDir);
        cli.setValidationDir(validationDir);
        cli.setSubmitDir(submitDir);
        cli.setFetchSample(false);
        cli.setFetchStudy(false);
        cli.setFetchSource(false);
        cli.setSample(AssemblyTestUtils.getDefaultSample());
        cli.setSource(AssemblyTestUtils.getDefaultSourceFeature());
        cli.setStudy(new Study());
        return cli;
    }

    private GenomeAssemblyWebinCli initValidator(Path manifestFile, GenomeAssemblyWebinCli validator) {
        validator.init(AssemblyTestUtils.createWebinCliParameters(manifestFile.toFile(), validator.getInputDir()));
        return validator;
    }

    private GenomeAssemblyWebinCli initValidatorThrows(Path manifestFile, GenomeAssemblyWebinCli validator) {
        assertThatThrownBy(() ->
                validator.init(AssemblyTestUtils.createWebinCliParameters(manifestFile.toFile(), validator.getInputDir())))
                .isInstanceOf(WebinCliException.class);
        return validator;
    }

    private void assertValidatorError(GenomeAssemblyWebinCli validator, WebinCliMessage message) {
        assertThat(validator.getManifestReader().getValidationResult().count(message.key(), Severity.ERROR)).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void
    testFileGroup() {
        for (boolean fasta : new boolean[]{false, true}) {
            for (boolean flatfile : new boolean[]{false, true}) {
                for (boolean agp : new boolean[]{false, true}) {
                    for (boolean chromosomeList : new boolean[]{false, true}) {
                        for (boolean unlocalisedList : new boolean[]{false, true}) {
                            Path manifestFile = new ManifestBuilder(tempInputDir).gzipTempFiles(
                                    fasta,
                                    flatfile,
                                    agp,
                                    chromosomeList,
                                    unlocalisedList).build();

                            int cnt = (fasta ? 1 : 0) +
                                    (flatfile ? 1 : 0) +
                                    (agp ? 1 : 0) +
                                    (chromosomeList ? 1 : 0) +
                                    (unlocalisedList ? 1 : 0);

                            if ((cnt == 1 && fasta) ||
                                    (cnt == 1 && flatfile) ||
                                    (cnt == 2 && fasta && flatfile) ||
                                    (cnt == 2 && fasta && agp) ||
                                    (cnt == 2 && flatfile && agp) ||
                                    (cnt == 3 && fasta && flatfile && agp) ||
                                    (cnt == 2 && fasta && chromosomeList) ||
                                    (cnt == 2 && flatfile && chromosomeList) ||
                                    (cnt == 3 && fasta && flatfile && chromosomeList) ||
                                    (cnt == 3 && fasta && agp && chromosomeList) ||
                                    (cnt == 3 && flatfile && agp && chromosomeList) ||
                                    (cnt == 4 && fasta && flatfile && agp && chromosomeList) ||
                                    (cnt == 3 && fasta && chromosomeList && unlocalisedList) ||
                                    (cnt == 3 && flatfile && chromosomeList && unlocalisedList) ||
                                    (cnt == 4 && fasta && flatfile && chromosomeList && unlocalisedList) ||
                                    (cnt == 4 && fasta && agp && chromosomeList && unlocalisedList) ||
                                    (cnt == 4 && flatfile && agp && chromosomeList && unlocalisedList) ||
                                    (cnt == 5 && fasta && flatfile && agp && chromosomeList && unlocalisedList)) {

                                GenomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(tempInputDir));
                                SubmissionFiles submissionFiles = validator.getSubmissionOptions().submissionFiles.get();
                                assertThat(submissionFiles.getFiles(FileType.FASTA).size()).isEqualTo(fasta ? 1 : 0);
                                assertThat(submissionFiles.getFiles(FileType.FLATFILE).size()).isEqualTo(flatfile ? 1 : 0);
                                assertThat(submissionFiles.getFiles(FileType.AGP).size()).isEqualTo(agp ? 1 : 0);
                                assertThat(submissionFiles.getFiles(FileType.CHROMOSOME_LIST).size()).isEqualTo(chromosomeList ? 1 : 0);
                                assertThat(submissionFiles.getFiles(FileType.UNLOCALISED_LIST).size()).isEqualTo(unlocalisedList ? 1 : 0);
                            } else if (cnt == 0) {
                                GenomeAssemblyWebinCli validator = initValidatorThrows(manifestFile, createValidator(tempInputDir));
                                assertValidatorError(validator, WebinCliMessage.Manifest.NO_DATA_FILES_ERROR);
                            } else {
                                GenomeAssemblyWebinCli validator = initValidatorThrows(manifestFile, createValidator(tempInputDir));
                                assertValidatorError(validator, WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void
    testFileGroupBinnedMetagenome() {
        testFileGroupFastaOnly("binned metagenome");
        testFileGroupFastaOnly("primary metagenome");
    }

    @Test
    public void
    testFileGroupPrimaryMetagenome() {
        testFileGroupFastaOnly("primary metagenome");
    }

    private void
    testFileGroupFastaOnly(String assemblyType) {
        for (boolean fasta : new boolean[]{false, true}) {
            for (boolean flatfile : new boolean[]{false, true}) {
                for (boolean agp : new boolean[]{false, true}) {
                    for (boolean chromosomeList : new boolean[]{false, true}) {
                        for (boolean unlocalisedList : new boolean[]{false, true}) {
                            Path manifestFile = new ManifestBuilder(tempInputDir).assemblyType(assemblyType)
                                    .gzipTempFiles(
                                            fasta,
                                            flatfile,
                                            agp,
                                            chromosomeList,
                                            unlocalisedList).build();

                            int cnt = (fasta ? 1 : 0) +
                                    (flatfile ? 1 : 0) +
                                    (agp ? 1 : 0) +
                                    (chromosomeList ? 1 : 0) +
                                    (unlocalisedList ? 1 : 0);

                            if (cnt == 1 && fasta) {
                                GenomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(tempInputDir));
                                SubmissionFiles submissionFiles = validator.getSubmissionOptions().submissionFiles.get();
                                assertThat(submissionFiles.getFiles().size()).isOne();
                            } else if (cnt == 0) {
                                GenomeAssemblyWebinCli validator = initValidatorThrows(manifestFile, createValidator(tempInputDir));
                                assertValidatorError(validator, WebinCliMessage.Manifest.NO_DATA_FILES_ERROR);
                            } else {
                                GenomeAssemblyWebinCli validator = initValidatorThrows(manifestFile, createValidator(tempInputDir));
                                assertValidatorError(validator, WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void
    testFileSuffix() {
        Path manifests[] = new Path[]{
                // Invalid suffix before .gz
                new ManifestBuilder(tempInputDir).gzipTempFasta(".INVALID.gz").build(),
                new ManifestBuilder(tempInputDir).gzipTempAgp(".INVALID.gz").gzipTempFasta().build(),
                // No .gz
                new ManifestBuilder(tempInputDir).gzipTempFasta(".fasta").build(),
                new ManifestBuilder(tempInputDir).gzipTempFlatfile(".txt").build(),
                new ManifestBuilder(tempInputDir).gzipTempAgp(".agp").gzipTempFasta().build(),
                new ManifestBuilder(tempInputDir).gzipTempChromosomeList(".txt").gzipTempFasta().build(),
                new ManifestBuilder(tempInputDir).gzipTempUnlocalisedList(".txt").gzipTempChromosomeList().gzipTempFasta().build(),};
        Arrays.stream(manifests).forEach(manifest -> {
            GenomeAssemblyWebinCli validator = initValidatorThrows(manifest, createValidator(tempInputDir));
            assertValidatorError(validator, WebinCliMessage.Manifest.INVALID_FILE_SUFFIX_ERROR);
        });
    }

    @Test
    public void
    testFileNoMoreThanOne() {
        Path manifests[] = new Path[]{
                new ManifestBuilder(tempInputDir).gzipTempFasta().gzipTempFasta().build(),
                new ManifestBuilder(tempInputDir).gzipTempFlatfile().gzipTempFlatfile().build(),
                new ManifestBuilder(tempInputDir).gzipTempAgp().gzipTempAgp().build(),
                new ManifestBuilder(tempInputDir).gzipTempChromosomeList().gzipTempChromosomeList().build(),
                new ManifestBuilder(tempInputDir).gzipTempUnlocalisedList().gzipTempUnlocalisedList().build()};
        Arrays.stream(manifests).forEach(manifest -> {
            GenomeAssemblyWebinCli validator = initValidatorThrows(manifest, createValidator(tempInputDir));
            assertValidatorError(validator, WebinCliMessage.Manifest.TOO_MANY_FIELDS_ERROR);
        });
    }


    @Test
    public void
    testValidFasta() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .fasta("valid_fasta.fasta.gz").build();

        GenomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(defaultInputDir));
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(1);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size()).isOne();
        validator.validate();
    }

    @Test
    public void
    testValidFlatFile() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .flatfile("valid_flatfile.txt.gz").build();

        GenomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(defaultInputDir));
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(1);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size()).isOne();
        validator.validate();
    }

    @Test
    public void
    testValidFastaAndAgp() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .fasta("valid_fasta.fasta.gz")
                .agp("valid_agp.agp.gz").build();

        GenomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(defaultInputDir));
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(2);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size()).isOne();
        validator.validate();
    }

    @Test
    public void
    testValidFlatFileAndAgp() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .flatfile("valid_flatfile.txt.gz")
                .agp("valid_agp.agp.gz").build();

        GenomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(defaultInputDir));
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(2);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size()).isOne();
        validator.validate();
    }

    @Test
    public void
    testValidFastaAndAgpAndChromosomeList() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .fasta("valid_fasta.fasta.gz")
                .agp("valid_agp.agp.gz")
                .chromosomeList("valid_chromosome_list.txt.gz").build();

        GenomeAssemblyWebinCli validator = createValidator(defaultInputDir);
        validator.setSample(AssemblyTestUtils.getHumanSample());
        validator.setSource(AssemblyTestUtils.getHumanSourceFeature());
        initValidator(manifestFile, validator);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(3);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size()).isOne();
        validator.validate();
    }

    @Test
    public void
    testValidFlatFileAndAgpAndChromosomeList() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .flatfile("valid_flatfile.txt.gz")
                .agp("valid_agp.agp.gz")
                .chromosomeList("valid_chromosome_list.txt.gz").build();

        GenomeAssemblyWebinCli validator = createValidator(defaultInputDir);
        validator.setSample(AssemblyTestUtils.getHumanSample());
        validator.setSource(AssemblyTestUtils.getHumanSourceFeature());
        initValidator(manifestFile, validator);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(3);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size()).isOne();
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size()).isOne();
        validator.validate();
    }

    @Test
    public void
    testInvalidFasta() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .fasta("invalid_fasta.fasta.gz").build();

        GenomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(defaultInputDir));
        assertThatThrownBy(validator::validate).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("fasta file validation failed");
    }

    @Test
    public void
    testInvalidFlatFile() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .flatfile("invalid_flatfile.txt.gz").build();

        GenomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(defaultInputDir));
        assertThatThrownBy(validator::validate).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("flatfile file validation failed");
    }

    @Test
    public void
    testInvalidAgp() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .fasta("valid_fasta.fasta.gz")
                .agp("invalid_agp.agp.gz").build();

        GenomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(defaultInputDir));
        assertThatThrownBy(validator::validate).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("agp file validation failed");
    }

    @Test
    public void
    testInvalidSequencelessChromosomeList() {
        Path manifestFile = new ManifestBuilder(defaultInputDir)
                .fasta("valid_fasta.fasta.gz")
                .chromosomeList("invalid_chromosome_list_sequenceless.txt.gz").build();

        GenomeAssemblyWebinCli validator = createValidator(defaultInputDir);
        validator.setSample(AssemblyTestUtils.getHumanSample());
        validator.setSource(AssemblyTestUtils.getHumanSourceFeature());
        initValidator(manifestFile, validator);
        assertThatThrownBy(validator::validate).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("Sequenceless chromosomes are not allowed in assembly");
    }
}
