package uk.ac.ebi.ena.webin.cli.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

public class ValidationReporter implements AutoCloseable {

    protected final OutputStream strm;

    private static final Logger log = LoggerFactory.getLogger(ValidationReporter.class);

    public ValidationReporter(OutputStream strm) {
        this.strm = strm;
    }

    public ValidationReporter(File file) {
        try {
            this.strm = Files.newOutputStream(
                    file.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.SYNC);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(ValidationResult result) {
        for (ValidationMessage message : result.getMessages()) {
            write(message);
        }
    }

    public void write(ValidationMessage message) {
        try {
            String str = formatForReport(message) + "\n";
            strm.write(str.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            if (ValidationMessage.Severity.ERROR.equals(message.getSeverity())) {
                log.error(message.getMessage());
            } else {
                log.info(message.getMessage());
            }
        }
    }

    public void write(ValidationMessage.Severity severity, String message) {
        write(new ValidationMessage(severity, message));
    }

    private final static String ORIGIN_BEGIN = "[";
    private final static String ORIGIN_END = "]";
    private final static String ORIGIN_SEPARATOR = ", ";

    public static String formatForReport(ValidationMessage message) {
        return String.format("%s: %s", message.getSeverity(),
                formatForLog(message));
    }

    public static String formatForLog(ValidationMessage message) {
        String origin = "";
        if (!message.getOrigins().isEmpty()) {
            origin = " " + message.getOrigins().stream().collect(
                    Collectors.joining(ORIGIN_SEPARATOR, ORIGIN_BEGIN, ORIGIN_END));
        }

        return String.format("%s%s",
                message.getMessage(),
                origin);
    }

    @Override
    public void close() {
        try {
            strm.flush();
            strm.close();
        } catch (IOException ex) {
        }
    }
}
