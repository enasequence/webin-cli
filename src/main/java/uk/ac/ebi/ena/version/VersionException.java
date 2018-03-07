package uk.ac.ebi.ena.version;

import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class VersionException extends WebinCliException {
    public VersionException(String msg, ErrorType errorType) {
        super(msg, errorType);
    }
}
