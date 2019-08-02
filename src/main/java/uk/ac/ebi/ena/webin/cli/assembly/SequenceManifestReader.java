package uk.ac.ebi.ena.webin.cli.assembly;

import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileGroup;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

import java.util.List;

public abstract class SequenceManifestReader<M extends Manifest> extends ManifestReader
{
    public SequenceManifestReader(List<ManifestFieldDefinition> fields, List<ManifestFileGroup> fileGroups) {
        super(fields, fileGroups);
    }

    public abstract M getManifest();

    // TODO: remove
    @Override
    public String getName() {
        if (getManifest() != null) {
            return getManifest().getName();
        }
        return null;
    }

    // TODO: remove
    @Override
    public String getDescription() {
        if (getManifest() != null) {
            return getManifest().getDescription();
        }
        return null;
    }
}
