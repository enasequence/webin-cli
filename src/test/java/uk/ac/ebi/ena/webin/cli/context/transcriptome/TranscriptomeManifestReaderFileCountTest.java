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
package uk.ac.ebi.ena.webin.cli.context.transcriptome;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileCountTester;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

public class TranscriptomeManifestReaderFileCountTest {

  @Test
  public void testFileCount() {
    new ManifestReaderFileCountTester<>(
            TranscriptomeManifestReader.class, TranscriptomeManifest.FileType.values())
        // Supported file groups
        .files(TranscriptomeManifest.FileType.FASTA)
        .files(TranscriptomeManifest.FileType.FLATFILE)
        .test();
  }
}
