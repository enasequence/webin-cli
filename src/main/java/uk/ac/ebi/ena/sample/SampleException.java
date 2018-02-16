package uk.ac.ebi.ena.sample;

import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class SampleException extends WebinCliException {
    public SampleException(String msg, ErrorType errorType) {
        super(msg, errorType);
    }
}