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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class WebinCliExecutorAliasTest {

  private final WebinCliParameters parameters = WebinCliTestUtils.createTestWebinCliParameters();

  @Test
  public void testGenome() {
    WebinCliExecutor<?> executor = WebinCliContext.genome.createExecutor(parameters);
    executor.getManifestReader().getManifest().setName("TEST_NAME  1");
    assertThat("webin-genome-TEST_NAME_1").isEqualTo(executor.getSubmissionAlias());
  }

  @Test
  public void testSequence() {
    WebinCliExecutor<?> executor = WebinCliContext.sequence.createExecutor(parameters);
    executor.getManifestReader().getManifest().setName("TEST_NAME  1");
    assertThat("webin-sequence-TEST_NAME_1").isEqualTo(executor.getSubmissionAlias());
  }

  @Test
  public void testTranscriptome() {
    WebinCliExecutor<?> executor = WebinCliContext.transcriptome.createExecutor(parameters);
    executor.getManifestReader().getManifest().setName("TEST_NAME  1");
    assertThat("webin-transcriptome-TEST_NAME_1").isEqualTo(executor.getSubmissionAlias());
  }

  @Test
  public void testReads() {
    WebinCliExecutor<?> executor = WebinCliContext.reads.createExecutor(parameters);
    executor.getManifestReader().getManifest().setName("TEST_NAME  1");
    assertThat("webin-reads-TEST_NAME_1").isEqualTo(executor.getSubmissionAlias());
  }
}
