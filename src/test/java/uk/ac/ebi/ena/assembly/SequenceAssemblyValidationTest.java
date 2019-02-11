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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
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

public class SequenceAssemblyValidationTest {

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

    @Before
    public void
    before() {
        Locale.setDefault(Locale.UK);
        ValidationMessage.setDefaultMessageFormatter(ValidationMessage.TEXT_MESSAGE_FORMATTER_TRAILING_LINE_END);
        ValidationResult.setDefaultMessageFormatter(null);
    }

    @Test
    public void
    validTemplates() {
        for (String testTsvFile : VALID_TSV_FILES) {
            File file = WebinCliTestUtils.getFile("uk/ac/ebi/ena/template/tsvfile/" + testTsvFile);
            SequenceAssemblyWebinCli validator = getValidator(file, FileType.TSV);
            validator.validate();
        }
    }

    private void assertThatTsvFileIsInvalid(String tsvFile, String expectedReportFile, String actualReportFile) {
        String tsvFileDir = "uk/ac/ebi/ena/template/tsvfile/";

        File file = WebinCliTestUtils.getFile(tsvFileDir + tsvFile);
        SequenceAssemblyWebinCli validator = getValidator(file, FileType.TSV);

        assertThatThrownBy(() -> {
            validator.validate();
        }).isInstanceOf(WebinCliException.class)
                .hasMessageContaining("tsv file validation failed");

        try {
            Path expectedReportFilePath = WebinCliTestUtils.getPath(tsvFileDir + expectedReportFile);
            Path actualReportFilePath = validator.getValidationDir().toPath().resolve(actualReportFile);

            String expectedReport = new String(Files.readAllBytes(expectedReportFilePath)).replaceAll("\\s+", "");
            String actualReport = new String(Files.readAllBytes(actualReportFilePath), StandardCharsets.UTF_8).replaceAll("\\s+", "");

            assertThat(actualReport).isEqualTo(expectedReport);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void
    invalidMandatoryFieldsPresent() {
        assertThatTsvFileIsInvalid(
                "Sequence-mandatory-field-missing.tsv.gz",
                "Sequence-mandatory-field-missing-expected-results.txt",
                "Sequence-mandatory-field-missing.tsv.gz.report");
    }

    @Test
    public void
    invalidAlphanumericEntrynumber() {
         assertThatTsvFileIsInvalid("Sequence-invalid-alphanumeric-entrynumber-.tsv.gz",
                 "Sequence-invalidAlphanumericEntrynumber-expected-results.txt",
                 "Sequence-invalid-alphanumeric-entrynumber-.tsv.gz.report");
    }

    @Test
    public void
    invalidMarker() {
        assertThatTsvFileIsInvalid("Sequence-invalid-marker.tsv.gz",
        "Sequence-invalidMarker-expected-results.txt",
        "Sequence-invalid-marker.tsv.gz.report");
    }

    @Test
    public void
    invalidSediment() {
         assertThatTsvFileIsInvalid("Sequence-invalid-sediment.tsv.gz",
        "Sequence-invalidSediment-expected-results.txt",
        "Sequence-invalid-sediment.tsv.gz.report");
    }

    @Test
    public void
    invalidEntryNumberStart() {
         assertThatTsvFileIsInvalid("Sequence-invalid-entrynumber-start-.tsv.gz",
        "Sequence-invalidEntrynumberStart-expected-results.txt",
        "Sequence-invalid-entrynumber-start-.tsv.gz.report");
    }

    @Test
    public void
    invalidNonAsciiCharacters() {
        assertThatTsvFileIsInvalid("Sequence-non-ascii-characters.gz",
        "Sequence-nonAsciiCharacters-expected-results.txt",
        "Sequence-non-ascii-characters.gz.report");
    }

    private SequenceAssemblyWebinCli
    getValidator(File file, FileType fileType) {
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
        validator.setSubmitDir(WebinCliTestUtils.createTempDir());
        validator.setSubmissionOptions(options);
        validator.setValidationDir(WebinCliTestUtils.createTempDir());
        return validator;
    }

}
