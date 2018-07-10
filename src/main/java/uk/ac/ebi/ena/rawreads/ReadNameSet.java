package uk.ac.ebi.ena.rawreads;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.skjegstad.utils.BloomFilter;

public class 
ReadNameSet<T> 
{
    //Bloom bloom;
    BloomFilter<String> bloom;
    Map<String, Set<T>> suspected;
    final double edup = 0.001;
    final AtomicLong adds_no = new AtomicLong();
    
    ReadNameSet( int expected_reads )
    {
        System.out.println( "expected reads: " + expected_reads );
        //this.bloom = new Bloom( edup, expected_reads );
        this.bloom = new BloomFilter<>( edup, expected_reads );
        this.suspected = Collections.synchronizedMap( new HashMap<>( (int) ( expected_reads * edup ) ) );
    }


    public void
    add( String read_name, T mark )
    {
        adds_no.incrementAndGet();
        
        if( bloom.contains( read_name ) )
        {
            Set<T> set = suspected.getOrDefault( read_name, new HashSet<>() );
            set.add( mark );
            suspected.put( read_name, set );
        } else
            bloom.add( read_name );
    }
    
    
    public long
    getAddsNumber()
    {
        return adds_no.get();
    }
    
    
    public boolean
    hasPossibleDuplicates()
    {
        return !suspected.isEmpty();
    }
    
    
    public Set<T>
    getDuplicateLocations( String read_name, T mark )
    {
        Set<T> set = suspected.getOrDefault( read_name, Collections.emptySet() );
        if( set.isEmpty() )
            return Collections.emptySet();
        
        Set<T> result = new HashSet<>( set );
        result.remove( mark );
        return result.isEmpty() ? Collections.emptySet() : result;
    }


    public boolean 
    contains( String read_name )
    {
        return bloom.contains( read_name );
    }
}
