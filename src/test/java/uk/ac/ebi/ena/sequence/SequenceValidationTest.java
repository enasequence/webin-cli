package uk.ac.ebi.ena.sequence;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.assembly.SequenceAssemblyWebinCli;

public class SequenceValidationTest {
    private final static String SEQUENCE_BASE_DIR = "src/test/resources/uk/ac/ebi/ena/template/tsvfile";
    private final static String[] allTemplatesA = {"ERT000002-rRNA.tsv.gz",
                                                    "ERT000003-EST-1.tsv.gz",
                                                    "ERT000006-SCM.tsv.gz",
                                                    "ERT000009-ITS.tsv.gz",
                                                    "ERT000020-COI.tsv.gz",
                                                    "ERT000024-GSS-1.tsv.gz",
                                                    "ERT000028-SVC.tsv.gz",
                                                    "ERT000029-SCGD.tsv.gz",
                                                    "ERT000030-MHC1.tsv.gz",
                                                    "ERT000031-viroid.tsv.gz",
                                                    "ERT000032-matK.tsv.gz",
                                                    "ERT000034-Dloop.tsv.gz",
                                                    "ERT000035-IGS.tsv.gz",
                                                    "ERT000036-MHC2.tsv.gz",
                                                    "ERT000037-intron.tsv.gz",
                                                    "ERT000038-hyloMarker.tsv.gz",
                                                    "ERT000039-Sat.tsv.gz",
                                                    "ERT000042-ncRNA.tsv.gz",
                                                    "ERT000047-betasat.tsv.gz",
                                                    "ERT000050-ISR.tsv.gz",
                                                    "ERT000051-poly.tsv.gz",
                                                    "ERT000052-ssRNA.tsv.gz",
                                                    "ERT000053-ETS.tsv.gz",
                                                    "ERT000054-prom.tsv.gz",
                                                    "ERT000055-STS.tsv.gz",
                                                    "ERT000056-mobele.tsv.gz",
                                                    "ERT000057-alphasat.tsv.gz",
                                                    "ERT000058-MLmarker.tsv.gz",
                                                    "ERT000060-vUTR.tsv.gz"};

    //TODO Default Locale handling is incorrect
    @Before public void 
    before()
    {
        Locale.setDefault( Locale.UK );
    }
    
    
    @Test
    public void mandatoryFieldsPresent()  {
        try {
            String testTsvFile = SEQUENCE_BASE_DIR + File.separator + "Sequence-mandatory-field-missing.tsv.gz";
            SequenceAssemblyWebinCli sequenceAssemblyWebinCli = new SequenceAssemblyWebinCli();
            sequenceAssemblyWebinCli.setReportsDir(SEQUENCE_BASE_DIR);
            StringBuilder resultsSb = sequenceAssemblyWebinCli.validateTestTsv(testTsvFile);
            String expectedResults = new String(Files.readAllBytes(Paths.get(SEQUENCE_BASE_DIR  + File.separator + "Sequence-mandatory-field-missing-expected-results.txt")));
            assertEquals(resultsSb.toString().replaceAll("\\s+", ""), expectedResults.replaceAll("\\s+", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAllTemplate() {
        try {
            String testTsvFile;;
            StringBuilder resultsSb = new StringBuilder();
            for (String file: allTemplatesA) {
                testTsvFile = SEQUENCE_BASE_DIR + File.separator + file;
                SequenceAssemblyWebinCli sequenceAssemblyWebinCli = new SequenceAssemblyWebinCli();
                sequenceAssemblyWebinCli.setReportsDir(SEQUENCE_BASE_DIR);
                resultsSb.append(sequenceAssemblyWebinCli.validateTestTsv(testTsvFile));
            }
            assertEquals("", resultsSb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void invalidAlphanumericEntrynumber()  {
        try {
            String testTsvFile = SEQUENCE_BASE_DIR + File.separator + "Sequence-invalid-alphanumeric-entrynumber-.tsv.gz";
            SequenceAssemblyWebinCli sequenceAssemblyWebinCli = new SequenceAssemblyWebinCli();
            sequenceAssemblyWebinCli.setReportsDir(SEQUENCE_BASE_DIR);
            StringBuilder resultsSb = sequenceAssemblyWebinCli.validateTestTsv(testTsvFile);
            String expectedResults = new String(Files.readAllBytes(Paths.get(SEQUENCE_BASE_DIR  + File.separator + "Sequence-invalidAlphanumericEntrynumber-expected-results.txt")));
            assertEquals(resultsSb.toString().trim(), expectedResults.trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void requiredHeadersMissing() {
        try {
            String testTsvFile = SEQUENCE_BASE_DIR + File.separator + "Sequence-ERT000039-missingheaders.tsv.gz";
            SequenceAssemblyWebinCli sequenceAssemblyWebinCli = new SequenceAssemblyWebinCli();
            sequenceAssemblyWebinCli.setReportsDir(SEQUENCE_BASE_DIR);
            StringBuilder resultsSb = sequenceAssemblyWebinCli.validateTestTsv(testTsvFile);
            String expectedResults = new String(Files.readAllBytes(Paths.get(SEQUENCE_BASE_DIR  + File.separator + "Sequence-ERT000039-missingheaders-expected-results.txt")));
            assertEquals(resultsSb.toString().replaceAll("\\s+", ""), expectedResults.replaceAll("\\s+", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void invalidMarker() {
        try {
            String testTsvFile = SEQUENCE_BASE_DIR + File.separator + "Sequence-invalid-marker.tsv.gz";
            SequenceAssemblyWebinCli sequenceAssemblyWebinCli = new SequenceAssemblyWebinCli();
            sequenceAssemblyWebinCli.setReportsDir(SEQUENCE_BASE_DIR);
            StringBuilder resultsSb = sequenceAssemblyWebinCli.validateTestTsv(testTsvFile);
            String expectedResults = new String(Files.readAllBytes(Paths.get(SEQUENCE_BASE_DIR  + File.separator + "Sequence-invalidMarker-expected-results.txt")));
            assertEquals(resultsSb.toString().trim(), expectedResults.trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void invalidSediment() {
        try {
            String testTsvFile = SEQUENCE_BASE_DIR + File.separator + "Sequence-invalid-sediment.tsv.gz";
            SequenceAssemblyWebinCli sequenceAssemblyWebinCli = new SequenceAssemblyWebinCli();
            sequenceAssemblyWebinCli.setReportsDir(SEQUENCE_BASE_DIR);
            StringBuilder resultsSb = sequenceAssemblyWebinCli.validateTestTsv(testTsvFile);
            String expectedResults = new String(Files.readAllBytes(Paths.get(SEQUENCE_BASE_DIR  + File.separator + "Sequence-invalidSediment-expected-results.txt")));
            assertEquals(resultsSb.toString().trim(), expectedResults.trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void invalidEntryNumberStart() {
        try {
            String testTsvFile = SEQUENCE_BASE_DIR + File.separator + "Sequence-invalid-entrynumber-start-.tsv.gz";
            SequenceAssemblyWebinCli sequenceAssemblyWebinCli = new SequenceAssemblyWebinCli();
            sequenceAssemblyWebinCli.setReportsDir(SEQUENCE_BASE_DIR);
            StringBuilder resultsSb = sequenceAssemblyWebinCli.validateTestTsv(testTsvFile);
            String expectedResults = new String(Files.readAllBytes(Paths.get(SEQUENCE_BASE_DIR  + File.separator + "Sequence-invalidEntrynumberStart-expected-results.txt")));
            assertEquals(resultsSb.toString().trim(), expectedResults.trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void nonAsciiCharacters() {
        try {
            String testTsvFile = SEQUENCE_BASE_DIR + File.separator + "Sequence-non-ascii-characters.gz";
            SequenceAssemblyWebinCli sequenceAssemblyWebinCli = new SequenceAssemblyWebinCli();
            sequenceAssemblyWebinCli.setReportsDir(SEQUENCE_BASE_DIR);
            StringBuilder resultsSb = sequenceAssemblyWebinCli.validateTestTsv(testTsvFile);
            String expectedResults = new String(Files.readAllBytes(Paths.get(SEQUENCE_BASE_DIR  + File.separator + "Sequence-nonAsciiCharacters-expected-results.txt")));
            assertEquals(resultsSb.toString().replaceAll("\\s+",""), expectedResults.replaceAll("\\s+", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
