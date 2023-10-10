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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.utils.HttpHeaderBuilder;
import uk.ac.ebi.ena.webin.cli.utils.ExceptionUtils;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;

public class
LoginService
{
    private static final Logger log = LoggerFactory.getLogger(LoginService.class);
    private final static String TEST_URL = "https://www.ebi.ac.uk/ena/submit/webin/auth";
    private final static String PRODUCTION_URL = "https://www.ebi.ac.uk/ena/submit/webin/auth";
    private final String username;
    private final String password;
    private final boolean test;

    public final static String SERVICE_NAME = "Login";

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
        RequestEntity< LoginRequestBody > request =  getAuthRequest("/login");

        RestTemplate restTemplate = new RestTemplate();

        LoginResponseBody responseBody = ExceptionUtils.executeWithRestExceptionHandling(

            () -> RetryUtils.executeWithRetry(
                context -> restTemplate.exchange(request, LoginResponseBody.class).getBody(),
                context -> log.warn("Retrying authentication."),
                HttpServerErrorException.class, ResourceAccessException.class),

            WebinCliMessage.CLI_AUTHENTICATION_ERROR.text(),
            null,
            WebinCliMessage.SERVICE_SYSTEM_ERROR.format(SERVICE_NAME));

        if (!responseBody.authenticated ||
            responseBody.principle == null ||
            !responseBody.principle.matches("^Webin-\\d+")) {
            throw WebinCliException.userError(WebinCliMessage.CLI_AUTHENTICATION_ERROR.text());
        }

        return responseBody.principle;
    }

    /**
     * This method returns authentication token for the given webin-cli user/paassword 
     * @return token
     */
    public String getAuthToken() {
        RequestEntity< LoginRequestBody > request =  getAuthRequest("/token");

        RestTemplate restTemplate = new RestTemplate();

        return ExceptionUtils.executeWithRestExceptionHandling(

            () -> RetryUtils.executeWithRetry(
                context -> restTemplate.exchange(request, String.class).getBody(),
                context -> log.warn("Retrying authentication."),
                HttpServerErrorException.class, ResourceAccessException.class),

            WebinCliMessage.CLI_AUTHENTICATION_ERROR.text(),
            null,
            WebinCliMessage.SERVICE_SYSTEM_ERROR.format(SERVICE_NAME));
    }
    
    private RequestEntity<LoginRequestBody> getAuthRequest(String url){
        LoginRequestBody requestBody = new LoginRequestBody(username, password);
        RequestEntity< LoginRequestBody > request=null;
        try {
            request = RequestEntity
                    .post(new URI(getUri(url, test)))
                    .header("Authorization", HttpHeaderBuilder.basicAuthHeaderValue(username, password))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody);
        }
        catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
        return request;
    }
}
