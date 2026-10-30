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

import java.util.List;
import java.util.stream.Collectors;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.webin.cli.context.annotation.Gff3Validator;
import uk.ac.ebi.ena.webin.cli.utils.FastaUtils;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.AnnotationManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

/**
 * Runs any GFF3 files, and any FASTA files with JSON headers ({@code >ID | {...}}), declared in the
 * manifest through {@link Gff3Validator} (gff3tools) before delegating standard manifest/metadata
 * validation to the sequencetools {@link SubmissionValidator}. GFF3 validation failure
 * short-circuits — {@code SubmissionValidator} does not run.
 *
 * <p>FASTA files without JSON headers are not passed to {@link Gff3Validator}; they are validated
 * by {@code SubmissionValidator} only.
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
    // Gff3 files and FASTA with json headers validated here.
    if (!gff3Validator.validateGFF3File(
        manifest.filesWithTypeName(GFF3_TYPE),
        getJsonHeaderFastaFiles(manifest.filesWithTypeName(FASTA_TYPE)))) {
      response.setStatus(ValidationResponse.status.VALIDATION_ERROR);
      return response;
    }

    if (manifest instanceof AnnotationManifest) {
      response.setStatus(ValidationResponse.status.VALIDATION_SUCCESS);
      return response;
    }

    // All other formates validated here.
    return submissionValidator.validate(manifest);
  }

  private static List<? extends SubmissionFile<?>> getJsonHeaderFastaFiles(
      List<? extends SubmissionFile<?>> fastaFiles) {
    return fastaFiles.stream()
        .filter(fastaFile -> FastaUtils.hasJsonFastaHeader(fastaFile.getFile()))
        .collect(Collectors.toList());
  }
}
