package uk.ac.ebi.ena.rawreads;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class 
BloomWrapperTest 
{
    String[] sar = new String[ 100_000 ]; 
    int sar_degeneration = -1;
    
    @Before public void
    before()
    {
        initStringArray( sar, 4, 10 );
        sar_degeneration = getArrayDegeneration( sar );
    }


    @Test( expected = RuntimeException.class ) public void 
    testLimits()
    {
        BloomWrapper rns = new BloomWrapper( 2, 1 );
        rns.add( "1" );
        rns.add( "1" );
        rns.add( "1" );
        rns.add( "2" );
        rns.add( "2" );
    }
    
    
    private String[] 
    initStringArray( String[] string_array, int min_string_len, int max_string_len )
    {
        for( int index = 0; index < string_array.length; index ++ )
        {
            string_array[ index ] = ThreadLocalRandom.current()
                                                     .ints( ThreadLocalRandom.current().nextInt( min_string_len, max_string_len ), 33, 65 )
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
        
        BloomWrapper rns = new BloomWrapper( (int)( sar.length ) / 10 );
        Set<String> set = new LinkedHashSet<>( sar.length );
        List<String> dup = new ArrayList<>( sar.length );
        int nadd = 0;
        for( int i = 0; i < sar.length; ++i )
        {
            boolean s = set.add( sar[ i ] );
            
            if( !s ) 
                dup.add( sar[ i ] );
            
            rns.add( sar[ i ] );
            
            System.out.printf( "%d\tvalue\t%s\tadded\t%b\n", i, sar[ i ], s );
            
        }
        System.out.printf( "total: %d, not-added: %d, set-size: %d\n", sar.length, nadd, set.size() );
        
        Map<String, Integer> dmap = dup.stream().collect( Collectors.toMap( e -> e, e -> Integer.valueOf( 1 ), ( v1, v2 ) -> v1 + v2, LinkedHashMap::new ) );
        
        
        Assert.assertTrue( rns.hasPossibleDuplicates() );
        
        if( rns.hasPossibleDuplicates() )
        {
            Map<String, Set<String>> dups = rns.findAllduplications( sar, sar.length );
            
            int total_count = 0;
            for( int i = 0; i < sar.length; ++i )
            {
                if( dups.containsKey( sar[ i ] ) )
                {
                    if( null == dmap.get( sar[ i ] ) )
                    {
                        total_count += 0;
                    }
                    Set<String> ds = dups.get( sar[ i ]  );
                    total_count += ds.size() - 1;
                    System.out.printf( "Multiple %d (%d) occurance of read %s at: %s\n", ds.size(), dmap.get( sar[ i ] ), sar[ i ], ds );
                    dups.remove( sar[ i ] );
                } 
            }

            
            Assert.assertTrue( String.format(  "total %d, sard: %d\n %s", total_count, sard, Arrays.asList( sar ) ), total_count == sard );
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
        initStringArray( sar, 2, 3 );
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
        
        BloomWrapper rns = new BloomWrapper( (int)( sar.length ) );
        for( int i = 0; i < sar.length; ++i )
            rns.add( sar[ i ] );

        int not_contains = 0;
        
        for( int i = 0; i < tar.length; ++ i )
            not_contains += rns.contains( tar[ i ] ) ? 0 : 1;
        
        System.out.printf( "not found %d records", not_contains );   
        Set<String> set1 = new HashSet<>();
        set1.addAll( Arrays.asList( sar ) );
        Set<String> set2 = new HashSet<>();
        set2.addAll( Arrays.asList( tar ) );
        set1.removeAll( set2 );
       
        Assert.assertTrue( String.format( "Not found in bloom %d, set size %d", not_contains, set1.size() ), 
                                          not_contains >= set1.size() - ( (double)rns.getAddsNumber() * 0.019 /* TODO check for Bloom degradation */) );
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
        
        BloomWrapper rns = new BloomWrapper( (int)( sar.length ) );
        for( int i = 0; i < sar.length; ++i )
            rns.add( sar[ i ] );

        for( int i = 0; i < sar.length; ++i )
            rns.add( sar[ i ] );

        Assert.assertTrue( rns.hasPossibleDuplicates() );
           
        for( int i = 0; i < sar.length; ++ i )
            Assert.assertTrue( "No duplication on pos: " + i, rns.contains( sar[ i ] ) );
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
}
