
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

import java.util.List;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;

public class FileSuffixProcessor implements ManifestFieldProcessor {

    private final List<String> suffixes;

    public FileSuffixProcessor(List<String> suffixes ) {
        this.suffixes = suffixes;
    }

    @Override
    public ValidationMessage<Origin> process(ManifestFieldValue fieldValue) {

        if( null == suffixes || suffixes.isEmpty() )
            return null;

        for( String suffix : suffixes )
        {
            if( fieldValue.getValue().endsWith( suffix ) )
                return null;
        }

        return WebinCliMessage.error(WebinCliMessage.Manifest.INVALID_FILE_SUFFIX_ERROR,
                fieldValue.getName(),
                fieldValue.getValue(),
                String.join(", ", suffixes));
    }
}
