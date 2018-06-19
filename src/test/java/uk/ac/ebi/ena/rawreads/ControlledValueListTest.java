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
