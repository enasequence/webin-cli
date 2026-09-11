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

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.getResourceDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.AnnotationManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.AnnotationManifest.FileType;

/**
 * Unit tests for {@link Gff3Validator} in the annotation context.
 *
 * <p>Unlike the genome Gff3Validator tests, these do not supply a FASTA file — annotation
 * submissions reference existing ENA sequences via PRIMARY_ID, so client-side GFF3 validation is
 * structural only. Sequence-level cross-validation happens server-side in webin-gff3-stages.
 */
public class Gff3ValidatorTest {

  private static final File RESOURCE_DIR = getResourceDir("uk/ac/ebi/ena/webin/cli/annotation");

  private AnnotationManifest manifestWith(String gff3Name) throws Exception {
    AnnotationManifest manifest = new AnnotationManifest();

    SubmissionFile<FileType> gff3 =
        new SubmissionFile<>(FileType.GFF3, new File(RESOURCE_DIR, gff3Name));

    File reportFile = File.createTempFile("gff3-", ".report");
    reportFile.deleteOnExit();
    gff3.setReportFile(reportFile);

    manifest.files().add(gff3);
    return manifest;
  }

  private String reportContent(AnnotationManifest manifest) throws Exception {
    File reportFile = manifest.files(FileType.GFF3).get(0).getReportFile();
    return new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8);
  }

  @Test
  public void testValidGff3() throws Exception {
    AnnotationManifest manifest = manifestWith("valid.gff3.gz");
    List<SubmissionFile<FileType>> gff3Files = manifest.files(FileType.GFF3);

    boolean valid = new Gff3Validator().validate(gff3Files);

    assertThat(valid).isTrue();
    assertThat(reportContent(manifest)).doesNotContain("ERROR:");
  }

  @Test
  public void testInvalidGff3() throws Exception {
    AnnotationManifest manifest = manifestWith("invalid.gff3.gz");
    List<SubmissionFile<FileType>> gff3Files = manifest.files(FileType.GFF3);

    boolean valid = new Gff3Validator().validate(gff3Files);

    assertThat(valid).isFalse();
    assertThat(reportContent(manifest)).contains("ERROR:");
  }

  @Test
  public void testNoGff3Files() {
    AnnotationManifest manifest = new AnnotationManifest();
    assertThat(new Gff3Validator().validate(manifest.files(FileType.GFF3))).isTrue();
  }
}
