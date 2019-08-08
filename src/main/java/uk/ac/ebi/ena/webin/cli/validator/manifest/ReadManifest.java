package uk.ac.ebi.ena.webin.cli.validator.manifest;

public class ReadManifest extends Manifest<SequenceManifest.FileType> {

    public enum FileType {
        BAM,
        CRAM,
        FASTQ
    }
}
