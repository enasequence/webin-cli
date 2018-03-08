package uk.ac.ebi.ena.version;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Version {
    private final static String SYSTEM_ERROR_INTERNAL = "An internal server error occurred when checking application version.";
    private final static String SYSTEM_ERROR_UNAVAILABLE = "A service unavailable error occurred when checking application version. ";
    private final static String SYSTEM_ERROR_BAD_REQUEST = "A bad request error occurred when attempting to submit. ";
    private final static String SYSTEM_ERROR_NOT_FOUND = "A not found request error occurred when attempting to submit. ";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when checking application version. ";

    public boolean isVersionValid(String version) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(("https://wwwdev.ebi.ac.uk/ena/submit/drop-box/check_version/cli/") + version);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            int responsecode = response.getStatusLine().getStatusCode();
            switch (responsecode) {
                case HttpStatus.SC_OK:
                    List<String> resultsList = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
                    Optional<String> optional = resultsList.stream()
                            .filter(p -> "true".equalsIgnoreCase(p))
                            .findFirst();
                    return optional.isPresent();
                case HttpStatus.SC_BAD_REQUEST:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_BAD_REQUEST);
                case HttpStatus.SC_NOT_FOUND:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_NOT_FOUND);
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_INTERNAL);
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_UNAVAILABLE);
                case HttpStatus.SC_UNAUTHORIZED:
                    throw WebinCliException.createUserError(WebinCli.AUTHENTICATION_ERROR);
                default:
                    throw WebinCliException.createSystemError(SYSTEM_ERROR_OTHER);
            }
        } catch (IOException e) {
            throw WebinCliException.createSystemError(SYSTEM_ERROR_OTHER, e.getMessage());
        }
    }
}
