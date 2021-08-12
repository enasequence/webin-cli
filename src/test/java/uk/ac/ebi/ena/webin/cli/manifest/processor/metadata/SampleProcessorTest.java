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
package uk.ac.ebi.ena.webin.cli.manifest.processor.metadata;

import static uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils.createFieldValue;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;

public class
SampleProcessorTest
{
    private final WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();

    @Test public void 
    testCorrect()
    {
        SampleProcessor processor = new SampleProcessor( parameters, (Sample sample) -> Assert.assertEquals( "SAMEA749881", sample.getBioSampleId() ) );
        
        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "SAMPLE", "ERS000002" );
        ValidationResult result = new ValidationResult();
        processor.process( result, fieldValue );
        Assert.assertTrue(result.isValid() );
        Assert.assertEquals( "SAMEA749881", fieldValue.getValue() );
    }

    
    @Test public void 
    testIncorrect()
    {
        SampleProcessor processor = new SampleProcessor( parameters, Assert::assertNull );
        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "SAMPLE", "SRP000392" );
        ValidationResult result = new ValidationResult();
        processor.process( result, fieldValue );
        Assert.assertFalse(result.isValid() );
        Assert.assertEquals( "SRP000392", fieldValue.getValue() );
    }
}
