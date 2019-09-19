/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.service.handler;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

public class NotFoundErrorHandler implements ResponseErrorHandler {

    private final String validationError;
    private final String systemError;

    public NotFoundErrorHandler(String validationError, String systemError) {
        this.validationError = validationError;
        this.systemError = systemError;
    }

    @Override
    public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
        return (httpResponse.getStatusCode().series() == CLIENT_ERROR ||
                httpResponse.getStatusCode().series() == SERVER_ERROR);
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse) throws IOException {
        switch (httpResponse.getStatusCode()) {
            case UNAUTHORIZED:
            case FORBIDDEN:
                throw WebinCliException.userError(WebinCliMessage.CLI_AUTHENTICATION_ERROR.text());
            case NOT_FOUND:
                throw WebinCliException.validationError(validationError);
            default:
                throw WebinCliException.systemError(systemError);
        }
    }
}