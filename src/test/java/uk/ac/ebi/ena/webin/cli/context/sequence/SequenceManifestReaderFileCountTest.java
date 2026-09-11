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
package uk.ac.ebi.ena.webin.cli.context.sequence;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileCountTester;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;

public class SequenceManifestReaderFileCountTest {

  @Test
  public void testFileCount() {
    new ManifestReaderFileCountTester<>(
            SequenceManifestReader.class, getFileTypes().toArray(new SequenceManifest.FileType[0]))
        // Supported file groups
        .files(SequenceManifest.FileType.TAB)
        .files(SequenceManifest.FileType.FLATFILE)
        .test();
  }

  private static List<SequenceManifest.FileType> getFileTypes() {
    /* SequenceManifestReader does not support the GFF3 field yet, even though
    SequenceManifest.FileType declares it, so we exclude it upfront. */
    return Arrays.stream(SequenceManifest.FileType.values())
        .filter(fileType -> !fileType.name().equals("GFF3"))
        .toList();
  }
}
