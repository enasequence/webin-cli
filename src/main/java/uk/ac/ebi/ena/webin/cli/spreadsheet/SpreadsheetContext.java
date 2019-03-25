package uk.ac.ebi.ena.webin.cli.spreadsheet;

import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.assembly.SequenceAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.assembly.TranscriptomeAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum SpreadsheetContext {

    GENOME(
            new GenomeAssemblyManifest(null, null, null),
            "genome",
            Arrays.asList(GenomeAssemblyManifest.Fields.ASSEMBLYNAME)
    ),
    TRANSCRIPTOME(
            new TranscriptomeAssemblyManifest(null, null, null),
            "transcriptome",
            Arrays.asList(TranscriptomeAssemblyManifest.Fields.ASSEMBLYNAME)
    ),
    SEQUENCE(
            new SequenceAssemblyManifest(null),
            "sequence",
            Arrays.asList()
    ),
    READ(
            new RawReadsManifest(null, null),
            "read",
            Arrays.asList(RawReadsManifest.Fields.__HORIZON)
    );

    SpreadsheetContext(ManifestReader manifest, String name, List<String> ignoreFields) {
        this.fileName = name + ".xlsx";
        this.sheetName = name;
        this.ignoreFields = new HashSet<>(ignoreFields);
        this.manifest = manifest;
    }

    private final String fileName;
    private final String sheetName;
    private final Set<String> ignoreFields;
    private final ManifestReader manifest;

    public String getFileName() {
        return fileName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public Set<String> getIgnoreFields() {
        return ignoreFields;
    }

    public ManifestReader getManifest() {
        return manifest;
    }
}
