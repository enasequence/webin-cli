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
package uk.ac.ebi.ena.webin.cli;

public class
WebinCliException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public enum ErrorType {
        USER_ERROR("user error"),
        SYSTEM_ERROR("system error"),
        VALIDATION_ERROR("validation error");

        public final String text;
        ErrorType(String text) {
            this.text = text;
        }
    }

    private final ErrorType errorType;

    private WebinCliException(ErrorType errorType, String message, String ... messages) {
        super(join(message, messages));
        this.errorType = errorType;
    }

    private WebinCliException(WebinCliException ex, String message, String ... messages) {
        super(join(ex.getMessage(), join(message, messages)), ex);
        this.errorType = ex.errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public static WebinCliException userError(String message, String ... messages) {
        return new WebinCliException(ErrorType.USER_ERROR, message, messages);
    }

    public static WebinCliException systemError(String message, String ... messages) {
        return new WebinCliException(ErrorType.SYSTEM_ERROR, message, messages);
    }

    public static WebinCliException validationError(String message, String ... messages) {
        return new WebinCliException(ErrorType.VALIDATION_ERROR, message, messages);
    }

    public static WebinCliException error(WebinCliException ex, String message, String ... messages) {
        return new WebinCliException(ex, message, messages);
    }

    private static String join(String message, String ... messages) {
        String str = message;
        for (String msg : messages) {
            str += " " + msg;
        }
        return str.trim().replaceAll(" +", " ");
    }
}
