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
package uk.ac.ebi.ena.webin.cli.assembly;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileCountTester;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;

public class GenomeAssemblyManifestReaderFileCountTest {

  @Test
  public void testDefaultFileCount() {
    new ManifestReaderFileCountTester<>(
            GenomeAssemblyManifestReader.class, GenomeManifest.FileType.values())
        // Supported file groups
        .files(GenomeManifest.FileType.FASTA)
        .files(GenomeManifest.FileType.FLATFILE)
        .files(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.FLATFILE)
        .files(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.AGP)
        .files(GenomeManifest.FileType.FLATFILE, GenomeManifest.FileType.AGP)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.AGP)
        .files(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(GenomeManifest.FileType.FLATFILE, GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.AGP,
            GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.AGP,
            GenomeManifest.FileType.CHROMOSOME_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.AGP,
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
            GenomeManifest.FileType.AGP,
            GenomeManifest.FileType.CHROMOSOME_LIST,
            GenomeManifest.FileType.UNLOCALISED_LIST)
        .files(
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.AGP,
            GenomeManifest.FileType.CHROMOSOME_LIST,
            GenomeManifest.FileType.UNLOCALISED_LIST)
        .files(
            GenomeManifest.FileType.FASTA,
            GenomeManifest.FileType.FLATFILE,
            GenomeManifest.FileType.AGP,
            GenomeManifest.FileType.CHROMOSOME_LIST,
            GenomeManifest.FileType.UNLOCALISED_LIST)
        .test();
  }

  @Test
  public void testBinnedMetagenomeFileCount() {
    new ManifestReaderFileCountTester<>(
            GenomeAssemblyManifestReader.class, GenomeManifest.FileType.values())
        // Supported file groups
        .files(GenomeManifest.FileType.FASTA)
        .field("ASSEMBLY_TYPE", "binned metagenome")
        .test();
  }

  @Test
  public void testPrimaryMetagenomeFileCount() {
    new ManifestReaderFileCountTester<>(
            GenomeAssemblyManifestReader.class, GenomeManifest.FileType.values())
        // Supported file groups
        .files(GenomeManifest.FileType.FASTA)
        .field("ASSEMBLY_TYPE", "primary metagenome")
        .test();
  }
}
