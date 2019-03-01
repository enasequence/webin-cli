
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
package uk.ac.ebi.ena.webin.cli.manifest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.FileSuffixProcessor;

public class ManifestReaderTest {

    private final Path inputDir = Paths.get( "." );

    private final static ManifestCVList CV_FIELD_1 = new ManifestCVList( "VALUE1" );

    static class ManifestReaderOneMetaFieldMin0Max1 extends TestManifestReader {
        ManifestReaderOneMetaFieldMin0Max1() {
            super(new ArrayList<ManifestFieldDefinition>() {{
                add(new ManifestFieldDefinition("META_FIELD_1", ManifestFieldType.META, 0, 1, new CVFieldProcessor(CV_FIELD_1 )));
            }});
        }
    }

    static class ManifestReaderTwoMetaFieldMin1Max1 extends TestManifestReader {
        ManifestReaderTwoMetaFieldMin1Max1() {
            super(new ArrayList<ManifestFieldDefinition>() {{
                add(new ManifestFieldDefinition("META_FIELD_1", ManifestFieldType.META, 1, 1, new CVFieldProcessor(CV_FIELD_1 )));
                add(new ManifestFieldDefinition("META_FIELD_2", ManifestFieldType.META, 1, 1));
            }});
        }
    }

    static class ManifestReaderOneFileFieldMin0Max1 extends TestManifestReader {
        ManifestReaderOneFileFieldMin0Max1() {
            super(new ArrayList<ManifestFieldDefinition>() {{
                add(new ManifestFieldDefinition("FILE_FIELD_1", ManifestFieldType.FILE, 0, 1,
                        new FileSuffixProcessor( Arrays.asList(".txt"))));
            }});
        }
    }

    static class ManifestReaderFiles extends TestManifestReader {
        ManifestReaderFiles() {
            super(new ArrayList<ManifestFieldDefinition>() {{
                add(new ManifestFieldDefinition("FILE_FIELD_1", ManifestFieldType.FILE, 0, 1));
                add(new ManifestFieldDefinition("FILE_FIELD_2", ManifestFieldType.FILE, 0, 2));
                add(new ManifestFieldDefinition("FILE_FIELD_3", ManifestFieldType.FILE, 0, 1));
                add(new ManifestFieldDefinition("FILE_FIELD_4", ManifestFieldType.FILE, 0, 2));
            }},
            new HashSet<List<ManifestFileCount>>() {{
                add(new ArrayList<ManifestFileCount>() {{
                    add(new ManifestFileCount("FILE_FIELD_1", 1, 1));
                }});
                add(new ArrayList<ManifestFileCount>() {{
                    add(new ManifestFileCount("FILE_FIELD_2", 1, 2));
                }});
                add(new ArrayList<ManifestFileCount>() {{
                    add(new ManifestFileCount("FILE_FIELD_3", 1, 1));
                    add(new ManifestFileCount("FILE_FIELD_4", 2, 2));
                }});
            }});
        }
    }

    @Test public void ErrorReadingManifest() {
        ManifestReader reader = new ManifestReaderOneMetaFieldMin0Max1();
        File manifest = new File("MISSING_MANIFEST");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.READING_MANIFEST_FILE_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void ErrorReadingInfo() {
        ManifestReader reader = new ManifestReaderOneMetaFieldMin0Max1();
        File manifest = createManifest("INFO MISSING_INFO");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.READING_INFO_FILE_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void UnknownField() {
        ManifestReader reader = new ManifestReaderOneMetaFieldMin0Max1();
        File manifest = createManifest("UNKNOWN_FIELD_1 VALUE_1");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.UNKNOWN_FIELD_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void InvalidField() {
        ManifestReader reader = new ManifestReaderOneMetaFieldMin0Max1();
        File manifest = createManifest("META_FIELD_1 INVALID_VALUE");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.INVALID_FIELD_VALUE_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void MissingField() {
        ManifestReader reader = new ManifestReaderTwoMetaFieldMin1Max1();
        File manifest = createManifest("META_FIELD_1\nMETA_FIELD_2 \n");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.MISSING_MANDATORY_FIELD_ERROR.key(), Severity.ERROR), 2);
    }

    @Test public void TooManyFields() {
        ManifestReader reader = new ManifestReaderTwoMetaFieldMin1Max1();
        File manifest = createManifest("META_FIELD_1 VALUE1\nMETA_FIELD_2 VALUE2\nMETA_FIELD_2 VALUE3\n");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.TOO_MANY_FIELDS_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void InvalidFileField() {
        ManifestReader reader = new ManifestReaderOneFileFieldMin0Max1();
        File manifest = createManifest("FILE_FIELD_1 MISSING_FILE.txt\n");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.INVALID_FILE_FIELD_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void InvalidFileSuffix() {
        ManifestReader reader = new ManifestReaderOneFileFieldMin0Max1();
        File manifest = createManifest("FILE_FIELD_1 MISSING_FILE.bam\n");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.INVALID_FILE_SUFFIX_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void MissingFiles() {
        ManifestReader reader = new ManifestReaderFiles();
        File manifest = createManifest("\n");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.NO_DATA_FILES_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void ValidFileGroup() {
        ManifestReader reader = new ManifestReaderFiles();
        File manifest = createManifest("FILE_FIELD_1 MISSING_FILE");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR.key(), Severity.ERROR), 0);
    }

    @Test public void ValidFileGroup2() {
        ManifestReader reader = new ManifestReaderFiles();
        File manifest = createManifest("FILE_FIELD_2 MISSING_FILE\nFILE_FIELD_2 MISSING_FILE");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR.key(), Severity.ERROR), 0);
    }

    @Test public void ValidFileGroup3() {
        ManifestReader reader = new ManifestReaderFiles();
        File manifest = createManifest("FILE_FIELD_3 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR.key(), Severity.ERROR), 0);
    }

    @Test public void InvalidFileGroup_DifferentGroups() {
        ManifestReader reader = new ManifestReaderFiles();
        File manifest = createManifest("FILE_FIELD_1 MISSING_FILE\nFILE_FIELD_2 MISSING_FILE");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void InvalidFileGroup_TooManyFiles() {
        ManifestReader reader = new ManifestReaderFiles();
        File manifest = createManifest("FILE_FIELD_1 MISSING_FILE\nFILE_FIELD_1 MISSING_FILE");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR.key(), Severity.ERROR), 1);
    }

    @Test public void InvalidFileGroup_TooManyFiles2() {
        ManifestReader reader = new ManifestReaderFiles();
        File manifest = createManifest("FILE_FIELD_3 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE\nFILE_FIELD_4 MISSING_FILE");
        reader.readManifest(inputDir, manifest);
        Assert.assertEquals(reader.getValidationResult().count(WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR.key(), Severity.ERROR), 1);

    }

    private static File createManifest(String contents) {
        try {
            return Files.write(Files.createTempFile("TEMP", "MANIFEST"),
                    (contents).getBytes(),
                    StandardOpenOption.SYNC, StandardOpenOption.CREATE).toFile();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
