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

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

public class TranscriptomeManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<TranscriptomeManifestReader> manifestReader = TranscriptomeManifestReader.class;
    valid(manifestReader, TranscriptomeManifest.FileType.FASTA, ".fasta.gz");
    valid(manifestReader, TranscriptomeManifest.FileType.FLATFILE, ".txt.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<TranscriptomeManifestReader> manifestReader = TranscriptomeManifestReader.class;
    // Invalid suffix before .gz
    invalid(manifestReader, TranscriptomeManifest.FileType.FASTA, ".INVALID.gz");
    // No .gz
    invalid(manifestReader, TranscriptomeManifest.FileType.FASTA, ".fasta");
    invalid(manifestReader, TranscriptomeManifest.FileType.FLATFILE, ".txt");
  }
}
