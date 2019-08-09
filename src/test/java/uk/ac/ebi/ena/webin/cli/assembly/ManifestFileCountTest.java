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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ManifestFileCountTest {

  private static class CountTest<
      FileType extends Enum<FileType>, T extends SequenceWebinCli> {

    private final List<FileType> fileTypes;
    private final List<Multiset<FileType>> validFileGroups = new ArrayList<>();
    private final ValidatorBuilder validatorBuilder;
    private HashMap<FileType, Integer> maxFiles;
    private String manifest;

    public CountTest(Class<T> validatorClass, FileType... fileTypes) {
      this.fileTypes = Arrays.asList(fileTypes);
      this.validatorBuilder =
          new ValidatorBuilder<>(validatorClass)
              .createOutputDirs(false)
              .manifestMetadataProcessors(false)
              .manifestValidateMandatory(false)
              .manifestValidateFileExists(false);
    }

    public CountTest add(FileType... validFileGroup) {
      validFileGroups.add(HashMultiset.create(Arrays.asList(validFileGroup)));
      return this;
    }

    private boolean isValidFileGroup(Multiset<FileType> files) {
      for (Multiset<FileType> validFileGroup : validFileGroups) {
        if (Multisets.difference(validFileGroup, files).isEmpty()
            && Multisets.difference(files, validFileGroup).isEmpty()) {
          return true;
        }
      }
      return false;
    }

    public void test() {
      test(null);
    }

    public void test(String manifest) {
      this.manifest = manifest;
      this.maxFiles = new HashMap<>();
      for (Multiset<FileType> validFileGroup : validFileGroups) {
        HashMap<FileType, Integer> maxFilesFileGroup = new HashMap<>();
        for (FileType fileType : validFileGroup) {
          if (maxFilesFileGroup.containsKey(fileType)) {
            maxFilesFileGroup.put(fileType, maxFilesFileGroup.get(fileType) + 1);
          } else {
            maxFilesFileGroup.put(fileType, 1);
          }
        }
        for (FileType fileType : maxFilesFileGroup.keySet()) {
          if (maxFiles.containsKey(fileType)) {
            if (maxFiles.get(fileType) < maxFilesFileGroup.get(fileType)) {
              maxFiles.put(fileType, maxFilesFileGroup.get(fileType));
            }
          } else {
            maxFiles.put(fileType, maxFilesFileGroup.get(fileType));
          }
        }
      }

      Multiset<FileType> files = HashMultiset.create();
      test(0, files);
    }

    private void test(int level, Multiset<FileType> files) {
      if (level < fileTypes.size()) {
        FileType fileType = fileTypes.get(level);
        int fileTypeMaxFiles = maxFiles.get(fileType) != null ? maxFiles.get(fileType) : 1;
        for (int fileCnt = 0; fileCnt <= fileTypeMaxFiles; ++fileCnt) {
          Multiset<FileType> s = HashMultiset.create(files);
          s.add(fileType, fileCnt);
          test(level + 1, s);
        }
      } else {
        ManifestBuilder manifestBuilder = new ManifestBuilder();
        manifestBuilder.manifest(manifest);
        System.out.println("Manifest files: " + files);
        for (FileType fileType : files) {
          manifestBuilder.file(fileType, files.count(fileType));
        }
        File manifestFile = manifestBuilder.build();

        SequenceWebinCli validator;
        if (files.isEmpty()) {
          validator =
              validatorBuilder.readManifestThrows(
                  manifestFile, WebinCliMessage.Manifest.NO_DATA_FILES_ERROR);

        } else if (isValidFileGroup(files)) {
          validator = validatorBuilder.readManifest(manifestFile);
        } else {
          validator =
              validatorBuilder.readManifestThrows(
                  manifestFile, WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR);
        }

        for (FileType fileType : fileTypes) {
          System.out.println(files);
          System.out.println(fileType);
          System.out.println(validator.getManifestReader().getManifest().files().get(fileType).size());
          assertThat(validator.getManifestReader().getManifest().files().get(fileType).size())
              .isEqualTo(files.count(fileType));
        }
      }
    }
  }

  @Test
  public void testGenomeNoAssemblyType() {
    CountTest test =
        new CountTest<>(
                GenomeAssemblyWebinCli.class, GenomeManifest.FileType.values())
            // Only the following file groups should be supported
            .add(GenomeManifest.FileType.FASTA)
            .add(GenomeManifest.FileType.FLATFILE)
            .add(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.FLATFILE)
            .add(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.AGP)
            .add(GenomeManifest.FileType.FLATFILE, GenomeManifest.FileType.AGP)
            .add(
                GenomeManifest.FileType.FASTA,
                GenomeManifest.FileType.FLATFILE,
                GenomeManifest.FileType.AGP)
            .add(GenomeManifest.FileType.FASTA, GenomeManifest.FileType.CHROMOSOME_LIST)
            .add(GenomeManifest.FileType.FLATFILE, GenomeManifest.FileType.CHROMOSOME_LIST)
            .add(
                GenomeManifest.FileType.FASTA,
                GenomeManifest.FileType.FLATFILE,
                GenomeManifest.FileType.CHROMOSOME_LIST)
            .add(
                GenomeManifest.FileType.FASTA,
                GenomeManifest.FileType.AGP,
                GenomeManifest.FileType.CHROMOSOME_LIST)
            .add(
                GenomeManifest.FileType.FLATFILE,
                GenomeManifest.FileType.AGP,
                GenomeManifest.FileType.CHROMOSOME_LIST)
            .add(
                GenomeManifest.FileType.FASTA,
                GenomeManifest.FileType.FLATFILE,
                GenomeManifest.FileType.AGP,
                GenomeManifest.FileType.CHROMOSOME_LIST)
            .add(
                GenomeManifest.FileType.FASTA,
                GenomeManifest.FileType.CHROMOSOME_LIST,
                GenomeManifest.FileType.UNLOCALISED_LIST)
            .add(
                GenomeManifest.FileType.FLATFILE,
                GenomeManifest.FileType.CHROMOSOME_LIST,
                GenomeManifest.FileType.UNLOCALISED_LIST)
            .add(
                GenomeManifest.FileType.FASTA,
                GenomeManifest.FileType.FLATFILE,
                GenomeManifest.FileType.CHROMOSOME_LIST,
                GenomeManifest.FileType.UNLOCALISED_LIST)
            .add(
                GenomeManifest.FileType.FASTA,
                GenomeManifest.FileType.AGP,
                GenomeManifest.FileType.CHROMOSOME_LIST,
                GenomeManifest.FileType.UNLOCALISED_LIST)
            .add(
                GenomeManifest.FileType.FLATFILE,
                GenomeManifest.FileType.AGP,
                GenomeManifest.FileType.CHROMOSOME_LIST,
                GenomeManifest.FileType.UNLOCALISED_LIST)
            .add(
                GenomeManifest.FileType.FASTA,
                GenomeManifest.FileType.FLATFILE,
                GenomeManifest.FileType.AGP,
                GenomeManifest.FileType.CHROMOSOME_LIST,
                GenomeManifest.FileType.UNLOCALISED_LIST);
    test.test();
  }

  @Test
  public void testGenomeBinnedMetagenomeAssemblyType() {
    CountTest test =
        new CountTest<>(
                GenomeAssemblyWebinCli.class, GenomeManifest.FileType.values())
            // Only the following file groups should be supported
            .add(GenomeManifest.FileType.FASTA);
    test.test("ASSEMBLY_TYPE\tbinned metagenome\n");
  }

  @Test
  public void testGenomePrimaryMetagenomeAssemblyType() {
    CountTest test =
        new CountTest<>(
                GenomeAssemblyWebinCli.class, GenomeManifest.FileType.values())
            // Only the following file groups should be supported
            .add(GenomeManifest.FileType.FASTA);
    test.test("ASSEMBLY_TYPE\tprimary metagenome\n");
  }

  @Test
  public void testTransciptome() {
    CountTest test =
        new CountTest<>(
                TranscriptomeAssemblyWebinCli.class, TranscriptomeManifest.FileType.values())
            // Only the following file groups should be supported
            .add(TranscriptomeManifest.FileType.FASTA)
            .add(TranscriptomeManifest.FileType.FLATFILE);
    test.test();
  }

  @Test
  public void testSequence() {
    CountTest test =
        new CountTest<>(
                SequenceAssemblyWebinCli.class, SequenceManifest.FileType.values())
            // Only the following file groups should be supported
            .add(SequenceManifest.FileType.TAB)
            .add(SequenceManifest.FileType.FLATFILE);
    test.test();
  }
}
