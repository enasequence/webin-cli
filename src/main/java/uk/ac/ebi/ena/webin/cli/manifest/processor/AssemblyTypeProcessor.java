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
package uk.ac.ebi.ena.webin.cli.manifest.processor;

import uk.ac.ebi.ena.webin.cli.context.genome.GenomeManifestReader;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;

public class AssemblyTypeProcessor implements ManifestFieldProcessor {

    @Override public void
    process(ValidationResult result, ManifestFieldValue fieldValue ) {
        String value = fieldValue.getValue();
        if (value == null) {
            return;
        }
        if (value.trim().toUpperCase().equals("CLONE") ||
            value.trim().toUpperCase().equals("ISOLATE")) {
            fieldValue.setValue(GenomeManifestReader.ASSEMBLY_TYPE_PRIMARY_CLONE_OR_ISOLATE);
        }
    }

}