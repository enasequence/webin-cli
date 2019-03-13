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
package uk.ac.ebi.ena.webin.cli.service.utils;

import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class HttpHeaderBuilder {

    private final HttpHeaders headers = new HttpHeaders();

    private HttpHeaderBuilder set(String headerName, String headerValue) {
        headers.set(headerName, headerValue);
        return this;
    }

    public HttpHeaderBuilder basicAuth(String userName, String password) {
        String auth = userName + ":" + password;
        return set("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));
    }

    public HttpHeaderBuilder multipartFormData() {
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return this;
    }

    public HttpHeaders build() {
        return headers;
    }
}