package uk.ac.ebi.ena.webin.cli.validator.manifest;

import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.reference.Analysis;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Manifest <FileType extends Enum<FileType>> {

    private String name;
    private String description;
    private String address;
    private String authors;
    private Sample sample;
    private Study study;
    private List<Run> run = new ArrayList<>();
    private List<Analysis> analysis = new ArrayList<>();
    private SubmissionFiles<FileType> files = new SubmissionFiles<>();
    private boolean ignoreErrors;

    /** Temporary files must written into this directory.
     */
    private File processDir;

    /** Validation messages must be written into this file.
     */
    private File reportDir;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    public List<Run> getRun() {
        return run;
    }

    public void setRun(List<Run> run) {
        this.run = run;
    }

    public List<Analysis> getAnalysis() {
        return analysis;
    }

    public void setAnalysis(List<Analysis> analysis) {
        this.analysis = analysis;
    }

    public SubmissionFiles<FileType> getFiles() {
        return files;
    }

    public void setFiles(SubmissionFiles<FileType> files) {
        this.files = files;
    }

    public SubmissionFiles<FileType> files() {
        return files;
    }

    public List<SubmissionFile<FileType>> files(FileType fileType) {
        return files.get().stream().filter(file -> file.isFileType(fileType)).collect(Collectors.toList());
    }

    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public File getProcessDir() {
        return processDir;
    }

    public void setProcessDir(File processDir) {
        this.processDir = processDir;
    }

    public File getReportDir() {
        return reportDir;
    }

    public void setReportDir(File reportDir) {
        this.reportDir = reportDir;
    }
}
