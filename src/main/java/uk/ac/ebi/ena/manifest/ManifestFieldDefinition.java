/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.manifest;

import java.util.List;

public class ManifestFieldDefinition {

    private final String name;
    private final ManifestFieldType type;
    private final int minCount;
    private final int maxCount;
    private final List<String> fieldValueOrFileSuffix;

    public ManifestFieldDefinition(String name, ManifestFieldType type, int minCount, int maxCount) {
        this.name = name;
        this.type = type;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.fieldValueOrFileSuffix = null;
    }

    public ManifestFieldDefinition(String name, ManifestFieldType type, int minCount, int maxCount, List<String> fieldValueOrFileSuffix) {
        this.name = name;
        this.type = type;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.fieldValueOrFileSuffix = fieldValueOrFileSuffix;
    }

    public String getName() {
        return name;
    }

    public ManifestFieldType getType() {
        return type;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public boolean isFieldValueOrFileSuffix() {
        return fieldValueOrFileSuffix != null &&
               !fieldValueOrFileSuffix.isEmpty();
    }

    public List<String> getFieldValueOrFileSuffix() {
        return fieldValueOrFileSuffix;
    }
}
