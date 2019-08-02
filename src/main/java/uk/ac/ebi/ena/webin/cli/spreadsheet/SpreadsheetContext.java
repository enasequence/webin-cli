/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.spreadsheet;

import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyManifestReader;
import uk.ac.ebi.ena.webin.cli.assembly.SequenceAssemblyManifestReader;
import uk.ac.ebi.ena.webin.cli.assembly.TranscriptomeAssemblyManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifest;

public enum SpreadsheetContext {

    GENOME(
            new GenomeAssemblyManifestReader( null, null, null, null, null ),
            "genome",
            "Additionally, primary and binned metagenomes must have " +
            ManifestReader.getFileGroupText( GenomeAssemblyManifestReader.PRIMARY_AND_BINNED_METAGENOME_FILE_GROUPS ) + " files."
    ),
    TRANSCRIPTOME(
            new TranscriptomeAssemblyManifestReader( null, null, null, null, null ),
            "transcriptome",
            null
    ),
    SEQUENCE(
            new SequenceAssemblyManifestReader( null, null, null ),
            "sequence",
            null
    ),
    READ(
            new RawReadsManifest( null, null ),
            "read",
            null
    );

    SpreadsheetContext(ManifestReader manifest, String name, String extraFileGroupText) {
        this.fileName = name + ".xlsx";
        this.sheetName = name;
        this.manifest = manifest;
        this.extraFileGroupText = extraFileGroupText;
    }

    private final String fileName;
    private final String sheetName;
    private final ManifestReader manifest;
    private final String extraFileGroupText;

    public ManifestReader getManifest() {
        return manifest;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public String getFileGroupText() {
        String fileGroupText =
                "Submission must have " +
                ManifestReader.getFileGroupText(manifest.getFileGroups()) +
                " files.";
        if (extraFileGroupText != null) {
            fileGroupText += " " + extraFileGroupText;
        }
        return fileGroupText;
    }
}
