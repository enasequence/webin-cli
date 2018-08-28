/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.rawreads;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;


public class 
ControlledValueListTest
{

    @Test public void test()
    {
        Assert.assertTrue( ControlledValueList.Platform.contains( "pacbiosmrt" ) );
        Assert.assertEquals( "PACBIO_SMRT", ControlledValueList.Platform.getKey( "pacbiosmrt" ) );
        
        Assert.assertEquals( "PacificBiosciences platform type for the single molecule real time (SMRT) technology.",
                             ControlledValueList.Platform.getValue( "pacbiosmrt" ) );
    }


    @Test public void listAllTest()
    {
        Stream.of( ControlledValueList.values() ).map( e -> e.toString() ).forEach( System.out::println );
    }

}
