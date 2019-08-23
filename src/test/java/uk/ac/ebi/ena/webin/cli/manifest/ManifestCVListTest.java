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
package uk.ac.ebi.ena.webin.cli.manifest;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader;

public class 
ManifestCVListTest
{
    @Test public void 
    testFromList()
    {
        ManifestCVList cvList = new ManifestCVList("TEST1", "test2");

        Assert.assertTrue( cvList.contains("TEST1") );
        Assert.assertTrue( cvList.contains("test1") );
        Assert.assertEquals( "TEST1", cvList.getKey( "te_st1" ) );
        Assert.assertEquals( "TEST1", cvList.getKey( "test1" ) );
        Assert.assertEquals( "TEST1", cvList.getKey( "TEST1" ) );
        Assert.assertEquals( "TEST1", cvList.getValue( "te_st1" ) );
        Assert.assertEquals( "TEST1", cvList.getValue( "test1" ) );
        Assert.assertEquals( "TEST1", cvList.getValue( "TEST1" ) );

        Assert.assertTrue( cvList.contains("TEST2") );
        Assert.assertTrue( cvList.contains("test2") );
        Assert.assertEquals( "test2", cvList.getKey( "te_st2" ) );
        Assert.assertEquals( "test2", cvList.getKey( "test2" ) );
        Assert.assertEquals( "test2", cvList.getKey( "TEST2" ) );
        Assert.assertEquals( "test2", cvList.getValue( "te_st2" ) );
        Assert.assertEquals( "test2", cvList.getValue( "test2" ) );
        Assert.assertEquals( "test2", cvList.getValue( "TEST2" ) );
    }

    
    @Test public void 
    testFromResource()
    {
        ManifestCVList cvList = ReadsManifestReader.CV_INSTRUMENT;

        Assert.assertTrue( cvList.contains("unspecified") );
        Assert.assertTrue( cvList.contains("UNSPECIFIED") );
        Assert.assertEquals( "unspecified", cvList.getKey( "uns_pecified" ) );
        Assert.assertEquals( "unspecified", cvList.getKey( "unspecified" ) );
        Assert.assertEquals( "unspecified", cvList.getKey( "UNSPECIFIED" ) );
        Assert.assertEquals( "LS454,ILLUMINA,PACBIO_SMRT,ION_TORRENT,OXFORD_NANOPORE", cvList.getValue( "unspecified" ) );
        Assert.assertEquals( "LS454,ILLUMINA,PACBIO_SMRT,ION_TORRENT,OXFORD_NANOPORE", cvList.getValue( "UNSPECIFIED" ) );

        Assert.assertTrue( cvList.contains("Illumina Genome Analyzer") );
        Assert.assertTrue( cvList.contains("illumina genome analyzer") );
        Assert.assertTrue( cvList.contains("ILLUMINA GENOME ANALYZER") );
        Assert.assertEquals( "Illumina Genome Analyzer", cvList.getKey( "Illumina Genome __ Analyzer" ) );
        Assert.assertEquals( "Illumina Genome Analyzer", cvList.getKey( "Illumina Genome Analyzer" ) );
        Assert.assertEquals( "Illumina Genome Analyzer", cvList.getKey( "illumina genome analyzer" ) );
        Assert.assertEquals( "Illumina Genome Analyzer", cvList.getKey( "ILLUMINA GENOME ANALYZER" ) );
        Assert.assertEquals( "ILLUMINA", cvList.getValue( "Illumina Genome Analyzer" ) );
        Assert.assertEquals( "ILLUMINA", cvList.getValue( "illumina genome analyzer" ) );
        Assert.assertEquals( "ILLUMINA", cvList.getValue( "ILLUMINA GENOME ANALYZER" ) );
    }
}
