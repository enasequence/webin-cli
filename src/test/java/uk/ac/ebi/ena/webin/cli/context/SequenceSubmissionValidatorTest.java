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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;
import org.junit.Test;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.webin.cli.context.genome.Gff3Validator;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest.FileType;

public class SequenceSubmissionValidatorTest {

  private final SubmissionValidator submissionValidator = mock(SubmissionValidator.class);
  private final Gff3Validator gff3Validator = mock(Gff3Validator.class);
  private final SequenceSubmissionValidator validator =
      new SequenceSubmissionValidator(submissionValidator, gff3Validator);

  @Test
  public void noGff3Files_gff3ValidatorNotCalled_returnsSubmissionResponse() {
    GenomeManifest manifest = new GenomeManifest();
    ValidationResponse expected =
        new ValidationResponse(ValidationResponse.status.VALIDATION_SUCCESS);
    when(submissionValidator.validate(any())).thenReturn(expected);

    ValidationResponse result = validator.validate(manifest);

    assertThat(result).isSameAs(expected);
    verify(gff3Validator, never()).validate(anyList(), anyList());
  }

  @Test
  public void gff3FilesPresent_gff3ValidationPasses_responseUnchanged() {
    GenomeManifest manifest = manifestWithFastaAndGff3();
    ValidationResponse expected =
        new ValidationResponse(ValidationResponse.status.VALIDATION_SUCCESS);
    when(submissionValidator.validate(any())).thenReturn(expected);
    when(gff3Validator.validate(anyList(), anyList())).thenReturn(true);

    ValidationResponse result = validator.validate(manifest);

    assertThat(result.getStatus()).isEqualTo(ValidationResponse.status.VALIDATION_SUCCESS);
  }

  @Test
  public void gff3FilesPresent_gff3ValidationFails_statusSetToValidationError() {
    GenomeManifest manifest = manifestWithFastaAndGff3();
    when(submissionValidator.validate(any()))
        .thenReturn(new ValidationResponse(ValidationResponse.status.VALIDATION_SUCCESS));
    when(gff3Validator.validate(anyList(), anyList())).thenReturn(false);

    ValidationResponse result = validator.validate(manifest);

    assertThat(result.getStatus()).isEqualTo(ValidationResponse.status.VALIDATION_ERROR);
  }

  @Test
  public void submissionValidatorReturnsNull_responseNotNull() {
    GenomeManifest manifest = new GenomeManifest();
    when(submissionValidator.validate(any())).thenReturn(null);

    ValidationResponse result = validator.validate(manifest);

    assertThat(result).isNotNull();
  }

  @Test
  public void gff3Only_submissionValidatorSkipped_gff3ValidatorCalledWithEmptyFastaList() {
    GenomeManifest manifest = manifestWithGff3Only();
    when(gff3Validator.validate(anyList(), eq(Collections.emptyList()))).thenReturn(true);

    ValidationResponse result = validator.validate(manifest);

    assertThat(result.getStatus()).isEqualTo(ValidationResponse.status.VALIDATION_SUCCESS);
    verify(submissionValidator, never()).validate(any());
  }

  @Test
  public void gff3Only_gff3ValidationFails_statusSetToValidationError() {
    GenomeManifest manifest = manifestWithGff3Only();
    when(gff3Validator.validate(anyList(), anyList())).thenReturn(false);

    ValidationResponse result = validator.validate(manifest);

    assertThat(result.getStatus()).isEqualTo(ValidationResponse.status.VALIDATION_ERROR);
    verify(submissionValidator, never()).validate(any());
  }

  private GenomeManifest manifestWithFastaAndGff3() {
    GenomeManifest manifest = new GenomeManifest();
    manifest.files().add(new SubmissionFile<>(FileType.FASTA, new File("dummy.fasta")));
    manifest.files().add(new SubmissionFile<>(FileType.GFF3, new File("dummy.gff3")));
    return manifest;
  }

  private GenomeManifest manifestWithGff3Only() {
    GenomeManifest manifest = new GenomeManifest();
    manifest.files().add(new SubmissionFile<>(FileType.GFF3, new File("dummy.gff3")));
    return manifest;
  }
}
