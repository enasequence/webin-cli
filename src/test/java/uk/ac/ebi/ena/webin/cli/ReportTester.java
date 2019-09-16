package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportTester {

    private final WebinCliExecutor<?,?> executor;

    public ReportTester(WebinCli cli) {
        this.executor = cli.getExecutor();
    }

    public ReportTester(WebinCliExecutor<?,?> executor) {
        this.executor = executor;
    }

    public void inFileReport(String dataFile, String message) {
        inFileReport(Paths.get(dataFile), message);
    }

    public void inFileReport(File dataFile, String message) {
        inFileReport(dataFile.toPath(), message);
    }

    public void inFileReport(Path dataFile, String message) {
        Path reportFile = executor.getValidationDir().toPath()
                .resolve(dataFile.getFileName().toString() + ".report");
        assertThat(readFile(reportFile)).contains(message);
    }

    public void inSubmissionReport(String message) {
        Path reportFile = executor.getSubmissionReportFile().toPath();
        assertThat(readFile(reportFile)).contains(message);
    }

    public void inManifestReport(String message) {
        Path reportFile = executor.getManifestReportFile().toPath();
        assertThat(readFile(reportFile)).contains(message);
    }

    public void notInManifestReport(String message) {
        Path reportFile = executor.getManifestReportFile().toPath();
        assertThat(readFile(reportFile)).doesNotContain(message);
    }

    private static String
    readFile(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
