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
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationReport;

public class 
ASCIIFileNameProcessorTest
{
    @Test public void 
    test() 
    {
        ASCIIFileNameProcessor processor = new ASCIIFileNameProcessor();

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "a.bam" );
        ValidationReport report = new ValidationReport();
        processor.process( report, fieldValue );
        Assert.assertEquals( 0, report.count() );
        Assert.assertEquals( "a.bam", fieldValue.getValue() );

        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "/a/b/c.bam" );
        report = new ValidationReport();
        processor.process( report, fieldValue );
        Assert.assertEquals( 0, report.count() );
        
        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "a:\\B\\c.bam" );
        report = new ValidationReport();
        processor.process( report, fieldValue );
        Assert.assertEquals( 0, report.count() );
        
        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "a|b.cram" );
        report = new ValidationReport();
        processor.process( report, fieldValue );
        Assert.assertEquals( false, report.isValid() );

        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "a&b.cram" );
        report = new ValidationReport();
        processor.process( report, fieldValue );
        Assert.assertEquals( false, report.isValid() );
        Assert.assertEquals( "a&b.cram", fieldValue.getValue() );
    }
}
