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
package uk.ac.ebi.ena.webin.cli.utils;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import java.util.function.Supplier;

public class ExceptionUtils {

    /**
     * Runs given code and translates any {@link RestClientException} to {@link WebinCliException} if thrown.
     * Mapping rules are:<br/><br/>
     * HttpClientErrorException.Unauthorized or HttpClientErrorException.Forbidden -> WebinCliException.userError<br/>
     * HttpClientErrorException.NotFound -> WebinCliException.validationError<br/>
     * RestClientException -> WebinCliException.systemError
     *
     * @param supplier
     * @param authenticationUserErrorMessage Message to use for WebinCliException.userError in case of authentication failure.
     * @param validationErrorMessage Message to use for WebinCliException.validationError thrown in case of validation error.
     *                               If this is null and validation error occurs then WebinCliException.systemError is thrown instead.
     * @param systemErrorMessage Message to use for WebinCliException.systemError thrown in case of server error.
     * @return Returns back what the given supplier returns.
     * @param <T>
     * @throws WebinCliException
     */
    public static <T> T executeWithRestExceptionHandling(
        Supplier<T> supplier,
        String authenticationUserErrorMessage,
        String validationErrorMessage,
        String systemErrorMessage) throws WebinCliException {

        try {
            return supplier.get();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw WebinCliException.userError(authenticationUserErrorMessage);
        } catch (HttpClientErrorException.NotFound ex) {
            if (validationErrorMessage != null) {
                throw WebinCliException.validationError(validationErrorMessage);
            } else {
                throw WebinCliException.systemError(systemErrorMessage);
            }
        } catch (RestClientException ex) {
            throw WebinCliException.systemError(systemErrorMessage);
        }
    }
}
