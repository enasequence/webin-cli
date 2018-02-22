package uk.ac.ebi.ena.upload;

import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class FtpException extends WebinCliException {
    public FtpException(String msg, ErrorType errorType) {
        super(msg, errorType);
    }
}