/*
 * Copyright 2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.webin.cli.manifest.processor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.entity.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

import static uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils.createFieldValue;

public class
StudyProcessorTest
{
    private final WebinCliParameters parameters = new WebinCliParameters();

    @Before
    public void
    before() {
        parameters.setUsername(System.getenv( "webin-cli-username" ));
        parameters.setPassword(System.getenv( "webin-cli-password" ));
        parameters.setTestMode(true);
    }

    @Test public void
    testCorrect()
    {
        StudyProcessor processor = new StudyProcessor(parameters, (Study study) -> Assert.assertEquals("PRJNA28545", study.getProjectId()));

        ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "STUDY", "SRP000392");
        Assert.assertNull( processor.process(fieldValue) );
        Assert.assertEquals( "PRJNA28545", fieldValue.getValue() );
    }

    @Test public void
    testIncorrect()
    {
        StudyProcessor processor = new StudyProcessor(parameters, Assert::assertNull);

        ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "STUDY", "ERS000002");
        Assert.assertEquals(processor.process(fieldValue).getSeverity(), Severity.ERROR);
        Assert.assertEquals( "ERS000002", fieldValue.getValue() );
    }
}
