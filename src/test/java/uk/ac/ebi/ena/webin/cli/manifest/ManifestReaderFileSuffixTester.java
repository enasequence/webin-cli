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
package uk.ac.ebi.ena.webin.cli.manifest;

import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

public class ManifestReaderFileSuffixTester {
  public static <FileType extends Enum<FileType>, T extends ManifestReader> void invalid(
      Class<T> manifestReaderClass, FileType fileType, String fileName) {
    ManifestReaderTester tester = new ManifestReaderTester(manifestReaderClass);
    ManifestBuilder manifestBuilder = new ManifestBuilder().field("NAME", "TEST").file(fileType, fileName);
    tester.testError(manifestBuilder, WebinCliMessage.FILE_SUFFIX_PROCESSOR_ERROR);
  }

  public static <FileType extends Enum<FileType>, T extends ManifestReader> void valid(
      Class<T> manifestReaderClass, FileType fileType, String fileName) {
    ManifestReaderTester tester = new ManifestReaderTester(manifestReaderClass);
    ManifestBuilder manifestBuilder = new ManifestBuilder().field("NAME", "TEST").file(fileType, fileName);
    tester.test(manifestBuilder);
  }
}
