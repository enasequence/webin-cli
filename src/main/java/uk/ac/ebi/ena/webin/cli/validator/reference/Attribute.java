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
package uk.ac.ebi.ena.webin.cli.validator.reference;

/**
 * Reference attribute with tag, value and unit.
 */
public class Attribute {

    private final String tag;
    private final String value;
    private final String unit;

    public Attribute(String tag) {
        this.tag = tag;
        this.value = null;
        this.unit = null;
    }

    public Attribute(String tag, String value) {
        this.tag = tag;
        this.value = value;
        this.unit = null;
    }

    public Attribute(String tag, String value, String unit) {
        this.tag = tag;
        this.value = value;
        this.unit = unit;
    }

    public String getTag() {
        return tag;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }
}
