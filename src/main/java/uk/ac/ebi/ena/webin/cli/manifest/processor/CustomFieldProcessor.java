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

import org.apache.commons.lang3.tuple.ImmutablePair;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

public class
CustomFieldProcessor implements ManifestFieldProcessor {

    private ManifestFieldProcessor.Callback<ImmutablePair<String, String>> callback;

    public void setCallback(Callback<ImmutablePair<String, String>> callback) {
        this.callback = callback;
    }

    @Override
    public void
    process(ValidationResult result, ManifestFieldValue fieldValue) {
        String value = fieldValue.getValue();

        if (value == null || value.trim().isEmpty()) {
            result.add(ValidationMessage.error(WebinCliMessage.CUSTOM_FIELD_PROCESSOR_INCORRECT_FIELD_VALUE, fieldValue.getName(), value));
            return;
        }

        String keyVal[] = value.split(":");
        if (keyVal.length == 2) {
            ImmutablePair<String, String> pair = new ImmutablePair<>(keyVal[0], keyVal[1]);
            callback.notify(pair);
        } else {
            result.add(ValidationMessage.error(WebinCliMessage.CUSTOM_FIELD_PROCESSOR_INCORRECT_FIELD_VALUE, fieldValue.getName(), value));
        }

    }

}
