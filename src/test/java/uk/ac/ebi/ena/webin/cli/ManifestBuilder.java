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
package uk.ac.ebi.ena.webin.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Path;

public class ManifestBuilder {

    public enum ManifestFormat {
        KEY_VALUE,
        JSON
    }

    private ManifestFormat manifestFormat = ManifestFormat.KEY_VALUE;

    private String manifest = "";

    private ObjectNode jsonManifest;

    public ManifestBuilder jsonFormat() {
        manifestFormat = ManifestFormat.JSON;
        return this;
    }

    public ManifestBuilder manifest(String manifest) {
        if (manifest != null) {
            this.manifest += manifest;
        }
        return this;
    }

    public ManifestBuilder manifest(ManifestBuilder manifest) {
        if (manifest != null) {
            this.manifest += manifest;
        }
        return this;
    }

    public ManifestBuilder name() {
        return field("NAME", String.format("TEST %X", System.currentTimeMillis()));
    }

    public ManifestBuilder field(String field, String value) {
        if (manifestFormat == ManifestFormat.KEY_VALUE) {
            if (field != null && value != null) {
                manifest += field + "\t" + value + "\n";
            }
        } else {
            if (jsonManifest == null) {
                jsonManifest = new ObjectMapper().createObjectNode();
            }

            jsonManifest.put(field, value);
        }

        return this;
    }

    public ManifestBuilder attribute(String field, String attributeKey, String attributeValue) {
        if (manifestFormat != ManifestFormat.JSON) {
            return this;
        }

        JsonNode fieldNode = jsonManifest.get(field);
        if (fieldNode == null) {
             throw new RuntimeException("Field not found : " + field);
        }

        if (fieldNode.isValueNode()) {
            //replace {field: value} with {field: {value: value, attributes: {...}}}

            String currentValue = fieldNode.asText();

            ObjectNode newField = jsonManifest.putObject(field);
            newField.put("value", currentValue);

            newField.putObject("attributes").put(attributeKey, attributeValue);
        } else {
            ObjectNode fieldObjectNode = (ObjectNode) fieldNode;

            if (fieldObjectNode.has("attributes")) {
                ObjectNode atts = (ObjectNode) fieldObjectNode.get("attributes");
                if (!atts.has(attributeKey)) {
                    atts.put(attributeKey, attributeValue);
                } else {
                    if (atts.get(attributeKey).isArray()) {
                        //if attribute is already an array then append the value in it.
                        ((ArrayNode)atts.get(attributeKey)).add(attributeValue);
                    } else {
                        //convert attribute to array and put back old and new values in it.

                        String currentValue = atts.get(attributeKey).asText();

                        ArrayNode attArray = atts.putArray(attributeKey);
                        attArray.add(currentValue);
                        attArray.add(attributeValue);
                    }
                }
            } else {
                fieldObjectNode.putObject("attributes").put(attributeKey, attributeValue);
            }
        }

        return this;
    }

    public ManifestBuilder file(String field, File file) {
        return field(field, file.getName());
    }

    public ManifestBuilder file(String field, String file) {
        return field(field, file);
    }

    public ManifestBuilder file(String field, Path file) {
        return field(field, file.getFileName().toString());
    }

    public <FileType extends Enum<FileType>> ManifestBuilder file(
            FileType fileType, String fileName) {
        return field(fileType.name(), fileName);
    }

    public <FileType extends Enum<FileType>> ManifestBuilder file(FileType fileType, File file) {
        return field(fileType.name(), file.getName());
    }

    public <FileType extends Enum<FileType>> ManifestBuilder file(FileType fileType, Path file) {
        return field(fileType.name(), file.getFileName().toString());
    }

    public <FileType extends Enum<FileType>> ManifestBuilder file(FileType fileType, int cnt) {
        for (int i = 0; i < cnt; ++i) {
            file(fileType);
        }
        return this;
    }

    public <FileType extends Enum<FileType>> ManifestBuilder file(FileType... fileTypes) {
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
        if (manifestFormat == ManifestFormat.KEY_VALUE) {
            return TempFileBuilder.file(manifest).toFile();
        } else {
            try {
                return TempFileBuilder.file(new ObjectMapper().writeValueAsString(jsonManifest)).toFile();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public File build(File inputDir) {
        return build(inputDir.toPath());
    }

    public File build(Path inputDir) {
        return  TempFileBuilder.file(inputDir, manifest).toFile();
    }

    @Override
    public String toString() {
        return manifest;
    }
}
