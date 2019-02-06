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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import java.io.IOException;
import java.util.Base64;

public final class IgnoreErrorsService {

    private IgnoreErrorsService() {
        throw new UnsupportedOperationException();
    }

    private final static String SYSTEM_ERROR_INTERNAL = "An internal server error occurred when attempting to ignore errors. ";
    private final static String SYSTEM_ERROR_UNAVAILABLE = "A service unavailable error occurred when attempting to ignore errors. ";
    private final static String SYSTEM_ERROR_BAD_REQUEST = "A bad request error occurred when attempting to ignore errors. ";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when when attempting to ignore errors. ";

    public static boolean getIgnoreErrors(String userName, String password, String context, String name, boolean TEST) {

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(TEST ?
                    "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/cli/ignore_errors/" :
                    "https://www.ebi.ac.uk/ena/submit/drop-box/cli/ignore_errors/");

            String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
            httpPost.setHeader("Authorization", "Basic " + encoding);

            httpPost.setEntity(
                    new StringEntity(
                            "{\n" +
                                    "\"context\":\"" + context + "\",\n" +
                                    "\"name\":\"" + name + "\"\n" +
                                    "}",
                            ContentType.APPLICATION_JSON));

            CloseableHttpResponse response = httpClient.execute(httpPost);

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    return Boolean.valueOf(EntityUtils.toString(response.getEntity()));
                case HttpStatus.SC_UNAUTHORIZED:
                    throw WebinCliException.createUserError(WebinCli.AUTHENTICATION_ERROR);
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_INTERNAL);
                case HttpStatus.SC_BAD_REQUEST:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_BAD_REQUEST);
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_UNAVAILABLE);
                default:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_OTHER);
            }
        } catch (IOException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_OTHER, e.getMessage());
        }
    }
}
