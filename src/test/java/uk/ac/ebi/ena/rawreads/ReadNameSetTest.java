package uk.ac.ebi.ena.rawreads;

import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
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
    int sar_degeneration = -1;
    
    @Before public void
    before()
    {
        initStringArray( sar, 4, 10 );
        sar_degeneration = getArrayDegeneration( sar );
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
        
        System.out.println( "String degeneration: " + getArrayDegeneration( string_array ) );
        return string_array;
    }

    
    private int 
    getArrayDegeneration( String[] string_array )
    {
        Set<String> set = new HashSet<String>( string_array.length );
        for( int index = 0; index < string_array.length; index ++ )
        {
            set.add( string_array[ index ] );
        }
    
        return string_array.length - set.size();
    }
    
    
    public void
    __testSingle( String[] sar )
    {
        int sard = getArrayDegeneration( sar );
        
        Assert.assertTrue( sard > 0 );
        
        ReadNameSet<String> rns = new ReadNameSet<>( (int)( sar.length ) );
        for( int i = 0; i < sar.length; ++i )
        {
            rns.add( sar[ i ], getMarker( "file", i ) );
        }
        
        Assert.assertTrue( rns.hasPossibleDuplicates() );
        
        if( rns.hasPossibleDuplicates() )
        {
            Map<String, Set<String>> dups = rns.findAllduplications( sar, sar.length );
            
            int total_count = 0;
            for( int i = 0; i < sar.length; ++i )
            {
                if( dups.containsKey( sar[ i ] ) )
                {
                    Set<String> ds = dups.get( sar[ i ]  );
                    total_count += ds.size();
                    System.out.printf( "Read %s at %d duplicated at: %s\n", sar[ i ], i, ds );
                    dups.remove( sar[ i ] );
                }   
            }
            
            Assert.assertTrue( total_count == sard );
        }   
    }
    
    
    @Test public void
    testSingle1()
    {
        String[] sar = new String[] { "QAZ", "QAZ", "QAZ",
                                      "123", "123", 
                                      "321" };
        __testSingle( sar );
    }

    
    @Test public void
    testSingle2()
    {
        String[] sar = new String[ 1024 ];
        initStringArray( sar, 2, 4 );
        __testSingle( sar );
    }
    
    
    /* attempting to check two randomly generated records with low percentage of degeneration */    
    @Test public void 
    testPaired1()
    {
        String[] sar = initStringArray( new String[ 1_000 ], 32, 64 );
        String[] tar = initStringArray( new String[ 1_000 ], 32, 64 );
        Assert.assertEquals( 0, getArrayDegeneration( sar ) );
        Assert.assertEquals( 0, getArrayDegeneration( tar ) );
        
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
       
        Assert.assertTrue( String.format( "Not found in bloom %d, set size %d", not_contains, set1.size() ), not_contains >= set1.size() - ( (double)rns.getAddsNumber() * 0.01 /* TODO */) );
    }
    
    
    public String 
    getMarker( String file, int i )
    {
        return String.format( "%s, line: %d", file, i );
    }
    
    
    /* attempting to add same array twice */
    @Test public void
    testPaired2()
    {
        String[] sar = initStringArray( new String[ 1 ], 32, 64 );
        System.out.println( "Sar size: " + Stream.of( sar ).map( e -> e.length() ).collect( Collectors.summarizingInt( e -> e ) ) );
        Assert.assertEquals( 0, getArrayDegeneration( sar ) );
        
        ReadNameSet<String> rns = new ReadNameSet<>( (int)( sar.length ) );
        for( int i = 0; i < sar.length; ++i )
            rns.add( sar[ i ], getMarker( "File 1", i ) );

        for( int i = 0; i < sar.length; ++i )
            rns.add( sar[ i ], getMarker( "File 1", i ) );

        Assert.assertTrue( rns.hasPossibleDuplicates() );
           
        for( int i = 0; i < sar.length; ++ i )
            Assert.assertTrue( "No duplication on pos: " + i, !rns.findDuplicateLocations( sar[ i ] ).isEmpty() );
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
