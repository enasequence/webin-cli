/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
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
        USER_ERROR,
        SYSTEM_ERROR,
        VALIDATION_ERROR
    }

    private final ErrorType errorType;

    private WebinCliException(ErrorType errorType, String ... messages) {
        super(trim(messages));
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void throwAddMessage(String userErrorMessage, String systemErrorMessage) {
        switch (getErrorType()) {
            case SYSTEM_ERROR:
                throw new WebinCliException(getErrorType(), trim(systemErrorMessage, getMessage()));
            default:
                throw new WebinCliException(getErrorType(), trim(userErrorMessage, getMessage()));
        }
    }

    public static WebinCliException createUserError(String ... messages) {
        return new WebinCliException(ErrorType.USER_ERROR, messages);
    }

    public static WebinCliException createSystemError(String ... messages) {
        return new WebinCliException(ErrorType.SYSTEM_ERROR, messages);
    }

    public static WebinCliException createValidationError(String ... messages) {
        return new WebinCliException(ErrorType.VALIDATION_ERROR, messages);
    }

    private static String trim(String ... messages) {
        String msg = "";
        for (String message : messages) {
            if (message != null) {
                msg += " " + message;
            }
        }
        return msg.trim().replaceAll(" +", " ");
    }
}
