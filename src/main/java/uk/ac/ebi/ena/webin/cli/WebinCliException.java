package uk.ac.ebi.ena.webin.cli;

public class WebinCliException extends Exception {

    public enum ErrorType {
        USER_ERROR,
        SYSTEM_ERROR
    }

    private ErrorType errorType;

    public WebinCliException(String msg, ErrorType errorType) {
        super(msg);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
