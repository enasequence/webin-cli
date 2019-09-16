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
package uk.ac.ebi.ena.webin.cli.context.reads;

import java.io.File;
import java.nio.file.Paths;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.*;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;
import static uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader.Field;

import static org.assertj.core.api.Assertions.assertThat;

public class
ReadsManifestReaderTest {
    private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/reads");

    private static ReadsManifestReader createManifestReader() {
        WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
        return new ReadsManifestReader(ManifestReader.DEFAULT_PARAMETERS, new MetadataProcessorFactory(parameters));
    }

    private static WebinCliExecutorBuilder<ReadsManifest, ReadsValidationResponse> executorBuilder =
            new WebinCliExecutorBuilder(
                    ReadsManifest.class, WebinCliExecutorBuilder.MetadataProcessorType.MOCK);

    private void assertManifestError(File manifestFile, String message) {
        WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
                executorBuilder.build(manifestFile, WebinCliTestUtils.createTempDir());

        assertThatThrownBy(executor::readManifest)
                .isInstanceOf(WebinCliException.class)
                .hasMessageStartingWith("Invalid manifest file");

        new ReportTester(executor).inManifestReport(message);
    }

    private void assertNoManifestError(File manifestFile, String message) {
        WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
                executorBuilder.build(manifestFile, RESOURCE_DIR);

        assertThatThrownBy(executor::readManifest)
                .isInstanceOf(WebinCliException.class)
                .hasMessageStartingWith("Invalid manifest file");

        new ReportTester(executor).notInManifestReport(message);
    }

    @Before
    public void
    before() {
        ValidationMessage.setDefaultMessageFormatter(ValidationMessage.TEXT_TIME_MESSAGE_FORMATTER_TRAILING_LINE_END);
        ValidationResult.setDefaultMessageFormatter(null);
        Locale.setDefault(Locale.UK);
    }

    @Test
    public void
    testValidManifest() {
        ReadsManifestReader manifestReader = createManifestReader();
        ReadsManifest manifest = manifestReader.getManifest();

        Assert.assertNull(manifest.getStudy());
        Assert.assertNull(manifest.getSample());
        Assert.assertNull(manifest.getPlatform());
        Assert.assertNull(manifest.getInstrument());
        Assert.assertNull(manifest.getLibraryStrategy());
        Assert.assertNull(manifest.getLibrarySource());
        Assert.assertNull(manifest.getLibrarySelection());
        Assert.assertNull(manifest.getLibraryName());
        Assert.assertNull(manifest.getLibraryConstructionProtocol());
        Assert.assertNull(manifest.getInsertSize());
        Assert.assertNull(manifest.getName());
        assertThat(manifest.files().files()).size().isZero();
        Assert.assertNull(manifest.getDescription());

        manifestReader.readManifest(Paths.get("."),
                new ManifestBuilder()
                        .field(Field.PLATFORM, " illumina")
                        .field(Field.INSTRUMENT, " Illumina HiScanSQ")
                        .field(Field.LIBRARY_STRATEGY, " CLONEEND")
                        .field(Field.LIBRARY_SOURCE, " OTHER")
                        .field(Field.LIBRARY_SELECTION, " Inverse rRNA selection")
                        .field(Field.LIBRARY_NAME, " Name library")
                        .field(Field.LIBRARY_CONSTRUCTION_PROTOCOL, " library construction protocol")
                        .field(Field.INSERT_SIZE, " 100500")
                        .field(Field.NAME, " SOME-FANCY-NAME")
                        .field(Field.DESCRIPTION, " description")
                        .file("BAM", TempFileBuilder.empty("bam"))
                        .build());

        Assert.assertEquals("ILLUMINA", manifest.getPlatform());
        Assert.assertEquals("Illumina HiScanSQ", manifest.getInstrument());
        Assert.assertEquals("CLONEEND", manifest.getLibraryStrategy());
        Assert.assertEquals("OTHER", manifest.getLibrarySource());
        Assert.assertEquals("Inverse rRNA selection", manifest.getLibrarySelection());
        Assert.assertEquals("Name library", manifest.getLibraryName());
        Assert.assertEquals("library construction protocol", manifest.getLibraryConstructionProtocol());
        Assert.assertEquals(Integer.valueOf(100500), manifest.getInsertSize());
        Assert.assertEquals("SOME-FANCY-NAME", manifest.getName());
        assertThat(manifest.files().files()).size().isOne();
        Assert.assertEquals("description", manifest.getDescription());
    }

    @Test
    public void
    missingInstrument() {
        ReadsManifestReader manifestReader = createManifestReader();
        ReadsManifest manifest = manifestReader.getManifest();

        Assert.assertNull(manifest.getPlatform());
        Assert.assertNull(manifest.getInstrument());

        manifestReader.readManifest(Paths.get("."),
                new ManifestBuilder()
                        .field(Field.PLATFORM, "illumina")
                        .build()
        );

        Assert.assertEquals("ILLUMINA", manifest.getPlatform());
        Assert.assertEquals("unspecified", manifest.getInstrument());
    }

    @Test
    public void
    unspecifiedInstrument() {
        ReadsManifestReader manifestReader = createManifestReader();
        ReadsManifest manifest = manifestReader.getManifest();

        Assert.assertNull(manifest.getPlatform());
        Assert.assertNull(manifest.getInstrument());

        manifestReader.readManifest(Paths.get("."),
                new ManifestBuilder()
                        .field(Field.PLATFORM, "ILLUMINA")
                        .field(Field.INSTRUMENT, "unspecifieD")
                        .build()
        );

        Assert.assertEquals("ILLUMINA", manifest.getPlatform());
        Assert.assertEquals("unspecified", manifest.getInstrument());
    }

    @Test
    public void
    platformOverride() {
        ReadsManifestReader manifestReader = createManifestReader();
        ReadsManifest manifest = manifestReader.getManifest();

        Assert.assertNull(manifest.getPlatform());
        Assert.assertNull(manifest.getInstrument());

        manifestReader.readManifest(Paths.get("."),
                new ManifestBuilder()
                        .field(Field.PLATFORM, "ILLUMINA")
                        .field(Field.INSTRUMENT, "454 GS FLX Titanium")
                        .build()
        );

        Assert.assertEquals("LS454", manifest.getPlatform());
        Assert.assertEquals("454 GS FLX Titanium", manifest.getInstrument());
    }

    @Test
    public void
    missingPlatformAndInstrument() {
        assertManifestError(
                new ManifestBuilder()
                        .build(),
                "ERROR: Platform and/or instrument should be defined");
    }

    @Test
    public void
    unspecifiedInstrumentMissingPlatform() {
        assertManifestError(
                new ManifestBuilder()
                        .field(Field.INSTRUMENT, "unspecified")
                        .build(),
                "ERROR: Platform and/or instrument should be defined");
    }

    @Test
    public void
    negativeInsertSize() {
        assertManifestError(
                new ManifestBuilder()
                        .field(Field.INSERT_SIZE, "-1")
                        .build(),
                "ERROR: Invalid INSERT_SIZE field value: \"-1\". Non-negative integer expected");
    }

    @Test
    public void
    invalidQualityScore() {
        assertManifestError(
                new ManifestBuilder()
                        .field(Field.QUALITY_SCORE, "PHRED_34")
                        .build(),
                "ERROR: Invalid QUALITY_SCORE field value");
    }

    @Test
    public void
    validQualityScore() {
        assertNoManifestError(
                new ManifestBuilder()
                        .field(Field.QUALITY_SCORE, "PHRED_33")
                        .build(),
                "ERROR: Invalid QUALITY_SCORE field value");
    }

    @Test
    public void
    dataFileIsMissing() {
        for (ReadsManifest.FileType fileType : ReadsManifest.FileType.values()) {
            assertManifestError(
                    new ManifestBuilder()
                            .file(fileType, "missing")
                            .build(),
                    "ERROR: Invalid " + fileType.name() + " file name");
        }
    }

    @Test
    public void
    dataFileIsDirectory() {
        File dir = WebinCliTestUtils.createTempDir();
        for (ReadsManifest.FileType fileType : ReadsManifest.FileType.values()) {
            assertManifestError(
                    new ManifestBuilder()
                            .file(fileType, dir)
                            .build(),
                    "ERROR: Invalid " + fileType.name() + " file name");
        }
    }

    @Test
    public void
    dataFileNoPath() {
        for (ReadsManifest.FileType fileType : ReadsManifest.FileType.values()) {
            assertManifestError(
                    new ManifestBuilder()
                            .file(fileType, "")
                            .build(),
                    "ERROR: No data files have been specified");
        }
    }

    @Test
    public void
    dataFileNonASCIIPath() {
        for (ReadsManifest.FileType fileType : ReadsManifest.FileType.values()) {
            assertManifestError(
                    new ManifestBuilder()
                            .file(fileType, TempFileBuilder.empty("Š"))
                            .build(),
                    "File name should conform following regular expression");
        }
    }
}
