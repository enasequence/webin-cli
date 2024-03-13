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
package uk.ac.ebi.ena.webin.cli.manifest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage.Severity;
import uk.ac.ebi.ena.webin.cli.validator.message.listener.MessageCounter;

public class ManifestReaderTest {

  private final Path inputDir = Paths.get(".");

  private static final ManifestCVList CV_FIELD_1 = new ManifestCVList("VALUE1");

  static class ManifestReaderOneMetaFieldMin0Max1 extends TestManifestReader {
    ManifestReaderOneMetaFieldMin0Max1() {
      super(
          new ManifestFieldDefinition.Builder()
              .meta()
              .optional()
              .name("META_FIELD_1")
              .desc("DESCRIPTION_1")
              .processor(new CVFieldProcessor(CV_FIELD_1))
              .build());
    }
  }

  static class ManifestReaderTwoMetaFieldMin1Max1 extends TestManifestReader {
    ManifestReaderTwoMetaFieldMin1Max1() {
      super(
          new ManifestFieldDefinition.Builder()
              .meta()
              .required()
              .name("META_FIELD_1")
              .desc("DESCRIPTION_1")
              .processor(new CVFieldProcessor(CV_FIELD_1))
              .and()
              .meta()
              .required()
              .name("META_FIELD_2")
              .desc("DESCRIPTION_2")
              .build());
    }
  }

  static class ManifestReaderOneFileFieldMin0Max1 extends TestManifestReader {
    ManifestReaderOneFileFieldMin0Max1() {
      super(
          new ManifestFieldDefinition.Builder()
              .file()
              .optional()
              .name("FILE_FIELD_1")
              .desc("DESCRIPTION_1")
              .processor(new FileSuffixProcessor(Arrays.asList(".txt")))
              .build());
    }
  }

  static class ManifestReaderFiles extends TestManifestReader {
    ManifestReaderFiles() {
      super(
          new ManifestFieldDefinition.Builder()
              .file()
              .optional()
              .name("FILE_FIELD_1")
              .desc("DESCRIPTION_1")
              .and()
              .file()
              .optional(2)
              .name("FILE_FIELD_2")
              .desc("DESCRIPTION_2")
              .and()
              .file()
              .optional()
              .name("FILE_FIELD_3")
              .desc("DESCRIPTION_3")
              .and()
              .file()
              .optional(2)
              .name("FILE_FIELD_4")
              .desc("DESCRIPTION_4")
              .build(),
          new ManifestFileCount.Builder()
              .group("TEST_GROUP_1")
              .required("FILE_FIELD_1")
              .and()
              .group("TEST_GROUP_2")
              .required("FILE_FIELD_2", 2)
              .and()
              .group("TEST_GROUP_3")
              .required("FILE_FIELD_3")
              .required("FILE_FIELD_4", 2, 2)
              .build());
    }
  }

  @Test
  public void ErrorReadingManifest() {
    ManifestReader reader = new ManifestReaderOneMetaFieldMin0Max1();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_MANIFEST_FILE_READ_ERROR.regex());
    reader.addListener(counter);
    File manifest = new File("MISSING_MANIFEST");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void ErrorReadingInfo() {
    ManifestReader reader = new ManifestReaderOneMetaFieldMin0Max1();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_INFO_FILE_READ_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("INFO MISSING_INFO");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void UnknownField() {
    ManifestReader reader = new ManifestReaderOneMetaFieldMin0Max1();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_UNKNOWN_FIELD_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("UNKNOWN_FIELD_1 VALUE_1");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void InvalidField() {
    ManifestReader reader = new ManifestReaderOneMetaFieldMin0Max1();
    MessageCounter counter =
        MessageCounter.regex(Severity.ERROR, WebinCliMessage.CV_FIELD_PROCESSOR_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("META_FIELD_1 INVALID_VALUE");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void MissingField() {
    ManifestReader reader = new ManifestReaderTwoMetaFieldMin1Max1();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_MISSING_MANDATORY_FIELD_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("META_FIELD_1\nMETA_FIELD_2 \n");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 2);
  }

  @Test
  public void TooManyFields() {
    ManifestReader reader = new ManifestReaderTwoMetaFieldMin1Max1();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_TOO_MANY_FIELDS_ERROR.regex());
    reader.addListener(counter);
    File manifest =
        createManifest("META_FIELD_1 VALUE1\nMETA_FIELD_2 VALUE2\nMETA_FIELD_2 VALUE3\n");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void InvalidFileField() {
    ManifestReader reader = new ManifestReaderOneFileFieldMin0Max1();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_INVALID_FILE_FIELD_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("FILE_FIELD_1 MISSING_FILE.txt\n");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void InvalidFileSuffix() {
    ManifestReader reader = new ManifestReaderOneFileFieldMin0Max1();
    MessageCounter counter =
        MessageCounter.regex(Severity.ERROR, WebinCliMessage.FILE_SUFFIX_PROCESSOR_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("FILE_FIELD_1 MISSING_FILE.bam\n");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void MissingFiles() {
    ManifestReader reader = new ManifestReaderFiles();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_NO_DATA_FILES_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("\n");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void ValidFileGroup() {
    ManifestReader reader = new ManifestReaderFiles();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("FILE_FIELD_1 MISSING_FILE");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 0);
  }

  @Test
  public void ValidFileGroup2() {
    ManifestReader reader = new ManifestReaderFiles();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("FILE_FIELD_2 MISSING_FILE\nFILE_FIELD_2 MISSING_FILE");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 0);
  }

  @Test
  public void ValidFileGroup3() {
    ManifestReader reader = new ManifestReaderFiles();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR.regex());
    reader.addListener(counter);
    File manifest =
        createManifest(
            "FILE_FIELD_3 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 0);
  }

  @Test
  public void InvalidFileGroup_DifferentGroups() {
    ManifestReader reader = new ManifestReaderFiles();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("FILE_FIELD_1 MISSING_FILE\nFILE_FIELD_2 MISSING_FILE");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void InvalidFileGroup_TooManyFiles() {
    ManifestReader reader = new ManifestReaderFiles();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR.regex());
    reader.addListener(counter);
    File manifest = createManifest("FILE_FIELD_1 MISSING_FILE\nFILE_FIELD_1 MISSING_FILE");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void InvalidFileGroup_TooManyFiles2() {
    ManifestReader reader = new ManifestReaderFiles();
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR.regex());
    reader.addListener(counter);
    File manifest =
        createManifest(
            "FILE_FIELD_3 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE");
    reader.readManifest(inputDir, manifest);
    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void InvalidNonUniqueFileNames() {
    MessageCounter counter =
        MessageCounter.regex(
            Severity.ERROR, WebinCliMessage.MANIFEST_READER_INVALID_FILE_NON_UNIQUE_NAMES.text());

    ManifestReader reader = new ManifestReaderFiles();
    reader.addListener(counter);

    File manifest =
        createManifest(
            "FILE_FIELD_2 file-name-1\n"
                + "FILE_FIELD_2 dir1/file-name-1\n"
                + "FILE_FIELD_4 file-name-2\n"
                + "FILE_FIELD_4 dir1/dir2/file-name-2");
    reader.readManifest(inputDir, manifest);

    Assert.assertEquals(counter.getCount(), 1);
  }

  @Test
  public void testKeyValueManifestFileWithPunctuations() {
    TestManifestReader manifestReader =
        new TestManifestReader(
            new ManifestFieldDefinition.Builder()
                .meta()
                .required()
                .name("FIELD_NAME_1")
                .desc("some desc")
                .and()
                .meta()
                .required()
                .name("FIELD_NAME_2")
                .desc("some desc")
                .and()
                .meta()
                .required()
                .name("FIELD_NAME_3")
                .desc("some desc")
                .build());

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field("fieldName1", "val1")
            .field("field_name_2", "val2")
            .field("field-name-3", "val3")
            .build());

    Assert.assertEquals(
        manifestReader.getManifestReaderResult().getField("FIELD_NAME_1").getValue(), "val1");
    Assert.assertEquals(
        manifestReader.getManifestReaderResult().getField("FIELD_NAME_2").getValue(), "val2");
    Assert.assertEquals(
        manifestReader.getManifestReaderResult().getField("FIELD_NAME_3").getValue(), "val3");
  }

  private static File createManifest(String contents) {
    try {
      return Files.write(
              Files.createTempFile("TEMP", "MANIFEST"),
              (contents).getBytes(),
              StandardOpenOption.SYNC,
              StandardOpenOption.CREATE)
          .toFile();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
