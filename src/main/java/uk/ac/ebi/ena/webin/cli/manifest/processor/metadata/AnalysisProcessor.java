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
package uk.ac.ebi.ena.webin.cli.manifest.processor.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.service.AnalysisService;
import uk.ac.ebi.ena.webin.cli.validator.reference.Analysis;

public class
AnalysisProcessor implements ManifestFieldProcessor
{
    private final WebinCliParameters parameters;
    private ManifestFieldProcessor.Callback<List<Analysis>> callback;

    public
    AnalysisProcessor( WebinCliParameters parameters, ManifestFieldProcessor.Callback<List<Analysis>> callback )
    {
        this.parameters = parameters;
        this.callback = callback;
    }

    public
    AnalysisProcessor( WebinCliParameters parameters )
    {
        this.parameters = parameters;
    }

    public void setCallback(Callback<List<Analysis>> callback) {
        this.callback = callback;
    }

    @Override public ValidationResult
    process( ManifestFieldValue fieldValue )
    {
        String value = fieldValue.getValue();
        String[] ids = value.split( ", *" );
        List<Analysis> analysis_list = new ArrayList<Analysis>( ids.length );
        ValidationResult result    = new ValidationResult();
        
        for( String a : ids )
        {
            String id = a.trim();
            if( id.isEmpty() )
                continue;

            try
            {
                AnalysisService analysisService = new AnalysisService.Builder()
                                                                     .setCredentials( parameters.getUsername(), parameters.getPassword() )
                                                                     .setTest( parameters.isTestMode() )
                                                                     .build();
                analysis_list.add( analysisService.getAnalysis( id ) );
                
            } catch( WebinCliException e )
            {
                result.append( WebinCliMessage.error( WebinCliMessage.Manifest.ANALYSIS_LOOKUP_ERROR, id, e.getMessage() ) );
            }
        }
        
        if( result.isValid() )
        {
            fieldValue.setValue( analysis_list.stream()
                                              .map( e -> e.getAnalysisId() )
                                              .collect( Collectors.joining( ", " ) ) );
            callback.notify( analysis_list );
        }
        
        return result;
    }
}
