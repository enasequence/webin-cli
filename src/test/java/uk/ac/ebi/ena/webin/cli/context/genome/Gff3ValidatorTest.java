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
package uk.ac.ebi.ena.webin.cli.context.genome;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest.FileType;

/**
 * Unit tests for {@link Gff3Validator} that drive the gff3tools integration directly, without going
 * through the executor (and therefore without the rate-limit / ignore-errors REST calls).
 */
public class Gff3ValidatorTest {

  private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/genome");

  private List<SubmissionFile<FileType>> fastaFiles(String fastaName) {
    return List.of(new SubmissionFile<>(FileType.FASTA, new File(RESOURCE_DIR, fastaName)));
  }

  private SubmissionFile<FileType> gff3File(String gff3Name) throws Exception {
    SubmissionFile<FileType> gff3 =
        new SubmissionFile<>(FileType.GFF3, new File(RESOURCE_DIR, gff3Name));
    File reportFile = File.createTempFile("gff3-", ".report");
    reportFile.deleteOnExit();
    gff3.setReportFile(reportFile);
    return gff3;
  }

  private String reportContent(SubmissionFile<FileType> gff3) throws Exception {
    return new String(Files.readAllBytes(gff3.getReportFile().toPath()), StandardCharsets.UTF_8);
  }

  @Test
  public void testValidGff3() throws Exception {
    SubmissionFile<FileType> gff3 = gff3File("valid-annotation.gff3.gz");
    boolean valid = new Gff3Validator().validate(List.of(gff3), fastaFiles("valid.fasta.gz"));
    assertThat(valid).isTrue();
    assertThat(reportContent(gff3)).doesNotContain("ERROR:");
  }

  @Test
  public void testInvalidGff3() throws Exception {
    SubmissionFile<FileType> gff3 = gff3File("invalid.gff3.gz");
    boolean valid = new Gff3Validator().validate(List.of(gff3), fastaFiles("valid.fasta.gz"));
    assertThat(valid).isFalse();
    assertThat(reportContent(gff3)).contains("ERROR:");
  }

  @Test
  public void testCrossValidationFailure() throws Exception {
    SubmissionFile<FileType> gff3 = gff3File("inconsistent.gff3.gz");
    boolean valid = new Gff3Validator().validate(List.of(gff3), fastaFiles("valid.fasta.gz"));
    assertThat(valid).isFalse();
    assertThat(reportContent(gff3)).contains("ERROR:");
  }

  @Test
  public void testNoGff3Files() {
    assertThat(new Gff3Validator().validate(List.of(), List.of())).isTrue();
  }
}
