/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.manifest.processor;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.manifest.ManifestCVList;
import uk.ac.ebi.ena.manifest.ManifestFieldType;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;

import static uk.ac.ebi.ena.manifest.processor.ProcessorTestUtils.createFieldValue;

public class CVFieldProcessorTest {

    @Test public void test()
    {
        CVFieldProcessor processor = new CVFieldProcessor( new ManifestCVList("TEST1", "test2") );

        ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "FIELD1", "TEST1");
        Assert.assertNull( processor.process(fieldValue) );
        Assert.assertEquals( "TEST1", fieldValue.getValue() );

        fieldValue = createFieldValue(ManifestFieldType.META, "FIELD1", "test1");
        Assert.assertTrue( processor.process(fieldValue).getSeverity().equals(Severity.INFO) );
        Assert.assertEquals( "TEST1", fieldValue.getValue() );

        fieldValue = createFieldValue(ManifestFieldType.META, "FIELD1", "te_st1");
        Assert.assertTrue( processor.process(fieldValue).getSeverity().equals(Severity.INFO) );
        Assert.assertEquals( "TEST1", fieldValue.getValue() );

        fieldValue = createFieldValue(ManifestFieldType.META, "FIELD1", "test2");
        Assert.assertNull( processor.process(fieldValue) );
        Assert.assertEquals( "test2", fieldValue.getValue() );

        fieldValue = createFieldValue(ManifestFieldType.META, "FIELD1", "TEST2");
        Assert.assertTrue( processor.process(fieldValue).getSeverity().equals(Severity.INFO) );
        Assert.assertEquals( "test2", fieldValue.getValue() );

        fieldValue = createFieldValue(ManifestFieldType.META, "FIELD1", "TE_ST2");
        Assert.assertTrue( processor.process(fieldValue).getSeverity().equals(Severity.INFO) );
        Assert.assertEquals( "test2", fieldValue.getValue() );

        fieldValue = createFieldValue(ManifestFieldType.META, "FIELD1", "TEST3");
        Assert.assertTrue( processor.process(fieldValue).getSeverity().equals(Severity.ERROR) );
        Assert.assertEquals( "TEST3", fieldValue.getValue() );
    }
}
