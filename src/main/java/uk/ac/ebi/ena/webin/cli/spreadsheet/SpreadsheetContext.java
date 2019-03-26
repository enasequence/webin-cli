package uk.ac.ebi.ena.webin.cli.spreadsheet;

import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.assembly.SequenceAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.assembly.TranscriptomeAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifest;

public enum SpreadsheetContext {

    GENOME(
            new GenomeAssemblyManifest(null, null, null),
            "genome"
    ),
    TRANSCRIPTOME(
            new TranscriptomeAssemblyManifest(null, null, null),
            "transcriptome"
    ),
    SEQUENCE(
            new SequenceAssemblyManifest(null),
            "sequence"
    ),
    READ(
            new RawReadsManifest(null, null),
            "read"
    );

    SpreadsheetContext(ManifestReader manifest, String name) {
        this.fileName = name + ".xlsx";
        this.sheetName = name;
        this.manifest = manifest;
    }

    private final String fileName;
    private final String sheetName;
    private final ManifestReader manifest;

    public ManifestReader getManifest() {
        return manifest;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSheetName() {
        return sheetName;
    }
}
