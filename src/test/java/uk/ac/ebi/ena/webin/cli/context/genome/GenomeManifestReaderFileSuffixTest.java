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
package uk.ac.ebi.ena.webin.cli.context.genome;

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;

public class GenomeManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<GenomeManifestReader> manifestReader = GenomeManifestReader.class;
    valid(manifestReader, GenomeManifest.FileType.FASTA, ".fasta.gz");
    valid(manifestReader, GenomeManifest.FileType.AGP, ".agp.gz");
    valid(manifestReader, GenomeManifest.FileType.FLATFILE, ".txt.gz");
    valid(manifestReader, GenomeManifest.FileType.CHROMOSOME_LIST, ".txt.gz");
    valid(manifestReader, GenomeManifest.FileType.UNLOCALISED_LIST, ".txt.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<GenomeManifestReader> manifestReader = GenomeManifestReader.class;
    // Invalid suffix before .gz
    invalid(manifestReader, GenomeManifest.FileType.FASTA, ".INVALID.gz");
    invalid(manifestReader, GenomeManifest.FileType.AGP, ".INVALID.gz");
    // No .gz
    invalid(manifestReader, GenomeManifest.FileType.FASTA, ".fasta");
    invalid(manifestReader, GenomeManifest.FileType.AGP, ".agp");
    invalid(manifestReader, GenomeManifest.FileType.FLATFILE, ".txt");
    invalid(manifestReader, GenomeManifest.FileType.CHROMOSOME_LIST, ".txt");
    invalid(manifestReader, GenomeManifest.FileType.UNLOCALISED_LIST, ".txt");
  }
}
