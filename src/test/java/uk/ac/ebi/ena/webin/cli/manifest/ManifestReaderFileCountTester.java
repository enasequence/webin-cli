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
package uk.ac.ebi.ena.webin.cli.manifest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

public class ManifestReaderFileCountTester<FileType extends Enum<FileType>, T extends ManifestReader> {

  private final List<FileType> fileTypes;
  private final List<Multiset<FileType>> validFileGroups = new ArrayList<>();
  private final ManifestReaderTester manifestReaderTester;
  private HashMap<FileType, Integer> maxFiles;
  private ManifestBuilder manifestBuilder = new ManifestBuilder();

  public ManifestReaderFileCountTester(Class<T> manifestReaderClass, FileType... fileTypes) {
    this.fileTypes = Arrays.asList(fileTypes);
    this.manifestReaderTester = new ManifestReaderTester(manifestReaderClass);
  }

  public ManifestReaderFileCountTester files(FileType... validFileGroup) {
    validFileGroups.add(HashMultiset.create(Arrays.asList(validFileGroup)));
    return this;
  }

  public ManifestReaderFileCountTester field(String field, String value) {
    manifestBuilder.field(field, value);
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
      ManifestBuilder manifest = new ManifestBuilder();
      manifest.manifest(this.manifestBuilder);

      System.out.println("Manifest files: " + files);
      for (FileType fileType : files.elementSet()) {
        manifest.file(fileType, files.count(fileType));
      }

      ManifestReader manifestReader;
      if (files.isEmpty()) {
        manifestReader =
            manifestReaderTester.testError(manifest, WebinCliMessage.MANIFEST_READER_NO_DATA_FILES_ERROR);

      } else if (isValidFileGroup(files)) {
        manifestReader = manifestReaderTester.test(manifest);
      } else {
        manifestReader =
            manifestReaderTester.testError(
                    manifest, WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR);
      }

      for (FileType fileType : fileTypes) {
          System.out.println("Manifest " + fileType + " file count: " + files.count(fileType));
          System.out.println("ManifestReader "+ fileType + " file count: " + manifestReader.getManifest().files().get(fileType).size());
          assertThat(manifestReader.getManifest().files().get(fileType).size())
              .isEqualTo(files.count(fileType));
      }
    }
  }
}
