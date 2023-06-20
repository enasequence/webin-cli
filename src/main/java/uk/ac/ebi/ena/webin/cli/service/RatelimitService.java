/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
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
import uk.ac.ebi.ena.webin.cli.service.models.RateLimitResult;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.utils.ExceptionUtils;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;

public class RatelimitService extends WebinService {

    private static final Logger log = LoggerFactory.getLogger(RatelimitService.class);

    protected RatelimitService(AbstractBuilder<?> builder) {
        super(builder);
    }

    public static class
    Builder extends AbstractBuilder<RatelimitService> {
        @Override
        public RatelimitService
        build() {
            return new RatelimitService(this);
        }
    }

    private static class RatelimitServiceRequest {
        public final String context;
        public final String submissionAccountId;
        public final String studyId;
        public final String sampleId;

        public RatelimitServiceRequest(String context, String submissionAccountId, String studyId, String sampleId) {
            this.context = context;
            this.submissionAccountId = submissionAccountId;
            this.studyId = studyId;
            this.sampleId = sampleId;
        }
    }

    public RateLimitResult ratelimit(String context, String submissionAccountId, String studyId, String sampleId) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth(getUserName(), getPassword()).build();
        String url = resolveAgainstWebinRestUri("cli/submission/v2/ratelimit/");

        ResponseEntity<RateLimitResult> response = ExceptionUtils.executeWithRestExceptionHandling(

            () -> RetryUtils.executeWithRetry(
                retryContext -> restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(new RatelimitService.RatelimitServiceRequest(context, submissionAccountId, studyId, sampleId), headers),
                    RateLimitResult.class),
                retryContext -> log.warn("Retrying submission rate limiting check on server."),
                HttpServerErrorException.class, ResourceAccessException.class),

            WebinCliMessage.SERVICE_AUTHENTICATION_ERROR.format("RateLimit"),
            null,
            WebinCliMessage.RATE_LIMIT_SERVICE_SYSTEM_ERROR.text());

        return response.getBody();
    }
}
