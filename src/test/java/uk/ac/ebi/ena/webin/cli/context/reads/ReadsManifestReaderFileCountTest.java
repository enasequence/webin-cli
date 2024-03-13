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
package uk.ac.ebi.ena.webin.cli.context.reads;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileCountTester;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;

public class ReadsManifestReaderFileCountTest {

  @Test
  public void testFileCount() {
    new ManifestReaderFileCountTester<>(ReadsManifestReader.class, ReadsManifest.FileType.values())
        // Supported file groups
        .files(ReadsManifest.FileType.BAM)
        .files(ReadsManifest.FileType.CRAM)
        // The way this test is written, all possible combinations up until the desired maximum must
        // be provided for it to pass.
        // That is why all following combinations of fastq files (1, 2, 3, 4) up until the maximum
        // of 4 are defined.
        // Skipping a combination e.g. 3 from (1, 2, 4) will fail the test.
        // If we decide to increase the number to, say 7 from 4, then the combinations will turn out
        // to be
        // (1, 2, 3, 4, 5, 6, 7) i.e. 5 and 6 must then be added explicitly as well.
        .files(ReadsManifest.FileType.FASTQ)
        .files(ReadsManifest.FileType.FASTQ, ReadsManifest.FileType.FASTQ)
        .files(
            ReadsManifest.FileType.FASTQ,
            ReadsManifest.FileType.FASTQ,
            ReadsManifest.FileType.FASTQ)
        .files(
            ReadsManifest.FileType.FASTQ,
            ReadsManifest.FileType.FASTQ,
            ReadsManifest.FileType.FASTQ,
            ReadsManifest.FileType.FASTQ)
        .test();
  }
}
