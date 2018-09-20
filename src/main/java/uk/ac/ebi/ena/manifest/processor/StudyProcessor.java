/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.manifest.processor;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class
StudyProcessor implements ManifestFieldProcessor
{
    private final WebinCliParameters parameters;
    private final ManifestFieldProcessor.Callback<Study> callback;

    public StudyProcessor(WebinCliParameters parameters, ManifestFieldProcessor.Callback<Study> callback )
    {
        this.parameters = parameters;
        this.callback = callback;
    }

    @Override public ValidationMessage<Origin>
    process(ManifestFieldValue fieldValue )
    {
        String value = fieldValue.getValue();

        try
        {
            Study study = Study.getStudy( value, parameters.getUsername(), parameters.getPassword(), parameters.isTestMode() );
            fieldValue.setValue(study.getProjectId());
            callback.notify(study);
            return null;
        } catch( WebinCliException e )
        {
            return ValidationMessage.error( "MANIFEST_STUDY_SERVER_ERROR", value, e.getMessage() );
        }
    }
}