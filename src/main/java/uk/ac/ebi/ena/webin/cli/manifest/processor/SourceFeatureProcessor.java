
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

import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.service.SourceFeatureService;

public class
SourceFeatureProcessor implements ManifestFieldProcessor
{
    private final WebinCliParameters parameters;
    private final ManifestFieldProcessor.Callback<SourceFeature> callback;

    public SourceFeatureProcessor(WebinCliParameters parameters, ManifestFieldProcessor.Callback<SourceFeature> callback)
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
            SourceFeatureService sourceFeatureService = new SourceFeatureService.Builder()
                                                                                .setCredentials( parameters.getUsername(), parameters.getPassword() )
                                                                                .setTest( parameters.isTestMode() )
                                                                                .build();
            SourceFeature source = sourceFeatureService.getSourceFeature( value );
            callback.notify(source);
            return null;
        } catch( WebinCliException e )
        {
            return WebinCliMessage.error(WebinCliMessage.Manifest.SAMPLE_LOOKUP_ERROR, value, e.getMessage() );
        }
    }
}
