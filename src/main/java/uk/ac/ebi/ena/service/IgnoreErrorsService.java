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

import lombok.Data;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.service.handler.IgnoreErrorsErrorHander;
import uk.ac.ebi.ena.service.utils.HttpUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliConfig;

public class IgnoreErrorsService {

    @Data
    private static class IgnoreErrorsRequest {
        private final String context;
        private final String name;
    }

    public final static String SYSTEM_ERROR =
            "A server error occurred when retrieving ignore error information.";


    WebinCliConfig config = new WebinCliConfig();

    private String getUri(boolean test) {
        String uri = "reference/cli/ignore_errors/";
        return (test) ?
                config.getWebinRestUriTest() + uri :
                config.getWebinRestUriProd() + uri;
    }

    public boolean getIgnoreErrors(String userName, String password, String context, String name, boolean test) {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new IgnoreErrorsErrorHander(SYSTEM_ERROR));

        ResponseEntity<String> response = restTemplate.exchange(
                getUri(test),
                HttpMethod.POST,
                new HttpEntity(new IgnoreErrorsRequest(context, name), HttpUtils.authHeader(userName, password)),
                String.class);
        return "true".equals(response.getBody());
    }
}
