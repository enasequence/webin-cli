package uk.ac.ebi.ena.webin.cli;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.WebinCliTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WebinCliReporterTest {

    @Test
    public void
    testWriteToReport_ErrorString_WithAndWithoutOrigin() throws Exception
    {
        Path reportFile = WebinCliTestUtils.createTempFile("test1");

        WebinCliReporter.writeToFile(reportFile.toFile(), Severity.ERROR, "MESSAGE1" );

        List<String> lines = Files.readAllLines(reportFile);
        Assert.assertEquals(1, lines.size());
        Assert.assertTrue(lines.get(0).endsWith("ERROR: MESSAGE1"));

        // With origin.

        Origin origin = new DefaultOrigin("ORIGIN2");

        reportFile = WebinCliTestUtils.createTempFile("test2");

        WebinCliReporter.writeToFile(reportFile.toFile(), Severity.ERROR, "MESSAGE2", origin );

        lines = Files.readAllLines(reportFile);
        Assert.assertEquals(1, lines.size());
        Assert.assertTrue(lines.get(0).endsWith("ERROR: MESSAGE2 [ORIGIN2]"));
    }

    @Test
    public void
    testWriteToReport_InfoString_WithAndWithoutOrigin() throws Exception
    {
        Path reportFile = WebinCliTestUtils.createTempFile("test");

        WebinCliReporter.writeToFile(reportFile.toFile(), Severity.INFO, "MESSAGE" );

        List<String> lines = Files.readAllLines(reportFile);
        Assert.assertEquals(1, lines.size());
        Assert.assertTrue(lines.get(0).endsWith("INFO: MESSAGE"));

        // With origin.

        Origin origin = new DefaultOrigin("ORIGIN2");

        reportFile = WebinCliTestUtils.createTempFile("test2");

        WebinCliReporter.writeToFile(reportFile.toFile(), Severity.INFO, "MESSAGE2", origin );

        lines = Files.readAllLines(reportFile);
        Assert.assertEquals(1, lines.size());
        Assert.assertTrue(lines.get(0).endsWith("INFO: MESSAGE2 [ORIGIN2]"));
    }



    @Test
    public void
    testWriteToReport_ErrorValidationMessage_WithAndWithoutOrigin() throws Exception
    {
        Path reportFile = WebinCliTestUtils.createTempFile("test1");

        WebinCliReporter.writeToFile(reportFile.toFile(), WebinCliReporter.createValidationMessage(Severity.ERROR, "MESSAGE1"));

        List<String> lines = Files.readAllLines(reportFile);
        Assert.assertEquals(1, lines.size());
        Assert.assertTrue(lines.get(0).endsWith("ERROR: MESSAGE1"));

        // With origin.

        Origin origin = new DefaultOrigin("ORIGIN2");

        reportFile = WebinCliTestUtils.createTempFile("test2");

        WebinCliReporter.writeToFile(reportFile.toFile(), WebinCliReporter.createValidationMessage(Severity.ERROR, "MESSAGE2", origin));

        lines = Files.readAllLines(reportFile);
        Assert.assertEquals(1, lines.size());
        Assert.assertTrue(lines.get(0).endsWith("ERROR: MESSAGE2 [ORIGIN2]"));
    }

    @Test
    public void
    testWriteToReport_InfoValidationMessage_WithAndWithoutOrigin() throws Exception
    {
        Path reportFile = WebinCliTestUtils.createTempFile("test1");

        WebinCliReporter.writeToFile(reportFile.toFile(), WebinCliReporter.createValidationMessage(Severity.INFO, "MESSAGE1"));

        List<String> lines = Files.readAllLines(reportFile);
        Assert.assertEquals(1, lines.size());
        Assert.assertTrue(lines.get(0).endsWith("INFO: MESSAGE1"));

        // With origin.

        Origin origin = new DefaultOrigin("ORIGIN2");

        reportFile = WebinCliTestUtils.createTempFile("test2");

        WebinCliReporter.writeToFile(reportFile.toFile(), WebinCliReporter.createValidationMessage(Severity.INFO, "MESSAGE2", origin));

        lines = Files.readAllLines(reportFile);
        Assert.assertEquals(1, lines.size());
        Assert.assertTrue(lines.get(0).endsWith("INFO: MESSAGE2 [ORIGIN2]"));
    }
}
