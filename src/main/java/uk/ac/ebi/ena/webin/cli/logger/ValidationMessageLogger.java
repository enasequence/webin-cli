
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
package uk.ac.ebi.ena.webin.cli.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;

public final class ValidationMessageLogger {

    private static final Logger log = LoggerFactory.getLogger(ValidationMessageLogger.class);

    private ValidationMessageLogger() {
    }

    public static void log(ValidationResult validationResult) {
        validationResult.getMessages().forEach(ValidationMessageLogger::log);
    }

    public static void log(ValidationMessage<Origin> validationMessage) {
        if (validationMessage.getSeverity() == Severity.ERROR) {
            log.error(validationMessage.getMessage());
        }
        if (validationMessage.getSeverity() == Severity.INFO) {
            log.info(validationMessage.getMessage());
        }
    }
}
