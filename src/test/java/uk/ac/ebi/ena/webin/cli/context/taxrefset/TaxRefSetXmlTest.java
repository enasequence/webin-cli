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
package uk.ac.ebi.ena.webin.cli.context.taxrefset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.*;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TaxRefSetManifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

public class TaxRefSetXmlTest {

  private static final String NAME = "test_taxon_xref_set";

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  private static TaxRefSetManifest getDefaultManifest() {
    TaxRefSetManifest manifest = new TaxRefSetManifest();
    manifest.setName(NAME);
    manifest.setStudy(new Study());
    manifest.getStudy().setBioProjectId("test_study");
    manifest.setDescription("test_description");
    manifest.setTaxonomySystem("12345");
    Map<String,String> customFields = new LinkedHashMap<>();
    customFields.put("test_key_1","test_val_1");
    customFields.put("test_key_2","test_val_2");
    manifest.addCustomFields(customFields);

    return manifest;
  }

  private static SubmissionBundle prepareSubmissionBundle(TaxRefSetManifest manifest) {
    TaxRefSetManifestReader manifestReader = mock(TaxRefSetManifestReader.class);
    when(manifestReader.getManifest()).thenReturn(manifest);
    WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    parameters.setOutputDir(WebinCliTestUtils.createTempDir());
    parameters.setManifestFile(TempFileBuilder.empty().toFile());
    parameters.setTest(false);
    WebinCliExecutor<TaxRefSetManifest, ValidationResponse> executor =
        (WebinCliExecutor<TaxRefSetManifest, ValidationResponse>)
            WebinCliContext.taxrefset.createExecutor(parameters, manifestReader);
    executor.prepareSubmissionBundle();
    return executor.readSubmissionBundle();
  }

  @Test
  public void testFastaAndTsvFile() {
    TaxRefSetManifest manifest = getDefaultManifest();

    Path fastaFile = TempFileBuilder.gzip("fastafile.dat.gz", "ID   ;");
    Path tsvFile = TempFileBuilder.gzip("tabFile.dat.gz", "ID   ;");
    manifest.files().add(new SubmissionFile(TaxRefSetManifest.FileType.FASTA, fastaFile.toFile()));
    manifest.files().add(new SubmissionFile(TaxRefSetManifest.FileType.TAB, tsvFile.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String analysisXml = sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.ANALYSIS).getXml();


    XmlTester.assertXml(
            analysisXml,
            "<ANALYSIS_SET>\n" +
                    "<ANALYSIS>\n"+
                    "<TITLE>Taxonomy reference set: test_taxon_xref_set</TITLE>\n" +
                    "<DESCRIPTION>test_description</DESCRIPTION>\n" +
                    "<STUDY_REF accession=\"test_study\"/>\n" +
                    "<ANALYSIS_TYPE>\n" +
                    "<TAXONOMIC_REFERENCE_SET>\n" +
                    "<NAME>test_taxon_xref_set</NAME>\n" +
                    "<TAXONOMY_SYSTEM>12345</TAXONOMY_SYSTEM>\n" +
                    "<CUSTOM_FIELDS>\n" +
                      "<FIELD>\n" +
                        "<NAME>test_key_1</NAME>\n" +
                        "<DESCRIPTION>test_val_1</DESCRIPTION>\n" +
                      "</FIELD>\n" +
                      "<FIELD>\n" +
                        "<NAME>test_key_2</NAME>\n" +
                        "<DESCRIPTION>test_val_2</DESCRIPTION>\n" +
                      "</FIELD>\n" +
                    "</CUSTOM_FIELDS>\n"+
                    "</TAXONOMIC_REFERENCE_SET>\n" +
                    "</ANALYSIS_TYPE>\n"+
                     "<FILES>\n"+
                     "      <FILE filename=\"webin-cli/taxrefset/"+ NAME+ "/"+ fastaFile.getFileName()+
                    "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n"+
                     "      <FILE filename=\"webin-cli/taxrefset/"+ NAME+ "/"+ tsvFile.getFileName()+
                     "\" filetype=\"tab\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n"+
                     "</FILES>\n"+
                     "</ANALYSIS>\n"+
                     "</ANALYSIS_SET>");
  }

  @Test
  public void testAioSubmission() {
    TaxRefSetManifest manifest = getDefaultManifest();

    Path fastaFile = TempFileBuilder.gzip("fastafile.dat.gz", "ID   ;");
    Path tsvFile = TempFileBuilder.gzip("tabFile.dat.gz", "ID   ;");
    manifest.files().add(new SubmissionFile(TaxRefSetManifest.FileType.FASTA, fastaFile.toFile()));
    manifest.files().add(new SubmissionFile(TaxRefSetManifest.FileType.TAB, tsvFile.toFile()));

    SubmissionBundle sb = prepareSubmissionBundle(manifest);

    String actualXml =
        sb.getXMLFile(SubmissionBundle.SubmissionXMLFileType.AIO_SUBMISSION).getXml();

    XmlTester.assertXml(
        actualXml, WebinCliTestUtils.encloseWithFixedAioSubmissionXml(
        "<ANALYSIS_SET>\n" +
            "<ANALYSIS>\n"+
            "<TITLE>Taxonomy reference set: test_taxon_xref_set</TITLE>\n" +
            "<DESCRIPTION>test_description</DESCRIPTION>\n" +
            "<STUDY_REF accession=\"test_study\"/>\n" +
            "<ANALYSIS_TYPE>\n" +
            "<TAXONOMIC_REFERENCE_SET>\n" +
            "<NAME>test_taxon_xref_set</NAME>\n" +
            "<TAXONOMY_SYSTEM>12345</TAXONOMY_SYSTEM>\n" +
            "<CUSTOM_FIELDS>\n" +
            "<FIELD>\n" +
            "<NAME>test_key_1</NAME>\n" +
            "<DESCRIPTION>test_val_1</DESCRIPTION>\n" +
            "</FIELD>\n" +
            "<FIELD>\n" +
            "<NAME>test_key_2</NAME>\n" +
            "<DESCRIPTION>test_val_2</DESCRIPTION>\n" +
            "</FIELD>\n" +
            "</CUSTOM_FIELDS>\n"+
            "</TAXONOMIC_REFERENCE_SET>\n" +
            "</ANALYSIS_TYPE>\n"+
            "<FILES>\n"+
            "      <FILE filename=\"webin-cli/taxrefset/"+ NAME+ "/"+ fastaFile.getFileName()+
            "\" filetype=\"fasta\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n"+
            "      <FILE filename=\"webin-cli/taxrefset/"+ NAME+ "/"+ tsvFile.getFileName()+
            "\" filetype=\"tab\" checksum_method=\"MD5\" checksum=\"e334ca8a758084ba2f9f5975e798039e\" />\n"+
            "</FILES>\n"+
            "</ANALYSIS>\n"+
            "</ANALYSIS_SET>"));
  }
}
