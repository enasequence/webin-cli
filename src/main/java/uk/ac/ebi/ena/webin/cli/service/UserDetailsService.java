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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.utils.ExceptionUtils;
import uk.ac.ebi.ena.webin.cli.utils.RemoteServiceUrlHelper;
import uk.ac.ebi.ena.webin.cli.utils.RetryUtils;

public class UserDetailsService {
  private static final Logger log = LoggerFactory.getLogger(UserDetailsService.class);

  public static final String SERVICE_NAME = "UserDetails";

  private final String authToken;

  private final boolean test;

  public static class UserDetails {
    private String submissionAccountId;
    private boolean absConsent;

    public String getSubmissionAccountId() {
      return submissionAccountId;
    }

    public boolean isAbsConsent() {
      return absConsent;
    }
  }

  public UserDetailsService(String authToken, boolean test) {
    this.authToken = authToken;
    this.test = test;
  }

  public UserDetails get() throws WebinCliException, RuntimeException {
    RestTemplate restTemplate = new RestTemplate();

    UserDetails responseBody =
        ExceptionUtils.executeWithRestExceptionHandling(
            () ->
                RetryUtils.executeWithRetry(
                    context -> restTemplate.exchange(createRequest(), UserDetails.class).getBody(),
                    context -> log.warn("Retrying getting user details."),
                    HttpServerErrorException.class,
                    ResourceAccessException.class),
            WebinCliMessage.CLI_AUTHENTICATION_ERROR.text(),
            null,
            WebinCliMessage.SERVICE_SYSTEM_ERROR.format(SERVICE_NAME));

    return responseBody;
  }

  private RequestEntity createRequest() throws RuntimeException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + this.authToken);

    try {
      return RequestEntity.get(
              new URI(RemoteServiceUrlHelper.getWebinAuthUrl(test) + "admin/submission-account"))
          .headers(headers)
          .accept(MediaType.APPLICATION_JSON)
          .build();
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
  }
}
