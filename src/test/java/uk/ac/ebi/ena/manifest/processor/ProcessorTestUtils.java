package uk.ac.ebi.ena.manifest.processor;

import uk.ac.ebi.ena.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.manifest.ManifestFieldType;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;

public class ProcessorTestUtils {

    static ManifestFieldValue
    createFieldValue(ManifestFieldType type, String name, String value) {
        return new ManifestFieldValue(new ManifestFieldDefinition(
                name, type, 0, 1), value, null);
    }
}
