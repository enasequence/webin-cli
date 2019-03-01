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
