
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
package uk.ac.ebi.ena.webin.cli.version;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.ena.webin.cli.version.HotSpotRuntimeVersion.VersionInfo;

public class 
HotSpotRuntimeVersionTest 
{
    @Test public void
    testVersionInfo()
    {
        //System.getProperties().list( System.out );

        Assert.assertEquals(0, new VersionInfo(1, 8, 0, 155).compareTo(new VersionInfo(1, 8, 0, 155)));
        Assert.assertTrue( new VersionInfo( 1, 8, 0, 155 ).compareTo( new VersionInfo( 1, 8, 0, 154 ) ) > 0 );
        Assert.assertTrue( new VersionInfo( 1, 8, 0, 154 ).compareTo( new VersionInfo( 1, 8, 0, 155 ) ) < 0 );

        Assert.assertTrue( new VersionInfo( 1, 8, 0, 0 ).compareTo( new VersionInfo( 1, 8, 0, 1 ) ) < 0 );
        
        Assert.assertTrue( new VersionInfo(  1, 8, 0, 0 ).compareTo( new VersionInfo( 11, 8, 0, 0 ) ) < 0 );
        Assert.assertTrue( new VersionInfo( 11, 8, 0, 0 ).compareTo( new VersionInfo(  1, 8, 0, 0 ) ) > 0 );
    }
    
    
    @Test public void
    test()
    {
        HotSpotRuntimeVersion jrv = new HotSpotRuntimeVersion();

        //1.8.0_172-b11
        Assert.assertEquals( Integer.valueOf( 1 ), jrv.getVersion( "1.8.0_172-b11" ).major );
        Assert.assertEquals( Integer.valueOf( 8 ), jrv.getVersion( "1.8.0_172-b11" ).minor );
        Assert.assertEquals( Integer.valueOf( 0 ), jrv.getVersion( "1.8.0_172-b11" ).security );
        Assert.assertEquals( Integer.valueOf( "172" ), jrv.getVersion( "1.8.0_172-b11" ).build );

        //11.0.1+13-LTS
        Assert.assertEquals( Integer.valueOf( 11 ), jrv.getVersion( "11.0.1+13-LTS" ).major );
        Assert.assertEquals( Integer.valueOf( 0 ), jrv.getVersion( "11.0.1+13-LTS" ).minor );
        Assert.assertEquals( Integer.valueOf( 1 ), jrv.getVersion( "11.0.1+13-LTS" ).security );
        Assert.assertEquals( Integer.valueOf( "13" ), jrv.getVersion( "11.0.1+13-LTS" ).build );

        Assert.assertNull(jrv.getVersion("something wrong"));
        Assert.assertNull(jrv.getVersion(""));
       
        if( jrv.isHotSpot() )
            Assert.assertTrue( jrv.isComplient() );
    }
}
