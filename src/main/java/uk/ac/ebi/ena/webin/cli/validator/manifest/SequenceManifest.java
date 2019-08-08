package uk.ac.ebi.ena.webin.cli.validator.manifest;

public class SequenceManifest extends Manifest<SequenceManifest.FileType> {

    public enum FileType {
        FLATFILE,
        TAB
    }
}
