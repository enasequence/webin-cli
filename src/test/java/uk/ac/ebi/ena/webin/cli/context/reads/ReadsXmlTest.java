/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.context.reads;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

import java.nio.file.Path;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadsXmlTest {
  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  private static final String NAME = "test_reads";

  private static ReadsManifest getDefaultManifest() {
    ReadsManifest manifest = new ReadsManifest();
    manifest.setName(NAME);
    manifest.setDescription("test_description");
    manifest.setSample(WebinCliTestUtils.getDefaultSample());
    manifest.getSample().setBioSampleId("test_sample");
    manifest.setStudy(new Study());
    manifest.getStudy().setBioProjectId("test_study");
    return manifest;
  }

  private static SubmissionBundle prepareSubmissionBundle(ReadsManifest manifest) {
    ReadsManifestReader manifestReader = mock(ReadsManifestReader.class);
    when(manifestReader.getManifest()).thenReturn(manifest);
    WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();
    parameters.setOutputDir(WebinCliTestUtils.createTempDir());
    parameters.setManifestFile(WebinCliTestUtils.createEmptyTempFile().toFile());
    parameters.setTest(false);
    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        (WebinCliExecutor<ReadsManifest,ReadsValidationResponse>)
            WebinCliContext.reads.createExecutor(parameters, manifestReader);
    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> mockedExecutor = Mockito.spy(executor);
    ReadsValidationResponse rvr = new ReadsValidationResponse();
    Mockito.when(mockedExecutor.getValidationResponse()).thenReturn(rvr);
    mockedExecutor.prepareSubmissionBundle();
    return mockedExecutor.readSubmissionBundle();
  }

  @Test
  public void testExperiment() {
    ReadsManifest manifest = getDefaultManifest();

    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    manifest.setLibraryStrategy("CLONEEND");
    manifest.setLibrarySource("OTHER");
    manifest.setLibrarySelection("Inverse rRNA selection");

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String experimentXml =
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.EXPERIMENT).getXml();

    WebinCliTestUtils.assertXml(
        experimentXml,
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<EXPERIMENT_SET>\n"
            + " <EXPERIMENT alias=\"webin-reads-test-reads\">\n"
            + "  <TITLE>Raw reads: test_reads</TITLE>\n"
            + "   <STUDY_REF accession=\"test_study\" />\n"
            + "    <DESIGN>\n"
            + "     <DESIGN_DESCRIPTION>test_description</DESIGN_DESCRIPTION>\n"
            + "     <SAMPLE_DESCRIPTOR accession=\"test_sample\" />\n"
            + "     <LIBRARY_DESCRIPTOR>\n"
            + "       <LIBRARY_STRATEGY>CLONEEND</LIBRARY_STRATEGY>\n"
            + "       <LIBRARY_SOURCE>OTHER</LIBRARY_SOURCE>\n"
            + "       <LIBRARY_SELECTION>Inverse rRNA selection</LIBRARY_SELECTION>\n"
            + "       <LIBRARY_LAYOUT>\n"
            + "        <SINGLE />\n"
            + "       </LIBRARY_LAYOUT>\n"
            + "     </LIBRARY_DESCRIPTOR>\n"
            + "    </DESIGN>\n"
            + "    <PLATFORM>\n"
            + "     <ILLUMINA>\n"
            + "       <INSTRUMENT_MODEL>unspecified</INSTRUMENT_MODEL>\n"
            + "     </ILLUMINA>\n"
            + "    </PLATFORM>\n"
            + " </EXPERIMENT>\n"
            + "</EXPERIMENT_SET>");
  }

  @Test
  public void testRunWithCramFile() {
    ReadsManifest manifest = getDefaultManifest();
    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    Path file = WebinCliTestUtils.createEmptyTempFile("cram");
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.CRAM, file.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXml();

    WebinCliTestUtils.assertXml(
        runXml,
        "<RUN_SET>\n"
            + "  <RUN>\n"
            + "    <TITLE>Raw reads: test_reads</TITLE>\n"
            + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
            + "    <DATA_BLOCK>\n"
            + "      <FILES>\n"
            + "        <FILE filename=\"webin-cli/reads/"
            + NAME
            + "/"
            + file.getFileName()
            + "\" filetype=\"cram\" checksum_method=\"MD5\" checksum=\"d41d8cd98f00b204e9800998ecf8427e\"/>\n"
            + "      </FILES>\n"
            + "    </DATA_BLOCK>\n"
            + "  </RUN>\n"
            + "</RUN_SET>");
  }

  @Test
  public void testRunWithBamFile() {
    ReadsManifest manifest = getDefaultManifest();
    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    Path file = WebinCliTestUtils.createEmptyTempFile("bam");
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.BAM, file.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXml();

    WebinCliTestUtils.assertXml(
            runXml,
            "<RUN_SET>\n"
                    + "  <RUN>\n"
                    + "    <TITLE>Raw reads: test_reads</TITLE>\n"
                    + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
                    + "    <DATA_BLOCK>\n"
                    + "      <FILES>\n"
                    + "        <FILE filename=\"webin-cli/reads/"
                    + NAME
                    + "/"
                    + file.getFileName()
                    + "\" filetype=\"bam\" checksum_method=\"MD5\" checksum=\"d41d8cd98f00b204e9800998ecf8427e\"/>\n"
                    + "      </FILES>\n"
                    + "    </DATA_BLOCK>\n"
                    + "  </RUN>\n"
                    + "</RUN_SET>");
  }

  @Test
  public void testRunWithFastqFile() {
    ReadsManifest manifest = getDefaultManifest();
    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    Path file = WebinCliTestUtils.createEmptyTempFile("fastq");
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXml();

    WebinCliTestUtils.assertXml(
            runXml,
            "<RUN_SET>\n"
                    + "  <RUN>\n"
                    + "    <TITLE>Raw reads: test_reads</TITLE>\n"
                    + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
                    + "    <DATA_BLOCK>\n"
                    + "      <FILES>\n"
                    + "        <FILE filename=\"webin-cli/reads/"
                    + NAME
                    + "/"
                    + file.getFileName()
                    + "\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"d41d8cd98f00b204e9800998ecf8427e\"/>\n"
                    + "      </FILES>\n"
                    + "    </DATA_BLOCK>\n"
                    + "  </RUN>\n"
                    + "</RUN_SET>");
  }

  @Test
  public void testRunWithTwoFastqFiles() {
    ReadsManifest manifest = getDefaultManifest();
    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    Path file1 = WebinCliTestUtils.createEmptyTempFile("fastq");
    Path file2= WebinCliTestUtils.createEmptyTempFile("fastq");
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file1.toFile()));
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file2.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXml();

    WebinCliTestUtils.assertXml(
            runXml,
            "<RUN_SET>\n"
                    + "  <RUN>\n"
                    + "    <TITLE>Raw reads: test_reads</TITLE>\n"
                    + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
                    + "    <DATA_BLOCK>\n"
                    + "      <FILES>\n"
                    + "        <FILE filename=\"webin-cli/reads/"
                    + NAME
                    + "/"
                    + file1.getFileName()
                    + "\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"d41d8cd98f00b204e9800998ecf8427e\"/>\n"
                    + "        <FILE filename=\"webin-cli/reads/"
                    + NAME
                    + "/"
                    + file2.getFileName()
                    + "\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"d41d8cd98f00b204e9800998ecf8427e\"/>\n"
                    + "      </FILES>\n"
                    + "    </DATA_BLOCK>\n"
                    + "  </RUN>\n"
                    + "</RUN_SET>");
  }

}
