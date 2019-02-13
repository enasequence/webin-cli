package uk.ac.ebi.ena.manifest;

import java.util.List;
import java.util.Set;

public class TestManifestReader extends ManifestReader {

    public TestManifestReader(List<ManifestFieldDefinition> fields) {
        super(fields);
    }

    public TestManifestReader(List<ManifestFieldDefinition> fields, Set<List<ManifestFileCount>> files) {
        super(fields, files);
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    protected void processManifest() {
    }

    @Override
    public String getDescription()
    {
        return "Description";
    }
}
