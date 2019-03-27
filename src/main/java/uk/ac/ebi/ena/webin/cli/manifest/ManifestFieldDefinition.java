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

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ManifestFieldDefinition {
  private final String name;
  private final String description;
  private final ManifestFieldType type;
  private final int minCount;
  private final int maxCount;
  private final int spreadsheetCount;
  private final List<ManifestFieldProcessor> processors;

  public static class Builder {
    private String name;
    private String description;
    private ManifestFieldType type;
    private Integer minCount;
    private Integer maxCount;
    private boolean spreadsheet = true;
    private List<ManifestFieldProcessor> processors = new ArrayList<>();

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder desc(String description) {
      this.description = description;
      return this;
    }

    public Builder meta() {
      this.type = ManifestFieldType.META;
      return this;
    }

    public Builder file() {
      this.type = ManifestFieldType.FILE;
      return this;
    }

    public Builder type(ManifestFieldType type) {
      this.type = type;
      return this;
    }

    public Builder optional() {
      this.minCount = 0;
      this.maxCount = 1;
      return this;
    }

    public Builder optional(int maxCount) {
      this.minCount = 0;
      this.maxCount = maxCount;
      return this;
    }

    public Builder required() {
      this.minCount = 1;
      this.maxCount = 1;
      return this;
    }

    public Builder spreadsheet(boolean spreadsheet) {
      this.spreadsheet = spreadsheet;
      return this;
    }

    public Builder processor(ManifestFieldProcessor... processors) {
      this.processors.addAll(
          Arrays.stream(processors).filter(Objects::nonNull).collect(Collectors.toList()));
      return this;
    }

    public ManifestFieldDefinition build() {
      return new ManifestFieldDefinition(
          name, description, type, minCount, maxCount, spreadsheet ? maxCount : 0, processors);
    }
  }

  private ManifestFieldDefinition(
      String name,
      String description,
      ManifestFieldType type,
      int minCount,
      int maxCount,
      int spreadsheetCount,
      List<ManifestFieldProcessor> processors) {
    Assert.notNull(name, "Field name must not be null");
    Assert.notNull(description, "Field description must not be null");
    Assert.notNull(type, "Field type must not be null");
    this.name = name;
    this.description = description;
    this.type = type;
    this.minCount = minCount;
    this.maxCount = maxCount;
    this.spreadsheetCount = spreadsheetCount;
    this.processors = processors;
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

  public List<ManifestFieldProcessor> getFieldProcessors() {
    return processors;
  }
}
