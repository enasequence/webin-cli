package uk.ac.ebi.ena.rawreads;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.skjegstad.utils.BloomFilter;



public class 
BloomTest 
{
    String[] sar = new String[ 100_000 ]; 
    
    @Before public void
    before()
    {
        for( int index = 0; index < sar.length; index ++ )
        {
            sar[ index ] = ThreadLocalRandom.current().ints( ThreadLocalRandom.current().nextInt( 32, 64 ), 
                                                             33, 127 ).mapToObj( e -> String.valueOf( Character.toString( (char)e ) ) ).collect( Collectors.joining() );
        }

        Set<String> set = new HashSet<String>( sar.length );
        for( int index = 0; index < sar.length; index ++ )
        {
            set.add( sar[ index ] );
        }
        
        System.out.println( "String degeneration: " + ( sar.length - set.size() ) );
    }
    
   
    @Test public void
    modTest()
    {
        BigInteger bi = new BigInteger( "100000" );
        Assert.assertTrue( bi.mod( new BigInteger( "10" ) ).equals( bi.remainder( new BigInteger( "10" ) ) ) );
        
        double falsePositiveProbability = 0.01;
        int num = 10_000_000;
        
        System.out.println( "bits per element: " + Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2))) / Math.log(2) );
        System.out.println( "Expected bitset size: " + (int) num * Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2))) / Math.log(2) );
        System.out.println( "Number of hash functions : " + (int)Math.ceil( -( Math.log( falsePositiveProbability ) / Math.log( 2 ) ) ) );
    }
   
    
    
    @Test public void
    test()
    {
        Bloom b = new Bloom( 10, 1_000_00, 7 );
        int falsep = 0;
        for( int index = 0; index < sar.length; index ++ )
        {
            falsep += b.contains( sar[ index ] ) ? 1: 0;
            b.add( sar[ index ] );
        }
        
        System.out.println( "Smirnov false positive rate: " + falsep );
        
        for( int index = 0; index < sar.length; index ++ )
            Assert.assertTrue( b.contains( sar[ index ] ) );
        
    }

    
    @Test public void
    testSk()
    {
        //BloomFilter<String> bf = new BloomFilter<String>( 0.001, 10_000 );
        BloomFilter<String> bf = new BloomFilter<String>( 0.001, 100_000 );
        //BloomFilter<String> bf = new BloomFilter<String>( 16_384, 10_000 );
        int falsep = 0;
        for( int index = 0; index < sar.length; index ++ )
        {
            falsep += bf.contains( sar[ index ] ) ? 1: 0;
            bf.add( sar[ index ] );
        }
        
        System.out.println( "Skjegstad false positive rate: " + falsep );
        
        for( int index = 0; index < sar.length; index ++ )
            Assert.assertTrue( bf.contains( sar[ index ] ) );
        
    }

}
