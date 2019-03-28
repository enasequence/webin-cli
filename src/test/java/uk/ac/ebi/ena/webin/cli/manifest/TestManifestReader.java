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
package uk.ac.ebi.ena.webin.cli.manifest;

import java.util.List;
import java.util.Set;

public class TestManifestReader extends ManifestReader {

    public TestManifestReader(List<ManifestFieldDefinition> fields) {
        super(fields);
    }

    public TestManifestReader(List<ManifestFieldDefinition> fields, List<List<ManifestFileCount>> files) {
        super(fields, files);
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    protected void processManifest() {
    }

    @Override
    public String getDescription()
    {
        return "Description";
    }
}
