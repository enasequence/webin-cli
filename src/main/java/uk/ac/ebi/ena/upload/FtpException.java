package uk.ac.ebi.ena.upload;

public class FtpException extends Exception {
    public FtpException(String msg) {
        super("All file(s) have successfully passed the validation stage but have not been uploaded (please note that all files have to be uploaded before they can be submitted): " + msg);
    }
}
