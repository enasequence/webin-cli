/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.service.handler.DefaultErrorHander;
import uk.ac.ebi.ena.webin.cli.WebinCliConfig;

public class VersionService {
    private final static String SYSTEM_ERROR = "VersionServiceSystemError";

    private WebinCliConfig config = new WebinCliConfig();

    private String getUri(boolean test) {
        String uri = "check_version/cli/{version}";
        return (test) ?
                config.getWebinRestUriTest() + uri :
                config.getWebinRestUriProd() + uri;
    }

    String getMessage(String messageKey) {
        return config.getServiceMessage(messageKey);
    }

    public boolean isVersionValid(String version, boolean test ) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultErrorHander(getMessage(SYSTEM_ERROR)));
        ResponseEntity<String> response = restTemplate.getForEntity(getUri(test), String.class, version);
        String body = response.getBody();
        return "true".equals(body);
    }
}
