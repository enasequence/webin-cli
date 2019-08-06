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

import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.model.reference.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.service.StudyService;

public class
StudyProcessor implements ManifestFieldProcessor
{
    private final WebinCliParameters parameters;
    private ManifestFieldProcessor.Callback<Study> callback;

    public
    StudyProcessor( WebinCliParameters parameters, ManifestFieldProcessor.Callback<Study> callback )
    {
        this.parameters = parameters;
        this.callback = callback;
    }

    public
    StudyProcessor( WebinCliParameters parameters )
    {
        this.parameters = parameters;
    }

    public void setCallback(Callback<Study> callback) {
        this.callback = callback;
    }
    
    @Override public ValidationResult
    process( ManifestFieldValue fieldValue )
    {
        String value = fieldValue.getValue();

        try
        {
            StudyService studyService = new StudyService.Builder()
                                                        .setCredentials( parameters.getUsername(), parameters.getPassword() )
                                                        .setTest( parameters.isTestMode() )
                                                        .build();
            Study study = studyService.getStudy( value );
            fieldValue.setValue( study.getBioProjectId() );
            callback.notify( study );
            return new ValidationResult();
            
        } catch( WebinCliException e ) {
            if (WebinCliMessage.Cli.AUTHENTICATION_ERROR.text.equals(e.getMessage())) {
                return new ValidationResult().append(WebinCliMessage.error(WebinCliMessage.Cli.AUTHENTICATION_ERROR));
            }
            return new ValidationResult().append(WebinCliMessage.error(WebinCliMessage.Manifest.STUDY_LOOKUP_ERROR, value, e.getMessage()));
        }
    }
}
