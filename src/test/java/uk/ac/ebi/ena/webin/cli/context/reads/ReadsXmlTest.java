/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
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
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.XmlTester;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

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
    WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    parameters.setOutputDir(WebinCliTestUtils.createTempDir());
    parameters.setManifestFile(TempFileBuilder.empty().toFile());
    parameters.setTest(false);
    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        (WebinCliExecutor<ReadsManifest,ReadsValidationResponse>)
            WebinCliContext.reads.createExecutor(parameters, manifestReader);
    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> mockedExecutor = Mockito.spy(executor);
    ReadsValidationResponse rvr = new ReadsValidationResponse();
    Mockito.when(mockedExecutor.getValidationResponse()).thenReturn(rvr);
    mockedExecutor.prepareSubmissionBundle();
    return mockedExecutor.getSubmissionBundle();
  }

  @Test
  public void testExperiment() {
    ReadsManifest manifest = getDefaultManifest();

    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    manifest.setLibraryStrategy("CLONEEND");
    manifest.setLibrarySource("OTHER");
    manifest.setLibrarySelection("Inverse rRNA selection");
    manifest.setLibraryConstructionProtocol("Protocol");
    manifest.setSubmissionTool("ST-001");
    manifest.setSubmissionToolVersion("STV-001");

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String experimentXml =
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.EXPERIMENT).getXmlContent();

    XmlTester.assertXml(
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
  public void testRun() {
    ReadsManifest manifest = getDefaultManifest();
    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    manifest.setSubmissionTool("ST-001");
    manifest.setSubmissionToolVersion("STV-001");

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXmlContent();

    XmlTester.assertXml(
            runXml,
            "<RUN_SET>\n"
                    + "  <RUN>\n"
                    + "    <TITLE>Raw reads: test_reads</TITLE>\n"
                    + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
                    + "    <DATA_BLOCK>\n"
                    + "        <FILES/>\n"
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
  public void testSubmissionXml() {
    ReadsManifest manifest = getDefaultManifest();

    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    manifest.setLibraryStrategy("CLONEEND");
    manifest.setLibrarySource("OTHER");
    manifest.setLibrarySelection("Inverse rRNA selection");
    manifest.setSubmissionTool("ST-001");
    manifest.setSubmissionToolVersion("STV-001");

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    XmlTester.assertSubmissionXmlWithEmptyManifestFile(
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.SUBMISSION).getXmlContent());
  }

  @Test
  public void testRunWithCramFile() {
    ReadsManifest manifest = getDefaultManifest();
    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    Path file = TempFileBuilder.empty("cram");
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.CRAM, file.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXmlContent();

    XmlTester.assertXml(
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
    Path file =  TempFileBuilder.empty("bam");
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.BAM, file.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXmlContent();

    XmlTester.assertXml(
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
    Path file =  TempFileBuilder.empty("fastq");
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXmlContent();

    XmlTester.assertXml(
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
    Path file1 = TempFileBuilder.empty("fastq");
    Path file2= TempFileBuilder.empty("fastq");
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file1.toFile()));
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file2.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXmlContent();

    XmlTester.assertXml(
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

  @Test
  public void testRunWithReadTypeFastqFile() {
    ReadsManifest manifest = getDefaultManifest();
    manifest.setPlatform("ILLUMINA");
    manifest.setInstrument("unspecified");
    Path file =  TempFileBuilder.empty("fastq");
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file.toFile(), createReadTypeAttributes("sample_barcode")));
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file.toFile(), createReadTypeAttributes("paired", "umi_barcode")));
    manifest.files().add(new SubmissionFile(ReadsManifest.FileType.FASTQ, file.toFile(), createReadTypeAttributes("paired", "cell_barcode")));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String runXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.RUN).getXmlContent();

    XmlTester.assertXml(
            runXml,
            "<RUN_SET>\n"
                    + "  <RUN>\n"
                    + "    <TITLE>Raw reads: test_reads</TITLE>\n"
                    + "    <EXPERIMENT_REF refname=\"webin-reads-test_reads\"/>\n"
                    + "    <DATA_BLOCK>\n"
                    + "      <FILES>\n"
                    + "        <FILE filename=\"webin-cli/reads/" + NAME + "/" + file.getFileName() + "\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"d41d8cd98f00b204e9800998ecf8427e\">\n"
                    + "          <READ_TYPE>sample_barcode</READ_TYPE>\n"
                    + "        </FILE>\n"
                    + "        <FILE filename=\"webin-cli/reads/" + NAME + "/" + file.getFileName() + "\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"d41d8cd98f00b204e9800998ecf8427e\">\n"
                    + "          <READ_TYPE>paired</READ_TYPE>\n"
                    + "          <READ_TYPE>umi_barcode</READ_TYPE>\n"
                    + "        </FILE>\n"
                    + "        <FILE filename=\"webin-cli/reads/" + NAME + "/" + file.getFileName() + "\" filetype=\"fastq\" checksum_method=\"MD5\" checksum=\"d41d8cd98f00b204e9800998ecf8427e\">\n"
                    + "          <READ_TYPE>paired</READ_TYPE>\n"
                    + "          <READ_TYPE>cell_barcode</READ_TYPE>\n"
                    + "        </FILE>\n"
                    + "      </FILES>\n"
                    + "    </DATA_BLOCK>\n"
                    + "  </RUN>\n"
                    + "</RUN_SET>");
  }

  private List<Map.Entry<String, String>> createReadTypeAttributes(String... atts) {
    List<Map.Entry<String, String>> readTypeAttributes = new ArrayList<>();

    Stream.of(atts).forEach(readType -> {
      Map<String, String> map = new HashMap<>();
      map.put("READ_TYPE", readType);

      readTypeAttributes.add(map.entrySet().stream().findFirst().get());
    });

    return readTypeAttributes;
  }
}
