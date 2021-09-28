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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.handler.DefaultErrorHander;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;

public class RatelimitService extends WebinService {
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

    public boolean ratelimit(String context, String submissionAccountId, String studyId, String sampleId) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultErrorHander(WebinCliMessage.RATE_LIMIT_SERVICE_SYSTEM_ERROR.text()));

        HttpHeaders headers = new HttpHeaderBuilder().basicAuth(getUserName(), getPassword()).build();
        String url = getWebinRestUri("cli/submission/ratelimit/genome/", getTest());
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(new RatelimitService.RatelimitServiceRequest(context, submissionAccountId, studyId, sampleId), headers),
                String.class);
        return "true".equals(response.getBody());
    }
}
