package uk.ac.ebi.ena.webin.cli;

import java.text.MessageFormat;

public interface WebinCliMessage {

    enum Service {
        SUBMISSION_SERVICE_SYSTEM_ERROR("A server error occurred when attempting to submit."),
        IGNORE_ERRORS_SERVICE_SYSTEM_ERROR("A server error occurred when retrieving ignore error information."),
        VERSION_SERVICE_SYSTEM_ERROR("A server error occurred when checking application version."),
        SAMPLE_SERVICE_VALIDATION_ERROR("Unknown sample {0} or the sample cannot be referenced by your submission account. Samples must be submitted before they can be referenced in the submission."),
        SAMPLE_SERVICE_SYSTEM_ERROR("A server error occurred when retrieving sample {0} information."),
        STUDY_SERVICE_VALIDATION_ERROR("Unknown study {0} or the study cannot be referenced by your submission account. Studies must be submitted before they can be referenced in the submission."),
        STUDY_SERVICE_SYSTEM_ERROR("A server error occurred when retrieving study {0} information.");

        final String text;
        Service(String text) {
            this.text = text;
        }
        public String format(Object... arguments) {
            try {
                return MessageFormat.format(text, arguments);
            } catch (RuntimeException ex) {
                return MessageFormat.format(text, arguments);
            }
        }
    }
}
