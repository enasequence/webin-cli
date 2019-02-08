package uk.ac.ebi.ena.service.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Base64;

public class HttpHeaderBuilder {

    private HttpHeaders headers = new HttpHeaders();

    public HttpHeaderBuilder set(String headerName, String headerValue) {
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