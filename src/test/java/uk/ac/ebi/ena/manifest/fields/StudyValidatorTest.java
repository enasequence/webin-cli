package uk.ac.ebi.ena.manifest.fields;

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.manifest.ManifestFieldType;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class 
StudyValidatorTest 
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
        ManifestFieldDefinition def = new ManifestFieldDefinition( "", ManifestFieldType.FILE, -1, -1 );
        Assert.assertNull( "Should be null", new StudyValidator( parameters ).validate( new ManifestFieldValue( def, "SRP000392", null ) ) );
    }
    
    
    @Test public void 
    testIncorrect()
    {
        ManifestFieldDefinition def = new ManifestFieldDefinition( "", ManifestFieldType.FILE, -1, -1 );
        Assert.assertNotNull( "Should not be null", new StudyValidator( parameters ).validate( new ManifestFieldValue( def, "ERS000002", null ) ) );
    }
}
