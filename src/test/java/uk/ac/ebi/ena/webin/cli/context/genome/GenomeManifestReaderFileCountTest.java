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

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileCountTester;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;

public class GenomeManifestReaderFileCountTest {

  @Test
  public void testDefaultFileCount() {
    final List<GenomeManifest.FileType> fileTypeList = getFileTypes();

    new ManifestReaderFileCountTester<>(
            GenomeManifestReader.class, fileTypeList.toArray(new GenomeManifest.FileType[0]))
        // Supported file groups
        .files(GenomeManifest.FileType.FASTA)
        .files(GenomeManifest.FileType.FLATFILE)
        .files(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.FLATFILE)
        .files(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.FLATFILE)
        .files(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(GenomeManifest.FileType.FLATFILE, GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(GenomeManifest.FileType.FLATFILE, GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.CHROMOSOME_LIST,
            GenomeManifest.FileType.UNLOCALISED_LIST)
        .files(
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.CHROMOSOME_LIST,
            GenomeManifest.FileType.UNLOCALISED_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.CHROMOSOME_LIST,
            GenomeManifest.FileType.UNLOCALISED_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.CHROMOSOME_LIST,
            GenomeManifest.FileType.UNLOCALISED_LIST)
        .files(
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.CHROMOSOME_LIST,
            GenomeManifest.FileType.UNLOCALISED_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.CHROMOSOME_LIST,
            GenomeManifest.FileType.UNLOCALISED_LIST)
        .test();
  }

  @Test
  public void testBinnedMetagenomeFileCount() {
    final List<GenomeManifest.FileType> fileTypeList = getFileTypes();

    new ManifestReaderFileCountTester<>(
            GenomeManifestReader.class, fileTypeList.toArray(new GenomeManifest.FileType[0]))
        // Supported file groups
        .files(GenomeManifest.FileType.FASTA)
        .field("ASSEMBLY_TYPE", "binned metagenome")
        .test();
  }

  @Test
  public void testPrimaryMetagenomeFileCount() {
    final List<GenomeManifest.FileType> fileTypeList = getFileTypes();

    new ManifestReaderFileCountTester<>(
            GenomeManifestReader.class, fileTypeList.toArray(new GenomeManifest.FileType[0]))
        // Supported file groups
        .files(GenomeManifest.FileType.FASTA)
        .field("ASSEMBLY_TYPE", "primary metagenome")
        .test();
  }

  @Test
  public void testClinicalIsolateAssemblyFileCount() {
    final List<GenomeManifest.FileType> fileTypeList = getFileTypes();

    new ManifestReaderFileCountTester<>(
            GenomeManifestReader.class, fileTypeList.toArray(new GenomeManifest.FileType[0]))
        // Supported file groups
        .files(GenomeManifest.FileType.FASTA)
        .field("ASSEMBLY_TYPE", "clinical isolate assembly")
        .test();
  }

  private static List<GenomeManifest.FileType> getFileTypes() {
    /* We are keeping webin-cli-validator and sequencetools unchanged,
    and hence we get AGP file type from GenomeManifest.FileType,
    we exclude it upfront */
    final List<GenomeManifest.FileType> fileTypeList =
        Arrays.stream(GenomeManifest.FileType.values())
            .filter(fileType -> !fileType.name().equals("AGP"))
            .toList();
    return fileTypeList;
  }
}
