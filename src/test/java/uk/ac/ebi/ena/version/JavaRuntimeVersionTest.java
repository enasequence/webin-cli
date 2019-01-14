package uk.ac.ebi.ena.version;

import org.junit.Assert;
import org.junit.Test;

public class 
JavaRuntimeVersionTest 
{
   @Test public void
   test()
   {
       JavaRuntimeVersion jrv = new JavaRuntimeVersion();
       
       //1.8.0_172-b11
       Assert.assertEquals( (Float)Float.parseFloat( "1.80" ), jrv.getVersion( "1.8.0_172-b11" ) );

       //11.0.1+13-LTS
       Assert.assertEquals( (Float)Float.parseFloat( "11.01" ), jrv.getVersion( "11.0.1+13-LTS" ) );
       
       Assert.assertEquals( (Float)Float.NaN, jrv.getVersion( "something wrong" ) );
       Assert.assertEquals( (Float)Float.NaN, jrv.getVersion( "" ) );
       
       Assert.assertTrue( jrv.isComplient() );
   }
    
}
