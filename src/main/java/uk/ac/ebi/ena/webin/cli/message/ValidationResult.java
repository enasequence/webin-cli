package uk.ac.ebi.ena.webin.cli.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.ena.webin.cli.message.ValidationMessage.Severity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.ena.webin.cli.message.ValidationReporter.formatForLog;

public class ValidationResult {

    private static final Logger log = LoggerFactory.getLogger(ValidationResult.class);

    private final ArrayList<ValidationMessage> messages = new ArrayList<>();

    public ValidationResult() {
    }

    public ValidationResult(ValidationMessage message) {
        messages.add(message);
    }

    public List<ValidationMessage> getMessages() {
        return messages;
    }

    public List<ValidationMessage> getMessages(Severity severity) {
        return messages.stream().filter(message ->
                severity.equals(message.getSeverity())).collect(Collectors.toList());
    }

    public ValidationResult add(ValidationMessage message) {
        messages.add(message);
        return this;
    }

    public ValidationResult add(ValidationResult result) {
        for (ValidationMessage message : result.getMessages()) {
            add(message);
        }
        return this;
    }

    public ValidationResult add(ValidationResult result, String ... origin) {
        result.messages.forEach(message -> message.addOrigin(origin));
        return add(result);
    }

    public ValidationResult add(ValidationResult result, Collection<String> origin) {
        result.messages.forEach(message -> message.addOrigin(origin));
        return add(result);
    }

    public boolean isValid() {
        return messages.stream().noneMatch(message ->
                Severity.ERROR.equals(message.getSeverity()));
    }

    public long count() {
        return messages.size();
    }

    public long count(Severity severity) {
        return messages.stream().filter(m ->
                m.getSeverity().equals(severity)).count();
    }

    public long count(Severity severity, String message) {
        return messages.stream().filter(m ->
                m.getSeverity().equals(severity) &&
                        m.getMessage().equals(message)).count();
    }

    public long countRegex(Severity severity, String regex) {
        return messages.stream().filter(m ->
                m.getSeverity().equals(severity) &&
                        m.getMessage().matches(regex)).count();
    }

    public void log() {
        messages.forEach(message -> {
            if (message.getSeverity() == Severity.ERROR) {
                log.error(formatForLog(message));
            } else {
                log.info(formatForLog(message));
            }
        });
    }
}
