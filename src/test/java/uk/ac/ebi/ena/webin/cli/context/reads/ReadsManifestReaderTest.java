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
package uk.ac.ebi.ena.webin.cli.context.reads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;
import static uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader.Field;

import java.io.File;
import java.nio.file.Paths;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.ReportTester;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutorBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

public class ReadsManifestReaderTest {
  private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/reads");

  private static ReadsManifestReader createManifestReader() {
    WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    return new ReadsManifestReader(parameters, new MetadataProcessorFactory(parameters));
  }

  private static WebinCliExecutorBuilder<ReadsManifest, ReadsValidationResponse> executorBuilder =
      new WebinCliExecutorBuilder(
          ReadsManifest.class, WebinCliExecutorBuilder.MetadataProcessorType.MOCK);

  private void assertErrorTextInManifestReport(File manifestFile, String message) {
    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, WebinCliTestUtils.createTempDir());

    assertThatThrownBy(executor::readManifest)
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");

    new ReportTester(executor).textInManifestReport(message);
  }

  private void assertErrorRegexInManifestReport(File manifestFile, String regex) {
    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, WebinCliTestUtils.createTempDir());

    assertThatThrownBy(executor::readManifest)
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");

    new ReportTester(executor).regexInManifestReport(regex);
  }

  private void assertErrorTextNotInManifestError(File manifestFile, String message) {
    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.build(manifestFile, RESOURCE_DIR);

    assertThatThrownBy(executor::readManifest)
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");

    new ReportTester(executor).textNotInManifestReport(message);
  }

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidManifest() {
    ReadsManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(Field.PLATFORM, " illumina")
            .field(Field.INSTRUMENT, " Illumina HiScanSQ")
            .field(Field.LIBRARY_STRATEGY, " CLONEEND")
            .field(Field.LIBRARY_SOURCE, " OTHER")
            .field(Field.LIBRARY_SELECTION, " Inverse rRNA selection")
            .field(Field.LIBRARY_NAME, " Name library")
            .field(Field.LIBRARY_CONSTRUCTION_PROTOCOL, " library construction protocol")
            .field(Field.INSERT_SIZE, " 100500")
            .field(ManifestReader.Fields.NAME, " SOME-FANCY-NAME")
            .field(Field.DESCRIPTION, " description")
            .file("BAM", TempFileBuilder.empty("bam"))
            .field(ManifestReader.Fields.SUBMISSION_TOOL, "ST-001")
            .field(ManifestReader.Fields.SUBMISSION_TOOL_VERSION, "STV-001")
            .build());

    ReadsManifest manifest = manifestReader.getManifests().stream().findFirst().get();

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
    Assert.assertEquals("ST-001", manifest.getSubmissionTool());
    Assert.assertEquals("STV-001", manifest.getSubmissionToolVersion());
  }

  @Test
  public void missingInstrument() {
    ReadsManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "TEST")
            .field(Field.PLATFORM, "illumina")
            .build());

    ReadsManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    Assert.assertEquals("ILLUMINA", manifest.getPlatform());
    Assert.assertEquals("unspecified", manifest.getInstrument());
  }

  @Test
  public void unspecifiedInstrument() {
    ReadsManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "TEST")
            .field(Field.PLATFORM, "ILLUMINA")
            .field(Field.INSTRUMENT, "unspecifieD")
            .build());

    ReadsManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    Assert.assertEquals("ILLUMINA", manifest.getPlatform());
    Assert.assertEquals("unspecified", manifest.getInstrument());
  }

  @Test
  public void platformOverride() {
    ReadsManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "TEST")
            .field(Field.PLATFORM, "ILLUMINA")
            .field(Field.INSTRUMENT, "454 GS FLX Titanium")
            .build());

    ReadsManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    Assert.assertEquals("LS454", manifest.getPlatform());
    Assert.assertEquals("454 GS FLX Titanium", manifest.getInstrument());
  }

  @Test
  public void missingPlatformAndInstrument() {
    assertErrorRegexInManifestReport(
        new ManifestBuilder().field(ManifestReader.Fields.NAME, "TEST").build(),
        WebinCliMessage.READS_MANIFEST_READER_MISSING_PLATFORM_AND_INSTRUMENT_ERROR.regex());
  }

  @Test
  public void unspecifiedInstrumentMissingPlatform() {
    assertErrorRegexInManifestReport(
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "TEST")
            .field(Field.INSTRUMENT, "unspecified")
            .build(),
        WebinCliMessage.READS_MANIFEST_READER_MISSING_PLATFORM_AND_INSTRUMENT_ERROR.regex());
  }

  @Test
  public void negativeInsertSize() {
    assertErrorRegexInManifestReport(
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "TEST")
            .field(Field.INSERT_SIZE, "-1")
            .build(),
        WebinCliMessage.MANIFEST_READER_INVALID_POSITIVE_INTEGER_ERROR.regex());
  }

  @Test
  public void invalidQualityScore() {
    assertErrorTextInManifestReport(
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "TEST")
            .field(Field.QUALITY_SCORE, "PHRED_34")
            .build(),
        "ERROR: Invalid QUALITY_SCORE field value");
  }

  @Test
  public void validQualityScore() {
    assertErrorTextNotInManifestError(
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "TEST")
            .field(Field.QUALITY_SCORE, "PHRED_33")
            .build(),
        "ERROR: Invalid QUALITY_SCORE field value");
  }

  @Test
  public void dataFileIsMissing() {
    for (ReadsManifest.FileType fileType : ReadsManifest.FileType.values()) {
      assertErrorRegexInManifestReport(
          new ManifestBuilder()
              .field(ManifestReader.Fields.NAME, "TEST")
              .file(fileType, "missing")
              .build(),
          WebinCliMessage.MANIFEST_READER_INVALID_FILE_FIELD_ERROR.regex());
    }
  }

  @Test
  public void dataFileIsDirectory() {
    File dir = WebinCliTestUtils.createTempDir();
    for (ReadsManifest.FileType fileType : ReadsManifest.FileType.values()) {
      assertErrorRegexInManifestReport(
          new ManifestBuilder()
              .field(ManifestReader.Fields.NAME, "TEST")
              .file(fileType, dir)
              .build(),
          WebinCliMessage.MANIFEST_READER_INVALID_FILE_FIELD_ERROR.regex());
    }
  }

  @Test
  public void dataFileNoPath() {
    for (ReadsManifest.FileType fileType : ReadsManifest.FileType.values()) {
      assertErrorTextInManifestReport(
          new ManifestBuilder()
              .field(ManifestReader.Fields.NAME, "TEST")
              .file(fileType, "")
              .build(),
          "ERROR: No data files have been specified");
    }
  }

  @Test
  public void dataFileNonASCIIPath() {
    for (ReadsManifest.FileType fileType : ReadsManifest.FileType.values()) {
      assertErrorTextInManifestReport(
          new ManifestBuilder()
              .field(ManifestReader.Fields.NAME, "TEST")
              .file(fileType, TempFileBuilder.empty("Š"))
              .build(),
          "File name should conform following regular expression");
    }
  }

  @Test
  public void testValidReadsManifestWithMultipleFastqs() {
    ReadsManifestReader manifestReader = createManifestReader();
    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .jsonFormat()
            .field(Field.PLATFORM, "illumina")
            .field(Field.INSTRUMENT, "Illumina HiScanSQ")
            .field(Field.LIBRARY_STRATEGY, "CLONEEND")
            .field(Field.LIBRARY_SOURCE, "OTHER")
            .field(Field.LIBRARY_SELECTION, "Inverse rRNA selection")
            .field(Field.LIBRARY_NAME, "Name library")
            .field(Field.LIBRARY_CONSTRUCTION_PROTOCOL, "library construction protocol")
            .field(Field.INSERT_SIZE, "100500")
            .field(ManifestReader.Fields.NAME, "SOME-FANCY-NAME")
            .field(Field.DESCRIPTION, "description")
            .field(ManifestReader.Fields.SUBMISSION_TOOL, "ST-001")
            .field(ManifestReader.Fields.SUBMISSION_TOOL_VERSION, "STV-001")
            .file("FASTQ", TempFileBuilder.empty("fastq"))
            .attribute("READ_TYPE", "paired")
            .attribute("READ_TYPE", "sample_barcode")
            .file("FASTQ", TempFileBuilder.empty("fastq"))
            .attribute("READ_TYPE", "paired")
            .file("FASTQ", TempFileBuilder.empty("fastq"))
            .attribute("READ_TYPE", "cell_barcode")
            .build());

    ReadsManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    Assert.assertEquals("ILLUMINA", manifest.getPlatform());
    Assert.assertEquals("Illumina HiScanSQ", manifest.getInstrument());
    Assert.assertEquals("CLONEEND", manifest.getLibraryStrategy());
    Assert.assertEquals("OTHER", manifest.getLibrarySource());
    Assert.assertEquals("Inverse rRNA selection", manifest.getLibrarySelection());
    Assert.assertEquals("Name library", manifest.getLibraryName());
    Assert.assertEquals("library construction protocol", manifest.getLibraryConstructionProtocol());
    Assert.assertEquals(Integer.valueOf(100500), manifest.getInsertSize());
    Assert.assertEquals("SOME-FANCY-NAME", manifest.getName());
    Assert.assertEquals("description", manifest.getDescription());
    Assert.assertEquals("ST-001", manifest.getSubmissionTool());
    Assert.assertEquals("STV-001", manifest.getSubmissionToolVersion());
    assertThat(manifest.files().files()).size().isEqualTo(3);
    Assert.assertTrue(
        manifest.files().get().get(0).getAttributes().stream()
            .anyMatch(att -> att.getValue().equals("paired")));
    Assert.assertTrue(
        manifest.files().get().get(0).getAttributes().stream()
            .anyMatch(att -> att.getValue().equals("sample_barcode")));
    Assert.assertTrue(
        manifest.files().get().get(1).getAttributes().stream()
            .anyMatch(att -> att.getValue().equals("paired")));
    Assert.assertTrue(
        manifest.files().get().get(2).getAttributes().stream()
            .anyMatch(att -> att.getValue().equals("cell_barcode")));
  }
}
