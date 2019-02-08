/*
 * Copyright 2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.service;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.service.handler.DefaultErrorHander;
import uk.ac.ebi.ena.service.utils.HttpUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliConfig;

public class IgnoreErrorsService {

    private static class IgnoreErrorsRequest {
        private final String context;
        private final String name;

        public IgnoreErrorsRequest(String context, String name) {
            this.context = context;
            this.name = name;
        }

        public String getContext() {
            return context;
        }

        public String getName() {
            return name;
        }
    }

    final static String SYSTEM_ERROR = "IgnoreErrorsServiceSystemError";

    private WebinCliConfig config = new WebinCliConfig();

    String getMessage(String messageKey) {
        return config.getServiceMessage(messageKey);
    }

    public boolean getIgnoreErrors(String userName, String password, String context, String name, boolean test) {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultErrorHander(getMessage(SYSTEM_ERROR)));

        ResponseEntity<String> response = restTemplate.exchange(
                WebinCliConfig.getWebinRestUri(config,"reference/cli/ignore_errors/", test),
                HttpMethod.POST,
                new HttpEntity(new IgnoreErrorsRequest(context, name), HttpUtils.authHeader(userName, password)),
                String.class);
        return "true".equals(response.getBody());
    }
}
