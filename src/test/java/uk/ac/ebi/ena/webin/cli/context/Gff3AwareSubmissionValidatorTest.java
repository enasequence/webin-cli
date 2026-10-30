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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.webin.cli.TempFileBuilder;
import uk.ac.ebi.ena.webin.cli.context.annotation.Gff3Validator;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;

public class Gff3AwareSubmissionValidatorTest {

  private static GenomeManifest createManifest(File... fastaFiles) {
    GenomeManifest manifest = new GenomeManifest();
    for (File fastaFile : fastaFiles) {
      manifest.files().add(new SubmissionFile<>(GenomeManifest.FileType.FASTA, fastaFile));
    }
    return manifest;
  }

  @SuppressWarnings("unchecked")
  private static List<SubmissionFile<?>> validateAndCaptureFastaFiles(GenomeManifest manifest) {
    Gff3Validator gff3Validator = mock(Gff3Validator.class);
    SubmissionValidator submissionValidator = mock(SubmissionValidator.class);
    when(gff3Validator.validate(anyList(), anyList())).thenReturn(true);
    when(submissionValidator.validate(any())).thenReturn(new ValidationResponse());

    new Gff3AwareSubmissionValidator(gff3Validator, submissionValidator).validate(manifest);

    ArgumentCaptor<List> fastaFiles = ArgumentCaptor.forClass(List.class);
    verify(gff3Validator).validate(anyList(), fastaFiles.capture());
    verify(submissionValidator).validate(manifest);
    return fastaFiles.getValue();
  }

  @Test
  public void testPlainFastaHeaderIsNotValidatedByGff3Validator() {
    File plainFasta = TempFileBuilder.file("plain.fasta", ">AF123456.1\nACGT\n").toFile();

    assertThat(validateAndCaptureFastaFiles(createManifest(plainFasta))).isEmpty();
  }

  @Test
  public void testJsonFastaHeaderIsValidatedByGff3Validator() {
    File jsonFasta =
        TempFileBuilder.file("json.fasta", ">AF123456.1 | {\"topology\":\"linear\"}\nACGT\n")
            .toFile();

    List<SubmissionFile<?>> fastaFiles = validateAndCaptureFastaFiles(createManifest(jsonFasta));

    assertThat(fastaFiles).hasSize(1);
    assertThat(fastaFiles.get(0).getFile()).isEqualTo(jsonFasta);
  }

  @Test
  public void testOnlyJsonFastaHeaderFilesAreValidatedByGff3Validator() {
    File plainFasta = TempFileBuilder.file("plain.fasta", ">AF1\nACGT\n").toFile();
    File jsonFasta = TempFileBuilder.file("json.fasta", ">AF2 | {}\nACGT\n").toFile();

    List<SubmissionFile<?>> fastaFiles =
        validateAndCaptureFastaFiles(createManifest(plainFasta, jsonFasta));

    assertThat(fastaFiles).hasSize(1);
    assertThat(fastaFiles.get(0).getFile()).isEqualTo(jsonFasta);
  }
}
