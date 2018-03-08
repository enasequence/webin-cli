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

    public WebinCliException(String message, ErrorType errorType) {
        super(trim(message));
        this.errorType = errorType;
    }

    public WebinCliException(String messageContext, String message, ErrorType errorType) {
        super(trim(messageContext, message));
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public WebinCliException addMessageContext(String userErrorMessageContext, String systemErrorMessageContext) {
        switch (getErrorType()) {
            case SYSTEM_ERROR:
                throw new WebinCliException(trim(systemErrorMessageContext, getMessage()), getErrorType());
            default:
                throw new WebinCliException(trim(userErrorMessageContext, getMessage()), getErrorType());
        }
    }

    private static String trim(String ... messages) {
        String msg = "";
        for (String message : messages) {
            if (message != null) {
                msg += " " + message;
            }
        }
        return msg.trim().replaceAll(" +", " ");
    }
}
