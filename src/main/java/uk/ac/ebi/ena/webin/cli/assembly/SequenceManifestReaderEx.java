package uk.ac.ebi.ena.webin.cli.assembly;

import uk.ac.ebi.ena.webin.cli.manifest.*;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

import java.util.List;

public abstract class SequenceManifestReaderEx<M extends Manifest> extends ManifestReader implements ManifestReaderEx<M>
{
    public SequenceManifestReaderEx(ManifestReaderParameters parameters, List<ManifestFieldDefinition> fields, List<ManifestFileGroup> fileGroups) {
        super(parameters, fields, fileGroups);
    }

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
