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
package uk.ac.ebi.ena.webin.cli.context.taxrefset;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.WebinCliSubmissionTest;
import uk.ac.ebi.ena.webin.cli.XmlTester;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

public class TaxRefSetXmlTest {

  private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/taxxrefset");

  private static final String NAME = "test_taxon_xref_set";

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testFastaAndTsvFile() throws Throwable {
    ManifestBuilder manifestBuilder = addDefaultFields(new ManifestBuilder())
        .file("FASTA", "valid/valid.fasta.gz")
        .file("TAB", "valid/valid_w_customs.tsv.gz");

    String analysisXml = getGeneratedXml(manifestBuilder, "analysis.xml");

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "<ANALYSIS>\n"
            + "<TITLE>Taxonomy reference set: test_taxon_xref_set</TITLE>\n"
            + "<DESCRIPTION>test_description</DESCRIPTION>\n"
            + "<STUDY_REF accession=\"PRJNA272616\"/>\n"
            + "<ANALYSIS_TYPE>\n"
            + "<TAXONOMIC_REFERENCE_SET>\n"
            + "<NAME>test_taxon_xref_set</NAME>\n"
            + "<TAXONOMY_SYSTEM>NCBI</TAXONOMY_SYSTEM>\n"
            + "<TAXONOMY_SYSTEM_VERSION>1</TAXONOMY_SYSTEM_VERSION>\n"
            + "<CUSTOM_FIELDS>\n"
            + "<FIELD>\n"
            + "<NAME>Annotation</NAME>\n"
            + "<DESCRIPTION>Source of annotation</DESCRIPTION>\n"
            + "</FIELD>\n"
            + "<FIELD>\n"
            + "<NAME>ITSoneDB URL</NAME>\n"
            + "<DESCRIPTION>URL within ITSoneDB</DESCRIPTION>\n"
            + "</FIELD>\n"
            + "</CUSTOM_FIELDS>\n"
            + "</TAXONOMIC_REFERENCE_SET>\n"
            + "</ANALYSIS_TYPE>\n"
            + "<FILES>\n"
            + "      <FILE filename=\"webin-cli-test/taxrefset/test_taxon_xref_set/valid.fasta.gz\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"7ff83b2954a1a9ac8f5e2a1d067d05be\" />\n"
            + "      <FILE filename=\"webin-cli-test/taxrefset/test_taxon_xref_set/valid_w_customs.tsv.gz\" filetype=\"tab\" checksum_method=\"MD5\" checksum=\"519fb2891a2b356983e278209989df33\"/>\n"
            + "</FILES>\n"
            + "</ANALYSIS>\n"
            + "</ANALYSIS_SET>");
  }

  @Test
  public void testSubmissionXml() throws Throwable {
    ManifestBuilder manifestBuilder = addDefaultFields(new ManifestBuilder())
        .file("FASTA", "valid/valid.fasta.gz")
        .file("TAB", "valid/valid_w_customs.tsv.gz");

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
            + "            <VALUE><![CDATA[NAME\ttest_taxon_xref_set\nDESCRIPTION\ttest_description\nSTUDY\tSRP052303\nTAXONOMY_SYSTEM\tNCBI\nTAXONOMY_SYSTEM_VERSION\t1\nCUSTOM_FIELD\tAnnotation:Source of annotation\nCUSTOM_FIELD\tITSoneDB URL:URL within ITSoneDB\nFASTA\tvalid/valid.fasta.gz\nTAB\tvalid/valid_w_customs.tsv.gz]]></VALUE>\n"
            + "        </SUBMISSION_ATTRIBUTE>\n"
            + "        <SUBMISSION_ATTRIBUTE>\n"
            + "            <TAG>ENA-MANIFEST-FILE-MD5</TAG>\n"
            + "            <VALUE>3e986982764e706803351c4c11a3f426</VALUE>\n"
            + "        </SUBMISSION_ATTRIBUTE>\n"
            + "    </SUBMISSION_ATTRIBUTES>\n"
            + "  </SUBMISSION>\n"
            + "</SUBMISSION_SET>";

    XmlTester.assertXml(submissionXml, expected);
  }

  private ManifestBuilder addDefaultFields(ManifestBuilder manifestBuilder) {
    return manifestBuilder
        .field("NAME", "test_taxon_xref_set")
        .field("DESCRIPTION", "test_description")
        .field("STUDY", "SRP052303")
        .field("TAXONOMY_SYSTEM", "NCBI")
        .field("TAXONOMY_SYSTEM_VERSION", "1")
        .field("CUSTOM_FIELD", "Annotation:Source of annotation")
        .field("CUSTOM_FIELD", "ITSoneDB URL:URL within ITSoneDB");
  }

  private String getGeneratedXml(ManifestBuilder manifestBuilder, String xmlFileName) throws Throwable {
    WebinCli webinCli = new WebinCliBuilder(WebinCliContext.taxrefset).submit(false)
        .build(RESOURCE_DIR, manifestBuilder);
    webinCli.execute();

    Path generatedXml = webinCli.getParameters().getOutputDir().toPath()
        .resolve(webinCli.getParameters().getContext().toString())
        .resolve("test_taxon_xref_set")
        .resolve("submit")
        .resolve(xmlFileName);

    return new String(Files.readAllBytes(generatedXml));
  }
}
