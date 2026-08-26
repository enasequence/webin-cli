/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.context.annotation;

import java.util.List;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

/**
 * Validator for the annotation (DECOUPLED_ANNOTATION) submission context.
 *
 * <p>Delegates to the embl-api-core {@link SubmissionValidator} for standard manifest/metadata
 * checks, then runs {@link Gff3Validator} on any GFF3 files declared in the manifest. GFF3
 * validation is structural-only client-side (no FASTA cross-check) because annotation
 * submissions reference existing ENA sequences via {@code PRIMARY_ID}; full sequence-level
 * validation happens server-side in the {@code webin-gff3-stages} pipeline.
 */
public class AnnotationSubmissionValidator implements Validator<Manifest<?>, ValidationResponse> {

  private static final String GFF3_TYPE = "GFF3";

  private final SubmissionValidator submissionValidator;
  private final Gff3Validator gff3Validator;

  public AnnotationSubmissionValidator() {
    this(new SubmissionValidator(), new Gff3Validator());
  }

  AnnotationSubmissionValidator(
      SubmissionValidator submissionValidator, Gff3Validator gff3Validator) {
    this.submissionValidator = submissionValidator;
    this.gff3Validator = gff3Validator;
  }

  @Override
  public ValidationResponse validate(Manifest<?> manifest) {
    ValidationResponse response = submissionValidator.validate(manifest);
    if (response == null) {
      response = new ValidationResponse();
    }

    List<? extends SubmissionFile<?>> gff3Files = manifest.filesWithTypeName(GFF3_TYPE);
    if (!gff3Files.isEmpty()) {
      if (!gff3Validator.validate(gff3Files)) {
        response.setStatus(ValidationResponse.status.VALIDATION_ERROR);
      }
    }

    return response;
  }
}
