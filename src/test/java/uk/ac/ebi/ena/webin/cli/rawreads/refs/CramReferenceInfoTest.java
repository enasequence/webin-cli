package uk.ac.ebi.ena.webin.cli.rawreads.refs;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.rawreads.refs.CramReferenceInfo.ReferenceInfo;

public class 
CramReferenceInfoTest 
{
    private static final String md5 = "a0d9851da00400dec1098a9255ac712e";
    
    @Test public void
    testFound()
    {
        
        CramReferenceInfo m = new CramReferenceInfo();
        ReferenceInfo info = m.fetchReferenceMetadata( md5, System.out );
        Assert.assertEquals( md5, info.getMd5() );
    }


    @Test public void
    testNotFound()
    {
        CramReferenceInfo m = new CramReferenceInfo();
        ReferenceInfo info = m.fetchReferenceMetadata( md5.substring( 1 ), System.out );
        Assert.assertNull(info.getMd5());
    }
}
