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

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.utils.ExceptionUtils;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;

public class 
IgnoreErrorsService extends WebinService {

    private static final Logger log = LoggerFactory.getLogger(IgnoreErrorsService.class);

    protected
    IgnoreErrorsService( AbstractBuilder<IgnoreErrorsService> builder )
    {
        super( builder );
    }


    public static class
    Builder extends AbstractBuilder<IgnoreErrorsService> {
        @Override
        public IgnoreErrorsService
        build() {
            return new IgnoreErrorsService(this);
        }
    }
    
    private static class IgnoreErrorsRequest {
        public final String context;
        public final String name;

        public IgnoreErrorsRequest(String context, String name) {
            this.context = context;
            this.name = name;
        }
    }

    public boolean
    getIgnoreErrors(String context, String name) {
        return getIgnoreErrors(getUserName(), getPassword(), context, name);
    }
    
    
    private boolean getIgnoreErrors(String userName, String password, String context, String name) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth(userName, password).build();

        ResponseEntity<String> response = ExceptionUtils.executeWithRestExceptionHandling(

            () -> RetryUtils.executeWithRetry(
                retryContext -> restTemplate.exchange(
                    resolveAgainstWebinRestV1Uri("cli/ignore_errors/"),
                    HttpMethod.POST,
                    new HttpEntity<>(new IgnoreErrorsRequest(context, name), headers),
                    String.class),
                retryContext -> log.warn("Retrying getting ignore error status from server."),
                HttpServerErrorException.class, ResourceAccessException.class),

            WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format("IgnoreError"),
            null,
            WebinCliMessage.IGNORE_ERRORS_SERVICE_SYSTEM_ERROR.text());

        return "true".equals(response.getBody());
    }
}
