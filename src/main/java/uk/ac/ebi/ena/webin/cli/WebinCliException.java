package uk.ac.ebi.ena.webin.cli;

public class WebinCliException extends RuntimeException {

    public enum ErrorType {
        USER_ERROR,
        SYSTEM_ERROR,
        VALIDATION_ERROR
    }

    private ErrorType errorType;

    private WebinCliException(ErrorType errorType, String ... messages) {
        super(trim(messages));
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public WebinCliException throwAddMessage(String userErrorMessage, String systemErrorMessage) {
        switch (getErrorType()) {
            case SYSTEM_ERROR:
                throw new WebinCliException(getErrorType(), trim(systemErrorMessage, getMessage()));
            default:
                throw new WebinCliException(getErrorType(), trim(userErrorMessage, getMessage()));
        }
    }

    public static WebinCliException createUserError(String ... messages) {
        return new WebinCliException(ErrorType.USER_ERROR, messages);
    }

    public static WebinCliException createSystemError(String ... messages) {
        return new WebinCliException(ErrorType.SYSTEM_ERROR, messages);
    }

    public static WebinCliException createValidationError(String ... messages) {
        return new WebinCliException(ErrorType.VALIDATION_ERROR, messages);
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
