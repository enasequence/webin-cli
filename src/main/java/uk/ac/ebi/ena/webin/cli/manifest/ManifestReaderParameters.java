package uk.ac.ebi.ena.webin.cli.manifest;

public interface ManifestReaderParameters {
    boolean isManifestValidateMandatory();
    boolean isManifestValidateFileExist();
    boolean isManifestValidateFileCount();
}
