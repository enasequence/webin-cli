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
package uk.ac.ebi.ena.webin.cli.manifest;

import java.util.ArrayList;
import java.util.List;

public class ManifestFileCount {
    private final String fileType;
    private final int minCount;
    private final Integer maxCount;

    public ManifestFileCount(String fileType, int minCount, Integer maxCount) {
        this.fileType = fileType;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public String getFileType() {
        return fileType;
    }
    public int getMinCount() {
        return minCount;
    }
    public Integer getMaxCount() {
        return maxCount;
    }

    public static class Builder {
        private final ArrayList<List<ManifestFileCount>> groups = new ArrayList<>();

        public static class Group {
            private final Builder builder;
            private final List<ManifestFileCount> files = new ArrayList<>();

            private Group(Builder builder) {
                this.builder = builder;
                builder.groups.add(this.files);
            }

            public Group required(String fieldName) {
                files.add(new ManifestFileCount(fieldName, 1, 1));
                return this;
            }

            public Group required(String fieldName, int maxCount) {
                files.add(new ManifestFileCount(fieldName, 1, maxCount));
                return this;
            }

            public Group required(String fieldName, int minCount, int maxCount) {
                files.add(new ManifestFileCount(fieldName, minCount, maxCount));
                return this;
            }

            public Group optional(String fieldName) {
                files.add(new ManifestFileCount(fieldName, 0, 1));
                return this;
            }

            public Group optional(String fieldName, int maxCount) {
                files.add(new ManifestFileCount(fieldName, 0, maxCount));
                return this;
            }

            public Builder and() {
                return builder;
            }

            public ArrayList<List<ManifestFileCount>> build() {
                return builder.groups;
            }
        }

        public Group group() {
            return new Group(this);
        }
    }
}