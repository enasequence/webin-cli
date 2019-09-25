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
package uk.ac.ebi.ena.webin.cli.message;

import uk.ac.ebi.ena.webin.cli.message.source.MessageSource;

import java.text.MessageFormat;
import java.util.*;

public class ValidationMessage {

    public enum Severity {
        ERROR,
        INFO
    }

    private final Severity severity;
    private final String message;
    private LinkedList<ValidationOrigin> origin = new LinkedList<>();
    private final Exception ex;

    public ValidationMessage(Severity severity, MessageSource message, Object... arguments) {
        this.severity = severity;
        this.message = message.format(arguments);
        this.ex = null;
    }

    public ValidationMessage(Severity severity, String message) {
        this.severity = severity;
        this.message = message;
        this.ex = null;
    }

    public ValidationMessage(Severity severity, Exception ex) {
        this.severity = severity;
        this.message = ex.getMessage();
        this.ex = ex;
    }

    public ValidationMessage appendOrigin(ValidationOrigin ... origin) {
        this.origin.addAll(Arrays.asList(origin));
        return this;
    }

    public ValidationMessage appendOrigin(List<ValidationOrigin> origin) {
        this.origin.addAll(origin);
        return this;
    }

    public ValidationMessage prependOrigin(ValidationOrigin ... origin) {
        for (int i = origin.length - 1; i >= 0; i--) {
            this.origin.addFirst(origin[i]);
        }
        return this;
    }

    public ValidationMessage prependOrigin(List<ValidationOrigin> origin) {
        for (int i = origin.size() - 1; i >= 0; i--) {
            this.origin.addFirst(origin.get(i));
        }
        return this;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public List<ValidationOrigin> getOrigin() {
        return origin;
    }

    public Exception getException() {
        return ex;
    }

    public static ValidationMessage error(MessageSource message, Object... arguments) {
        return new ValidationMessage(Severity.ERROR, message, arguments);
    }

    public static ValidationMessage error(String message, Object... arguments) {
        return new ValidationMessage(Severity.ERROR, MessageFormat.format(message, arguments));
    }

    public static ValidationMessage error(String message) {
        return new ValidationMessage(Severity.ERROR, message);
    }

    public static ValidationMessage error(Exception ex) {
        return new ValidationMessage(Severity.ERROR, ex);
    }

    public static ValidationMessage info(MessageSource message, Object... arguments) {
        return new ValidationMessage(Severity.INFO, message, arguments);
    }

    public static ValidationMessage info(String message, Object... arguments) {
        return new ValidationMessage(Severity.INFO, MessageFormat.format(message, arguments));
    }

    public static ValidationMessage info(String message) {
        return new ValidationMessage(Severity.INFO, message);
    }
}
