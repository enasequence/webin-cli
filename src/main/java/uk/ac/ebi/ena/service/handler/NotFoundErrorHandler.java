package uk.ac.ebi.ena.service.handler;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

import java.io.IOException;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

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
                throw WebinCliException.userError(WebinCliMessage.Cli.AUTHENTICATION_ERROR.format());
            case NOT_FOUND:
                throw WebinCliException.validationError(validationError);
            default:
                throw WebinCliException.systemError(systemError);
        }
    }
}