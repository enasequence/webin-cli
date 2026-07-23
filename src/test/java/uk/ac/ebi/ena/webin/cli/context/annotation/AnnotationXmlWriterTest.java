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
package uk.ac.ebi.ena.webin.cli.context.annotation;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.XmlTester;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.AnnotationManifest;

public class AnnotationXmlWriterTest {

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testCreateXml() {
    File gff3File =
        TempFileBuilder.file("annotation.gff3", "##gff-version 3\n").toFile();

    AnnotationManifest manifest = new AnnotationManifest();
    manifest.setName("my_annotation_20260722");
    manifest.setDescription("Structural annotation of assembly GCA_000001.1");
    manifest.setAnalysisType(AnnotationManifestReader.ANALYSIS_TYPE_DECOUPLED_ANNOTATION);
    manifest.setPrimaryId("GCA_000001.1");
    manifest.files().add(new SubmissionFile<>(AnnotationManifest.FileType.GFF3, gff3File));
    manifest.addAttribute("ANNOTATION_SOURCE", "Prokka v1.14.6");
    manifest.addAttribute("GENE_CALLER", "Prodigal");
    manifest.addAttribute("PLOIDY", "2");

    Path inputDir = gff3File.getParentFile().toPath();
    Path uploadDir = Paths.get("webin-cli-test/annotation/my_annotation_20260722");

    String analysisXml =
        new AnnotationXmlWriter()
            .createXml(
                manifest,
                null,
                null,
                "Annotation: my_annotation_20260722",
                "webin-annotation-my_annotation_20260722",
                inputDir,
                uploadDir)
            .get(SubmissionBundle.SubmissionXMLFileType.ANALYSIS);

    XmlTester.assertXml(
        analysisXml,
        "<ANALYSIS_SET>\n"
            + "  <ANALYSIS>\n"
            + "    <TITLE>Annotation: my_annotation_20260722</TITLE>\n"
            + "    <DESCRIPTION>Structural annotation of assembly GCA_000001.1</DESCRIPTION>\n"
            + "    <PRIMARY_ID>GCA_000001.1</PRIMARY_ID>\n"
            + "    <ANALYSIS_TYPE>\n"
            + "      <DECOUPLED_ANNOTATION/>\n"
            + "    </ANALYSIS_TYPE>\n"
            + "    <FILES>\n"
            + "      <FILE filename=\"webin-cli-test/annotation/my_annotation_20260722/"
            + gff3File.getName()
            + "\""
            + " filetype=\"gff3\" checksum_method=\"MD5\""
            + " checksum=\"df19e1b84ba6f691d20c72b397c88abf\"/>\n"
            + "    </FILES>\n"
            + "    <ANALYSIS_ATTRIBUTES>\n"
            + "      <ANALYSIS_ATTRIBUTE>\n"
            + "        <TAG>ANNOTATION_SOURCE</TAG>\n"
            + "        <VALUE>Prokka v1.14.6</VALUE>\n"
            + "      </ANALYSIS_ATTRIBUTE>\n"
            + "      <ANALYSIS_ATTRIBUTE>\n"
            + "        <TAG>GENE_CALLER</TAG>\n"
            + "        <VALUE>Prodigal</VALUE>\n"
            + "      </ANALYSIS_ATTRIBUTE>\n"
            + "      <ANALYSIS_ATTRIBUTE>\n"
            + "        <TAG>PLOIDY</TAG>\n"
            + "        <VALUE>2</VALUE>\n"
            + "      </ANALYSIS_ATTRIBUTE>\n"
            + "    </ANALYSIS_ATTRIBUTES>\n"
            + "  </ANALYSIS>\n"
            + "</ANALYSIS_SET>");
  }
}
