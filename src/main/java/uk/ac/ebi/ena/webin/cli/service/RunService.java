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
package uk.ac.ebi.ena.webin.cli.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.handler.NotFoundErrorHandler;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;

public class
RunService extends WebinService
{

    protected 
    RunService( AbstractBuilder<RunService> builder )
    {
        super( builder );
    }

    
    public static class 
    Builder extends AbstractBuilder<RunService>
    {
        @Override public RunService 
        build()
        {
            return new RunService( this );
        }
    };

    
    public static class 
    RunResponse 
    {
        public String id;
        public String alias;
        public boolean canBeReferenced;
    }

    
    public Run 
    getRun( String runId )
    {
        return getRun( runId, getUserName(), getPassword(), getTest() );
    }

    
    private Run 
    getRun( String runId, String userName, String password, boolean test )
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler( new NotFoundErrorHandler( WebinCliMessage.Service.RUN_SERVICE_VALIDATION_ERROR.format( runId ),
                                                                WebinCliMessage.Service.RUN_SERVICE_SYSTEM_ERROR.format( runId ) ) );

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth( userName, password ).build();

        ResponseEntity<RunResponse> response = restTemplate.exchange( getWebinRestUri( "reference/run/{id}", test ),
                                                                      HttpMethod.GET,
                                                                      new HttpEntity<>( headers ),
                                                                      RunResponse.class,
                                                                      runId.trim() );

        RunResponse runResponse = response.getBody();
        if( runResponse == null || !runResponse.canBeReferenced )
            throw WebinCliException.userError( WebinCliMessage.Service.RUN_SERVICE_VALIDATION_ERROR.format( runId ) );

        Run run = new Run();
        run.setRunId(runResponse.id);
        run.setName(runResponse.alias);
        return run;
    }
}
