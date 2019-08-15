package uk.ac.ebi.ena.webin.cli.manifest;

import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

public interface ManifestReaderEx<M extends Manifest> {
    M getManifest();
}
