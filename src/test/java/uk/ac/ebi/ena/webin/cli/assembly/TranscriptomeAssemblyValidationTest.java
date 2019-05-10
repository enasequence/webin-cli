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
package uk.ac.ebi.ena.webin.cli.assembly;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.entity.Study;

public class TranscriptomeAssemblyValidationTest {

    private File defaultInputDir = new File(this.getClass().getClassLoader().getResource(
            "uk/ac/ebi/ena/webin/cli/transcriptome/valid_fasta.fasta.gz").getFile()).getParentFile();
    private File tempInputDir = WebinCliTestUtils.createTempDir();
    private File validationDir = WebinCliTestUtils.createTempDir();
    private File processDir = WebinCliTestUtils.createTempDir();
    private File submitDir = WebinCliTestUtils.createTempDir();

    @Before
    public void
    before() {
        Locale.setDefault(Locale.UK);
    }

    private class ManifestBuilder {
        private String manifest;
        private final File inputDir;

        ManifestBuilder(File inputDir) {
            manifest =
                    "STUDY test\n" +
                            "SAMPLE test\n" +
                            "PLATFORM test\n" +
                            "PROGRAM test\n" +
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

        ManifestBuilder gzipTempFasta() {
            return gzipTempFile(FileType.FASTA, ".fasta.gz");
        }

        ManifestBuilder gzipTempFlatfile() {
            return gzipTempFile(FileType.FLATFILE, ".dat.gz");
        }

        ManifestBuilder gzipTempFiles(
                boolean fasta,
                boolean flatfile) {
            if (fasta) gzipTempFile(FileType.FASTA, ".fasta.gz");
            if (flatfile) gzipTempFile(FileType.FLATFILE, ".dat.gz");
            return this;
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


    private TranscriptomeAssemblyWebinCli createValidator(File inputDir) {
        TranscriptomeAssemblyWebinCli cli = new TranscriptomeAssemblyWebinCli();
        cli.setTestMode(true);
        cli.setInputDir(inputDir);
        cli.setValidationDir(validationDir);
        cli.setProcessDir(processDir);
        cli.setSubmitDir(submitDir);
        cli.setMetadataServiceActive(false);
        cli.setSample(AssemblyTestUtils.getDefaultSample());
        cli.setSource(AssemblyTestUtils.getDefaultSourceFeature());
        cli.setStudy(new Study());
        return cli;
    }

    private TranscriptomeAssemblyWebinCli initValidator(Path manifestFile, TranscriptomeAssemblyWebinCli validator) {
        validator.readManifest(AssemblyTestUtils.createWebinCliParameters(manifestFile.toFile(), validator.getInputDir()));
        return validator;
    }

    private TranscriptomeAssemblyWebinCli initValidatorThrows(Path manifestFile, TranscriptomeAssemblyWebinCli validator) {
        assertThatThrownBy(() ->
                validator.readManifest(AssemblyTestUtils.createWebinCliParameters(manifestFile.toFile(), validator.getInputDir())))
                .isInstanceOf(WebinCliException.class);
        return validator;
    }

    private void assertValidatorError(TranscriptomeAssemblyWebinCli validator, WebinCliMessage message) {
        assertThat(validator.getManifestReader().getValidationResult().count(message.key(), Severity.ERROR)).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void
    testFileGroup() {
        for (boolean fasta : new boolean[]{false, true}) {
            for (boolean flatfile : new boolean[]{false, true}) {
                Path manifestFile = new ManifestBuilder(tempInputDir).gzipTempFiles(
                        fasta,
                        flatfile).build();

                int cnt = (fasta ? 1 : 0) +
                        (flatfile ? 1 : 0);

                if (cnt == 1) {
                    TranscriptomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(tempInputDir));
                    SubmissionFiles submissionFiles = validator.getSubmissionOptions().submissionFiles.get();
                    assertThat(submissionFiles.getFiles(FileType.FASTA).size()).isEqualTo(fasta ? 1 : 0);
                    assertThat(submissionFiles.getFiles(FileType.FLATFILE).size()).isEqualTo(flatfile ? 1 : 0);
                } else if (cnt == 0) {
                    TranscriptomeAssemblyWebinCli validator = initValidatorThrows(manifestFile, createValidator(tempInputDir));
                    assertValidatorError(validator, WebinCliMessage.Manifest.NO_DATA_FILES_ERROR);
                } else {
                    TranscriptomeAssemblyWebinCli validator = initValidatorThrows(manifestFile, createValidator(tempInputDir));
                    assertValidatorError(validator, WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR);
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
                // No .gz
                new ManifestBuilder(tempInputDir).gzipTempFasta(".fasta").build(),
                new ManifestBuilder(tempInputDir).gzipTempFlatfile(".txt").build()};
        Arrays.stream(manifests).forEach(manifest -> {
            TranscriptomeAssemblyWebinCli validator = initValidatorThrows(manifest, createValidator(tempInputDir));
            assertValidatorError(validator, WebinCliMessage.Manifest.INVALID_FILE_SUFFIX_ERROR);
        });
    }

    @Test
    public void
    testFileNoMoreThanOne() {
        Path manifests[] = new Path[]{
                new ManifestBuilder(tempInputDir).gzipTempFasta().gzipTempFasta().build(),
                new ManifestBuilder(tempInputDir).gzipTempFlatfile().gzipTempFlatfile().build()};
        Arrays.stream(manifests).forEach(manifest -> {
            TranscriptomeAssemblyWebinCli validator = initValidatorThrows(manifest, createValidator(tempInputDir));
            assertValidatorError(validator, WebinCliMessage.Manifest.TOO_MANY_FIELDS_ERROR);
        });
    }

    @Test
    public void
    testValidFasta() {
        Path manifestFile = new TranscriptomeAssemblyValidationTest.ManifestBuilder(defaultInputDir)
                .fasta("valid_fasta.fasta.gz").build();

        TranscriptomeAssemblyWebinCli validator = initValidator(manifestFile, createValidator(defaultInputDir));
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles().size()).isEqualTo(1);
        assertThat(validator.getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size()).isOne();
        validator.validate();
    }
}
