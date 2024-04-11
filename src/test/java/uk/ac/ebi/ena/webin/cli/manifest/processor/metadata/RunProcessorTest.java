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
package uk.ac.ebi.ena.webin.cli.manifest.processor.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils.createFieldValue;
import static uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage.Severity;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.message.listener.MessageCounter;

public class RunProcessorTest {
  private final WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();

  @Test
  public void testCorrect() {
    RunProcessor processor =
        new RunProcessor(
            parameters,
            (fieldGroup, e) -> {
              Assert.assertEquals(1, e.size());
              Assert.assertEquals("ERR2836765", e.get(0).getRunId());
            });

    ManifestFieldValue fieldValue =
        createFieldValue(
            ManifestFieldType.META,
            "RUN_REF",
            "ERR2836765" /*"ena-RUN-UNIVERSITY OF MINNESOTA-11-10-2018-17:17:11:460-400"*/);
    ValidationResult result = new ValidationResult();
    processor.process(result, fieldValue);
    Assert.assertTrue(result.isValid());
    Assert.assertEquals("ERR2836765", fieldValue.getValue());
  }

  @Test
  public void testCorrectList() {
    RunProcessor processor =
        new RunProcessor(
            parameters,
            (fieldGroup, e) -> {
              Assert.assertEquals(3, e.size());
              Assert.assertEquals("ERR2836765", e.get(0).getRunId());
              Assert.assertEquals("ERR2836764", e.get(1).getRunId());
              Assert.assertEquals("ERR2836763", e.get(2).getRunId());
            });

    ManifestFieldValue fieldValue =
        createFieldValue(
            ManifestFieldType.META,
            "RUN_REF",
            "ERR2836765, ERR2836764, ERR2836763,ERR2836763" /*"ena-RUN-UNIVERSITY OF MINNESOTA-11-10-2018-17:17:11:460-400"*/);
    ValidationResult result = new ValidationResult();
    processor.process(result, fieldValue);
    Assert.assertTrue(result.isValid());
    Assert.assertEquals("ERR2836765, ERR2836764, ERR2836763", fieldValue.getValue());
  }

  @Test
  public void testIncorrect() {
    RunProcessor processor =
        new RunProcessor(parameters, (fieldGroup, run) -> Assert.assertNull(run));
    ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "RUN_REF", "INVALID");
    ValidationResult result = new ValidationResult();
    MessageCounter counter =
        MessageCounter.regex(Severity.ERROR, WebinCliMessage.RUN_SERVICE_VALIDATION_ERROR.regex());
    result.add(counter);
    processor.process(result, fieldValue);
    Assert.assertFalse(result.isValid());
    assertThat(result.count(Severity.ERROR)).isOne();
    assertThat(counter.getCount()).isOne();
  }

  @Test
  public void testIncorrectList() {
    RunProcessor processor =
        new RunProcessor(parameters, (fieldGroup, run) -> Assert.assertNull(run));

    ManifestFieldValue fieldValue =
        createFieldValue(ManifestFieldType.META, "RUN_REF", "INVALID1, ERR2836765, INVALID2");
    ValidationResult result = new ValidationResult();
    MessageCounter counter =
        MessageCounter.regex(Severity.ERROR, WebinCliMessage.RUN_SERVICE_VALIDATION_ERROR.regex());
    result.add(counter);
    processor.process(result, fieldValue);
    Assert.assertFalse(result.isValid());
    assertThat(result.count(Severity.ERROR)).isEqualTo(2);
    assertThat(counter.getCount()).isEqualTo(2);
  }
}
