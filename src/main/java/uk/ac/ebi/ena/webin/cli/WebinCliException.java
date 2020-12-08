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

    private WebinCliException(ErrorType errorType, Exception ex, String ... messages) {
        super(join(messages), ex);
        this.errorType = errorType;
    }

    private WebinCliException(ErrorType errorType, String ... messages) {
        super(join(messages));
        this.errorType = errorType;
    }

    private WebinCliException(WebinCliException ex, String ... messages) {
        super(join(ex.getMessage(), join(messages)), ex);
        this.errorType = ex.errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public static WebinCliException userError(Exception ex) {
        return userError(ex, ex.getMessage());
    }

    public static WebinCliException systemError(Exception ex) {
        return systemError(ex, ex.getMessage());
    }

    public static WebinCliException validationError(Exception ex) {
        return validationError(ex, ex.getMessage());
    }

    public static WebinCliException userError(Exception ex, String ... messages) {
        return new WebinCliException(ErrorType.USER_ERROR, ex, messages);
    }

    public static WebinCliException systemError(Exception ex, String ... messages) {
        return new WebinCliException(ErrorType.SYSTEM_ERROR, ex, messages);
    }

    public static WebinCliException validationError(Exception ex, String ... messages) {
        return new WebinCliException(ErrorType.VALIDATION_ERROR, ex, messages);
    }

    public static WebinCliException userError(String ... messages) {
        return new WebinCliException(ErrorType.USER_ERROR, messages);
    }

    public static WebinCliException systemError(String ... messages) {
        return new WebinCliException(ErrorType.SYSTEM_ERROR, messages);
    }

    public static WebinCliException validationError(String ... messages) {
        return new WebinCliException(ErrorType.VALIDATION_ERROR,messages);
    }

    public static WebinCliException error(WebinCliException ex, String ... messages) {
        return new WebinCliException(ex, messages);
    }

    private static String join(String ... messages) {
        String str = "";
        for (String msg : messages) {
            str += " " + msg;
        }
        return str.trim().replaceAll(" +", " ");
    }
}
