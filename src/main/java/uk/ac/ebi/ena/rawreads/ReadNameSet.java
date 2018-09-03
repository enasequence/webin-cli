package uk.ac.ebi.ena.rawreads;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
            Set<T> set = suspected.getOrDefault( read_name, new LinkedHashSet<>() );
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
    getDuplicateLocations( String read_name )
    {
        Set<T> set = suspected.getOrDefault( read_name, Collections.emptySet() );
        if( set.isEmpty() )
            return Collections.emptySet();
        return set.isEmpty() ? Collections.emptySet() : set;
    }

    
    public Map<String, Set<T>>
    getAllduplications( String[] read_names, int limit )
    {
        Map<String, Set<T>> result = new HashMap<>( limit );
        for( String read_name : read_names )
        {
            if( hasPossibleDuplicates() )
            {
                Set<T> dlist = getDuplicateLocations( read_name );
                if( !dlist.isEmpty() )
                    result.put( read_name, dlist );
            }
            
            if( result.size() >= limit )
                break;
        }
        return result;
    }
    
    
    public Map<String, Set<T>>
    getAllduplications( Iterator<String> read_name_iterator, int limit )
    {
        Map<String, Set<T>> result = new HashMap<>( limit );
        while( read_name_iterator.hasNext() )
        {
            String read_name = read_name_iterator.next();
            if( hasPossibleDuplicates() )
            {
                Set<T> dlist = getDuplicateLocations( read_name );
                if( !dlist.isEmpty() )
                    result.put( read_name, dlist );
            }
            
            if( result.size() >= limit )
                break;
        }
        return result;
    }
    
    
    public boolean 
    contains( String read_name )
    {
        return bloom.contains( read_name );
    }
}
