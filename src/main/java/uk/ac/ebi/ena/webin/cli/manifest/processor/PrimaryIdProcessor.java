/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.manifest.processor;

import java.util.regex.Pattern;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

/** Validates that a primary id is an assembly, WGS/TSA set or sequence accession. */
public class PrimaryIdProcessor implements ManifestFieldProcessor {
  // Assembly: must be versioned, e.g. GCA_945867215.1.
  static final Pattern ASSEMBLY_PATTERN = Pattern.compile("^GCA_\\d{9}\\.\\d+$");

  // WGS (CBIHLC010000000) and TSA (HCKQ01000001) share the same shape: a 4 or 6 letter prefix,
  // a 2 digit version and a 6+ digit sequence number.
  static final Pattern WGS_TSA_PATTERN = Pattern.compile("^([A-Z]{4}\\d{8,9}|[A-Z]{6}\\d{9,10})$");

  // Standard sequence: 1 letter + 5 digits, 2 letters + 6 digits (OZ307626) or 2 letters + 8
  // digits, optionally versioned.
  static final Pattern SEQUENCE_PATTERN =
      Pattern.compile("^([A-Z]\\d{5}|[A-Z]{2}\\d{6}|[A-Z]{2}\\d{8})(\\.\\d+)?$");

  @Override
  public void process(ValidationResult result, ManifestFieldValue fieldValue) {
    String value = fieldValue.getValue();
    if (!ASSEMBLY_PATTERN.matcher(value).matches()
        && !WGS_TSA_PATTERN.matcher(value).matches()
        && !SEQUENCE_PATTERN.matcher(value).matches()) {
      result.add(
          ValidationMessage.error(
              WebinCliMessage.PRIMARY_ID_PROCESSOR_ERROR, fieldValue.getName(), value));
    }
  }
}
