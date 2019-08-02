package uk.ac.ebi.ena.webin.cli.manifest;

import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

import java.io.File;
import java.nio.file.Path;

public interface ManifestReaderInterface<M extends Manifest> {

    void readManifest( Path inputDir, File file );

    M getManifest();

    ManifestReaderResult getResult();

    ValidationResult getValidationResult();
}
