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

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils.createFieldValue;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage.Severity;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationReport;
import uk.ac.ebi.ena.webin.cli.validator.message.listener.MessageCounter;

public class
AnalysisProcessorTest
{
    private final WebinCliParameters parameters = WebinCliTestUtils.getTestWebinCliParameters();

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
        ValidationReport report = new ValidationReport();
        processor.process( report, fieldValue );
        Assert.assertTrue( report.isValid() );
        Assert.assertEquals( analysis_id, fieldValue.getValue() );
    }
    

    @Test public void
    testCorrectList()
    {
        AnalysisProcessor processor = new AnalysisProcessor( parameters, 
                                                             ( e ) -> {
                                                                 Assert.assertEquals( 3, e.size() );
                                                                 Assert.assertEquals( "ERZ690501", e.get( 0 ).getAnalysisId() );
                                                                 Assert.assertEquals( "ERZ690500", e.get( 1 ).getAnalysisId() );
                                                                 Assert.assertEquals( "ERZ690502", e.get( 2 ).getAnalysisId() );
                                                             } );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "ANALYSIS_REF", "ERZ690501, ERZ690500, ERZ690500, ERZ690502" );
        ValidationReport report = new ValidationReport();
        processor.process( report, fieldValue );
        Assert.assertTrue( report.isValid() );
        Assert.assertEquals( "ERZ690501, ERZ690500, ERZ690502", fieldValue.getValue() );
    }

    @Test public void
    testIncorrect()
    {
        AnalysisProcessor processor = new AnalysisProcessor( parameters, Assert::assertNull );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "ANALYSIS_REF", "INVALID" );
        ValidationReport report = new ValidationReport();
        MessageCounter counter = MessageCounter.regex(Severity.ERROR,
                WebinCliMessage.ANALYSIS_PROCESSOR_LOOKUP_ERROR.regex());
        report.add(counter);
        processor.process( report, fieldValue );
        Assert.assertFalse( report.isValid() );
        assertThat( report.count( Severity.ERROR ) ).isOne();
        assertThat( counter.getCount()).isOne();
    }

    @Test public void
    testIncorrectList()
    {
        AnalysisProcessor processor = new AnalysisProcessor( parameters, Assert::assertNull );

        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "ANALYSIS_REF", "INVALID1, ERZ690500, INVALID2" );
        ValidationReport report = new ValidationReport();
        MessageCounter counter = MessageCounter.regex(Severity.ERROR,
                WebinCliMessage.ANALYSIS_PROCESSOR_LOOKUP_ERROR.regex());
        report.add(counter);
        processor.process( report, fieldValue );
        Assert.assertFalse( report.isValid() );
        assertThat( report.count( Severity.ERROR ) ).isEqualTo(2);
        assertThat( counter.getCount()).isEqualTo(2);
    }
}
