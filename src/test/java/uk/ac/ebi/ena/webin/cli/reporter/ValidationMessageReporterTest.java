package uk.ac.ebi.ena.webin.cli.reporter;

import org.junit.Test;
import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.WebinCliTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ValidationMessageReporterTest {

    @Test
    public void
    testWriteString() throws IOException
    {
        Path reportFile = WebinCliTestUtils.createEmptyTempFile("test");
        ValidationMessageReporter reporter = new ValidationMessageReporter(reportFile.toFile());

        reporter.write(Severity.ERROR, "MESSAGE1" );
        reporter.write(Severity.ERROR, "MESSAGE2" );
        reporter.write(Severity.INFO, "MESSAGE3" );
        reporter.write(Severity.INFO, "MESSAGE4" );

        List<String> lines = Files.readAllLines(reportFile);
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
        Path reportFile = WebinCliTestUtils.createEmptyTempFile("test");
        ValidationMessageReporter reporter = new ValidationMessageReporter(reportFile.toFile());

        reporter.write(ValidationMessageReporter.createValidationMessage(Severity.ERROR, "MESSAGE1"));
        reporter.write(ValidationMessageReporter.createValidationMessage(Severity.ERROR, "MESSAGE2"));
        reporter.write(ValidationMessageReporter.createValidationMessage(Severity.INFO, "MESSAGE3"));
        reporter.write(ValidationMessageReporter.createValidationMessage(Severity.INFO, "MESSAGE4"));

        List<String> lines = Files.readAllLines(reportFile);
        assertThat(lines.size()).isEqualTo(4);
        assertThat(lines.get(0)).endsWith("ERROR: MESSAGE1");
        assertThat(lines.get(1)).endsWith("ERROR: MESSAGE2");
        assertThat(lines.get(2)).endsWith("INFO: MESSAGE3");
        assertThat(lines.get(3)).endsWith("INFO: MESSAGE4");

        // With origin.

        reportFile = WebinCliTestUtils.createEmptyTempFile("test");
        reporter = new ValidationMessageReporter(reportFile.toFile());

        Origin origin = new DefaultOrigin("ORIGIN2");
        reporter.write(ValidationMessageReporter.createValidationMessage(Severity.ERROR, "MESSAGE1", origin));
        reporter.write(ValidationMessageReporter.createValidationMessage(Severity.ERROR, "MESSAGE2", origin));
        reporter.write(ValidationMessageReporter.createValidationMessage(Severity.INFO, "MESSAGE3", origin));
        reporter.write(ValidationMessageReporter.createValidationMessage(Severity.INFO, "MESSAGE4", origin));

        lines = Files.readAllLines(reportFile);
        assertThat(lines.size()).isEqualTo(4);
        assertThat(lines.get(0)).endsWith("ERROR: MESSAGE1 [ORIGIN2]");
        assertThat(lines.get(1)).endsWith("ERROR: MESSAGE2 [ORIGIN2]");
        assertThat(lines.get(2)).endsWith("INFO: MESSAGE3 [ORIGIN2]");
        assertThat(lines.get(3)).endsWith("INFO: MESSAGE4 [ORIGIN2]");
    }
}
