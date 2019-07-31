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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.service.RunService;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;

public class
RunProcessor implements ManifestFieldProcessor
{
    private final WebinCliParameters parameters;
    private final ManifestFieldProcessor.Callback<List<Run>> callback;

    public 
    RunProcessor( WebinCliParameters parameters, ManifestFieldProcessor.Callback<List<Run>> callback )
    {
        this.parameters = parameters;
        this.callback = callback;
    }

    
    @Override public ValidationResult
    process( ManifestFieldValue fieldValue )
    {
        ValidationResult result = new ValidationResult();
        String value = fieldValue.getValue();
        String[] ids = value.split( ", *" );
        List<Run> run_list = new ArrayList<Run>( ids.length );
        
        for( String r : ids )
        {
            String id = r.trim();
            if( id.isEmpty() )
                continue;

            try
            {
                RunService runService = new RunService.Builder()
                                                      .setCredentials( parameters.getUsername(), parameters.getPassword() )
                                                      .setTest( parameters.isTestMode() )
                                                      .build();
                run_list.add( runService.getRun( id ) );
                           
            } catch( WebinCliException e )
            {
                result.append( WebinCliMessage.error( WebinCliMessage.Manifest.RUN_LOOKUP_ERROR, id, e.getMessage() ) );
            }
        }
        
        if( result.isValid() )
        {
            fieldValue.setValue( run_list.stream()
                                         .map( e -> e.getRunId() )
                                         .collect( Collectors.joining( ", " ) ) );
            
            callback.notify( run_list );
        }
        
        return result;
    }
}
