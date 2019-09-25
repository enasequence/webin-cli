/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
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
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFileSuffix;
import uk.ac.ebi.ena.webin.cli.message.ValidationResult;

public class 
FileSuffixProcessorTest 
{
    @Test public void 
    test() 
    {
        ValidationResult result = new ValidationResult();

        FileSuffixProcessor processor = new FileSuffixProcessor( ManifestFileSuffix.BAM_FILE_SUFFIX );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "a.bam" );
        processor.process( result, fieldValue );
        Assert.assertTrue( result.isValid() );
        Assert.assertEquals( "a.bam", fieldValue.getValue() );

        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "a.cram" );
        processor.process( result, fieldValue );
        Assert.assertFalse( result.isValid() );
        Assert.assertEquals( "a.cram", fieldValue.getValue() );
    }
}
