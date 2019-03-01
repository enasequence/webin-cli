
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
package uk.ac.ebi.ena.webin.cli.reporter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;

public class ValidationMessageReporter implements Closeable {

    protected final OutputStream strm;
    protected final boolean close;

    private static final HashSet<Severity> REPORT_SEVERITY = new HashSet<>(Arrays.asList(
            Severity.INFO,
            Severity.ERROR));

    private static final Logger log = LoggerFactory.getLogger(ValidationMessageReporter.class);

    public ValidationMessageReporter(File file) {
        OutputStream strm;
        boolean close;
            try {
                strm = Files.newOutputStream(
                        file.toPath(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.SYNC);
                close = true;
            } catch (IOException e) {
                log.warn("Failed to create validation reporter: " + file.getName());
                strm = System.out;
                close = false;
            }
        this.strm = strm;
        this.close = close;
    }

    public static ValidationMessage<Origin>
    createValidationMessage(Severity severity, String message) {
        return createValidationMessage(severity, message, null);
    }

    public static ValidationMessage<Origin>
    createValidationMessage(Severity severity, String message, Origin origin) {
        ValidationMessage<Origin> validationMessage = new ValidationMessage<>(severity, ValidationMessage.NO_KEY);
        validationMessage.setMessage(message);
        validationMessage.append(origin);
        return validationMessage;
    }

    public void write(ValidationResult validationResult) {
        for (ValidationMessage<Origin> validationMessage : validationResult.getMessages()) {
            write(validationMessage);
        }
    }

    public void write(Severity severity, String message) {
        ValidationMessage<Origin> validationMessage = createValidationMessage(severity, message);
        write(validationMessage);
    }

    public void write(ValidationMessage<Origin> validationMessage) {
        if (REPORT_SEVERITY.contains(validationMessage.getSeverity())) {
            byte[] message = format(validationMessage,  null /* targetOrigin */).getBytes(StandardCharsets.UTF_8);
            try {
                strm.write(message);
            } catch (IOException ex) {
                System.out.println(message);
            }
            /*
            try {
                strm.flush();
            } catch (IOException ex) {
            }
            */
        }
    }

    private String format(ValidationMessage<Origin> validationMessage, String targetOrigin) {
        try {
            StringWriter str = new StringWriter();
            validationMessage.writeMessage( str, targetOrigin );
            return str.toString();
        } catch (IOException e) {
            return validationMessage.getMessageKey();
        }
    }

    @Override
    public void close() {
        try {
            if (close) {
                strm.close();
            }
        } catch (IOException ex) {
        }
    }
}
