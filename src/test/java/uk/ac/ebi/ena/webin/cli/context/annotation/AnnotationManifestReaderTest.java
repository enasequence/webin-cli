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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.nio.file.Paths;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.AnnotationManifest;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.listener.MessageCounter;

public class AnnotationManifestReaderTest {
  private static AnnotationManifestReader createManifestReader() {
    WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    return new AnnotationManifestReader(parameters, new MetadataProcessorFactory(parameters));
  }

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidMinimalManifest() {
    AnnotationManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, " SOME-FANCY-NAME")
            .file(
                AnnotationManifestReader.Field.GFF3,
                TempFileBuilder.empty("annotation.gff3.gz"))
            .field(
                AnnotationManifestReader.Field.ANALYSIS_TYPE,
                AnnotationManifestReader.ANALYSIS_TYPE_DECOUPLED_ANNOTATION)
            .build());

    AnnotationManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    assertEquals("SOME-FANCY-NAME", manifest.getName());
    assertEquals(
        AnnotationManifestReader.ANALYSIS_TYPE_DECOUPLED_ANNOTATION, manifest.getAnalysisType());
    assertThat(manifest.files().files()).hasSize(1);
    assertThat(manifest.files().get(AnnotationManifest.FileType.GFF3)).hasSize(1);
    assertThat(manifest.getAttributes()).isEmpty();
  }

  @Test
  public void testValidManifestWithMultipleAttributes() {
    AnnotationManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "SOME-FANCY-NAME")
            .file(
                AnnotationManifestReader.Field.GFF3,
                TempFileBuilder.empty("annotation.gff3.gz"))
            .field(
                AnnotationManifestReader.Field.ANALYSIS_TYPE,
                AnnotationManifestReader.ANALYSIS_TYPE_DECOUPLED_ANNOTATION)
            .field(
                AnnotationManifestReader.Field.ANALYSIS_ATTRIBUTE,
                "ANNOTATION_SOURCE:Prokka v1.14.6")
            .field(AnnotationManifestReader.Field.ANALYSIS_ATTRIBUTE, "GENE_CALLER:Prodigal")
            .field(AnnotationManifestReader.Field.ANALYSIS_ATTRIBUTE, "PLOIDY:2")
            .build());

    AnnotationManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    assertThat(manifest.getAttributes()).hasSize(3);
    assertEquals("Prokka v1.14.6", manifest.getAttributes().get("ANNOTATION_SOURCE"));
    assertEquals("Prodigal", manifest.getAttributes().get("GENE_CALLER"));
    assertEquals("2", manifest.getAttributes().get("PLOIDY"));
  }

  @Test
  public void testDuplicateAttributeTagLastValueWins() {
    AnnotationManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "SOME-FANCY-NAME")
            .file(
                AnnotationManifestReader.Field.GFF3,
                TempFileBuilder.empty("annotation.gff3.gz"))
            .field(
                AnnotationManifestReader.Field.ANALYSIS_TYPE,
                AnnotationManifestReader.ANALYSIS_TYPE_DECOUPLED_ANNOTATION)
            .field(AnnotationManifestReader.Field.ANALYSIS_ATTRIBUTE, "PLOIDY:2")
            .field(AnnotationManifestReader.Field.ANALYSIS_ATTRIBUTE, "PLOIDY:4")
            .build());

    AnnotationManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    // Duplicate tags are not rejected today - the later value silently overwrites the
    // earlier one. Same behaviour as TaxRefSetManifest's customFields map.
    assertThat(manifest.getAttributes()).hasSize(1);
    assertEquals("4", manifest.getAttributes().get("PLOIDY"));
  }

  @Test
  public void testMissingGff3ReportsError() {
    AnnotationManifestReader manifestReader = createManifestReader();

    MessageCounter counter =
        MessageCounter.regex(
            ValidationMessage.Severity.ERROR,
            WebinCliMessage.MANIFEST_READER_NO_DATA_FILES_ERROR.regex());
    manifestReader.addListener(counter);

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "SOME-FANCY-NAME")
            .field(
                AnnotationManifestReader.Field.ANALYSIS_TYPE,
                AnnotationManifestReader.ANALYSIS_TYPE_DECOUPLED_ANNOTATION)
            .build());

    assertThat(counter.getCount()).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void testMalformedAttributeReportsError() {
    AnnotationManifestReader manifestReader = createManifestReader();

    MessageCounter counter =
        MessageCounter.regex(
            ValidationMessage.Severity.ERROR,
            WebinCliMessage.CUSTOM_FIELD_PROCESSOR_INCORRECT_FIELD_VALUE.regex());
    manifestReader.addListener(counter);

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "SOME-FANCY-NAME")
            .file(
                AnnotationManifestReader.Field.GFF3,
                TempFileBuilder.empty("annotation.gff3.gz"))
            .field(
                AnnotationManifestReader.Field.ANALYSIS_TYPE,
                AnnotationManifestReader.ANALYSIS_TYPE_DECOUPLED_ANNOTATION)
            .field(AnnotationManifestReader.Field.ANALYSIS_ATTRIBUTE, "MISSING_COLON_VALUE")
            .build());

    assertThat(counter.getCount()).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void testMissingAnalysisTypeReportsError() {
    AnnotationManifestReader manifestReader = createManifestReader();

    MessageCounter counter =
        MessageCounter.regex(
            ValidationMessage.Severity.ERROR,
            WebinCliMessage.MANIFEST_READER_MISSING_MANDATORY_FIELD_ERROR.regex());
    manifestReader.addListener(counter);

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "SOME-FANCY-NAME")
            .file(
                AnnotationManifestReader.Field.GFF3,
                TempFileBuilder.empty("annotation.gff3.gz"))
            .build());

    assertThat(counter.getCount()).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void testInvalidAnalysisTypeReportsError() {
    AnnotationManifestReader manifestReader = createManifestReader();

    MessageCounter counter =
        MessageCounter.regex(
            ValidationMessage.Severity.ERROR, WebinCliMessage.CV_FIELD_PROCESSOR_ERROR.regex());
    manifestReader.addListener(counter);

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, "SOME-FANCY-NAME")
            .file(
                AnnotationManifestReader.Field.GFF3,
                TempFileBuilder.empty("annotation.gff3.gz"))
            .field(AnnotationManifestReader.Field.ANALYSIS_TYPE, "SEQUENCE_ANNOTATION")
            .build());

    assertThat(counter.getCount()).isGreaterThanOrEqualTo(1);
  }
}
