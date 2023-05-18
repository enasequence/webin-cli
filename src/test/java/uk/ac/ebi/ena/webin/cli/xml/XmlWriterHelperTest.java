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
package uk.ac.ebi.ena.webin.cli.xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.utils.FileUtils;

public class XmlWriterHelperTest {

    @Test
    public void testFileNameDoesNotHaveExtraDirectories() throws IOException {
        Path inputDir = Files.createTempDirectory("test-dir-");
        Path uploadDir = Paths.get("webin-cli/context/submission-01");
        Path file = Files.createTempFile(Files.createDirectories(inputDir.resolve("dir1").resolve("dir2"))
            , "test-file-", ".temp");

        Element fileElement = XmlWriterHelper.createFileElement(inputDir, uploadDir, file, "FASTQ");

        String expectedFileName = FileUtils.replaceIncompatibleFileSeparators(uploadDir.resolve(file.getFileName()).toString());

        String actualFileName = fileElement.getAttribute("filename").getValue();

        Assert.assertEquals(expectedFileName, actualFileName);
    }
}
