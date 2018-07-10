package uk.ac.ebi.ena.rawreads;

import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.skjegstad.utils.BloomFilter;



public class 
ReadNameSetTest 
{
    String[] sar = new String[ 100_000 ]; 
    
    @Before public void
    before()
    {
        initStringArray( sar, 4, 10 );
    }


    private String[] 
    initStringArray( String[] string_array, int min_string_len, int max_string_len )
    {
        for( int index = 0; index < string_array.length; index ++ )
        {
            string_array[ index ] = ThreadLocalRandom.current()
                                                     .ints( ThreadLocalRandom.current().nextInt( min_string_len, max_string_len ), 33, 127 )
                                                     .mapToObj( e -> String.valueOf( Character.toString( (char)e ) ) )
                                                     .collect( Collectors.joining() );
        }

        Set<String> set = new HashSet<String>( string_array.length );
        for( int index = 0; index < string_array.length; index ++ )
        {
            set.add( string_array[ index ] );
        }
        
        System.out.println( "String degeneration: " + ( string_array.length - set.size() ) );
        return string_array;
    }

    
    @Test public void
    testSingle()
    {
        ReadNameSet<Integer> rns = new ReadNameSet<>( (int)( sar.length ) );
        for( int i = 0; i < sar.length; ++i )
        {
            rns.add( sar[ i ], i );
        }
        
        if( rns.hasPossibleDuplicates() )
        {
            for( int i = 0; i < sar.length; ++i )
            {
                Set<Integer> set = new HashSet<>( rns.getDuplicateLocations( sar[ i ], i ) );
                if( set.isEmpty() )
                    continue;
                set.add( new Integer( i ) );
                Set<Integer> s = new HashSet<>( set.size() );
                s.addAll( set );
                s.remove( new Integer( i ) );
                if( !s.isEmpty() )
                    System.out.printf( "For read no %d [%s] dublicates are %s\n", i, sar[ i ], s.toString() );
                    
            }
        }   
    }
    
   
    @Test public void
    testPaired1()
    {
        String[] sar = initStringArray( new String[ 100_000 ], 32, 64 );
        String[] tar = initStringArray( new String[ 100_000 ], 32, 64 );
        System.out.println( "Sar size: " + Stream.of( sar ).map( e -> e.length() ).collect( Collectors.summarizingInt( e -> e ) ) );
        System.out.println( "Tar size: " + Stream.of( tar ).map( e -> e.length() ).collect( Collectors.summarizingInt( e -> e ) ) );
        
        ReadNameSet<SimpleEntry<String, Long>> rns = new ReadNameSet<>( (int)( sar.length ) );
        for( int i = 0; i < sar.length; ++i )
            rns.add( sar[ i ], new SimpleEntry<String, Long>( String.valueOf( i ), (long)i ) );

        int not_contains = 0;
        
        for( int i = 0; i < tar.length; ++ i )
            not_contains += rns.contains( tar[ i ] ) ? 0 : 1;
        
        System.out.printf( "not found %d records", not_contains );   
        Set<String> set1 = new HashSet<>();
        set1.addAll( Arrays.asList( sar ) );
        Set<String> set2 = new HashSet<>();
        set2.addAll( Arrays.asList( tar ) );
        set1.removeAll( set2 );
       
        Assert.assertTrue( String.format( "%f", not_contains / (double)set1.size() ), ( not_contains / (double)set1.size() ) < 0.9999 );
        
        
    }
    
    
    @Test public void
    testPaired2()
    {
        String[] sar = initStringArray( new String[ 1 ], 32, 64 );
        System.out.println( "Sar size: " + Stream.of( sar ).map( e -> e.length() ).collect( Collectors.summarizingInt( e -> e ) ) );
        
        ReadNameSet<Integer> rns = new ReadNameSet<>( (int)( sar.length ) );
        for( int i = 0; i < sar.length; ++i )
            rns.add( sar[ i ], 1 );

        for( int i = 0; i < sar.length; ++i )
            rns.add( sar[ i ], 2 );

        int not_contains = 0;
        
        for( int i = 0; i < sar.length; ++ i )
            not_contains += rns.getDuplicateLocations( sar[ i ], 1 ).isEmpty() ? 1 : 0;
        
        Assert.assertEquals( 0, not_contains );
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
        //BloomFilter<String> bf = new BloomFilter<String>( 0.001, 100_000 );
        BloomFilter<String> bf = new BloomFilter<String>( 10, 100_000, 7 );
        //BloomFilter<String> bf = new BloomFilter<String>( 16_384, 10_000 );
        int falsep = 0;
        for( int index = 0; index < sar.length; index ++ )
        {
            if( bf.contains( sar[ index ] ) )
                falsep += 1;
            else
                bf.add( sar[ index ] );
        }
        
        System.out.println( "Skjegstad false positive rate: " + falsep );
        
        for( int index = 0; index < sar.length; index ++ )
            Assert.assertTrue( bf.contains( sar[ index ] ) );
        
    }

}
