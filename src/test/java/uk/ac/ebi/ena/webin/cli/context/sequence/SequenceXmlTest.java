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
package uk.ac.ebi.ena.webin.cli.context.sequence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.*;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Analysis;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

public class SequenceXmlTest {
  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  private static final String NAME = "test_sequence";

  private static SequenceManifest getDefaultManifest() {
    SequenceManifest manifest = new SequenceManifest();
    manifest.setName(NAME);
    manifest.setStudy(new Study());
    manifest.getStudy().setBioProjectId("test_study");
    manifest.setDescription("test_description");
    return manifest;
  }

  private static Collection<SubmissionBundle> prepareSubmissionBundle(SequenceManifest manifest) {
    SequenceManifestReader manifestReader = mock(SequenceManifestReader.class);
    when(manifestReader.getManifests()).thenReturn(Arrays.asList(manifest));
    WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    parameters.setOutputDir(WebinCliTestUtils.createTempDir());
    parameters.setManifestFile(TempFileBuilder.empty().toFile());
    parameters.setTest(false);
    WebinCliExecutor<SequenceManifest, ValidationResponse> executor =
        (WebinCliExecutor<SequenceManifest, ValidationResponse>)
            WebinCliContext.sequence.createExecutor(parameters, manifestReader);
    executor.prepareSubmissionBundles();
    return executor.getSubmissionBundles();
  }

  @Test
  public void testRunAndAnalysisRef() {
    SequenceManifest manifest = getDefaultManifest();
    manifest.addAnalysis(
        new Analysis("ANALYSIS_ID1", "ANALYSIS_ID1_ALIAS"),
        new Analysis("ANALYSIS_ID2", "ANALYSIS_ID2_ALIAS"));
    manifest.addRun(new Run("RUN_ID1", "RUN_ID1_ALIAS"), new Run("RUN_ID2", "RUN_ID2_ALIAS"));
    manifest.setSubmissionTool("ST-001");
    manifest.setSubmissionToolVersion("STV-001");

    SubmissionBundle sb = prepareSubmissionBundle(manifest).stream().findFirst().get();

    String analysisXml =
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.ANALYSIS).getXmlContent();

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "<ANALYSIS>\n"
            + "<TITLE>Sequence assembly: test_sequence</TITLE>\n"
            + "<DESCRIPTION>test_description</DESCRIPTION>\n"
            + "<STUDY_REF accession=\"test_study\"/>\n"
            + "    <RUN_REF accession=\"RUN_ID1\"/>\n"
            + "    <RUN_REF accession=\"RUN_ID2\"/>\n"
            + "    <ANALYSIS_REF accession=\"ANALYSIS_ID1\"/>\n"
            + "    <ANALYSIS_REF accession=\"ANALYSIS_ID2\"/>\n"
            + "<ANALYSIS_TYPE>\n"
            + "<SEQUENCE_FLATFILE/>\n"
            + "</ANALYSIS_TYPE>\n"
            + "    <FILES />\n"
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
            + "</ANALYSIS>\n"
            + "</ANALYSIS_SET>");
  }

  @Test
  public void testSubmissionXml() {
    SequenceManifest manifest = getDefaultManifest();
    manifest.addAnalysis(
        new Analysis("ANALYSIS_ID1", "ANALYSIS_ID1_ALIAS"),
        new Analysis("ANALYSIS_ID2", "ANALYSIS_ID2_ALIAS"));
    manifest.addRun(new Run("RUN_ID1", "RUN_ID1_ALIAS"), new Run("RUN_ID2", "RUN_ID2_ALIAS"));
    manifest.setSubmissionTool("ST-001");
    manifest.setSubmissionToolVersion("STV-001");

    SubmissionBundle sb = prepareSubmissionBundle(manifest).stream().findFirst().get();

    XmlTester.assertSubmissionXmlWithEmptyManifestFile(
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.SUBMISSION).getXmlContent());
  }

  @Test
  public void testFlatFile() {
    SequenceManifest manifest = getDefaultManifest();

    Path flatFile = TempFileBuilder.gzip("flatfile.dat.gz", "ID   ;");
    manifest.files().add(new SubmissionFile(SequenceManifest.FileType.FLATFILE, flatFile.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest).stream().findFirst().get();

    String analysisXml =
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.ANALYSIS).getXmlContent();

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "<ANALYSIS>\n"
            + "<TITLE>Sequence assembly: test_sequence</TITLE>\n"
            + "<DESCRIPTION>test_description</DESCRIPTION>\n"
            + "<STUDY_REF accession=\"test_study\"/>\n"
            + "<ANALYSIS_TYPE>\n"
            + "<SEQUENCE_FLATFILE/>\n"
            + "</ANALYSIS_TYPE>\n"
            + "<FILES>\n"
            + "      <FILE filename=\"webin-cli/sequence/"
            + NAME
            + "/"
            + flatFile.getFileName()
            + "\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n"
            + "</FILES>\n"
            + "</ANALYSIS>\n"
            + "</ANALYSIS_SET>");
  }

  @Test
  public void testAuthorsAndAddress() {
    SequenceManifest manifest = getDefaultManifest();

    manifest.setAuthors("test_author1,test_author2.");
    manifest.setAddress("ena,ebi,embl,UK");

    SubmissionBundle sb = prepareSubmissionBundle(manifest).stream().findFirst().get();

    String analysisXml =
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.ANALYSIS).getXmlContent();

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "<ANALYSIS>\n"
            + "<TITLE>Sequence assembly: test_sequence</TITLE>\n"
            + "<DESCRIPTION>test_description</DESCRIPTION>\n"
            + "<STUDY_REF accession=\"test_study\"/>\n"
            + "<ANALYSIS_TYPE>\n"
            + "<SEQUENCE_FLATFILE>\n"
            + "<AUTHORS>test_author1,test_author2.</AUTHORS>\n"
            + "<ADDRESS>ena,ebi,embl,UK</ADDRESS>\n"
            + "</SEQUENCE_FLATFILE>\n"
            + "</ANALYSIS_TYPE>\n"
            + "<FILES />\n"
            + "</ANALYSIS>\n"
            + "</ANALYSIS_SET>");
  }
}
