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
package uk.ac.ebi.ena.webin.cli.message;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.ac.ebi.ena.webin.cli.message.ValidationReporter.formatForLog;
import static uk.ac.ebi.ena.webin.cli.message.ValidationReporter.formatForReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.TempFileBuilder;

public class ValidationReporterTest {

    @Test
    public void
    testFormatForReport() {
        ValidationMessage error = ValidationMessage.error("MESSAGE1");
        ValidationMessage info = ValidationMessage.info("MESSAGE2");

        assertThat(formatForReport(error)).isEqualTo("ERROR: MESSAGE1");
        assertThat(formatForReport(info)).isEqualTo("INFO: MESSAGE2");

        String origin = "ORIGIN";
        error.addOrigin(origin);
        info.addOrigin(origin);
        assertThat(formatForReport(error)).isEqualTo("ERROR: MESSAGE1 [ORIGIN]");
        assertThat(formatForReport(info)).isEqualTo("INFO: MESSAGE2 [ORIGIN]");

        origin = "ORIGIN2";
        error.addOrigin(origin);
        info.addOrigin(origin);
        assertThat(formatForReport(error)).isEqualTo("ERROR: MESSAGE1 [ORIGIN, ORIGIN2]");
        assertThat(formatForReport(info)).isEqualTo("INFO: MESSAGE2 [ORIGIN, ORIGIN2]");
    }

    @Test
    public void
    testFormatForLog() {
        ValidationMessage error = ValidationMessage.error("MESSAGE1");
        ValidationMessage info = ValidationMessage.info("MESSAGE2");

        assertThat(formatForLog(error)).isEqualTo("MESSAGE1");
        assertThat(formatForLog(info)).isEqualTo("MESSAGE2");

        String origin = "ORIGIN";
        error.addOrigin(origin);
        info.addOrigin(origin);
        assertThat(formatForLog(error)).isEqualTo("MESSAGE1 [ORIGIN]");
        assertThat(formatForLog(info)).isEqualTo("MESSAGE2 [ORIGIN]");

        origin = "ORIGIN2";
        error.addOrigin(origin);
        info.addOrigin(origin);
        assertThat(formatForLog(error)).isEqualTo("MESSAGE1 [ORIGIN, ORIGIN2]");
        assertThat(formatForLog(info)).isEqualTo("MESSAGE2 [ORIGIN, ORIGIN2]");
    }

    @Test
    public void
    testWriteString() throws IOException
    {
        Path reportFile = TempFileBuilder.empty("test");
        ValidationReporter reporter = new ValidationReporter(reportFile.toFile());

        reporter.write(ValidationMessage.Severity.ERROR, "MESSAGE1" );
        reporter.write(ValidationMessage.Severity.ERROR, "MESSAGE2" );
        reporter.write(ValidationMessage.Severity.INFO, "MESSAGE3" );
        reporter.write(ValidationMessage.Severity.INFO, "MESSAGE4" );
        reporter.close();

        List<String> lines = Files.readAllLines(reportFile);
        System.out.println(lines);
        assertThat(lines.size()).isEqualTo(4);
        assertThat(lines.get(0)).endsWith("ERROR: MESSAGE1");
        assertThat(lines.get(1)).endsWith("ERROR: MESSAGE2");
        assertThat(lines.get(2)).endsWith("INFO: MESSAGE3");
        assertThat(lines.get(3)).endsWith("INFO: MESSAGE4");
    }

    @Test
    public void
    testWriteValidationMessage() throws IOException
    {
        Path reportFile = TempFileBuilder.empty("test");
        ValidationReporter reporter = new ValidationReporter(reportFile.toFile());

        reporter.write(ValidationMessage.error("MESSAGE1"));
        reporter.write(ValidationMessage.error("MESSAGE2"));
        reporter.write(ValidationMessage.info("MESSAGE3"));
        reporter.write(ValidationMessage.info("MESSAGE4"));
        reporter.close();

        List<String> lines = Files.readAllLines(reportFile);
        assertThat(lines.size()).isEqualTo(4);
        assertThat(lines.get(0)).endsWith("ERROR: MESSAGE1");
        assertThat(lines.get(1)).endsWith("ERROR: MESSAGE2");
        assertThat(lines.get(2)).endsWith("INFO: MESSAGE3");
        assertThat(lines.get(3)).endsWith("INFO: MESSAGE4");

        // With origin.

        reportFile = TempFileBuilder.empty("test");
        reporter = new ValidationReporter(reportFile.toFile());

        String origin = "ORIGIN";
        reporter.write(ValidationMessage.error("MESSAGE1").addOrigin(origin));
        reporter.write(ValidationMessage.error("MESSAGE2").addOrigin(origin));
        reporter.write(ValidationMessage.info("MESSAGE3").addOrigin(origin));
        reporter.write(ValidationMessage.info("MESSAGE4").addOrigin(origin));

        lines = Files.readAllLines(reportFile);
        assertThat(lines.size()).isEqualTo(4);
        assertThat(lines.get(0)).endsWith("ERROR: MESSAGE1 [ORIGIN]");
        assertThat(lines.get(1)).endsWith("ERROR: MESSAGE2 [ORIGIN]");
        assertThat(lines.get(2)).endsWith("INFO: MESSAGE3 [ORIGIN]");
        assertThat(lines.get(3)).endsWith("INFO: MESSAGE4 [ORIGIN]");
    }
}
