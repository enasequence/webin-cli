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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils.createFieldValue;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.reference.Attribute;

public class SampleXmlProcessorTest {

  private final WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();

  private static final String STRAIN_NAME = "SK1";

  @Test
  public void testCorrect() {
    SampleXmlProcessor processor =
        new SampleXmlProcessor(
            parameters,
            (fieldGroup, sample) -> {
              Assert.assertNotNull(sample);

              boolean assertStrain = false;
              for (Attribute attribute : sample.getAttributes()) {
                if (attribute.getName().equals("strain")
                    && attribute.getValue().equals(STRAIN_NAME)) {
                  assertStrain = true;
                }
              }
              assertThat(assertStrain);
            });

    ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "SAMPLE", "ERS000002");
    ValidationResult result = new ValidationResult();
    processor.process(result, null, fieldValue);
    Assert.assertTrue(result.isValid());
  }

  @Test
  public void testIncorrect() {
    SampleXmlProcessor processor = new SampleXmlProcessor(parameters, (fieldGroup, sample) -> Assert.assertNull(sample));
    ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "SAMPLE", "SRP000392");
    ValidationResult result = new ValidationResult();
    processor.process(result, null, fieldValue);
    Assert.assertFalse(result.isValid());
    Assert.assertEquals("SRP000392", fieldValue.getValue());
  }
}
