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

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class 
ReadNameSet<T> 
{
    //Bloom bloom;
    private BloomFilter<String> bloom;
    private Map<String, Set<T>> suspected;
    private final boolean collect_suspected;
    private final double edup = 0.01;
    private final AtomicLong adds_no = new AtomicLong();
    private final AtomicLong susp_no = new AtomicLong();
    private final static int COLLECT_MAX = 100_000; 
    
    
    //TODO: Map cannot accomodate more than MAXINT
    
    public
    ReadNameSet( int expected_reads )
    {
        this( expected_reads, true );
    }
    
    
    public 
    ReadNameSet( int expected_reads, boolean collect_suspected )
    {
//        System.out.println( "expected reads: " + expected_reads );
        //this.bloom = new Bloom( edup, expected_reads );
        this.bloom = BloomFilter.create( Funnels.unencodedCharsFunnel(), expected_reads, edup );
        this.suspected = Collections.synchronizedMap( new HashMap<>( (int) ( expected_reads * edup ) ) );
        this.collect_suspected = collect_suspected;
    }


    public void
    add( String read_name, T mark )
    {
        adds_no.incrementAndGet();
        
        if( bloom.mightContain( read_name ) )
        {
            if( collect_suspected && susp_no.get() < COLLECT_MAX )
            {
                Set<T> set = suspected.getOrDefault( read_name, new LinkedHashSet<>() );
                set.add( mark );
                suspected.put( read_name, set );
            }
            susp_no.incrementAndGet();
        } else
            bloom.put( read_name );
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
        return bloom.mightContain( read_name );
    }
}
