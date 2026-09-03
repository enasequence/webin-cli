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
package uk.ac.ebi.ena.webin.cli.context.annotation;

import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

/**
 * Validator for the annotation (DECOUPLED_ANNOTATION) submission context.
 *
 * <p>Runs {@link Gff3Validator} on any GFF3 files declared in the manifest. GFF3 validation is
 * structural-only client-side (no FASTA cross-check) because annotation submissions reference
 * existing ENA sequences via {@code PRIMARY_ID}; full sequence-level validation happens
 * server-side in the {@code webin-gff3-stages} pipeline.
 *
 * <p>Standard manifest/metadata validation is not delegated to the sequencetools {@code
 * SubmissionValidator} because it does not support {@code AnnotationManifest}: it recognises only
 * genome/transcriptome/sequence/polysample manifests and fails with a {@link ClassCastException}
 * for any other type.
 */
public class AnnotationSubmissionValidator implements Validator<Manifest<?>, ValidationResponse> {

  private static final String GFF3_TYPE = "GFF3";

  private final Gff3Validator gff3Validator;

  public AnnotationSubmissionValidator() {
    this(new Gff3Validator());
  }

  AnnotationSubmissionValidator(Gff3Validator gff3Validator) {
    this.gff3Validator = gff3Validator;
  }

  @Override
  public ValidationResponse validate(Manifest<?> manifest) {

    ValidationResponse validationResponse = new ValidationResponse();
    if (gff3Validator.validate(manifest.filesWithTypeName(GFF3_TYPE))) {
      validationResponse.setStatus(ValidationResponse.status.VALIDATION_SUCCESS);
    } else {
      validationResponse.setStatus(ValidationResponse.status.VALIDATION_ERROR);
    }

    return validationResponse;
  }
}
