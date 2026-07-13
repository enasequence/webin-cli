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
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.webin.cli.context.genome.Gff3Validator;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

/**
 * Composite validator for genome, transcriptome, and sequence submissions. Runs the sequencetools
 * {@link SubmissionValidator} first, then performs client-side GFF3 validation when the manifest
 * declares a GFF3 file.
 *
 * <p>Instantiated reflectively by {@link uk.ac.ebi.ena.webin.cli.WebinCliContext}, so a public
 * no-arg constructor is required.
 */
public class SequenceSubmissionValidator implements Validator<Manifest<?>, ValidationResponse> {

  private static final String GFF3_TYPE = "GFF3";
  private static final String FASTA_TYPE = "FASTA";
  private static final String FLATFILE_TYPE = "FLATFILE";

  private final SubmissionValidator submissionValidator;
  private final Gff3Validator gff3Validator;

  public SequenceSubmissionValidator() {
    this(new SubmissionValidator(), new Gff3Validator());
  }

  SequenceSubmissionValidator(SubmissionValidator submissionValidator, Gff3Validator gff3Validator) {
    this.submissionValidator = submissionValidator;
    this.gff3Validator = gff3Validator;
  }

  @Override
  public ValidationResponse validate(Manifest<?> manifest) {
    List<? extends SubmissionFile<?>> gff3Files = manifest.filesWithTypeName(GFF3_TYPE);
    List<? extends SubmissionFile<?>> fastaFiles = manifest.filesWithTypeName(FASTA_TYPE);
    boolean gff3Only =
        !gff3Files.isEmpty()
            && fastaFiles.isEmpty()
            && manifest.filesWithTypeName(FLATFILE_TYPE).isEmpty();

    ValidationResponse response;
    if (gff3Only) {
      // sequencetools' SubmissionValidator requires at least one FASTA/FLATFILE sequence
      // to compute contig/scaffold/chromosome counts, which a GFF3-only submission never
      // has. Skip it entirely and rely on Gff3Validator below instead.
      response = new ValidationResponse(ValidationResponse.status.VALIDATION_SUCCESS);
    } else {
      response = submissionValidator.validate(manifest);
      if (response == null) {
        response = new ValidationResponse();
      }
    }

    if (!gff3Files.isEmpty() && !gff3Validator.validate(gff3Files, fastaFiles)) {
      response.setStatus(ValidationResponse.status.VALIDATION_ERROR);
    }

    return response;
  }
}
