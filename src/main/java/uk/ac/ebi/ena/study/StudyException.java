package uk.ac.ebi.ena.study;

public class StudyException extends Exception {
    public StudyException(String msg) {
        super("Error occured while attempting to validate study - " + msg);
    }
}
