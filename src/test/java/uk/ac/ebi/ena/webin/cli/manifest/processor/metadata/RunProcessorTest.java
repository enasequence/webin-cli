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
package uk.ac.ebi.ena.webin.cli.manifest.processor.metadata;

import static uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils.createFieldValue;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;

public class
RunProcessorTest
{
    private final WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();

    @Test public void
    testCorrect()
    {
        RunProcessor processor = new RunProcessor( parameters,
                                                   ( e ) -> {
                                                           Assert.assertEquals( 1, e.size() );
                                                           Assert.assertEquals( "ERR2836765", e.get( 0 ).getRunId() ); 
                                                       } );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "RUN_REF", "ERR2836765" /*"ena-RUN-UNIVERSITY OF MINNESOTA-11-10-2018-17:17:11:460-400"*/ );
        Assert.assertTrue( processor.process( fieldValue ).isValid() );
        Assert.assertEquals( "ERR2836765", fieldValue.getValue() );
    }
    

    @Test public void
    testCorrectList()
    {
        RunProcessor processor = new RunProcessor( parameters, 
                                                   ( e ) -> {
                                                           Assert.assertEquals( 3, e.size() );
                                                           Assert.assertEquals( "ERR2836765", e.get( 0 ).getRunId() );
                                                           Assert.assertEquals( "ERR2836764", e.get( 1 ).getRunId() );
                                                           Assert.assertEquals( "ERR2836763", e.get( 2 ).getRunId() ); 
                                                       } );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "RUN_REF", "ERR2836765, ERR2836764, ERR2836763,ERR2836763" /*"ena-RUN-UNIVERSITY OF MINNESOTA-11-10-2018-17:17:11:460-400"*/ );
        Assert.assertTrue( processor.process( fieldValue ).isValid() );
        Assert.assertEquals( "ERR2836765, ERR2836764, ERR2836763", fieldValue.getValue() );
    }

    
    
    @Test public void
    testIncorrect()
    {
        RunProcessor processor = new RunProcessor( parameters, Assert::assertNull );

        final String run_id = "SOME_RUN_ID";
        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "RUN_REF", run_id );
        ValidationResult vr = processor.process( fieldValue );
        Assert.assertFalse( vr.isValid() );
        Assert.assertEquals( 1, vr.count( Severity.ERROR ) );
        Assert.assertTrue( vr.getMessages( Severity.ERROR ).iterator().next().getMessage().contains( run_id ) );
        Assert.assertEquals( run_id, fieldValue.getValue() );
    }
    
    
    @Test public void
    testIncorrectList()
    {
        RunProcessor processor = new RunProcessor( parameters, Assert::assertNull );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "RUN_REF", "SOME_RUN_ID1, ERR2836765, SOME_RUN_ID2" );
        ValidationResult vr = processor.process( fieldValue );
        vr.getMessages( Severity.ERROR ).stream().forEach( System.out::println );
        Assert.assertFalse( vr.isValid() );
        Assert.assertEquals( 2, vr.count( Severity.ERROR ) );
        Assert.assertTrue(  vr.getMessages( Severity.ERROR ).stream().anyMatch( e -> e.getMessage().contains( "SOME_RUN_ID1" ) ) );
        Assert.assertFalse( vr.getMessages( Severity.ERROR ).stream().anyMatch( e -> e.getMessage().contains( "ERR2836765" ) ) );
        Assert.assertTrue(  vr.getMessages( Severity.ERROR ).stream().anyMatch( e -> e.getMessage().contains( "SOME_RUN_ID2" ) ) );
        Assert.assertEquals( "SOME_RUN_ID1, ERR2836765, SOME_RUN_ID2", fieldValue.getValue() );
    }

}
