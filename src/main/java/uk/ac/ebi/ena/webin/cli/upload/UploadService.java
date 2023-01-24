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
package uk.ac.ebi.ena.webin.cli.upload;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface
UploadService
{
    void connect( String userName, String password );
    void upload(List<File> uploadFilesList, String uploadDir, Path inputDir );
    void disconnect();
    boolean isAvailable();

    static RetryTemplate createRetryTemplate(Class<? extends Exception>... retryOnErrors) {
        RetryTemplateBuilder builder = RetryTemplate.builder()
            .maxAttempts(6)
            .exponentialBackoff(5000, 2, 60_000); // 5s, 10s, 20s, 40s, 60s

        if (retryOnErrors != null) {
            for (int i = 0; i < retryOnErrors.length; i++) {
                builder = builder.retryOn(retryOnErrors[i]);
            }
        }

        return builder.build();
    }

    /**
     *
     * @param retryCallback
     * @param beforeRetryCallback - Invoked before every retry attempt. This does not include the first attempt.
     * @param retryOnErrors
     * @return
     * @param <T>
     * @param <E>
     * @throws E
     */
    static <T, E extends Throwable> T executeWithRetry(
        RetryCallback<T, E> retryCallback,
        Consumer<RetryContext> beforeRetryCallback,
        Class<? extends Exception>... retryOnErrors) throws E {

        RetryTemplate retryTemplate = createRetryTemplate(retryOnErrors);

        return retryTemplate.execute(ctx -> {
            if (ctx.getRetryCount() > 0) {
                beforeRetryCallback.accept(ctx);
            }

            return retryCallback.doWithRetry(ctx);
        });
    }
}
