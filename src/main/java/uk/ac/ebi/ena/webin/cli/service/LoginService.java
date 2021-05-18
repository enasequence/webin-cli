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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.handler.DefaultErrorHander;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;

public class
LoginService
{
    private final static String TEST_URL = "https://www.ebi.ac.uk/ena/submit/webin/auth";
    private final static String PRODUCTION_URL = "https://www.ebi.ac.uk/ena/submit/webin/auth";
    private final String username;
    private final String password;
    private final boolean test;

    public static class LoginRequestBody {
        public final List<String> authRealms = new ArrayList<>();
        public final String username;
        public final String password;

        public LoginRequestBody(String username, String password) {
            this.authRealms.add("ENA");
            this.username = username;
            this.password = password;
        }
    }

    public static class LoginResponseBody {
        public boolean authenticated;
        public String principle;
    }

    private String getUri(String uri, boolean test) {
        return (test) ?
                TEST_URL + uri :
                PRODUCTION_URL + uri;
    }

    public LoginService(String username, String password, boolean test) {
        this.username = username;
        this.password = password;
        this.test = test;
    }

    public String login() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultErrorHander(WebinCliMessage.CLI_AUTHENTICATION_ERROR.text()));

        LoginRequestBody requestBody = new LoginRequestBody(username, password);

        RequestEntity< LoginRequestBody > request;
        try {
            request = RequestEntity
                    .post(new URI(getUri("/login", test)))
                    .header("Authorization", HttpHeaderBuilder.basicAuthHeaderValue(username, password))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody);
        }
        catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }

        LoginResponseBody responseBody = restTemplate.exchange(request, LoginResponseBody.class).getBody();

        if (!responseBody.authenticated ||
                responseBody.principle == null ||
                !responseBody.principle.matches("^Webin-\\d+")) {
            throw WebinCliException.userError(WebinCliMessage.CLI_AUTHENTICATION_ERROR.text());
        }

        return responseBody.principle;
    }
}
