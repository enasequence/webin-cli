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
package uk.ac.ebi.ena.webin.cli.context.genome;

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

public class GenomeXmlTest {

  private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/genome");

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testRunAndAnalysisRef() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder())
            .field("DESCRIPTION", "test_description")
            .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
            .field("RUN_REF", "ERR2836762, ERR2836753")
            .field("AUTHORS", "test_author1,test_author2.")
            .field("ADDRESS", "ena,ebi,embl,UK")
            .field("SUBMISSION_TOOL", "ST-001")
            .field("SUBMISSION_TOOL_VERSION", "STV-001")
            .file("FLATFILE", "valid.flatfile.gz");

    String analysisXml = getGeneratedXml(manifestBuilder, "analysis.xml");

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "  <ANALYSIS>\n"
            + "    <TITLE>Genome assembly: test_genome</TITLE>\n"
            + "    <DESCRIPTION>test_description</DESCRIPTION>\n"
            + "    <STUDY_REF accession=\"PRJNA272616\" />\n"
            + "    <SAMPLE_REF accession=\"SAMEA4734564\" />\n"
            + "    <RUN_REF accession=\"ERR2836762\"/>\n"
            + "    <RUN_REF accession=\"ERR2836753\"/>\n"
            + "    <ANALYSIS_REF accession=\"ERZ690501\"/>\n"
            + "    <ANALYSIS_REF accession=\"ERZ690500\"/>\n"
            + "    <ANALYSIS_TYPE>\n"
            + "      <SEQUENCE_ASSEMBLY>\n"
            + "        <NAME>test_genome</NAME>\n"
            + "        <TYPE>clone or isolate</TYPE>\n"
            + "        <PARTIAL>false</PARTIAL>\n"
            + "        <COVERAGE>1</COVERAGE>\n"
            + "        <PROGRAM>test_program</PROGRAM>\n"
            + "        <PLATFORM>test_platform</PLATFORM>\n"
            + "        <MOL_TYPE>genomic DNA</MOL_TYPE>\n"
            + "        <AUTHORS>test_author1,test_author2.</AUTHORS>\n"
            + "        <ADDRESS>ena,ebi,embl,UK</ADDRESS>\n"
            + "      </SEQUENCE_ASSEMBLY>\n"
            + "    </ANALYSIS_TYPE>\n"
            + "    <FILES>\n"
            + "      <FILE filename=\"webin-cli-test/genome/test_genome/valid.flatfile.gz\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"ff20876a8ad754ecae0979af92a84cbc\"/>\n"
            + "    </FILES>\n"
            + "    <ANALYSIS_ATTRIBUTES>\n"
            + "        <ANALYSIS_ATTRIBUTE>\n"
            + "            <TAG>SUBMISSION_TOOL</TAG>\n"
            + "            <VALUE>ST-001</VALUE>\n"
            + "        </ANALYSIS_ATTRIBUTE>\n"
            + "        <ANALYSIS_ATTRIBUTE>\n"
            + "            <TAG>SUBMISSION_TOOL_VERSION</TAG>\n"
            + "            <VALUE>STV-001</VALUE>\n"
            + "        </ANALYSIS_ATTRIBUTE>\n"
            + "    </ANALYSIS_ATTRIBUTES>\n"
            + "  </ANALYSIS>\n"
            + "</ANALYSIS_SET>\n");
  }

  @Test
  public void testFastaFile() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder()).file("FASTA", "valid.fasta.gz");

    String analysisXml = getGeneratedXml(manifestBuilder, "analysis.xml");

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "  <ANALYSIS>\n"
            + "    <TITLE>Genome assembly: test_genome</TITLE>\n"
            + "    <STUDY_REF accession=\"PRJNA272616\" />\n"
            + "    <SAMPLE_REF accession=\"SAMEA4734564\" />\n"
            + "    <ANALYSIS_TYPE>\n"
            + "      <SEQUENCE_ASSEMBLY>\n"
            + "        <NAME>test_genome</NAME>\n"
            + "        <TYPE>clone or isolate</TYPE>\n"
            + "        <PARTIAL>false</PARTIAL>\n"
            + "        <COVERAGE>1</COVERAGE>\n"
            + "        <PROGRAM>test_program</PROGRAM>\n"
            + "        <PLATFORM>test_platform</PLATFORM>\n"
            + "        <MOL_TYPE>genomic DNA</MOL_TYPE>\n"
            + "      </SEQUENCE_ASSEMBLY>\n"
            + "    </ANALYSIS_TYPE>\n"
            + "    <FILES>\n"
            + "      <FILE filename=\"webin-cli-test/genome/test_genome/valid.fasta.gz\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"4d5b603a968abcec9e377cdcd172af33\" />\n"
            + "    </FILES>\n"
            + "  </ANALYSIS>\n"
            + "</ANALYSIS_SET>\n");
  }

  @Test
  public void testSubmissionXml() throws Throwable {
    ManifestBuilder manifestBuilder =
        addDefaultFields(new ManifestBuilder())
            .field("ANALYSIS_REF", "ERZ690501, ERZ690500")
            .field("RUN_REF", "ERR2836762, ERR2836753")
            .field("SUBMISSION_TOOL", "ST-001")
            .field("SUBMISSION_TOOL_VERSION", "STV-001")
            .file("FASTA", "valid.fasta.gz");

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
            + "            <VALUE><![CDATA[NAME\ttest_genome\nASSEMBLY_TYPE\tclone or isolate\nSAMPLE\tERS2554688\nSTUDY\tSRP052303\nCOVERAGE\t1\nPROGRAM\ttest_program\nPLATFORM\ttest_platform\nANALYSIS_REF\tERZ690501, ERZ690500\nRUN_REF\tERR2836762, ERR2836753\nSUBMISSION_TOOL\tST-001\nSUBMISSION_TOOL_VERSION\tSTV-001\nFASTA\tvalid.fasta.gz]]></VALUE>\n"
            + "        </SUBMISSION_ATTRIBUTE>\n"
            + "        <SUBMISSION_ATTRIBUTE>\n"
            + "            <TAG>ENA-MANIFEST-FILE-MD5</TAG>\n"
            + "            <VALUE>d61d2f2c51d8b95868479af98886cf57</VALUE>\n"
            + "        </SUBMISSION_ATTRIBUTE>\n"
            + "    </SUBMISSION_ATTRIBUTES>\n"
            + "  </SUBMISSION>\n"
            + "</SUBMISSION_SET>";

    XmlTester.assertXml(submissionXml, expected);
  }

  private ManifestBuilder addDefaultFields(ManifestBuilder manifestBuilder) {
    return manifestBuilder
        .field("NAME", "test_genome")
        .field("ASSEMBLY_TYPE", "clone or isolate")
        .field("SAMPLE", "ERS2554688")
        .field("STUDY", "SRP052303")
        .field("COVERAGE", "1")
        .field("PROGRAM", "test_program")
        .field("PLATFORM", "test_platform");
  }

  private String getGeneratedXml(ManifestBuilder manifestBuilder, String xmlFileName)
      throws Throwable {
    WebinCli webinCli =
        WebinCliBuilder.createForGenome().submit(false).build(RESOURCE_DIR, manifestBuilder);
    webinCli.execute();

    Path generatedXml =
        webinCli
            .getParameters()
            .getOutputDir()
            .toPath()
            .resolve(webinCli.getParameters().getContext().toString())
            .resolve("test_genome")
            .resolve("submit")
            .resolve(xmlFileName);

    return new String(Files.readAllBytes(generatedXml));
  }
}
