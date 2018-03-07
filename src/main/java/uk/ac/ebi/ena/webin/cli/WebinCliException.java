package uk.ac.ebi.ena.webin.cli;

public class WebinCliException extends RuntimeException {

    public enum ErrorType {
        USER_ERROR,
        SYSTEM_ERROR,
        VALIDATION_ERROR
    }

    private ErrorType errorType;

    public WebinCliException(ErrorType errorType) {
        super();
        this.errorType = errorType;
    }

    public WebinCliException(String msg, ErrorType errorType) {
        super(msg);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public WebinCliException addToMessageAndThrow(String nonSystemError, String systemError) {
        switch (getErrorType()) {
            case SYSTEM_ERROR:
                throw new WebinCliException((systemError + " " + getMessage()).trim().replaceAll(" +", " "), getErrorType());
            default:
                throw new WebinCliException((nonSystemError + " " + getMessage()).trim().replaceAll(" +", " "), getErrorType());
        }
    }
}
