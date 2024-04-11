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
package uk.ac.ebi.ena.webin.cli.context.reads;

import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliBuilder;
import uk.ac.ebi.ena.webin.cli.XmlTester;

public class ReadsXmlTest {

  private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/reads");

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testExperiment() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder())
            .field("LIBRARY_CONSTRUCTION_PROTOCOL", "Protocol")
            .field("SUBMISSION_TOOL", "ST-001")
            .field("SUBMISSION_TOOL_VERSION", "STV-001")
            .file("FASTQ", "valid.fastq.gz");

    String experimentXml = getGeneratedXml(manifestBuilder, "experiment.xml");

    XmlTester.assertXml(
        experimentXml,
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<EXPERIMENT_SET>\n"
            + " <EXPERIMENT alias=\"webin-reads-test_reads\">\n"
            + "  <TITLE>Raw reads: test_reads</TITLE>\n"
            + "   <STUDY_REF accession=\"PRJNA272616\" />\n"
            + "    <DESIGN>\n"
            + "     <DESIGN_DESCRIPTION>test_description</DESIGN_DESCRIPTION>\n"
            + "     <SAMPLE_DESCRIPTOR accession=\"SAMEA4734564\" />\n"
            + "     <LIBRARY_DESCRIPTOR>\n"
            + "       <LIBRARY_STRATEGY>CLONEEND</LIBRARY_STRATEGY>\n"
            + "       <LIBRARY_SOURCE>OTHER</LIBRARY_SOURCE>\n"
            + "       <LIBRARY_SELECTION>Inverse rRNA selection</LIBRARY_SELECTION>\n"
            + "       <LIBRARY_LAYOUT>\n"
            + "        <SINGLE />\n"
            + "       </LIBRARY_LAYOUT>\n"
            + "       <LIBRARY_CONSTRUCTION_PROTOCOL>Protocol</LIBRARY_CONSTRUCTION_PROTOCOL>\n"
            + "     </LIBRARY_DESCRIPTOR>\n"
            + "    </DESIGN>\n"
            + "    <PLATFORM>\n"
            + "     <ILLUMINA>\n"
            + "       <INSTRUMENT_MODEL>unspecified</INSTRUMENT_MODEL>\n"
            + "     </ILLUMINA>\n"
            + "    </PLATFORM>\n"
            + "    <EXPERIMENT_ATTRIBUTES>\n"
            + "        <EXPERIMENT_ATTRIBUTE>\n"
            + "            <TAG>SUBMISSION_TOOL</TAG>\n"
            + "            <VALUE>ST-001</VALUE>\n"
            + "        </EXPERIMENT_ATTRIBUTE>\n"
            + "        <EXPERIMENT_ATTRIBUTE>\n"
            + "            <TAG>SUBMISSION_TOOL_VERSION</TAG>\n"
            + "            <VALUE>STV-001</VALUE>\n"
            + "        </EXPERIMENT_ATTRIBUTE>\n"
            + "    </EXPERIMENT_ATTRIBUTES>\n"
            + " </EXPERIMENT>\n"
            + "</EXPERIMENT_SET>");
  }

  @Test
  public void testRunWithCramFile() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder())
            .field("SUBMISSION_TOOL", "ST-001")
            .field("SUBMISSION_TOOL_VERSION", "STV-001")
            .file("CRAM", "valid.cram");

    String runXml = getGeneratedXml(manifestBuilder, "run.xml");

    XmlTester.assertXml(
        runXml,
        "<RUN_SET>\n"
            + "  <RUN>\n"
            + "    <TITLE>Raw reads: test_reads</TITLE>\n"
            + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
            + "    <DATA_BLOCK>\n"
            + "      <FILES>\n"
            + "        <FILE filename=\"webin-cli-test/reads/test_reads/valid.cram\" filetype=\"cram\" checksum_method=\"MD5\" checksum=\"a126720856898632c01680313a010ff2\"/>\n"
            + "      </FILES>\n"
            + "    </DATA_BLOCK>\n"
            + "    <RUN_ATTRIBUTES>\n"
            + "        <RUN_ATTRIBUTE>\n"
            + "            <TAG>SUBMISSION_TOOL</TAG>\n"
            + "            <VALUE>ST-001</VALUE>\n"
            + "        </RUN_ATTRIBUTE>\n"
            + "        <RUN_ATTRIBUTE>\n"
            + "            <TAG>SUBMISSION_TOOL_VERSION</TAG>\n"
            + "            <VALUE>STV-001</VALUE>\n"
            + "        </RUN_ATTRIBUTE>\n"
            + "    </RUN_ATTRIBUTES>\n"
            + "  </RUN>\n"
            + "</RUN_SET>");
  }

  @Test
  public void testRunWithBamFile() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder()).file("BAM", "valid.bam");

    String runXml = getGeneratedXml(manifestBuilder, "run.xml");

    XmlTester.assertXml(
        runXml,
        "<RUN_SET>\n"
            + "  <RUN>\n"
            + "    <TITLE>Raw reads: test_reads</TITLE>\n"
            + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
            + "    <DATA_BLOCK>\n"
            + "      <FILES>\n"
            + "        <FILE filename=\"webin-cli-test/reads/test_reads/valid.bam\" filetype=\"bam\" checksum_method=\"MD5\" checksum=\"dc0c64b1a05e1ce118dab8f1a3feece9\"/>\n"
            + "      </FILES>\n"
            + "    </DATA_BLOCK>\n"
            + "  </RUN>\n"
            + "</RUN_SET>");
  }

  @Test
  public void testRunWithTwoFastqFiles() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder())
            .file("FASTQ", "valid_paired_1.fastq.gz")
            .file("FASTQ", "valid_paired_2.fastq.gz");

    String runXml = getGeneratedXml(manifestBuilder, "run.xml");

    XmlTester.assertXml(
        runXml,
        "<RUN_SET>\n"
            + "  <RUN>\n"
            + "    <TITLE>Raw reads: test_reads</TITLE>\n"
            + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
            + "    <DATA_BLOCK>\n"
            + "      <FILES>\n"
            + "        <FILE filename=\"webin-cli-test/reads/test_reads/valid_paired_1.fastq.gz\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"ffc88c295546725b457fcea36263d740\"/>\n"
            + "        <FILE filename=\"webin-cli-test/reads/test_reads/valid_paired_2.fastq.gz\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"16b124d9b7832b7614a4530e62d3cbbc\"/>\n"
            + "      </FILES>\n"
            + "    </DATA_BLOCK>\n"
            + "  </RUN>\n"
            + "</RUN_SET>");
  }

  @Test
  public void testRunWithReadTypeFastqFile() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder().jsonFormat())
            .file("FASTQ", "valid.fastq.gz")
            .attribute("READ_TYPE", "sample_barcode")
            .file("FASTQ", "valid_paired_1.fastq.gz")
            .attribute("READ_TYPE", "paired")
            .attribute("READ_TYPE", "umi_barcode")
            .file("FASTQ", "valid_paired_2.fastq.gz")
            .attribute("READ_TYPE", "paired")
            .attribute("READ_TYPE", "cell_barcode");

    String runXml = getGeneratedXml(manifestBuilder, "run.xml");

    XmlTester.assertXml(
        runXml,
        "<RUN_SET>\n"
            + "  <RUN>\n"
            + "    <TITLE>Raw reads: test_reads</TITLE>\n"
            + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
            + "    <DATA_BLOCK>\n"
            + "      <FILES>\n"
            + "        <FILE filename=\"webin-cli-test/reads/test_reads/valid.fastq.gz\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"3e6d022526f711b1e4383bc1391e2e97\">\n"
            + "          <READ_TYPE>sample_barcode</READ_TYPE>\n"
            + "        </FILE>\n"
            + "        <FILE filename=\"webin-cli-test/reads/test_reads/valid_paired_1.fastq.gz\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"ffc88c295546725b457fcea36263d740\">\n"
            + "          <READ_TYPE>paired</READ_TYPE>\n"
            + "          <READ_TYPE>umi_barcode</READ_TYPE>\n"
            + "        </FILE>\n"
            + "        <FILE filename=\"webin-cli-test/reads/test_reads/valid_paired_2.fastq.gz\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"16b124d9b7832b7614a4530e62d3cbbc\">\n"
            + "          <READ_TYPE>paired</READ_TYPE>\n"
            + "          <READ_TYPE>cell_barcode</READ_TYPE>\n"
            + "        </FILE>\n"
            + "      </FILES>\n"
            + "    </DATA_BLOCK>\n"
            + "  </RUN>\n"
            + "</RUN_SET>");
  }

  @Test
  public void testSubmissionXml() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder()).file("FASTQ", "valid.fastq.gz");

    String submissionXml = getGeneratedXml(manifestBuilder, "submission.xml");

    String expected =
        "<SUBMISSION_SET>\n"
            + "  <SUBMISSION>\n"
            + "    <ACTIONS>\n"
            + "        <ACTION>\n"
            + "               <ADD />\n"
            + "        </ACTION>\n"
            + "    </ACTIONS>\n"
            + "    <SUBMISSION_ATTRIBUTES>\n"
            + "        <SUBMISSION_ATTRIBUTE>\n"
            + "            <TAG>ENA-SUBMISSION-TOOL</TAG>\n"
            + "            <VALUE>WebinCli</VALUE>\n"
            + "        </SUBMISSION_ATTRIBUTE>\n"
            + "        <SUBMISSION_ATTRIBUTE>\n"
            + "            <TAG>ENA-MANIFEST-FILE</TAG>\n"
            + "            <VALUE><![CDATA[NAME\ttest_reads\nDESCRIPTION\ttest_description\nSAMPLE\tERS2554688\nSTUDY\tSRP052303\nPLATFORM\tILLUMINA\nINSTRUMENT\tunspecified\nLIBRARY_STRATEGY\tCLONEEND\nLIBRARY_SOURCE\tOTHER\nLIBRARY_SELECTION\tInverse rRNA selection\nFASTQ\tvalid.fastq.gz]]></VALUE>\n"
            + "        </SUBMISSION_ATTRIBUTE>\n"
            + "        <SUBMISSION_ATTRIBUTE>\n"
            + "            <TAG>ENA-MANIFEST-FILE-MD5</TAG>\n"
            + "            <VALUE>a3077be9942f870ccf3e3f3e327ef163</VALUE>\n"
            + "        </SUBMISSION_ATTRIBUTE>\n"
            + "    </SUBMISSION_ATTRIBUTES>\n"
            + "  </SUBMISSION>\n"
            + "</SUBMISSION_SET>";

    XmlTester.assertXml(submissionXml, expected);
  }

  private ManifestBuilder addDefaultFields(ManifestBuilder manifestBuilder) {
    return manifestBuilder
        .field("NAME", "test_reads")
        .field("DESCRIPTION", "test_description")
        .field("SAMPLE", "ERS2554688")
        .field("STUDY", "SRP052303")
        .field("PLATFORM", "ILLUMINA")
        .field("INSTRUMENT", "unspecified")
        .field("LIBRARY_STRATEGY", "CLONEEND")
        .field("LIBRARY_SOURCE", "OTHER")
        .field("LIBRARY_SELECTION", "Inverse rRNA selection");
  }

  private String getGeneratedXml(ManifestBuilder manifestBuilder, String xmlFileName)
      throws Throwable {
    WebinCli webinCli =
        WebinCliBuilder.createForReads().submit(false).build(RESOURCE_DIR, manifestBuilder);
    webinCli.execute();

    Path generatedXml =
        webinCli
            .getParameters()
            .getOutputDir()
            .toPath()
            .resolve(webinCli.getParameters().getContext().toString())
            .resolve("test_reads")
            .resolve("submit")
            .resolve(xmlFileName);

    return new String(Files.readAllBytes(generatedXml));
  }
}
