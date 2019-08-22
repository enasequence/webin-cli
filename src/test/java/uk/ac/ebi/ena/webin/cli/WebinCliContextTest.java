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
package uk.ac.ebi.ena.webin.cli;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.assembly.*;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifestReader;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsXmlWriter;

import static org.assertj.core.api.Assertions.assertThat;

public class WebinCliContextTest {

  @Test
  public void testCreateExecutor() {
    WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();

    assertThat(WebinCliContext.genome.createExecutor(parameters).getManifestReader()).isInstanceOf(GenomeAssemblyManifestReader.class);
    assertThat(WebinCliContext.genome.createExecutor(parameters).getXmlWriter()).isInstanceOf(GenomeAssemblyXmlWriter.class);

    assertThat(WebinCliContext.transcriptome.createExecutor(parameters).getManifestReader()).isInstanceOf(TranscriptomeAssemblyManifestReader.class);
    assertThat(WebinCliContext.transcriptome.createExecutor(parameters).getXmlWriter()).isInstanceOf(TranscriptomeAssemblyXmlWriter.class);

    assertThat(WebinCliContext.sequence.createExecutor(parameters).getManifestReader()).isInstanceOf(SequenceAssemblyManifestReader.class);
    assertThat(WebinCliContext.sequence.createExecutor(parameters).getXmlWriter()).isInstanceOf(SequenceAssemblyXmlWriter.class);

    assertThat(WebinCliContext.reads.createExecutor(parameters).getManifestReader()).isInstanceOf(RawReadsManifestReader.class);
    assertThat(WebinCliContext.reads.createExecutor(parameters).getXmlWriter()).isInstanceOf(RawReadsXmlWriter.class);
  }
}
