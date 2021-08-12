/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
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

import uk.ac.ebi.ena.webin.cli.context.genome.GenomeManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

public class
AssemblyTypeProcessorTest {
    @Test
    public void
    test() {
        AssemblyTypeProcessor processor = new AssemblyTypeProcessor();

        for (String value : new String[]{"clone", "CLONE", "isolate", "ISOLATE"}) {
            ManifestFieldValue fieldValue =
                    createFieldValue(ManifestFieldType.META, GenomeManifestReader.Field.ASSEMBLY_TYPE, value);
            ValidationResult result = new ValidationResult();
            processor.process(result, fieldValue);
            Assert.assertEquals(0, result.count());
            Assert.assertEquals(GenomeManifestReader.ASSEMBLY_TYPE_PRIMARY_CLONE_OR_ISOLATE, fieldValue.getValue());
        }
        ManifestFieldValue fieldValue =
                createFieldValue(ManifestFieldType.META, GenomeManifestReader.Field.ASSEMBLY_TYPE, "test");
        ValidationResult result = new ValidationResult();
        processor.process(result, fieldValue);
        Assert.assertEquals(0, result.count());
            Assert.assertEquals("test", fieldValue.getValue());
    }
}
