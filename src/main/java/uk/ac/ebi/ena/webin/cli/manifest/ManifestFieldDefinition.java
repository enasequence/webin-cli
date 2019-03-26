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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class
ManifestFieldDefinition {
    private final String name;
    private final String description;
    private final ManifestFieldType type;
    private final int minCount;
    private final int maxCount;
    private final int spreadsheetCount;
    private final List<ManifestFieldProcessor> processors;

    public List<ManifestFieldProcessor> getFieldProcessors() {
        return processors;
    }

    public ManifestFieldDefinition(String name, String description, ManifestFieldType type, int minCount, int maxCount, int spreadsheetCount) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.spreadsheetCount = spreadsheetCount;
        this.processors = Collections.emptyList();
    }

    public ManifestFieldDefinition(String name, String description, ManifestFieldType type, int minCount, int maxCount, int spreadsheetCount, ManifestFieldProcessor... processors) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.spreadsheetCount = spreadsheetCount;
        this.processors = Arrays.stream(processors).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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

    public int getSpreadsheetCount() {
        return spreadsheetCount;
    }

    @Override
    public String toString() {
        return "ManifestFieldDefinition{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", minCount=" + minCount +
                ", maxCount=" + maxCount +
                ", spreadsheetCount=" + spreadsheetCount +
                ", processors=" + processors +
                '}';
    }
}
