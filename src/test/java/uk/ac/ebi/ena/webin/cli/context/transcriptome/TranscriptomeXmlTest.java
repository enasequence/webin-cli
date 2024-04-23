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
package uk.ac.ebi.ena.webin.cli.context.transcriptome;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.*;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldGroup;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Analysis;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

public class TranscriptomeXmlTest {

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  private static final String NAME = "test_transcriptome";

  private static TranscriptomeManifest getDefaultManifest() {
    TranscriptomeManifest manifest = new TranscriptomeManifest();
    manifest.setName(NAME);
    manifest.setDescription("test_description");
    manifest.setSample(WebinCliTestUtils.getDefaultSample());
    manifest.getSample().setBioSampleId("test_sample");
    manifest.setStudy(new Study());
    manifest.getStudy().setBioProjectId("test_study");
    manifest.setProgram("test_program");
    manifest.setPlatform("test_platform");
    return manifest;
  }

  private static Collection<SubmissionBundle> prepareSubmissionBundle(TranscriptomeManifest manifest) {
    ManifestFieldGroup fieldGroup = mock(ManifestFieldGroup.class);

    TranscriptomeManifestReader manifestReader = mock(TranscriptomeManifestReader.class);
    when(manifestReader.getManifests()).thenReturn(Arrays.asList(manifest));
    when(manifestReader.getManifestFieldGroup(manifest)).thenReturn(fieldGroup);
    WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    parameters.setOutputDir(WebinCliTestUtils.createTempDir());
    parameters.setManifestFile(TempFileBuilder.empty().toFile());
    parameters.setTest(false);
    WebinCliExecutor<TranscriptomeManifest, ValidationResponse> executor =
        (WebinCliExecutor<TranscriptomeManifest, ValidationResponse>)
            WebinCliContext.transcriptome.createExecutor(parameters, manifestReader);
    executor.prepareSubmissionBundles(false);
    return executor.getSubmissionBundles();
  }

  @Test
  public void testAnalysisAndRunRef() {
    TranscriptomeManifest manifest = getDefaultManifest();
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
            + "  <ANALYSIS>\n"
            + "    <TITLE>Transcriptome assembly: test_transcriptome</TITLE>\n"
            + "    <DESCRIPTION>test_description</DESCRIPTION>\n"
            + "    <STUDY_REF accession=\"test_study\" />\n"
            + "    <SAMPLE_REF accession=\"test_sample\" />\n"
            + "    <RUN_REF accession=\"RUN_ID1\"/>\n"
            + "    <RUN_REF accession=\"RUN_ID2\"/>\n"
            + "    <ANALYSIS_REF accession=\"ANALYSIS_ID1\"/>\n"
            + "    <ANALYSIS_REF accession=\"ANALYSIS_ID2\"/>\n"
            + "    <ANALYSIS_TYPE>\n"
            + "      <TRANSCRIPTOME_ASSEMBLY>\n"
            + "        <NAME>test_transcriptome</NAME>\n"
            + "        <PROGRAM>test_program</PROGRAM>\n"
            + "        <PLATFORM>test_platform</PLATFORM>\n"
            + "      </TRANSCRIPTOME_ASSEMBLY>\n"
            + "    </ANALYSIS_TYPE>\n"
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
            + "  </ANALYSIS>\n"
            + "</ANALYSIS_SET>\n");
  }

  @Test
  public void testSubmissionXml() {
    TranscriptomeManifest manifest = getDefaultManifest();
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
  public void testTpa() {
    TranscriptomeManifest manifest = getDefaultManifest();
    manifest.setTpa(true);
    SubmissionBundle sb = prepareSubmissionBundle(manifest).stream().findFirst().get();

    String analysisXml =
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.ANALYSIS).getXmlContent();

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "  <ANALYSIS>\n"
            + "    <TITLE>Transcriptome assembly: test_transcriptome</TITLE>\n"
            + "    <DESCRIPTION>test_description</DESCRIPTION>\n"
            + "    <STUDY_REF accession=\"test_study\" />\n"
            + "    <SAMPLE_REF accession=\"test_sample\" />\n"
            + "    <ANALYSIS_TYPE>\n"
            + "      <TRANSCRIPTOME_ASSEMBLY>\n"
            + "        <NAME>test_transcriptome</NAME>\n"
            + "        <PROGRAM>test_program</PROGRAM>\n"
            + "        <PLATFORM>test_platform</PLATFORM>\n"
            + "        <TPA>true</TPA>\n"
            + "      </TRANSCRIPTOME_ASSEMBLY>\n"
            + "    </ANALYSIS_TYPE>\n"
            + "    <FILES />\n"
            + "  </ANALYSIS>\n"
            + "</ANALYSIS_SET>\n");
  }

  @Test
  public void testFastaFile() {
    TranscriptomeManifest manifest = getDefaultManifest();
    Path fastaFile = TempFileBuilder.gzip("fasta.gz", ">123\nACGT");
    manifest
        .files()
        .add(new SubmissionFile(TranscriptomeManifest.FileType.FASTA, fastaFile.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest).stream().findFirst().get();

    String analysisXml =
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.ANALYSIS).getXmlContent();

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "  <ANALYSIS>\n"
            + "    <TITLE>Transcriptome assembly: test_transcriptome</TITLE>\n"
            + "    <DESCRIPTION>test_description</DESCRIPTION>\n"
            + "    <STUDY_REF accession=\"test_study\" />\n"
            + "    <SAMPLE_REF accession=\"test_sample\" />\n"
            + "    <ANALYSIS_TYPE>\n"
            + "      <TRANSCRIPTOME_ASSEMBLY>\n"
            + "        <NAME>test_transcriptome</NAME>\n"
            + "        <PROGRAM>test_program</PROGRAM>\n"
            + "        <PLATFORM>test_platform</PLATFORM>\n"
            + "      </TRANSCRIPTOME_ASSEMBLY>\n"
            + "    </ANALYSIS_TYPE>\n"
            + "    <FILES>\n"
            + "      <FILE filename=\"webin-cli/transcriptome/"
            + NAME
            + "/"
            + fastaFile.getFileName()
            + "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n"
            + "    </FILES>\n"
            + "  </ANALYSIS>\n"
            + "</ANALYSIS_SET>\n");
  }

  @Test
  public void testFlatFile() {
    TranscriptomeManifest manifest = getDefaultManifest();

    Path fastaFile = TempFileBuilder.gzip("flatfile.dat.gz", ">123\nACGT");
    manifest
        .files()
        .add(new SubmissionFile(TranscriptomeManifest.FileType.FLATFILE, fastaFile.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest).stream().findFirst().get();

    String analysisXml =
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.ANALYSIS).getXmlContent();

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "  <ANALYSIS>\n"
            + "    <TITLE>Transcriptome assembly: test_transcriptome</TITLE>\n"
            + "    <DESCRIPTION>test_description</DESCRIPTION>\n"
            + "    <STUDY_REF accession=\"test_study\" />\n"
            + "    <SAMPLE_REF accession=\"test_sample\" />\n"
            + "    <ANALYSIS_TYPE>\n"
            + "      <TRANSCRIPTOME_ASSEMBLY>\n"
            + "        <NAME>test_transcriptome</NAME>\n"
            + "        <PROGRAM>test_program</PROGRAM>\n"
            + "        <PLATFORM>test_platform</PLATFORM>\n"
            + "      </TRANSCRIPTOME_ASSEMBLY>\n"
            + "    </ANALYSIS_TYPE>\n"
            + "    <FILES>\n"
            + "      <FILE filename=\"webin-cli/transcriptome/"
            + NAME
            + "/"
            + fastaFile.getFileName()
            + "\" filetype=\"flatfile\" checksum_method=\"MD5\" checksum=\"661926c1c03b059929caaead3ea351a3\" />\n"
            + "    </FILES>\n"
            + "  </ANALYSIS>\n"
            + "</ANALYSIS_SET>\n");
  }
}
