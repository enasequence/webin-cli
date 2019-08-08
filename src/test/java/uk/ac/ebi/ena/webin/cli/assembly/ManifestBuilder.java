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

import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ManifestBuilder<FileType extends Enum<FileType>> {
    private String manifest = "";
    private final List<FileType> fileTypes = new ArrayList<>();

    public ManifestBuilder manifest(String manifest) {
        if (manifest != null) {
            this.manifest += manifest;
        }
        return this;
    }

    public ManifestBuilder field(String field, String value) {
        if (field != null && value != null) {
            manifest += field + "\t" + value + "\n";
        }
        return this;
    }

    public ManifestBuilder file(FileType fileType, String fileName) {
        fileTypes.add(fileType);
        return field(fileType.name(), fileName);
    }

    public ManifestBuilder file(FileType fileType, int cnt) {
        for (int i = 0 ; i < cnt ; ++i) {
            file(fileType);
        }
        return this;
    }

    public ManifestBuilder file(FileType ... fileTypes) {
        for (FileType fileType : fileTypes) {
            if (fileType != null) {
                switch (fileType.name()) {
                    case "FASTA":
                        file(fileType, ".fasta.gz");
                        break;
                    case "FLATFILE":
                        file(fileType, ".dat.gz");
                        break;
                    case "AGP":
                        file(fileType, ".agp.gz");
                        break;
                    case "CHROMOSOME_LIST":
                    case "UNLOCALISED_LIST":
                    case "TAB":
                        file(fileType, ".tab.gz");
                        break;
                    case "BAM":
                        file(fileType, ".bam");
                        break;
                    case "CRAM":
                        file(fileType, ".cram");
                        break;
                    case "FASTQ":
                        file(fileType, ".fastq.gz");
                        break;
                    default:
                        throw new RuntimeException("Unknown file type: " + fileType.name());
                }
            }
        }
        return this;
    }

    public File build() {
        return WebinCliTestUtils.createTempFile(manifest).toFile();
    }

    @Override
    public String toString() {
        return manifest;
    }
}
