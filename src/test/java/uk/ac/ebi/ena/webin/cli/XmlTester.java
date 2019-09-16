package uk.ac.ebi.ena.webin.cli;

import org.junit.Assert;

import java.util.Arrays;
import java.util.stream.Collectors;

public class XmlTester {
    public static void
    assertXml(String xml, String expectedXml) {
        xml = xml.replaceAll("<\\?xml.*", "");
        xml = xml.replaceAll("\\r\\n?", "\n");
        xml = Arrays.stream(xml.split("\n"))
                .filter( line -> !line.trim().isEmpty() )
                .map( line -> line.replaceAll("^\\s+", ""))
                .map( line -> line.replaceAll("\\s+$", ""))
                .collect(Collectors.joining("\n"));
        xml = xml.replaceAll("<\\s+", "<");
        xml = xml.replaceAll("\\s+/>", "/>");
        xml = xml.replaceAll("\\s*alias=\"[^\"]+\"", "");
        expectedXml = expectedXml.replaceAll("<\\?xml.*", "");
        expectedXml = expectedXml.replaceAll("\\r\\n?", "\n");
        expectedXml = Arrays.stream(expectedXml.split("\n"))
                .filter( line -> !line.trim().isEmpty() )
                .map( line -> line.replaceAll("^\\s+", ""))
                .map( line -> line.replaceAll("\\s+$", ""))
                .collect(Collectors.joining("\n"));
        expectedXml = expectedXml.replaceAll("<\\s+", "<");
        expectedXml = expectedXml.replaceAll("\\s+/>", "/>");
        expectedXml = expectedXml.replaceAll("\\s*alias=\"[^\"]+\"", "");
        Assert.assertFalse(xml.isEmpty());
        Assert.assertFalse(expectedXml.isEmpty());
        Assert.assertEquals(expectedXml, xml);
    }
}
