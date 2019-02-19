package uk.ac.ebi.ena.webin.cli.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;

public final class ValidationMessageLogger {

    private static final Logger log = LoggerFactory.getLogger(ValidationMessageLogger.class);

    private ValidationMessageLogger() {
    }

    public static void log(ValidationResult validationResult) {
        validationResult.getMessages().forEach(ValidationMessageLogger::log);
    }

    public static void log(ValidationMessage<Origin> validationMessage) {
        if (validationMessage.getSeverity() == Severity.ERROR) {
            log.error(validationMessage.getMessage());
        }
        if (validationMessage.getSeverity() == Severity.INFO) {
            log.info(validationMessage.getMessage());
        }
    }
}
