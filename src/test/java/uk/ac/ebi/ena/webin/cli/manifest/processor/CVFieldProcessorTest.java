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

import uk.ac.ebi.ena.webin.cli.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.message.ValidationMessage.Severity;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestCVList;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;

public class
CVFieldProcessorTest 
{

    @Test public void 
    test()
    {
        CVFieldProcessor processor = new CVFieldProcessor( new ManifestCVList( "TEST1", "test2" ) );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "TEST1" );
        ValidationResult vr = processor.process( fieldValue );
        Assert.assertTrue( vr.isValid() );
        Assert.assertEquals( "TEST1", fieldValue.getValue() );

        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "test1" );
        vr = processor.process( fieldValue );
        Assert.assertTrue( vr.isValid() );
        Assert.assertEquals( 1, vr.count( Severity.INFO ) );
        Assert.assertEquals( "TEST1", fieldValue.getValue() );

        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "te_st1" );
        vr = processor.process( fieldValue );
        Assert.assertTrue( vr.isValid() );
        Assert.assertEquals( 1, vr.count( Severity.INFO ) );
        Assert.assertEquals( "TEST1", fieldValue.getValue() );

        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "test2" );
        vr = processor.process( fieldValue );
        Assert.assertTrue( vr.isValid() );
        Assert.assertEquals( 0, vr.count( Severity.INFO ) );
        Assert.assertEquals( "test2", fieldValue.getValue() );

        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "TEST2" );
        vr = processor.process( fieldValue );
        Assert.assertTrue( vr.isValid() );
        Assert.assertEquals( 1, vr.count( Severity.INFO ) );
        Assert.assertEquals( "test2", fieldValue.getValue() );

        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "TE_ST2" );
        vr = processor.process( fieldValue );
        Assert.assertTrue( vr.isValid() );
        Assert.assertEquals( 1, vr.count( Severity.INFO ) );
        Assert.assertEquals( "test2", fieldValue.getValue() );

        fieldValue = createFieldValue( ManifestFieldType.META, "FIELD1", "TEST3" );
        vr = processor.process( fieldValue );
        Assert.assertFalse( vr.isValid() );
        Assert.assertEquals( "TEST3", fieldValue.getValue() );
    }
}
