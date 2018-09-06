package uk.ac.ebi.ena.rawreads;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.skjegstad.utils.BloomFilter;

public class 
ReadNameSet<T> 
{
    //Bloom bloom;
    BloomFilter<String> bloom;
    Map<String, Set<T>> suspected;
    final double edup = 0.001;
    final AtomicLong adds_no = new AtomicLong();
    final AtomicLong susp_no = new AtomicLong();
    
    
    //TODO: Map cannot accomodate more than MAXINT
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
            susp_no.incrementAndGet();
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
    
    
    public Long
    getPossibleDuplicateCount()
    {
        return susp_no.get();
    }

    
    public Set<T>
    findDuplicateLocations( String read_name )
    {
        Set<T> set = suspected.getOrDefault( read_name, Collections.emptySet() );
        if( set.isEmpty() )
            return Collections.emptySet();
        return set.isEmpty() ? Collections.emptySet() : set;
    }

    
    public Map<String, Set<T>>
    findAllduplications( String[] read_names, int limit )
    {
        Map<String, Set<T>> result = new HashMap<>( limit );
        for( String read_name : read_names )
        {
            if( hasPossibleDuplicates() )
            {
                Set<T> dlist = findDuplicateLocations( read_name );
                if( !dlist.isEmpty() )
                    result.put( read_name, dlist );
            }
            
            if( result.size() >= limit )
                break;
        }
        return result;
    }
    

    public Map<String, Set<T>>
    findAllduplications( Iterator<String> read_name_iterator, int limit )
    {
        Map<String, Set<T>> result = new LinkedHashMap<>( limit );
        Map<String, Long> first_seen = new LinkedHashMap<>( limit );
        long index = 1;
        while( read_name_iterator.hasNext() )
        {
            String read_name = read_name_iterator.next();
            if( hasPossibleDuplicates() )
            {
                Set<T> dlist = findDuplicateLocations( read_name );
                if( !dlist.isEmpty() )
                {
                    first_seen.putIfAbsent( read_name, index );
                    result.put( read_name, dlist );
                }
            }
            index ++;
            if( result.size() >= limit )
                break;
        }
        return result.entrySet().stream().collect( Collectors.toMap( e -> String.format( "%s, Read no: %d", e.getKey(), first_seen.get( e.getKey() ) ), 
                                                                     e -> e.getValue(),
                                                                     ( e1, e2 ) -> e1,
                                                                     LinkedHashMap::new ) );
    }
    
    
    public boolean 
    contains( String read_name )
    {
        return bloom.contains( read_name );
    }
}
