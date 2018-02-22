package uk.ac.ebi.ena.submit;

import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class SubmitException extends WebinCliException {
    public SubmitException(String msg, ErrorType errorType) {
        super(msg, errorType);
    }
}