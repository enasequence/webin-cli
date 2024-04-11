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
package uk.ac.ebi.ena.webin.cli.manifest;

import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

import java.util.ArrayList;
import java.util.Collection;

public class ManifestReaderResult {

  private final ValidationResult validationResult;

  private final Collection<ManifestFieldGroup> manifestFieldGroups = new ArrayList<>();

  public ManifestReaderResult(ValidationResult validationResult) {
    this.validationResult = validationResult;
  }

  public ValidationResult getValidationResult() {
    return validationResult;
  }

  public Collection<ManifestFieldGroup> getManifestFieldGroups() {
    return manifestFieldGroups;
  }
}
