package uk.ac.ebi.ena.webin.cli.spreasheet;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.spreadsheet.SpreadsheetContext;
import uk.ac.ebi.ena.webin.cli.spreadsheet.SpreadsheetWriter;

import java.io.IOException;
import java.nio.file.Files;

public class SpreadsheetTest {

    @Test
    public void writeAll() throws IOException {
        SpreadsheetWriter.writeAll(Files.createTempDirectory("TEST"));
    }


    @Test
    public void testGenome() throws IOException {
        SpreadsheetWriter writer = new SpreadsheetWriter(SpreadsheetContext.GENOME);
        writer.addRow("TEST1", "TEST2", "TEST3", "TEST4", "TEST5", "TEST6", "TEST7", "TEST8", "TEST9", "TEST10", "TEST11", "TEST12");
        writer.write(Files.createTempDirectory("TEST"));
    }
}
