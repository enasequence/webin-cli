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
package uk.ac.ebi.ena.webin.cli.manifest.processor;

import static uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils.createFieldValue;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

public class PrimaryIdProcessorTest {
  private static ValidationResult process(String value) {
    ValidationResult result = new ValidationResult();
    new PrimaryIdProcessor()
        .process(result, createFieldValue(ManifestFieldType.META, "PRIMARY_ID", value));
    return result;
  }

  @Test
  public void testValid() {
    // Assembly
    Assert.assertTrue(process("GCA_982311725").isValid());
    Assert.assertTrue(process("GCA_982311725.1").isValid());
    // WGS
    Assert.assertTrue(process("CBIHLC010000000").isValid());
    Assert.assertTrue(process("AAAA01000000").isValid());
    // Transcriptome
    Assert.assertTrue(process("HCKQ01000001").isValid());
    // Sequence
    Assert.assertTrue(process("OZ307626").isValid());
    Assert.assertTrue(process("OZ307626.1").isValid());
    Assert.assertTrue(process("A12345").isValid());
    Assert.assertTrue(process("MN90894712").isValid());
  }

  @Test
  public void testInvalid() {
    Assert.assertFalse(process("foo").isValid());
    Assert.assertFalse(process("GCA_98231172").isValid());
    Assert.assertFalse(process("GCF_982311725").isValid());
    Assert.assertFalse(process("gca_982311725").isValid());
    Assert.assertFalse(process("OZ30762").isValid());
    Assert.assertFalse(process("ERZ123456").isValid());
    Assert.assertFalse(process("CBIHLC01").isValid());
  }
}
