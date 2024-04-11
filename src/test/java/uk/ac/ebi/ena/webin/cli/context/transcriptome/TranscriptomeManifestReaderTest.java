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

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader.Field;

import java.nio.file.Paths;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

public class TranscriptomeManifestReaderTest {

  private static TranscriptomeManifestReader createManifestReader() {
    WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();
    return new TranscriptomeManifestReader(parameters, new MetadataProcessorFactory(parameters));
  }

  @Before
  public void before() {
    Locale.setDefault(Locale.UK);
  }

  @Test
  public void testValidManifest() {
    TranscriptomeManifestReader manifestReader = createManifestReader();

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .field(Field.PLATFORM, " illumina")
            .field(Field.NAME, " SOME-FANCY-NAME")
            .field(Field.DESCRIPTION, " description")
            .file("FASTA", TempFileBuilder.empty("fasta"))
            .field(ManifestReader.Fields.SUBMISSION_TOOL, "ST-001")
            .field(ManifestReader.Fields.SUBMISSION_TOOL_VERSION, "STV-001")
            .build());

    TranscriptomeManifest manifest = manifestReader.getManifests().stream().findFirst().get();

    Assert.assertEquals("illumina", manifest.getPlatform());
    Assert.assertEquals("SOME-FANCY-NAME", manifest.getName());
    assertThat(manifest.files().files()).size().isOne();
    Assert.assertEquals("description", manifest.getDescription());
    Assert.assertEquals("ST-001", manifest.getSubmissionTool());
    Assert.assertEquals("STV-001", manifest.getSubmissionToolVersion());
  }
}
