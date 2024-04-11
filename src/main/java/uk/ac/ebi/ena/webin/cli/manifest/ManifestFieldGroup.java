package uk.ac.ebi.ena.webin.cli.manifest;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A manifest group is a collection of fields read from a manifest file. A single manifest file can have multiple groups
 * of fields. Each group is considered as an independent submission manifest.
 */
public class ManifestFieldGroup extends ArrayList<ManifestFieldValue> {

    public ManifestFieldValue getField(String fieldName) {
        return this.stream()
            .filter(field -> field.getName().equalsIgnoreCase(fieldName))
            .findFirst()
            .orElse(null);
    }

    public String getValue(String fieldName) {
        return this.stream()
            .filter(field -> field.getName().equalsIgnoreCase(fieldName))
            .findFirst()
            .map(field -> field.getValue())
            .orElse(null);
    }

    public Map<String, String> getNonEmptyValues(String... fieldNames) {
        Map<String, String> nameValues = new HashMap<>();

        this.forEach(
            field -> {
                for (String fieldName : fieldNames) {
                    if (field.getName().equalsIgnoreCase(fieldName)
                        && StringUtils.isNotBlank(field.getValue())) {
                        nameValues.put(fieldName, field.getValue());
                    }
                }
            });

        return nameValues;
    }

    public Collection<String> getValues(String fieldName) {
        return this.stream()
            .filter(field -> field.getName().equalsIgnoreCase(fieldName))
            .map(field -> field.getValue())
            .collect(Collectors.toList());
    }

    public int getCount(String fieldName) {
        return (int)
            this.stream().filter(field -> field.getName().equalsIgnoreCase(fieldName)).count();
    }
}
