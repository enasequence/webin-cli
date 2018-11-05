package uk.ac.ebi.ena.sample;

import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.feature.SourceFeature;

public class
SampleTest 
{
    String passwd;
    String usernm;
    
    @Before
    public void
    before() throws UnsupportedEncodingException
    {
        usernm = System.getenv( "webin-cli-username" );
        passwd = System.getenv( "webin-cli-password" );
        
        Assert.assertNotNull( "please set up environment variable: webin-cli-username", usernm );
        Assert.assertNotNull( "please set up environment variable: webin-cli-password", passwd );
    }
    
    
    @Test public void
    testSourceFeature()
    {
        SourceFeature source = Sample.getSourceFeature( "SAMEA2757765", usernm, passwd, true );
        Assert.assertNotNull( source );

        source = Sample.getSourceFeature( "SAMEA275776", usernm, passwd, true );
        Assert.assertNotNull( source );
      
    }
    
}
