package uk.ac.ebi.ena.manifest;

import java.util.Arrays;
import java.util.List;

public interface ManifestFieldCv {

    List<String> BOOLEAN_FIELD_VALUES = Arrays.asList(
            "yes",
            "no",
            "true",
            "false",
            "Y",
            "N"
    );
}
