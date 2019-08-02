package uk.ac.ebi.ena.webin.cli.validator.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** List of submitted files.
 */
public class SubmissionFiles <FileType extends Enum<FileType>> {

    private List<SubmissionFile<FileType>> files = new ArrayList<>();

    public void set(List<SubmissionFile<FileType>> files) {
        this.files = files;
    }

    public SubmissionFiles<FileType> add(SubmissionFile<FileType> file) {
        this.files.add(file);
        return this;
    }

    public List<SubmissionFile<FileType>> get() {
        return files;
    }

    public List<File> files() {
        return files.stream().map(file -> file.getFile()).collect(Collectors.toList());
    }
}
