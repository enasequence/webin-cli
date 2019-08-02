package uk.ac.ebi.ena.webin.cli.validator.file;

import java.io.File;

/** Submitted file.
*/
public class SubmissionFile<FileType extends Enum<FileType>> {

    private final FileType fileType;
    private final File file;

    /** Validation messages must be written into this file.
     */
    private final File reportFile;

    public SubmissionFile(FileType fileType, File file) { // TODO: , File reportFile) {
        this.fileType = fileType;
        this.file = file;
        this.reportFile = null; // TODO: reportFile;
    }

    public FileType getFileType() {
        return fileType;
    }

    public boolean isFileType(FileType fileType) {
        return fileType != null && this.fileType != null && fileType.equals(this.fileType);
    }

    public File getFile() {
        return file;
    }

    public File getReportFile() {
        return reportFile;
    }
}
