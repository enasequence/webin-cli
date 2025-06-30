/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.context.polysample;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.*;

public class PolySampleXmlTest {
  private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/sequence");

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testPolySampleFull() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder())
            .field("ANALYSIS_TYPE", "SEQUENCE_SET")
            .field("ANALYSIS_PROTOCOL", "TEST")
            .field("ANALYSIS_DATE", "2021-11-12")
            .field("TARGET_LOCUS", "12S")
            .field("ANALYSIS_CODE", "TEST")
            .field("ANALYSIS_VERSION", "1.0")
            .field("ORGANELLE", "mitochondrion")
            .field("FORWARD_PRIMER_NAME", "TEST")
            .field("FORWARD_PRIMER_SEQUENCE", "TEST")
            .field("REVERSE_PRIMER_NAME", "TEST")
            .field("REVERSE_PRIMER_SEQUENCE", "TEST")
            .field("ANALYSIS_CENTER", "TEST")
            .file("SAMPLE_TSV", "valid/ERT000061-polysample-sample_tsv.tsv.gz")
            .file("TAX_TSV", "valid/ERT000061-polysample-tax_tsv.tsv.gz")
            .file("FASTA", "valid/valid.fasta.gz");

    String analysisXml = getGeneratedXml(manifestBuilder, "analysis.xml");

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "<ANALYSIS>\n"
            + "<TITLE>Polysample: test_sequence</TITLE>\n"
            + "<DESCRIPTION>test_description</DESCRIPTION>\n"
            + "<STUDY_REF accession=\"PRJNA272616\"/>\n"
            + "<ANALYSIS_TYPE>\n"
            + "<ENVIRONMENTAL_SEQUENCE_SET/>\n"
            + "</ANALYSIS_TYPE>\n"
            + "<FILES>\n"
            + "<FILE filename=\"webin-cli-test/polysample/test_sequence/valid.fasta.gz\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"74aa17746f4f2b20299d46c9f5127c02\"/>\n"
            + "<FILE filename=\"webin-cli-test/polysample/test_sequence/ERT000061-polysample-sample_tsv.tsv.gz\" filetype=\"sample_tsv\" checksum_method=\"MD5\" checksum=\"b68cb46a6511aa16594e443cbdd409b3\"/>\n"
            + "<FILE filename=\"webin-cli-test/polysample/test_sequence/ERT000061-polysample-tax_tsv.tsv.gz\" filetype=\"tax_tsv\" checksum_method=\"MD5\" checksum=\"43cd1b9dc53f1e0830d5155f0c8aa2e6\"/>\n"
            + "</FILES>\n"
            + "<ANALYSIS_ATTRIBUTES>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>REVERSE PRIMER NAME</TAG>\n"
            + "<VALUE>TEST</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>ANALYSIS_DATE</TAG>\n"
            + "<VALUE>2021-11-12</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>FORWARD PRIMER SEQUENCE</TAG>\n"
            + "<VALUE>TEST</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>ANALYSIS_CODE</TAG>\n"
            + "<VALUE>TEST</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>FORWARD PRIMER NAME</TAG>\n"
            + "<VALUE>TEST</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>ANALYSIS_VERSION</TAG>\n"
            + "<VALUE>1.0</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>ANALYSIS_TYPE</TAG>\n"
            + "<VALUE>SEQUENCE_SET</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>ANALYSIS_PROTOCOL</TAG>\n"
            + "<VALUE>TEST</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>TARGET_LOCUS</TAG>\n"
            + "<VALUE>12S</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>REVERSE PRIMER SEQUENCE</TAG>\n"
            + "<VALUE>TEST</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>ORGANELLE</TAG>\n"
            + "<VALUE>mitochondrion</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "<ANALYSIS_ATTRIBUTE>\n"
            + "<TAG>ANALYSIS_CENTER</TAG>\n"
            + "<VALUE>TEST</VALUE>\n"
            + "</ANALYSIS_ATTRIBUTE>\n"
            + "</ANALYSIS_ATTRIBUTES>\n"
            + "</ANALYSIS>\n"
            + "</ANALYSIS_SET>");
  }

  @Test
  public void testPolySampleInvalidFiles() {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder())
            .field("ANALYSIS_TYPE", "SEQUENCE_SET")
            .file("FASTA", "valid/valid.fasta.gz");

    Throwable t =
        assertThrows(
            WebinCliException.class, () -> getGeneratedXml(manifestBuilder, "analysis.xml"));

    assertTrue(t.getMessage().contains("Invalid manifest file"));
  }

  @Test
  public void testSubmissionXml() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder())
            .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
            .field("RUN_REF", "ERR2836762, ERR2836753")
            .field("SUBMISSION_TOOL", "ST-001")
            .field("SUBMISSION_TOOL_VERSION", "STV-001")
            .file("SAMPLE_TSV", "valid/ERT000061-polysample-sample_tsv.tsv.gz")
            .file("TAX_TSV", "valid/ERT000061-polysample-tax_tsv.tsv.gz")
            .file("FASTA", "valid/valid.fasta.gz");

    String submissionXml = getGeneratedXml(manifestBuilder, "submission.xml");

    String expected =
        "<SUBMISSION_SET>\n"
            + "<SUBMISSION>\n"
            + "<ACTIONS>\n"
            + "<ACTION>\n"
            + "<ADD/>\n"
            + "</ACTION>\n"
            + "</ACTIONS>\n"
            + "<SUBMISSION_ATTRIBUTES>\n"
            + "<SUBMISSION_ATTRIBUTE>\n"
            + "<TAG>ENA-SUBMISSION-TOOL</TAG>\n"
            + "<VALUE>WebinCli</VALUE>\n"
            + "</SUBMISSION_ATTRIBUTE>\n"
            + "<SUBMISSION_ATTRIBUTE>\n"
            + "<TAG>ENA-MANIFEST-FILE</TAG>\n"
            + "<VALUE><![CDATA[NAME\ttest_sequence\n"
            + "STUDY\tSRP052303\n"
            + "DESCRIPTION\ttest_description\n"
            + "ANALYSIS_REF\tERZ690501, ERZ690500\n"
            + "RUN_REF\tERR2836762, ERR2836753\n"
            + "SUBMISSION_TOOL\tST-001\n"
            + "SUBMISSION_TOOL_VERSION\tSTV-001\n"
            + "SAMPLE_TSV\tvalid/ERT000061-polysample-sample_tsv.tsv.gz\n"
            + "TAX_TSV\tvalid/ERT000061-polysample-tax_tsv.tsv.gz\n"
            + "FASTA\tvalid/valid.fasta.gz]]></VALUE>\n"
            + "</SUBMISSION_ATTRIBUTE>\n"
            + "<SUBMISSION_ATTRIBUTE>\n"
            + "<TAG>ENA-MANIFEST-FILE-MD5</TAG>\n"
            + "<VALUE>3a9997203c06cd9cd3e95f3cde04d2cd</VALUE>\n"
            + "</SUBMISSION_ATTRIBUTE>\n"
            + "</SUBMISSION_ATTRIBUTES>\n"
            + "</SUBMISSION>\n"
            + "</SUBMISSION_SET>";

    XmlTester.assertXml(submissionXml, expected);
  }

  private ManifestBuilder addDefaultFields(ManifestBuilder manifestBuilder) {
    return manifestBuilder
        .field("NAME", "test_sequence")
        .field("STUDY", "SRP052303")
        .field("DESCRIPTION", "test_description");
  }

  private String getGeneratedXml(ManifestBuilder manifestBuilder, String xmlFileName)
      throws Throwable {
    WebinCli webinCli =
        WebinCliBuilder.createForPolySample().submit(false).build(RESOURCE_DIR, manifestBuilder);
    webinCli.execute();

    Path generatedXml =
        webinCli
            .getParameters()
            .getOutputDir()
            .toPath()
            .resolve(webinCli.getParameters().getContext().toString())
            .resolve("test_sequence")
            .resolve("submit")
            .resolve(xmlFileName);

    return new String(Files.readAllBytes(generatedXml));
  }
}
