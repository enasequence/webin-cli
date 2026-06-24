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
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

/**
 * Single validator for Genome, Transcriptome, Sequence and Polysample submissions. Delegates to
 * the {@code sequencetools} {@link SubmissionValidator} and, when the manifest declares a GFF3
 * file (Genome or Transcriptome only), also runs client-side GFF3 validation via {@link
 * Gff3Validator}.
 *
 * <p>This class is instantiated reflectively by {@code WebinCliContext} so it must keep a public
 * no-arg constructor.
 */
public class SequenceSubmissionValidator implements Validator<Manifest, ValidationResponse> {

  private final SubmissionValidator submissionValidator = new SubmissionValidator();
  private final Gff3Validator gff3Validator = new Gff3Validator();

  @Override
  public ValidationResponse validate(Manifest manifest) {
    ValidationResponse response = submissionValidator.validate(manifest);
    if (response == null) {
      response = new ValidationResponse();
    }

    if (manifest instanceof GenomeManifest) {
      GenomeManifest genomeManifest = (GenomeManifest) manifest;
      List<SubmissionFile<GenomeManifest.FileType>> gff3Files =
          genomeManifest.files(GenomeManifest.FileType.GFF3);
      if (gff3Files != null && !gff3Files.isEmpty()) {
        List<SubmissionFile<GenomeManifest.FileType>> fastaFiles =
            genomeManifest.files(GenomeManifest.FileType.FASTA);
        if (!gff3Validator.validate(gff3Files, fastaFiles)) {
          response.setStatus(ValidationResponse.status.VALIDATION_ERROR);
        }
      }
    } else if (manifest instanceof TranscriptomeManifest) {
      TranscriptomeManifest transcriptomeManifest = (TranscriptomeManifest) manifest;
      List<SubmissionFile<TranscriptomeManifest.FileType>> gff3Files =
          transcriptomeManifest.files(TranscriptomeManifest.FileType.GFF3);
      if (gff3Files != null && !gff3Files.isEmpty()) {
        List<SubmissionFile<TranscriptomeManifest.FileType>> fastaFiles =
            transcriptomeManifest.files(TranscriptomeManifest.FileType.FASTA);
        if (!gff3Validator.validate(gff3Files, fastaFiles)) {
          response.setStatus(ValidationResponse.status.VALIDATION_ERROR);
        }
      }
    }

    return response;
  }
}
