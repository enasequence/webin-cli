package uk.ac.ebi.ena.manifest.processor;

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.ena.manifest.ManifestFieldType;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.entity.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

import static uk.ac.ebi.ena.manifest.processor.ProcessorTestUtils.createFieldValue;

public class
StudyProcessorTest
{
    WebinCliParameters parameters = new WebinCliParameters();

    @Before
    public void
    before() throws UnsupportedEncodingException
    {
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
        StudyProcessor processor = new StudyProcessor(parameters, (Study study) -> Assert.assertNull(study));

        ManifestFieldValue fieldValue = createFieldValue(ManifestFieldType.META, "STUDY", "ERS000002");
        Assert.assertTrue( processor.process(fieldValue).getSeverity().equals(Severity.ERROR) );
        Assert.assertEquals( "ERS000002", fieldValue.getValue() );
    }
}
