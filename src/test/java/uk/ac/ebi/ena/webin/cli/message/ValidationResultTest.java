package uk.ac.ebi.ena.webin.cli.message;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.message.ValidationMessage.Severity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.message.ValidationResult.formatForLog;
import static uk.ac.ebi.ena.webin.cli.message.ValidationResult.formatForReport;

public class ValidationResultTest {

    @Test
    public void testCount() {
        ValidationResult result = new ValidationResult();

        result.add(new ValidationMessage(Severity.ERROR, "Test"));
        assertThat(result.count()).isOne();
        assertThat(result.count(Severity.ERROR)).isOne();

        result.add(new ValidationMessage(Severity.INFO, "Test"));
        assertThat(result.count()).isEqualTo(2);
        assertThat(result.count(Severity.ERROR)).isOne();
        assertThat(result.count(Severity.INFO)).isOne();

        result.add(new ValidationMessage(Severity.ERROR, "Test"));
        assertThat(result.count()).isEqualTo(3);
        assertThat(result.count(Severity.ERROR)).isEqualTo(2);
        assertThat(result.count(Severity.INFO)).isOne();

        result.add(new ValidationMessage(Severity.INFO, "Test"));
        assertThat(result.count()).isEqualTo(4);
        assertThat(result.count(Severity.ERROR)).isEqualTo(2);
        assertThat(result.count(Severity.INFO)).isEqualTo(2);
    }

    @Test
    public void testCountParent() {
        ValidationResult result1 = new ValidationResult();
        ValidationResult result2 = result1.create();

        result2.add(new ValidationMessage(Severity.ERROR, "Test"));
        assertThat(result1.count()).isOne();
        assertThat(result1.count(Severity.ERROR)).isOne();
        assertThat(result2.count()).isOne();
        assertThat(result2.count(Severity.ERROR)).isOne();

        result2.add(new ValidationMessage(Severity.INFO, "Test"));
        assertThat(result1.count()).isEqualTo(2);
        assertThat(result1.count(Severity.ERROR)).isOne();
        assertThat(result1.count(Severity.INFO)).isOne();
        assertThat(result2.count()).isEqualTo(2);
        assertThat(result2.count(Severity.ERROR)).isOne();
        assertThat(result2.count(Severity.INFO)).isOne();

        result2.add(new ValidationMessage(Severity.ERROR, "Test"));
        assertThat(result1.count()).isEqualTo(3);
        assertThat(result1.count(Severity.ERROR)).isEqualTo(2);
        assertThat(result1.count(Severity.INFO)).isOne();
        assertThat(result2.count()).isEqualTo(3);
        assertThat(result2.count(Severity.ERROR)).isEqualTo(2);
        assertThat(result2.count(Severity.INFO)).isOne();

        result2.add(new ValidationMessage(Severity.INFO, "Test"));
        assertThat(result1.count()).isEqualTo(4);
        assertThat(result1.count(Severity.ERROR)).isEqualTo(2);
        assertThat(result1.count(Severity.INFO)).isEqualTo(2);
        assertThat(result2.count()).isEqualTo(4);
        assertThat(result2.count(Severity.ERROR)).isEqualTo(2);
        assertThat(result2.count(Severity.INFO)).isEqualTo(2);

        result1.add(new ValidationMessage(Severity.ERROR, "Test"));
        assertThat(result1.count()).isEqualTo(5);
        assertThat(result1.count(Severity.ERROR)).isEqualTo(3);
        assertThat(result1.count(Severity.INFO)).isEqualTo(2);
        assertThat(result2.count()).isEqualTo(4);
        assertThat(result2.count(Severity.ERROR)).isEqualTo(2);
        assertThat(result2.count(Severity.INFO)).isEqualTo(2);

        result1.add(new ValidationMessage(Severity.INFO, "Test"));
        assertThat(result1.count()).isEqualTo(6);
        assertThat(result1.count(Severity.ERROR)).isEqualTo(3);
        assertThat(result1.count(Severity.INFO)).isEqualTo(3);
        assertThat(result2.count()).isEqualTo(4);
        assertThat(result2.count(Severity.ERROR)).isEqualTo(2);
        assertThat(result2.count(Severity.INFO)).isEqualTo(2);
    }

    @Test
    public void testIsValid() {
        ValidationResult result = new ValidationResult();
        assertThat(result.isValid()).isTrue();
        result.add(new ValidationMessage(Severity.INFO, "Test"));
        assertThat(result.isValid()).isTrue();
        result.add(new ValidationMessage(Severity.ERROR, "Test"));
        assertThat(result.isValid()).isFalse();
    }

    @Test
    public void
    testFormatForReport() {
        ValidationMessage error = ValidationMessage.error("MESSAGE1");
        ValidationMessage info = ValidationMessage.info("MESSAGE2");

        AssertionsForClassTypes.assertThat(formatForReport(error)).isEqualTo("ERROR: MESSAGE1");
        AssertionsForClassTypes.assertThat(formatForReport(info)).isEqualTo("INFO: MESSAGE2");

        ValidationOrigin origin = new ValidationOrigin("ORIGIN", "TEST");
        error.appendOrigin(origin);
        info.appendOrigin(origin);
        AssertionsForClassTypes.assertThat(formatForReport(error)).isEqualTo("ERROR: MESSAGE1 [ORIGIN: TEST]");
        AssertionsForClassTypes.assertThat(formatForReport(info)).isEqualTo("INFO: MESSAGE2 [ORIGIN: TEST]");

        origin = new ValidationOrigin("ORIGIN2", "TEST");
        error.appendOrigin(origin);
        info.appendOrigin(origin);
        AssertionsForClassTypes.assertThat(formatForReport(error)).isEqualTo("ERROR: MESSAGE1 [ORIGIN: TEST, ORIGIN2: TEST]");
        AssertionsForClassTypes.assertThat(formatForReport(info)).isEqualTo("INFO: MESSAGE2 [ORIGIN: TEST, ORIGIN2: TEST]");
    }

    @Test
    public void
    testFormatForLog() {
        ValidationMessage error = ValidationMessage.error("MESSAGE1");
        ValidationMessage info = ValidationMessage.info("MESSAGE2");

        AssertionsForClassTypes.assertThat(formatForLog(error)).isEqualTo("MESSAGE1");
        AssertionsForClassTypes.assertThat(formatForLog(info)).isEqualTo("MESSAGE2");

        ValidationOrigin origin = new ValidationOrigin("ORIGIN", "TEST");
        error.appendOrigin(origin);
        info.appendOrigin(origin);
        AssertionsForClassTypes.assertThat(formatForLog(error)).isEqualTo("MESSAGE1 [ORIGIN: TEST]");
        AssertionsForClassTypes.assertThat(formatForLog(info)).isEqualTo("MESSAGE2 [ORIGIN: TEST]");

        origin = new ValidationOrigin("ORIGIN2", "TEST");
        error.appendOrigin(origin);
        info.appendOrigin(origin);
        AssertionsForClassTypes.assertThat(formatForLog(error)).isEqualTo("MESSAGE1 [ORIGIN: TEST, ORIGIN2: TEST]");
        AssertionsForClassTypes.assertThat(formatForLog(info)).isEqualTo("MESSAGE2 [ORIGIN: TEST, ORIGIN2: TEST]");
    }

    @Test
    public void
    testWriteReport() throws IOException {
        Path reportFile = TempFileBuilder.empty("test");
        ValidationResult result = new ValidationResult(reportFile.toFile());
        result.add(ValidationMessage.error("MESSAGE1"));
        result.add(ValidationMessage.error("MESSAGE2"));
        result.add(ValidationMessage.info("MESSAGE3"));
        result.add(ValidationMessage.info("MESSAGE4"));
        result.close();

        List<String> lines = Files.readAllLines(reportFile);
        AssertionsForClassTypes.assertThat(lines.size()).isEqualTo(4);
        AssertionsForClassTypes.assertThat(lines.get(0)).endsWith("ERROR: MESSAGE1");
        AssertionsForClassTypes.assertThat(lines.get(1)).endsWith("ERROR: MESSAGE2");
        AssertionsForClassTypes.assertThat(lines.get(2)).endsWith("INFO: MESSAGE3");
        AssertionsForClassTypes.assertThat(lines.get(3)).endsWith("INFO: MESSAGE4");

        // With origin.

        reportFile = TempFileBuilder.empty("test");
        result = new ValidationResult(reportFile.toFile());

        ValidationOrigin origin = new ValidationOrigin("ORIGIN", "TEST");
        result.add(ValidationMessage.error("MESSAGE1").appendOrigin(origin));
        result.add(ValidationMessage.error("MESSAGE2").appendOrigin(origin));
        result.add(ValidationMessage.info("MESSAGE3").appendOrigin(origin));
        result.add(ValidationMessage.info("MESSAGE4").appendOrigin(origin));

        lines = Files.readAllLines(reportFile);
        AssertionsForClassTypes.assertThat(lines.size()).isEqualTo(4);
        AssertionsForClassTypes.assertThat(lines.get(0)).endsWith("ERROR: MESSAGE1 [ORIGIN: TEST]");
        AssertionsForClassTypes.assertThat(lines.get(1)).endsWith("ERROR: MESSAGE2 [ORIGIN: TEST]");
        AssertionsForClassTypes.assertThat(lines.get(2)).endsWith("INFO: MESSAGE3 [ORIGIN: TEST]");
        AssertionsForClassTypes.assertThat(lines.get(3)).endsWith("INFO: MESSAGE4 [ORIGIN: TEST]");
    }

    @Test
    public void
    testWriteLog() throws IOException {
        try {
            ByteArrayOutputStream strm = new ByteArrayOutputStream();
            System.setOut(new PrintStream(strm));

            ValidationResult result = new ValidationResult();
            result.add(ValidationMessage.error("MESSAGE1"));
            result.add(ValidationMessage.error("MESSAGE2"));
            result.add(ValidationMessage.info("MESSAGE3"));
            result.add(ValidationMessage.info("MESSAGE4"));
            result.close();

            strm.flush();
            List<String> lines = Arrays.asList(strm.toString().split(System.lineSeparator()));
            AssertionsForClassTypes.assertThat(lines.size()).isEqualTo(4);
            AssertionsForClassTypes.assertThat(lines.get(0)).endsWith("MESSAGE1");
            AssertionsForClassTypes.assertThat(lines.get(1)).endsWith("MESSAGE2");
            AssertionsForClassTypes.assertThat(lines.get(2)).endsWith("MESSAGE3");
            AssertionsForClassTypes.assertThat(lines.get(3)).endsWith("MESSAGE4");

            // With origin.

            strm = new ByteArrayOutputStream();
            System.setOut(new PrintStream(strm));

            result = new ValidationResult();
            ValidationOrigin origin = new ValidationOrigin("ORIGIN", "TEST");
            result.add(ValidationMessage.error("MESSAGE1").appendOrigin(origin));
            result.add(ValidationMessage.error("MESSAGE2").appendOrigin(origin));
            result.add(ValidationMessage.info("MESSAGE3").appendOrigin(origin));
            result.add(ValidationMessage.info("MESSAGE4").appendOrigin(origin));

            strm.flush();
            lines = Arrays.asList(strm.toString().split(System.lineSeparator()));
            AssertionsForClassTypes.assertThat(lines.size()).isEqualTo(4);
            AssertionsForClassTypes.assertThat(lines.get(0)).endsWith("MESSAGE1 [ORIGIN: TEST]");
            AssertionsForClassTypes.assertThat(lines.get(1)).endsWith("MESSAGE2 [ORIGIN: TEST]");
            AssertionsForClassTypes.assertThat(lines.get(2)).endsWith("MESSAGE3 [ORIGIN: TEST]");
            AssertionsForClassTypes.assertThat(lines.get(3)).endsWith("MESSAGE4 [ORIGIN: TEST]");
        } finally {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        }
    }
}
