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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

public class ManifestFieldDefinition {

  private final String name;
  private final String synonym;
  private final String description;
  private final ManifestFieldType type;
  private final int minCount;
  private final int maxCount;
  private final int recommendedMinCount;
  private final int recommendedMaxCount;
  private final List<ManifestFieldProcessor> processors;
  private final List<ManifestFieldDefinition> attributes;

  private ManifestFieldDefinition(
      String name,
      String synonym,
      String description,
      ManifestFieldType type,
      int minCount,
      int maxCount,
      int recommendedMinCount,
      int recommendedMaxCount,
      List<ManifestFieldProcessor> processors,
      List<ManifestFieldDefinition> attributes) {
    Assert.notNull(name, "Field name must not be null");
    Assert.notNull(description, "Field description must not be null");
    Assert.notNull(type, "Field type must not be null");
    this.name = name;
    this.synonym = synonym;
    this.description = description;
    this.type = type;
    this.minCount = minCount;
    this.maxCount = maxCount;
    this.recommendedMinCount = recommendedMinCount;
    this.recommendedMaxCount = recommendedMaxCount;
    this.processors = processors;
    this.attributes = attributes;
  }

  public String getName() {
    return name;
  }

  public String getSynonym() {
    return synonym;
  }

  public boolean matchSynonym(String name) {
    return synonym != null && synonym.equalsIgnoreCase(name);
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

  public int getRecommendedMinCount() {
    return recommendedMinCount;
  }

  public int getRecommendedMaxCount() {
    return recommendedMaxCount;
  }

  public List<ManifestFieldProcessor> getFieldProcessors() {
    return processors;
  }

  public List<ManifestFieldDefinition> getFieldAttributes() {
    return attributes;
  }

  public static class Builder {

    private final List<ManifestFieldDefinition> fields = new ArrayList<>();

    public Field meta() {
      return new Field(this, ManifestFieldType.META);
    }

    public Field file() {
      return new Field(this, ManifestFieldType.FILE);
    }

    public Field attribute() {
      return new Field(this, ManifestFieldType.ATTRIBUTE);
    }

    public Field type(ManifestFieldType type) {
      return new Field(this, type);
    }

    public static class Field {
      private final Builder builder;
      private final ManifestFieldType type;
      private String name;
      private String synonym;
      private String description;
      private Integer minCount;
      private Integer maxCount;
      private boolean hidden = false;
      private boolean recommended = false;
      private List<ManifestFieldProcessor> processors = new ArrayList<>();
      private List<ManifestFieldDefinition> attributes = new ArrayList<>();

      private Field(Builder builder, ManifestFieldType type) {
        this.builder = builder;
        this.type = type;
      }

      public Field name(String name) {
        this.name = name;
        return this;
      }

      public Field synonym(String synonym) {
        this.synonym = synonym;
        return this;
      }

      public Field desc(String description) {
        this.description = description;
        return this;
      }

      public Field optional() {
        this.minCount = 0;
        this.maxCount = 1;
        return this;
      }

      public Field optional(int maxCount) {
        this.minCount = 0;
        this.maxCount = maxCount;
        return this;
      }

      public Field required() {
        this.minCount = 1;
        this.maxCount = 1;
        return this;
      }

      public Field hidden() {
        this.hidden = true;
        return this;
      }

      public Field recommended() {
        this.recommended = true;
        return this;
      }

      public Field processor(ManifestFieldProcessor... processors) {
        this.processors.addAll(
                Arrays.stream(processors).filter(Objects::nonNull).collect(Collectors.toList()));
        return this;
      }

      public Field attributes(List<ManifestFieldDefinition> attributes) {
        this.attributes.addAll(attributes.stream().filter(Objects::nonNull).collect(Collectors.toList()));

        return this;
      }

      public Builder and() {
        add();
        return builder;
      }

      public List<ManifestFieldDefinition> build() {
        add();
        return builder.fields;
      }

      private void add() {
        int recommendedMinCount = minCount;
        int recommendedMaxCount = maxCount;
        if (recommended) {
          recommendedMinCount = 1;
          if (recommendedMaxCount < 1) {
            recommendedMaxCount = 1;
          }
        }
        else if (hidden) {
          recommendedMinCount = 0;
          recommendedMaxCount = 0;
        }

        builder.fields.add(new ManifestFieldDefinition(
                name,
                synonym,
                description,
                type,
                minCount, maxCount,
                recommendedMinCount, recommendedMaxCount,
                processors,
                attributes));
      }
    }
  }
}
