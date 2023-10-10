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

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;

public class ReadsManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<ReadsManifestReader> manifestReader = ReadsManifestReader.class;
    valid(manifestReader, ReadsManifest.FileType.BAM, ".bam");
    valid(manifestReader, ReadsManifest.FileType.CRAM, ".cram");
    valid(manifestReader, ReadsManifest.FileType.FASTQ, ".fastq.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<ReadsManifestReader> manifestReader = ReadsManifestReader.class;
    // Invalid suffix
    invalid(manifestReader, ReadsManifest.FileType.BAM, ".INVALID");
    invalid(manifestReader, ReadsManifest.FileType.CRAM, ".INVALID");
    // Not allowed .gz
    invalid(manifestReader, ReadsManifest.FileType.BAM, ".bam.gz");
    invalid(manifestReader, ReadsManifest.FileType.CRAM, ".cram.gz");
    // No .gz
    invalid(manifestReader, ReadsManifest.FileType.FASTQ, ".fastq");
  }
}
