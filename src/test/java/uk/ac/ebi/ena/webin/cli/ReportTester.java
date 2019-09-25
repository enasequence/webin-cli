package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportTester {

    private final WebinCliExecutor<?,?> executor;

    public ReportTester(WebinCli cli) {
        this.executor = cli.getExecutor();
    }

    public ReportTester(WebinCliExecutor<?,?> executor) {
        this.executor = executor;
    }

    // Submission report
    //

    public void textInSubmissionReport(String message) {
        textInReport(executor.getSubmissionReportFile(), message);
    }

    public void textNotInSubmissionReport(String message) {
        textNotInReport(executor.getSubmissionReportFile(), message);
    }

    public void regexInSubmissionReport(String message) {
        regexInReport(executor.getSubmissionReportFile(), message);
    }

    public void regexNotInSubmissionReport(String message) {
        regexNotInReport(executor.getSubmissionReportFile(), message);
    }

    // Manifest report
    //

    public void textInManifestReport(String message) {
        textInReport(executor.getManifestReportFile(), message);
    }

    public void textNotInManifestReport(String message) {
        textNotInReport(executor.getManifestReportFile(), message);
    }

    public void regexInManifestReport(String regex) {
        regexInReport(executor.getManifestReportFile(), regex);
    }

    public void regexNotInManifestReport(String regex) {
        regexNotInReport(executor.getManifestReportFile(), regex);
    }

    // Data file report
    //

    private File getFileReport(Path dataFile) {
        return executor.getValidationDir().toPath()
                .resolve(dataFile.getFileName().toString() + ".report").toFile();
    }

    public void textInFileReport(String dataFile, String message) {
        textInReport(getFileReport(Paths.get(dataFile)), message);
    }

    public void textNotInFileReport(String dataFile, String message) {
        textNotInReport(getFileReport(Paths.get(dataFile)), message);
    }

    public void regexInFileReport(String dataFile, String message) {
        regexInReport(getFileReport(Paths.get(dataFile)), message);
    }

    public void regexNotInFileReport(String dataFile, String message) {
        regexNotInReport(getFileReport(Paths.get(dataFile)), message);
    }

    // Generic report
    //

    private static void textInReport(File reportFile, String message) {
        assertThat(readFile(reportFile.toPath())).contains(message);
    }

    private static void textNotInReport(File reportFile, String message) {
        assertThat(readFile(reportFile.toPath())).doesNotContain(message);
    }

    private static void regexInReport(File reportFile, String regex) {
        assertThat(readFile(reportFile.toPath())).containsPattern(Pattern.compile(regex));
    }

    private static void regexNotInReport(File reportFile, String regex) {
        assertThat(readFile(reportFile.toPath())).doesNotContainPattern(Pattern.compile(regex));
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
