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
package uk.ac.ebi.ena.webin.cli.context;

import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.webin.cli.context.annotation.Gff3Validator;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.manifest.AnnotationManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

/**
 * Runs any GFF3 or FASTA files declared in the manifest through {@link Gff3Validator} (gff3tools)
 * before delegating standard manifest/metadata validation to the sequencetools {@link
 * SubmissionValidator}. GFF3 validation failure short-circuits — {@code SubmissionValidator} does
 * not run.
 *
 * <p>{@link AnnotationManifest} is a special case: {@code SubmissionValidator} does not support it
 * (it recognises only genome/transcriptome/sequence/polysample manifests and throws {@link
 * ClassCastException} for any other type), so GFF3 validation is the entirety of annotation context
 * validation.
 *
 * <p>File lookup goes through {@link Manifest#filesWithTypeName(String)} rather than a concrete
 * {@code FileType} enum, so one instance works across manifest types without a cast.
 */
public class Gff3AwareSubmissionValidator implements Validator<Manifest<?>, ValidationResponse> {

  private static final String GFF3_TYPE = "GFF3";
  private static final String FASTA_TYPE = "FASTA";

  private final Gff3Validator gff3Validator;
  private final SubmissionValidator submissionValidator;

  public Gff3AwareSubmissionValidator() {
    this(new Gff3Validator(), new SubmissionValidator());
  }

  Gff3AwareSubmissionValidator(
      Gff3Validator gff3Validator, SubmissionValidator submissionValidator) {
    this.gff3Validator = gff3Validator;
    this.submissionValidator = submissionValidator;
  }

  @Override
  public ValidationResponse validate(Manifest<?> manifest) {
    ValidationResponse response = new ValidationResponse();
    if (!gff3Validator.validate(
        manifest.filesWithTypeName(GFF3_TYPE), manifest.filesWithTypeName(FASTA_TYPE))) {
      response.setStatus(ValidationResponse.status.VALIDATION_ERROR);
      return response;
    }

    if (manifest instanceof AnnotationManifest) {
      response.setStatus(ValidationResponse.status.VALIDATION_SUCCESS);
      return response;
    }

    return submissionValidator.validate(manifest);
  }
}
