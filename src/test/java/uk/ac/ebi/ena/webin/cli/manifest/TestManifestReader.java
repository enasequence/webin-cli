/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.manifest;

import java.util.List;

import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

public class TestManifestReader extends ManifestReader {

    public TestManifestReader(List<ManifestFieldDefinition> fields) {
        super(null, fields);
    }

    public TestManifestReader(List<ManifestFieldDefinition> fields, List<ManifestFileGroup> fileGroups) {
        super(null, fields, fileGroups);
    }

    @Override
    protected void processManifest() {
    }

    @Override
    public Manifest getManifest() {
        return null;
    }
}
