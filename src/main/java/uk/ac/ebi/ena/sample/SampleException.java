package uk.ac.ebi.ena.sample;

public class SampleException extends Exception {
    public SampleException(String msg) {
        super("Error occured while attempting to validate sample - " + msg);
    }
}
