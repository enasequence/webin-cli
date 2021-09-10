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
package uk.ac.ebi.ena.webin.cli.context.sequence;

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;

import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;

public class SequenceManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<SequenceManifestReader> manifestReader = SequenceManifestReader.class;
    valid(manifestReader, SequenceManifest.FileType.TAB, ".tsv.gz");
    valid(manifestReader, SequenceManifest.FileType.TAB, ".tab.gz");
    valid(manifestReader, SequenceManifest.FileType.FLATFILE, ".txt.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<SequenceManifestReader> manifestReader = SequenceManifestReader.class;
    // Invalid suffix before .gz
    invalid(manifestReader, SequenceManifest.FileType.TAB, ".INVALID.gz");
    // No .gz
    invalid(manifestReader, SequenceManifest.FileType.TAB, ".tsv");
    invalid(manifestReader, SequenceManifest.FileType.TAB, ".tab");
    invalid(manifestReader, SequenceManifest.FileType.FLATFILE, ".txt");
  }
}
