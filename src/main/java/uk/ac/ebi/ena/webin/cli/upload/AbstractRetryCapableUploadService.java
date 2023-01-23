package uk.ac.ebi.ena.webin.cli.upload;

import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.util.stream.Stream;

public abstract class AbstractRetryCapableUploadService implements UploadService {

    protected RetryTemplate createRetryTemplate(Class<? extends Exception>... retryOnErrors) {
        RetryTemplateBuilder builder = RetryTemplate.builder()
            .maxAttempts(4)
            .exponentialBackoff(1_000, 5, 10_000); // 1, 5, 10

        if (retryOnErrors != null) {
            for (int i = 0; i < retryOnErrors.length; i++) {
                builder = builder.retryOn(retryOnErrors[i]);
            }
        }

        return builder.build();
    }
}
