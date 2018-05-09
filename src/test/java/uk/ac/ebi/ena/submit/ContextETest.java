package uk.ac.ebi.ena.submit;

import org.junit.Assert;
import org.junit.Test;

public class
ContextETest
{
    @Test public void 
    titleTest()
    {
        String name = "MY-YOBA-TITLE";
        for( ContextE c : ContextE.values() )
        {
            String title = c.getTitle( name );
            Assert.assertTrue( title.contains( name ) );
        }
    }
}
