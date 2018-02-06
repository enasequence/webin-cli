package uk.ac.ebi.ena.submit;

public class SubmitException extends Exception {
    public SubmitException(String msg) {
        super("ERROR - Failed to Submit data: " + msg);
    }
}
