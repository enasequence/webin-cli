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
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

public class AuthorProcessor implements ManifestFieldProcessor {

  @Override
  public void process(ValidationResult result, ManifestFieldValue fieldValue) {
    if (null != fieldValue.getValue()) {
      fieldValue.setValue(
          replaceMultiplePeriods(replaceMultipleSpaces(fieldValue.getValue().replaceAll(";", ""))));
    }
  }

  private String replaceMultipleSpaces(String string) {
    if (string == null) {
      return null;
    }
    string = string.trim();
    Pattern pattern = Pattern.compile(" {2,}");
    return pattern.matcher(string).replaceAll(" ");
  }

  private String replaceMultiplePeriods(String string) {
    if (string == null) {
      return null;
    }
    string = string.trim();
    Pattern pattern = Pattern.compile("\\.{2,}");
    return pattern.matcher(string).replaceAll(".");
  }
}
