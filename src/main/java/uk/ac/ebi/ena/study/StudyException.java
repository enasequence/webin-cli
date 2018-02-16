package uk.ac.ebi.ena.study;

import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class StudyException  extends WebinCliException {
    public StudyException(String msg, ErrorType errorType) {
        super(msg, errorType);
    }
}