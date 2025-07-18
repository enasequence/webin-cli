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
package uk.ac.ebi.ena.webin.cli.context.polysample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader.Field;

import java.nio.file.Paths;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.PolySampleManifest;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

public class PolySampleManifestReaderTest {
  private static PolySampleManifestReader createManifestReader() {
    WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    return new PolySampleManifestReader(parameters, new MetadataProcessorFactory(parameters));
  }

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidSequenceSetManifest() {
    PolySampleManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(ManifestReader.Fields.NAME, " SOME-FANCY-NAME")
            .field(Field.DESCRIPTION, " description")
            .file("FASTA", TempFileBuilder.empty(".fasta.gz"))
            .field(ManifestReader.Fields.SUBMISSION_TOOL, "ST-001")
            .field(ManifestReader.Fields.SUBMISSION_TOOL_VERSION, "STV-001")
            .field("ANALYSIS_TYPE", "SEQUENCE_SET")
            .build());
    ValidationResult validationResult = manifestReader.getValidationResult();

    assertFalse(validationResult.isValid());

    PolySampleManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    assertEquals("SOME-FANCY-NAME", manifest.getName());
    assertThat(manifest.files().files()).size().isOne();
    assertEquals("description", manifest.getDescription());
    assertEquals("ST-001", manifest.getSubmissionTool());
    assertEquals("STV-001", manifest.getSubmissionToolVersion());
  }
}
