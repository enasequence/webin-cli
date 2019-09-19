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
package uk.ac.ebi.ena.webin.cli;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.ebi.ena.webin.cli.WebinCli.getSafeOutputDir;
import static uk.ac.ebi.ena.webin.cli.WebinCli.getSafeOutputDirs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.io.File;
import java.util.UUID;

public class WebinCliTest {

    @Test
    public void testGetSafeOutputDir() {
        assertThat("A_aZ").isEqualTo(getSafeOutputDir("A aZ"));
        assertThat("A_a_Z").isEqualTo(getSafeOutputDir("A a Z"));
        assertThat("A_a_Z").isEqualTo(getSafeOutputDir("A  a   Z"));
        assertThat("AaZ").isEqualTo(getSafeOutputDir("AaZ"));
        assertThat("A_AA").isEqualTo(getSafeOutputDir("A&AA"));
        assertThat("A.AA").isEqualTo(getSafeOutputDir("A.AA"));
        assertThat("A-AA").isEqualTo(getSafeOutputDir("A-AA"));
        assertThat("A_AA").isEqualTo(getSafeOutputDir("A_____AA"));
        assertThat("AA").isEqualTo(getSafeOutputDir("_____AA"));
        assertThat("AA").isEqualTo(getSafeOutputDir("AA_____"));
        assertThat("_").isEqualTo(getSafeOutputDir("_______"));
        assertThat(".").isEqualTo(getSafeOutputDir("."));
    }

    @Test
    public void testGetSafeOutputDirs() {
        assertThat(".").isEqualTo(getSafeOutputDirs(".", "E_vermicularis_upd")[0]);
        assertThat("E_vermicularis_upd").isEqualTo(getSafeOutputDirs(".", "E_vermicularis_upd")[1]);
        assertThat("AaZ").isEqualTo(getSafeOutputDirs("AaZ", "AaZ")[0]);
        assertThat("A.AA").isEqualTo(getSafeOutputDirs("AaZ", "A.AA")[1]);
    }

    @Test
    public void testInputDirIsAFileError() {
        WebinCliCommand cmd = new WebinCliCommand();
        cmd.inputDir = TempFileBuilder.file("test").toFile();
        cmd.outputDir = WebinCliTestUtils.createTempDir();

        assertThatThrownBy(() -> new WebinCli(cmd) )
                .isInstanceOf(WebinCliException.class)
                .hasMessage(
                        WebinCliMessage.CLI_INPUT_PATH_NOT_DIR.format(
                                cmd.inputDir.getAbsoluteFile()));
    }

    @Test
    public void testOutputDirIsAFileError() {
        WebinCliCommand cmd = new WebinCliCommand();
        cmd.inputDir = WebinCliTestUtils.createTempDir();
        cmd.outputDir = TempFileBuilder.file("test").toFile();

        assertThatThrownBy(() -> new WebinCli(cmd) )
                .isInstanceOf(WebinCliException.class)
                .hasMessage(
                    WebinCliMessage.CLI_OUTPUT_PATH_NOT_DIR.format(
                                cmd.outputDir.getAbsoluteFile()));
    }

    @Test
    public void testInputDirMissingError() {
        WebinCliCommand cmd = new WebinCliCommand();
        cmd.inputDir = new File(UUID.randomUUID().toString());
        cmd.outputDir = WebinCliTestUtils.createTempDir();

        assertThatThrownBy(() -> new WebinCli(cmd) )
                .isInstanceOf(WebinCliException.class)
                .hasMessage(
                        WebinCliMessage.CLI_INPUT_PATH_NOT_DIR.format(
                                cmd.inputDir.getName()));
    }

    @Test
    public void testOutputDirMissingError() {
        WebinCliCommand cmd = new WebinCliCommand();
        cmd.inputDir = WebinCliTestUtils.createTempDir();
        cmd.outputDir = new File(UUID.randomUUID().toString());

        assertThatThrownBy(() -> new WebinCli(cmd) )
                .isInstanceOf(WebinCliException.class)
                .hasMessage(
                        WebinCliMessage.CLI_OUTPUT_PATH_NOT_DIR.format(
                                cmd.outputDir.getName()));
    }
}
