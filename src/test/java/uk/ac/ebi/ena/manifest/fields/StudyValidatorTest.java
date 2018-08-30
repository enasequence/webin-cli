package uk.ac.ebi.ena.manifest.fields;

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.manifest.ManifestFieldType;
import uk.ac.ebi.ena.manifest.ManifestFieldValue;

public class 
StudyValidatorTest 
{
    String passwd;
    String usernm;
    
    @Before
    public void
    before() throws UnsupportedEncodingException
    {
        usernm = System.getenv( "webin-cli-username" );
        passwd = System.getenv( "webin-cli-password" );
    }
    
    
    @Test public void 
    testCorrect()
    {
        ManifestFieldDefinition def = new ManifestFieldDefinition( "", ManifestFieldType.FILE, -1, -1 );
        Assert.assertNull( "Should be null", new StudyValidator( usernm, passwd, true ).validate( new ManifestFieldValue( def, "SRP000392", null ) ) );
    }
    
    
    @Test public void 
    testIncorrect()
    {
        ManifestFieldDefinition def = new ManifestFieldDefinition( "", ManifestFieldType.FILE, -1, -1 );
        Assert.assertNotNull( "Should not be null", new StudyValidator( usernm, passwd, true ).validate( new ManifestFieldValue( def, "ERS000002", null ) ) );
    }
}
