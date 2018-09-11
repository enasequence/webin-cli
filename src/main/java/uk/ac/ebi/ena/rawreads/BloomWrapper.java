package uk.ac.ebi.ena.rawreads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class 
BloomWrapper 
{
    //Bloom bloom;
    private BloomFilter<String> bloom;
    private final double edup = 0.01;
    
    private final AtomicLong adds_no = new AtomicLong();
    private final AtomicLong susp_no = new AtomicLong();
    private final static int COLLECT_MAX = 100_000; 
    private Set<String> suspected = new HashSet<>( COLLECT_MAX );
    
    
    public 
    BloomWrapper( int expected_reads )
    {
        this.bloom = BloomFilter.create( Funnels.unencodedCharsFunnel(), expected_reads, edup );
    }


    public void
    add( String read_name )
    {
        adds_no.incrementAndGet();
        
        if( bloom.mightContain( read_name ) )
        {
            susp_no.incrementAndGet();
            if( suspected.size() < COLLECT_MAX )
                suspected.add( read_name );
        } else
        {
            bloom.put( read_name );
        }
    }
    
    
    public long
    getAddsNumber()
    {
        return adds_no.get();
    }
    
    
    public boolean
    hasPossibleDuplicates()
    {
        return getPossibleDuplicateCount() > 0;
    }
    
    
    public Long
    getPossibleDuplicateCount()
    {
        return susp_no.get();
    }

    
    public Set<String>
    getSuspected()
    {
        return suspected;
    }
    
    
    public Map<String, Set<String>>
    findAllduplications( String[] read_names, int limit )
    {
        Map<String, Integer>      counts = new HashMap<>( limit );
        Map<String, Set<String>>  result = new HashMap<>( limit );
        
        if( !hasPossibleDuplicates() )
            return result;
        
        AtomicInteger index = new AtomicInteger();
        for( String read_name : read_names )
        {
            index.incrementAndGet();
            if( contains( read_name ) )
            {
                counts.put( read_name, counts.getOrDefault( read_name, 0 ) + 1 );
                Set<String> dlist = result.getOrDefault( read_name, new LinkedHashSet<>() ); //findDuplicateLocations( read_name );
                dlist.add( String.format( "%s", index.get() ) );
                result.put( read_name, dlist );
            }
            //TODO
            if( result.size() >= limit )
                break;
        }
        //return result;
        return result.entrySet().stream().filter( e-> counts.get( e.getKey() ).intValue() > 1 ).collect( Collectors.toMap( e -> e.getKey(), e -> e.getValue() ) );
    }
    
   
    public boolean 
    contains( String read_name )
    {
        return bloom.mightContain( read_name );
    }
}
