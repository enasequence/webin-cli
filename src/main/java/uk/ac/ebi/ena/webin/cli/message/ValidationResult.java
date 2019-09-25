package uk.ac.ebi.ena.webin.cli.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.ena.webin.cli.message.ValidationMessage.Severity;
import uk.ac.ebi.ena.webin.cli.message.listener.MessageListener;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationResult implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationResult.class);

    private final OutputStream strm;
    private final ValidationResult parentResult;
    private final List<ValidationOrigin> origin = new ArrayList<>();
    private final List<MessageListener> listener = new ArrayList<>();
    private boolean log = true;
    private int infoCount = 0;
    private int errorCount = 0;

    /** Creates a new validation result that logs messages.
     */
    public ValidationResult() {
        this.strm = null;
        this.parentResult = null;
    }

    /** Creates a new validation result that logs messages.
     */
    public ValidationResult(ValidationOrigin ... origin) {
        this();
        this.origin.addAll(Arrays.asList(origin));
    }

    /** Creates a new validation result that logs messages.
     */
    public ValidationResult(List<ValidationOrigin> origin) {
        this();
        this.origin.addAll(origin);
    }

    /** Creates a new validation result that writes messages
     * to a file. By default also logs messages.
     */
    public ValidationResult(File reportFile) {
        try {
            if (reportFile != null) {
                this.strm = Files.newOutputStream(
                        reportFile.toPath(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.SYNC);
            }
            else {
                this.strm = null;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        this.parentResult = null;
    }

    /** Creates a new validation result that writes messages
     * to a file. By default also logs messages.
     */
    public ValidationResult(File reportFile, ValidationOrigin ... origin) {
        this(reportFile);
        this.origin.addAll(Arrays.asList(origin));
    }

    /** Creates a new validation result that writes messages
     * to a file. By default also logs messages.
     */
    public ValidationResult(File reportFile, List<ValidationOrigin> origin) {
        this(reportFile);
        this.origin.addAll(origin);
    }

    private ValidationResult(ValidationResult parentResult) {
        this.strm = null;
        this.parentResult = parentResult;
    }

    private ValidationResult(ValidationResult parentResult, ValidationOrigin ... origin) {
        this(parentResult);
        this.origin.addAll(Arrays.asList(origin));
    }

    private ValidationResult(ValidationResult parentResult, List<ValidationOrigin> origin) {
        this(parentResult);
        this.origin.addAll(origin);
    }

    /** Creates a new validation result that is linked to this validation result.
     */
    public ValidationResult create() {
        return new ValidationResult(this);
    }

    /** Creates a new validation result that is linked to this validation result.
     */
    public ValidationResult create(ValidationOrigin ... origin) {
        return new ValidationResult(this, origin);
    }

    /** Creates a new validation result that is linked to this validation result.
     */
    public ValidationResult create(List<ValidationOrigin> origin) {
        return new ValidationResult(this, origin);
    }

    public boolean isLog() {
        return log;
    }

    /** Enables or disables message logging.
     */
    public void setLog(boolean log) {
        this.log = log;
    }

    /** Adds a new validation message to the validation result.
     * The validation message will be included in all linked
     * validation results and will contain all the origins
     * associated with them.
     */
    public ValidationResult add(ValidationMessage message) {
        if (Severity.ERROR.equals(message.getSeverity())) {
            errorCount++;
        }
        else {
            infoCount++;
        }
        listener.forEach(l -> l.listen(message));
        message.prependOrigin(origin);
        if (parentResult != null) {
            parentResult.add(message);
        }
        // Write messages only once.
        if (this.parentResult == null) {
            if (strm != null) {
                report(message);
            }
            if (this.log) {
                log(message);
            }
        }
        return this;
    }

    private void report(ValidationMessage message) {
        try {
            String str = formatForReport(message) + "\n";
            strm.write(str.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            if (!this.log) {
                log(message);
            }
        }
    }

    private void log(ValidationMessage message) {
        String str = formatForLog(message);
        if (ValidationMessage.Severity.ERROR.equals(message.getSeverity())) {
            LOGGER.error(str);
        } else {
            LOGGER.info(str);
        }
    }

    public static String formatForLog(ValidationMessage message) {
        String originStr = "";
        if (!message.getOrigin().isEmpty()) {
            originStr = " " + message.getOrigin().stream()
                    .map(origin -> origin.toString())
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        return String.format("%s%s",
                message.getMessage(),
                originStr);
    }

    public static String formatForReport(ValidationMessage message) {
        return String.format("%s: %s", message.getSeverity(),
                formatForLog(message));
    }

    /** Adds a new validation listener to the validation result.
     */
    public ValidationResult add(MessageListener listener) {
        this.listener.add(listener);
        return this;
    }

    /** Returns true if this validation result does not contain
     * any validation messages with ERROR severity.
     */
    public boolean isValid() {
        return errorCount == 0;
    }

    /** Returns the number of validation messages in this
     * validation result.
     */
    public long count() {
        return infoCount + errorCount;
    }

    /** Returns the number of validation messages in this
     * validation result for a given severity.
     */
    public long count(Severity severity) {
        if (Severity.ERROR.equals(severity)) {
            return errorCount;
        }
        else {
            return infoCount;
        }
    }

    @Override
    public void close() {
        if (strm != null) {
            try {
                strm.flush();
                strm.close();
            } catch (IOException ex) {
            }
        }
    }
}
