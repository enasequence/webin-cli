package uk.ac.ebi.ena.service.utils;

import org.springframework.http.HttpHeaders;

import java.util.Base64;

public class HttpUtils {

    public static HttpHeaders authHeader(String userName, String password) {
        return new HttpHeaders() {{
                String auth = userName + ":" + password;
                set("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));
            }};
    }
}
