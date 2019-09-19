package uk.ac.ebi.ena.webin.cli.message;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationResultTest {

    @Test
    public void testGetMessagesAndCount() {
        ValidationResult result = new ValidationResult();

        result.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "Test"));
        assertThat(result.getMessages(ValidationMessage.Severity.ERROR)).size().isOne();
        assertThat(result.getMessages()).size().isOne();
        assertThat(result.getMessages().get(0).getMessage()).isEqualTo("Test");
        assertThat(result.getMessages().get(0).getSeverity()).isEqualTo(ValidationMessage.Severity.ERROR);
        assertThat(result.count()).isOne();
        assertThat(result.count(ValidationMessage.Severity.ERROR)).isOne();
        assertThat(result.count(ValidationMessage.Severity.ERROR, "Test")).isOne();
        assertThat(result.countRegex(ValidationMessage.Severity.ERROR, ".*")).isOne();
        assertThat(result.count(ValidationMessage.Severity.ERROR, "Unknown")).isZero();

        result.add(new ValidationMessage(ValidationMessage.Severity.INFO, "Test"));
        assertThat(result.getMessages(ValidationMessage.Severity.ERROR)).size().isOne();
        assertThat(result.getMessages(ValidationMessage.Severity.INFO)).size().isOne();
        assertThat(result.getMessages()).size().isEqualTo(2);
        assertThat(result.getMessages().get(0).getMessage()).isEqualTo("Test");
        assertThat(result.getMessages().get(0).getSeverity()).isEqualTo(ValidationMessage.Severity.ERROR);
        assertThat(result.getMessages().get(1).getMessage()).isEqualTo("Test");
        assertThat(result.getMessages().get(1).getSeverity()).isEqualTo(ValidationMessage.Severity.INFO);
        assertThat(result.count()).isEqualTo(2);
        assertThat(result.count(ValidationMessage.Severity.ERROR)).isOne();
        assertThat(result.count(ValidationMessage.Severity.ERROR, "Test")).isOne();
        assertThat(result.countRegex(ValidationMessage.Severity.ERROR, ".*")).isOne();
        assertThat(result.count(ValidationMessage.Severity.ERROR, "Unknown")).isZero();
        assertThat(result.count(ValidationMessage.Severity.INFO)).isOne();
        assertThat(result.count(ValidationMessage.Severity.INFO, "Test")).isOne();
        assertThat(result.countRegex(ValidationMessage.Severity.INFO, ".*")).isOne();
        assertThat(result.count(ValidationMessage.Severity.INFO, "Unknown")).isZero();

        result.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "Test2"));
        assertThat(result.getMessages(ValidationMessage.Severity.ERROR)).size().isEqualTo(2);
        assertThat(result.getMessages(ValidationMessage.Severity.INFO)).size().isOne();
        assertThat(result.getMessages()).size().isEqualTo(3);
        assertThat(result.getMessages().get(0).getMessage()).isEqualTo("Test");
        assertThat(result.getMessages().get(0).getSeverity()).isEqualTo(ValidationMessage.Severity.ERROR);
        assertThat(result.getMessages().get(1).getMessage()).isEqualTo("Test");
        assertThat(result.getMessages().get(1).getSeverity()).isEqualTo(ValidationMessage.Severity.INFO);
        assertThat(result.getMessages().get(2).getMessage()).isEqualTo("Test2");
        assertThat(result.getMessages().get(2).getSeverity()).isEqualTo(ValidationMessage.Severity.ERROR);
        assertThat(result.count()).isEqualTo(3);
        assertThat(result.count(ValidationMessage.Severity.ERROR)).isEqualTo(2);
        assertThat(result.count(ValidationMessage.Severity.ERROR, "Test")).isOne();
        assertThat(result.count(ValidationMessage.Severity.ERROR, "Test2")).isOne();
        assertThat(result.countRegex(ValidationMessage.Severity.ERROR, ".*")).isEqualTo(2);
        assertThat(result.countRegex(ValidationMessage.Severity.ERROR, "T.*")).isEqualTo(2);
        assertThat(result.count(ValidationMessage.Severity.ERROR, "Unknown")).isZero();
        assertThat(result.count(ValidationMessage.Severity.INFO)).isOne();
        assertThat(result.count(ValidationMessage.Severity.INFO, "Test")).isOne();
        assertThat(result.countRegex(ValidationMessage.Severity.INFO, ".*")).isOne();
        assertThat(result.count(ValidationMessage.Severity.INFO, "Unknown")).isZero();
    }

    @Test
    public void testIsValid() {
        ValidationResult result = new ValidationResult();
        assertThat(result.isValid()).isTrue();
        result.add(new ValidationMessage(ValidationMessage.Severity.INFO, "Test"));
        assertThat(result.isValid()).isTrue();
        result.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "Test"));
        assertThat(result.isValid()).isFalse();
    }

    @Test
    public void testAddResultWithOrigin() {
        ValidationResult result1 = new ValidationResult();
        ValidationResult result2 = new ValidationResult();
        result1.add(new ValidationMessage(ValidationMessage.Severity.INFO, "Test"));
        result1.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "Test"));
        result2.add(result1, "TEST1", "TEST2");
        assertThat(ValidationReporter.formatForReport(result2.getMessages().get(0))).isEqualTo("INFO: Test [TEST1, TEST2]");
        assertThat(ValidationReporter.formatForReport(result2.getMessages().get(1))).isEqualTo("ERROR: Test [TEST1, TEST2]");
    }

    @Test
    public void testAddResultWithoutOrigin() {
        ValidationResult result1 = new ValidationResult();
        ValidationResult result2 = new ValidationResult();
        result1.add(new ValidationMessage(ValidationMessage.Severity.INFO, "Test"));
        assertThat(result1.getMessages().get(0).getMessage()).isEqualTo("Test");
        result2.add(result1);
        assertThat(result2.getMessages().get(0).getMessage()).isEqualTo("Test");
    }
}
