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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.submission.Context;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.entity.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

public class SequenceAssemblyValidationTest {

    private File tempInputDir = WebinCliTestUtils.createTempDir();
    private File validationDir = WebinCliTestUtils.createTempDir();
    private File submitDir = WebinCliTestUtils.createTempDir();

    private final static String[] VALID_TSV_FILES = {
            "ERT000002-rRNA.tsv.gz",
            "ERT000003-EST-1.tsv.gz",
            "ERT000006-SCM.tsv.gz",
            "ERT000009-ITS.tsv.gz",
            "ERT000020-COI.tsv.gz",
            "ERT000024-GSS-1.tsv.gz",
            "ERT000028-SVC.tsv.gz",
            "ERT000029-SCGD.tsv.gz",
            "ERT000030-MHC1.tsv.gz",
            "ERT000031-viroid.tsv.gz",
            "ERT000032-matK.tsv.gz",
            "ERT000034-Dloop.tsv.gz",
            "ERT000035-IGS.tsv.gz",
            "ERT000036-MHC2.tsv.gz",
            "ERT000037-intron.tsv.gz",
            "ERT000038-hyloMarker.tsv.gz",
            "ERT000039-Sat.tsv.gz",
            "ERT000042-ncRNA.tsv.gz",
            "ERT000047-betasat.tsv.gz",
            "ERT000050-ISR.tsv.gz",
            "ERT000051-poly.tsv.gz",
            "ERT000052-ssRNA.tsv.gz",
            "ERT000053-ETS.tsv.gz",
            "ERT000054-prom.tsv.gz",
            "ERT000055-STS.tsv.gz",
            "ERT000056-mobele.tsv.gz",
            "ERT000057-alphasat.tsv.gz",
            "ERT000058-MLmarker.tsv.gz",
            "ERT000060-vUTR.tsv.gz"};


    private class ManifestBuilder {
        private String manifest;
        private final File inputDir;

        ManifestBuilder(File inputDir) {
            manifest =
                    "STUDY test\n" +
                            "NAME test\n";
            this.inputDir = inputDir;
        }

        ManifestBuilder field(String field, String value) {
            manifest += field + "\t" + value + "\n";
            return this;
        }

        ManifestBuilder gzipTempFile(String field, String fileName) {
            return field(field, WebinCliTestUtils.createEmptyGzippedTempFile(
                    fileName, inputDir.toPath()).getFileName().toString());
        }

        ManifestBuilder gzipTempFlatfile(String fileName) {
            return gzipTempFile("FLATFILE", fileName);
        }

        ManifestBuilder gzipTempTab(String fileName) {
            return gzipTempFile("TAB", fileName);
        }

        ManifestBuilder gzipTempFlatfile() {
            return gzipTempFile("FLATFILE", ".dat.gz");
        }

        ManifestBuilder gzipTempTab() {
            return gzipTempFile("TAB", ".tsv.gz");
        }

        ManifestBuilder gzipTempFiles(
                boolean flatfile,
                boolean tab) {
            if (flatfile) gzipTempFlatfile();
            if (tab) gzipTempTab();
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


    @Before
    public void
    before() {
        Locale.setDefault(Locale.UK);
        ValidationMessage.setDefaultMessageFormatter(ValidationMessage.TEXT_MESSAGE_FORMATTER_TRAILING_LINE_END);
        ValidationResult.setDefaultMessageFormatter(null);
    }


    private SequenceAssemblyWebinCli
    createTsvValidator(File file, FileType fileType) {
        SubmissionOptions options = new SubmissionOptions();
        if (file != null) {
            SubmissionFiles files = new SubmissionFiles();
            SubmissionFile SubmissionFile = new SubmissionFile(fileType, file);
            files.addFile(SubmissionFile);
            options.submissionFiles = Optional.of(files);
        }
        options.assemblyInfoEntry = Optional.of(new AssemblyInfoEntry());
        options.context = Optional.of(Context.sequence);
        options.isFixMode = true;
        options.isRemote = true;
        SequenceAssemblyWebinCli validator = new SequenceAssemblyWebinCli();
        validator.setTestMode(true);
        validator.setStudy(new Study());
        validator.setSubmitDir(submitDir);
        validator.setValidationDir(validationDir);
        validator.setSubmissionOptions(options);
        return validator;
    }

    private void assertTsvValidatorError(String tsvFile, String expectedReportFile, String actualReportFile) {
        String tsvFileDir = "uk/ac/ebi/ena/template/tsvfile/";

        File file = WebinCliTestUtils.getFile(tsvFileDir + tsvFile);
        SequenceAssemblyWebinCli validator = createTsvValidator(file, FileType.TSV);

        assertThatThrownBy(validator::validate).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("tsv file validation failed");

        try {
            Path expectedReportFilePath = WebinCliTestUtils.getPath(tsvFileDir + expectedReportFile);
            Path actualReportFilePath = validator.getValidationDir().toPath().resolve(actualReportFile);

            String expectedReport = new String(Files.readAllBytes(expectedReportFilePath)).replaceAll("\\s+", "");
            String actualReport = new String(Files.readAllBytes(actualReportFilePath), StandardCharsets.UTF_8).replaceAll("\\s+", "");

            assertThat(actualReport).isEqualTo(expectedReport);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private SequenceAssemblyWebinCli createValidator(File inputDir) {
        SequenceAssemblyWebinCli cli = new SequenceAssemblyWebinCli();
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

    private SequenceAssemblyWebinCli initValidator(Path manifestFile, SequenceAssemblyWebinCli validator) {
        validator.init(AssemblyTestUtils.createWebinCliParameters(manifestFile.toFile(), validator.getInputDir()));
        return validator;
    }

    private SequenceAssemblyWebinCli initValidatorThrows(Path manifestFile, SequenceAssemblyWebinCli validator) {
        assertThatThrownBy(() ->
                validator.init(AssemblyTestUtils.createWebinCliParameters(manifestFile.toFile(), validator.getInputDir())))
                .isInstanceOf(WebinCliException.class);
        return validator;
    }

    private void assertValidatorError(SequenceAssemblyWebinCli validator, WebinCliMessage message) {
        AssertionsForClassTypes.assertThat(validator.getManifestReader().getValidationResult().count(message.key(), Severity.ERROR)).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void
    testValidTsv() {
        for (String testTsvFile : VALID_TSV_FILES) {
            File file = WebinCliTestUtils.getFile("uk/ac/ebi/ena/template/tsvfile/" + testTsvFile);
            SequenceAssemblyWebinCli validator = createTsvValidator(file, FileType.TSV);
            validator.validate();
        }
    }

    @Test
    public void
    testInvalidMandatoryFieldsPresent() {
        assertTsvValidatorError(
                "Sequence-mandatory-field-missing.tsv.gz",
                "Sequence-mandatory-field-missing-expected-results.txt",
                "Sequence-mandatory-field-missing.tsv.gz.report");
    }

    @Test
    public void
    testInvalidAlphanumericEntrynumber() {
        assertTsvValidatorError("Sequence-invalid-alphanumeric-entrynumber-.tsv.gz",
                "Sequence-invalid-alphanumeric-entrynumber-expected-results.txt",
                "Sequence-invalid-alphanumeric-entrynumber-.tsv.gz.report");
    }

    @Test
    public void
    testInvalidMarker() {
        assertTsvValidatorError("Sequence-invalid-marker.tsv.gz",
                "Sequence-invalid-marker-expected-results.txt",
                "Sequence-invalid-marker.tsv.gz.report");
    }

    @Test
    public void
    testInvalidSediment() {
        assertTsvValidatorError("Sequence-invalid-sediment.tsv.gz",
                "Sequence-invalid-sediment-expected-results.txt",
                "Sequence-invalid-sediment.tsv.gz.report");
    }

    @Test
    public void
    testInvalidEntryNumberStart() {
        assertTsvValidatorError("Sequence-invalid-entrynumber-start.tsv.gz",
                "Sequence-invalid-entrynumber-start-expected-results.txt",
                "Sequence-invalid-entrynumber-start.tsv.gz.report");
    }

    @Test
    public void
    testInvalidNonAsciiCharacters() {
        assertTsvValidatorError("Sequence-non-ascii-characters.gz",
                "Sequence-non-ascii-characters-expected-results.txt",
                "Sequence-non-ascii-characters.gz.report");
    }


    @Test
    public void
    testFileGroup() {
        for (boolean flatfile : new boolean[]{false, true}) {
            for (boolean tab : new boolean[]{false, true}) {
                Path manifestFile = new ManifestBuilder(tempInputDir).gzipTempFiles(
                        flatfile,
                        tab).build();

                int cnt = (flatfile ? 1 : 0) +
                        (tab ? 1 : 0);

                if (cnt == 1) {
                    SequenceAssemblyWebinCli validator = initValidator(manifestFile, createValidator(tempInputDir));
                    SubmissionFiles submissionFiles = validator.getSubmissionOptions().submissionFiles.get();
                    AssertionsForClassTypes.assertThat(submissionFiles.getFiles(FileType.FLATFILE).size()).isEqualTo(flatfile ? 1 : 0);
                    AssertionsForClassTypes.assertThat(submissionFiles.getFiles(FileType.TSV).size()).isEqualTo(tab ? 1 : 0);
                } else if (cnt == 0) {
                    SequenceAssemblyWebinCli validator = initValidatorThrows(manifestFile, createValidator(tempInputDir));
                    assertValidatorError(validator, WebinCliMessage.Manifest.NO_DATA_FILES_ERROR);
                } else {
                    SequenceAssemblyWebinCli validator = initValidatorThrows(manifestFile, createValidator(tempInputDir));
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
                new ManifestBuilder(tempInputDir).gzipTempTab(".INVALID.gz").build(),
                // No .gz
                new ManifestBuilder(tempInputDir).gzipTempFlatfile(".txt").build(),
                new ManifestBuilder(tempInputDir).gzipTempTab(".tsv").build()};
        Arrays.stream(manifests).forEach(manifest -> {
            SequenceAssemblyWebinCli validator = initValidatorThrows(manifest, createValidator(tempInputDir));
            assertValidatorError(validator, WebinCliMessage.Manifest.INVALID_FILE_SUFFIX_ERROR);
        });
    }

    @Test
    public void
    testFileNoMoreThanOne() {
        Path manifests[] = new Path[]{
                new ManifestBuilder(tempInputDir).gzipTempFlatfile().gzipTempFlatfile().build(),
                new ManifestBuilder(tempInputDir).gzipTempTab().gzipTempTab().build()};
        Arrays.stream(manifests).forEach(manifest -> {
            SequenceAssemblyWebinCli validator = initValidatorThrows(manifest, createValidator(tempInputDir));
            assertValidatorError(validator, WebinCliMessage.Manifest.TOO_MANY_FIELDS_ERROR);
        });
    }
}
