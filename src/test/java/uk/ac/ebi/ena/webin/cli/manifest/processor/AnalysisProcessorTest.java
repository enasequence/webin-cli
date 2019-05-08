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
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;

public class
AnalysisProcessorTest
{
    private final WebinCliParameters parameters = new WebinCliParameters();

    @Before public void
    before() 
    {
        parameters.setUsername( System.getenv( "webin-cli-username" ) );
        parameters.setPassword( System.getenv( "webin-cli-password" ) );
        parameters.setTestMode( true );
    }


    @Test public void
    testCorrect()
    {
        final String analysis_id = "ERZ690501";
        AnalysisProcessor processor = new AnalysisProcessor( parameters, 
                                                            ( e ) -> {
                                                                Assert.assertEquals( 1, e.size() );
                                                                Assert.assertEquals( analysis_id, e.get( 0 ).getAnalysisId() ); 
                                                            } );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "ANALYSIS_REF", analysis_id );
        Assert.assertTrue( processor.process( fieldValue ).isValid() );
        Assert.assertEquals( analysis_id, fieldValue.getValue() );
    }
    

    @Test public void
    testCorrectList()
    {
        AnalysisProcessor processor = new AnalysisProcessor( parameters, 
                                                             ( e ) -> {
                                                                 Assert.assertEquals( 2, e.size() );
                                                                 Assert.assertEquals( "ERZ690501", e.get( 0 ).getAnalysisId() );
                                                                 Assert.assertEquals( "ERZ690500", e.get( 1 ).getAnalysisId() );
                                                             } );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "ANALYSIS_REF", "ERZ690501, ERZ690500" );
        Assert.assertTrue( processor.process( fieldValue ).isValid() );
        Assert.assertEquals( "ERZ690501, ERZ690500", fieldValue.getValue() );
    }

    
    
    @Test public void
    testIncorrect()
    {
        AnalysisProcessor processor = new AnalysisProcessor( parameters, Assert::assertNull );

        final String analysis_id = "SOME_ANALYSIS_ID";
        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "ANALYSIS_REF", analysis_id );
        ValidationResult vr = processor.process( fieldValue );
        Assert.assertFalse( vr.isValid() );
        Assert.assertEquals( 1, vr.count( Severity.ERROR ) );
        Assert.assertTrue( vr.getMessages( Severity.ERROR ).iterator().next().getMessage().contains( analysis_id ) );
        Assert.assertEquals( analysis_id, fieldValue.getValue() );
    }
    
    
    @Test public void
    testIncorrectList()
    {
        AnalysisProcessor processor = new AnalysisProcessor( parameters, Assert::assertNull );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "ANALYSIS_REF", "SOME_ANALYSIS_ID1, ERZ690500, SOME_ANALYSIS_ID2" );
        ValidationResult vr = processor.process( fieldValue );
        vr.getMessages( Severity.ERROR ).stream().forEach( System.out::println );
        Assert.assertFalse( vr.isValid() );
        Assert.assertEquals( 2, vr.count( Severity.ERROR ) );
        Assert.assertTrue(  vr.getMessages( Severity.ERROR ).stream().anyMatch( e -> e.getMessage().contains( "SOME_ANALYSIS_ID1" ) ) );
        Assert.assertFalse( vr.getMessages( Severity.ERROR ).stream().anyMatch( e -> e.getMessage().contains( "ERZ690500" ) ) );
        Assert.assertTrue(  vr.getMessages( Severity.ERROR ).stream().anyMatch( e -> e.getMessage().contains( "SOME_ANALYSIS_ID2" ) ) );
        Assert.assertEquals( "SOME_ANALYSIS_ID1, ERZ690500, SOME_ANALYSIS_ID2", fieldValue.getValue() );
    }

}
