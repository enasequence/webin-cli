/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.utils.ExceptionUtils;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;

public class
RunService extends WebinService
{
    private static final Logger log = LoggerFactory.getLogger(RunService.class);

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
        return getRun( runId, getUserName(), getPassword() );
    }

    
    private Run 
    getRun( String runId, String userName, String password )
    {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth( userName, password ).build();

        ResponseEntity<RunResponse> response = ExceptionUtils.executeWithRestExceptionHandling(

            () -> RetryUtils.executeWithRetry(
                retryContext -> restTemplate.exchange(resolveAgainstWebinRestV1Uri("cli/reference/run/{id}"),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    RunResponse.class,
                    runId.trim()),
                retryContext -> log.warn("Retrying run retrieval from server."),
                HttpServerErrorException.class, ResourceAccessException.class),

            WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format("Run"),
            WebinCliMessage.RUN_SERVICE_VALIDATION_ERROR.format( runId ),
            WebinCliMessage.RUN_SERVICE_SYSTEM_ERROR.format( runId ));

        RunResponse runResponse = response.getBody();
        if (runResponse == null || !runResponse.canBeReferenced)
            throw WebinCliException.userError(WebinCliMessage.RUN_SERVICE_VALIDATION_ERROR.format(runId));

        Run run = new Run();
        run.setRunId(runResponse.id);
        run.setName(runResponse.alias);
        return run;
    }
}
